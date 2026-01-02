package hostname

import "sync"

type Provider interface {
	Start() error
	LookupHostname(ip string) (string, bool)
}

type store struct {
	mu           sync.RWMutex
	ipToHostname map[string]string
}

func newStore() *store {
	return &store{
		ipToHostname: make(map[string]string),
	}
}

func (s *store) update(entries map[string]string) {
	s.mu.Lock()
	defer s.mu.Unlock()
	for ip, hostname := range entries {
		if hostname == "" {
			delete(s.ipToHostname, ip)
			continue
		}
		s.ipToHostname[ip] = hostname
	}
}

func (s *store) lookup(ip string) (string, bool) {
	s.mu.RLock()
	defer s.mu.RUnlock()
	hostname, ok := s.ipToHostname[ip]
	if !ok || hostname == "" {
		return "", false
	}
	return hostname, true
}
