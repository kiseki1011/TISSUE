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

// keyword is optional (empty = all) — page is 0-based.
func (s *IssueService) SearchProjectIssues(ctx context.Context, projectKey, keyword string, page, size int) (IssuePage, error) {
	p, sz := int32(page), int32(size)
	params := &client.SearchProjectIssuesParams{Page: &p, Size: &sz}
	if keyword != "" {
		params.Keyword = &keyword
	}
	resp, err := s.api.SearchProjectIssuesWithResponse(ctx, projectKey, params)
	if err != nil {
		return IssuePage{}, fmt.Errorf("search project issues: %w", err)
	}
	if resp.JSON200 == nil {
		return IssuePage{}, &APIError{Status: resp.StatusCode()}
	}
	return toIssuePage(resp.JSON200), nil
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
		Key:           deref(it.IssueKey),
		Title:         deref(it.Title),
		TypeName:      deref(it.IssueTypeName),
		TypeColor:     enumStr(it.IssueTypeColor),
		Priority:      enumStr(it.Priority),
		StateLabel:    deref(it.CurrentStateLabel),
		StateCategory: enumStr(it.CurrentStateCategory),
		Assigned:      it.AssigneeMemberId != nil,
		AssigneeID:    derefInt64to64(it.AssigneeMemberId),
		StoryPoint:    derefInt32(it.StoryPoint),
		Progress:      derefInt32(it.CountBasedProgress),
		DueAt:         derefTime(it.DueAt),
		SprintID:      derefInt64to64(it.SprintId),
	}
}

// the generated enums are all string aliases.
func enumStr[T ~string](p *T) string {
	if p == nil {
		return ""
	}
	return string(*p)
}
