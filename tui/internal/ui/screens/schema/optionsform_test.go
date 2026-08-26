package schema

import (
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func keyO() tea.KeyPressMsg { return tea.KeyPressMsg{Code: 'o', Text: "o"} }

func optionsDeps() deps.Deps {
	return deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
}

// o opens the options editor seeded from a SELECT_OPTION field, and is a no-op otherwise.
func TestOpenFieldOptions(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown()) // field 11 (Story Points, INTEGER — no options)
	if _, _, ok := m.openFieldOptions(); ok {
		t.Error("options editor opened for a non-option field")
	}
	m = m.moveTypeElem(keyDown()) // field 12 (Severity, SELECT_OPTION)
	m, _ = m.Update(keyO())
	if !m.optionsEditing || !m.CapturingInput() {
		t.Fatalf("o did not open the options editor (editing=%v)", m.optionsEditing)
	}
	if m.options.fieldID != 12 || len(m.options.rows) != 2 {
		t.Errorf("options seeded fieldID=%d rows=%d, want 12 / 2", m.options.fieldID, len(m.options.rows))
	}
}

// Rows are edited locally. diff then reports each change against the original options.
func TestOptionsDiff(t *testing.T) {
	orig := []domain.FieldOption{{ID: 1, Name: "low"}, {ID: 2, Name: "high"}, {ID: 3, Name: "urgent"}}
	f := newOptionsForm(optionsDeps(), 1, 12, "Severity", orig)

	f = f.openRename(0)
	f.input.SetValue("lowest")
	f = f.commitInput()
	f = f.removeRow(1)
	f = f.openAdd()
	f.input.SetValue("blocker")
	f = f.commitInput()

	dels, renames, adds := f.diff()
	if len(dels) != 1 || dels[0] != 2 {
		t.Errorf("dels = %v, want [2]", dels)
	}
	if len(renames) != 1 || renames[0].id != 1 || renames[0].name != "lowest" {
		t.Errorf("renames = %+v, want [{1 lowest}]", renames)
	}
	if len(adds) != 1 || adds[0] != "blocker" {
		t.Errorf("adds = %v, want [blocker]", adds)
	}
}

// A case-only rename is not sent (the backend rejects it as a duplicate of itself).
func TestOptionsCaseOnlyRenameSkipped(t *testing.T) {
	f := newOptionsForm(optionsDeps(), 1, 12, "Severity", []domain.FieldOption{{ID: 1, Name: "todo"}})
	f = f.openRename(0)
	f.input.SetValue("TODO")
	f = f.commitInput()
	_, renames, _ := f.diff()
	if len(renames) != 0 {
		t.Errorf("case-only rename produced renames %+v, want none", renames)
	}
}

// A name that collides case-insensitively is rejected in the prompt.
func TestOptionsDuplicateRejected(t *testing.T) {
	f := newOptionsForm(optionsDeps(), 1, 12, "Severity", []domain.FieldOption{{ID: 1, Name: "low"}, {ID: 2, Name: "high"}})
	f = f.openAdd()
	f.input.SetValue("HIGH") // collides with "high"
	f = f.commitInput()
	if !f.inputOpen || f.inputErr == "" {
		t.Error("duplicate add was not rejected")
	}
	if len(f.rows) != 2 {
		t.Errorf("a duplicate row was added (rows=%d)", len(f.rows))
	}
}

// A no-op save closes the editor without a network call.
func TestOptionsNoChangeCloses(t *testing.T) {
	f := newOptionsForm(optionsDeps(), 1, 12, "Severity", []domain.FieldOption{{ID: 1, Name: "low"}})
	f, cmd := f.submit()
	if f.submitting {
		t.Error("a no-op save marked the form submitting")
	}
	if cmd == nil {
		t.Error("a no-op save did not return the cancel command")
	}
}

func TestOptionsSubmitCommits(t *testing.T) {
	f := newOptionsForm(optionsDeps(), 1, 12, "Severity", []domain.FieldOption{{ID: 1, Name: "low"}})
	f = f.openAdd()
	f.input.SetValue("high")
	f = f.commitInput()
	f, cmd := f.submit()
	if !f.submitting || cmd == nil {
		t.Error("a changed option set did not submit")
	}
}

// A successful commit closes the editor and refetches. A failure keeps it open with the message.
func TestOptionsSaveAndFailure(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown())
	m = m.moveTypeElem(keyDown()) // Severity (SELECT_OPTION)
	m, _ = m.Update(keyO())

	m2, _ := m.Update(optionsFailedMsg{message: "dup"})
	if !m2.optionsEditing || m2.options.status != "dup" {
		t.Errorf("failure not surfaced (editing=%v status=%q)", m2.optionsEditing, m2.options.status)
	}

	m3, cmd := m.Update(optionsSavedMsg{typeID: 1})
	if m3.optionsEditing {
		t.Fatal("save did not close the options editor")
	}
	if _, ok := m3.typeDetail[1]; ok || !m3.detailPending[1] || cmd == nil {
		t.Error("options save did not invalidate + refetch")
	}
}

// esc closes the add/rename prompt first, then the modal.
func TestOptionsEscNesting(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown())
	m = m.moveTypeElem(keyDown())
	m, _ = m.Update(keyO())
	m.options = m.options.openAdd()
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if !m.optionsEditing {
		t.Fatal("esc closed the whole modal while the prompt was open")
	}
	if m.options.inputOpen {
		t.Error("esc did not close the add prompt")
	}
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if m.optionsEditing {
		t.Error("second esc did not close the modal")
	}
}
