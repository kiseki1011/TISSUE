package project

import (
	"strings"
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func TestDetailRendersBranches(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Wire it up",
		Branches: []domain.IssueBranch{
			{
				BranchName: "feature/login", BranchURL: "https://github.com/acme/repo/tree/feature/login",
				LatestCommitHash: "abc1234def", LatestCommitMsg: "add login form",
				LatestCommitURL: "https://github.com/acme/repo/commit/abc1234def",
			},
			{BranchName: "bugfix/npe", BranchURL: "https://github.com/acme/repo/tree/bugfix/npe"},
		},
	}})
	body := plain(m.View())
	for _, want := range []string{"Branches (2)", "feature/login", "bugfix/npe", "abc1234", "add login form"} {
		if !strings.Contains(body, want) {
			t.Errorf("branches section missing %q:\n%s", want, body)
		}
	}
	if strings.Contains(body, "abc1234def") {
		t.Errorf("commit hash should be shortened to 7 chars:\n%s", body)
	}
}

// Who pushed and when separates a live branch from one abandoned weeks ago.
func TestDetailRendersBranchPushMeta(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Wire it up",
		Branches: []domain.IssueBranch{{
			BranchName: "feature/login", BranchURL: "https://github.com/acme/repo/tree/feature/login",
			LatestCommitHash: "abc1234def", LatestCommitMsg: "add login form",
			PusherName: "Hong Gildong", PushedAt: time.Now().Add(-2 * time.Hour),
		}},
	}})
	body := plain(m.View())
	if !strings.Contains(body, "Hong Gildong") {
		t.Errorf("the branch should name who pushed it:\n%s", body)
	}
}

// A never-pushed branch has neither half, so it must not render a bare separator.
func TestDetailBranchPushMetaOmittedWhenUnknown(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Wire it up",
		Branches: []domain.IssueBranch{{BranchName: "bugfix/npe", BranchURL: "https://github.com/acme/repo/tree/bugfix/npe"}},
	}})
	for _, ln := range strings.Split(plain(m.View()), "\n") {
		if strings.TrimSpace(ln) == "·" {
			t.Errorf("an unknown pusher and date should leave no separator behind:\n%s", plain(m.View()))
		}
	}
}

// Only one half known still reads cleanly, with no dangling separator.
func TestDetailBranchPushMetaPartial(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Wire it up",
		Branches: []domain.IssueBranch{{
			BranchName: "bugfix/npe", BranchURL: "https://github.com/acme/repo/tree/bugfix/npe",
			PusherName: "Kim Cheolsu",
		}},
	}})
	body := plain(m.View())
	if !strings.Contains(body, "Kim Cheolsu") {
		t.Errorf("a known pusher should still show:\n%s", body)
	}
	if strings.Contains(body, "Kim Cheolsu ·") {
		t.Errorf("a missing date should leave no separator:\n%s", body)
	}
}

// Branches arrive from webhooks, so an issue may have none and there is no "+ add" affordance.
func TestDetailNoBranchesNoSection(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "No branches here",
	}})
	if body := plain(m.View()); strings.Contains(body, "Branches (") {
		t.Errorf("an issue with no branches should show no Branches section:\n%s", body)
	}
}

func TestShortHash(t *testing.T) {
	if got := shortHash("abcdef1234567"); got != "abcdef1" {
		t.Errorf("shortHash long = %q, want abcdef1", got)
	}
	if got := shortHash("abc"); got != "abc" {
		t.Errorf("shortHash short = %q, want abc", got)
	}
}
