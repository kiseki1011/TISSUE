package schema

import (
	"strconv"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

func openVcsEditor(t *testing.T, opened, merged int) Model {
	t.Helper()
	m := vcsModel(t, opened, merged)
	m, _ = m.Update(keyRune('v'))
	if !m.vcsEditing {
		t.Fatal("v did not open the VCS editor")
	}
	return m
}

// v on a loaded workflow opens the VCS editor seeded with its mappings and transitions.
func TestOpenVcsSeedsForm(t *testing.T) {
	m := openVcsEditor(t, 10, 12) // 10 = Start, 12 = Approve
	if !m.CapturingInput() {
		t.Error("VCS editor should capture input")
	}
	if m.vcs.openedID != 10 || m.vcs.mergedID != 12 {
		t.Errorf("seeded opened=%d merged=%d, want 10/12", m.vcs.openedID, m.vcs.mergedID)
	}
	if len(m.vcs.transitions) != len(exampleWorkflow().Transitions) {
		t.Errorf("form has %d transitions, want %d", len(m.vcs.transitions), len(exampleWorkflow().Transitions))
	}
}

// The editor does not open for an issue-type selection or before the graph is loaded.
func TestOpenVcsOnlyForLoadedWorkflow(t *testing.T) {
	m := mkWorkflowModel(t)
	delete(m.wfDetail, 1) // graph not loaded
	m, _ = m.Update(keyRune('v'))
	if m.vcsEditing {
		t.Error("VCS editor opened before the graph was loaded")
	}
}

// Enter on a field opens a dropdown of None + every transition.
func TestVcsPickerSelectsTransition(t *testing.T) {
	m := openVcsEditor(t, 0, 0) // both None
	m.vcs.focus = vfOpened
	m.vcs, _ = m.vcs.onKey(keyEnter())
	if !m.vcs.pickOpen {
		t.Fatal("enter on a field did not open the transition dropdown")
	}
	if got, want := len(m.vcs.pick.options), len(exampleWorkflow().Transitions)+1; got != want {
		t.Fatalf("dropdown has %d options, want None + transitions = %d", got, want)
	}
	m.vcs.pick = m.vcs.pick.move(1) // off None onto the first transition
	want, _ := m.vcs.pick.selected()
	m.vcs = m.vcs.applyPick()
	if m.vcs.pickOpen {
		t.Error("selecting did not close the dropdown")
	}
	wantID, _ := strconv.Atoi(want.value)
	if m.vcs.openedID != wantID {
		t.Errorf("PR opened = %d, want the picked %d", m.vcs.openedID, wantID)
	}
}

// Choosing None clears a mapping back to zero.
func TestVcsPickerNoneClears(t *testing.T) {
	m := openVcsEditor(t, 10, 0)
	m.vcs.focus = vfOpened
	m.vcs, _ = m.vcs.onKey(keyEnter()) // seeded at Start (id 10)
	m.vcs.pick = m.vcs.pick.move(-1)   // wraps/moves onto None
	// force cursor to the None option to be explicit
	m.vcs.pick.cursor = 0
	m.vcs = m.vcs.applyPick()
	if m.vcs.openedID != 0 {
		t.Errorf("PR opened = %d, want 0 (None)", m.vcs.openedID)
	}
}

// esc closes the dropdown first, then the modal.
func TestVcsEscClosesPickerThenModal(t *testing.T) {
	m := openVcsEditor(t, 0, 0)
	m, _ = m.Update(keyEnter()) // opens the dropdown for the focused field
	if !m.vcs.pickOpen {
		t.Fatal("enter did not open the dropdown")
	}
	m, _ = m.Update(pressEsc())
	if !m.vcsEditing || m.vcs.pickOpen {
		t.Fatalf("esc should close only the dropdown (editing=%v pickOpen=%v)", m.vcsEditing, m.vcs.pickOpen)
	}
	m, _ = m.Update(pressEsc())
	if m.vcsEditing {
		t.Fatal("esc did not close the VCS editor")
	}
}

func TestVcsSubmitIssuesSave(t *testing.T) {
	m := openVcsEditor(t, 10, 0)
	m.vcs.focus = vfSave
	var cmd tea.Cmd
	m.vcs, cmd = m.vcs.onKey(keyEnter())
	if !m.vcs.submitting || cmd == nil {
		t.Fatalf("save did not start (submitting=%v cmd=%v)", m.vcs.submitting, cmd == nil)
	}
}

// Clicking the VCS rule's Edit button opens the editor (its zone survives the render pipeline).
func TestVcsEditButtonOpensEditor(t *testing.T) {
	m := vcsModel(t, 10, 0)
	_ = scanView(t, m.View(), "schema.vcs.edit")
	z := zone.Get("schema.vcs.edit")
	m, _ = m.Update(tea.MouseClickMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY, Button: tea.MouseLeft})
	if !m.vcsEditing {
		t.Fatal("clicking the VCS Edit button did not open the editor")
	}
}

// Hovering the VCS Edit button records it and repaints. Moving away clears it.
func TestVcsEditButtonHover(t *testing.T) {
	m := vcsModel(t, 10, 0)
	_ = scanView(t, m.View(), "schema.vcs.edit")
	z := zone.Get("schema.vcs.edit")
	base := m.View()

	hovered, _ := m.Update(tea.MouseMotionMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY})
	if hovered.hoverAction != "schema.vcs.edit" {
		t.Fatalf("hoverAction = %q, want schema.vcs.edit", hovered.hoverAction)
	}
	if hovered.View() == base {
		t.Error("hovering the Edit button did not change its appearance")
	}

	away, _ := m.Update(tea.MouseMotionMsg{X: 0, Y: 0})
	if away.hoverAction != "" {
		t.Errorf("hoverAction not cleared off the button: %q", away.hoverAction)
	}
}

// A successful save closes the editor, drops the cached graph, and refetches it.
func TestVcsSaveInvalidatesAndRefetches(t *testing.T) {
	m := openVcsEditor(t, 10, 0)
	m, cmd := m.Update(vcsSavedMsg{wfID: 1})
	if m.vcsEditing {
		t.Fatal("save did not close the VCS editor")
	}
	if _, ok := m.wfDetail[1]; ok {
		t.Error("cached graph not invalidated after save")
	}
	if !m.wfPending[1] || cmd == nil {
		t.Error("no refetch issued after save")
	}
}
