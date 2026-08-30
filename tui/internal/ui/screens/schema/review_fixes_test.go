package schema

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// Temp names must be distinct and clear of every option name, or the two-phase rename collides.
func TestTempRenameNamesAvoidCollisions(t *testing.T) {
	avoid := map[string]bool{"tmp0": true, "tmp1": true, "low": true, "high": true}
	names := tempRenameNames(3, avoid)
	if len(names) != 3 {
		t.Fatalf("got %d temp names, want 3", len(names))
	}
	seen := map[string]bool{}
	for _, n := range names {
		if avoid[strings.ToLower(n)] {
			t.Errorf("temp name %q collides with the avoid set", n)
		}
		if seen[n] {
			t.Errorf("temp name %q is not unique", n)
		}
		seen[n] = true
	}
}

// Renaming one option into a name another still holds is allowed: the two-phase rename resolves
// the intermediate collision server-side.
func TestOptionsRenameChainSubmits(t *testing.T) {
	f := newOptionsForm(optionsDeps(), 1, 12, "Sev", []domain.FieldOption{{ID: 1, Name: "A"}, {ID: 2, Name: "B"}})
	f = f.openRename(1) // B
	f.input.SetValue("B2")
	f = f.commitInput()
	f = f.openRename(0) // A -> B (now unique, since row 1 is B2)
	f.input.SetValue("B")
	f = f.commitInput()
	if f.inputErr != "" {
		t.Fatalf("the rename chain was blocked in the editor: %q", f.inputErr)
	}
	_, renames, _ := f.diff()
	if len(renames) != 2 {
		t.Fatalf("diff renames = %d, want 2", len(renames))
	}
	f, cmd := f.submit()
	if !f.submitting || cmd == nil {
		t.Error("the rename chain did not submit")
	}
}

// A partial-commit failure refetches to reseed the baseline, so applied changes are not re-issued.
func TestOptionsFailureRefetchesAndReseeds(t *testing.T) {
	m := typeFieldsModel(t)
	m = m.moveTypeElem(keyDown())
	m = m.moveTypeElem(keyDown()) // Severity (SELECT_OPTION), field 12
	m, _ = m.Update(keyO())
	m, cmd := m.Update(optionsFailedMsg{message: "in use"})
	if !m.optionsEditing || m.options.status != "in use" {
		t.Fatalf("failure not surfaced (open=%v status=%q)", m.optionsEditing, m.options.status)
	}
	if _, ok := m.typeDetail[1]; ok || !m.detailPending[1] || cmd == nil {
		t.Error("failure did not invalidate + refetch to reseed")
	}
	// the refetch reseeds the baseline and preserves the error
	m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
		ID: 1, Fields: []domain.IssueField{{
			ID: 12, Name: "Severity", Type: "SELECT_OPTION",
			Options: []domain.FieldOption{{ID: 3, Name: "only"}},
		}},
	}})
	if !m.optionsEditing {
		t.Fatal("reseed closed the editor unexpectedly")
	}
	if len(m.options.orig) != 1 || m.options.orig[0].ID != 3 {
		t.Errorf("baseline not reseeded from server: %+v", m.options.orig)
	}
	if m.options.status != "in use" {
		t.Errorf("reseed dropped the error message, status=%q", m.options.status)
	}
}

// A 20-character Korean name is accepted: the bound is characters, not bytes.
func TestTypeCreateAcceptsLongKoreanName(t *testing.T) {
	wfs := []domain.WorkflowSummary{{ID: 1, Name: "Flow"}}
	f := newCreateTypeForm(optionsDeps(), wfs)
	f.name.SetValue(strings.Repeat("가", 20)) // 20 chars, 60 bytes
	f, cmd := f.submit()
	if f.nameErr != "" {
		t.Errorf("a 20-character Korean name was rejected: %q", f.nameErr)
	}
	if !f.submitting || cmd == nil {
		t.Error("a valid Korean-named type did not submit")
	}
}

// Workflow name length is counted in characters, and an over-long state name gets its own message.
func TestWorkflowCreateNameRuleUnicode(t *testing.T) {
	f := newCreateWorkflowForm(optionsDeps())
	f.name.SetValue(strings.Repeat("가", 20)) // 20 chars, 60 bytes, within 32
	f = f.applyEdge("Go", "n1", "n2")        // wire the seed so only the name is under test
	if msg := f.validate(); msg != "" {
		t.Errorf("a 20-character Korean workflow name was rejected: %q", msg)
	}
	f2 := newCreateWorkflowForm(optionsDeps())
	f2.name.SetValue("Valid")
	f2.states[0].name = strings.Repeat("x", 40)
	if msg := f2.validate(); !strings.Contains(msg, "State names") {
		t.Errorf("over-long state name not caught with a state message: %q", msg)
	}
}
