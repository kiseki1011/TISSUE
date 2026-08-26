package home

import (
	"errors"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func keyPress(s string) tea.KeyPressMsg {
	if len(s) == 1 {
		return tea.KeyPressMsg{Code: rune(s[0]), Text: s}
	}
	return tea.KeyPressMsg{Text: s}
}

func homeModel(t *testing.T, mouse bool) Model {
	t.Helper()
	zone.NewGlobal()
	m := New(deps.Deps{
		Server: "srv",
		Config: &config.Config{},
		Styles: theme.New(theme.TokyoNight()),
		Glyphs: glyph.New(glyph.Nerd),
		Mouse:  mouse,
	}, domain.SystemInfo{}, "")
	m, _ = m.Update(tea.WindowSizeMsg{Width: 140, Height: 24})
	m, _ = m.Update(ProjectsLoadedMsg{projects: []domain.Project{{Key: "ALPHA", Title: "Alpha", Visibility: "PUBLIC"}}})
	return m
}

// RefreshMsg reloads silently — the list stays visible while the title flags "(loading)".
func TestHomeRefreshMsgReloads(t *testing.T) {
	m := homeModel(t, true)
	m, cmd := m.Update(RefreshMsg{})
	if !m.loading {
		t.Error("RefreshMsg should start a reload")
	}
	if cmd == nil {
		t.Error("RefreshMsg should fetch the projects list")
	}
	if len(m.projects) == 0 {
		t.Error("the list should stay visible during the reload")
	}
}

// A failed refresh keeps the showing list rather than replacing the dashboard over a transient blip.
func TestHomeRefreshFailureKeepsList(t *testing.T) {
	m := homeModel(t, true) // has landed a projects list, so m.loaded is true
	m, _ = m.Update(RefreshMsg{})
	m, _ = m.Update(ProjectsErrMsg{err: errors.New("blip")})
	if m.err != nil {
		t.Error("a refresh failure with a loaded list should not raise the dashboard error")
	}
	if len(m.projects) == 0 {
		t.Error("the list should be preserved on a refresh failure")
	}
}

// An INITIAL load failure does surface the error, so the empty dashboard explains itself.
func TestHomeInitialLoadFailureShowsError(t *testing.T) {
	m := New(deps.Deps{
		Server: "srv", Config: &config.Config{},
		Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Nerd),
	}, domain.SystemInfo{}, "")
	m, _ = m.Update(ProjectsErrMsg{err: errors.New("down")})
	if m.err == nil {
		t.Error("an initial load failure should surface the error")
	}
}

func TestHomeRefreshKey(t *testing.T) {
	m := homeModel(t, true)
	m.focus = focusList
	m, cmd := m.Update(keyPress("R"))
	if !m.loading || cmd == nil {
		t.Errorf("R should refresh the list: loading=%v cmd=%v", m.loading, cmd != nil)
	}
}

func TestHomeFilterOpensViaFKey(t *testing.T) {
	m := homeModel(t, true)
	m.focus = focusList
	m, _ = m.Update(keyPress("f"))
	if !m.filtering {
		t.Fatal("f from the list should open the filter modal")
	}
}

// With the mouse off, the filter/create buttons are not Tab stops and take no width in the search row.
func TestHomeMouseOffHidesButtons(t *testing.T) {
	m := homeModel(t, false)
	if m.focusAvailable(focusFilter) || m.focusAvailable(focusPlus) {
		t.Error("filter/create buttons should not be Tab stops with the mouse off")
	}
	if m.trailingButtonsW() != 0 {
		t.Errorf("mouse-off trailing buttons width should be 0, got %d", m.trailingButtonsW())
	}
	// the f key still opens the filter even though its button is hidden
	m.focus = focusList
	m, _ = m.Update(keyPress("f"))
	if !m.filtering {
		t.Error("f should still open the filter with the mouse off")
	}
}
