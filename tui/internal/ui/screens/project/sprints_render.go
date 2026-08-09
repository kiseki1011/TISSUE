package project

import (
	"fmt"
	"image/color"
	"strconv"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const (
	zoneSprintDetail = "project.sprint.detail"
	zoneSprintIssues = "project.sprint.issues"

	// sprint list columns (title flexes to fill the rest).
	colSprintMark   = 2  // a leading marker column: the current (ACTIVE) sprint gets a dot
	colSprintStatus = 10 // "Completed" is the widest status label
	colSprintKey    = 12
	colSprintDue    = 12 // a yyyy-mm-dd date (or "-")
	sprintRowH      = 2  // a blank separator above each row, matching the issue list

	// sprintCurrentMark is the leading dot on the current (ACTIVE) sprint's row - a BMP glyph so it keeps a
	// stable width in both nerd and plain terminals (unlike the Run tab glyph's emoji fallback).
	sprintCurrentMark = "●" // ●
)

func sprintRowZone(i int) string { return "project.sprint.row." + strconv.Itoa(i) }

// sprintsView is the Sprints tab: the sprint list on the left, and for the cursor's sprint its detail
// (top right) over the issues that belong to it (bottom right). An open action (edit form, complete
// confirmation, or start/edit-due calendar) floats over a dimmed copy, mirroring issuesView.
func (m Model) sprintsView() string {
	t := m.deps.Styles.Theme
	base := m.sprintsTab()
	switch {
	case m.sprintEditing:
		form, _, _ := components.ScrollBox(m.sprintEditUI.View(), m.height, m.sprintEditScroll, t.Primary, t.Border)
		over := m.floatOver(base, form)
		if m.dating {
			return m.floatOver(over, m.datePick.View(m.deps.Styles)) // the edit-due calendar floats over the form
		}
		return over
	case m.sprintConfirming:
		return m.floatOver(base, m.sprintConfirmUI.View()) // complete/cancel confirmation
	case m.picking:
		return m.floatOver(base, m.picker.View(m.deps.Styles)) // the remove-issue picker
	case m.dating:
		return m.floatOver(base, m.datePick.View(m.deps.Styles)) // the required start due-date calendar
	}
	return base
}

// sprintsTab mirrors the Issues layout - a list beside a Details region - but splits that region
// vertically 1:1 into the sprint detail and the sprint's issues. On a narrow terminal the region
// stacks under the list instead of sitting beside it.
func (m Model) sprintsTab() string {
	if m.narrow() {
		w := m.innerWidth()
		listH, topH, botH := m.sprintNarrowHeights()
		stack := lipgloss.JoinVertical(lipgloss.Left,
			m.sprintListPanel(w, listH), m.sprintDetailPanel(w, topH), m.sprintIssuesPanel(w, botH))
		return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, stack)
	}
	leftW, rightW := m.sprintColWidths()
	topH := m.height / 2
	right := lipgloss.JoinVertical(lipgloss.Left, m.sprintDetailPanel(rightW, topH), m.sprintIssuesPanel(rightW, m.height-topH))
	content := lipgloss.JoinHorizontal(lipgloss.Top, m.sprintListPanel(leftW, m.height), " ", right)
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, content)
}

// sprintNarrowHeights splits the terminal height into the stacked list / sprint detail / sprint issues
// panels. Each right panel is floored to 3 rows because windowedPanel renders exactly its height only at
// h>=3 (below that it draws its two rules plus a body row, overflowing) - so the stack never spills past
// the terminal at the minimum height.
func (m Model) sprintNarrowHeights() (listH, topH, botH int) {
	half := m.height / 2
	topH = max(3, half/2)
	botH = max(3, half-half/2)
	listH = max(1, m.height-topH-botH)
	return listH, topH, botH
}

// sprintColWidths splits the inner width into the sprint list and the detail-side region (1:1), with a
// one-cell gap between them - mirroring the Issues tab's list:detail split.
func (m Model) sprintColWidths() (list, right int) {
	inner := m.innerWidth()
	list = max(24, inner/2)
	right = max(1, inner-list-1)
	return
}

// sprintRightGeom is the width and the two panel heights of the detail-side region, shared by the
// renderer and the scroll-bound helpers so the panel window and its scrollbar agree.
func (m Model) sprintRightGeom() (w, topH, botH int) {
	if m.narrow() {
		_, topH, botH = m.sprintNarrowHeights()
		return m.innerWidth(), topH, botH
	}
	_, w = m.sprintColWidths()
	topH = m.height / 2
	return w, topH, m.height - topH
}

func (m Model) sprintPanelContentW(w int) int {
	return max(1, w-detailInsetL-detailInsetR-detailScrollbarW)
}

// sprintDetailScrollMax / sprintIssueScrollMax bound the two right panels' scroll offsets, computed
// like detailScrollMax: the rendered body's line count less the panel's visible rows (viewH matches
// windowedPanel's own h-2-detailPadBottom).
func (m Model) sprintDetailScrollMax() int {
	w, topH, _ := m.sprintRightGeom()
	cw := m.sprintPanelContentW(w)
	lines := lipgloss.Height(lipgloss.NewStyle().Width(cw).Render("\n" + m.sprintDetailBody(cw)))
	return max(0, lines-max(1, topH-2-detailPadBottom))
}

func (m Model) sprintIssueScrollMax() int {
	w, _, botH := m.sprintRightGeom()
	cw := m.sprintPanelContentW(w)
	lines := lipgloss.Height(lipgloss.NewStyle().Width(cw).Render("\n" + m.sprintIssuesBody(cw)))
	return max(0, lines-max(1, botH-2-detailPadBottom))
}

func (m Model) sprintListPanel(w, h int) string {
	t := m.deps.Styles.Theme
	bodyW := max(10, w-4)
	body := lipgloss.NewStyle().Width(bodyW).Height(h - 2).MaxHeight(h - 2).Render(m.sprintListBody(bodyW, h-2))
	return zone.Mark("project.sprint.list", components.TitledRule(m.sprintListTitle(), m.sprintListCounter(), body, t.Primary))
}

func (m Model) sprintListTitle() string {
	if m.sprintsLoading {
		return "Sprints (loading)"
	}
	return fmt.Sprintf("Sprints (%d)", m.sprintPage.TotalElements)
}

func (m Model) sprintListCounter() string {
	if m.sprintsLoading || len(m.sprints) == 0 {
		return ""
	}
	return fmt.Sprintf("%d/%d", len(m.sprints), m.sprintPage.TotalElements)
}

func (m Model) sprintListBody(w, avail int) string {
	s := m.deps.Styles
	switch {
	case m.sprintsLoading:
		return s.Muted.Render("Loading sprints…")
	case m.sprintsErr:
		return s.Error.Render("Failed to load sprints. Reopen the tab to retry.")
	case len(m.sprints) == 0:
		return s.Muted.Render("No sprints in this project.")
	}
	titleW := m.sprintTitleWidth(w)
	rows := []string{m.sprintHeaderRow(titleW)}
	visible := max(1, (avail-1)/sprintRowH) // reserve the header row out of the visible budget
	top := listTop(m.sprintCursor, visible, len(m.sprints))
	for j := top; j < len(m.sprints) && j < top+visible; j++ {
		// the blank separator sits OUTSIDE zone.Mark so click/hover still map to the content line
		rows = append(rows, "")
		rows = append(rows, zone.Mark(sprintRowZone(j), m.sprintRow(m.sprints[j], j, titleW, w, m.hover == sprintRowZone(j))))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// sprintTitleWidth gives the Title column all the space left after the fixed columns and their gaps.
func (m Model) sprintTitleWidth(w int) int {
	return max(6, w-colSprintMark-colSprintStatus-colSprintKey-colSprintDue-4) // 4 single-column gaps
}

func (m Model) sprintHeaderRow(titleW int) string {
	head := lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Muted).Bold(true)
	cols := []string{
		pad("", colSprintMark),
		pad("Status", colSprintStatus),
		pad("Key", colSprintKey),
		pad("Title", titleW),
		pad("Due", colSprintDue),
	}
	return head.Render(strings.Join(cols, " "))
}

// sprintRow is one sprint list row: "[mark] Status Key Title Due", banded like the issue rows when
// selected or hovered (plain text so the highlight paints uniformly), coloured per column otherwise. The
// leading mark flags the current (ACTIVE) sprint.
func (m Model) sprintRow(sp domain.SprintSummary, i, titleW, w int, hovered bool) string {
	t := m.deps.Styles.Theme
	sel := i == m.sprintCursor
	banded := sel || hovered
	current := isCurrentSprint(sp)

	markGlyph := " "
	if current {
		markGlyph = sprintCurrentMark
	}
	mark := pad(markGlyph, colSprintMark)
	status := pad(fit(sprintStatusLabel(sp.Status), colSprintStatus), colSprintStatus)
	key := pad(fit(sp.Key, colSprintKey), colSprintKey)
	title := pad(fit(orDash(sp.Title), titleW), titleW)
	due := pad(fit(formatDateOnly(sp.DueAt), colSprintDue), colSprintDue)

	if !banded {
		if current {
			mark = lipgloss.NewStyle().Foreground(t.Accent).Bold(true).Render(mark)
		}
		status = lipgloss.NewStyle().Foreground(sprintStatusColor(t, sp.Status)).Render(status)
		key = lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render(key)
		title = lipgloss.NewStyle().Foreground(t.Text).Render(title)
		due = lipgloss.NewStyle().Foreground(t.Muted).Render(due)
	}

	row := strings.Join([]string{mark, status, key, title, due}, " ")
	switch {
	case sel:
		row = m.selBand().Width(w).Render(row)
	case hovered:
		row = m.hoverBand().Width(w).Render(row)
	}
	return row
}

func (m Model) sprintDetailPanel(w, h int) string {
	return m.windowedPanel("Details", zoneSprintDetail, m.sprintDetailBody, m.sprintDetailScroll, w, h, false)
}

func (m Model) sprintIssuesPanel(w, h int) string {
	return m.windowedPanel(m.sprintIssuesTitle(), zoneSprintIssues, m.sprintIssuesBody, m.sprintIssueScroll, w, h, false)
}

func (m Model) sprintIssuesTitle() string {
	if p, ok := m.sprintIssues[m.selSprintID]; ok {
		return fmt.Sprintf("Issues (%d)", p.TotalElements)
	}
	return "Issues"
}

// sprintDetailBody renders the selected sprint's fields (top-right panel). The goal is a section of its
// own so a full sentence wraps rather than truncating in a value column.
func (m Model) sprintDetailBody(w int) string {
	s := m.deps.Styles
	t := s.Theme
	sp, ok := m.selectedSprint()
	if !ok {
		return s.Muted.Render("No sprint selected.")
	}
	g := m.deps.Glyphs
	labelW := 13
	valueW := max(1, w-labelW)
	row := func(icon, label, value string) string {
		if icon != "" {
			label = icon + "  " + label
		}
		return lipgloss.NewStyle().Foreground(t.Muted).Width(labelW).Render(label) + components.Trunc(value, valueW)
	}
	title := lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(orDash(flattenLine(sp.Title)))
	statusVal := lipgloss.NewStyle().Foreground(sprintStatusColor(t, sp.Status)).Render(sprintStatusLabel(sp.Status))
	if isCurrentSprint(sp) {
		statusVal += lipgloss.NewStyle().Foreground(t.Accent).Bold(true).Render("  " + sprintCurrentMark + " current")
	}
	key := lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render(orDash(sp.Key))

	rows := []string{
		title, "",
		row(g.Or(g.Key, ""), "Key", key),
		row(g.Or(g.Flag, ""), "Status", statusVal),
		row(g.Or(g.Clock, ""), "Started", formatLocalDay(sp.StartedAt)),
		row(g.Or(g.Calendar, ""), "Due", formatDateOnly(sp.DueAt)),
	}
	if !sp.CompletedAt.IsZero() {
		rows = append(rows, row(g.Or(g.Check, ""), "Completed", formatLocalDay(sp.CompletedAt)))
	}
	rows = append(rows, "", sectionRule(s, "Goal", w), "", m.sprintGoalBlock(sp.Goal, w))
	rows = append(rows, "", sectionRule(s, "Report", w), "", m.sprintReportBlock(sp.ID, w))
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// sprintReportBlock renders the selected sprint's completion summary - done %, the issue split (total /
// done / carried, and aborted when any) and story-point progress - loaded in lockstep with the issue list.
// A category breakdown is intentionally omitted: "carried" already is the still-open work and the issue
// list below shows the per-state detail.
func (m Model) sprintReportBlock(sprintID int64, w int) string {
	s := m.deps.Styles
	t := s.Theme
	// A failed load wins over any cached report: once an issue mutation re-arms the lockstep refresh, a
	// still-cached report predates the change and would contradict the fresh issue list beside it, so show
	// "unavailable" rather than stale numbers. An in-flight refresh keeps failed=false, so SWR still shows
	// the prior report while it revalidates.
	if m.sprintReportFailed[sprintID] {
		return s.Muted.Render("Report unavailable")
	}
	r, cached := m.sprintReport[sprintID]
	if !cached {
		return s.Muted.Render("Loading report…")
	}
	muted := lipgloss.NewStyle().Foreground(t.Muted)
	const labelW = 8
	barW := max(8, min(24, w-labelW-6))
	pct := int(r.CompletionRate*100 + 0.5)
	done := muted.Width(labelW).Render("Done") +
		lipgloss.NewStyle().Foreground(t.Success).Bold(true).Render(fmt.Sprintf("%3d%% ", pct)) +
		statBar(r.CompletedIssues, r.TotalIssues, barW, t.Success, t.Border)

	parts := []string{
		fmt.Sprintf("%d total", r.TotalIssues),
		fmt.Sprintf("%d done", r.CompletedIssues),
		fmt.Sprintf("%d carried", r.OpenIssues),
	}
	if aborted := r.TotalIssues - r.CompletedIssues - r.OpenIssues; aborted > 0 {
		parts = append(parts, fmt.Sprintf("%d aborted", aborted))
	}
	issues := muted.Width(labelW).Render("Issues") + muted.Render(strings.Join(parts, " · "))

	rows := []string{done, issues}
	if r.TotalStoryPoints > 0 {
		ppct := int(r.PointsCompletionRate*100 + 0.5)
		rows = append(rows, muted.Width(labelW).Render("Points")+
			muted.Render(fmt.Sprintf("%d committed · %d done (%d%%)", r.TotalStoryPoints, r.CompletedStoryPoints, ppct)))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// sprintGoalBlock renders the sprint goal as wrapped plain text, or a muted placeholder when empty. The
// text is sanitized so a stray control character cannot corrupt the frame, like the issue content block.
func (m Model) sprintGoalBlock(goal string, w int) string {
	if strings.TrimSpace(goal) == "" {
		return m.deps.Styles.Muted.Render("No goal set.")
	}
	return lipgloss.NewStyle().Width(w).Render(sanitizeBlock(goal))
}

// sprintIssuesBody renders the issues belonging to the selected sprint (bottom-right panel): a skeleton
// note while they load, an error note on failure, or the compact issue rows (with a "… N more" note
// when the sprint holds more than the loaded page).
func (m Model) sprintIssuesBody(w int) string {
	s := m.deps.Styles
	if m.selSprintID == 0 {
		return s.Muted.Render("No sprint selected.")
	}
	if p, ok := m.sprintIssues[m.selSprintID]; ok {
		if len(p.Issues) == 0 {
			return s.Muted.Render("No issues in this sprint.")
		}
		rows := make([]string, 0, len(p.Issues)*2)
		for i, it := range p.Issues {
			if i > 0 {
				rows = append(rows, "")
			}
			rows = append(rows, m.sprintIssueRow(it, w))
		}
		if p.HasNext {
			rows = append(rows, "", s.Muted.Render(fmt.Sprintf("… %d more", p.TotalElements-len(p.Issues))))
		}
		return lipgloss.JoinVertical(lipgloss.Left, rows...)
	}
	if m.sprintIssuesFailed[m.selSprintID] {
		return s.Error.Render("Failed to load this sprint's issues.")
	}
	return s.Muted.Render("Loading issues…")
}

// sprintIssueRow is one issue in the sprint's issue panel: "Pri Key Title State", read-only (no cursor
// banding - this panel is not a focusable list yet), coloured per column.
func (m Model) sprintIssueRow(it domain.IssueSummary, w int) string {
	t := m.deps.Styles.Theme
	pri := lipgloss.NewStyle().Foreground(priorityColor(t, it.Priority)).Render(pad(orDash(it.Priority), 3))
	key := lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render(pad(fit(it.Key, colKey), colKey))
	state := lipgloss.NewStyle().Foreground(stateColor(t, it.StateCategory)).Render(fit(orDash(it.StateLabel), colStatus))
	titleW := max(6, w-3-colKey-colStatus-3) // pri(3) + key + state, with three single-cell gaps
	title := lipgloss.NewStyle().Foreground(t.Text).Render(fit(orDash(it.Title), titleW))
	return pri + " " + key + " " + title + " " + state
}

// sprintStatusLabel is the display form of a sprint status enum (PLANNING/ACTIVE/COMPLETED/CANCELLED).
func sprintStatusLabel(status string) string {
	switch status {
	case "PLANNING":
		return "Planning"
	case "ACTIVE":
		return "Active"
	case "COMPLETED":
		return "Completed"
	case "CANCELLED":
		return "Cancelled"
	}
	return orDash(status)
}

// sprintStatusColor colours a sprint status: active primary, completed green, cancelled red, planning
// muted - so the list and detail read the lifecycle at a glance.
func sprintStatusColor(t theme.Theme, status string) color.Color {
	switch status {
	case "ACTIVE":
		return t.Primary
	case "COMPLETED":
		return t.Success
	case "CANCELLED":
		return t.Error
	default: // PLANNING or unknown
		return t.Muted
	}
}
