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
