package domain

import "time"

// SprintSummary is one row in a project's sprint list (the listProjectSprints projection). The status
// is one of PLANNING | ACTIVE | COMPLETED | CANCELLED; the timestamps are zero until the lifecycle
// reaches them (started/due are set when a sprint starts, completed when it finishes).
type SprintSummary struct {
	ID          int64
	Key         string
	Title       string
	Status      string
	Goal        string
	StartedAt   time.Time
	DueAt       time.Time
	CompletedAt time.Time
}

// SprintPage is one page of a project's sprints under offset pagination, mirroring IssuePage.
type SprintPage struct {
	Sprints       []SprintSummary
	Page          int
	Size          int
	HasNext       bool
	TotalElements int
	TotalPages    int
}

// SprintEdit is the set of sprint fields the edit form can change (a PATCH). A nil pointer leaves the
// field untouched; ClearDue sets the due date to null (distinct from leaving it as is).
type SprintEdit struct {
	Title    *string
	Goal     *string
	DueAt    *time.Time
	ClearDue bool
}

func (e SprintEdit) Empty() bool {
	return e.Title == nil && e.Goal == nil && e.DueAt == nil && !e.ClearDue
}
