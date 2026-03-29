//go:build linux

package ipmapping

import (
	"context"
	"reflect"
	"strings"
	"testing"

	"github.com/vishvananda/netlink"
	"golang.org/x/sys/unix"
)

func TestAllowsLinkIndexFromSet(t *testing.T) {
	allowed := map[int]string{
		3: "br-lan",
		7: "wlan0",
	}

	if !allowsLinkIndexFromSet(nil, 99) {
		t.Fatal("expected nil filter to allow all interfaces")
	}
	if !allowsLinkIndexFromSet(allowed, 3) {
		t.Fatal("expected configured interface to be allowed")
	}
	if allowsLinkIndexFromSet(allowed, 5) {
		t.Fatal("expected unconfigured interface to be rejected")
	}
}

func TestInitializationMessageIncludesManagedInterfaces(t *testing.T) {
	provider := &NetlinkProvider{
		managedInterfaces: []string{"br-lan", "wlan0"},
		managedIfIndexes: map[int]string{
			7: "wlan0",
			3: "br-lan",
		},
	}

	got := provider.initializationMessage()
	want := "netlink observed-state initialization complete; accepting live notifications on managed interfaces=br-lan#3,wlan0#7"
	if got != want {
		t.Fatalf("unexpected init message: %q", got)
	}
}

func TestResolveManagedInterfacesFailsForMissingName(t *testing.T) {
	_, err := resolveManagedInterfaces([]string{"this-interface-should-not-exist"})
	if err == nil {
		t.Fatal("expected missing interface resolution to fail")
	}
	if !strings.Contains(err.Error(), `resolve managed interface "this-interface-should-not-exist"`) {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestNormalizeNeighborAcceptsIPv4AndIPv6WithLinkIndexPreservedExternally(t *testing.T) {
	ipv4, ok := normalizeNeighbor(netlink.Neigh{
		LinkIndex:    3,
		State:        netlink.NUD_REACHABLE,
		IP:           []byte{192, 0, 2, 10},
		HardwareAddr: []byte{0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff},
	}, map[int]string{3: "br-lan"})
	if !ok {
		t.Fatal("expected ipv4 neighbor to normalize")
	}
	if !reflect.DeepEqual(ipv4, rawEvent{IP: "192.0.2.10", MAC: "aa:bb:cc:dd:ee:ff", InterfaceName: "br-lan"}) {
		t.Fatalf("unexpected ipv4 event: %#v", ipv4)
	}

	ipv6, ok := normalizeRawUpdate(netlink.NeighUpdate{
		Type: unix.RTM_NEWNEIGH,
		Neigh: netlink.Neigh{
			LinkIndex:    7,
			State:        netlink.NUD_DELAY,
			IP:           []byte{0xfe, 0x80, 0, 0, 0, 0, 0, 0, 0x02, 0x11, 0x22, 0xff, 0xfe, 0x33, 0x44, 0x55},
			HardwareAddr: []byte{0x02, 0x11, 0x22, 0x33, 0x44, 0x55},
		},
	}, map[int]string{7: "wlan0"})
	if !ok {
		t.Fatal("expected ipv6 update to normalize")
	}
	if !reflect.DeepEqual(ipv6, rawEvent{IP: "fe80::11:22ff:fe33:4455", MAC: "02:11:22:33:44:55", InterfaceName: "wlan0"}) {
		t.Fatalf("unexpected ipv6 event: %#v", ipv6)
	}
}

func TestNormalizeNeighborRejectsNonLiveBootstrapStates(t *testing.T) {
	for _, tc := range []struct {
		name  string
		state int
	}{
		{name: "stale", state: netlink.NUD_STALE},
		{name: "failed", state: netlink.NUD_FAILED},
		{name: "incomplete", state: netlink.NUD_INCOMPLETE},
	} {
		t.Run(tc.name, func(t *testing.T) {
			_, ok := normalizeNeighbor(netlink.Neigh{
				LinkIndex:    3,
				State:        tc.state,
				IP:           []byte{192, 0, 2, 10},
				HardwareAddr: []byte{0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff},
			}, map[int]string{3: "br-lan"})
			if ok {
				t.Fatalf("expected %s neighbor to be excluded from bootstrap", tc.name)
			}
		})
	}
}

func TestNormalizeRawUpdateDeletesNonLiveStates(t *testing.T) {
	for _, tc := range []struct {
		name  string
		state int
	}{
		{name: "stale", state: netlink.NUD_STALE},
		{name: "failed", state: netlink.NUD_FAILED},
		{name: "incomplete", state: netlink.NUD_INCOMPLETE},
	} {
		t.Run(tc.name, func(t *testing.T) {
			event, ok := normalizeRawUpdate(netlink.NeighUpdate{
				Type: unix.RTM_NEWNEIGH,
				Neigh: netlink.Neigh{
					LinkIndex:    3,
					State:        tc.state,
					IP:           []byte{192, 0, 2, 10},
					HardwareAddr: []byte{0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff},
				},
			}, map[int]string{3: "br-lan"})
			if !ok {
				t.Fatalf("expected %s update to normalize into delete", tc.name)
			}
			want := rawEvent{IP: "192.0.2.10", InterfaceName: "br-lan", Deleted: true}
			if !reflect.DeepEqual(event, want) {
				t.Fatalf("unexpected delete event: %#v", event)
			}
		})
	}
}

func TestNormalizeNeighborAcceptsStaticLiveStates(t *testing.T) {
	for _, tc := range []struct {
		name  string
		state int
	}{
		{name: "permanent", state: netlink.NUD_PERMANENT},
		{name: "noarp", state: netlink.NUD_NOARP},
		{name: "probe", state: netlink.NUD_PROBE},
	} {
		t.Run(tc.name, func(t *testing.T) {
			event, ok := normalizeNeighbor(netlink.Neigh{
				LinkIndex:    3,
				State:        tc.state,
				IP:           []byte{192, 0, 2, 10},
				HardwareAddr: []byte{0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff},
			}, map[int]string{3: "br-lan"})
			if !ok {
				t.Fatalf("expected %s neighbor to be kept", tc.name)
			}
			want := rawEvent{IP: "192.0.2.10", MAC: "aa:bb:cc:dd:ee:ff", InterfaceName: "br-lan"}
			if !reflect.DeepEqual(event, want) {
				t.Fatalf("unexpected event: %#v", event)
			}
		})
	}
}

func TestLiveStateTransitionRemovesAndReaddsEntry(t *testing.T) {
	provider := &NetlinkProvider{
		ctx:   context.Background(),
		store: newStore(context.Background(), nil),
		live:  true,
	}

	upsert, ok := normalizeRawUpdate(netlink.NeighUpdate{
		Type: unix.RTM_NEWNEIGH,
		Neigh: netlink.Neigh{
			LinkIndex:    3,
			State:        netlink.NUD_REACHABLE,
			IP:           []byte{192, 0, 2, 10},
			HardwareAddr: []byte{0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff},
		},
	}, map[int]string{3: "br-lan"})
	if !ok {
		t.Fatal("expected live neighbor to normalize")
	}
	provider.handleRawEvent(upsert)

	if mac, ok := provider.store.lookupMAC("192.0.2.10"); !ok || mac != "aa:bb:cc:dd:ee:ff" {
		t.Fatalf("expected live entry to exist, got %q %v", mac, ok)
	}

	remove, ok := normalizeRawUpdate(netlink.NeighUpdate{
		Type: unix.RTM_NEWNEIGH,
		Neigh: netlink.Neigh{
			LinkIndex:    3,
			State:        netlink.NUD_STALE,
			IP:           []byte{192, 0, 2, 10},
			HardwareAddr: []byte{0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff},
		},
	}, map[int]string{3: "br-lan"})
	if !ok {
		t.Fatal("expected stale update to normalize")
	}
	provider.handleRawEvent(remove)

	if _, ok := provider.store.lookupMAC("192.0.2.10"); ok {
		t.Fatal("expected stale transition to remove entry")
	}

	readd, ok := normalizeRawUpdate(netlink.NeighUpdate{
		Type: unix.RTM_NEWNEIGH,
		Neigh: netlink.Neigh{
			LinkIndex:    3,
			State:        netlink.NUD_PROBE,
			IP:           []byte{192, 0, 2, 10},
			HardwareAddr: []byte{0xaa, 0xbb, 0xcc, 0xdd, 0xee, 0xff},
		},
	}, map[int]string{3: "br-lan"})
	if !ok {
		t.Fatal("expected probe update to normalize")
	}
	provider.handleRawEvent(readd)

	if mac, ok := provider.store.lookupMAC("192.0.2.10"); !ok || mac != "aa:bb:cc:dd:ee:ff" {
		t.Fatalf("expected entry to reappear, got %q %v", mac, ok)
	}
}
