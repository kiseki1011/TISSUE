// Package auth stores credentials and injects them into API requests, keeping
// authentication out of the UI. The token store persists tokens per server and
// Transport refreshes them transparently on a 401.
package auth

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"

	"github.com/zalando/go-keyring"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

const keyringService = "tissue"

// TokenStore persists a server's tokens between runs.
type TokenStore interface {
	Load(server string) (domain.TokenPair, bool, error)
	Save(server string, tokens domain.TokenPair) error
	Clear(server string) error
}

// NewTokenStore returns an OS-keyring store, falling back to a 0600 file when
// no keyring is available (headless SSH, containers, CI).
func NewTokenStore() (TokenStore, error) {
	if keyringAvailable() {
		return keyringStore{}, nil
	}
	path, err := credentialsPath()
	if err != nil {
		return nil, err
	}
	return fileStore{path: path}, nil
}

func keyringAvailable() bool {
	// A working backend answers a probe with either a value or ErrNotFound.
	_, err := keyring.Get(keyringService, "__probe__")
	return err == nil || errors.Is(err, keyring.ErrNotFound)
}

func credentialsPath() (string, error) {
	dir, err := config.Dir()
	if err != nil {
		return "", err
	}
	return filepath.Join(dir, "credentials.json"), nil
}

type keyringStore struct{}

// Access and refresh tokens go in separate entries: their combined size can
// exceed the macOS keychain's per-item limit.
func accessKey(server string) string  { return server + " (access)" }
func refreshKey(server string) string { return server + " (refresh)" }

func (keyringStore) Load(server string) (domain.TokenPair, bool, error) {
	access, err := keyring.Get(keyringService, accessKey(server))
	if errors.Is(err, keyring.ErrNotFound) {
		return domain.TokenPair{}, false, nil
	}
	if err != nil {
		return domain.TokenPair{}, false, fmt.Errorf("read access token: %w", err)
	}
	refresh, err := keyring.Get(keyringService, refreshKey(server))
	if err != nil {
		return domain.TokenPair{}, false, fmt.Errorf("read refresh token: %w", err)
	}
	return domain.TokenPair{Access: access, Refresh: refresh}, true, nil
}

func (keyringStore) Save(server string, tokens domain.TokenPair) error {
	if err := keyring.Set(keyringService, accessKey(server), tokens.Access); err != nil {
		return fmt.Errorf("save access token: %w", err)
	}
	if err := keyring.Set(keyringService, refreshKey(server), tokens.Refresh); err != nil {
		return fmt.Errorf("save refresh token: %w", err)
	}
	return nil
}

func (keyringStore) Clear(server string) error {
	for _, key := range []string{accessKey(server), refreshKey(server)} {
		err := keyring.Delete(keyringService, key)
		if err != nil && !errors.Is(err, keyring.ErrNotFound) {
			return fmt.Errorf("clear token: %w", err)
		}
	}
	return nil
}

type fileStore struct {
	path string
}

func (s fileStore) Load(server string) (domain.TokenPair, bool, error) {
	all, err := s.readAll()
	if err != nil {
		return domain.TokenPair{}, false, err
	}
	tokens, ok := all[server]
	return tokens, ok, nil
}

func (s fileStore) Save(server string, tokens domain.TokenPair) error {
	all, err := s.readAll()
	if err != nil {
		return err
	}
	all[server] = tokens
	return s.writeAll(all)
}

func (s fileStore) Clear(server string) error {
	all, err := s.readAll()
	if err != nil {
		return err
	}
	if _, ok := all[server]; !ok {
		return nil
	}
	delete(all, server)
	return s.writeAll(all)
}

func (s fileStore) readAll() (map[string]domain.TokenPair, error) {
	data, err := os.ReadFile(s.path)
	if errors.Is(err, fs.ErrNotExist) {
		return map[string]domain.TokenPair{}, nil
	}
	if err != nil {
		return nil, fmt.Errorf("read credentials: %w", err)
	}
	all := map[string]domain.TokenPair{}
	if err := json.Unmarshal(data, &all); err != nil {
		return nil, fmt.Errorf("parse credentials: %w", err)
	}
	return all, nil
}

func (s fileStore) writeAll(all map[string]domain.TokenPair) error {
	data, err := json.MarshalIndent(all, "", "  ")
	if err != nil {
		return fmt.Errorf("encode credentials: %w", err)
	}
	if err := os.MkdirAll(filepath.Dir(s.path), 0o700); err != nil {
		return fmt.Errorf("create credentials dir: %w", err)
	}
	tmp := s.path + ".tmp"
	if err := os.WriteFile(tmp, data, 0o600); err != nil {
		return fmt.Errorf("write credentials: %w", err)
	}
	if err := os.Rename(tmp, s.path); err != nil {
		return fmt.Errorf("replace credentials: %w", err)
	}
	return nil
}
