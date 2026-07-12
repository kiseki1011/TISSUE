package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// The eight-wide grid puts the sixteen ANSI colors in the first two rows (normal then bright),
// with the extended colors following, and holds every ColorType.
func TestColorPickerAnsiRows(t *testing.T) {
	cp := newColorPicker("c", "", colorGridCols)
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

// The 2D cursor clamps at the grid edges: horizontal steps run the palette linearly, a down
// step jumps a whole row, and neither runs off the (short) last row.
func TestColorPickerNavigation(t *testing.T) {
	cp := newColorPicker("c", "ANSI_BLACK", colorGridCols) // cursor 0
	if cp.cursor != 0 {
		t.Fatalf("seeded cursor %d, want 0", cp.cursor)
	}
	if cp.move(-1, 0).cursor != 0 || cp.move(0, -1).cursor != 0 {
		t.Error("cursor moved past the top-left edge")
	}
	if got := cp.move(1, 0).cursor; got != 1 {
		t.Errorf("right = %d, want 1", got)
	}
	if got := cp.move(0, 1).cursor; got != colorGridCols {
		t.Errorf("down = %d, want %d", got, colorGridCols)
	}
	last := len(cp.names) - 1
	end := cp
	end.cursor = last
	if got := end.move(0, 1).cursor; got != last {
		t.Errorf("down past the last row = %d, want it clamped at %d", got, last)
	}
	if got := end.move(1, 0).cursor; got != last {
		t.Errorf("right past the end = %d, want it clamped at %d", got, last)
	}
}

// The grid seeds its cursor at the current color and reports it as selected.
func TestColorPickerSeedsCurrent(t *testing.T) {
	cp := newColorPicker("c", "indigo", colorGridCols) // case-insensitive
	if got, _ := cp.selected(); got != "INDIGO" {
		t.Errorf("selected = %q, want INDIGO", got)
	}
}

// Each swatch is a click zone; clicking one resolves to its index.
func TestColorPickerClickHitsCell(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	cp := newColorPicker("Pick a color", "ANSI_BLACK", colorGridCols)
	const target = 5
	_ = scanView(t, cp.View(s), colorCellZone(target))
	z := zone.Get(colorCellZone(target))
	click := tea.MouseClickMsg{X: z.StartX, Y: z.StartY, Button: tea.MouseLeft}
	if got := cp.hitCell(click); got != target {
		t.Fatalf("hitCell = %d, want %d", got, target)
	}
}

// Opening the grid from a state's edit modal renders it over the dashboard.
func TestColorGridRendersInModal(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemState, 2}) // In Progress, has a color
	m, _ = m.Update(pressE())
	m.edit, _ = m.edit.focusOn(efColor)
	m.edit, _ = m.edit.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if !m.edit.picking {
		t.Fatal("enter on the color field did not open the grid")
	}
	if view := stripANSI(zone.Scan(m.View())); !strings.Contains(view, "Pick a color") {
		t.Error("the color grid is not shown over the dashboard")
	}
}
