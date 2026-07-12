package home

import (
	"fmt"
	"image/color"
	"regexp"
	"strings"
	"time"

	"charm.land/bubbles/v2/progress"
	"charm.land/bubbles/v2/table"
	lipgloss "charm.land/lipgloss/v2"
	runewidth "github.com/mattn/go-runewidth"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func tableStyles(d deps.Deps) table.Styles {
	t := d.Styles.Theme
	return table.Styles{
		Header: lipgloss.NewStyle().Bold(true).Foreground(t.Text).Padding(0, 1),
		Cell:   lipgloss.NewStyle().Padding(1, 1, 0, 1), // one blank line above each row, none below
		// The selection highlight is painted per content line in listView (paintRow), so the
		// widget must not band the whole 2-line row block.
		Selected: lipgloss.NewStyle(),
	}
}

// activityColW fits the widest time value
// (example: "12mon")
const activityColW = 6

// projectColumns lays out the columns: Activity/Key fixed, Repository fixed when
// shown, Visibility/Archived sized to their headers, Title taking the rest. Narrow
// terminals drop Repository (see Model.showRepo) since it only holds a placeholder.
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

// glyphColW sizes a status column to its header.
// Wide enough for a fallback word but never narrower than a glyph plus a little room.
func glyphColW(title string) int {
	if w := lipgloss.Width(title); w > 3 {
		return w
	}
	return 3
}

// columnTitles resolves the Activity, Visibility and Archived headers.
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

// roleValue renders the caller's project role, a dash("-") if not a member.
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

// visGlyph is the raw visibility glyph.
// An open eye for public, a crossed eye for private.
func visGlyph(g glyph.Set, v string) string {
	switch strings.ToUpper(v) {
	case "PUBLIC":
		return g.Eye
	case "PRIVATE":
		return g.EyeOff
	}
	return ""
}

// visColor is the accent for a visibility value.
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

func formatDate(t time.Time) string {
	if t.IsZero() {
		return "-"
	}
	return t.Format("2006-01-02")
}

// humanizeSince renders a compact age like "3d" or "12mon", or "-" if no value.
func humanizeSince(t time.Time) string {
	if t.IsZero() {
		return "-"
	}
	d := time.Since(t)
	if d < 0 {
		d = 0
	}
	switch {
	case d < time.Minute:
		return fmt.Sprintf("%ds", int(d.Seconds()))
	case d < time.Hour:
		return fmt.Sprintf("%dm", int(d.Minutes()))
	case d < 24*time.Hour:
		return fmt.Sprintf("%dh", int(d.Hours()))
	case d < 7*24*time.Hour:
		return fmt.Sprintf("%dd", int(d.Hours())/24)
	case d < 30*24*time.Hour:
		return fmt.Sprintf("%dw", int(d.Hours())/(24*7))
	case d < 365*24*time.Hour:
		return fmt.Sprintf("%dmon", int(d.Hours())/(24*30))
	default:
		return fmt.Sprintf("%dyr", int(d.Hours())/(24*365))
	}
}

// csiPattern matches any ANSI CSI sequence: SGR color codes and bubblezone's
// zero-width zone markers (which terminate in 'z'). Stripping both yields plain text.
var csiPattern = regexp.MustCompile("\x1b\\[[0-9;]*[A-Za-z]")

// stripANSI removes all CSI sequences, leaving plain text with its column layout intact.
func stripANSI(s string) string {
	return csiPattern.ReplaceAllString(s, "")
}

// overlayDim splices fg over a dimmed copy of the plain backdrop at (x,y). fg is
// copied in verbatim so its zone markers survive; lipgloss.NewCompositor would drop
// them, since it rebuilds the frame cell by cell.
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

// splitCols splits a plain (ANSI-free) string at visible column c, returning the
// left part padded to exactly c columns and the remainder. A wide rune straddling
// c is replaced with spaces on both sides so the total column count is preserved.
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

// mixColors linearly blends a toward b by t in [0,1] (t=0 is a, t=1 is b).
func mixColors(a, b color.Color, t float64) color.Color {
	ar, ag, ab, _ := a.RGBA()
	br, bg, bb, _ := b.RGBA()
	blend := func(x, y uint32) uint8 {
		return uint8(float64(x>>8)*(1-t) + float64(y>>8)*t)
	}
	return color.RGBA{R: blend(ar, br), G: blend(ag, bg), B: blend(ab, bb), A: 0xff}
}

// statLabelW is the fixed width of a bar row's label column (must fit "MICROTASK").
const statLabelW = 11

// statsBlock renders the project statistics.
// "Issues" divider + KPI band + by state bars + by hierarchy bars
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

// sectionDivider renders "<glyph> Label ─────" with a bold label and muted rule.
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
	// the glyph shares the label's bold color, the rule stays muted
	title := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true).Render(prefix + label)
	return title + s.Muted.Render(" "+strings.Repeat("─", dashes))
}

// statSubheader is a bold group label inside the stats block.
func statSubheader(s theme.Styles, label string) string {
	return lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true).Render(label)
}

// sectionRule is a full-width muted rule.
func sectionRule(s theme.Styles, width int) string {
	if width < 0 {
		width = 0
	}
	return s.Muted.Render(strings.Repeat("─", width))
}

// ruleWithTitle is a full-width horizontal rule with a left-inset title, used as a top border that
// has no sides.
func ruleWithTitle(title string, width int, c color.Color) string {
	label := " " + title + " "
	const lead = 2
	rest := width - lead - lipgloss.Width(label)
	if rest < 0 {
		rest = 0
	}
	return lipgloss.NewStyle().Foreground(c).Render(strings.Repeat("─", lead) + label + strings.Repeat("─", rest))
}

// scrollbarColumn builds a one-cell-per-row scrollbar. It is blank when the content fits.
func scrollbarColumn(off, total, view int, t theme.Theme) []string {
	cells := make([]string, view)
	if total <= view {
		for i := range cells {
			cells[i] = " "
		}
		return cells
	}
	thumb := max(1, view*view/total)
	pos := 0
	if span := total - view; span > 0 {
		pos = off * (view - thumb) / span
	}
	track := lipgloss.NewStyle().Foreground(t.Muted).Render("│")
	head := lipgloss.NewStyle().Foreground(t.Primary).Render("█")
	for i := range cells {
		if i >= pos && i < pos+thumb {
			cells[i] = head
		} else {
			cells[i] = track
		}
	}
	return cells
}

// kpiRows renders the two headline KPI lines. Completion percent is computed here.
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

// statRow is one labelled bar.
// "LABEL <bar> count"
func statRow(s theme.Styles, label string, count, total, barW int, fg color.Color) string {
	labelCell := lipgloss.NewStyle().Foreground(s.Theme.Muted).Width(statLabelW).Render(label)
	countCell := lipgloss.NewStyle().Foreground(s.Theme.Text).Render(fmt.Sprintf("%4d", count))
	return labelCell + statBar(count, total, barW, fg) + " " + countCell
}

// statBar renders a fixed-width solid bar filled to count/total (percentage).
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

// stateColor maps a StateCategory to its bar color.
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

// attentionColor highlights a KPI.
func attentionColor(s theme.Styles, n int) color.Color {
	if n > 0 {
		return s.Theme.Warning
	}
	return s.Theme.Muted
}

// effectiveActivity is the time a row sorts by. It prefers real issue activity and
// falls back to the project's own timestamp, so a fresh project reads as recent.
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
