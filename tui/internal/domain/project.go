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

type StatBucket struct {
	Label string
	Count int
}
