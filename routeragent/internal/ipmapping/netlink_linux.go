//go:build linux

package ipmapping

import (
	"context"
	"fmt"
	"log"
	"slices"
	"strconv"
	"strings"
	"sync"

	"github.com/vishvananda/netlink"
	"golang.org/x/sys/unix"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/normalize"
)

// NetlinkProvider bridges the kernel neighbor table into the observed-state
// store and closes the subscribe/scan startup gap by buffering early events.
type NetlinkProvider struct {
	ctx               context.Context
	updates           chan Update
	store             *store
	managedInterfaces []string
	managedIfIndexes  map[int]string

	bufferMu sync.Mutex
	live     bool
	pending  []rawEvent
}

func NewNetlinkProvider(ctx context.Context, managedInterfaces []string) *NetlinkProvider {
	updates := make(chan Update, UpdateChannelSize)
	return &NetlinkProvider{
		ctx:               ctx,
		updates:           updates,
		store:             newStore(ctx, updates),
		managedInterfaces: slices.Clone(managedInterfaces),
	}
}

// Start subscribes first, scans second, then replays buffered notifications so
// no netlink updates are lost before live enqueueing begins.
func (n *NetlinkProvider) Start() error {
	ifIndexes, err := resolveManagedInterfaces(n.managedInterfaces)
	if err != nil {
		return err
	}
	n.managedIfIndexes = ifIndexes

	neighUpdates := make(chan netlink.NeighUpdate, UpdateChannelSize)
	done := make(chan struct{})

	if err := netlink.NeighSubscribe(neighUpdates, done); err != nil {
		return err
	}

	go func() {
		defer close(done)
		for {
			select {
			case <-n.ctx.Done():
				return
			case update, ok := <-neighUpdates:
				if !ok {
					return
				}
				if !n.allowsLinkIndex(update.Neigh.LinkIndex) {
					continue
				}
				event, ok := normalizeRawUpdate(update, n.managedIfIndexes)
				if !ok {
					continue
				}
				n.handleRawEvent(event)
			}
		}
	}()

	initial, err := initialNeighborSnapshot(n.managedIfIndexes)
	if err != nil {
		return err
	}

	n.bufferMu.Lock()
	replay := append([]rawEvent(nil), n.pending...)
	doneBootstrap := make(chan struct{})
	n.store.enqueue(func(state *writerState) {
		state.bootstrap(initial, replay)
		close(doneBootstrap)
	})
	n.live = true
	n.pending = nil
	n.bufferMu.Unlock()

	<-doneBootstrap
	log.Printf("%s (initial=%d replayed=%d)", n.initializationMessage(), len(initial), len(replay))
	return nil
}

// handleRawEvent buffers notifications until bootstrap completes and enqueues
// them directly on the writer actor once live mode is enabled.
func (n *NetlinkProvider) handleRawEvent(event rawEvent) {
	n.bufferMu.Lock()
	if !n.live {
		n.pending = append(n.pending, event)
		n.bufferMu.Unlock()
		return
	}
	n.bufferMu.Unlock()

	n.store.enqueue(func(state *writerState) {
		if event.Deleted {
			state.applyDelete(event.IP)
			return
		}
		state.applyUpsert(event.IP, event.MAC, event.InterfaceName)
	})
}

// initialNeighborSnapshot captures the current neighbor-table seed that becomes
// the base snapshot before buffered gap events are replayed.
func initialNeighborSnapshot(managedIfIndexes map[int]string) ([]rawEvent, error) {
	families := []int{unix.AF_INET, unix.AF_INET6}
	events := make([]rawEvent, 0)

	for _, family := range families {
		neighs, err := netlink.NeighList(0, family)
		if err != nil {
			return nil, err
		}
		for _, neigh := range neighs {
			if !allowsLinkIndexFromSet(managedIfIndexes, neigh.LinkIndex) {
				continue
			}
			event, ok := normalizeNeighbor(neigh, managedIfIndexes)
			if !ok || event.Deleted {
				continue
			}
			events = append(events, event)
		}
	}

	return events, nil
}

func normalizeRawUpdate(update netlink.NeighUpdate, managedIfIndexes map[int]string) (rawEvent, bool) {
	ip, ok := normalize.IPFromNet(update.Neigh.IP)
	if !ok {
		return rawEvent{}, false
	}
	if update.Type == unix.RTM_DELNEIGH {
		return rawEvent{IP: ip, InterfaceName: linkNameForIndex(update.Neigh.LinkIndex, managedIfIndexes), Deleted: true}, true
	}
	if !isLiveNeighborState(update.Neigh.State) {
		return rawEvent{IP: ip, InterfaceName: linkNameForIndex(update.Neigh.LinkIndex, managedIfIndexes), Deleted: true}, true
	}

	mac, ok := normalize.MACFromNet(update.Neigh.HardwareAddr)
	if !ok {
		return rawEvent{}, false
	}
	return rawEvent{
		IP:            ip,
		MAC:           mac,
		InterfaceName: linkNameForIndex(update.Neigh.LinkIndex, managedIfIndexes),
	}, true
}

func normalizeNeighbor(neigh netlink.Neigh, managedIfIndexes map[int]string) (rawEvent, bool) {
	ip, ok := normalize.IPFromNet(neigh.IP)
	if !ok {
		return rawEvent{}, false
	}
	if !isLiveNeighborState(neigh.State) {
		return rawEvent{}, false
	}
	mac, ok := normalize.MACFromNet(neigh.HardwareAddr)
	if !ok {
		return rawEvent{}, false
	}
	return rawEvent{
		IP:            ip,
		MAC:           mac,
		InterfaceName: linkNameForIndex(neigh.LinkIndex, managedIfIndexes),
	}, true
}

func linkNameForIndex(linkIndex int, managedIfIndexes map[int]string) string {
	if linkIndex == 0 {
		return ""
	}
	if managedIfIndexes != nil {
		if name, ok := managedIfIndexes[linkIndex]; ok {
			return name
		}
	}
	link, err := netlink.LinkByIndex(linkIndex)
	if err != nil {
		return ""
	}
	if attrs := link.Attrs(); attrs != nil {
		return attrs.Name
	}
	return ""
}

func (n *NetlinkProvider) Updates() <-chan Update {
	return n.updates
}

func (n *NetlinkProvider) LookupMAC(ip string) (string, bool) {
	return n.store.lookupMAC(ip)
}

func (n *NetlinkProvider) IPsForMAC(mac string) []string {
	return n.store.ipsForMAC(mac)
}

func (n *NetlinkProvider) ListClients() []ClientView {
	return n.store.listClients()
}

func (n *NetlinkProvider) allowsLinkIndex(linkIndex int) bool {
	return allowsLinkIndexFromSet(n.managedIfIndexes, linkIndex)
}

func allowsLinkIndexFromSet(managedIfIndexes map[int]string, linkIndex int) bool {
	if len(managedIfIndexes) == 0 {
		return true
	}
	_, ok := managedIfIndexes[linkIndex]
	return ok
}

func resolveManagedInterfaces(names []string) (map[int]string, error) {
	if len(names) == 0 {
		return nil, nil
	}

	indexes := make(map[int]string, len(names))
	for _, name := range names {
		link, err := netlink.LinkByName(name)
		if err != nil {
			return nil, fmt.Errorf("resolve managed interface %q: %w", name, err)
		}
		indexes[link.Attrs().Index] = name
	}
	return indexes, nil
}

func (n *NetlinkProvider) initializationMessage() string {
	if len(n.managedIfIndexes) == 0 {
		return "netlink observed-state initialization complete; accepting live notifications on all interfaces"
	}

	parts := make([]string, 0, len(n.managedInterfaces))
	for _, name := range n.managedInterfaces {
		for index, currentName := range n.managedIfIndexes {
			if currentName != name {
				continue
			}
			parts = append(parts, name+"#"+strconv.Itoa(index))
			break
		}
	}
	return "netlink observed-state initialization complete; accepting live notifications on managed interfaces=" + strings.Join(parts, ",")
}

func isLiveNeighborState(state int) bool {
	if state&netlink.NUD_REACHABLE != 0 {
		return true
	}
	if state&netlink.NUD_DELAY != 0 {
		return true
	}
	if state&netlink.NUD_PROBE != 0 {
		return true
	}
	if state&netlink.NUD_PERMANENT != 0 {
		return true
	}
	if state&netlink.NUD_NOARP != 0 {
		return true
	}
	return false
}
