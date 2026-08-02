package home

import (
	"testing"

	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// The bubbles table re-renders eagerly on every SetColumns/SetRows/SetWidth call, and
// renderRow indexes cols[i] while ranging a row's cells. So a table row must never hold
// more cells than there are columns, or it panics with "index out of range". Hiding the
// Repository column on narrow terminals changes the column count, which makes this easy
// to violate across a resize; these tests guard both crash paths we hit.

func testModel(t *testing.T) Model {
	t.Helper()
	m := New(deps.Deps{
		Server: "srv",
		Config: &config.Config{},
		Styles: theme.New(theme.TokyoNight()),
		Glyphs: glyph.New(glyph.Nerd),
	}, domain.SystemInfo{}, "")
	m.loading = false
	m.projects = []domain.Project{
		{Key: "ALPHA", Title: "Alpha", Visibility: "PUBLIC", MyRole: "MEMBER"},
		{Key: "BETA", Title: "Beta", Visibility: "PRIVATE", Archived: true},
		{Key: "GAMMA", Title: "Gamma", Visibility: "PUBLIC", MyRole: "MANAGER"},
	}
	return m
}

func assertRowsMatchCols(t *testing.T, m Model, tag string) {
	t.Helper()
	n := len(m.table.Columns())
	for i, r := range m.table.Rows() {
		if len(r) != n {
			t.Fatalf("%s: row %d has %d cells but there are %d columns", tag, i, len(r), n)
		}
	}
}

// TestMinWidthFloor pins the smallest width that renders the dashboard: nerd mode
// floors at the narrower five-column layout (Repository is hidden that low), while
// fallback headers pack wider and hold at the six-column floor.
func TestMinWidthFloor(t *testing.T) {
	if got := testModel(t).minWidth(); got != 110 {
		t.Fatalf("nerd minWidth = %d, want 110", got)
	}
	unicode := New(deps.Deps{
		Server: "srv",
		Config: &config.Config{},
		Styles: theme.New(theme.TokyoNight()),
		Glyphs: glyph.New(glyph.Unicode),
	}, domain.SystemInfo{}, "")
	if got := unicode.minWidth(); got != 137 {
		t.Fatalf("unicode minWidth = %d, want 137", got)
	}
}

// TestRelayoutResizeAcrossRepoThreshold covers the crash from shrinking the terminal
// across the Repository threshold: relayout swapped columns to 5 while the table still
// held 6-cell rows. It also checks the grow direction and cursor preservation.
func TestRelayoutResizeAcrossRepoThreshold(t *testing.T) {
	zone.NewGlobal()
	m := testModel(t)

	m.width, m.height = 140, 20 // repo shown -> 6 cols
	m.relayout()
	if got := len(m.table.Columns()); got != 6 {
		t.Fatalf("wide: want 6 cols, got %d", got)
	}
	assertRowsMatchCols(t, m, "wide")

	m.table.SetCursor(2) // a non-first row, to verify it survives the swap

	m.width, m.height = 125, 20 // shrink below threshold -> repo hidden, 5 cols (crash site)
	m.relayout()
	if got := len(m.table.Columns()); got != 5 {
		t.Fatalf("narrow: want 5 cols, got %d", got)
	}
	assertRowsMatchCols(t, m, "narrow")
	if got := m.table.Cursor(); got != 2 {
		t.Fatalf("narrow: cursor should stay 2, got %d", got)
	}

	m.width, m.height = 160, 20 // grow back above threshold -> repo shown, 6 cols
	m.relayout()
	if got := len(m.table.Columns()); got != 6 {
		t.Fatalf("grow: want 6 cols, got %d", got)
	}
	assertRowsMatchCols(t, m, "grow")
	if got := m.table.Cursor(); got != 2 {
		t.Fatalf("grow: cursor should stay 2, got %d", got)
	}

	_ = zone.Scan(m.dashboard()) // full render must not panic
}

// TestRebuildRowsMatchesStaleColumns covers the crash where a resize to a too-short
// terminal makes relayout early-return, so the width crosses the Repository threshold
// while the columns stay put; a later rebuildRows (projectsLoaded, key nav, etc.) must
// build rows matching the installed columns, not showRepo(width).
func TestRebuildRowsMatchesStaleColumns(t *testing.T) {
	zone.NewGlobal()
	m := testModel(t)

	m.width, m.height = 125, 20 // repo hidden -> 5 cols
	m.relayout()
	if got := len(m.table.Columns()); got != 5 {
		t.Fatalf("want 5 cols, got %d", got)
	}

	// Resize wide but too short: relayout early-returns (height below minimum), so the
	// columns stay at 5 while showRepo(width) would now be true.
	m.width, m.height = 160, 8
	m.relayout()
	if got := len(m.table.Columns()); got != 5 {
		t.Fatalf("columns should stay stale at 5, got %d", got)
	}
	if !m.showRepo() {
		t.Fatalf("width 160 should make showRepo(width) true (the trap)")
	}

	m.rebuildRows() // must not build 6-cell rows into the 5-column table
	assertRowsMatchCols(t, m, "stale-columns")
}
