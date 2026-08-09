package project

import (
	"context"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// loadSprintList kicks off the sprint list fetch, flagging the request so re-opening the tab does not
// refetch a list already loaded. Used both for the first open (via selectTab) and to retry after a
// failed load (reopening the tab, which clears sprintsRequested).
func (m Model) loadSprintList() (Model, tea.Cmd) {
	m.sprintsRequested = true
	m.sprintsLoading = true
	m.sprintsErr = false
	m.sprintReqGen++
	return m, loadSprints(m.deps, m.projectKey, m.sprintReqGen)
}

func (m Model) onSprintsLoaded(msg sprintsLoadedMsg) (Model, tea.Cmd) {
	if msg.key != m.projectKey || msg.gen != m.sprintReqGen {
		// a stale load from a previously-opened project (New resets sprintReqGen, so generations collide
		// across projects) or a superseded reload landed late
		return m, nil
	}
	m.sprintsLoading = false
	if msg.err {
		if len(m.sprints) > 0 {
			// a silent post-action refresh failed but the list we were showing is still good: keep it
			// (a wiped list plus the action's success toast would contradict each other) and just note the
			// hiccup, mirroring the issue list's append-failure handling
			m.sprintRestoreID = 0
			// the shown list may now be stale about which sprint is active (e.g. we just completed it), so the
			// issue tab's currentSprint pointer can no longer be trusted: re-fetch it authoritatively
			m.currentSprintGen++
			return m, tea.Batch(
				toast.Show(toast.Error, "Couldn't refresh sprints."),
				loadCurrentSprint(m.deps, m.projectKey, m.currentSprintGen),
			)
		}
		m.sprintsErr = true
		m.sprintsRequested = false // let reopening the tab retry, so no reload key is needed
		return m, nil
	}
	m.sprintsErr = false
	m.sprintPage = msg.page
	m.sprints = sortSprintsRecentFirst(msg.page.Sprints)
	// the freshly loaded list is the authoritative source of the active sprint: take ownership by bumping
	// currentSprintGen so any in-flight (prefetch or lifecycle) loadCurrentSprint cannot land late and
	// clobber this value with an older snapshot.
	m.currentSprint = findActiveSprint(m.sprints)
	m.currentSprintGen++
	// a drill-in from a sprint notification asked to land on a specific sprint: select it and load its
	// issues (syncSprintSelection), taking precedence over the cursor-restore / default-selection paths.
	if id := m.navSprintID; id != 0 {
		m.navSprintID = 0
		if idx := indexOfSprint(m.sprints, id); idx >= 0 {
			m.sprintCursor = idx
			m.selSprintID = 0 // force syncSprintSelection to load the notified sprint's issues
			return m.syncSprintSelection()
		}
		// the notified sprint isn't in the single loaded page (only possible with many sprints, and never
		// for a recent start/complete, which sort to the top): fall through to the default selection so we
		// still land on the Sprints tab rather than pinning the wrong sprint as the target.
	}
	// after a start/complete/edit the list is reloaded to reflect the new status/fields: keep the same
	// sprint selected (its issues are unchanged, so no refetch) - the detail re-renders from the updated
	// summary. selSprintID is unchanged across the reload, so syncSprintSelection would no-op anyway.
	if id := m.sprintRestoreID; id != 0 {
		m.sprintRestoreID = 0
		if idx := indexOfSprint(m.sprints, id); idx >= 0 {
			m.sprintCursor = idx
			m.selSprintID = id // keep the anchor consistent with the cursor even if the user moved during the reload
			return m, nil
		}
	}
	if m.sprintCursor >= len(m.sprints) {
		m.sprintCursor = max(0, len(m.sprints)-1)
	}
	m.selSprintID = 0 // force syncSprintSelection to load the now-selected sprint's issues
	return m.syncSprintSelection()
}

func (m Model) onSprintIssuesLoaded(msg sprintIssuesLoadedMsg) (Model, tea.Cmd) {
	if msg.gen != m.sprintIssuesGen[msg.sprintID] {
		return m, nil // a superseded load for this sprint landed late
	}
	delete(m.sprintIssuesPending, msg.sprintID)
	if msg.err {
		m.sprintIssuesFailed[msg.sprintID] = true
		return m, nil
	}
	m.sprintIssuesFailed[msg.sprintID] = false
	m.sprintIssues[msg.sprintID] = msg.page
	if msg.sprintID == m.selSprintID {
		m.sprintIssueScroll = clampScroll(m.sprintIssueScroll, m.sprintIssueScrollMax())
	}
	return m, nil
}

// onSprintListKey drives the sprint list while the Sprints tab is showing: the cursor keys move the
// selection (which loads that sprint's detail and issues on the right). The 1-5 tab switch and the
// leave key are handled ahead of this in onListKey.
func (m Model) onSprintListKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	m.hover = "" // the keyboard is driving now, so drop any stale mouse-hover highlight
	switch msg.String() {
	case "up", "k":
		return m.moveSprintCursor(-1)
	case "down", "j":
		return m.moveSprintCursor(1)
	case "home", "g":
		m.sprintCursor = 0
		return m.syncSprintSelection()
	case "end", "G":
		m.sprintCursor = max(0, len(m.sprints)-1)
		return m.syncSprintSelection()
	case "n":
		return m.openSprintCreateForm()
	case "e":
		return m.openSprintEditForm()
	case "d":
		return m.openSprintDelete()
	case "s":
		return m.openSprintStart()
	case "c":
		return m.openSprintComplete()
	case "x":
		return m.openSprintCancel()
	case "r":
		return m.openSprintRemoveIssuePicker()
	case "m":
		return m.openMigrate()
	}
	return m, nil
}

// moveSprintCursor moves the sprint selection by delta, clamped to the loaded list. The list loads a
// single page, so there is no load-more at the bottom (a "… N more" note flags any overflow).
func (m Model) moveSprintCursor(delta int) (Model, tea.Cmd) {
	if len(m.sprints) == 0 {
		return m, nil
	}
	next := m.sprintCursor + delta
	if next < 0 {
		next = 0
	}
	if next >= len(m.sprints) {
		next = len(m.sprints) - 1
	}
	m.sprintCursor = next
	return m.syncSprintSelection()
}

// syncSprintSelection points the right panels at the cursor's sprint, resetting their scroll and
// loading that sprint's issues (SWR: a cached page shows at once, otherwise a deduped fetch). A no-op
// when the selection is unchanged.
func (m Model) syncSprintSelection() (Model, tea.Cmd) {
	sp, ok := m.selectedSprint()
	if !ok {
		m.selSprintID = 0
		return m, nil
	}
	if sp.ID == m.selSprintID {
		return m, nil
	}
	m.selSprintID = sp.ID
	m.sprintDetailScroll = 0
	m.sprintIssueScroll = 0
	if _, cached := m.sprintIssues[sp.ID]; !cached && !m.sprintIssuesPending[sp.ID] {
		return m, m.startSprintIssuesLoad(sp.ID)
	}
	return m, nil
}

func (m Model) selectedSprint() (domain.SprintSummary, bool) {
	if m.sprintCursor < 0 || m.sprintCursor >= len(m.sprints) {
		return domain.SprintSummary{}, false
	}
	return m.sprints[m.sprintCursor], true
}

// startSprintIssuesLoad bumps the load generation for a sprint and returns the fetch command, so a
// superseded in-flight load for the same sprint is ignored when it lands (mirrors startDetailLoad).
// Pointer receiver so the bump propagates through the value-receiver syncSprintSelection.
func (m *Model) startSprintIssuesLoad(id int64) tea.Cmd {
	m.sprintIssuesGen[id]++
	m.sprintIssuesPending[id] = true
	m.sprintIssuesFailed[id] = false
	// the report is fetched in lockstep with the issues so the summary never disagrees with the list below
	// it - every path that refreshes a sprint's issues (select, add/remove, migrate) refreshes its report too
	m.sprintReportGen[id]++
	m.sprintReportPending[id] = true
	m.sprintReportFailed[id] = false
	return tea.Batch(
		loadSprintIssues(m.deps, m.projectKey, id, m.sprintIssuesGen[id]),
		loadSprintReport(m.deps, m.projectKey, id, m.sprintReportGen[id]),
	)
}

// onSprintReportLoaded stores a sprint's report snapshot (mirrors onSprintIssuesLoaded): a superseded load
// is dropped by generation, a failure is remembered so the block reads "unavailable" instead of hanging.
func (m Model) onSprintReportLoaded(msg sprintReportLoadedMsg) (Model, tea.Cmd) {
	if msg.gen != m.sprintReportGen[msg.sprintID] {
		return m, nil
	}
	delete(m.sprintReportPending, msg.sprintID)
	if msg.err {
		m.sprintReportFailed[msg.sprintID] = true
		return m, nil
	}
	m.sprintReportFailed[msg.sprintID] = false
	m.sprintReport[msg.sprintID] = msg.report
	return m, nil
}

// onSprintWheel routes the wheel to whichever sprint panel is under the cursor: the detail and issues
// panels scroll independently, and the wheel over the sprint list moves the cursor.
func (m Model) onSprintWheel(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	if zone.Get(zoneSprintDetail).InBounds(msg) {
		m.sprintDetailScroll = wheelClamp(m.sprintDetailScroll, msg.Button, m.sprintDetailScrollMax())
		return m, nil
	}
	if zone.Get(zoneSprintIssues).InBounds(msg) {
		m.sprintIssueScroll = wheelClamp(m.sprintIssueScroll, msg.Button, m.sprintIssueScrollMax())
		return m, nil
	}
	switch msg.Button {
	case tea.MouseWheelUp:
		return m.moveSprintCursor(-1)
	case tea.MouseWheelDown:
		return m.moveSprintCursor(1)
	}
	return m, nil
}

type sprintsLoadedMsg struct {
	key  string // the project the load was for, so a stale cross-project result is ignored
	gen  int
	page domain.SprintPage
	err  bool
}

func loadSprints(d deps.Deps, projectKey string, gen int) tea.Cmd {
	return func() tea.Msg {
		p, err := d.Sprints.ListProjectSprints(context.Background(), projectKey, 0, pageSize)
		return sprintsLoadedMsg{key: projectKey, gen: gen, page: p, err: err != nil}
	}
}

type sprintIssuesLoadedMsg struct {
	sprintID int64
	gen      int
	page     domain.IssuePage
	err      bool
}

func loadSprintIssues(d deps.Deps, projectKey string, sprintID int64, gen int) tea.Cmd {
	return func() tea.Msg {
		f := domain.IssueFilter{SprintIDs: []int64{sprintID}}
		p, err := d.Issues.SearchProjectIssues(context.Background(), projectKey, f, 0, pageSize)
		return sprintIssuesLoadedMsg{sprintID: sprintID, gen: gen, page: p, err: err != nil}
	}
}

type sprintReportLoadedMsg struct {
	sprintID int64
	gen      int
	report   domain.SprintReport
	err      bool
}

func loadSprintReport(d deps.Deps, projectKey string, sprintID int64, gen int) tea.Cmd {
	return func() tea.Msg {
		r, err := d.Projects.GetSprintReport(context.Background(), projectKey, sprintID)
		return sprintReportLoadedMsg{sprintID: sprintID, gen: gen, report: r, err: err != nil}
	}
}
