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

// Shared row/overlay render helpers. Screens keep thin lowercase wrappers so call sites stay untouched.

var csiPattern = regexp.MustCompile("\\x1b\\[[0-9;]*[A-Za-z]")

func StripANSI(s string) string { return csiPattern.ReplaceAllString(s, "") }

// OverlayDim splices fg over a dimmed copy of the plain backdrop at (x,y). fg is copied in verbatim
// so its zone markers survive (a cell-by-cell compositor would rebuild the frame and drop them).
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

// splitCols splits a plain (ANSI-free) string at visible column c, returning the left part padded
// to exactly c columns and the remainder. A wide rune straddling c is replaced with spaces on both
// sides so the total column count is preserved.
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

// MixColors blends a toward b by t in [0,1], used for the dimmer hover band.
func MixColors(a, b color.Color, t float64) color.Color {
	ar, ag, ab, _ := a.RGBA()
	br, bg, bb, _ := b.RGBA()
	blend := func(x, y uint32) uint8 {
		return uint8(float64(x>>8)*(1-t) + float64(y>>8)*t)
	}
	return color.RGBA{R: blend(ar, br), G: blend(ag, bg), B: blend(ab, bb), A: 0xff}
}

// breaking reports whether r would split a row across lines or make lipgloss mis-measure its width.
// ESC is spared so ANSI styling survives.
func breaking(r rune) bool { return (r < 0x20 && r != 0x1b) || r == 0x7f }

// Flatten replaces breaking control characters with a space. Catalog names and descriptions are
// admin free text that reaches us unsanitized, and ansi.Truncate carries one running width across
// the whole string, so an embedded newline would otherwise survive truncation and then let lipgloss
// pad each line separately into two rows.
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

// Trunc clips s to w cells, ANSI-aware. lipgloss Width() WRAPS rather than clips, so a list row must
// be truncated first or it silently becomes two rows.
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

// minGap is the smallest space kept between a row's head and its right-aligned tail, so a clipped
// tail never butts up against the head.
const minGap = 2

// AlignRow lays out "head ....... tail" in exactly w cells. The tail is clipped first so the head
// always stays readable. Only a head that overflows on its own is clipped.
func AlignRow(head, tail string, w int, fill lipgloss.Style) string {
	if w < 1 {
		w = 1
	}
	// flatten before measuring: lipgloss.Width reports only the widest line, so a multi-line tail
	// would be sized as tiny and then truncated away entirely
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

// join pads head and tail apart to exactly w cells, styling the gap so a selected row's background
// reaches the tail.
func join(head, tail string, w int, fill lipgloss.Style) string {
	gap := w - lipgloss.Width(head) - lipgloss.Width(tail)
	if gap < 0 {
		gap = 0
	}
	return FitLine(head+fill.Render(strings.Repeat(" ", gap))+tail, w)
}

// HintBar renders an inline shortcut hint as "key label · key label …", tinting the key glyphs
// (Secondary) so they read as pressable while the labels stay muted. Arguments alternate key, label —
// a label may be empty for a bare key affordance.
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
