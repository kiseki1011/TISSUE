package domain

import (
	"testing"
	"time"
)

func notif(typ string, data map[string]string, ref EntityRef) Notification {
	return Notification{Type: typ, ActorName: "Alice", Data: data, Ref: ref, CreatedAt: time.Now()}
}

func TestHeadlinePerType(t *testing.T) {
	issueRef := EntityRef{ResourceType: "ISSUE", ProjectKey: "PROJ", IssueKey: "PROJ-12"}
	cases := []struct {
		name string
		n    Notification
		want string
	}{
		{
			"assigned", notif(NotifIssueAssigned, map[string]string{"issueKey": "PROJ-12"}, issueRef),
			"Alice assigned PROJ-12 to you",
		},
		{
			"unassigned", notif(NotifIssueUnassigned, nil, issueRef),
			"Alice unassigned you from PROJ-12",
		},
		{
			"status", notif(NotifIssueStatusChanged, map[string]string{"oldState": "To Do", "newState": "In Progress"}, issueRef),
			"Alice moved PROJ-12: To Do → In Progress",
		},
		{
			"reviewer-added", notif(NotifIssueReviewerAdded, nil, issueRef),
			"Alice added you as a reviewer on PROJ-12",
		},
		{
			"reviewer-removed", notif(NotifIssueReviewerRemoved, nil, issueRef),
			"Alice removed you as a reviewer on PROJ-12",
		},
		{
			"review-requested", notif(NotifIssueReviewRequested, nil, issueRef),
			"Alice requested your review on PROJ-12",
		},
		{
			"review-submitted", notif(NotifIssueReviewSubmitted, map[string]string{"status": "APPROVED"}, issueRef),
			"Alice reviewed PROJ-12 (APPROVED)",
		},
		{
			"deleted", notif(NotifIssueDeleted, nil, issueRef),
			"Alice deleted PROJ-12",
		},
		{
			"comment", notif(NotifIssueCommentAdded, map[string]string{"content": "hi"}, issueRef),
			"Alice commented on PROJ-12",
		},
		{
			"comment-updated", notif(NotifIssueCommentUpdated, map[string]string{"content": "hi"}, issueRef),
			"Alice edited a comment on PROJ-12",
		},
		{
			"mentioned", notif(NotifIssueMentioned, map[string]string{"content": "@you"}, issueRef),
			"Alice mentioned you on PROJ-12",
		},
		{
			"sprint-started", notif(NotifSprintStarted, map[string]string{"sprintTitle": "S1"},
				EntityRef{ResourceType: "SPRINT", ProjectKey: "PROJ"}),
			"Alice started sprint \"S1\"",
		},
		{
			"sprint-completed", notif(NotifSprintCompleted, map[string]string{"sprintTitle": "S1"},
				EntityRef{ResourceType: "SPRINT", ProjectKey: "PROJ"}),
			"Alice completed sprint \"S1\"",
		},
		{
			"role-changed", notif(NotifProjectRoleChanged, map[string]string{"oldRole": "MEMBER", "newRole": "ADMIN"},
				EntityRef{ResourceType: "PROJECT_MEMBER", ProjectKey: "PROJ"}),
			"Alice changed your role in PROJ: MEMBER → ADMIN",
		},
	}
	for _, c := range cases {
		if got := c.n.Headline(); got != c.want {
			t.Errorf("%s: Headline() = %q, want %q", c.name, got, c.want)
		}
	}
}

// A missing state still renders a placeholder rather than a dangling arrow.
func TestHeadlineStatusMissingState(t *testing.T) {
	n := notif(NotifIssueStatusChanged, map[string]string{"newState": "Done"}, EntityRef{IssueKey: "P-1"})
	if got, want := n.Headline(), "Alice moved P-1: ? → Done"; got != want {
		t.Errorf("Headline() = %q, want %q", got, want)
	}
}

// An unknown type falls back to a humanized label instead of an empty string.
func TestHeadlineUnknownType(t *testing.T) {
	n := notif("SOME_NEW_EVENT", nil, EntityRef{})
	if got, want := n.Headline(), "Some new event"; got != want {
		t.Errorf("Headline() = %q, want %q", got, want)
	}
}

// The actor falls back to the data map, then to a generic label, so a verb never dangles.
func TestActorFallback(t *testing.T) {
	fromData := Notification{Type: NotifIssueDeleted, Data: map[string]string{"actorName": "Bob", "issueKey": "P-1"}}
	if got := fromData.Headline(); got != "Bob deleted P-1" {
		t.Errorf("data-actor Headline() = %q", got)
	}
	generic := Notification{Type: NotifIssueDeleted, Data: map[string]string{"issueKey": "P-1"}}
	if got := generic.Headline(); got != "Someone deleted P-1" {
		t.Errorf("generic-actor Headline() = %q", got)
	}
}

// Target is the issue key for issue events and the project key for sprint/role events.
func TestTarget(t *testing.T) {
	issue := Notification{Ref: EntityRef{IssueKey: "P-9", ProjectKey: "P"}}
	if got := issue.Target(); got != "P-9" {
		t.Errorf("issue Target() = %q, want P-9", got)
	}
	sprint := Notification{Type: NotifSprintStarted, Ref: EntityRef{ProjectKey: "P"}, Data: map[string]string{"projectKey": "P"}}
	if got := sprint.Target(); got != "P" {
		t.Errorf("sprint Target() = %q, want P", got)
	}
}

// Only comment/mention notifications carry a preview body.
func TestDetail(t *testing.T) {
	comment := Notification{Type: NotifIssueCommentAdded, Data: map[string]string{"content": "body"}}
	if got := comment.Detail(); got != "body" {
		t.Errorf("comment Detail() = %q, want body", got)
	}
	assigned := Notification{Type: NotifIssueAssigned, Data: map[string]string{"content": "body"}}
	if got := assigned.Detail(); got != "" {
		t.Errorf("assigned Detail() = %q, want empty", got)
	}
}

func TestHumanizeNotificationType(t *testing.T) {
	cases := map[string]string{
		"ISSUE_ASSIGNED": "Issue assigned",
		"SPRINT_STARTED": "Sprint started",
		"":               "Notification",
	}
	for in, want := range cases {
		if got := HumanizeNotificationType(in); got != want {
			t.Errorf("HumanizeNotificationType(%q) = %q, want %q", in, got, want)
		}
	}
}
