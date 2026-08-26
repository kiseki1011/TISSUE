package home

import (
	"context"
	"strings"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// enterProject offers a non-member a join first, since issues, members and sprints all require
// membership to read. Only a system admin can self-join a PRIVATE project, so others are told instead.
func (m Model) enterProject(p domain.Project) (Model, tea.Cmd) {
	if p.MyRole != "" {
		return m, openProjectCmd(p) // already a member
	}
	private := strings.EqualFold(p.Visibility, "PRIVATE")
	if private && !m.isAdmin {
		return m, toast.Show(toast.Info, projectName(p)+" is private — ask a manager to add you.")
	}
	m.joining = true
	m.joinTarget = p
	prompt := "You're not a member of " + projectName(p) + ". Join it to open?"
	if private {
		prompt = projectName(p) + " is private. Join it as an admin to open?"
	}
	m.joinUI = widgets.NewConfirmForm(m.deps.Styles, "Join project", prompt, "Join")
	return m, m.joinUI.Init()
}

func openProjectCmd(p domain.Project) tea.Cmd {
	return func() tea.Msg { return nav.OpenProjectMsg{ProjectKey: p.Key, Title: p.Title} }
}

func (m Model) updateJoin(msg tea.Msg) (Model, tea.Cmd) {
	switch msg.(type) {
	case widgets.ConfirmAcceptedMsg:
		// hold the modal open while the POST is in flight — its input capture stops an app-level key
		// switching away and dropping the home-internal joinDoneMsg
		return m, joinProjectCmd(m.deps, m.joinTarget)
	case widgets.ConfirmCancelledMsg:
		m.joining = false
		return m, nil
	}
	var cmd tea.Cmd
	m.joinUI, cmd = m.joinUI.Update(msg)
	return m, cmd
}

type joinDoneMsg struct {
	project domain.Project
	err     bool
	errText string
}

func joinProjectCmd(d deps.Deps, p domain.Project) tea.Cmd {
	return func() tea.Msg {
		err := d.Projects.JoinProject(context.Background(), p.Key)
		return joinDoneMsg{project: p, err: err != nil, errText: errmsg.Message(err, "Couldn't join the project.")}
	}
}

// onJoinDone opens the project, or surfaces the reason the join failed.
func (m Model) onJoinDone(msg joinDoneMsg) (Model, tea.Cmd) {
	m.joining = false
	if msg.err {
		return m, toast.Show(toast.Error, msg.errText)
	}
	return m, tea.Batch(
		toast.Show(toast.Success, "Joined "+projectName(msg.project)+"."),
		openProjectCmd(msg.project),
		loadProjects(m.deps), // the row's MyRole is now stale, so a return would not reflect membership
	)
}

// WithAdmin records system-admin status (self-join even a PRIVATE project), back-filled from the profile.
func (m Model) WithAdmin(v bool) Model {
	m.isAdmin = v
	return m
}

// projectName is the title (or the key when untitled), flattened to one line and quoted for a prompt.
func projectName(p domain.Project) string {
	name := strings.TrimSpace(p.Title)
	if name == "" {
		name = p.Key
	}
	return "\"" + strings.ReplaceAll(name, "\n", " ") + "\""
}
