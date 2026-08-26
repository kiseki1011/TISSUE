package project

import (
	"context"
	"strconv"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

func (m Model) openAssigneePicker() (Model, tea.Cmd) {
	// without a loaded detail the picker preselects Unassigned, so a stray Enter unassigns
	d, ok := m.details[m.viewKey]
	if !ok {
		return m, toast.Show(toast.Info, "Still loading this issue…")
	}
	if !m.membersLoaded {
		return m, toast.Show(toast.Info, "Loading members…")
	}
	if len(m.members) == 0 {
		return m, toast.Show(toast.Info, "No members to assign.")
	}

	// error color so Unassigned reads as clearing the assignee, not as a member
	opts := []widgets.PickerOption{{Value: "", Label: "Unassigned", Color: m.deps.Styles.Theme.Error}}
	w := pickerMinW
	for _, mem := range m.members {
		label := mem.Name()
		opts = append(opts, widgets.PickerOption{Value: strconv.FormatInt(mem.MemberID, 10), Label: label})
		if lw := lipgloss.Width(label) + 2; lw > w {
			w = lw
		}
	}
	if w > pickerMaxW {
		w = pickerMaxW
	}

	current := ""
	if d.AssigneeID != 0 {
		current = strconv.FormatInt(d.AssigneeID, 10)
	}

	m.picking = true
	m.pickKind = pickAssignee
	m.picker = widgets.NewSearchableListPicker("Assign to", opts, current, assigneeRows, w)
	return m, nil
}

// assigneeRows is taller than the transition picker's window: a project can have many members.
const assigneeRows = 10

func (m Model) selectAssignee() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	if opt.Value == "" {
		m.applyAssignee(m.viewKey, 0, "")
		return m, unassignIssue(m.deps, m.viewKey)
	}
	id, err := strconv.ParseInt(opt.Value, 10, 64)
	if err != nil {
		return m, nil
	}
	m.applyAssignee(m.viewKey, id, opt.Label)
	return m, assignIssue(m.deps, m.viewKey, id, opt.Label)
}

// applyAssignee optimistically sets the cached detail's assignee and its list row. Bumping the load
// generation stops an earlier in-flight refetch from clobbering it.
func (m *Model) applyAssignee(key string, id int64, name string) {
	d, ok := m.details[key]
	if !ok {
		return
	}
	m.detailGen[key]++
	d.AssigneeID = id
	d.AssigneeName = name
	m.details[key] = d
	m.patchRow(key, d)
}

// AssignDoneMsg is exported so the app shell can route it after the user leaves the drill-in.
type AssignDoneMsg struct {
	key      string
	assignee string // display name, or "" when unassigned
	err      bool
	errText  string // the resolved failure toast line (server reason / mapped code / fallback)
}

func assignIssue(d deps.Deps, key string, memberID int64, name string) tea.Cmd {
	return func() tea.Msg {
		err := d.Issues.AssignIssue(context.Background(), key, memberID)
		return AssignDoneMsg{key: key, assignee: name, err: err != nil, errText: errmsg.Message(err, "Could not change the assignee.")}
	}
}

func unassignIssue(d deps.Deps, key string) tea.Cmd {
	return func() tea.Msg {
		err := d.Issues.UnassignIssue(context.Background(), key)
		return AssignDoneMsg{key: key, assignee: "", err: err != nil, errText: errmsg.Message(err, "Could not change the assignee.")}
	}
}

type membersLoadedMsg struct {
	members []domain.ProjectMember
	err     bool
}

func loadMembers(d deps.Deps, projectKey string) tea.Cmd {
	return func() tea.Msg {
		members, err := d.Projects.ListMembers(context.Background(), projectKey)
		return membersLoadedMsg{members: members, err: err != nil}
	}
}
