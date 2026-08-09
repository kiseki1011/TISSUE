package domain

import (
	"testing"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

func TestToIssueDetailMapsCommonAndRelations(t *testing.T) {
	priority := client.IssueCommonDetailPriorityP1
	category := client.StateInfoCategoryACTIVE
	view := &client.IssueDetailView{
		Common: &client.IssueCommonDetail{
			IssueKey:           ptr("ENG-7"),
			Title:              ptr("Fix the login loop"),
			Content:            ptr("# Steps\nreproduce it"),
			Summary:            ptr("Short summary"),
			Priority:           &priority,
			StoryPoint:         ptr(int32(5)),
			CountBasedProgress: ptr(int32(40)),
			CurrentState:       &client.StateInfo{DisplayName: ptr("In Progress"), Category: &category},
			IssueType:          &client.IssueTypeInfo{DisplayName: ptr("Bug"), CanUseStoryPoint: ptr(true)},
			Assignee:           &client.ProjectMemberInfo{MemberId: ptr(int64(42)), DisplayName: ptr("Hong Gildong")},
			Author:             &client.ProjectMemberInfo{Username: ptr("kim")}, // falls back to username
			Reviewers: &[]client.ReviewerInfo{
				{Participant: &client.ProjectMemberInfo{MemberId: ptr(int64(3)), DisplayName: ptr("Reviewer One")}, Status: ptr(client.ReviewerInfoStatusAPPROVED)},
				{Participant: &client.ProjectMemberInfo{MemberId: ptr(int64(4)), DisplayName: ptr("Reviewer Two")}, Status: ptr(client.ReviewerInfoStatusPENDING)},
			},
		},
		Parent: &client.IssueIdentifierResponse{
			IssueKey:     ptr("ENG-1"),
			IssueType:    &client.IssueTypeInfo{DisplayName: ptr("Epic"), Color: ptr(client.IssueTypeInfoColor("INDIGO"))},
			CurrentState: &client.StateInfo{DisplayName: ptr("In Progress"), Category: ptr(client.StateInfoCategoryACTIVE)},
		},
		Children: &[]client.IssueIdentifierResponse{
			{IssueKey: ptr("ENG-8"), IssueType: &client.IssueTypeInfo{DisplayName: ptr("Task"), Color: ptr(client.IssueTypeInfoColor("ANSI_RED"))},
				CurrentState: &client.StateInfo{DisplayName: ptr("Todo"), Category: ptr(client.StateInfoCategoryINITIAL)}},
			{IssueKey: ptr("ENG-9")},
			{}, // no key -> dropped
		},
		AvailableTransitions: &[]client.AvailableTransition{
			{TransitionId: ptr(int64(10)), DisplayLabel: ptr("Resolve"), CanExecute: ptr(true),
				TargetState: &client.StateInfo{DisplayName: ptr("Done"), Category: ptr(client.StateInfoCategoryCOMPLETED)}},
			{TransitionId: ptr(int64(11)), DisplayLabel: ptr("Abandon"), CanExecute: ptr(false),
				BlockedReasons: &[]client.GuardViolation{
					{Message: ptr("needs approval")}, {}, {Message: ptr("assignee required")}}},
		},
	}

	d := toIssueDetail(view)

	if len(d.Transitions) != 2 {
		t.Fatalf("expected 2 transitions, got %d", len(d.Transitions))
	}
	if tr := d.Transitions[0]; tr.ID != 10 || tr.Label != "Resolve" || !tr.CanExecute || tr.TargetLabel != "Done" || tr.TargetCategory != "COMPLETED" {
		t.Errorf("executable transition wrong: %+v", tr)
	}
	// every guard the server reported is carried, not just the first; a violation with no message is
	// dropped rather than rendered as a blank condition
	if tr := d.Transitions[1]; tr.CanExecute ||
		len(tr.BlockedReasons) != 2 || tr.BlockedReasons[0] != "needs approval" || tr.BlockedReasons[1] != "assignee required" {
		t.Errorf("blocked transition wrong: %+v", tr)
	}

	if d.Key != "ENG-7" || d.Title != "Fix the login loop" || d.Summary != "Short summary" {
		t.Errorf("core fields wrong: %+v", d)
	}
	if d.Priority != "P1" || d.StateLabel != "In Progress" || d.StateCategory != "ACTIVE" {
		t.Errorf("state/priority wrong: %+v", d)
	}
	if d.TypeName != "Bug" || d.StoryPoint != 5 || d.Progress != 40 {
		t.Errorf("type/points/progress wrong: %+v", d)
	}
	if !d.CanUseStoryPoint {
		t.Errorf("the issue type's canUseStoryPoint should map through, got %v", d.CanUseStoryPoint)
	}
	if d.AssigneeName != "Hong Gildong" || d.AssigneeID != 42 {
		t.Errorf("assignee should map name and id, got %q/%d", d.AssigneeName, d.AssigneeID)
	}
	if d.AuthorName != "kim" {
		t.Errorf("author should fall back to the username, got %q", d.AuthorName)
	}
	if len(d.Reviewers) != 2 {
		t.Fatalf("expected 2 reviewers, got %d: %+v", len(d.Reviewers), d.Reviewers)
	}
	if r := d.Reviewers[0]; r.Name != "Reviewer One" || r.Status != "APPROVED" || r.MemberID != 3 {
		t.Errorf("first reviewer wrong: %+v", r)
	}
	if d.Reviewers[1].Status != "PENDING" {
		t.Errorf("second reviewer status wrong: %+v", d.Reviewers[1])
	}
	if d.Parent == nil || d.Parent.Key != "ENG-1" || d.Parent.TypeName != "Epic" || d.Parent.TypeColor != "INDIGO" || d.Parent.StateCategory != "ACTIVE" {
		t.Errorf("parent ref wrong: %+v", d.Parent)
	}
	// the keyless child is dropped, so 2 remain
	if len(d.Children) != 2 {
		t.Fatalf("expected 2 children (keyless dropped), got %d: %+v", len(d.Children), d.Children)
	}
	if c := d.Children[0]; c.Key != "ENG-8" || c.TypeName != "Task" || c.TypeColor != "ANSI_RED" || c.StateLabel != "Todo" || c.StateCategory != "INITIAL" {
		t.Errorf("first child wrong: %+v", c)
	}
	if d.Children[1].Key != "ENG-9" {
		t.Errorf("second child key wrong: %+v", d.Children[1])
	}
}

// A response with no Common block maps to a zero detail rather than panicking.
func TestToIssueDetailEmpty(t *testing.T) {
	d := toIssueDetail(&client.IssueDetailView{})
	if d.Key != "" || d.Title != "" {
		t.Errorf("an empty view should map to a zero detail, got %+v", d)
	}
}

// The story point rides a separate endpoint, so it counts toward Empty() but not HasCommonFields().
func TestIssueEditStoryPointClassification(t *testing.T) {
	sp := 3
	spOnly := IssueEdit{StoryPoint: &sp}
	if spOnly.Empty() {
		t.Error("a story point change is not an empty edit")
	}
	if spOnly.HasCommonFields() {
		t.Error("a story-point-only edit must not trigger the common-fields request")
	}
	title := "T"
	withCommon := IssueEdit{Title: &title}
	if !withCommon.HasCommonFields() {
		t.Error("a title change should count as a common field")
	}
	if !(IssueEdit{}).Empty() {
		t.Error("a zero edit should be empty")
	}
}
