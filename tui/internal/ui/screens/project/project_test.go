package project

import (
	"regexp"
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var csi = regexp.MustCompile("\\x1b\\[[0-9;]*[A-Za-z]")

func plain(s string) string { return csi.ReplaceAllString(zone.Scan(s), "") }

func testDeps() deps.Deps {
	return deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode)}
}

const testKey = "TIS"

func loaded(t *testing.T, w, h int, page domain.IssuePage) Model {
	t.Helper()
	zone.NewGlobal()
	m := New(testDeps(), testKey, "Tissue")
	m, _ = m.Update(tea.WindowSizeMsg{Width: w, Height: h})
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, page: page})
	return m
}

func issues(n int) []domain.IssueSummary {
	out := make([]domain.IssueSummary, n)
	for i := range out {
		out[i] = domain.IssueSummary{Key: "TIS-" + string(rune('1'+i%9)), Title: "Issue", StateCategory: "ACTIVE"}
	}
	return out
}

func press(s string) tea.KeyPressMsg {
	if len(s) == 1 {
		return tea.KeyPressMsg{Code: rune(s[0]), Text: s}
	}
	return tea.KeyPressMsg{Text: s}
}

func TestIssuesLoadedPopulates(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(3), TotalElements: 3})
	if m.loading {
		t.Error("still loading after issuesLoadedMsg")
	}
	if len(m.issues) != 3 || m.cursor != 0 {
		t.Errorf("issues=%d cursor=%d, want 3/0", len(m.issues), m.cursor)
	}
}

func TestLoadError(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps(), testKey, "Tissue")
	m, _ = m.Update(tea.WindowSizeMsg{Width: 90, Height: 20})
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, err: true})
	if !m.loadErr {
		t.Fatal("loadErr not set")
	}
	if !strings.Contains(plain(m.View()), "Failed to load issues") {
		t.Error("view does not surface the load error")
	}
}

// Moving past the last loaded row with more pages available requests the next page (appended).
func TestMoveLoadsNextPage(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(3), TotalElements: 10, HasNext: true, Page: 0})
	m.cursor = 2 // last loaded
	m, cmd := m.moveCursor(1)
	if !m.loadingMore {
		t.Error("moving past the end with HasNext did not start loading more")
	}
	if cmd == nil {
		t.Error("expected a next-page load command")
	}
}

// Without a next page, moving past the end just clamps and issues no command.
func TestMoveAtEndNoMore(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(3), TotalElements: 3, HasNext: false})
	m.cursor = 2
	m, cmd := m.moveCursor(1)
	if m.loadingMore || cmd != nil {
		t.Error("moving past the end without HasNext should not load")
	}
	if m.cursor != 2 {
		t.Errorf("cursor = %d, want clamped at 2", m.cursor)
	}
}

// Appending a page keeps existing issues and adds the new ones without resetting the cursor.
func TestAppendPage(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(3), TotalElements: 6, HasNext: true, Page: 0})
	m.cursor = 2
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, page: domain.IssuePage{Issues: issues(3), TotalElements: 6, HasNext: false, Page: 1}, append: true})
	if len(m.issues) != 6 {
		t.Errorf("issues = %d, want 6 after append", len(m.issues))
	}
	if m.cursor != 2 {
		t.Errorf("append reset the cursor to %d, want 2", m.cursor)
	}
}

// Esc (and q/backspace) request a return to the Projects tab.
func TestBackNavigation(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(2), TotalElements: 2})
	for _, k := range []string{"esc", "q"} {
		_, cmd := m.Update(press(k))
		if cmd == nil {
			t.Fatalf("%s produced no message", k)
		}
		if _, ok := cmd().(nav.CloseProjectMsg); !ok {
			t.Errorf("%s did not request a close (got %T)", k, cmd())
		}
	}
}

// 'r' reloads the first page.
func TestReload(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(2), TotalElements: 2})
	m, cmd := m.Update(press("r"))
	if !m.loading || cmd == nil {
		t.Error("r should re-enter the loading state and issue a load command")
	}
}

// A load result for a different project (a stale drill-in) is ignored, not applied to this model.
func TestStaleCrossProjectLoadIgnored(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(3), TotalElements: 3})
	m, _ = m.Update(issuesLoadedMsg{key: "OTHER", gen: m.reqGen, page: domain.IssuePage{Issues: issues(9), TotalElements: 9}})
	if len(m.issues) != 3 {
		t.Errorf("a load for a different project was applied: issues=%d, want 3 unchanged", len(m.issues))
	}
}

// A reload (r) supersedes an in-flight load: the older generation's late result is dropped.
func TestReloadSupersedesInFlight(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(3), TotalElements: 6, HasNext: true, Page: 0})
	staleGen := m.reqGen
	m, _ = m.Update(press("r")) // bumps reqGen, starts a fresh load
	// the earlier load-more/page finally lands with the OLD generation — must be ignored
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: staleGen, page: domain.IssuePage{Issues: issues(3), Page: 1}, append: true})
	if len(m.issues) != 3 || m.loading != true {
		t.Errorf("a superseded (old-gen) result was applied: issues=%d loading=%v", len(m.issues), m.loading)
	}
}

// A failed load-more keeps the already-loaded pages instead of blanking the whole list.
func TestLoadMoreErrorKeepsList(t *testing.T) {
	m := loaded(t, 90, 20, domain.IssuePage{Issues: issues(3), TotalElements: 6, HasNext: true, Page: 0})
	m.loadingMore = true
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, append: true, err: true})
	if m.loadErr {
		t.Error("a load-more failure set loadErr (blanks the list); it should not")
	}
	if len(m.issues) != 3 {
		t.Errorf("a load-more failure dropped the loaded issues: %d, want 3", len(m.issues))
	}
	if m.loadingMore {
		t.Error("loadingMore not cleared after a failed load-more")
	}
}

// A list longer than the panel windows to the selected row and never overflows the height budget.
func TestListWindowsToSelection(t *testing.T) {
	const h = 14
	m := loaded(t, 90, h, domain.IssuePage{Issues: issues(50), TotalElements: 50})
	m.cursor = 49
	out := plain(m.View())
	if got := len(strings.Split(out, "\n")); got > h {
		t.Errorf("view = %d rows, want <= %d (list overflowed)", got, h)
	}
	// the selected row registers a click zone this frame => it is inside the window
	for i := 0; i < 1000; i++ {
		if _ = zone.Scan(m.View()); zone.Get(issueRowZone(49)) != nil {
			break
		}
	}
	if zone.Get(issueRowZone(49)) == nil {
		t.Error("the selected (last) issue row is not rendered — it scrolled out of the window")
	}
}
