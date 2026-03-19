package ipmapping

import (
	"context"
	"slices"
	"sync/atomic"

	"github.com/tidwall/btree"
)

type Update struct {
	IP      string
	MAC     string
	Deleted bool
}

type ClientView struct {
	MAC string
	IPs []string
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
	IP  string
	MAC string
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
	IP      string
	MAC     string
	Deleted bool
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

func (s *store) update(ipStr, macStr string) (string, string, bool) {
	var ip string
	var mac string
	var changed bool
	s.applySync(func(state *writerState) {
		ip, mac, changed = state.applyUpsert(ipStr, macStr)
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

func (s *snapshot) ipsForMAC(mac string) []string {
	item, ok := s.byMAC.Get(macToClient{MAC: mac})
	if !ok || len(item.IPs) == 0 {
		return nil
	}
	return slices.Clone(item.IPs)
}

func (s *snapshot) listClients() []ClientView {
	clients := make([]ClientView, 0, s.byMAC.Len())
	s.byMAC.Scan(func(item macToClient) bool {
		clients = append(clients, ClientView{
			MAC: item.MAC,
			IPs: slices.Clone(item.IPs),
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
func (s *snapshot) withUpsert(ipStr, macStr string) (*snapshot, Update, bool) {
	if ipStr == "" || macStr == "" {
		return s, Update{}, false
	}

	currentMAC, exists := s.lookupMAC(ipStr)
	if exists && currentMAC == macStr {
		return s, Update{}, false
	}

	next := &snapshot{
		byIP:  s.byIP.Copy(),
		byMAC: s.byMAC.Copy(),
	}

	if exists {
		removeIPFromClientTree(next.byMAC, currentMAC, ipStr)
	}

	next.byIP.Set(ipToMAC{IP: ipStr, MAC: macStr})
	upsertIPIntoClientTree(next.byMAC, macStr, ipStr)

	return next, Update{IP: ipStr, MAC: macStr}, true
}

// withDelete removes one observed IP as a path-copy CoW update across both
// indices and returns a new immutable snapshot only when state changes.
func (s *snapshot) withDelete(ipStr string) (*snapshot, Update, bool) {
	if ipStr == "" {
		return s, Update{}, false
	}

	currentMAC, exists := s.lookupMAC(ipStr)
	if !exists {
		return s, Update{}, false
	}

	next := &snapshot{
		byIP:  s.byIP.Copy(),
		byMAC: s.byMAC.Copy(),
	}
	next.byIP.Delete(ipToMAC{IP: ipStr})
	removeIPFromClientTree(next.byMAC, currentMAC, ipStr)

	return next, Update{IP: ipStr, MAC: currentMAC, Deleted: true}, true
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

func (w *writerState) applyUpsert(ipStr, macStr string) (string, string, bool) {
	next, update, changed := w.current.withUpsert(ipStr, macStr)
	if !changed {
		return "", "", false
	}
	w.publish(next)
	w.emit(update)
	return update.IP, update.MAC, true
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

// bootstrap installs the full scan result as the base snapshot, replays the
// buffered gap events in order, then emits replayed deltas after publication.
func (w *writerState) bootstrap(initial []rawEvent, replay []rawEvent) {
	next := newSnapshot()
	for _, event := range initial {
		if event.Deleted {
			continue
		}
		next, _, _ = next.withUpsert(event.IP, event.MAC)
	}

	replayed := make([]Update, 0, len(replay))
	for _, event := range replay {
		var (
			update  Update
			changed bool
		)
		if event.Deleted {
			next, update, changed = next.withDelete(event.IP)
		} else {
			next, update, changed = next.withUpsert(event.IP, event.MAC)
		}
		if changed {
			replayed = append(replayed, update)
		}
	}

	w.publish(next)
	for _, update := range replayed {
		w.emit(update)
	}
}
