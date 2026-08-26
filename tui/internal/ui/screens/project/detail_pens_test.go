package project

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// detailWithPens focuses a loaded detail so the Details panel renders its inline edit pens.
func detailWithPens(t *testing.T) Model {
	t.Helper()
	// 60 rows so the modal never needs the ScrollBox: a windowed modal collapses its right-anchored pen
	// zones to zero width (an unrelated narrow+scroll quirk).
	m := loaded(t, 120, 60, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m, _ = m.Update(membersLoadedMsg{members: []domain.ProjectMember{
		{MemberID: 1, DisplayName: "Hong"}, {MemberID: 2, DisplayName: "Kim"},
	}})
	m, _ = m.Update(press("enter")) // focus the panel (m.viewKey == ENG-1)
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "X", StateLabel: "Active", StateCategory: "ACTIVE",
		AssigneeID: 2, AssigneeName: "Kim",
		Transitions: []domain.IssueTransition{{ID: 5, Label: "Start", CanExecute: true}},
	}})
	return m
}

func TestDetailEditPensOpenEditors(t *testing.T) {
	m := detailWithPens(t)

	ms, _ := m.Update(clickZone(t, m, zoneEditState))
	if !ms.picking || ms.pickKind != pickTransition {
		t.Errorf("the State pen should open the transition picker, got picking=%v kind=%v", ms.picking, ms.pickKind)
	}

	ma, _ := m.Update(clickZone(t, m, zoneEditAssignee))
	if !ma.picking || ma.pickKind != pickAssignee {
		t.Errorf("the Assignee pen should open the assignee picker, got picking=%v kind=%v", ma.picking, ma.pickKind)
	}

	me, _ := m.Update(clickZone(t, m, zoneEditIssue))
	if !me.editing {
		t.Errorf("the title pen should open the edit form, got editing=%v", me.editing)
	}
}

// Asserted on plain view text, not zones, to avoid cross-test bubblezone state.
func TestDetailEditPensHiddenWithMouseOff(t *testing.T) {
	build := func(mouse bool) Model {
		zone.NewGlobal()
		d := testDeps()
		d.Mouse = mouse
		m := New(d, testKey, "Tissue")
		m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 30})
		m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, page: domain.IssuePage{
			Issues: []domain.IssueSummary{{Key: testKey + "-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
		}})
		m, _ = m.Update(membersLoadedMsg{members: []domain.ProjectMember{
			{MemberID: 1, DisplayName: "Hong"}, {MemberID: 2, DisplayName: "Kim"},
		}})
		m, _ = m.Update(press("enter"))
		m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
			Key: m.viewKey, Title: "X", StateLabel: "Active", StateCategory: "ACTIVE", AssigneeID: 2, AssigneeName: "Kim",
			Transitions: []domain.IssueTransition{{ID: 5, Label: "Start", CanExecute: true}},
		}})
		return m
	}

	// the exact marker penCell renders
	pen := testDeps().Glyphs.Or(testDeps().Glyphs.PenSquare, "edit")
	if !strings.Contains(plain(build(true).View()), pen) {
		t.Errorf("mouse-on Details should show the edit pen %q", pen)
	}
	if strings.Contains(plain(build(false).View()), pen) {
		t.Error("mouse-off Details must hide the edit pens")
	}

	m := build(false)
	m, _ = m.Update(press("a"))
	if !m.picking || m.pickKind != pickAssignee {
		t.Error("the a key should still open the assignee picker with the mouse off")
	}
}
