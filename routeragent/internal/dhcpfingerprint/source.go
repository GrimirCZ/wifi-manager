package dhcpfingerprint

import "context"

type lineSource interface {
	Start(ctx context.Context, onLine func(string)) error
}
