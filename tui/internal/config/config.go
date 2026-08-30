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

	// Icons selects the glyph set: "auto", "nerd", or "unicode" (see glyph.ParseMode).
	Icons string `json:"icons"`

	// Mouse toggles mouse capture (click + hover). "" or "on" enables it, "off" disables it.
	Mouse string `json:"mouse,omitempty"`

	// Pinned maps a server URL to its pinned project keys, in pin order.
	Pinned map[string][]string `json:"pinned,omitempty"`

	// LastProject maps a server URL to the project open when the app last closed. An absent entry
	// means the dashboard.
	LastProject map[string]string `json:"last_project,omitempty"`

	// ProjectFilters maps a server URL to its per-project saved issue filters, restored on re-open.
	ProjectFilters map[string]map[string]FilterState `json:"project_filters,omitempty"`

	path string
}

// FilterState is a project's persisted issue filter. The transient search keyword is deliberately not
// stored. An entry's presence is the "user set a filter" signal, so an all-empty state (show
// everything) differs from no entry (the default open-issues view).
type FilterState struct {
	StateCategories   []string `json:"stateCategories,omitempty"`
	Priorities        []string `json:"priorities,omitempty"`
	IssueTypeIDs      []int64  `json:"issueTypeIds,omitempty"`
	SprintIDs         []int64  `json:"sprintIds,omitempty"`
	CurrentSprintOnly bool     `json:"currentSprintOnly,omitempty"`
	AssigneeMe        bool     `json:"assigneeMe,omitempty"`
	AuthorMe          bool     `json:"authorMe,omitempty"`
	ReviewerMe        bool     `json:"reviewerMe,omitempty"`
	ReviewerStatuses  []string `json:"reviewerStatuses,omitempty"`
}

func Dir() (string, error) {
	base, err := os.UserConfigDir()
	if err != nil {
		return "", fmt.Errorf("locate config dir: %w", err)
	}
	dir := filepath.Join(base, appName)
	if err := os.MkdirAll(dir, 0o700); err != nil {
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

// LastProjectFor returns the saved project key, or "" when none is remembered.
func (c *Config) LastProjectFor(server string) string {
	return c.LastProject[server]
}

// SetLastProject remembers the last-open project (an empty key forgets it). Re-setting the current
// value skips the disk write.
func (c *Config) SetLastProject(server, key string) error {
	if key == "" {
		if _, ok := c.LastProject[server]; !ok {
			return nil
		}
		delete(c.LastProject, server)
		return c.Save()
	}
	if c.LastProject[server] == key {
		return nil
	}
	if c.LastProject == nil {
		c.LastProject = map[string]string{}
	}
	c.LastProject[server] = key
	return c.Save()
}

func (c *Config) ProjectFilter(server, key string) (FilterState, bool) {
	m, ok := c.ProjectFilters[server]
	if !ok {
		return FilterState{}, false
	}
	f, ok := m[key]
	return f, ok
}

// SetProjectFilter stores a project's filter. It writes unconditionally: filtering is rare enough that
// diffing the slice fields is not worth it.
func (c *Config) SetProjectFilter(server, key string, f FilterState) error {
	if c.ProjectFilters == nil {
		c.ProjectFilters = map[string]map[string]FilterState{}
	}
	if c.ProjectFilters[server] == nil {
		c.ProjectFilters[server] = map[string]FilterState{}
	}
	c.ProjectFilters[server][key] = f
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
