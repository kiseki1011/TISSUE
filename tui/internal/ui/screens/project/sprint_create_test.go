package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// A project with no sprints yet is exactly when one is needed, so n must work without a selection.
func TestSprintCreateOpensOnAnEmptyTab(t *testing.T) {
	m := sprintsTabModel(t, nil)
	m, _ = m.Update(press("n"))

	if !m.sprintEditing || !m.sprintCreating {
		t.Fatalf("n should open the create modal (editing=%v creating=%v)", m.sprintEditing, m.sprintCreating)
	}
	if got := plain(m.sprintEditUI.View()); !strings.Contains(got, "New sprint") {
		t.Errorf("the modal should be titled New sprint:\n%s", got)
	}
}

// The create call takes only a title and goal - a new sprint's due date is chosen when it is started -
// so offering Due here would collect a value that is silently dropped.
func TestSprintCreateFormHasNoDueField(t *testing.T) {
	m := sprintsTabModel(t, nil)
	m, _ = m.Update(press("n"))

	if fs := m.sprintEditUI.fields(); indexOfInt(fs, sefDue) >= 0 {
		t.Errorf("the create form should not offer Due, got ring %v", fs)
	}
	if got := plain(m.sprintEditUI.View()); strings.Contains(got, "Due") {
		t.Errorf("the create form should not render a Due row:\n%s", got)
	}
}

// The edit form keeps its Due field, so dropping it for create must not have removed it everywhere.
func TestSprintEditFormStillHasDue(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 1, Title: "S1", Status: "PLANNING"}})
	m, _ = m.Update(press("e"))

	if !m.sprintEditing || m.sprintCreating {
		t.Fatalf("e should open the edit modal (editing=%v creating=%v)", m.sprintEditing, m.sprintCreating)
	}
	if fs := m.sprintEditUI.fields(); indexOfInt(fs, sefDue) < 0 {
		t.Errorf("the edit form should still offer Due, got ring %v", fs)
	}
}

func TestSprintCreateRejectsAShortTitle(t *testing.T) {
	m := sprintsTabModel(t, nil)
	m, _ = m.Update(press("n"))
	m.sprintEditUI.title.SetValue("x")

	f, cmd := m.sprintEditUI.submit()
	if f.titleErr == "" {
		t.Error("a one-character title should be rejected in the form")
	}
	// focusing the offending field returns its own (blink) command, so check the kind rather than nil
	if cmd != nil {
		if _, submitted := cmd().(sprintEditSubmittedMsg); submitted {
			t.Error("a rejected title should not submit")
		}
	}
	if f.focus != sefTitle {
		t.Errorf("focus should return to the offending field, got %d", f.focus)
	}
}

func TestSprintCreateSubmitsTitleAndGoal(t *testing.T) {
	m := sprintsTabModel(t, nil)
	m, _ = m.Update(press("n"))
	m.sprintEditUI.title.SetValue("  Sprint 7  ")
	m.sprintEditUI.goal.SetValue("  ship the importer  ")

	_, cmd := m.sprintEditUI.submit()
	msg, ok := cmd().(sprintEditSubmittedMsg)
	if !ok {
		t.Fatalf("submit should emit sprintEditSubmittedMsg, got %T", cmd())
	}
	if msg.v.title != "Sprint 7" || msg.v.goal != "ship the importer" {
		t.Errorf("title/goal should be trimmed, got %q / %q", msg.v.title, msg.v.goal)
	}

	m, cmd = m.Update(msg)
	if m.sprintEditing || m.sprintCreating {
		t.Error("submitting should close the create modal and clear the creating flag")
	}
	if cmd == nil {
		t.Fatal("submitting a create should fire the create command")
	}
}

// Cancelling must clear the creating flag, or the next e (edit) would send a create instead of a patch.
func TestSprintCreateCancelClearsTheFlag(t *testing.T) {
	m := sprintsTabModel(t, nil)
	m, _ = m.Update(press("n"))
	m, _ = m.Update(sprintEditCancelledMsg{})

	if m.sprintEditing || m.sprintCreating {
		t.Errorf("esc should close the modal and clear both flags (editing=%v creating=%v)",
			m.sprintEditing, m.sprintCreating)
	}
}

// A created sprint's id comes back on the result so the reloaded list can land the cursor on it.
func TestSprintCreatedRestoresCursorToTheNewSprint(t *testing.T) {
	m := sprintsTabModel(t, sprintList(2))
	m, _ = m.Update(sprintActionDoneMsg{action: "create", id: 99})

	if m.sprintRestoreID != 99 {
		t.Errorf("sprintRestoreID = %d, want the new sprint 99", m.sprintRestoreID)
	}
	if sprintActionOkText("create") != "Sprint created." {
		t.Errorf("unexpected success copy: %q", sprintActionOkText("create"))
	}
}

// The server deletes only a CANCELLED sprint, so the guard explains instead of letting the call fail.
func TestSprintDeleteGating(t *testing.T) {
	m := sprintsTabModel(t, []domain.SprintSummary{{ID: 4, Title: "S4", Status: "CANCELLED"}})
	m, _ = m.Update(press("d"))
	if !m.sprintConfirming || m.sprintConfirmKind != "delete" || m.sprintActionID != 4 {
		t.Fatalf("d should open the delete confirm (confirming=%v kind=%q id=%d)",
			m.sprintConfirming, m.sprintConfirmKind, m.sprintActionID)
	}
	m, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if m.sprintConfirming || cmd == nil {
		t.Error("accepting should close the confirm and fire the delete command")
	}

	for _, status := range []string{"PLANNING", "ACTIVE", "COMPLETED"} {
		live := sprintsTabModel(t, []domain.SprintSummary{{ID: 5, Title: "S5", Status: status}})
		live, _ = live.Update(press("d"))
		if live.sprintConfirming {
			t.Errorf("d on a %s sprint should not open the delete confirm", status)
		}
	}
}

// A deleted sprint is gone from the reloaded list, so restoring the cursor to it must fall back to a
// valid row rather than leaving the selection dangling.
func TestSprintDeletedLeavesAValidSelection(t *testing.T) {
	m := sprintsTabModel(t, sprintList(3))
	m, _ = m.Update(sprintActionDoneMsg{action: "delete", id: 3})
	m, _ = m.Update(sprintsLoadedMsg{key: testKey, gen: m.sprintReqGen, page: domain.SprintPage{
		Sprints:       []domain.SprintSummary{{ID: 1, Title: "S1", Status: "ACTIVE"}},
		TotalElements: 1,
	}})

	if m.sprintCursor >= len(m.sprints) {
		t.Errorf("cursor %d is past the reloaded list of %d", m.sprintCursor, len(m.sprints))
	}
	if m.sprintRestoreID != 0 {
		t.Errorf("the restore id should be consumed by the reload, got %d", m.sprintRestoreID)
	}
}

// n is offered even with nothing selected; d only where the server would accept it.
func TestSprintFooterAdvertisesCreateAndDelete(t *testing.T) {
	empty := sprintsTabModel(t, nil)
	if !hasKey(empty.HelpKeys(), "n") {
		t.Error("the footer should offer n on an empty Sprints tab")
	}
	if hasKey(empty.HelpKeys(), "d") {
		t.Error("delete should not be offered with nothing selected")
	}

	active := sprintsTabModel(t, []domain.SprintSummary{{ID: 1, Status: "ACTIVE"}})
	if hasKey(active.HelpKeys(), "d") {
		t.Error("delete should not be offered for an active sprint")
	}

	cancelled := sprintsTabModel(t, []domain.SprintSummary{{ID: 1, Status: "CANCELLED"}})
	if !hasKey(cancelled.HelpKeys(), "d") {
		t.Error("delete should be offered for a cancelled sprint")
	}
}
