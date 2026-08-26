package project

import (
	"strings"
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func prDetail(t *testing.T, prs []domain.IssuePullRequest) Model {
	t.Helper()
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Wire it up", PullRequests: prs,
	}})
	return m
}

func TestDetailRendersPullRequests(t *testing.T) {
	m := prDetail(t, []domain.IssuePullRequest{{
		Number: 42, Title: "PROJ-12: add login", URL: "https://github.com/acme/repo/pull/42",
		AuthorName: "octocat", State: "OPEN", LastEventAt: time.Now(),
	}})
	body := plain(m.View())
	for _, want := range []string{"Pull requests (1)", "#42", "PROJ-12: add login", "octocat"} {
		if !strings.Contains(body, want) {
			t.Errorf("pull request section missing %q:\n%s", want, body)
		}
	}
}

// The three states read differently, so an open PR is picked out of a list of finished ones.
func TestDetailPullRequestStates(t *testing.T) {
	m := prDetail(t, []domain.IssuePullRequest{
		{Number: 47, Title: "still going", URL: "https://ex.co/47", State: "OPEN"},
		{Number: 42, Title: "shipped", URL: "https://ex.co/42", State: "MERGED"},
		{Number: 39, Title: "abandoned", URL: "https://ex.co/39", State: "CLOSED"},
	})
	body := plain(m.View())
	for _, want := range []string{"open", "merged", "closed"} {
		if !strings.Contains(body, want) {
			t.Errorf("pull request states missing %q:\n%s", want, body)
		}
	}
}

// No author and no timestamp must leave no dangling separator behind.
func TestDetailPullRequestMetaOmittedWhenUnknown(t *testing.T) {
	m := prDetail(t, []domain.IssuePullRequest{{
		Number: 42, Title: "PROJ-12: add login", URL: "https://ex.co/42", State: "OPEN",
	}})
	for _, ln := range strings.Split(plain(m.View()), "\n") {
		if strings.TrimSpace(ln) == "·" {
			t.Errorf("an unknown author and date should leave no separator behind:\n%s", plain(m.View()))
		}
	}
}

// No section when empty: PRs arrive from webhooks, so there is no "+ add" entry point to keep visible.
func TestDetailNoPullRequestsNoSection(t *testing.T) {
	m := prDetail(t, nil)
	if body := plain(m.View()); strings.Contains(body, "Pull requests (") {
		t.Errorf("an issue with no pull requests should show no section:\n%s", body)
	}
}

// merged and closed must not collapse into one reading.
func TestPullRequestStateLabels(t *testing.T) {
	th := theme.New(theme.TokyoNight()).Theme
	cases := map[string]string{"OPEN": "open", "MERGED": "merged", "CLOSED": "closed", "": "open"}
	for state, want := range cases {
		if got, _ := pullRequestState(th, state); got != want {
			t.Errorf("pullRequestState(%q) = %q, want %q", state, got, want)
		}
	}
	merged, mergedColor := pullRequestState(th, "MERGED")
	closed, closedColor := pullRequestState(th, "CLOSED")
	if merged == closed || mergedColor == closedColor {
		t.Error("merged and closed should not read the same")
	}
}
