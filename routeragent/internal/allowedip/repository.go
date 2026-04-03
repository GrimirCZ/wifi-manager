package allowedip

import "sync"

type Repository interface {
	SetAll(ips []string) (added []string, removed []string)
	Add(ips []string) (added []string)
	Remove(ips []string) (removed []string)
	Contains(ip string) bool
	List() []string
}

type MemoryRepository struct {
	mu  sync.RWMutex
	set map[string]struct{}
}

func NewMemoryRepository() *MemoryRepository {
	return &MemoryRepository{
		set: make(map[string]struct{}),
	}
}

func (m *MemoryRepository) SetAll(ips []string) ([]string, []string) {
	m.mu.Lock()
	defer m.mu.Unlock()

	next := make(map[string]struct{}, len(ips))
	for _, ip := range ips {
		next[ip] = struct{}{}
	}

	var added []string
	for ip := range next {
		if _, ok := m.set[ip]; !ok {
			added = append(added, ip)
		}
	}

	var removed []string
	for ip := range m.set {
		if _, ok := next[ip]; !ok {
			removed = append(removed, ip)
		}
	}

	m.set = next
	return added, removed
}

func (m *MemoryRepository) Add(ips []string) []string {
	m.mu.Lock()
	defer m.mu.Unlock()

	var added []string
	for _, ip := range ips {
		if _, ok := m.set[ip]; ok {
			continue
		}
		m.set[ip] = struct{}{}
		added = append(added, ip)
	}
	return added
}

func (m *MemoryRepository) Remove(ips []string) []string {
	m.mu.Lock()
	defer m.mu.Unlock()

	var removed []string
	for _, ip := range ips {
		if _, ok := m.set[ip]; !ok {
			continue
		}
		delete(m.set, ip)
		removed = append(removed, ip)
	}
	return removed
}

func (m *MemoryRepository) Contains(ip string) bool {
	m.mu.RLock()
	defer m.mu.RUnlock()
	_, ok := m.set[ip]
	return ok
}

func (m *MemoryRepository) List() []string {
	m.mu.RLock()
	defer m.mu.RUnlock()
	out := make([]string, 0, len(m.set))
	for ip := range m.set {
		out = append(out, ip)
	}
	return out
}
