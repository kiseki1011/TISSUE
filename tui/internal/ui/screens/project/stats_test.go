package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// sampleStats is a small snapshot used across the Stats tests.
var sampleStats = domain.ProjectStats{
	Total: 10, Open: 6, Completed: 4, Unassigned: 2, Overdue: 1,
	ByState:     []domain.StatBucket{{Label: "ACTIVE", Count: 6}, {Label: "COMPLETED", Count: 4}},
	ByPriority:  []domain.StatBucket{{Label: "P0", Count: 1}, {Label: "P2", Count: 9}},
	ByHierarchy: []domain.StatBucket{{Label: "STANDARD", Count: 7}, {Label: "SUBTASK", Count: 3}},
}

// statsTabModel is a loaded model sitting on the Stats tab with a snapshot already loaded, for
// exercising the view without a network load.
func statsTabModel(t *testing.T, st domain.ProjectStats) Model {
	t.Helper()
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabStats
	m.statsLoaded = true
	m.stats = st
	return m
}

// Opening the Stats tab kicks off the snapshot load, and shows a skeleton until it lands.
func TestStatsLazyLoad(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, cmd := m.Update(press("3"))
	if m.tab != tabStats {
		t.Fatalf("key 3 should select the Stats tab, got %v", m.tab)
	}
	if !m.statsLoading || cmd == nil {
		t.Fatalf("opening Stats should load: loading=%v cmd=%v", m.statsLoading, cmd != nil)
	}
	if body := plain(m.View()); !strings.Contains(body, "Loading") {
		t.Errorf("Stats should show a loading note before the snapshot lands:\n%s", body)
	}
}

// A load already in flight is not duplicated by reopening the tab.
func TestStatsOpenWhileLoadingIsInert(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, _ = m.Update(press("3")) // load now in flight (statsLoading)
	_, cmd := m.Update(press("3"))
	if cmd != nil {
		t.Error("reopening while a load is in flight should not fire a duplicate fetch")
	}
}

// Once loaded, reopening the tab silently refreshes it (SWR): a fetch fires while the numbers already on
// screen stay visible (no skeleton).
func TestStatsReopenRefreshesInPlace(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	m, cmd := m.Update(press("3"))
	if cmd == nil {
		t.Fatal("reopening a loaded Stats tab should refresh it")
	}
	if !m.statsLoaded {
		t.Error("a refresh must keep the loaded snapshot visible, not reset it")
	}
	if body := plain(m.View()); strings.Contains(body, "Loading") || !strings.Contains(body, "By State") {
		t.Errorf("a refresh should keep showing the overview, not a skeleton:\n%s", body)
	}
}

// The overview renders KPIs and three labelled sections with humanized enum labels.
func TestStatsRendersOverview(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	out := plain(m.View())
	for _, want := range []string{
		"Statistics", "Total", "Open", "Done", "Unassigned", "Overdue",
		"By State", "Active", "Completed",
		"By Priority", "P0", "P2",
		"By Hierarchy", "Standard", "Subtask",
	} {
		if !strings.Contains(out, want) {
			t.Errorf("Stats overview missing %q:\n%s", want, out)
		}
	}
}

// A brand-new project (no issues) still shows the KPIs, with each grouping reading None.
func TestStatsEmptySnapshot(t *testing.T) {
	m := statsTabModel(t, domain.ProjectStats{})
	out := plain(m.View())
	if !strings.Contains(out, "Total") || !strings.Contains(out, "By State") {
		t.Errorf("an empty snapshot should still show the KPIs and sections:\n%s", out)
	}
	if strings.Count(out, "None") < 3 {
		t.Errorf("each empty grouping should read None:\n%s", out)
	}
}

// A failed first load shows an error, and clears the in-flight guards so reopening the tab retries.
func TestStatsFirstLoadError(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, _ = m.Update(press("3"))
	m, _ = m.Update(statsLoadedMsg{key: m.projectKey, gen: m.statsReqGen, err: true})
	if m.statsLoaded {
		t.Error("a failed first load must not mark the snapshot loaded")
	}
	if body := plain(m.View()); !strings.Contains(body, "Failed") {
		t.Errorf("a failed load should render an error note:\n%s", body)
	}
	_, cmd := m.Update(press("3")) // reopening retries
	if cmd == nil {
		t.Error("reopening after a failed load should retry the fetch")
	}
}

// A failed silent refresh keeps the numbers already on screen and surfaces a toast instead of blanking.
func TestStatsRefreshErrorKeepsData(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	m, _ = m.Update(press("3")) // refresh in flight
	m, cmd := m.Update(statsLoadedMsg{key: m.projectKey, gen: m.statsReqGen, err: true})
	if !m.statsLoaded || m.stats.Total != 10 {
		t.Error("a failed refresh should keep the cached snapshot")
	}
	if cmd == nil {
		t.Error("a failed refresh should surface a toast")
	}
	if body := plain(m.View()); strings.Contains(body, "Failed") || !strings.Contains(body, "By State") {
		t.Errorf("a failed refresh should keep the overview, not show the error state:\n%s", body)
	}
}

// A stale (superseded gen) or cross-project result is ignored.
func TestStatsStaleResultDropped(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	m, _ = m.Update(press("3")) // bumps statsReqGen; a fresh fetch is in flight
	newer := m.stats
	// a result from an earlier generation must not overwrite the snapshot
	m, _ = m.Update(statsLoadedMsg{key: m.projectKey, gen: m.statsReqGen - 1, stats: domain.ProjectStats{Total: 999}})
	if m.stats.Total != newer.Total {
		t.Error("a superseded-generation result must be dropped")
	}
	// a result for a different project must not overwrite it either
	m, _ = m.Update(statsLoadedMsg{key: "OTHER", gen: m.statsReqGen, stats: domain.ProjectStats{Total: 999}})
	if m.stats.Total != newer.Total {
		t.Error("a cross-project result must be dropped")
	}
}

// Scroll keys clamp: with nothing to scroll the offset stays at zero (so it is not stuck negative).
func TestStatsScrollClamps(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	m, _ = m.Update(press("k")) // up with nothing above
	if m.statsScroll != 0 {
		t.Errorf("scrolling up at the top should stay at 0, got %d", m.statsScroll)
	}
}

// The advanced sections (aging, cycle/lead time, flow) render below the snapshot when their OK flags are
// set, with human-formatted durations and both flow series.
func TestStatsRendersAdvancedSections(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	m.agingOK = true
	m.aging = domain.AgingStats{OpenTotal: 6, Under3d: 3, Days3to7: 1, Weeks1to2: 1, Over2w: 1, Blocked: 2}
	m.cycleOK = true
	m.cycle = domain.CycleTimeStats{
		Window: "MONTH",
		Cycle:  domain.DurationStat{Count: 4, AvgSeconds: 90000, P50Seconds: 3600, P90Seconds: 9000},
		Lead:   domain.DurationStat{Count: 4, AvgSeconds: 180000, P50Seconds: 7200, P90Seconds: 18000},
	}
	m.flowOK = true
	m.flow = domain.FlowStats{Window: "MONTH", Days: []domain.FlowDay{
		{Created: 2, Resolved: 0}, {Created: 0, Resolved: 3}, {Created: 1, Resolved: 1},
	}}
	out := plain(m.View())
	for _, want := range []string{
		"Aging (open)", "< 3d", "3-7d", "1-2w", "> 2w", "Blocked", "2",
		"Cycle & Lead (30d)", "Cycle", "Lead", "avg", "p50", "p90", "1d 1h", "n=4",
		"Flow (30d)", "Created", "Resolved",
	} {
		if !strings.Contains(out, want) {
			t.Errorf("advanced stats missing %q:\n%s", want, out)
		}
	}
}

// A section whose OK flag is false is simply omitted, so one failing endpoint never blanks the panel.
func TestStatsHidesUnavailableSections(t *testing.T) {
	m := statsTabModel(t, sampleStats) // agingOK/cycleOK/flowOK all false
	out := plain(m.View())
	for _, absent := range []string{"Aging (open)", "Cycle & Lead", "Flow (30d)"} {
		if strings.Contains(out, absent) {
			t.Errorf("unavailable section %q should be hidden:\n%s", absent, out)
		}
	}
}

func TestHumanizeDur(t *testing.T) {
	cases := []struct {
		in   int
		want string
	}{
		{0, "0m"},
		{-5, "0m"},
		{30, "1m"},
		{90, "1m"},
		{3600, "1h"},
		{3660, "1h 1m"},
		{9000, "2h 30m"},
		{86400, "1d"},
		{90000, "1d 1h"},
	}
	for _, c := range cases {
		if got := humanizeDur(c.in); got != c.want {
			t.Errorf("humanizeDur(%d) = %q, want %q", c.in, got, c.want)
		}
	}
}

func TestSparkline(t *testing.T) {
	got := []rune(sparkline([]int{0, 5, 10}, 10))
	if len(got) != 3 {
		t.Fatalf("sparkline length = %d, want 3", len(got))
	}
	if got[0] != '▁' {
		t.Errorf("zero should map to the lowest block, got %q", string(got[0]))
	}
	if got[2] != '█' {
		t.Errorf("peak should map to the full block, got %q", string(got[2]))
	}
	// a zero peak (all-empty window) stays flat rather than dividing by zero
	if flat := sparkline([]int{0, 0}, 0); flat != "▁▁" {
		t.Errorf("flat series = %q, want ▁▁", flat)
	}
}

// Velocity renders in the trend column: the mean plus one proportional bar per recent completed sprint.
func TestStatsRendersVelocity(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	m.velocOK = true
	m.velocity = domain.Velocity{
		AverageStoryPoints: 7.5,
		Sprints: []domain.VelocityPoint{
			{SprintKey: "S-1", CompletedStoryPoints: 5, CompletedIssues: 2},
			{SprintKey: "S-2", CompletedStoryPoints: 10, CompletedIssues: 4},
		},
	}
	out := plain(m.View())
	for _, want := range []string{"Velocity", "Avg", "7.5 pts", "2 sprints", "S-1", "S-2"} {
		if !strings.Contains(out, want) {
			t.Errorf("velocity section missing %q:\n%s", want, out)
		}
	}
}

// With no completed sprints the velocity section notes that rather than rendering an empty chart.
func TestStatsVelocityEmpty(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	m.velocOK = true
	m.velocity = domain.Velocity{}
	if out := plain(m.View()); !strings.Contains(out, "No completed sprints") {
		t.Errorf("empty velocity should note no completed sprints:\n%s", out)
	}
}

// The attention KPIs show their share as a percentage: overdue of open, unassigned of total.
func TestStatsKPIShowsPercent(t *testing.T) {
	m := statsTabModel(t, sampleStats) // overdue 1 of open 6 => 17%, unassigned 2 of total 10 => 20%
	out := plain(m.View())
	for _, want := range []string{"17%", "20%"} {
		if !strings.Contains(out, want) {
			t.Errorf("KPI should show share %q:\n%s", want, out)
		}
	}
}

// The Blocked line shows its share of open work when something is blocked.
func TestStatsBlockedShowsPercent(t *testing.T) {
	m := statsTabModel(t, sampleStats)
	m.agingOK = true
	m.aging = domain.AgingStats{OpenTotal: 4, Under3d: 2, Days3to7: 2, Blocked: 1} // 1 of 4 => 25%
	if out := plain(m.View()); !strings.Contains(out, "Blocked") || !strings.Contains(out, "25%") {
		t.Errorf("Blocked should show its share of open work (25%%):\n%s", out)
	}
}
