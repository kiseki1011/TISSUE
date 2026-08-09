package domain

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

func cf(label, ftype string, value interface{}, opts ...client.FieldOptionDetail) client.CustomFieldValueInfo {
	ft := client.CustomFieldValueInfoIssueFieldType(ftype)
	c := client.CustomFieldValueInfo{FieldLabel: ptr(label), IssueFieldType: &ft, Value: value}
	if len(opts) > 0 {
		c.Options = &opts
	}
	return c
}

// Scalar types map to the display Text: strings pass through, INTEGER stringifies (JSON numbers arrive
// as float64), DECIMAL/DATE are already display-ready strings from the backend.
func TestCustomFieldScalars(t *testing.T) {
	cases := []struct {
		ftype string
		value interface{}
		want  string
	}{
		{"SHORT_TEXT", "hello", "hello"},
		{"TEXT", "# body", "# body"},
		{"INTEGER", float64(42), "42"},
		{"DECIMAL", "3.14", "3.14"},
		{"DATE", "2026-08-14", "2026-08-14"},
	}
	for _, c := range cases {
		if got := toCustomField(cf("F", c.ftype, c.value)); got.Text != c.want {
			t.Errorf("%s: Text=%q, want %q", c.ftype, got.Text, c.want)
		}
	}
}

func TestCustomFieldBoolean(t *testing.T) {
	if got := toCustomField(cf("Done", "BOOLEAN", true)); got.Bool == nil || !*got.Bool {
		t.Errorf("true boolean wrong: %+v", got)
	}
	if got := toCustomField(cf("Done", "BOOLEAN", false)); got.Bool == nil || *got.Bool {
		t.Errorf("false boolean wrong: %+v", got)
	}
	if got := toCustomField(cf("Done", "BOOLEAN", nil)); got.Bool != nil {
		t.Errorf("unset boolean should be nil, got %+v", got.Bool)
	}
}

func TestCustomFieldPercentage(t *testing.T) {
	if got := toCustomField(cf("Prog", "PERCENTAGE", float64(75))); got.Percent == nil || *got.Percent != 75 {
		t.Errorf("percentage wrong: %+v", got)
	}
	if got := toCustomField(cf("Prog", "PERCENTAGE", nil)); got.Percent != nil {
		t.Errorf("unset percentage should be nil, got %+v", got.Percent)
	}
}

func TestCustomFieldSelectOption(t *testing.T) {
	opts := []client.FieldOptionDetail{{Id: ptr(int64(5)), Name: ptr("High")}, {Id: ptr(int64(6)), Name: ptr("Low")}}
	if got := toCustomField(cf("Sev", "SELECT_OPTION", float64(6), opts...)); got.Text != "Low" {
		t.Errorf("select option should resolve to name, got %q", got.Text)
	}
	if got := toCustomField(cf("Sev", "SELECT_OPTION", float64(99), opts...)); got.Text != "#99" {
		t.Errorf("unknown option should fall back to #id, got %q", got.Text)
	}
}

func TestCustomFieldChecklist(t *testing.T) {
	opts := []client.FieldOptionDetail{
		{Id: ptr(int64(1)), Name: ptr("A")}, {Id: ptr(int64(2)), Name: ptr("B")}, {Id: ptr(int64(3)), Name: ptr("C")},
	}
	got := toCustomField(cf("Tasks", "CHECKLIST", map[string]interface{}{"1": true, "3": false}, opts...))
	if len(got.Items) != 3 {
		t.Fatalf("expected 3 items in option order, got %d: %+v", len(got.Items), got.Items)
	}
	if got.Items[0].Name != "A" || !got.Items[0].Checked {
		t.Errorf("item A should be checked: %+v", got.Items[0])
	}
	if got.Items[1].Name != "B" || got.Items[1].Checked { // absent from the map -> unchecked
		t.Errorf("item B (absent) should be unchecked: %+v", got.Items[1])
	}
	if got.Items[2].Checked { // explicitly false
		t.Errorf("item C should be unchecked: %+v", got.Items[2])
	}
	// an unset checklist (value not a map) maps to nil so the UI renders "-"
	if u := toCustomField(cf("Tasks", "CHECKLIST", nil, opts...)); u.Items != nil {
		t.Errorf("unset checklist should be nil, got %+v", u.Items)
	}
}

func TestCustomFieldTimestamp(t *testing.T) {
	got := toCustomField(cf("At", "TIMESTAMP", "2026-08-14T10:30:00Z"))
	if len(got.Text) != 16 || !strings.Contains(got.Text, ":") || !strings.Contains(got.Text, "-") {
		t.Errorf("timestamp should format to YYYY-MM-DD HH:MM, got %q", got.Text)
	}
	// an unparseable value falls back to the raw string rather than dropping it
	if raw := toCustomField(cf("At", "TIMESTAMP", "not-a-time")); raw.Text != "not-a-time" {
		t.Errorf("unparseable timestamp should pass through, got %q", raw.Text)
	}
}

// An unset scalar leaves every carrier zero, so the UI renders "-".
func TestCustomFieldUnset(t *testing.T) {
	got := toCustomField(cf("F", "SHORT_TEXT", nil))
	if got.Text != "" || got.Bool != nil || got.Percent != nil || got.Items != nil {
		t.Errorf("unset field should be all-zero, got %+v", got)
	}
}
