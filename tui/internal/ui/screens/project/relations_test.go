package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func detailWithRelations() domain.IssueDetail {
	d := sampleDetail()
	d.Relations = []domain.IssueRelationGroup{
		{Kind: "Blocks", Items: []domain.RelatedIssue{{Key: "ENG-42", Title: "Migration", StateLabel: "In Review", StateCategory: "ACTIVE"}}},
		{Kind: "Blocked by", Items: []domain.RelatedIssue{{Key: "ENG-7", Title: "Staging DB", StateLabel: "Done", StateCategory: "COMPLETED"}}},
	}
	return d
}

// The detail modal renders a Relations section grouped by kind, with each linked issue's key and state.
func TestRelationsRender(t *testing.T) {
	m := editReady(t, detailWithRelations())
	m.detailScroll = m.detailScrollMax()
	body := plain(m.View())
	for _, want := range []string{"Relations", "Blocks", "ENG-42", "Migration", "Blocked by", "ENG-7", "Done"} {
		if !strings.Contains(body, want) {
			t.Errorf("relations section missing %q:\n%s", want, body)
		}
	}
}

// An issue with no relations still shows the Relations section (with a "(0)" count) so its "+ Relation"
// entry point is visible, matching Children/Reviewers.
func TestRelationsAlwaysShown(t *testing.T) {
	m := editReady(t, sampleDetail()) // no relations
	m.detailScroll = m.detailScrollMax()
	body := plain(m.View())
	if !strings.Contains(body, "Relations (0)") {
		t.Errorf("an empty Relations section should still show with a (0) count:\n%s", body)
	}
	if !strings.Contains(body, "+ Relation") {
		t.Errorf("an empty Relations section should show its + Relation button:\n%s", body)
	}
}

// A control character in an untrusted related-issue title is flattened, not emitted into the frame.
func TestRelationTitleSanitized(t *testing.T) {
	d := sampleDetail()
	d.Relations = []domain.IssueRelationGroup{
		{Kind: "Blocks", Items: []domain.RelatedIssue{{Key: "ENG-2", Title: "bad\rtitle", StateCategory: "ACTIVE"}}},
	}
	m := editReady(t, d)
	m.detailScroll = m.detailScrollMax()
	if strings.ContainsRune(m.View(), '\r') {
		t.Error("a carriage return in a related-issue title must be flattened, not emitted into the frame")
	}
}
