package agent

import (
	"context"
	"log"
	"sync"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/allowedip"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/normalize"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentgrpc"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentpb"
)

type Agent struct {
	firewall      firewall.Backend
	ipMapping     ipmapping.Provider
	hostname      hostname.Provider
	allowedIPs    allowedip.Repository
	allowedMACs   map[string]struct{}
	mu            sync.RWMutex
	actionTimeout time.Duration
}

func New(
	firewall firewall.Backend,
	ipMapping ipmapping.Provider,
	hostname hostname.Provider,
	allowedIPs allowedip.Repository,
	actionTimeout time.Duration,
) *Agent {
	return &Agent{
		firewall:      firewall,
		ipMapping:     ipMapping,
		hostname:      hostname,
		allowedIPs:    allowedIPs,
		allowedMACs:   make(map[string]struct{}),
		actionTimeout: actionTimeout,
	}
}

func (a *Agent) OnIPMappingUpdate(ctx context.Context, update ipmapping.Update) {
	if update.IP == "" || update.MAC == "" {
		if update.Deleted && update.IP != "" {
			a.removeAllowedIP(ctx, update.IP)
		}
		return
	}
	if update.Deleted {
		a.removeAllowedIP(ctx, update.IP)
		return
	}
	if !a.isMACAllowed(update.MAC) {
		return
	}
	added := a.allowedIPs.Add([]string{update.IP})
	if len(added) == 0 {
		return
	}
	if err := a.withTimeout(ctx, func(ctx context.Context) error {
		return a.firewall.AllowIPs(ctx, added)
	}); err != nil {
		log.Printf("failed to allow ip=%s for mac=%s: %v", update.IP, update.MAC, err)
	}
}

func (a *Agent) removeAllowedIP(ctx context.Context, ip string) {
	removed := a.allowedIPs.Remove([]string{ip})
	if len(removed) == 0 {
		return
	}
	if err := a.withTimeout(ctx, func(ctx context.Context) error {
		return a.firewall.RemoveIPs(ctx, removed)
	}); err != nil {
		log.Printf("failed to revoke ip=%s: %v", ip, err)
	}
}

func (a *Agent) HandleCommand(ctx context.Context, stream *routeragentgrpc.Stream, cmd *routeragentpb.RouterAgentCommand) error {
	if cmd == nil {
		return nil
	}

	switch c := cmd.Command.(type) {
	case *routeragentpb.RouterAgentCommand_GetClientInfo:
		return a.handleGetClientInfo(stream, c.GetClientInfo)
	case *routeragentpb.RouterAgentCommand_AllowClientAccess:
		return a.handleAllowClientAccess(ctx, stream, c.AllowClientAccess)
	case *routeragentpb.RouterAgentCommand_RevokeClientAccess:
		return a.handleRevokeClientAccess(ctx, stream, c.RevokeClientAccess)
	case *routeragentpb.RouterAgentCommand_SetAllowedClients:
		return a.handleSetAllowedClients(ctx, stream, c.SetAllowedClients)
	default:
		return nil
	}
}

func (a *Agent) handleGetClientInfo(stream *routeragentgrpc.Stream, cmd *routeragentpb.GetClientInfo) error {
	if cmd == nil {
		return nil
	}

	ip, ok := normalize.IP(cmd.IpAddress)
	if !ok {
		return stream.SendAck(buildAck(cmd.Id, errInvalidIP(cmd.IpAddress)))
	}

	mac, ok := a.ipMapping.LookupMAC(ip)
	hostname, _ := a.hostname.LookupHostname(ip)

	ack := &routeragentpb.CommandAck{
		Id:      cmd.Id,
		Success: true,
	}
	if ok {
		ack.MacAddress = stringPtr(mac)
	}
	if hostname != "" {
		ack.Hostname = stringPtr(hostname)
	}
	return stream.SendAck(ack)
}

func (a *Agent) handleAllowClientAccess(ctx context.Context, stream *routeragentgrpc.Stream, cmd *routeragentpb.AllowClientAccess) error {
	if cmd == nil {
		return nil
	}
	macs, err := normalizeMACs(cmd.MacAddresses)
	if err != nil {
		return stream.SendAck(buildAck(cmd.Id, err))
	}
	err = a.withTimeout(ctx, func(ctx context.Context) error {
		return a.allowMACs(ctx, macs)
	})
	return stream.SendAck(buildAck(cmd.Id, err))
}

func (a *Agent) handleRevokeClientAccess(ctx context.Context, stream *routeragentgrpc.Stream, cmd *routeragentpb.RevokeClientAccess) error {
	if cmd == nil {
		return nil
	}
	macs, err := normalizeMACs(cmd.MacAddresses)
	if err != nil {
		return stream.SendAck(buildAck(cmd.Id, err))
	}
	err = a.withTimeout(ctx, func(ctx context.Context) error {
		return a.revokeMACs(ctx, macs)
	})
	return stream.SendAck(buildAck(cmd.Id, err))
}

func (a *Agent) handleSetAllowedClients(ctx context.Context, stream *routeragentgrpc.Stream, cmd *routeragentpb.SetAllowedClients) error {
	if cmd == nil {
		return nil
	}
	macs, err := normalizeMACs(cmd.MacAddresses)
	if err != nil {
		return stream.SendAck(buildAck(cmd.Id, err))
	}
	err = a.withTimeout(ctx, func(ctx context.Context) error {
		return a.setAllowedMACs(ctx, macs)
	})
	return stream.SendAck(buildAck(cmd.Id, err))
}

func (a *Agent) setAllowedMACs(ctx context.Context, macs []string) error {
	a.mu.Lock()
	a.allowedMACs = make(map[string]struct{}, len(macs))
	for _, mac := range macs {
		a.allowedMACs[mac] = struct{}{}
	}
	a.mu.Unlock()

	allIPs := a.collectIPsForMACs(macs)
	a.allowedIPs.SetAll(allIPs)

	if err := a.firewall.Clear(ctx); err != nil {
		return err
	}
	if err := a.firewall.AllowIPs(ctx, a.allowedIPs.List()); err != nil {
		return err
	}
	return nil
}

func (a *Agent) allowMACs(ctx context.Context, macs []string) error {
	a.mu.Lock()
	for _, mac := range macs {
		a.allowedMACs[mac] = struct{}{}
	}
	a.mu.Unlock()

	allIPs := a.collectIPsForMACs(macs)
	added := a.allowedIPs.Add(allIPs)
	if err := a.firewall.AllowIPs(ctx, added); err != nil {
		return err
	}
	return nil
}

func (a *Agent) revokeMACs(ctx context.Context, macs []string) error {
	a.mu.Lock()
	for _, mac := range macs {
		delete(a.allowedMACs, mac)
	}
	a.mu.Unlock()

	allIPs := a.collectIPsForMACs(macs)
	removed := a.allowedIPs.Remove(allIPs)
	if err := a.firewall.RemoveIPs(ctx, removed); err != nil {
		return err
	}
	return nil
}

func (a *Agent) collectIPsForMACs(macs []string) []string {
	var ips []string
	for _, mac := range macs {
		ips = append(ips, a.ipMapping.IPsForMAC(mac)...)
	}
	return ips
}

func (a *Agent) isMACAllowed(mac string) bool {
	a.mu.RLock()
	defer a.mu.RUnlock()
	_, ok := a.allowedMACs[mac]
	return ok
}

func (a *Agent) withTimeout(ctx context.Context, fn func(context.Context) error) error {
	ctx, cancel := context.WithTimeout(ctx, a.actionTimeout)
	defer cancel()
	return fn(ctx)
}

func buildAck(id string, err error) *routeragentpb.CommandAck {
	if err == nil {
		return &routeragentpb.CommandAck{
			Id:      id,
			Success: true,
		}
	}
	return &routeragentpb.CommandAck{
		Id:           id,
		Success:      false,
		ErrorMessage: stringPtr(err.Error()),
	}
}

func normalizeMACs(macs []string) ([]string, error) {
	out := make([]string, 0, len(macs))
	for _, mac := range macs {
		normalized, ok := normalize.MAC(mac)
		if !ok {
			return nil, errInvalidMAC(mac)
		}
		out = append(out, normalized)
	}
	return out, nil
}

func stringPtr(value string) *string {
	return &value
}

func errInvalidMAC(value string) error {
	return &inputError{kind: "mac", value: value}
}

func errInvalidIP(value string) error {
	return &inputError{kind: "ip", value: value}
}

type inputError struct {
	kind  string
	value string
}

func (e *inputError) Error() string {
	return "invalid " + e.kind + " address: " + e.value
}
