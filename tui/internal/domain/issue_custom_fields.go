package domain

import (
	"strconv"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// CustomField is one custom field value on an issue, decoded from the detail BFF into a render-ready
// shape. The backend stores each field type as a specific JSON scalar/shape (numbers for INTEGER/
// PERCENTAGE/SELECT_OPTION, strings for TEXT/SHORT_TEXT/DECIMAL/DATE/TIMESTAMP, a bool for BOOLEAN, and a
// {optionId: bool} map for CHECKLIST); toCustomField normalizes those. Exactly one carrier below is
// meaningful per Type - the rest stay zero, and an unset field leaves them all zero (rendered "-").
type CustomField struct {
	ID    int64 // the field definition id, for resolving an activity's customFields.{id} change key
	Label string
	Type  string // IssueFieldType enum name

	Text    string          // scalar text: SHORT_TEXT/TEXT/INTEGER/DECIMAL/DATE/TIMESTAMP/SELECT_OPTION (resolved name)
	Bool    *bool           // BOOLEAN (nil = unset)
	Percent *int            // PERCENTAGE (nil = unset)
	Items   []ChecklistItem // CHECKLIST (nil = unset)

	// Raw and Options carry what the render-ready carriers above discard, so the value can be edited and
	// not just shown: Raw is the stored JSON value verbatim (nil = unset), Options the field's selectable
	// options. Together with Type they are enough to rebuild an editable input without refetching the
	// issue type's schema.
	Raw     interface{}
	Options []FieldOption
}

// Definition reconstructs the field's schema from what the detail carries, for seeding an editable input.
// Required is not part of the detail payload, so it reads false here: the edit form leaves the "this
// field is required" judgement to the server, which rejects clearing one.
func (c CustomField) Definition() IssueField {
	return IssueField{ID: int(c.ID), Name: c.Label, Type: c.Type, Options: c.Options}
}

// ChecklistItem is one option of a CHECKLIST field with its checked state.
type ChecklistItem struct {
	Name    string
	Checked bool
}

func toCustomField(v client.CustomFieldValueInfo) CustomField {
	f := CustomField{
		ID: derefInt64to64(v.FieldId), Label: deref(v.FieldLabel), Type: enumStr(v.IssueFieldType),
		Raw: v.Value, Options: toFieldOptions(v.Options),
	}
	switch f.Type {
	case "TEXT", "SHORT_TEXT", "DECIMAL", "DATE":
		if s, ok := v.Value.(string); ok { // backend delivers these as display-ready strings
			f.Text = s
		}
	case "INTEGER":
		if n, ok := v.Value.(float64); ok { // JSON numbers decode to float64
			f.Text = strconv.FormatInt(int64(n), 10)
		}
	case "TIMESTAMP":
		if s, ok := v.Value.(string); ok {
			f.Text = formatTimestamp(s)
		}
	case "BOOLEAN":
		if b, ok := v.Value.(bool); ok {
			f.Bool = &b
		}
	case "PERCENTAGE":
		if n, ok := v.Value.(float64); ok {
			p := int(n)
			f.Percent = &p
		}
	case "SELECT_OPTION":
		if n, ok := v.Value.(float64); ok { // the stored value is the option id
			f.Text = optionName(v.Options, int64(n))
		}
	case "CHECKLIST":
		f.Items = checklistItems(v.Options, v.Value)
	}
	return f
}

// formatTimestamp renders an ISO-8601 instant string as "2006-01-02 15:04" in local time, or the raw
// string if it does not parse (better to show something than to drop the value).
func formatTimestamp(s string) string {
	if s == "" {
		return ""
	}
	t, err := time.Parse(time.RFC3339, s)
	if err != nil {
		return s
	}
	return t.Local().Format("2006-01-02 15:04")
}

// optionName resolves a SELECT_OPTION id to its display name via the field's options, falling back to
// "#id" when the option is not found among them.
func optionName(options *[]client.FieldOptionDetail, id int64) string {
	if options != nil {
		for _, o := range *options {
			if derefInt64to64(o.Id) == id {
				return deref(o.Name)
			}
		}
	}
	return "#" + strconv.FormatInt(id, 10)
}

// toFieldOptions copies a field's selectable options into the domain shape, preserving their order.
func toFieldOptions(options *[]client.FieldOptionDetail) []FieldOption {
	if options == nil {
		return nil
	}
	out := make([]FieldOption, 0, len(*options))
	for _, o := range *options {
		out = append(out, FieldOption{ID: int(derefInt64to64(o.Id)), Name: deref(o.Name)})
	}
	return out
}

// checklistItems builds the checklist from the field's options (preserving their defined order), marking
// each checked per the stored {optionId: bool} map. An unset field (value not a map) maps to nil, so the
// UI renders "-" rather than a list of all-unchecked options.
func checklistItems(options *[]client.FieldOptionDetail, value interface{}) []ChecklistItem {
	checked, ok := value.(map[string]interface{})
	if !ok || options == nil {
		return nil
	}
	items := make([]ChecklistItem, 0, len(*options))
	for _, o := range *options {
		key := strconv.FormatInt(derefInt64to64(o.Id), 10)
		isChecked, _ := checked[key].(bool)
		items = append(items, ChecklistItem{Name: deref(o.Name), Checked: isChecked})
	}
	return items
}
