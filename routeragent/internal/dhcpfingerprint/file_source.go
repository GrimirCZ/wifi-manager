package dhcpfingerprint

import (
	"context"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/dnsmasqfile"
)

type fileLineSource struct {
	path string
}

func newFileLineSource(path string) lineSource {
	return &fileLineSource{path: path}
}

func (s *fileLineSource) Start(ctx context.Context, onLine func(string)) error {
	return dnsmasqfile.FollowLines(ctx, s.path, onLine)
}
