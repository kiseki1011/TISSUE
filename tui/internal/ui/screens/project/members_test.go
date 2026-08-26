package project

import (
	"strings"
	"testing"
	"time"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func memberList(n int) []domain.ProjectMember {
	out := make([]domain.ProjectMember, n)
	for i := range out {
		role := "MEMBER"
		if i == 0 {
			role = "MANAGER"
		}
		out[i] = domain.ProjectMember{
			MemberID:    int64(i + 1),
			DisplayName: "User " + string(rune('1'+i%9)),
			Username:    "u" + string(rune('1'+i%9)),
			Role:        role,
		}
	}
	return out
}

// membersTabModel is a loaded model on the Members tab with the given roster, cursor on the first.
func membersTabModel(t *testing.T, members []domain.ProjectMember) Model {
	t.Helper()
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabMembers
	m.membersLoaded = true
	m.members = members
	if len(members) > 0 {
		m.selMemberID = members[0].MemberID
	}
	return m
}

// Opening the tab while the Init prefetch is in flight must not re-dispatch the roster load: a late-failing
// duplicate would clobber the good roster. It does kick off the per-member stats batch.
func TestMembersTabInflightNoDoubleLoad(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(2), TotalElements: 2}) // membersRequested=true
	m, _ = m.Update(press("4"))
	if m.tab != tabMembers {
		t.Fatalf("key 4 should select the Members tab, got %v", m.tab)
	}
	if m.membersLoaded {
		t.Error("roster should not be marked loaded before the prefetch lands")
	}
	if !m.membersRequested {
		t.Error("the in-flight roster prefetch guard should still hold (no re-dispatch)")
	}
	if !m.memberStatsRequested {
		t.Error("opening the Members tab should kick off the per-member stats batch")
	}
}

// After the roster load fails, reopening the Members tab retries it.
func TestMembersTabRetriesAfterError(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, _ = m.Update(membersLoadedMsg{err: true}) // prefetch failed: membersRequested cleared, membersErr set
	m, cmd := m.Update(press("4"))
	if !m.membersErr {
		t.Fatal("expected membersErr after a failed roster load")
	}
	if cmd == nil {
		t.Error("reopening the Members tab after an error should retry the roster load")
	}
}

// Reopening retries a failed work load - a one-member project can never recover it by moving the cursor.
func TestMembersTabRetriesFailedWork(t *testing.T) {
	m := membersTabModel(t, memberList(1))
	m.memberWorkFailed[1] = true // a prior work fetch failed, nothing cached
	m, cmd := m.selectTab(tabMembers)
	if cmd == nil {
		t.Error("reopening the tab should retry the failed work load for the selected member")
	}
}

// A landed roster on the Members tab selects the first member and loads their work.
func TestMembersLoadedSelectsFirst(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, _ = m.Update(press("4"))
	m, cmd := m.Update(membersLoadedMsg{members: memberList(3)})
	if !m.membersLoaded || m.membersErr {
		t.Fatalf("roster not marked loaded: loaded=%v err=%v", m.membersLoaded, m.membersErr)
	}
	if m.selMemberID != 1 {
		t.Errorf("selMemberID = %d, want 1 (first member)", m.selMemberID)
	}
	if cmd == nil {
		t.Error("selecting the first member should load their work")
	}
}

// Moving the roster cursor selects the next member and loads that member's work.
func TestMemberCursorMovesAndLoads(t *testing.T) {
	m := membersTabModel(t, memberList(3))
	m, cmd := m.Update(press("j"))
	if m.memberCursor != 1 {
		t.Errorf("memberCursor = %d, want 1", m.memberCursor)
	}
	if m.selMemberID != 2 {
		t.Errorf("selMemberID = %d, want 2", m.selMemberID)
	}
	if cmd == nil {
		t.Error("moving to a new member should load their work")
	}
}

// A member-work load caches the result. A superseded (stale-gen) load is dropped.
func TestMemberWorkLoadedGenGuard(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m.memberWorkGen[1] = 1
	m, _ = m.Update(memberWorkLoadedMsg{memberID: 1, gen: 1, work: memberWorkload{
		assigned: domain.IssuePage{Issues: issues(2), TotalElements: 2},
	}})
	if got := m.memberWork[1].assigned.TotalElements; got != 2 {
		t.Fatalf("work not cached: assigned total = %d, want 2", got)
	}
	m, _ = m.Update(memberWorkLoadedMsg{memberID: 1, gen: 0, work: memberWorkload{
		assigned: domain.IssuePage{Issues: issues(5), TotalElements: 5},
	}})
	if got := m.memberWork[1].assigned.TotalElements; got != 2 {
		t.Errorf("stale-gen load should be dropped: assigned total = %d, want 2", got)
	}
}

func i64ptr(v int64) *int64 { return &v }

// A landed stats batch renders the Stats section (counts, points, completion %, humanized avg resolve).
func TestMemberStatsRendered(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m.memberStatsGen = 1
	m, _ = m.Update(memberStatsLoadedMsg{gen: 1, stats: []domain.MemberStats{
		{MemberID: 1, ResolvedCount: 5, OpenAssignedCount: 5, TotalStoryPoints: 21, CompletionRate: 0.5, AvgResolveSeconds: i64ptr(7200)},
	}})
	if !m.memberStatsLoaded {
		t.Fatal("stats should be marked loaded")
	}
	out := plain(m.View())
	for _, want := range []string{"Stats", "Resolved", "Story points", "21", "50%", "2h"} {
		if !strings.Contains(out, want) {
			t.Errorf("stats section missing %q:\n%s", want, out)
		}
	}
}

func TestMemberStatsGenGuard(t *testing.T) {
	m := membersTabModel(t, memberList(1))
	m.memberStatsGen = 2
	m, _ = m.Update(memberStatsLoadedMsg{gen: 1, stats: []domain.MemberStats{{MemberID: 1, ResolvedCount: 9}}})
	if m.memberStatsLoaded {
		t.Error("a stale-gen stats load should be dropped")
	}
	if len(m.memberStats) != 0 {
		t.Error("stale stats should not populate the map")
	}
}

// A member absent from the batch reads as zeros, with "—" for the average resolve time.
func TestMemberStatsZeroFill(t *testing.T) {
	m := membersTabModel(t, memberList(1))
	m.memberStatsGen = 1
	m, _ = m.Update(memberStatsLoadedMsg{gen: 1, stats: []domain.MemberStats{}}) // no row for member 1
	out := plain(m.View())
	if !strings.Contains(out, "Avg resolve") {
		t.Fatalf("stats section not rendered:\n%s", out)
	}
	if !strings.Contains(out, "—") {
		t.Error("avg resolve should render — when the member has no resolved issues")
	}
}

// Details spells out human/agent and names an agent's owner (humans show no Owner row).
func TestMemberDetailShowsTypeAndOwner(t *testing.T) {
	agent := domain.ProjectMember{MemberID: 1, DisplayName: "Bot", Username: "agent-bot", Role: "MEMBER", IsAgent: true, OwnerName: "Gildong", OwnerUser: "gildong"}
	human := domain.ProjectMember{MemberID: 2, DisplayName: "Ana", Username: "ana", Role: "MANAGER"}
	m := membersTabModel(t, []domain.ProjectMember{agent, human})

	out := plain(m.View()) // cursor starts on the agent
	for _, want := range []string{"Type", "Agent", "Owner", "Gildong"} {
		if !strings.Contains(out, want) {
			t.Errorf("agent detail missing %q:\n%s", want, out)
		}
	}

	m, _ = m.Update(press("j")) // move to the human
	out = plain(m.View())
	if !strings.Contains(out, "Human") {
		t.Errorf("a human should read Type Human:\n%s", out)
	}
	if strings.Contains(out, "Owner") {
		t.Errorf("a human should not show an Owner row:\n%s", out)
	}
}

// The view renders the roster (name + role) and the Assigned/Reviewing sections with counts.
func TestMembersViewRendersRosterAndSections(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m.memberWorkGen[1] = 1
	m, _ = m.Update(memberWorkLoadedMsg{memberID: 1, gen: 1, work: memberWorkload{
		assigned: domain.IssuePage{Issues: issues(1), TotalElements: 1},
		reviewer: domain.IssuePage{TotalElements: 0},
	}})
	out := plain(m.View())
	for _, want := range []string{"Members (2)", "Manager", "Assigned (1)", "Reviewing (0)", "None open."} {
		if !strings.Contains(out, want) {
			t.Errorf("members view missing %q:\n%s", want, out)
		}
	}
}

// Loaded work renders the heatmap: summary line, shaded grid (peak day at full block), legend.
func TestMemberContribHeatmapRenders(t *testing.T) {
	m := membersTabModel(t, memberList(2)) // selected member id = 1
	base := time.Date(2026, 8, 1, 0, 0, 0, 0, time.UTC)
	days := make([]domain.ContributionDay, 14)
	for i := range days {
		days[i] = domain.ContributionDay{Date: base.AddDate(0, 0, i)}
	}
	days[3].Count = 5 // the peak
	days[10].Count = 2
	m.memberWork[1] = memberWorkload{contrib: domain.Contributions{Days: days, TotalResolved: 7, MaxDaily: 5}}

	out := plain(m.View())
	for _, want := range []string{"Contributions", "7 resolved", "Less", "More", "█"} {
		if !strings.Contains(out, want) {
			t.Errorf("contribution heatmap missing %q:\n%s", want, out)
		}
	}
}

// An empty window notes there were no resolutions rather than rendering a blank grid.
func TestMemberContribEmpty(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m.memberWork[1] = memberWorkload{contrib: domain.Contributions{}}
	if out := plain(m.View()); !strings.Contains(out, "No resolutions") {
		t.Errorf("empty contributions should note no resolutions:\n%s", out)
	}
}

func TestContribLevel(t *testing.T) {
	cases := []struct{ count, peak, want int }{
		{0, 10, 0}, {1, 0, 1}, {1, 10, 1}, {5, 10, 2}, {7, 10, 3}, {10, 10, 4},
	}
	for _, c := range cases {
		if got := contribLevel(c.count, c.peak); got != c.want {
			t.Errorf("contribLevel(%d,%d) = %d, want %d", c.count, c.peak, got, c.want)
		}
	}
}

// contribSeries is n consecutive days from a Sunday, each with one resolution, so every cell is drawn.
func contribSeries(n int) domain.Contributions {
	base := time.Date(2026, 8, 2, 0, 0, 0, 0, time.UTC)
	base = base.AddDate(0, 0, -int(base.Weekday())) // snap to Sunday so the weeks line up as whole columns
	days := make([]domain.ContributionDay, n)
	for i := range days {
		days[i] = domain.ContributionDay{Date: base.AddDate(0, 0, i), Count: 1}
	}
	return domain.Contributions{Days: days, TotalResolved: n, MaxDaily: 1}
}

// Week columns are separated by a blank column, so a busy run does not read as one solid bar.
func TestContribHeatmapSpacesColumns(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	lines := m.contribHeatmap(contribSeries(21), 40) // 3 whole weeks
	if len(lines) < 8 {
		t.Fatalf("expected a summary plus 7 grid rows, got %d lines", len(lines))
	}
	if row := plain(lines[1]); !strings.Contains(row, "█ █") {
		t.Errorf("grid columns should be separated by a blank column, got %q", row)
	}
}

// The grid must fit the panel: the gap doubles a column's cost, so the week-fitting maths must account for
// it or the rows wrap. Only grid rows are checked - the summary and legend are fixed prose.
func TestContribHeatmapFitsPanelWidth(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	c := contribSeries(126) // the 18 weeks the Members tab actually requests
	for _, w := range []int{21, 40, 44, 80} {
		lines := m.contribHeatmap(c, w)
		if len(lines) < 8 {
			t.Fatalf("w=%d: expected 7 grid rows, got %d lines", w, len(lines))
		}
		for r, ln := range lines[1:8] {
			if got := lipgloss.Width(ln); got > w {
				t.Errorf("w=%d: grid row %d is %d cells wide: %q", w, r, got, plain(ln))
			}
		}
	}
}
