package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"slices"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/agent"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/allowedip"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/dhcpfingerprint"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/grpcclient"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/macutil"
)

var grpcRun func(context.Context, config.Config, grpcclient.CommandHandler) error = grpcclient.Run
var newIPProvider = func(ctx context.Context, cfg config.Config) ipmapping.Provider {
	return ipmapping.New(ctx, cfg)
}
var newHostnameProvider = func(ctx context.Context, path string) hostname.Provider {
	return hostname.NewDnsmasqProvider(ctx, path)
}
var newDHCPFingerprintProvider = func(ctx context.Context, cfg config.Config) (dhcpfingerprint.Provider, error) {
	return dhcpfingerprint.New(cfg, ctx)
}
var nowObservedNetworkState = func() time.Time {
	return time.Now().UTC()
}

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("config error: %v", err)
	}

	firewallBackend, err := firewall.New(cfg)
	if err != nil {
		log.Fatalf("firewall backend error: %v", err)
	}

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	if err := run(ctx, cfg, firewallBackend, log.Default()); err != nil {
		log.Fatalf("runtime error: %v", err)
	}
}

func run(ctx context.Context, cfg config.Config, firewallBackend firewall.Backend, logger *log.Logger) error {
	logger.Printf("firewall backend: %s", firewallBackend)

	ipProvider := newIPProvider(ctx, cfg)
	hostnameProvider := newHostnameProvider(ctx, cfg.DnsmasqLeasesPath)
	dhcpFingerprintProvider, err := newDHCPFingerprintProvider(ctx, cfg)
	if err != nil {
		return errRuntime("dhcp fingerprint provider", err)
	}
	allowedIPs := allowedip.NewMemoryRepository()
	routerAgent := agent.New(firewallBackend, ipProvider, hostnameProvider, allowedIPs, dhcpFingerprintProvider, cfg.ActionTimeout)
	routerAgent.SetClientLifecycleLogScope(cfg.ClientLifecycleLogScope)
	startObservedStateDumpOnSignal(ctx, ipProvider, hostnameProvider, dhcpFingerprintProvider)

	// Start consuming provider updates before ipProvider.Start so replayed
	// bootstrap deltas are not missed while the initial snapshot goes live.
	go func() {
		updates := ipProvider.Updates()
		if updates == nil {
			return
		}
		for {
			select {
			case <-ctx.Done():
				return
			case update, ok := <-updates:
				if !ok {
					return
				}
				routerAgent.OnIPMappingUpdate(ctx, update)
			}
		}
	}()

	if err := ipProvider.Start(); err != nil {
		return errRuntime("ip mapping provider", err)
	}
	routerAgent.StartClientActivityLogger(ctx, cfg.ClientInactiveAfter)

	if err := hostnameProvider.Start(); err != nil {
		return errRuntime("hostname provider", err)
	}

	// Force one post-bootstrap snapshot read before DHCP log replay starts.
	_ = ipProvider.ListClients()

	if dhcpFingerprintProvider != nil {
		logger.Print(dhcpFingerprintProviderStartupMessage(cfg))
		if err := dhcpFingerprintProvider.Start(); err != nil {
			return errRuntime("dhcp fingerprint provider", err)
		}
	}

	if cfg.ObserveMode {
		logger.Print("observe mode enabled; gRPC client disabled")
		<-ctx.Done()
		return nil
	}

	routerAgent.StartReconciler(ctx, cfg.ReconcileInterval)

	if err := grpcRun(ctx, cfg, routerAgent); err != nil {
		return errRuntime("grpc loop", err)
	}

	return nil
}

func errRuntime(component string, err error) error {
	return &runtimeError{component: component, err: err}
}

func dhcpFingerprintProviderStartupMessage(cfg config.Config) string {
	switch cfg.DnsmasqDHCPSource {
	case "file":
		return "starting dhcp fingerprint provider source=file path=" + cfg.DnsmasqDHCPLogPath
	case "journald":
		return "starting dhcp fingerprint provider source=journald unit=" + cfg.DnsmasqDHCPJournalUnit
	default:
		return "starting dhcp fingerprint provider source=disabled"
	}
}

func startObservedStateDumpOnSignal(
	ctx context.Context,
	ipProvider ipmapping.Provider,
	hostnameProvider hostname.Provider,
	dhcpFingerprintProvider dhcpfingerprint.Provider,
) {
	dumpSignals := make(chan os.Signal, 1)
	signal.Notify(dumpSignals, syscall.SIGUSR1)

	go func() {
		defer signal.Stop(dumpSignals)
		for {
			select {
			case <-ctx.Done():
				return
			case <-dumpSignals:
				dumpObservedNetworkState(log.Default(), ipProvider, hostnameProvider, dhcpFingerprintProvider)
			}
		}
	}()
}

func dumpObservedNetworkState(
	logger *log.Logger,
	ipProvider ipmapping.Provider,
	hostnameProvider hostname.Provider,
	dhcpFingerprintProvider dhcpfingerprint.Provider,
) {
	for _, line := range observedNetworkStateLogLines(ipProvider, hostnameProvider, dhcpFingerprintProvider) {
		logger.Print(line)
	}
}

func observedNetworkStateLogLines(
	ipProvider ipmapping.Provider,
	hostnameProvider hostname.Provider,
	dhcpFingerprintProvider dhcpfingerprint.Provider,
) []string {
	return observedNetworkStateLogLinesAt(nowObservedNetworkState(), ipProvider, hostnameProvider, dhcpFingerprintProvider)
}

func observedNetworkStateLogLinesAt(
	now time.Time,
	ipProvider ipmapping.Provider,
	hostnameProvider hostname.Provider,
	dhcpFingerprintProvider dhcpfingerprint.Provider,
) []string {
	clients := ipProvider.ListClients()
	if len(clients) == 0 {
		return []string{"observed network state dump (signal=SIGUSR1): no clients observed"}
	}

	cutoff := now.Add(-15 * time.Minute)
	recent := make([]ipmapping.ClientView, 0, len(clients))
	older := make([]ipmapping.ClientView, 0, len(clients))
	for _, client := range clients {
		if !client.LastSeenAt.IsZero() && client.LastSeenAt.Before(cutoff) {
			older = append(older, client)
			continue
		}
		recent = append(recent, client)
	}

	lines := make([]string, 0, len(clients)+3)
	lines = append(lines, "observed network state dump (signal=SIGUSR1): current clients")
	lines = append(lines, "observed network state section=seen_within_15m clients="+formatCount(len(recent)))
	for _, client := range recent {
		lines = append(lines, observedNetworkClientLogLine(client, hostnameProvider, dhcpFingerprintProvider))
	}
	lines = append(lines, "observed network state section=older_than_15m clients="+formatCount(len(older)))
	for _, client := range older {
		lines = append(lines, observedNetworkClientLogLine(client, hostnameProvider, dhcpFingerprintProvider))
	}
	return lines
}

func observedNetworkClientLogLine(
	client ipmapping.ClientView,
	hostnameProvider hostname.Provider,
	dhcpFingerprintProvider dhcpfingerprint.Provider,
) string {
	hostnames := observedHostnamesForIPs(hostnameProvider, client.IPs)
	dhcpDetails := observedDHCPFingerprintDetails(dhcpFingerprintProvider, client.MAC)
	macDetails := "mac=" + client.MAC +
		" randomized=" + formatBool(macutil.IsRandomizedMAC(client.MAC)) +
		" status=" + string(client.Status) +
		" last_seen_at=" + formatObservedStateTimestamp(client.LastSeenAt)
	if len(hostnames) == 0 {
		line := "observed client " + macDetails + " ips=" + formatList(client.IPs)
		if dhcpDetails != "" {
			line += " " + dhcpDetails
		}
		return line
	}
	line := "observed client " + macDetails + " ips=" + formatList(client.IPs) + " hostnames=" + formatList(hostnames)
	if dhcpDetails != "" {
		line += " " + dhcpDetails
	}
	return line
}

func formatObservedStateTimestamp(t time.Time) string {
	if t.IsZero() {
		return ""
	}
	return t.UTC().Format(time.RFC3339)
}

func formatCount(n int) string {
	return strconv.Itoa(n)
}

func observedHostnamesForIPs(hostnameProvider hostname.Provider, ips []string) []string {
	if len(ips) == 0 {
		return nil
	}

	hostnames := make([]string, 0, len(ips))
	for _, ip := range ips {
		hostname, ok := hostnameProvider.LookupHostname(ip)
		if !ok || hostname == "" {
			continue
		}
		hostnames = append(hostnames, hostname)
	}
	if len(hostnames) == 0 {
		return nil
	}

	slices.Sort(hostnames)
	return slices.Compact(hostnames)
}

func observedDHCPFingerprintDetails(dhcpFingerprintProvider dhcpfingerprint.Provider, mac string) string {
	if dhcpFingerprintProvider == nil || mac == "" {
		return ""
	}

	observation, ok := dhcpFingerprintProvider.LookupByMAC(mac)
	if !ok {
		return ""
	}

	parts := make([]string, 0, 3)
	if observation.VendorClass != "" {
		parts = append(parts, "dhcp_vendor_class="+observation.VendorClass)
	}
	if observation.PRLHash != "" {
		parts = append(parts, "dhcp_prl_hash="+observation.PRLHash)
	}
	if observation.Hostname != "" {
		parts = append(parts, "dhcp_hostname="+observation.Hostname)
	}
	if len(parts) == 0 {
		return ""
	}

	return strings.Join(parts, " ")
}

func formatList(values []string) string {
	if len(values) == 0 {
		return "[]"
	}
	result := "["
	for i, value := range values {
		if i > 0 {
			result += " "
		}
		result += value
	}
	return result + "]"
}

func formatBool(value bool) string {
	if value {
		return "true"
	}
	return "false"
}

type runtimeError struct {
	component string
	err       error
}

func (e *runtimeError) Error() string {
	return e.component + " error: " + e.err.Error()
}

func (e *runtimeError) Unwrap() error {
	return e.err
}
