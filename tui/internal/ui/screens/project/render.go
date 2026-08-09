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
	zoneBack     = "project.back"
	zoneSearch   = "project.search"
	zoneFilter   = "project.filter.button"
	zoneNew      = "project.new.button"
	zoneDetail   = "project.detail"
	zoneActivity = "project.activity"

	// inline edit pens in the Details panel (mouse only; e/t/a do the same from the keyboard)
	zoneEditIssue     = "project.detail.edit"      // pen on the title -> the edit form (Parent is edited there)
	zoneEditState     = "project.detail.state"     // pen on the State row -> the transition picker
	zoneEditAssignee  = "project.detail.assignee"  // pen on the Assignee row -> the assignee picker
	zoneEditReviewers = "project.detail.reviewers" // "+ Reviewer" button -> the reviewers picker (r)
	zoneAddChild      = "project.detail.addchild"  // "+ Child" button -> the child-create form
	zoneAddRelation   = "project.detail.addrel"    // "+ Relation" button -> the relation type picker (L)
	zoneEditContent   = "project.detail.content"   // pen on the Content header -> the content editor (E)

	filterButtonW = 5  // rounded border + glyph + border, matching the home dashboard's filter button
	newButtonW    = 5  // the "+ New" button beside the filter button, same footprint
	detailLabelW  = 17 // label column width in the Details panel's meta rows (glyph + gap + label)

	// Details panel body insets (matching the home dashboard): a left margin, a gap before the
	// scrollbar, the scrollbar column itself, and a blank row kept above the bottom rule.
	detailInsetL     = 2
	detailInsetR     = 1
	detailScrollbarW = 1
	detailPadBottom  = 1

	// responsive layout: at sideMinW and wider the list and Details sit side by side; below it the list
	// goes full width and the detail shows as a read-only modal (opened from the list). listFloorW is the
	// width that full-width list needs to render.
	sideMinW   = 128
	listFloorW = 80
	// triColMinW is the width at and above which the toggled-on Activity view gets its own third column
	// beside Details. Below it (but still side by side) Activity swaps in for Details instead.
	triColMinW = 160
	// listColsMinW is the narrowest the list column may get before the issue table would wrap: the fixed
	// columns (colKey+colType+colAsg+colState+colPri+colAct), their six one-cell gaps, a minimal title, and
	// the panel's insets. The 1:1 split floors to this so a half-width list still renders the table.
	listColsMinW = colKey + colType + colAsg + colState + colPri + colAct + 6 + 8 + 4
)

// The ANSI-strip and overlay helpers live in components. These thin wrappers keep this package's call
// sites unqualified, matching home and schema. See components/render.go for the implementations.

func stripANSI(s string) string { return components.StripANSI(s) }

func overlayDim(backdrop, fg string, x, y int, dim color.Color) string {
	return components.OverlayDim(backdrop, fg, x, y, dim)
}

func issueRowZone(i int) string { return "project.row." + strconv.Itoa(i) }

// column widths for the issue table (title flexes to fill the rest).
const (
	colKey   = 11
	colType  = 12
	colAsg   = 14
	colState = 14
	colPri   = 5 // header is a priority glyph (fallback "Pri"); values are P0..P4
	colAct   = 6 // last-activity relative time, e.g. "45m" / "11mon"
	rowH     = 2 // a blank separator line above each issue row, matching the Projects/Schema lists
)

func (m Model) View() string {
	if m.width == 0 {
		return ""
	}
	s := m.deps.Styles
	if m.width < m.minWidthFloor() || m.height < minHeight {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center, s.Muted.Render("Terminal too small"))
	}
	if m.filtering {
		return m.modalView()
	}
	if m.tab == tabIssues {
		return m.issuesView()
	}
	if m.tab == tabSprints {
		return m.sprintsView()
	}
	if m.tab == tabMembers {
		return m.membersView()
	}
	if m.tab == tabConfig {
		return m.configView()
	}
	if m.tab == tabStats {
		return m.statsView()
	}
	return m.placeholder(m.tabLabel())
}

// issuesView is the list/Details layout, with any open action form (edit, comment, delete confirmation,
// or a picker) - and, on a narrow terminal, the read-only detail modal - floated over a dimmed copy.
func (m Model) issuesView() string {
	t := m.deps.Styles.Theme
	base := m.issuesTab()
	switch {
	case m.peeking:
		scrolled, _, _ := components.ScrollBox(m.peekModal(), m.height, m.peekScroll, t.Primary, t.Border)
		return m.floatOver(base, scrolled)
	case m.creating:
		form, _, _ := components.ScrollBox(m.createUI.View(), m.height, m.createScroll, t.Primary, t.Border)
		over := m.floatOver(base, form)
		if m.picking && m.pickKind == pickParent {
			return m.floatOver(over, m.picker.View(m.deps.Styles)) // the parent picker floats over the form
		}
		if m.dating {
			return m.floatOver(over, m.datePick.View(m.deps.Styles)) // the calendar floats over the form
		}
		return over
	case m.editing:
		form, _, _ := components.ScrollBox(m.editUI.View(), m.height, m.editScroll, t.Primary, t.Border)
		over := m.floatOver(base, form)
		if m.picking && m.pickKind == pickParentEdit {
			return m.floatOver(over, m.picker.View(m.deps.Styles)) // the parent picker floats over the form
		}
		if m.dating {
			return m.floatOver(over, m.datePick.View(m.deps.Styles))
		}
		return over
	case m.editingContent:
		form, _, _ := components.ScrollBox(m.contentUI.View(), m.height, m.contentScroll, t.Primary, t.Border)
		return m.floatOver(base, form)
	case m.reviewing:
		form, _, _ := components.ScrollBox(m.reviewUI.View(), m.height, m.reviewScroll, t.Primary, t.Border)
		return m.floatOver(base, form)
	case m.commenting:
		modal, _, _ := m.commentModalView()
		scrolled, _, _ := components.ScrollBox(modal, m.height, m.commentScroll, t.Primary, t.Border)
		over := m.floatOver(base, scrolled)
		if m.commentDeleting {
			return m.floatOver(over, m.commentDeleteUI.View()) // the confirmation floats over the thread
		}
		return over
	case m.deleting:
		return m.floatOver(base, m.deleteUI.View())
	case m.picking:
		return m.floatOver(base, m.picker.View(m.deps.Styles))
	}
	// a narrow terminal has no room for a side panel, so the focused view shows as a read-only modal
	// floated over the full-width list (opened from the list, closed with esc). The toggle swaps the
	// modal between Details and Activity.
	if m.narrow() && m.focus == focusDetail {
		modal, _, _ := components.ScrollBox(m.activeModal(), m.height, m.activeScroll(), t.Primary, t.Border)
		return m.floatOver(base, modal)
	}
	return base
}

// floatOver centers fg over a dimmed copy of base, splicing it in by hand (not a compositor, which
// drops zone marks) so the form stays clickable, matching the filter modal.
func (m Model) floatOver(base, fg string) string {
	backdrop := stripANSI(lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Top, base))
	x := max(0, (m.width-lipgloss.Width(fg))/2)
	y := max(0, (m.height-lipgloss.Height(fg))/2)
	return overlayDim(backdrop, fg, x, y, m.deps.Styles.Theme.Muted)
}

// detailContentW is the Details panel body width: the panel width less the left margin, the gap, and
// the scrollbar column.
func (m Model) detailContentW() int {
	_, detailW, _ := m.panelWidths()
	return max(1, detailW-detailInsetL-detailInsetR-detailScrollbarW)
}

// detailViewH is the number of visible body rows in the Details panel (its height less the two rules
// and the bottom pad).
func (m Model) detailViewH() int { return max(1, m.height-2-detailPadBottom) }

// detailPage is the PgUp/PgDn step: the visible body height less one line of overlap. The read-only
// modal and the side panel size their visible area differently, so the step follows the active one.
func (m Model) detailPage() int {
	if m.narrow() {
		return max(1, m.height-3) // ScrollBox shows m.height-2 interior rows; keep one line of overlap
	}
	return max(1, m.detailViewH()-1)
}

// detailLineCount is how many rendered lines the current Details body occupies at the panel width. It
// counts the same body detailPanel renders (with the leading blank line under the top rule), so the
// scroll bound and the panel window agree.
func (m Model) detailLineCount() int {
	w := m.detailContentW()
	return lipgloss.Height(lipgloss.NewStyle().Width(w).Render("\n" + m.detailBody(w)))
}

// detailScrollMax is the largest scroll offset. In narrow mode it bounds the read-only modal's window;
// side by side it bounds the panel's windowed body.
func (m Model) detailScrollMax() int {
	if m.narrow() {
		return max(0, lipgloss.Height(m.detailModal())-m.height)
	}
	return max(0, m.detailLineCount()-m.detailViewH())
}

// detailModalContentW is the read-only detail modal's body width in narrow mode: it tracks the terminal
// but is capped so long lines stay readable.
func (m Model) detailModalContentW() int { return max(20, min(m.innerWidth()-4, 96)) }

// detailModal / activityModal are the read-only views shown on a narrow terminal (no room for a side
// panel): the Details or Activity body, boxed and padded, floated over the list and scrolled with
// ScrollBox. The toggle swaps between them.
func (m Model) detailModal() string   { return m.modalOf("Details", m.detailBody) }
func (m Model) activityModal() string { return m.modalOf("Activity", m.activityBody) }

// modalOf boxes a body function into the centered read-only modal. The body is folded to the content
// width first: a comment author or relation label is unbounded, untrusted server text, so without the
// wrap the box could outgrow the terminal and the overlay would spill past m.width and corrupt the frame.
func (m Model) modalOf(title string, bodyFn func(int) string) string {
	w := m.detailModalContentW()
	body := lipgloss.NewStyle().Width(w).Render(bodyFn(w))
	body = lipgloss.NewStyle().Padding(1, 1).Render(body)
	return components.TitledBoxCentered(title, body, m.deps.Styles.Theme.Primary)
}

// peekBody renders the linked issue being peeked, from the same detail cache as the main view (skeleton
// while it loads, an error note on failure). The body is the reused detailContent, which renders read-only
// because m.peeking suppresses its pens, add-buttons and peek links.
func (m Model) peekBody(w int) string {
	s := m.deps.Styles
	if m.peekKey == "" {
		return s.Muted.Render("No issue.")
	}
	if d, ok := m.details[m.peekKey]; ok {
		return m.detailContent(d, w)
	}
	if m.detailsFailed[m.peekKey] {
		return s.Error.Render("Failed to load this issue.") + "\n\n" + s.Muted.Render("Press R to retry.")
	}
	return detailSkeleton(s, w)
}

// peekModal is the boxed read-only peek, titled with the linked issue's key.
func (m Model) peekModal() string { return m.modalOf(m.peekKey, m.peekBody) }

// peekScrollMax bounds the peek's scroll window, mirroring commentScrollMax.
func (m Model) peekScrollMax() int { return max(0, lipgloss.Height(m.peekModal())-m.height) }

// peekPage is the PgUp/PgDn step for the peek: ScrollBox shows m.height-2 interior rows, so a one-line
// overlap makes the step m.height-3 (the side panel's detailPage would under-step it).
func (m Model) peekPage() int { return max(1, m.height-3) }

// activeModal is the read-only modal currently shown in narrow mode: Activity when toggled on, else Details.
func (m Model) activeModal() string {
	if m.showActivity {
		return m.activityModal()
	}
	return m.detailModal()
}

// activeScroll is the scroll offset of the view the keyboard/wheel drives in narrow mode.
func (m Model) activeScroll() int {
	if m.showActivity {
		return m.activityScroll
	}
	return m.detailScroll
}

// activityContentW is the Activity view's body width: the third column when it shows, else the detail
// slot it swaps into (or the modal width in narrow mode).
func (m Model) activityContentW() int {
	if m.narrow() {
		return m.detailModalContentW()
	}
	_, detailW, activityW := m.panelWidths()
	w := detailW
	if m.threeCol() {
		w = activityW
	}
	return max(1, w-detailInsetL-detailInsetR-detailScrollbarW)
}

// activityLineCount / activityScrollMax / activityPage mirror the Details scroll helpers for the
// Activity view.
func (m Model) activityLineCount() int {
	w := m.activityContentW()
	return lipgloss.Height(lipgloss.NewStyle().Width(w).Render("\n" + m.activityBody(w)))
}

func (m Model) activityScrollMax() int {
	if m.narrow() {
		return max(0, lipgloss.Height(m.activityModal())-m.height)
	}
	return max(0, m.activityLineCount()-m.detailViewH())
}

func (m Model) activityPage() int {
	if m.narrow() {
		return max(1, m.height-3)
	}
	return max(1, m.detailViewH()-1)
}

// activePage is the PgUp/PgDn step for whichever view the keyboard drives.
func (m Model) activePage() int {
	if m.showActivity {
		return m.activityPage()
	}
	return m.detailPage()
}

func (m Model) detailBody(w int) string {
	s := m.deps.Styles
	if m.viewKey == "" {
		return s.Muted.Render("No issue selected.")
	}
	if d, ok := m.details[m.viewKey]; ok {
		return m.detailContent(d, w)
	}
	if m.detailsFailed[m.viewKey] {
		return s.Error.Render("Failed to load this issue.") + "\n\n" +
			s.Muted.Render("Press R to retry.")
	}
	return detailSkeleton(s, w)
}

// detailPanel is the borderless "Details" panel (matching the home dashboard). It shows the
// cursor-selected issue and is the keyboard's scroll target unless the Activity view is toggled on.
func (m Model) detailPanel(w, h int) string {
	return m.windowedPanel("Details", zoneDetail, m.detailBody, m.detailScroll, w, h, m.focus == focusDetail && !m.showActivity)
}

// activityPanel is the borderless "Activity" panel: the issue's audit trail, windowed like Details. It
// is the keyboard's scroll target while the Activity view is toggled on.
func (m Model) activityPanel(w, h int) string {
	return m.windowedPanel("Activity", zoneActivity, m.activityBody, m.activityScroll, w, h, m.focus == focusDetail && m.showActivity)
}

// windowedPanel is the shared borderless side panel: a titled top rule, the windowed body (rendered at
// the content width by bodyFn) with a scrollbar column, and a bottom rule. focused tints the borders.
func (m Model) windowedPanel(title, zoneID string, bodyFn func(int) string, scroll, w, h int, focused bool) string {
	t := m.deps.Styles.Theme
	contentW := max(1, w-detailInsetL-detailInsetR-detailScrollbarW)
	totalRows := max(1, h-2) // rows between the top and bottom rules
	viewH := max(1, totalRows-detailPadBottom)

	body := "\n" + bodyFn(contentW) // a blank line under the top rule, like home
	lines := strings.Split(lipgloss.NewStyle().Width(contentW).Render(body), "\n")
	off := min(max(scroll, 0), max(0, len(lines)-viewH))
	bar := components.ScrollbarColumn(off, len(lines), viewH, t.Primary, t.Muted)

	blank := strings.Repeat(" ", contentW)
	padL := strings.Repeat(" ", detailInsetL)
	padR := strings.Repeat(" ", detailInsetR)
	rows := make([]string, totalRows)
	for i := range rows {
		line, barCell := blank, " "
		if i < viewH {
			if di := off + i; di < len(lines) {
				line = lines[di]
			}
			barCell = bar[i]
		}
		rows[i] = padL + line + padR + barCell
	}

	border := t.Primary
	if focused {
		border = t.Accent
	}
	top := components.RuleWithTitle(title, w, border)
	bottom := lipgloss.NewStyle().Foreground(border).Render(strings.Repeat("─", w))
	panel := lipgloss.JoinVertical(lipgloss.Left, top, strings.Join(rows, "\n"), bottom)
	return zone.Mark(zoneID, panel)
}

func (m Model) detailContent(d domain.IssueDetail, w int) string {
	s := m.deps.Styles
	t := s.Theme

	// a meta value is clipped to the space left of its label so a long name never wraps to column 0.
	// each row leads with a glyph (empty on plain terminals) matching the home dashboard's Details.
	g := m.deps.Glyphs
	valueW := max(1, w-detailLabelW)
	row := func(icon, label, value string) string {
		if icon != "" {
			label = icon + "  " + label
		}
		return lipgloss.NewStyle().Foreground(t.Muted).Width(detailLabelW).Render(label) + components.Trunc(value, valueW)
	}
	// editRow is a meta row with an inline edit affordance right-anchored at the panel edge (mouse only),
	// matching the schema dashboard's Details pens.
	editRow := func(icon, label, value, zoneID, glyph, fallback string) string {
		if icon != "" {
			label = icon + "  " + label
		}
		left := lipgloss.NewStyle().Foreground(t.Muted).Width(detailLabelW).Render(label) + components.Trunc(value, valueW)
		pen := m.penAction(zoneID, glyph, fallback)
		if pen == "" {
			return left
		}
		return rightAlignAction(left, pen, w)
	}
	// parentRow is the Parent meta row with a clickable key (opens its peek). The key is zone-marked after
	// styling and kept out of components.Trunc (which would strip the marker), so its cell stays clickable.
	parentRow := func(icon, pk string) string {
		label := "Parent"
		if icon != "" {
			label = icon + "  " + label
		}
		left := lipgloss.NewStyle().Foreground(t.Muted).Width(detailLabelW).Render(label)
		if pk == "" {
			return left + components.Trunc("-", valueW)
		}
		return left + m.peekLink("p", pk, lipgloss.NewStyle().Foreground(m.peekKeyColor("p", pk, t.Primary)).Bold(true).Render(components.Trunc(pk, valueW)))
	}

	titleLeft := lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(orDash(flattenLine(d.Title)))
	title := titleLeft
	if pen := m.penAction(zoneEditIssue, g.PenSquare, "edit"); pen != "" {
		title = rightAlignAction(titleLeft, pen, w)
	}
	key := lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render(d.Key)
	state := lipgloss.NewStyle().Foreground(stateColor(t, d.StateCategory)).Render(orDash(d.StateLabel))
	pri := lipgloss.NewStyle().Foreground(priorityColor(t, d.Priority)).Render(orDash(d.Priority))

	rows := []string{
		title, "",
		row(g.Or(g.Key, ""), "Key", key),
		row(g.Or(g.Tag, ""), "Type", m.typeValue(d.TypeName, d.TypeColor)),
		editRow(g.Or(g.Flag, ""), "State", state, zoneEditState, g.Workflow, "transition"),
		row(g.Or(g.Priority, ""), "Priority", pri),
		editRow(g.Or(g.Person, ""), "Assignee", orDash(d.AssigneeName), zoneEditAssignee, "+", "+"),
		row(g.Or(g.PersonFeed, ""), "Author", orDash(d.AuthorName)),
		parentRow(g.Or(g.Hierarchy, ""), parentKeyOf(d)), // clickable key opens its peek; edited from the "Edit issue" form

		row(g.Or(g.Number, ""), "Story point", storyPointLabel(d.StoryPoint)),
		row(g.Or(g.Percent, ""), "Progress", m.progressValue(d.Progress, valueW)),
		row(g.Or(g.Clock, ""), "Due", formatDateOnly(d.DueAt)),
		row(g.Or(g.Calendar, ""), "Created", formatLocalDay(d.CreatedAt)),
		row(g.Or(g.LastUpdated, ""), "Updated", formatLocalDay(d.LastUpdatedAt)),
	}
	if len(d.CustomFields) > 0 {
		rows = append(rows, "", sectionRule(s, "Custom fields", w), "", m.customFieldsBlock(d.CustomFields, w))
	}
	// Children and Reviewers always show their section so the entry point (a "+ Child" / "+ Reviewer"
	// button in the body) is visible even when empty. Children stays hidden only when this issue can have
	// none (a bottom-level type with no existing children).
	if addChild := m.canAddChild(d); len(d.Children) > 0 || addChild {
		rows = append(rows, "", sectionRule(s, fmt.Sprintf("Children (%d)", len(d.Children)), w), "", m.childrenBlock(d.Children, addChild, w))
	}
	rows = append(rows, "", sectionRule(s, fmt.Sprintf("Reviewers (%d)", len(d.Reviewers)), w), "", m.reviewersBlock(d.Reviewers, w))
	// Relations sits under Reviewers and always shows (like Children/Reviewers) so its "+ Relation" entry
	// point is visible even when empty.
	rows = append(rows, "", sectionRule(s, fmt.Sprintf("Relations (%d)", relationCount(d.Relations)), w), "", m.relationsBlock(d.Relations, w))
	// Branches show only when the issue has any (they arrive from push webhooks, not user action, so there
	// is no "+ add" entry point). Each links out to the repository.
	if len(d.Branches) > 0 {
		rows = append(rows, "", sectionRule(s, fmt.Sprintf("Branches (%d)", len(d.Branches)), w), "", m.branchesBlock(d.Branches, w))
	}
	// Pull requests follow the same rule as Branches: webhook-fed, so no "+ add" entry point to keep
	// visible when empty.
	if len(d.PullRequests) > 0 {
		rows = append(rows, "", sectionRule(s, fmt.Sprintf("Pull requests (%d)", len(d.PullRequests)), w), "",
			m.pullRequestsBlock(d.PullRequests, w))
	}
	rows = append(rows,
		"", m.contentHeader(w), "",
		m.mdBlock(d.Content, "No content.", w),
	)
	rows = append(rows,
		"", sectionRule(s, fmt.Sprintf("Comments (%d)", d.CommentCount), w), "",
		m.commentsBlock(d, w),
	)
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// branchZone / branchCommitZone id the clickable branch-name and commit-hash cells (by branch index) so a
// click opens the branch or commit URL in the browser.
func branchZone(i int) string       { return "project.detail.branch." + strconv.Itoa(i) }
func branchCommitZone(i int) string { return "project.detail.commit." + strconv.Itoa(i) }

// branchesBlock lists the issue's linked VCS branches: each branch name links out to the branch on the
// remote, and its latest commit's short hash links to that commit, with the commit subject alongside.
// Links are mouse-only and suppressed inside a read-only peek (whose reused body renders no nested links).
func (m Model) branchesBlock(branches []domain.IssueBranch, w int) string {
	s := m.deps.Styles
	t := s.Theme
	g := m.deps.Glyphs
	linkable := m.deps.Mouse && !m.peeking
	var rows []string
	for i, b := range branches {
		if i > 0 {
			rows = append(rows, "")
		}
		nameStyle := lipgloss.NewStyle().Foreground(t.Accent)
		if linkable && b.BranchURL != "" {
			nameStyle = nameStyle.Underline(true)
		}
		nameCell := g.Or(g.Branch, "") + " " + nameStyle.Render(components.Trunc(b.BranchName, max(1, w-4)))
		if linkable && b.BranchURL != "" {
			nameCell = zone.Mark(branchZone(i), nameCell)
		}
		rows = append(rows, nameCell)

		if b.LatestCommitHash != "" {
			hash := shortHash(b.LatestCommitHash)
			hashStyle := lipgloss.NewStyle().Foreground(t.Secondary)
			if linkable && b.LatestCommitURL != "" {
				hashStyle = hashStyle.Underline(true)
			}
			hashCell := hashStyle.Render(hash)
			if linkable && b.LatestCommitURL != "" {
				hashCell = zone.Mark(branchCommitZone(i), hashCell)
			}
			line := "  " + g.Or(g.Commit, "") + " " + hashCell
			if b.LatestCommitMsg != "" {
				line += " " + lipgloss.NewStyle().Foreground(t.Muted).
					Render(components.Trunc(b.LatestCommitMsg, max(1, w-len(hash)-6)))
			}
			rows = append(rows, line)
		}
		if meta := branchPushMeta(b); meta != "" {
			rows = append(rows, "  "+lipgloss.NewStyle().Foreground(t.Muted).
				Render(components.Trunc(meta, max(1, w-2))))
		}
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// branchPushMeta is who last pushed the branch and when. It tells a stale branch apart from the one being
// worked on, which the commit subject alone cannot: two plausible-looking branches on the same issue are
// separated by their dates. Either half may be missing, so the line carries whichever is known.
func branchPushMeta(b domain.IssueBranch) string {
	var parts []string
	if b.PusherName != "" {
		parts = append(parts, flattenLine(b.PusherName))
	}
	if !b.PushedAt.IsZero() {
		parts = append(parts, formatRelative(b.PushedAt))
	}
	return strings.Join(parts, " · ")
}

func pullRequestZone(i int) string { return "project.detail.pr." + strconv.Itoa(i) }

// pullRequestsBlock lists the issue's linked pull requests as current state: the number and title link out
// to the PR, with a coloured state beside them so an open one is picked out of a list of merged ones at a
// glance. Links are mouse-only and suppressed inside a read-only peek, matching branchesBlock.
func (m Model) pullRequestsBlock(pullRequests []domain.IssuePullRequest, w int) string {
	s := m.deps.Styles
	t := s.Theme
	g := m.deps.Glyphs
	linkable := m.deps.Mouse && !m.peeking
	var rows []string
	for i, pr := range pullRequests {
		if i > 0 {
			rows = append(rows, "")
		}
		label, color := pullRequestState(t, pr.State)
		state := lipgloss.NewStyle().Foreground(color).Render(label)

		numStyle := lipgloss.NewStyle().Foreground(t.Accent)
		if linkable && pr.URL != "" {
			numStyle = numStyle.Underline(true)
		}
		head := fmt.Sprintf("#%d %s", pr.Number, flattenLine(pr.Title))
		headCell := g.Or(g.PullRequest, "") + " " +
			numStyle.Render(components.Trunc(head, max(1, w-len(label)-6)))
		if linkable && pr.URL != "" {
			headCell = zone.Mark(pullRequestZone(i), headCell)
		}
		rows = append(rows, headCell+"  "+state)

		if meta := pullRequestMeta(pr); meta != "" {
			rows = append(rows, "  "+lipgloss.NewStyle().Foreground(t.Muted).
				Render(components.Trunc(meta, max(1, w-2))))
		}
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// pullRequestState is how a PR state should read. Merged and closed both mean "no longer open", but only
// closed-without-merge is a dead end, so they do not share a colour.
func pullRequestState(t theme.Theme, state string) (string, color.Color) {
	switch state {
	case "MERGED":
		return "merged", t.Secondary
	case "CLOSED":
		return "closed", t.Muted
	default:
		return "open", t.Success
	}
}

// pullRequestMeta is who opened the PR and when it last moved. Either half may be missing, so the line
// carries whichever is known.
func pullRequestMeta(pr domain.IssuePullRequest) string {
	var parts []string
	if pr.AuthorName != "" {
		parts = append(parts, flattenLine(pr.AuthorName))
	}
	if !pr.LastEventAt.IsZero() {
		parts = append(parts, formatRelative(pr.LastEventAt))
	}
	return strings.Join(parts, " · ")
}

// shortHash abbreviates a commit hash to its conventional 7-character short form, leaving a shorter hash
// untouched.
func shortHash(h string) string {
	if len(h) > 7 {
		return h[:7]
	}
	return h
}

// penAction is an inline edit affordance marked for zoneID, right-anchored in the Details rows. It is
// mouse-only (the e/t/a keys do the same when off, so hiding it leaves no action unreachable) and uses
// Primary, brightening to Secondary on hover, matching the schema dashboard's Details pens. glyph is the
// icon; fallback is its plain-terminal text.
func (m Model) penAction(zoneID, glyph, fallback string) string {
	if !m.deps.Mouse || m.peeking {
		return "" // the peek modal is read-only: suppress every edit affordance in its (reused) detail body
	}
	icon := m.deps.Glyphs.Or(glyph, fallback)
	return zone.Mark(zoneID, lipgloss.NewStyle().Foreground(m.affordColor(zoneID)).Bold(true).Render(" "+icon+" "))
}

// addButton is a clickable "+ Label" affordance rendered in a relationship section's body (Children /
// Reviewers), sitting below the items so the section reads as a list you can extend. Mouse-only like the
// pens (the r key still opens reviewers; child-create is the mouse "+" or n + parent), Primary and
// brightening to Secondary on hover. Returns "" with the mouse off so callers fall back to a placeholder.
func (m Model) addButton(zoneID, label string) string {
	if !m.deps.Mouse || m.peeking {
		return "" // read-only in the peek modal, like the edit pens
	}
	return "  " + zone.Mark(zoneID, lipgloss.NewStyle().Foreground(m.affordColor(zoneID)).Bold(true).Render("+ "+label))
}

// affordColor is a Details affordance's colour (the edit pens and the "+ …" buttons): Primary at rest, the
// focus Accent while hovered - so every clickable Details control highlights the same way.
func (m Model) affordColor(zoneID string) color.Color {
	if m.hover == zoneID {
		return m.deps.Styles.Theme.Accent
	}
	return m.deps.Styles.Theme.Primary
}

// peekZone is the click target for a linked issue that opens its read-only peek. The section prefix
// ("p"/"c"/"r") keeps the id distinct when the same issue key appears in two sections (e.g. a parent that
// is also a relation target) - without it the two zone.Marks would share one id and the earlier-rendered
// link would be a dead click. Within a section a key is unique (a child appears once; the backend's
// (source,target) uniqueness means a relation target appears in at most one group).
func peekZone(section, key string) string { return "project.detail.peek." + section + "." + key }

// peekLink makes a linked issue's key cell clickable to open its peek modal. Mouse-only (click is the v1
// entry point) and off while a peek is already open, so the reused detail body renders no nested links.
// The zone.Mark wraps the already-styled/padded cell, never a value that a later Trunc would re-encode and
// strip the marker from. section is the row's origin ("p" parent, "c" child, "r" relation).
func (m Model) peekLink(section, key, rendered string) string {
	if key == "" || !m.deps.Mouse || m.peeking {
		return rendered
	}
	return zone.Mark(peekZone(section, key), rendered)
}

// peekKeyColor is a linked issue key's colour: its base normally, the focus Accent while hovered - so an
// issue link highlights like the Details pens and buttons. Call sites style the key with it before
// peekLink wraps the (zone-marked) cell. A peek is read-only, so its own links never highlight.
func (m Model) peekKeyColor(section, key string, base color.Color) color.Color {
	if !m.peeking && m.deps.Mouse && m.hover == peekZone(section, key) {
		return m.deps.Styles.Theme.Accent
	}
	return base
}

// rightAlignAction lays left text and a right-anchored action within width cells, matching the schema
// dashboard's edit-pen placement (it truncates the left so the action always fits at the edge).
func rightAlignAction(left, action string, width int) string {
	aw := lipgloss.Width(action)
	left = components.Trunc(left, max(1, width-aw-1))
	gap := max(1, width-lipgloss.Width(left)-aw)
	return left + strings.Repeat(" ", gap) + action
}

// parentKeyOf is the issue's parent key, or "" when it has none (the Parent row shows "-" then).
func parentKeyOf(d domain.IssueDetail) string {
	if d.Parent == nil {
		return ""
	}
	return d.Parent.Key
}

// typeValue is the issue type as a background colour chip, for the Details "Type" row.
func (m Model) typeValue(name, colorName string) string {
	if chip, ok := components.ColorChip(colorName, orDash(name)); ok {
		return chip
	}
	return orDash(name)
}

// progressValue renders the progress percentage as a small bar plus the number, or just the number when
// the value column is too narrow to fit a readable bar.
func (m Model) progressValue(pct, valueW int) string {
	t := m.deps.Styles.Theme
	label := fmt.Sprintf(" %d%%", pct)
	if valueW < 14 {
		return fmt.Sprintf("%d%%", pct)
	}
	barW := min(12, valueW-lipgloss.Width(label))
	return progressBar(pct, barW, t.Primary, t.Muted) + label
}

// progressBar draws a filled/empty block bar of width cells for pct in [0,100].
func progressBar(pct, width int, fill, track color.Color) string {
	if width < 1 {
		width = 1
	}
	pct = min(100, max(0, pct))
	filled := pct * width / 100
	return lipgloss.NewStyle().Foreground(fill).Render(strings.Repeat("█", filled)) +
		lipgloss.NewStyle().Foreground(track).Render(strings.Repeat("░", width-filled))
}

// relationsBlock renders the issue's linked issues grouped by relation kind. Empty groups were already
// dropped in the mapping, so every group shown has at least one issue.
// relationCount is the total number of linked issues across all relation groups (the section header count).
func relationCount(groups []domain.IssueRelationGroup) int {
	n := 0
	for _, g := range groups {
		n += len(g.Items)
	}
	return n
}

// relationsBlock renders the linked issues grouped by kind, then a "+ Relation" button below (a blank line
// apart) so the section reads as a list you can extend, matching Children/Reviewers. Empty shows just the
// button (or a muted placeholder with the mouse off).
func (m Model) relationsBlock(groups []domain.IssueRelationGroup, w int) string {
	t := m.deps.Styles.Theme
	btn := m.addButton(zoneAddRelation, "Relation")
	if len(groups) == 0 {
		if btn == "" {
			return m.deps.Styles.Muted.Render("No linked issues yet.")
		}
		return btn
	}
	var rows []string
	for i, g := range groups {
		if i > 0 {
			rows = append(rows, "")
		}
		rows = append(rows, lipgloss.NewStyle().Foreground(t.Muted).Render(g.Kind))
		for _, it := range g.Items {
			rows = append(rows, m.relationRow(it, w))
		}
	}
	if btn != "" {
		rows = append(rows, "", btn)
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// colStatus is the shared right-anchored status/state column for the detail's relationship rows
// (relations, children, parent, reviewers), so every trailing status starts at the same x. Wide
// enough for the longest fixed review status, "Changes requested".
const colStatus = 18

// relationRow is one linked issue: its key, title, and current state, aligned like a compact list row.
func (m Model) relationRow(it domain.RelatedIssue, w int) string {
	t := m.deps.Styles.Theme
	key := m.peekLink("r", it.Key, lipgloss.NewStyle().Foreground(m.peekKeyColor("r", it.Key, t.Primary)).Bold(true).Render(pad(fit(it.Key, colKey), colKey)))
	state := lipgloss.NewStyle().Foreground(stateColor(t, it.StateCategory)).Render(fit(orDash(it.StateLabel), colStatus))
	titleW := max(6, w-colKey-colStatus-4) // 2-cell indent + two single-cell gaps
	title := lipgloss.NewStyle().Foreground(t.Text).Render(fit(it.Title, titleW))
	return "  " + key + " " + title + " " + state
}

// reviewersBlock renders each reviewer as a compact row (name + review status), then a "+ Reviewer"
// button below (a blank line apart), so the roster reads as a list you can extend. An empty roster shows
// just the button; with the mouse off (button hidden) it falls back to a muted placeholder.
func (m Model) reviewersBlock(reviewers []domain.Reviewer, w int) string {
	btn := m.addButton(zoneEditReviewers, "Reviewer")
	if len(reviewers) == 0 {
		if btn == "" {
			return m.deps.Styles.Muted.Render("No reviewers yet.")
		}
		return btn
	}
	rows := make([]string, 0, len(reviewers)+2)
	for _, rv := range reviewers {
		rows = append(rows, m.reviewerRow(rv, w))
	}
	if btn != "" {
		rows = append(rows, "", btn)
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// reviewerRow is one reviewer: "  Name   STATUS", the status label coloured by the review decision.
// The status uses the shared colStatus column so it lines up with the children/parent/relation state.
func (m Model) reviewerRow(rv domain.Reviewer, w int) string {
	t := m.deps.Styles.Theme
	label, col := reviewStatus(t, rv.Status)
	status := lipgloss.NewStyle().Foreground(col).Render(fit(label, colStatus))
	nameW := max(6, w-colStatus-3) // 2-cell indent + one-cell gap
	name := lipgloss.NewStyle().Foreground(t.Text).Render(fit(orDash(rv.Name), nameW))
	return "  " + name + " " + status
}

// reviewStatus maps a review decision to a display label and colour (approved green, changes-requested
// red, pending muted).
func reviewStatus(t theme.Theme, status string) (string, color.Color) {
	switch status {
	case "APPROVED":
		return "Approved", t.Success
	case "CHANGES_REQUESTED":
		return "Changes requested", t.Error
	default: // PENDING (or unknown)
		return "Pending", t.Muted
	}
}

// customFieldsBlock renders the issue's custom fields, one per type: scalar fields inline (label +
// value), BOOLEAN as a coloured ✓/✗, PERCENTAGE as a bar, TEXT as an indented markdown block, and
// CHECKLIST as a checkbox list. Unset values render as "-".
func (m Model) customFieldsBlock(fields []domain.CustomField, w int) string {
	labelW := m.customLabelWidth(fields)
	var rows []string
	for _, f := range fields {
		rows = append(rows, m.customFieldRows(f, labelW, w)...)
	}
	// a TEXT field leaves a trailing blank after its block; drop it at the section's end so the gap to
	// the next section is not doubled (inter-field blanks are interior, so those stay in place).
	for len(rows) > 0 && strings.TrimSpace(rows[len(rows)-1]) == "" {
		rows = rows[:len(rows)-1]
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// fieldLabel is a custom field's label prefixed with its type glyph (empty on plain terminals, so the
// bare label shows), matching the icon the schema field picker uses for that type.
func (m Model) fieldLabel(f domain.CustomField) string {
	if g := m.deps.Glyphs.FieldTypeGlyph(f.Type); g != "" {
		return g + "  " + f.Label
	}
	return f.Label
}

// customLabelWidth is the shared label column width for the inline custom fields: the widest glyphed
// label plus a two-cell gap (so the value never abuts the label), clamped so a very long label neither
// collapses the value nor is over-truncated.
func (m Model) customLabelWidth(fields []domain.CustomField) int {
	w := 0
	for _, f := range fields {
		if lw := lipgloss.Width(components.Flatten(m.fieldLabel(f))); lw > w {
			w = lw
		}
	}
	return min(26, max(14, w+2))
}

func (m Model) customFieldRows(f domain.CustomField, labelW, w int) []string {
	t := m.deps.Styles.Theme
	label := lipgloss.NewStyle().Foreground(t.Muted).Width(labelW).Render(components.Trunc(m.fieldLabel(f), labelW))
	valueW := max(1, w-labelW)

	switch f.Type {
	case "TEXT":
		if strings.TrimSpace(f.Text) == "" {
			return []string{label + "-"}
		}
		// the rendered markdown gets a trailing blank line so it reads apart from the next field
		return append(append([]string{m.fieldHead(m.fieldLabel(f), w)}, indentLines(m.mdBlock(f.Text, "-", max(1, w-2)), 2)...), "")
	case "CHECKLIST":
		if len(f.Items) == 0 {
			return []string{label + "-"}
		}
		out := []string{m.fieldHead(m.fieldLabel(f), w)}
		for _, it := range f.Items {
			out = append(out, "  "+checkbox(t, it.Checked)+" "+components.Trunc(it.Name, max(1, w-6)))
		}
		return out
	case "BOOLEAN":
		return []string{label + m.boolValue(f.Bool)}
	case "PERCENTAGE":
		return []string{label + m.percentValue(f.Percent, valueW)}
	default: // SHORT_TEXT / INTEGER / DECIMAL / DATE / TIMESTAMP / SELECT_OPTION
		return []string{label + components.Trunc(orDash(f.Text), valueW)}
	}
}

// fieldHead is a field's own-line label for the block-style types (TEXT, CHECKLIST). Trunc flattens the
// untrusted server label (a stray control char would corrupt the frame) and clips it to the width, like
// every other label path in this file.
func (m Model) fieldHead(label string, w int) string {
	return lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Muted).Render(components.Trunc(label, w))
}

// boolValue renders a BOOLEAN custom field as a coloured ✓ (true) or ✗ (false), or "-" when unset.
func (m Model) boolValue(b *bool) string {
	t := m.deps.Styles.Theme
	if b == nil {
		return "-"
	}
	if *b {
		return lipgloss.NewStyle().Foreground(t.Success).Render("✓")
	}
	return lipgloss.NewStyle().Foreground(t.Error).Render("✗")
}

// percentValue renders a PERCENTAGE custom field as a bar plus the number, or "-" when unset.
func (m Model) percentValue(p *int, valueW int) string {
	if p == nil {
		return "-"
	}
	return m.progressValue(*p, valueW)
}

// checkbox is a coloured [✓] (checked) or [ ] (unchecked) marker for a checklist item. The brackets are
// ASCII so the marker keeps a stable width regardless of the terminal's symbol handling.
func checkbox(t theme.Theme, checked bool) string {
	if checked {
		return lipgloss.NewStyle().Foreground(t.Success).Render("[✓]")
	}
	return lipgloss.NewStyle().Foreground(t.Muted).Render("[ ]")
}

// indentLines prefixes every line of s with n spaces, so a multi-line block sits under its field label.
func indentLines(s string, n int) []string {
	pad := strings.Repeat(" ", n)
	lines := strings.Split(s, "\n")
	for i := range lines {
		lines[i] = pad + lines[i]
	}
	return lines
}

// childrenBlock renders the child issues, each as a compact row (key, Type colour chip, state). Aborted
// children sink to the bottom, muted, so the active ones read first - an aborted child otherwise shouts
// in the error state colour and clutters the "what's left" view. When canAdd, a "+ Child" button follows
// below (a blank line apart) so the section reads as a list you can extend; an empty list shows just the
// button, falling back to a muted placeholder with the mouse off (or when a child cannot be added).
func (m Model) childrenBlock(children []domain.IssueRef, canAdd bool, w int) string {
	var btn string
	if canAdd {
		btn = m.addButton(zoneAddChild, "Child")
	}
	if len(children) == 0 {
		if btn == "" {
			return m.deps.Styles.Muted.Render("No child issues yet.")
		}
		return btn
	}
	active := make([]domain.IssueRef, 0, len(children))
	var aborted []domain.IssueRef
	for _, c := range children {
		if c.StateCategory == "ABORTED" {
			aborted = append(aborted, c)
		} else {
			active = append(active, c)
		}
	}
	rows := make([]string, 0, len(children)+2)
	for _, c := range active {
		rows = append(rows, m.hierarchyRow(c, w, false))
	}
	for _, c := range aborted {
		rows = append(rows, m.hierarchyRow(c, w, true))
	}
	if btn != "" {
		rows = append(rows, "", btn)
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// hierarchyRow is one parent/child issue row: "  KEY  [Type chip]  STATE", aligned like the relations
// rows (the Type chip takes the column the relation rows spend on the title, since these have no title).
// When muted (an aborted child), the whole row is dimmed and the key struck through so it recedes.
func (m Model) hierarchyRow(r domain.IssueRef, w int, muted bool) string {
	t := m.deps.Styles.Theme
	typeW := max(6, w-colKey-colStatus-4) // 2-cell indent + two single-cell gaps
	if muted {
		mut := lipgloss.NewStyle().Foreground(t.Muted)
		key := m.peekLink("c", r.Key, lipgloss.NewStyle().Foreground(m.peekKeyColor("c", r.Key, t.Muted)).Strikethrough(true).Render(pad(fit(r.Key, colKey), colKey)))
		typeName := mut.Render(pad(fit(r.TypeName, typeW), typeW))
		state := mut.Render(fit(orDash(r.StateLabel), colStatus))
		return "  " + key + " " + typeName + " " + state
	}
	key := m.peekLink("c", r.Key, lipgloss.NewStyle().Foreground(m.peekKeyColor("c", r.Key, t.Primary)).Bold(true).Render(pad(fit(r.Key, colKey), colKey)))
	state := lipgloss.NewStyle().Foreground(stateColor(t, r.StateCategory)).Render(fit(orDash(r.StateLabel), colStatus))
	return "  " + key + " " + m.typeCell(r.TypeName, r.TypeColor, typeW) + " " + state
}

// commentsBlock renders the Details panel's comment thread (with per-comment Reply links), or a muted
// placeholder when empty. Only the first page rides along in the detail BFF; a "more" note is shown when
// further pages exist.
func (m Model) commentsBlock(d domain.IssueDetail, w int) string {
	if len(d.Comments) == 0 {
		return m.deps.Styles.Muted.Render("No comments yet.")
	}
	return m.commentThread(d, w, m.replyLink)
}

// commentThread renders the issue's comment thread. actionsFor renders one comment's trailing affordances
// (nil for none): the Details panel passes its mouse "Reply" link, the modal passes its focusable button
// row. It takes the depth because replying is a root-only action while editing is not.
func (m Model) commentThread(d domain.IssueDetail, w int, actionsFor func(domain.IssueComment, int) string) string {
	s := m.deps.Styles
	var rows []string
	for i, c := range d.Comments {
		if i > 0 {
			rows = append(rows, "")
		}
		rows = append(rows, m.commentRows(c, w, 0, actionsFor)...)
	}
	if d.CommentsHasMore {
		rows = append(rows, "", s.Muted.Render(fmt.Sprintf("… %d more (showing %d of %d)", d.CommentCount-len(d.Comments), len(d.Comments), d.CommentCount)))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// commentRows renders one comment (and its replies) as a header line plus its wrapped body. A reply is
// set off by a blank line and a muted vertical gutter, one bar per nesting level, so the thread reads.
// replyFor (nil for none) renders a root comment's reply affordance; the header reserves room for it.
func (m Model) commentRows(c domain.IssueComment, w, depth int, actionsFor func(domain.IssueComment, int) string) []string {
	s := m.deps.Styles
	t := s.Theme
	gutter := m.commentGutter(depth)
	gw := lipgloss.Width(gutter)

	name := flattenLine(c.AuthorName) // untrusted server text: a stray CR would overwrite the modal frame
	if name == "" {
		name = "Unknown"
	}
	author := name
	if u := flattenLine(c.AuthorUsername); u != "" && u != name {
		author = name + " (@" + u + ")" // "{name} (@{username})", matching the old Textual header
	}
	meta := ""
	if !c.CreatedAt.IsZero() {
		meta = " · " + formatLocalDay(c.CreatedAt)
	}
	if c.Edited {
		meta += " (edited)"
	}
	// a root comment's reply affordance rides the header; reserve its width so the (untrusted) author is
	// clamped to fit, since a long author would otherwise overflow and, in the floated modal, corrupt the
	// frame. replies thread only under a root comment, so only depth-0 comments get one.
	reply := ""
	if actionsFor != nil {
		reply = actionsFor(c, depth)
	}
	// a review's verdict is a separate coloured segment rather than part of the muted header text, so the
	// decision stays legible at a glance and survives the author truncation below
	badge := m.reviewBadge(c.ReviewStatus)
	budget := w - gw - lipgloss.Width(badge)
	if reply != "" {
		budget -= lipgloss.Width(reply)
	}
	header := gutter + lipgloss.NewStyle().Foreground(t.Muted).Render(components.Trunc(author+meta, max(1, budget))) + badge + reply

	var body string
	if c.Deleted {
		body = s.Muted.Render("[deleted]")
	} else {
		body = m.mdBlock(c.Content, "", max(10, w-gw)) // comments render as markdown, like the Content section
	}

	rows := []string{header}
	for _, line := range strings.Split(body, "\n") {
		rows = append(rows, gutter+line)
	}
	for _, r := range c.Replies {
		rows = append(rows, "") // a blank line sets a reply apart from the comment above it
		rows = append(rows, m.commentRows(r, w, depth+1, actionsFor)...)
	}
	return rows
}

// reviewBadge labels a comment that was submitted as a review with its frozen verdict, in the same colour
// the reviewer roster uses for that decision. An ordinary comment gets nothing.
func (m Model) reviewBadge(status string) string {
	if status == "" {
		return ""
	}
	t := m.deps.Styles.Theme
	g := m.deps.Glyphs
	label, col := reviewStatus(t, status)
	mark := g.Or(g.Check, "+")
	if status != "APPROVED" {
		mark = g.Or(g.Cross, "!")
	}
	return "  " + lipgloss.NewStyle().Foreground(col).Bold(true).Render(mark+" "+label)
}

// commentGutter is a reply's indent: one vertical bar per nesting level, so a reply reads as a branch of
// the comment above it. It borrows the Text color of a Details section title (sectionRule) so the thread's
// structure reads with the same weight as the "Children"/"Reviewers" rules. Depth 0 has no gutter.
func (m Model) commentGutter(depth int) string {
	if depth == 0 {
		return ""
	}
	return lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Text).Render(strings.Repeat("│ ", depth))
}

// replyLink is the per-comment "Reply" affordance: a zone-marked click target that opens the reply
// composer threaded under this comment. Muted, accenting on hover, and omitted for a deleted comment.
func (m Model) replyLink(c domain.IssueComment, depth int) string {
	if c.Deleted || c.ID == 0 || m.peeking || depth > 0 {
		// read-only in the peek modal; and a reply cannot be replied to (the server caps nesting at 1)
		return ""
	}
	col := m.deps.Styles.Theme.Muted
	if m.hover == commentReplyZone(c.ID) {
		col = m.deps.Styles.Theme.Accent
	}
	g := m.deps.Glyphs
	link := lipgloss.NewStyle().Foreground(col).Render(g.Reply + " Reply") // Reply is the hardcoded ↩ in both modes
	return "  " + zone.Mark(commentReplyZone(c.ID), link)
}

func commentReplyZone(id int64) string { return "project.comment.reply." + strconv.FormatInt(id, 10) }

// mdBlock renders an issue's markdown body to a styled, width-wrapped block, or a muted placeholder
// when it is empty. The text is sanitized first so a stray control character cannot corrupt the frame.
func (m Model) mdBlock(text, empty string, w int) string {
	text = sanitizeBlock(text)
	if strings.TrimSpace(text) == "" {
		return m.deps.Styles.Muted.Render(empty)
	}
	return components.Markdown(text, w, components.IsDark(m.deps.Styles.Theme.Background))
}

// detailSkeleton is the placeholder shown while the detail loads: shimmer bars around a loading note.
func detailSkeleton(s theme.Styles, w int) string {
	bar := func(frac float64) string {
		n := int(float64(w) * frac)
		if n < 1 {
			n = 1
		}
		return s.Muted.Render(strings.Repeat("░", n))
	}
	return lipgloss.JoinVertical(lipgloss.Left,
		bar(0.55), "",
		s.Muted.Render("Loading issue…"), "",
		bar(0.9), bar(0.8), bar(0.85), bar(0.5), "",
		bar(0.7), bar(0.6),
	)
}

// flattenLine collapses every control character (newlines included) to a space, for a single-line
// field like the title.
func flattenLine(s string) string {
	return strings.Map(func(r rune) rune {
		if r < 0x20 || r == 0x7f {
			return ' '
		}
		return r
	}, s)
}

// sanitizeBlock keeps the newlines that give a multi-line body its line breaks but strips every other
// control character, so a lone carriage return in server free text cannot reset the cursor to column
// zero and overwrite the modal frame.
func sanitizeBlock(s string) string {
	return strings.Map(func(r rune) rune {
		if r == '\n' {
			return r
		}
		if r < 0x20 || r == 0x7f {
			return ' '
		}
		return r
	}, s)
}

// contentHeader is the "Content" section rule with a right-anchored edit pen (mouse only; E does the same
// from the keyboard). The rule is rendered shorter by the pen's width so the dashes stop cleanly before
// it (reusing rightAlignAction would Trunc the already-full-width rule and stamp a stray "…" into it).
func (m Model) contentHeader(w int) string {
	pen := m.penAction(zoneEditContent, m.deps.Glyphs.PenSquare, "edit")
	if pen == "" {
		return sectionRule(m.deps.Styles, "Content", w)
	}
	return sectionRule(m.deps.Styles, "Content", max(1, w-lipgloss.Width(pen))) + pen
}

// canAddChild reports whether a child issue can be created under d: its type resolves to a hierarchy with
// a level below it, and the catalog has at least one type at that child level.
func (m Model) canAddChild(d domain.IssueDetail) bool {
	hier, ok := m.hierarchyForType(d.TypeName)
	if !ok {
		return false
	}
	childHier, ok := childHierarchy(hier)
	if !ok {
		return false
	}
	return len(m.typeIDsAtHierarchy(childHier)) > 0
}

// sectionRule is a bold title followed by a rule filling the rest of the width.
func sectionRule(s theme.Styles, title string, w int) string {
	head := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true).Render(title)
	dashes := w - lipgloss.Width(title) - 1
	if dashes < 0 {
		dashes = 0
	}
	return head + s.Muted.Render(" "+strings.Repeat("─", dashes))
}

func storyPointLabel(sp int) string {
	if sp <= 0 {
		return "-"
	}
	return strconv.Itoa(sp)
}

func orDash(s string) string {
	if s == "" {
		return "-"
	}
	return s
}

// formatLocalDay renders an instant as the calendar day it falls on in the viewer's own timezone. Every
// timestamp the server sends is an absolute instant serialized as UTC, so formatting it raw would show
// UTC's day - which, east of Greenwich, is still yesterday for anything before local mid-morning.
func formatLocalDay(t time.Time) string {
	if t.IsZero() {
		return "-"
	}
	return t.Local().Format("2006-01-02")
}

// formatDateOnly renders a date-only value - a due date, which the wire carries as UTC midnight of the
// chosen day. It is deliberately NOT localized: west of Greenwich that midnight falls on the previous
// day, so localizing would quietly move every due date a day earlier.
func formatDateOnly(t time.Time) string {
	if t.IsZero() {
		return "-"
	}
	return t.UTC().Format("2006-01-02")
}

// modalView centers the filter modal over the dimmed Issues tab, splicing it in by hand (not
// lipgloss.NewCompositor, which drops zone marks) so it stays clickable, matching home.
func (m Model) modalView() string {
	t := m.deps.Styles.Theme
	backdrop := stripANSI(lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Top, m.issuesTab()))
	modal, _, _ := components.ScrollBox(m.filterUI.View(), m.height, m.modalScroll, t.Primary, t.Border)
	mx := max(0, (m.width-lipgloss.Width(modal))/2)
	my := max(0, (m.height-lipgloss.Height(modal))/2)
	return overlayDim(backdrop, modal, mx, my, t.Muted)
}

// issuesTab lays out the search row + issue list. On a wide terminal the Details panel sits to the
// right of the list; on a narrow one the list fills the width and the detail shows as a read-only modal
// (opened from the list, floated in issuesView).
func (m Model) issuesTab() string {
	if m.narrow() {
		leftW := m.innerWidth()
		left := lipgloss.JoinVertical(lipgloss.Left, m.searchRow(leftW), m.listPanel(leftW, m.height-searchRowH))
		return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, left)
	}
	leftW, detailW, activityW := m.panelWidths()
	left := lipgloss.JoinVertical(lipgloss.Left, m.searchRow(leftW), m.listPanel(leftW, m.height-searchRowH))
	if m.threeCol() {
		// list | Details | Activity, each its own column
		content := lipgloss.JoinHorizontal(lipgloss.Top,
			left, " ", m.detailPanel(detailW, m.height), " ", m.activityPanel(activityW, m.height))
		return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, content)
	}
	// two columns: the right panel shows Activity when toggled on (no room for a third), else Details
	right := m.detailPanel(detailW, m.height)
	if m.showActivity {
		right = m.activityPanel(detailW, m.height)
	}
	content := lipgloss.JoinHorizontal(lipgloss.Top, left, " ", right)
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, content)
}

func (m Model) innerWidth() int { return m.width - 2*hInset }

// narrow reports whether the terminal is too narrow for a side Details panel, so the list goes full
// width and the detail shows as a read-only modal instead.
func (m Model) narrow() bool { return m.width < sideMinW }

// minWidthFloor is the smallest width that still renders: the full-width list must fit. The side-by-side
// split degrades to the narrow list+modal layout below sideMinW, so the floor is the list's width.
func (m Model) minWidthFloor() int { return listFloorW }

// threeCol reports whether the toggled-on Activity view gets its own third column beside Details (only
// when side by side and wide enough); otherwise Activity swaps in for Details.
func (m Model) threeCol() bool { return !m.narrow() && m.showActivity && m.width >= triColMinW }

// panelWidths splits the inner width into the list, detail, and (when the third column shows) activity
// columns, each separated by a one-cell gap. The list is half the width (list:detail = 1:1); toggling
// Activity on keeps the list width and splits the other half into Details and Activity. In narrow mode
// there is no side panel, so the list takes the full inner width; in the two-column swap the activity
// view reuses the detail width, so activity is 0 here (only the true third column reports a width).
func (m Model) panelWidths() (list, detail, activity int) {
	if m.narrow() {
		return m.innerWidth(), 0, 0
	}
	inner := m.innerWidth()
	list = max(listColsMinW, inner/2) // 1:1 list:detail, floored so the issue table still fits
	rest := inner - list - 1          // the Details (and, when shown, Activity) region after a one-cell gap
	if m.threeCol() {
		region := rest - 1      // Details + Activity, after the one-cell gap between them
		detail = region * 3 / 5 // Details:Activity = 3:2, so Activity reads as a slimmer side rail
		return list, detail, region - detail
	}
	return list, rest, 0
}

// trailingButtonsW is the width the filter + New buttons and their one-cell gaps take at the right of
// the search row. They are click-only, so with the mouse off they are hidden and the search box reclaims
// the space (the f / n keys still open the filter and the create form).
func (m Model) trailingButtonsW() int {
	if !m.deps.Mouse {
		return 0
	}
	return filterButtonW + 1 + newButtonW + 1
}

// searchBoxWidth is the outer width of the search box: the list column width less the trailing buttons,
// so the search row exactly fills the list column below.
func (m Model) searchBoxWidth() int {
	leftW, _, _ := m.panelWidths()
	return max(1, leftW-m.trailingButtonsW())
}

func (m Model) searchInputWidth() int {
	iconW := lipgloss.Width(m.deps.Glyphs.Search) + 1
	return max(1, m.searchBoxWidth()-4-iconW)
}

// searchRow is the search box and, beside it (mouse on), the filter and New buttons, together filling w.
func (m Model) searchRow(w int) string {
	t := m.deps.Styles.Theme
	boxW := w - m.trailingButtonsW()
	border := t.Muted
	switch {
	case m.focus == focusSearch:
		border = t.Accent
	case m.hover == zoneSearch:
		border = t.Secondary
	}
	icon := lipgloss.NewStyle().Foreground(t.Muted).Render(m.deps.Glyphs.Search)
	inner := icon + " " + m.search.View()
	inputBody := lipgloss.NewStyle().Width(boxW - 4).MaxWidth(boxW - 4).MaxHeight(1).Render(inner)
	box := zone.Mark(zoneSearch, components.TitledBoxWeighted("Search", inputBody, border, m.focus == focusSearch))
	if !m.deps.Mouse {
		return box // the filter/New buttons are click-only, so the box fills the whole row
	}
	return lipgloss.JoinHorizontal(lipgloss.Top, box, " ", m.filterButton(), " ", m.newButton())
}

// newButton opens the New issue form; it glows on hover, matching the filter button's affordance.
func (m Model) newButton() string {
	t := m.deps.Styles.Theme
	col := t.Muted
	if m.hover == zoneNew {
		col = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(col).Bold(true).Render("+")
	return zone.Mark(zoneNew, components.TitledBoxWeighted("", body, col, false))
}

// filterButton glows accent while focused or while a non-default filter is active.
func (m Model) filterButton() string {
	t := m.deps.Styles.Theme
	col := t.Muted
	switch {
	case m.focus == focusFilter:
		col = t.Accent
	case m.hover == zoneFilter:
		col = t.Secondary
	case m.filterActive():
		col = t.Accent
	}
	g := m.deps.Glyphs.Or(m.deps.Glyphs.Filter, "F")
	body := lipgloss.NewStyle().Foreground(col).Bold(true).Render(g)
	return zone.Mark(zoneFilter, components.TitledBoxWeighted("", body, col, m.focus == focusFilter))
}

// filterActive reports whether the applied filter differs from the default open-issues view (the
// search keyword is excluded — the search box signals that on its own).
func (m Model) filterActive() bool {
	f := m.filter
	if len(f.Priorities) > 0 || len(f.IssueTypeIDs) > 0 || f.AssigneeMe {
		return true
	}
	def := domain.OpenIssuesFilter().StateCategories
	if len(f.StateCategories) != len(def) {
		return true
	}
	for i, s := range f.StateCategories {
		if s != def[i] {
			return true
		}
	}
	return false
}

func (m Model) tabLabel() string {
	for _, pt := range projectTabs {
		if pt.tab == m.tab {
			return pt.label
		}
	}
	return ""
}

// HeaderInfo is the project drill-in's header-right content: a back affordance to the projects list
// and the project tabs. The app shell draws it in the header band. The back zone keeps mouse users a
// way home now that esc is the keyboard route. On a narrow (compact) header the back link is dropped so
// the tabs still fit (backspace/esc remain the keyboard route home).
func (m Model) HeaderInfo(compact bool) string {
	t := m.deps.Styles.Theme
	active := lipgloss.NewStyle().Foreground(t.Accent).Bold(true).Underline(true)
	inactive := lipgloss.NewStyle().Foreground(t.Muted)
	numActive := lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	numInactive := lipgloss.NewStyle().Foreground(t.Muted)
	cells := make([]string, 0, len(projectTabs))
	for i, pt := range projectTabs {
		style, numStyle := inactive, numInactive
		switch {
		case m.tab == pt.tab:
			style, numStyle = active, numActive
		case m.hover == pt.zone:
			style = lipgloss.NewStyle().Foreground(t.Secondary)
		}
		content := pt.label
		if gl := m.tabGlyph(pt.tab); gl != "" {
			content = gl + " " + pt.label
		}
		cell := numStyle.Render(strconv.Itoa(i+1)+": ") + style.Render(content)
		cells = append(cells, zone.Mark(pt.zone, cell))
	}
	tabs := strings.Join(cells, "  ")
	if compact {
		return tabs // a narrow header drops the back link so the tabs still fit
	}
	backStyle := lipgloss.NewStyle().Foreground(t.Accent)
	if m.hover == zoneBack {
		backStyle = backStyle.Underline(true)
	}
	back := zone.Mark(zoneBack, backStyle.Render("‹ Projects"))
	sep := lipgloss.NewStyle().Foreground(t.Muted).Render("   ")
	return back + sep + tabs
}

func (m Model) tabGlyph(tab projectTab) string {
	g := m.deps.Glyphs
	switch tab {
	case tabIssues:
		return g.Or(g.List, "")
	case tabSprints:
		return g.Or(g.Run, "")
	case tabStats:
		return g.Or(g.Project, "")
	case tabMembers:
		return g.Or(g.People, "")
	case tabConfig:
		return g.Or(g.Gear, "")
	}
	return ""
}

func (m Model) placeholder(name string) string {
	s := m.deps.Styles
	msg := s.Muted.Render(name + " - coming soon")
	return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center, msg)
}

// listPanel is a titled borderless panel matching Projects/Schema/Agents. TitledRule insets its body
// by two cells each side, so the body is sized w-4 to make the panel exactly w wide.
func (m Model) listPanel(w, h int) string {
	t := m.deps.Styles.Theme
	bodyW := max(10, w-4)
	body := lipgloss.NewStyle().Width(bodyW).Height(h - 2).MaxHeight(h - 2).Render(m.listBody(bodyW, h-2))
	return zone.Mark("project.list", components.TitledRule(m.listTitle(), m.listCounter(), body, t.Primary))
}

func (m Model) listTitle() string {
	if m.loading {
		return "Issues (loading)"
	}
	return fmt.Sprintf("Issues (%d)", m.page.TotalElements)
}

func (m Model) listCounter() string {
	if m.loading || len(m.issues) == 0 {
		return ""
	}
	more := ""
	if m.page.HasNext {
		more = " · ↓ more"
	}
	return fmt.Sprintf("%d/%d%s", len(m.issues), m.page.TotalElements, more)
}

func (m Model) listBody(w, avail int) string {
	s := m.deps.Styles
	switch {
	case m.loading:
		return s.Muted.Render("Loading issues…")
	case m.loadErr:
		return s.Error.Render("Failed to load issues. Press R to retry.")
	case len(m.issues) == 0:
		if m.filter.Keyword != "" {
			return s.Muted.Render("No issues match your search.")
		}
		return s.Muted.Render("No open issues in this project.")
	}

	titleW := m.titleWidth(w)
	rows := []string{m.headerRow(titleW)}

	// reserve the header row (and, when shown, the "Loading more…" row) out of the visible budget so
	// neither the indicator nor the last issue row is clipped by the panel's MaxHeight.
	chrome := 1
	if m.loadingMore {
		chrome++
	}
	visible := max(1, (avail-chrome)/rowH)
	top := listTop(m.cursor, visible, len(m.issues))
	for j := top; j < len(m.issues) && j < top+visible; j++ {
		// the airy blank separator sits OUTSIDE zone.Mark so click/hover still map to the content line
		rows = append(rows, "")
		rows = append(rows, zone.Mark(issueRowZone(j), m.issueRow(m.issues[j], j, titleW, w, m.hover == issueRowZone(j))))
	}
	if m.loadingMore {
		rows = append(rows, s.Muted.Render("Loading more…"))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// titleWidth trims the Title column to ~60% of the space it could take, keeping the table compact and
// leaving the right of each row free for the detail panel that lands next.
func (m Model) titleWidth(w int) int {
	avail := max(1, w-colKey-colType-colAsg-colState-colPri-colAct-6) // 6 single-column gaps
	titleW := avail * 3 / 5
	if titleW < 18 {
		titleW = min(18, avail) // give the title the rest, but never overflow the row (cols sum stays <= w)
	}
	return titleW
}

func (m Model) headerRow(titleW int) string {
	g := m.deps.Glyphs
	head := lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Muted).Bold(true)
	cols := []string{
		pad(g.Or(g.LastUpdated, "◷"), colAct),
		pad("Key", colKey), pad("Type", colType), pad("Title", titleW),
		pad("Assignee", colAsg), pad("State", colState), pad(g.Or(g.Priority, "Pri"), colPri),
	}
	return head.Render(strings.Join(cols, " "))
}

func (m Model) issueRow(it domain.IssueSummary, i, titleW, w int, hovered bool) string {
	t := m.deps.Styles.Theme
	sel := i == m.cursor
	banded := sel || hovered

	// Plain, exact-width cells. When the row is banded (selected or hovered) they stay plain text so the
	// full-width highlight paints them uniformly — the same look as the Projects and Schema lists. When
	// not banded each cell carries its own colour (the Type chip, state and priority hues).
	act := pad(fit(components.HumanizeSince(it.LastActivity), colAct), colAct)
	key := pad(fit(it.Key, colKey), colKey)
	typ := pad(fit(it.TypeName, colType), colType)
	title := pad(fit(it.Title, titleW), titleW)
	asg := pad("-", colAsg)
	if it.Assigned {
		asg = pad(fit(assigneeLabel(it), colAsg), colAsg)
	}
	state := pad(fit(it.StateLabel, colState), colState)
	pri := pad(it.Priority, colPri)

	if !banded {
		txt := lipgloss.NewStyle().Foreground(t.Text)
		muted := lipgloss.NewStyle().Foreground(t.Muted)
		act = muted.Render(act)
		key = txt.Render(key)
		typ = m.typeCell(it.TypeName, it.TypeColor, colType)
		title = txt.Render(title)
		if it.Assigned {
			asg = txt.Render(asg)
		} else {
			asg = muted.Render(asg)
		}
		state = lipgloss.NewStyle().Foreground(stateColor(t, it.StateCategory)).Render(state)
		pri = lipgloss.NewStyle().Foreground(priorityColor(t, it.Priority)).Render(pri)
	}

	row := strings.Join([]string{act, key, typ, title, asg, state, pri}, " ")
	switch {
	case sel:
		row = m.selBand().Width(w).Render(row)
	case hovered:
		row = m.hoverBand().Width(w).Render(row)
	}
	return row
}

// typeCell renders the Type column as a background colour chip - the type name painted on its own
// colour with a contrasting text colour, like the Schema tab's hierarchy chips - padded to width.
// Falls back to muted plain text when the type has no colour.
func (m Model) typeCell(name, colorName string, width int) string {
	muted := lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Muted)
	label := components.Trunc(name, max(1, width-2)) // the chip pads one space each side
	chip, ok := components.ColorChip(colorName, label)
	if !ok {
		return muted.Render(pad(fit(name, width), width))
	}
	// left-align the chip; the trailing pad carries no background so the column stays aligned
	return chip + strings.Repeat(" ", max(0, width-lipgloss.Width(chip)))
}

// assigneeLabel is the assignee's name, falling back to their member id when the name did not resolve
// (an inactive member the batched name lookup skipped).
func assigneeLabel(it domain.IssueSummary) string {
	if it.AssigneeName != "" {
		return it.AssigneeName
	}
	return "#" + strconv.FormatInt(it.AssigneeID, 10)
}

// selBand is the solid highlight the selected (cursor) row gets, matching the Projects and Schema
// lists: the row's text on the Selection background, bold.
func (m Model) selBand() lipgloss.Style {
	t := m.deps.Styles.Theme
	return lipgloss.NewStyle().Foreground(t.Text).Background(t.Selection).Bold(true)
}

// hoverBand is the subtle background band a mouse-hovered row gets, dimmer than the selection. On the
// ANSI theme (no real background to dim) it tints the text instead.
func (m Model) hoverBand() lipgloss.Style {
	t := m.deps.Styles.Theme
	if _, noBg := t.Background.(lipgloss.NoColor); noBg {
		return lipgloss.NewStyle().Foreground(t.Secondary)
	}
	return lipgloss.NewStyle().Foreground(t.Text).Background(components.MixColors(t.Selection, t.Background, 0.5))
}

func stateColor(t theme.Theme, cat string) color.Color {
	switch cat {
	case "COMPLETED":
		return t.Success
	case "ACTIVE":
		return t.Primary
	case "ABORTED":
		return t.Error
	default: // INITIAL
		return t.Muted
	}
}

func priorityColor(t theme.Theme, p string) color.Color {
	switch p {
	case "P0":
		return t.Error
	case "P1":
		return t.Warning
	case "P2":
		return t.Primary // the middle/default priority, set apart from the muted low ones (P3/P4)
	default:
		return t.Muted
	}
}

func listTop(cursor, visible, n int) int {
	top := 0
	if cursor >= visible {
		top = cursor - visible + 1
	}
	if maxTop := n - visible; top > maxTop {
		top = maxTop
	}
	if top < 0 {
		top = 0
	}
	return top
}

// pad fits s to w cells. MaxHeight(1) stops an over-wide cell wrapping into a second row and breaking the table.
func pad(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return lipgloss.NewStyle().Width(w).MaxWidth(w).MaxHeight(1).Render(s)
}

func fit(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return lipgloss.NewStyle().Width(w).MaxWidth(w).MaxHeight(1).Render(components.Flatten(s))
}
