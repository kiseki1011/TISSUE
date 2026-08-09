package project

import (
	"time"

	tea "charm.land/bubbletea/v2"
)

// rtReloadDebounce coalesces a burst of new-issue events (a teammate creating several) into one silent
// list reload.
const rtReloadDebounce = 300 * time.Millisecond

// RealtimeIssueEventMsg is one issue realtime (SSE) event, delivered by the app shell only when this
// screen is drilled into the event's project. The Type is the backend's "ISSUE_*" name; IssueKey is the
// affected issue (always set for issue events, including the newly created one).
type RealtimeIssueEventMsg struct {
	Type     string
	IssueKey string
}

// realtimeReloadMsg is the debounced trigger for the new-issue silent reload; seq drops a superseded one.
type realtimeReloadMsg struct{ seq int }

// RealtimeSprintEventMsg is one sprint realtime (SSE) event, delivered by the app shell only when this
// screen is drilled into the event's project. Type is the backend's "SPRINT_*" name; SprintID is the
// affected sprint; IssueKeys is set only for SPRINT_ISSUES_ADDED / SPRINT_ISSUES_REMOVED.
type RealtimeSprintEventMsg struct {
	Type      string
	SprintID  int64
	IssueKeys []string
}

// realtimeSprintReloadMsg is the debounced trigger for the silent sprint-list reload; seq drops a
// superseded one.
type realtimeSprintReloadMsg struct{ seq int }

// ProjectKey is the project this screen is drilled into, so the app shell can route realtime events for
// the matching project here and drop the rest.
func (m Model) ProjectKey() string { return m.projectKey }

// indexOfIssue is the row index of key in the loaded page, or -1.
func (m Model) indexOfIssue(key string) int {
	for i := range m.issues {
		if m.issues[i].Key == key {
			return i
		}
	}
	return -1
}

// blockingModalOpen reports whether an interaction that owns the screen or pins viewKey is open, so a
// realtime reload/re-point does not disrupt it.
func (m Model) blockingModalOpen() bool {
	return m.editing || m.editingContent || m.commenting || m.deleting ||
		m.picking || m.creating || m.filtering || m.dating || m.peeking
}

// onRealtimeIssueEvent folds one issue event into the screen without a visible reload:
//   - CREATED  -> schedule a debounced silent reload of the first page (a new issue may have appeared).
//   - DELETED  -> drop the row and evict its caches (mirrors the delete-confirm success path).
//   - anything else (transition/assign/fields/reviewer/relation) -> if the issue is on screen or cached,
//     evict-free SWR refetch of its detail (and activity), which also repatches the list row.
//
// Self-echo is not filtered: the SWR refetch is idempotent, so re-applying our own action's echo is a
// harmless extra fetch, not a wrong state.
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

// realtimeRefreshIssue SWR-refetches the affected issue when it is the one being viewed/peeked or is
// already cached, so its detail (and the list row, via patchRow on landing) updates in place. An issue
// the user has never opened is left alone - refetching a detail per foreign event would be wasteful, and
// its row refreshes on the next natural load.
//
// Known transient: if an event lands for an issue with an optimistic write still in flight (e.g. a rapid
// transition+assign where only the first has been persisted), this refetch can briefly show the server
// state that predates the un-persisted field before the write's own *DoneMsg refetch reconciles it - a
// self-correcting new->old->new flicker, not a wrong final state. Accepted alongside the un-filtered
// self-echo decision; a fix would require tracking in-flight optimistic writes per key.
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
		cmds = append(cmds, m.startActivityLoad(key)) // keep a cached activity log fresh too
	}
	return m, tea.Batch(cmds...)
}

// realtimeRemoveIssue drops a deleted issue from the list and (when nothing owns the screen) evicts its
// caches and re-points the panel. No-op when the issue is neither listed nor cached.
//
// While a blocking modal is open, only the row is dropped: the caches and viewKey are left intact so the
// open modal keeps rendering its (now-deleted) target and the panel does not collapse to a stuck skeleton;
// it re-points on the next navigation after the modal closes.
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
		m.removeIssue(key) // only a listed row decrements page.TotalElements; a cache-only issue was never counted
	}
	if m.peeking && m.peekKey == key {
		m.peeking, m.peekKey, m.peekScroll = false, "", 0 // the peeked issue is gone; close its modal
	}
	if m.blockingModalOpen() {
		return m, nil // a modal still owns the screen; keep its target renderable, re-point when it closes
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
		// the loaded page emptied but more issues exist server-side; silently pull the next page (mirrors the
		// delete-confirm path) so moveCursor - which cannot page an empty list - is not stranded
		m.reqGen++
		return m, loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false)
	}
	if m.viewKey == key {
		m.viewKey = "" // the viewed issue is gone; re-point the panel at the issue now under the cursor
	} else if idx := m.indexOfIssue(m.viewKey); idx >= 0 {
		m.cursor = idx // rows shifted when a row above was removed; keep the cursor on the still-viewed issue
	}
	return m.syncSelection()
}

// onRealtimeReload performs the debounced silent reload after a new-issue event: it refreshes the first
// page in the background (no loading flash - the current rows stay until the fresh page lands) and
// preserves the selected issue. It is skipped when a blocking modal is open, the user has focused or
// paged past the first page, or a load is already in flight - so it never yanks an engaged reader.
func (m Model) onRealtimeReload(msg realtimeReloadMsg) (Model, tea.Cmd) {
	if msg.seq != m.rtReloadSeq {
		return m, nil // a later new-issue event superseded this debounce
	}
	if m.blockingModalOpen() || m.focus == focusDetail || m.morePagesLoaded || m.loading || m.loadingMore {
		return m, nil // don't disrupt an active interaction, a deep scroll, or an in-flight load
	}
	m.reqGen++ // supersede any in-flight load; the landing page replaces the list silently (loading stays false)
	m.rtRestoreKey = m.viewKey
	m.rtRestoreGen = m.reqGen
	return m, loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false)
}

// onRealtimeSprintEvent folds one sprint event into the screen without a visible reload:
//   - SPRINT_ISSUES_ADDED / REMOVED -> evict the affected sprint's cached issue list (refetch when it is
//     the selected sprint) and repoint the moved issues' list rows, so the bottom panel and the "already
//     in sprint" guard stay correct.
//   - any lifecycle event (created/updated/started/completed/cancelled/deleted) -> a debounced silent
//     reload of the sprint list (statuses, ordering and the current-sprint marker may all have changed).
//
// Self-echo is not filtered: the reload/refetch is idempotent, so re-applying our own action's echo is a
// harmless extra fetch (mirrors the issue realtime decision).
func (m Model) onRealtimeSprintEvent(msg RealtimeSprintEventMsg) (Model, tea.Cmd) {
	switch msg.Type {
	case "SPRINT_ISSUES_ADDED", "SPRINT_ISSUES_REMOVED":
		return m.realtimeSprintIssuesChanged(msg)
	default:
		return m.realtimeSprintLifecycle()
	}
}

// realtimeSprintLifecycle schedules a debounced silent reload of the sprint list when it has been loaded;
// otherwise it just refreshes the issue tab's current-sprint pointer (a teammate may have started or
// completed the active sprint while the Sprints tab was never opened).
func (m Model) realtimeSprintLifecycle() (Model, tea.Cmd) {
	if !m.sprintsRequested {
		m.currentSprintGen++
		return m, loadCurrentSprint(m.deps, m.projectKey, m.currentSprintGen)
	}
	m.rtSprintReloadSeq++
	seq := m.rtSprintReloadSeq
	return m, tea.Tick(rtReloadDebounce, func(time.Time) tea.Msg { return realtimeSprintReloadMsg{seq: seq} })
}

// onRealtimeSprintReload performs the debounced silent reload after a sprint lifecycle event: it refetches
// the list in the background (the current rows stay until the fresh page lands) and re-points the cursor
// at the same sprint. Unlike the issue-list reload it is NOT skipped for open modals: every sprint action
// modal pins its target (sprintActionID / the migrate snapshot), so a background list reload cannot
// redirect it - and skipping would strand the list stale, since reopening the tab does not refetch. It
// only defers to an in-flight load (whose fresh page supersedes this one anyway).
func (m Model) onRealtimeSprintReload(msg realtimeSprintReloadMsg) (Model, tea.Cmd) {
	if msg.seq != m.rtSprintReloadSeq {
		return m, nil // a later lifecycle event superseded this debounce
	}
	if m.sprintsLoading {
		return m, nil // a load is already in flight; its fresh page will land instead
	}
	m.sprintRestoreID = m.selSprintID // keep the cursor on the same sprint across the silent reload
	m.sprintReqGen++
	return m, loadSprints(m.deps, m.projectKey, m.sprintReqGen)
}

// realtimeSprintIssuesChanged reacts to an add/remove/migrate and repoints the moved issues' list rows.
// The event names only the one sprint it targets, but a migrate moves issues BETWEEN two sprints and emits
// only ISSUES_ADDED for the target - the source is never in the event and cannot be reliably guessed. So
// rather than evict just the named sprint, it evicts EVERY cached sprint issue list (refetching the one on
// screen), guaranteeing no panel - the viewed source included - can show a stale membership. Sprint issue
// events are infrequent, so the extra refetch of the selected sprint (SWR: no flicker) is cheap.
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
