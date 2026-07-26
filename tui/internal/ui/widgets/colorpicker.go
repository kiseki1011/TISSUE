// Package widgets holds reusable stateful Bubble Tea sub-models shared across the catalog screens.
package widgets

import (
	"slices"
	"strconv"
	"strings"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// ColorGridCols is eight wide so the sixteen ANSI colors fill the first two rows (normal then bright).
const ColorGridCols = 8

// ColorPicker is a 2D swatch-grid dropdown for choosing one ColorType. Swatches are painted with
// spaces, not block glyphs, so they are immune to ambiguous glyph widths. Cursor is exported so a
// form's motion/click handler can set it directly after a HitCell.
type ColorPicker struct {
	title  string
	names  []string // ColorType names in palette order
	cols   int
	Cursor int // flat index into names
}

func NewColorPicker(title, current string, cols int) ColorPicker {
	names := components.ColorTypeNames()
	cp := ColorPicker{title: title, names: names, cols: cols}
	if i := slices.Index(names, strings.ToUpper(strings.TrimSpace(current))); i >= 0 {
		cp.Cursor = i
	}
	return cp
}

func (cp ColorPicker) rows() int {
	if cp.cols <= 0 {
		return 0
	}
	return (len(cp.names) + cp.cols - 1) / cp.cols
}

// Move shifts the cursor one cell, clamping at edges. Vertical steps stay put on the short last row.
func (cp ColorPicker) Move(dx, dy int) ColorPicker {
	n := len(cp.names)
	switch {
	case dx < 0 && cp.Cursor > 0:
		cp.Cursor--
	case dx > 0 && cp.Cursor < n-1:
		cp.Cursor++
	case dy < 0 && cp.Cursor-cp.cols >= 0:
		cp.Cursor -= cp.cols
	case dy > 0 && cp.Cursor+cp.cols < n:
		cp.Cursor += cp.cols
	}
	return cp
}

func (cp ColorPicker) Selected() (string, bool) {
	if cp.Cursor < 0 || cp.Cursor >= len(cp.names) {
		return "", false
	}
	return cp.names[cp.Cursor], true
}

func colorCellZone(i int) string { return "widgets.colorpicker.cell." + strconv.Itoa(i) }

func (cp ColorPicker) HitCell(msg tea.MouseMsg) int {
	for i := range cp.names {
		if z := zone.Get(colorCellZone(i)); z != nil && z.InBounds(msg) {
			return i
		}
	}
	return -1
}

func (cp ColorPicker) swatch(i int, t theme.Theme) string {
	bg, fg, ok := components.ChipColors(cp.names[i])
	st := lipgloss.NewStyle()
	if ok {
		st = st.Background(bg)
	} else {
		st, fg = st.Background(t.Muted), t.Text
	}
	cell := "  "
	if i == cp.Cursor {
		st, cell = st.Foreground(fg).Bold(true), "● "
	}
	return zone.Mark(colorCellZone(i), st.Render(cell))
}

func (cp ColorPicker) View(s theme.Styles) string {
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
	if n, ok := cp.Selected(); ok {
		name = components.ColorLabel(n)
	}
	footer := lipgloss.NewStyle().Foreground(t.Accent).Render("▸ ") +
		lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(name)

	body := lipgloss.JoinVertical(lipgloss.Left, grid, "", footer)
	inner := lipgloss.NewStyle().Padding(1, 1).Render(body)
	return components.TitledBoxCentered(cp.title, inner, t.Primary)
}
