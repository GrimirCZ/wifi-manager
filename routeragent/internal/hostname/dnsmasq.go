package hostname

import (
	"bufio"
	"context"
	"log"
	"os"
	"strings"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/dnsmasqfile"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/normalize"
)

type DnsmasqProvider struct {
	ctx        context.Context
	leasesPath string
	store      *store
}

func NewDnsmasqProvider(ctx context.Context, leasesPath string) *DnsmasqProvider {
	return &DnsmasqProvider{
		ctx:        ctx,
		leasesPath: leasesPath,
		store:      newStore(),
	}
}

func (d *DnsmasqProvider) Start() error {
	if d.leasesPath == "" {
		return nil
	}

	return dnsmasqfile.WatchFile(d.ctx, d.leasesPath, func() error {
		entries, err := readDnsmasqLeases(d.leasesPath)
		if err != nil {
			if os.IsNotExist(err) {
				d.store.replace(make(map[string]string))
				return nil
			}
			log.Printf("dnsmasq leases read error: %v", err)
			return nil
		}
		d.store.replace(entries)
		return nil
	})
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
