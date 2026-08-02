package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

// The lists render Projects-style column headers: Issue Types carries Name/Hierarchy/Workflow/
// System, Workflows carries Name/Description/System.
func TestListShowsColumnHeaders(t *testing.T) {
	m := mk(120, 25, 4, 3, false)
	col := plain(m.leftColumn())
	for _, want := range []string{"Name", "Hierarchy", "Workflow", "System", "Description"} {
		if !strings.Contains(col, want) {
			t.Errorf("list is missing the %q column header:\n%s", want, col)
		}
	}
}

// Narrow panels shed the lowest-priority columns (like the Projects table drops Repository)
// rather than crushing the flex name column below its minimum.
func TestListDropsColumnsWhenNarrow(t *testing.T) {
	wide := typeCols(55)
	if len(wide) != 4 {
		t.Fatalf("wide panel keeps %d columns, want all 4", len(wide))
	}
	narrow := typeCols(24)
	if len(narrow) >= len(wide) {
		t.Fatalf("narrow panel kept %d columns, want fewer than %d", len(narrow), len(wide))
	}
	if narrow[0].title != "Name" {
		t.Errorf("the name column was dropped: %+v", narrow)
	}
	if got := flexWidth(narrow, 24); got < nameMinW {
		t.Errorf("flex name width = %d after dropping, want >= %d", got, nameMinW)
	}
}

// On a wide panel the Name column is capped and the surplus widens Workflow, so the flex name
// column never balloons past a readable width.
func TestTypeNameColumnCapped(t *testing.T) {
	const contentW = 80 // wide enough that an uncapped Name flex would far exceed its cap
	cols := typeCols(contentW)
	ws := colWidths(cols, contentW)
	var nameW, wfW int
	for i, c := range cols {
		switch c.title {
		case "Name":
			nameW = ws[i]
		case "Workflow":
			wfW = ws[i]
		}
	}
	if nameW != 18 {
		t.Errorf("Name width = %d on a wide panel, want it capped at 18", nameW)
	}
	if wfW <= 14 {
		t.Errorf("Workflow width = %d, want it widened past its 14 base by the surplus", wfW)
	}
	// widths + gutters still fill the panel exactly, so the box stays rectangular
	sum := 0
	for _, w := range ws {
		sum += w
	}
	if sum+2*(len(cols)-1) != contentW {
		t.Errorf("columns fill %d cells, want contentW=%d", sum+2*(len(cols)-1), contentW)
	}
}

// Hovering a non-selected row tracks it and repaints the row; moving off the list clears it.
func TestListHoverHighlightsRow(t *testing.T) {
	m := mk(120, 25, 6, 3, false)
	_ = scanView(t, m.View(), "schema.types")
	z := zone.Get("schema.types")
	// content line of row 1 (not the cursor, which sits on row 0): border+padY+header+one airy row
	y := z.StartY + 1 + listPadY + 1 + rowHeight
	x := z.StartX + 4
	base := m.View()

	hov, _ := m.Update(tea.MouseMotionMsg{X: x, Y: y})
	if hov.listHoverPane != paneTypes || hov.listHoverRow != 1 {
		t.Fatalf("hover = pane %d row %d, want paneTypes row 1", hov.listHoverPane, hov.listHoverRow)
	}
	if hov.View() == base {
		t.Error("hovering a row did not change its appearance")
	}

	away, _ := hov.Update(tea.MouseMotionMsg{X: 0, Y: 0})
	if away.listHoverRow != -1 || away.listHoverPane != -1 {
		t.Errorf("hover not cleared off the list: pane %d row %d", away.listHoverPane, away.listHoverRow)
	}
}

// A keypress drops a stale list hover so the band does not linger while the keyboard drives.
func TestListKeyClearsHover(t *testing.T) {
	m := mk(120, 25, 6, 3, false)
	m.listHoverPane, m.listHoverRow = paneTypes, 2
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyDown})
	if m.listHoverRow != -1 || m.listHoverPane != -1 {
		t.Fatalf("list hover survived a keypress: pane %d row %d", m.listHoverPane, m.listHoverRow)
	}
}

// The selected row of the active list is painted with the Selection background band.
func TestListSelectedRowBanded(t *testing.T) {
	m := mk(120, 25, 6, 3, false) // focus defaults to paneTypes, so its cursor row is active
	m.typeCursor = 2
	lines := strings.Split(m.leftColumn(), "\n") // keep ANSI to see the band
	// content line of row 2 within the types box
	contentY := 1 + listPadY + 1 + rowHeight*2 + 1
	row := lines[contentY]
	if !strings.Contains(stripANSI(row), "T2") {
		t.Fatalf("row at %d is not T2: %q", contentY, stripANSI(row))
	}
	if !strings.Contains(row, "48;2") && !strings.Contains(row, "48;5") {
		t.Errorf("selected row has no background band SGR: %q", row)
	}
}
