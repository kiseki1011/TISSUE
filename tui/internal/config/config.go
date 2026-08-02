// Package config loads and persists app state as JSON in the OS config directory
package config

import (
	"encoding/json"
	"errors"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
)

const appName = "tissue"

type Config struct {
	// ServerURL is the last server connected to, reused when tissue runs without `-c`.
	ServerURL string `json:"server_url"`

	// Theme is the color palette name (see theme.Names)
	Theme string `json:"theme"`

	// Icons selects the glyph set (see glyph.ParseMode)
	// - "auto"
	// - "nerd"
	// - "unicode"
	Icons string `json:"icons"`

	// Pinned maps a server URL to its pinned project keys, in pin order.
	Pinned map[string][]string `json:"pinned,omitempty"`

	path string
}

func Dir() (string, error) {
	base, err := os.UserConfigDir()
	if err != nil {
		return "", fmt.Errorf("locate config dir: %w", err)
	}
	dir := filepath.Join(base, appName)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return "", fmt.Errorf("create config dir %s: %w", dir, err)
	}
	return dir, nil
}

// Load reads config.json, returning defaults when the file does not exist.
func Load() (*Config, error) {
	dir, err := Dir()
	if err != nil {
		return nil, err
	}
	path := filepath.Join(dir, "config.json")

	cfg := &Config{path: path}
	data, err := os.ReadFile(path)
	if errors.Is(err, fs.ErrNotExist) {
		return cfg, nil
	}
	if err != nil {
		return nil, fmt.Errorf("read config %s: %w", path, err)
	}
	if err := json.Unmarshal(data, cfg); err != nil {
		return nil, fmt.Errorf("parse config %s: %w", path, err)
	}
	return cfg, nil
}

func (c *Config) SetServer(server string) error {
	c.ServerURL = server
	return c.Save()
}

// PinnedProjects returns the pinned project keys for a server, in pin order.
func (c *Config) PinnedProjects(server string) []string {
	return c.Pinned[server]
}

func (c *Config) IsPinned(server, key string) bool {
	for _, k := range c.Pinned[server] {
		if k == key {
			return true
		}
	}
	return false
}

func (c *Config) TogglePin(server, key string) error {
	if c.Pinned == nil {
		c.Pinned = map[string][]string{}
	}
	if c.IsPinned(server, key) {
		kept := make([]string, 0, len(c.Pinned[server]))
		for _, k := range c.Pinned[server] {
			if k != key {
				kept = append(kept, k)
			}
		}
		c.Pinned[server] = kept
	} else {
		c.Pinned[server] = append(c.Pinned[server], key)
	}
	return c.Save()
}

func (c *Config) Save() error {
	if c.path == "" {
		dir, err := Dir()
		if err != nil {
			return err
		}
		c.path = filepath.Join(dir, "config.json")
	}

	data, err := json.MarshalIndent(c, "", "  ")
	if err != nil {
		return fmt.Errorf("encode config: %w", err)
	}

	tmp := c.path + ".tmp"
	if err := os.WriteFile(tmp, data, 0o600); err != nil {
		return fmt.Errorf("write config %s: %w", tmp, err)
	}
	if err := os.Rename(tmp, c.path); err != nil {
		return fmt.Errorf("replace config %s: %w", c.path, err)
	}
	return nil
}
