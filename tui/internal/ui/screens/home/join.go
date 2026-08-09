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

// enterProject opens the selected project. A member opens it directly; a non-member is offered a join
// first, because the project's issues, members and sprints all require membership to read - browsing
// without joining is not possible. A PRIVATE project can only be self-joined by a system admin; anyone
// else must be added by a manager, so they are told rather than shown a join that would only 403.
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

// updateJoin drives the join confirmation: accept joins the target, cancel closes it.
func (m Model) updateJoin(msg tea.Msg) (Model, tea.Cmd) {
	switch msg.(type) {
	case widgets.ConfirmAcceptedMsg:
		// keep the modal open in its submitting state while the join POST is in flight: this holds input
		// capture (so app-level tab/option keys cannot switch away and drop the home-internal joinDoneMsg)
		// until onJoinDone lands and closes it
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

// onJoinDone opens the project on a successful join (and reloads the list in the background so the row
// reflects the new membership when the user returns); on failure it surfaces the reason - a PRIVATE
// project a non-admin cannot self-join maps to "ask a manager to add you".
func (m Model) onJoinDone(msg joinDoneMsg) (Model, tea.Cmd) {
	m.joining = false // close the submitting modal now that the result has landed
	if msg.err {
		return m, toast.Show(toast.Error, msg.errText)
	}
	return m, tea.Batch(
		toast.Show(toast.Success, "Joined "+projectName(msg.project)+"."),
		openProjectCmd(msg.project),
		loadProjects(m.deps), // the row's MyRole is now stale; refresh so a return reflects membership
	)
}

// WithAdmin records whether the caller is a system admin (able to self-join even a PRIVATE project). It is
// back-filled from the profile once that loads.
func (m Model) WithAdmin(v bool) Model {
	m.isAdmin = v
	return m
}

// projectName is the project's title (or its key when untitled), flattened to a single line and quoted for
// a toast or modal prompt.
func projectName(p domain.Project) string {
	name := strings.TrimSpace(p.Title)
	if name == "" {
		name = p.Key
	}
	return "\"" + strings.ReplaceAll(name, "\n", " ") + "\""
}
