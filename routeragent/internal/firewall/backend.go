package firewall

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net"
	"os/exec"
	"slices"
	"strings"
	"sync"
)

// Backend exposes firewall mutation operations plus ListIPs, which returns the
// actual backend state used by reconciliation.
type Backend interface {
	Clear(ctx context.Context) error
	AllowIPs(ctx context.Context, ips []string) error
	RemoveIPs(ctx context.Context, ips []string) error
	ListIPs(ctx context.Context) ([]string, error)
	fmt.Stringer
}

type DummyBackend struct {
	mu  sync.Mutex
	set map[string]struct{}
}

func NewDummyBackend() *DummyBackend {
	return &DummyBackend{
		set: make(map[string]struct{}),
	}
}

func (d *DummyBackend) Clear(ctx context.Context) error {
	d.mu.Lock()
	defer d.mu.Unlock()

	clear(d.set)
	log.Printf("[dummy] flush firewall sets")
	return nil
}

func (d *DummyBackend) AllowIPs(ctx context.Context, ips []string) error {
	if len(ips) == 0 {
		return nil
	}

	d.mu.Lock()
	defer d.mu.Unlock()

	for _, ip := range ips {
		d.set[ip] = struct{}{}
	}
	log.Printf("[dummy] allow ips=%v", ips)
	return nil
}

func (d *DummyBackend) RemoveIPs(ctx context.Context, ips []string) error {
	if len(ips) == 0 {
		return nil
	}

	d.mu.Lock()
	defer d.mu.Unlock()

	for _, ip := range ips {
		delete(d.set, ip)
	}
	log.Printf("[dummy] revoke ips=%v", ips)
	return nil
}

func (d *DummyBackend) ListIPs(ctx context.Context) ([]string, error) {
	d.mu.Lock()
	defer d.mu.Unlock()

	ips := make([]string, 0, len(d.set))
	for ip := range d.set {
		ips = append(ips, ip)
	}
	slices.Sort(ips)
	return ips, nil
}

func (d *DummyBackend) String() string {
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

// ListIPs reads and unions the actual contents of the configured nft sets so
// reconciliation can diff expected state against backend reality.
func (n NftablesBackend) ListIPs(ctx context.Context) ([]string, error) {
	seen := make(map[string]struct{})
	if n.SetV4 != "" {
		ips, err := n.listSetIPs(ctx, n.SetV4)
		if err != nil {
			return nil, err
		}
		for _, ip := range ips {
			seen[ip] = struct{}{}
		}
	}
	if n.SetV6 != "" {
		ips, err := n.listSetIPs(ctx, n.SetV6)
		if err != nil {
			return nil, err
		}
		for _, ip := range ips {
			seen[ip] = struct{}{}
		}
	}

	ips := make([]string, 0, len(seen))
	for ip := range seen {
		ips = append(ips, ip)
	}
	slices.Sort(ips)
	return ips, nil
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

func (n NftablesBackend) listSetIPs(ctx context.Context, setName string) ([]string, error) {
	output, err := n.runOutput(ctx, "-j", "list", "set", n.Family, n.Table, setName)
	if err != nil {
		return nil, err
	}
	return parseNftListSetIPs(output, setName)
}

// parseNftListSetIPs accepts only the supported nft JSON layout:
// nftables[].set.elem as a flat array of IP address strings.
func parseNftListSetIPs(output []byte, setName string) ([]string, error) {
	var payload nftListSetResponse
	if err := json.Unmarshal(output, &payload); err != nil {
		return nil, fmt.Errorf("decode nft json for set %s: %w", setName, err)
	}

	seen := make(map[string]struct{})
	for _, entry := range payload.Nftables {
		if entry.Set == nil {
			continue
		}
		for _, value := range entry.Set.Elem {
			parsed := net.ParseIP(value)
			if parsed == nil {
				return nil, fmt.Errorf("invalid ip %q in nft set %s", value, setName)
			}
			seen[parsed.String()] = struct{}{}
		}
	}

	ips := make([]string, 0, len(seen))
	for ip := range seen {
		ips = append(ips, ip)
	}
	slices.Sort(ips)
	return ips, nil
}

func (n NftablesBackend) run(ctx context.Context, args ...string) error {
	output, err := n.runOutput(ctx, args...)
	if err != nil {
		return err
	}
	_ = output
	return nil
}

func (n NftablesBackend) runOutput(ctx context.Context, args ...string) ([]byte, error) {
	cmd := exec.CommandContext(ctx, "nft", args...)
	output, err := cmd.CombinedOutput()
	if err != nil {
		return nil, fmt.Errorf("nft %s failed: %w: %s", strings.Join(args, " "), err, strings.TrimSpace(string(output)))
	}
	return output, nil
}

type nftListSetResponse struct {
	Nftables []nftListSetEntry `json:"nftables"`
}

type nftListSetEntry struct {
	Set *nftSetPayload `json:"set,omitempty"`
}

type nftSetPayload struct {
	Elem []string `json:"elem"`
}
