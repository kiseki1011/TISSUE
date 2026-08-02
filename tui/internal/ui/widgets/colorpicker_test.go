package widgets

import (
	"os"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"
	runewidth "github.com/mattn/go-runewidth"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// TestMain disables ambiguous-width counting so box-drawing runes measure as width 1, matching the
// CLI's startup fix; otherwise bubblezone's scanned coordinates double under a CJK locale.
func TestMain(m *testing.M) {
	runewidth.DefaultCondition.EastAsianWidth = false
	os.Exit(m.Run())
}

// settleZone scans a view and waits for the given zone's coordinates to settle (the global manager
// stores zones on a background worker, so a Get right after Scan can race).
func settleZone(t *testing.T, view, id string) {
	t.Helper()
	zone.Clear(id)
	zone.Scan(view)
	var sx, sy, ex, ey, stable int
	seen := false
	for i := 0; i < 1000; i++ {
		if z := zone.Get(id); z != nil && !z.IsZero() {
			if seen && z.StartX == sx && z.StartY == sy && z.EndX == ex && z.EndY == ey {
				if stable++; stable >= 5 {
					return
				}
			} else {
				sx, sy, ex, ey, stable, seen = z.StartX, z.StartY, z.EndX, z.EndY, 0, true
			}
		}
		time.Sleep(time.Millisecond)
	}
	t.Fatalf("zone %q did not settle after scan", id)
}

// The eight-wide grid puts the sixteen ANSI colors in the first two rows (normal then bright), with
// the extended colors following, and holds every ColorType.
func TestColorPickerAnsiRows(t *testing.T) {
	cp := NewColorPicker("c", "", ColorGridCols)
	if cp.cols != 8 {
		t.Fatalf("cols = %d, want 8", cp.cols)
	}
	if cp.names[0] != "ANSI_BLACK" || cp.names[8] != "ANSI_BRIGHT_BLACK" {
		t.Errorf("ANSI rows misplaced: names[0]=%q names[8]=%q", cp.names[0], cp.names[8])
	}
	if len(cp.names) != 30 {
		t.Errorf("palette has %d colors, want 30", len(cp.names))
	}
}

// The 2D cursor clamps at the grid edges: horizontal steps run the palette linearly, a down step
// jumps a whole row, and neither runs off the (short) last row.
func TestColorPickerNavigation(t *testing.T) {
	cp := NewColorPicker("c", "ANSI_BLACK", ColorGridCols) // cursor 0
	if cp.Cursor != 0 {
		t.Fatalf("seeded cursor %d, want 0", cp.Cursor)
	}
	if cp.Move(-1, 0).Cursor != 0 || cp.Move(0, -1).Cursor != 0 {
		t.Error("cursor moved past the top-left edge")
	}
	if got := cp.Move(1, 0).Cursor; got != 1 {
		t.Errorf("right = %d, want 1", got)
	}
	if got := cp.Move(0, 1).Cursor; got != ColorGridCols {
		t.Errorf("down = %d, want %d", got, ColorGridCols)
	}
	last := len(cp.names) - 1
	end := cp
	end.Cursor = last
	if got := end.Move(0, 1).Cursor; got != last {
		t.Errorf("down past the last row = %d, want it clamped at %d", got, last)
	}
	if got := end.Move(1, 0).Cursor; got != last {
		t.Errorf("right past the end = %d, want it clamped at %d", got, last)
	}
}

// The grid seeds its cursor at the current color (case-insensitive) and reports it as selected.
func TestColorPickerSeedsCurrent(t *testing.T) {
	cp := NewColorPicker("c", "indigo", ColorGridCols)
	if got, _ := cp.Selected(); got != "INDIGO" {
		t.Errorf("selected = %q, want INDIGO", got)
	}
}

// Each swatch is a click zone; clicking one resolves to its index.
func TestColorPickerClickHitsCell(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	cp := NewColorPicker("Pick a color", "ANSI_BLACK", ColorGridCols)
	const target = 5
	settleZone(t, cp.View(s), colorCellZone(target))
	z := zone.Get(colorCellZone(target))
	click := tea.MouseClickMsg{X: z.StartX, Y: z.StartY, Button: tea.MouseLeft}
	if got := cp.HitCell(click); got != target {
		t.Fatalf("hitCell = %d, want %d", got, target)
	}
}
