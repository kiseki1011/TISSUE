package schema

import (
	"testing"

	tea "charm.land/bubbletea/v2"
)

func keyUp() tea.KeyPressMsg { return tea.KeyPressMsg{Code: tea.KeyUp} }

// Details focuses a graph element, not a section button, keeping the "edit the start state" default.
func TestDetailDefaultSelectsGraphElement(t *testing.T) {
	m := mkWorkflowModel(t)
	e, ok := m.selectedElem()
	if !ok {
		t.Fatal("nothing selected in a loaded workflow's Details")
	}
	if e.kind == elemVcsEdit || e.kind == elemFlowEdit {
		t.Fatalf("default selection = %+v, want a graph element", e)
	}
}

// The section Edit buttons are focus stops above the graph: up reaches Flow, then VCS.
func TestDetailActionButtonsNavigable(t *testing.T) {
	m := mkWorkflowModel(t)
	m, _ = m.Update(keyUp())
	if e, _ := m.selectedElem(); e.kind != elemFlowEdit {
		t.Fatalf("up from the first element selected %+v, want the Flow Edit button", e)
	}
	m, _ = m.Update(keyUp())
	if e, _ := m.selectedElem(); e.kind != elemVcsEdit {
		t.Fatalf("up again selected %+v, want the VCS Edit button", e)
	}
}

// A section button reports focused only while the Details pane holds focus.
func TestDetailActionFocusFlag(t *testing.T) {
	m := mkWorkflowModel(t)
	if m.actionFocused(elemVcsEdit) || m.actionFocused(elemFlowEdit) {
		t.Fatal("a section button is focused by default, want a graph element focused")
	}
	m = selectElem(m, wfElem{elemVcsEdit, 0})
	if !m.actionFocused(elemVcsEdit) {
		t.Error("selecting the VCS Edit button did not mark it focused")
	}
	away, _ := m.setFocus(paneWorkflows)
	if away.actionFocused(elemVcsEdit) {
		t.Error("button still reports focused after the Details pane lost focus")
	}
}

func TestDetailActionEnterOpensVcs(t *testing.T) {
	m := selectElem(vcsModel(t, 10, 0), wfElem{elemVcsEdit, 0})
	m, _ = m.Update(keyEnter())
	if !m.vcsEditing {
		t.Fatal("enter on the focused VCS Edit button did not open the VCS editor")
	}
}

func TestDetailActionEnterOpensFlow(t *testing.T) {
	m := selectElem(flowDetailModel(t), wfElem{elemFlowEdit, 0})
	m, _ = m.Update(keyEnter())
	if !m.flowEditing {
		t.Fatal("enter on the focused Flow Edit button did not open the flow editor")
	}
}

func TestDetailActionEnterIgnoredOnElement(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemState, 1})
	m, _ = m.Update(keyEnter())
	if m.vcsEditing || m.flowEditing {
		t.Error("enter on a graph element wrongly opened a section editor")
	}
}
