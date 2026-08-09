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

// openAssigneePicker builds a picker of the project's active members plus an Unassigned option, with
// the cursor on the issue's current assignee.
func (m Model) openAssigneePicker() (Model, tea.Cmd) {
	// the detail must be loaded so the cursor can start on the real assignee; opening on the skeleton
	// would preselect Unassigned and let an accidental Enter silently unassign an assigned issue
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

	// Unassigned is shown in the error color so it reads as clearing the assignee, not as a normal member
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
	// searchable + a taller window, since a project can have many members
	m.picker = widgets.NewSearchableListPicker("Assign to", opts, current, assigneeRows, w)
	return m, nil
}

// assigneeRows is the visible row budget of the assignee picker, taller than the transition picker
// because a project can have many members (the filter narrows them, the window scrolls the rest).
const assigneeRows = 10

// selectAssignee assigns the highlighted member, or clears the assignee for the Unassigned row.
func (m Model) selectAssignee() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	if opt.Value == "" {
		m.applyAssignee(m.viewKey, 0, "") // optimistic: show Unassigned at once
		return m, unassignIssue(m.deps, m.viewKey)
	}
	id, err := strconv.ParseInt(opt.Value, 10, 64)
	if err != nil {
		return m, nil
	}
	m.applyAssignee(m.viewKey, id, opt.Label) // optimistic: show the new assignee at once
	return m, assignIssue(m.deps, m.viewKey, id, opt.Label)
}

// applyAssignee optimistically sets the cached detail's assignee (and its list row) so the modal
// updates smoothly before the server confirms; a background refetch reconciles it. It bumps the load
// generation so an earlier in-flight refetch cannot land and clobber this optimistic write.
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

// AssignDoneMsg is exported so the app shell can route this background result back to the project
// screen even when the user has left the drill-in before it landed (so the toast still shows).
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
