// Package components holds small shared render helpers used across screens.
package components

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
)

// TitledBox draws a rounded border around body with an optional title spliced into the top edge,
// since lipgloss has no native border title. The title sits left-inset.
func TitledBox(title, body string, borderColor color.Color) string {
	return titledBox(title, "", body, borderColor, false)
}

// TitledBoxSub is TitledBox with a subtitle spliced into the bottom edge, right.
// Useful for a status readout such as a scroll position.
func TitledBoxSub(title, subtitle, body string, borderColor color.Color) string {
	return titledBox(title, subtitle, body, borderColor, false)
}

// TitledBoxCentered is TitledBox with the title centered on the top edge.
func TitledBoxCentered(title, body string, borderColor color.Color) string {
	return titledBox(title, "", body, borderColor, true)
}

// The box fits the widest body line plus a one-cell inset on each side, and every row is padded so
// the box stays rectangular under centered layout.
func titledBox(title, subtitle, body string, borderColor color.Color, center bool) string {
	b := lipgloss.RoundedBorder()
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
