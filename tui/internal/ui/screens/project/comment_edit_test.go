package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// commentModel opens the comment modal on TIS-1 as viewer, with the given roster and thread.
func commentModel(t *testing.T, viewer string, members []domain.ProjectMember, thread domain.IssueDetail) Model {
	t.Helper()
	m := loaded(t, 120, 60, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "TIS-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m = m.WithViewer(viewer)
	m, _ = m.Update(membersLoadedMsg{members: members})
	m, _ = m.Update(press("enter"))
	thread.Key = m.viewKey
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: thread})
	m, _ = m.Update(press("c"))
	return m
}

var roster = []domain.ProjectMember{
	{MemberID: 1, DisplayName: "Kim", Username: "kim", Role: "MEMBER"},
	{MemberID: 2, DisplayName: "Lee", Username: "lee", Role: "MEMBER"},
	{MemberID: 3, DisplayName: "Boss", Username: "boss", Role: "MANAGER"},
}

func twoComments() domain.IssueDetail {
	return domain.IssueDetail{CommentCount: 2, Comments: []domain.IssueComment{
		{ID: 1, AuthorName: "Kim", AuthorUsername: "kim", Content: "mine"},
		{ID: 2, AuthorName: "Lee", AuthorUsername: "lee", Content: "theirs"},
	}}
}

func TestCommentEditableOnlyForTheAuthor(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	mine, theirs := m.details[m.viewKey].Comments[0], m.details[m.viewKey].Comments[1]

	if !m.commentEditable(mine) {
		t.Error("the caller should be able to edit their own comment")
	}
	if m.commentEditable(theirs) {
		t.Error("a plain member should not be able to edit someone else's comment")
	}
}

// A project manager moderates the thread, matching the server's rule.
func TestCommentEditableForAManager(t *testing.T) {
	m := commentModel(t, "boss", roster, twoComments())
	if !m.commentEditable(m.details[m.viewKey].Comments[0]) {
		t.Error("a manager should be able to edit any comment")
	}
}

// Without a profile there is no way to tell whose comment it is, so nothing is offered.
func TestCommentNotEditableBeforeTheProfileLands(t *testing.T) {
	m := commentModel(t, "", roster, twoComments())
	if m.commentEditable(m.details[m.viewKey].Comments[0]) {
		t.Error("an unknown viewer should get no edit affordance")
	}
}

func TestCommentNotEditableWhenDeletedOrPeeking(t *testing.T) {
	m := commentModel(t, "kim", roster, domain.IssueDetail{CommentCount: 1, Comments: []domain.IssueComment{
		{ID: 1, AuthorName: "Kim", AuthorUsername: "kim", Content: "mine", Deleted: true},
	}})
	if m.commentEditable(m.details[m.viewKey].Comments[0]) {
		t.Error("a tombstone has nothing left to edit")
	}

	live := commentModel(t, "kim", roster, twoComments())
	live.peeking = true
	if live.commentEditable(live.details[live.viewKey].Comments[0]) {
		t.Error("a peeked issue is read-only")
	}
}

func TestCommentModalShowsEditOnlyOnEditableComments(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	body, _, _ := m.commentModalBody()
	view := plain(body)

	if n := strings.Count(view, "Edit"); n != 1 {
		t.Errorf("only the caller's own comment should offer Edit, got %d:\n%s", n, view)
	}
	if n := strings.Count(view, "Delete"); n != 1 {
		t.Errorf("only the caller's own comment should offer Delete, got %d:\n%s", n, view)
	}
	if n := strings.Count(view, "Reply"); n != 2 {
		t.Errorf("both root comments should offer Reply, got %d", n)
	}
}

// A reply cannot be replied to (the server caps nesting at 1) but its author may still edit it.
func TestCommentRepliesOfferEditButNotReply(t *testing.T) {
	m := commentModel(t, "kim", roster, domain.IssueDetail{CommentCount: 2, Comments: []domain.IssueComment{
		{ID: 1, AuthorName: "Lee", AuthorUsername: "lee", Content: "root", Replies: []domain.IssueComment{
			{ID: 2, AuthorName: "Kim", AuthorUsername: "kim", Content: "my reply"},
		}},
	}})

	ring := m.commentFocusRing()
	if indexOfCommentFocus(ring, commentFocus{2, partEdit}) < 0 {
		t.Errorf("the reply's author should be able to edit it: %v", ring)
	}
	if indexOfCommentFocus(ring, commentFocus{2, partReply}) >= 0 {
		t.Errorf("a reply should carry no Reply affordance: %v", ring)
	}
	if indexOfCommentFocus(ring, commentFocus{1, partEdit}) >= 0 {
		t.Error("someone else's root comment should carry no Edit affordance")
	}
}

func TestOpenInlineEditPrefillsTheBody(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	m, _ = m.openInlineEdit(1)

	if m.commentUI.editingID != 1 {
		t.Fatalf("editingID = %d, want 1", m.commentUI.editingID)
	}
	if got := m.commentUI.edit.Value(); got != "mine" {
		t.Errorf("the composer should be prefilled with the comment body, got %q", got)
	}
	if m.commentUI.focus != (commentFocus{1, partEditText}) {
		t.Errorf("focus should land in the edit composer, got %+v", m.commentUI.focus)
	}
}

// Two open composers on one comment would be ambiguous, so opening an edit closes any open reply.
func TestOpenInlineEditClosesAnOpenReply(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	m, _ = m.openInlineReply(1)
	m, _ = m.openInlineEdit(1)

	if m.commentUI.replyingTo != 0 {
		t.Errorf("the reply composer should have closed, replyingTo = %d", m.commentUI.replyingTo)
	}
}

// Resending an unchanged body would stamp the comment "edited" for nothing.
func TestInlineEditSkipsAnUnchangedBody(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	m, _ = m.openInlineEdit(1)
	m.commentUI.edit.SetValue("  mine  ")

	m, _ = m.submitInlineEdit()
	if m.commentUI.sending != -1 {
		t.Error("an unchanged body should not be sent")
	}
	if m.commentUI.editingID != 0 {
		t.Error("the composer should close on a no-op save")
	}
}

func TestInlineEditRejectsAnEmptyBody(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	m, _ = m.openInlineEdit(1)
	m.commentUI.edit.SetValue("   ")

	m, _ = m.submitInlineEdit()
	if m.commentUI.editErr == "" {
		t.Error("an empty body should be rejected in the composer")
	}
	if m.commentUI.sending != -1 {
		t.Error("an empty body should not be sent")
	}
}

func TestInlineEditSendsAChangedBody(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	m, _ = m.openInlineEdit(1)
	m.commentUI.edit.SetValue("mine, fixed")

	m, cmd := m.submitInlineEdit()
	if cmd == nil {
		t.Fatal("a changed body should fire the update command")
	}
	if m.commentUI.sending != 1 {
		t.Errorf("sending = %d, want the edited comment 1 so a second enter cannot double-send", m.commentUI.sending)
	}

	m, _ = m.Update(CommentEditDoneMsg{key: "TIS-1", id: 1})
	if m.commentUI.editingID != 0 {
		t.Error("a successful edit should close the composer")
	}
	if !m.commenting {
		t.Error("the modal should stay open so the refreshed thread shows the change in place")
	}
}

// esc backs out of the edit composer rather than closing the whole modal.
func TestEscClosesTheEditComposerFirst(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	m, _ = m.openInlineEdit(1)
	m, _ = m.Update(press("esc"))

	if m.commentUI.editingID != 0 {
		t.Error("esc should close the edit composer")
	}
	if !m.commenting {
		t.Fatal("esc should not also close the modal")
	}
	m, cmd := m.Update(press("esc"))
	if cmd == nil {
		t.Fatal("a second esc should ask to close the modal")
	}
	m, _ = m.Update(cmd()) // the close is a command, so run it to reach the closed state
	if m.commenting {
		t.Error("a second esc should close the modal")
	}
}

func TestCommentDeleteConfirms(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	m, _ = m.openCommentDelete(1)

	if !m.commentDeleting || m.commentDeleteID != 1 {
		t.Fatalf("delete should open a confirmation (deleting=%v id=%d)", m.commentDeleting, m.commentDeleteID)
	}
	m, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if m.commentDeleting || cmd == nil {
		t.Error("accepting should close the confirmation and fire the delete")
	}

	again := commentModel(t, "kim", roster, twoComments())
	again, _ = again.openCommentDelete(1)
	again, _ = again.Update(widgets.ConfirmCancelledMsg{})
	if again.commentDeleting {
		t.Error("cancelling should close the confirmation")
	}
	if !again.commenting {
		t.Error("cancelling a delete should leave the comment modal open")
	}
}

func TestCommentDeleteRefusedOnSomeoneElses(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	m, _ = m.openCommentDelete(2) // Lee's

	if m.commentDeleting {
		t.Error("a plain member should not be able to delete someone else's comment")
	}
}

// The thread pages on from the BFF's embedded first page, so the next index follows the loaded roots.
func TestLoadMoreRequestsTheNextPage(t *testing.T) {
	full := make([]domain.IssueComment, domain.CommentPageSize)
	for i := range full {
		full[i] = domain.IssueComment{ID: int64(i + 1), AuthorName: "Kim", AuthorUsername: "kim", Content: "c"}
	}
	m := commentModel(t, "kim", roster, domain.IssueDetail{
		CommentCount: 25, CommentsHasMore: true, Comments: full,
	})

	if idx := indexOfCommentFocus(m.commentFocusRing(), commentFocus{moreCommentID, partMore}); idx < 0 {
		t.Fatal("a partially-loaded thread should offer a Load more control")
	}
	m, cmd := m.loadMoreComments()
	if !m.commentUI.loadingMore || cmd == nil {
		t.Fatalf("load more should mark itself in flight and fetch (loading=%v cmd=%v)", m.commentUI.loadingMore, cmd != nil)
	}

	m, _ = m.Update(commentPageLoadedMsg{key: "TIS-1", page: domain.IssueCommentPage{
		Comments: []domain.IssueComment{{ID: 99, AuthorName: "Lee", Content: "later"}},
		HasNext:  false,
		Total:    21,
	}})

	d := m.details["TIS-1"]
	if len(d.Comments) != domain.CommentPageSize+1 {
		t.Errorf("the page should be appended, got %d comments", len(d.Comments))
	}
	if d.Comments[len(d.Comments)-1].ID != 99 {
		t.Error("the fetched page should land at the end of the thread")
	}
	if d.CommentsHasMore {
		t.Error("the last page should clear the more flag")
	}
	if d.CommentCount != 21 {
		t.Errorf("CommentCount = %d, want the fresh total 21", d.CommentCount)
	}
	if m.commentUI.loadingMore {
		t.Error("the in-flight marker should clear when the page lands")
	}
}

// A thread that fits in one page offers nothing to load.
func TestLoadMoreAbsentWhenTheThreadIsComplete(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	if indexOfCommentFocus(m.commentFocusRing(), commentFocus{moreCommentID, partMore}) >= 0 {
		t.Error("a fully-loaded thread should carry no Load more control")
	}
	if _, cmd := m.loadMoreComments(); cmd != nil {
		t.Error("there is nothing more to load")
	}
}

// A page that lands after the user moved to another issue must not be grafted onto that issue's thread.
func TestLoadMoreIgnoresAStaleIssue(t *testing.T) {
	m := commentModel(t, "kim", roster, twoComments())
	before := len(m.details["TIS-1"].Comments)

	m, _ = m.Update(commentPageLoadedMsg{key: "OTHER-9", page: domain.IssueCommentPage{
		Comments: []domain.IssueComment{{ID: 99, Content: "wrong issue"}},
	}})

	if got := len(m.details["TIS-1"].Comments); got != before {
		t.Errorf("a page for another issue should be dropped, got %d comments (was %d)", got, before)
	}
}
