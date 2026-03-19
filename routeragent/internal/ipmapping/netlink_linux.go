//go:build linux

package ipmapping

import (
	"context"
	"sync"

	"github.com/vishvananda/netlink"
	"golang.org/x/sys/unix"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/normalize"
)

// NetlinkProvider bridges the kernel neighbor table into the observed-state
// store and closes the subscribe/scan startup gap by buffering early events.
type NetlinkProvider struct {
	ctx     context.Context
	updates chan Update
	store   *store

	bufferMu sync.Mutex
	live     bool
	pending  []rawEvent
}

func NewNetlinkProvider(ctx context.Context) *NetlinkProvider {
	updates := make(chan Update, UpdateChannelSize)
	return &NetlinkProvider{
		ctx:     ctx,
		updates: updates,
		store:   newStore(ctx, updates),
	}
}

// Start subscribes first, scans second, then replays buffered notifications so
// no netlink updates are lost before live enqueueing begins.
func (n *NetlinkProvider) Start() error {
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
				event, ok := normalizeRawUpdate(update)
				if !ok {
					continue
				}
				n.handleRawEvent(event)
			}
		}
	}()

	initial, err := initialNeighborSnapshot()
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
		state.applyUpsert(event.IP, event.MAC)
	})
}

// initialNeighborSnapshot captures the current neighbor-table seed that becomes
// the base snapshot before buffered gap events are replayed.
func initialNeighborSnapshot() ([]rawEvent, error) {
	families := []int{unix.AF_INET, unix.AF_INET6}
	events := make([]rawEvent, 0)

	for _, family := range families {
		neighs, err := netlink.NeighList(0, family)
		if err != nil {
			return nil, err
		}
		for _, neigh := range neighs {
			event, ok := normalizeNeighbor(neigh)
			if !ok || event.Deleted {
				continue
			}
			events = append(events, event)
		}
	}

	return events, nil
}

func normalizeRawUpdate(update netlink.NeighUpdate) (rawEvent, bool) {
	ip, ok := normalize.IPFromNet(update.Neigh.IP)
	if !ok {
		return rawEvent{}, false
	}
	if update.Type == unix.RTM_DELNEIGH {
		return rawEvent{IP: ip, Deleted: true}, true
	}

	mac, ok := normalize.MACFromNet(update.Neigh.HardwareAddr)
	if !ok {
		return rawEvent{}, false
	}
	return rawEvent{
		IP:  ip,
		MAC: mac,
	}, true
}

func normalizeNeighbor(neigh netlink.Neigh) (rawEvent, bool) {
	ip, ok := normalize.IPFromNet(neigh.IP)
	if !ok {
		return rawEvent{}, false
	}
	mac, ok := normalize.MACFromNet(neigh.HardwareAddr)
	if !ok {
		return rawEvent{}, false
	}
	return rawEvent{
		IP:  ip,
		MAC: mac,
	}, true
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
