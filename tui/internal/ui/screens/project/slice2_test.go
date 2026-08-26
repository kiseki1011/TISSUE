package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// Parent is a fixed read-only meta row. It is edited from the "Edit issue" form, not here.
func TestDetailChildrenAndParent(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T",
		Parent: &domain.IssueRef{Key: "ENG-1", TypeName: "Epic", TypeColor: "INDIGO", StateLabel: "In Progress", StateCategory: "ACTIVE"},
		Children: []domain.IssueRef{
			{Key: "ENG-8", TypeName: "Task", TypeColor: "ANSI_RED", StateLabel: "Todo", StateCategory: "INITIAL"},
			{Key: "ENG-9", TypeName: "Bug", TypeColor: "LIMEGREEN", StateLabel: "Done", StateCategory: "COMPLETED"},
		},
	}})
	body := plain(m.View())
	for _, want := range []string{"Parent", "ENG-1", "Children (2)", "ENG-8", "ENG-9", "Task", "Todo", "Done"} {
		if !strings.Contains(body, want) {
			t.Errorf("detail body missing %q:\n%s", want, body)
		}
	}
}

// The Parent row still shows as "-" with no parent, but Children stays hidden when empty.
func TestDetailHierarchyOmittedWhenEmpty(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "T"}})
	body := plain(m.View())
	if strings.Contains(body, "Children (") {
		t.Errorf("no children should mean no Children section:\n%s", body)
	}
	if !strings.Contains(body, "Parent") {
		t.Errorf("the Parent meta row should always show (as a fixed row):\n%s", body)
	}
}

// Long keys/type names/states in the hierarchy must not overflow the narrow read-only modal.
func TestDetailHierarchyFitsNarrow(t *testing.T) {
	m := openDetailOn(t, 100, 30, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T",
		Parent: &domain.IssueRef{Key: "VERYLONGPARENTKEY-12345", TypeName: strings.Repeat("Epic", 10), TypeColor: "INDIGO", StateLabel: "In Progress", StateCategory: "ACTIVE"},
		Children: []domain.IssueRef{
			{Key: "ENG-8", TypeName: strings.Repeat("Task", 12), TypeColor: "ANSI_RED", StateLabel: strings.Repeat("Todo", 8), StateCategory: "INITIAL"},
		},
	}})
	for _, line := range strings.Split(plain(m.View()), "\n") {
		if lipgloss.Width(line) > m.width {
			t.Errorf("a hierarchy row overflowed the modal: width %d > %d:\n%q", lipgloss.Width(line), m.width, line)
		}
	}
}
