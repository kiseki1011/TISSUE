package project

import (
	"context"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// openStatsTab is called when the Stats tab is selected. The first open shows a skeleton while the
// snapshot loads; a re-open silently refreshes it (the numbers stay on screen while the fetch runs) so
// the counts reflect issues added or resolved since the tab was last viewed. A load already in flight is
// left alone so rapid tab-toggling cannot pile up duplicate fetches.
func (m Model) openStatsTab() (Model, tea.Cmd) {
	if m.statsLoading {
		return m, nil
	}
	return m.loadProjectStats()
}

// loadProjectStats kicks off the statistics fetch (first load or a refresh). It always sets statsLoading;
// the body only shows the skeleton until the first snapshot lands (statsLoaded), so a refresh does not
// blank the panel. Mirrors the Config tab's lazy load.
func (m Model) loadProjectStats() (Model, tea.Cmd) {
	m.statsLoading = true
	m.statsErr = false
	m.statsReqGen++
	return m, loadStats(m.deps, m.projectKey, m.statsReqGen)
}

func (m Model) onStatsLoaded(msg statsLoadedMsg) (Model, tea.Cmd) {
	if msg.key != m.projectKey || msg.gen != m.statsReqGen {
		return m, nil // a stale cross-project result or a superseded reload landed late
	}
	m.statsLoading = false
	if msg.err {
		if m.statsLoaded {
			// a silent refresh failed but the numbers we were showing are still good: keep them
			return m, toast.Show(toast.Error, "Couldn't refresh the statistics.")
		}
		// the error shows in the body; reopening the tab retries via openStatsTab (statsLoading is now false)
		m.statsErr = true
		return m, nil
	}
	m.statsErr = false
	m.statsLoaded = true
	m.stats = msg.stats
	m.aging, m.agingOK = msg.aging, msg.agingOK
	m.cycle, m.cycleOK = msg.cycle, msg.cycleOK
	m.flow, m.flowOK = msg.flow, msg.flowOK
	m.velocity, m.velocOK = msg.velocity, msg.velocityOK
	// a refresh can shrink the body (fewer distinct states/priorities), so re-clamp the scroll offset -
	// else a stale offset eats the first scroll key as a dead press (mirrors the config/sprint handlers)
	m.statsScroll = clampScroll(m.statsScroll, m.statsScrollMax())
	return m, nil
}

func (m Model) onStatsKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "up", "k":
		m.statsScroll = clampScroll(m.statsScroll-1, m.statsScrollMax())
		return m, nil
	case "down", "j":
		m.statsScroll = clampScroll(m.statsScroll+1, m.statsScrollMax())
		return m, nil
	}
	return m, nil
}

func (m Model) onStatsWheel(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	m.statsScroll = wheelClamp(m.statsScroll, msg.Button, m.statsScrollMax())
	return m, nil
}

// statsWindow is the time window for the flow and cycle-time sections: a rolling 30 days.
const statsWindow = "month"

type statsLoadedMsg struct {
	key   string
	gen   int
	stats domain.ProjectStats
	err   bool // a hard failure of the core snapshot (simple-stats); the panel shows an error

	aging      domain.AgingStats
	agingOK    bool
	cycle      domain.CycleTimeStats
	cycleOK    bool
	flow       domain.FlowStats
	flowOK     bool
	velocity   domain.Velocity
	velocityOK bool
}

// loadStats fetches the core snapshot plus the advanced sections (aging, cycle time, flow). Only the core
// snapshot is load-bearing: if an advanced section fails it is simply hidden (its OK flag stays false) so
// one flaky endpoint never blanks the whole overview.
func loadStats(d deps.Deps, projectKey string, gen int) tea.Cmd {
	return func() tea.Msg {
		ctx := context.Background()
		st, err := d.Projects.GetProjectStats(ctx, projectKey)
		msg := statsLoadedMsg{key: projectKey, gen: gen, stats: st, err: err != nil}
		if aging, e := d.Projects.GetProjectAging(ctx, projectKey); e == nil {
			msg.aging, msg.agingOK = aging, true
		}
		if cycle, e := d.Projects.GetProjectCycleTime(ctx, projectKey, statsWindow); e == nil {
			msg.cycle, msg.cycleOK = cycle, true
		}
		if flow, e := d.Projects.GetProjectFlow(ctx, projectKey, statsWindow); e == nil {
			msg.flow, msg.flowOK = flow, true
		}
		if velocity, e := d.Projects.GetProjectVelocity(ctx, projectKey); e == nil {
			msg.velocity, msg.velocityOK = velocity, true
		}
		return msg
	}
}
