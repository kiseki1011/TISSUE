package domain

import (
	"testing"
	"time"
)

// toCreateBody sends the required fields and omits optional scalars left at their zero value.
func TestToCreateBodyOmitsEmptyOptionals(t *testing.T) {
	body := toCreateBody(CreateIssueInput{IssueTypeID: 7, Title: "Fix", Priority: "P1"})
	if body.IssueTypeId != 7 || body.Title != "Fix" || string(body.Priority) != "P1" {
		t.Errorf("required fields wrong: %+v", body)
	}
	if body.Summary != nil || body.Content != nil || body.DueAt != nil {
		t.Errorf("empty optionals should be omitted, got summary=%v content=%v due=%v", body.Summary, body.Content, body.DueAt)
	}
}

// A fully populated input carries every optional through.
func TestToCreateBodyIncludesSetOptionals(t *testing.T) {
	due := time.Date(2026, 9, 1, 0, 0, 0, 0, time.UTC)
	body := toCreateBody(CreateIssueInput{
		IssueTypeID: 3, Title: "T", Priority: "P0",
		Summary: "sum", Content: "# body", DueAt: due,
	})
	if body.Summary == nil || *body.Summary != "sum" {
		t.Errorf("summary not carried: %v", body.Summary)
	}
	if body.Content == nil || *body.Content != "# body" {
		t.Errorf("content not carried: %v", body.Content)
	}
	if body.DueAt == nil || !body.DueAt.Equal(due) {
		t.Errorf("due not carried: %v", body.DueAt)
	}
}

// A parent key is sent when set, omitted when empty.
func TestToCreateBodyParent(t *testing.T) {
	with := toCreateBody(CreateIssueInput{IssueTypeID: 1, Title: "T", Priority: "P2", ParentKey: "ENG-2"})
	if with.ParentIssueKey == nil || *with.ParentIssueKey != "ENG-2" {
		t.Errorf("parent key not carried: %v", with.ParentIssueKey)
	}
	without := toCreateBody(CreateIssueInput{IssueTypeID: 1, Title: "T", Priority: "P2"})
	if without.ParentIssueKey != nil {
		t.Errorf("an empty parent key should be omitted, got %v", without.ParentIssueKey)
	}
}

// Custom fields are sent (keyed by field id) when set, omitted when empty.
func TestToCreateBodyCustomFields(t *testing.T) {
	with := toCreateBody(CreateIssueInput{IssueTypeID: 1, Title: "T", Priority: "P2", CustomFields: map[string]interface{}{"10": int64(2)}})
	if with.CustomFields == nil || (*with.CustomFields)["10"] != int64(2) {
		t.Errorf("custom fields not carried: %v", with.CustomFields)
	}
	without := toCreateBody(CreateIssueInput{IssueTypeID: 1, Title: "T", Priority: "P2"})
	if without.CustomFields != nil {
		t.Errorf("empty custom fields should be omitted, got %v", without.CustomFields)
	}
}
