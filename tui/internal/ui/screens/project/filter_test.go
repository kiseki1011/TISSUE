package project

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// Applying carries the toggled axes, keeping the default open states, in canonical order.
func TestFilterFormApplyCarriesToggledAxes(t *testing.T) {
	zone.NewGlobal()
	f := newFilterForm(testDeps(), domain.OpenIssuesFilter(), nil, true)
	f, _ = f.activate(f.indexOfKind(kindPriority)) // first priority row = P0
	f, _ = f.activate(f.indexOfKind(kindAssignee))
	_, cmd := f.activate(f.indexOfKind(kindApply))
	if cmd == nil {
		t.Fatal("apply should emit a command")
	}
	msg, ok := cmd().(filterAppliedMsg)
	if !ok {
		t.Fatalf("apply should emit filterAppliedMsg, got %T", cmd())
	}
	if len(msg.states) != 2 || msg.states[0] != "INITIAL" || msg.states[1] != "ACTIVE" {
		t.Errorf("states = %v, want [INITIAL ACTIVE]", msg.states)
	}
	if len(msg.priorities) != 1 || msg.priorities[0] != "P0" {
		t.Errorf("priorities = %v, want [P0]", msg.priorities)
	}
	if !msg.assigneeMe {
		t.Error("assigneeMe should be set")
	}
}

// The form seeds its checkboxes from the current filter, so reopening shows the active selection.
func TestFilterFormSeedsFromCurrentFilter(t *testing.T) {
	zone.NewGlobal()
	f := newFilterForm(testDeps(), domain.IssueFilter{
		StateCategories: []string{"INITIAL", "ACTIVE"},
		Priorities:      []string{"P1"},
		AssigneeMe:      true,
	}, nil, true)
	body := plain(f.View())
	if !strings.Contains(body, "[x] P1") {
		t.Errorf("P1 should be checked:\n%s", body)
	}
	if strings.Contains(body, "[x] P0") {
		t.Errorf("P0 should be unchecked:\n%s", body)
	}
	if !strings.Contains(body, "[x] Assigned to me") {
		t.Errorf("assignee should be checked:\n%s", body)
	}
}

func TestFilterFormCancel(t *testing.T) {
	zone.NewGlobal()
	f := newFilterForm(testDeps(), domain.OpenIssuesFilter(), nil, true)
	_, cmd := f.activate(f.indexOfKind(kindCancel))
	if cmd == nil {
		t.Fatal("cancel should emit a command")
	}
	if _, ok := cmd().(filterCancelledMsg); !ok {
		t.Errorf("cancel should emit filterCancelledMsg, got %T", cmd())
	}
}

// Until the catalog loads the Type section shows a loading note rather than an empty gap.
func TestFilterFormTypeLoadingState(t *testing.T) {
	zone.NewGlobal()
	loadingBody := plain(newFilterForm(testDeps(), domain.OpenIssuesFilter(), nil, false).View())
	if !strings.Contains(loadingBody, "Loading") {
		t.Errorf("an unloaded catalog should show a loading note:\n%s", loadingBody)
	}
	loadedBody := plain(newFilterForm(testDeps(), domain.OpenIssuesFilter(),
		[]domain.IssueTypeSummary{{ID: 3, Name: "Bug"}}, true).View())
	if !strings.Contains(loadedBody, "Bug") {
		t.Errorf("a loaded catalog should list the issue types:\n%s", loadedBody)
	}
}

// tab twice reaches the filter button, and enter opens the modal (which captures input).
func TestFilterOpensViaKeyboard(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{})
	m, _ = m.Update(press("tab")) // list -> search
	m, _ = m.Update(press("tab")) // search -> filter
	if m.focus != focusFilter {
		t.Fatalf("two tabs should land on the filter button, got %v", m.focus)
	}
	m, _ = m.Update(press("enter"))
	if !m.filtering {
		t.Fatal("enter on the filter button should open the modal")
	}
	if !m.CapturingInput() {
		t.Error("an open modal should capture input")
	}
}

// Applying commits every axis, preserves the search keyword, supersedes in-flight loads, and reloads.
func TestFilterApplyUpdatesFilterAndReloads(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{})
	m.filter.Keyword = "bug"
	m, _ = m.openFilter()
	gen := m.reqGen
	m, cmd := m.Update(filterAppliedMsg{
		states:     []string{"COMPLETED"},
		priorities: []string{"P0"},
		typeIDs:    []int64{7},
		assigneeMe: true,
	})
	if m.filtering {
		t.Error("apply should close the modal")
	}
	if len(m.filter.StateCategories) != 1 || m.filter.StateCategories[0] != "COMPLETED" {
		t.Errorf("states not applied: %v", m.filter.StateCategories)
	}
	if len(m.filter.Priorities) != 1 || !m.filter.AssigneeMe || len(m.filter.IssueTypeIDs) != 1 {
		t.Errorf("axes not applied: %+v", m.filter)
	}
	if m.filter.Keyword != "bug" {
		t.Errorf("apply should preserve the search keyword, got %q", m.filter.Keyword)
	}
	if m.reqGen != gen+1 {
		t.Error("apply should bump the request generation")
	}
	if cmd == nil {
		t.Fatal("apply should trigger a reload")
	}
}

func TestFilterCancelKeepsFilter(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{})
	before := len(m.filter.StateCategories)
	m, _ = m.openFilter()
	m, cmd := m.Update(filterCancelledMsg{})
	if m.filtering {
		t.Error("cancel should close the modal")
	}
	if cmd != nil {
		t.Error("cancel should not reload")
	}
	if len(m.filter.StateCategories) != before {
		t.Error("cancel should leave the filter unchanged")
	}
}

// The filter button reads inactive on the default open view and active once an axis is set.
func TestFilterButtonActiveState(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{})
	if m.filterActive() {
		t.Error("the default open filter should not read as active")
	}
	m.filter.Priorities = []string{"P0"}
	if !m.filterActive() {
		t.Error("a priority filter should read as active")
	}
}

// A click that lands off the search and filter zones (a tab, a row, empty space) returns focus to the
// list, so keyboard nav and esc-to-back keep working. Regression: onClick reset only the search focus.
func TestClickOffControlsClearsFilterFocus(t *testing.T) {
	m := loaded(t, 120, 20, domain.IssuePage{Issues: issues(3), TotalElements: 3})
	m.focus = focusFilter
	// (0,0) is the top-left padding cell - outside the search box, the filter button, and every row
	m, _ = m.Update(tea.MouseClickMsg{X: 0, Y: 0, Button: tea.MouseLeft})
	if m.focus != focusList {
		t.Errorf("a click off the controls should return focus to the list, got %v", m.focus)
	}
}

// A catalog load that lands while the modal is already open fills in the Type rows in place, keeping
// any toggles the user has already made. Regression: the open modal kept its empty snapshot.
func TestLateIssueTypesFillOpenModal(t *testing.T) {
	m := loaded(t, 120, 30, domain.IssuePage{})
	m, _ = m.openFilter() // opened before the prefetch returns (typesLoaded == false)
	if !strings.Contains(plain(m.View()), "Loading") {
		t.Fatal("modal should show the loading note before the catalog arrives")
	}
	m, _ = m.Update(issueTypesLoadedMsg{types: []domain.IssueTypeSummary{{ID: 5, Name: "Chore"}}})
	if !m.filtering {
		t.Fatal("the modal should still be open after the catalog loads")
	}
	if body := plain(m.View()); !strings.Contains(body, "Chore") {
		t.Errorf("a late catalog load should fill the open modal's Type section:\n%s", body)
	}
}

func TestWithTypesPreservesSelection(t *testing.T) {
	zone.NewGlobal()
	f := newFilterForm(testDeps(), domain.OpenIssuesFilter(), nil, false)
	f, _ = f.activate(f.indexOfKind(kindPriority)) // check P0 before the catalog loads
	f = f.withTypes([]domain.IssueTypeSummary{{ID: 1, Name: "Bug"}}, true)
	body := plain(f.View())
	if !strings.Contains(body, "[x] P0") {
		t.Errorf("withTypes should preserve the in-progress selection:\n%s", body)
	}
	if !strings.Contains(body, "Bug") {
		t.Errorf("withTypes should add the loaded type rows:\n%s", body)
	}
}

// Keyboard navigation drops a stale mouse-hover index, so only the focused row is highlighted.
func TestFilterFormKeyboardClearsHover(t *testing.T) {
	zone.NewGlobal()
	f := newFilterForm(testDeps(), domain.OpenIssuesFilter(), nil, true)
	f.hover = 2 // a leftover mouse hover
	f, _ = f.onKey(press("down"))
	if f.hover != -1 {
		t.Errorf("keyboard navigation should clear the hover, got %d", f.hover)
	}
}

func TestIssueTypesFeedFilterModal(t *testing.T) {
	m := loaded(t, 120, 30, domain.IssuePage{})
	m, _ = m.Update(issueTypesLoadedMsg{types: []domain.IssueTypeSummary{{ID: 3, Name: "Defect"}}})
	if !m.typesLoaded || len(m.types) != 1 {
		t.Fatalf("types not stored: loaded=%v n=%d", m.typesLoaded, len(m.types))
	}
	m, _ = m.openFilter()
	if body := plain(m.View()); !strings.Contains(body, "Defect") {
		t.Errorf("filter modal should list the loaded issue type:\n%s", body)
	}
}
