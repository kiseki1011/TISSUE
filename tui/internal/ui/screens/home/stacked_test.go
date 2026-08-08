package home

import (
	"regexp"
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var stackCSI = regexp.MustCompile("\\x1b\\[[0-9;]*[A-Za-z]")

func stackPlain(s string) string { return stackCSI.ReplaceAllString(zone.Scan(s), "") }

func stackModel(t *testing.T, w, h int) Model {
	t.Helper()
	zone.NewGlobal()
	m := New(deps.Deps{
		Server: "srv", Config: &config.Config{},
		Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Nerd),
	}, domain.SystemInfo{}, "")
	m, _ = m.Update(tea.WindowSizeMsg{Width: w, Height: h})
	m, _ = m.Update(projectsLoadedMsg{projects: []domain.Project{
		{Key: "ALPHA", Title: "Alpha", Visibility: "PUBLIC", MyRole: "MANAGER"},
		{Key: "BETA", Title: "Beta", Visibility: "PRIVATE"},
	}})
	return m
}

// A narrow-and-tall terminal stacks the Details above the project list and stays within the height
// budget (a regression guard: the list must window to its slice, not fill the whole height).
func TestHomeStackedLayout(t *testing.T) {
	m := stackModel(t, 95, 40)
	if !m.stacked() {
		t.Fatalf("expected stacked at 95x40 (sideFloor=%d, minWidth=%d)", m.sideFloor(), m.minWidth())
	}
	view := stackPlain(m.View())
	if got := len(strings.Split(view, "\n")); got != 40 {
		t.Errorf("stacked view = %d rows, want 40 (the list overflowed its slice)", got)
	}
	detailAt, listAt := strings.Index(view, "Details"), strings.Index(view, "Projects (")
	if detailAt < 0 || listAt < 0 {
		t.Fatalf("stacked view missing panes: details=%d projects=%d", detailAt, listAt)
	}
	if detailAt > listAt {
		t.Error("stacked layout should render Details above the project list")
	}
	// the list zone sits below the Details slice, and its click hit-testing is zone-relative, so a
	// click resolves to the right row even though the list moved down
	if z := zone.Get("home.list"); z == nil || z.IsZero() {
		t.Error("home.list zone did not register in the stacked layout")
	} else if z.StartY < m.stackDetailH()/2 {
		t.Errorf("home.list zone (StartY=%d) is not offset below the Details slice (~%d)", z.StartY, m.stackDetailH())
	}
}

// A wide terminal keeps the side-by-side layout.
func TestHomeSideWhenWide(t *testing.T) {
	m := stackModel(t, 140, 34)
	if m.stacked() {
		t.Errorf("140x34 should be side by side, not stacked (sideFloor=%d)", m.sideFloor())
	}
	if got := len(strings.Split(stackPlain(m.View()), "\n")); got != 34 {
		t.Errorf("side view = %d rows, want 34", got)
	}
}

// Stacking lowers the render floor: a width below the side-by-side floor still renders when the
// terminal is tall enough to stack.
func TestHomeStackingLowersFloor(t *testing.T) {
	m := stackModel(t, 95, 40)
	if m.minWidth() >= m.sideFloor() {
		t.Errorf("stacked minWidth (%d) should be below the side floor (%d)", m.minWidth(), m.sideFloor())
	}
	short := stackModel(t, 95, 20) // too short to stack: floor stays at the side value
	if short.minWidth() != short.sideFloor() {
		t.Errorf("short terminal minWidth (%d) should equal the side floor (%d)", short.minWidth(), short.sideFloor())
	}
}
