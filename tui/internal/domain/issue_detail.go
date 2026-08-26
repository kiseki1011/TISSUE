package domain

import (
	"context"
	"fmt"
	"time"

	"github.com/oapi-codegen/nullable"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// IssueDetail is the read model behind the issue detail modal, from the single detail BFF response.
type IssueDetail struct {
	Key              string
	Title            string
	Content          string
	Summary          string
	TypeName         string
	TypeColor        string // an IssueTypeColor enum name
	Priority         string // P0 (highest) .. P4
	StateLabel       string
	StateCategory    string // INITIAL | ACTIVE | COMPLETED | ABORTED
	AssigneeID       int64  // 0 when unassigned
	AssigneeName     string
	AuthorName       string
	Parent           *IssueRef // nil when the issue has no parent
	Children         []IssueRef
	Reviewers        []Reviewer
	CustomFields     []CustomField
	StoryPoint       int
	CanUseStoryPoint bool // the issue type permits a story point, gates the edit field
	Progress         int  // count-based progress percentage
	CreatedAt        time.Time
	LastUpdatedAt    time.Time
	DueAt            time.Time
	StartedAt        time.Time
	ResolvedAt       time.Time
	Transitions      []IssueTransition

	Comments        []IssueComment // only the first page
	CommentCount    int            // total comments across all pages
	CommentsHasMore bool

	Relations []IssueRelationGroup // linked issues grouped by relation kind

	Branches     []IssueBranch      // linked by push webhooks, read-only
	PullRequests []IssuePullRequest // linked by PR webhooks, read-only
}

// IssueBranch is a VCS branch linked to the issue, read-only. The URLs arrive ready to open.
// Commit fields are empty until a push has been seen.
type IssueBranch struct {
	RepoURL          string
	BranchName       string
	BranchURL        string
	LatestCommitHash string
	LatestCommitMsg  string
	LatestCommitURL  string
	PusherName       string
	PushedAt         time.Time
}

// IssuePullRequest holds the PR's current state, not its event history.
type IssuePullRequest struct {
	Number      int
	Title       string
	URL         string
	AuthorName  string
	State       string // OPEN | CLOSED | MERGED
	LastEventAt time.Time
}

// IssueTransition is one workflow move. A blocked one (CanExecute false) carries the guard reasons.
type IssueTransition struct {
	ID             int64
	Label          string
	TargetLabel    string
	TargetCategory string // INITIAL | ACTIVE | COMPLETED | ABORTED
	CanExecute     bool
	// BlockedReasons is every guard the issue fails, in server evaluation order, not just the first.
	BlockedReasons []string
}

// GetIssueDetail fetches the whole detail view for one issue in a single BFF call.
func (s *IssueService) GetIssueDetail(ctx context.Context, issueKey string) (IssueDetail, error) {
	resp, err := s.api.GetIssueDetailViewWithResponse(ctx, issueKey, &client.GetIssueDetailViewParams{})
	if err != nil {
		return IssueDetail{}, fmt.Errorf("get issue detail: %w", err)
	}
	if resp.JSON200 == nil {
		return IssueDetail{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toIssueDetail(resp.JSON200), nil
}

func toIssueDetail(v *client.IssueDetailView) IssueDetail {
	var d IssueDetail
	if c := v.Common; c != nil {
		d.Key = deref(c.IssueKey)
		d.Title = deref(c.Title)
		d.Content = deref(c.Content)
		d.Summary = deref(c.Summary)
		d.Priority = enumStr(c.Priority)
		d.StoryPoint = derefInt32(c.StoryPoint)
		d.Progress = derefInt32(c.CountBasedProgress)
		d.CreatedAt = derefTime(c.CreatedAt)
		d.LastUpdatedAt = derefTime(c.LastUpdatedAt)
		d.DueAt = derefTime(c.DueAt)
		d.StartedAt = derefTime(c.StartedAt)
		d.ResolvedAt = derefTime(c.ResolvedAt)
		if c.IssueType != nil {
			d.TypeName = deref(c.IssueType.DisplayName)
			d.TypeColor = enumStr(c.IssueType.Color)
			d.CanUseStoryPoint = derefBool(c.IssueType.CanUseStoryPoint)
		}
		if c.CurrentState != nil {
			d.StateLabel = deref(c.CurrentState.DisplayName)
			d.StateCategory = enumStr(c.CurrentState.Category)
		}
		d.AssigneeName = memberName(c.Assignee)
		if c.Assignee != nil {
			d.AssigneeID = derefInt64to64(c.Assignee.MemberId)
		}
		d.AuthorName = memberName(c.Author)
		if c.Reviewers != nil {
			for _, rv := range *c.Reviewers {
				d.Reviewers = append(d.Reviewers, toReviewer(rv))
			}
		}
	}
	if v.Parent != nil {
		if ref := toIssueRef(*v.Parent); ref.Key != "" {
			d.Parent = &ref
		}
	}
	if v.Children != nil {
		for _, ch := range *v.Children {
			if ref := toIssueRef(ch); ref.Key != "" {
				d.Children = append(d.Children, ref)
			}
		}
	}
	if v.CustomFields != nil {
		for _, cf := range *v.CustomFields {
			d.CustomFields = append(d.CustomFields, toCustomField(cf))
		}
	}
	if v.Relations != nil {
		d.Relations = toRelationGroups(v.Relations)
	}
	if v.Branches != nil {
		for _, b := range *v.Branches {
			d.Branches = append(d.Branches, IssueBranch{
				RepoURL:          deref(b.RepoUrl),
				BranchName:       deref(b.BranchName),
				BranchURL:        deref(b.BranchUrl),
				LatestCommitHash: deref(b.LatestCommitHash),
				LatestCommitMsg:  deref(b.LatestCommitMessage),
				LatestCommitURL:  deref(b.LatestCommitUrl),
				PusherName:       deref(b.PusherName),
				PushedAt:         derefTime(b.PushedAt),
			})
		}
	}
	if v.PullRequests != nil {
		for _, pr := range *v.PullRequests {
			d.PullRequests = append(d.PullRequests, IssuePullRequest{
				Number:      derefInt32(pr.Number),
				Title:       deref(pr.Title),
				URL:         deref(pr.Url),
				AuthorName:  deref(pr.AuthorName),
				State:       enumStr(pr.State),
				LastEventAt: derefTime(pr.LastEventAt),
			})
		}
	}
	if v.Comments != nil {
		d.CommentCount = derefInt64(v.Comments.TotalElements)
		d.CommentsHasMore = derefBool(v.Comments.HasNext)
		if v.Comments.Content != nil {
			for _, c := range *v.Comments.Content {
				d.Comments = append(d.Comments, toComment(c))
			}
		}
	}
	if v.AvailableTransitions != nil {
		for _, at := range *v.AvailableTransitions {
			tr := IssueTransition{
				ID:         derefInt64to64(at.TransitionId),
				Label:      deref(at.DisplayLabel),
				CanExecute: derefBool(at.CanExecute),
			}
			if at.TargetState != nil {
				tr.TargetLabel = deref(at.TargetState.DisplayName)
				tr.TargetCategory = enumStr(at.TargetState.Category)
			}
			if at.BlockedReasons != nil {
				for _, v := range *at.BlockedReasons {
					if msg := deref(v.Message); msg != "" {
						tr.BlockedReasons = append(tr.BlockedReasons, msg)
					}
				}
			}
			d.Transitions = append(d.Transitions, tr)
		}
	}
	return d
}

func (s *IssueService) PerformTransition(ctx context.Context, issueKey string, transitionID int64) error {
	resp, err := s.api.PerformIssueTransitionWithResponse(ctx, issueKey,
		client.PerformTransitionRequest{TransitionId: transitionID})
	if err != nil {
		return fmt.Errorf("perform transition: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// IssueEdit is a partial edit of an issue's common fields. A nil pointer means untouched, so only
// changed fields are sent. ClearDue (with a nil DueAt) clears the due date.
type IssueEdit struct {
	Title    *string
	Summary  *string
	Content  *string
	Priority *string
	DueAt    *time.Time
	ClearDue bool
	// StoryPoint rides its own endpoint. nil leaves it unchanged, 0 unsets it and is sent explicitly.
	StoryPoint *int
	// CustomFields also rides its own endpoint, keyed by field id. A nil value clears one.
	CustomFields map[string]interface{}
}

// Empty reports whether the edit would touch no field, so the caller can skip a needless request.
func (e IssueEdit) Empty() bool {
	return !e.HasCommonFields() && e.StoryPoint == nil && len(e.CustomFields) == 0
}

// HasCommonFields excludes the story point, which has its own endpoint.
func (e IssueEdit) HasCommonFields() bool {
	return e.Title != nil || e.Summary != nil || e.Content != nil || e.Priority != nil ||
		e.DueAt != nil || e.ClearDue
}

// UpdateIssueCommonFields patches an issue's common fields, sending only the fields the edit touched.
func (s *IssueService) UpdateIssueCommonFields(ctx context.Context, issueKey string, e IssueEdit) error {
	resp, err := s.api.UpdateIssueCommonFieldsWithResponse(ctx, issueKey, toUpdateCommonBody(e))
	if err != nil {
		return fmt.Errorf("update issue common fields: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// UpdateIssueStoryPoint uses a dedicated endpoint. 0 unsets it and is sent explicitly, not omitted.
func (s *IssueService) UpdateIssueStoryPoint(ctx context.Context, issueKey string, storyPoint int) error {
	sp := int32(storyPoint)
	resp, err := s.api.UpdateIssueStoryPointWithResponse(ctx, issueKey, client.UpdateStoryPointRequest{StoryPoint: &sp})
	if err != nil {
		return fmt.Errorf("update issue story point: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// UpdateIssueCustomFields sends only changed fields. The server merges them into the existing values.
// A nil value clears a field, which the server rejects when it is required.
func (s *IssueService) UpdateIssueCustomFields(
	ctx context.Context, issueKey string, values map[string]interface{},
) error {
	resp, err := s.api.UpdateIssueCustomFieldsWithResponse(
		ctx, issueKey, client.UpdateCustomFieldsRequest{CustomFields: values})
	if err != nil {
		return fmt.Errorf("update issue custom fields: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// toUpdateCommonBody omits untouched fields and sends a cleared due date as an explicit null.
func toUpdateCommonBody(e IssueEdit) client.UpdateCommonFieldsRequest {
	var body client.UpdateCommonFieldsRequest
	if e.Title != nil {
		body.Title = nullable.NewNullableWithValue(*e.Title)
	}
	if e.Summary != nil {
		body.Summary = nullable.NewNullableWithValue(*e.Summary)
	}
	if e.Content != nil {
		body.Content = nullable.NewNullableWithValue(*e.Content)
	}
	if e.Priority != nil {
		body.Priority = nullable.NewNullableWithValue(*e.Priority)
	}
	switch {
	case e.ClearDue:
		body.DueAt = nullable.NewNullNullable[time.Time]()
	case e.DueAt != nil:
		body.DueAt = nullable.NewNullableWithValue(*e.DueAt)
	}
	return body
}

func (s *IssueService) AssignIssue(ctx context.Context, issueKey string, memberID int64) error {
	resp, err := s.api.AssignIssueWithResponse(ctx, issueKey, memberID)
	if err != nil {
		return fmt.Errorf("assign issue: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// DeleteIssue soft-deletes into the project's trash. The TUI has no trash screen, so it looks final.
func (s *IssueService) DeleteIssue(ctx context.Context, issueKey string) error {
	resp, err := s.api.DeleteIssueWithResponse(ctx, issueKey)
	if err != nil {
		return fmt.Errorf("delete issue: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func (s *IssueService) UnassignIssue(ctx context.Context, issueKey string) error {
	resp, err := s.api.UnassignIssueWithResponse(ctx, issueKey)
	if err != nil {
		return fmt.Errorf("unassign issue: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// memberName prefers the display name and falls back to the username.
func memberName(m *client.ProjectMemberInfo) string {
	if m == nil {
		return ""
	}
	if n := deref(m.DisplayName); n != "" {
		return n
	}
	return deref(m.Username)
}
