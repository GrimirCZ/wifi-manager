package agent

import (
	"reflect"
	"testing"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/allowedip"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentpb"
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
	if !reflect.DeepEqual(ack.Clients, want) {
		t.Fatalf("unexpected clients: %#v", ack.Clients)
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

var _ ipmapping.Provider = (*stubIPMappingProvider)(nil)
var _ hostname.Provider = (*stubHostnameProvider)(nil)
var _ ackSender = (*stubAckSender)(nil)

func TestAllowListHelpersStillUseProviderSnapshot(t *testing.T) {
	agent := New(
		firewall.NewDummyBackend(),
		&stubIPMappingProvider{
			lookupByIP: map[string]string{"192.0.2.10": "aa:aa:aa:aa:aa:aa"},
			ipsByMAC:   map[string][]string{"aa:aa:aa:aa:aa:aa": []string{"192.0.2.10"}},
		},
		&stubHostnameProvider{},
		allowedip.NewMemoryRepository(),
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
