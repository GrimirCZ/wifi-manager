package ipmapping

import (
	"context"
	"reflect"
	"testing"
)

func TestStoreListClientsGroupsAndOrdersEntries(t *testing.T) {
	store := newStore(context.Background(), nil)

	store.update("192.0.2.11", "aa:aa:aa:aa:aa:aa")
	store.update("192.0.2.20", "bb:bb:bb:bb:bb:bb")
	store.update("192.0.2.10", "aa:aa:aa:aa:aa:aa")

	clients := store.listClients()
	if len(clients) != 2 {
		t.Fatalf("expected 2 clients, got %d", len(clients))
	}

	want := []ClientView{
		{
			MAC: "aa:aa:aa:aa:aa:aa",
			IPs: []string{"192.0.2.10", "192.0.2.11"},
		},
		{
			MAC: "bb:bb:bb:bb:bb:bb",
			IPs: []string{"192.0.2.20"},
		},
	}
	if !reflect.DeepEqual(clients, want) {
		t.Fatalf("unexpected clients: %#v", clients)
	}
}

func TestStoreMoveAndDeleteKeepSnapshotsIsolated(t *testing.T) {
	store := newStore(context.Background(), nil)

	store.update("192.0.2.10", "aa:aa:aa:aa:aa:aa")
	store.update("192.0.2.20", "bb:bb:bb:bb:bb:bb")
	store.update("192.0.2.10", "bb:bb:bb:bb:bb:bb")

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
