package domain

import (
	"context"
	"fmt"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// Reviewer is a member assigned to review the issue, with their current review decision. MemberID lets
// a "set reviewers" flow diff the desired roster against the current one (the backend offers only
// per-member add/remove, no bulk set).
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

// memberID is the participant's member id, or 0 when absent.
func memberID(m *client.ProjectMemberInfo) int64 {
	if m == nil || m.MemberId == nil {
		return 0
	}
	return *m.MemberId
}

// AddReviewer adds a member to the issue's reviewer roster. The backend has no bulk set, so a roster
// change is applied as a diff of per-member Add/Remove calls.
func (s *IssueService) AddReviewer(ctx context.Context, issueKey string, memberID int64) error {
	resp, err := s.api.AddIssueReviewerWithResponse(ctx, issueKey, memberID)
	if err != nil {
		return fmt.Errorf("add reviewer: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// RemoveReviewer removes a member from the issue's reviewer roster.
func (s *IssueService) RemoveReviewer(ctx context.Context, issueKey string, memberID int64) error {
	resp, err := s.api.RemoveIssueReviewerWithResponse(ctx, issueKey, memberID)
	if err != nil {
		return fmt.Errorf("remove reviewer: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// SubmitReview records the caller's verdict on the issue. A non-empty comment is stored as the review's
// feedback body, appearing in the issue's comment thread stamped with the verdict.
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

// RequestReview asks the named reviewers to look again, resetting their verdicts to PENDING. It does not
// add reviewers - only members already on the roster are reset.
func (s *IssueService) RequestReview(ctx context.Context, issueKey string, memberIDs []int64) error {
	body := client.RequestReviewRequest{ReviewerMemberIds: memberIDs}
	resp, err := s.api.RequestIssueReviewWithResponse(ctx, issueKey, body)
	if err != nil {
		return fmt.Errorf("request review: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}
