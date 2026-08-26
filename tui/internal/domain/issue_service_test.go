package domain

import (
	"testing"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// The Members tab passes a member id, the filter modal passes AssigneeMe. Both must compose.
func TestApplyFilterMemberIDs(t *testing.T) {
	params := &client.SearchProjectIssuesParams{}
	applyFilter(params, IssueFilter{
		AssigneeMemberIDs: []string{"42"},
		ReviewerMemberIDs: []string{"7"},
		StateCategories:   []string{"INITIAL", "ACTIVE"},
	})
	if params.AssigneeMemberIds == nil || len(*params.AssigneeMemberIds) != 1 || (*params.AssigneeMemberIds)[0] != "42" {
		t.Errorf("assignee ids = %v, want [42]", params.AssigneeMemberIds)
	}
	if params.ReviewerMemberIds == nil || (*params.ReviewerMemberIds)[0] != "7" {
		t.Errorf("reviewer ids = %v, want [7]", params.ReviewerMemberIds)
	}
	if params.StateCategories == nil || len(*params.StateCategories) != 2 {
		t.Errorf("state categories not applied: %v", params.StateCategories)
	}
}

func TestApplyFilterAssigneeMeComposes(t *testing.T) {
	params := &client.SearchProjectIssuesParams{}
	applyFilter(params, IssueFilter{AssigneeMemberIDs: []string{"42"}, AssigneeMe: true})
	if params.AssigneeMemberIds == nil || len(*params.AssigneeMemberIds) != 2 {
		t.Fatalf("assignee ids = %v, want [42 me]", params.AssigneeMemberIds)
	}
	got := *params.AssigneeMemberIds
	if got[0] != "42" || got[1] != "me" {
		t.Errorf("assignee ids = %v, want [42 me]", got)
	}
}

func TestApplyFilterNoAssigneeOmitted(t *testing.T) {
	params := &client.SearchProjectIssuesParams{}
	applyFilter(params, IssueFilter{Keyword: "x"})
	if params.AssigneeMemberIds != nil {
		t.Errorf("assignee ids should be nil when unset, got %v", params.AssigneeMemberIds)
	}
}
