package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	GrpcTarget          string
	ManagedInterfaces   []string
	ObserveMode         bool
	TLSEnabled          bool
	TLSCAFile           string
	TLSCertFile         string
	TLSKeyFile          string
	TLSServerName       string
	DummyMode           bool
	NftFamily           string
	NftTable            string
	NftSetV4            string
	NftSetV6            string
	DnsmasqLeasesPath   string
	DnsmasqPollInterval time.Duration
	ReconnectDelay      time.Duration
	ActionTimeout       time.Duration
	SyncInterval        time.Duration
	ReconcileInterval   time.Duration
}

func Load() (Config, error) {
	cfg := Config{
		GrpcTarget:          strings.TrimSpace(os.Getenv("ROUTERAGENT_GRPC_TARGET")),
		ManagedInterfaces:   envNames("ROUTERAGENT_MANAGED_INTERFACES"),
		ObserveMode:         envBool("ROUTERAGENT_OBSERVE_MODE", false),
		TLSEnabled:          envBool("ROUTERAGENT_TLS_ENABLED", false),
		TLSCAFile:           strings.TrimSpace(os.Getenv("ROUTERAGENT_TLS_CA_FILE")),
		TLSCertFile:         strings.TrimSpace(os.Getenv("ROUTERAGENT_TLS_CERT_FILE")),
		TLSKeyFile:          strings.TrimSpace(os.Getenv("ROUTERAGENT_TLS_KEY_FILE")),
		TLSServerName:       strings.TrimSpace(os.Getenv("ROUTERAGENT_TLS_SERVER_NAME")),
		DummyMode:           envBool("ROUTERAGENT_DUMMY_MODE", false),
		NftFamily:           envString("ROUTERAGENT_NFT_FAMILY", "inet"),
		NftTable:            strings.TrimSpace(os.Getenv("ROUTERAGENT_NFT_TABLE")),
		NftSetV4:            strings.TrimSpace(os.Getenv("ROUTERAGENT_NFT_SET_V4")),
		NftSetV6:            strings.TrimSpace(os.Getenv("ROUTERAGENT_NFT_SET_V6")),
		DnsmasqLeasesPath:   strings.TrimSpace(os.Getenv("ROUTERAGENT_DNSMASQ_LEASES_PATH")),
		DnsmasqPollInterval: envDuration("ROUTERAGENT_DNSMASQ_POLL_INTERVAL", 5*time.Second),
		ReconnectDelay:      envDuration("ROUTERAGENT_GRPC_RECONNECT_DELAY", 3*time.Second),
		ActionTimeout:       envDuration("ROUTERAGENT_ACTION_TIMEOUT", 5*time.Second),
		SyncInterval:        envDuration("ROUTERAGENT_SYNC_INTERVAL", 5*time.Minute),
		ReconcileInterval:   envDuration("ROUTERAGENT_RECONCILE_INTERVAL", time.Minute),
	}

	if !cfg.ObserveMode && cfg.GrpcTarget == "" {
		return cfg, fmt.Errorf("ROUTERAGENT_GRPC_TARGET is required")
	}

	if cfg.TLSEnabled {
		if cfg.TLSCAFile == "" || cfg.TLSCertFile == "" || cfg.TLSKeyFile == "" {
			return cfg, fmt.Errorf("ROUTERAGENT_TLS_CA_FILE, ROUTERAGENT_TLS_CERT_FILE, and ROUTERAGENT_TLS_KEY_FILE are required when TLS is enabled")
		}
	}

	return cfg, nil
}

func envString(key, fallback string) string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	return value
}

func envBool(key string, fallback bool) bool {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	if err != nil {
		return fallback
	}
	return parsed
}

func envDuration(key string, fallback time.Duration) time.Duration {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	parsed, err := time.ParseDuration(value)
	if err != nil {
		return fallback
	}
	return parsed
}

func envNames(key string) []string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return nil
	}

	fields := strings.Fields(value)
	if len(fields) == 0 {
		return nil
	}

	names := make([]string, 0, len(fields))
	seen := make(map[string]struct{}, len(fields))
	for _, field := range fields {
		if _, ok := seen[field]; ok {
			continue
		}
		seen[field] = struct{}{}
		names = append(names, field)
	}
	return names
}
