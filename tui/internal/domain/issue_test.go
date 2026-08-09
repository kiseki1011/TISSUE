package domain

import (
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// toIssueSummary maps the projection nil-safely, stringifies the enum pointers, and sets Assigned
// from the presence of an assignee id.
func TestToIssueSummaryMapping(t *testing.T) {
	pri := client.IssueSummaryPriority("P1")
	cat := client.IssueSummaryCurrentStateCategory("ACTIVE")
	col := client.IssueSummaryIssueTypeColor("ANSI_BLUE")
	lastAct := time.Date(2026, 8, 17, 10, 0, 0, 0, time.UTC)
	got := toIssueSummary(&client.IssueSummary{
		IssueKey: ptr("TIS-7"), Title: ptr("Do the thing"), IssueTypeName: ptr("Task"),
		IssueTypeColor: &col, Priority: &pri, CurrentStateLabel: ptr("In Progress"),
		CurrentStateCategory: &cat, AssigneeMemberId: ptr(int64(99)), StoryPoint: ptr(int32(5)),
		LastActivityAt: &lastAct,
	})
	if got.Key != "TIS-7" || got.Title != "Do the thing" || got.TypeName != "Task" {
		t.Errorf("core fields wrong: %+v", got)
	}
	if got.Priority != "P1" || got.StateCategory != "ACTIVE" || got.TypeColor != "ANSI_BLUE" {
		t.Errorf("enum stringify wrong: pri=%q cat=%q col=%q", got.Priority, got.StateCategory, got.TypeColor)
	}
	if !got.Assigned || got.AssigneeID != 99 || got.StoryPoint != 5 {
		t.Errorf("assignee/storypoint wrong: assigned=%v id=%d sp=%d", got.Assigned, got.AssigneeID, got.StoryPoint)
	}
	if !got.LastActivity.Equal(lastAct) {
		t.Errorf("lastActivity not mapped: got %v want %v", got.LastActivity, lastAct)
	}

	// no assignee => Assigned false; a nil lastActivity => zero; all-nil must not panic
	un := toIssueSummary(&client.IssueSummary{IssueKey: ptr("TIS-8")})
	if un.Assigned {
		t.Error("issue with no assignee id should be Assigned=false")
	}
	if !un.LastActivity.IsZero() {
		t.Errorf("a nil lastActivity should map to the zero time, got %v", un.LastActivity)
	}
	if z := toIssueSummary(&client.IssueSummary{}); z.Key != "" {
		t.Errorf("empty summary should map to zero, got %+v", z)
	}
}

// toIssuePage carries pagination metadata and maps the content slice.
func TestToIssuePageMapping(t *testing.T) {
	content := []client.IssueSummary{{IssueKey: ptr("A-1")}, {IssueKey: ptr("A-2")}}
	got := toIssuePage(&client.PageResponseIssueSummary{
		Content: &content, HasNext: ptr(true), Page: ptr(int32(2)),
		Size: ptr(int32(50)), TotalElements: ptr(int64(120)), TotalPages: ptr(int32(3)),
	})
	if len(got.Issues) != 2 || got.Issues[0].Key != "A-1" {
		t.Errorf("content mapping wrong: %+v", got.Issues)
	}
	if !got.HasNext || got.Page != 2 || got.Size != 50 || got.TotalElements != 120 || got.TotalPages != 3 {
		t.Errorf("page metadata wrong: %+v", got)
	}
	if z := toIssuePage(&client.PageResponseIssueSummary{}); z.Issues != nil || z.TotalElements != 0 {
		t.Errorf("empty page should be zero, got %+v", z)
	}
}
