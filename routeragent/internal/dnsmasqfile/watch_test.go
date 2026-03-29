package dnsmasqfile

import (
	"context"
	"os"
	"path/filepath"
	"sync"
	"testing"
	"time"
)

func TestFollowLinesReadsInitialAndAppendedLines(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")
	if err := os.WriteFile(path, []byte("one\ntwo\n"), 0o644); err != nil {
		t.Fatalf("write file: %v", err)
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var mu sync.Mutex
	lines := make([]string, 0)
	if err := FollowLines(ctx, path, func(line string) {
		mu.Lock()
		defer mu.Unlock()
		lines = append(lines, line)
	}); err != nil {
		t.Fatalf("follow lines: %v", err)
	}

	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return len(lines) == 2
	})

	appendFile(t, path, "three\n")

	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return len(lines) == 3 && lines[2] == "three"
	})
}

func TestFollowLinesWaitsForTrailingNewline(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")
	if err := os.WriteFile(path, []byte("one\n"), 0o644); err != nil {
		t.Fatalf("write file: %v", err)
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var mu sync.Mutex
	lines := make([]string, 0)
	if err := FollowLines(ctx, path, func(line string) {
		mu.Lock()
		defer mu.Unlock()
		lines = append(lines, line)
	}); err != nil {
		t.Fatalf("follow lines: %v", err)
	}

	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return len(lines) == 1
	})

	appendFile(t, path, "partial")
	time.Sleep(100 * time.Millisecond)

	mu.Lock()
	if len(lines) != 1 {
		t.Fatalf("unexpected lines before newline: %#v", lines)
	}
	mu.Unlock()

	appendFile(t, path, "\n")
	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return len(lines) == 2 && lines[1] == "partial"
	})
}

func TestFollowLinesHandlesMissingFileAndRecreate(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var mu sync.Mutex
	lines := make([]string, 0)
	if err := FollowLines(ctx, path, func(line string) {
		mu.Lock()
		defer mu.Unlock()
		lines = append(lines, line)
	}); err != nil {
		t.Fatalf("follow lines: %v", err)
	}

	if err := os.WriteFile(path, []byte("created\n"), 0o644); err != nil {
		t.Fatalf("write file: %v", err)
	}

	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return len(lines) == 1 && lines[0] == "created"
	})
}

func TestFollowLinesHandlesRenameAndContinue(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.log")
	if err := os.WriteFile(path, []byte("before\n"), 0o644); err != nil {
		t.Fatalf("write file: %v", err)
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var mu sync.Mutex
	lines := make([]string, 0)
	if err := FollowLines(ctx, path, func(line string) {
		mu.Lock()
		defer mu.Unlock()
		lines = append(lines, line)
	}); err != nil {
		t.Fatalf("follow lines: %v", err)
	}

	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return len(lines) == 1
	})

	rotated := filepath.Join(dir, "dnsmasq.log.1")
	if err := os.Rename(path, rotated); err != nil {
		t.Fatalf("rename: %v", err)
	}
	if err := os.WriteFile(path, []byte("after\n"), 0o644); err != nil {
		t.Fatalf("write new file: %v", err)
	}

	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return len(lines) == 2 && lines[1] == "after"
	})
}

func TestWatchFileFiresOnInitialLoadAndRewrite(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "dnsmasq.leases")
	if err := os.WriteFile(path, []byte("initial\n"), 0o644); err != nil {
		t.Fatalf("write file: %v", err)
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var mu sync.Mutex
	count := 0
	if err := WatchFile(ctx, path, func() error {
		mu.Lock()
		defer mu.Unlock()
		count += 1
		return nil
	}); err != nil {
		t.Fatalf("watch file: %v", err)
	}

	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return count >= 1
	})

	if err := os.WriteFile(path, []byte("rewritten\n"), 0o644); err != nil {
		t.Fatalf("rewrite file: %v", err)
	}

	waitFor(t, func() bool {
		mu.Lock()
		defer mu.Unlock()
		return count >= 2
	})
}

func appendFile(t *testing.T, path, value string) {
	t.Helper()
	file, err := os.OpenFile(path, os.O_APPEND|os.O_WRONLY|os.O_CREATE, 0o644)
	if err != nil {
		t.Fatalf("open append file: %v", err)
	}
	defer file.Close()
	if _, err := file.WriteString(value); err != nil {
		t.Fatalf("append file: %v", err)
	}
}

func waitFor(t *testing.T, condition func() bool) {
	t.Helper()
	deadline := time.Now().Add(3 * time.Second)
	for time.Now().Before(deadline) {
		if condition() {
			return
		}
		time.Sleep(10 * time.Millisecond)
	}
	t.Fatal("condition not met before timeout")
}
