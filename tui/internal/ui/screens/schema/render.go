package schema

import (
	"image/color"
	"regexp"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"
	zone "github.com/lrstanley/bubblezone/v2"
	"github.com/mattn/go-runewidth"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// affordanceStyle colors an action-bar handle (+ State / Save / Cancel …): the accent + bold while
// it is the keyboard-focused item, its base color underlined while merely hovered, and the plain
// base color at rest. Shared by the create-workflow modal and the in-place flow editor.
func affordanceStyle(t theme.Theme, base color.Color, focused, hovered bool) lipgloss.Style {
	switch {
	case focused:
		return lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	case hovered:
		return lipgloss.NewStyle().Foreground(base).Underline(true)
	default:
		return lipgloss.NewStyle().Foreground(base)
	}
}

var schemaCSI = regexp.MustCompile("\\x1b\\[[0-9;]*[A-Za-z]")

// stripANSI removes all CSI sequences, leaving plain text with its column layout intact.
func stripANSI(s string) string { return schemaCSI.ReplaceAllString(s, "") }

// overlayDim splices fg over a dimmed copy of the plain backdrop at (x,y). fg is copied in
// verbatim so its zone markers survive (lipgloss.NewCompositor would rebuild the frame cell
// by cell and drop them).
func overlayDim(backdrop, fg string, x, y int, dim color.Color) string {
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

// splitCols splits a plain (ANSI-free) string at visible column c, returning the left part
// padded to exactly c columns and the remainder. A wide rune straddling c is replaced with
// spaces on both sides so the total column count is preserved.
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

// breaking reports whether r would split a row across lines or make lipgloss mis-measure
// its width. ESC is spared so ANSI styling survives.
func breaking(r rune) bool { return (r < 0x20 && r != 0x1b) || r == 0x7f }

// flatten replaces breaking control characters with a space. Catalog names and
// descriptions are admin free text that reaches us unsanitized (the backend's
// normalizeText only strips surrounding whitespace), and ansi.Truncate carries one running
// width across the whole string, so an embedded newline survives truncation and then
// lipgloss pads each line separately into two rows.
func flatten(s string) string {
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

// trunc clips s to w cells, ANSI-aware. lipgloss Width() WRAPS rather than clips, so a
// list row must be truncated first or it silently becomes two rows.
func trunc(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return ansi.Truncate(flatten(s), w, "…")
}

// fitLine truncates s to w cells and pads it back to exactly w, so the row occupies
// exactly one terminal line.
func fitLine(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return lipgloss.NewStyle().Width(w).Render(trunc(s, w))
}

// minGap is the smallest space kept between a row's head and its right-aligned tail, so a
// clipped tail never butts up against the head.
const minGap = 2

// alignRow lays out "head ....... tail" in exactly w cells. The tail is clipped first so
// the head always stays readable; only a head that overflows on its own is clipped.
func alignRow(head, tail string, w int, fill lipgloss.Style) string {
	if w < 1 {
		w = 1
	}
	// flatten before measuring: lipgloss.Width reports only the widest line, so a
	// multi-line tail would be sized as tiny and then truncated away entirely
	head, tail = flatten(head), flatten(tail)
	hw := lipgloss.Width(head)
	if hw >= w { // no room left for the tail
		return fitLine(head, w)
	}
	if avail := w - hw - minGap; lipgloss.Width(tail) > avail {
		tail = trunc(tail, avail)
	}
	return join(head, tail, w, fill)
}

// join pads head and tail apart to exactly w cells, styling the gap so a selected row's
// background reaches the tail. The final fit is unconditional: every caller depends on a
// row occupying exactly one line of exactly w cells.
func join(head, tail string, w int, fill lipgloss.Style) string {
	gap := w - lipgloss.Width(head) - lipgloss.Width(tail)
	if gap < 0 {
		gap = 0
	}
	return fitLine(head+fill.Render(strings.Repeat(" ", gap))+tail, w)
}

// metaLabelW is the width of the label column in a Details meta row; the value follows it,
// so this also sets the gap between a label and its value.
const metaLabelW = 15

// detailRow renders a muted fixed-width label followed by its value.
func detailRow(s theme.Styles, label, value string) string {
	return lipgloss.NewStyle().Foreground(s.Theme.Muted).Width(metaLabelW).Render(label) + value
}

// metaRow is a detailRow with a leading glyph in the label column. The glyph shares the
// label's muted color and, like home's section headers, is followed by two spaces; an empty
// glyph (plain terminals) leaves the label flush.
func metaRow(s theme.Styles, glyph, label, value string) string {
	if glyph != "" {
		label = glyph + "  " + label
	}
	return detailRow(s, label, value)
}

// sectionRule is a bold title followed by a muted rule filling the width.
func sectionRule(s theme.Styles, title string, width int) string {
	head := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true).Render(title)
	dashes := width - lipgloss.Width(title) - 1
	if dashes < 0 {
		dashes = 0
	}
	return head + s.Muted.Render(" "+strings.Repeat("─", dashes))
}

// sectionRuleAction is sectionRule with a clickable action embedded near the right end of the
// rule — "Title ───── action ──" — zone-marked with zoneID so a click can be routed to it. The
// button uses the field color (Primary); it takes the Accent focus color while keyboard-focused
// and brightens to Secondary while merely hovered, so it reads as interactive. Falls back to a
// plain rule when the panel is too narrow to seat the button.
func sectionRuleAction(s theme.Styles, title, action, zoneID string, width int, hovered, focused bool) string {
	const rightDashes = 2
	btnW := lipgloss.Width(action) + 2 // a space on each side of the label
	left := width - lipgloss.Width(title) - 1 - btnW - rightDashes
	if left < 1 {
		return sectionRule(s, title, width)
	}
	btnCol := s.Theme.Primary
	switch {
	case focused:
		btnCol = s.Theme.Accent
	case hovered:
		btnCol = s.Theme.Secondary
	}
	head := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true).Render(title)
	btn := zone.Mark(zoneID, lipgloss.NewStyle().Foreground(btnCol).Bold(true).Render(" "+action+" "))
	return head + s.Muted.Render(" "+strings.Repeat("─", left)) + btn + s.Muted.Render(strings.Repeat("─", rightDashes))
}

// hintBar renders an inline shortcut hint as "key label · key label …", tinting the key glyphs
// (Secondary) so they read as pressable while the labels stay muted. Arguments alternate
// key, label; a label may be empty for a bare key affordance.
func hintBar(s theme.Styles, pairs ...string) string {
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

// closingRule is a full-width muted rule that closes a section.
func closingRule(s theme.Styles, width int) string {
	if width < 0 {
		width = 0
	}
	return s.Muted.Render(strings.Repeat("─", width))
}

// ruleWithTitle is a full-width horizontal rule with a left-inset title, used as a top
// border with no sides.
func ruleWithTitle(title string, width int, c color.Color) string {
	label := " " + title + " "
	const lead = 2
	rest := width - lead - lipgloss.Width(label)
	if rest < 0 {
		rest = 0
	}
	return lipgloss.NewStyle().Foreground(c).Render(strings.Repeat("─", lead) + label + strings.Repeat("─", rest))
}

// scrollbar builds a one-cell-per-row scrollbar, blank when the content fits.
func scrollbar(off, total, view int, thumb, track color.Color) []string {
	cells := make([]string, view)
	if total <= view {
		for i := range cells {
			cells[i] = " "
		}
		return cells
	}
	th := max(1, view*view/total)
	pos := 0
	if span := total - view; span > 0 {
		pos = off * (view - th) / span
	}
	trackCell := lipgloss.NewStyle().Foreground(track).Render("│")
	thumbCell := lipgloss.NewStyle().Foreground(thumb).Render("█")
	for i := range cells {
		if i >= pos && i < pos+th {
			cells[i] = thumbCell
		} else {
			cells[i] = trackCell
		}
	}
	return cells
}

func orDash(s string) string {
	if s == "" {
		return "-"
	}
	return s
}

func yesNo(b bool) string {
	if b {
		return "Yes"
	}
	return "No"
}
