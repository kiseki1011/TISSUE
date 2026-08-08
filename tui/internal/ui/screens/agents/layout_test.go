package agents

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// layout resolves from the terminal size: side by side when wide, stacked when narrow and tall.
func TestLayoutSelection(t *testing.T) {
	base := New(testDeps())
	cases := []struct {
		w, h int
		want layoutKind
	}{
		{120, 24, layoutSide},
		{70, 30, layoutStacked},
		{70, 14, layoutSide}, // too short to stack, stays side by side
	}
	for _, c := range cases {
		m := base
		m.width, m.height = c.w, c.h
		if got := m.layout(); got != c.want {
			t.Errorf("%dx%d: layout = %v, want %v", c.w, c.h, got, c.want)
		}
	}
}

// The stacked arrangement renders the detail pane above the agents list.
func TestStackedLayoutPutsDetailAboveList(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps())
	m, _ = m.Update(tea.WindowSizeMsg{Width: 70, Height: 30})
	m, _ = m.Update(AgentsLoadedMsg{Agents: []domain.Agent{{ID: 1, Name: "Build Bot", Username: "agent-build"}}})
	if m.layout() != layoutStacked {
		t.Fatalf("expected stacked layout at 70x30, got %v", m.layout())
	}
	view := plain(m.View())
	detailAt, listAt := strings.Index(view, "Details"), strings.Index(view, "Agents (")
	if detailAt < 0 || listAt < 0 {
		t.Fatalf("view missing panes: details=%d agents=%d", detailAt, listAt)
	}
	if detailAt > listAt {
		t.Error("stacked layout should render Details above the Agents list")
	}
}
