package domain

import (
	"context"
	"fmt"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// IssueComment is one comment in the issue detail's comment thread. A deleted comment keeps its slot
// (its content is replaced by a tombstone) so the thread structure is preserved.
type IssueComment struct {
	ID             int64
	AuthorName     string
	AuthorUsername string // the author's @handle, shown beside the name; empty when the server omits it
	Content        string
	CreatedAt      time.Time
	Edited         bool
	Deleted        bool
	// ReviewStatus is set only when the comment is the feedback body of a submitted review, and holds
	// the verdict it was submitted with (APPROVED | CHANGES_REQUESTED). Frozen at submission: a later
	// re-review request resets the reviewer's own status but must not rewrite what this comment said.
	ReviewStatus string
	Replies      []IssueComment
}

func toComment(c client.CommentDetailResponse) IssueComment {
	ic := IssueComment{
		ID:             derefInt64to64(c.CommentId),
		AuthorName:     commentAuthor(c.Author),
		AuthorUsername: commentUsername(c.Author),
		Content:        deref(c.Content),
		CreatedAt:      derefTime(c.CreatedAt),
		Edited:         derefBool(c.IsEdited),
		Deleted:        derefBool(c.IsDeleted),
		ReviewStatus:   enumStr(c.ReviewStatus),
	}
	if c.Replies != nil {
		for _, r := range *c.Replies {
			ic.Replies = append(ic.Replies, toComment(r))
		}
	}
	return ic
}

// commentAuthor prefers the display name and falls back to the username.
func commentAuthor(a *client.CommentAuthorInfo) string {
	if a == nil {
		return ""
	}
	if n := deref(a.DisplayName); n != "" {
		return n
	}
	return deref(a.Username)
}

// commentUsername is the author's raw @handle, for the "{name} (@{username})" header. Empty when the
// server omits it (the render then shows just the name).
func commentUsername(a *client.CommentAuthorInfo) string {
	if a == nil {
		return ""
	}
	return deref(a.Username)
}

// CreateComment posts a top-level comment on an issue. mentions is the list of @-mentioned usernames
// (the server drives the ISSUE_MENTIONED notification off it; it does not parse the body), sent only
// when non-empty so a plain comment omits the field.
func (s *IssueService) CreateComment(ctx context.Context, issueKey, content string, mentions []string) error {
	req := client.AddCommentRequest{Content: content}
	if len(mentions) > 0 {
		req.MentionedUsernames = &mentions
	}
	resp, err := s.api.CreateCommentWithResponse(ctx, issueKey, req)
	if err != nil {
		return fmt.Errorf("create comment: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CreateReply posts a reply to an existing comment, threaded under it via parentCommentID. mentions
// carries the @-mentioned usernames, as CreateComment.
func (s *IssueService) CreateReply(ctx context.Context, issueKey string, parentCommentID int64, content string, mentions []string) error {
	req := client.AddCommentRequest{Content: content, ParentCommentId: &parentCommentID}
	if len(mentions) > 0 {
		req.MentionedUsernames = &mentions
	}
	resp, err := s.api.CreateCommentWithResponse(ctx, issueKey, req)
	if err != nil {
		return fmt.Errorf("create reply: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CommentPageSize matches the detail BFF's embedded first page (its commentSize default), so paging on
// from it lines up: page 1 continues exactly where the embedded page 0 stopped. It is exported because
// the UI derives the next page index from how many roots it already holds.
const CommentPageSize = 20

// IssueCommentPage is one page of an issue's root comments (replies ride along inside each root).
type IssueCommentPage struct {
	Comments []IssueComment
	HasNext  bool
	Total    int
}

// ListComments fetches one page of an issue's comment thread. page is 0-based; the detail BFF already
// carries page 0, so the UI's "load more" starts at 1.
func (s *IssueService) ListComments(ctx context.Context, issueKey string, page int) (IssueCommentPage, error) {
	p, sz := int32(page), int32(CommentPageSize)
	params := &client.ListIssueCommentsParams{Pageable: client.Pageable{Page: &p, Size: &sz}}
	resp, err := s.api.ListIssueCommentsWithResponse(ctx, issueKey, params)
	if err != nil {
		return IssueCommentPage{}, fmt.Errorf("list comments: %w", err)
	}
	if resp.JSON200 == nil {
		return IssueCommentPage{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	out := IssueCommentPage{
		HasNext: derefBool(resp.JSON200.HasNext),
		Total:   derefInt64(resp.JSON200.TotalElements),
	}
	if resp.JSON200.Content != nil {
		for _, c := range *resp.JSON200.Content {
			out.Comments = append(out.Comments, toComment(c))
		}
	}
	return out, nil
}

// UpdateComment rewrites a comment's body. The server marks it edited; a review comment keeps the verdict
// it was submitted with, since only the content is sent.
func (s *IssueService) UpdateComment(ctx context.Context, issueKey string, commentID int64, content string, mentions []string) error {
	req := client.UpdateCommentRequest{Content: content}
	if len(mentions) > 0 {
		req.MentionedUsernames = &mentions
	}
	resp, err := s.api.UpdateCommentWithResponse(ctx, issueKey, commentID, req)
	if err != nil {
		return fmt.Errorf("update comment: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// DeleteComment soft-deletes a comment. The thread keeps its slot as a tombstone so replies underneath
// stay readable.
func (s *IssueService) DeleteComment(ctx context.Context, issueKey string, commentID int64) error {
	resp, err := s.api.DeleteCommentWithResponse(ctx, issueKey, commentID)
	if err != nil {
		return fmt.Errorf("delete comment: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}
