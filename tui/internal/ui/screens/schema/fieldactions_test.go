package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func shiftDown() tea.KeyPressMsg { return tea.KeyPressMsg{Code: tea.KeyDown, Mod: tea.ModShift} }

func TestAddFieldAffordanceRendersAndSelectable(t *testing.T) {
	m := typeFieldsModel(t)
	elems := m.typeElems()
	last := elems[len(elems)-1]
	if last.kind != elemAddField {
		t.Fatalf("last type element = %+v, want elemAddField", last)
	}
	view := m.View()
	if !strings.Contains(stripANSI(view), "+ Field") {
		t.Error("Details does not render the + Field handle")
	}
	_ = scanView(t, view, "schema.field.new")
	if z := zone.Get("schema.field.new"); z == nil || z.IsZero() {
		t.Error("+ Field has no clickable zone")
	}
}

// The modal is seeded with the append position: one past the highest existing field position.
func TestOpenFieldCreateComputesPosition(t *testing.T) {
	m := typeFieldsModel(t)
	for i := 0; i < len(m.typeElems())-1; i++ {
		m = m.moveTypeElem(keyDown())
	}
	if e, ok := m.selectedTypeElem(); !ok || e.kind != elemAddField {
		t.Fatalf("selection = %+v, want the + Field handle", e)
	}
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if !m.creatingField || !m.CapturingInput() {
		t.Fatalf("enter on + Field did not open the create modal (creating=%v)", m.creatingField)
	}
	// fixture fields have Position 0, so the append position is 1
	if m.cfield.position != 1 {
		t.Errorf("append position = %d, want 1", m.cfield.position)
	}
	if m.cfield.typeID != 1 {
		t.Errorf("create form typeID = %d, want 1", m.cfield.typeID)
	}
}

func TestFieldCreateClickOpens(t *testing.T) {
	m := typeFieldsModel(t)
	_ = scanView(t, m.View(), "schema.field.new")
	z := zone.Get("schema.field.new")
	m, _ = m.Update(tea.MouseClickMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY, Button: tea.MouseLeft})
	if !m.creatingField {
		t.Fatal("clicking + Field did not open the create modal")
	}
}

func TestCreateFieldSubmitRefetches(t *testing.T) {
	m := typeFieldsModel(t)
	m, _, _ = m.openFieldCreate()
	m, cmd := m.Update(fieldCreatedMsg{typeID: 1})
	if m.creatingField {
		t.Fatal("create did not close the modal")
	}
	if _, ok := m.typeDetail[1]; ok {
		t.Error("create did not invalidate the field cache")
	}
	if !m.detailPending[1] || cmd == nil {
		t.Error("no refetch issued after create")
	}
}

func TestCreateFieldFormTypeAndValidation(t *testing.T) {
	m := typeFieldsModel(t)
	m, _, _ = m.openFieldCreate()
	f := m.cfield
	if f.ftype != "TEXT" {
		t.Errorf("default type = %q, want TEXT", f.ftype)
	}
	f, _ = f.focusOn(cffType)
	f = f.openTypePicker()
	if !f.pickOpen {
		t.Fatal("type picker did not open")
	}
	f.pick.cursor = indexOf(fieldTypes, "SELECT_OPTION")
	f = f.applyPick()
	if f.ftype != "SELECT_OPTION" {
		t.Errorf("after pick, type = %q, want SELECT_OPTION", f.ftype)
	}

	f.name.SetValue("   ")
	f, _ = f.submit()
	if f.submitting {
		t.Error("submitted despite an empty name")
	}
	if f.nameErr == "" {
		t.Error("empty name was not flagged")
	}

	f.name.SetValue("Priority")
	f, cmd := f.submit()
	if !f.submitting || cmd == nil {
		t.Error("a valid field did not submit")
	}
}

func TestDeleteFieldOpensConfirm(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown()) // metadata -> field 11
	if e, ok := m.selectedTypeElem(); !ok || e.kind != elemField {
		t.Fatalf("selection = %+v, want a field", e)
	}
	m, _ = m.Update(keyX())
	if !m.confirming {
		t.Fatal("x did not open the confirm dialog")
	}
	if m.pendingDeleteField != 11 {
		t.Errorf("pending delete field = %d, want 11", m.pendingDeleteField)
	}
}

func TestConfirmDeleteRunsDelete(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown())
	m, _ = m.Update(keyX())
	m, cmd := m.Update(confirmAcceptedMsg{})
	if cmd == nil {
		t.Fatal("accepting did not fire a delete command")
	}
	m, _ = m.Update(fieldDeleteFailedMsg{message: "in use"})
	if !m.confirming {
		t.Fatal("a delete failure closed the dialog")
	}
	if m.confirm.Status != "in use" || m.confirm.Submitting {
		t.Errorf("failure not surfaced (status=%q submitting=%v)", m.confirm.Status, m.confirm.Submitting)
	}
	m, cmd = m.Update(fieldDeletedMsg{typeID: 1})
	if m.confirming {
		t.Fatal("success did not close the dialog")
	}
	if _, ok := m.typeDetail[1]; ok || !m.detailPending[1] || cmd == nil {
		t.Error("delete success did not invalidate + refetch")
	}
}

func TestConfirmCancelKeepsField(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown())
	m, _ = m.Update(keyX())
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if m.confirming || m.pendingDeleteField != 0 {
		t.Error("esc did not cancel the confirm dialog")
	}
}

func TestReorderFieldMovesAndCommits(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown()) // select field 11 (Story Points, first)
	m, cmd := m.Update(shiftDown())
	if cmd == nil {
		t.Fatal("reorder did not issue a command")
	}
	got := fieldIDOrder(m.typeDetail[1].Fields)
	if got[0] != 12 || got[1] != 11 {
		t.Errorf("order after shift+down = %v, want [12 11]", got)
	}
	if e, ok := m.selectedTypeElem(); !ok || e != (wfElem{elemField, 11}) {
		t.Errorf("selection = %+v, want field 11 (it moved with the field)", e)
	}
	// the now-last field cannot move further down
	if _, _, ok := m.reorderSelectedField(1); ok {
		t.Error("reordering past the end should be rejected")
	}
}

// A reorder failure refetches to resync the optimistic order.
func TestReorderFailedRefetches(t *testing.T) {
	m := typeFieldsModel(t)
	m, cmd := m.Update(fieldsReorderFailedMsg{typeID: 1})
	if _, ok := m.typeDetail[1]; ok || !m.detailPending[1] || cmd == nil {
		t.Error("reorder failure did not invalidate + refetch")
	}
}

func fieldIDOrder(fields []domain.IssueField) []int {
	ids := make([]int, len(fields))
	for i, f := range fields {
		ids[i] = f.ID
	}
	return ids
}
