package components

import (
	"image/color"
	"regexp"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"
	"github.com/mattn/go-runewidth"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var csiPattern = regexp.MustCompile("\\x1b\\[[0-9;]*[A-Za-z]")

func StripANSI(s string) string { return csiPattern.ReplaceAllString(s, "") }

// OverlayDim splices fg over a dimmed copy of the plain backdrop at (x,y). fg is copied verbatim so
// its zone markers survive — a cell-by-cell compositor would rebuild the frame and drop them.
func OverlayDim(backdrop, fg string, x, y int, dim color.Color) string {
	style := lipgloss.NewStyle().Foreground(dim)
	bgLines := strings.Split(backdrop, "\n")
	fgLines := strings.Split(fg, "\n")
	fgW := lipgloss.Width(fg)
	for i := range bgLines {
		row := i - y
		if row < 0 || row >= len(fgLines) {
			bgLines[i] = style.Render(bgLines[i])
			continue
		}
		left, rest := splitCols(bgLines[i], x)
		_, right := splitCols(rest, fgW)
		bgLines[i] = style.Render(left) + fgLines[row] + style.Render(right)
	}
	return strings.Join(bgLines, "\n")
}

// splitCols splits a plain (ANSI-free) string at visible column c, the left part padded to exactly c
// columns. A wide rune straddling c becomes spaces on both sides so the column count is preserved.
func splitCols(plain string, c int) (string, string) {
	if c <= 0 {
		return "", plain
	}
	col := 0
	for i, r := range plain {
		if col == c {
			return plain[:i], plain[i:]
		}
		w := runewidth.RuneWidth(r)
		if col+w > c {
			left := plain[:i] + strings.Repeat(" ", c-col)
			right := strings.Repeat(" ", col+w-c) + plain[i+len(string(r)):]
			return left, right
		}
		col += w
	}
	return plain + strings.Repeat(" ", c-col), ""
}

// MixColors blends a toward b by t in [0,1].
func MixColors(a, b color.Color, t float64) color.Color {
	ar, ag, ab, _ := a.RGBA()
	br, bg, bb, _ := b.RGBA()
	blend := func(x, y uint32) uint8 {
		return uint8(float64(x>>8)*(1-t) + float64(y>>8)*t)
	}
	return color.RGBA{R: blend(ar, br), G: blend(ag, bg), B: blend(ab, bb), A: 0xff}
}

// breaking reports control chars that split a row or break lipgloss' width math. ESC is spared so
// ANSI styling survives.
func breaking(r rune) bool { return (r < 0x20 && r != 0x1b) || r == 0x7f }

// Flatten replaces breaking control characters with a space. Catalog text arrives unsanitized, and
// ansi.Truncate carries one running width, so an embedded newline would survive and split the row.
func Flatten(s string) string {
	if !strings.ContainsFunc(s, breaking) {
		return s
	}
	return strings.Map(func(r rune) rune {
		if breaking(r) {
			return ' '
		}
		return r
	}, s)
}

// Trunc clips s to w cells, ANSI-aware. lipgloss WRAPS rather than clips, so an untruncated list row
// silently becomes two rows.
func Trunc(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return ansi.Truncate(Flatten(s), w, "…")
}

func FitLine(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return lipgloss.NewStyle().Width(w).Render(Trunc(s, w))
}

// minGap keeps a clipped tail from butting up against the head.
const minGap = 2

// AlignRow lays out "head ... tail" in exactly w cells, clipping the tail first so the head stays readable.
func AlignRow(head, tail string, w int, fill lipgloss.Style) string {
	if w < 1 {
		w = 1
	}
	// flatten first: lipgloss.Width reports only the widest line, so a multi-line tail measures tiny
	head, tail = Flatten(head), Flatten(tail)
	hw := lipgloss.Width(head)
	if hw >= w { // no room left for the tail
		return FitLine(head, w)
	}
	if avail := w - hw - minGap; lipgloss.Width(tail) > avail {
		tail = Trunc(tail, avail)
	}
	return join(head, tail, w, fill)
}

// join styles the gap so a selected row's background reaches the tail.
func join(head, tail string, w int, fill lipgloss.Style) string {
	gap := w - lipgloss.Width(head) - lipgloss.Width(tail)
	if gap < 0 {
		gap = 0
	}
	return FitLine(head+fill.Render(strings.Repeat(" ", gap))+tail, w)
}

// RuleWithTitle draws a titled top rule for a borderless panel. c carries focus, not the weight.
func RuleWithTitle(title string, width int, c color.Color) string {
	label := " " + title + " "
	const lead = 2
	rest := width - lead - lipgloss.Width(label)
	if rest < 0 {
		rest = 0
	}
	return lipgloss.NewStyle().Foreground(c).Render(strings.Repeat("─", lead) + label + strings.Repeat("─", rest))
}

// ScrollbarColumn returns view cells for a scrollbar, blank when everything fits.
func ScrollbarColumn(off, total, view int, thumb, track color.Color) []string {
	cells := make([]string, view)
	if total <= view {
		for i := range cells {
			cells[i] = " "
		}
		return cells
	}
	size := max(1, view*view/total)
	pos := 0
	if span := total - view; span > 0 {
		pos = off * (view - size) / span
	}
	trackCell := lipgloss.NewStyle().Foreground(track).Render("│")
	thumbCell := lipgloss.NewStyle().Foreground(thumb).Render("█")
	for i := range cells {
		if i >= pos && i < pos+size {
			cells[i] = thumbCell
		} else {
			cells[i] = trackCell
		}
	}
	return cells
}

// HintBar renders "key label · key label …" from pairs alternating key, label. An empty label leaves
// a bare key affordance.
func HintBar(s theme.Styles, pairs ...string) string {
	keyStyle := lipgloss.NewStyle().Foreground(s.Theme.Secondary)
	var b strings.Builder
	for i := 0; i+1 < len(pairs); i += 2 {
		if i > 0 {
			b.WriteString(s.Muted.Render(" · "))
		}
		b.WriteString(keyStyle.Render(pairs[i]))
		if pairs[i+1] != "" {
			b.WriteString(" ")
			b.WriteString(s.Muted.Render(pairs[i+1]))
		}
	}
	return b.String()
}
