package schema

import (
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// typeFieldsModel returns a model with an issue type selected in the Details pane and its fields
// loaded (Story Points id 11 required, Severity id 12 with options).
func typeFieldsModel(t *testing.T) Model {
	t.Helper()
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneDetail) // kind selType, T0 (id 1) selected, metadata is elem 0
	m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
		ID: 1, Name: "T0",
		Fields: []domain.IssueField{
			{ID: 11, Name: "Story Points", Type: "INTEGER", Required: true, Description: "Relative effort"},
			{ID: 12, Name: "Severity", Type: "SELECT_OPTION", Options: []domain.FieldOption{{ID: 1, Name: "low"}, {ID: 2, Name: "high"}}},
		},
	}})
	return m
}

func keyDown() tea.KeyPressMsg { return tea.KeyPressMsg{Code: tea.KeyDown} }

// Each custom field renders its own clickable edit-pen zone.
func TestFieldPensRenderPerField(t *testing.T) {
	m := typeFieldsModel(t)
	view := m.View()
	for _, id := range []int{11, 12} {
		zid := fieldEditZone(id)
		_ = scanView(t, view, zid)
		if z := zone.Get(zid); z == nil || z.IsZero() {
			t.Errorf("field %d has no edit-pen zone %q", id, zid)
		}
	}
}

// The Details selection starts on the metadata block; pressing down walks into the fields, and e
// opens the selected field's editor seeded with its values.
func TestSelectFieldWithArrowThenEdit(t *testing.T) {
	m := typeFieldsModel(t)
	if e, ok := m.selectedTypeElem(); !ok || e.kind != elemTypeMeta {
		t.Fatalf("initial selection = %+v, want the metadata block", e)
	}
	m = m.moveTypeElem(keyDown()) // metadata -> first field (Story Points)
	if e, ok := m.selectedTypeElem(); !ok || e != (wfElem{elemField, 11}) {
		t.Fatalf("after down, selection = %+v, want field 11", e)
	}
	m, _ = m.Update(pressE())
	if !m.fieldEditing || !m.CapturingInput() {
		t.Fatalf("e did not open the field editor (editing=%v)", m.fieldEditing)
	}
	if m.field.fieldID != 11 || m.field.name.Value() != "Story Points" || !m.field.required {
		t.Errorf("field editor seeded id=%d name=%q required=%v, want 11/Story Points/true",
			m.field.fieldID, m.field.name.Value(), m.field.required)
	}
}

// Clicking a field's pen opens that field's editor directly.
func TestFieldPenClickOpens(t *testing.T) {
	m := typeFieldsModel(t)
	_ = scanView(t, m.View(), fieldEditZone(12))
	z := zone.Get(fieldEditZone(12))
	m, _ = m.Update(tea.MouseClickMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY, Button: tea.MouseLeft})
	if !m.fieldEditing || m.field.fieldID != 12 {
		t.Fatalf("clicking field 12's pen did not open its editor (editing=%v id=%d)", m.fieldEditing, m.field.fieldID)
	}
}

// A successful field save closes the editor, drops the owning type's field cache, and refetches it.
func TestFieldSaveRefetchesType(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown())
	m, _ = m.Update(pressE())
	m, cmd := m.Update(fieldSavedMsg{typeID: 1})
	if m.fieldEditing {
		t.Fatal("save did not close the field editor")
	}
	if _, ok := m.typeDetail[1]; ok {
		t.Error("field save did not invalidate the type's field cache")
	}
	if !m.detailPending[1] || cmd == nil {
		t.Error("no refetch was issued after the field save")
	}
}

// The field form toggles required with space and rejects an empty name before any network call.
func TestFieldFormRequiredToggleAndValidation(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	f := newFieldForm(d, 1, 11, "Story Points", "effort", false)

	f, _ = f.focusOn(ffRequired)
	f, _ = f.onKey(tea.KeyPressMsg{Code: ' ', Text: " "}) // String() == "space"
	if !f.required {
		t.Error("space did not toggle required on")
	}

	f.name.SetValue("   ")
	f, _ = f.submit() // an invalid submit refocuses the name field (returning its focus cmd)
	if f.submitting {
		t.Error("submitted despite an empty name")
	}
	if f.nameErr == "" || f.focus != ffName {
		t.Error("empty-name submit did not flag the name field")
	}

	f.name.SetValue("Points")
	f, cmd := f.submit()
	if !f.submitting || cmd == nil {
		t.Error("a valid field did not submit")
	}
}
