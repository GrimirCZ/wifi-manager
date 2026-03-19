package firewall

import (
	"fmt"
	"log"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/config"
)

func New(cfg config.Config) (Backend, error) {
	if cfg.DummyMode {
		log.Printf("warning: dummy firewall backend enabled")
		return NewDummyBackend(), nil
	}

	if cfg.NftTable == "" {
		return nil, fmt.Errorf("ROUTERAGENT_NFT_TABLE is required unless dummy mode is enabled")
	}
	if cfg.NftSetV4 == "" && cfg.NftSetV6 == "" {
		return nil, fmt.Errorf("ROUTERAGENT_NFT_SET_V4 or ROUTERAGENT_NFT_SET_V6 is required unless dummy mode is enabled")
	}

	return NftablesBackend{
		Family: cfg.NftFamily,
		Table:  cfg.NftTable,
		SetV4:  cfg.NftSetV4,
		SetV6:  cfg.NftSetV6,
	}, nil
}
