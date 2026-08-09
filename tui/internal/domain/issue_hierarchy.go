package domain

import (
	"context"
	"fmt"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// IssueRef is a lightweight reference to a hierarchy-related issue (a parent or a child). The backend
// identifies these by key + type + state only (there is no title on the identifier), so that is all it
// carries; the Type colour lets the UI paint a chip like the list rows.
type IssueRef struct {
	Key           string
	TypeName      string
	TypeColor     string // an IssueTypeColor enum name
	StateLabel    string
	StateCategory string // INITIAL | ACTIVE | COMPLETED | ABORTED
}

// AssignParent sets (or changes) the issue's parent to parentKey. The backend validates the hierarchy
// (a parent must be exactly one level above the child) and rejects an ineligible pairing.
func (s *IssueService) AssignParent(ctx context.Context, issueKey, parentKey string) error {
	resp, err := s.api.AssignIssueParentWithResponse(ctx, issueKey, client.AssignParentIssueRequest{ParentIssueKey: parentKey})
	if err != nil {
		return fmt.Errorf("assign parent: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// RemoveParent detaches the issue from its current parent.
func (s *IssueService) RemoveParent(ctx context.Context, issueKey string) error {
	resp, err := s.api.RemoveIssueParentWithResponse(ctx, issueKey)
	if err != nil {
		return fmt.Errorf("remove parent: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func toIssueRef(r client.IssueIdentifierResponse) IssueRef {
	out := IssueRef{Key: deref(r.IssueKey)}
	if r.IssueType != nil {
		out.TypeName = deref(r.IssueType.DisplayName)
		out.TypeColor = enumStr(r.IssueType.Color)
	}
	if r.CurrentState != nil {
		out.StateLabel = deref(r.CurrentState.DisplayName)
		out.StateCategory = enumStr(r.CurrentState.Category)
	}
	return out
}
