package project

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// parentEditReady loads a STANDARD-hierarchy issue with a catalog that has an EPIC type (a valid parent
// level), so the parent-edit picker can resolve eligible candidates.
func parentEditReady(t *testing.T, parent *domain.IssueRef) Model {
	t.Helper()
	det := sampleDetail()
	det.TypeName = "Story"
	det.Parent = parent
	m := editReady(t, det)
	m.types = []domain.IssueTypeSummary{
		{ID: 5, Name: "Story", Hierarchy: "STANDARD"},
		{ID: 6, Name: "Epic", Hierarchy: "EPIC"},
	}
	m.typesLoaded = true
	return m
}

// openParentPickerViaForm opens the parent picker the way the UI now does it: the 'p' shortcut is gone,
// so Parent is a field in the "Edit issue" form (directly below Title) that opens the picker on Enter.
func openParentPickerViaForm(t *testing.T, m Model) (Model, tea.Cmd) {
	t.Helper()
	m, _ = m.Update(press("e"))
	if !m.editing {
		t.Fatal("e should open the edit form")
	}
	m.editUI, _ = m.editUI.focusOn(efParent)
	m, cmd := m.Update(press("enter"))
	if cmd == nil {
		t.Fatal("Enter on the Parent field should emit the open-picker command")
	}
	return m.Update(cmd()) // run openParentEditForm -> openParentEditPicker
}

// The Parent field sits directly below Title in the edit form for a parentable type, and is absent for a
// top-level (EPIC) type that cannot have a parent.
func TestEditFormParentFieldPlacement(t *testing.T) {
	det := sampleDetail()
	det.TypeName = "Story"

	withParent := newEditForm(testDeps(), det, true)
	fields := withParent.fields()
	if len(fields) < 2 || fields[0] != efTitle || fields[1] != efParent {
		t.Errorf("Parent should be the field right after Title, got order %v", fields)
	}
	if !strings.Contains(plain(withParent.body()), "Parent") {
		t.Errorf("the edit form body should render a Parent field:\n%s", plain(withParent.body()))
	}

	topLevel := newEditForm(testDeps(), det, false)
	if indexOfInt(topLevel.fields(), efParent) >= 0 {
		t.Error("a top-level issue type should have no Parent field in the edit form")
	}
}

// Enter on the edit form's Parent field loads the eligible parents and opens the picker; the issue
// itself is never offered as its parent.
func TestParentEditOpensFromEditForm(t *testing.T) {
	m := parentEditReady(t, nil)
	m, cmd := openParentPickerViaForm(t, m)
	if cmd == nil {
		t.Fatal("opening Parent should start a parent-candidate load")
	}
	m, _ = m.Update(parentEditCandidatesLoadedMsg{
		gen: m.parentEditGen, key: m.viewKey,
		candidates: []domain.IssueSummary{{Key: "EPIC-1", Title: "Big"}, {Key: m.viewKey, Title: "Myself"}},
	})
	if !m.picking || m.pickKind != pickParentEdit {
		t.Fatalf("candidates should open the parent-edit picker, picking=%v kind=%v", m.picking, m.pickKind)
	}
	view := plain(m.picker.View(m.deps.Styles))
	if !strings.Contains(view, "EPIC-1") || !strings.Contains(view, "None") {
		t.Errorf("picker should list None + eligible parents:\n%s", view)
	}
	if strings.Contains(view, "Myself") {
		t.Error("an issue must not be offered as its own parent")
	}
}

// Selecting a candidate optimistically sets the parent, fires the assign, and updates the still-open
// edit form's Parent row.
func TestParentEditSelectAssigns(t *testing.T) {
	m := parentEditReady(t, nil)
	m, _ = openParentPickerViaForm(t, m)
	m, _ = m.Update(parentEditCandidatesLoadedMsg{gen: m.parentEditGen, key: m.viewKey, candidates: []domain.IssueSummary{{Key: "EPIC-1"}}})
	m.picker = m.picker.Move(1) // None -> EPIC-1
	m, cmd := m.Update(press("enter"))
	if m.picking {
		t.Error("selecting should close the picker")
	}
	if p := m.details[m.viewKey].Parent; p == nil || p.Key != "EPIC-1" {
		t.Errorf("parent should be optimistically set to EPIC-1, got %v", p)
	}
	if m.editUI.parentKey != "EPIC-1" {
		t.Errorf("the edit form's Parent row should reflect the pick, got %q", m.editUI.parentKey)
	}
	if cmd == nil {
		t.Error("selecting a parent should fire the assign")
	}
}

// Choosing "None" detaches the current parent and clears the edit form's Parent row.
func TestParentEditNoneClears(t *testing.T) {
	m := parentEditReady(t, &domain.IssueRef{Key: "EPIC-1"})
	m, _ = openParentPickerViaForm(t, m)
	// EPIC-1 is not among the returned candidates, so the cursor stays on None (the first option)
	m, _ = m.Update(parentEditCandidatesLoadedMsg{gen: m.parentEditGen, key: m.viewKey, candidates: []domain.IssueSummary{{Key: "EPIC-2"}}})
	m, cmd := m.Update(press("enter")) // None
	if p := m.details[m.viewKey].Parent; p != nil {
		t.Errorf("None should clear the parent, got %v", p)
	}
	if m.editUI.parentKey != "" {
		t.Errorf("the edit form's Parent row should clear, got %q", m.editUI.parentKey)
	}
	if cmd == nil {
		t.Error("clearing a parent should fire the remove")
	}
}

// A SUBTASK/MICROTASK requires a parent, so the edit picker offers no "None (clear parent)" (mirroring
// the create flow). Regression for the review's CONFIRMED finding.
func TestParentEditRequiredTypeHasNoNone(t *testing.T) {
	det := sampleDetail()
	det.TypeName = "Subtask"
	det.Parent = &domain.IssueRef{Key: "STD-1"}
	m := editReady(t, det)
	m.types = []domain.IssueTypeSummary{
		{ID: 5, Name: "Story", Hierarchy: "STANDARD"},
		{ID: 7, Name: "Subtask", Hierarchy: "SUBTASK"},
	}
	m.typesLoaded = true
	m, _ = openParentPickerViaForm(t, m)
	m, _ = m.Update(parentEditCandidatesLoadedMsg{gen: m.parentEditGen, key: m.viewKey, candidates: []domain.IssueSummary{{Key: "STD-1"}, {Key: "STD-2"}}})
	if !m.picking {
		t.Fatal("candidates should open the picker")
	}
	if view := plain(m.picker.View(m.deps.Styles)); strings.Contains(view, "None") {
		t.Errorf("a parent-required type must not offer None (clear parent):\n%s", view)
	}
}

// A stale candidate load (superseded gen, or a different issue) is dropped rather than popping a picker.
func TestParentEditDropsStaleCandidates(t *testing.T) {
	m := parentEditReady(t, nil)
	m, _ = openParentPickerViaForm(t, m)
	m, _ = m.Update(parentEditCandidatesLoadedMsg{gen: m.parentEditGen - 1, key: m.viewKey, candidates: []domain.IssueSummary{{Key: "EPIC-1"}}})
	if m.picking {
		t.Error("a superseded-gen candidate load must not open the picker")
	}
}
