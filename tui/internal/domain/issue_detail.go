package domain

import (
	"context"
	"fmt"
	"time"

	"github.com/oapi-codegen/nullable"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// IssueDetail is the read model behind the issue detail modal, mapped from the detail BFF's Common
// block plus a couple of relationship counts. Comments, relations, custom fields, and the available
// transitions ride along in the same BFF response but are mapped in later slices.
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
	Parent           *IssueRef     // the parent issue (key/type/state), nil when the issue has no parent
	Children         []IssueRef    // child issues (key/type/state), from the detail BFF
	Reviewers        []Reviewer    // reviewers with their review status, from the detail BFF
	CustomFields     []CustomField // the issue type's custom fields with their values, from the detail BFF
	StoryPoint       int
	CanUseStoryPoint bool // the issue type permits a story point (some types disallow it); gates the edit field
	Progress         int  // count-based progress percentage
	CreatedAt        time.Time
	LastUpdatedAt    time.Time
	DueAt            time.Time
	StartedAt        time.Time
	ResolvedAt       time.Time
	Transitions      []IssueTransition

	Comments        []IssueComment // the first page of comments, from the detail BFF
	CommentCount    int            // total comments across all pages
	CommentsHasMore bool           // more comment pages exist beyond the ones carried here

	Relations []IssueRelationGroup // linked issues grouped by relation kind, from the detail BFF

	Branches     []IssueBranch      // VCS branches linked to the issue (from push webhooks), read-only
	PullRequests []IssuePullRequest // pull requests linked to the issue (from PR webhooks), read-only
}

// IssueBranch is a VCS branch linked to the issue, surfaced read-only on the detail. The URLs are
// ready-to-open links (from the server), so the UI can deep-link to the repository. Commit fields are
// empty until a push has been seen.
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

// IssuePullRequest is a pull request linked to the issue, surfaced read-only on the detail. Held as
// current state rather than history, so State is what the PR is now - not the last event seen.
type IssuePullRequest struct {
	Number      int
	Title       string
	URL         string
	AuthorName  string
	State       string // OPEN | CLOSED | MERGED
	LastEventAt time.Time
}

// IssueTransition is one workflow move available from the issue's current state. A blocked transition
// (CanExecute false) carries the guard reasons so the UI can explain why it cannot run.
type IssueTransition struct {
	ID             int64
	Label          string
	TargetLabel    string
	TargetCategory string // INITIAL | ACTIVE | COMPLETED | ABORTED
	CanExecute     bool
	// BlockedReasons is every guard the issue currently fails for this move, in the order the server
	// evaluated them. The server collects them all rather than stopping at the first, so the picker can
	// list the full set of conditions instead of sending the user round one at a time.
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

// PerformTransition executes a workflow transition on an issue, moving it to the transition's target state.
func (s *IssueService) PerformTransition(ctx context.Context, issueKey string, transitionID int64) error {
	resp, err := s.api.PerformIssueTransitionWithResponse(ctx, issueKey,
		client.PerformTransitionRequest{TransitionId: transitionID})
	if err != nil {
		return fmt.Errorf("perform transition: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// IssueEdit is a partial edit of an issue's common fields. Each pointer is nil when the field was not
// changed, so only touched fields are sent (a PATCH). ClearDue set (with a nil DueAt) clears the due date.
type IssueEdit struct {
	Title    *string
	Summary  *string
	Content  *string
	Priority *string
	DueAt    *time.Time
	ClearDue bool
	// StoryPoint is set via a separate endpoint (not the common-fields PATCH): nil leaves it unchanged, a
	// non-nil value sets it (0 unsets it, sent explicitly since the wire field is a pointer to 0).
	StoryPoint *int
	// CustomFields likewise rides its own endpoint, keyed by field id: only changed fields appear, and a
	// nil value clears one. Empty (or nil) means no custom field changed.
	CustomFields map[string]interface{}
}

// Empty reports whether the edit would touch no field, so the caller can skip a needless request.
func (e IssueEdit) Empty() bool {
	return !e.HasCommonFields() && e.StoryPoint == nil && len(e.CustomFields) == 0
}

// HasCommonFields reports whether any field handled by the common-fields PATCH changed (story point is
// excluded — it has its own endpoint), so the caller can skip that request when only the point changed.
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

// UpdateIssueStoryPoint sets an issue's story point via its dedicated endpoint. A value of 0 unsets it;
// the wire field is a pointer so 0 is sent explicitly rather than omitted.
func (s *IssueService) UpdateIssueStoryPoint(ctx context.Context, issueKey string, storyPoint int) error {
	sp := int32(storyPoint)
	resp, err := s.api.UpdateIssueStoryPointWithResponse(ctx, issueKey, client.UpdateStoryPointRequest{StoryPoint: &sp})
	if err != nil {
		return fmt.Errorf("update issue story point: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// UpdateIssueCustomFields patches an issue's custom field values. The server merges the map into the
// existing values, so only the fields that changed need to be sent; a nil value clears one (which the
// server rejects for a required field).
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

// toUpdateCommonBody maps an edit to the PATCH body: a field is left unspecified (omitted) unless the
// edit touched it, and a cleared due date is sent as an explicit null.
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

// AssignIssue sets an issue's assignee to the given member.
func (s *IssueService) AssignIssue(ctx context.Context, issueKey string, memberID int64) error {
	resp, err := s.api.AssignIssueWithResponse(ctx, issueKey, memberID)
	if err != nil {
		return fmt.Errorf("assign issue: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// DeleteIssue soft-deletes an issue into the project's trash, from where it can be restored. The TUI
// has no trash screen yet, so from here the delete looks final even though the server keeps the row.
func (s *IssueService) DeleteIssue(ctx context.Context, issueKey string) error {
	resp, err := s.api.DeleteIssueWithResponse(ctx, issueKey)
	if err != nil {
		return fmt.Errorf("delete issue: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// UnassignIssue clears an issue's assignee.
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
