package dhcpfingerprint

import (
	"bufio"
	"context"
	"fmt"
	"io"
	"log"
	"os/exec"
	"strings"
)

var journalctlCommandContext = exec.CommandContext

type journalctlLineSource struct {
	unit string
}

func newJournalctlLineSource(unit string) lineSource {
	return &journalctlLineSource{unit: unit}
}

func (s *journalctlLineSource) Start(ctx context.Context, onLine func(string)) error {
	if strings.TrimSpace(s.unit) == "" {
		return nil
	}

	cmd := journalctlCommandContext(
		ctx,
		"journalctl",
		"-u", s.unit,
		"-o", "cat",
		"--no-tail",
		"-f",
	)

	stdout, err := cmd.StdoutPipe()
	if err != nil {
		return fmt.Errorf("journalctl stdout: %w", err)
	}
	stderr, err := cmd.StderrPipe()
	if err != nil {
		return fmt.Errorf("journalctl stderr: %w", err)
	}
	if err := cmd.Start(); err != nil {
		return fmt.Errorf("start journalctl: %w", err)
	}

	go scanLines(stdout, onLine)
	go logJournalctlStderr(stderr)
	go func() {
		if err := cmd.Wait(); err != nil && ctx.Err() == nil {
			log.Printf("dhcp journald stream stopped unexpectedly for unit=%s: %v", s.unit, err)
		}
	}()

	return nil
}

func scanLines(reader io.Reader, onLine func(string)) {
	scanner := bufio.NewScanner(reader)
	buffer := make([]byte, 0, 64*1024)
	scanner.Buffer(buffer, 1024*1024)
	for scanner.Scan() {
		onLine(scanner.Text())
	}
}

func logJournalctlStderr(reader io.Reader) {
	scanner := bufio.NewScanner(reader)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		log.Printf("dhcp journald stream stderr: %s", line)
	}
}
