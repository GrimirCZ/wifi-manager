package hostname

import (
	"bufio"
	"context"
	"log"
	"os"
	"strings"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/normalize"
)

type DnsmasqProvider struct {
	ctx          context.Context
	leasesPath   string
	pollInterval time.Duration
	store        *store
}

func NewDnsmasqProvider(ctx context.Context, leasesPath string, pollInterval time.Duration) *DnsmasqProvider {
	return &DnsmasqProvider{
		ctx:          ctx,
		leasesPath:   leasesPath,
		pollInterval: pollInterval,
		store:        newStore(),
	}
}

func (d *DnsmasqProvider) Start() error {
	if d.leasesPath == "" {
		return nil
	}

	if entries, err := readDnsmasqLeases(d.leasesPath); err != nil {
		log.Printf("dnsmasq leases read error: %v", err)
	} else {
		d.store.update(entries)
	}

	ticker := time.NewTicker(d.pollInterval)
	go func() {
		defer ticker.Stop()
		for {
			select {
			case <-d.ctx.Done():
				return
			case <-ticker.C:
				entries, err := readDnsmasqLeases(d.leasesPath)
				if err != nil {
					log.Printf("dnsmasq leases read error: %v", err)
					continue
				}
				d.store.update(entries)
			}
		}
	}()

	return nil
}

func (d *DnsmasqProvider) LookupHostname(ip string) (string, bool) {
	return d.store.lookup(ip)
}

func readDnsmasqLeases(path string) (map[string]string, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer file.Close()

	entries := make(map[string]string)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 4 {
			continue
		}
		ip, ok := normalize.IP(fields[2])
		if !ok {
			continue
		}
		hostname := fields[3]
		if hostname == "*" {
			hostname = ""
		}
		entries[ip] = hostname
	}
	if err := scanner.Err(); err != nil {
		return nil, err
	}
	return entries, nil
}
