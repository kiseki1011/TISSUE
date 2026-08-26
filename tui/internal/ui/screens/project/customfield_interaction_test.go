package project

import (
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func checklistField() domain.IssueField {
	return domain.IssueField{
		ID: 30, Name: "Tags", Type: "CHECKLIST", Position: 1,
		Options: []domain.FieldOption{{ID: 7, Name: "alpha"}, {ID: 8, Name: "beta"}},
	}
}

// realSpace is what a terminal actually delivers for the spacebar. tea.Key.String() never returns " ",
// so a handler matching " " is dead code that a hand-rolled msg can hide.
func realSpace() tea.KeyPressMsg { return tea.KeyPressMsg{Code: tea.KeySpace, Text: " "} }

// Regression: the CHECKLIST toggle was bound to " ", which the key never reports, so it never worked.
func TestChecklistTogglesWithSpacebar(t *testing.T) {
	if got := realSpace().String(); got != "space" {
		t.Fatalf("the spacebar reports %q - the handlers below are keyed on that", got)
	}
	c := newCustomFieldInput(checklistField())

	c, consumed := c.handleKey(realSpace())
	if !consumed {
		t.Fatal("the checklist should consume the spacebar")
	}
	if !c.checked[7] {
		t.Error("spacebar should check the option under the cursor")
	}
	if c, _ = c.handleKey(realSpace()); c.checked[7] {
		t.Error("a second spacebar should uncheck it")
	}
}

func TestChecklistCursorThenToggle(t *testing.T) {
	c := newCustomFieldInput(checklistField())
	c, _ = c.handleKey(press("right"))
	c, _ = c.handleKey(realSpace())
	if c.checked[7] {
		t.Error("the first option should be untouched")
	}
	if !c.checked[8] {
		t.Error("the option under the moved cursor should be checked")
	}
}

func TestCycleAdvancesWithSpacebar(t *testing.T) {
	c := newCustomFieldInput(domain.IssueField{ID: 31, Name: "Flag", Type: "BOOLEAN"})
	c, consumed := c.handleKey(realSpace())
	if !consumed || c.ix != 1 {
		t.Errorf("spacebar should step the cycle, got consumed=%v ix=%d", consumed, c.ix)
	}
}

// checklistModel opens the create form on a type whose only custom field is a checklist.
func checklistModel(t *testing.T) Model {
	t.Helper()
	m := createReady(t)
	m.typeFields = map[int64][]domain.IssueField{5: {checklistField()}}
	m, _ = m.Update(press("n"))
	if len(m.createUI.customFields) != 1 {
		t.Fatalf("expected the checklist field to load, got %d", len(m.createUI.customFields))
	}
	return m
}

// Regression: a click anywhere in the field only moved focus, so a checklist was unfillable by mouse.
func TestChecklistOptionClickToggles(t *testing.T) {
	m := checklistModel(t)
	click := clickZone(t, m, "project.create.custom.0.opt.1")
	m, _ = m.Update(click)

	c := m.createUI.customFields[0]
	if !c.checked[8] {
		t.Error("clicking the second option should check it")
	}
	if c.checked[7] {
		t.Error("clicking the second option must not touch the first")
	}
	if c.cursor != 1 {
		t.Errorf("the keyboard cursor should follow the click, got %d", c.cursor)
	}

	m, _ = m.Update(clickZone(t, m, "project.create.custom.0.opt.1"))
	if m.createUI.customFields[0].checked[8] {
		t.Error("clicking a checked option should uncheck it")
	}
}

func TestCycleArrowClickSteps(t *testing.T) {
	m := createReady(t)
	m.typeFields = map[int64][]domain.IssueField{5: {{
		ID: 32, Name: "Severity", Type: "SELECT_OPTION", Position: 1,
		Options: []domain.FieldOption{{ID: 1, Name: "Low"}, {ID: 2, Name: "High"}},
	}}}
	m, _ = m.Update(press("n"))

	m, _ = m.Update(clickZone(t, m, "project.create.custom.0.next"))
	if got := m.createUI.customFields[0].ix; got != 1 {
		t.Errorf("clicking › should step forward, got ix=%d", got)
	}
	m, _ = m.Update(clickZone(t, m, "project.create.custom.0.prev"))
	if got := m.createUI.customFields[0].ix; got != 0 {
		t.Errorf("clicking ‹ should step back, got ix=%d", got)
	}
}
