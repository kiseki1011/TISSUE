package project

import (
	"time"

	tea "charm.land/bubbletea/v2"
)

// rtReloadDebounce coalesces a burst of new-issue events into one silent list reload.
const rtReloadDebounce = 300 * time.Millisecond

// RealtimeIssueEventMsg is one issue SSE event for this project. Type is the backend's "ISSUE_*" name.
type RealtimeIssueEventMsg struct {
	Type     string
	IssueKey string
}

// realtimeReloadMsg is the debounced trigger for the silent reload. seq drops a superseded one.
type realtimeReloadMsg struct{ seq int }

// RealtimeSprintEventMsg is one sprint SSE event. IssueKeys is set only on SPRINT_ISSUES_ADDED/REMOVED.
type RealtimeSprintEventMsg struct {
	Type      string
	SprintID  int64
	IssueKeys []string
}

// realtimeSprintReloadMsg is the debounced trigger for the silent sprint-list reload. seq drops a stale one.
type realtimeSprintReloadMsg struct{ seq int }

// ProjectKey lets the app shell route realtime events for the matching project here.
func (m Model) ProjectKey() string { return m.projectKey }

func (m Model) indexOfIssue(key string) int {
	for i := range m.issues {
		if m.issues[i].Key == key {
			return i
		}
	}
	return -1
}

func (m Model) blockingModalOpen() bool {
	return m.editing || m.editingContent || m.commenting || m.deleting ||
		m.picking || m.creating || m.filtering || m.dating || m.peeking
}

// onRealtimeIssueEvent folds one issue event in. Self-echo is not filtered: the refetch is idempotent.
func (m Model) onRealtimeIssueEvent(msg RealtimeIssueEventMsg) (Model, tea.Cmd) {
	switch msg.Type {
	case "ISSUE_CREATED":
		m.rtReloadSeq++
		seq := m.rtReloadSeq
		return m, tea.Tick(rtReloadDebounce, func(time.Time) tea.Msg { return realtimeReloadMsg{seq: seq} })
	case "ISSUE_DELETED":
		return m.realtimeRemoveIssue(msg.IssueKey)
	default:
		return m.realtimeRefreshIssue(msg.IssueKey)
	}
}

// realtimeRefreshIssue refetches only a viewed/peeked/cached issue: a fetch per foreign event is wasteful.
// Known: it can flicker new->old->new against an in-flight optimistic write.
func (m Model) realtimeRefreshIssue(key string) (Model, tea.Cmd) {
	if key == "" {
		return m, nil
	}
	_, cached := m.details[key]
	relevant := key == m.viewKey || cached || (m.peeking && m.peekKey == key)
	if !relevant {
		return m, nil
	}
	cmds := []tea.Cmd{m.startDetailLoad(key)} // the cached copy stays visible until the fresh detail lands
	if _, ok := m.activities[key]; ok {
		cmds = append(cmds, m.startActivityLoad(key))
	}
	return m, tea.Batch(cmds...)
}

// realtimeRemoveIssue drops the row. Under a modal the caches survive, so the modal still renders.
func (m Model) realtimeRemoveIssue(key string) (Model, tea.Cmd) {
	if key == "" {
		return m, nil
	}
	listed := m.indexOfIssue(key) >= 0
	_, cached := m.details[key]
	if !listed && !cached {
		return m, nil
	}
	if listed {
		m.removeIssue(key) // only a listed row decrements page.TotalElements. a cache-only issue was never counted
	}
	if m.peeking && m.peekKey == key {
		m.peeking, m.peekKey, m.peekScroll = false, "", 0
	}
	if m.blockingModalOpen() {
		return m, nil
	}
	delete(m.details, key)
	delete(m.detailsPending, key)
	delete(m.detailsFailed, key)
	m.detailGen[key]++ // supersede any in-flight detail load so it cannot resurrect the evicted cache
	delete(m.activities, key)
	delete(m.activitiesPending, key)
	delete(m.activitiesFailed, key)
	m.activityGen[key]++
	if len(m.issues) == 0 && m.page.HasNext {
		// moveCursor cannot page an empty list, so pull the next page here
		m.reqGen++
		return m, loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false)
	}
	if m.viewKey == key {
		m.viewKey = "" // the viewed issue is gone, so re-point the panel at the cursor's issue
	} else if idx := m.indexOfIssue(m.viewKey); idx >= 0 {
		m.cursor = idx // rows shifted when one above was removed, so keep the cursor on the viewed issue
	}
	return m.syncSelection()
}

// onRealtimeReload performs the debounced silent reload: no loading flash, selection preserved.
func (m Model) onRealtimeReload(msg realtimeReloadMsg) (Model, tea.Cmd) {
	if msg.seq != m.rtReloadSeq {
		return m, nil // a later new-issue event superseded this debounce
	}
	if m.blockingModalOpen() || m.focus == focusDetail || m.morePagesLoaded || m.loading || m.loadingMore {
		return m, nil // don't disrupt an active interaction, a deep scroll, or an in-flight load
	}
	m.reqGen++ // supersede any in-flight load. the landing page replaces the list silently
	m.rtRestoreKey = m.viewKey
	m.rtRestoreGen = m.reqGen
	return m, loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false)
}

// onRealtimeSprintEvent folds one sprint event in. Self-echo is not filtered (idempotent).
func (m Model) onRealtimeSprintEvent(msg RealtimeSprintEventMsg) (Model, tea.Cmd) {
	switch msg.Type {
	case "SPRINT_ISSUES_ADDED", "SPRINT_ISSUES_REMOVED":
		return m.realtimeSprintIssuesChanged(msg)
	default:
		return m.realtimeSprintLifecycle()
	}
}

// realtimeSprintLifecycle refreshes only the current-sprint pointer until the Sprints tab is opened.
func (m Model) realtimeSprintLifecycle() (Model, tea.Cmd) {
	if !m.sprintsRequested {
		m.currentSprintGen++
		return m, loadCurrentSprint(m.deps, m.projectKey, m.currentSprintGen)
	}
	m.rtSprintReloadSeq++
	seq := m.rtSprintReloadSeq
	return m, tea.Tick(rtReloadDebounce, func(time.Time) tea.Msg { return realtimeSprintReloadMsg{seq: seq} })
}

// onRealtimeSprintReload is not skipped under modals, unlike the issue list: they pin their target.
func (m Model) onRealtimeSprintReload(msg realtimeSprintReloadMsg) (Model, tea.Cmd) {
	if msg.seq != m.rtSprintReloadSeq {
		return m, nil // a later lifecycle event superseded this debounce
	}
	if m.sprintsLoading {
		return m, nil // a load is already in flight, so its fresh page lands instead
	}
	m.sprintRestoreID = m.selSprintID // keep the cursor on the same sprint across the silent reload
	m.sprintReqGen++
	return m, loadSprints(m.deps, m.projectKey, m.sprintReqGen)
}

// realtimeSprintIssuesChanged evicts EVERY cached sprint list: a migrate names only the target sprint.
func (m Model) realtimeSprintIssuesChanged(msg RealtimeSprintEventMsg) (Model, tea.Cmd) {
	targets := map[int64]bool{}
	for id := range m.sprintIssues {
		targets[id] = true
	}
	if m.selSprintID != 0 {
		targets[m.selSprintID] = true // may not be cached yet (its first load could be racing this event)
	}
	var cmds []tea.Cmd
	for id := range targets {
		cmds = append(cmds, m.evictSprintIssues(id)...)
	}
	// keep the issue-list rows' sprint membership in step for the "already in sprint" guard
	added := msg.Type == "SPRINT_ISSUES_ADDED"
	for _, k := range msg.IssueKeys {
		if added {
			m.patchRowSprintID(k, msg.SprintID)
		} else {
			m.patchRowSprintID(k, 0)
		}
	}
	return m, tea.Batch(cmds...)
}
