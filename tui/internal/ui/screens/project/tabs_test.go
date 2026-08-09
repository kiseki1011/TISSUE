package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
)

// The Issues list shows an assignee-name column: the name when assigned, "-" when not.
func TestAssigneeColumnRenders(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{
		TotalElements: 2,
		Issues: []domain.IssueSummary{
			{Key: "T-1", Title: "A", Assigned: true, AssigneeName: "Alice", StateCategory: "ACTIVE", Priority: "P1"},
			{Key: "T-2", Title: "B", StateCategory: "INITIAL", Priority: "P3"},
		},
	})
	body := plain(m.View())
	if !strings.Contains(body, "Assignee") {
		t.Errorf("header row missing the Assignee column:\n%s", body)
	}
	if !strings.Contains(body, "Alice") {
		t.Errorf("assigned issue should show the assignee name:\n%s", body)
	}
}

// tab / "/" focuses the search box (capturing input); esc hands focus back to the list, and a second
// esc steps back to the list; backspace backs out of the drill-in.
func TestSearchFocusAndBlur(t *testing.T) {
	m := loaded(t, 120, 16, domain.IssuePage{})
	m, _ = m.Update(press("tab"))
	if m.focus != focusSearch || !m.CapturingInput() {
		t.Fatal("tab should focus the search box")
	}
	m, _ = m.Update(press("esc"))
	if m.focus != focusList {
		t.Fatal("esc should return focus to the list")
	}
	if _, cmd := m.Update(press("backspace")); cmd == nil {
		t.Error("backspace on the list should back out of the drill-in")
	}
}

// Typing is debounced: only the most recent keystroke's timer commits the keyword and reloads.
func TestSearchDebounce(t *testing.T) {
	m := loaded(t, 120, 16, domain.IssuePage{})
	m, _ = m.Update(press("tab"))
	for _, k := range []string{"b", "u", "g"} {
		m, _ = m.Update(press(k))
	}
	if m.search.Value() != "bug" {
		t.Fatalf("typing did not reach the input: %q", m.search.Value())
	}

	if sup, cmd := m.Update(searchDebounceMsg{seq: m.searchSeq - 1}); cmd != nil || sup.filter.Keyword != "" {
		t.Error("a superseded debounce must not search")
	}
	committed, cmd := m.Update(searchDebounceMsg{seq: m.searchSeq})
	if cmd == nil {
		t.Fatal("the latest debounce should trigger a reload")
	}
	if committed.filter.Keyword != "bug" {
		t.Errorf("keyword not committed to the filter: %q", committed.filter.Keyword)
	}
}

// While the search is focused, a digit types into the box instead of switching tabs.
func TestDigitTypesIntoSearch(t *testing.T) {
	m := loaded(t, 120, 16, domain.IssuePage{})
	m, _ = m.Update(press("tab"))
	m, _ = m.Update(press("2"))
	if m.tab != tabIssues {
		t.Error("a digit should not switch tabs while the search is focused")
	}
	if m.search.Value() != "2" {
		t.Errorf("the digit should type into the search box: %q", m.search.Value())
	}
}

// The drill-in defaults to the open-issues view (INITIAL + ACTIVE).
func TestDefaultFilterIsOpen(t *testing.T) {
	m := New(testDeps(), testKey, "T")
	got := m.filter.StateCategories
	if len(got) != 2 || got[0] != "INITIAL" || got[1] != "ACTIVE" {
		t.Errorf("project should open on the open-issues filter, got %v", got)
	}
}

// A digit key switches project tabs; key 3 opens the Stats tab, which shows a loading note until its
// snapshot lands and then renders the statistics overview.
func TestTabSwitchByKey(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(2), TotalElements: 2})

	m, _ = m.Update(press("3"))
	if m.tab != tabStats {
		t.Fatalf("key 3 should select the Stats tab, got %v", m.tab)
	}
	if body := plain(m.View()); !strings.Contains(body, "Statistics") {
		t.Errorf("Stats tab should render the statistics panel:\n%s", body)
	}

	// deliver the snapshot the tab requested and confirm the overview renders (KPIs + labelled sections
	// with humanized enum labels)
	m, _ = m.Update(statsLoadedMsg{key: testKey, gen: m.statsReqGen, stats: domain.ProjectStats{
		Total: 10, Open: 6, Completed: 4, Unassigned: 2, Overdue: 1,
		ByState:     []domain.StatBucket{{Label: "ACTIVE", Count: 6}, {Label: "COMPLETED", Count: 4}},
		ByPriority:  []domain.StatBucket{{Label: "P1", Count: 3}},
		ByHierarchy: []domain.StatBucket{{Label: "STANDARD", Count: 7}},
	}})
	body := plain(m.View())
	for _, want := range []string{"Total", "By State", "Active", "By Priority", "P1", "By Hierarchy", "Standard"} {
		if !strings.Contains(body, want) {
			t.Errorf("Stats overview missing %q:\n%s", want, body)
		}
	}

	m, _ = m.Update(press("1"))
	if m.tab != tabIssues {
		t.Fatalf("key 1 should return to the Issues tab, got %v", m.tab)
	}
	if body := plain(m.View()); !strings.Contains(body, "Issues (2)") {
		t.Errorf("Issues tab should render the list:\n%s", body)
	}
}

// The header content carries the back affordance and all four tab labels; a compact header drops the
// back link but keeps every tab.
func TestHeaderInfoShowsBackAndTabs(t *testing.T) {
	m := loaded(t, 120, 20, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	h := plain(m.HeaderInfo(false))
	for _, want := range []string{"‹ Projects", "Issues", "Sprints", "Stats", "Members", "Config"} {
		if !strings.Contains(h, want) {
			t.Errorf("HeaderInfo missing %q:\n%s", want, h)
		}
	}
	c := plain(m.HeaderInfo(true))
	if strings.Contains(c, "‹ Projects") {
		t.Errorf("a compact header should drop the back link:\n%s", c)
	}
	for _, want := range []string{"Issues", "Sprints", "Stats", "Members", "Config"} {
		if !strings.Contains(c, want) {
			t.Errorf("a compact header should still show every tab, missing %q:\n%s", want, c)
		}
	}
}

// List-navigation keys act only on the Issues tab, so they do not stir state on the other tabs.
func TestListKeysGatedToIssuesTab(t *testing.T) {
	m := loaded(t, 120, 20, domain.IssuePage{Issues: issues(5), TotalElements: 5})
	m, _ = m.Update(press("j")) // move down on the Issues tab
	if m.cursor != 1 {
		t.Fatalf("expected the cursor to move on the Issues tab, got %d", m.cursor)
	}

	m, _ = m.Update(press("3")) // Stats tab (a placeholder, no list)
	before := m.cursor
	m, _ = m.Update(press("j"))
	if m.cursor != before {
		t.Errorf("list keys should be inert off the Issues tab (cursor moved %d -> %d)", before, m.cursor)
	}
	if _, cmd := m.Update(press("R")); cmd != nil {
		t.Error("reload should be inert off the Issues tab")
	}
}

// backspace leaves the drill-in regardless of the active tab.
func TestBackspaceBacksOut(t *testing.T) {
	m := loaded(t, 120, 20, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(press("4")) // Config tab
	_, cmd := m.Update(press("backspace"))
	if cmd == nil {
		t.Fatal("backspace should emit a back command")
	}
	if _, ok := cmd().(nav.CloseProjectMsg); !ok {
		t.Errorf("backspace should emit nav.CloseProjectMsg, got %T", cmd())
	}
}
