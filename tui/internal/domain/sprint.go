package domain

import "time"

// SprintSummary is one row in a project's sprint list. Status is PLANNING | ACTIVE | COMPLETED | CANCELLED.
// Started and due are set when a sprint starts, completed when it finishes, zero until then.
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

// SprintEdit is a PATCH of the fields the edit form can change. A nil pointer leaves the field untouched.
// ClearDue nulls the due date, which is distinct from leaving it as is.
type SprintEdit struct {
	Title    *string
	Goal     *string
	DueAt    *time.Time
	ClearDue bool
}

func (e SprintEdit) Empty() bool {
	return e.Title == nil && e.Goal == nil && e.DueAt == nil && !e.ClearDue
}
