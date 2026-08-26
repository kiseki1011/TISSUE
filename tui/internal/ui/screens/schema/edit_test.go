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

// mkWorkflowModel loads one workflow and focuses the Details pane.
func mkWorkflowModel(t *testing.T) Model {
	t.Helper()
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	m := New(d)
	m, _ = m.Update(tea.WindowSizeMsg{Width: 160, Height: 30}) // wide enough for the example graph in a 40%-width Details
	m, _ = m.Update(LoadedMsg{Workflows: []domain.WorkflowSummary{{ID: 1, Name: "Development"}}})
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(WorkflowDetailLoadedMsg{ID: 1, Detail: exampleWorkflow()})
	m, _ = m.setFocus(paneDetail)
	return m
}

func selectElem(m Model, e wfElem) Model {
	for i, el := range m.workflowElems() {
		if el == e {
			m.wfSel = i
		}
	}
	return m
}

func pressE() tea.KeyPressMsg   { return tea.KeyPressMsg{Code: 'e', Text: "e"} }
func pressEsc() tea.KeyPressMsg { return tea.KeyPressMsg{Code: tea.KeyEscape} }

func TestOpenEditStateSeedsForm(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemState, 3}) // In Review, INDIGO
	m, _ = m.Update(pressE())
	if !m.editing || !m.CapturingInput() {
		t.Fatalf("editor did not open (editing=%v capturing=%v)", m.editing, m.CapturingInput())
	}
	if m.edit.kind != editState {
		t.Fatalf("kind = %v, want editState", m.edit.kind)
	}
	if got := m.edit.name.Value(); got != "In Review" {
		t.Errorf("name seeded %q, want %q", got, "In Review")
	}
	if m.edit.colors == nil {
		t.Fatal("state editor has no color picker")
	}
	if got := m.edit.colors[m.edit.colorIx]; got != "INDIGO" {
		t.Errorf("color picker at %q, want the state's INDIGO", got)
	}
}

func TestOpenEditTransitionHasNoColor(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemTransition, 13}) // Reject
	m, _ = m.Update(pressE())
	if !m.editing || m.edit.kind != editTransition {
		t.Fatalf("transition editor did not open (editing=%v kind=%v)", m.editing, m.edit.kind)
	}
	if m.edit.colors != nil {
		t.Error("transition editor should have no color picker")
	}
	if got := m.edit.name.Value(); got != "Reject" {
		t.Errorf("name seeded %q, want %q", got, "Reject")
	}
}

func TestEditCancelClosesModal(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemState, 1})
	m, _ = m.Update(pressE())
	m, _ = m.Update(pressEsc())
	if m.editing {
		t.Fatal("esc did not close the editor")
	}

	m, _ = m.Update(pressE())
	m, _ = m.Update(editCancelledMsg{})
	if m.editing {
		t.Fatal("cancel message did not close the editor")
	}
}

// Save drops the cached graph and refetches so the diagram shows the change.
func TestEditSaveInvalidatesAndRefetches(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemState, 1})
	m, _ = m.Update(pressE())
	m, cmd := m.Update(editSavedMsg{wfID: 1})
	if m.editing {
		t.Fatal("save did not close the editor")
	}
	if _, ok := m.wfDetail[1]; ok {
		t.Error("cached graph was not invalidated after save")
	}
	if !m.wfPending[1] {
		t.Error("workflow was not marked pending for refetch")
	}
	if cmd == nil {
		t.Error("no refetch command was issued")
	}
}

func TestColorGridPicksSwatch(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	f := newEditForm(d, editState, 1, 3, "Edit State", "In Review", "ANSI_BLACK", "", true)

	f, _ = f.focusOn(efColor)
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if !f.picking {
		t.Fatal("enter on the color field did not open the swatch grid")
	}
	if got, _ := f.cpick.Selected(); got != "ANSI_BLACK" {
		t.Errorf("grid opened at %q, want the current ANSI_BLACK", got)
	}

	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyRight})
	want, _ := f.cpick.Selected()
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if f.picking {
		t.Fatal("enter did not close the grid")
	}
	if got := f.colors[f.colorIx]; got != want {
		t.Errorf("committed color = %q, want the highlighted %q", got, want)
	}
}

func TestColorGridEscKeepsColor(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	f := newEditForm(d, editState, 1, 3, "Edit State", "In Review", "ANSI_BLACK", "", true)
	f, _ = f.focusOn(efColor)
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyRight})
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if f.picking {
		t.Fatal("esc did not close the grid")
	}
	if got := f.colors[f.colorIx]; got != "ANSI_BLACK" {
		t.Errorf("esc changed the color to %q, want it unchanged at ANSI_BLACK", got)
	}
}

func TestEditRequiresName(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	f := newEditForm(d, editTransition, 1, 13, "Edit Transition", "Reject", "", "", false)
	f.name.SetValue("   ")
	f, _ = f.submit()
	if f.submitting {
		t.Error("submitted despite an empty name (a save was issued)")
	}
	if f.nameErr == "" {
		t.Error("no validation error for the empty name")
	}
	if f.focus != efName {
		t.Error("invalid submit did not focus the name field for correction")
	}
}
