package project

import (
	"sort"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// reviewersReady loads ENG-1 whose sole reviewer is member 2, with three members loaded.
func reviewersReady(t *testing.T) Model {
	t.Helper()
	m := loaded(t, 120, 30, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m, _ = m.Update(membersLoadedMsg{members: []domain.ProjectMember{
		{MemberID: 1, DisplayName: "Hong"}, {MemberID: 2, DisplayName: "Kim"}, {MemberID: 3, DisplayName: "Lee"},
	}})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "X",
		Reviewers: []domain.Reviewer{{MemberID: 2, Name: "Kim", Status: "PENDING"}},
	}})
	return m
}

func sortedI64(v []int64) []int64 {
	sort.Slice(v, func(i, j int) bool { return v[i] < v[j] })
	return v
}

func TestReviewerDiff(t *testing.T) {
	add, remove := reviewerDiff(map[int64]bool{1: true, 3: true}, map[int64]bool{2: true, 3: true})
	if got := sortedI64(add); len(got) != 1 || got[0] != 1 {
		t.Errorf("add = %v, want [1]", got)
	}
	if got := sortedI64(remove); len(got) != 1 || got[0] != 2 {
		t.Errorf("remove = %v, want [2]", got)
	}
	if a, r := reviewerDiff(map[int64]bool{5: true}, map[int64]bool{5: true}); len(a) != 0 || len(r) != 0 {
		t.Errorf("an unchanged roster should diff empty, got add=%v remove=%v", a, r)
	}
}

func TestReviewersPickerOpensPreChecked(t *testing.T) {
	m := reviewersReady(t)
	m, _ = m.Update(press("r"))
	if !m.picking || m.pickKind != pickReviewers {
		t.Fatalf("r should open the reviewers picker, got picking=%v kind=%v", m.picking, m.pickKind)
	}
	if !m.picker.Multi() {
		t.Error("the reviewers picker should be multi-select")
	}
	if sel := m.picker.Selections(); len(sel) != 1 || sel[0] != "2" {
		t.Errorf("the current reviewer (member 2) should be pre-checked, got %v", sel)
	}
}

func TestReviewersConfirmDiff(t *testing.T) {
	m := reviewersReady(t)
	m, _ = m.Update(press("r"))

	noop, cmd := m.confirmReviewers()
	if noop.picking {
		t.Error("confirm should close the picker")
	}
	if cmd != nil {
		t.Error("confirming an unchanged roster should be a no-op")
	}

	m.picker = m.picker.Toggle() // cursor 0 = member 1 (Hong) -> adds a reviewer
	if _, cmd := m.confirmReviewers(); cmd == nil {
		t.Error("toggling a new reviewer should produce an apply command")
	}
}

// A reviewer with no picker row (inactive member) must survive an unchanged confirm, not be removed.
func TestReviewersInactivePreservedOnNoOp(t *testing.T) {
	m := loaded(t, 120, 30, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m, _ = m.Update(membersLoadedMsg{members: []domain.ProjectMember{
		{MemberID: 1, DisplayName: "Hong"}, {MemberID: 2, DisplayName: "Kim"},
	}})
	m, _ = m.Update(press("enter"))
	// reviewer 9 is NOT among the active members (deactivated), so the picker has no row for it
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "X",
		Reviewers: []domain.Reviewer{{MemberID: 2, Name: "Kim"}, {MemberID: 9, Name: "Ghost"}},
	}})
	m, _ = m.Update(press("r"))
	if _, cmd := m.confirmReviewers(); cmd != nil {
		t.Error("confirming unchanged must not remove a reviewer that has no picker row")
	}
}
