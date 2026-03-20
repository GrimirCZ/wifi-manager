package main

import (
	"context"
	"io"
	"log"
	"reflect"
	"testing"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/grpcclient"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
)

func TestObservedNetworkStateLogLinesIncludesClientsAndHostnames(t *testing.T) {
	lines := observedNetworkStateLogLines(
		&stubIPMappingProvider{
			clients: []ipmapping.ClientView{
				{
					MAC: "02:11:22:33:44:55",
					IPs: []string{"192.0.2.10", "192.0.2.11"},
				},
				{
					MAC: "00:11:22:33:44:55",
					IPs: []string{"192.0.2.20"},
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
	)

	want := []string{
		"observed network state dump (signal=SIGUSR1): current clients",
		"observed client mac=02:11:22:33:44:55 randomized=true ips=[192.0.2.10 192.0.2.11] hostnames=[laptop]",
		"observed client mac=00:11:22:33:44:55 randomized=false ips=[192.0.2.20] hostnames=[phone]",
	}
	if !reflect.DeepEqual(lines, want) {
		t.Fatalf("unexpected log lines: %#v", lines)
	}
}

func TestObservedNetworkStateLogLinesHandlesEmptySnapshot(t *testing.T) {
	lines := observedNetworkStateLogLines(&stubIPMappingProvider{}, &stubHostnameProvider{})

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
		GrpcTarget:          "localhost:9091",
		DummyMode:           true,
		DnsmasqPollInterval: time.Second,
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

type stubIPMappingProvider struct {
	clients []ipmapping.ClientView
}

func (s *stubIPMappingProvider) Start() error {
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

func testLogger() *log.Logger {
	return log.New(io.Discard, "", 0)
}
