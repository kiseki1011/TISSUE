package domain

import (
	"testing"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

func TestToActivityPage(t *testing.T) {
	page := &client.CursorPageActivityLogResponse{
		HasNext: ptr(true),
		Content: &[]client.ActivityLogResponse{
			{
				Id: ptr(int64(9)), Type: ptr(client.ActivityLogResponseTypeISSUEUPDATED),
				ActorMemberId: ptr(int64(5)),
				Changes: &map[string]client.FieldChange{
					"title":    {From: "old", To: "new"},
					"priority": {From: "P2", To: "P0"},
				},
			},
			{Id: ptr(int64(1)), Type: ptr(client.ActivityLogResponseTypeISSUECREATED)}, // system actor, no changes
		},
	}

	got := toActivityPage(page)

	if !got.HasNext || len(got.Items) != 2 {
		t.Fatalf("page metadata/items wrong: hasNext=%v items=%d", got.HasNext, len(got.Items))
	}
	a := got.Items[0]
	if a.ID != 9 || a.Type != "ISSUE_UPDATED" || a.ActorID != 5 {
		t.Errorf("first activity mapped wrong: %+v", a)
	}
	if len(a.Changes) != 2 || a.Changes[0].Field != "priority" || a.Changes[1].Field != "title" {
		t.Errorf("changes should be sorted by field, got %+v", a.Changes)
	}
	if a.Changes[0].From != "P2" || a.Changes[0].To != "P0" {
		t.Errorf("change values wrong: %+v", a.Changes[0])
	}
	if got.Items[1].ActorID != 0 {
		t.Errorf("a missing actor should map to 0 (system), got %d", got.Items[1].ActorID)
	}
}

func TestChangeVal(t *testing.T) {
	if got := changeVal(nil); got != "" {
		t.Errorf("a missing value should map to empty, got %q", got)
	}
	if got := changeVal("P0"); got != "P0" {
		t.Errorf("a string value should pass through, got %q", got)
	}
	if got := changeVal(5); got != "5" {
		t.Errorf("a numeric value should render, got %q", got)
	}
}
