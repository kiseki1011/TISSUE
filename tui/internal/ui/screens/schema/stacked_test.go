package schema

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// A narrow-and-tall terminal stacks Details above the lists, within the height budget.
func TestSchemaStackedLayout(t *testing.T) {
	m := mk(80, 34, 2, 2, false)
	m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
		ID: 1, Fields: []domain.IssueField{{ID: 5, Name: "Priority", Type: "SELECT_OPTION"}},
	}})
	if !m.stacked() {
		t.Fatalf("expected stacked at 80x34")
	}
	view := plain(m.View())
	if got := len(strings.Split(view, "\n")); got != 34 {
		t.Errorf("stacked view = %d rows, want m.height=34", got)
	}
	detailAt, typesAt := strings.Index(view, "Details"), strings.Index(view, "Issue Types")
	if detailAt < 0 || typesAt < 0 {
		t.Fatalf("stacked view missing panes: details=%d types=%d", detailAt, typesAt)
	}
	if detailAt > typesAt {
		t.Error("stacked layout should render Details above the Issue Types list")
	}
}

func TestSchemaSideWhenWide(t *testing.T) {
	m := mk(120, 26, 2, 2, false)
	if m.stacked() {
		t.Error("120x26 should be side by side, not stacked")
	}
	if got := len(strings.Split(plain(m.View()), "\n")); got != 26 {
		t.Errorf("side view = %d rows, want 26", got)
	}
}
