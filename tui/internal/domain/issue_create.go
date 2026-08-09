package domain

import (
	"context"
	"fmt"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// CreateIssueInput is the core new-issue payload. Optional scalars are the zero value when unset (an
// empty string / zero time), which CreateIssue omits from the request. Parent, custom fields, assignee
// and story point are later slices.
type CreateIssueInput struct {
	IssueTypeID int64
	Title       string
	Summary     string
	Content     string
	Priority    string    // P0 .. P4
	DueAt       time.Time // zero = no due date
	ParentKey   string    // "" = no parent

	// CustomFields maps a field-definition id (as a string) to its already type-serialized value (a
	// number for INTEGER/PERCENTAGE/SELECT_OPTION, a string for the text/date/timestamp/decimal types, a
	// bool for BOOLEAN, a map[string]bool for CHECKLIST). Only set fields are present.
	CustomFields map[string]interface{}
}

// toCreateBody maps the input to the request body, omitting optional scalars left at their zero value.
func toCreateBody(in CreateIssueInput) client.CreateIssueRequest {
	body := client.CreateIssueRequest{
		IssueTypeId: in.IssueTypeID,
		Title:       in.Title,
		Priority:    client.CreateIssueRequestPriority(in.Priority),
	}
	if in.Summary != "" {
		v := in.Summary
		body.Summary = &v
	}
	if in.Content != "" {
		v := in.Content
		body.Content = &v
	}
	if !in.DueAt.IsZero() {
		v := in.DueAt
		body.DueAt = &v
	}
	if in.ParentKey != "" {
		v := in.ParentKey
		body.ParentIssueKey = &v
	}
	if len(in.CustomFields) > 0 {
		cf := in.CustomFields
		body.CustomFields = &cf
	}
	return body
}

// CreateIssue creates an issue in the project and returns its new key.
func (s *IssueService) CreateIssue(ctx context.Context, projectKey string, in CreateIssueInput) (string, error) {
	resp, err := s.api.CreateIssueWithResponse(ctx, projectKey, toCreateBody(in))
	if err != nil {
		return "", fmt.Errorf("create issue: %w", err)
	}
	if err := apiError(resp.StatusCode(), resp.Body); err != nil {
		return "", err
	}
	if resp.JSON201 == nil || resp.JSON201.IssueKey == nil {
		return "", fmt.Errorf("create issue: no key in response")
	}
	return *resp.JSON201.IssueKey, nil
}
