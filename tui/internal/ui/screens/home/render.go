package home

import (
	"fmt"
	"image/color"
	"strings"
	"time"

	"charm.land/bubbles/v2/progress"
	"charm.land/bubbles/v2/table"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func tableStyles(d deps.Deps) table.Styles {
	t := d.Styles.Theme
	return table.Styles{
		Header: lipgloss.NewStyle().Bold(true).Foreground(t.Text).Padding(0, 1),
		Cell:   lipgloss.NewStyle().Padding(1, 1, 0, 1), // one blank line above each row, none below
		// listView paints the highlight per content line, so the widget must not band the row block.
		Selected: lipgloss.NewStyle(),
	}
}

// activityColW fits the widest time value (example: "12mon").
const activityColW = 6

// Narrow terminals drop Repository (see Model.showRepo) since it only holds a placeholder.
func projectColumns(tableW int, actTitle, visTitle, archTitle string, showRepo bool) []table.Column {
	const keyW, repoW = 10, 21
	visW, archW := glyphColW(visTitle), glyphColW(archTitle)
	// per-column cell padding is two cells: six columns with Repository, five without.
	ncols, fixed := 6, activityColW+keyW+repoW+visW+archW
	if !showRepo {
		ncols, fixed = 5, activityColW+keyW+visW+archW
	}
	titleW := tableW - 2*ncols - fixed
	if titleW < 8 {
		titleW = 8
	}
	cols := []table.Column{
		{Title: actTitle, Width: activityColW},
		{Title: "Key", Width: keyW},
		{Title: "Title", Width: titleW},
	}
	if showRepo {
		cols = append(cols, table.Column{Title: "Repository", Width: repoW})
	}
	return append(cols,
		table.Column{Title: visTitle, Width: visW},
		table.Column{Title: archTitle, Width: archW},
	)
}

// Wide enough for a fallback word but never narrower than a glyph plus a little room.
func glyphColW(title string) int {
	if w := lipgloss.Width(title); w > 3 {
		return w
	}
	return 3
}

// A glyph in nerd mode, a plain fallback otherwise.
func columnTitles(g glyph.Set) (act, vis, arch string) {
	return g.Or(g.LastUpdated, "◷"), g.Or(g.Eye, "Visibility"), g.Or(g.Cabinet, "Archived")
}

func matchesQuery(p domain.Project, query string) bool {
	return strings.Contains(strings.ToLower(p.Key), query) ||
		strings.Contains(strings.ToLower(p.Title), query)
}

func detailRow(s theme.Styles, label, value string) string {
	return lipgloss.NewStyle().Foreground(s.Theme.Muted).Width(17).Render(label) + value
}

func roleLabel(v string) string {
	switch strings.ToUpper(v) {
	case "MEMBER":
		return "Member"
	case "MANAGER":
		return "Manager"
	default:
		return "-"
	}
}

func roleValue(s theme.Styles, v string) string {
	if v == "" {
		return s.Muted.Render("-")
	}
	return roleLabel(v)
}

func visibilityLabel(v string) string {
	switch strings.ToUpper(v) {
	case "PUBLIC":
		return "Public"
	case "PRIVATE":
		return "Private"
	case "":
		return "-"
	default:
		return v
	}
}

func visGlyph(g glyph.Set, v string) string {
	switch strings.ToUpper(v) {
	case "PUBLIC":
		return g.Eye
	case "PRIVATE":
		return g.EyeOff
	}
	return ""
}

func visColor(s theme.Styles, v string) color.Color {
	switch strings.ToUpper(v) {
	case "PUBLIC":
		return s.Theme.Success
	case "PRIVATE":
		return s.Theme.Warning
	}
	return s.Theme.Text
}

func yesNo(b bool) string {
	if b {
		return "Yes"
	}
	return "No"
}

// formatDate renders the day in the viewer's timezone. The server serializes instants as UTC, so a
// raw format would show UTC's day.
func formatDate(t time.Time) string {
	if t.IsZero() {
		return "-"
	}
	return t.Local().Format("2006-01-02")
}

// Thin wrappers over components so this package's call sites stay unqualified.

func stripANSI(s string) string { return components.StripANSI(s) }

func overlayDim(backdrop, fg string, x, y int, dim color.Color) string {
	return components.OverlayDim(backdrop, fg, x, y, dim)
}

func mixColors(a, b color.Color, t float64) color.Color { return components.MixColors(a, b, t) }

// statLabelW is the fixed width of a bar row's label column (must fit "MICROTASK").
const statLabelW = 11

func statsBlock(s theme.Styles, g glyph.Set, st domain.ProjectStats, contentW int) []string {
	usable := contentW
	// usable minus the label + count + spacing around the bar
	barW := usable - statLabelW - 5
	if barW < 6 {
		barW = 6
	}
	if barW > 22 {
		barW = 22
	}

	rows := []string{sectionDivider(s, g.Or(g.Project, ""), "Issues", usable), ""}
	rows = append(rows, kpiRows(s, st)...)
	rows = append(rows, "", statSubheader(s, "By State"))
	for _, b := range st.ByState {
		rows = append(rows, statRow(s, b.Label, b.Count, st.Total, barW, stateColor(s, b.Label)))
	}
	rows = append(rows, "", statSubheader(s, "By Hierarchy"))
	for _, b := range st.ByHierarchy {
		rows = append(rows, statRow(s, b.Label, b.Count, st.Total, barW, s.Theme.Primary))
	}
	return rows
}

func sectionDivider(s theme.Styles, icon, label string, width int) string {
	prefix := ""
	if icon != "" {
		prefix = icon + "  "
	}
	head := prefix + label + " " // plain form for width math
	dashes := width - lipgloss.Width(head) - 1
	if dashes < 0 {
		dashes = 0
	}
	title := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true).Render(prefix + label)
	return title + s.Muted.Render(" "+strings.Repeat("─", dashes))
}

func statSubheader(s theme.Styles, label string) string {
	return lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true).Render(label)
}

func sectionRule(s theme.Styles, width int) string {
	if width < 0 {
		width = 0
	}
	return s.Muted.Render(strings.Repeat("─", width))
}

// Focus is signalled by the rule COLOUR (c), not weight.
func ruleWithTitle(title string, width int, c color.Color) string {
	return components.RuleWithTitle(title, width, c)
}

func scrollbarColumn(off, total, view int, t theme.Theme) []string {
	return components.ScrollbarColumn(off, total, view, t.Primary, t.Muted)
}

func kpiRows(s theme.Styles, st domain.ProjectStats) []string {
	pct := 0
	if st.Total > 0 {
		pct = (st.Completed*100 + st.Total/2) / st.Total
	}
	kpi := func(label, value string, c color.Color) string {
		return lipgloss.NewStyle().Foreground(s.Theme.Muted).Render(label+" ") +
			lipgloss.NewStyle().Foreground(c).Bold(true).Render(value)
	}
	line1 := strings.Join([]string{
		kpi("Total", fmt.Sprintf("%d", st.Total), s.Theme.Text),
		kpi("Open", fmt.Sprintf("%d", st.Open), s.Theme.Accent),
		kpi("Done", fmt.Sprintf("%d%%", pct), s.Theme.Success),
	}, "  ")
	line2 := strings.Join([]string{
		kpi("Unassigned", fmt.Sprintf("%d", st.Unassigned), attentionColor(s, st.Unassigned)),
		kpi("Overdue", fmt.Sprintf("%d", st.Overdue), attentionColor(s, st.Overdue)),
	}, "  ")
	return []string{line1, line2}
}

func statRow(s theme.Styles, label string, count, total, barW int, fg color.Color) string {
	labelCell := lipgloss.NewStyle().Foreground(s.Theme.Muted).Width(statLabelW).Render(label)
	countCell := lipgloss.NewStyle().Foreground(s.Theme.Text).Render(fmt.Sprintf("%4d", count))
	return labelCell + statBar(count, total, barW, fg) + " " + countCell
}

func statBar(count, total, width int, fg color.Color) string {
	frac := 0.0
	if total > 0 {
		frac = float64(count) / float64(total)
	}
	bar := progress.New(
		progress.WithWidth(width),
		progress.WithoutPercentage(),
		progress.WithFillCharacters('█', '░'),
		progress.WithColors(fg),
	)
	return bar.ViewAs(frac)
}

func stateColor(s theme.Styles, v string) color.Color {
	switch strings.ToUpper(v) {
	case "INITIAL":
		return s.Theme.Warning
	case "ACTIVE":
		return s.Theme.Accent
	case "COMPLETED":
		return s.Theme.Success
	case "ABORTED":
		return s.Theme.Muted
	}
	return s.Theme.Text
}

func attentionColor(s theme.Styles, n int) color.Color {
	if n > 0 {
		return s.Theme.Warning
	}
	return s.Theme.Muted
}

// effectiveActivity falls back to the project's own timestamp so a fresh project reads as recent.
func effectiveActivity(p domain.Project) time.Time {
	switch {
	case !p.LastActivity.IsZero():
		return p.LastActivity
	case !p.UpdatedAt.IsZero():
		return p.UpdatedAt
	default:
		return p.CreatedAt
	}
}
