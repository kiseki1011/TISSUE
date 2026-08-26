package schema

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// affordanceStyle styles an action-bar handle by focus state.
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

// Thin wrappers over components/render.go so call sites stay unqualified.

func stripANSI(s string) string { return components.StripANSI(s) }

func overlayDim(backdrop, fg string, x, y int, dim color.Color) string {
	return components.OverlayDim(backdrop, fg, x, y, dim)
}

func flatten(s string) string        { return components.Flatten(s) }
func trunc(s string, w int) string   { return components.Trunc(s, w) }
func fitLine(s string, w int) string { return components.FitLine(s, w) }

func alignRow(head, tail string, w int, fill lipgloss.Style) string {
	return components.AlignRow(head, tail, w, fill)
}

// minGap is the smallest space between a row's head and its right-aligned tail.
const minGap = 2

// metaLabelW is the label column width in a Details meta row, which also sets the label/value gap.
const metaLabelW = 15

func detailRow(s theme.Styles, label, value string) string {
	return lipgloss.NewStyle().Foreground(s.Theme.Muted).Width(metaLabelW).Render(label) + value
}

// metaRow is a detailRow with a leading glyph. An empty glyph leaves the label flush.
func metaRow(s theme.Styles, glyph, label, value string) string {
	if glyph != "" {
		label = glyph + "  " + label
	}
	return detailRow(s, label, value)
}

func sectionRule(s theme.Styles, title string, width int) string {
	head := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true).Render(title)
	dashes := width - lipgloss.Width(title) - 1
	if dashes < 0 {
		dashes = 0
	}
	return head + s.Muted.Render(" "+strings.Repeat("─", dashes))
}

// sectionRuleAction is sectionRule with a clickable action. Falls back to a plain rule when narrow.
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

func hintBar(s theme.Styles, pairs ...string) string { return components.HintBar(s, pairs...) }

func closingRule(s theme.Styles, width int) string {
	if width < 0 {
		width = 0
	}
	return s.Muted.Render(strings.Repeat("─", width))
}

// ruleWithTitle is a top border with a left-inset title. Focus is signalled by colour, not weight.
func ruleWithTitle(title string, width int, c color.Color) string {
	dash := "─"
	label := " " + title + " "
	const lead = 2
	rest := width - lead - lipgloss.Width(label)
	if rest < 0 {
		rest = 0
	}
	return lipgloss.NewStyle().Foreground(c).Render(strings.Repeat(dash, lead) + label + strings.Repeat(dash, rest))
}

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
