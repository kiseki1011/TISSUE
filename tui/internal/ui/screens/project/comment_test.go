package project

import (
	"strings"
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

func detailWithComments() domain.IssueDetail {
	d := sampleDetail()
	d.CommentCount = 2
	d.Comments = []domain.IssueComment{
		{ID: 1, AuthorName: "Hong", Content: "still repros", CreatedAt: time.Date(2026, 8, 8, 0, 0, 0, 0, time.UTC), Edited: true,
			Replies: []domain.IssueComment{{ID: 2, AuthorName: "Kim", Content: "on it"}}},
		{ID: 3, AuthorName: "bot", Deleted: true},
	}
	return d
}

// The detail modal renders the comment thread: count, author, content, a reply, and a deleted tombstone.
func TestCommentThreadRenders(t *testing.T) {
	m := editReady(t, detailWithComments())
	m.detailScroll = m.detailScrollMax() // scroll to the comments at the bottom
	body := plain(m.View())
	for _, want := range []string{"Comments (2)", "Hong", "still repros", "on it", "[deleted]"} {
		if !strings.Contains(body, want) {
			t.Errorf("comment thread missing %q:\n%s", want, body)
		}
	}
}

// Only root comments show a Reply link; replies (which thread only under a root) do not, and a deleted
// comment shows none. detailWithComments has one root with a reply plus a deleted root, so exactly one
// "Reply" affordance renders.
func TestReplyLinkOnlyOnRootComments(t *testing.T) {
	m := editReady(t, detailWithComments())
	m.detailScroll = m.detailScrollMax()
	body := plain(m.View())
	if !strings.Contains(body, "on it") {
		t.Fatalf("precondition: the reply should be visible in the scrolled view:\n%s", body)
	}
	if n := strings.Count(body, "Reply"); n != 1 {
		t.Errorf("expected exactly one Reply link (root comment only), got %d:\n%s", n, body)
	}
}

// A control character in an untrusted comment author name is flattened, not emitted into the frame
// (a stray CR would otherwise reset the cursor to column 0 and overwrite the modal border).
func TestCommentAuthorSanitized(t *testing.T) {
	d := sampleDetail()
	d.CommentCount = 1
	d.Comments = []domain.IssueComment{{ID: 1, AuthorName: "Bob\rEVIL", Content: "hi"}}
	m := editReady(t, d)
	m.detailScroll = m.detailScrollMax()
	if strings.ContainsRune(m.View(), '\r') {
		t.Error("a carriage return in a comment author must be flattened, not emitted into the frame")
	}
	if !strings.Contains(plain(m.View()), "Bob EVIL") {
		t.Errorf("the flattened author should render:\n%s", plain(m.View()))
	}
}

func TestCommentsEmptyPlaceholder(t *testing.T) {
	m := editReady(t, sampleDetail()) // no comments
	m.detailScroll = m.detailScrollMax()
	if body := plain(m.View()); !strings.Contains(body, "No comments yet.") {
		t.Errorf("an issue with no comments should show a placeholder:\n%s", body)
	}
}

// 'c' opens the composer, and it works even before the detail body has loaded (a comment needs only the key).
func TestCommentComposerOpensDuringSkeleton(t *testing.T) {
	m := loaded(t, 120, 44, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1})
	m, _ = m.Update(press("enter")) // modal open, detail still loading
	m, cmd := m.Update(press("c"))
	if !m.commenting {
		t.Fatal("c should open the comment modal")
	}
	if cmd == nil {
		t.Error("opening the modal should start the input blink")
	}
	if body := plain(m.View()); !strings.Contains(body, "Comments") {
		t.Errorf("comment modal not rendered:\n%s", body)
	}
}

// The comment modal shows the existing thread (with reply buttons) and the persistent bottom composer.
func TestCommentModalShowsThreadAndComposers(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	if !m.commenting {
		t.Fatal("c should open the comment modal")
	}
	view, _, _ := m.commentModalView() // the modal in isolation, not the dimmed backdrop
	body := plain(view)
	for _, want := range []string{"Comments", "still repros", "Reply", "Add a comment"} {
		if !strings.Contains(body, want) {
			t.Errorf("the modal should show the thread + reply buttons + root composer (%q):\n%s", want, body)
		}
	}
}

// A root comment's Reply button is keyboard-focusable; Enter on it opens an inline reply composer under
// that comment, and submitting threads the reply under it. A deleted comment carries no Reply button.
func TestCommentModalInlineReply(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	// the focus ring offers one reply button (comment 1; comment 3 is deleted) plus the root composer
	if got := len(m.commentFocusRing()); got != 4 { // {1,reply} + root {text,submit,cancel}
		t.Fatalf("expected 1 reply button + the root composer's 3 controls, got %d focus targets", got)
	}
	m, _ = m.focusComment(commentFocus{1, partReply})
	m, _ = m.Update(press("enter")) // open the inline composer under comment 1
	if m.commentUI.replyingTo != 1 {
		t.Fatalf("enter on the reply button should open the inline composer, replyingTo=%d", m.commentUI.replyingTo)
	}
	if m.commentUI.focus != (commentFocus{1, partText}) {
		t.Errorf("focus should move into the inline composer, got %+v", m.commentUI.focus)
	}
	if view, _, _ := m.commentModalView(); !strings.Contains(plain(view), "Reply to Hong") {
		t.Errorf("the inline composer should be labeled with the parent author:\n%s", plain(view))
	}
	m.commentUI.reply.SetValue("on it too")
	m, cmd := m.submitComposer(1)
	if cmd == nil {
		t.Fatal("submitting the inline reply should emit")
	}
	if sub, ok := cmd().(commentSubmittedMsg); !ok || sub.parentID != 1 || sub.content != "on it too" {
		t.Errorf("the inline reply should thread under comment 1, got %#v", cmd())
	}
}

// The modal's per-comment Reply button is mouse-clickable: its bubblezone survives the width-fold and
// the floated overlay, and clicking it opens that comment's inline composer.
func TestCommentModalReplyButtonClickable(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m, _ = m.Update(clickZone(t, m, commentZoneID(commentFocus{1, partReply})))
	if m.commentUI.replyingTo != 1 {
		t.Errorf("clicking a modal Reply button should open its inline composer, replyingTo=%d", m.commentUI.replyingTo)
	}
	if m.commentUI.focus != (commentFocus{1, partText}) {
		t.Errorf("clicking Reply should focus the inline composer, got %+v", m.commentUI.focus)
	}
}

// A comment mapped to ID 0 (the server omitted its commentId) must not open a phantom inline composer.
// Without the c.ID != 0 guard, replyingTo's 0 sentinel matches it and renders the uninitialized reply
// textarea, whose View() panics. Regression for the review's HIGH finding.
func TestCommentModalToleratesZeroIDComment(t *testing.T) {
	d := sampleDetail()
	d.CommentCount = 1
	d.Comments = []domain.IssueComment{{ID: 0, AuthorName: "ghost", Content: "no id"}}
	m := editReady(t, d)
	m, _ = m.Update(press("c")) // opening renders the modal; must not panic on the ID==0 comment
	if !m.commenting {
		t.Fatal("c should open the modal")
	}
	if view, _, _ := m.commentModalView(); plain(view) == "" { // force a full render; must not panic
		t.Fatal("the modal should render")
	}
	if m.commentUI.replyingTo != 0 {
		t.Errorf("an ID==0 comment must not open a phantom inline composer, replyingTo=%d", m.commentUI.replyingTo)
	}
}

// The inline composer renders only for a live (non-deleted, real-id) reply target, matching the focus
// ring — so a reply target tombstoned by a live refetch drops its composer instead of stranding a
// visible-but-unreachable one. Regression for the review's dead-click finding.
func TestCommentModalInlineComposerGatedToLiveTarget(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m, _ = m.openInlineReply(1)
	if view, _, _ := m.commentModalView(); !strings.Contains(plain(view), "Reply to Hong") {
		t.Fatal("precondition: the inline composer should render for the live target")
	}
	// simulate a background refetch tombstoning the replied-to comment
	dd := m.details[m.viewKey]
	dd.Comments = []domain.IssueComment{{ID: 1, AuthorName: "Hong", Content: "x", Deleted: true}}
	m.details[m.viewKey] = dd
	if view, _, _ := m.commentModalView(); strings.Contains(plain(view), "Reply to Hong") {
		t.Errorf("a deleted reply target must not keep its inline composer rendered:\n%s", plain(view))
	}
	for _, tok := range m.commentFocusRing() {
		if tok.id == 1 {
			t.Errorf("a deleted comment must not appear in the focus ring, got %+v", tok)
		}
	}
}

// esc on an open inline composer closes just that composer (back to its Reply button), not the modal.
func TestCommentModalEscClosesInlineReplyFirst(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m, _ = m.openInlineReply(1)
	m, _ = m.Update(press("esc"))
	if !m.commenting {
		t.Error("esc on an open inline composer should not close the whole modal")
	}
	if m.commentUI.replyingTo != 0 {
		t.Errorf("esc should close the inline composer, replyingTo=%d", m.commentUI.replyingTo)
	}
	if m.commentUI.focus != (commentFocus{1, partReply}) {
		t.Errorf("closing the inline composer should return focus to its Reply button, got %+v", m.commentUI.focus)
	}
}

// Submitting a comment runs the create and, stay-open, keeps the modal up with the in-flight submit
// marked so its result can clear the right composer.
func TestCommentSubmitCreates(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m, cmd := m.Update(commentSubmittedMsg{content: "looks good"}) // parentID 0 (root)
	if !m.commenting {
		t.Error("stay-open: submitting keeps the modal open")
	}
	if m.commentUI.sending != 0 {
		t.Errorf("the in-flight root submit should be marked (sending=0), got %d", m.commentUI.sending)
	}
	if cmd == nil {
		t.Fatal("submitting a comment should run the create")
	}
}

// stay-open: a successful root comment clears the bottom composer, resets the in-flight marker, keeps
// the modal open, and refetches so the new comment lands in the live thread.
func TestCommentStayOpenClearsRootOnSuccess(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m.commentUI.root.SetValue("looks good")
	m, _ = m.Update(commentSubmittedMsg{content: "looks good", parentID: 0})
	m, cmd := m.Update(CommentDoneMsg{key: m.viewKey}) // the create lands
	if !m.commenting {
		t.Error("the modal should stay open after a successful comment")
	}
	if v := m.commentUI.root.Value(); v != "" {
		t.Errorf("a posted root comment should clear the composer, got %q", v)
	}
	if m.commentUI.sending != -1 {
		t.Errorf("the in-flight marker should reset, got %d", m.commentUI.sending)
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("a refetch should be pending to pull the new comment into the live thread")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Success {
		t.Errorf("expected a success toast, got %+v ok=%v", ts, ok)
	}
}

// stay-open: a successful reply closes its inline composer, focuses the parent's Reply button, and
// refetches - the modal stays open.
func TestCommentStayOpenReplyClosesInlineOnSuccess(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m, _ = m.openInlineReply(1)
	m, _ = m.Update(commentSubmittedMsg{content: "on it", parentID: 1})
	if m.commentUI.sending != 1 {
		t.Fatalf("the reply submit should be in flight, sending=%d", m.commentUI.sending)
	}
	m, _ = m.Update(CommentDoneMsg{key: m.viewKey})
	if !m.commenting {
		t.Error("the modal should stay open after a successful reply")
	}
	if m.commentUI.replyingTo != 0 {
		t.Errorf("a posted reply should close the inline composer, replyingTo=%d", m.commentUI.replyingTo)
	}
	if m.commentUI.focus != (commentFocus{1, partReply}) {
		t.Errorf("closing the inline composer should focus its Reply button, got %+v", m.commentUI.focus)
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("a refetch should be pending after a reply")
	}
}

// stay-open: if the user opens an inline reply while a root submit is in flight, the root success clears
// the (posted) root composer but must NOT yank focus out of the inline reply they are mid-typing.
func TestCommentStayOpenRootSuccessKeepsInlineReplyFocus(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m.commentUI.root.SetValue("root draft")
	m, _ = m.Update(commentSubmittedMsg{content: "root draft", parentID: 0}) // root in flight
	m, _ = m.openInlineReply(1)                                              // user opens a reply during the send
	m.commentUI.reply.SetValue("mid-typing")
	m, _ = m.Update(CommentDoneMsg{key: m.viewKey}) // the root create completes
	if m.commentUI.focus != (commentFocus{1, partText}) {
		t.Errorf("a root success must not steal focus from an open inline reply, got %+v", m.commentUI.focus)
	}
	if m.commentUI.replyingTo != 1 {
		t.Errorf("the inline reply should stay open, replyingTo=%d", m.commentUI.replyingTo)
	}
	if m.commentUI.reply.Value() != "mid-typing" {
		t.Errorf("the in-progress reply text must be preserved, got %q", m.commentUI.reply.Value())
	}
	if m.commentUI.root.Value() != "" {
		t.Errorf("the posted root content should still be cleared, got %q", m.commentUI.root.Value())
	}
}

// stay-open: a failed comment keeps the modal open AND preserves the typed text for a retry.
func TestCommentStayOpenErrorKeepsText(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m.commentUI.root.SetValue("draft")
	m, _ = m.Update(commentSubmittedMsg{content: "draft", parentID: 0})
	m, cmd := m.Update(CommentDoneMsg{key: m.viewKey, err: true})
	if !m.commenting {
		t.Error("the modal should stay open after a failed comment")
	}
	if v := m.commentUI.root.Value(); v != "draft" {
		t.Errorf("a failed comment should keep the typed text for a retry, got %q", v)
	}
	if m.commentUI.sending != -1 {
		t.Errorf("the in-flight marker should reset even on failure, got %d", m.commentUI.sending)
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Error {
		t.Errorf("expected an error toast, got %+v ok=%v", ts, ok)
	}
}

// submitComposer marks the submit in flight SYNCHRONOUSLY (not only when commentSubmittedMsg lands), so
// a rapid second Enter before the first result cannot double-post.
func TestCommentDoubleSubmitGuarded(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m.commentUI.root.SetValue("hi")
	m, cmd1 := m.submitComposer(0)
	if cmd1 == nil {
		t.Fatal("the first submit should fire the create")
	}
	if m.commentUI.sending != 0 {
		t.Errorf("submit should mark in-flight synchronously, got sending=%d", m.commentUI.sending)
	}
	if _, cmd2 := m.submitComposer(0); cmd2 != nil {
		t.Error("a rapid second submit while one is in flight must be ignored")
	}
}

// An empty root comment keeps the modal open with an error rather than sending it.
func TestCommentEmptyValidation(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c")) // focus starts on the root composer
	m.commentUI.root.SetValue("   ")
	m, _ = m.submitComposer(0)
	if m.commentUI.rootErr == "" {
		t.Error("an empty comment should raise an error rather than submit")
	}
	if m.commentUI.focus != (commentFocus{0, partText}) {
		t.Errorf("an empty submit should keep focus on the composer, got %+v", m.commentUI.focus)
	}
	if !m.commenting {
		t.Error("an empty submit must not close the modal")
	}
}

func TestCommentDoneRefetches(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, cmd := m.Update(CommentDoneMsg{key: m.viewKey})
	if _, cached := m.details[m.viewKey]; !cached {
		t.Error("a successful comment should keep the cached detail (SWR) so the modal does not flash a skeleton")
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("a background refetch should be pending to pull in the new comment")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Success || !strings.Contains(ts.Text, "Comment") {
		t.Errorf("expected a success toast, got %+v ok=%v", ts, ok)
	}
}

// A failed comment does not evict the detail (a comment is additive - there is nothing to roll back).
func TestCommentDoneError(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, cmd := m.Update(CommentDoneMsg{key: m.viewKey, err: true})
	if _, cached := m.details[m.viewKey]; !cached {
		t.Error("a failed comment must not evict the detail")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Error {
		t.Errorf("expected an error toast, got %+v ok=%v", ts, ok)
	}
}

// esc from the composer returns to the detail modal without closing it.
func TestCommentCancel(t *testing.T) {
	m := editReady(t, detailWithComments())
	m, _ = m.Update(press("c"))
	m, _ = m.Update(press("esc")) // form emits cancelComment
	m, _ = m.Update(commentCancelledMsg{})
	if m.commenting {
		t.Error("esc should cancel the composer")
	}
	if m.focus != focusDetail {
		t.Error("cancelling the composer should keep the detail modal open")
	}
}
