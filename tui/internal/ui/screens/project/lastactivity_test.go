package project

import (
	"strings"
	"testing"
	"time"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// #9: an issue row shows its last activity as a compact relative time in the trailing column.
func TestIssueRowShowsLastActivity(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	it := domain.IssueSummary{Key: "TIS-1", Title: "X", StateCategory: "ACTIVE", LastActivity: time.Now().Add(-3 * time.Hour)}
	row := plain(m.issueRow(it, 0, m.titleWidth(160), 160, false))
	if !strings.Contains(row, "3h") {
		t.Errorf("the issue row should show the relative last-activity time:\n%s", row)
	}
}

// #9: the activity column is added to BOTH the header and the rows, so the table stays aligned (equal
// rendered widths) rather than the header drifting from its rows.
func TestIssueListActivityColumnAligned(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	titleW := m.titleWidth(160)
	it := domain.IssueSummary{Key: "TIS-1", Title: "Some issue", StateCategory: "ACTIVE", LastActivity: time.Now().Add(-2 * time.Hour)}
	head := lipgloss.Width(m.headerRow(titleW))
	// an unbanded row (not the cursor, not hovered) keeps its natural cell widths; a selected/hovered
	// row is deliberately padded to the full panel width by its highlight band.
	row := lipgloss.Width(m.issueRow(it, m.cursor+1, titleW, 160, false))
	if head != row {
		t.Errorf("header width %d must match row width %d once the activity column is added", head, row)
	}
}

// the activity column leads the row: the relative time appears before the issue key.
func TestActivityColumnIsFirst(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	it := domain.IssueSummary{Key: "TIS-1", Title: "X", StateCategory: "ACTIVE", LastActivity: time.Now().Add(-4 * time.Hour)}
	row := plain(m.issueRow(it, 0, m.titleWidth(160), 160, false))
	if ago, key := strings.Index(row, "4h"), strings.Index(row, "TIS-1"); ago < 0 || key < 0 || ago > key {
		t.Errorf("the activity time should lead the row, before the key (ago=%d key=%d):\n%s", ago, key, row)
	}
	head := plain(m.headerRow(m.titleWidth(160)))
	if act, k := strings.Index(head, "◷"), strings.Index(head, "Key"); act < 0 || k < 0 || act > k {
		t.Errorf("the activity header glyph should lead, before 'Key':\n%s", head)
	}
}
