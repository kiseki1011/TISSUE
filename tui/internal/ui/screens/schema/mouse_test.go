package schema

import (
	"os"
	"strings"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"
	runewidth "github.com/mattn/go-runewidth"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// TestMain mirrors the CLI's startup fix (internal/cli/root.go): under a CJK locale
// go-runewidth counts ambiguous-width runes (box-drawing) as width 2, which would double
// bubblezone's scanned coordinates and break the mouse hit-testing these tests rely on.
func TestMain(m *testing.M) {
	runewidth.DefaultCondition.EastAsianWidth = false
	os.Exit(m.Run())
}

// scanView scans a rendered view and waits for the given zone's coordinates to settle. The
// global manager is shared across tests (zone.NewGlobal is idempotent) and stores zones on a
// background worker that drains a buffered channel, so another test's earlier scan (possibly at
// a different terminal width) can transiently set this id before this scan is processed. This
// scan's items are last in the FIFO, so once the coordinates stop changing they are ours.
func scanView(t *testing.T, view, id string) string {
	t.Helper()
	zone.Clear(id)
	out := zone.Scan(view)
	var sx, sy, ex, ey, stable int
	seen := false
	for i := 0; i < 1000; i++ {
		if z := zone.Get(id); z != nil && !z.IsZero() {
			if seen && z.StartX == sx && z.StartY == sy && z.EndX == ex && z.EndY == ey {
				if stable++; stable >= 5 {
					return out
				}
			} else {
				sx, sy, ex, ey, stable, seen = z.StartX, z.StartY, z.EndX, z.EndY, 0, true
			}
		}
		time.Sleep(time.Millisecond)
	}
	t.Fatalf("zone %q did not settle after scan", id)
	return out
}

// locate finds needle in the scanned view and returns the display-cell position of its first
// column, matching the coordinate space bubblezone hit-tests against.
func locate(view, needle string) (x, y int, ok bool) {
	for ln, line := range strings.Split(view, "\n") {
		plain := stripANSI(line)
		if i := strings.Index(plain, needle); i >= 0 {
			return lipgloss.Width(plain[:i]), ln, true
		}
	}
	return 0, 0, false
}

// Clicking a state chip in the graph selects that state and opens its metadata editor, even
// when a different element was selected before.
func TestClickOpensEditForElement(t *testing.T) {
	m := mkWorkflowModel(t)
	view := scanView(t, m.View(), "schema.detail")
	x, y, ok := locate(view, "In Progress")
	if !ok {
		t.Fatal("could not find the In Progress state in the rendered graph")
	}
	m, _ = m.Update(tea.MouseClickMsg{X: x + 1, Y: y, Button: tea.MouseLeft})
	if !m.editing {
		t.Fatal("clicking a state did not open the editor")
	}
	if m.edit.kind != editState {
		t.Fatalf("editor kind = %v, want editState", m.edit.kind)
	}
	if got := m.edit.name.Value(); got != "In Progress" {
		t.Errorf("editor seeded name %q, want In Progress", got)
	}
}

// Each spine transition shows a "+ Guard" affordance below its name/guards, and clicking it
// opens the guard editor for that transition with the add-guard dropdown already open.
func TestClickAddGuardOpensGuardEditor(t *testing.T) {
	m := mkWorkflowModel(t)
	view := scanView(t, m.View(), "schema.detail")
	if !strings.Contains(stripANSI(view), addGuardText) {
		t.Fatalf("graph does not show the %q affordance", addGuardText)
	}
	x, y, ok := locate(view, addGuardText) // the first one, under the top spine transition (Start)
	if !ok {
		t.Fatal("could not find a + Guard button")
	}
	m, _ = m.Update(tea.MouseClickMsg{X: x + 1, Y: y, Button: tea.MouseLeft})
	if !m.guardsEditing {
		t.Fatal("clicking + Guard did not open the guard editor")
	}
	if !m.guards.pickOpen {
		t.Error("the add-guard dropdown did not open")
	}
	if m.guards.transNm != "Start" {
		t.Errorf("guard editor opened for %q, want the Start transition", m.guards.transNm)
	}
	if m.editing {
		t.Error("clicking + Guard wrongly opened the metadata editor")
	}
}

// Moving the cursor over an element records it as hovered; moving off the panel clears it.
func TestMotionTracksHover(t *testing.T) {
	m := mkWorkflowModel(t)
	view := scanView(t, m.View(), "schema.detail")
	x, y, ok := locate(view, "In Progress")
	if !ok {
		t.Fatal("could not find the In Progress state in the rendered graph")
	}
	m, _ = m.Update(tea.MouseMotionMsg{X: x + 1, Y: y})
	if m.wfHover != (wfElem{elemState, 2}) {
		t.Fatalf("hover = %+v, want the In Progress state", m.wfHover)
	}
	m, _ = m.Update(tea.MouseMotionMsg{X: 0, Y: 0}) // off the Details panel
	if m.wfHover != (wfElem{}) {
		t.Fatalf("hover not cleared off the panel: %+v", m.wfHover)
	}
}

// A keypress drops a stale hover so the highlight does not linger while the keyboard drives.
func TestKeyClearsHover(t *testing.T) {
	m := mkWorkflowModel(t)
	m.wfHover = wfElem{elemState, 2}
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyDown})
	if m.wfHover != (wfElem{}) {
		t.Fatalf("hover survived a keypress: %+v", m.wfHover)
	}
}

// Clicking empty Details space (the top rule) focuses the panel without opening an editor.
func TestClickEmptyDetailFocusesOnly(t *testing.T) {
	m := mkWorkflowModel(t)
	m, _ = m.setFocus(paneWorkflows)
	_ = scanView(t, m.View(), "schema.detail")
	z := zone.Get("schema.detail")
	m, _ = m.Update(tea.MouseClickMsg{X: z.StartX + 1, Y: z.StartY, Button: tea.MouseLeft})
	if m.editing {
		t.Fatal("clicking empty detail space opened an editor")
	}
	if m.focus != paneDetail {
		t.Fatalf("clicking the detail panel did not focus it (focus=%d)", m.focus)
	}
}

// Hovering an element visibly changes the diagram without shifting its layout.
func TestGraphHoverHighlightsElement(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	plain, _, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{}, wfElem{}, true)
	hovered, _, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{}, wfElem{elemState, 2}, true)
	if strings.Join(plain, "\n") == strings.Join(hovered, "\n") {
		t.Fatal("hovering a state produced no visible change")
	}
	if len(plain) != len(hovered) {
		t.Fatalf("hover changed the line count: %d vs %d", len(plain), len(hovered))
	}
}

// On the ANSI theme, which has no real surface to tint, hover still highlights (via the
// foreground) rather than doing nothing.
func TestGraphHoverAnsiTheme(t *testing.T) {
	s := theme.New(theme.ANSI())
	plain, _, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{}, wfElem{}, true)
	hovered, _, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{}, wfElem{elemState, 2}, true)
	if strings.Join(plain, "\n") == strings.Join(hovered, "\n") {
		t.Fatal("hover produced no change on the ANSI theme")
	}
}
