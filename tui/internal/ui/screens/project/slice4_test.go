package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func pbool(b bool) *bool { return &b }
func pint(i int) *int    { return &i }

// The Custom fields section renders each type: scalar inline, BOOLEAN as ✓, PERCENTAGE with a bar,
// CHECKLIST as a checkbox list, TEXT as a block, and an unset value as "-".
func TestDetailCustomFields(t *testing.T) {
	m := openDetailOn(t, 160, 60, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T",
		CustomFields: []domain.CustomField{
			{Label: "Severity", Type: "SHORT_TEXT", Text: "High"},
			{Label: "Shipped", Type: "BOOLEAN", Bool: pbool(true)},
			{Label: "Completion", Type: "PERCENTAGE", Percent: pint(50)},
			{Label: "Tasks", Type: "CHECKLIST", Items: []domain.ChecklistItem{{Name: "Design", Checked: true}, {Name: "Build"}}},
			{Label: "Notes", Type: "TEXT", Text: "some free text"},
			{Label: "Estimate", Type: "INTEGER", Text: ""}, // unset -> "-"
		},
	}})
	body := plain(m.View())
	for _, want := range []string{
		"Custom fields", "Severity", "High", "Shipped", "✓", "Completion", "50%",
		"Tasks", "[✓] Design", "[ ] Build", "Notes", "some free text", "Estimate",
	} {
		if !strings.Contains(body, want) {
			t.Errorf("custom fields body missing %q:\n%s", want, body)
		}
	}
}

// Wide custom-field values (long labels, long text, long checklist names) must not overflow the narrow
// read-only modal.
func TestDetailCustomFieldsFitNarrow(t *testing.T) {
	m := openDetailOn(t, 100, 30, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T",
		CustomFields: []domain.CustomField{
			{Label: strings.Repeat("VeryLongFieldLabel", 4), Type: "SHORT_TEXT", Text: strings.Repeat("value ", 40)},
			{Label: "Pct", Type: "PERCENTAGE", Percent: pint(100)},
			{Label: strings.Repeat("LongListLabel", 8), Type: "CHECKLIST", Items: []domain.ChecklistItem{{Name: strings.Repeat("item ", 40), Checked: true}}},
			{Label: strings.Repeat("LongTextLabel", 8), Type: "TEXT", Text: strings.Repeat("word ", 80)},
		},
	}})
	for _, line := range strings.Split(plain(m.View()), "\n") {
		if lipgloss.Width(line) > m.width {
			t.Errorf("a custom field overflowed the modal: %d > %d:\n%q", lipgloss.Width(line), m.width, line)
		}
	}
}

// A custom field label is untrusted server text; a stray control char in a TEXT/CHECKLIST label header
// must be flattened so it cannot reset the cursor and corrupt the frame. Regression for the raw head.
func TestCustomFieldLabelSanitized(t *testing.T) {
	m := openDetailOn(t, 160, 60, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T",
		CustomFields: []domain.CustomField{
			{Label: "Notes\rEVIL", Type: "TEXT", Text: "body"},
			{Label: "Tasks\rBAD", Type: "CHECKLIST", Items: []domain.ChecklistItem{{Name: "A", Checked: true}}},
		},
	}})
	if strings.ContainsRune(plain(m.View()), '\r') {
		t.Error("a carriage return in a custom field label must not reach the rendered frame")
	}
}
