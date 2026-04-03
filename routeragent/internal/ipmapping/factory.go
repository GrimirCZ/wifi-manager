package ipmapping

import (
	"context"
	"log"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
)

func New(ctx context.Context, cfg config.Config) Provider {
	if cfg.DummyMode {
		log.Printf("warning: dummy ip mapping provider enabled")
		return NewDummyProvider(ctx)
	}
	return NewNetlinkProvider(ctx, cfg.ManagedInterfaces)
}
