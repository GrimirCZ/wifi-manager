package main

import (
	"context"
	"io"
	"log"
	"reflect"
	"sync"
	"testing"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/dhcpfingerprint"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/grpcclient"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
)

func TestObservedNetworkStateLogLinesIncludesClientsAndHostnames(t *testing.T) {
	lines := observedNetworkStateLogLinesAt(
		time.Date(2026, time.April, 19, 12, 10, 0, 0, time.UTC),
		&stubIPMappingProvider{
			clients: []ipmapping.ClientView{
				{
					MAC:        "02:11:22:33:44:55",
					IPs:        []string{"192.0.2.10", "192.0.2.11"},
					Status:     ipmapping.NeighborStatusLive,
					LastSeenAt: time.Date(2026, time.April, 19, 12, 0, 0, 0, time.UTC),
				},
				{
					MAC:        "00:11:22:33:44:55",
					IPs:        []string{"192.0.2.20"},
					Status:     ipmapping.NeighborStatusStale,
					LastSeenAt: time.Date(2026, time.April, 19, 11, 30, 0, 0, time.UTC),
				},
			},
		},
		&stubHostnameProvider{
			hostnames: map[string]string{
				"192.0.2.10": "laptop",
				"192.0.2.11": "laptop",
				"192.0.2.20": "phone",
			},
		},
		&stubDHCPFingerprintProvider{
			observations: map[string]dhcpfingerprint.Observation{
				"02:11:22:33:44:55": {
					VendorClass: "android-dhcp-14",
					PRLHash:     "hash-a",
					Hostname:    "laptop.local",
				},
				"00:11:22:33:44:55": {
					VendorClass: "ios",
				},
			},
		},
	)

	want := []string{
		"observed network state dump (signal=SIGUSR1): current clients",
		"observed network state section=seen_within_15m clients=1",
		"observed client mac=02:11:22:33:44:55 randomized=true status=live last_seen_at=2026-04-19T12:00:00Z ips=[192.0.2.10 192.0.2.11] hostnames=[laptop] dhcp_vendor_class=android-dhcp-14 dhcp_prl_hash=hash-a dhcp_hostname=laptop.local",
		"observed network state section=older_than_15m clients=1",
		"observed client mac=00:11:22:33:44:55 randomized=false status=stale last_seen_at=2026-04-19T11:30:00Z ips=[192.0.2.20] hostnames=[phone] dhcp_vendor_class=ios",
	}
	if !reflect.DeepEqual(lines, want) {
		t.Fatalf("unexpected log lines: %#v", lines)
	}
}

func TestObservedNetworkStateLogLinesHandlesEmptySnapshot(t *testing.T) {
	lines := observedNetworkStateLogLines(&stubIPMappingProvider{}, &stubHostnameProvider{}, &stubDHCPFingerprintProvider{})

	want := []string{"observed network state dump (signal=SIGUSR1): no clients observed"}
	if !reflect.DeepEqual(lines, want) {
		t.Fatalf("unexpected log lines: %#v", lines)
	}
}

func TestRunObserveModeSkipsGrpcAndWaitsForCancellation(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	called := make(chan struct{}, 1)
	originalGrpcRun := grpcRun
	grpcRun = func(ctx context.Context, cfg config.Config, handler grpcclient.CommandHandler) error {
		called <- struct{}{}
		return nil
	}
	defer func() {
		grpcRun = originalGrpcRun
	}()

	done := make(chan error, 1)
	go func() {
		done <- run(ctx, config.Config{
			ObserveMode: true,
			DummyMode:   true,
		}, firewall.NewDummyBackend(), testLogger())
	}()

	select {
	case <-done:
		t.Fatal("run returned before context cancellation")
	case <-time.After(100 * time.Millisecond):
	}

	select {
	case <-called:
		t.Fatal("grpc runner should not be called in observe mode")
	default:
	}

	cancel()

	select {
	case err := <-done:
		if err != nil {
			t.Fatalf("unexpected error: %v", err)
		}
	case <-time.After(time.Second):
		t.Fatal("run did not exit after cancellation")
	}
}

func TestRunNormalModeInvokesGrpcRunner(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	called := make(chan struct{}, 1)
	originalGrpcRun := grpcRun
	grpcRun = func(ctx context.Context, cfg config.Config, handler grpcclient.CommandHandler) error {
		called <- struct{}{}
		return nil
	}
	defer func() {
		grpcRun = originalGrpcRun
	}()

	err := run(ctx, config.Config{
		GrpcTarget: "localhost:9091",
		DummyMode:  true,
	}, firewall.NewDummyBackend(), testLogger())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	select {
	case <-called:
	default:
		t.Fatal("expected grpc runner to be called")
	}
}

func TestRunStartsDHCPAfterInitialClientSnapshotRead(t *testing.T) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	originalGrpcRun := grpcRun
	originalNewIPProvider := newIPProvider
	originalNewHostnameProvider := newHostnameProvider
	originalNewDHCPFingerprintProvider := newDHCPFingerprintProvider
	defer func() {
		grpcRun = originalGrpcRun
		newIPProvider = originalNewIPProvider
		newHostnameProvider = originalNewHostnameProvider
		newDHCPFingerprintProvider = originalNewDHCPFingerprintProvider
	}()

	var (
		mu     sync.Mutex
		events []string
	)
	record := func(event string) {
		mu.Lock()
		defer mu.Unlock()
		events = append(events, event)
	}

	ipProvider := &stubIPMappingProvider{
		clients: []ipmapping.ClientView{
			{MAC: "00:11:22:33:44:55", IPs: []string{"192.0.2.10"}},
		},
		onStart:       func() { record("ip-start") },
		onListClients: func() { record("ip-list") },
	}
	dhcpProvider := &stubDHCPFingerprintProvider{
		onStart: func() { record("dhcp-start") },
	}

	newIPProvider = func(ctx context.Context, cfg config.Config) ipmapping.Provider {
		record("dhcp-factory-prep")
		return ipProvider
	}
	newHostnameProvider = func(ctx context.Context, path string) hostname.Provider {
		return &stubHostnameProvider{}
	}
	newDHCPFingerprintProvider = func(ctx context.Context, cfg config.Config) (dhcpfingerprint.Provider, error) {
		record("dhcp-factory")
		return dhcpProvider, nil
	}
	grpcRun = func(ctx context.Context, cfg config.Config, handler grpcclient.CommandHandler) error {
		record("grpc")
		return nil
	}

	err := run(ctx, config.Config{
		GrpcTarget:        "localhost:9091",
		DnsmasqDHCPSource: "journald",
		DummyMode:         true,
	}, firewall.NewDummyBackend(), testLogger())
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	want := []string{"dhcp-factory-prep", "dhcp-factory", "ip-start", "ip-list", "dhcp-start", "grpc"}
	if !reflect.DeepEqual(events, want) {
		t.Fatalf("unexpected startup order: %#v", events)
	}
}

type stubIPMappingProvider struct {
	clients       []ipmapping.ClientView
	onStart       func()
	onListClients func()
}

func (s *stubIPMappingProvider) Start() error {
	if s.onStart != nil {
		s.onStart()
	}
	return nil
}

func (s *stubIPMappingProvider) Updates() <-chan ipmapping.Update {
	return nil
}

func (s *stubIPMappingProvider) LookupMAC(ip string) (string, bool) {
	return "", false
}

func (s *stubIPMappingProvider) IPsForMAC(mac string) []string {
	return nil
}

func (s *stubIPMappingProvider) ListClients() []ipmapping.ClientView {
	if s.onListClients != nil {
		s.onListClients()
	}
	return append([]ipmapping.ClientView(nil), s.clients...)
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

var _ ipmapping.Provider = (*stubIPMappingProvider)(nil)
var _ hostname.Provider = (*stubHostnameProvider)(nil)

type stubDHCPFingerprintProvider struct {
	observations map[string]dhcpfingerprint.Observation
	onStart      func()
}

func (s *stubDHCPFingerprintProvider) Start() error {
	if s.onStart != nil {
		s.onStart()
	}
	return nil
}

func (s *stubDHCPFingerprintProvider) LookupByMAC(mac string) (dhcpfingerprint.Observation, bool) {
	if s.observations == nil {
		return dhcpfingerprint.Observation{}, false
	}
	observation, ok := s.observations[mac]
	return observation, ok
}

var _ dhcpfingerprint.Provider = (*stubDHCPFingerprintProvider)(nil)

func testLogger() *log.Logger {
	return log.New(io.Discard, "", 0)
}
