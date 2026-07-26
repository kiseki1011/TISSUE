package components

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"
)

// ScrollBox fits an already-rendered, single bordered box into maxH rows so a modal taller than the
// terminal can be scrolled instead of silently clipped. It assumes box is rectangular and its first
// and last lines are the top/bottom border, as every components.TitledBox* produces. The scrollbar
// column surgery is ANSI-aware (ansi.Cut), so the border colours on either side of it survive.
func ScrollBox(box string, maxH, offset int, thumb, track color.Color) (out string, clamped int, scrolled bool) {
	lines := strings.Split(box, "\n")
	h := len(lines)
	if maxH < 3 || h <= maxH {
		return box, 0, false
	}
	w := lipgloss.Width(box)
	visible := maxH - 2 // interior rows that fit between the kept top/bottom borders
	interior := lines[1 : h-1]
	total := len(interior)

	maxOff := total - visible
	if offset < 0 {
		offset = 0
	}
	if offset > maxOff {
		offset = maxOff
	}

	thumbLen := visible * visible / total
	if thumbLen < 1 {
		thumbLen = 1
	}
	pos := 0
	if maxOff > 0 {
		pos = offset * (visible - thumbLen) / maxOff
	}

	thumbCell := lipgloss.NewStyle().Foreground(thumb).Render("█")
	trackCell := lipgloss.NewStyle().Foreground(track).Render("░")

	rows := make([]string, 0, maxH)
	rows = append(rows, lines[0])
	for j := 0; j < visible; j++ {
		cell := trackCell
		if j >= pos && j < pos+thumbLen {
			cell = thumbCell
		}
		rows = append(rows, spliceCol(interior[offset+j], w-2, cell))
	}
	rows = append(rows, lines[h-1])
	return strings.Join(rows, "\n"), offset, true
}

// spliceCol replaces the single visible column at x in line with cell. ansi.Cut is grapheme- and
// SGR-aware, so the border char kept to the right of the scrollbar retains its colour. The reset
// before cell stops the left content's style tinting it.
func spliceCol(line string, x int, cell string) string {
	left := ansi.Cut(line, 0, x)
	right := ansi.Cut(line, x+1, ansi.StringWidth(line))
	return left + "\x1b[0m" + cell + right
}
