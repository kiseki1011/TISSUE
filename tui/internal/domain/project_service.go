package domain

import (
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/oapi-codegen/nullable"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

type ProjectService struct {
	api *client.ClientWithResponses
}

func NewProjectService(api *client.ClientWithResponses) *ProjectService {
	return &ProjectService{api: api}
}

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
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toProjects(resp.JSON200.Content), nil
}

type ProjectMember struct {
	MemberID    int64
	DisplayName string
	Username    string
	Email       string // "" when the instance runs with email-required disabled
	Role        string // MANAGER | MEMBER
	JoinedAt    time.Time
	IsAgent     bool   // an AI agent rather than a human member
	OwnerName   string // for an agent, the owning member's display name ("" for a human)
	OwnerUser   string // for an agent, the owning member's username ("" for a human)
}

// Name is the member's display name, falling back to their username.
func (m ProjectMember) Name() string {
	if m.DisplayName != "" {
		return m.DisplayName
	}
	return m.Username
}

// Owner is the agent owner's label, name falling back to username. "" for a human.
func (m ProjectMember) Owner() string {
	if m.OwnerName != "" {
		return m.OwnerName
	}
	return m.OwnerUser
}

// MemberStats is one member's contribution stats. AvgResolveSeconds is nil with no resolved issues.
type MemberStats struct {
	MemberID          int64
	ResolvedCount     int64
	OpenAssignedCount int64
	TotalStoryPoints  int64
	CompletionRate    float64
	AvgResolveSeconds *int64
}

// MemberStats returns per-member contribution stats. The backend returns a row only for members with
// at least one assigned issue, so callers zero-fill the rest from the roster.
func (s *ProjectService) MemberStats(ctx context.Context, projectKey string) ([]MemberStats, error) {
	resp, err := s.api.GetProjectMemberStatsWithResponse(ctx, projectKey)
	if err != nil {
		return nil, fmt.Errorf("get project member stats: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	out := make([]MemberStats, 0, len(*resp.JSON200))
	for _, st := range *resp.JSON200 {
		out = append(out, MemberStats{
			MemberID:          derefInt64to64(st.MemberId),
			ResolvedCount:     derefInt64to64(st.ResolvedCount),
			OpenAssignedCount: derefInt64to64(st.OpenAssignedCount),
			TotalStoryPoints:  derefInt64to64(st.TotalStoryPoints),
			CompletionRate:    derefFloat64(st.CompletionRate),
			AvgResolveSeconds: st.AvgResolveSeconds,
		})
	}
	return out, nil
}

// ListMembers returns a project's active members (the assignable ones) in one generous page.
func (s *ProjectService) ListMembers(ctx context.Context, projectKey string) ([]ProjectMember, error) {
	size := int32(500)
	params := &client.ListProjectMembersParams{Pageable: client.Pageable{Size: &size}}
	resp, err := s.api.ListProjectMembersWithResponse(ctx, projectKey, params)
	if err != nil {
		return nil, fmt.Errorf("list project members: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	var out []ProjectMember
	if resp.JSON200.Content != nil {
		for _, mem := range *resp.JSON200.Content {
			if !derefBool(mem.Active) {
				continue // only active members can be assigned
			}
			out = append(out, ProjectMember{
				MemberID:    derefInt64to64(mem.MemberId),
				DisplayName: deref(mem.DisplayName),
				Username:    deref(mem.Username),
				Email:       deref(mem.Email),
				Role:        enumStr(mem.Role),
				JoinedAt:    derefTime(mem.JoinedAt),
				IsAgent:     enumStr(mem.MemberType) == "AGENT",
				OwnerName:   deref(mem.OwnerName),
				OwnerUser:   deref(mem.OwnerUsername),
			})
		}
	}
	return out, nil
}

// ListMemberCandidates returns people not already in the project, optionally narrowed by a keyword.
// One generous page. The add-member picker filters it further client-side.
func (s *ProjectService) ListMemberCandidates(ctx context.Context, projectKey, keyword string) ([]ProjectMember, error) {
	size := int32(100)
	params := &client.ListMemberCandidatesParams{Pageable: client.Pageable{Size: &size}}
	if kw := strings.TrimSpace(keyword); kw != "" {
		params.Keyword = &kw
	}
	resp, err := s.api.ListMemberCandidatesWithResponse(ctx, projectKey, params)
	if err != nil {
		return nil, fmt.Errorf("list member candidates: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	var out []ProjectMember
	if resp.JSON200.Content != nil {
		for _, c := range *resp.JSON200.Content {
			out = append(out, ProjectMember{
				MemberID:    derefInt64to64(c.MemberId),
				DisplayName: deref(c.DisplayName),
				Username:    deref(c.Username),
				Email:       deref(c.Email),
			})
		}
	}
	return out, nil
}

// AddProjectMembers adds members (by member id) to the project in one batch. Manager-only server-side.
func (s *ProjectService) AddProjectMembers(ctx context.Context, projectKey string, memberIDs []int64) error {
	body := client.AddProjectMembersRequest{TargetMemberIds: memberIDs}
	resp, err := s.api.AddProjectMembersWithResponse(ctx, projectKey, body)
	if err != nil {
		return fmt.Errorf("add project members: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// UpdateMemberRole changes a member's project role (MANAGER or MEMBER). Manager-only server-side.
func (s *ProjectService) UpdateMemberRole(ctx context.Context, projectKey string, memberID int64, role string) error {
	body := client.ChangeRoleRequest{Role: client.ChangeRoleRequestRole(role)}
	resp, err := s.api.UpdateProjectMemberRoleWithResponse(ctx, projectKey, memberID, body)
	if err != nil {
		return fmt.Errorf("update member role: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// KickProjectMember removes a member from the project. Manager-only server-side.
func (s *ProjectService) KickProjectMember(ctx context.Context, projectKey string, memberID int64) error {
	resp, err := s.api.KickProjectMemberWithResponse(ctx, projectKey, memberID)
	if err != nil {
		return fmt.Errorf("kick project member: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func (s *ProjectService) GetProjectStats(ctx context.Context, projectKey string) (ProjectStats, error) {
	resp, err := s.api.GetProjectSimpleStatsWithResponse(ctx, projectKey)
	if err != nil {
		return ProjectStats{}, fmt.Errorf("get project stats: %w", err)
	}
	if resp.JSON200 == nil {
		return ProjectStats{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toStats(resp.JSON200), nil
}

// JoinProject self-joins a PUBLIC project. A PRIVATE one rejects it with 403 PROJECT_JOIN_NOT_ALLOWED.
func (s *ProjectService) JoinProject(ctx context.Context, projectKey string) error {
	resp, err := s.api.JoinProjectWithResponse(ctx, projectKey)
	if err != nil {
		return fmt.Errorf("join project: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func (s *ProjectService) GetProjectDetail(ctx context.Context, projectKey string) (Project, error) {
	resp, err := s.api.GetProjectDetailWithResponse(ctx, projectKey)
	if err != nil {
		return Project{}, fmt.Errorf("get project detail: %w", err)
	}
	if resp.JSON200 == nil {
		return Project{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	d := resp.JSON200
	vis := ""
	if d.Visibility != nil {
		vis = string(*d.Visibility)
	}
	return Project{
		Key:         deref(d.Key),
		Title:       deref(d.Title),
		Description: deref(d.Description),
		Visibility:  vis,
		Archived:    derefBool(d.Archived),
		CreatedAt:   derefTime(d.CreatedAt),
		UpdatedAt:   derefTime(d.LastUpdatedAt),
	}, nil
}

// ProjectEdit is a settings PATCH. Only set fields are sent, and ClearDescription empties the description.
type ProjectEdit struct {
	Title            *string
	Description      *string
	ClearDescription bool
	Visibility       *string // "PUBLIC" | "PRIVATE"
}

func (e ProjectEdit) Empty() bool {
	return e.Title == nil && e.Description == nil && !e.ClearDescription && e.Visibility == nil
}

// UpdateProject sends a project settings PATCH. Manager/owner-gated server-side.
func (s *ProjectService) UpdateProject(ctx context.Context, projectKey string, e ProjectEdit) error {
	var body client.UpdateProjectRequest
	if e.Title != nil {
		body.Title = nullable.NewNullableWithValue(*e.Title)
	}
	switch {
	case e.ClearDescription:
		body.Description = nullable.NewNullNullable[string]()
	case e.Description != nil:
		body.Description = nullable.NewNullableWithValue(*e.Description)
	}
	if e.Visibility != nil {
		body.ProjectVisibility = nullable.NewNullableWithValue(*e.Visibility)
	}
	resp, err := s.api.UpdateProjectWithResponse(ctx, projectKey, body)
	if err != nil {
		return fmt.Errorf("update project: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

// ArchiveProject makes a project and every item in it read-only (reversible via UnarchiveProject).
func (s *ProjectService) ArchiveProject(ctx context.Context, projectKey string) error {
	resp, err := s.api.ArchiveProjectWithResponse(ctx, projectKey)
	if err != nil {
		return fmt.Errorf("archive project: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

func (s *ProjectService) UnarchiveProject(ctx context.Context, projectKey string) error {
	resp, err := s.api.UnarchiveProjectWithResponse(ctx, projectKey)
	if err != nil {
		return fmt.Errorf("unarchive project: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}

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
		return "", newAPIError(resp.StatusCode(), resp.Body)
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

func derefFloat64(p *float64) float64 {
	if p == nil {
		return 0
	}
	return *p
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

// AgingStats is the open-issue aging snapshot. Age runs from when work started, else from creation.
// Blocked counts open issues held by a still-open blocker.
type AgingStats struct {
	OpenTotal int
	Under3d   int
	Days3to7  int
	Weeks1to2 int
	Over2w    int
	Blocked   int
}

// DurationStat summarizes durations in seconds: count, mean, p50, p90.
type DurationStat struct {
	Count      int
	AvgSeconds int
	P50Seconds int
	P90Seconds int
}

// CycleTimeStats holds cycle time (work start to resolution) and lead time (creation to resolution).
type CycleTimeStats struct {
	Window string
	Cycle  DurationStat
	Lead   DurationStat
}

type FlowDay struct {
	Date     time.Time
	Created  int
	Resolved int
}

// FlowStats is the dense, one-per-day created-vs-resolved series over a window.
type FlowStats struct {
	Window string
	Days   []FlowDay
}

// GetProjectAging loads the open-issue aging + blocked snapshot. Member-only server-side.
func (s *ProjectService) GetProjectAging(ctx context.Context, projectKey string) (AgingStats, error) {
	resp, err := s.api.GetProjectAgingStatsWithResponse(ctx, projectKey)
	if err != nil {
		return AgingStats{}, fmt.Errorf("get project aging: %w", err)
	}
	if resp.JSON200 == nil {
		return AgingStats{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toAging(resp.JSON200), nil
}

// GetProjectCycleTime loads cycle & lead time stats over the given window (week|month|sprint). Member-only.
func (s *ProjectService) GetProjectCycleTime(ctx context.Context, projectKey, window string) (CycleTimeStats, error) {
	resp, err := s.api.GetProjectCycleTimeStatsWithResponse(
		ctx, projectKey, &client.GetProjectCycleTimeStatsParams{Window: &window})
	if err != nil {
		return CycleTimeStats{}, fmt.Errorf("get project cycle time: %w", err)
	}
	if resp.JSON200 == nil {
		return CycleTimeStats{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toCycleTime(resp.JSON200), nil
}

// GetProjectFlow loads the created-vs-resolved daily series. Days are cut on the viewer's timezone
// (see LocalZoneID). Without it the server buckets on UTC, shifting every point. Member-only.
func (s *ProjectService) GetProjectFlow(ctx context.Context, projectKey, window string) (FlowStats, error) {
	zone := LocalZoneID()
	resp, err := s.api.GetProjectFlowStatsWithResponse(
		ctx, projectKey, &client.GetProjectFlowStatsParams{Window: &window, ZoneId: &zone})
	if err != nil {
		return FlowStats{}, fmt.Errorf("get project flow: %w", err)
	}
	if resp.JSON200 == nil {
		return FlowStats{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toFlow(resp.JSON200), nil
}

func toAging(a *client.ProjectAgingStats) AgingStats {
	if a == nil {
		return AgingStats{}
	}
	return AgingStats{
		OpenTotal: derefInt64(a.OpenTotal),
		Under3d:   derefInt64(a.AgingUnder3d),
		Days3to7:  derefInt64(a.Aging3to7d),
		Weeks1to2: derefInt64(a.Aging1to2w),
		Over2w:    derefInt64(a.AgingOver2w),
		Blocked:   derefInt64(a.Blocked),
	}
}

func toCycleTime(c *client.ProjectCycleTimeStats) CycleTimeStats {
	if c == nil {
		return CycleTimeStats{}
	}
	win := ""
	if c.Window != nil {
		win = string(*c.Window)
	}
	return CycleTimeStats{Window: win, Cycle: toDurationStat(c.CycleTime), Lead: toDurationStat(c.LeadTime)}
}

func toDurationStat(d *client.DurationStats) DurationStat {
	if d == nil {
		return DurationStat{}
	}
	return DurationStat{
		Count:      derefInt64(d.Count),
		AvgSeconds: derefInt64(d.AvgSeconds),
		P50Seconds: derefInt64(d.P50Seconds),
		P90Seconds: derefInt64(d.P90Seconds),
	}
}

func toFlow(f *client.ProjectFlowStats) FlowStats {
	if f == nil {
		return FlowStats{}
	}
	win := ""
	if f.Window != nil {
		win = string(*f.Window)
	}
	out := FlowStats{Window: win}
	if f.Points != nil {
		out.Days = make([]FlowDay, 0, len(*f.Points))
		for _, p := range *f.Points {
			day := FlowDay{Created: derefInt64(p.Created), Resolved: derefInt64(p.Resolved)}
			if p.Date != nil {
				day.Date = p.Date.Time
			}
			out.Days = append(out.Days, day)
		}
	}
	return out
}

// SprintReport is a snapshot of one sprint's scope as it stands now, not a burndown: the server keeps
// no scope history. The state distribution is dropped, since OpenIssues is already the still-open work.
type SprintReport struct {
	TotalIssues          int
	CompletedIssues      int
	OpenIssues           int // still-open (INITIAL + ACTIVE) - the work that would carry over
	CompletionRate       float64
	TotalStoryPoints     int
	CompletedStoryPoints int
	PointsCompletionRate float64
}

// GetSprintReport loads the report for one sprint. Member-only server-side.
func (s *ProjectService) GetSprintReport(ctx context.Context, projectKey string, sprintID int64) (SprintReport, error) {
	resp, err := s.api.GetProjectSprintReportWithResponse(
		ctx, projectKey, &client.GetProjectSprintReportParams{SprintId: sprintID})
	if err != nil {
		return SprintReport{}, fmt.Errorf("get sprint report: %w", err)
	}
	if resp.JSON200 == nil {
		return SprintReport{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toSprintReport(resp.JSON200), nil
}

func toSprintReport(r *client.ProjectSprintReport) SprintReport {
	if r == nil {
		return SprintReport{}
	}
	return SprintReport{
		TotalIssues:          derefInt64(r.TotalIssues),
		CompletedIssues:      derefInt64(r.CompletedIssues),
		OpenIssues:           derefInt64(r.OpenIssues),
		CompletionRate:       derefFloat64(r.CompletionRate),
		TotalStoryPoints:     derefInt64(r.TotalStoryPoints),
		CompletedStoryPoints: derefInt64(r.CompletedStoryPoints),
		PointsCompletionRate: derefFloat64(r.PointsCompletionRate),
	}
}

// Velocity is delivered work per COMPLETED sprint, oldest first, plus the mean over them.
// Each point is that sprint's final tally. EPICs are excluded from the story points.
type Velocity struct {
	Sprints                []VelocityPoint
	AverageStoryPoints     float64
	AverageCompletedIssues float64
}

type VelocityPoint struct {
	SprintID             int64
	SprintKey            string
	Title                string
	CompletedAt          time.Time // zero when not recorded
	CompletedIssues      int
	CompletedStoryPoints int
}

// GetProjectVelocity loads the per-sprint velocity series. Member-only server-side.
func (s *ProjectService) GetProjectVelocity(ctx context.Context, projectKey string) (Velocity, error) {
	resp, err := s.api.GetProjectVelocityWithResponse(ctx, projectKey)
	if err != nil {
		return Velocity{}, fmt.Errorf("get project velocity: %w", err)
	}
	if resp.JSON200 == nil {
		return Velocity{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toVelocity(resp.JSON200), nil
}

func toVelocity(v *client.ProjectVelocity) Velocity {
	if v == nil {
		return Velocity{}
	}
	out := Velocity{
		AverageStoryPoints:     derefFloat64(v.AverageStoryPoints),
		AverageCompletedIssues: derefFloat64(v.AverageCompletedIssues),
	}
	if v.Sprints != nil {
		out.Sprints = make([]VelocityPoint, 0, len(*v.Sprints))
		for _, p := range *v.Sprints {
			pt := VelocityPoint{
				SprintID:             derefInt64to64(p.SprintId),
				SprintKey:            deref(p.SprintKey),
				Title:                deref(p.Title),
				CompletedIssues:      derefInt64(p.CompletedIssues),
				CompletedStoryPoints: derefInt64(p.CompletedStoryPoints),
			}
			if p.CompletedAt != nil {
				pt.CompletedAt = *p.CompletedAt
			}
			out.Sprints = append(out.Sprints, pt)
		}
	}
	return out
}

// Contributions is a per-member resolution heatmap ("잔디"): zero-filled resolved counts per day, oldest
// first, plus the busiest day for shading. Attribution is by the issue's current assignee.
type Contributions struct {
	Days          []ContributionDay
	TotalResolved int
	MaxDaily      int
}

type ContributionDay struct {
	Date  time.Time // calendar day, as midnight UTC
	Count int
}

// GetProjectContributions loads a member's daily heatmap over the last `days` days (server clamps to a
// year). Days are cut on the viewer's timezone, so a morning's work lands on their day, not UTC's.
func (s *ProjectService) GetProjectContributions(
	ctx context.Context, projectKey string, memberID int64, days int,
) (Contributions, error) {
	d := int32Of(days)
	zone := LocalZoneID()
	resp, err := s.api.GetProjectContributionsWithResponse(
		ctx, projectKey, &client.GetProjectContributionsParams{MemberId: memberID, Days: &d, ZoneId: &zone})
	if err != nil {
		return Contributions{}, fmt.Errorf("get project contributions: %w", err)
	}
	if resp.JSON200 == nil {
		return Contributions{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toContributions(resp.JSON200), nil
}

func toContributions(c *client.ProjectContributionStats) Contributions {
	if c == nil {
		return Contributions{}
	}
	out := Contributions{TotalResolved: derefInt64(c.TotalResolved), MaxDaily: derefInt64(c.MaxDaily)}
	if c.Days != nil {
		out.Days = make([]ContributionDay, 0, len(*c.Days))
		for _, d := range *c.Days {
			cd := ContributionDay{Count: derefInt64(d.Count)}
			if d.Date != nil {
				cd.Date = d.Date.Time
			}
			out.Days = append(out.Days, cd)
		}
	}
	return out
}

// GithubIntegration is a project's GitHub webhook status. Connected is false when no integration exists
// yet (the server 404s). WebhookURL is the endpoint to register in GitHub.
type GithubIntegration struct {
	Connected   bool
	WebhookURL  string
	SyncEnabled bool
}

// GithubSecret is the one-time reveal of the signing secret. The server will not show it again.
type GithubSecret struct {
	WebhookURL string
	Secret     string
}

// GetGithubIntegration reports a 404 as a not-connected status rather than an error.
func (s *ProjectService) GetGithubIntegration(ctx context.Context, projectKey string) (GithubIntegration, error) {
	resp, err := s.api.GetGithubIntegrationWithResponse(ctx, projectKey)
	if err != nil {
		return GithubIntegration{}, fmt.Errorf("get github integration: %w", err)
	}
	if resp.StatusCode() == 404 {
		return GithubIntegration{Connected: false}, nil
	}
	if resp.JSON200 == nil {
		return GithubIntegration{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return GithubIntegration{
		Connected:   true,
		WebhookURL:  deref(resp.JSON200.WebhookUrl),
		SyncEnabled: derefBool(resp.JSON200.IsSyncEnabled),
	}, nil
}

// RegenerateGithubSecret creates the integration if needed and returns the fresh secret once.
func (s *ProjectService) RegenerateGithubSecret(ctx context.Context, projectKey string) (GithubSecret, error) {
	resp, err := s.api.RegenerateGithubSecretWithResponse(ctx, projectKey)
	if err != nil {
		return GithubSecret{}, fmt.Errorf("regenerate github secret: %w", err)
	}
	if resp.JSON200 == nil {
		return GithubSecret{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return GithubSecret{WebhookURL: deref(resp.JSON200.WebhookUrl), Secret: deref(resp.JSON200.Secret)}, nil
}

// SetGithubSync pauses or resumes acting on webhooks. The secret stays, so resuming needs no re-registration.
func (s *ProjectService) SetGithubSync(ctx context.Context, projectKey string, enabled bool) (GithubIntegration, error) {
	resp, err := s.api.UpdateGithubSyncWithResponse(ctx, projectKey, client.UpdateVcsSyncRequest{SyncEnabled: enabled})
	if err != nil {
		return GithubIntegration{}, fmt.Errorf("update github sync: %w", err)
	}
	if resp.JSON200 == nil {
		return GithubIntegration{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return GithubIntegration{
		Connected:   true,
		WebhookURL:  deref(resp.JSON200.WebhookUrl),
		SyncEnabled: derefBool(resp.JSON200.IsSyncEnabled),
	}, nil
}

// WebhookDelivery is one inbound webhook and how it was handled. Detail is what diagnoses a silently
// inert integration.
type WebhookDelivery struct {
	EventType     string
	Status        string // PROCESSED | IGNORED | FAILED | DEAD | RECEIVED
	Detail        string // the result detail, or the failure reason when the delivery failed
	AttemptCount  int
	ReceivedAt    time.Time
	NextAttemptAt time.Time // zero unless a retry is pending
}

// ListGithubDeliveries requires a project manager, so a plain member gets a 403 to treat as "not available".
func (s *ProjectService) ListGithubDeliveries(ctx context.Context, projectKey string, size int32) ([]WebhookDelivery, error) {
	params := &client.ListGithubDeliveriesParams{Pageable: client.Pageable{Size: &size}}
	resp, err := s.api.ListGithubDeliveriesWithResponse(ctx, projectKey, params)
	if err != nil {
		return nil, fmt.Errorf("list github deliveries: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, newAPIError(resp.StatusCode(), resp.Body)
	}
	var out []WebhookDelivery
	if resp.JSON200.Content != nil {
		for _, d := range *resp.JSON200.Content {
			// a failed delivery has no result detail yet, so its error stands in as the reason
			detail := deref(d.ResultDetail)
			if detail == "" {
				detail = deref(d.LastError)
			}
			out = append(out, WebhookDelivery{
				EventType:     deref(d.EventType),
				Status:        enumStr(d.Status),
				Detail:        detail,
				AttemptCount:  derefInt32(d.AttemptCount),
				ReceivedAt:    derefTime(d.ReceivedAt),
				NextAttemptAt: derefTime(d.NextAttemptAt),
			})
		}
	}
	return out, nil
}

func (s *ProjectService) RemoveGithubIntegration(ctx context.Context, projectKey string) error {
	resp, err := s.api.RemoveGithubIntegrationWithResponse(ctx, projectKey)
	if err != nil {
		return fmt.Errorf("remove github integration: %w", err)
	}
	return apiError(resp.StatusCode(), resp.Body)
}
