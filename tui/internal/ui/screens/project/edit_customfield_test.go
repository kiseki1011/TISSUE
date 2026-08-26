package project

import (
	"reflect"
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// editDetail carries one of each interesting custom field, already valued.
func editDetail() domain.IssueDetail {
	return domain.IssueDetail{
		Key: "PROJ-1", Title: "Wire it up", Priority: "P2",
		CustomFields: []domain.CustomField{
			{ID: 40, Label: "Notes", Type: "SHORT_TEXT", Raw: "before"},
			{ID: 41, Label: "Estimate", Type: "INTEGER", Raw: float64(3)},
			{ID: 42, Label: "Severity", Type: "SELECT_OPTION", Raw: float64(2),
				Options: []domain.FieldOption{{ID: 1, Name: "Low"}, {ID: 2, Name: "High"}}},
			{ID: 43, Label: "Tags", Type: "CHECKLIST", Raw: map[string]interface{}{"7": true},
				Options: []domain.FieldOption{{ID: 7, Name: "alpha"}, {ID: 8, Name: "beta"}}},
		},
	}
}

func editFormWithCustom(t *testing.T) editForm {
	t.Helper()
	f := newEditForm(testDeps(), editDetail(), false)
	if len(f.customFields) != 4 {
		t.Fatalf("expected 4 custom inputs, got %d", len(f.customFields))
	}
	return f
}

// The form must open on the current values, else saving would wipe every field not retyped.
func TestEditFormSeedsCustomValues(t *testing.T) {
	f := editFormWithCustom(t)
	if got := f.customFields[0].text.Value(); got != "before" {
		t.Errorf("SHORT_TEXT not seeded, got %q", got)
	}
	if got := f.customFields[1].text.Value(); got != "3" {
		t.Errorf("INTEGER not seeded, got %q", got)
	}
	if got := f.customFields[2].ix; got != 2 { // option id 2 is the second option -> ix 2
		t.Errorf("SELECT_OPTION not seeded, got ix=%d", got)
	}
	if !f.customFields[3].checked[7] || f.customFields[3].checked[8] {
		t.Errorf("CHECKLIST not seeded, got %v", f.customFields[3].checked)
	}
}

// Re-sending an unchanged value would stamp it as an edit in the activity log.
func TestEditFormUntouchedSendsNoCustomFields(t *testing.T) {
	f := editFormWithCustom(t)
	_, diff, ok := f.diffCustom()
	if !ok {
		t.Fatal("an untouched form should validate")
	}
	if len(diff) != 0 {
		t.Errorf("nothing changed, so nothing should be sent, got %v", diff)
	}
}

func TestEditFormSendsOnlyChangedCustomFields(t *testing.T) {
	f := editFormWithCustom(t)
	f.customFields[0].text.SetValue("after")
	f.customFields[3], _ = f.customFields[3].handleKey(realSpace()) // toggles option 7 off

	_, diff, ok := f.diffCustom()
	if !ok {
		t.Fatal("the form should validate")
	}
	want := map[string]interface{}{
		"40": "after",
		"43": nil, // the only checked option was cleared, so the field is now empty
	}
	if !reflect.DeepEqual(diff, want) {
		t.Errorf("unexpected diff\n got: %#v\nwant: %#v", diff, want)
	}
}

// Clearing a set text field sends an explicit nil, which the server reads as "clear this field".
func TestEditFormClearedFieldSendsNil(t *testing.T) {
	f := editFormWithCustom(t)
	f.customFields[0].text.SetValue("")

	_, diff, ok := f.diffCustom()
	if !ok {
		t.Fatal("clearing an optional field should validate")
	}
	v, present := diff["40"]
	if !present {
		t.Fatal("a cleared field must be sent, not omitted")
	}
	if v != nil {
		t.Errorf("a cleared field should be sent as nil, got %#v", v)
	}
}

func TestEditFormInvalidCustomFieldBlocksSave(t *testing.T) {
	f := editFormWithCustom(t)
	f.customFields[1].text.SetValue("not-a-number")

	f, _, ok := f.diffCustom()
	if ok {
		t.Error("an unparseable INTEGER should block the save")
	}
	if f.customFields[1].err == "" {
		t.Error("the offending field should carry an error message")
	}
}

func TestEditFormRendersCustomFields(t *testing.T) {
	f := editFormWithCustom(t)
	ids := f.fields()
	for i := range f.customFields {
		if indexOfInt(ids, nfCustomBase+i) < 0 {
			t.Errorf("custom field %d is not a tab stop", i)
		}
	}
	if indexOfInt(ids, nfCustomBase) > indexOfInt(ids, efSave) {
		t.Error("custom fields should come before the Save button in tab order")
	}
	out := plain(f.View())
	for _, want := range []string{"Notes", "Estimate", "Severity", "Tags", "alpha", "beta"} {
		if !strings.Contains(out, want) {
			t.Errorf("edit modal missing %q:\n%s", want, out)
		}
	}
}

func TestEditFormChecklistSpacebar(t *testing.T) {
	f := editFormWithCustom(t)
	f, _ = f.focusOn(nfCustomBase + 3)
	f, _ = f.onKey(realSpace())
	if f.customFields[3].checked[7] {
		t.Error("the spacebar should have unchecked the seeded option")
	}
}
