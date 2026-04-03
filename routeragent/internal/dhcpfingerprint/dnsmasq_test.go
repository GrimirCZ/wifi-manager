package dhcpfingerprint

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"os"
	"path/filepath"
	"testing"
	"time"
)

func TestDnsmasqProviderLoadsInitialLog(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")
	writeFile(t, path, `dnsmasq-dhcp[1]: 3420445372 vendor class: "android-dhcp-14"`+"\n"+
		`dnsmasq-dhcp[1]: 3420445372 DHCPREQUEST(ens19) 172.16.1.129 00:11:22:33:44:55`+"\n")

	provider := NewDnsmasqProvider(context.Background(), path)
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	waitFor(t, func() bool {
		observation, ok := provider.LookupByMAC("00:11:22:33:44:55")
		return ok && observation.VendorClass == "android-dhcp-14"
	})
}

func TestDnsmasqProviderUpdatesObservationOnAppend(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")
	writeFile(t, path, "")

	provider := NewDnsmasqProvider(context.Background(), path)
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	appendFile(t, path, `dnsmasq-dhcp[1]: 3420445372 vendor class: android-dhcp-15`+"\n")
	appendFile(t, path, `dnsmasq-dhcp[1]: 3420445372 DHCPREQUEST(ens19) 172.16.1.129 00:11:22:33:44:55`+"\n")
	appendFile(t, path, `dnsmasq-dhcp[1]: 3420445372 requested options: 1:netmask, 3:router, 6:dns-server, 15:domain-name,`+"\n")
	appendFile(t, path, `dnsmasq-dhcp[1]: 3420445372 requested options: 26:mtu, 28:broadcast, 51:lease-time, 58:T1,`+"\n")
	appendFile(t, path, `dnsmasq-dhcp[1]: 3420445372 requested options: 59:T2, 43:vendor-encap, 114, 108`+"\n")
	appendFile(t, path, `dnsmasq-dhcp[1]: 3420445372 hostname: "laptop"`+"\n")

	waitFor(t, func() bool {
		observation, ok := provider.LookupByMAC("00:11:22:33:44:55")
		return ok &&
			observation.VendorClass == "android-dhcp-15" &&
			observation.PRLHash == hashRequestedOptions("1", "3", "6", "15", "26", "28", "51", "58", "59", "43", "114", "108") &&
			observation.Hostname == "laptop"
	})
}

func TestDnsmasqProviderParsesPrefixlessFileFormat(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")
	writeFile(t, path, "")

	provider := NewDnsmasqProvider(context.Background(), path)
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	appendFile(t, path, `3420445372 vendor class: android-dhcp-15`+"\n")
	appendFile(t, path, `3420445372 DHCPREQUEST(ens19) 172.16.1.129 86:56:5f:ee:be:75`+"\n")
	appendFile(t, path, `3420445372 requested options: 1:netmask, 3:router, 6:dns-server, 15:domain-name`+"\n")

	waitFor(t, func() bool {
		observation, ok := provider.LookupByMAC("86:56:5f:ee:be:75")
		return ok &&
			observation.VendorClass == "android-dhcp-15" &&
			observation.PRLHash == hashRequestedOptions("1", "3", "6", "15")
	})
}

func TestDnsmasqProviderContinuesAfterRotation(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")
	writeFile(t, path, `dnsmasq-dhcp[1]: 3420445372 vendor class: "android-dhcp-14"`+"\n"+
		`dnsmasq-dhcp[1]: 3420445372 DHCPREQUEST(ens19) 172.16.1.129 00:11:22:33:44:55`+"\n")

	provider := NewDnsmasqProvider(context.Background(), path)
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}
	waitFor(t, func() bool {
		_, ok := provider.LookupByMAC("00:11:22:33:44:55")
		return ok
	})

	rotated := filepath.Join(dir, "dnsmasq.log.1")
	if err := os.Rename(path, rotated); err != nil {
		t.Fatalf("rename: %v", err)
	}
	writeFile(t, path, `dnsmasq-dhcp[1]: 3420445373 hostname: "phone"`+"\n"+
		`dnsmasq-dhcp[1]: 3420445373 DHCPACK(ens19) 172.16.1.130 aa:bb:cc:dd:ee:ff`+"\n")

	waitFor(t, func() bool {
		observation, ok := provider.LookupByMAC("aa:bb:cc:dd:ee:ff")
		return ok && observation.Hostname == "phone"
	})
}

func TestDnsmasqProviderLegacyMacFormatStillWorks(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")
	writeFile(t, path, "")

	provider := NewDnsmasqProvider(context.Background(), path)
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	appendFile(t, path, `dnsmasq-dhcp[1]: 00:11:22:33:44:55 requested options: 1,3,6`+"\n")

	waitFor(t, func() bool {
		observation, ok := provider.LookupByMAC("00:11:22:33:44:55")
		return ok && observation.PRLHash == hashRequestedOptions("1", "3", "6")
	})
}

func TestDnsmasqProviderIgnoresOrphanTransactionWithoutMac(t *testing.T) {
	provider := NewDnsmasqProvider(context.Background(), "")

	provider.consumeLine(`dnsmasq-dhcp[1]: 3420445372 vendor class: android-dhcp-15`)
	provider.consumeLine(`dnsmasq-dhcp[1]: 3420445372 requested options: 1:netmask, 3:router`)

	if _, ok := provider.LookupByMAC("00:11:22:33:44:55"); ok {
		t.Fatal("unexpected observation for unresolved transaction")
	}
}

func TestDnsmasqProviderCleansUpExpiredTransactions(t *testing.T) {
	provider := NewDnsmasqProvider(context.Background(), "")
	now := time.Date(2026, 3, 27, 23, 0, 0, 0, time.UTC)
	provider.now = func() time.Time { return now }

	provider.consumeLine(`dnsmasq-dhcp[1]: 3420445372 vendor class: android-dhcp-15`)
	if len(provider.transactions) != 1 {
		t.Fatalf("expected one tracked transaction, got %d", len(provider.transactions))
	}

	now = now.Add(transactionTTL + time.Second)
	provider.consumeLine(`dnsmasq-dhcp[1]: 3420445373 vendor class: android-dhcp-16`)

	if len(provider.transactions) != 1 {
		t.Fatalf("expected expired transaction cleanup, got %d active transactions", len(provider.transactions))
	}
	if _, ok := provider.transactions["3420445372"]; ok {
		t.Fatal("expected old transaction to be cleaned up")
	}
}

func hashRequestedOptions(values ...string) string {
	sum := sha256.Sum256([]byte(joinRequestedOptions(values)))
	return hex.EncodeToString(sum[:])
}

func joinRequestedOptions(values []string) string {
	if len(values) == 0 {
		return ""
	}
	result := values[0]
	for _, value := range values[1:] {
		result += "," + value
	}
	return result
}

func writeFile(t *testing.T, path, content string) {
	t.Helper()
	if err := os.WriteFile(path, []byte(content), 0o644); err != nil {
		t.Fatalf("write file: %v", err)
	}
}

func appendFile(t *testing.T, path, content string) {
	t.Helper()
	file, err := os.OpenFile(path, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0o644)
	if err != nil {
		t.Fatalf("open append file: %v", err)
	}
	defer file.Close()
	if _, err := file.WriteString(content); err != nil {
		t.Fatalf("append file: %v", err)
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
