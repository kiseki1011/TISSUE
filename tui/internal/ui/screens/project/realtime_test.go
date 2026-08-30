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

func TestRealtimeCreatedSchedulesReload(t *testing.T) {
	m := threeIssues(t)
	before := m.rtReloadSeq
	m, cmd := m.Update(RealtimeIssueEventMsg{Type: "ISSUE_CREATED", IssueKey: "ENG-9"})
	if m.rtReloadSeq != before+1 || cmd == nil {
		t.Errorf("a new-issue event should arm a debounced reload: seq %d->%d cmd=%v", before, m.rtReloadSeq, cmd != nil)
	}
}

// The silent reload (no loading flash) keeps the cursor on the selected issue even when it reorders.
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

func TestRealtimeDeletedClosesPeek(t *testing.T) {
	m := threeIssues(t)
	m.details["ENG-3"] = domain.IssueDetail{Key: "ENG-3"}
	m.peeking, m.peekKey = true, "ENG-3"

	m, _ = m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ENG-3"})

	if m.peeking || m.peekKey != "" {
		t.Errorf("peeking the deleted issue should close the peek, got peeking=%v key=%q", m.peeking, m.peekKey)
	}
}

func TestRealtimeDeletedIgnoredWhenUnknown(t *testing.T) {
	m := threeIssues(t)
	before := len(m.issues)
	m, cmd := m.Update(RealtimeIssueEventMsg{Type: "ISSUE_DELETED", IssueKey: "ZZZ-9"})
	if len(m.issues) != before || cmd != nil {
		t.Error("a delete for an unknown issue should be a no-op")
	}
}

// Under a modal the row still goes, but viewKey stays pinned so the open interaction keeps its target.
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

// An event refetches a viewed or cached issue, keeping the cached copy visible meanwhile.
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

// VCS events must keep taking the generic refresh path - no per-type handling.
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

// A listed-but-never-opened issue is ignored: a detail fetch per foreign event would be wasteful.
func TestRealtimeRefreshIgnoresUnrelated(t *testing.T) {
	m := threeIssues(t) // viewKey ENG-1. ENG-2 is listed but not viewed/cached
	m, cmd := m.Update(RealtimeIssueEventMsg{Type: "ISSUE_ASSIGNED", IssueKey: "ENG-2"})
	if m.detailsPending["ENG-2"] || cmd != nil {
		t.Error("an event for a listed-but-unopened issue should be a no-op")
	}
}

// The reload keeps the CURRENT selection, so navigation during the in-flight window is not thrown away.
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
		{Key: "ENG-9", StateCategory: "ACTIVE"},
		{Key: "ENG-1", StateCategory: "ACTIVE"},
		{Key: "ENG-2", StateCategory: "ACTIVE"},
		{Key: "ENG-3", StateCategory: "ACTIVE"},
	}, TotalElements: 4}})

	if got := m.issues[m.cursor].Key; got != "ENG-3" {
		t.Errorf("the reload should keep the user's mid-flight selection ENG-3, got %q", got)
	}
}

// Deleting a row ABOVE the cursor must not slide the cursor onto the neighbor.
func TestRealtimeDeleteAboveCursorKeepsSelection(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: []domain.IssueSummary{
		{Key: "ENG-1", StateCategory: "ACTIVE"},
		{Key: "ENG-2", StateCategory: "ACTIVE"},
		{Key: "ENG-3", StateCategory: "ACTIVE"},
		{Key: "ENG-4", StateCategory: "ACTIVE"},
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

// A delete that empties the page must pull the next one instead of stranding empty.
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

// Under a modal the caches survive the delete, so the panel does not collapse to a stuck skeleton.
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

// A cache-only delete (never listed) must not decrement the list total.
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

func TestRealtimeSprintLifecycleSchedulesReload(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 1, Status: "ACTIVE"}})
	m.sprintsRequested = true // the Sprints tab has been opened, so a list exists to refresh
	m, cmd := m.Update(RealtimeSprintEventMsg{Type: "SPRINT_STARTED", SprintID: 1})
	if m.rtSprintReloadSeq != 1 || cmd == nil {
		t.Fatalf("a lifecycle event should arm a debounced reload (seq=%d cmd=%v)", m.rtSprintReloadSeq, cmd != nil)
	}
}

// With no sprint list loaded, a lifecycle event only refreshes the current-sprint pointer.
func TestRealtimeSprintLifecycleNotLoaded(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{}) // issues tab, Sprints tab never opened
	before := m.currentSprintGen
	m, cmd := m.Update(RealtimeSprintEventMsg{Type: "SPRINT_COMPLETED", SprintID: 3})
	if m.currentSprintGen != before+1 || cmd == nil {
		t.Fatalf("with no loaded list a lifecycle event should refresh currentSprint (gen %d->%d cmd=%v)", before, m.currentSprintGen, cmd != nil)
	}
	if m.rtSprintReloadSeq != 0 {
		t.Error("no list reload should be armed when the sprint list was never loaded")
	}
}

// The debounced sprint reload preserves the selection and still fires under a modal (targets pinned).
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
	if _, cmd := m.onRealtimeSprintReload(realtimeSprintReloadMsg{seq: seq - 1}); cmd != nil {
		t.Error("a stale-seq sprint reload should be dropped")
	}
	// still fires under a modal: targets are pinned
	modal := m
	modal.sprintConfirming = true
	if _, cmd := modal.onRealtimeSprintReload(realtimeSprintReloadMsg{seq: seq}); cmd == nil {
		t.Error("the reload should still fire under a sprint modal")
	}
	loading := m
	loading.sprintsLoading = true
	if _, cmd := loading.onRealtimeSprintReload(realtimeSprintReloadMsg{seq: seq}); cmd != nil {
		t.Error("the reload should defer to an in-flight load")
	}
}

// SPRINT_ISSUES_ADDED evicts the sprint's cache and repoints the row for the "already in sprint" guard.
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

// A migrate names only the target, so every cached sprint list is evicted.
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
