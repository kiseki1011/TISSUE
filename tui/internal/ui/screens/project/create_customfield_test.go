package project

import (
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func customTypeFields() []domain.IssueField {
	return []domain.IssueField{
		{ID: 10, Name: "Severity", Type: "SELECT_OPTION", Required: true, Position: 1, Options: []domain.FieldOption{{ID: 1, Name: "Low"}, {ID: 2, Name: "High"}}},
		{ID: 11, Name: "Estimate", Type: "INTEGER", Position: 2},
		{ID: 12, Name: "Notes", Type: "SHORT_TEXT", Position: 3},
	}
}

// customReady opens the create form with a type whose custom fields are already cached, so they load in.
func customReady(t *testing.T) Model {
	t.Helper()
	m := createReady(t) // types = [{ID:5}]
	m.typeFields = map[int64][]domain.IssueField{5: customTypeFields()}
	m, _ = m.Update(press("n"))
	return m
}

// Opening the form loads the selected type's custom fields from the cache.
func TestCustomFieldsLoadIntoForm(t *testing.T) {
	m := customReady(t)
	if len(m.createUI.customFields) != 3 {
		t.Fatalf("expected 3 custom fields loaded, got %d", len(m.createUI.customFields))
	}
	if m.createUI.customFields[0].field.Name != "Severity" {
		t.Errorf("custom fields should be in position order, got %q first", m.createUI.customFields[0].field.Name)
	}
}

// A field load lands on the form when the gen matches and the type is unchanged; a stale gen is dropped.
func TestTypeFieldsLoadedSetsFormAndDropsStale(t *testing.T) {
	m := createReady(t)
	m, _ = m.Update(press("n")) // no cache -> fires a load, bumps typeFieldsGen, form has no custom fields yet
	m, _ = m.Update(typeFieldsLoadedMsg{gen: m.typeFieldsGen, typeID: 5, fields: customTypeFields()})
	if len(m.createUI.customFields) != 3 {
		t.Fatalf("a matching load should populate the form, got %d", len(m.createUI.customFields))
	}
	before := len(m.createUI.customFields)
	m, _ = m.Update(typeFieldsLoadedMsg{gen: m.typeFieldsGen - 1, typeID: 5, fields: customTypeFields()[:1]})
	if len(m.createUI.customFields) != before {
		t.Error("a stale-gen field load must not replace the form's fields")
	}
}

// Submitting collects each set custom field into the payload, keyed by field id.
func TestSubmitCollectsCustomFields(t *testing.T) {
	f := newCreateForm(testDeps(), []domain.IssueTypeSummary{{ID: 5, Name: "Story"}})
	f = f.withCustomFields(buildCustomInputs(customTypeFields()))
	f.title.SetValue("thing")
	f.customFields[0].ix = 2                // Severity -> option id 2
	f.customFields[2].text.SetValue("note") // Notes
	f, _ = f.focusOn(nfCreate)
	_, cmd := f.onKey(press("enter"))
	sub, ok := cmd().(createSubmittedMsg)
	if !ok {
		t.Fatalf("expected a submit, got %T", cmd())
	}
	if sub.v.customFields["10"] != int64(2) {
		t.Errorf("Severity not collected: %#v", sub.v.customFields)
	}
	if sub.v.customFields["12"] != "note" {
		t.Errorf("Notes not collected: %#v", sub.v.customFields)
	}
	if _, present := sub.v.customFields["11"]; present {
		t.Errorf("an unset optional field should be omitted, got %#v", sub.v.customFields)
	}
}

// A required custom field left empty blocks submit and is flagged + focused.
func TestSubmitBlocksRequiredCustomField(t *testing.T) {
	f := newCreateForm(testDeps(), []domain.IssueTypeSummary{{ID: 5, Name: "Story"}})
	f = f.withCustomFields(buildCustomInputs(customTypeFields()))
	f.title.SetValue("thing") // Severity (required) left unset
	f, _ = f.focusOn(nfCreate)
	f, cmd := f.onKey(press("enter"))
	if f.customFields[0].err == "" {
		t.Error("a missing required custom field should be flagged")
	}
	if f.focus != nfCustomBase+0 {
		t.Errorf("submit should focus the offending field, got focus=%d", f.focus)
	}
	if cmd != nil {
		if _, ok := cmd().(createSubmittedMsg); ok {
			t.Error("a missing required custom field must block submit")
		}
	}
}

// Submitting while a type's fields are still loading is blocked (so required custom fields are never
// silently skipped), and no create command fires.
func TestSubmitBlockedWhileFieldsLoading(t *testing.T) {
	m := createReady(t) // types = [{ID:5}], no field cache -> opening fires a load
	m, _ = m.Update(press("n"))
	if !m.createUI.customLoading {
		t.Fatal("opening with an uncached type should mark the fields as loading")
	}
	m.createUI.title.SetValue("thing")
	m.createUI, _ = m.createUI.focusOn(nfCreate)
	_, cmd := m.createUI.onKey(press("enter"))
	if cmd != nil {
		if _, ok := cmd().(createSubmittedMsg); ok {
			t.Error("submit must be blocked while the type's fields are loading")
		}
	}
}

// A TEXT custom field is a multi-line area: enter inserts a newline (it does not advance focus), while a
// single-line custom field still advances on enter.
func TestCustomTextFieldEnterDoesNotAdvance(t *testing.T) {
	area := newCreateForm(testDeps(), []domain.IssueTypeSummary{{ID: 5, Name: "Story"}})
	area = area.withCustomFields(buildCustomInputs([]domain.IssueField{{ID: 20, Name: "Notes", Type: "TEXT", Position: 1}}))
	area, _ = area.focusOn(nfCustomBase)
	if !area.focusIsArea() {
		t.Fatal("a TEXT custom field should be an area focus")
	}
	if area, _ = area.onKey(press("enter")); area.focus != nfCustomBase {
		t.Errorf("enter in a TEXT area must not advance focus, got focus=%d", area.focus)
	}

	line := newCreateForm(testDeps(), []domain.IssueTypeSummary{{ID: 5, Name: "Story"}})
	line = line.withCustomFields(buildCustomInputs([]domain.IssueField{{ID: 21, Name: "Tag", Type: "SHORT_TEXT", Position: 1}}))
	line, _ = line.focusOn(nfCustomBase)
	if line, _ = line.onKey(press("enter")); line.focus == nfCustomBase {
		t.Error("enter in a single-line custom field should advance focus")
	}
}

// A TEXT area's value preserves newlines through to the payload.
func TestCustomTextAreaValueMultiline(t *testing.T) {
	c := newCustomFieldInput(domain.IssueField{ID: 20, Name: "Notes", Type: "TEXT"})
	c.area.SetValue("line1\nline2")
	v, present, errMsg := c.value()
	if !present || errMsg != "" {
		t.Fatalf("a filled TEXT area should be present without error, got present=%v err=%q", present, errMsg)
	}
	if v != "line1\nline2" {
		t.Errorf("TEXT value should preserve newlines, got %q", v)
	}
}

// Changing the type clears the old fields and requests the new type's fields.
func TestCycleTypeReloadsFields(t *testing.T) {
	f := newCreateForm(testDeps(), []domain.IssueTypeSummary{{ID: 5, Name: "Story"}, {ID: 6, Name: "Bug"}})
	f = f.withCustomFields(buildCustomInputs(customTypeFields()))
	f, _ = f.focusOn(nfType)
	f, cmd := f.onKey(press("right"))
	if len(f.customFields) != 0 {
		t.Error("cycling the type should clear the old type's custom fields until the new load")
	}
	if cmd == nil {
		t.Fatal("cycling the type should request the new type's fields")
	}
	if msg, ok := cmd().(createTypeFieldsMsg); !ok || msg.typeID != 6 {
		t.Errorf("expected a field request for type 6, got %#v", cmd())
	}
}
