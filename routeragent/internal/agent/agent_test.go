package agent

import (
	"context"
	"fmt"
	"reflect"
	"strings"
	"testing"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/allowedip"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/dhcpfingerprint"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentpb"
	"google.golang.org/protobuf/proto"
)

func TestBuildListNetworkClientsAckUsesCurrentSnapshot(t *testing.T) {
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{
			clients: []ipmapping.ClientView{
				{
					MAC:        "aa:aa:aa:aa:aa:aa",
					IPs:        []string{"192.0.2.10", "192.0.2.11"},
					Status:     ipmapping.NeighborStatusLive,
					LastSeenAt: time.Date(2026, time.April, 19, 12, 0, 0, 0, time.UTC),
				},
				{
					MAC:        "bb:bb:bb:bb:bb:bb",
					IPs:        []string{"192.0.2.20"},
					Status:     ipmapping.NeighborStatusStale,
					LastSeenAt: time.Date(2026, time.April, 19, 11, 45, 0, 0, time.UTC),
				},
			},
		},
		&stubHostnameProvider{
			hostnames: map[string]string{
				"192.0.2.11": "laptop",
				"192.0.2.20": "phone",
			},
		},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)

	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}

	ack := agent.buildListNetworkClientsAck("cmd-1")
	if !ack.Success {
		t.Fatalf("expected success ack, got %#v", ack)
	}
	if ack.Id != "cmd-1" {
		t.Fatalf("unexpected ack id: %q", ack.Id)
	}

	want := []*routeragentpb.NetworkClient{
		{
			MacAddress:     "aa:aa:aa:aa:aa:aa",
			IpAddresses:    []string{"192.0.2.10", "192.0.2.11"},
			Hostname:       stringPtr("laptop"),
			Allowed:        true,
			NeighborStatus: neighborStatusPtr(routeragentpb.NeighborStatus_NEIGHBOR_STATUS_LIVE),
			LastSeenAt:     stringPtr("2026-04-19T12:00:00Z"),
		},
		{
			MacAddress:     "bb:bb:bb:bb:bb:bb",
			IpAddresses:    []string{"192.0.2.20"},
			Hostname:       stringPtr("phone"),
			Allowed:        false,
			NeighborStatus: neighborStatusPtr(routeragentpb.NeighborStatus_NEIGHBOR_STATUS_STALE),
			LastSeenAt:     stringPtr("2026-04-19T11:45:00Z"),
		},
	}
	if len(ack.Clients) != len(want) {
		t.Fatalf("unexpected client count: got %d want %d", len(ack.Clients), len(want))
	}
	for index := range want {
		if !proto.Equal(ack.Clients[index], want[index]) {
			t.Fatalf("unexpected client at index %d: got %#v want %#v", index, ack.Clients[index], want[index])
		}
	}
}

func TestBuildAllowedClientsPresenceIncludesAllowedWithNonZeroLastSeen(t *testing.T) {
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{
			clients: []ipmapping.ClientView{
				{
					MAC:        "aa:aa:aa:aa:aa:aa",
					IPs:        []string{"192.0.2.10"},
					Status:     ipmapping.NeighborStatusLive,
					LastSeenAt: time.Date(2026, time.April, 19, 12, 0, 0, 0, time.UTC),
				},
				{
					MAC:        "bb:bb:bb:bb:bb:bb",
					IPs:        []string{"192.0.2.20"},
					Status:     ipmapping.NeighborStatusStale,
					LastSeenAt: time.Date(2026, time.April, 19, 11, 45, 0, 0, time.UTC),
				},
				{
					MAC:        "cc:cc:cc:cc:cc:cc",
					IPs:        []string{"192.0.2.30"},
					Status:     ipmapping.NeighborStatusLive,
					LastSeenAt: time.Time{},
				},
			},
		},
		&stubHostnameProvider{},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}
	agent.allowedMACs["cc:cc:cc:cc:cc:cc"] = struct{}{}

	got := agent.buildAllowedClientsPresence()
	if got == nil {
		t.Fatal("expected non-nil presence")
	}
	if len(got.Entries) != 1 {
		t.Fatalf("expected 1 entry, got %d", len(got.Entries))
	}
	entry := got.Entries[0]
	if entry.MacAddress != "aa:aa:aa:aa:aa:aa" {
		t.Fatalf("unexpected mac: %q", entry.MacAddress)
	}
	if entry.LastSeenAt != "2026-04-19T12:00:00Z" {
		t.Fatalf("unexpected last_seen_at: %q", entry.LastSeenAt)
	}
	if entry.NeighborStatus != routeragentpb.NeighborStatus_NEIGHBOR_STATUS_LIVE {
		t.Fatalf("unexpected neighbor status: %v", entry.NeighborStatus)
	}
}

func TestHandleListNetworkClientsSendsAck(t *testing.T) {
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{
			clients: []ipmapping.ClientView{
				{
					MAC: "aa:aa:aa:aa:aa:aa",
					IPs: []string{"192.0.2.10"},
				},
			},
		},
		&stubHostnameProvider{},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)

	sender := &stubAckSender{}
	err := agent.handleListNetworkClients(sender, &routeragentpb.ListNetworkClients{Id: "cmd-2"})
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if sender.ack == nil {
		t.Fatal("expected ack to be sent")
	}
	if sender.ack.Id != "cmd-2" || !sender.ack.Success {
		t.Fatalf("unexpected ack: %#v", sender.ack)
	}
}

func TestAuthorizedMACAppearanceEmitsDelayedObservationWithLatestIPv4AndDHCPData(t *testing.T) {
	provider := &stubIPMappingProvider{
		lookupByIP: map[string]string{
			"192.0.2.10":   "aa:aa:aa:aa:aa:aa",
			"192.0.2.11":   "aa:aa:aa:aa:aa:aa",
			"2001:db8::10": "aa:aa:aa:aa:aa:aa",
		},
		ipsByMAC: map[string][]string{
			"aa:aa:aa:aa:aa:aa": []string{"192.0.2.10"},
		},
	}
	hostnames := &stubHostnameProvider{
		hostnames: map[string]string{
			"192.0.2.10": "laptop",
			"192.0.2.11": "laptop",
		},
	}
	dhcp := &stubDHCPFingerprintProvider{
		observations: map[string]dhcpfingerprint.Observation{
			"aa:aa:aa:aa:aa:aa": {
				VendorClass: "android-dhcp-14",
				PRLHash:     "hash-a",
				Hostname:    "laptop.local",
			},
		},
	}
	agent := New(firewall.NewDummyBackend(), provider, hostnames, allowedip.NewMemoryRepository(), dhcp, time.Second)
	agent.observationDelay = 20 * time.Millisecond
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}

	sender := &stubObservationSender{observed: make(chan *routeragentpb.AuthorizedClientObserved, 1)}
	agent.OnStreamConnected(sender)

	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "wlan0",
		Status:        ipmapping.NeighborStatusLive,
	})

	provider.ipsByMAC["aa:aa:aa:aa:aa:aa"] = []string{"2001:db8::10", "192.0.2.10", "192.0.2.11"}
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.11",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "wlan0",
		Status:        ipmapping.NeighborStatusLive,
	})

	select {
	case observed := <-sender.observed:
		if observed.MacAddress != "aa:aa:aa:aa:aa:aa" {
			t.Fatalf("unexpected mac: %q", observed.MacAddress)
		}
		if !reflect.DeepEqual(observed.IpAddresses, []string{"192.0.2.10", "192.0.2.11"}) {
			t.Fatalf("unexpected ipv4 addresses: %#v", observed.IpAddresses)
		}
		if observed.DhcpVendorClass == nil || *observed.DhcpVendorClass != "android-dhcp-14" {
			t.Fatalf("unexpected vendor class: %#v", observed.DhcpVendorClass)
		}
		if observed.DhcpPrlHash == nil || *observed.DhcpPrlHash != "hash-a" {
			t.Fatalf("unexpected prl hash: %#v", observed.DhcpPrlHash)
		}
		if observed.DhcpHostname == nil || *observed.DhcpHostname != "laptop.local" {
			t.Fatalf("unexpected dhcp hostname: %#v", observed.DhcpHostname)
		}
	case <-time.After(200 * time.Millisecond):
		t.Fatal("expected delayed observation")
	}
}

func TestGetClientInfoCancelsPendingObservationAfterAppearanceUpdate(t *testing.T) {
	provider := &stubIPMappingProvider{
		lookupByIP: map[string]string{
			"192.0.2.10": "aa:aa:aa:aa:aa:aa",
		},
		ipsByMAC: map[string][]string{
			"aa:aa:aa:aa:aa:aa": []string{"192.0.2.10"},
		},
	}
	agent := New(firewall.NewDummyBackend(), provider, &stubHostnameProvider{}, allowedip.NewMemoryRepository(), &stubDHCPFingerprintProvider{}, time.Second)
	agent.observationDelay = 40 * time.Millisecond
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}

	sender := &stubObservationSender{observed: make(chan *routeragentpb.AuthorizedClientObserved, 1)}
	agent.OnStreamConnected(sender)
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "wlan0",
		Status:        ipmapping.NeighborStatusLive,
	})

	ackSender := &stubAckSender{}
	err := agent.handleGetClientInfo(ackSender, &routeragentpb.GetClientInfo{Id: "cmd-1", IpAddress: "192.0.2.10"})
	if err != nil {
		t.Fatalf("unexpected get client info error: %v", err)
	}

	select {
	case observed := <-sender.observed:
		t.Fatalf("unexpected delayed observation after get client info cancellation: %#v", observed)
	case <-time.After(100 * time.Millisecond):
	}
}

func TestDeletedObservationBeforeDelayDoesNotEmit(t *testing.T) {
	provider := &stubIPMappingProvider{
		ipsByMAC: map[string][]string{
			"aa:aa:aa:aa:aa:aa": []string{"192.0.2.10"},
		},
	}
	agent := New(firewall.NewDummyBackend(), provider, &stubHostnameProvider{}, allowedip.NewMemoryRepository(), &stubDHCPFingerprintProvider{}, time.Second)
	agent.observationDelay = 30 * time.Millisecond
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}

	sender := &stubObservationSender{observed: make(chan *routeragentpb.AuthorizedClientObserved, 1)}
	agent.OnStreamConnected(sender)
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "wlan0",
		Status:        ipmapping.NeighborStatusLive,
	})

	provider.ipsByMAC["aa:aa:aa:aa:aa:aa"] = nil
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:      "192.0.2.10",
		MAC:     "aa:aa:aa:aa:aa:aa",
		Deleted: true,
	})

	select {
	case observed := <-sender.observed:
		t.Fatalf("unexpected delayed observation after delete: %#v", observed)
	case <-time.After(100 * time.Millisecond):
	}
}

func TestUnauthorizedMACAppearanceDoesNotScheduleObservation(t *testing.T) {
	provider := &stubIPMappingProvider{
		ipsByMAC: map[string][]string{
			"aa:aa:aa:aa:aa:aa": []string{"192.0.2.10"},
		},
	}
	agent := New(firewall.NewDummyBackend(), provider, &stubHostnameProvider{}, allowedip.NewMemoryRepository(), &stubDHCPFingerprintProvider{}, time.Second)
	agent.observationDelay = 30 * time.Millisecond

	sender := &stubObservationSender{observed: make(chan *routeragentpb.AuthorizedClientObserved, 1)}
	agent.OnStreamConnected(sender)
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "wlan0",
		Status:        ipmapping.NeighborStatusLive,
	})

	select {
	case observed := <-sender.observed:
		t.Fatalf("unexpected delayed observation for unauthorized mac: %#v", observed)
	case <-time.After(100 * time.Millisecond):
	}
}

func TestIPMoveToDifferentMACRemovesOldAllowedIP(t *testing.T) {
	provider := &stubIPMappingProvider{
		ipsByMAC: map[string][]string{
			"aa:aa:aa:aa:aa:aa": []string{"192.0.2.10"},
			"bb:bb:bb:bb:bb:bb": nil,
		},
	}
	agent := New(firewall.NewDummyBackend(), provider, &stubHostnameProvider{}, allowedip.NewMemoryRepository(), &stubDHCPFingerprintProvider{}, time.Second)
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}

	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "wlan0",
		Status:        ipmapping.NeighborStatusLive,
	})
	if got := agent.allowedIPs.List(); !reflect.DeepEqual(got, []string{"192.0.2.10"}) {
		t.Fatalf("unexpected allowed ips after first appearance: %#v", got)
	}

	provider.ipsByMAC["aa:aa:aa:aa:aa:aa"] = nil
	provider.ipsByMAC["bb:bb:bb:bb:bb:bb"] = []string{"192.0.2.10"}
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		Deleted:       true,
		InterfaceName: "wlan0",
		Status:        ipmapping.NeighborStatusLive,
	})

	if got := agent.allowedIPs.List(); len(got) != 0 {
		t.Fatalf("expected moved ip to be removed from allowed set, got %#v", got)
	}
}

func TestAuthorizationLogsAllowedAndDisallowedStateChangesOnce(t *testing.T) {
	lines := captureClientLogs(t)
	provider := &stubIPMappingProvider{
		ipsByMAC: map[string][]string{
			"aa:aa:aa:aa:aa:aa": {"192.0.2.10", "192.0.2.11"},
			"bb:bb:bb:bb:bb:bb": {"192.0.2.20"},
		},
	}
	agent := New(firewall.NewDummyBackend(), provider, &stubHostnameProvider{}, allowedip.NewMemoryRepository(), &stubDHCPFingerprintProvider{}, time.Second)
	disallowedUpdate := ipmapping.Update{
		IP:            "192.0.2.20",
		MAC:           "bb:bb:bb:bb:bb:bb",
		InterfaceName: "br-lan",
		Status:        ipmapping.NeighborStatusLive,
	}

	if err := agent.allowMACs(context.Background(), []string{"aa:aa:aa:aa:aa:aa"}); err != nil {
		t.Fatalf("unexpected allow error: %v", err)
	}
	if err := agent.allowMACs(context.Background(), []string{"aa:aa:aa:aa:aa:aa"}); err != nil {
		t.Fatalf("unexpected repeated allow error: %v", err)
	}
	agent.OnIPMappingUpdate(context.Background(), disallowedUpdate)
	agent.OnIPMappingUpdate(context.Background(), disallowedUpdate)

	if got := countLogLinesWithPrefix(*lines, "client ips allowed mac=aa:aa:aa:aa:aa:aa ips=[192.0.2.10 192.0.2.11]"); got != 1 {
		t.Fatalf("expected one allowed log, got %d in %#v", got, *lines)
	}
	if got := countLogLinesWithPrefix(*lines, "client ips disallowed mac=bb:bb:bb:bb:bb:bb ips=[192.0.2.20]"); got != 1 {
		t.Fatalf("expected one disallowed log, got %d in %#v", got, *lines)
	}
}

func TestAuthorizationLogsRemovalWhenMappingDeletedOrPolicyRevoked(t *testing.T) {
	lines := captureClientLogs(t)
	provider := &stubIPMappingProvider{
		ipsByMAC: map[string][]string{
			"aa:aa:aa:aa:aa:aa": {"192.0.2.10"},
			"bb:bb:bb:bb:bb:bb": {"192.0.2.20", "192.0.2.21"},
		},
	}
	agent := New(firewall.NewDummyBackend(), provider, &stubHostnameProvider{}, allowedip.NewMemoryRepository(), &stubDHCPFingerprintProvider{}, time.Second)
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}
	agent.allowedMACs["bb:bb:bb:bb:bb:bb"] = struct{}{}

	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "br-lan",
		Status:        ipmapping.NeighborStatusLive,
	})
	if err := agent.allowMACs(context.Background(), []string{"bb:bb:bb:bb:bb:bb"}); err != nil {
		t.Fatalf("unexpected allow error: %v", err)
	}
	provider.ipsByMAC["aa:aa:aa:aa:aa:aa"] = nil
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:      "192.0.2.10",
		MAC:     "aa:aa:aa:aa:aa:aa",
		Deleted: true,
	})

	if err := agent.revokeMACs(context.Background(), []string{"bb:bb:bb:bb:bb:bb"}); err != nil {
		t.Fatalf("unexpected revoke error: %v", err)
	}

	if !containsLogLine(*lines, "client ips disallowed mac=aa:aa:aa:aa:aa:aa ips=[192.0.2.10] reason=deleted") {
		t.Fatalf("expected delete removal log in %#v", *lines)
	}
	if !containsLogLine(*lines, "client ips disallowed mac=bb:bb:bb:bb:bb:bb ips=[192.0.2.20 192.0.2.21] reason=policy_revoked") {
		t.Fatalf("expected revoke removal log in %#v", *lines)
	}
}

func TestLifecycleLogsAppearanceInactiveAndActiveAgain(t *testing.T) {
	lines := captureClientLogs(t)
	base := time.Date(2026, time.April, 19, 12, 0, 0, 0, time.UTC)
	nowUTC = func() time.Time { return base }
	defer func() {
		nowUTC = func() time.Time { return time.Now().UTC() }
	}()

	provider := &stubIPMappingProvider{
		ipsByMAC: map[string][]string{
			"aa:aa:aa:aa:aa:aa": {"192.0.2.10"},
		},
		clients: []ipmapping.ClientView{
			{
				MAC:        "aa:aa:aa:aa:aa:aa",
				IPs:        []string{"192.0.2.10"},
				Status:     ipmapping.NeighborStatusLive,
				LastSeenAt: base,
			},
		},
	}
	agent := New(firewall.NewDummyBackend(), provider, &stubHostnameProvider{}, allowedip.NewMemoryRepository(), &stubDHCPFingerprintProvider{}, time.Second)
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}

	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "br-lan",
		Status:        ipmapping.NeighborStatusLive,
		LastSeenAt:    base,
	})
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.11",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "br-lan",
		Status:        ipmapping.NeighborStatusLive,
		LastSeenAt:    base.Add(time.Minute),
	})
	agent.logInactiveClients(base.Add(16*time.Minute), 15*time.Minute)
	agent.logInactiveClients(base.Add(17*time.Minute), 15*time.Minute)
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "br-lan",
		Status:        ipmapping.NeighborStatusLive,
		LastSeenAt:    base.Add(18 * time.Minute),
	})

	if got := countLogLinesWithPrefix(*lines, "client appeared mac=aa:aa:aa:aa:aa:aa first_ip=192.0.2.10"); got != 1 {
		t.Fatalf("expected one appeared log, got %d in %#v", got, *lines)
	}
	if got := countLogLinesWithPrefix(*lines, "client inactive mac=aa:aa:aa:aa:aa:aa"); got != 1 {
		t.Fatalf("expected one inactive log, got %d in %#v", got, *lines)
	}
	if got := countLogLinesWithPrefix(*lines, "client active again mac=aa:aa:aa:aa:aa:aa ip=192.0.2.10"); got != 1 {
		t.Fatalf("expected one active-again log, got %d in %#v", got, *lines)
	}
}

func TestLifecycleLogScopeAllowedSuppressesNonAllowedMACs(t *testing.T) {
	lines := captureClientLogs(t)
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{ipsByMAC: map[string][]string{"aa:aa:aa:aa:aa:aa": {"192.0.2.10"}}},
		&stubHostnameProvider{},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)

	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "br-lan",
		Status:        ipmapping.NeighborStatusLive,
	})

	if containsLogLineWithPrefix(*lines, "client appeared mac=aa:aa:aa:aa:aa:aa") {
		t.Fatalf("unexpected lifecycle log for non-allowed mac: %#v", *lines)
	}
	if !containsLogLineWithPrefix(*lines, "client ips disallowed mac=aa:aa:aa:aa:aa:aa ips=[192.0.2.10]") {
		t.Fatalf("expected authorization log to remain visible: %#v", *lines)
	}
}

func TestLifecycleLogScopeAllLogsNonAllowedMACs(t *testing.T) {
	lines := captureClientLogs(t)
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{ipsByMAC: map[string][]string{"aa:aa:aa:aa:aa:aa": {"192.0.2.10"}}},
		&stubHostnameProvider{},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)
	agent.SetClientLifecycleLogScope(config.ClientLifecycleLogScopeAll)

	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.10",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "br-lan",
		Status:        ipmapping.NeighborStatusLive,
	})

	if !containsLogLineWithPrefix(*lines, "client appeared mac=aa:aa:aa:aa:aa:aa first_ip=192.0.2.10") {
		t.Fatalf("expected lifecycle log for non-allowed mac: %#v", *lines)
	}
}

func captureClientLogs(t *testing.T) *[]string {
	t.Helper()
	var lines []string
	previous := logClientEventf
	logClientEventf = func(format string, args ...any) {
		lines = append(lines, fmt.Sprintf(format, args...))
	}
	t.Cleanup(func() {
		logClientEventf = previous
	})
	return &lines
}

func containsLogLine(lines []string, want string) bool {
	for _, line := range lines {
		if line == want {
			return true
		}
	}
	return false
}

func containsLogLineWithPrefix(lines []string, prefix string) bool {
	return countLogLinesWithPrefix(lines, prefix) > 0
}

func countLogLinesWithPrefix(lines []string, prefix string) int {
	count := 0
	for _, line := range lines {
		if strings.HasPrefix(line, prefix) {
			count++
		}
	}
	return count
}

type stubIPMappingProvider struct {
	lookupByIP map[string]string
	ipsByMAC   map[string][]string
	clients    []ipmapping.ClientView
}

func (s *stubIPMappingProvider) Start() error {
	return nil
}

func (s *stubIPMappingProvider) Updates() <-chan ipmapping.Update {
	return nil
}

func (s *stubIPMappingProvider) LookupMAC(ip string) (string, bool) {
	if s.lookupByIP == nil {
		return "", false
	}
	mac, ok := s.lookupByIP[ip]
	return mac, ok
}

func (s *stubIPMappingProvider) IPsForMAC(mac string) []string {
	if s.ipsByMAC == nil {
		return nil
	}
	return append([]string(nil), s.ipsByMAC[mac]...)
}

func (s *stubIPMappingProvider) ListClients() []ipmapping.ClientView {
	clients := make([]ipmapping.ClientView, 0, len(s.clients))
	for _, client := range s.clients {
		clients = append(clients, ipmapping.ClientView{
			MAC:        client.MAC,
			IPs:        append([]string(nil), client.IPs...),
			Status:     client.Status,
			LastSeenAt: client.LastSeenAt,
		})
	}
	return clients
}

func neighborStatusPtr(value routeragentpb.NeighborStatus) *routeragentpb.NeighborStatus {
	return &value
}

type stubHostnameProvider struct {
	hostnames map[string]string
}

func (s *stubHostnameProvider) Start() error {
	return nil
}

func (s *stubHostnameProvider) LookupHostname(ip string) (string, bool) {
	if s.hostnames == nil {
		return "", false
	}
	hostname, ok := s.hostnames[ip]
	return hostname, ok
}

type stubAckSender struct {
	ack *routeragentpb.CommandAck
}

func (s *stubAckSender) SendAck(ack *routeragentpb.CommandAck) error {
	s.ack = ack
	return nil
}

type stubObservationSender struct {
	observed chan *routeragentpb.AuthorizedClientObserved
}

func (s *stubObservationSender) SendAuthorizedClientObserved(observed *routeragentpb.AuthorizedClientObserved) error {
	s.observed <- observed
	return nil
}

type stubDHCPFingerprintProvider struct {
	observations map[string]dhcpfingerprint.Observation
}

func (s *stubDHCPFingerprintProvider) Start() error {
	return nil
}

func (s *stubDHCPFingerprintProvider) LookupByMAC(mac string) (dhcpfingerprint.Observation, bool) {
	if s.observations == nil {
		return dhcpfingerprint.Observation{}, false
	}
	observation, ok := s.observations[mac]
	return observation, ok
}

var _ ipmapping.Provider = (*stubIPMappingProvider)(nil)
var _ hostname.Provider = (*stubHostnameProvider)(nil)
var _ ackSender = (*stubAckSender)(nil)
var _ observationSender = (*stubObservationSender)(nil)
var _ dhcpfingerprint.Provider = (*stubDHCPFingerprintProvider)(nil)

func TestAllowListHelpersStillUseProviderSnapshot(t *testing.T) {
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{
			lookupByIP: map[string]string{"192.0.2.10": "aa:aa:aa:aa:aa:aa"},
			ipsByMAC:   map[string][]string{"aa:aa:aa:aa:aa:aa": []string{"192.0.2.10"}},
		},
		&stubHostnameProvider{},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)

	mac, ok := agent.ipMapping.LookupMAC("192.0.2.10")
	if !ok || mac != "aa:aa:aa:aa:aa:aa" {
		t.Fatalf("unexpected lookup result: %q %v", mac, ok)
	}
	if got := agent.collectIPsForMACs([]string{"aa:aa:aa:aa:aa:aa"}); !reflect.DeepEqual(got, []string{"192.0.2.10"}) {
		t.Fatalf("unexpected collected ips: %#v", got)
	}
}

func TestReconcileFirewallRepairsDrift(t *testing.T) {
	firewallBackend := firewall.NewDummyBackend()
	_ = firewallBackend.AllowIPs(nil, []string{"192.0.2.20"})

	agent := New(
		firewallBackend,
		&stubIPMappingProvider{
			clients: []ipmapping.ClientView{
				{
					MAC: "aa:aa:aa:aa:aa:aa",
					IPs: []string{"192.0.2.10"},
				},
			},
		},
		&stubHostnameProvider{},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}

	if err := agent.reconcileFirewall(nil); err != nil {
		t.Fatalf("unexpected reconcile error: %v", err)
	}

	actual, err := firewallBackend.ListIPs(nil)
	if err != nil {
		t.Fatalf("unexpected firewall list error: %v", err)
	}
	if !reflect.DeepEqual(actual, []string{"192.0.2.10"}) {
		t.Fatalf("unexpected firewall ips after reconcile: %#v", actual)
	}
	if !reflect.DeepEqual(agent.allowedIPs.List(), []string{"192.0.2.10"}) {
		t.Fatalf("unexpected allowed ips after reconcile: %#v", agent.allowedIPs.List())
	}
}

func TestDelayedObservationEmitsOnlyIPv4Addresses(t *testing.T) {
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{
			ipsByMAC: map[string][]string{
				"aa:aa:aa:aa:aa:aa": {"fe80::1", "192.0.2.10"},
			},
		},
		&stubHostnameProvider{
			hostnames: map[string]string{
				"192.0.2.10": "phone",
			},
		},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{
			observations: map[string]dhcpfingerprint.Observation{
				"aa:aa:aa:aa:aa:aa": {
					VendorClass: "android",
					PRLHash:     "hash",
					Hostname:    "phone-dhcp",
				},
			},
		},
		time.Second,
	)
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}
	agent.observationDelay = 10 * time.Millisecond
	sender := &stubObservationSender{observed: make(chan *routeragentpb.AuthorizedClientObserved, 1)}
	agent.OnStreamConnected(sender)

	agent.schedulePendingObservation("aa:aa:aa:aa:aa:aa", "br-lan")

	select {
	case observed := <-sender.observed:
		if !reflect.DeepEqual(observed.IpAddresses, []string{"192.0.2.10"}) {
			t.Fatalf("unexpected ipv4 addresses: %#v", observed.IpAddresses)
		}
		if observed.InterfaceName == nil || *observed.InterfaceName != "br-lan" {
			t.Fatalf("unexpected interface: %#v", observed.InterfaceName)
		}
	case <-time.After(200 * time.Millisecond):
		t.Fatal("expected delayed observation to be emitted")
	}
}

func TestGetClientInfoCancelsPendingObservation(t *testing.T) {
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{
			lookupByIP: map[string]string{
				"192.0.2.10": "aa:aa:aa:aa:aa:aa",
			},
			ipsByMAC: map[string][]string{
				"aa:aa:aa:aa:aa:aa": {"192.0.2.10"},
			},
		},
		&stubHostnameProvider{},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}
	agent.observationDelay = 50 * time.Millisecond
	sender := &stubObservationSender{observed: make(chan *routeragentpb.AuthorizedClientObserved, 1)}
	agent.OnStreamConnected(sender)

	agent.schedulePendingObservation("aa:aa:aa:aa:aa:aa", "br-lan")

	ackSender := &stubAckSender{}
	if err := agent.handleGetClientInfo(ackSender, &routeragentpb.GetClientInfo{Id: "cmd", IpAddress: "192.0.2.10"}); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	select {
	case observed := <-sender.observed:
		t.Fatalf("unexpected delayed observation after cancellation: %#v", observed)
	case <-time.After(120 * time.Millisecond):
	}
}

func TestDelayedObservationSurvivesDisconnectedStreamAndFlushesOnReconnect(t *testing.T) {
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{
			ipsByMAC: map[string][]string{
				"aa:aa:aa:aa:aa:aa": {"192.0.2.10"},
			},
		},
		&stubHostnameProvider{
			hostnames: map[string]string{
				"192.0.2.10": "phone",
			},
		},
		allowedip.NewMemoryRepository(),
		&stubDHCPFingerprintProvider{},
		time.Second,
	)
	agent.allowedMACs["aa:aa:aa:aa:aa:aa"] = struct{}{}
	agent.observationDelay = 10 * time.Millisecond

	agent.schedulePendingObservation("aa:aa:aa:aa:aa:aa", "br-lan")

	time.Sleep(40 * time.Millisecond)

	sender := &stubObservationSender{observed: make(chan *routeragentpb.AuthorizedClientObserved, 1)}
	agent.OnStreamConnected(sender)

	select {
	case observed := <-sender.observed:
		if observed.MacAddress != "aa:aa:aa:aa:aa:aa" {
			t.Fatalf("unexpected mac after reconnect flush: %q", observed.MacAddress)
		}
	case <-time.After(200 * time.Millisecond):
		t.Fatal("expected delayed observation to flush after reconnect")
	}
}
