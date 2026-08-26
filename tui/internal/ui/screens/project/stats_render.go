package project

import (
	"fmt"
	"image/color"
	"strings"
	"time"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const zoneStatsPanel = "project.stats.panel"

// statLabelW is the fixed width of a bar row's label column (fits "Microtask").
const statLabelW = 11

// statsView is the Stats tab: a single centered panel of the project's issue statistics - headline KPIs
// plus distribution bars by workflow state, priority and hierarchy. It is read-only, so nothing floats
// over it (unlike the Config tab's edit form).
func (m Model) statsView() string {
	panel := m.statsPanel(m.statsWidth(), m.height)
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, panel)
}

// statsWidth uses the full inner width so the panel fills the tab horizontally. The bars themselves stay
// readable regardless: statBarWidth clamps each bar's run, so only the rules and layout span the width.
func (m Model) statsWidth() int { return m.innerWidth() }

func (m Model) statsPanel(w, h int) string {
	return m.windowedPanel("Statistics", zoneStatsPanel, m.statsBody, m.statsScroll, w, h, false)
}

func (m Model) statsScrollMax() int {
	cw := m.sprintPanelContentW(m.statsWidth())
	lines := lipgloss.Height(lipgloss.NewStyle().Width(cw).Render("\n" + m.statsBody(cw)))
	return max(0, lines-max(1, m.height-2-detailPadBottom))
}

// statsBody renders the statistics overview: a full-width KPI banner, then a 1:1 split of a snapshot
// column (issue distributions + aging - "how the project looks now") and a trend column (velocity + flow
// + cycle/lead - "how it moves over time"). On a terminal too narrow for two readable columns the two
// stacks fall back to one column. A brand-new project still shows the KPIs, with each section reading
// "None".
func (m Model) statsBody(w int) string {
	s := m.deps.Styles
	switch {
	case !m.statsLoaded && !m.statsErr:
		return s.Muted.Render("Loading statistics…")
	case m.statsErr:
		return s.Error.Render("Failed to load the statistics. Reopen the tab to retry.")
	}
	header := lipgloss.JoinVertical(lipgloss.Left, m.statsKPIRows()...)

	const gap = 3
	colW := (w - gap) / 2
	if colW < 34 {
		// too narrow for two columns: stack the snapshot then the trend sections in one column
		rows := append([]string{header, ""}, m.statsSnapshotRows(w)...)
		rows = append(rows, "")
		rows = append(rows, m.statsTrendRows(w)...)
		return lipgloss.JoinVertical(lipgloss.Left, rows...)
	}
	left := lipgloss.NewStyle().Width(colW).Render(lipgloss.JoinVertical(lipgloss.Left, m.statsSnapshotRows(colW)...))
	right := lipgloss.NewStyle().Width(colW).Render(lipgloss.JoinVertical(lipgloss.Left, m.statsTrendRows(colW)...))
	cols := lipgloss.JoinHorizontal(lipgloss.Top, left, strings.Repeat(" ", gap), right)
	return lipgloss.JoinVertical(lipgloss.Left, header, "", cols)
}

// statsSnapshotRows is the left column: the issue distributions (state, priority, hierarchy) and the aging
// buckets - a picture of the project's current shape. Bars are sized to the column width.
func (m Model) statsSnapshotRows(w int) []string {
	s := m.deps.Styles
	t := s.Theme
	st := m.stats
	barW := statBarWidth(w)
	rows := []string{sectionRule(s, "By State", w)}
	rows = append(rows, m.statBucketRows(st.ByState, st.Total, barW, titleCaseEnum, func(b domain.StatBucket) color.Color {
		return stateColor(t, b.Label)
	})...)
	rows = append(rows, "", sectionRule(s, "By Priority", w))
	rows = append(rows, m.statBucketRows(st.ByPriority, st.Total, barW, keepLabel, func(b domain.StatBucket) color.Color {
		return priorityColor(t, b.Label)
	})...)
	rows = append(rows, "", sectionRule(s, "By Hierarchy", w))
	rows = append(rows, m.statBucketRows(st.ByHierarchy, st.Total, barW, titleCaseEnum, func(domain.StatBucket) color.Color {
		return t.Primary
	})...)
	if m.agingOK {
		rows = append(rows, "", sectionRule(s, "Aging (open)", w))
		rows = append(rows, m.agingRows(barW)...)
	}
	return rows
}

// statsTrendRows is the right column: velocity (delivered points per sprint), then cycle/lead time and the
// created-vs-resolved flow - how the project moves over time. Each section is gated on its data loading; a
// leading blank separates sections only once there is something above them.
func (m Model) statsTrendRows(w int) []string {
	s := m.deps.Styles
	var rows []string
	sec := func(title string, body []string) {
		if len(rows) > 0 {
			rows = append(rows, "")
		}
		rows = append(rows, sectionRule(s, title, w))
		rows = append(rows, body...)
	}
	if m.velocOK {
		sec("Velocity", m.velocityRows(w))
	}
	if m.cycleOK {
		sec("Cycle & Lead (30d)", m.cycleTimeRows())
	}
	if m.flowOK {
		sec("Flow (30d)", m.flowRows())
	}
	if len(rows) == 0 {
		rows = []string{s.Muted.Render("No trend data")}
	}
	return rows
}

// velocityRows renders the delivered story points of the last few COMPLETED sprints as proportional bars
// (scaled to the busiest shown sprint), headed by the mean - the basis for planning the next commitment.
func (m Model) velocityRows(w int) []string {
	s := m.deps.Styles
	t := s.Theme
	v := m.velocity
	muted := lipgloss.NewStyle().Foreground(t.Muted)
	if len(v.Sprints) == 0 {
		return []string{muted.Render("No completed sprints yet")}
	}
	noun := "sprints"
	if len(v.Sprints) == 1 {
		noun = "sprint"
	}
	avg := muted.Render("Avg ") +
		lipgloss.NewStyle().Foreground(t.Success).Bold(true).Render(fmt.Sprintf("%.1f pts", v.AverageStoryPoints)) +
		muted.Render(fmt.Sprintf("  · %d %s", len(v.Sprints), noun))
	rows := []string{avg, ""}

	const showN = 8
	shown := v.Sprints
	if len(shown) > showN {
		shown = shown[len(shown)-showN:] // the most recent sprints, oldest of them first
	}
	peak := 0
	for _, sp := range shown {
		peak = max(peak, sp.CompletedStoryPoints)
	}
	barW := statBarWidth(w)
	for _, sp := range shown {
		labelCell := muted.Width(statLabelW).Render(sp.SprintKey)
		countCell := lipgloss.NewStyle().Foreground(t.Text).Render(fmt.Sprintf("%4d", sp.CompletedStoryPoints))
		rows = append(rows, labelCell+" "+statBar(sp.CompletedStoryPoints, peak, barW, t.Accent, t.Border)+" "+countCell)
	}
	return rows
}

// agingRows renders the open-issue age buckets as proportional bars (warmer as work sits longer), plus a
// blocked-count line that lights up only when something is actually blocked.
func (m Model) agingRows(barW int) []string {
	s := m.deps.Styles
	t := s.Theme
	a := m.aging
	if a.OpenTotal == 0 {
		return []string{s.Muted.Render("No open issues")}
	}
	rows := []string{
		statRow(s, "< 3d", a.Under3d, a.OpenTotal, barW, t.Success),
		statRow(s, "3-7d", a.Days3to7, a.OpenTotal, barW, t.Text),
		statRow(s, "1-2w", a.Weeks1to2, a.OpenTotal, barW, t.Warning),
		statRow(s, "> 2w", a.Over2w, a.OpenTotal, barW, t.Error),
	}
	blockedFg := t.Muted
	if a.Blocked > 0 {
		blockedFg = t.Error
	}
	blockedLine := lipgloss.NewStyle().Foreground(t.Muted).Render("Blocked ") +
		lipgloss.NewStyle().Foreground(blockedFg).Bold(true).Render(fmt.Sprintf("%d", a.Blocked))
	if a.Blocked > 0 && a.OpenTotal > 0 {
		blockedLine += lipgloss.NewStyle().Foreground(t.Muted).
			Render(fmt.Sprintf(" (%d%%)", (a.Blocked*100+a.OpenTotal/2)/a.OpenTotal))
	}
	rows = append(rows, blockedLine)
	return rows
}

// cycleTimeRows renders cycle and lead time as one line each: average, p50 and p90 as human durations with
// the sample size. A window with no resolved issues reads "no data" rather than a row of zeros.
func (m Model) cycleTimeRows() []string {
	t := m.deps.Styles.Theme
	muted := lipgloss.NewStyle().Foreground(t.Muted)
	line := func(name string, d domain.DurationStat, fg color.Color) string {
		label := muted.Width(statLabelW).Render(name)
		if d.Count == 0 {
			return label + " " + muted.Render("no data")
		}
		val := lipgloss.NewStyle().Foreground(fg).Bold(true)
		return label + " " +
			muted.Render("avg ") + val.Render(humanizeDur(d.AvgSeconds)) + "  " +
			muted.Render("p50 ") + val.Render(humanizeDur(d.P50Seconds)) + "  " +
			muted.Render("p90 ") + val.Render(humanizeDur(d.P90Seconds)) + "  " +
			muted.Render(fmt.Sprintf("(n=%d)", d.Count))
	}
	return []string{
		line("Cycle", m.cycle.Cycle, t.Accent),
		line("Lead", m.cycle.Lead, t.Success),
	}
}

// flowRows renders the created and resolved daily series as two sparklines scaled to a shared peak, each
// with its window total, so the two trends are directly comparable.
func (m Model) flowRows() []string {
	s := m.deps.Styles
	t := s.Theme
	days := m.flow.Days
	if len(days) == 0 {
		return []string{s.Muted.Render("No activity in the window")}
	}
	created := make([]int, len(days))
	resolved := make([]int, len(days))
	sumC, sumR, peak := 0, 0, 0
	for i, d := range days {
		created[i], resolved[i] = d.Created, d.Resolved
		sumC += d.Created
		sumR += d.Resolved
		peak = max(peak, max(d.Created, d.Resolved))
	}
	muted := lipgloss.NewStyle().Foreground(t.Muted)
	row := func(name string, vals []int, sum int, fg color.Color) string {
		return muted.Width(statLabelW).Render(name) + " " +
			lipgloss.NewStyle().Foreground(fg).Render(sparkline(vals, peak)) +
			muted.Render(fmt.Sprintf("  Σ%d", sum))
	}
	return []string{
		row("Created", created, sumC, t.Accent),
		row("Resolved", resolved, sumR, t.Success),
	}
}

var sparkRunes = []rune("▁▂▃▄▅▆▇█")

// sparkline maps each value to a block rune scaled against a shared peak, so a run of values renders as a
// compact one-line trend. Zero (and a zero peak) render as the lowest block, keeping the width stable.
func sparkline(vals []int, peak int) string {
	var b strings.Builder
	for _, v := range vals {
		idx := 0
		if peak > 0 && v > 0 {
			idx = (v*(len(sparkRunes)-1) + peak/2) / peak
			if idx >= len(sparkRunes) {
				idx = len(sparkRunes) - 1
			}
		}
		b.WriteRune(sparkRunes[idx])
	}
	return b.String()
}

// humanizeDur formats a second count as a compact duration ("2d 3h", "5h 20m", "45m"), rounding to the two
// most significant units. Non-positive input reads "0m".
func humanizeDur(seconds int) string {
	if seconds <= 0 {
		return "0m"
	}
	d := time.Duration(seconds) * time.Second
	days := int(d.Hours()) / 24
	hours := int(d.Hours()) % 24
	mins := int(d.Minutes()) % 60
	switch {
	case days > 0:
		if hours > 0 {
			return fmt.Sprintf("%dd %dh", days, hours)
		}
		return fmt.Sprintf("%dd", days)
	case hours > 0:
		if mins > 0 {
			return fmt.Sprintf("%dh %dm", hours, mins)
		}
		return fmt.Sprintf("%dh", hours)
	default:
		return fmt.Sprintf("%dm", max(1, mins))
	}
}

// statsKPIRows is the headline: totals and completion on the first line, the attention counts (unassigned,
// overdue) on the second, coloured like the dashboard's stats panel.
func (m Model) statsKPIRows() []string {
	t := m.deps.Styles.Theme
	st := m.stats
	pct := 0
	if st.Total > 0 {
		pct = (st.Completed*100 + st.Total/2) / st.Total // rounded percent complete
	}
	kpi := func(label, value string, c color.Color) string {
		return lipgloss.NewStyle().Foreground(t.Muted).Render(label+" ") +
			lipgloss.NewStyle().Foreground(c).Bold(true).Render(value)
	}
	line1 := strings.Join([]string{
		kpi("Total", fmt.Sprintf("%d", st.Total), t.Text),
		kpi("Open", fmt.Sprintf("%d", st.Open), t.Accent),
		kpi("Done", fmt.Sprintf("%d%%", pct), t.Success),
	}, "   ")
	line2 := strings.Join([]string{
		kpi("Completed", fmt.Sprintf("%d", st.Completed), t.Success),
		kpi("Unassigned", withPct(st.Unassigned, st.Total), attention(t, st.Unassigned)),
		kpi("Overdue", withPct(st.Overdue, st.Open), attention(t, st.Overdue)),
	}, "   ")
	return []string{line1, line2}
}

// withPct renders a count with its share of a denominator ("3 (25%)"), omitting the percent when the count
// is zero (a calm "0") or the denominator is empty (no meaningful share).
func withPct(n, denom int) string {
	if n == 0 || denom <= 0 {
		return fmt.Sprintf("%d", n)
	}
	return fmt.Sprintf("%d (%d%%)", n, (n*100+denom/2)/denom)
}

// statBucketRows renders one bar per bucket, or a single muted "None" when the grouping is empty.
func (m Model) statBucketRows(buckets []domain.StatBucket, total, barW int, label func(string) string, colorOf func(domain.StatBucket) color.Color) []string {
	s := m.deps.Styles
	if len(buckets) == 0 {
		return []string{s.Muted.Render("None")}
	}
	out := make([]string, 0, len(buckets))
	for _, b := range buckets {
		out = append(out, statRow(s, label(b.Label), b.Count, total, barW, colorOf(b)))
	}
	return out
}

// statBarWidth sizes the bar to the panel width, leaving room for the label column and the right-hand
// count cell, and clamped so it stays readable on both narrow and very wide terminals.
func statBarWidth(contentW int) int {
	barW := contentW - statLabelW - 7 // label cell + a gap on each side + the 4-wide count cell
	if barW < 6 {
		barW = 6
	}
	if barW > 32 {
		barW = 32
	}
	return barW
}

func statRow(s theme.Styles, label string, count, total, barW int, fg color.Color) string {
	t := s.Theme
	labelCell := lipgloss.NewStyle().Foreground(t.Muted).Width(statLabelW).Render(label)
	countCell := lipgloss.NewStyle().Foreground(t.Text).Render(fmt.Sprintf("%4d", count))
	return labelCell + " " + statBar(count, total, barW, fg, t.Border) + " " + countCell
}

// statBar draws a proportional bar: the filled run in fg over a muted track. An empty (zero) bar is all
// track, so every row keeps the same width and the columns stay aligned.
func statBar(count, total, width int, fg, track color.Color) string {
	if width < 1 {
		width = 1
	}
	filled := 0
	if total > 0 {
		filled = int(float64(count)/float64(total)*float64(width) + 0.5)
	}
	if filled > width {
		filled = width
	}
	if filled < 0 {
		filled = 0
	}
	on := lipgloss.NewStyle().Foreground(fg).Render(strings.Repeat("█", filled))
	off := lipgloss.NewStyle().Foreground(track).Render(strings.Repeat("░", width-filled))
	return on + off
}

// attention colours a count that wants the eye when non-zero (unassigned or overdue issues), and stays
// muted at zero so a clean project reads as calm.
func attention(t theme.Theme, n int) color.Color {
	if n > 0 {
		return t.Warning
	}
	return t.Muted
}

func keepLabel(s string) string { return s }

// titleCaseEnum turns an ALL-CAPS enum label into a friendly one ("INITIAL" -> "Initial", "MICROTASK" ->
// "Microtask"). An empty label passes through unchanged.
func titleCaseEnum(s string) string {
	if s == "" {
		return s
	}
	return strings.ToUpper(s[:1]) + strings.ToLower(s[1:])
}
