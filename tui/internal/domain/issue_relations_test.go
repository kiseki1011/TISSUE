package domain

import (
	"context"
	"encoding/json"
	"net/http"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

func TestToRelationGroups(t *testing.T) {
	rel := &client.IssueRelationsDetail{
		BlockedBy: &[]client.RelatedIssueInfo{
			{
				IssueKey: ptr("ENG-7"), Title: ptr("Staging DB"),
				IssueType:    &client.IssueTypeInfo{DisplayName: ptr("Task")},
				CurrentState: &client.StateInfo{DisplayName: ptr("Done"), Category: ptr(client.StateInfoCategoryCOMPLETED)},
			},
		},
		Blocks:   &[]client.RelatedIssueInfo{{IssueKey: ptr("ENG-42"), Title: ptr("Migration")}},
		Relevant: &[]client.RelatedIssueInfo{}, // empty -> dropped
	}

	groups := toRelationGroups(rel)

	if len(groups) != 2 {
		t.Fatalf("expected 2 non-empty groups, got %d: %+v", len(groups), groups)
	}
	if groups[0].Kind != "Blocks" || groups[1].Kind != "Blocked by" {
		t.Errorf("groups out of order: %q, %q", groups[0].Kind, groups[1].Kind)
	}
	if got := groups[0].Items[0]; got.Key != "ENG-42" || got.Title != "Migration" {
		t.Errorf("Blocks item wrong: %+v", got)
	}
	if got := groups[1].Items[0]; got.Key != "ENG-7" || got.TypeName != "Task" || got.StateLabel != "Done" || got.StateCategory != "COMPLETED" {
		t.Errorf("Blocked-by item wrong: %+v", got)
	}
}

func TestToRelationGroupsEmpty(t *testing.T) {
	if g := toRelationGroups(&client.IssueRelationsDetail{}); len(g) != 0 {
		t.Errorf("an empty relations detail should map to no groups, got %+v", g)
	}
}

// The inverse of a directional relation lives on its source, so it is unremovable here.
func TestRelationGroupsFlagRemovability(t *testing.T) {
	one := []client.RelatedIssueInfo{{IssueKey: ptr("TIS-2")}}
	groups := toRelationGroups(&client.IssueRelationsDetail{
		Blocks:       &one,
		BlockedBy:    &one,
		Causes:       &one,
		CausedBy:     &one,
		Duplicates:   &one,
		DuplicatedBy: &one,
		Relevant:     &one,
	})

	want := map[string]bool{
		"Blocks": true, "Blocked by": false,
		"Causes": true, "Caused by": false,
		"Duplicates": true, "Duplicated by": false,
		"Related to": true,
	}
	if len(groups) != len(want) {
		t.Fatalf("got %d groups, want %d", len(groups), len(want))
	}
	for _, g := range groups {
		if g.Removable != want[g.Kind] {
			t.Errorf("%q removable = %v, want %v", g.Kind, g.Removable, want[g.Kind])
		}
	}
}

func TestRemoveRelationSendsTheTarget(t *testing.T) {
	svc, req, body := issueServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	})

	if err := svc.RemoveRelation(context.Background(), "TIS-1", "OTHER", "OTHER-4"); err != nil {
		t.Fatalf("RemoveRelation: %v", err)
	}
	if req.Method != http.MethodDelete {
		t.Errorf("method = %s, want DELETE", req.Method)
	}
	var sent map[string]any
	if err := json.Unmarshal(*body, &sent); err != nil {
		t.Fatalf("decoding %q: %v", *body, err)
	}
	if sent["targetIssueKey"] != "OTHER-4" || sent["targetProjectKey"] != "OTHER" {
		t.Errorf("body = %v, want the target key and its project", sent)
	}
}

// A relation may point at another project, so the project comes off the target's own key.
func TestProjectKeyOf(t *testing.T) {
	cases := map[string]string{
		"TIS-12":    "TIS",
		"MY-PROJ-7": "MY-PROJ",
		"NOSUFFIX":  "NOSUFFIX",
		"-5":        "-5",
	}
	for in, want := range cases {
		if got := ProjectKeyOf(in); got != want {
			t.Errorf("ProjectKeyOf(%q) = %q, want %q", in, got, want)
		}
	}
}
