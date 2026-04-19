package ipmapping

import (
	"context"
	"fmt"
	"log"
	"reflect"
	"testing"
	"time"
)

func TestStoreListClientsGroupsAndOrdersEntries(t *testing.T) {
	nowUTC = func() time.Time {
		return time.Date(2026, time.April, 19, 12, 0, 0, 0, time.UTC)
	}
	defer func() {
		nowUTC = func() time.Time { return time.Now().UTC() }
	}()

	store := newStore(context.Background(), nil)

	store.update("192.0.2.11", "aa:aa:aa:aa:aa:aa", "br-lan")
	store.update("192.0.2.20", "bb:bb:bb:bb:bb:bb", "br-lan")
	store.update("192.0.2.10", "aa:aa:aa:aa:aa:aa", "br-lan")

	clients := store.listClients()
	if len(clients) != 2 {
		t.Fatalf("expected 2 clients, got %d", len(clients))
	}

	want := []ClientView{
		{
			MAC:        "aa:aa:aa:aa:aa:aa",
			IPs:        []string{"192.0.2.10", "192.0.2.11"},
			Status:     NeighborStatusLive,
			LastSeenAt: nowUTC(),
		},
		{
			MAC:        "bb:bb:bb:bb:bb:bb",
			IPs:        []string{"192.0.2.20"},
			Status:     NeighborStatusLive,
			LastSeenAt: nowUTC(),
		},
	}
	if !reflect.DeepEqual(clients, want) {
		t.Fatalf("unexpected clients: %#v", clients)
	}
}

func TestStoreMoveAndDeleteKeepSnapshotsIsolated(t *testing.T) {
	store := newStore(context.Background(), nil)

	store.update("192.0.2.10", "aa:aa:aa:aa:aa:aa", "br-lan")
	store.update("192.0.2.20", "bb:bb:bb:bb:bb:bb", "br-lan")
	store.update("192.0.2.10", "bb:bb:bb:bb:bb:bb", "br-lan")

	mac, ok := store.lookupMAC("192.0.2.10")
	if !ok || mac != "bb:bb:bb:bb:bb:bb" {
		t.Fatalf("unexpected mac mapping: %q %v", mac, ok)
	}

	if ips := store.ipsForMAC("aa:aa:aa:aa:aa:aa"); len(ips) != 0 {
		t.Fatalf("expected old mac to have no ips, got %#v", ips)
	}

	clients := store.listClients()
	if len(clients) != 1 {
		t.Fatalf("expected 1 client, got %d", len(clients))
	}
	if !reflect.DeepEqual(clients[0].IPs, []string{"192.0.2.10", "192.0.2.20"}) {
		t.Fatalf("unexpected merged ips: %#v", clients[0].IPs)
	}

	clients[0].IPs[0] = "mutated"
	if got := store.ipsForMAC("bb:bb:bb:bb:bb:bb"); !reflect.DeepEqual(got, []string{"192.0.2.10", "192.0.2.20"}) {
		t.Fatalf("store leaked mutable slice: %#v", got)
	}

	removedMAC, removed := store.removeByIP("192.0.2.10")
	if !removed || removedMAC != "bb:bb:bb:bb:bb:bb" {
		t.Fatalf("unexpected remove result: %q %v", removedMAC, removed)
	}
	if got := store.ipsForMAC("bb:bb:bb:bb:bb:bb"); !reflect.DeepEqual(got, []string{"192.0.2.20"}) {
		t.Fatalf("unexpected ips after remove: %#v", got)
	}
}

func TestStoreLogsMacLifecycleOnlyOnFirstCreateAndLastDelete(t *testing.T) {
	nowUTC = func() time.Time {
		return time.Date(2026, time.April, 19, 12, 0, 0, 0, time.UTC)
	}
	defer func() {
		nowUTC = func() time.Time { return time.Now().UTC() }
	}()

	var lines []string
	logMACLifecyclef = func(format string, args ...any) {
		lines = append(lines, fmt.Sprintf(format, args...))
	}
	defer func() {
		logMACLifecyclef = log.Printf
	}()

	store := newStore(context.Background(), nil)

	store.update("192.0.2.10", "aa:aa:aa:aa:aa:aa", "br-lan")
	store.update("192.0.2.11", "aa:aa:aa:aa:aa:aa", "br-lan")
	store.removeByIP("192.0.2.10")
	store.removeByIP("192.0.2.11")

	want := []string{
		"observed client created mac=aa:aa:aa:aa:aa:aa first_ip=192.0.2.10 interface=br-lan status=live last_seen_at=2026-04-19T12:00:00Z",
		"observed client deleted mac=aa:aa:aa:aa:aa:aa last_ip=192.0.2.11 interface=br-lan status=live last_seen_at=2026-04-19T12:00:00Z",
	}
	if !reflect.DeepEqual(lines, want) {
		t.Fatalf("unexpected lifecycle logs: %#v", lines)
	}
}
