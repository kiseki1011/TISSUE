package domain

import (
	"context"
	"fmt"
	"time"

	"github.com/oapi-codegen/nullable"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

type SprintService struct {
	api *client.ClientWithResponses
}

func NewSprintService(api *client.ClientWithResponses) *SprintService {
	return &SprintService{api: api}
}

// ListProjectSprints returns one page of a project's sprints (all statuses). page is 0-based. The
// backend paginates via a Spring Pageable, so page/size go through the Pageable struct rather than
// flat query params like the issue search does.
func (s *SprintService) ListProjectSprints(ctx context.Context, projectKey string, page, size int) (SprintPage, error) {
	p, sz := int32(page), int32(size)
	params := &client.ListProjectSprintsParams{Pageable: client.Pageable{Page: &p, Size: &sz}}
	resp, err := s.api.ListProjectSprintsWithResponse(ctx, projectKey, params)
	if err != nil {
		return SprintPage{}, fmt.Errorf("list project sprints: %w", err)
	}
	if resp.JSON200 == nil {
		return SprintPage{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toSprintPage(resp.JSON200), nil
}

// StartSprint moves a PLANNING sprint to ACTIVE with the given due date (Instant). The server sets the
// start timestamp to now, so only the due date is sent.
func (s *SprintService) StartSprint(ctx context.Context, id int64, dueAt time.Time) error {
	resp, err := s.api.StartSprintWithResponse(ctx, id, client.StartSprintRequest{DueAt: dueAt})
	if err != nil {
		return fmt.Errorf("start sprint: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CompleteSprint closes an ACTIVE sprint. The server rejects it when the sprint still holds incomplete
// issues.
func (s *SprintService) CompleteSprint(ctx context.Context, id int64) error {
	resp, err := s.api.CompleteSprintWithResponse(ctx, id)
	if err != nil {
		return fmt.Errorf("complete sprint: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CancelSprint cancels a PLANNING or ACTIVE sprint (moving it to CANCELLED).
func (s *SprintService) CancelSprint(ctx context.Context, id int64) error {
	resp, err := s.api.CancelSprintWithResponse(ctx, id)
	if err != nil {
		return fmt.Errorf("cancel sprint: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// AddSprintIssues adds issues (by key) to a sprint. Rejected when the sprint is closed or an issue
// belongs to a different project.
func (s *SprintService) AddSprintIssues(ctx context.Context, id int64, keys []string) error {
	resp, err := s.api.AddSprintIssuesWithResponse(ctx, id, client.AddSprintIssuesRequest{IssueKeys: keys})
	if err != nil {
		return fmt.Errorf("add sprint issues: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// RemoveSprintIssues removes issues (by key) from a sprint. Rejected when the sprint is closed.
func (s *SprintService) RemoveSprintIssues(ctx context.Context, id int64, keys []string) error {
	resp, err := s.api.RemoveSprintIssuesWithResponse(ctx, id, client.RemoveSprintIssuesRequest{IssueKeys: keys})
	if err != nil {
		return fmt.Errorf("remove sprint issues: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// MigrateSprintIssues carries the source sprint's incomplete issues over to the target sprint. The
// server moves every incomplete issue in the source (there is no per-issue selection); both sprints must
// be open. Rejected with 403 without the MANAGER role.
func (s *SprintService) MigrateSprintIssues(ctx context.Context, sourceID, targetID int64) error {
	resp, err := s.api.MigrateSprintIssuesWithResponse(ctx, sourceID, client.MigrateIssuesRequest{NewSprintId: targetID})
	if err != nil {
		return fmt.Errorf("migrate sprint issues: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CreateSprint adds a sprint to the project in PLANNING, returning its id so the caller can point the
// list at it. The sprint's due date is not set here: it is chosen when the sprint is started.
func (s *SprintService) CreateSprint(ctx context.Context, projectKey, title, goal string) (int64, error) {
	body := client.CreateSprintRequest{Title: title}
	if goal != "" {
		body.Goal = &goal
	}
	resp, err := s.api.CreateSprintWithResponse(ctx, projectKey, body)
	if err != nil {
		return 0, fmt.Errorf("create sprint: %w", err)
	}
	if resp.JSON201 == nil {
		return 0, newAPIError(resp.StatusCode(), resp.Body)
	}
	return derefInt64to64(resp.JSON201.SprintId), nil
}

// DeleteSprint permanently removes a sprint. The server allows this only for a CANCELLED sprint, so a
// sprint with live work cannot be erased.
func (s *SprintService) DeleteSprint(ctx context.Context, id int64) error {
	resp, err := s.api.DeleteSprintWithResponse(ctx, id)
	if err != nil {
		return fmt.Errorf("delete sprint: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CurrentSprint returns the project's active sprint, or nil when there is none. Used by the issue tab's
// "add to current sprint" action, which needs the active sprint's id without loading the whole list.
func (s *SprintService) CurrentSprint(ctx context.Context, projectKey string) (*SprintSummary, error) {
	page, size := int32(0), int32(1)
	statuses := []client.ListProjectSprintsParamsStatuses{client.ListProjectSprintsParamsStatusesACTIVE}
	params := &client.ListProjectSprintsParams{Statuses: &statuses, Pageable: client.Pageable{Page: &page, Size: &size}}
	resp, err := s.api.ListProjectSprintsWithResponse(ctx, projectKey, params)
	if err != nil {
		return nil, fmt.Errorf("current sprint: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	p := toSprintPage(resp.JSON200)
	if len(p.Sprints) == 0 {
		return nil, nil
	}
	sp := p.Sprints[0]
	return &sp, nil
}

// UpdateSprint PATCHes a sprint's title, goal, and/or due date. Only the set fields are sent; ClearDue
// nulls the due date.
func (s *SprintService) UpdateSprint(ctx context.Context, id int64, e SprintEdit) error {
	var body client.UpdateSprintRequest
	if e.Title != nil {
		body.Title = nullable.NewNullableWithValue(*e.Title)
	}
	if e.Goal != nil {
		body.Goal = nullable.NewNullableWithValue(*e.Goal)
	}
	switch {
	case e.ClearDue:
		body.DueAt = nullable.NewNullNullable[time.Time]()
	case e.DueAt != nil:
		body.DueAt = nullable.NewNullableWithValue(*e.DueAt)
	}
	resp, err := s.api.UpdateSprintWithResponse(ctx, id, body)
	if err != nil {
		return fmt.Errorf("update sprint: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func toSprintPage(p *client.PageResponseSprintSummary) SprintPage {
	if p == nil {
		return SprintPage{}
	}
	page := SprintPage{
		Page:          derefInt32(p.Page),
		Size:          derefInt32(p.Size),
		HasNext:       derefBool(p.HasNext),
		TotalElements: derefInt64(p.TotalElements),
		TotalPages:    derefInt32(p.TotalPages),
	}
	if p.Content != nil {
		page.Sprints = make([]SprintSummary, 0, len(*p.Content))
		for _, sp := range *p.Content {
			page.Sprints = append(page.Sprints, toSprintSummary(&sp))
		}
	}
	return page
}

func toSprintSummary(sp *client.SprintSummary) SprintSummary {
	if sp == nil {
		return SprintSummary{}
	}
	return SprintSummary{
		ID:          derefInt64to64(sp.Id),
		Key:         deref(sp.SprintKey),
		Title:       deref(sp.Title),
		Status:      enumStr(sp.Status),
		Goal:        deref(sp.Goal),
		StartedAt:   derefTime(sp.StartedAt),
		DueAt:       derefTime(sp.DueAt),
		CompletedAt: derefTime(sp.CompletedAt),
	}
}
