package dhcpfingerprint

import "sync"

type Observation struct {
	VendorClass string
	PRLHash     string
	Hostname    string
}

type Provider interface {
	Start() error
	LookupByMAC(mac string) (Observation, bool)
}

type store struct {
	mu    sync.RWMutex
	byMAC map[string]Observation
}

func newStore() *store {
	return &store{byMAC: make(map[string]Observation)}
}

func (s *store) replace(next map[string]Observation) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.byMAC = next
}

func (s *store) update(mac string, observation Observation) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.byMAC[mac] = observation
}

func (s *store) lookup(mac string) (Observation, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	observation, ok := s.byMAC[mac]
	return observation, ok
}
