package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// assignReady opens the detail modal on ENG-1 (assigned to member 2) with a loaded member list.
func assignReady(t *testing.T) Model {
	t.Helper()
	m := loaded(t, 120, 30, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m, _ = m.Update(membersLoadedMsg{members: []domain.ProjectMember{
		{MemberID: 1, DisplayName: "Hong"}, {MemberID: 2, DisplayName: "Kim"}, {MemberID: 3, Username: "bot"},
	}})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "X", AssigneeID: 2, AssigneeName: "Kim",
	}})
	return m
}

func TestAssignPickerOpens(t *testing.T) {
	m := assignReady(t)
	m, cmd := m.Update(press("a"))
	if !m.picking || m.pickKind != pickAssignee {
		t.Fatalf("a should open the assignee picker, got picking=%v kind=%v", m.picking, m.pickKind)
	}
	if cmd != nil {
		t.Error("opening the picker should not run a command")
	}
	if sel, _ := m.picker.Selected(); sel.Value != "2" {
		t.Errorf("the cursor should start on the current assignee (id 2), got %q", sel.Value)
	}
	if body := plain(m.View()); !strings.Contains(body, "Assign to") || !strings.Contains(body, "Unassigned") || !strings.Contains(body, "Hong") {
		t.Errorf("assignee picker not rendered:\n%s", body)
	}
}

func TestAssignPickerNotLoaded(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey}})
	m, cmd := m.Update(press("a"))
	if m.picking {
		t.Error("the picker should not open before members load")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Info {
		t.Errorf("expected an info toast, got %+v ok=%v", ts, ok)
	}
}

// Regression: without a detail-loaded guard the picker preselects Unassigned and a stray Enter unassigns.
func TestAssignPickerWaitsForDetail(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1})
	m, _ = m.Update(membersLoadedMsg{members: []domain.ProjectMember{{MemberID: 1, DisplayName: "Hong"}}})
	m, _ = m.Update(press("enter")) // opens the modal but the detail is still loading (skeleton)
	m, cmd := m.Update(press("a"))
	if m.picking {
		t.Error("the assignee picker must not open before the detail loads")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Info {
		t.Errorf("expected an info toast while the detail is loading, got %+v ok=%v", ts, ok)
	}
}

func TestAssignPickerSearchFilters(t *testing.T) {
	m := loaded(t, 120, 30, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1})
	m, _ = m.Update(membersLoadedMsg{members: []domain.ProjectMember{
		{MemberID: 1, DisplayName: "Hong"},
		{MemberID: 2, DisplayName: "Kim Younghee"},
		{MemberID: 3, DisplayName: "Kim Cheolsu"},
		{MemberID: 4, DisplayName: "Lee"},
	}})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey}})
	m, _ = m.Update(press("a"))
	if !m.picker.Searchable() {
		t.Fatal("the assignee picker should be searchable")
	}
	for _, k := range []string{"k", "i", "m"} {
		m, _ = m.Update(press(k))
	}
	if sel, _ := m.picker.Selected(); sel.Label != "Kim Younghee" {
		t.Errorf("filtering 'kim' should land on the first Kim, got %q", sel.Label)
	}
	if body := plain(m.View()); strings.Contains(body, "Hong") || strings.Contains(body, "Lee") {
		t.Errorf("filtered-out members should not render:\n%s", body)
	}
}

func TestAssignExecutes(t *testing.T) {
	m := assignReady(t)
	m, _ = m.Update(press("a"))
	m, _ = m.Update(press("up")) // move off the current assignee to another member
	m, cmd := m.Update(press("enter"))
	if m.picking {
		t.Error("selecting an assignee should close the picker")
	}
	if cmd == nil {
		t.Fatal("selecting a member should run an assign")
	}
}

func TestUnassignExecutes(t *testing.T) {
	m := assignReady(t)
	m, _ = m.Update(press("a"))
	// cursor starts on member 2 (index 2), so go up twice to the Unassigned row (index 0)
	m, _ = m.Update(press("up"))
	m, _ = m.Update(press("up"))
	sel, _ := m.picker.Selected()
	if sel.Value != "" {
		t.Fatalf("expected the Unassigned row, got %q", sel.Value)
	}
	_, cmd := m.Update(press("enter"))
	if cmd == nil {
		t.Fatal("selecting Unassigned should run an unassign")
	}
}

func TestAssignDoneRefetches(t *testing.T) {
	m := assignReady(t)
	m, cmd := m.Update(AssignDoneMsg{key: m.viewKey, assignee: "Hong"})
	if _, cached := m.details[m.viewKey]; !cached {
		t.Error("an assign should keep the cached detail (SWR) so the modal does not flash a skeleton")
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("a background refetch should be pending after an assign")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Success || !strings.Contains(ts.Text, "Hong") {
		t.Errorf("expected a success toast naming the assignee, got %+v ok=%v", ts, ok)
	}
}

func TestAssignIsOptimistic(t *testing.T) {
	m := assignReady(t) // ENG-1 assigned to Kim (id 2)
	m, _ = m.Update(press("a"))
	m, _ = m.Update(press("up")) // cursor Kim(index 2) -> Hong(index 1)
	m, _ = m.Update(press("enter"))
	if got := m.details[m.viewKey].AssigneeName; got != "Hong" {
		t.Errorf("assign should optimistically update the cached detail, got %q", got)
	}
	if got := m.issues[0].AssigneeName; got != "Hong" || !m.issues[0].Assigned {
		t.Errorf("assign should optimistically patch the list row, got %q assigned=%v", got, m.issues[0].Assigned)
	}
}

// A failed assign evicts the optimistic value so a fabricated assignee is never left on screen.
func TestAssignErrorEvictsOptimistic(t *testing.T) {
	m := assignReady(t) // ENG-1 assigned to Kim (id 2)
	m, _ = m.Update(press("a"))
	m, _ = m.Update(press("up")) // Kim -> Hong
	m, _ = m.Update(press("enter"))
	if m.details[m.viewKey].AssigneeName != "Hong" {
		t.Fatal("precondition: the optimistic assignee should be Hong")
	}
	m, cmd := m.Update(AssignDoneMsg{key: m.viewKey, assignee: "Hong", err: true})
	if _, cached := m.details[m.viewKey]; cached {
		t.Error("a failed assign should evict the optimistic value (never persist fabricated data)")
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("a refetch should be pending to restore the true assignee")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Error {
		t.Errorf("expected an error toast, got %+v ok=%v", ts, ok)
	}
}

// An optimistic assign bumps the load generation so an in-flight refetch cannot clobber it.
func TestOptimisticBumpsGenGuard(t *testing.T) {
	m := assignReady(t)
	oldGen := m.detailGen[m.viewKey]
	m, _ = m.Update(press("a"))
	m, _ = m.Update(press("up")) // Kim -> Hong
	m, _ = m.Update(press("enter"))
	if m.detailGen[m.viewKey] == oldGen {
		t.Fatal("an optimistic assign should bump the detail generation")
	}
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: oldGen, detail: domain.IssueDetail{Key: m.viewKey, AssigneeName: "STALE"}})
	if got := m.details[m.viewKey].AssigneeName; got != "Hong" {
		t.Errorf("a stale (old-gen) refetch must not clobber the optimistic assignee, got %q", got)
	}
}

func TestUnassignDoneToast(t *testing.T) {
	m := assignReady(t)
	m, cmd := m.Update(AssignDoneMsg{key: m.viewKey, assignee: ""})
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Success || !strings.Contains(ts.Text, "Unassigned") {
		t.Errorf("expected an Unassigned success toast, got %+v ok=%v", ts, ok)
	}
}

func TestAssignDoneError(t *testing.T) {
	m := assignReady(t)
	m, cmd := m.Update(AssignDoneMsg{key: m.viewKey, err: true})
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Error {
		t.Errorf("expected an error toast, got %+v ok=%v", ts, ok)
	}
}

func TestProjectMemberName(t *testing.T) {
	if got := (domain.ProjectMember{DisplayName: "Alice", Username: "a"}).Name(); got != "Alice" {
		t.Errorf("Name should prefer the display name, got %q", got)
	}
	if got := (domain.ProjectMember{Username: "bob"}).Name(); got != "bob" {
		t.Errorf("Name should fall back to the username, got %q", got)
	}
}

func TestProjectMemberOwner(t *testing.T) {
	if got := (domain.ProjectMember{OwnerName: "Gildong", OwnerUser: "gildong"}).Owner(); got != "Gildong" {
		t.Errorf("Owner should prefer the owner's name, got %q", got)
	}
	if got := (domain.ProjectMember{OwnerUser: "gildong"}).Owner(); got != "gildong" {
		t.Errorf("Owner should fall back to the owner's username, got %q", got)
	}
	if got := (domain.ProjectMember{}).Owner(); got != "" {
		t.Errorf("a human has no owner, got %q", got)
	}
}
