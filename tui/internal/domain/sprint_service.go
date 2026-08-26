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

// ListProjectSprints returns one 0-based page of all statuses. This endpoint paginates via a Spring
// Pageable, not the flat query params the issue search uses.
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

// StartSprint moves a PLANNING sprint to ACTIVE. The server timestamps the start, so only due is sent.
func (s *SprintService) StartSprint(ctx context.Context, id int64, dueAt time.Time) error {
	resp, err := s.api.StartSprintWithResponse(ctx, id, client.StartSprintRequest{DueAt: dueAt})
	if err != nil {
		return fmt.Errorf("start sprint: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CompleteSprint closes an ACTIVE sprint. Rejected while the sprint still holds incomplete issues.
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

// AddSprintIssues adds issues by key. Rejected when the sprint is closed or an issue is cross-project.
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

// MigrateSprintIssues moves every incomplete issue, with no per-issue selection. Both sprints must be open.
func (s *SprintService) MigrateSprintIssues(ctx context.Context, sourceID, targetID int64) error {
	resp, err := s.api.MigrateSprintIssuesWithResponse(ctx, sourceID, client.MigrateIssuesRequest{NewSprintId: targetID})
	if err != nil {
		return fmt.Errorf("migrate sprint issues: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CreateSprint adds a PLANNING sprint and returns its id. The due date is chosen when the sprint starts.
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

// DeleteSprint permanently removes a sprint. The server allows it only for a CANCELLED one.
func (s *SprintService) DeleteSprint(ctx context.Context, id int64) error {
	resp, err := s.api.DeleteSprintWithResponse(ctx, id)
	if err != nil {
		return fmt.Errorf("delete sprint: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// CurrentSprint returns the project's active sprint, or nil. Avoids loading the whole list for one id.
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

// UpdateSprint PATCHes title, goal, and due. Only set fields are sent. ClearDue nulls the due date.
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
