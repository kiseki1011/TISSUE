package domain

import (
	"context"
	"fmt"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

type IssueService struct {
	api *client.ClientWithResponses
}

func NewIssueService(api *client.ClientWithResponses) *IssueService {
	return &IssueService{api: api}
}

// page is 0-based.
func (s *IssueService) SearchProjectIssues(ctx context.Context, projectKey string, filter IssueFilter, page, size int) (IssuePage, error) {
	p, sz := int32(page), int32(size)
	params := &client.SearchProjectIssuesParams{Page: &p, Size: &sz}
	applyFilter(params, filter)
	resp, err := s.api.SearchProjectIssuesWithResponse(ctx, projectKey, params)
	if err != nil {
		return IssuePage{}, fmt.Errorf("search project issues: %w", err)
	}
	if resp.JSON200 == nil {
		return IssuePage{}, newAPIError(resp.StatusCode(), resp.Body)
	}
	return toIssuePage(resp.JSON200), nil
}

func applyFilter(params *client.SearchProjectIssuesParams, filter IssueFilter) {
	if filter.Keyword != "" {
		params.Keyword = &filter.Keyword
	}
	if len(filter.StateCategories) > 0 {
		cats := make([]client.SearchProjectIssuesParamsStateCategories, len(filter.StateCategories))
		for i, c := range filter.StateCategories {
			cats[i] = client.SearchProjectIssuesParamsStateCategories(c)
		}
		params.StateCategories = &cats
	}
	if len(filter.Priorities) > 0 {
		pr := make([]client.SearchProjectIssuesParamsPriorities, len(filter.Priorities))
		for i, p := range filter.Priorities {
			pr[i] = client.SearchProjectIssuesParamsPriorities(p)
		}
		params.Priorities = &pr
	}
	if len(filter.IssueTypeIDs) > 0 {
		ids := append([]int64(nil), filter.IssueTypeIDs...)
		params.IssueTypeIds = &ids
	}
	if len(filter.SprintIDs) > 0 {
		ids := append([]int64(nil), filter.SprintIDs...)
		params.SprintIds = &ids
	}
	if filter.CurrentSprintOnly {
		on := true
		params.CurrentSprintOnly = &on
	}
	assignees := append([]string(nil), filter.AssigneeMemberIDs...)
	if filter.AssigneeMe {
		assignees = append(assignees, "me")
	}
	if len(assignees) > 0 {
		params.AssigneeMemberIds = &assignees
	}
	if filter.AuthorMe {
		params.AuthorMemberIds = &[]string{"me"}
	}
	reviewers := append([]string(nil), filter.ReviewerMemberIDs...)
	if filter.ReviewerMe {
		reviewers = append(reviewers, "me")
	}
	if len(reviewers) > 0 {
		params.ReviewerMemberIds = &reviewers
		// only sent alongside a reviewer: the backend drops the status filter when no reviewer is named,
		// so sending it alone would silently widen the result set instead of narrowing it
		if len(filter.ReviewerStatuses) > 0 {
			st := make([]client.SearchProjectIssuesParamsReviewerStatuses, len(filter.ReviewerStatuses))
			for i, v := range filter.ReviewerStatuses {
				st[i] = client.SearchProjectIssuesParamsReviewerStatuses(v)
			}
			params.ReviewerStatuses = &st
		}
	}
}

func toIssuePage(p *client.PageResponseIssueSummary) IssuePage {
	if p == nil {
		return IssuePage{}
	}
	page := IssuePage{
		Page:          derefInt32(p.Page),
		Size:          derefInt32(p.Size),
		HasNext:       derefBool(p.HasNext),
		TotalElements: derefInt64(p.TotalElements),
		TotalPages:    derefInt32(p.TotalPages),
	}
	if p.Content != nil {
		page.Issues = make([]IssueSummary, 0, len(*p.Content))
		for _, it := range *p.Content {
			page.Issues = append(page.Issues, toIssueSummary(&it))
		}
	}
	return page
}

func toIssueSummary(it *client.IssueSummary) IssueSummary {
	if it == nil {
		return IssueSummary{}
	}
	return IssueSummary{
		Key:            deref(it.IssueKey),
		Title:          deref(it.Title),
		TypeName:       deref(it.IssueTypeName),
		TypeColor:      enumStr(it.IssueTypeColor),
		Priority:       enumStr(it.Priority),
		StateLabel:     deref(it.CurrentStateLabel),
		StateCategory:  enumStr(it.CurrentStateCategory),
		Assigned:       it.AssigneeMemberId != nil,
		AssigneeID:     derefInt64to64(it.AssigneeMemberId),
		AssigneeName:   deref(it.AssigneeName),
		StoryPoint:     derefInt32(it.StoryPoint),
		Progress:       derefInt32(it.CountBasedProgress),
		DueAt:          derefTime(it.DueAt),
		SprintID:       derefInt64to64(it.SprintId),
		LastActivity:   derefTime(it.LastActivityAt),
		MyReviewStatus: enumStr(it.MyReviewStatus),
	}
}

// the generated enums are all string aliases.
func enumStr[T ~string](p *T) string {
	if p == nil {
		return ""
	}
	return string(*p)
}
