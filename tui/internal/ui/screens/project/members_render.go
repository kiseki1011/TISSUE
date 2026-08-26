package project

import (
	"fmt"
	"image/color"
	"strconv"
	"strings"
	"time"

	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const (
	zoneMemberDetail = "project.member.detail"

	colMemberRole = 8 // "Manager" is the widest role label
	memberRowH    = 2 // a blank separator above each row, matching the issue/sprint lists
)

func memberRowZone(i int) string { return "project.member.row." + strconv.Itoa(i) }

// membersView is the Members tab: roster left, Details for the cursor's member right. An open picker or
// confirmation floats over a dimmed copy, mirroring issuesView.
func (m Model) membersView() string {
	base := m.membersTab()
	switch {
	case m.picking:
		return m.floatOver(base, m.picker.View(m.deps.Styles)) // the add-member or set-role picker
	case m.memberConfirming:
		return m.floatOver(base, m.memberConfirmUI.View()) // the remove-member confirmation
	}
	return base
}

func (m Model) membersTab() string {
	if m.narrow() {
		w := m.innerWidth()
		listH := m.height / 2
		return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, lipgloss.JoinVertical(lipgloss.Left,
			m.memberListPanel(w, listH), m.memberDetailPanel(w, m.height-listH)))
	}
	leftW, rightW := m.memberColWidths()
	content := lipgloss.JoinHorizontal(lipgloss.Top,
		m.memberListPanel(leftW, m.height), " ", m.memberDetailPanel(rightW, m.height))
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, content)
}

// memberColWidths splits the inner width 1:1 with a one-cell gap, mirroring the Issues/Sprints tabs.
func (m Model) memberColWidths() (list, right int) {
	inner := m.innerWidth()
	list = max(24, inner/2)
	right = max(1, inner-list-1)
	return
}

// memberRightGeom is the Details panel geometry, shared so the panel window and its scrollbar agree.
func (m Model) memberRightGeom() (w, h int) {
	if m.narrow() {
		return m.innerWidth(), m.height - m.height/2
	}
	_, w = m.memberColWidths()
	return w, m.height
}

func (m Model) memberDetailScrollMax() int {
	w, h := m.memberRightGeom()
	cw := m.sprintPanelContentW(w)
	lines := lipgloss.Height(lipgloss.NewStyle().Width(cw).Render("\n" + m.memberDetailBody(cw)))
	return max(0, lines-max(1, h-2-detailPadBottom))
}

func (m Model) memberListPanel(w, h int) string {
	t := m.deps.Styles.Theme
	bodyW := max(10, w-4)
	inner := max(8, bodyW-2) // one column of breathing room inside the list on each side
	body := lipgloss.NewStyle().Width(bodyW).Height(h-2).MaxHeight(h-2).Padding(0, 1).
		Render(m.memberListBody(inner, h-2))
	return zone.Mark("project.member.list", components.TitledRule(m.memberListTitle(), "", body, t.Primary))
}

func (m Model) memberListTitle() string {
	if !m.membersLoaded && !m.membersErr {
		return "Members (loading)"
	}
	return fmt.Sprintf("Members (%d)", len(m.members))
}

func (m Model) memberListBody(w, avail int) string {
	s := m.deps.Styles
	switch {
	case !m.membersLoaded && !m.membersErr:
		return s.Muted.Render("Loading members…")
	case m.membersErr:
		return s.Error.Render("Failed to load members. Reopen the tab to retry.")
	case len(m.members) == 0:
		return s.Muted.Render("No members in this project.")
	}
	nameW := max(6, w-colMemberRole-1)
	rows := []string{m.memberHeaderRow(nameW)}
	visible := max(1, (avail-1)/memberRowH) // reserve the header row out of the visible budget
	top := listTop(m.memberCursor, visible, len(m.members))
	for j := top; j < len(m.members) && j < top+visible; j++ {
		rows = append(rows, "")
		rows = append(rows, zone.Mark(memberRowZone(j), m.memberRow(m.members[j], j, nameW, w, m.hover == memberRowZone(j))))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

func (m Model) memberHeaderRow(nameW int) string {
	head := lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Muted).Bold(true)
	return head.Render(strings.Join([]string{pad("Name", nameW), pad("Role", colMemberRole)}, " "))
}

// memberTypeGlyph is the human/agent indicator, empty when glyphs are off (Details spells the type out).
func (m Model) memberTypeGlyph(isAgent bool) string {
	g := m.deps.Glyphs
	if isAgent {
		return g.Or(g.Robot, "")
	}
	return g.Or(g.Person, "")
}

func (m Model) memberRow(mem domain.ProjectMember, i, nameW, w int, hovered bool) string {
	t := m.deps.Styles.Theme
	sel := i == m.memberCursor
	banded := sel || hovered
	label := mem.Name()
	if gl := m.memberTypeGlyph(mem.IsAgent); gl != "" {
		label = gl + " " + label
	}
	name := pad(fit(label, nameW), nameW)
	role := pad(fit(memberRoleLabel(mem.Role), colMemberRole), colMemberRole)
	if !banded {
		name = lipgloss.NewStyle().Foreground(t.Text).Render(name)
		role = lipgloss.NewStyle().Foreground(memberRoleColor(t, mem.Role)).Render(role)
	}
	row := name + " " + role
	switch {
	case sel:
		row = m.selBand().Width(w).Render(row)
	case hovered:
		row = m.hoverBand().Width(w).Render(row)
	}
	return row
}

func (m Model) memberDetailPanel(w, h int) string {
	return m.windowedPanel(m.memberDetailTitle(), zoneMemberDetail, m.memberDetailBody, m.memberDetailScroll, w, h, false)
}

func (m Model) memberDetailTitle() string {
	if mem, ok := m.selectedMember(); ok {
		return "Details · " + flattenLine(mem.Name())
	}
	return "Details"
}

func (m Model) memberDetailBody(w int) string {
	s := m.deps.Styles
	t := s.Theme
	mem, ok := m.selectedMember()
	if !ok {
		return s.Muted.Render("Select a member to see their work.")
	}
	g := m.deps.Glyphs
	labelW := 12
	row := func(icon, label, value string) string {
		if icon != "" {
			label = icon + "  " + label
		}
		return lipgloss.NewStyle().Foreground(t.Muted).Width(labelW).Render(label) + components.Trunc(value, max(1, w-labelW))
	}
	name := lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(orDash(flattenLine(mem.Name())))
	roleVal := lipgloss.NewStyle().Foreground(memberRoleColor(t, mem.Role)).Render(memberRoleLabel(mem.Role))

	typeLabel, typeIcon := "Human", g.Or(g.Person, "")
	if mem.IsAgent {
		typeLabel, typeIcon = "Agent", g.Or(g.Robot, "")
	}
	rows := []string{
		name, "",
		row(g.Or(g.Person, ""), "Username", orDash(mem.Username)),
		row(typeIcon, "Type", typeLabel),
	}
	if mem.IsAgent {
		rows = append(rows, row(g.Or(g.PersonFeed, ""), "Owner", orDash(mem.Owner())))
	}
	rows = append(rows, row(g.Or(g.Flag, ""), "Role", roleVal))
	if mem.Email != "" {
		rows = append(rows, row(g.Or(g.Mail, ""), "Email", mem.Email))
	}
	if !mem.JoinedAt.IsZero() {
		rows = append(rows, row(g.Or(g.Calendar, ""), "Joined", formatLocalDay(mem.JoinedAt)))
	}
	rows = append(rows, "")
	rows = append(rows, m.memberStatsSection(mem.MemberID, w)...)
	rows = append(rows, "")
	rows = append(rows, m.memberContribSection(mem.MemberID, w)...)
	rows = append(rows, "")
	rows = append(rows, m.memberIssueSection(mem.MemberID, "Assigned", true, w)...)
	rows = append(rows, "")
	rows = append(rows, m.memberIssueSection(mem.MemberID, "Reviewing", false, w)...)
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// memberStatsSection reads as zeros for a member absent from the stats batch (no assigned issues).
func (m Model) memberStatsSection(memberID int64, w int) []string {
	s := m.deps.Styles
	t := s.Theme
	out := []string{sectionRule(s, "Stats", w), ""}
	switch {
	case !m.memberStatsLoaded && !m.memberStatsErr:
		return append(out, s.Muted.Render("Loading…"))
	case m.memberStatsErr:
		return append(out, s.Error.Render("Failed to load stats."))
	}
	st := m.memberStats[memberID] // zero value when the member has no assigned issues
	labelW := 14
	stat := func(label, value string) string {
		return lipgloss.NewStyle().Foreground(t.Muted).Width(labelW).Render(label) +
			lipgloss.NewStyle().Foreground(t.Text).Render(value)
	}
	// the bar is driven from the rate directly (count/1000), so it always agrees with the printed percentage
	completion := func(rate float64) string {
		barW := max(8, min(20, w-labelW-8))
		pct := int(rate*100 + 0.5)
		return lipgloss.NewStyle().Foreground(t.Muted).Width(labelW).Render("Completion") +
			statBar(int(rate*1000+0.5), 1000, barW, t.Success, t.Border) +
			lipgloss.NewStyle().Foreground(t.Text).Render(fmt.Sprintf(" %d%%", pct))
	}
	return append(out,
		stat("Resolved", strconv.FormatInt(st.ResolvedCount, 10)),
		stat("Open", strconv.FormatInt(st.OpenAssignedCount, 10)),
		stat("Story points", strconv.FormatInt(st.TotalStoryPoints, 10)),
		completion(st.CompletionRate),
		stat("Avg resolve", avgResolveLabel(st.AvgResolveSeconds)))
}

func avgResolveLabel(seconds *int64) string {
	if seconds == nil {
		return "—"
	}
	return components.HumanizeDuration(time.Duration(*seconds) * time.Second)
}

// shadeRunes are the four intensity blocks of the heatmap. A day with no resolutions is a muted dot.
var shadeRunes = []rune("░▒▓█")

// memberContribSection renders the resolution heatmap ("잔디"). It shares the member-work load, so it
// mirrors that load's loading/failure states.
func (m Model) memberContribSection(memberID int64, w int) []string {
	s := m.deps.Styles
	out := []string{sectionRule(s, "Contributions", w), ""}
	work, cached := m.memberWork[memberID]
	switch {
	case !cached && m.memberWorkFailed[memberID]:
		return append(out, s.Error.Render("Failed to load."))
	case !cached:
		return append(out, s.Muted.Render("Loading…"))
	}
	return append(out, m.contribHeatmap(work.contrib, w)...)
}

// contribHeatmap lays the daily counts into a 7-row (Sun..Sat) by N-week grid, shaded by each day's share
// of the busiest. A blank column separates weeks so a busy run is not one solid bar. Oldest weeks drop first.
func (m Model) contribHeatmap(c domain.Contributions, w int) []string {
	s := m.deps.Styles
	t := s.Theme
	if len(c.Days) == 0 {
		return []string{s.Muted.Render("No resolutions in the window")}
	}
	first := c.Days[0].Date
	origin := first.AddDate(0, 0, -int(first.Weekday())) // the Sunday on/before the first day
	last := c.Days[len(c.Days)-1].Date
	totalCols := int(last.Sub(origin).Hours()/24)/7 + 1
	startCol := 0
	// a column now costs two cells (its own plus the gap before it), except the first, which has no gap
	if maxCols := max(1, (w+1)/2); totalCols > maxCols {
		startCol = totalCols - maxCols // keep the most recent weeks when the grid is wider than the panel
	}
	cols := totalCols - startCol

	grid := make([][]int, 7)
	for r := range grid {
		grid[r] = make([]int, cols)
		for cc := range grid[r] {
			grid[r][cc] = -1 // no cell here (a day outside the actual range)
		}
	}
	for _, d := range c.Days {
		col := int(d.Date.Sub(origin).Hours()/24) / 7
		// guard both ends: a malformed response (unsorted days) would otherwise index outside the grid
		if col < startCol || col-startCol >= cols {
			continue
		}
		grid[int(d.Date.Weekday())][col-startCol] = contribLevel(d.Count, c.MaxDaily)
	}

	muted := lipgloss.NewStyle().Foreground(t.Muted)
	active := lipgloss.NewStyle().Foreground(t.Success)
	cell := func(idx int) string {
		switch {
		case idx < 0:
			return " "
		case idx == 0:
			return muted.Render("·")
		default:
			return active.Render(string(shadeRunes[idx-1]))
		}
	}
	lines := make([]string, 0, 9)
	lines = append(lines, muted.Render(fmt.Sprintf("%d resolved · last %d weeks", c.TotalResolved, cols)))
	for r := 0; r < 7; r++ {
		var b strings.Builder
		for cc := 0; cc < cols; cc++ {
			if cc > 0 {
				b.WriteString(" ")
			}
			b.WriteString(cell(grid[r][cc]))
		}
		lines = append(lines, b.String())
	}
	lines = append(lines, muted.Render("Less ")+muted.Render("·")+active.Render(string(shadeRunes))+muted.Render(" More"))
	return lines
}

// contribLevel maps a count to 0..4 against the busiest day. Any non-zero count is at least 1 so it shows.
func contribLevel(count, peak int) int {
	if count <= 0 {
		return 0
	}
	if peak <= 0 {
		return 1
	}
	lvl := 1 + (count*3)/peak
	if lvl > 4 {
		lvl = 4
	}
	return lvl
}

// memberIssueSection renders one Assigned/Reviewing section, reusing the Sprints tab's issue row.
func (m Model) memberIssueSection(memberID int64, title string, assigned bool, w int) []string {
	s := m.deps.Styles
	work, cached := m.memberWork[memberID]
	page := work.assigned
	if !assigned {
		page = work.reviewer
	}
	heading := title
	if cached {
		heading = fmt.Sprintf("%s (%d)", title, page.TotalElements)
	}
	out := []string{sectionRule(s, heading, w), ""}
	switch {
	case !cached && m.memberWorkFailed[memberID]:
		return append(out, s.Error.Render("Failed to load."))
	case !cached:
		return append(out, s.Muted.Render("Loading…"))
	case len(page.Issues) == 0:
		return append(out, s.Muted.Render("None open."))
	}
	for i, it := range page.Issues {
		if i > 0 {
			out = append(out, "")
		}
		out = append(out, m.sprintIssueRow(it, w))
	}
	if page.HasNext {
		out = append(out, "", s.Muted.Render(fmt.Sprintf("… %d more", page.TotalElements-len(page.Issues))))
	}
	return out
}

func memberRoleLabel(role string) string {
	switch role {
	case "MANAGER":
		return "Manager"
	case "MEMBER":
		return "Member"
	}
	return orDash(role)
}

// memberRoleColor tints managers with Primary so the roster reads the hierarchy at a glance.
func memberRoleColor(t theme.Theme, role string) color.Color {
	if role == "MANAGER" {
		return t.Primary
	}
	return t.Muted
}
