package ui

import (
	"fmt"
	"strings"
	"testing"

	"charm.land/bubbles/v2/help"
	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/agents"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/schema"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func keyPress(s string) tea.KeyPressMsg { return tea.KeyPressMsg{Text: s} }

// helpApp builds an App parked on the Schema tab, sized, ready to receive input.
func helpApp() App {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://localhost:8080"}
	a := New(d)
	a.screen = screenSchema
	a.schema = schema.New(d)
	a.width, a.height = 100, 30
	return a
}

// ? opens the app-level help modal, and it carries the active screen's title, an about blurb, and
// both the global and screen key sections drawn from the live bindings.
func TestQuestionMarkOpensHelp(t *testing.T) {
	a := helpApp()
	m, _ := a.Update(keyPress("?"))
	app := m.(App)
	if app.modal == nil {
		t.Fatal("? did not open the help modal")
	}
	view := stripCSI(app.modal.View())
	for _, want := range []string{"Schema", "Global", "quit"} {
		if !strings.Contains(view, want) {
			t.Errorf("help modal is missing %q:\n%s", want, view)
		}
	}
}

// esc closes the modal (via the modalClosedMsg it emits), returning the shell to the plain screen.
func TestHelpEscCloses(t *testing.T) {
	a := helpApp()
	m, _ := a.Update(keyPress("?"))
	m, cmd := m.(App).Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if cmd == nil {
		t.Fatal("esc did not emit a close command")
	}
	m, _ = m.(App).Update(cmd()) // deliver the modalClosedMsg
	if m.(App).modal != nil {
		t.Error("esc did not close the help modal")
	}
}

// A left click outside the modal box dismisses it; a click inside leaves it open.
func TestHelpClickOutsideCloses(t *testing.T) {
	a := helpApp()
	m, _ := a.Update(keyPress("?"))
	app := m.(App)

	inside := tea.MouseClickMsg{X: app.width / 2, Y: app.height / 2, Button: tea.MouseLeft}
	m, _ = app.Update(inside)
	if m.(App).modal == nil {
		t.Fatal("a click inside the modal wrongly closed it")
	}
	outside := tea.MouseClickMsg{X: 0, Y: 0, Button: tea.MouseLeft}
	m, _ = m.(App).Update(outside)
	if m.(App).modal != nil {
		t.Error("a click outside the modal did not close it")
	}
}

// While the help modal is open it owns the keyboard, so a tab-switch digit does not change tabs.
func TestModalSuppressesTabSwitch(t *testing.T) {
	a := helpApp()
	m, _ := a.Update(keyPress("?"))
	m, _ = m.(App).Update(keyPress("1")) // would switch to Projects if not captured
	app := m.(App)
	if app.screen != screenSchema {
		t.Errorf("a tab digit switched screens while the modal was open: %v", app.screen)
	}
	if app.modal == nil {
		t.Error("the digit closed the modal instead of being swallowed")
	}
}

// Content taller than the terminal scrolls: the viewport reports scrollable and a down key advances
// the offset.
func TestHelpModalScrolls(t *testing.T) {
	keys := make([]key.Binding, 0, 30)
	for i := 0; i < 30; i++ {
		keys = append(keys, key.NewBinding(key.WithKeys("k"), key.WithHelp("k", fmt.Sprintf("action-%d", i))))
	}
	m := newHelpModal(theme.TokyoNight(), help.New(), "Test", "about", nil, keys, 80, 12)
	if !m.scroll {
		t.Fatal("tall content did not enable scrolling")
	}
	before := m.vp.YOffset()
	next, _ := m.Update(tea.KeyPressMsg{Code: tea.KeyDown})
	nm := next.(helpModal)
	if got := nm.vp.YOffset(); got <= before {
		t.Errorf("down did not scroll the viewport: offset %d -> %d", before, got)
	}
}

// The Agents tab titles its help "Agents · Help" - it satisfies describer like Projects and Schema, so it
// no longer falls back to the "Help · Help" default.
func TestAgentsHelpTitle(t *testing.T) {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://localhost:8080"}
	a := New(d)
	a.screen = screenAgents
	a.agents = agents.New(d)
	a.width, a.height = 100, 30

	m, _ := a.Update(keyPress("?"))
	app := m.(App)
	if app.modal == nil {
		t.Fatal("? did not open the help modal on the Agents tab")
	}
	view := stripCSI(app.modal.View())
	if !strings.Contains(view, "Agents") {
		t.Errorf("agents help modal title is not \"Agents\":\n%s", view)
	}
	if strings.Contains(view, "Help · Help") {
		t.Errorf("agents help modal fell back to the default title:\n%s", view)
	}
}
