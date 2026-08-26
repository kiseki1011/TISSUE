package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
)

// focusAccentSGR is the TokyoNight accent (#ff9e64) mk() builds every form with. Focus is shown by
// painting the focused control's border in it.
const focusAccentSGR = "38;2;255;158;100"

// focusedBorderRow is the ground truth for FocusRow. Focus shows as border COLOUR, not weight, so the
// line carrying an accent-coloured corner (╭ form field, ┌ graph state) is the focused top row.
func focusedBorderRow(view string) int {
	for i, ln := range strings.Split(view, "\n") {
		if strings.ContainsAny(accentedRunes([]string{ln}, focusAccentSGR), "╭┌") {
			return i
		}
	}
	return -1
}

// FocusRow must match the real render row at every stop, or the host scrolls to the wrong place.
func TestCreateFieldFocusRowMatchesRender(t *testing.T) {
	m := openCreateFieldModal(t)
	f := m.cfield
	// the 6 stops: Name, Type, Required, Description, Create, Cancel
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

// This modal has a Color field, unlike the others.
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

// A long status wraps at the field width. FocusRow must count the wrapped height, or the buttons row
// it reports drifts above where they render.
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

// Covers both metadata fields and draft-graph states: both paint an accent border when focused.
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

// A wrapped transition label must report its full height, or the window reveals only its first row.
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

// The picker view replaces the form, so there is no control to scroll to.
func TestCreateFieldFocusRowSuppressedWhilePicking(t *testing.T) {
	m := openCreateFieldModal(t)
	f := m.cfield
	f, _ = f.focusOn(cffType)
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if !f.pickOpen {
		t.Fatal("type picker did not open")
	}
	if _, _, ok := f.FocusRow(); ok {
		t.Error("FocusRow should be not-ok while the picker is open")
	}
}

// On a terminal too short for the modal, the focused control must stay visible while tabbing.
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
		// the control's top must be visible, and its whole span too when it fits
		return top >= 1+off && (bottom < 1+off+visible || height >= visible)
	}

	if !inWindow(m) {
		t.Fatal("initial focus (Name) is not inside the window")
	}
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
	// tab once more wraps to Name, so the window must return to the top
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyTab})
	if !inWindow(m) {
		t.Error("wrapping focus back to Name did not bring the window to the top")
	}
}

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
