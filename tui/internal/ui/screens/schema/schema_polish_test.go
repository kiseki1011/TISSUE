package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

func keyCtrlS() tea.KeyPressMsg { return tea.KeyPressMsg{Code: 's', Mod: tea.ModCtrl} }
func keyTab() tea.KeyPressMsg   { return tea.KeyPressMsg{Code: tea.KeyTab} }

// ctrl+s submits the create-workflow modal from any focus, once the graph is valid.
func TestWorkflowCreateCtrlSSaves(t *testing.T) {
	f := newCreateWorkflowForm(optionsDeps())
	f.name.SetValue("Flow")
	f = f.applyEdge("Go", "n1", "n2") // wire To Do -> Done so the graph validates
	f, cmd := f.onKey(keyCtrlS())
	if !f.submitting || cmd == nil {
		t.Fatal("ctrl+s did not submit the create-workflow modal")
	}
}

// Hovering an action-bar button in the create-workflow modal tracks it, and a key press clears the
// stale hover so the keyboard focus is the sole highlight again.
func TestWorkflowCreateButtonHover(t *testing.T) {
	f := newCreateWorkflowForm(optionsDeps())
	_ = scanView(t, f.View(), "cw.save")
	z := zone.Get("cw.save")
	f, _ = f.Update(tea.MouseMotionMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY})
	if f.hover != "cw.save" {
		t.Fatalf("hover after motion = %q, want cw.save", f.hover)
	}
	f, _ = f.onKey(keyTab())
	if f.hover != "" {
		t.Errorf("hover after a key press = %q, want it cleared", f.hover)
	}
}

// The in-place flow editor tracks hover over its action-bar buttons too.
func TestFlowEditorButtonHover(t *testing.T) {
	m := flowModel(t)
	_ = scanView(t, m.View(), "flow.save")
	z := zone.Get("flow.save")
	m, _ = m.Update(tea.MouseMotionMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY})
	if m.flow.hover != "flow.save" {
		t.Fatalf("flow hover after motion = %q, want flow.save", m.flow.hover)
	}
	m, _ = m.Update(keyTab())
	if m.flow.hover != "" {
		t.Errorf("flow hover after a key press = %q, want it cleared", m.flow.hover)
	}
}

// ctrl+s submits the in-place flow editor from any focus.
func TestFlowEditorCtrlSSaves(t *testing.T) {
	m := flowModel(t) // the example graph is already valid
	m, cmd := m.updateFlow(keyCtrlS())
	if !m.flow.submitting || cmd == nil {
		t.Fatal("ctrl+s did not submit the flow editor")
	}
}

// The VCS transition picker annotates each transition with its source → target flow.
func TestVcsPickerShowsTransitionFlow(t *testing.T) {
	d := exampleWorkflow()
	f := newVcsForm(optionsDeps(), 1, d.Transitions, d.States, 0, 0)
	f = f.openPicker(vfOpened)
	var start string
	for _, o := range f.pick.options {
		if strings.HasPrefix(o.label, "Start") {
			start = o.label
		}
	}
	if !strings.Contains(start, "To Do → In Progress") {
		t.Errorf("Start option = %q, want it to include (To Do → In Progress)", start)
	}
}
