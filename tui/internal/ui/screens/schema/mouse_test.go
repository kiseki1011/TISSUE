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

// Mirrors the CLI's startup fix: under a CJK locale go-runewidth counts box-drawing runes as
// width 2, doubling bubblezone's coordinates and breaking mouse hit-testing. NewGlobal here so a test
// that marks a zone does not depend on another test having initialised the manager first.
func TestMain(m *testing.M) {
	runewidth.DefaultCondition.EastAsianWidth = false
	zone.NewGlobal()
	os.Exit(m.Run())
}

// scanView waits for a zone's coordinates to settle: the shared global manager stores zones on a
// background worker, so another test's earlier scan can transiently set this id first.
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

// locate returns needle's display-cell position, the space bubblezone hit-tests against.
func locate(view, needle string) (x, y int, ok bool) {
	for ln, line := range strings.Split(view, "\n") {
		plain := stripANSI(line)
		if i := strings.Index(plain, needle); i >= 0 {
			return lipgloss.Width(plain[:i]), ln, true
		}
	}
	return 0, 0, false
}

// Clicking a state selects it and opens its metadata editor, whatever was selected before.
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

// Clicking a spine transition's "+ Guard" opens the guard editor with the add dropdown open.
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

// Moving over an element records the hover. Moving off the panel clears it.
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

// The ANSI theme has no surface to tint, so hover highlights via the foreground instead.
func TestGraphHoverAnsiTheme(t *testing.T) {
	s := theme.New(theme.ANSI())
	plain, _, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{}, wfElem{}, true)
	hovered, _, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{}, wfElem{elemState, 2}, true)
	if strings.Join(plain, "\n") == strings.Join(hovered, "\n") {
		t.Fatal("hover produced no change on the ANSI theme")
	}
}
