package domain

import (
	"context"
	"fmt"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// ProjectService performs project calls against the API
type ProjectService struct {
	api *client.ClientWithResponses
}

func NewProjectService(api *client.ClientWithResponses) *ProjectService {
	return &ProjectService{api: api}
}

// ListProjects returns the projects visible to the caller.
// One generous project list page.
// TODO: Might need to change to server-side project search if there is a chance of +1000 projects.
func (s *ProjectService) ListProjects(ctx context.Context, includeArchived bool) ([]Project, error) {
	size := int32(1000)
	inc := includeArchived
	resp, err := s.api.ListProjectsWithResponse(ctx, &client.ListProjectsParams{
		IncludeArchived: &inc,
		Pageable:        client.Pageable{Size: &size},
	})
	if err != nil {
		return nil, fmt.Errorf("list projects: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, &APIError{Status: resp.StatusCode()}
	}
	return toProjects(resp.JSON200.Content), nil
}

// GetProjectStats returns snapshot issue statistics for a project.
func (s *ProjectService) GetProjectStats(ctx context.Context, projectKey string) (ProjectStats, error) {
	resp, err := s.api.GetProjectSimpleStatsWithResponse(ctx, projectKey)
	if err != nil {
		return ProjectStats{}, fmt.Errorf("get project stats: %w", err)
	}
	if resp.JSON200 == nil {
		return ProjectStats{}, &APIError{Status: resp.StatusCode()}
	}
	return toStats(resp.JSON200), nil
}

// CreateProject creates a project and returns its key.
func (s *ProjectService) CreateProject(ctx context.Context, key, title, description string) (string, error) {
	body := client.CreateProjectRequest{ProjectKey: key, Title: title}
	if description != "" {
		body.Description = &description
	}
	resp, err := s.api.CreateProjectWithResponse(ctx, body)
	if err != nil {
		return "", fmt.Errorf("create project: %w", err)
	}
	if resp.JSON201 == nil || resp.JSON201.ProjectKey == nil {
		return "", &APIError{Status: resp.StatusCode()}
	}
	return *resp.JSON201.ProjectKey, nil
}

func toProjects(content *[]client.ProjectSummary) []Project {
	if content == nil {
		return nil
	}
	out := make([]Project, 0, len(*content))
	for _, p := range *content {
		out = append(out, Project{
			Key:          deref(p.Key),
			Title:        deref(p.Title),
			Description:  deref(p.Description),
			Visibility:   visibility(p.Visibility),
			Archived:     derefBool(p.Archived),
			CreatedAt:    derefTime(p.CreatedAt),
			UpdatedAt:    derefTime(p.LastUpdatedAt),
			LastActivity: derefTime(p.LastActivityAt),
			MemberCount:  memberCount(p.MemberCount),
			MyRole:       role(p.MyRole),
		})
	}
	return out
}

func visibility(p *client.ProjectSummaryVisibility) string {
	if p == nil {
		return ""
	}
	return string(*p)
}

func memberCount(p *int64) int {
	if p == nil {
		return 0
	}
	return int(*p)
}

// role is the caller's project role, empty if not a member
func role(p *client.ProjectSummaryMyRole) string {
	if p == nil {
		return ""
	}
	return string(*p)
}

func derefTime(p *time.Time) time.Time {
	if p == nil {
		return time.Time{}
	}
	return *p
}

func derefInt64(p *int64) int {
	if p == nil {
		return 0
	}
	return int(*p)
}

func toStats(s *client.ProjectSimpleStats) ProjectStats {
	if s == nil {
		return ProjectStats{}
	}
	return ProjectStats{
		Total:       derefInt64(s.Total),
		Open:        derefInt64(s.Open),
		Completed:   derefInt64(s.Completed),
		Unassigned:  derefInt64(s.Unassigned),
		Overdue:     derefInt64(s.Overdue),
		ByState:     categoryBuckets(s.ByStateCategory),
		ByHierarchy: hierarchyBuckets(s.ByHierarchy),
		ByPriority:  priorityBuckets(s.ByPriority),
	}
}

func categoryBuckets(rows *[]client.CategoryCount) []StatBucket {
	if rows == nil {
		return nil
	}
	out := make([]StatBucket, 0, len(*rows))
	for _, r := range *rows {
		label := ""
		if r.Category != nil {
			label = string(*r.Category)
		}
		out = append(out, StatBucket{Label: label, Count: derefInt64(r.Count)})
	}
	return out
}

func hierarchyBuckets(rows *[]client.HierarchyCount) []StatBucket {
	if rows == nil {
		return nil
	}
	out := make([]StatBucket, 0, len(*rows))
	for _, r := range *rows {
		label := ""
		if r.Hierarchy != nil {
			label = string(*r.Hierarchy)
		}
		out = append(out, StatBucket{Label: label, Count: derefInt64(r.Count)})
	}
	return out
}

func priorityBuckets(rows *[]client.PriorityCount) []StatBucket {
	if rows == nil {
		return nil
	}
	out := make([]StatBucket, 0, len(*rows))
	for _, r := range *rows {
		label := ""
		if r.Priority != nil {
			label = string(*r.Priority)
		}
		out = append(out, StatBucket{Label: label, Count: derefInt64(r.Count)})
	}
	return out
}
