package domain

import (
	"testing"
	"time"
)

func TestIssueEditEmpty(t *testing.T) {
	if !(IssueEdit{}).Empty() {
		t.Error("a zero edit should be empty")
	}
	title := "x"
	if (IssueEdit{Title: &title}).Empty() {
		t.Error("a touched field should not be empty")
	}
	if (IssueEdit{ClearDue: true}).Empty() {
		t.Error("clearing the due date should not be empty")
	}
}

func TestToUpdateCommonBodyOnlyTouched(t *testing.T) {
	title, priority := "New title", "P1"
	body := toUpdateCommonBody(IssueEdit{Title: &title, Priority: &priority})

	if !body.Title.IsSpecified() || body.Title.IsNull() || body.Title.MustGet() != "New title" {
		t.Errorf("title should be sent with its value, got %+v", body.Title)
	}
	if !body.Priority.IsSpecified() || body.Priority.MustGet() != "P1" {
		t.Errorf("priority should be sent with its value, got %+v", body.Priority)
	}
	if body.Summary.IsSpecified() || body.Content.IsSpecified() || body.DueAt.IsSpecified() {
		t.Error("untouched fields should be left unspecified (omitted)")
	}
}

func TestToUpdateCommonBodyDue(t *testing.T) {
	cleared := toUpdateCommonBody(IssueEdit{ClearDue: true})
	if !cleared.DueAt.IsSpecified() || !cleared.DueAt.IsNull() {
		t.Errorf("clearing the due date should send an explicit null, got %+v", cleared.DueAt)
	}

	due := time.Date(2026, 8, 15, 0, 0, 0, 0, time.UTC)
	set := toUpdateCommonBody(IssueEdit{DueAt: &due})
	if !set.DueAt.IsSpecified() || set.DueAt.IsNull() || !set.DueAt.MustGet().Equal(due) {
		t.Errorf("setting the due date should send the instant, got %+v", set.DueAt)
	}

	// ClearDue wins over a stray DueAt so a clear is never silently turned into a value
	both := toUpdateCommonBody(IssueEdit{ClearDue: true, DueAt: &due})
	if !both.DueAt.IsNull() {
		t.Error("ClearDue should take precedence and send null")
	}
}
