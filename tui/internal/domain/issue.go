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
	StoryPoint    int // 0 when unset
	Progress      int // count-based progress percentage
	DueAt         time.Time
	SprintID      int64
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
