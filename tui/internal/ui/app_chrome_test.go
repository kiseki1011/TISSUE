package ui

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/project"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func chromeApp(w, h int) App {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://localhost:8080", Mouse: true}
	a := New(d)
	a.screen = screenProject
	a.project = project.New(d, "ENG", "Engineering")
	a.user = domain.Profile{Username: "admin"}
	m, _ := a.Update(tea.WindowSizeMsg{Width: w, Height: h})
	return m.(App)
}

// #8: a wide header shows the server url, the username, and the drill-in's back link; a narrow one
// (width <= headerCompactW) drops all three but keeps the brand and the tabs.
func TestHeaderCompactDropsUsernameAndBack(t *testing.T) {
	wide := stripCSI(chromeApp(headerCompactW+20, 30).headerView())
	for _, want := range []string{"admin", "‹ Projects", "Tissue Server", "localhost:8080", "Issues"} {
		if !strings.Contains(wide, want) {
			t.Errorf("a wide header should show %q:\n%s", want, wide)
		}
	}
	// the threshold is inclusive: at exactly headerCompactW the header is already compact
	narrow := stripCSI(chromeApp(headerCompactW, 30).headerView())
	for _, gone := range []string{"admin", "‹ Projects", "localhost:8080"} {
		if strings.Contains(narrow, gone) {
			t.Errorf("a narrow header should drop %q:\n%s", gone, narrow)
		}
	}
	for _, want := range []string{"Tissue Server", "Issues", "Config"} {
		if !strings.Contains(narrow, want) {
			t.Errorf("a narrow header should still show %q:\n%s", want, narrow)
		}
	}
}

// #8: on a narrow terminal the footer wraps its key hints onto extra lines instead of ellipsizing, so
// every hint stays readable (the project drill-in has the longest hint row).
func TestFooterWrapsWithoutTruncating(t *testing.T) {
	a := chromeApp(110, 24)
	footer := stripCSI(a.footerView())
	if lines := strings.Count(footer, "\n") + 1; lines < 3 {
		t.Errorf("the long project hint row should wrap onto extra footer lines, got %d lines:\n%s", lines, footer)
	}
	// every screen action hint survives the wrap (none ellipsized away)
	for _, want := range []string{"e edit", "t transition", "a assign", "r reviewers", "c comment", "d delete", "v activity", "n new", "f filter", "/ search"} {
		if !strings.Contains(footer, want) {
			t.Errorf("the wrapped footer dropped the hint %q:\n%s", want, footer)
		}
	}
	// no ellipsis marker leaks in (ShortHelpView would have appended one)
	if strings.Contains(footer, "…") {
		t.Errorf("a wrapped footer must not ellipsize:\n%s", footer)
	}
}

// #8: the wrapped footer never overflows the terminal width (each line is clipped to width) and its hints
// still all render in the full composed view (the wrapped lines are charged to the body).
func TestFooterWrapFitsFullView(t *testing.T) {
	a := chromeApp(110, 24)
	view := stripCSI(a.View().Content)
	if rows := strings.Split(view, "\n"); len(rows) > 24 {
		t.Errorf("the composed view must not exceed the terminal height, got %d rows", len(rows))
	}
	for _, want := range []string{"n new", "/ search", "v activity"} {
		if !strings.Contains(view, want) {
			t.Errorf("a footer hint was clipped from the full view: missing %q", want)
		}
	}
}

// The wrapped rows come out of the body's budget, not out of the bottom margin: however many lines the
// hints take, the terminal's last row stays blank and no hint is clipped away. Before this, a 3-line
// footer ate the margin and a 4-line one (width 80) silently dropped its last hints off the screen.
func TestFooterWrapKeepsBottomPaddingAndEveryHint(t *testing.T) {
	for _, width := range []int{200, 110, 80, 60} {
		a := chromeApp(width, 24)
		rows := strings.Split(stripCSI(a.View().Content), "\n")
		if len(rows) > 24 {
			t.Errorf("width %d: the composed view must not exceed the terminal height, got %d rows", width, len(rows))
		}
		if last := strings.TrimRight(rows[len(rows)-1], " "); last != "" {
			t.Errorf("width %d: the terminal's last row should stay blank, got %q", width, last)
		}
		for _, want := range []string{"n new", "f filter", "/ search", "v activity", "s add to sprint"} {
			if !strings.Contains(stripCSI(a.View().Content), want) {
				t.Errorf("width %d: a wrapped hint was clipped from the full view: missing %q", width, want)
			}
		}
	}
}

// Charging the wrapped rows to the body has a floor: on a terminal too small to hold the hints at all,
// the footer's own tail gives way instead. The body must never be handed nothing (or a negative grant),
// and the bottom margin must survive even there.
func TestFooterNeverStarvesTheBody(t *testing.T) {
	for _, size := range [][2]int{{30, 24}, {20, 24}, {16, 24}, {80, 8}, {60, 8}} {
		width, height := size[0], size[1]
		a := chromeApp(width, height)
		if a.sentSize.Height < 1 {
			t.Errorf("%dx%d: the body was handed %d rows", width, height, a.sentSize.Height)
		}
		rows := strings.Split(stripCSI(a.View().Content), "\n")
		if len(rows) > height {
			t.Errorf("%dx%d: the composed view is %d rows", width, height, len(rows))
		}
		if last := strings.TrimRight(rows[len(rows)-1], " "); last != "" {
			t.Errorf("%dx%d: the terminal's last row should stay blank, got %q", width, height, last)
		}
	}
}

// The body is sized by message, so a footer that grows a wrapped row has to hand those rows back to the
// screen - otherwise the screen keeps laying out at its old height and the extra rows are clipped.
func TestFooterWrapChargesTheExtraRowsToTheBody(t *testing.T) {
	wide := chromeApp(200, 24)
	narrow := chromeApp(80, 24)
	if wide.sentSize.Height <= narrow.sentSize.Height {
		t.Errorf("a wrapping footer should leave the body fewer rows: wide=%d narrow=%d",
			wide.sentSize.Height, narrow.sentSize.Height)
	}
	if got, want := narrow.sentSize.Height, 24-narrow.reservedRows(); got != want {
		t.Errorf("the screen was told %d rows but the chrome reserves %d", got, want)
	}
}

// The hint rows are state-dependent, so the footer can change height with no terminal resize behind it:
// opening the help modal swaps the screen's long hint row for the modal's short one. Screens are sized
// by message, so the row the footer gives up has to be handed back, or the body keeps laying out short.
func TestFooterHeightChangeWithoutAResizeReachesTheScreen(t *testing.T) {
	a := chromeApp(110, 24) // narrow enough that the project screen's hints wrap onto a third line
	if h := lipgloss.Height(a.footerView()); h <= footerHeight {
		t.Fatalf("precondition: the hints should wrap here, got a %d-row footer", h)
	}
	before := a.sentSize.Height

	m, _ := a.Update(keyPress("?"))
	next := mustApp(t, m)
	if next.modal == nil {
		t.Fatal("? should open the help modal")
	}
	if h := lipgloss.Height(next.footerView()); h != footerHeight {
		t.Fatalf("the modal's short hint row should fit the band, got %d rows", h)
	}
	if next.sentSize.Height != before+1 {
		t.Errorf("the row the footer gave up should go back to the body: was %d, now %d", before, next.sentSize.Height)
	}
	if next.sentSize.Height != 24-next.reservedRows() {
		t.Errorf("the screen was told %d rows but the chrome reserves %d", next.sentSize.Height, next.reservedRows())
	}
}

func mustApp(t *testing.T, m tea.Model) App {
	t.Helper()
	a, ok := m.(App)
	if !ok {
		t.Fatalf("expected an App, got %T", m)
	}
	return a
}
