package dhcpfingerprint

import (
	"context"
	"os"
	"os/exec"
	"reflect"
	"testing"
	"time"
)

func TestJournaldProviderReplaysHistoryAndContinuesFollowing(t *testing.T) {
	original := journalctlCommandContext
	journalctlCommandContext = func(ctx context.Context, name string, args ...string) *exec.Cmd {
		if name != "journalctl" {
			t.Fatalf("unexpected command: %s", name)
		}
		want := []string{"-u", "dnsmasq.service", "-o", "cat", "--no-tail", "-f"}
		if !reflect.DeepEqual(args, want) {
			t.Fatalf("unexpected args: %#v", args)
		}
		return helperJournalctlCommand(ctx, "replay-follow")
	}
	defer func() {
		journalctlCommandContext = original
	}()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	provider := NewJournaldProvider(ctx, "dnsmasq.service")
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	waitFor(t, func() bool {
		observation, ok := provider.LookupByMAC("00:11:22:33:44:55")
		return ok &&
			observation.VendorClass == "android-dhcp-15" &&
			observation.PRLHash == hashRequestedOptions("1", "3", "6", "15") &&
			observation.Hostname == "laptop"
	})
}

func TestJournaldProviderIgnoresEmptyAndNonMatchingLines(t *testing.T) {
	original := journalctlCommandContext
	journalctlCommandContext = func(ctx context.Context, name string, args ...string) *exec.Cmd {
		return helperJournalctlCommand(ctx, "ignore-noise")
	}
	defer func() {
		journalctlCommandContext = original
	}()

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	provider := NewJournaldProvider(ctx, "dnsmasq.service")
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	waitFor(t, func() bool {
		observation, ok := provider.LookupByMAC("aa:bb:cc:dd:ee:ff")
		return ok && observation.Hostname == "phone"
	})
}

func TestJournaldProviderReturnsStartFailure(t *testing.T) {
	original := journalctlCommandContext
	journalctlCommandContext = func(ctx context.Context, name string, args ...string) *exec.Cmd {
		return exec.CommandContext(ctx, "/path/that/does/not/exist")
	}
	defer func() {
		journalctlCommandContext = original
	}()

	provider := NewJournaldProvider(context.Background(), "dnsmasq.service")
	if err := provider.Start(); err == nil {
		t.Fatal("expected start failure")
	}
}

func TestJournaldProviderStopsOnContextCancellation(t *testing.T) {
	original := journalctlCommandContext
	journalctlCommandContext = func(ctx context.Context, name string, args ...string) *exec.Cmd {
		return helperJournalctlCommand(ctx, "wait")
	}
	defer func() {
		journalctlCommandContext = original
	}()

	ctx, cancel := context.WithCancel(context.Background())
	provider := NewJournaldProvider(ctx, "dnsmasq.service")
	if err := provider.Start(); err != nil {
		t.Fatalf("start provider: %v", err)
	}

	cancel()
	time.Sleep(100 * time.Millisecond)
}

func helperJournalctlCommand(ctx context.Context, mode string) *exec.Cmd {
	cmd := exec.CommandContext(ctx, os.Args[0], "-test.run=TestHelperProcessJournalctl", "--")
	cmd.Env = append(os.Environ(),
		"GO_WANT_HELPER_PROCESS=1",
		"TEST_JOURNALCTL_MODE="+mode,
	)
	return cmd
}

func TestHelperProcessJournalctl(t *testing.T) {
	if os.Getenv("GO_WANT_HELPER_PROCESS") != "1" {
		return
	}

	switch os.Getenv("TEST_JOURNALCTL_MODE") {
	case "replay-follow":
		_, _ = os.Stdout.WriteString("dnsmasq-dhcp[1]: 3420445372 vendor class: android-dhcp-15\n")
		_, _ = os.Stdout.WriteString("dnsmasq-dhcp[1]: 3420445372 DHCPREQUEST(ens19) 172.16.1.129 00:11:22:33:44:55\n")
		_ = os.Stdout.Sync()
		time.Sleep(50 * time.Millisecond)
		_, _ = os.Stdout.WriteString("dnsmasq-dhcp[1]: 3420445372 requested options: 1:netmask, 3:router, 6:dns-server, 15:domain-name\n")
		_, _ = os.Stdout.WriteString("dnsmasq-dhcp[1]: 3420445372 hostname: laptop\n")
		_ = os.Stdout.Sync()
		time.Sleep(5 * time.Second)
	case "ignore-noise":
		_, _ = os.Stdout.WriteString("\n")
		_, _ = os.Stdout.WriteString("not a dnsmasq line\n")
		_, _ = os.Stdout.WriteString("dnsmasq-dhcp[1]: 3420445373 hostname: phone\n")
		_, _ = os.Stdout.WriteString("dnsmasq-dhcp[1]: 3420445373 DHCPACK(ens19) 172.16.1.130 aa:bb:cc:dd:ee:ff\n")
		_ = os.Stdout.Sync()
		time.Sleep(5 * time.Second)
	case "wait":
		time.Sleep(5 * time.Second)
	}

	os.Exit(0)
}
