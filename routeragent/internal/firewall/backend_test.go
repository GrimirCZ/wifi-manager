package firewall

import (
	"reflect"
	"strings"
	"testing"
)

func TestParseNftListSetIPsFlatElemArray(t *testing.T) {
	output := []byte(`{"nftables":[{"metainfo":{"version":"1.0.9"}},{"set":{"family":"ip","name":"allowedIps","table":"captive","type":"ipv4_addr","handle":9,"elem":["172.16.0.102","172.16.0.131","172.16.0.102"]}}]}`)

	ips, err := parseNftListSetIPs(output, "allowedIps")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if want := []string{"172.16.0.102", "172.16.0.131"}; !reflect.DeepEqual(ips, want) {
		t.Fatalf("unexpected ips: %#v", ips)
	}
}

func TestParseNftListSetIPsMissingElemYieldsEmpty(t *testing.T) {
	output := []byte(`{"nftables":[{"set":{"family":"ip","name":"allowedIps","table":"captive","type":"ipv4_addr","handle":9}}]}`)

	ips, err := parseNftListSetIPs(output, "allowedIps")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if len(ips) != 0 {
		t.Fatalf("expected empty ips, got %#v", ips)
	}
}

func TestParseNftListSetIPsMalformedJSONReturnsError(t *testing.T) {
	output := []byte(`{"nftables":[{"set":{"elem":"not-an-array"}}]}`)

	_, err := parseNftListSetIPs(output, "allowedIps")
	if err == nil {
		t.Fatal("expected error")
	}
	if !strings.Contains(err.Error(), "decode nft json") {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestParseNftListSetIPsInvalidIPReturnsError(t *testing.T) {
	output := []byte(`{"nftables":[{"set":{"elem":["not-an-ip"]}}]}`)

	_, err := parseNftListSetIPs(output, "allowedIps")
	if err == nil {
		t.Fatal("expected error")
	}
	if !strings.Contains(err.Error(), `invalid ip "not-an-ip"`) {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestListIPsDualSetUnion(t *testing.T) {
	backend := NewDummyBackend()
	if err := backend.AllowIPs(nil, []string{"172.16.0.102", "2001:db8::1"}); err != nil {
		t.Fatalf("unexpected setup error: %v", err)
	}

	ips, err := backend.ListIPs(nil)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if want := []string{"172.16.0.102", "2001:db8::1"}; !reflect.DeepEqual(ips, want) {
		t.Fatalf("unexpected ips: %#v", ips)
	}
}
