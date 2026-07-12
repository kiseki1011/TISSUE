package domain

import "time"

type Project struct {
	Key          string
	Title        string
	Description  string
	Visibility   string
	Archived     bool
	CreatedAt    time.Time
	UpdatedAt    time.Time
	LastActivity time.Time
	MemberCount  int
	MyRole       string
}

// ProjectStats is the snapshot issue statistics for a project.
type ProjectStats struct {
	Total       int
	Open        int
	Completed   int
	Unassigned  int
	Overdue     int
	ByState     []StatBucket
	ByHierarchy []StatBucket
	ByPriority  []StatBucket
}

// StatBucket is one labelled count in a stat breakdown.
type StatBucket struct {
	Label string
	Count int
}
