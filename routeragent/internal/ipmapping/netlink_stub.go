//go:build !linux

package ipmapping

import (
	"context"
	"errors"
)

type NetlinkProvider struct{}

func NewNetlinkProvider(ctx context.Context, managedInterfaces []string) *NetlinkProvider {
	_ = ctx
	_ = managedInterfaces
	return &NetlinkProvider{}
}

func (n *NetlinkProvider) Start() error {
	return errors.New("netlink provider not supported on this OS")
}

func (n *NetlinkProvider) Updates() <-chan Update {
	return nil
}

func (n *NetlinkProvider) LookupMAC(ip string) (string, bool) {
	return "", false
}

func (n *NetlinkProvider) IPsForMAC(mac string) []string {
	return nil
}

func (n *NetlinkProvider) ListClients() []ClientView {
	return nil
}
