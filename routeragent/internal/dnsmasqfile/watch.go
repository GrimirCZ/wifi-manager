package dnsmasqfile

import (
	"bytes"
	"context"
	"errors"
	"io"
	"os"
	"path/filepath"
	"strings"

	"github.com/fsnotify/fsnotify"
)

type LineHandler func(string)

type FileHandler func() error

func FollowLines(
	ctx context.Context,
	path string,
	onLine LineHandler,
) error {
	if strings.TrimSpace(path) == "" {
		return nil
	}

	state := &lineFollowerState{
		path:   path,
		onLine: onLine,
	}
	return watchPath(
		ctx,
		path,
		func() error {
			return state.syncCurrentFile()
		},
		func() error {
			state.close()
			return nil
		},
	)
}

func WatchFile(
	ctx context.Context,
	path string,
	onChange FileHandler,
) error {
	if strings.TrimSpace(path) == "" {
		return nil
	}

	return watchPath(
		ctx,
		path,
		onChange,
		onChange,
	)
}

func watchPath(
	ctx context.Context,
	path string,
	onFileChanged func() error,
	onFileMissing func() error,
) error {
	dir := filepath.Dir(path)
	target := filepath.Base(path)

	watcher, err := fsnotify.NewWatcher()
	if err != nil {
		return err
	}
	if err := watcher.Add(dir); err != nil {
		_ = watcher.Close()
		return err
	}

	if _, err := os.Stat(path); err == nil {
		if err := onFileChanged(); err != nil {
			_ = watcher.Close()
			return err
		}
	} else if err != nil && !errors.Is(err, os.ErrNotExist) {
		_ = watcher.Close()
		return err
	}

	go func() {
		defer watcher.Close()
		for {
			select {
			case <-ctx.Done():
				return
			case event, ok := <-watcher.Events:
				if !ok {
					return
				}
				if filepath.Base(event.Name) != target {
					continue
				}
				if event.Op&(fsnotify.Remove|fsnotify.Rename) != 0 {
					if err := onFileMissing(); err != nil {
						return
					}
					continue
				}
				if event.Op&(fsnotify.Create|fsnotify.Write) != 0 {
					if err := onFileChanged(); err != nil {
						return
					}
				}
			case <-watcher.Errors:
				// Keep running; later events may still succeed.
			}
		}
	}()

	return nil
}

type lineFollowerState struct {
	path      string
	file      *os.File
	offset    int64
	remainder []byte
	onLine    LineHandler
}

func (s *lineFollowerState) syncCurrentFile() error {
	if err := s.ensureOpen(); err != nil {
		if errors.Is(err, os.ErrNotExist) {
			s.close()
			return nil
		}
		return err
	}

	info, err := s.file.Stat()
	if err != nil {
		s.close()
		if errors.Is(err, os.ErrNotExist) {
			return nil
		}
		return err
	}
	if info.Size() < s.offset {
		s.offset = 0
		s.remainder = nil
	}

	if _, err := s.file.Seek(s.offset, io.SeekStart); err != nil {
		return err
	}

	buffer := make([]byte, 4096)
	for {
		n, err := s.file.Read(buffer)
		if n > 0 {
			s.offset += int64(n)
			s.consume(buffer[:n])
		}
		if errors.Is(err, io.EOF) {
			return nil
		}
		if err != nil {
			return err
		}
	}
}

func (s *lineFollowerState) ensureOpen() error {
	currentInfo, currentErr := os.Stat(s.path)
	if currentErr != nil {
		return currentErr
	}
	if s.file != nil {
		if openInfo, err := s.file.Stat(); err == nil && os.SameFile(openInfo, currentInfo) {
			return nil
		}
		s.close()
	}

	file, err := os.Open(s.path)
	if err != nil {
		return err
	}
	s.file = file
	s.offset = 0
	s.remainder = nil
	return nil
}

func (s *lineFollowerState) consume(chunk []byte) {
	s.remainder = append(s.remainder, chunk...)
	for {
		index := bytes.IndexByte(s.remainder, '\n')
		if index < 0 {
			return
		}
		line := string(bytes.TrimRight(s.remainder[:index], "\r"))
		s.remainder = s.remainder[index+1:]
		s.onLine(line)
	}
}

func (s *lineFollowerState) close() {
	if s.file != nil {
		_ = s.file.Close()
		s.file = nil
	}
	s.offset = 0
	s.remainder = nil
}
