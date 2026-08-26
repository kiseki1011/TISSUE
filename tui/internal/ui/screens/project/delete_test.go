package project

import (
	"net/http"
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// twoIssuesOpen loads a two-issue page and opens the detail modal on the first (ENG-1).
func twoIssuesOpen(t *testing.T) Model {
	t.Helper()
	m := loaded(t, 120, 40, domain.IssuePage{
		Issues:        []domain.IssueSummary{{Key: "ENG-1", Title: "First", StateCategory: "ACTIVE"}, {Key: "ENG-2", Title: "Second", StateCategory: "ACTIVE"}},
		TotalElements: 2,
	})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "First"}})
	return m
}

func TestDeleteOpensConfirm(t *testing.T) {
	m := twoIssuesOpen(t)
	m, cmd := m.Update(press("d"))
	if !m.deleting || m.deleteKey != "ENG-1" {
		t.Fatalf("d should open the delete confirmation for ENG-1, got deleting=%v key=%q", m.deleting, m.deleteKey)
	}
	_ = cmd
	if body := plain(m.View()); !strings.Contains(body, "Delete issue") || !strings.Contains(body, "First") {
		t.Errorf("delete dialog not rendered:\n%s", body)
	}
}

// The dialog defaults focus to Cancel, so a stray enter cancels rather than deleting.
func TestDeleteStrayEnterCancels(t *testing.T) {
	m := twoIssuesOpen(t)
	m, _ = m.Update(press("d"))
	m, cmd := m.Update(press("enter"))
	m, _ = m.Update(cmd())
	if m.deleting {
		t.Error("a stray enter should cancel the delete, not run it")
	}
	if len(m.issues) != 2 {
		t.Errorf("cancelling must not remove any issue, got %d", len(m.issues))
	}
}

// The dialog stays open until the result lands.
func TestDeleteAcceptRunsDelete(t *testing.T) {
	m := twoIssuesOpen(t)
	m, _ = m.Update(press("d"))
	m, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if !m.deleting {
		t.Error("the dialog should stay open while the delete runs")
	}
	if cmd == nil {
		t.Fatal("accepting should run the delete")
	}
}

func TestDeleteSuccessRemovesRow(t *testing.T) {
	m := twoIssuesOpen(t)
	m, _ = m.Update(press("d"))
	m, cmd := m.Update(IssueDeletedMsg{key: "ENG-1"})
	if m.deleting || m.focus == focusDetail {
		t.Error("a successful delete should close both the dialog and the detail modal")
	}
	if len(m.issues) != 1 || m.issues[0].Key != "ENG-2" {
		t.Errorf("the deleted row should be removed, got %+v", m.issues)
	}
	if m.page.TotalElements != 1 {
		t.Errorf("the total should be decremented, got %d", m.page.TotalElements)
	}
	if _, cached := m.details["ENG-1"]; cached {
		t.Error("the deleted issue's cached detail should be evicted")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Success || !strings.Contains(ts.Text, "deleted") {
		t.Errorf("expected a success toast, got %+v ok=%v", ts, ok)
	}
}

// A late in-flight detail load must not resurrect the evicted cache.
func TestDeleteBumpsDetailGen(t *testing.T) {
	m := twoIssuesOpen(t)
	gen := m.detailGen["ENG-1"]
	m, _ = m.Update(press("d"))
	m, _ = m.Update(IssueDeletedMsg{key: "ENG-1"})
	m, _ = m.Update(IssueDetailLoadedMsg{key: "ENG-1", gen: gen, detail: domain.IssueDetail{Key: "ENG-1", Title: "GHOST"}})
	if _, cached := m.details["ENG-1"]; cached {
		t.Error("a late detail load must not resurrect a deleted issue's cache")
	}
}

// Deleting the last loaded row while more pages exist reloads, so the list is not stranded empty.
func TestDeleteEmptyReloadsNextPage(t *testing.T) {
	m := loaded(t, 120, 40, domain.IssuePage{
		Issues:        []domain.IssueSummary{{Key: "ENG-1", Title: "only", StateCategory: "ACTIVE"}},
		TotalElements: 5, HasNext: true,
	})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey}})
	m, _ = m.Update(press("d"))
	m, cmd := m.Update(IssueDeletedMsg{key: "ENG-1"})
	if len(m.issues) != 0 {
		t.Fatalf("the last row should be removed, got %d", len(m.issues))
	}
	if !m.loading {
		t.Error("deleting the last loaded row while more pages exist should trigger a reload")
	}
	if cmd == nil {
		t.Fatal("expected a reload command after emptying the page")
	}
}

func TestDeleteErrorStaysOpen(t *testing.T) {
	m := twoIssuesOpen(t)
	m, _ = m.Update(press("d"))
	m, _ = m.Update(IssueDeletedMsg{key: "ENG-1", err: true, message: "This issue cannot be deleted (it may have sub-issues)."})
	if !m.deleting {
		t.Error("a failed delete should keep the dialog open")
	}
	if m.deleteUI.Submitting {
		t.Error("a failed delete should clear the submitting state")
	}
	if !strings.Contains(m.deleteUI.Status, "sub-issues") {
		t.Errorf("the dialog should show the failure reason, got %q", m.deleteUI.Status)
	}
	if len(m.issues) != 2 {
		t.Errorf("a failed delete must not remove the row, got %d", len(m.issues))
	}
}

func TestRemoveIssueClampsCursor(t *testing.T) {
	m := loaded(t, 120, 40, domain.IssuePage{
		Issues:        []domain.IssueSummary{{Key: "A"}, {Key: "B"}, {Key: "C"}},
		TotalElements: 3,
	})
	m.cursor = 2 // on the last row
	m.removeIssue("C")
	if len(m.issues) != 2 {
		t.Fatalf("expected 2 rows after removal, got %d", len(m.issues))
	}
	if m.cursor != 1 {
		t.Errorf("cursor should clamp to the new last row, got %d", m.cursor)
	}
}

func TestDeleteErrorMessage(t *testing.T) {
	cases := map[int]string{
		http.StatusForbidden: "permission",
		http.StatusConflict:  "sub-issues",
		http.StatusNotFound:  "no longer exists",
	}
	for status, want := range cases {
		if got := deleteErrorMessage(&domain.APIError{Status: status}); !strings.Contains(got, want) {
			t.Errorf("status %d: message %q should contain %q", status, got, want)
		}
	}
	if got := deleteErrorMessage(&domain.APIError{Status: 500}); !strings.Contains(got, "Try again") {
		t.Errorf("an unmapped status should fall back to a generic message, got %q", got)
	}
}

// The server soft-deletes, so the dialog must not claim the issue is gone for good.
func TestDeleteConfirmDoesNotClaimPermanence(t *testing.T) {
	m := detailWith(t, nil)
	m, _ = m.Update(press("d"))

	body := plain(m.View())
	for _, lie := range []string{"permanently", "cannot be undone"} {
		if strings.Contains(body, lie) {
			t.Errorf("the delete dialog should not say %q - the server soft-deletes:\n%s", lie, body)
		}
	}
	if !strings.Contains(body, "trash") || !strings.Contains(body, "restored") {
		t.Errorf("the dialog should say where the issue goes and that it comes back:\n%s", body)
	}
}
