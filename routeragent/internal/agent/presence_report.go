package agent

import (
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/ipmapping"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentgrpc"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/routeragentpb"
)

// buildAllowedClientsPresence returns allowed MACs currently visible on L2 with non-zero last_seen_at.
// report_at is wall-clock on the router; entry last_seen_at values come from ipmapping observation times.
func (a *Agent) buildAllowedClientsPresence() *routeragentpb.AllowedClientsPresence {
	allowed := a.allowedMACSnapshot()
	views := a.ipMapping.ListClients()
	reportAt := nowUTC().Format(time.RFC3339)
	entries := make([]*routeragentpb.AllowedClientsPresenceEntry, 0, len(views))
	for _, view := range views {
		if !allowed[view.MAC] {
			continue
		}
		if view.LastSeenAt.IsZero() {
			continue
		}
		entries = append(entries, &routeragentpb.AllowedClientsPresenceEntry{
			MacAddress:     view.MAC,
			LastSeenAt:     formatTimestamp(view.LastSeenAt),
			NeighborStatus: neighborStatusToProto(view.Status),
		})
	}
	return &routeragentpb.AllowedClientsPresence{
		ReportAt: reportAt,
		Entries:  entries,
	}
}

func neighborStatusToProto(status ipmapping.NeighborStatus) routeragentpb.NeighborStatus {
	if status == ipmapping.NeighborStatusStale {
		return routeragentpb.NeighborStatus_NEIGHBOR_STATUS_STALE
	}
	return routeragentpb.NeighborStatus_NEIGHBOR_STATUS_LIVE
}

// ReportAllowedClientsPresence sends a presence snapshot on the gRPC stream; no-op when there is nothing to report.
func (a *Agent) ReportAllowedClientsPresence(stream *routeragentgrpc.Stream) error {
	if stream == nil {
		return nil
	}
	presence := a.buildAllowedClientsPresence()
	if presence == nil || len(presence.Entries) == 0 {
		return nil
	}
	return stream.SendAllowedClientsPresence(presence)
}
