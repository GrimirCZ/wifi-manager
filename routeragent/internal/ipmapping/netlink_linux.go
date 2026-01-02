//go:build linux

package ipmapping

import (
	"context"
	"log"

	"github.com/vishvananda/netlink"
	"golang.org/x/sys/unix"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/normalize"
)

type NetlinkProvider struct {
	ctx     context.Context
	updates chan Update
	store   *store
}

const UpdateChannelSize = 1024

func NewNetlinkProvider(ctx context.Context) *NetlinkProvider {
	return &NetlinkProvider{
		ctx:     ctx,
		updates: make(chan Update, UpdateChannelSize),
		store:   newStore(),
	}
}

func (n *NetlinkProvider) Start() error {
	updates := make(chan netlink.NeighUpdate)
	done := make(chan struct{})

	if err := netlink.NeighSubscribe(updates, done); err != nil {
		return err
	}

	go func() {
		defer close(done)
		for {
			select {
			case <-n.ctx.Done():
				return
			case update, ok := <-updates:
				if !ok {
					return
				}
				ip, ok := normalize.IPFromNet(update.Neigh.IP)
				if !ok {
					continue
				}
				if update.Type == unix.RTM_DELNEIGH {
					mac, _ := n.store.removeByIP(ip)
					select {
					case n.updates <- Update{IP: ip, MAC: mac, Deleted: true}:
					default:
						log.Printf("ipmapping update channel full, dropping delete ip=%s mac=%s", ip, mac)
					}
					continue
				}

				mac, ok := normalize.MACFromNet(update.Neigh.HardwareAddr)
				if !ok {
					continue
				}
				ip, mac, ok = n.store.update(ip, mac)
				if !ok {
					continue
				}
				select {
				case n.updates <- Update{IP: ip, MAC: mac}:
				default:
					log.Printf("ipmapping update channel full, dropping update ip=%s mac=%s", ip, mac)
				}
			}
		}
	}()

	return nil
}

func (n *NetlinkProvider) Updates() <-chan Update {
	return n.updates
}

func (n *NetlinkProvider) LookupMAC(ip string) (string, bool) {
	return n.store.lookupMAC(ip)
}

func (n *NetlinkProvider) IPsForMAC(mac string) []string {
	return n.store.ipsForMAC(mac)
}
