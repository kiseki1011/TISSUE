package schema

import (
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func autoOpenModel() Model {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode)}
	m := New(d)
	m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 30})
	m, _ = m.Update(LoadedMsg{Types: []domain.IssueTypeSummary{{ID: 1, Name: "Bug"}}})
	m.creatingField = true
	return m
}

// Creating a SELECT_OPTION/CHECKLIST field auto-opens its options editor once the reloaded detail
// carries the new field (matched by name, since the create call returns no id).
func TestCreateSelectFieldAutoOpensOptions(t *testing.T) {
	m := autoOpenModel()
	m, _ = m.Update(fieldCreatedMsg{typeID: 1, fieldType: "SELECT_OPTION", name: "Priority"})
	if m.pendingOptionsType != 1 || m.pendingOptionsName != "Priority" {
		t.Fatalf("pending not armed: type=%d name=%q", m.pendingOptionsType, m.pendingOptionsName)
	}
	if m.optionsEditing {
		t.Fatal("options editor opened before the detail reloaded")
	}
	m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
		ID: 1, Fields: []domain.IssueField{{ID: 5, Name: "Priority", Type: "SELECT_OPTION"}},
	}})
	if !m.optionsEditing {
		t.Fatal("options editor did not auto-open for the new select field")
	}
	if m.options.fieldID != 5 {
		t.Errorf("options editor targets field %d, want 5", m.options.fieldID)
	}
	if m.pendingOptionsType != 0 {
		t.Error("pending not cleared after opening")
	}
}

// A plain field type creates without opening the options editor.
func TestCreateTextFieldSkipsOptions(t *testing.T) {
	m := autoOpenModel()
	m, _ = m.Update(fieldCreatedMsg{typeID: 1, fieldType: "TEXT", name: "Notes"})
	if m.pendingOptionsType != 0 {
		t.Errorf("text field armed pending options: %d", m.pendingOptionsType)
	}
	m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
		ID: 1, Fields: []domain.IssueField{{ID: 6, Name: "Notes", Type: "TEXT"}},
	}})
	if m.optionsEditing {
		t.Error("options editor opened for a non-option field")
	}
}
