package firewall

import (
	"context"
	"fmt"
	"log"
	"net"
	"os/exec"
	"strings"
)

type Backend interface {
	Clear(ctx context.Context) error
	AllowIPs(ctx context.Context, ips []string) error
	RemoveIPs(ctx context.Context, ips []string) error
	fmt.Stringer
}

type DummyBackend struct{}

func (d DummyBackend) Clear(ctx context.Context) error {
	log.Printf("[dummy] flush firewall sets")
	return nil
}

func (d DummyBackend) AllowIPs(ctx context.Context, ips []string) error {
	if len(ips) == 0 {
		return nil
	}
	log.Printf("[dummy] allow ips=%v", ips)
	return nil
}

func (d DummyBackend) RemoveIPs(ctx context.Context, ips []string) error {
	if len(ips) == 0 {
		return nil
	}
	log.Printf("[dummy] revoke ips=%v", ips)
	return nil
}

func (d DummyBackend) String() string {
	return "dummy"
}

type NftablesBackend struct {
	Family string
	Table  string
	SetV4  string
	SetV6  string
}

func (n NftablesBackend) Clear(ctx context.Context) error {
	if n.SetV4 != "" {
		if err := n.run(ctx, "flush", "set", n.Family, n.Table, n.SetV4); err != nil {
			return err
		}
	}
	if n.SetV6 != "" {
		if err := n.run(ctx, "flush", "set", n.Family, n.Table, n.SetV6); err != nil {
			return err
		}
	}
	return nil
}

func (n NftablesBackend) AllowIPs(ctx context.Context, ips []string) error {
	return n.applyElements(ctx, "add", ips)
}

func (n NftablesBackend) RemoveIPs(ctx context.Context, ips []string) error {
	return n.applyElements(ctx, "delete", ips)
}

func (n NftablesBackend) String() string {
	return fmt.Sprintf("nftables family=%s table=%s set_v4=%s set_v6=%s", n.Family, n.Table, n.SetV4, n.SetV6)
}

func (n NftablesBackend) setForIP(ip string) (string, error) {
	parsed := net.ParseIP(ip)
	if parsed == nil {
		return "", fmt.Errorf("invalid ip address %q", ip)
	}
	if parsed.To4() != nil {
		if n.SetV4 == "" {
			return "", fmt.Errorf("ipv4 set is not configured")
		}
		return n.SetV4, nil
	}
	if n.SetV6 == "" {
		return "", fmt.Errorf("ipv6 set is not configured")
	}
	return n.SetV6, nil
}

func (n NftablesBackend) applyElements(ctx context.Context, action string, ips []string) error {
	if len(ips) == 0 {
		return nil
	}

	bySet := make(map[string][]string)
	for _, ip := range ips {
		setName, err := n.setForIP(ip)
		if err != nil {
			return err
		}
		bySet[setName] = append(bySet[setName], ip)
	}

	for setName, setIPs := range bySet {
		if err := n.run(ctx, action, "element", n.Family, n.Table, setName, "{", strings.Join(setIPs, ", "), "}"); err != nil {
			return err
		}
	}
	return nil
}

func (n NftablesBackend) run(ctx context.Context, args ...string) error {
	cmd := exec.CommandContext(ctx, "nft", args...)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("nft %s failed: %w: %s", strings.Join(args, " "), err, strings.TrimSpace(string(output)))
	}
	return nil
}
