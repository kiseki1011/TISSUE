package components

import (
	"fmt"
	"strings"
	"testing"
	"time"

	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

// tallBox builds a bordered box with n coloured interior rows.
func tallBox(n int) string {
	rows := make([]string, n)
	for i := range rows {
		rows[i] = lipgloss.NewStyle().Foreground(lipgloss.Color("#7aa2f7")).Render(fmt.Sprintf("row %02d content", i))
	}
	return TitledBoxCentered("Modal", lipgloss.NewStyle().Padding(1, 1).Render(strings.Join(rows, "\n")), lipgloss.Color("#ff9e64"))
}

var thumbCol = lipgloss.Color("#ff9e64")
var trackCol = lipgloss.Color("#414868")

// A box that already fits within maxH is returned untouched.
func TestScrollBoxFits(t *testing.T) {
	box := tallBox(4)
	h := lipgloss.Height(box)
	out, clamped, scrolled := ScrollBox(box, h+3, 5, thumbCol, trackCol)
	if scrolled {
		t.Error("a fitting box must not report scrolled")
	}
	if out != box {
		t.Error("a fitting box must be returned unchanged")
	}
	if clamped != 0 {
		t.Errorf("clamped offset = %d, want 0 for a fitting box", clamped)
	}
}

// A guard rejects a maxH too small to draw a windowed box.
func TestScrollBoxTinyMaxH(t *testing.T) {
	box := tallBox(10)
	if out, _, scrolled := ScrollBox(box, 2, 0, thumbCol, trackCol); scrolled || out != box {
		t.Error("maxH < 3 must return the box unchanged")
	}
}

// A box taller than maxH is windowed to exactly maxH rows, its width and top/bottom borders intact,
// with a scrollbar drawn just inside the right border.
func TestScrollBoxWindows(t *testing.T) {
	box := tallBox(20)
	fullW := lipgloss.Width(box)
	in := strings.Split(box, "\n")

	out, _, scrolled := ScrollBox(box, 10, 0, thumbCol, trackCol)
	if !scrolled {
		t.Fatal("a box taller than maxH must report scrolled")
	}
	got := strings.Split(out, "\n")
	if len(got) != 10 {
		t.Fatalf("windowed height = %d, want 10", len(got))
	}
	for i, ln := range got {
		if w := lipgloss.Width(ln); w != fullW {
			t.Errorf("line %d width = %d, want %d (%q)", i, w, fullW, StripANSI(ln))
		}
	}
	if got[0] != in[0] {
		t.Error("top border row changed")
	}
	if got[len(got)-1] != in[len(in)-1] {
		t.Error("bottom border row changed")
	}
	// every interior row keeps its right border, with a scrollbar cell just inside it
	for _, ln := range got[1 : len(got)-1] {
		r := []rune(StripANSI(ln))
		if r[len(r)-1] != '│' {
			t.Errorf("interior row lost its right border: %q", string(r))
		}
		if c := r[len(r)-2]; c != '█' && c != '░' {
			t.Errorf("no scrollbar cell before the right border: got %q in %q", string(c), string(r))
		}
	}
}

// The offset is clamped into range; an out-of-range request lands on the last window and shows the
// bottom-most interior rows.
func TestScrollBoxOffsetClamps(t *testing.T) {
	box := tallBox(20)
	interior := 20 + 2 // padding adds a blank row top and bottom of the body
	out, clamped, _ := ScrollBox(box, 10, 999, thumbCol, trackCol)
	wantMax := interior - (10 - 2)
	if clamped != wantMax {
		t.Errorf("clamped offset = %d, want %d", clamped, wantMax)
	}
	// the last content row must be visible at the bottom window
	if !strings.Contains(StripANSI(out), "row 19 content") {
		t.Errorf("bottom window does not show the last row:\n%s", StripANSI(out))
	}
	// a negative offset clamps to the top
	out2, clamped2, _ := ScrollBox(box, 10, -5, thumbCol, trackCol)
	if clamped2 != 0 {
		t.Errorf("negative offset clamped to %d, want 0", clamped2)
	}
	if !strings.Contains(StripANSI(out2), "row 00 content") {
		t.Error("top window does not show the first row")
	}
}

// Windowing must keep the bubblezone click markers on the visible rows intact — the scrollbar column
// surgery cuts each interior row, so a marker-dropping cut would silently break clicks on a modal's
// buttons whenever it is scrolled.
func TestScrollBoxPreservesClickZones(t *testing.T) {
	zone.NewGlobal()
	rows := make([]string, 16)
	for i := range rows {
		rows[i] = zone.Mark(fmt.Sprintf("btn.%d", i), lipgloss.NewStyle().Foreground(lipgloss.Color("#7aa2f7")).Render(fmt.Sprintf("button-%02d", i)))
	}
	box := TitledBoxCentered("Marks", lipgloss.NewStyle().Padding(1, 1).Render(strings.Join(rows, "\n")), lipgloss.Color("#ff9e64"))

	out, _, _ := ScrollBox(box, 10, 3, thumbCol, trackCol)
	plain := StripANSI(out)

	var visibleIDs []string
	for i := 0; i < 16; i++ {
		if strings.Contains(plain, fmt.Sprintf("button-%02d", i)) {
			id := fmt.Sprintf("btn.%d", i)
			visibleIDs = append(visibleIDs, id)
			zone.Clear(id)
		}
	}
	if len(visibleIDs) < 2 {
		t.Fatalf("expected several buttons visible in the window, got %d", len(visibleIDs))
	}
	zone.Scan(out)

	// bubblezone stores zones on a background worker, so poll until every visible button's marker
	// registers. If windowing had dropped a marker it would never register and this would time out.
	for attempt := 0; attempt < 1000; attempt++ {
		allSet := true
		for _, id := range visibleIDs {
			if z := zone.Get(id); z == nil || z.IsZero() {
				allSet = false
				break
			}
		}
		if allSet {
			return
		}
		time.Sleep(time.Millisecond)
	}
	t.Fatal("some visible click zones never registered after ScrollBox+Scan (windowing damaged a marker)")
}
