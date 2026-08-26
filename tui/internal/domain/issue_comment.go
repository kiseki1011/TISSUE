package domain

import (
	"context"
	"fmt"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// IssueComment is one comment in an issue's thread. A deleted one keeps its slot as a tombstone.
type IssueComment struct {
	ID             int64
	AuthorName     string
	AuthorUsername string // the author's @handle, empty when the server omits it
	Content        string
	CreatedAt      time.Time
	Edited         bool
	Deleted        bool
	// ReviewStatus holds the verdict a review was submitted with (APPROVED | CHANGES_REQUESTED).
	// It is frozen at submission. A later re-review must not rewrite what this comment said.
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

func commentUsername(a *client.CommentAuthorInfo) string {
	if a == nil {
		return ""
	}
	return deref(a.Username)
}

// CreateComment posts a top-level comment. The server drives ISSUE_MENTIONED off mentions and never
// parses the body, so mentions is sent only when non-empty.
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

// CreateReply threads a reply under parentCommentID. mentions works as in CreateComment.
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

// CommentPageSize matches the detail BFF's embedded first page (its commentSize default), so page 1
// continues exactly where the embedded page 0 stopped.
const CommentPageSize = 20

// IssueCommentPage is one page of an issue's root comments (replies ride along inside each root).
type IssueCommentPage struct {
	Comments []IssueComment
	HasNext  bool
	Total    int
}

// ListComments fetches one page, 0-based. The detail BFF carries page 0, so "load more" starts at 1.
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

// UpdateComment rewrites a comment's body. Only content is sent, so a review comment keeps its verdict.
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

// DeleteComment soft-deletes. The slot stays as a tombstone so replies underneath stay readable.
func (s *IssueService) DeleteComment(ctx context.Context, issueKey string, commentID int64) error {
	resp, err := s.api.DeleteCommentWithResponse(ctx, issueKey, commentID)
	if err != nil {
		return fmt.Errorf("delete comment: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}
