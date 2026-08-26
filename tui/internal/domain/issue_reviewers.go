package domain

import (
	"context"
	"fmt"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// Reviewer is a member assigned to review, with their current decision. MemberID lets a "set reviewers"
// flow diff against the current roster, since the backend offers only per-member add/remove.
type Reviewer struct {
	MemberID int64
	Name     string
	Status   string // PENDING | APPROVED | CHANGES_REQUESTED
}

func toReviewer(r client.ReviewerInfo) Reviewer {
	return Reviewer{
		MemberID: memberID(r.Participant),
		Name:     memberName(r.Participant),
		Status:   enumStr(r.Status),
	}
}

func memberID(m *client.ProjectMemberInfo) int64 {
	if m == nil || m.MemberId == nil {
		return 0
	}
	return *m.MemberId
}

func (s *IssueService) AddReviewer(ctx context.Context, issueKey string, memberID int64) error {
	resp, err := s.api.AddIssueReviewerWithResponse(ctx, issueKey, memberID)
	if err != nil {
		return fmt.Errorf("add reviewer: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func (s *IssueService) RemoveReviewer(ctx context.Context, issueKey string, memberID int64) error {
	resp, err := s.api.RemoveIssueReviewerWithResponse(ctx, issueKey, memberID)
	if err != nil {
		return fmt.Errorf("remove reviewer: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// SubmitReview records the caller's verdict. A non-empty comment lands in the thread stamped with it.
func (s *IssueService) SubmitReview(ctx context.Context, issueKey string, approved bool, comment string) error {
	body := client.SubmitReviewRequest{Approved: approved}
	if comment != "" {
		body.Comment = &comment
	}
	resp, err := s.api.SubmitIssueReviewWithResponse(ctx, issueKey, body)
	if err != nil {
		return fmt.Errorf("submit review: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// RequestReview resets the named reviewers to PENDING. It does not add anyone to the roster.
func (s *IssueService) RequestReview(ctx context.Context, issueKey string, memberIDs []int64) error {
	body := client.RequestReviewRequest{ReviewerMemberIds: memberIDs}
	resp, err := s.api.RequestIssueReviewWithResponse(ctx, issueKey, body)
	if err != nil {
		return fmt.Errorf("request review: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}
