package ui

import (
	"regexp"
	"strings"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

func toastApp() App {
	a := App{width: 80, height: 24}
	a.toasts = toast.New(theme.TokyoNight(), glyph.New(glyph.Unicode))
	return a
}

var csiSeq = regexp.MustCompile(`\x1b\[[0-9;]*[A-Za-z]`)

func stripCSI(s string) string { return csiSeq.ReplaceAllString(s, "") }

func TestSpliceAtPreservesLeftAndPlacesInsert(t *testing.T) {
	left := "\x1b[31mred\x1b[0m" // "red" — 3 visible columns, styled
	out := spliceAt(left, 10, "XYZ")
	p := stripCSI(out)
	if !strings.HasPrefix(p, "red") {
		t.Errorf("left content lost: %q", p)
	}
	if !strings.HasSuffix(p, "XYZ") {
		t.Errorf("insert not placed at the cut: %q", p)
	}
	if lipgloss.Width(out) != 13 { // 10 padded columns + the 3-wide insert
		t.Errorf("spliced width = %d, want 13 (%q)", lipgloss.Width(out), p)
	}
	if !strings.Contains(out, "\x1b[31m") {
		t.Error("the left segment's SGR styling was dropped")
	}
}

func TestOverlayToastsCompositesBottomRight(t *testing.T) {
	a := App{width: 80, height: 20}
	a.toasts = toast.New(theme.TokyoNight(), glyph.New(glyph.Unicode))
	a.toasts, _ = a.toasts.Update(toast.ShowMsg{Level: toast.Success, Text: "Deleted workflow"})

	lines := make([]string, 20)
	for i := range lines {
		lines[i] = strings.Repeat("·", 80) // a full-width backdrop row
	}
	frame := strings.Join(lines, "\n")
	out := a.overlayToasts(frame)
	outLines := strings.Split(out, "\n")

	if len(outLines) != 20 {
		t.Fatalf("overlay changed the row count: %d, want 20", len(outLines))
	}
	if !strings.Contains(stripCSI(out), "Deleted workflow") {
		t.Error("the toast text was not composited into the frame")
	}
	if stripCSI(outLines[0]) != strings.Repeat("·", 80) {
		t.Errorf("a top row was altered by the overlay: %q", stripCSI(outLines[0]))
	}
	if !strings.HasPrefix(stripCSI(outLines[15]), "·") {
		t.Errorf("the toast overran the left of its row: %q", stripCSI(outLines[15]))
	}
}

func TestOverlayToastsNoopWhenEmpty(t *testing.T) {
	a := App{width: 80, height: 20}
	a.toasts = toast.New(theme.TokyoNight(), glyph.New(glyph.Unicode))
	frame := "one\ntwo\nthree"
	if got := a.overlayToasts(frame); got != frame {
		t.Errorf("empty stack changed the frame: %q", got)
	}
}

// A wide grapheme straddling the cut drops whole, not half.
func TestSpliceAtWideClusterStraddle(t *testing.T) {
	// each of 가/나/다 is 2 columns, so cutting at x=3 lands in the middle of '나'
	out := spliceAt("\x1b[31m가나다\x1b[0m", 3, "XY")
	p := stripCSI(out)
	if !strings.HasPrefix(p, "가") {
		t.Errorf("first cluster lost: %q", p)
	}
	if strings.ContainsRune(p, '나') || strings.ContainsRune(p, '다') {
		t.Errorf("content at/after the straddled cut was not dropped: %q", p)
	}
	if !strings.HasSuffix(p, "XY") {
		t.Errorf("insert not placed: %q", p)
	}
	if lipgloss.Width(out) != 5 { // '가'(2) + 1 pad column to x=3 + "XY"(2)
		t.Errorf("width = %d, want 5 (%q)", lipgloss.Width(out), p)
	}
}

func TestSpliceAtDropsRightAndKeepsLeftSGR(t *testing.T) {
	out := spliceAt("\x1b[31mredblue\x1b[0m", 3, "XY")
	p := stripCSI(out)
	if !strings.HasPrefix(p, "red") {
		t.Errorf("left content wrong: %q", p)
	}
	if strings.Contains(p, "blue") {
		t.Errorf("content past the cut not dropped: %q", p)
	}
	if lipgloss.Width(out) != 5 { // 3 kept columns + the 2-wide insert
		t.Errorf("width = %d, want 5", lipgloss.Width(out))
	}
	if !strings.HasPrefix(out, "\x1b[31mred") {
		t.Errorf("left SGR not preserved byte-for-byte: %q", out)
	}
}

// settleZone waits out bubblezone's async scan worker, returning the zone once its bounds are stable.
func settleZone(t *testing.T, id string) *zone.ZoneInfo {
	t.Helper()
	var sx, sy, ex, ey, stable int
	seen := false
	for i := 0; i < 1000; i++ {
		if z := zone.Get(id); z != nil && !z.IsZero() {
			if seen && z.StartX == sx && z.StartY == sy && z.EndX == ex && z.EndY == ey {
				if stable++; stable >= 5 {
					return z
				}
			} else {
				sx, sy, ex, ey, stable, seen = z.StartX, z.StartY, z.EndX, z.EndY, 0, true
			}
		}
		time.Sleep(time.Millisecond)
	}
	t.Fatalf("zone %q did not settle after scan", id)
	return nil
}

// The invariant behind overlaying after zone.Scan: a zone outside the toast keeps its exact bounds.
func TestOverlayPreservesZonesOutsideToast(t *testing.T) {
	zone.NewGlobal()
	a := toastApp()
	a.toasts, _ = a.toasts.Update(toast.ShowMsg{Level: toast.Info, Text: "note"})

	// a clickable region in the top-left, far from the bottom-right toast
	rows := make([]string, 24)
	rows[0] = zone.Mark("app.tab.projects", "Projects")
	for i := 1; i < len(rows); i++ {
		rows[i] = strings.Repeat(" ", 80)
	}
	scanned := zone.Scan(strings.Join(rows, "\n"))
	before := settleZone(t, "app.tab.projects")
	sx, sy, ex, ey := before.StartX, before.StartY, before.EndX, before.EndY

	_ = a.overlayToasts(scanned)
	after := zone.Get("app.tab.projects")
	if after == nil || after.StartX != sx || after.StartY != sy || after.EndX != ex || after.EndY != ey {
		t.Errorf("overlay disturbed a zone outside the toast: want (%d,%d,%d,%d), got %+v",
			sx, sy, ex, ey, after)
	}
}

// Deterministic counterpart: every row outside the toast band stays byte-identical.
func TestOverlayLeavesRowsOutsideBandUntouched(t *testing.T) {
	a := toastApp()
	a.toasts, _ = a.toasts.Update(toast.ShowMsg{Level: toast.Error, Text: "still in use"})
	_, top, bottom, _, ok := a.toastLayout()
	if !ok {
		t.Fatal("layout not ok while a toast is showing")
	}
	rows := make([]string, 24)
	for i := range rows {
		rows[i] = "row" + strings.Repeat("·", 77)
	}
	out := strings.Split(a.overlayToasts(strings.Join(rows, "\n")), "\n")
	if len(out) != len(rows) {
		t.Fatalf("overlay changed the row count: %d, want %d", len(out), len(rows))
	}
	for i := range rows {
		if i >= top && i <= bottom {
			continue // the toast band is expected to change
		}
		if out[i] != rows[i] {
			t.Errorf("row %d outside the toast band [%d,%d] was modified", i, top, bottom)
		}
	}
}

// mouseOnToast lets the shell swallow input that would pass through the toast to the widget beneath.
func TestMouseOnToastBounds(t *testing.T) {
	a := toastApp()
	if a.mouseOnToast(tea.MouseClickMsg{X: 70, Y: 22}) {
		t.Error("no toast is showing, yet a point was reported inside it")
	}
	a.toasts, _ = a.toasts.Update(toast.ShowMsg{Level: toast.Success, Text: "Deleted workflow"})
	x, top, bottom, w, ok := a.toastLayout()
	if !ok {
		t.Fatal("layout not ok while a toast is showing")
	}
	mid := tea.MouseClickMsg{X: x + w/2, Y: (top + bottom) / 2}
	if !a.mouseOnToast(mid) {
		t.Errorf("a point inside the toast (%d,%d) was not detected", mid.X, mid.Y)
	}
	if a.mouseOnToast(tea.MouseClickMsg{X: 0, Y: 0}) {
		t.Error("a top-left point was wrongly reported inside the toast")
	}
	if a.mouseOnToast(tea.MouseClickMsg{X: x - 1, Y: top}) {
		t.Error("a point just left of the toast was reported inside")
	}
}
