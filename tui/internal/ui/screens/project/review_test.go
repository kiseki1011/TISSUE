package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// reviewReady loads TIS-1 with the caller's own review state set to myStatus ("" = not a reviewer) and
// the given reviewer roster, with the Details panel open on it.
func reviewReady(t *testing.T, myStatus string, reviewers ...domain.Reviewer) Model {
	t.Helper()
	m := loaded(t, 120, 40, domain.IssuePage{
		Issues: []domain.IssueSummary{
			{Key: "TIS-1", Title: "Login flow", StateCategory: "ACTIVE", MyReviewStatus: myStatus},
		},
		TotalElements: 1,
	})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Login flow", Reviewers: reviewers,
	}})
	return m
}

func TestReviewOpensForAReviewer(t *testing.T) {
	m := reviewReady(t, "PENDING", domain.Reviewer{MemberID: 2, Name: "Kim", Status: "PENDING"})
	m, _ = m.Update(press("V"))

	if !m.reviewing {
		t.Fatal("V should open the review modal for a reviewer")
	}
	if !m.reviewUI.canReview {
		t.Error("a reviewer should be offered a verdict")
	}
	if len(m.reviewUI.rerequestIDs) != 0 {
		t.Errorf("nobody has responded yet, so there is nothing to re-request: %v", m.reviewUI.rerequestIDs)
	}
}

// Someone with no part in the review has nothing to do in the modal, so it is refused rather than opened
// empty.
func TestReviewRefusedWhenUninvolved(t *testing.T) {
	m := reviewReady(t, "", domain.Reviewer{MemberID: 2, Name: "Kim", Status: "PENDING"})
	m, _ = m.Update(press("V"))

	if m.reviewing {
		t.Error("V should refuse when the caller is neither a reviewer nor able to re-request")
	}
}

// A non-reviewer may still ask reviewers who already responded to look again, so the modal opens with
// only that half.
func TestReviewOpensForRerequestAlone(t *testing.T) {
	m := reviewReady(t, "",
		domain.Reviewer{MemberID: 2, Name: "Kim", Status: "APPROVED"},
		domain.Reviewer{MemberID: 3, Name: "Lee", Status: "PENDING"},
	)
	m, _ = m.Update(press("V"))

	if !m.reviewing {
		t.Fatal("a responded reviewer should make the modal worth opening")
	}
	if m.reviewUI.canReview {
		t.Error("a non-reviewer must not be offered a verdict")
	}
	if got := m.reviewUI.rerequestIDs; len(got) != 1 || got[0] != 2 {
		t.Errorf("re-request should target only the reviewer who responded, got %v", got)
	}
	if fs := m.reviewUI.fields(); indexOfInt(fs, rvSubmit) >= 0 {
		t.Errorf("the tab ring should not contain Submit without a verdict to submit: %v", fs)
	}
}

// A PENDING reviewer has nothing to reset, and re-requesting would notify them for no change.
func TestRespondedReviewersSkipsPending(t *testing.T) {
	ids, names := respondedReviewers([]domain.Reviewer{
		{MemberID: 1, Name: "A", Status: "PENDING"},
		{MemberID: 2, Name: "B", Status: "APPROVED"},
		{MemberID: 3, Name: "C", Status: "CHANGES_REQUESTED"},
	})
	if len(ids) != 2 || ids[0] != 2 || ids[1] != 3 {
		t.Errorf("ids = %v, want [2 3]", ids)
	}
	if len(names) != 2 || names[0] != "B" {
		t.Errorf("names = %v, want [B C]", names)
	}
}

func TestReviewVerdictDefaultsToApproveAndToggles(t *testing.T) {
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(press("V"))

	if !m.reviewUI.approved {
		t.Error("the modal should open on Approve")
	}
	m, _ = m.Update(press("right"))
	if m.reviewUI.approved {
		t.Error("right should move the verdict to Request changes")
	}
	if m.reviewUI.focus != rvReject {
		t.Errorf("focus should follow the picked verdict, got %d", m.reviewUI.focus)
	}
	m, _ = m.Update(press("left"))
	if !m.reviewUI.approved {
		t.Error("left should move the verdict back to Approve")
	}
}

func TestReviewSubmitCarriesTheVerdictAndFeedback(t *testing.T) {
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(press("V"))
	m, _ = m.Update(press("right")) // request changes
	m.reviewUI.focus = rvFeedback
	m.reviewUI.feedback.SetValue("  rename the method  ")

	f, cmd := m.reviewUI.submit()
	if !f.sending {
		t.Error("submitting should mark the form in flight so a double enter cannot fire twice")
	}
	msg, ok := cmd().(reviewSubmittedMsg)
	if !ok {
		t.Fatalf("submit should emit reviewSubmittedMsg, got %T", cmd())
	}
	if msg.approved {
		t.Error("the picked verdict was Request changes")
	}
	if msg.comment != "rename the method" {
		t.Errorf("feedback should be trimmed, got %q", msg.comment)
	}
}

// Whitespace-only feedback is not feedback: it must not become an empty comment on the issue.
func TestReviewSubmitDropsBlankFeedback(t *testing.T) {
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(press("V"))
	m.reviewUI.feedback.SetValue("   \n  ")

	_, cmd := m.reviewUI.submit()
	if msg := cmd().(reviewSubmittedMsg); msg.comment != "" {
		t.Errorf("blank feedback should submit no comment, got %q", msg.comment)
	}
}

// A failure keeps the modal up with the reason beside the controls that produced it, so the user can fix
// and retry instead of losing what they typed to a toast that scrolls away.
func TestReviewFailureKeepsTheModalOpen(t *testing.T) {
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(press("V"))
	m.reviewUI.sending = true
	m, _ = m.Update(ReviewDoneMsg{key: "TIS-1", err: true, errText: "You are not a reviewer."})

	if !m.reviewing {
		t.Fatal("a failed review should leave the modal open")
	}
	if m.reviewUI.sending {
		t.Error("the form should be writable again after a failure")
	}
	if m.reviewUI.status != "You are not a reviewer." {
		t.Errorf("status = %q, want the server reason", m.reviewUI.status)
	}
}

func TestReviewSuccessClosesTheModal(t *testing.T) {
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(press("V"))
	m, _ = m.Update(ReviewDoneMsg{key: "TIS-1", text: "Review submitted: approved."})

	if m.reviewing {
		t.Error("a successful review should close the modal")
	}
}

// The verdict reads back on the comment itself, so the thread shows who decided what without expanding
// the reviewer roster.
func TestReviewCommentShowsItsVerdict(t *testing.T) {
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Login flow", CommentCount: 2,
		Comments: []domain.IssueComment{
			{ID: 1, AuthorName: "Kim", Content: "rename it", ReviewStatus: "CHANGES_REQUESTED"},
			{ID: 2, AuthorName: "Lee", Content: "sure"},
		},
	}})

	view := plain(m.View())
	if !strings.Contains(view, "Changes requested") {
		t.Errorf("the review comment should be labelled with its verdict:\n%s", view)
	}
	if strings.Count(view, "Changes requested") != 1 {
		t.Error("the ordinary comment should carry no verdict label")
	}
}

func TestReviewBadgeIsColouredByVerdict(t *testing.T) {
	m := reviewReady(t, "PENDING")
	t.Setenv("CLICOLOR_FORCE", "1")

	approved := m.reviewBadge("APPROVED")
	changes := m.reviewBadge("CHANGES_REQUESTED")
	if approved == changes {
		t.Error("approve and request-changes must not render identically")
	}
	if m.reviewBadge("") != "" {
		t.Error("an ordinary comment gets no badge")
	}
	if !strings.Contains(plain(approved), "Approved") || !strings.Contains(plain(changes), "Changes requested") {
		t.Errorf("badges should carry their label, got %q / %q", plain(approved), plain(changes))
	}
}

// tab can move focus off the selected verdict, so space commits whichever button is focused rather than
// flipping the selection.
func TestReviewSpaceCommitsTheFocusedVerdict(t *testing.T) {
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(press("V"))
	m, _ = m.Update(press("tab")) // Approve -> Request changes, selection untouched

	if m.reviewUI.focus != rvReject || !m.reviewUI.approved {
		t.Fatalf("setup: focus=%d approved=%v, want focus on Reject with Approve still selected",
			m.reviewUI.focus, m.reviewUI.approved)
	}
	m, _ = m.Update(press("space"))
	if m.reviewUI.approved {
		t.Error("space should commit the focused button (Request changes)")
	}
}

// A review status narrows the reviewer axis and is dropped server-side without one, so checking a status
// turns the reviewer row on rather than leaving a filter that quietly does nothing.
// filterOpen returns a model with the filter modal open. The filter key only fires from the list focus,
// so the Details panel reviewReady leaves focused is stepped out of first.
func filterOpen(t *testing.T) Model {
	t.Helper()
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(press("esc"))
	m, _ = m.Update(press("f"))
	if !m.filtering {
		t.Fatal("f should open the filter modal from the list")
	}
	return m
}

func TestFilterReviewStatusImpliesTheReviewer(t *testing.T) {
	m := filterOpen(t)

	i := m.filterUI.indexOfKind(kindReviewStatus)
	if i < 0 {
		t.Fatal("the filter should offer review statuses")
	}
	f, _ := m.filterUI.activate(i)
	if !f.reviewerMe {
		t.Error("checking a review status should turn on the reviewer row it narrows")
	}
	if !f.reviewSel[f.items[i].value] {
		t.Error("the status itself should be checked")
	}
}

func TestFilterClearingTheReviewerClearsItsStatuses(t *testing.T) {
	m := filterOpen(t)

	statusIdx := m.filterUI.indexOfKind(kindReviewStatus)
	f, _ := m.filterUI.activate(statusIdx) // implies reviewerMe
	f, _ = f.activate(f.indexOfKind(kindReviewer))

	if f.reviewerMe {
		t.Fatal("the reviewer row should have toggled off")
	}
	if len(f.reviewSel) != 0 {
		for k, v := range f.reviewSel {
			if v {
				t.Errorf("status %q survived clearing the reviewer row", k)
			}
		}
	}
}

func TestFilterAppliesTheReviewAxes(t *testing.T) {
	m := filterOpen(t)

	f, _ := m.filterUI.activate(m.filterUI.indexOfKind(kindReviewStatus)) // PENDING, implying reviewerMe
	m.filterUI = f
	m, _ = m.Update(f.apply()())

	if !m.filter.ReviewerMe {
		t.Error("the applied filter should carry the reviewer axis")
	}
	if got := m.filter.ReviewerStatuses; len(got) != 1 || got[0] != "PENDING" {
		t.Errorf("ReviewerStatuses = %v, want [PENDING]", got)
	}
}

// The feedback box is labelled "Feedback" under a "Your verdict" heading, so the placeholder only has to
// say the field may be left empty. Exact equality: the old sentence began with "Optional" too.
func TestReviewFeedbackPlaceholderIsBare(t *testing.T) {
	m := reviewReady(t, "PENDING")
	m, _ = m.Update(press("V"))

	if got := m.reviewUI.feedback.Placeholder; got != "Optional" {
		t.Errorf("the feedback placeholder should be the bare word, got %q", got)
	}
	if strings.Contains(plain(m.View()), "stamped") {
		t.Error("the explanatory placeholder tail should not reach the rendered modal")
	}
}
