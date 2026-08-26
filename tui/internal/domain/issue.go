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
	LastActivity  time.Time // most recent activity, comments included. zero when none
	// MyReviewStatus is server-computed: the caller's review state, or "" when they are not a reviewer.
	MyReviewStatus string
}

// IssueFilter maps the issue list's search axes onto the backend's single :search query.
// A zero value carries no filter. The "me" flags resolve server-side to the caller.
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
	AssigneeMemberIDs []string // specific assignees (member id strings), composes with AssigneeMe
	ReviewerMemberIDs []string // specific reviewers (member id strings), composes with ReviewerMe
	// ReviewerStatuses is ignored by the backend unless a reviewer is also named.
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
