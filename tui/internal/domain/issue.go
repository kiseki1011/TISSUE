package domain

import "time"

// IssueSummary is one row in a project's issue list (the searchIssues projection).
type IssueSummary struct {
	Key           string
	Title         string
	TypeName      string
	TypeColor     string // an IssueTypeColor enum name (ANSI_* or a named colour)
	Priority      string // P0 (highest) .. P4
	StateLabel    string
	StateCategory string // INITIAL | ACTIVE | COMPLETED | ABORTED
	Assigned      bool
	AssigneeID    int64
	AssigneeName  string
	StoryPoint    int // 0 when unset
	Progress      int // count-based progress percentage
	DueAt         time.Time
	SprintID      int64
	LastActivity  time.Time // most recent activity on the issue (comments included); zero when none
	// MyReviewStatus is the caller's own review state on this issue, or "" when they are not a reviewer.
	// The server computes it, so the client needs no member id of its own to know whether it may review.
	MyReviewStatus string
}

// IssueFilter is the set of search axes the issue list exposes, mapped onto the backend's
// :search query. A zero value carries no filter; the "me" flags resolve server-side to the caller.
type IssueFilter struct {
	Keyword           string
	StateCategories   []string // INITIAL | ACTIVE | COMPLETED | ABORTED
	Priorities        []string // P0 .. P4
	IssueTypeIDs      []int64
	SprintIDs         []int64
	CurrentSprintOnly bool
	AssigneeMe        bool
	AuthorMe          bool
	ReviewerMe        bool
	AssigneeMemberIDs []string // specific assignees (member id strings); composes with AssigneeMe
	ReviewerMemberIDs []string // specific reviewers (member id strings); composes with ReviewerMe
	// ReviewerStatuses narrows the reviewer axis to these review states. The backend ignores it unless a
	// reviewer is also named, so it only takes effect alongside ReviewerMe or ReviewerMemberIDs.
	ReviewerStatuses []string // PENDING | APPROVED | CHANGES_REQUESTED
}

// OpenIssuesFilter is the default view: issues that are not yet done or dropped.
func OpenIssuesFilter() IssueFilter {
	return IssueFilter{StateCategories: []string{"INITIAL", "ACTIVE"}}
}

// IssuePage is one page of a project's issues under offset pagination.
type IssuePage struct {
	Issues        []IssueSummary
	Page          int
	Size          int
	HasNext       bool
	TotalElements int
	TotalPages    int
}
