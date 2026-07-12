package schema

import (
	"strings"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// colorGridCols lays the palette out eight swatches wide, so the sixteen ANSI colors fill the
// first two rows (normal then bright) and the extended colors follow beneath them.
const colorGridCols = 8

// colorPicker is a 2D swatch-grid dropdown for choosing one ColorType. Each swatch is a
// background-painted block (spaces, not block glyphs, so it is immune to ambiguous glyph
// widths), the cursor swatch carries a contrasting dot, and the highlighted color's name shows
// beneath the grid. It is the color counterpart to picker (a 1D text list) and shares its
// modal/zone/box conventions.
type colorPicker struct {
	title  string
	names  []string // ColorType names in palette order
	cols   int
	cursor int // flat index into names
}

func newColorPicker(title, current string, cols int) colorPicker {
	names := components.ColorTypeNames()
	cp := colorPicker{title: title, names: names, cols: cols}
	if i := indexOf(names, strings.ToUpper(strings.TrimSpace(current))); i >= 0 {
		cp.cursor = i
	}
	return cp
}

func (cp colorPicker) rows() int {
	if cp.cols <= 0 {
		return 0
	}
	return (len(cp.names) + cp.cols - 1) / cp.cols
}

// move shifts the cursor one cell, clamping at the grid edges. Horizontal steps run the palette
// linearly (wrapping across rows); vertical steps jump a whole row and stay put when the target
// cell is off the (possibly short) last row.
func (cp colorPicker) move(dx, dy int) colorPicker {
	n := len(cp.names)
	switch {
	case dx < 0 && cp.cursor > 0:
		cp.cursor--
	case dx > 0 && cp.cursor < n-1:
		cp.cursor++
	case dy < 0 && cp.cursor-cp.cols >= 0:
		cp.cursor -= cp.cols
	case dy > 0 && cp.cursor+cp.cols < n:
		cp.cursor += cp.cols
	}
	return cp
}

func (cp colorPicker) selected() (string, bool) {
	if cp.cursor < 0 || cp.cursor >= len(cp.names) {
		return "", false
	}
	return cp.names[cp.cursor], true
}

func colorCellZone(i int) string { return "colorpicker.cell." + itoa(i) }

// hitCell returns the swatch index under the cursor, or -1.
func (cp colorPicker) hitCell(msg tea.MouseMsg) int {
	for i := range cp.names {
		if z := zone.Get(colorCellZone(i)); z != nil && z.InBounds(msg) {
			return i
		}
	}
	return -1
}

// swatch renders one color cell: two background-painted cells, marked with a contrasting dot
// when it is the cursor.
func (cp colorPicker) swatch(i int, t theme.Theme) string {
	bg, fg, ok := components.ChipColors(cp.names[i])
	st := lipgloss.NewStyle()
	if ok {
		st = st.Background(bg)
	} else {
		st, fg = st.Background(t.Muted), t.Text
	}
	cell := "  "
	if i == cp.cursor {
		st, cell = st.Foreground(fg).Bold(true), "● "
	}
	return zone.Mark(colorCellZone(i), st.Render(cell))
}

func (cp colorPicker) View(s theme.Styles) string {
	t := s.Theme
	lines := make([]string, 0, cp.rows())
	for r := 0; r < cp.rows(); r++ {
		cells := make([]string, 0, cp.cols)
		for c := 0; c < cp.cols; c++ {
			i := r*cp.cols + c
			if i >= len(cp.names) {
				break
			}
			cells = append(cells, cp.swatch(i, t))
		}
		lines = append(lines, strings.Join(cells, " "))
	}
	grid := lipgloss.JoinVertical(lipgloss.Left, lines...)

	name := ""
	if n, ok := cp.selected(); ok {
		name = components.ColorLabel(n)
	}
	footer := lipgloss.NewStyle().Foreground(t.Accent).Render("▸ ") +
		lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(name)

	body := lipgloss.JoinVertical(lipgloss.Left, grid, "", footer)
	// one cell of padding each way, so the swatches are not tight against the border
	// (TitledBox itself only insets one cell horizontally and none vertically)
	inner := lipgloss.NewStyle().Padding(1, 1).Render(body)
	return components.TitledBoxCentered(cp.title, inner, t.Primary)
}
