package dhcpfingerprint

import (
	"context"
	"crypto/sha256"
	"encoding/hex"
	"regexp"
	"strings"
	"time"

	"github.com/GrimirCZ/wifi-manager/routeragent/internal/dnsmasqfile"
	"github.com/GrimirCZ/wifi-manager/routeragent/internal/normalize"
)

type DnsmasqProvider struct {
	ctx                  context.Context
	logPath              string
	store                *store
	transactions         map[string]*transactionObservation
	lastTransactionByMAC map[string]string
	now                  func() time.Time
}

func NewDnsmasqProvider(ctx context.Context, logPath string) *DnsmasqProvider {
	return &DnsmasqProvider{
		ctx:                  ctx,
		logPath:              logPath,
		store:                newStore(),
		transactions:         make(map[string]*transactionObservation),
		lastTransactionByMAC: make(map[string]string),
		now:                  time.Now,
	}
}

func (d *DnsmasqProvider) Start() error {
	if strings.TrimSpace(d.logPath) == "" {
		return nil
	}

	return dnsmasqfile.FollowLines(d.ctx, d.logPath, d.consumeLine)
}

func (d *DnsmasqProvider) LookupByMAC(mac string) (Observation, bool) {
	return d.store.lookup(strings.ToLower(strings.TrimSpace(mac)))
}

var macPattern = regexp.MustCompile(`(?i)([0-9a-f]{2}(?::[0-9a-f]{2}){5})`)
var transactionPattern = regexp.MustCompile(`^(?:.*dnsmasq-dhcp\[[0-9]+\]:\s+)?([0-9]+)\b`)
var optionPrefixPattern = regexp.MustCompile(`^\s*(\d+)`)

func (d *DnsmasqProvider) consumeLine(line string) {
	line = strings.TrimSpace(line)
	if line == "" {
		return
	}

	now := d.now()
	d.cleanupTransactions(now)

	txid, hasTx := extractTransactionID(line)
	if !hasTx {
		return
	}

	tx := d.transaction(txid, now)
	lower := strings.ToLower(line)

	if value, ok := extractValueAfter(lower, []string{"vendor class:", "vendor-class:", "vendor-class(60):"}); ok {
		tx.observation.VendorClass = strings.Trim(value, `" `)
	}
	if value, ok := extractValueAfter(lower, []string{"requested options:", "requested-options:", "parameter request list:", "option 55:"}); ok {
		tx.prlOptions = append(tx.prlOptions, extractRequestedOptionNumbers(value)...)
		tx.observation.PRLHash = hashOrderedList(tx.prlOptions)
	}
	if value, ok := extractValueAfter(lower, []string{"hostname:", "client-hostname:"}); ok {
		tx.observation.Hostname = strings.Trim(value, `" `)
	}

	if mac, ok := extractMAC(line); ok {
		tx.mac = mac
		d.lastTransactionByMAC[mac] = txid
	}

	tx.lastUpdatedAt = now
	if tx.mac != "" {
		d.store.update(tx.mac, tx.observation)
	}
}

func extractValueAfter(line string, prefixes []string) (string, bool) {
	for _, prefix := range prefixes {
		index := strings.Index(line, prefix)
		if index < 0 {
			continue
		}
		return strings.TrimSpace(line[index+len(prefix):]), true
	}
	return "", false
}

func extractTransactionID(line string) (string, bool) {
	match := transactionPattern.FindStringSubmatch(line)
	if len(match) < 2 {
		return "", false
	}
	return match[1], true
}

func extractMAC(line string) (string, bool) {
	match := macPattern.FindStringSubmatch(line)
	if len(match) < 2 {
		return "", false
	}
	return normalize.MAC(match[1])
}

func extractRequestedOptionNumbers(value string) []string {
	tokens := strings.Split(value, ",")
	options := make([]string, 0, len(tokens))
	for _, token := range tokens {
		match := optionPrefixPattern.FindStringSubmatch(token)
		if len(match) < 2 {
			continue
		}
		options = append(options, match[1])
	}
	if len(options) == 0 {
		return nil
	}
	return options
}

func hashOrderedList(values []string) string {
	if len(values) == 0 {
		return ""
	}
	normalized := strings.Join(values, ",")
	sum := sha256.Sum256([]byte(normalized))
	return hex.EncodeToString(sum[:])
}

func (d *DnsmasqProvider) transaction(
	txid string,
	now time.Time,
) *transactionObservation {
	if tx, ok := d.transactions[txid]; ok {
		tx.lastUpdatedAt = now
		return tx
	}
	tx := &transactionObservation{lastUpdatedAt: now}
	d.transactions[txid] = tx
	return tx
}

func (d *DnsmasqProvider) cleanupTransactions(now time.Time) {
	for txid, tx := range d.transactions {
		if now.Sub(tx.lastUpdatedAt) <= transactionTTL {
			continue
		}
		delete(d.transactions, txid)
	}
	for mac, txid := range d.lastTransactionByMAC {
		tx, ok := d.transactions[txid]
		if !ok || tx.mac != mac {
			delete(d.lastTransactionByMAC, mac)
		}
	}
}

type transactionObservation struct {
	observation   Observation
	prlOptions    []string
	mac           string
	lastUpdatedAt time.Time
}

const transactionTTL = 2 * time.Minute
