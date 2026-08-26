package project

import (
	"testing"

	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// mouseOffDeps mirrors "mouse = off": click affordances hidden, f/n drive the filter and create form.
func mouseOffDeps() deps.Deps {
	return deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: false}
}

func mouseOffModel(t *testing.T) Model {
	t.Helper()
	zone.NewGlobal()
	m := New(mouseOffDeps(), testKey, "Tissue")
	m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 24})
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, page: domain.IssuePage{Issues: issues(2), TotalElements: 2}})
	return m
}

// f opens the filter modal straight from the list, without tabbing onto the button.
func TestFilterOpensViaFKey(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{})
	if m.focus != focusList {
		t.Fatalf("precondition: expected list focus, got %v", m.focus)
	}
	m, _ = m.Update(press("f"))
	if !m.filtering {
		t.Fatal("f from the list should open the filter modal")
	}
}

// With the mouse off the filter button is not a Tab stop, so the ring skips it (list -> search -> list).
func TestMouseOffSkipsFilterFocus(t *testing.T) {
	m := mouseOffModel(t)
	if m.focusAvailable(focusFilter) {
		t.Error("the filter button should not be a Tab stop with the mouse off")
	}
	m, _ = m.Update(press("tab")) // list -> search
	if m.focus != focusSearch {
		t.Fatalf("first tab should focus search, got %v", m.focus)
	}
	m, _ = m.Update(press("tab")) // search -> (filter skipped) -> list
	if m.focus == focusFilter {
		t.Errorf("the Tab ring must skip the hidden filter button, landed on %v", m.focus)
	}
	// the f key still opens the filter even though its button is hidden
	m, _ = m.Update(press("f"))
	if !m.filtering {
		t.Error("f should still open the filter with the mouse off")
	}
}

// With the mouse off the search box reclaims the width the hidden buttons would have taken.
func TestMouseOffSearchBoxReclaimsButtonWidth(t *testing.T) {
	on := loaded(t, 120, 24, domain.IssuePage{Issues: issues(2), TotalElements: 2}) // Mouse: true via testDeps
	off := mouseOffModel(t)
	if off.trailingButtonsW() != 0 {
		t.Errorf("mouse-off trailing buttons width should be 0, got %d", off.trailingButtonsW())
	}
	if on.trailingButtonsW() == 0 {
		t.Error("mouse-on trailing buttons width should be non-zero")
	}
	if off.searchBoxWidth() <= on.searchBoxWidth() {
		t.Errorf("mouse-off search box (%d) should be wider than mouse-on (%d)", off.searchBoxWidth(), on.searchBoxWidth())
	}
}

// GlobalKeys carries the sub-tab switch always, reload only on the Issues tab.
func TestGlobalKeysTabAndReload(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	if !hasHelp(m.GlobalKeys(), "tab") {
		t.Error("GlobalKeys should advertise the 1-4 sub-tab switch")
	}
	if !hasHelp(m.GlobalKeys(), "reload") {
		t.Error("GlobalKeys on the Issues tab should advertise reload")
	}
	// the bottom (contextual) line must no longer carry the nav hints that moved up
	if hasHelp(m.HelpKeys(), "reload") {
		t.Error("HelpKeys should no longer carry reload (it moved to the top line)")
	}
	m, _ = m.Update(press("4")) // Config tab
	if hasHelp(m.GlobalKeys(), "reload") {
		t.Error("GlobalKeys off the Issues tab should drop reload")
	}
	if !hasHelp(m.GlobalKeys(), "tab") {
		t.Error("GlobalKeys should still advertise the sub-tab switch off the Issues tab")
	}
}

// onFilterKey ignores r at the filter focus, so the row must not advertise an inert key.
func TestGlobalKeysDropsReloadAtFilterFocus(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(press("tab")) // list -> search
	m, _ = m.Update(press("tab")) // search -> filter
	if m.focus != focusFilter {
		t.Fatalf("precondition: expected filter focus, got %v", m.focus)
	}
	if hasHelp(m.GlobalKeys(), "reload") {
		t.Error("GlobalKeys must not advertise reload while the filter button is focused (r is inert there)")
	}
	if hasKey(m.GlobalKeys(), "backspace") {
		t.Error("GlobalKeys must not advertise backspace while the filter button is focused (it is inert there)")
	}
	if !hasHelp(m.GlobalKeys(), "tab") {
		t.Error("GlobalKeys should still advertise the sub-tab switch at the filter focus")
	}
}

// The leave key lives on the top (global) line, so it must not stay on the bottom HelpKeys line.
func TestHelpKeysFilterAndBackspace(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	if !hasHelp(m.HelpKeys(), "filter") {
		t.Error("the list HelpKeys should advertise the f filter key")
	}
	if hasKey(m.HelpKeys(), "backspace") {
		t.Error("backspace moved to the top (global) line; it must not stay on the bottom HelpKeys line")
	}
	if !hasKey(m.GlobalKeys(), "backspace") {
		t.Error("GlobalKeys should advertise backspace as the leave key on the top line")
	}
}

func hasHelp(binds []key.Binding, desc string) bool {
	for _, b := range binds {
		if b.Help().Desc == desc {
			return true
		}
	}
	return false
}

func hasKey(binds []key.Binding, want string) bool {
	for _, b := range binds {
		for _, k := range b.Keys() {
			if k == want {
				return true
			}
		}
	}
	return false
}
