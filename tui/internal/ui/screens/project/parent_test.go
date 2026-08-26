package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func hierTypes() []domain.IssueTypeSummary {
	return []domain.IssueTypeSummary{
		{ID: 1, Name: "Epic", Hierarchy: "EPIC"},
		{ID: 2, Name: "Story", Hierarchy: "STANDARD"},
		{ID: 3, Name: "Subtask", Hierarchy: "SUBTASK"},
	}
}

func TestParentHierarchyHelpers(t *testing.T) {
	if p, ok := parentHierarchy("SUBTASK"); !ok || p != "STANDARD" {
		t.Errorf("SUBTASK parent should be STANDARD, got %q/%v", p, ok)
	}
	if p, ok := parentHierarchy("STANDARD"); !ok || p != "EPIC" {
		t.Errorf("STANDARD parent should be EPIC, got %q/%v", p, ok)
	}
	if _, ok := parentHierarchy("EPIC"); ok {
		t.Error("EPIC is top-level and should have no parent hierarchy")
	}
	if !parentRequired("SUBTASK") || !parentRequired("MICROTASK") {
		t.Error("SUBTASK/MICROTASK must require a parent")
	}
	if parentRequired("STANDARD") || parentRequired("EPIC") {
		t.Error("STANDARD/EPIC must not require a parent")
	}
}

// A SUBTASK cannot submit without a parent. Setting one unblocks the submit and carries it.
func TestCreateSubtaskRequiresParent(t *testing.T) {
	f := newCreateForm(testDeps(), hierTypes())
	f.typeIx = 2 // Subtask
	f.title.SetValue("child")
	f, _ = f.focusOn(nfCreate)
	f, cmd := f.onKey(press("enter"))
	if f.parentErr == "" {
		t.Fatal("a subtask without a parent should be blocked with an error")
	}
	if cmd != nil {
		if _, ok := cmd().(createSubmittedMsg); ok {
			t.Fatal("a subtask without a parent must not submit")
		}
	}
	f = f.withParent("ENG-2", "ENG-2 A story")
	f, _ = f.focusOn(nfCreate)
	_, cmd = f.onKey(press("enter"))
	sub, ok := cmd().(createSubmittedMsg)
	if !ok {
		t.Fatalf("with a parent the subtask should submit, got %T", cmd())
	}
	if sub.v.parentKey != "ENG-2" {
		t.Errorf("submitted parentKey = %q, want ENG-2", sub.v.parentKey)
	}
}

// Changing the type clears any picked parent, since the eligible parent level changes.
func TestCreateCycleTypeClearsParent(t *testing.T) {
	f := newCreateForm(testDeps(), hierTypes())
	f.typeIx = 2
	f = f.withParent("ENG-2", "ENG-2 A story")
	f = f.cycleType(1)
	if f.parentKey != "" {
		t.Errorf("cycling the type should clear the parent, got %q", f.parentKey)
	}
}

// Enter on the Parent field asks the model to open the picker.
func TestParentFieldEnterOpensPicker(t *testing.T) {
	f := newCreateForm(testDeps(), hierTypes())
	f, _ = f.focusOn(nfParent)
	_, cmd := f.onKey(press("enter"))
	if cmd == nil {
		t.Fatal("enter on Parent should emit a command")
	}
	if _, ok := cmd().(createParentPickMsg); !ok {
		t.Errorf("enter on Parent should emit createParentPickMsg, got %T", cmd())
	}
}

// A top-level (EPIC) type explains itself instead of opening a picker.
func TestOpenParentPickerTopLevel(t *testing.T) {
	m := createReady(t)
	m.types = hierTypes()
	m, _ = m.Update(press("n"))
	m.createUI.typeIx = 0 // Epic
	m, _ = m.openParentPicker()
	if m.picking {
		t.Error("a top-level type should not open the parent picker")
	}
}

// Loaded candidates open the picker. An optional parent gets a leading None, a required one does not.
func TestOnParentCandidatesOpensPicker(t *testing.T) {
	cands := []domain.IssueSummary{{Key: "ENG-2", Title: "A story"}}

	// required (SUBTASK, parent level STANDARD): no None option
	m := createReady(t)
	m.types = hierTypes()
	m, _ = m.Update(press("n"))
	m.createUI.typeIx = 2
	m, _ = m.Update(parentCandidatesLoadedMsg{gen: m.parentGen, hier: "STANDARD", candidates: cands})
	if !m.picking || m.pickKind != pickParent {
		t.Fatal("candidates should open the parent picker")
	}
	if strings.Contains(plain(m.picker.View(m.deps.Styles)), "None") {
		t.Error("a required parent picker should not offer None")
	}

	// optional (STANDARD, parent level EPIC): has None
	m2 := createReady(t)
	m2.types = hierTypes()
	m2, _ = m2.Update(press("n"))
	m2.createUI.typeIx = 1
	m2, _ = m2.Update(parentCandidatesLoadedMsg{gen: m2.parentGen, hier: "EPIC", candidates: cands})
	if !strings.Contains(plain(m2.picker.View(m2.deps.Styles)), "None") {
		t.Error("an optional parent picker should offer None")
	}
}

// Candidates landing after a type change are dropped, so the picker never offers the wrong hierarchy.
func TestParentCandidatesStaleTypeDropped(t *testing.T) {
	m := createReady(t)
	m.types = hierTypes()
	m, _ = m.Update(press("n"))
	m.createUI.typeIx = 2 // Subtask -> parent level STANDARD requested
	// the user cycled to Story (STANDARD, parent level EPIC) before the STANDARD candidates landed
	m.createUI.typeIx = 1
	m, _ = m.Update(parentCandidatesLoadedMsg{gen: m.parentGen, hier: "STANDARD", candidates: []domain.IssueSummary{{Key: "ENG-2"}}})
	if m.picking {
		t.Error("stale candidates for a since-changed type must not open the picker")
	}
}

// A load from a prior form session must not pop a picker over the reopened form.
func TestParentCandidatesStaleGenDropped(t *testing.T) {
	m := createReady(t)
	m.types = hierTypes()
	m, _ = m.Update(press("n"))
	staleGen := m.parentGen // a parent load was issued for this session
	m, _ = m.Update(createCancelledMsg{})
	m, _ = m.Update(press("n")) // reopen: a fresh session bumps parentGen past staleGen
	m.createUI.typeIx = 2       // same parent hierarchy (STANDARD) the stale load was for
	m, _ = m.Update(parentCandidatesLoadedMsg{gen: staleGen, hier: "STANDARD", candidates: []domain.IssueSummary{{Key: "ENG-2"}}})
	if m.picking {
		t.Error("a parent load from a prior form session must not open a picker over the reopened form")
	}
}

// With two quick activations only the latest opens the picker.
func TestParentDoubleLoadOnlyLatestOpens(t *testing.T) {
	m := createReady(t)
	m.types = hierTypes()
	m, _ = m.Update(press("n"))
	m.createUI.typeIx = 2 // Subtask -> parent STANDARD (eligible types exist)
	m, _ = m.openParentPicker()
	gen1 := m.parentGen
	m, _ = m.openParentPicker() // a second activation supersedes the first
	m, _ = m.Update(parentCandidatesLoadedMsg{gen: gen1, hier: "STANDARD", candidates: []domain.IssueSummary{{Key: "ENG-2"}}})
	if m.picking {
		t.Error("an earlier activation's load must not open the picker after a re-activation")
	}
	m, _ = m.Update(parentCandidatesLoadedMsg{gen: m.parentGen, hier: "STANDARD", candidates: []domain.IssueSummary{{Key: "ENG-2"}}})
	if !m.picking {
		t.Error("the latest activation's load should open the picker")
	}
}

// Selecting a candidate records it on the form and closes the picker.
func TestSelectParentSetsForm(t *testing.T) {
	m := createReady(t)
	m.types = hierTypes()
	m, _ = m.Update(press("n"))
	m.createUI.typeIx = 2 // required -> cursor lands on the first candidate
	m, _ = m.Update(parentCandidatesLoadedMsg{gen: m.parentGen, hier: "STANDARD", candidates: []domain.IssueSummary{{Key: "ENG-2", Title: "A story"}}})
	m, _ = m.selectParent()
	if m.picking {
		t.Error("selecting a parent should close the picker")
	}
	if m.createUI.parentKey != "ENG-2" {
		t.Errorf("selecting should set the parent, got %q", m.createUI.parentKey)
	}
}
