package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// The issue list matches the Projects/Schema lists: the selected (cursor) row is painted with a
// full-width highlight band, and every row has a blank separator line above it.
func TestIssueListAiryRowsAndBandedSelection(t *testing.T) {
	m := loaded(t, 120, 20, domain.IssuePage{Issues: issues(3), TotalElements: 3})
	titleW := m.titleWidth(120)
	it := domain.IssueSummary{Key: "TIS-1", Title: "Some issue", StateCategory: "ACTIVE"}

	// the selected row fills the panel width with its band; an unselected row keeps its natural width.
	if got := lipgloss.Width(m.issueRow(it, m.cursor, titleW, 120, false)); got != 120 {
		t.Errorf("the selected row should fill the panel width with its band: got %d, want 120", got)
	}
	if got := lipgloss.Width(m.issueRow(it, m.cursor+1, titleW, 120, false)); got >= 120 {
		t.Errorf("an unselected row keeps its natural width, got %d (>= panel width 120)", got)
	}

	// a blank separator line sits above each issue row.
	lines := strings.Split(plain(m.listBody(120, 20)), "\n")
	rowIdx := -1
	for i, ln := range lines {
		if strings.Contains(ln, "TIS-2") {
			rowIdx = i
			break
		}
	}
	if rowIdx <= 0 {
		t.Fatalf("expected an issue row for TIS-2 in the body:\n%s", strings.Join(lines, "\n"))
	}
	if strings.TrimSpace(lines[rowIdx-1]) != "" {
		t.Errorf("each issue row should have a blank separator line above it; line above TIS-2 was %q", lines[rowIdx-1])
	}
}
