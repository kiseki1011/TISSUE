// Package components holds small shared render helpers used across screens.
package components

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
)

// TitledBox splices the title into the top edge, left-inset, since lipgloss has no native border title.
func TitledBox(title, body string, borderColor color.Color) string {
	return titledBox(lipgloss.RoundedBorder(), title, "", body, borderColor, false)
}

// TitledBoxSub is TitledBox with a subtitle spliced into the bottom edge, right.
func TitledBoxSub(title, subtitle, body string, borderColor color.Color) string {
	return titledBox(lipgloss.RoundedBorder(), title, subtitle, body, borderColor, false)
}

func TitledBoxCentered(title, body string, borderColor color.Color) string {
	return titledBox(lipgloss.RoundedBorder(), title, "", body, borderColor, true)
}

// TitledBoxWeighted is TitledBox that signals focus by border COLOUR (the borderColor passed by the
// caller). The focused parameter is accepted and ignored — retained for call-site compatibility —
// because focus is never shown by thickening the border, only by its colour.
func TitledBoxWeighted(title, body string, borderColor color.Color, focused bool) string {
	return TitledBox(title, body, borderColor)
}

// TitledBoxSubWeighted is TitledBoxSub that signals focus by colour (see TitledBoxWeighted).
func TitledBoxSubWeighted(title, subtitle, body string, borderColor color.Color, focused bool) string {
	return TitledBoxSub(title, subtitle, body, borderColor)
}

// TitledRule renders body between titled top/bottom rules with no side borders — the "Details" look.
// Its outer width and height match TitledBox exactly (chrome rows + one-cell blank insets each side),
// so it drops into a boxed layout without disturbing neighbouring sizes. Focus is signalled by the
// rule COLOUR, not weight.
func TitledRule(title, subtitle, body string, borderColor color.Color) string {
	line := lipgloss.NewStyle().Foreground(borderColor)
	dash := "─"

	contentW := lipgloss.Width(body)
	label := ""
	if title != "" {
		label = " " + title + " "
	}
	sub := ""
	if subtitle != "" {
		sub = " " + subtitle + " "
	}
	inner := contentW + 2 // one-cell inset on each side, as in titledBox
	for _, lw := range []int{lipgloss.Width(label), lipgloss.Width(sub)} {
		if lw > inner {
			inner = lw
			contentW = inner - 2
		}
	}
	outer := inner + 2 // the two columns a bordered box would spend on side borders

	const lead = 2 // aligns the title with the body content (blank border col + one-cell inset)
	topRest := outer - lead - lipgloss.Width(label)
	if topRest < 0 {
		topRest = 0
	}
	top := line.Render(strings.Repeat(dash, lead) + label + strings.Repeat(dash, topRest))

	botFill := outer - lead - lipgloss.Width(sub)
	if botFill < 0 {
		botFill = 0
	}
	bottom := line.Render(strings.Repeat(dash, botFill) + sub + strings.Repeat(dash, lead))

	rows := []string{top}
	body = lipgloss.NewStyle().Width(contentW).Render(body)
	for _, ln := range strings.Split(body, "\n") {
		rows = append(rows, "  "+ln+"  ") // blank border col + inset, each side — no glyph
	}
	rows = append(rows, bottom)
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// Every row is padded to the box width so it stays rectangular under centered layout.
func titledBox(b lipgloss.Border, title, subtitle, body string, borderColor color.Color, center bool) string {
	line := lipgloss.NewStyle().Foreground(borderColor)

	contentW := lipgloss.Width(body)
	label := ""
	if title != "" {
		label = " " + title + " "
	}
	sub := ""
	if subtitle != "" {
		sub = " " + subtitle + " "
	}
	inner := contentW + 2 // one-cell inset on each side
	for _, lw := range []int{lipgloss.Width(label), lipgloss.Width(sub)} {
		if lw > inner {
			inner = lw
			contentW = inner - 2
		}
	}
	dashes := inner - lipgloss.Width(label)
	if dashes < 0 {
		dashes = 0
	}

	var top string
	if center {
		left := dashes / 2
		top = b.TopLeft + strings.Repeat(b.Top, left) + label + strings.Repeat(b.Top, dashes-left) + b.TopRight
	} else {
		top = b.TopLeft + label + strings.Repeat(b.Top, dashes) + b.TopRight
	}

	subDashes := inner - lipgloss.Width(sub)
	if subDashes < 0 {
		subDashes = 0
	}
	bottom := b.BottomLeft + strings.Repeat(b.Bottom, subDashes) + sub + b.BottomRight

	rows := []string{line.Render(top)}
	body = lipgloss.NewStyle().Width(contentW).Render(body)
	for _, ln := range strings.Split(body, "\n") {
		rows = append(rows, line.Render(b.Left)+" "+ln+" "+line.Render(b.Right))
	}
	rows = append(rows, line.Render(bottom))
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}
