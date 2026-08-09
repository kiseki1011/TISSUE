package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// listActionReady loads a project with one issue and its detail but leaves focus on the list (no enter),
// with members and a transition present so every per-issue action can open from the list.
func listActionReady(t *testing.T) Model {
	t.Helper()
	m := loaded(t, 120, 44, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "First", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m.members = []domain.ProjectMember{{MemberID: 7, DisplayName: "Bob"}}
	m.membersLoaded = true
	m, _ = m.Update(IssueDetailLoadedMsg{
		key: m.viewKey, gen: m.detailGen[m.viewKey],
		detail: domain.IssueDetail{
			Key: m.viewKey, Title: "First", Priority: "P2",
			Transitions: []domain.IssueTransition{{ID: 3, Label: "Start", TargetLabel: "In progress", CanExecute: true}},
		},
	})
	if m.focus != focusList {
		t.Fatalf("setup must stay on the list focus, got %v", m.focus)
	}
	return m
}

// Every per-issue action opens from the list focus without first tabbing into Details.
func TestIssueActionsOpenFromList(t *testing.T) {
	cases := []struct {
		key  string
		open func(Model) bool
	}{
		{"e", func(m Model) bool { return m.editing }},
		{"c", func(m Model) bool { return m.commenting }},
		{"d", func(m Model) bool { return m.deleting }},
		{"t", func(m Model) bool { return m.picking && m.pickKind == pickTransition }},
		{"a", func(m Model) bool { return m.picking && m.pickKind == pickAssignee }},
	}
	for _, c := range cases {
		m := listActionReady(t)
		if m.focus != focusList {
			t.Fatalf("%q: precondition is list focus", c.key)
		}
		m, _ = m.Update(press(c.key))
		if !c.open(m) {
			t.Errorf("%q should open its action from the list focus", c.key)
		}
	}
}

// v toggles the Activity view from the list, without first focusing Details.
func TestActivityToggleFromList(t *testing.T) {
	m := listActionReady(t)
	if m.showActivity {
		t.Fatal("activity should start hidden")
	}
	m, _ = m.Update(press("v"))
	if !m.showActivity {
		t.Error("v should toggle activity on from the list focus")
	}
	m, _ = m.Update(press("v"))
	if m.showActivity {
		t.Error("v should toggle activity back off from the list focus")
	}
}

// In narrow mode, esc-ing out of the Activity modal clears the toggle, so a later v reopens it in a
// single press rather than leaving showActivity stale-true (an invisible dead first press).
func TestNarrowActivityReopensInOnePress(t *testing.T) {
	m := loaded(t, 110, 40, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "First", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "First"}})
	if !m.narrow() {
		t.Fatal("110 should be narrow")
	}
	m, _ = m.Update(press("v")) // open the Activity modal
	if !m.showActivity || m.focus != focusDetail {
		t.Fatal("v should open the Activity modal (showActivity + focusDetail)")
	}
	m, _ = m.Update(press("esc")) // close it
	if m.focus != focusList {
		t.Fatal("esc should return to the list")
	}
	if m.showActivity {
		t.Error("esc in narrow must clear the Activity toggle so it does not linger invisibly true")
	}
	m, _ = m.Update(press("v")) // reopen in one press
	if !m.showActivity || m.focus != focusDetail {
		t.Error("v should reopen the Activity modal in a single press")
	}
}

// The filter-button focus is a control, not an issue view, so action keys do not fire there (matching
// its help, which advertises only enter/tab/esc).
func TestFilterFocusNoAction(t *testing.T) {
	m := listActionReady(t)
	m, _ = m.setFocus(focusFilter)
	if m.focus != focusFilter {
		t.Fatalf("expected filter focus, got %v", m.focus)
	}
	m, _ = m.Update(press("e"))
	if m.editing {
		t.Error("action keys must not fire from the filter-button focus")
	}
}

// While the search box is focused an action letter types into the query rather than firing the action.
func TestSearchFocusTypesNotAction(t *testing.T) {
	m := listActionReady(t)
	m, _ = m.Update(press("/")) // focus the search box
	if m.focus != focusSearch {
		t.Fatalf("/ should focus search, got %v", m.focus)
	}
	m, _ = m.Update(press("e"))
	if m.editing {
		t.Error("e must type into the search box, not open the edit form, while search is focused")
	}
	if !strings.Contains(m.search.Value(), "e") {
		t.Errorf("e should have typed into the query, got %q", m.search.Value())
	}
}
