package project

import (
	"reflect"
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func textField(t string, req bool, val string) customFieldInput {
	c := newCustomFieldInput(domain.IssueField{ID: 1, Name: "F", Type: t, Required: req})
	switch {
	case cfIsArea(t): // TEXT is a multi-line textarea
		c.area.SetValue(val)
	case cfIsDate(t): // DATE/TIMESTAMP are set from the calendar, not typed
		if val != "" {
			c = c.setDate(parseTestDate(t, val), true)
		}
	default: // SHORT_TEXT / number types are single-line inputs
		c.text.SetValue(val)
	}
	return c
}

// parseTestDate builds the time.Time a DATE/TIMESTAMP field holds after a calendar pick: always
// UTC-stored (matching the picker), with TIMESTAMP's components being the intended local wall-clock.
func parseTestDate(fieldType, val string) time.Time {
	if fieldType == "TIMESTAMP" {
		tt, _ := time.Parse("2006-01-02 15:04", val) // Parse defaults to UTC: components = the wall-clock
		return tt
	}
	tt, _ := time.Parse("2006-01-02", val)
	return tt
}

// value serializes each field type to the backend's expected JSON shape.
func TestCustomFieldValueSerialization(t *testing.T) {
	cases := []struct {
		name    string
		input   customFieldInput
		want    interface{}
		present bool
		wantErr bool
	}{
		{"short_text", textField("SHORT_TEXT", false, "hi"), "hi", true, false},
		{"text", textField("TEXT", false, "body"), "body", true, false},
		{"integer", textField("INTEGER", false, "42"), 42, true, false},
		{"integer_bad", textField("INTEGER", false, "x"), nil, false, true},
		{"percentage", textField("PERCENTAGE", false, "80"), 80, true, false},
		{"percentage_range", textField("PERCENTAGE", false, "150"), nil, false, true},
		{"decimal_string", textField("DECIMAL", false, "3.14"), "3.14", true, false}, // string keeps precision
		{"decimal_bad", textField("DECIMAL", false, "abc"), nil, false, true},
		{"date", textField("DATE", false, "2026-09-01"), "2026-09-01", true, false},
		{"date_empty_optional", textField("DATE", false, ""), nil, false, false},
		{"date_empty_required", textField("DATE", true, ""), nil, false, true},
		{"empty_optional", textField("SHORT_TEXT", false, ""), nil, false, false},
		{"empty_required", textField("SHORT_TEXT", true, ""), nil, false, true},
	}
	for _, c := range cases {
		val, present, errMsg := c.input.value()
		if present != c.present {
			t.Errorf("%s: present=%v want %v", c.name, present, c.present)
		}
		if (errMsg != "") != c.wantErr {
			t.Errorf("%s: err=%q wantErr=%v", c.name, errMsg, c.wantErr)
		}
		if c.present && !reflect.DeepEqual(val, c.want) {
			t.Errorf("%s: value=%#v want %#v", c.name, val, c.want)
		}
	}
}

// A TIMESTAMP entry is wall-clock in the local zone: the emitted instant, rendered back in local time,
// must match what was typed (timezone-independent, unlike a hard-coded "Z" assertion).
func TestCustomFieldTimestampRoundTrips(t *testing.T) {
	v, present, errMsg := textField("TIMESTAMP", false, "2026-08-15 14:00").value()
	if !present || errMsg != "" {
		t.Fatalf("valid timestamp should be present without error, got present=%v err=%q", present, errMsg)
	}
	inst, err := time.Parse(time.RFC3339, v.(string))
	if err != nil {
		t.Fatalf("emitted value should be an RFC3339 instant, got %q: %v", v, err)
	}
	if got := inst.Local().Format("2006-01-02 15:04"); got != "2026-08-15 14:00" {
		t.Errorf("timestamp did not round-trip through local time: got %q", got)
	}
}

// BOOLEAN cycles unset -> Yes(true) -> No(false).
func TestCustomFieldBoolean(t *testing.T) {
	c := newCustomFieldInput(domain.IssueField{ID: 1, Type: "BOOLEAN"})
	if _, present, _ := c.value(); present {
		t.Error("an unset boolean should not be present")
	}
	c.ix = 1
	if v, present, _ := c.value(); !present || v != true {
		t.Errorf("ix=1 should be true, got %v present=%v", v, present)
	}
	c.ix = 2
	if v, present, _ := c.value(); !present || v != false {
		t.Errorf("ix=2 should be false, got %v present=%v", v, present)
	}
}

// SELECT_OPTION sends the chosen option's id as a number.
func TestCustomFieldSelectOption(t *testing.T) {
	c := newCustomFieldInput(domain.IssueField{ID: 1, Type: "SELECT_OPTION", Options: []domain.FieldOption{{ID: 7, Name: "High"}, {ID: 8, Name: "Low"}}})
	if _, present, _ := c.value(); present {
		t.Error("an unset select should not be present")
	}
	c.ix = 2 // second option (id 8)
	v, present, _ := c.value()
	if !present || v != int64(8) {
		t.Errorf("select should send option id 8, got %#v present=%v", v, present)
	}
}

// CHECKLIST sends {optionIdString: true} for each checked option.
func TestCustomFieldChecklist(t *testing.T) {
	c := newCustomFieldInput(domain.IssueField{ID: 1, Type: "CHECKLIST", Required: true, Options: []domain.FieldOption{{ID: 3, Name: "A"}, {ID: 4, Name: "B"}}})
	if _, present, err := c.value(); present || err == "" {
		t.Error("an empty required checklist should be absent with an error")
	}
	c.checked[4] = true
	v, present, err := c.value()
	if !present || err != "" {
		t.Fatalf("a checked checklist should be present without error, got present=%v err=%q", present, err)
	}
	if !reflect.DeepEqual(v, map[string]bool{"4": true}) {
		t.Errorf("checklist value = %#v, want {\"4\": true}", v)
	}
}
