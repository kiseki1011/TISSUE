package domain

import (
	"fmt"
	"strings"
	"time"
)

// Mirrors the backend NotificationType enum. The payload carries no sentence, so Headline builds it.
const (
	NotifIssueStatusChanged   = "ISSUE_STATUS_CHANGED"
	NotifIssueCommentAdded    = "ISSUE_COMMENT_ADDED"
	NotifIssueCommentUpdated  = "ISSUE_COMMENT_UPDATED"
	NotifIssueMentioned       = "ISSUE_MENTIONED"
	NotifIssueAssigned        = "ISSUE_ASSIGNED"
	NotifIssueUnassigned      = "ISSUE_UNASSIGNED"
	NotifIssueReviewerAdded   = "ISSUE_REVIEWER_ADDED"
	NotifIssueReviewerRemoved = "ISSUE_REVIEWER_REMOVED"
	NotifIssueReviewRequested = "ISSUE_REVIEW_REQUESTED"
	NotifIssueReviewSubmitted = "ISSUE_REVIEW_SUBMITTED"
	NotifIssueDeleted         = "ISSUE_DELETED"
	NotifSprintStarted        = "SPRINT_STARTED"
	NotifSprintCompleted      = "SPRINT_COMPLETED"
	NotifProjectRoleChanged   = "PROJECT_ROLE_CHANGED"
)

// Data-map keys from the backend NotificationDataKeys. Only the subset the inbox reads.
const (
	ndIssueKey    = "issueKey"
	ndActorName   = "actorName"
	ndOldState    = "oldState"
	ndNewState    = "newState"
	ndStatus      = "status"
	ndContent     = "content"
	ndProjectKey  = "projectKey"
	ndSprintTitle = "sprintTitle"
	ndOldRole     = "oldRole"
	ndNewRole     = "newRole"
)

// Notification is one item in the caller's personal inbox.
type Notification struct {
	ID        int64
	Type      string            // raw backend NotificationType, e.g. ISSUE_ASSIGNED
	ActorName string            // actorDisplayName, e.g. "System (webhook)". "" when none
	Data      map[string]string // per-type payload (issueKey, oldState, content, …)
	Ref       EntityRef         // the resource the notification concerns
	IsRead    bool
	CreatedAt time.Time
}

// EntityRef points a notification at the resource it concerns.
type EntityRef struct {
	ResourceType string // ISSUE | SPRINT | ISSUE_COMMENT | PROJECT_MEMBER
	ResourceID   int64  // sprint id / comment id, 0 when none
	ProjectKey   string
	IssueKey     string
	MemberID     int64
}

// NotificationPage is one cursor page of the inbox, newest first. There is no total, only HasNext.
type NotificationPage struct {
	Items      []Notification
	HasNext    bool
	NextCursor string
}

// ChannelEmail is the only channel the backend exposes. The in-app inbox is always stored.
const ChannelEmail = "EMAIL"

// NotificationPref is one (type × channel) delivery preference. The backend defaults them to enabled.
type NotificationPref struct {
	Type    string
	Channel string
	Enabled bool
}

// actor falls back to the data map, then a generic label, so a sentence never dangles.
func (n Notification) actor() string {
	if n.ActorName != "" {
		return n.ActorName
	}
	if a := n.Data[ndActorName]; a != "" {
		return a
	}
	return "Someone"
}

func (n Notification) Actor() string { return n.actor() }

// issueKey prefers the entity reference's key (authoritative) and falls back to the data map.
func (n Notification) issueKey() string {
	if n.Ref.IssueKey != "" {
		return n.Ref.IssueKey
	}
	return n.Data[ndIssueKey]
}

func (n Notification) projectKey() string {
	if n.Ref.ProjectKey != "" {
		return n.Ref.ProjectKey
	}
	return n.Data[ndProjectKey]
}

// Target is the issue key, else the project key. Sprint and role events carry no issue.
func (n Notification) Target() string {
	if k := n.issueKey(); k != "" {
		return k
	}
	return n.projectKey()
}

// Headline renders the one-line summary. The recipient is always the caller, so "you" means them.
func (n Notification) Headline() string {
	actor, key, d := n.actor(), n.issueKey(), n.Data
	switch n.Type {
	case NotifIssueAssigned:
		return fmt.Sprintf("%s assigned %s to you", actor, key)
	case NotifIssueUnassigned:
		return fmt.Sprintf("%s unassigned you from %s", actor, key)
	case NotifIssueStatusChanged:
		if d[ndOldState] != "" || d[ndNewState] != "" {
			return fmt.Sprintf("%s moved %s: %s → %s", actor, key, orQuestion(d[ndOldState]), orQuestion(d[ndNewState]))
		}
		return fmt.Sprintf("%s changed the status of %s", actor, key)
	case NotifIssueReviewerAdded:
		return fmt.Sprintf("%s added you as a reviewer on %s", actor, key)
	case NotifIssueReviewerRemoved:
		return fmt.Sprintf("%s removed you as a reviewer on %s", actor, key)
	case NotifIssueReviewRequested:
		return fmt.Sprintf("%s requested your review on %s", actor, key)
	case NotifIssueReviewSubmitted:
		if s := d[ndStatus]; s != "" {
			return fmt.Sprintf("%s reviewed %s (%s)", actor, key, s)
		}
		return fmt.Sprintf("%s reviewed %s", actor, key)
	case NotifIssueDeleted:
		return fmt.Sprintf("%s deleted %s", actor, key)
	case NotifIssueCommentAdded:
		return fmt.Sprintf("%s commented on %s", actor, key)
	case NotifIssueCommentUpdated:
		return fmt.Sprintf("%s edited a comment on %s", actor, key)
	case NotifIssueMentioned:
		return fmt.Sprintf("%s mentioned you on %s", actor, key)
	case NotifSprintStarted:
		return fmt.Sprintf("%s started sprint %s", actor, quoteTitle(d[ndSprintTitle]))
	case NotifSprintCompleted:
		return fmt.Sprintf("%s completed sprint %s", actor, quoteTitle(d[ndSprintTitle]))
	case NotifProjectRoleChanged:
		return fmt.Sprintf("%s changed your role in %s: %s → %s",
			actor, n.projectKey(), orQuestion(d[ndOldRole]), orQuestion(d[ndNewRole]))
	}
	return HumanizeNotificationType(n.Type)
}

// Detail is the preview line, "" when there is none. Only comment and mention events carry a body.
func (n Notification) Detail() string {
	switch n.Type {
	case NotifIssueCommentAdded, NotifIssueCommentUpdated, NotifIssueMentioned:
		return n.Data[ndContent]
	}
	return ""
}

// HumanizeNotificationType turns "SOME_NEW_EVENT" into "Some new event", also the headline fallback.
func HumanizeNotificationType(t string) string {
	if t == "" {
		return "Notification"
	}
	words := strings.Split(strings.ToLower(t), "_")
	joined := strings.Join(words, " ")
	r := []rune(joined)
	r[0] = []rune(strings.ToUpper(string(r[0])))[0]
	return string(r)
}

func orQuestion(s string) string {
	if s == "" {
		return "?"
	}
	return s
}

func quoteTitle(s string) string {
	if s == "" {
		return "a sprint"
	}
	return "\"" + s + "\""
}
