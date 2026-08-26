package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// childReady loads a STANDARD ("Story") issue with Epic/Story/Subtask types, so it can take children.
func childReady(t *testing.T, children []domain.IssueRef, reviewers []domain.Reviewer) Model {
	t.Helper()
	det := sampleDetail()
	det.TypeName = "Story"
	det.Children = children
	det.Reviewers = reviewers
	m := editReady(t, det)
	m.types = []domain.IssueTypeSummary{
		{ID: 6, Name: "Epic", Hierarchy: "EPIC"},
		{ID: 5, Name: "Story", Hierarchy: "STANDARD"},
		{ID: 7, Name: "Subtask", Hierarchy: "SUBTASK"},
	}
	m.typesLoaded = true
	m.members = []domain.ProjectMember{{MemberID: 1, DisplayName: "Hong"}, {MemberID: 2, DisplayName: "Kim"}}
	m.membersLoaded = true
	return m
}

// The Reviewers section always shows, even empty, so its entry point stays visible.
func TestReviewersSectionAlwaysShown(t *testing.T) {
	m := childReady(t, nil, nil)
	body := plain(m.detailContent(m.details[m.viewKey], 90))
	if !strings.Contains(body, "Reviewers (0)") {
		t.Errorf("the Reviewers section should show even when empty:\n%s", body)
	}
	if !strings.Contains(body, "+ Reviewer") {
		t.Errorf("an empty Reviewers section should show the + Reviewer button:\n%s", body)
	}
	if strings.Contains(body, "No reviewers yet.") {
		t.Errorf("the + Reviewer button should replace the placeholder with the mouse on:\n%s", body)
	}
}

// The add button sits below the existing items (a blank line apart), not on the section rule.
func TestAddButtonBelowItems(t *testing.T) {
	m := childReady(t,
		[]domain.IssueRef{{Key: "SUB-1", TypeName: "Subtask", StateLabel: "Todo", StateCategory: "INITIAL"}},
		[]domain.Reviewer{{MemberID: 2, Name: "Kim", Status: "PENDING"}},
	)
	lines := strings.Split(plain(m.detailContent(m.details[m.viewKey], 90)), "\n")
	for _, pair := range []struct{ item, btn string }{{"SUB-1", "+ Child"}, {"Kim", "+ Reviewer"}} {
		itemIdx, btnIdx := -1, -1
		for i, line := range lines {
			if strings.Contains(line, pair.item) {
				itemIdx = i
			}
			if strings.Contains(line, pair.btn) && btnIdx < 0 {
				btnIdx = i
			}
		}
		if itemIdx < 0 || btnIdx < 0 {
			t.Fatalf("missing rows for %q: item=%d btn=%d", pair.btn, itemIdx, btnIdx)
		}
		if btnIdx <= itemIdx {
			t.Errorf("%q should sit below its item %q (item=%d btn=%d)", pair.btn, pair.item, itemIdx, btnIdx)
		}
		if strings.TrimSpace(lines[btnIdx-1]) != "" {
			t.Errorf("%q should be a blank line below the item, got %q above it", pair.btn, lines[btnIdx-1])
		}
	}
}

// Clicking "+ Reviewer" opens the same picker r opens.
func TestReviewersButtonOpensPicker(t *testing.T) {
	m := childReady(t, nil, nil)
	m, _ = m.Update(clickZone(t, m, zoneEditReviewers))
	if !m.picking || m.pickKind != pickReviewers {
		t.Fatalf("clicking + Reviewer should open the reviewers picker, got picking=%v kind=%v", m.picking, m.pickKind)
	}
}

// A Story (STANDARD) can take Subtask children, so its Children section shows even when empty.
func TestChildrenSectionShownWhenCanAddChild(t *testing.T) {
	m := childReady(t, nil, nil)
	if !m.canAddChild(m.details[m.viewKey]) {
		t.Fatal("a Story with Subtask types loaded should be able to add a child")
	}
	body := plain(m.detailContent(m.details[m.viewKey], 90))
	if !strings.Contains(body, "Children (0)") {
		t.Errorf("the Children section should show when children are possible:\n%s", body)
	}
	if !strings.Contains(body, "+ Child") {
		t.Errorf("an empty Children section should show the + Child button:\n%s", body)
	}
	if strings.Contains(body, "No child issues yet.") {
		t.Errorf("the + Child button should replace the placeholder with the mouse on:\n%s", body)
	}
}

// A Microtask is bottom-level, so its Children section stays hidden.
func TestChildrenSectionHiddenWhenBottomLevel(t *testing.T) {
	det := sampleDetail()
	det.TypeName = "Micro"
	m := editReady(t, det)
	m.types = []domain.IssueTypeSummary{
		{ID: 7, Name: "Subtask", Hierarchy: "SUBTASK"},
		{ID: 8, Name: "Micro", Hierarchy: "MICROTASK"},
	}
	m.typesLoaded = true
	if m.canAddChild(m.details[m.viewKey]) {
		t.Fatal("a bottom-level Microtask must not be able to add a child")
	}
	body := plain(m.detailContent(m.details[m.viewKey], 90))
	if strings.Contains(body, "Children (") {
		t.Errorf("a childless bottom-level issue should show no Children section:\n%s", body)
	}
}

// Clicking the Children "+" opens the create form with the parent locked and types cut to Subtask.
func TestAddChildOpensLockedForm(t *testing.T) {
	m := childReady(t, nil, nil)
	key := m.viewKey
	m, _ = m.Update(clickZone(t, m, zoneAddChild))
	if !m.creating {
		t.Fatal("clicking + should open the child-create form")
	}
	f := m.createUI
	if !f.lockedParent {
		t.Error("the child-create form should lock the parent")
	}
	if f.parentKey != key {
		t.Errorf("the child-create parent should be the current issue %q, got %q", key, f.parentKey)
	}
	if len(f.types) != 1 || f.types[0].Name != "Subtask" {
		t.Errorf("the type cycle should be restricted to the child (Subtask) level, got %v", f.types)
	}
}

// A locked parent survives a type cycle and is not a tab stop.
func TestChildCreateFormLocksParent(t *testing.T) {
	f := newChildCreateForm(testDeps(), []domain.IssueTypeSummary{
		{ID: 7, Name: "Subtask", Hierarchy: "SUBTASK"},
		{ID: 9, Name: "Chore", Hierarchy: "SUBTASK"},
	}, "ENG-1", "ENG-1  Parent story")
	f2 := f.cycleType(1)
	if f2.parentKey != "ENG-1" {
		t.Errorf("cycling the type must keep the locked parent, got %q", f2.parentKey)
	}
	for _, id := range f.fields() {
		if id == nfParent {
			t.Error("a locked parent must not be a tab stop")
		}
	}
	if body := plain(f.parentContent()); !strings.Contains(body, "Parent story") {
		t.Errorf("the locked Parent field should show the parent label, got %q", body)
	}
}

// openChildCreateForm refuses a bottom-level issue with a toast.
func TestOpenChildCreateGuardsBottomLevel(t *testing.T) {
	det := sampleDetail()
	det.TypeName = "Micro"
	m := editReady(t, det)
	m.types = []domain.IssueTypeSummary{{ID: 8, Name: "Micro", Hierarchy: "MICROTASK"}}
	m.typesLoaded = true
	m2, cmd := m.openChildCreateForm()
	if m2.creating {
		t.Error("a bottom-level issue must not open the child-create form")
	}
	if cmd == nil {
		t.Error("the refusal should surface a toast")
	}
}
