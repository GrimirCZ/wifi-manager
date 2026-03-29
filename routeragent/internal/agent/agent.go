package agent

import (
	"context"
	"log"
	"net"
	"slices"
	"sync"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/allowedip"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/dhcpfingerprint"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/firewall"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/hostname"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/macutil"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/normalize"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentgrpc"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentpb"
)

// Agent combines three state domains: observed MAC/IP state from ipmapping,
// allowed-MAC policy state held in memory, and firewall state that is gated
// separately so reconciliation can compare and repair backend reality.
type Agent struct {
	firewall          firewall.Backend
	ipMapping         ipmapping.Provider
	hostname          hostname.Provider
	allowedIPs        allowedip.Repository
	dhcpFingerprint   dhcpfingerprint.Provider
	allowedMACs       map[string]struct{}
	mu                sync.RWMutex
	firewallMu        sync.RWMutex
	senderMu          sync.RWMutex
	pendingMu         sync.Mutex
	pendingObserved   map[string]*pendingObservation
	observationSender observationSender
	actionTimeout     time.Duration
	observationDelay  time.Duration
}

type pendingObservation struct {
	cancel        context.CancelFunc
	interfaceName string
	ready         bool
	sending       bool
}

type ackSender interface {
	SendAck(ack *routeragentpb.CommandAck) error
}

type observationSender interface {
	SendAuthorizedClientObserved(observed *routeragentpb.AuthorizedClientObserved) error
}

func New(
	firewall firewall.Backend,
	ipMapping ipmapping.Provider,
	hostname hostname.Provider,
	allowedIPs allowedip.Repository,
	dhcpFingerprint dhcpfingerprint.Provider,
	actionTimeout time.Duration,
) *Agent {
	return &Agent{
		firewall:         firewall,
		ipMapping:        ipMapping,
		hostname:         hostname,
		allowedIPs:       allowedIPs,
		dhcpFingerprint:  dhcpFingerprint,
		allowedMACs:      make(map[string]struct{}),
		pendingObserved:  make(map[string]*pendingObservation),
		actionTimeout:    actionTimeout,
		observationDelay: 30 * time.Second,
	}
}

// OnIPMappingUpdate bridges observed-state changes into derived whitelist
// updates so newly seen allowed devices are whitelisted incrementally.
func (a *Agent) OnIPMappingUpdate(ctx context.Context, update ipmapping.Update) {
	if update.IP == "" || update.MAC == "" {
		if update.Deleted && update.IP != "" {
			a.removeAllowedIP(ctx, update.IP)
		}
		return
	}
	if update.Deleted {
		a.removeAllowedIP(ctx, update.IP)
		if len(a.ipMapping.IPsForMAC(update.MAC)) == 0 {
			a.cancelPendingObservation(update.MAC)
		}
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
		return a.allowIPs(ctx, added)
	}); err != nil {
		log.Printf("failed to allow ip=%s for mac=%s: %v", update.IP, update.MAC, err)
	}
	if len(a.ipMapping.IPsForMAC(update.MAC)) == 1 {
		a.schedulePendingObservation(update.MAC, update.InterfaceName)
		return
	}
	a.refreshPendingObservation(update.MAC, update.InterfaceName)
}

func (a *Agent) removeAllowedIP(ctx context.Context, ip string) {
	removed := a.allowedIPs.Remove([]string{ip})
	if len(removed) == 0 {
		return
	}
	if err := a.withTimeout(ctx, func(ctx context.Context) error {
		return a.removeIPs(ctx, removed)
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
	case *routeragentpb.RouterAgentCommand_ListNetworkClients:
		return a.handleListNetworkClients(stream, c.ListNetworkClients)
	default:
		return nil
	}
}

func (a *Agent) handleGetClientInfo(stream ackSender, cmd *routeragentpb.GetClientInfo) error {
	if cmd == nil {
		return nil
	}

	ip, ok := normalize.IP(cmd.IpAddress)
	if !ok {
		return stream.SendAck(buildAck(cmd.Id, errInvalidIP(cmd.IpAddress)))
	}

	mac, ok := a.ipMapping.LookupMAC(ip)
	if ok {
		a.cancelPendingObservation(mac)
	}
	hostname, _ := a.hostname.LookupHostname(ip)
	dhcpObservation, dhcpOK := a.dhcpLookup(mac)

	ack := &routeragentpb.CommandAck{
		Id:      cmd.Id,
		Success: true,
	}
	if ok {
		ack.MacAddress = optionalStringPtr(mac)
	}
	ack.Hostname = optionalStringPtr(hostname)
	if dhcpOK {
		ack.DhcpVendorClass = optionalStringPtr(dhcpObservation.VendorClass)
		ack.DhcpPrlHash = optionalStringPtr(dhcpObservation.PRLHash)
		ack.DhcpHostname = optionalStringPtr(dhcpObservation.Hostname)
	}
	return stream.SendAck(ack)
}

func (a *Agent) handleAllowClientAccess(ctx context.Context, stream ackSender, cmd *routeragentpb.AllowClientAccess) error {
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

func (a *Agent) handleRevokeClientAccess(ctx context.Context, stream ackSender, cmd *routeragentpb.RevokeClientAccess) error {
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

func (a *Agent) handleSetAllowedClients(ctx context.Context, stream ackSender, cmd *routeragentpb.SetAllowedClients) error {
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

func (a *Agent) handleListNetworkClients(stream ackSender, cmd *routeragentpb.ListNetworkClients) error {
	if cmd == nil {
		return nil
	}
	return stream.SendAck(a.buildListNetworkClientsAck(cmd.Id))
}

// setAllowedMACs replaces the policy set and derives the full expected IP
// whitelist from the current observed snapshot before rebuilding the firewall.
func (a *Agent) setAllowedMACs(ctx context.Context, macs []string) error {
	removed := a.removedAllowedMACs(macs)
	a.mu.Lock()
	a.allowedMACs = make(map[string]struct{}, len(macs))
	for _, mac := range macs {
		a.allowedMACs[mac] = struct{}{}
	}
	a.mu.Unlock()
	a.cancelPendingObservations(removed)

	allIPs := a.collectIPsForMACs(macs)
	a.allowedIPs.SetAll(allIPs)

	if err := a.clearFirewall(ctx); err != nil {
		return err
	}
	if err := a.allowIPs(ctx, a.allowedIPs.List()); err != nil {
		return err
	}
	return nil
}

// allowMACs adds policy entries and derives only the newly expected IPs from
// the current observed snapshot.
func (a *Agent) allowMACs(ctx context.Context, macs []string) error {
	a.mu.Lock()
	for _, mac := range macs {
		a.allowedMACs[mac] = struct{}{}
	}
	a.mu.Unlock()

	allIPs := a.collectIPsForMACs(macs)
	added := a.allowedIPs.Add(allIPs)
	if err := a.allowIPs(ctx, added); err != nil {
		return err
	}
	return nil
}

// revokeMACs removes policy entries and derives only the IPs that should no
// longer be present based on the current observed snapshot.
func (a *Agent) revokeMACs(ctx context.Context, macs []string) error {
	a.mu.Lock()
	for _, mac := range macs {
		delete(a.allowedMACs, mac)
	}
	a.mu.Unlock()
	a.cancelPendingObservations(macs)

	allIPs := a.collectIPsForMACs(macs)
	removed := a.allowedIPs.Remove(allIPs)
	if err := a.removeIPs(ctx, removed); err != nil {
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

func (a *Agent) buildListNetworkClientsAck(id string) *routeragentpb.CommandAck {
	allowed := a.allowedMACSnapshot()
	views := a.ipMapping.ListClients()

	clients := make([]*routeragentpb.NetworkClient, 0, len(views))
	for _, view := range views {
		dhcpObservation, _ := a.dhcpLookup(view.MAC)
		hostnames := a.hostnamesForIPs(view.IPs)
		clients = append(clients, &routeragentpb.NetworkClient{
			MacAddress:      view.MAC,
			IpAddresses:     slices.Clone(view.IPs),
			Hostname:        firstStringPtr(hostnames),
			Allowed:         allowed[view.MAC],
			DhcpVendorClass: optionalStringPtr(dhcpObservation.VendorClass),
			DhcpPrlHash:     optionalStringPtr(dhcpObservation.PRLHash),
			DhcpHostname:    optionalStringPtr(dhcpObservation.Hostname),
		})
	}

	return &routeragentpb.CommandAck{
		Id:      id,
		Success: true,
		Clients: clients,
	}
}

func (a *Agent) OnStreamConnected(sender observationSender) {
	a.senderMu.Lock()
	defer a.senderMu.Unlock()
	a.observationSender = sender
	go a.flushReadyPendingObservations()
}

func (a *Agent) OnStreamDisconnected(sender observationSender) {
	a.senderMu.Lock()
	defer a.senderMu.Unlock()
	if a.observationSender == sender {
		a.observationSender = nil
	}
}

func (a *Agent) emitAuthorizedClientObserved(
	mac string,
	interfaceName string,
) bool {
	sender := a.currentObservationSender()
	if sender == nil {
		return false
	}
	if !a.isMACAllowed(mac) {
		return false
	}

	ips := ipv4Addresses(a.ipMapping.IPsForMAC(mac))
	if len(ips) == 0 {
		return false
	}
	dhcpObservation, _ := a.dhcpLookup(mac)
	hostnames := a.hostnamesForIPs(ips)
	observed := &routeragentpb.AuthorizedClientObserved{
		MacAddress:      mac,
		IpAddresses:     slices.Clone(ips),
		Hostname:        firstStringPtr(hostnames),
		ObservedAt:      time.Now().UTC().Format(time.RFC3339),
		InterfaceName:   optionalStringPtr(interfaceName),
		IsRandomized:    macutil.IsRandomizedMAC(mac),
		DhcpVendorClass: optionalStringPtr(dhcpObservation.VendorClass),
		DhcpPrlHash:     optionalStringPtr(dhcpObservation.PRLHash),
		DhcpHostname:    optionalStringPtr(dhcpObservation.Hostname),
	}
	if err := sender.SendAuthorizedClientObserved(observed); err != nil {
		log.Printf("failed to send authorized client observed mac=%s: %v", mac, err)
		return false
	}
	return true
}

func (a *Agent) schedulePendingObservation(mac, interfaceName string) {
	if mac == "" {
		return
	}

	ctx, cancel := context.WithCancel(context.Background())
	pending := &pendingObservation{
		cancel:        cancel,
		interfaceName: interfaceName,
	}

	a.pendingMu.Lock()
	if existing, ok := a.pendingObserved[mac]; ok {
		if interfaceName != "" {
			existing.interfaceName = interfaceName
		}
		a.pendingMu.Unlock()
		cancel()
		return
	}
	a.pendingObserved[mac] = pending
	a.pendingMu.Unlock()

	go func() {
		timer := time.NewTimer(a.observationDelay)
		defer timer.Stop()

		select {
		case <-ctx.Done():
			return
		case <-timer.C:
		}

		if !a.markPendingObservationReady(mac, pending) {
			return
		}
		a.flushPendingObservation(mac)
	}()
}

func (a *Agent) refreshPendingObservation(mac, interfaceName string) {
	if mac == "" || interfaceName == "" {
		return
	}
	a.pendingMu.Lock()
	defer a.pendingMu.Unlock()
	if pending, ok := a.pendingObserved[mac]; ok {
		pending.interfaceName = interfaceName
	}
	go a.flushPendingObservation(mac)
}

func (a *Agent) cancelPendingObservation(mac string) {
	if mac == "" {
		return
	}
	var cancel context.CancelFunc
	a.pendingMu.Lock()
	if pending, ok := a.pendingObserved[mac]; ok {
		delete(a.pendingObserved, mac)
		cancel = pending.cancel
	}
	a.pendingMu.Unlock()
	if cancel != nil {
		cancel()
	}
}

func (a *Agent) cancelPendingObservations(macs []string) {
	for _, mac := range macs {
		a.cancelPendingObservation(mac)
	}
}

func (a *Agent) markPendingObservationReady(mac string, pending *pendingObservation) bool {
	a.pendingMu.Lock()
	defer a.pendingMu.Unlock()
	current, ok := a.pendingObserved[mac]
	if !ok || current != pending {
		return false
	}
	current.ready = true
	return true
}

func (a *Agent) flushPendingObservation(mac string) {
	if mac == "" {
		return
	}

	var (
		pending       *pendingObservation
		interfaceName string
	)
	a.pendingMu.Lock()
	current, ok := a.pendingObserved[mac]
	if !ok || !current.ready || current.sending {
		a.pendingMu.Unlock()
		return
	}
	current.sending = true
	pending = current
	interfaceName = current.interfaceName
	a.pendingMu.Unlock()

	sent := a.emitAuthorizedClientObserved(mac, interfaceName)

	a.pendingMu.Lock()
	defer a.pendingMu.Unlock()
	current, ok = a.pendingObserved[mac]
	if !ok || current != pending {
		return
	}
	if sent {
		delete(a.pendingObserved, mac)
		return
	}
	current.sending = false
}

func (a *Agent) flushReadyPendingObservations() {
	a.pendingMu.Lock()
	macs := make([]string, 0, len(a.pendingObserved))
	for mac, pending := range a.pendingObserved {
		if pending.ready {
			macs = append(macs, mac)
		}
	}
	a.pendingMu.Unlock()

	for _, mac := range macs {
		a.flushPendingObservation(mac)
	}
}

func (a *Agent) currentObservationSender() observationSender {
	a.senderMu.RLock()
	defer a.senderMu.RUnlock()
	return a.observationSender
}

func (a *Agent) dhcpLookup(mac string) (dhcpfingerprint.Observation, bool) {
	if a.dhcpFingerprint == nil {
		return dhcpfingerprint.Observation{}, false
	}
	return a.dhcpFingerprint.LookupByMAC(mac)
}

func (a *Agent) hostnamesForIPs(ips []string) []string {
	if len(ips) == 0 {
		return nil
	}
	hostnames := make([]string, 0, len(ips))
	for _, ip := range ips {
		hostname, ok := a.hostname.LookupHostname(ip)
		if !ok || hostname == "" {
			continue
		}
		hostnames = append(hostnames, hostname)
	}
	if len(hostnames) == 0 {
		return nil
	}
	slices.Sort(hostnames)
	return hostnames
}

func (a *Agent) isMACAllowed(mac string) bool {
	a.mu.RLock()
	defer a.mu.RUnlock()
	_, ok := a.allowedMACs[mac]
	return ok
}

func (a *Agent) allowedMACSnapshot() map[string]bool {
	a.mu.RLock()
	defer a.mu.RUnlock()

	allowed := make(map[string]bool, len(a.allowedMACs))
	for mac := range a.allowedMACs {
		allowed[mac] = true
	}
	return allowed
}

func (a *Agent) removedAllowedMACs(nextAllowed []string) []string {
	next := make(map[string]struct{}, len(nextAllowed))
	for _, mac := range nextAllowed {
		next[mac] = struct{}{}
	}

	a.mu.RLock()
	defer a.mu.RUnlock()

	removed := make([]string, 0)
	for mac := range a.allowedMACs {
		if _, ok := next[mac]; ok {
			continue
		}
		removed = append(removed, mac)
	}
	return removed
}

func (a *Agent) withTimeout(ctx context.Context, fn func(context.Context) error) error {
	ctx, cancel := context.WithTimeout(ctx, a.actionTimeout)
	defer cancel()
	return fn(ctx)
}

// StartReconciler starts the periodic repair loop that compares expected
// whitelist contents from state with the actual firewall backend contents.
func (a *Agent) StartReconciler(ctx context.Context, interval time.Duration) {
	if interval <= 0 {
		return
	}

	go func() {
		ticker := time.NewTicker(interval)
		defer ticker.Stop()

		for {
			select {
			case <-ctx.Done():
				return
			case <-ticker.C:
				if err := a.reconcileFirewall(ctx); err != nil {
					log.Printf("firewall reconciliation failed: %v", err)
				}
			}
		}
	}()
}

// reconcileFirewall computes the expected whitelist from state, reads the
// actual nft contents, repairs drift, and refreshes the in-memory mirror.
func (a *Agent) reconcileFirewall(ctx context.Context) error {
	allowed := a.allowedMACSnapshot()
	expected := a.expectedAllowedIPs(allowed)

	a.firewallMu.Lock()
	defer a.firewallMu.Unlock()

	actual, err := a.firewall.ListIPs(ctx)
	if err != nil {
		return err
	}

	expectedSet := make(map[string]struct{}, len(expected))
	for _, ip := range expected {
		expectedSet[ip] = struct{}{}
	}
	actualSet := make(map[string]struct{}, len(actual))
	for _, ip := range actual {
		actualSet[ip] = struct{}{}
	}

	var toAdd []string
	for _, ip := range expected {
		if _, ok := actualSet[ip]; !ok {
			toAdd = append(toAdd, ip)
		}
	}

	var toRemove []string
	for _, ip := range actual {
		if _, ok := expectedSet[ip]; !ok {
			toRemove = append(toRemove, ip)
		}
	}

	if err := a.firewall.AllowIPs(ctx, toAdd); err != nil {
		return err
	}
	if err := a.firewall.RemoveIPs(ctx, toRemove); err != nil {
		return err
	}

	a.allowedIPs.SetAll(expected)
	return nil
}

// expectedAllowedIPs is the authoritative derived whitelist computation from
// allowed MAC policy and the current observed MAC -> IP view.
func (a *Agent) expectedAllowedIPs(allowed map[string]bool) []string {
	if len(allowed) == 0 {
		return nil
	}

	seen := make(map[string]struct{})
	for _, client := range a.ipMapping.ListClients() {
		if !allowed[client.MAC] {
			continue
		}
		for _, ip := range client.IPs {
			parsed := net.ParseIP(ip)
			if parsed == nil {
				continue
			}
			seen[parsed.String()] = struct{}{}
		}
	}

	ips := make([]string, 0, len(seen))
	for ip := range seen {
		ips = append(ips, ip)
	}
	slices.Sort(ips)
	return ips
}

func (a *Agent) allowIPs(ctx context.Context, ips []string) error {
	if len(ips) == 0 {
		return nil
	}
	a.firewallMu.RLock()
	defer a.firewallMu.RUnlock()
	return a.firewall.AllowIPs(ctx, ips)
}

func (a *Agent) removeIPs(ctx context.Context, ips []string) error {
	if len(ips) == 0 {
		return nil
	}
	a.firewallMu.RLock()
	defer a.firewallMu.RUnlock()
	return a.firewall.RemoveIPs(ctx, ips)
}

func (a *Agent) clearFirewall(ctx context.Context) error {
	a.firewallMu.RLock()
	defer a.firewallMu.RUnlock()
	return a.firewall.Clear(ctx)
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

func optionalStringPtr(value string) *string {
	if value == "" {
		return nil
	}
	return stringPtr(value)
}

func firstStringPtr(values []string) *string {
	if len(values) == 0 {
		return nil
	}
	return stringPtr(values[0])
}

func ipv4Addresses(values []string) []string {
	if len(values) == 0 {
		return nil
	}
	filtered := make([]string, 0, len(values))
	for _, value := range values {
		ip := net.ParseIP(value)
		if ip == nil || ip.To4() == nil {
			continue
		}
		filtered = append(filtered, value)
	}
	if len(filtered) == 0 {
		return nil
	}
	return filtered
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
