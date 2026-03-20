package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"slices"
	"syscall"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/agent"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/allowedip"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/grpcclient"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
)

var grpcRun func(context.Context, config.Config, grpcclient.CommandHandler) error = grpcclient.Run

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

	ipProvider := ipmapping.New(ctx, cfg)
	hostnameProvider := hostname.NewDnsmasqProvider(ctx, cfg.DnsmasqLeasesPath, cfg.DnsmasqPollInterval)
	allowedIPs := allowedip.NewMemoryRepository()
	routerAgent := agent.New(firewallBackend, ipProvider, hostnameProvider, allowedIPs, cfg.ActionTimeout)
	startObservedStateDumpOnSignal(ctx, ipProvider, hostnameProvider)

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

	if err := hostnameProvider.Start(); err != nil {
		return errRuntime("hostname provider", err)
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

func startObservedStateDumpOnSignal(ctx context.Context, ipProvider ipmapping.Provider, hostnameProvider hostname.Provider) {
	dumpSignals := make(chan os.Signal, 1)
	signal.Notify(dumpSignals, syscall.SIGUSR1)

	go func() {
		defer signal.Stop(dumpSignals)
		for {
			select {
			case <-ctx.Done():
				return
			case <-dumpSignals:
				dumpObservedNetworkState(log.Default(), ipProvider, hostnameProvider)
			}
		}
	}()
}

func dumpObservedNetworkState(logger *log.Logger, ipProvider ipmapping.Provider, hostnameProvider hostname.Provider) {
	for _, line := range observedNetworkStateLogLines(ipProvider, hostnameProvider) {
		logger.Print(line)
	}
}

func observedNetworkStateLogLines(ipProvider ipmapping.Provider, hostnameProvider hostname.Provider) []string {
	clients := ipProvider.ListClients()
	if len(clients) == 0 {
		return []string{"observed network state dump (signal=SIGUSR1): no clients observed"}
	}

	lines := make([]string, 0, len(clients)+1)
	lines = append(lines, "observed network state dump (signal=SIGUSR1): current clients")
	for _, client := range clients {
		hostnames := observedHostnamesForIPs(hostnameProvider, client.IPs)
		if len(hostnames) == 0 {
			lines = append(lines, "observed client mac="+client.MAC+" ips="+formatList(client.IPs))
			continue
		}
		lines = append(lines, "observed client mac="+client.MAC+" ips="+formatList(client.IPs)+" hostnames="+formatList(hostnames))
	}
	return lines
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
