package ipmapping

import "context"

type DummyProvider struct {
	store *store
}

func NewDummyProvider(ctx context.Context) *DummyProvider {
	s := newStore(ctx, nil)
	s.update("127.0.0.1", "00:00:00:00:00:00", "")
	return &DummyProvider{store: s}
}

func (d *DummyProvider) Start() error {
	return nil
}

func (d *DummyProvider) Updates() <-chan Update {
	return nil
}

func (d *DummyProvider) LookupMAC(ip string) (string, bool) {
	return d.store.lookupMAC(ip)
}

func (d *DummyProvider) IPsForMAC(mac string) []string {
	return d.store.ipsForMAC(mac)
}

func (d *DummyProvider) ListClients() []ClientView {
	return d.store.listClients()
}
