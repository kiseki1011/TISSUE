package project

import (
	"context"
	"strconv"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// memberWorkload is a member's open (INITIAL/ACTIVE) assigned and reviewing issues plus their
// contribution heatmap, fetched and cached as one unit.
type memberWorkload struct {
	assigned domain.IssuePage
	reviewer domain.IssuePage
	contrib  domain.Contributions
}

// contribDays is the heatmap window (~18 weeks).
const contribDays = 126

func (m Model) selectedMember() (domain.ProjectMember, bool) {
	if m.memberCursor < 0 || m.memberCursor >= len(m.members) {
		return domain.ProjectMember{}, false
	}
	return m.members[m.memberCursor], true
}

// syncMemberSelection retargets the Details panel at the cursor's member. SWR: a cached page shows at
// once, else a deduped fetch.
func (m Model) syncMemberSelection() (Model, tea.Cmd) {
	mem, ok := m.selectedMember()
	if !ok {
		m.selMemberID = 0
		return m, nil
	}
	if mem.MemberID == m.selMemberID {
		return m, nil
	}
	m.selMemberID = mem.MemberID
	m.memberDetailScroll = 0
	if _, cached := m.memberWork[mem.MemberID]; !cached && !m.memberWorkPending[mem.MemberID] {
		return m, m.startMemberWorkLoad(mem.MemberID)
	}
	return m, nil
}

func (m Model) moveMemberCursor(delta int) (Model, tea.Cmd) {
	if len(m.members) == 0 {
		return m, nil
	}
	next := m.memberCursor + delta
	if next < 0 {
		next = 0
	}
	if next >= len(m.members) {
		next = len(m.members) - 1
	}
	m.memberCursor = next
	return m.syncMemberSelection()
}

// startMemberWorkLoad bumps the load generation so a superseded in-flight load is dropped. Pointer
// receiver so the bump propagates through the value-receiver syncMemberSelection.
func (m *Model) startMemberWorkLoad(id int64) tea.Cmd {
	m.memberWorkGen[id]++
	m.memberWorkPending[id] = true
	m.memberWorkFailed[id] = false
	return loadMemberWork(m.deps, m.projectKey, id, m.memberWorkGen[id])
}

func (m Model) onMemberListKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	m.hover = ""
	switch msg.String() {
	case "up", "k":
		return m.moveMemberCursor(-1)
	case "down", "j":
		return m.moveMemberCursor(1)
	case "home", "g":
		m.memberCursor = 0
		return m.syncMemberSelection()
	case "end", "G":
		m.memberCursor = max(0, len(m.members)-1)
		return m.syncMemberSelection()
	case "a":
		return m.openAddMemberPicker()
	case "r":
		return m.openMemberRolePicker()
	case "d":
		return m.openMemberKickConfirm()
	}
	return m, nil
}

func (m Model) onMemberWheel(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	if zone.Get(zoneMemberDetail).InBounds(msg) {
		m.memberDetailScroll = wheelClamp(m.memberDetailScroll, msg.Button, m.memberDetailScrollMax())
		return m, nil
	}
	switch msg.Button {
	case tea.MouseWheelUp:
		return m.moveMemberCursor(-1)
	case tea.MouseWheelDown:
		return m.moveMemberCursor(1)
	}
	return m, nil
}

// pruneMemberWork drops cached and in-flight work for ids no longer in the roster, so re-adding that id
// cannot show the pre-kick entry. Runs on every fresh roster.
func (m *Model) pruneMemberWork() {
	keep := make(map[int64]bool, len(m.members))
	for _, mem := range m.members {
		keep[mem.MemberID] = true
	}
	for id := range m.memberWork {
		if !keep[id] {
			delete(m.memberWork, id)
		}
	}
	for id := range m.memberWorkFailed {
		if !keep[id] {
			delete(m.memberWorkFailed, id)
		}
	}
	for id := range m.memberWorkPending {
		if !keep[id] {
			m.memberWorkGen[id]++ // supersede any in-flight load so its late result cannot repopulate the entry
			delete(m.memberWorkPending, id)
		}
	}
}

func (m Model) onMemberWorkLoaded(msg memberWorkLoadedMsg) (Model, tea.Cmd) {
	if msg.gen != m.memberWorkGen[msg.memberID] {
		return m, nil // a superseded load for this member landed late
	}
	delete(m.memberWorkPending, msg.memberID)
	if msg.err {
		m.memberWorkFailed[msg.memberID] = true
		return m, nil
	}
	m.memberWorkFailed[msg.memberID] = false
	m.memberWork[msg.memberID] = msg.work
	if msg.memberID == m.selMemberID {
		m.memberDetailScroll = clampScroll(m.memberDetailScroll, m.memberDetailScrollMax())
	}
	return m, nil
}

type memberWorkLoadedMsg struct {
	memberID int64
	gen      int
	work     memberWorkload
	err      bool
}

type memberStatsLoadedMsg struct {
	gen   int
	stats []domain.MemberStats
	err   bool
}

func loadMemberStats(d deps.Deps, projectKey string, gen int) tea.Cmd {
	return func() tea.Msg {
		stats, err := d.Projects.MemberStats(context.Background(), projectKey)
		return memberStatsLoadedMsg{gen: gen, stats: stats, err: err != nil}
	}
}

func loadMemberWork(d deps.Deps, projectKey string, memberID int64, gen int) tea.Cmd {
	return func() tea.Msg {
		id := strconv.FormatInt(memberID, 10)
		open := []string{"INITIAL", "ACTIVE"}
		assigned, aErr := d.Issues.SearchProjectIssues(context.Background(), projectKey,
			domain.IssueFilter{AssigneeMemberIDs: []string{id}, StateCategories: open}, 0, pageSize)
		reviewer, rErr := d.Issues.SearchProjectIssues(context.Background(), projectKey,
			domain.IssueFilter{ReviewerMemberIDs: []string{id}, StateCategories: open}, 0, pageSize)
		// a contributions failure leaves an empty heatmap rather than failing the whole member load
		contrib, _ := d.Projects.GetProjectContributions(context.Background(), projectKey, memberID, contribDays)
		return memberWorkLoadedMsg{
			memberID: memberID,
			gen:      gen,
			work:     memberWorkload{assigned: assigned, reviewer: reviewer, contrib: contrib},
			err:      aErr != nil || rErr != nil,
		}
	}
}
