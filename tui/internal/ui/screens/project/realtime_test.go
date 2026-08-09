package project

import (
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// threeIssues is a loaded list of ENG-1..ENG-3 with the cursor on ENG-1.
func threeIssues(t *testing.T) Model {
	t.Helper()
	return loaded(t, 160, 40, domain.IssuePage{Issues: []domain.IssueSummary{
		{Key: "ENG-1", Title: "A", StateCategory: "ACTIVE"},
		{Key: "ENG-2", Title: "B", StateCategory: "ACTIVE"},
		{Key: "ENG-3", Title: "C", StateCategory: "ACTIVE"},
	}, TotalElements: 3})
}

// A new-issue event schedules one debounced reload (bumping the seq), not an immediate one.
func TestRealtimeCreatedSchedulesReload(t *testing.T) {
	m := threeIssues(t)
	before := m.rtReloadSeq
	m, cmd := m.Update(RealtimeIssueEventMsg{Type: "ISSUE_CREATED", IssueKey: "ENG-9"})
	if m.rtReloadSeq != before+1 || cmd == nil {
		t.Errorf("a new-issue event should arm a debounced reload: seq %d->%d cmd=%v", before, m.rtReloadSeq, cmd != nil)
	}
}

// The debounced reload refetches the first page silently (no loading flash) and restores the cursor to
// the issue that was selected, even after it is reordered in the fresh page.
func TestRealtimeReloadPreservesSelection(t *testing.T) {
	m := threeIssues(t)
	m, _ = m.Update(press("down")) // cursor 1 -> viewKey ENG-2
	if m.viewKey != "ENG-2" {
		t.Fatalf("setup: cursor should sit on ENG-2, got %q", m.viewKey)
	}

	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_CREATED"})
	m, cmd := m.Update(realtimeReloadMsg{seq: m.rtReloadSeq})
	if cmd == nil {
		t.Fatal("the debounced reload should fire a list load")
	}
	if m.loading {
		t.Error("a realtime reload must be silent (no loading flash)")
	}
	if m.rtRestoreKey != "ENG-2" {
		t.Fatalf("the reload should capture the selected key, got %q", m.rtRestoreKey)
	}
	gen := m.rtRestoreGen

	// the fresh page pushes a new issue to the top, moving ENG-2 down
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: gen, page: domain.IssuePage{Issues: []domain.IssueSummary{
		{Key: "ENG-9", Title: "New", StateCategory: "ACTIVE"},
		{Key: "ENG-1", Title: "A", StateCategory: "ACTIVE"},
		{Key: "ENG-2", Title: "B", StateCategory: "ACTIVE"},
		{Key: "ENG-3", Title: "C", StateCategory: "ACTIVE"},
	}, TotalElements: 4}})

	if got := m.issues[m.cursor].Key; got != "ENG-2" {
		t.Errorf("the cursor should be restored to ENG-2 after the reload, got %q", got)
	}
	if m.rtRestoreKey != "" {
		t.Error("the restore key should be consumed after the reload lands")
	}
}

// The reload is skipped whenever it would disrupt the user: a blocking modal, a focused detail panel, a
// list paged past the first page, or a superseded debounce.
func TestRealtimeReloadSkippedWhenBusy(t *testing.T) {
	base := threeIssues(t)
	base, _ = base.Update(RealtimeIssueEventMsg{Type: "ISSUE_CREATED"})
	seq := base.rtReloadSeq

	cases := map[string]func(Model) Model{
		"modal open":   func(m Model) Model { m.creating = true; return m },
		"detail focus": func(m Model) Model { m.focus = focusDetail; return m },
		"paged":        func(m Model) Model { m.morePagesLoaded = true; return m },
	}
	for name, mut := range cases {
		m := mut(base)
		if _, cmd := m.onRealtimeReload(realtimeReloadMsg{seq: seq}); cmd != nil {
			t.Errorf("%s: reload should be skipped", name)
		}
	}
	if _, cmd := base.onRealtimeReload(realtimeReloadMsg{seq: seq - 1}); cmd != nil {
		t.Error("a superseded debounce seq should be dropped")
	}
}

// A delete event drops the row and evicts its detail/activity caches, then re-points the panel off the
// gone issue.
func TestRealtimeDeletedRemovesRowAndEvicts(t *testing.T) {
	m := threeIssues(t)
	m.details["ENG-2"] = domain.IssueDetail{Key: "ENG-2"}
	m.activities["ENG-2"] = domain.IssueActivityPage{}
	m, _ = m.Update(press("down")) // view ENG-2

	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ENG-2"})

	if m.indexOfIssue("ENG-2") >= 0 {
		t.Error("the deleted issue's row should be removed")
	}
	if _, ok := m.details["ENG-2"]; ok {
		t.Error("the deleted issue's detail cache should be evicted")
	}
	if _, ok := m.activities["ENG-2"]; ok {
		t.Error("the deleted issue's activity cache should be evicted")
	}
	if m.viewKey == "ENG-2" {
		t.Error("the panel should re-point off the deleted issue")
	}
}

// Deleting the issue currently being peeked closes the peek modal.
func TestRealtimeDeletedClosesPeek(t *testing.T) {
	m := threeIssues(t)
	m.details["ENG-3"] = domain.IssueDetail{Key: "ENG-3"}
	m.peeking, m.peekKey = true, "ENG-3"

	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ENG-3"})

	if m.peeking || m.peekKey != "" {
		t.Errorf("peeking the deleted issue should close the peek, got peeking=%v key=%q", m.peeking, m.peekKey)
	}
}

// A delete for an issue that is neither listed nor cached is a no-op.
func TestRealtimeDeletedIgnoredWhenUnknown(t *testing.T) {
	m := threeIssues(t)
	before := len(m.issues)
	m, cmd := m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ZZZ-9"})
	if len(m.issues) != before || cmd != nil {
		t.Error("a delete for an unknown issue should be a no-op")
	}
}

// While a blocking modal is open, a delete still removes the row but keeps viewKey pinned so the open
// interaction's target is not pulled out from under it.
func TestRealtimeDeletedDuringModalKeepsViewKey(t *testing.T) {
	m := threeIssues(t)
	m.editing = true // an edit form is open on ENG-1 (viewKey)

	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ENG-1"})

	if m.indexOfIssue("ENG-1") >= 0 {
		t.Error("the row should still be removed under an open modal")
	}
	if m.viewKey != "ENG-1" {
		t.Errorf("viewKey should stay pinned while a modal is open, got %q", m.viewKey)
	}
}

// A field/transition/etc. event SWR-refetches the affected issue when it is the one being viewed, or is
// already cached - keeping the cached copy visible meanwhile.
func TestRealtimeRefreshRefetchesRelevantIssue(t *testing.T) {
	m := threeIssues(t)
	// land the viewed issue's initial detail so its pending flag is clear
	m, _ = m.Update(IssueDetailLoadedMsg{key: "ENG-1", gen: m.detailGen["ENG-1"], detail: domain.IssueDetail{Key: "ENG-1"}})
	if m.detailsPending["ENG-1"] {
		t.Fatal("setup: the viewed issue's detail should have landed")
	}
	gen := m.detailGen["ENG-1"]

	m, cmd := m.Update(RealtimeIssueEventMsg{Type: "ISSUE_TRANSITIONED", IssueKey: "ENG-1"})
	if !m.detailsPending["ENG-1"] || m.detailGen["ENG-1"] <= gen || cmd == nil {
		t.Error("an event on the viewed issue should SWR-refetch its detail")
	}
	if _, ok := m.details["ENG-1"]; !ok {
		t.Error("the cached detail must stay visible during the refetch (not evicted)")
	}

	// a cached-but-not-viewed issue is also refreshed
	m.details["ENG-3"] = domain.IssueDetail{Key: "ENG-3"}
	m, cmd = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_ASSIGNED", IssueKey: "ENG-3"})
	if !m.detailsPending["ENG-3"] || cmd == nil {
		t.Error("an event on a cached issue should refetch it")
	}
}

// VCS events reach the issue through the same generic path as any other change, so a push, a pull request
// or a system-driven transition refreshes the detail - which is what keeps the Branches and Pull requests
// sections live without a reload. This needs no per-type handling; the test guards that staying true.
func TestRealtimeRefreshHandlesVcsEvents(t *testing.T) {
	for _, eventType := range []string{
		"ISSUE_BRANCH_CONNECTED",
		"ISSUE_VCS_CONNECTION_LINKED",
		"ISSUE_TRANSITIONED_BY_SYSTEM",
	} {
		m := threeIssues(t)
		m, _ = m.Update(IssueDetailLoadedMsg{key: "ENG-1", gen: m.detailGen["ENG-1"], detail: domain.IssueDetail{Key: "ENG-1"}})
		gen := m.detailGen["ENG-1"]

		m, cmd := m.Update(RealtimeIssueEventMsg{Type: eventType, IssueKey: "ENG-1"})

		if !m.detailsPending["ENG-1"] || m.detailGen["ENG-1"] <= gen || cmd == nil {
			t.Errorf("%s on the viewed issue should SWR-refetch its detail", eventType)
		}
	}
}

// An event for a listed-but-never-opened issue (not viewed, not cached) is ignored: refetching a detail
// per foreign event would be wasteful, and its row refreshes on the next natural load.
func TestRealtimeRefreshIgnoresUnrelated(t *testing.T) {
	m := threeIssues(t) // viewKey ENG-1; ENG-2 is listed but not viewed/cached
	m, cmd := m.Update(RealtimeIssueEventMsg{Type: "ISSUE_ASSIGNED", IssueKey: "ENG-2"})
	if m.detailsPending["ENG-2"] || cmd != nil {
		t.Error("an event for a listed-but-unopened issue should be a no-op")
	}
}

// The silent reload preserves the user's CURRENT selection, not the one they had when the reload was
// dispatched - so navigation done during the in-flight window is not thrown away (review finding F1).
func TestRealtimeReloadPreservesNavigationDuringWindow(t *testing.T) {
	m := threeIssues(t)
	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_CREATED"})
	m, cmd := m.Update(realtimeReloadMsg{seq: m.rtReloadSeq}) // arms the silent reload (rtRestoreKey=ENG-1)
	if cmd == nil {
		t.Fatal("the reload should fire")
	}
	gen := m.rtRestoreGen

	// the user navigates to ENG-3 while the reload is in flight
	m, _ = m.Update(press("down"))
	m, _ = m.Update(press("down"))
	if m.viewKey != "ENG-3" {
		t.Fatalf("setup: navigation should have moved to ENG-3, got %q", m.viewKey)
	}

	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: gen, page: domain.IssuePage{Issues: []domain.IssueSummary{
		{Key: "ENG-9", StateCategory: "ACTIVE"}, {Key: "ENG-1", StateCategory: "ACTIVE"},
		{Key: "ENG-2", StateCategory: "ACTIVE"}, {Key: "ENG-3", StateCategory: "ACTIVE"},
	}, TotalElements: 4}})

	if got := m.issues[m.cursor].Key; got != "ENG-3" {
		t.Errorf("the reload should keep the user's mid-flight selection ENG-3, got %q", got)
	}
}

// Deleting a row ABOVE the cursor shifts the rows up; the cursor must stay anchored on the viewed issue,
// not slide onto its neighbor (review finding F2).
func TestRealtimeDeleteAboveCursorKeepsSelection(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: []domain.IssueSummary{
		{Key: "ENG-1", StateCategory: "ACTIVE"}, {Key: "ENG-2", StateCategory: "ACTIVE"},
		{Key: "ENG-3", StateCategory: "ACTIVE"}, {Key: "ENG-4", StateCategory: "ACTIVE"},
	}, TotalElements: 4})
	m, _ = m.Update(press("down"))
	m, _ = m.Update(press("down")) // cursor index 2 -> ENG-3
	if m.viewKey != "ENG-3" {
		t.Fatalf("setup: cursor should sit on ENG-3, got %q", m.viewKey)
	}

	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ENG-1"}) // a row above the cursor

	if m.viewKey != "ENG-3" {
		t.Errorf("deleting a row above the cursor should keep the panel on ENG-3, got %q", m.viewKey)
	}
	if got := m.issues[m.cursor].Key; got != "ENG-3" {
		t.Errorf("the cursor should stay anchored on ENG-3, got %q", got)
	}
}

// When a delete empties the loaded page but more pages exist server-side, the list silently pulls the
// next page instead of stranding empty (review finding F3).
func TestRealtimeDeleteEmptyingListReloads(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", StateCategory: "ACTIVE"}}, TotalElements: 50, HasNext: true,
	})
	before := m.reqGen
	m, cmd := m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ENG-1"})
	if len(m.issues) != 0 {
		t.Fatal("setup: the only loaded row should be removed")
	}
	if cmd == nil || m.reqGen <= before {
		t.Error("emptying the page while more exist server-side should trigger a silent reload")
	}
}

// A delete of the viewed issue while a blocking modal is open drops the row but keeps its caches, so the
// open modal keeps rendering and the panel does not collapse to a stuck skeleton (review finding F4).
func TestRealtimeDeleteUnderModalKeepsCaches(t *testing.T) {
	m := threeIssues(t)
	m.details["ENG-1"] = domain.IssueDetail{Key: "ENG-1", Title: "A"}
	m.editing = true // an edit modal is open on the viewed issue ENG-1

	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ENG-1"})

	if m.indexOfIssue("ENG-1") >= 0 {
		t.Error("the row should be removed even under a modal")
	}
	if _, ok := m.details["ENG-1"]; !ok {
		t.Error("under an open modal the detail cache must be kept so the panel does not go blank")
	}
	if m.viewKey != "ENG-1" {
		t.Errorf("viewKey should stay pinned under the modal, got %q", m.viewKey)
	}
}

// Deleting a cached-but-unlisted issue (e.g. a peeked link filtered out of the current page) evicts its
// cache but must NOT decrement the list total it was never counted in (review finding F5).
func TestRealtimeDeleteCacheOnlyKeepsTotal(t *testing.T) {
	m := threeIssues(t)
	m.details["ENG-99"] = domain.IssueDetail{Key: "ENG-99"} // cached via peek, not in the filtered list
	total := m.page.TotalElements

	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ENG-99"})

	if _, ok := m.details["ENG-99"]; ok {
		t.Error("the cached issue should be evicted")
	}
	if m.page.TotalElements != total {
		t.Errorf("a cache-only delete must not decrement the list total, got %d want %d", m.page.TotalElements, total)
	}
}

// A sprint lifecycle event on a loaded list arms one debounced silent reload.
func TestRealtimeSprintLifecycleSchedulesReload(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 1, Status: "ACTIVE"}})
	m.sprintsRequested = true // the Sprints tab has been opened, so a list exists to refresh
	m, cmd := m.Update(RealtimeSprintEventMsg{Type: "SPRINT_STARTED", SprintID: 1})
	if m.rtSprintReloadSeq != 1 || cmd == nil {
		t.Fatalf("a lifecycle event should arm a debounced reload (seq=%d cmd=%v)", m.rtSprintReloadSeq, cmd != nil)
	}
}

// With no sprint list ever loaded, a lifecycle event only refreshes the issue tab's current-sprint
// pointer (a teammate may have started/completed the active sprint) - no list reload is scheduled.
func TestRealtimeSprintLifecycleNotLoaded(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{}) // issues tab; Sprints tab never opened
	before := m.currentSprintGen
	m, cmd := m.Update(RealtimeSprintEventMsg{Type: "SPRINT_COMPLETED", SprintID: 3})
	if m.currentSprintGen != before+1 || cmd == nil {
		t.Fatalf("with no loaded list a lifecycle event should refresh currentSprint (gen %d->%d cmd=%v)", before, m.currentSprintGen, cmd != nil)
	}
	if m.rtSprintReloadSeq != 0 {
		t.Error("no list reload should be armed when the sprint list was never loaded")
	}
}

// The debounced sprint reload preserves the selected sprint, drops a superseded seq, still fires under an
// open sprint modal (targets are pinned), and defers only to an in-flight load.
func TestRealtimeSprintReload(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 1, Status: "ACTIVE"}, {ID: 2, Status: "PLANNING"}})
	m.sprintsRequested = true
	m.selSprintID = 2
	m, _ = m.Update(RealtimeSprintEventMsg{Type: "SPRINT_UPDATED", SprintID: 2})
	seq := m.rtSprintReloadSeq

	m2, cmd := m.onRealtimeSprintReload(realtimeSprintReloadMsg{seq: seq})
	if cmd == nil || m2.sprintRestoreID != 2 {
		t.Fatalf("the reload should preserve the selected sprint (restoreID=%d cmd=%v)", m2.sprintRestoreID, cmd != nil)
	}
	// a superseded (stale-seq) debounce is dropped
	if _, cmd := m.onRealtimeSprintReload(realtimeSprintReloadMsg{seq: seq - 1}); cmd != nil {
		t.Error("a stale-seq sprint reload should be dropped")
	}
	// still fires under an open sprint modal (a background list reload cannot redirect a pinned-target modal,
	// and skipping would strand the list stale)
	modal := m
	modal.sprintConfirming = true
	if _, cmd := modal.onRealtimeSprintReload(realtimeSprintReloadMsg{seq: seq}); cmd == nil {
		t.Error("the reload should still fire under a sprint modal")
	}
	// deferred only while a list load is already in flight
	loading := m
	loading.sprintsLoading = true
	if _, cmd := loading.onRealtimeSprintReload(realtimeSprintReloadMsg{seq: seq}); cmd != nil {
		t.Error("the reload should defer to an in-flight load")
	}
}

// SPRINT_ISSUES_ADDED evicts the target sprint's cache (refetching the selected one) and repoints the
// added issue's list row so the "already in sprint" guard stays correct.
func TestRealtimeSprintIssuesAdded(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 5, Status: "ACTIVE"}})
	m.selSprintID = 5
	m.sprintIssues[5] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-9"}}}
	m.issues = []domain.IssueSummary{{Key: "TIS-1", SprintID: 0}}
	m, _ = m.Update(RealtimeSprintEventMsg{Type: "SPRINT_ISSUES_ADDED", SprintID: 5, IssueKeys: []string{"TIS-1"}})
	if _, cached := m.sprintIssues[5]; cached {
		t.Error("the target sprint's issue cache should be evicted")
	}
	if !m.sprintIssuesPending[5] {
		t.Error("the selected sprint's issues should be refetched (pending)")
	}
	if m.issues[0].SprintID != 5 {
		t.Errorf("the added issue's row SprintID should be repointed to 5, got %d", m.issues[0].SprintID)
	}
}

// A migrate emits only SPRINT_ISSUES_ADDED for the target (never the source), so the handler evicts EVERY
// cached sprint issue list - a foreign/source sprint the user isn't looking at cannot keep a stale panel.
func TestRealtimeSprintIssuesEvictsAllCaches(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 6, Status: "PLANNING"}, {ID: 5, Status: "ACTIVE"}})
	m.selSprintID = 6
	m.sprintIssues[5] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1"}}} // foreign/source, cached
	m.sprintIssues[6] = domain.IssuePage{Issues: []domain.IssueSummary{}}               // selected
	m.issues = []domain.IssueSummary{{Key: "TIS-1", SprintID: 5}}
	m, _ = m.Update(RealtimeSprintEventMsg{Type: "SPRINT_ISSUES_ADDED", SprintID: 6, IssueKeys: []string{"TIS-1"}})
	if _, cached := m.sprintIssues[5]; cached {
		t.Error("a foreign/source sprint's cache should be evicted too (the event carries no reliable source)")
	}
	if _, cached := m.sprintIssues[6]; cached {
		t.Error("the selected sprint's cache should be evicted")
	}
	if !m.sprintIssuesPending[6] {
		t.Error("the selected sprint should be refetched (pending)")
	}
	if m.issues[0].SprintID != 6 {
		t.Errorf("the migrated issue's row should point at the target (6), got %d", m.issues[0].SprintID)
	}
}

// SPRINT_ISSUES_REMOVED evicts the sprint's cache and clears the removed issue's row sprint.
func TestRealtimeSprintIssuesRemoved(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 5, Status: "ACTIVE"}})
	m.selSprintID = 5
	m.sprintIssues[5] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1"}}}
	m.issues = []domain.IssueSummary{{Key: "TIS-1", SprintID: 5}}
	m, _ = m.Update(RealtimeSprintEventMsg{Type: "SPRINT_ISSUES_REMOVED", SprintID: 5, IssueKeys: []string{"TIS-1"}})
	if _, cached := m.sprintIssues[5]; cached {
		t.Error("the sprint's issue cache should be evicted on remove")
	}
	if m.issues[0].SprintID != 0 {
		t.Errorf("the removed issue's row SprintID should be cleared, got %d", m.issues[0].SprintID)
	}
}
