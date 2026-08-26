package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

func keyN() tea.KeyPressMsg { return tea.KeyPressMsg{Code: 'n', Text: "n"} }

// The seed graph is To Do / Done with no transitions — the author wires it.
func TestWorkflowCreateOpensWithSeedGraph(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyN())
	if !m.creatingWorkflow || !m.CapturingInput() {
		t.Fatalf("n did not open the create-workflow modal (creating=%v)", m.creatingWorkflow)
	}
	f := m.cworkflow
	if len(f.states) != 2 || len(f.trans) != 0 {
		t.Fatalf("seed graph = %d states / %d trans, want 2 / 0", len(f.states), len(f.trans))
	}
	// with a name set, only the missing transition blocks validation
	f.name.SetValue("Seed Flow")
	if msg := f.validate(); msg != "Add at least one transition." {
		t.Errorf("named seed with no transition = %q, want the add-transition prompt", msg)
	}
	f = f.applyEdge("Go", "n1", "n2") // To Do -> Done
	if msg := f.validate(); msg != "" {
		t.Errorf("seed graph is not valid once named and wired: %s", msg)
	}
}

// Every state in the payload carries a temp key, and transitions reference those keys.
func TestWorkflowCreateBuildInputs(t *testing.T) {
	f := newCreateWorkflowForm(optionsDeps())
	f.name.SetValue("Bugfix")
	f = f.applyNode("In Progress", "ACTIVE", "ANSI_YELLOW") // adds n3 alongside the two seeds
	f = f.applyEdge("Start", "n1", "n3")
	states, trans := f.buildInputs()
	if len(states) != 3 {
		t.Fatalf("states = %d, want 3", len(states))
	}
	for _, st := range states {
		if st.TempKey == "" || st.Name == "" || st.Color == "" || st.Category == "" {
			t.Errorf("incomplete state in payload: %+v", st)
		}
	}
	found := false
	for _, tr := range trans {
		if tr.Name == "Start" && tr.SourceTempKey == "n1" && tr.TargetTempKey == "n3" {
			found = true
		}
	}
	if !found {
		t.Errorf("added transition not wired by temp key: %+v", trans)
	}
}

// Marking a second state INITIAL demotes the previous one — exactly one initial survives.
func TestWorkflowCreateSingleInitial(t *testing.T) {
	f := newCreateWorkflowForm(optionsDeps())
	// state 1 (Done) -> INITIAL demotes state 0 (To Do)
	f.states[1].category = "INITIAL"
	f = f.enforceInitial(1)
	initial := 0
	for _, st := range f.states {
		if st.category == "INITIAL" {
			initial++
		}
	}
	if initial != 1 {
		t.Errorf("initial count = %d, want 1", initial)
	}
	if f.states[0].category == "INITIAL" {
		t.Error("the original initial state was not demoted")
	}
}

func TestWorkflowCreateValidation(t *testing.T) {
	f := newCreateWorkflowForm(optionsDeps())
	f.name.SetValue("x")
	if f.validate() == "" {
		t.Error("a one-character name was accepted")
	}
	f.name.SetValue("Valid Name")
	f.states[1].category = "ACTIVE" // demote Done: no COMPLETED state now
	if msg := f.validate(); !strings.Contains(msg, "completed") {
		t.Errorf("missing-completed not caught: %q", msg)
	}
}

func TestWorkflowCreateSaveReloads(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyN())
	m, cmd := m.Update(workflowCreatedMsg{})
	if m.creatingWorkflow {
		t.Fatal("create did not close the modal")
	}
	if cmd == nil {
		t.Error("create success did not reload the catalog")
	}
}

func TestWorkflowCreateFailureKeepsOpen(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyN())
	m, _ = m.Update(createWorkflowFailedMsg{message: "boom"})
	if !m.creatingWorkflow || m.cworkflow.status != "boom" {
		t.Errorf("failure not surfaced (open=%v status=%q)", m.creatingWorkflow, m.cworkflow.status)
	}
}

// esc backs out of an open node sub-form first, then closes the modal.
func TestWorkflowCreateEscNesting(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyN())
	m.cworkflow = m.cworkflow.openAddState()
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if !m.creatingWorkflow {
		t.Fatal("esc closed the whole modal while a sub-form was open")
	}
	if m.cworkflow.nodeOpen {
		t.Error("esc did not close the node sub-form")
	}
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if m.creatingWorkflow {
		t.Error("second esc did not close the modal")
	}
}

// scanView polls because a bare Scan+Get races bubblezone's background scan worker. NewGlobal keeps
// the test from depending on another test to initialize the zone manager.
func TestWorkflowCreateActionZones(t *testing.T) {
	zone.NewGlobal()
	f := newCreateWorkflowForm(optionsDeps())
	view := f.View()
	for _, id := range []string{"cw.name", "cw.desc", "cw.addstate", "cw.addtrans", "cw.save", "cw.cancel"} {
		scanView(t, view, id)
	}
}
