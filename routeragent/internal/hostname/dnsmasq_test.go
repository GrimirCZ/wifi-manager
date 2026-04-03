package hostname

import (
	"context"
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestDnsmasqProviderLoadsInitialLeases(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.leases")
	writeFile(t, path, "123 aa:bb:cc:dd:ee:ff 192.0.2.10 laptop *\n")

	provider := NewDnsmasqProvider(context.Background(), path)
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	waitFor(t, func() bool {
		hostname, ok := provider.LookupHostname("192.0.2.10")
		return ok && hostname == "laptop"
	})
}

func TestDnsmasqProviderReplacesSnapshotOnRewrite(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.leases")
	writeFile(t, path, "123 aa:bb:cc:dd:ee:ff 192.0.2.10 laptop *\n")

	provider := NewDnsmasqProvider(context.Background(), path)
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}
	waitFor(t, func() bool {
		_, ok := provider.LookupHostname("192.0.2.10")
		return ok
	})

	writeFile(t, path, "456 aa:bb:cc:dd:ee:11 192.0.2.20 phone *\n")

	waitFor(t, func() bool {
		_, oldOk := provider.LookupHostname("192.0.2.10")
		newHostname, newOk := provider.LookupHostname("192.0.2.20")
		return !oldOk && newOk && newHostname == "phone"
	})
}

func TestDnsmasqProviderOmitsWildcardHostname(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.leases")
	writeFile(t, path, "123 aa:bb:cc:dd:ee:ff 192.0.2.10 * *\n")

	provider := NewDnsmasqProvider(context.Background(), path)
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	time.Sleep(100 * time.Millisecond)
	if _, ok := provider.LookupHostname("192.0.2.10"); ok {
		t.Fatal("expected wildcard hostname to be omitted")
	}
}

func writeFile(t *testing.T, path, content string) {
	t.Helper()
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatalf("write file: %v", err)
	}
}

func waitFor(t *testing.T, condition func() bool) {
	t.Helper()
	deadline := time.Now().Add(3 * time.Second)
	for time.Now().Before(deadline) {
		if condition() {
			return
		}
		time.Sleep(10 * time.Millisecond)
	}
	t.Fatal("condition not met before timeout")
}
