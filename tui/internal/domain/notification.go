package domain

import (
	"fmt"
	"strings"
	"time"
)

// Notification types mirror the backend NotificationType enum. The inbox renders each into a human
// sentence (the REST payload carries only the raw type plus a data map — the sentence is the client's
// to build).
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

// Notification data-map keys, mirroring the backend NotificationDataKeys. Only the subset the inbox
// reads is named here.
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
	ActorName string            // actorDisplayName (e.g. "System (webhook)"); "" when none
	Data      map[string]string // per-type payload (issueKey, oldState, content, …)
	Ref       EntityRef         // the resource the notification concerns
	IsRead    bool
	CreatedAt time.Time
}

// EntityRef points a notification at the resource it concerns, for display and (later) navigation.
type EntityRef struct {
	ResourceType string // ISSUE | SPRINT | ISSUE_COMMENT | PROJECT_MEMBER
	ResourceID   int64  // sprint id / comment id, 0 when none
	ProjectKey   string
	IssueKey     string
	MemberID     int64
}

// NotificationPage is one cursor page of the inbox (newest first). The list is cursor-paginated (no
// total), so HasNext plus the opaque NextCursor is all that is needed to pull the next page.
type NotificationPage struct {
	Items      []Notification
	HasNext    bool
	NextCursor string
}

// ChannelEmail is the only delivery channel the backend exposes today (the in-app inbox is always
// stored and cannot be disabled), so a preference is effectively "email me about this type: on/off".
const ChannelEmail = "EMAIL"

// NotificationPref is one delivery preference: whether a given notification type is delivered over a
// given channel. The backend returns one per (type × channel), defaulting to enabled.
type NotificationPref struct {
	Type    string
	Channel string
	Enabled bool
}

// actor is the display name of whoever caused the notification, falling back to the data map then a
// generic label so a sentence never renders a dangling verb.
func (n Notification) actor() string {
	if n.ActorName != "" {
		return n.ActorName
	}
	if a := n.Data[ndActorName]; a != "" {
		return a
	}
	return "Someone"
}

// Actor is the display name of whoever caused the notification, for the detail pane's "From" row.
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

// Target is the short resource label shown in the list's right column: the issue key for issue
// notifications, else the project key (sprint / role events).
func (n Notification) Target() string {
	if k := n.issueKey(); k != "" {
		return k
	}
	return n.projectKey()
}

// Headline renders the one-line human summary of the notification. The recipient is always the caller,
// so the second person ("you", "your") refers to them.
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

// Detail is the secondary preview line for a notification, or "" when it has none. Only comment and
// mention events carry a body worth previewing.
func (n Notification) Detail() string {
	switch n.Type {
	case NotifIssueCommentAdded, NotifIssueCommentUpdated, NotifIssueMentioned:
		return n.Data[ndContent]
	}
	return ""
}

// HumanizeNotificationType turns a raw enum ("SOME_NEW_EVENT") into a readable label ("Some new
// event"), used for the detail pane's Type row and as the headline fallback for an unrecognized type.
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
