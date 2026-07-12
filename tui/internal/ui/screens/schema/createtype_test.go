package schema

import (
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// Pressing n on the Issue Types pane opens the create-type modal, seeded with the workflow list,
// the default STANDARD hierarchy, and the first workflow preselected.
func TestTypeCreateOpensSeeded(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	m, _ = m.Update(keyN())
	if !m.creatingType || !m.CapturingInput() {
		t.Fatalf("n did not open the create-type modal (creating=%v)", m.creatingType)
	}
	if len(m.ctype.workflows) != 2 {
		t.Errorf("modal seeded %d workflows, want 2", len(m.ctype.workflows))
	}
	if m.ctype.hierarchy != "STANDARD" {
		t.Errorf("default hierarchy = %q, want STANDARD", m.ctype.hierarchy)
	}
	if m.ctype.wfID == 0 {
		t.Error("no workflow preselected")
	}
}

// The workflow dropdown selects a workflow by id, and the hierarchy dropdown selects a level.
func TestTypeCreatePickers(t *testing.T) {
	wfs := []domain.WorkflowSummary{{ID: 7, Name: "Alpha"}, {ID: 9, Name: "Beta"}}
	f := newCreateTypeForm(optionsDeps(), wfs)

	f = f.openWorkflowPicker()
	if !f.pickOpen {
		t.Fatal("workflow picker did not open")
	}
	f.pick.cursor = 1 // Beta
	f = f.applyPick()
	if f.wfID != 9 {
		t.Errorf("selected workflow id = %d, want 9", f.wfID)
	}

	f = f.openHierarchyPicker()
	f.pick.cursor = indexOf(issueHierarchies, "EPIC")
	f = f.applyPick()
	if f.hierarchy != "EPIC" {
		t.Errorf("selected hierarchy = %q, want EPIC", f.hierarchy)
	}
}

// Validation rejects a too-short name and a missing workflow before any network call.
func TestTypeCreateValidation(t *testing.T) {
	f := newCreateTypeForm(optionsDeps(), nil) // no workflows -> wfID stays 0
	f.name.SetValue("x")
	f, _ = f.submit()
	if f.submitting || f.nameErr == "" {
		t.Error("short name was not rejected")
	}
	f.name.SetValue("Bug")
	f, cmd := f.submit()
	if f.submitting || cmd != nil {
		t.Error("submitted with no workflow selected")
	}
	if f.status == "" {
		t.Error("missing-workflow was not surfaced")
	}
}

// A valid form submits a create command.
func TestTypeCreateSubmits(t *testing.T) {
	wfs := []domain.WorkflowSummary{{ID: 3, Name: "Flow"}}
	f := newCreateTypeForm(optionsDeps(), wfs)
	f.name.SetValue("Story")
	f, cmd := f.submit()
	if !f.submitting || cmd == nil {
		t.Error("a valid type did not submit")
	}
}

// A successful create closes the modal and reloads the catalog.
func TestTypeCreateSaveReloads(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	m, _ = m.Update(keyN())
	m, cmd := m.Update(typeCreatedMsg{})
	if m.creatingType {
		t.Fatal("create did not close the modal")
	}
	if cmd == nil {
		t.Error("create success did not reload the catalog")
	}
}

// A create failure keeps the modal open with the error shown.
func TestTypeCreateFailureKeepsOpen(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	m, _ = m.Update(keyN())
	m, _ = m.Update(createTypeFailedMsg{message: "taken"})
	if !m.creatingType || m.ctype.status != "taken" {
		t.Errorf("failure not surfaced (open=%v status=%q)", m.creatingType, m.ctype.status)
	}
}

// esc closes an open dropdown first, then the modal.
func TestTypeCreateEscNesting(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	m, _ = m.Update(keyN())
	m.ctype = m.ctype.openHierarchyPicker()
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if !m.creatingType {
		t.Fatal("esc closed the modal while a dropdown was open")
	}
	if m.ctype.pickOpen {
		t.Error("esc did not close the dropdown")
	}
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if m.creatingType {
		t.Error("second esc did not close the modal")
	}
}
