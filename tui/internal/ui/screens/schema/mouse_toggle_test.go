package schema

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// With the mouse off the Edit pens (issue-type/workflow headers and per-field rows) are hidden, since
// they only duplicate the focused block's e/enter. The field itself and the +Field focus stop stay.
func TestMouseOffHidesEditPens(t *testing.T) {
	build := func(mouse bool) Model {
		m := mk(120, 26, 1, 1, false)
		m.deps.Mouse = mouse
		m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
			ID: 1, Fields: []domain.IssueField{{ID: 5, Name: "Priority", Type: "SELECT_OPTION"}},
		}})
		return m
	}

	if on := plain(build(true).detailPanel()); !strings.Contains(on, "Edit") {
		t.Errorf("mouse-on detail should show the type Edit pen:\n%s", on)
	}

	off := plain(build(false).detailPanel())
	if strings.Contains(off, "Edit") || strings.Contains(off, "edit") {
		t.Errorf("mouse-off detail should hide the Edit pens:\n%s", off)
	}
	if !strings.Contains(off, "Priority") || !strings.Contains(off, "+ Field") {
		t.Errorf("mouse-off detail dropped keyboard-navigable content (field / +Field):\n%s", off)
	}
}
