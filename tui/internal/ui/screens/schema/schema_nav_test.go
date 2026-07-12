package schema

import (
	"testing"
)

// e on the Issue Types list opens the type metadata editor directly, without entering Details.
func TestListEditOpensTypeMeta(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	m, _ = m.onKey(pressE())
	if !m.editing || m.edit.kind != editIssueType {
		t.Fatalf("e on the types list did not open the type editor (editing=%v kind=%v)", m.editing, m.edit.kind)
	}
	if m.focus != paneTypes {
		t.Errorf("focus moved to %d, want it to stay on the types list", m.focus)
	}
}

// e on the Workflows list opens the workflow metadata editor directly.
func TestListEditOpensWorkflowMeta(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.onKey(pressE())
	if !m.editing || m.edit.kind != editWorkflow {
		t.Fatalf("e on the workflows list did not open the workflow editor (editing=%v kind=%v)", m.editing, m.edit.kind)
	}
}

// a on the Issue Types list opens the new-field modal (the type detail is already loaded).
func TestListAddFieldFromTypes(t *testing.T) {
	m := typeFieldsModel(t)      // detail loaded, Details focused
	m, _ = m.setFocus(paneTypes) // move focus back to the list
	m, _ = m.onKey(keyRune('a'))
	if !m.creatingField {
		t.Fatal("a on the types list did not open the new-field modal")
	}
}

// v and f on the Workflows list open the VCS and flow editors once the graph is loaded.
func TestListVcsAndFlowFromWorkflows(t *testing.T) {
	base := mkWorkflowModel(t) // wfDetail[1] loaded, Details focused
	base, _ = base.setFocus(paneWorkflows)

	m, _ := base.onKey(keyRune('v'))
	if !m.vcsEditing {
		t.Error("v on the workflows list did not open the VCS editor")
	}
	m, _ = base.onKey(keyRune('f'))
	if !m.flowEditing {
		t.Error("f on the workflows list did not open the flow editor")
	}
}

// → and enter drill from a list into the Details pane, keeping the same item selected.
func TestListDrillIntoDetails(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	before := m.typeCursor
	m, _ = m.onKey(keyRight())
	if m.focus != paneDetail {
		t.Fatalf("→ from the list focus = %d, want paneDetail", m.focus)
	}
	if m.typeCursor != before {
		t.Errorf("drill-in changed the selected item (%d -> %d)", before, m.typeCursor)
	}
	// enter also drills in
	m2 := mk(120, 30, 3, 2, false)
	m2, _ = m2.setFocus(paneWorkflows)
	m2, _ = m2.onKey(keyEnter())
	if m2.focus != paneDetail {
		t.Fatalf("enter from the list focus = %d, want paneDetail", m2.focus)
	}
}

// ← and esc back out of Details to the list the detail belongs to.
func TestDetailDrillOutToList(t *testing.T) {
	m := typeFieldsModel(t) // kind selType, Details focused
	m, _ = m.onKey(keyLeft())
	if m.focus != paneTypes {
		t.Fatalf("← from a type's Details focus = %d, want paneTypes", m.focus)
	}

	w := mkWorkflowModel(t) // kind selWorkflow, Details focused
	w, _ = w.onKey(pressEsc())
	if w.focus != paneWorkflows {
		t.Fatalf("esc from a workflow's Details focus = %d, want paneWorkflows", w.focus)
	}
}
