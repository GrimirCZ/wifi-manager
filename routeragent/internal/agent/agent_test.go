package agent

import (
	"context"
	"reflect"
	"testing"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/allowedip"
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
					MAC: "aa:aa:aa:aa:aa:aa",
					IPs: []string{"192.0.2.10", "192.0.2.11"},
				},
				{
					MAC: "bb:bb:bb:bb:bb:bb",
					IPs: []string{"192.0.2.20"},
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
			MacAddress:  "aa:aa:aa:aa:aa:aa",
			IpAddresses: []string{"192.0.2.10", "192.0.2.11"},
			Hostname:    stringPtr("laptop"),
			Allowed:     true,
		},
		{
			MacAddress:  "bb:bb:bb:bb:bb:bb",
			IpAddresses: []string{"192.0.2.20"},
			Hostname:    stringPtr("phone"),
			Allowed:     false,
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
	})

	provider.ipsByMAC["aa:aa:aa:aa:aa:aa"] = []string{"2001:db8::10", "192.0.2.10", "192.0.2.11"}
	agent.OnIPMappingUpdate(context.Background(), ipmapping.Update{
		IP:            "192.0.2.11",
		MAC:           "aa:aa:aa:aa:aa:aa",
		InterfaceName: "wlan0",
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
	})

	select {
	case observed := <-sender.observed:
		t.Fatalf("unexpected delayed observation for unauthorized mac: %#v", observed)
	case <-time.After(100 * time.Millisecond):
	}
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
			MAC: client.MAC,
			IPs: append([]string(nil), client.IPs...),
		})
	}
	return clients
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
