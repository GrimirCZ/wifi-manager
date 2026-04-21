package ipmapping

import (
	"context"
	"slices"
	"sync/atomic"
	"time"

	"github.com/tidwall/btree"
)

type NeighborStatus string

const (
	NeighborStatusLive  NeighborStatus = "live"
	NeighborStatusStale NeighborStatus = "stale"
)

var nowUTC = func() time.Time {
	return time.Now().UTC()
}

type Update struct {
	IP            string
	MAC           string
	InterfaceName string
	Status        NeighborStatus
	LastSeenAt    time.Time
	Deleted       bool
}

type ClientView struct {
	MAC        string
	IPs        []string
	Status     NeighborStatus
	LastSeenAt time.Time
}

type Provider interface {
	Start() error
	Updates() <-chan Update
	LookupMAC(ip string) (string, bool)
	IPsForMAC(mac string) []string
	ListClients() []ClientView
}

const UpdateChannelSize = 1024

// ipToMAC is the point-lookup index entry for one observed IP address.
// Within a published snapshot, one IP must resolve to at most one MAC.
type ipToMAC struct {
	IP            string
	MAC           string
	InterfaceName string
	Status        NeighborStatus
	LastSeenAt    time.Time
}

// macToClient is the grouped-scan index entry for one observed MAC address.
// IPs must stay sorted and deduplicated and represent only the agent's current observed view.
type macToClient struct {
	MAC string
	IPs []string
}

// rawEvent is the normalized event shape before it is applied to the store.
// The same form is used for bootstrap replay and live netlink updates.
type rawEvent struct {
	IP            string
	MAC           string
	InterfaceName string
	Status        NeighborStatus
	LastSeenAt    time.Time
	Deleted       bool
}

// snapshot is one immutable observed-state version.
// byIP and byMAC must describe the same state and must be updated together before publication.
type snapshot struct {
	byIP  *btree.BTreeG[ipToMAC]     // byIP is the point-lookup index used for IP -> MAC resolution.
	byMAC *btree.BTreeG[macToClient] // byMAC is the grouped index used for stable MAC-ordered scans.
}

// store owns snapshot publication and the single mutation path.
// All observed-state writes must flow through writeCh, and updates must only be emitted after state is published.
type store struct {
	state   atomic.Pointer[snapshot] // state always points to the latest published immutable snapshot.
	updates chan Update              // updates carries only deltas that are already reflected in state.
	writeCh chan func(*writerState)  // writeCh is the only path that may mutate observed state.
}

// writerState exists only on the writer goroutine.
// current is the writer-owned working version that may be replaced and then published.
type writerState struct {
	store   *store
	current *snapshot // current must never be mutated outside the writer goroutine.
}

// newStore publishes the initial empty snapshot and starts the writer actor
// that serializes all observed-state mutations.
func newStore(ctx context.Context, updates chan Update) *store {
	if ctx == nil {
		ctx = context.Background()
	}

	s := &store{
		updates: updates,
		writeCh: make(chan func(*writerState), UpdateChannelSize),
	}
	initial := newSnapshot()
	s.state.Store(initial)

	go s.runWriter(ctx, initial)
	return s
}

// runWriter is the only execution context allowed to mutate observed state.
// Each queued closure sees the latest writer-owned snapshot and may publish a new one.
func (s *store) runWriter(ctx context.Context, initial *snapshot) {
	state := &writerState{
		store:   s,
		current: initial,
	}

	for {
		select {
		case <-ctx.Done():
			return
		case fn, ok := <-s.writeCh:
			if !ok {
				return
			}
			if fn == nil {
				continue
			}
			fn(state)
		}
	}
}

func (s *store) load() *snapshot {
	current := s.state.Load()
	if current != nil {
		return current
	}
	empty := newSnapshot()
	s.state.Store(empty)
	return empty
}

// applySync sends a mutation closure to the writer actor and waits until the
// closure has completed, including any snapshot publication it performs.
func (s *store) applySync(fn func(*writerState)) {
	done := make(chan struct{})
	s.writeCh <- func(state *writerState) {
		fn(state)
		close(done)
	}
	<-done
}

// enqueue schedules a mutation on the writer actor without waiting for completion.
func (s *store) enqueue(fn func(*writerState)) {
	s.writeCh <- fn
}

func (s *store) update(ipStr, macStr, interfaceName string) (string, string, bool) {
	var ip string
	var mac string
	var changed bool
	s.applySync(func(state *writerState) {
		ip, mac, changed = state.applyUpsert(ipStr, macStr, interfaceName)
	})
	return ip, mac, changed
}

func (s *store) removeByIP(ipStr string) (string, bool) {
	var mac string
	var changed bool
	s.applySync(func(state *writerState) {
		mac, changed = state.applyDelete(ipStr)
	})
	return mac, changed
}

func (s *store) lookupMAC(ip string) (string, bool) {
	return s.load().lookupMAC(ip)
}

func (s *store) lookupEntry(ip string) (ipToMAC, bool) {
	return s.load().lookupEntry(ip)
}

func (s *store) ipsForMAC(mac string) []string {
	return s.load().ipsForMAC(mac)
}

func (s *store) listClients() []ClientView {
	return s.load().listClients()
}

func newSnapshot() *snapshot {
	return &snapshot{
		byIP:  btree.NewBTreeGOptions[ipToMAC](lessIPToMAC, btree.Options{NoLocks: true}),
		byMAC: btree.NewBTreeGOptions[macToClient](lessMACToClient, btree.Options{NoLocks: true}),
	}
}

func lessIPToMAC(a, b ipToMAC) bool {
	return a.IP < b.IP
}

func lessMACToClient(a, b macToClient) bool {
	return a.MAC < b.MAC
}

func (s *snapshot) lookupMAC(ip string) (string, bool) {
	item, ok := s.byIP.Get(ipToMAC{IP: ip})
	if !ok {
		return "", false
	}
	return item.MAC, true
}

func (s *snapshot) lookupEntry(ip string) (ipToMAC, bool) {
	item, ok := s.byIP.Get(ipToMAC{IP: ip})
	if !ok {
		return ipToMAC{}, false
	}
	return item, true
}

func (s *snapshot) ipsForMAC(mac string) []string {
	item, ok := s.byMAC.Get(macToClient{MAC: mac})
	if !ok || len(item.IPs) == 0 {
		return nil
	}
	return slices.Clone(item.IPs)
}

func (s *snapshot) hasMAC(mac string) bool {
	item, ok := s.byMAC.Get(macToClient{MAC: mac})
	return ok && len(item.IPs) > 0
}

func (s *snapshot) listClients() []ClientView {
	clients := make([]ClientView, 0, s.byMAC.Len())
	s.byMAC.Scan(func(item macToClient) bool {
		status := NeighborStatusStale
		var lastSeenAt time.Time
		for _, ip := range item.IPs {
			entry, ok := s.lookupEntry(ip)
			if !ok {
				continue
			}
			if entry.Status == NeighborStatusLive {
				status = NeighborStatusLive
			}
			if entry.LastSeenAt.After(lastSeenAt) {
				lastSeenAt = entry.LastSeenAt
			}
		}
		clients = append(clients, ClientView{
			MAC:        item.MAC,
			IPs:        slices.Clone(item.IPs),
			Status:     status,
			LastSeenAt: lastSeenAt,
		})
		return true
	})
	if len(clients) == 0 {
		return nil
	}
	return clients
}

// withUpsert applies one observed IP -> MAC mapping as a path-copy CoW update
// across both indices and returns a new immutable snapshot only when state changes.
func (s *snapshot) withUpsert(ipStr, macStr, interfaceName string, observedAt time.Time) (*snapshot, Update, bool) {
	if ipStr == "" || macStr == "" {
		return s, Update{}, false
	}
	if observedAt.IsZero() {
		observedAt = nowUTC()
	}

	current, exists := s.lookupEntry(ipStr)
	if exists &&
		current.MAC == macStr &&
		current.InterfaceName == interfaceName &&
		current.Status == NeighborStatusLive &&
		current.LastSeenAt.Equal(observedAt) {
		return s, Update{}, false
	}

	next := &snapshot{
		byIP:  s.byIP.Copy(),
		byMAC: s.byMAC.Copy(),
	}

	if exists && current.MAC != macStr {
		removeIPFromClientTree(next.byMAC, current.MAC, ipStr)
	}

	next.byIP.Set(ipToMAC{
		IP:            ipStr,
		MAC:           macStr,
		InterfaceName: interfaceName,
		Status:        NeighborStatusLive,
		LastSeenAt:    observedAt,
	})
	upsertIPIntoClientTree(next.byMAC, macStr, ipStr)

	return next, Update{
		IP:            ipStr,
		MAC:           macStr,
		InterfaceName: interfaceName,
		Status:        NeighborStatusLive,
		LastSeenAt:    observedAt,
	}, true
}

func (s *snapshot) withMarkStale(ipStr string) (*snapshot, Update, bool) {
	if ipStr == "" {
		return s, Update{}, false
	}

	current, exists := s.lookupEntry(ipStr)
	if !exists || current.Status == NeighborStatusStale {
		return s, Update{}, false
	}

	next := &snapshot{
		byIP:  s.byIP.Copy(),
		byMAC: s.byMAC.Copy(),
	}
	current.Status = NeighborStatusStale
	next.byIP.Set(current)

	return next, Update{
		IP:            current.IP,
		MAC:           current.MAC,
		InterfaceName: current.InterfaceName,
		Status:        NeighborStatusStale,
		LastSeenAt:    current.LastSeenAt,
	}, true
}

// withDelete removes one observed IP as a path-copy CoW update across both
// indices and returns a new immutable snapshot only when state changes.
func (s *snapshot) withDelete(ipStr string) (*snapshot, Update, bool) {
	if ipStr == "" {
		return s, Update{}, false
	}

	current, exists := s.lookupEntry(ipStr)
	if !exists {
		return s, Update{}, false
	}

	next := &snapshot{
		byIP:  s.byIP.Copy(),
		byMAC: s.byMAC.Copy(),
	}
	next.byIP.Delete(ipToMAC{IP: ipStr})
	removeIPFromClientTree(next.byMAC, current.MAC, ipStr)

	return next, Update{
		IP:            ipStr,
		MAC:           current.MAC,
		InterfaceName: current.InterfaceName,
		Status:        current.Status,
		LastSeenAt:    current.LastSeenAt,
		Deleted:       true,
	}, true
}

func (s *snapshot) withEvent(event rawEvent) (*snapshot, Update, bool) {
	if event.Deleted {
		return s.withDelete(event.IP)
	}

	if event.Status == NeighborStatusStale {
		next := s
		if event.IP != "" && event.MAC != "" {
			next, _, _ = next.withUpsert(event.IP, event.MAC, event.InterfaceName, event.LastSeenAt)
		}
		return next.withMarkStale(event.IP)
	}

	return s.withUpsert(event.IP, event.MAC, event.InterfaceName, event.LastSeenAt)
}

func upsertIPIntoClientTree(tree *btree.BTreeG[macToClient], mac, ip string) {
	client, ok := tree.Get(macToClient{MAC: mac})
	if !ok {
		tree.Set(macToClient{
			MAC: mac,
			IPs: []string{ip},
		})
		return
	}

	index, found := slices.BinarySearch(client.IPs, ip)
	if found {
		return
	}
	client.IPs = slices.Insert(client.IPs, index, ip)
	tree.Set(client)
}

func removeIPFromClientTree(tree *btree.BTreeG[macToClient], mac, ip string) {
	client, ok := tree.Get(macToClient{MAC: mac})
	if !ok {
		return
	}

	index, found := slices.BinarySearch(client.IPs, ip)
	if !found {
		return
	}
	client.IPs = slices.Delete(client.IPs, index, index+1)
	if len(client.IPs) == 0 {
		tree.Delete(macToClient{MAC: mac})
		return
	}
	tree.Set(client)
}

func (w *writerState) publish(next *snapshot) {
	if next == nil || next == w.current {
		return
	}
	w.current = next
	w.store.state.Store(next)
}

func (w *writerState) emit(update Update) {
	if w.store.updates == nil {
		return
	}
	select {
	case w.store.updates <- update:
	default:
	}
}

func (w *writerState) applyUpsert(ipStr, macStr, interfaceName string) (string, string, bool) {
	next, update, changed := w.current.withUpsert(ipStr, macStr, interfaceName, nowUTC())
	if !changed {
		return "", "", false
	}
	w.publish(next)
	w.emit(update)
	return update.IP, update.MAC, true
}

func (w *writerState) applyMarkStale(ipStr string) (string, bool) {
	next, update, changed := w.current.withMarkStale(ipStr)
	if !changed {
		return "", false
	}
	w.publish(next)
	w.emit(update)
	return update.MAC, true
}

func (w *writerState) applyDelete(ipStr string) (string, bool) {
	next, update, changed := w.current.withDelete(ipStr)
	if !changed {
		return "", false
	}
	w.publish(next)
	w.emit(update)
	return update.MAC, true
}

func (w *writerState) applyEvent(event rawEvent) (Update, bool) {
	next, update, changed := w.current.withEvent(event)
	if !changed {
		return Update{}, false
	}
	w.publish(next)
	w.emit(update)
	return update, true
}

// bootstrap installs the full scan result as the base snapshot, replays the
// buffered gap events in order, then emits replayed deltas after publication.
func (w *writerState) bootstrap(initial []rawEvent, replay []rawEvent) {
	next := newSnapshot()
	for _, event := range initial {
		if event.Deleted {
			continue
		}
		next, _, _ = next.withUpsert(event.IP, event.MAC, event.InterfaceName, event.LastSeenAt)
		if event.Status == NeighborStatusStale {
			next, _, _ = next.withMarkStale(event.IP)
		}
	}

	replayed := make([]Update, 0, len(replay))
	for _, event := range replay {
		updateSnapshot, update, changed := next.withEvent(event)
		next = updateSnapshot
		if changed {
			replayed = append(replayed, update)
		}
	}

	w.publish(next)
	for _, update := range replayed {
		w.emit(update)
	}
}
