package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func TestDetailReviewers(t *testing.T) {
	m := openDetailOn(t, 160, 60, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T",
		Reviewers: []domain.Reviewer{
			{Name: "Alice", Status: "APPROVED"},
			{Name: "Bob", Status: "CHANGES_REQUESTED"},
			{Name: "Carol", Status: "PENDING"},
		},
	}})
	body := plain(m.View())
	for _, want := range []string{"Reviewers (3)", "Alice", "Approved", "Bob", "Changes requested", "Carol", "Pending"} {
		if !strings.Contains(body, want) {
			t.Errorf("detail body missing %q:\n%s", want, body)
		}
	}
}

func TestParentIsSectionAboveChildren(t *testing.T) {
	m := openDetailOn(t, 160, 60, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T",
		Parent:   &domain.IssueRef{Key: "ENG-1", TypeName: "Epic", TypeColor: "INDIGO", StateLabel: "Active", StateCategory: "ACTIVE"},
		Children: []domain.IssueRef{{Key: "ENG-8", TypeName: "Task", TypeColor: "ANSI_RED", StateLabel: "Todo", StateCategory: "INITIAL"}},
	}})
	body := plain(m.View())
	pIdx := strings.Index(body, "Parent")
	cIdx := strings.Index(body, "Children (")
	if pIdx < 0 || cIdx < 0 {
		t.Fatalf("expected both a Parent and a Children section:\n%s", body)
	}
	if pIdx > cIdx {
		t.Errorf("the Parent section should appear above Children (parent@%d children@%d)", pIdx, cIdx)
	}
}

// A long name plus the longest status must not overflow the narrow modal.
func TestDetailReviewersFitNarrow(t *testing.T) {
	m := openDetailOn(t, 100, 30, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T",
		Reviewers: []domain.Reviewer{{Name: strings.Repeat("VeryLongReviewerName", 6), Status: "CHANGES_REQUESTED"}},
	}})
	for _, line := range strings.Split(plain(m.View()), "\n") {
		if lipgloss.Width(line) > m.width {
			t.Errorf("a reviewer row overflowed the modal: %d > %d:\n%q", lipgloss.Width(line), m.width, line)
		}
	}
}
