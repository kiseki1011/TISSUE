package home

import (
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// On first open the window sizes before the project list arrives, so the initial relayout runs
// SetRows on an empty list and the table cursor lands at -1. Once projects load, the top row (the
// topmost pinned project, or the topmost project when none are pinned) must be selected by default.
func TestInitialLoadSelectsTopRow(t *testing.T) {
	zone.NewGlobal()
	m := New(deps.Deps{
		Server: "srv",
		Config: &config.Config{},
		Styles: theme.New(theme.TokyoNight()),
		Glyphs: glyph.New(glyph.Nerd),
	}, domain.SystemInfo{}, "")

	// window sizes first (before the list loads), mirroring the real startup order
	m, _ = m.Update(tea.WindowSizeMsg{Width: 140, Height: 20})
	m, _ = m.Update(ProjectsLoadedMsg{projects: []domain.Project{
		{Key: "ALPHA", Title: "Alpha", Visibility: "PUBLIC"},
		{Key: "BETA", Title: "Beta", Visibility: "PUBLIC"},
	}})

	if got := m.table.Cursor(); got != 0 {
		t.Fatalf("cursor after initial load = %d, want 0", got)
	}
	p, ok := m.selectedProject()
	if !ok {
		t.Fatal("no project selected after initial load")
	}
	if p.Key != "ALPHA" {
		t.Errorf("selected %q, want the top row ALPHA", p.Key)
	}
}
