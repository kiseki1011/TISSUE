package domain

import (
	"context"
	"encoding/json"
	"net/http"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

func TestToIssueDetailMapsComments(t *testing.T) {
	view := &client.IssueDetailView{
		Comments: &client.PageResponseCommentDetailResponse{
			TotalElements: ptr(int64(5)),
			HasNext:       ptr(true),
			Content: &[]client.CommentDetailResponse{
				{
					CommentId: ptr(int64(1)), Content: ptr("still repros"),
					Author:   &client.CommentAuthorInfo{DisplayName: ptr("Hong"), Username: ptr("hong99")},
					IsEdited: ptr(true),
					Replies: &[]client.CommentDetailResponse{
						{CommentId: ptr(int64(2)), Content: ptr("on it"), Author: &client.CommentAuthorInfo{Username: ptr("kim")}},
					},
				},
				{CommentId: ptr(int64(3)), IsDeleted: ptr(true), Author: &client.CommentAuthorInfo{DisplayName: ptr("bot")}},
			},
		},
	}

	d := toIssueDetail(view)

	if d.CommentCount != 5 || !d.CommentsHasMore {
		t.Errorf("comment page metadata wrong: count=%d hasMore=%v", d.CommentCount, d.CommentsHasMore)
	}
	if len(d.Comments) != 2 {
		t.Fatalf("expected 2 top-level comments, got %d", len(d.Comments))
	}
	top := d.Comments[0]
	if top.ID != 1 || top.AuthorName != "Hong" || top.Content != "still repros" || !top.Edited {
		t.Errorf("first comment mapped wrong: %+v", top)
	}
	if top.AuthorUsername != "hong99" {
		t.Errorf("comment should map the author's @handle, got %q", top.AuthorUsername)
	}
	if len(top.Replies) != 1 || top.Replies[0].AuthorName != "kim" || top.Replies[0].Content != "on it" {
		t.Errorf("reply mapped wrong: %+v", top.Replies)
	}
	if !d.Comments[1].Deleted {
		t.Errorf("deleted comment should carry its tombstone flag, got %+v", d.Comments[1])
	}
}

func TestCommentAuthorFallback(t *testing.T) {
	if got := commentAuthor(&client.CommentAuthorInfo{DisplayName: ptr("Alice"), Username: ptr("a")}); got != "Alice" {
		t.Errorf("author should prefer the display name, got %q", got)
	}
	if got := commentAuthor(&client.CommentAuthorInfo{Username: ptr("bob")}); got != "bob" {
		t.Errorf("author should fall back to the username, got %q", got)
	}
	if got := commentAuthor(nil); got != "" {
		t.Errorf("a nil author should map to empty, got %q", got)
	}
}

func TestCommentUsername(t *testing.T) {
	if got := commentUsername(&client.CommentAuthorInfo{DisplayName: ptr("Alice"), Username: ptr("a")}); got != "a" {
		t.Errorf("username should be the raw @handle, got %q", got)
	}
	if got := commentUsername(&client.CommentAuthorInfo{DisplayName: ptr("Alice")}); got != "" {
		t.Errorf("a missing username should map to empty, got %q", got)
	}
	if got := commentUsername(nil); got != "" {
		t.Errorf("a nil author should map to empty, got %q", got)
	}
}

// The verdict rides the comment so the client need not read the reviewer roster, which a re-request resets.
func TestToCommentCarriesTheReviewVerdict(t *testing.T) {
	c := toComment(client.CommentDetailResponse{
		CommentId:    ptr(int64(9)),
		Content:      ptr("rename the method"),
		ReviewStatus: ptr(client.CommentDetailResponseReviewStatusCHANGESREQUESTED),
	})
	if c.ReviewStatus != "CHANGES_REQUESTED" {
		t.Errorf("ReviewStatus = %q, want CHANGES_REQUESTED", c.ReviewStatus)
	}
}

func TestToCommentLeavesOrdinaryCommentsUnstamped(t *testing.T) {
	c := toComment(client.CommentDetailResponse{CommentId: ptr(int64(9)), Content: ptr("just a note")})
	if c.ReviewStatus != "" {
		t.Errorf("an ordinary comment should carry no verdict, got %q", c.ReviewStatus)
	}
}

func TestUpdateCommentSendsTheBody(t *testing.T) {
	svc, req, body := issueServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	})

	if err := svc.UpdateComment(context.Background(), "PROJ-1", 5, "fixed", []string{"kim"}); err != nil {
		t.Fatalf("UpdateComment: %v", err)
	}
	if req.Method != http.MethodPatch {
		t.Errorf("method = %s, want PATCH", req.Method)
	}
	if want := "/api/v1/issues/PROJ-1/comments/5"; req.URL.Path != want {
		t.Errorf("path = %s, want %s", req.URL.Path, want)
	}
	var sent map[string]any
	if err := json.Unmarshal(*body, &sent); err != nil {
		t.Fatalf("decoding %q: %v", *body, err)
	}
	if sent["content"] != "fixed" {
		t.Errorf("body = %v, want the new content", sent)
	}
}

// Mentions are optional. An empty list would be a meaningless field on the wire.
func TestUpdateCommentOmitsEmptyMentions(t *testing.T) {
	svc, _, body := issueServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	})

	if err := svc.UpdateComment(context.Background(), "PROJ-1", 5, "fixed", nil); err != nil {
		t.Fatalf("UpdateComment: %v", err)
	}
	var sent map[string]any
	if err := json.Unmarshal(*body, &sent); err != nil {
		t.Fatalf("decoding %q: %v", *body, err)
	}
	if _, present := sent["mentionedUsernames"]; present {
		t.Errorf("no mentions should mean no field, got %v", sent)
	}
}

func TestDeleteCommentCallsTheEndpoint(t *testing.T) {
	svc, req, _ := issueServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	})

	if err := svc.DeleteComment(context.Background(), "PROJ-1", 5); err != nil {
		t.Fatalf("DeleteComment: %v", err)
	}
	if req.Method != http.MethodDelete {
		t.Errorf("method = %s, want DELETE", req.Method)
	}
}

// A refusal (someone else's comment) must surface with the server's reason, not be swallowed.
func TestDeleteCommentSurfacesTheRefusal(t *testing.T) {
	svc, _, _ := issueServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/problem+json")
		w.WriteHeader(http.StatusForbidden)
		_, _ = w.Write([]byte(`{"title":"COMMENT_EDIT_NOT_ALLOWED","detail":"You cannot edit this comment"}`))
	})

	err := svc.DeleteComment(context.Background(), "PROJ-1", 5)
	if err == nil {
		t.Fatal("a 403 should be reported as an error")
	}
	if got := ErrorReason(err); got != "You cannot edit this comment" {
		t.Errorf("reason = %q, want the server's explanation", got)
	}
}

// The comment list pages on from the BFF's embedded first page, so the client asks for the same size.
func TestListCommentsAsksForTheBffPageSize(t *testing.T) {
	svc, req, _ := issueServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"content":[{"commentId":9,"content":"later"}],"hasNext":true,"totalElements":25}`))
	})

	page, err := svc.ListComments(context.Background(), "PROJ-1", 1)
	if err != nil {
		t.Fatalf("ListComments: %v", err)
	}
	if len(page.Comments) != 1 || page.Comments[0].ID != 9 {
		t.Errorf("page not mapped: %+v", page)
	}
	if !page.HasNext || page.Total != 25 {
		t.Errorf("hasNext/total not mapped: %+v", page)
	}
	q := req.URL.Query()
	if q.Get("page") != "1" {
		t.Errorf("page = %q, want 1", q.Get("page"))
	}
	if q.Get("size") != "20" {
		t.Errorf("size = %q, want the BFF's 20 so the pages line up", q.Get("size"))
	}
}
