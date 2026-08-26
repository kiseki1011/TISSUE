package agents

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// Mouse off hides the click-only affordances (+ New, the Edit pen) but keeps the list and detail.
func TestMouseOffHidesButtons(t *testing.T) {
	build := func(mouse bool) Model {
		zone.NewGlobal()
		d := testDeps()
		d.Mouse = mouse
		m := New(d)
		m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 20})
		m, _ = m.Update(AgentsLoadedMsg{Agents: []domain.Agent{{ID: 1, Name: "Build Bot", Username: "bot"}}})
		return m
	}

	on := plain(build(true).View())
	for _, s := range []string{"New agent", "New token", "Edit"} {
		if !strings.Contains(on, s) {
			t.Errorf("mouse-on view should show the %q affordance", s)
		}
	}

	off := plain(build(false).View())
	for _, s := range []string{"New agent", "New token", "Edit"} {
		if strings.Contains(off, s) {
			t.Errorf("mouse-off view should hide the %q affordance:\n%s", s, off)
		}
	}
	if !strings.Contains(off, "Build Bot") {
		t.Errorf("mouse-off view dropped the agent list:\n%s", off)
	}
}
