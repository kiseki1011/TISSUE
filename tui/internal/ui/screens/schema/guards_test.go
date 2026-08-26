package schema

import (
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func keyRune(r rune) tea.KeyPressMsg { return tea.KeyPressMsg{Code: r, Text: string(r)} }
func keyEnter() tea.KeyPressMsg      { return tea.KeyPressMsg{Code: tea.KeyEnter} }

func guardsDeps() deps.Deps {
	return deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
}

// g on a transition opens the guard editor seeded with its guards. On a state it does nothing.
func TestOpenGuardsOnlyForTransition(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemTransition, 12}) // Approve, has APPROVAL_REQUIRED min 2
	m, _ = m.Update(keyRune('g'))
	if !m.guardsEditing || !m.CapturingInput() {
		t.Fatalf("guard editor did not open (editing=%v capturing=%v)", m.guardsEditing, m.CapturingInput())
	}
	if len(m.guards.rows) != 1 || m.guards.rows[0].gtype != guardApproval {
		t.Fatalf("rows not seeded from the transition: %+v", m.guards.rows)
	}
	if got := m.guards.rows[0].minApprovals(); got != 2 {
		t.Errorf("min_approvals seeded %d, want 2", got)
	}

	ms := selectElem(mkWorkflowModel(t), wfElem{elemState, 1})
	ms, _ = ms.Update(keyRune('g'))
	if ms.guardsEditing {
		t.Error("guard editor opened for a state")
	}
}

// The Add dropdown offers only unused types, and refuses to open once all five exist.
func TestGuardsAddPickerUniqueTypes(t *testing.T) {
	f := newGuardsForm(guardsDeps(), 1, 12, "Approve", []domain.WorkflowGuard{
		{Type: guardApproval, Params: map[string]any{"min_approvals": 2.0}},
	})
	seen := map[string]bool{guardApproval: true}
	for i := 0; i < len(guardTypes)-1; i++ {
		f = f.openAddPicker()
		if !f.pickOpen {
			t.Fatalf("add dropdown did not open at step %d", i)
		}
		for _, o := range f.pick.options {
			if seen[o.value] {
				t.Fatalf("dropdown offered an already-used type %q", o.value)
			}
		}
		f = f.applyPick() // adds the highlighted (first available) type
		seen[f.rows[len(f.rows)-1].gtype] = true
	}
	if len(f.rows) != len(guardTypes) {
		t.Fatalf("have %d rows, want all %d types", len(f.rows), len(guardTypes))
	}
	f = f.openAddPicker() // all types used
	if f.pickOpen || f.status == "" {
		t.Errorf("opening Add past the full set opened the dropdown or gave no message (open=%v status=%q)", f.pickOpen, f.status)
	}
}

// Retyping a row keeps its own type selectable and applies the chosen one.
func TestGuardsRetypeViaPicker(t *testing.T) {
	f := newGuardsForm(guardsDeps(), 1, 12, "Approve", []domain.WorkflowGuard{
		{Type: "ASSIGNEE_REQUIRED"},
		{Type: guardApproval, Params: map[string]any{"min_approvals": 2.0}},
	})
	f.focus = 0
	f, _ = f.onKey(keyEnter())
	if !f.pickOpen {
		t.Fatal("enter on a guard row did not open the type dropdown")
	}
	offered := map[string]bool{}
	for _, o := range f.pick.options {
		offered[o.value] = true
	}
	if offered[guardApproval] {
		t.Error("dropdown offered a type already used by another row")
	}
	if !offered["ASSIGNEE_REQUIRED"] {
		t.Error("dropdown dropped the row's own current type")
	}
	f.pick = f.pick.move(1) // pick a different type
	want, _ := f.pick.selected()
	f = f.applyPick()
	if f.rows[0].gtype != want.value {
		t.Errorf("row type = %q, want the picked %q", f.rows[0].gtype, want.value)
	}
}

// The approval count is edited with ←/→ and never drops below 1.
func TestGuardsApprovalCount(t *testing.T) {
	f := newGuardsForm(guardsDeps(), 1, 12, "Approve", []domain.WorkflowGuard{
		{Type: guardApproval, Params: map[string]any{"min_approvals": 2.0}},
	})
	f.focus = 0
	f, _ = f.onKey(tea.KeyPressMsg{Code: tea.KeyRight})
	if got := f.rows[0].minApprovals(); got != 3 {
		t.Fatalf("after →, min = %d, want 3", got)
	}
	f, _ = f.onKey(tea.KeyPressMsg{Code: tea.KeyLeft})
	f, _ = f.onKey(tea.KeyPressMsg{Code: tea.KeyLeft})
	f, _ = f.onKey(tea.KeyPressMsg{Code: tea.KeyLeft})
	if got := f.rows[0].minApprovals(); got != 1 {
		t.Fatalf("min = %d, want it clamped at 1", got)
	}
}

// Editing a guard must not mutate the cached graph's parameter maps.
func TestGuardsDoNotMutateCache(t *testing.T) {
	orig := map[string]any{"min_approvals": 2.0, "block_on_change_request": true}
	f := newGuardsForm(guardsDeps(), 1, 12, "Approve", []domain.WorkflowGuard{{Type: guardApproval, Params: orig}})
	f.focus = 0
	f, _ = f.onKey(tea.KeyPressMsg{Code: tea.KeyRight}) // 2 -> 3 on the form's clone
	if orig["min_approvals"] != 2.0 {
		t.Errorf("editing changed the source params map: %v", orig["min_approvals"])
	}
}

// Saving an empty guard list is refused before any network call (the endpoint needs ≥1).
func TestGuardsSubmitEmptyRefused(t *testing.T) {
	f := newGuardsForm(guardsDeps(), 1, 12, "Approve", nil)
	f, cmd := f.submit()
	if f.submitting || cmd != nil {
		t.Error("empty guard list was submitted")
	}
	if f.status == "" {
		t.Error("no message explaining the empty-list refusal")
	}
}

func TestGuardsSubmitIssuesSave(t *testing.T) {
	f := newGuardsForm(guardsDeps(), 1, 12, "Approve", []domain.WorkflowGuard{{Type: "ASSIGNEE_REQUIRED"}})
	f, cmd := f.submit()
	if !f.submitting || cmd == nil {
		t.Fatalf("save did not start (submitting=%v cmd=%v)", f.submitting, cmd == nil)
	}
}

// A successful save closes the editor, drops the cached graph, and refetches it.
func TestGuardsSaveInvalidatesAndRefetches(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemTransition, 12})
	m, _ = m.Update(keyRune('g'))
	m, cmd := m.Update(guardsSavedMsg{wfID: 1})
	if m.guardsEditing {
		t.Fatal("save did not close the guard editor")
	}
	if _, ok := m.wfDetail[1]; ok {
		t.Error("cached graph not invalidated")
	}
	if !m.wfPending[1] || cmd == nil {
		t.Error("no refetch issued after guard save")
	}
}

// Esc and cancel both close the guard editor.
func TestGuardsCancelCloses(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemTransition, 12})
	m, _ = m.Update(keyRune('g'))
	m, _ = m.Update(pressEsc())
	if m.guardsEditing {
		t.Fatal("esc did not close the guard editor")
	}
	m, _ = m.Update(keyRune('g'))
	m, _ = m.Update(guardsCancelledMsg{})
	if m.guardsEditing {
		t.Fatal("cancel did not close the guard editor")
	}
}
