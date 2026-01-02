package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/agent"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/allowedip"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/grpcclient"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
)

func main() {
	cfg, err := config.Load()
	if err != nil {
		log.Fatalf("config error: %v", err)
	}

	firewallBackend, err := firewall.New(cfg)
	if err != nil {
		log.Fatalf("firewall backend error: %v", err)
	}
	log.Printf("firewall backend: %s", firewallBackend)

	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	ipProvider := ipmapping.New(ctx, cfg)
	if err := ipProvider.Start(); err != nil {
		log.Fatalf("ip mapping provider error: %v", err)
	}

	hostnameProvider := hostname.NewDnsmasqProvider(ctx, cfg.DnsmasqLeasesPath, cfg.DnsmasqPollInterval)
	if err := hostnameProvider.Start(); err != nil {
		log.Fatalf("hostname provider error: %v", err)
	}

	allowedIPs := allowedip.NewMemoryRepository()
	routerAgent := agent.New(firewallBackend, ipProvider, hostnameProvider, allowedIPs, cfg.ActionTimeout)

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

	if err := grpcclient.Run(ctx, cfg, routerAgent); err != nil {
		log.Fatalf("grpc loop error: %v", err)
	}
}
