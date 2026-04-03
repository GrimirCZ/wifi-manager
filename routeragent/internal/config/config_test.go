package config

import (
	"reflect"
	"strings"
	"testing"
)

func TestLoadAllowsObserveModeWithoutGrpcTarget(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "true")
	t.Setenv("ROUTERAGENT_GRPC_TARGET", "")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("expected load to succeed, got error: %v", err)
	}
	if !cfg.ObserveMode {
		t.Fatal("expected observe mode to be enabled")
	}
	if cfg.GrpcTarget != "" {
		t.Fatalf("expected empty grpc target, got %q", cfg.GrpcTarget)
	}
}

func TestLoadRequiresGrpcTargetOutsideObserveMode(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "false")
	t.Setenv("ROUTERAGENT_GRPC_TARGET", "")

	_, err := Load()
	if err == nil {
		t.Fatal("expected missing grpc target to fail")
	}
	if !strings.Contains(err.Error(), "ROUTERAGENT_GRPC_TARGET is required") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestLoadParsesManagedInterfaces(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "true")
	t.Setenv("ROUTERAGENT_MANAGED_INTERFACES", "br-lan wlan0 br-lan vlan20")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("expected load to succeed, got error: %v", err)
	}

	want := []string{"br-lan", "wlan0", "vlan20"}
	if !reflect.DeepEqual(cfg.ManagedInterfaces, want) {
		t.Fatalf("unexpected managed interfaces: %#v", cfg.ManagedInterfaces)
	}
}

func TestLoadAllowsDHCPFingerprintSourceToBeDisabled(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "true")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_SOURCE", "")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_LOG_PATH", "")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_JOURNAL_UNIT", "")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("expected load to succeed, got error: %v", err)
	}
	if cfg.DnsmasqDHCPSource != "" {
		t.Fatalf("expected disabled DHCP fingerprint source, got %q", cfg.DnsmasqDHCPSource)
	}
}

func TestLoadParsesDHCPFingerprintFileSource(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "true")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_SOURCE", "file")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_LOG_PATH", "/var/log/dnsmasq.log")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("expected load to succeed, got error: %v", err)
	}
	if cfg.DnsmasqDHCPSource != "file" {
		t.Fatalf("unexpected source: %q", cfg.DnsmasqDHCPSource)
	}
	if cfg.DnsmasqDHCPLogPath != "/var/log/dnsmasq.log" {
		t.Fatalf("unexpected log path: %q", cfg.DnsmasqDHCPLogPath)
	}
}

func TestLoadRequiresDHCPLogPathForFileSource(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "true")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_SOURCE", "file")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_LOG_PATH", "")

	_, err := Load()
	if err == nil {
		t.Fatal("expected missing DHCP log path to fail")
	}
	if !strings.Contains(err.Error(), "ROUTERAGENT_DNSMASQ_DHCP_LOG_PATH is required") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestLoadParsesDHCPFingerprintJournaldSource(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "true")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_SOURCE", "journald")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_JOURNAL_UNIT", "dnsmasq.service")

	cfg, err := Load()
	if err != nil {
		t.Fatalf("expected load to succeed, got error: %v", err)
	}
	if cfg.DnsmasqDHCPSource != "journald" {
		t.Fatalf("unexpected source: %q", cfg.DnsmasqDHCPSource)
	}
	if cfg.DnsmasqDHCPJournalUnit != "dnsmasq.service" {
		t.Fatalf("unexpected journal unit: %q", cfg.DnsmasqDHCPJournalUnit)
	}
}

func TestLoadRequiresDHCPJournalUnitForJournaldSource(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "true")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_SOURCE", "journald")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_JOURNAL_UNIT", "")

	_, err := Load()
	if err == nil {
		t.Fatal("expected missing journal unit to fail")
	}
	if !strings.Contains(err.Error(), "ROUTERAGENT_DNSMASQ_DHCP_JOURNAL_UNIT is required") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestLoadRejectsUnknownDHCPFingerprintSource(t *testing.T) {
	t.Setenv("ROUTERAGENT_OBSERVE_MODE", "true")
	t.Setenv("ROUTERAGENT_DNSMASQ_DHCP_SOURCE", "syslog")

	_, err := Load()
	if err == nil {
		t.Fatal("expected invalid DHCP source to fail")
	}
	if !strings.Contains(err.Error(), "ROUTERAGENT_DNSMASQ_DHCP_SOURCE must be one of") {
		t.Fatalf("unexpected error: %v", err)
	}
}
