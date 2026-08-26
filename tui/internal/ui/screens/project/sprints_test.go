package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// sprintsTabModel is a loaded model sitting on the Sprints tab with the given sprints (unsorted, as set),
// the cursor on the first, for exercising the action keys without going through a network load.
func sprintsTabModel(t *testing.T, sprints []domain.SprintSummary) Model {
	t.Helper()
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabSprints
	m.sprints = sprints
	m.sprintPage = domain.SprintPage{TotalElements: len(sprints)}
	if len(sprints) > 0 {
		m.selSprintID = sprints[0].ID
	}
	return m
}

func sprintList(n int) []domain.SprintSummary {
	out := make([]domain.SprintSummary, n)
	for i := range out {
		out[i] = domain.SprintSummary{
			ID:     int64(i + 1),
			Key:    "SPR-" + string(rune('1'+i%9)),
			Title:  "Sprint " + string(rune('1'+i%9)),
			Status: "ACTIVE",
		}
	}
	return out
}

// Opening the Sprints tab (key 2) kicks off the lazy list load and shows its loading note.
func TestSprintTabKeyStartsLazyLoad(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(2), TotalElements: 2})
	m, cmd := m.Update(press("2"))
	if m.tab != tabSprints {
		t.Fatalf("key 2 should select the Sprints tab, got %v", m.tab)
	}
	if !m.sprintsRequested || !m.sprintsLoading {
		t.Errorf("opening the tab should start the load (requested=%v loading=%v)", m.sprintsRequested, m.sprintsLoading)
	}
	if cmd == nil {
		t.Error("opening the Sprints tab should return a load command")
	}
	if body := plain(m.View()); !strings.Contains(body, "Loading sprints") {
		t.Errorf("the Sprints tab should show its loading note:\n%s", body)
	}
}

// A landed sprint list populates the tab newest-first, selects the top (most recent) sprint, and loads
// that sprint's issues. sprintList(3) has ids 1..3, so recent-first sorting puts id 3 at the top.
func TestSprintsLoadedSelectsFirst(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, _ = m.Update(press("2"))
	m, cmd := m.Update(sprintsLoadedMsg{key: testKey, gen: m.sprintReqGen, page: domain.SprintPage{Sprints: sprintList(3), TotalElements: 3}})
	if m.sprintsLoading {
		t.Error("a landed load should clear the loading flag")
	}
	if len(m.sprints) != 3 {
		t.Fatalf("expected 3 sprints, got %d", len(m.sprints))
	}
	if m.sprints[0].ID != 3 {
		t.Errorf("sprints should be sorted newest-first, got top id %d", m.sprints[0].ID)
	}
	if m.selSprintID != 3 {
		t.Errorf("the top (most recent) sprint should be selected, got id %d", m.selSprintID)
	}
	if !m.sprintIssuesPending[3] {
		t.Error("the selected sprint's issue load should be marked pending")
	}
	if cmd == nil {
		t.Error("selecting a sprint should load its issues")
	}
}

// A superseded sprint-list result (an earlier reqGen) is ignored when it lands late.
func TestStaleSprintsLoadIgnored(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, _ = m.Update(press("2")) // sprintReqGen == 1
	m, _ = m.Update(sprintsLoadedMsg{key: testKey, gen: 0, page: domain.SprintPage{Sprints: sprintList(2)}})
	if len(m.sprints) != 0 {
		t.Errorf("a stale (superseded) sprint load must not populate the list, got %d", len(m.sprints))
	}
}

// A sprint-list result for a different project is ignored - generations collide across projects because
// New resets sprintReqGen, so only the project key distinguishes them.
func TestSprintsLoadWrongProjectIgnored(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, _ = m.Update(press("2")) // sprintReqGen == 1 for project testKey
	m, _ = m.Update(sprintsLoadedMsg{key: "OTHER", gen: m.sprintReqGen, page: domain.SprintPage{Sprints: sprintList(2), TotalElements: 2}})
	if len(m.sprints) != 0 {
		t.Errorf("a load for a different project must not populate this project's list, got %d", len(m.sprints))
	}
	if m.sprintsLoading != true {
		t.Error("the in-flight flag should stay set until this project's own load lands")
	}
}

// Reopening the Sprints tab retries a failed issue load for the selected sprint (the recovery path a
// single-sprint project cannot reach by moving the cursor).
func TestSprintIssueRetryOnReopen(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabSprints
	m.sprintsRequested = true
	m.sprints = sprintList(1)
	m.selSprintID = 1
	m.sprintIssuesFailed[1] = true // a prior issue load failed, nothing cached
	m, cmd := m.selectTab(tabSprints)
	if cmd == nil {
		t.Fatal("reopening the tab should retry the failed issue load")
	}
	if !m.sprintIssuesPending[1] {
		t.Error("the retry should mark the issue load pending")
	}
}

// The Sprints tab renders the list beside the split detail region (sprint detail over the sprint's
// issues).
func TestSprintTabRenders(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabSprints
	m.sprints = []domain.SprintSummary{
		{ID: 1, Key: "SPR-1", Title: "Alpha sprint", Status: "ACTIVE", Goal: "Ship the thing"},
		{ID: 2, Key: "SPR-2", Title: "Beta sprint", Status: "PLANNING"},
	}
	m.sprintPage = domain.SprintPage{TotalElements: 2}
	m.selSprintID = 1
	body := plain(m.View())
	for _, want := range []string{"Sprints (2)", "SPR-1", "Alpha sprint", "Sprint", "Goal", "Ship the thing", "Issues"} {
		if !strings.Contains(body, want) {
			t.Errorf("Sprints tab render missing %q:\n%s", want, body)
		}
	}
}

// Moving the sprint cursor reselects and loads the newly selected sprint's issues.
func TestSprintCursorMovesSelection(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabSprints
	m.sprints = sprintList(3)
	m.selSprintID = 1
	m, cmd := m.Update(press("j"))
	if m.sprintCursor != 1 {
		t.Fatalf("j should move the sprint cursor, got %d", m.sprintCursor)
	}
	if m.selSprintID != 2 {
		t.Errorf("moving the cursor should reselect (got id %d)", m.selSprintID)
	}
	if cmd == nil {
		t.Error("reselecting a sprint should load its issues")
	}
}

// The bottom panel lists the issues that belong to the selected sprint.
func TestSprintIssuesPanelRows(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabSprints
	m.sprints = []domain.SprintSummary{{ID: 7, Key: "SPR-7", Title: "S", Status: "ACTIVE"}}
	m.selSprintID = 7
	m.sprintIssues[7] = domain.IssuePage{
		TotalElements: 1,
		Issues: []domain.IssueSummary{
			{Key: "TIS-9", Title: "Fix bug", StateCategory: "ACTIVE", StateLabel: "In Progress", Priority: "P1"},
		},
	}
	body := plain(m.View())
	for _, want := range []string{"Issues (1)", "TIS-9", "Fix bug", "In Progress"} {
		if !strings.Contains(body, want) {
			t.Errorf("sprint issues panel missing %q:\n%s", want, body)
		}
	}
}

// The current (ACTIVE) sprint is flagged with a marker in the list and a "current" badge in the detail.
func TestSprintCurrentMarker(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 1, Key: "S-1", Title: "Now", Status: "ACTIVE"}})
	body := plain(m.View())
	if !strings.Contains(body, sprintCurrentMark) {
		t.Errorf("the current sprint should show its marker %q:\n%s", sprintCurrentMark, body)
	}
	if !strings.Contains(body, "current") {
		t.Errorf("the detail should badge the current sprint:\n%s", body)
	}
}

// s opens the required start date picker only for a PLANNING sprint.
func TestSprintStartGating(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 5, Key: "S-5", Status: "PLANNING"}})
	m, _ = m.Update(press("s"))
	if !m.dating || m.dateTarget != dateSprintStart || m.sprintActionID != 5 {
		t.Fatalf("s on a planning sprint should open the start picker (dating=%v target=%v id=%d)", m.dating, m.dateTarget, m.sprintActionID)
	}
	m, cmd := m.Update(press("enter")) // confirm the date
	if m.dating {
		t.Error("confirming should close the picker")
	}
	if cmd == nil {
		t.Error("confirming the start date should fire the start command")
	}

	active := sprintsTabModel(t, []domain.SprintSummary{{ID: 6, Status: "ACTIVE"}})
	active, _ = active.Update(press("s"))
	if active.dating {
		t.Error("s on an active sprint should not open the start picker")
	}
}

// c opens the complete confirmation only for an ACTIVE sprint; accepting fires the command and closes it.
func TestSprintCompleteGating(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 7, Status: "ACTIVE"}})
	m, _ = m.Update(press("c"))
	if !m.sprintConfirming || m.sprintConfirmKind != "complete" || m.sprintActionID != 7 {
		t.Fatalf("c on an active sprint should open the complete confirm (confirming=%v kind=%q id=%d)", m.sprintConfirming, m.sprintConfirmKind, m.sprintActionID)
	}
	m, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if m.sprintConfirming {
		t.Error("accepting should close the confirm")
	}
	if cmd == nil {
		t.Error("accepting should fire the complete command")
	}

	planning := sprintsTabModel(t, []domain.SprintSummary{{ID: 8, Status: "PLANNING"}})
	planning, _ = planning.Update(press("c"))
	if planning.sprintConfirming {
		t.Error("c on a planning sprint should not open the confirm")
	}
}

// x opens the cancel confirmation for a PLANNING/ACTIVE sprint; accepting fires the cancel command. A
// closed sprint can't be cancelled.
func TestSprintCancelGating(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 4, Status: "PLANNING"}})
	m, _ = m.Update(press("x"))
	if !m.sprintConfirming || m.sprintConfirmKind != "cancel" || m.sprintActionID != 4 {
		t.Fatalf("x should open the cancel confirm (confirming=%v kind=%q id=%d)", m.sprintConfirming, m.sprintConfirmKind, m.sprintActionID)
	}
	m, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if m.sprintConfirming || cmd == nil {
		t.Error("accepting should close the confirm and fire the cancel command")
	}

	closed := sprintsTabModel(t, []domain.SprintSummary{{ID: 5, Status: "COMPLETED"}})
	closed, _ = closed.Update(press("x"))
	if closed.sprintConfirming {
		t.Error("x on a completed sprint should not open the cancel confirm")
	}
}

// r opens a picker of the sprint's issues; selecting one fires the remove command and refetches the
// selected sprint's issues. An empty or closed sprint offers nothing to remove.
func TestSprintRemoveIssue(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 6, Status: "ACTIVE"}})
	m.selSprintID = 6
	m.sprintIssues[6] = domain.IssuePage{
		TotalElements: 1,
		Issues:        []domain.IssueSummary{{Key: "TIS-3", Title: "Bug"}},
	}
	m, _ = m.Update(press("r"))
	if !m.picking || m.pickKind != pickSprintRemoveIssue || m.sprintActionID != 6 {
		t.Fatalf("r should open the remove-issue picker (picking=%v kind=%v id=%d)", m.picking, m.pickKind, m.sprintActionID)
	}
	m, cmd := m.Update(press("enter")) // select the highlighted issue
	if m.picking {
		t.Error("selecting should close the picker")
	}
	if cmd == nil {
		t.Error("selecting an issue should fire the remove command")
	}

	empty := sprintsTabModel(t, []domain.SprintSummary{{ID: 7, Status: "ACTIVE"}})
	empty.selSprintID = 7
	empty, _ = empty.Update(press("r"))
	if empty.picking {
		t.Error("r on a sprint with no loaded issues should not open the picker")
	}
}

// The issue tab's s adds the selected issue to the current (ACTIVE) sprint; it is a no-op (toast) with
// no active sprint or when the issue is already in it.
func TestAddIssueToCurrentSprint(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1", SprintID: 0}}, TotalElements: 1})
	// no active sprint prefetched yet -> a toast, no command that would hit the network
	_, cmd := m.Update(press("s"))
	if cmd == nil {
		t.Error("s with no active sprint should surface a toast")
	}
	// with an active sprint known, s on an unsprinted issue fires the add command
	m.currentSprint = &domain.SprintSummary{ID: 9, Status: "ACTIVE"}
	m2, cmd := m.Update(press("s"))
	_ = m2
	if cmd == nil {
		t.Error("s should fire the add command when an active sprint exists")
	}
	// an issue already in the current sprint is a no-op toast (not an add)
	m.issues[0].SprintID = 9
	_, cmd = m.Update(press("s"))
	if cmd == nil {
		t.Error("s on an already-sprinted issue should still surface a toast")
	}
}

// A successful add/remove refetches the selected sprint's issues; a failure only toasts.
func TestSprintIssuesChanged(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 6, Status: "ACTIVE"}})
	m.selSprintID = 6
	m.sprintIssues[6] = domain.IssuePage{TotalElements: 1, Issues: []domain.IssueSummary{{Key: "TIS-3"}}}
	m, cmd := m.Update(sprintIssuesChangedMsg{sprintID: 6, issueKey: "TIS-3", added: false})
	if _, cached := m.sprintIssues[6]; cached {
		t.Error("a successful change should evict the sprint's issue cache to force a refetch")
	}
	if !m.sprintIssuesPending[6] {
		t.Error("the selected sprint's issues should be refetched (pending) after a change")
	}
	if cmd == nil {
		t.Error("a successful change should refetch and toast")
	}

	failed := sprintsTabModel(t, []domain.SprintSummary{{ID: 6, Status: "ACTIVE"}})
	failed.selSprintID = 6
	failed.sprintIssues[6] = domain.IssuePage{TotalElements: 1, Issues: []domain.IssueSummary{{Key: "TIS-3"}}}
	failed, _ = failed.Update(sprintIssuesChangedMsg{sprintID: 6, issueKey: "TIS-3", added: true, err: true, status: 400})
	if _, cached := failed.sprintIssues[6]; !cached {
		t.Error("a failed change should keep the existing issue cache")
	}
}

// A successful add/remove patches the issue-list row's SprintID so the "already in the current sprint"
// guard works for the rest of the session (patchRow can't carry it - IssueDetail has no sprint field).
func TestSprintChangePatchesRowSprintID(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 9, Status: "ACTIVE"}})
	m.issues = []domain.IssueSummary{{Key: "TIS-1", SprintID: 0}}
	m, _ = m.Update(sprintIssuesChangedMsg{sprintID: 9, issueKey: "TIS-1", added: true})
	if m.issues[0].SprintID != 9 {
		t.Errorf("an add should patch the row's SprintID to the target sprint, got %d", m.issues[0].SprintID)
	}
	m, _ = m.Update(sprintIssuesChangedMsg{sprintID: 9, issueKey: "TIS-1", added: false})
	if m.issues[0].SprintID != 0 {
		t.Errorf("a remove should clear the row's SprintID, got %d", m.issues[0].SprintID)
	}
}

// Moving an issue from one sprint into another evicts BOTH sprints' cached issue lists (the target and
// the one it left), so a re-select of either refetches rather than showing a stale membership.
func TestSprintAddEvictsPreviousSprintCache(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 9, Status: "ACTIVE"}, {ID: 8, Status: "PLANNING"}})
	m.selSprintID = 9
	m.sprintIssues[9] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-2"}}}
	m.sprintIssues[8] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1"}}} // TIS-1 currently in sprint 8
	m, _ = m.Update(sprintIssuesChangedMsg{sprintID: 9, prevSprintID: 8, issueKey: "TIS-1", added: true})
	if _, cached := m.sprintIssues[8]; cached {
		t.Error("the previous sprint's cached issue list should be evicted after the issue moved out of it")
	}
	if _, cached := m.sprintIssues[9]; cached {
		t.Error("the target sprint's cached issue list should be evicted (refetched) after the add")
	}
}

// A late fire-once Init prefetch of the current sprint must not clobber the fresher value the sprint
// list has since set (generation guard).
func TestCurrentSprintPrefetchGenGuard(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	// the sprint list loads and sets the authoritative current sprint, bumping the generation is not needed
	// here; the Init prefetch carries gen 0 while a later refresh would carry a higher gen
	m.currentSprint = &domain.SprintSummary{ID: 5, Status: "ACTIVE"}
	m.currentSprintGen = 1 // a newer refresh has claimed ownership
	m, _ = m.Update(currentSprintLoadedMsg{gen: 0, sprint: &domain.SprintSummary{ID: 2, Status: "ACTIVE"}})
	if m.currentSprint == nil || m.currentSprint.ID != 5 {
		t.Errorf("a superseded prefetch (gen 0) must not overwrite the fresher current sprint, got %+v", m.currentSprint)
	}
	// a matching-gen result is applied
	m, _ = m.Update(currentSprintLoadedMsg{gen: 1, sprint: &domain.SprintSummary{ID: 7, Status: "ACTIVE"}})
	if m.currentSprint == nil || m.currentSprint.ID != 7 {
		t.Errorf("a current-gen result should be applied, got %+v", m.currentSprint)
	}
}

// e opens the edit form for a PLANNING/ACTIVE sprint (not a closed one); a changed field fires the PATCH.
func TestSprintEditOpensAndSubmits(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 9, Title: "Old", Goal: "g", Status: "PLANNING"}})
	m, _ = m.Update(press("e"))
	if !m.sprintEditing || m.sprintActionID != 9 {
		t.Fatalf("e should open the edit form (editing=%v id=%d)", m.sprintEditing, m.sprintActionID)
	}
	m, cmd := m.Update(sprintEditSubmittedMsg{v: sprintEditValues{title: "New", goal: "g"}})
	if m.sprintEditing {
		t.Error("submitting should close the edit form")
	}
	if cmd == nil {
		t.Error("a changed title should fire the update command")
	}

	closed := sprintsTabModel(t, []domain.SprintSummary{{ID: 10, Status: "COMPLETED"}})
	closed, _ = closed.Update(press("e"))
	if closed.sprintEditing {
		t.Error("e on a completed sprint should not open the edit form")
	}
}

// A successful action reloads the list silently and re-points the cursor at the acted-on sprint; a
// failed one only toasts (no reload, no restore).
func TestSprintActionDone(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 3, Status: "PLANNING"}, {ID: 2, Status: "ACTIVE"}})
	m.sprintCursor, m.selSprintID = 1, 2

	m, cmd := m.Update(sprintActionDoneMsg{action: "complete", id: 2})
	if cmd == nil {
		t.Fatal("a successful action should reload the list")
	}
	if m.sprintRestoreID != 2 {
		t.Errorf("the restore id should be the acted-on sprint, got %d", m.sprintRestoreID)
	}
	m, _ = m.Update(sprintsLoadedMsg{key: testKey, gen: m.sprintReqGen, page: domain.SprintPage{
		Sprints:       []domain.SprintSummary{{ID: 3, Status: "PLANNING"}, {ID: 2, Status: "COMPLETED"}},
		TotalElements: 2,
	}})
	if got := m.sprints[m.sprintCursor].ID; got != 2 {
		t.Errorf("the cursor should re-point at the acted-on sprint, landed on id %d", got)
	}
	if m.sprintRestoreID != 0 {
		t.Error("the restore id should be cleared after use")
	}

	failed := sprintsTabModel(t, []domain.SprintSummary{{ID: 2, Status: "ACTIVE"}})
	before := failed.sprintReqGen
	failed, cmd = failed.Update(sprintActionDoneMsg{action: "complete", id: 2, err: true, status: 400})
	if failed.sprintReqGen != before || failed.sprintRestoreID != 0 {
		t.Error("a failed action should not reload or set a restore id")
	}
	if cmd == nil {
		t.Error("a failed action should surface an error toast")
	}
}

// A failed silent post-action refresh keeps the currently-visible list (rather than wiping it to an
// error body while the action's success toast is still on screen); a first-load failure still shows the
// error state so reopening the tab retries.
func TestSprintReloadFailureKeepsList(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 2, Status: "ACTIVE"}})
	m, _ = m.Update(sprintActionDoneMsg{action: "complete", id: 2}) // schedules a silent reload, sets restore id
	m, cmd := m.Update(sprintsLoadedMsg{key: testKey, gen: m.sprintReqGen, err: true})
	if m.sprintsErr {
		t.Error("a failed refresh with a good list must not flip to the error state")
	}
	if len(m.sprints) != 1 {
		t.Errorf("the previously-loaded list should survive a failed refresh, got %d", len(m.sprints))
	}
	if m.sprintRestoreID != 0 {
		t.Error("the restore id should be cleared on a failed refresh")
	}
	if cmd == nil {
		t.Error("a failed refresh should surface a transient toast")
	}

	first := loaded(t, 160, 40, domain.IssuePage{})
	first, _ = first.Update(press("2")) // lazy load, no prior list
	first, _ = first.Update(sprintsLoadedMsg{key: testKey, gen: first.sprintReqGen, err: true})
	if !first.sprintsErr {
		t.Error("a first-load failure (no prior list) should show the error state")
	}
	if first.sprintsRequested {
		t.Error("a first-load failure should reset sprintsRequested so reopening retries")
	}
}

// The edit form diffs against an open-time snapshot, so a background reload that mutates the live summary
// mid-edit cannot make an untouched save revert it. The goal is compared trimmed on both sides.
func TestSprintEditSnapshot(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 9, Title: "Old", Goal: "g", Status: "PLANNING"}})
	m, _ = m.Update(press("e"))
	if m.sprintEditBase.Title != "Old" || m.sprintEditBase.Goal != "g" {
		t.Fatalf("the edit form should snapshot the open-time summary, got %+v", m.sprintEditBase)
	}
	m.sprints[0].Title = "Changed" // a background reload lands while the form is open
	if e := diffSprintEdit(m.sprintEditBase, sprintEditValues{title: "Old", goal: "g"}); !e.Empty() {
		t.Error("submitting unchanged snapshot values should produce an empty diff (no revert of the live change)")
	}

	// an untouched goal must diff as unchanged despite stored surrounding whitespace (form value is trimmed)
	if e := diffSprintEdit(domain.SprintSummary{Title: "T", Goal: "  spaced  "}, sprintEditValues{title: "T", goal: "spaced"}); e.Goal != nil {
		t.Errorf("a trimmed-equal goal should not be in the diff, got %q", *e.Goal)
	}
}

// The Sprints tab view never renders taller than the terminal, in either the narrow stack (list over
// the split region) or the wide side-by-side layout, down to the minimum height where the windowed
// panels are floored.
func TestSprintViewFitsHeight(t *testing.T) {
	for _, tc := range []struct{ w, h int }{{100, 10}, {100, 12}, {100, 24}, {160, 10}, {160, 40}} {
		m := loaded(t, tc.w, tc.h, domain.IssuePage{})
		m.tab = tabSprints
		m.sprints = sprintList(3)
		m.selSprintID = 1
		if got := lipgloss.Height(m.View()); got > tc.h {
			t.Errorf("sprint view at %dx%d rendered %d lines (overflow)", tc.w, tc.h, got)
		}
	}
}

// findNextPlanningSprint picks the oldest (lowest-id) PLANNING sprint as the migrate target, regardless
// of the active sprint's id: a backlog sprint planned before the active one was started is still valid.
func TestFindNextPlanningSprint(t *testing.T) {
	sprints := []domain.SprintSummary{
		{ID: 12, Status: "PLANNING"},
		{ID: 11, Status: "ACTIVE"},   // current (higher id than the backlog planning sprint below)
		{ID: 10, Status: "PLANNING"}, // backlog planning, older than the active sprint - still the target
		{ID: 9, Status: "COMPLETED"},
	}
	if got := findNextPlanningSprint(sprints); got == nil || got.ID != 10 {
		t.Errorf("the oldest planning sprint (10) should be the migrate target, got %+v", got)
	}
	// no planning sprint at all
	none := []domain.SprintSummary{{ID: 11, Status: "ACTIVE"}, {ID: 9, Status: "COMPLETED"}}
	if got := findNextPlanningSprint(none); got != nil {
		t.Errorf("no planning sprint should return nil, got %+v", got)
	}
}

// The migrate picker opens asynchronously when its candidate fetch lands. If the user opened another
// modal (or left the Sprints tab) while it loaded, the late result must be dropped, never stealing or
// replacing what they are now interacting with.
func TestMigrateLoadDoesNotClobberOpenModal(t *testing.T) {
	base := func(t *testing.T) Model {
		m := sprintsTabModel(t, []domain.SprintSummary{{ID: 10, Status: "ACTIVE"}, {ID: 11, Status: "PLANNING"}})
		m.currentSprint = &domain.SprintSummary{ID: 10, Status: "ACTIVE"}
		m.selSprintID = 10
		m.sprintIssues[10] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1", Title: "x"}}}
		m, _ = m.Update(press("m")) // start the candidate load; no picker open yet
		return m
	}

	// a remove picker opened during the load must not be replaced by the migrate confirmation
	m := base(t)
	gen := m.migrateGen
	m, _ = m.Update(press("r"))
	if !m.picking || m.pickKind != pickSprintRemoveIssue {
		t.Fatalf("r should open the remove picker while migrate loads (picking=%v kind=%v)", m.picking, m.pickKind)
	}
	m, _ = m.Update(migrateCandidatesLoadedMsg{gen: gen, issues: []domain.IssueSummary{{Key: "TIS-1"}}})
	if m.pickKind != pickSprintRemoveIssue || m.sprintConfirming {
		t.Errorf("a late migrate load must not replace the open remove picker (kind=%v confirming=%v)", m.pickKind, m.sprintConfirming)
	}

	// leaving the Sprints tab during the load also drops the result (no dialog floats over the Issues tab)
	m2 := base(t)
	gen2 := m2.migrateGen
	m2, _ = m2.Update(press("1")) // switch to the Issues tab
	m2, _ = m2.Update(migrateCandidatesLoadedMsg{gen: gen2, issues: []domain.IssueSummary{{Key: "TIS-1"}}})
	if m2.sprintConfirming || m2.picking {
		t.Error("a migrate load landing after a tab switch must not open the confirmation")
	}
}

// m migrates the current sprint's incomplete issues; it is gated on there being an active sprint and a
// later planning sprint to move them into (otherwise a toast, no candidate load).
func TestMigrateGating(t *testing.T) {
	// no active sprint prefetched -> toast, no load
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 10, Status: "PLANNING"}})
	m, cmd := m.Update(press("m"))
	if m.migrateLoading || m.picking {
		t.Error("m with no active sprint should not start a migrate")
	}
	if cmd == nil {
		t.Error("m with no active sprint should surface a toast")
	}

	// active sprint but no later planning target -> toast, no load
	noTarget := sprintsTabModel(t, []domain.SprintSummary{{ID: 10, Status: "ACTIVE"}})
	noTarget.currentSprint = &domain.SprintSummary{ID: 10, Status: "ACTIVE"}
	noTarget, cmd = noTarget.Update(press("m"))
	if noTarget.migrateLoading {
		t.Error("m with no planning target should not start a candidate load")
	}
	if cmd == nil {
		t.Error("m with no planning target should surface a toast")
	}

	// active sprint + a later planning sprint -> fetches candidates for the source, remembers the target
	ok := sprintsTabModel(t, []domain.SprintSummary{{ID: 10, Status: "ACTIVE"}, {ID: 11, Status: "PLANNING"}})
	ok.currentSprint = &domain.SprintSummary{ID: 10, Status: "ACTIVE"}
	ok, cmd = ok.Update(press("m"))
	if !ok.migrateLoading || cmd == nil {
		t.Fatalf("m with an active sprint and a planning target should load candidates (loading=%v cmd=%v)", ok.migrateLoading, cmd != nil)
	}
	if ok.migrateSourceID != 10 || ok.migrateTargetID != 11 {
		t.Errorf("migrate should target the current sprint (10) -> next planning (11), got source=%d target=%d", ok.migrateSourceID, ok.migrateTargetID)
	}
}

// The loaded candidates open a yes/no confirmation carrying the incomplete issue keys; accepting fires
// the migrate command. An empty result or a superseded (stale-gen) load never opens the dialog.
func TestMigrateConfirm(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 10, Status: "ACTIVE"}, {ID: 11, Status: "PLANNING"}})
	m.currentSprint = &domain.SprintSummary{ID: 10, Status: "ACTIVE"}
	m, _ = m.Update(press("m")) // sets migrateGen and the source/target names
	gen := m.migrateGen
	m, _ = m.Update(migrateCandidatesLoadedMsg{gen: gen, issues: []domain.IssueSummary{{Key: "TIS-1", Title: "a"}, {Key: "TIS-2", Title: "b"}}})
	if !m.sprintConfirming || m.sprintConfirmKind != "migrate" || m.picking {
		t.Fatalf("loaded candidates should open the migrate confirmation (confirming=%v kind=%q picking=%v)", m.sprintConfirming, m.sprintConfirmKind, m.picking)
	}
	if len(m.migrateKeys) != 2 {
		t.Errorf("the confirmation should carry the incomplete issue keys, got %v", m.migrateKeys)
	}
	// accepting fires the migrate command and closes the dialog
	m2, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if m2.sprintConfirming || cmd == nil {
		t.Errorf("accepting should fire the migrate command and close the dialog (confirming=%v cmd=%v)", m2.sprintConfirming, cmd != nil)
	}

	// empty candidate set -> no dialog, just a note
	empty := sprintsTabModel(t, []domain.SprintSummary{{ID: 10, Status: "ACTIVE"}, {ID: 11, Status: "PLANNING"}})
	empty.currentSprint = &domain.SprintSummary{ID: 10, Status: "ACTIVE"}
	empty, _ = empty.Update(press("m"))
	empty, cmd = empty.Update(migrateCandidatesLoadedMsg{gen: empty.migrateGen, issues: nil})
	if empty.sprintConfirming {
		t.Error("an empty candidate set should not open the confirmation")
	}
	if cmd == nil {
		t.Error("an empty candidate set should surface a note")
	}

	// a superseded load (older gen) is dropped
	stale := sprintsTabModel(t, []domain.SprintSummary{{ID: 10, Status: "ACTIVE"}, {ID: 11, Status: "PLANNING"}})
	stale.currentSprint = &domain.SprintSummary{ID: 10, Status: "ACTIVE"}
	stale, _ = stale.Update(press("m"))
	stale, _ = stale.Update(migrateCandidatesLoadedMsg{gen: stale.migrateGen - 1, issues: []domain.IssueSummary{{Key: "TIS-9"}}})
	if stale.sprintConfirming {
		t.Error("a stale-gen candidate load should be ignored")
	}
}

// A successful migrate evicts both sprints' cached issue lists (refetching the selected one) and patches
// the moved rows' SprintID to the target; a failure keeps the caches and only toasts.
func TestSprintMigrateDone(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 9, Status: "ACTIVE"}, {ID: 10, Status: "PLANNING"}})
	m.selSprintID = 9
	m.sprintIssues[9] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1"}, {Key: "TIS-2"}}}
	m.sprintIssues[10] = domain.IssuePage{Issues: []domain.IssueSummary{}}
	m.issues = []domain.IssueSummary{{Key: "TIS-1", SprintID: 9}, {Key: "TIS-2", SprintID: 9}, {Key: "TIS-3", SprintID: 0}}
	m, cmd := m.Update(sprintMigrateDoneMsg{sourceID: 9, targetID: 10, keys: []string{"TIS-1", "TIS-2"}})
	if _, cached := m.sprintIssues[9]; cached {
		t.Error("the source sprint's cached issue list should be evicted after a migrate")
	}
	if _, cached := m.sprintIssues[10]; cached {
		t.Error("the target sprint's cached issue list should be evicted after a migrate")
	}
	if !m.sprintIssuesPending[9] {
		t.Error("the selected (source) sprint's issues should be refetched (pending) after a migrate")
	}
	if m.issues[0].SprintID != 10 || m.issues[1].SprintID != 10 {
		t.Errorf("migrated rows should point at the target sprint, got %d and %d", m.issues[0].SprintID, m.issues[1].SprintID)
	}
	if m.issues[2].SprintID != 0 {
		t.Errorf("an unmigrated row should be untouched, got %d", m.issues[2].SprintID)
	}
	if cmd == nil {
		t.Error("a successful migrate should refetch and toast")
	}

	failed := sprintsTabModel(t, []domain.SprintSummary{{ID: 9, Status: "ACTIVE"}, {ID: 10, Status: "PLANNING"}})
	failed.selSprintID = 9
	failed.sprintIssues[9] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1"}}}
	failed.issues = []domain.IssueSummary{{Key: "TIS-1", SprintID: 9}}
	failed, _ = failed.Update(sprintMigrateDoneMsg{sourceID: 9, targetID: 10, keys: []string{"TIS-1"}, err: true, status: 403})
	if _, cached := failed.sprintIssues[9]; !cached {
		t.Error("a failed migrate should keep the existing issue cache")
	}
	if failed.issues[0].SprintID != 9 {
		t.Errorf("a failed migrate should not move the row, got SprintID %d", failed.issues[0].SprintID)
	}
}

// A transition (or any detail change) on the Issues tab is mirrored into the Sprints tab's cached issue
// list, so the sprint's issue panel shows the new state without a refetch.
func TestTransitionPatchesSprintIssueCache(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1", StateLabel: "To Do", StateCategory: "INITIAL"}}, TotalElements: 1})
	m.details["TIS-1"] = domain.IssueDetail{Key: "TIS-1", StateLabel: "To Do", StateCategory: "INITIAL"}
	m.sprintIssues[5] = domain.IssuePage{Issues: []domain.IssueSummary{{Key: "TIS-1", StateLabel: "To Do", StateCategory: "INITIAL"}}}
	m.viewKey = "TIS-1"
	m.applyTransition("TIS-1", domain.IssueTransition{TargetLabel: "In Progress", TargetCategory: "ACTIVE"})
	got := m.sprintIssues[5].Issues[0]
	if got.StateLabel != "In Progress" || got.StateCategory != "ACTIVE" {
		t.Errorf("a transition should patch the cached sprint issue row, got %q/%q", got.StateLabel, got.StateCategory)
	}
	if m.issues[0].StateLabel != "In Progress" {
		t.Errorf("the issue-list row should still be patched, got %q", m.issues[0].StateLabel)
	}
}

// The Report block renders the selected sprint's completion summary from the cached report.
func TestSprintReportBlockRenders(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 7, Key: "S-7", Title: "Now", Status: "ACTIVE"}})
	m.sprintReport[7] = domain.SprintReport{
		TotalIssues: 12, CompletedIssues: 5, OpenIssues: 6, CompletionRate: 0.42,
		TotalStoryPoints: 21, CompletedStoryPoints: 9, PointsCompletionRate: 0.43,
	}
	body := plain(m.View())
	for _, want := range []string{
		"Report", "Done", "42%", "12 total", "5 done", "6 carried", "1 aborted",
		"21 committed", "9 done", "43%",
	} {
		if !strings.Contains(body, want) {
			t.Errorf("sprint report block missing %q:\n%s", want, body)
		}
	}
}

// While a sprint's report is loading (no cache yet), the block shows a loading note rather than blanking.
func TestSprintReportLoadingNote(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 7, Key: "S-7", Title: "Now", Status: "ACTIVE"}})
	m.sprintReportPending[7] = true
	if body := plain(m.View()); !strings.Contains(body, "Loading report") {
		t.Errorf("a loading report should show a note:\n%s", body)
	}
}

// Loading a sprint's issues also kicks off its report (lockstep), so the summary tracks the list.
func TestSprintReportLoadsWithIssues(t *testing.T) {
	m := sprintsTabModel(t, sprintList(1))
	if cmd := m.startSprintIssuesLoad(1); cmd == nil {
		t.Fatal("startSprintIssuesLoad should return a batched load command")
	}
	if !m.sprintReportPending[1] || m.sprintReportGen[1] != 1 {
		t.Errorf("the report load should be armed alongside the issues (pending=%v gen=%d)",
			m.sprintReportPending[1], m.sprintReportGen[1])
	}
}

// A superseded report result (stale generation) is dropped rather than overwriting a fresher one.
func TestSprintReportStaleDropped(t *testing.T) {
	m := sprintsTabModel(t, sprintList(1))
	m.sprintReportGen[1] = 2
	m, _ = m.onSprintReportLoaded(sprintReportLoadedMsg{sprintID: 1, gen: 1, report: domain.SprintReport{TotalIssues: 99}})
	if _, cached := m.sprintReport[1]; cached {
		t.Error("a stale-generation report result should be dropped")
	}
}

// A refresh that fails AFTER a report was cached must not render the now-stale numbers (they would
// contradict the freshly-reloaded issue list) - the block reads "unavailable" instead.
func TestSprintReportFailedRefreshHidesStale(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 7, Key: "S-7", Title: "Now", Status: "ACTIVE"}})
	m.sprintReport[7] = domain.SprintReport{TotalIssues: 12, CompletedIssues: 5, OpenIssues: 6, CompletionRate: 0.42}
	m.sprintReportFailed[7] = true // a lockstep refresh failed while the old report was still cached
	body := plain(m.View())
	if strings.Contains(body, "42%") || strings.Contains(body, "12 total") {
		t.Errorf("a failed refresh must not render the stale cached report:\n%s", body)
	}
	if !strings.Contains(body, "Report unavailable") {
		t.Errorf("a failed refresh should read unavailable:\n%s", body)
	}
}
