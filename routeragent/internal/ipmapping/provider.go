package ipmapping

import "sync"

type Update struct {
	IP      string
	MAC     string
	Deleted bool
}

type Provider interface {
	Start() error
	Updates() <-chan Update
	LookupMAC(ip string) (string, bool)
	IPsForMAC(mac string) []string
}

type store struct {
	mu      sync.RWMutex
	ipToMAC map[string]string
	macToIP map[string]map[string]struct{}
}

func newStore() *store {
	return &store{
		ipToMAC: make(map[string]string),
		macToIP: make(map[string]map[string]struct{}),
	}
}

func (s *store) update(ipStr, macStr string) (string, string, bool) {
	if ipStr == "" || macStr == "" {
		return "", "", false
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	if existing, ok := s.ipToMAC[ipStr]; ok && existing != macStr {
		s.removeIPLocked(existing, ipStr)
	}

	s.ipToMAC[ipStr] = macStr
	s.addIPLocked(macStr, ipStr)
	return ipStr, macStr, true
}

func (s *store) removeByIP(ipStr string) (string, bool) {
	if ipStr == "" {
		return "", false
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	mac, ok := s.ipToMAC[ipStr]
	if !ok {
		return "", false
	}
	delete(s.ipToMAC, ipStr)
	s.removeIPLocked(mac, ipStr)
	return mac, true
}

func (s *store) lookupMAC(ip string) (string, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	mac, ok := s.ipToMAC[ip]
	return mac, ok
}

func (s *store) ipsForMAC(mac string) []string {
	s.mu.RLock()
	defer s.mu.RUnlock()
	ips := s.macToIP[mac]
	if len(ips) == 0 {
		return nil
	}
	out := make([]string, 0, len(ips))
	for ip := range ips {
		out = append(out, ip)
	}
	return out
}

func (s *store) addIPLocked(mac, ip string) {
	ips := s.macToIP[mac]
	if ips == nil {
		ips = make(map[string]struct{})
		s.macToIP[mac] = ips
	}
	ips[ip] = struct{}{}
}

func (s *store) removeIPLocked(mac, ip string) {
	ips := s.macToIP[mac]
	if ips == nil {
		return
	}
	delete(ips, ip)
	if len(ips) == 0 {
		delete(s.macToIP, mac)
	}
}
