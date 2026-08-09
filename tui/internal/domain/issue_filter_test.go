package domain

import (
	"testing"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

func TestApplyFilterMapsAxes(t *testing.T) {
	params := &client.SearchProjectIssuesParams{}
	applyFilter(params, IssueFilter{
		Keyword:           "bug",
		StateCategories:   []string{"INITIAL", "ACTIVE"},
		Priorities:        []string{"P0"},
		IssueTypeIDs:      []int64{7, 8},
		SprintIDs:         []int64{3},
		CurrentSprintOnly: true,
		AssigneeMe:        true,
	})

	if params.Keyword == nil || *params.Keyword != "bug" {
		t.Error("keyword not mapped")
	}
	if params.StateCategories == nil || len(*params.StateCategories) != 2 {
		t.Errorf("stateCategories not mapped: %v", params.StateCategories)
	}
	if params.Priorities == nil || len(*params.Priorities) != 1 {
		t.Error("priorities not mapped")
	}
	if params.IssueTypeIds == nil || len(*params.IssueTypeIds) != 2 || (*params.IssueTypeIds)[0] != 7 {
		t.Errorf("issueTypeIds not mapped: %v", params.IssueTypeIds)
	}
	if params.SprintIds == nil || (*params.SprintIds)[0] != 3 {
		t.Error("sprintIds not mapped")
	}
	if params.CurrentSprintOnly == nil || !*params.CurrentSprintOnly {
		t.Error("currentSprintOnly not mapped")
	}
	if params.AssigneeMemberIds == nil || (*params.AssigneeMemberIds)[0] != "me" {
		t.Errorf("assigneeMe should map to the \"me\" token: %v", params.AssigneeMemberIds)
	}
}

// A zero filter carries nothing, so an empty search stays unscoped (all issues).
func TestApplyFilterEmptyIsNoOp(t *testing.T) {
	params := &client.SearchProjectIssuesParams{}
	applyFilter(params, IssueFilter{})
	if params.Keyword != nil || params.StateCategories != nil || params.AssigneeMemberIds != nil ||
		params.IssueTypeIds != nil || params.CurrentSprintOnly != nil {
		t.Error("a zero filter should set no params")
	}
}

func TestOpenIssuesFilterIsInitialAndActive(t *testing.T) {
	got := OpenIssuesFilter().StateCategories
	if len(got) != 2 || got[0] != "INITIAL" || got[1] != "ACTIVE" {
		t.Errorf("default open filter should be [INITIAL ACTIVE], got %v", got)
	}
}

// The backend drops reviewerStatuses when no reviewer is named, so sending them alone would silently
// widen the search instead of narrowing it. The client must not emit that combination.
func TestApplyFilterOmitsReviewerStatusesWithoutAReviewer(t *testing.T) {
	params := &client.SearchProjectIssuesParams{}
	applyFilter(params, IssueFilter{ReviewerStatuses: []string{"PENDING"}})

	if params.ReviewerStatuses != nil {
		t.Errorf("reviewerStatuses should be dropped without a reviewer, got %v", *params.ReviewerStatuses)
	}
	if params.ReviewerMemberIds != nil {
		t.Errorf("no reviewer was asked for, got %v", *params.ReviewerMemberIds)
	}
}

func TestApplyFilterSendsReviewerStatusesWithAReviewer(t *testing.T) {
	params := &client.SearchProjectIssuesParams{}
	applyFilter(params, IssueFilter{ReviewerMe: true, ReviewerStatuses: []string{"PENDING", "CHANGES_REQUESTED"}})

	if params.ReviewerMemberIds == nil || (*params.ReviewerMemberIds)[0] != "me" {
		t.Fatalf("reviewerMe should map to the \"me\" token: %v", params.ReviewerMemberIds)
	}
	if params.ReviewerStatuses == nil || len(*params.ReviewerStatuses) != 2 {
		t.Fatalf("reviewerStatuses not mapped: %v", params.ReviewerStatuses)
	}
	if got := (*params.ReviewerStatuses)[0]; string(got) != "PENDING" {
		t.Errorf("reviewerStatuses[0] = %q, want PENDING", got)
	}
}
