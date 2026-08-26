package project

import (
	"reflect"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const filterServer = "http://srv"

// filterDeps points config at a temp dir, so tests never touch the user's real config.
func filterDeps(t *testing.T) (deps.Deps, *config.Config) {
	t.Helper()
	tmp := t.TempDir()
	t.Setenv("HOME", tmp)
	t.Setenv("XDG_CONFIG_HOME", tmp)
	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("load config: %v", err)
	}
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: filterServer, Config: cfg}
	return d, cfg
}

func TestNewLoadsSavedFilter(t *testing.T) {
	d, cfg := filterDeps(t)
	if err := cfg.SetProjectFilter(filterServer, "ENG", config.FilterState{Priorities: []string{"P0"}, AssigneeMe: true}); err != nil {
		t.Fatalf("seed: %v", err)
	}
	m := New(d, "ENG", "")
	if len(m.filter.StateCategories) != 0 {
		t.Errorf("a saved filter should replace the default states, got %v", m.filter.StateCategories)
	}
	if len(m.filter.Priorities) != 1 || m.filter.Priorities[0] != "P0" || !m.filter.AssigneeMe {
		t.Errorf("saved filter not restored: %+v", m.filter)
	}
}

func TestNewDefaultsWhenNoSavedFilter(t *testing.T) {
	d, _ := filterDeps(t)
	m := New(d, "ENG", "")
	if !reflect.DeepEqual(m.filter.StateCategories, domain.OpenIssuesFilter().StateCategories) {
		t.Errorf("without a saved filter New should use the default, got %v", m.filter.StateCategories)
	}
}

func TestNewNilConfigDefaults(t *testing.T) {
	m := New(testDeps(), "ENG", "")
	if !reflect.DeepEqual(m.filter.StateCategories, domain.OpenIssuesFilter().StateCategories) {
		t.Errorf("a nil config must fall back to the default filter, got %v", m.filter.StateCategories)
	}
}

func TestApplyFilterPersists(t *testing.T) {
	d, _ := filterDeps(t)
	m := New(d, "ENG", "")
	m.filtering = true // the filter modal is open when it emits filterAppliedMsg
	m, _ = m.Update(filterAppliedMsg{
		states: []string{"COMPLETED"}, priorities: []string{"P0", "P1"}, typeIDs: []int64{5}, assigneeMe: true,
	})

	reloaded, err := config.Load()
	if err != nil {
		t.Fatalf("reload: %v", err)
	}
	fs, ok := reloaded.ProjectFilter(filterServer, "ENG")
	if !ok {
		t.Fatal("applying a filter should persist it")
	}
	if !reflect.DeepEqual(fs.StateCategories, []string{"COMPLETED"}) ||
		!reflect.DeepEqual(fs.Priorities, []string{"P0", "P1"}) ||
		!reflect.DeepEqual(fs.IssueTypeIDs, []int64{5}) || !fs.AssigneeMe {
		t.Errorf("persisted filter mismatch: %+v", fs)
	}

	m2 := New(d, "ENG", "")
	if !reflect.DeepEqual(m2.filter.StateCategories, []string{"COMPLETED"}) || !m2.filter.AssigneeMe {
		t.Errorf("re-opened project did not restore the applied filter: %+v", m2.filter)
	}
}

func TestApplyFilterExcludesKeyword(t *testing.T) {
	d, _ := filterDeps(t)
	m := New(d, "ENG", "")
	m.filter.Keyword = "urgent" // as if typed into the search box
	m.filtering = true
	m, _ = m.Update(filterAppliedMsg{states: []string{"ACTIVE"}})

	m2 := New(d, "ENG", "")
	if m2.filter.Keyword != "" {
		t.Errorf("the search keyword must not persist, got %q", m2.filter.Keyword)
	}
}
