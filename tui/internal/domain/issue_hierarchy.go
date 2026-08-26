package domain

import (
	"context"
	"fmt"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// IssueRef mirrors the backend identifier, which carries no title. Only key, type, and state.
type IssueRef struct {
	Key           string
	TypeName      string
	TypeColor     string // an IssueTypeColor enum name
	StateLabel    string
	StateCategory string // INITIAL | ACTIVE | COMPLETED | ABORTED
}

// AssignParent sets the issue's parent. The backend requires the parent be exactly one level above.
func (s *IssueService) AssignParent(ctx context.Context, issueKey, parentKey string) error {
	resp, err := s.api.AssignIssueParentWithResponse(ctx, issueKey, client.AssignParentIssueRequest{ParentIssueKey: parentKey})
	if err != nil {
		return fmt.Errorf("assign parent: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

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
