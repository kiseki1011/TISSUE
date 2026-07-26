package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
)

// focusAccentSGR is the TokyoNight accent (#ff9e64) that mk() builds every form with; focus is shown
// by painting the focused control's border in this colour.
const focusAccentSGR = "38;2;255;158;100"

// focusedBorderRow is the line index of the focused control's top border in a form's full, unwindowed
// render. Focus is shown by the accent border COLOUR (not weight); a box's top-left corner is ╭ (a
// form field) or ┌ (a graph state box), so the line carrying an accent-coloured corner is the focused
// control's top row — the same row FocusRow reports. It is the ground truth for FocusRow.
func focusedBorderRow(view string) int {
	for i, ln := range strings.Split(view, "\n") {
		if strings.ContainsAny(accentedRunes([]string{ln}, focusAccentSGR), "╭┌") {
			return i
		}
	}
	return -1
}

// FocusRow must report the row where the focused control actually renders, for every focus stop —
// so the host scrolls to the right place. The accent-coloured border marks the focused control.
func TestCreateFieldFocusRowMatchesRender(t *testing.T) {
	m := openCreateFieldModal(t)
	f := m.cfield
	// walk every focus stop (Name, Type, Required, Description, Create, Cancel) and check agreement
	for stop := 0; stop < 6; stop++ {
		row, _, ok := f.FocusRow()
		if !ok {
			t.Fatalf("stop %d: FocusRow reported not-ok", stop)
		}
		if actual := focusedBorderRow(f.View()); actual != row {
			t.Errorf("focus %d: FocusRow row=%d but the focused control renders at row %d", f.focus, row, actual)
		}
		f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyTab})
	}
}

// The Edit Field modal reports the focused control's row correctly at every stop.
func TestFieldFormFocusRowMatchesRender(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown()) // metadata -> first field
	m, _ = m.Update(pressE())     // open the field editor
	if !m.fieldEditing {
		t.Fatal("field editor did not open")
	}
	f := m.field
	for stop := 0; stop < 5; stop++ { // Name, Description, Required, Save, Cancel
		row, _, ok := f.FocusRow()
		if !ok {
			t.Fatalf("stop %d: not-ok", stop)
		}
		if actual := focusedBorderRow(f.View()); actual != row {
			t.Errorf("fieldForm focus %d: FocusRow=%d but renders at %d", f.focus, row, actual)
		}
		f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyTab})
	}
}

// The Edit (issue type) modal — which includes a Color field — reports the focused row correctly.
func TestEditFormFocusRowMatchesRender(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	m, _ = m.onKey(pressE())
	if !m.editing {
		t.Fatal("type editor did not open")
	}
	f := m.edit
	for stop := 0; stop < 5; stop++ { // Name, Color, Description, Save, Cancel
		row, _, ok := f.FocusRow()
		if !ok {
			t.Fatalf("stop %d: not-ok", stop)
		}
		if actual := focusedBorderRow(f.View()); actual != row {
			t.Errorf("editForm focus %d: FocusRow=%d but renders at %d", f.focus, row, actual)
		}
		f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyTab})
	}
}

// The New Issue Type modal — five fields plus buttons — reports the focused row correctly.
func TestCreateTypeFocusRowMatchesRender(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	m, _ = m.Update(keyN())
	if !m.creatingType {
		t.Fatal("create-type modal did not open")
	}
	f := m.ctype
	for stop := 0; stop < 7; stop++ { // Name, Color, Hierarchy, Workflow, Description, Create, Cancel
		row, _, ok := f.FocusRow()
		if !ok {
			t.Fatalf("stop %d: not-ok", stop)
		}
		if actual := focusedBorderRow(f.View()); actual != row {
			t.Errorf("createTypeForm focus %d: FocusRow=%d but renders at %d", f.focus, row, actual)
		}
		f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyTab})
	}
}

// createTypeForm width-constrains its status message, so a long one wraps to two lines. FocusRow must
// count the wrapped height, or the buttons row it reports drifts above where they actually render.
func TestCreateTypeFocusRowWithWrappingStatus(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneTypes)
	m, _ = m.Update(keyN())
	f := m.ctype
	f.status = "Pick a workflow (create one first if the list is empty)." // wraps at the 40-col field width
	f.focus = ctSubmit                                                    // the Create button sits below the status
	if lipgloss.Height(f.deps.Styles.Error.Width(editFieldW).Padding(0, 1).Render(f.status)) < 2 {
		t.Fatal("precondition: the status was expected to wrap to >=2 lines")
	}
	row, _, ok := f.FocusRow()
	if !ok {
		t.Fatal("FocusRow not-ok with a status present")
	}
	if actual := focusedBorderRow(f.View()); actual != row {
		t.Errorf("with a wrapping status: FocusRow=%d but the Create button renders at %d", row, actual)
	}
}

// The New Workflow modal reports the focused row for both its metadata fields and the states inside
// the draft graph. Each renders an accent-coloured border when focused (fields via TitledBoxWeighted,
// the selected state via the graph's emphasizeBorder), so that colour is the ground truth.
func TestCreateWorkflowFocusRowMatchesRender(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyN())
	f := m.cworkflow
	// items: Name(0), Description(1), state To Do(2), state Done(3), then the action-bar affordances
	for _, stop := range []int{0, 1, 2, 3} {
		f.focus = stop
		row, _, ok := f.FocusRow()
		if !ok {
			t.Fatalf("item %d: FocusRow not-ok", stop)
		}
		if actual := focusedBorderRow(f.View()); actual != row {
			t.Errorf("createWorkflow item %d (kind %d): FocusRow=%d but renders at %d", stop, f.cur().kind, row, actual)
		}
	}
}

// Focusing a state low in the draft graph scrolls a windowed New Workflow modal so that state stays
// fully visible.
func TestCreateWorkflowFocusFollowsGraph(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyN())
	f := m.cworkflow
	f.focus = 3 // the lower "Done" state
	m.cworkflow = f
	m.height = 16 // shorter than the ~30-row modal
	m = m.followModalFocus()
	if m.modalScroll == 0 {
		t.Fatal("focusing a lower graph state did not scroll the window")
	}
	row, height, _ := m.cworkflow.FocusRow()
	visible, off := m.height-2, m.modalScroll
	if row < 1+off || row+height-1 > off+visible {
		t.Errorf("selected state rows %d..%d are not inside the window [%d,%d]", row, row+height-1, 1+off, off+visible)
	}
}

// A routed (back/skip) transition whose multi-word name wraps to several rows must report its FULL
// height, so a windowed modal reveals every line and not just the label's first row.
func TestCreateWorkflowTransitionHeightCoversWrappedLabel(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyN())
	f := m.cworkflow
	f = f.applyEdge("Send Back For Rework Now", "n2", "n1") // Done -> To Do: a back edge, name wraps
	f.focus = 4                                             // items: name, desc, state0, state1, trans0
	if f.cur().kind != cwTrans {
		t.Fatalf("focus %d is kind %d, want a transition", f.focus, f.cur().kind)
	}
	sel, _ := f.selElem()
	_, _, ghits := renderWorkflowGraph(f.draftDetail(), f.deps.Styles, cwStructW, sel, wfElem{}, false)
	wantH := ghits[sel].r1 - ghits[sel].r0 + 1
	if wantH < 2 {
		t.Fatalf("precondition: the transition label was expected to wrap to >=2 rows, got %d", wantH)
	}
	if _, h, ok := f.FocusRow(); !ok || h != wantH {
		t.Errorf("FocusRow height=%d (ok=%v) for a wrapped transition, want %d (its full label span)", h, ok, wantH)
	}
}

// While the type picker is open, FocusRow reports not-ok (the picker view replaces the form, so
// there is no form control to scroll to).
func TestCreateFieldFocusRowSuppressedWhilePicking(t *testing.T) {
	m := openCreateFieldModal(t)
	f := m.cfield
	f, _ = f.focusOn(cffType)
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyEnter}) // open the type picker
	if !f.pickOpen {
		t.Fatal("type picker did not open")
	}
	if _, _, ok := f.FocusRow(); ok {
		t.Error("FocusRow should be not-ok while the picker is open")
	}
}

// On a terminal too short for the modal, tabbing down scrolls the window so the focused control stays
// visible, and tabbing back up brings it back to the top.
func TestModalFocusFollowScrolls(t *testing.T) {
	m := openCreateFieldModal(t)
	m.height = 14 // shorter than the ~23-row New Field modal
	visible := m.height - 2

	inWindow := func(m Model) bool {
		row, height, ok := m.cfield.FocusRow()
		if !ok {
			return false
		}
		off := m.modalScroll
		top, bottom := row, row+height-1
		// the control's top must be visible; its whole span too when it fits the window
		return top >= 1+off && (bottom < 1+off+visible || height >= visible)
	}

	if !inWindow(m) {
		t.Fatal("initial focus (Name) is not inside the window")
	}
	// tab down to the buttons; the focused control must stay in the window at every step
	for i := 0; i < 5; i++ {
		m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyTab})
		if !inWindow(m) {
			row, _, _ := m.cfield.FocusRow()
			t.Fatalf("after %d tabs, focus row %d fell outside the window (offset %d)", i+1, row, m.modalScroll)
		}
	}
	if m.modalScroll == 0 {
		t.Error("tabbing to the bottom controls never scrolled the window")
	}
	if !strings.Contains(stripANSI(m.View()), "Create") {
		t.Error("the Create button is not visible after tabbing to it")
	}
	// tab forward once more wraps to Name (top); the window must return to the top
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyTab})
	if !inWindow(m) {
		t.Error("wrapping focus back to Name did not bring the window to the top")
	}
}

// A modal that fits the terminal is never scrolled by focus movement.
func TestModalFocusFollowNoScrollWhenItFits(t *testing.T) {
	m := openCreateFieldModal(t)
	m.height = lipgloss.Height(m.cfield.View()) + 6 // ample room
	for i := 0; i < 6; i++ {
		m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyTab})
		if m.modalScroll != 0 {
			t.Fatalf("focus movement scrolled a modal that fits: offset %d", m.modalScroll)
		}
	}
}
