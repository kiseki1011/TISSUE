package project

import (
	"context"
	"net/http"
	"strings"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// loadProjectConfig fetches the project settings on first open of the Config tab, and on retry.
func (m Model) loadProjectConfig() (Model, tea.Cmd) {
	m.configRequested = true
	m.configLoading = true
	m.configErr = false
	m.configReqGen++
	return m, loadConfig(m.deps, m.projectKey, m.configReqGen)
}

func (m Model) onConfigLoaded(msg configLoadedMsg) (Model, tea.Cmd) {
	if msg.key != m.projectKey || msg.gen != m.configReqGen {
		return m, nil // a stale cross-project result or a superseded reload landed late
	}
	m.configLoading = false
	if msg.err {
		if m.configLoaded {
			// the refresh failed but the settings on screen are still good
			return m, toast.Show(toast.Error, "Couldn't refresh the project settings.")
		}
		m.configErr = true
		m.configRequested = false // let reopening the tab retry
		return m, nil
	}
	m.configErr = false
	m.configLoaded = true
	m.project = msg.project
	m.github, m.githubLoaded = msg.github, msg.githubOK
	m.deliveries, m.deliveriesLoaded = msg.deliveries, msg.deliveriesOK
	// fresh data hides any one-time secret from a prior reveal
	m.githubSecret, m.githubSecretCopied, m.githubURLCopied = domain.GithubSecret{}, false, false
	m.title = msg.project.Title // keep the drill-in header/breadcrumb in step with an edited title
	// a reload can shrink the body, so re-clamp: a stale offset eats the first scroll key
	m.configDetailScroll = clampScroll(m.configDetailScroll, m.configScrollMax())
	return m, nil
}

func (m Model) onConfigKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "e":
		return m.openConfigEditForm()
	case "a":
		return m.toggleArchive()
	case "g":
		return m.regenerateGithubSecret()
	case "s":
		return m.toggleGithubSync()
	case "x":
		return m.disconnectGithub()
	case "y", "c":
		return m.copyGithubSecret()
	case "u":
		return m.copyGithubURL()
	case "up", "k":
		m.configDetailScroll = clampScroll(m.configDetailScroll-1, m.configScrollMax())
		return m, nil
	case "down", "j":
		m.configDetailScroll = clampScroll(m.configDetailScroll+1, m.configScrollMax())
		return m, nil
	}
	return m, nil
}

func (m Model) onConfigWheel(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	m.configDetailScroll = wheelClamp(m.configDetailScroll, msg.Button, m.configScrollMax())
	return m, nil
}

func (m Model) openConfigEditForm() (Model, tea.Cmd) {
	if !m.configLoaded {
		return m, toast.Show(toast.Info, "Still loading the project settings…")
	}
	if m.project.Archived {
		// an archived project is read-only: the server rejects edits, so restore first
		return m, toast.Show(toast.Info, "Restore the project first to edit its settings.")
	}
	m.configEditing = true
	m.configEditScroll = 0
	m.configEditBase = m.project // diff the save against this snapshot, not a value a background reload may change
	m.configEditUI = newConfigEditForm(m.deps, m.project)
	return m, m.configEditUI.Init()
}

func (m Model) updateConfigEdit(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case configEditCancelledMsg:
		m.configEditing = false
		return m, nil
	case configEditSubmittedMsg:
		return m.submitConfigEdit(msg.v)
	case tea.MouseWheelMsg:
		if lipgloss.Height(m.configEditUI.View()) > m.height {
			switch msg.Button {
			case tea.MouseWheelUp:
				m.configEditScroll = clampScroll(m.configEditScroll-1, m.configEditScrollMax())
				return m, nil
			case tea.MouseWheelDown:
				m.configEditScroll = clampScroll(m.configEditScroll+1, m.configEditScrollMax())
				return m, nil
			}
		}
	}
	var cmd tea.Cmd
	m.configEditUI, cmd = m.configEditUI.Update(msg)
	return m.followConfigEditFocus(), cmd
}

func (m Model) configEditScrollMax() int {
	return max(0, lipgloss.Height(m.configEditUI.View())-m.height)
}

// followConfigEditFocus scrolls the windowed edit modal so the focused control stays visible.
func (m Model) followConfigEditFocus() Model {
	row, height, ok := m.configEditUI.FocusRow()
	if !ok {
		return m
	}
	boxH := lipgloss.Height(m.configEditUI.View())
	if boxH <= m.height {
		return m
	}
	visible := m.height - 2
	off := m.configEditScroll
	top, bottom := row, row+max(1, height)-1
	if top < 1+off {
		off = top - 1
	} else if bottom > off+visible {
		off = bottom - visible
	}
	m.configEditScroll = min(max(off, 0), boxH-m.height)
	return m
}

// submitConfigEdit sends only the fields that changed (a PATCH), diffed against the open-time snapshot.
func (m Model) submitConfigEdit(v configEditValues) (Model, tea.Cmd) {
	m.configEditing = false
	edit := diffConfigEdit(m.configEditBase, v)
	if edit.Empty() {
		return m, toast.Show(toast.Info, "No changes.")
	}
	return m, updateProjectCmd(m.deps, m.projectKey, edit)
}

// diffConfigEdit includes only changed fields. A cleared description is sent as an explicit clear.
func diffConfigEdit(orig domain.Project, v configEditValues) domain.ProjectEdit {
	var out domain.ProjectEdit
	if v.title != strings.TrimSpace(orig.Title) {
		out.Title = &v.title
	}
	if v.description != strings.TrimSpace(orig.Description) {
		if v.description == "" {
			out.ClearDescription = true
		} else {
			out.Description = &v.description
		}
	}
	if v.visibility != orig.Visibility {
		out.Visibility = &v.visibility
	}
	return out
}

// Archiving is confirmed (it makes everything read-only). Restoring is direct and reversible.
func (m Model) toggleArchive() (Model, tea.Cmd) {
	if !m.configLoaded {
		return m, toast.Show(toast.Info, "Still loading the project settings…")
	}
	if m.project.Archived {
		return m, unarchiveProjectCmd(m.deps, m.projectKey)
	}
	m.configConfirming = true
	m.configConfirmUI = widgets.NewConfirmForm(m.deps.Styles, "Archive project",
		"Archive "+flattenLine(m.project.Title)+"? The project and everything in it becomes read-only until you restore it.", "Archive")
	return m, m.configConfirmUI.Init()
}

// One confirmation serves both archive and GitHub disconnect. githubConfirming says which accept fires.
func (m Model) updateConfigConfirm(msg tea.Msg) (Model, tea.Cmd) {
	switch msg.(type) {
	case widgets.ConfirmAcceptedMsg:
		m.configConfirming = false
		if m.githubConfirming {
			m.githubConfirming = false
			return m, removeGithubCmd(m.deps, m.projectKey)
		}
		return m, archiveProjectCmd(m.deps, m.projectKey)
	case widgets.ConfirmCancelledMsg:
		m.configConfirming = false
		m.githubConfirming = false
		return m, nil
	}
	var cmd tea.Cmd
	m.configConfirmUI, cmd = m.configConfirmUI.Update(msg)
	return m, cmd
}

// The secret comes back once, revealed until the tab is reopened. The integration is created lazily.
func (m Model) regenerateGithubSecret() (Model, tea.Cmd) {
	if !m.configLoaded {
		return m, toast.Show(toast.Info, "Still loading the project settings…")
	}
	return m, regenerateGithubSecretCmd(m.deps, m.projectKey)
}

func (m Model) disconnectGithub() (Model, tea.Cmd) {
	if !m.configLoaded {
		return m, toast.Show(toast.Info, "Still loading the project settings…")
	}
	if !m.github.Connected {
		return m, toast.Show(toast.Info, "GitHub isn't connected.")
	}
	m.configConfirming = true
	m.githubConfirming = true
	m.configConfirmUI = widgets.NewConfirmForm(m.deps.Styles, "Disconnect GitHub",
		"Disconnect the GitHub integration? Branches already linked stay, but new pushes stop syncing.", "Disconnect")
	return m, m.configConfirmUI.Init()
}

// Unlike disconnecting, pausing keeps the secret valid, so resuming needs no re-registration on GitHub.
func (m Model) toggleGithubSync() (Model, tea.Cmd) {
	if !m.configLoaded {
		return m, toast.Show(toast.Info, "Still loading the project settings…")
	}
	if !m.github.Connected {
		return m, toast.Show(toast.Info, "GitHub isn't connected.")
	}
	return m, setGithubSyncCmd(m.deps, m.projectKey, !m.github.SyncEnabled)
}

// copyGithubSecret is bound only while a secret is on screen: the value is unrecoverable once the
// reveal is gone, so a copy key that silently did nothing would be worse than no key.
func (m Model) copyGithubSecret() (Model, tea.Cmd) {
	secret := m.githubSecret.Secret
	if secret == "" {
		return m, nil
	}
	m.githubSecretCopied = true
	return m, tea.Batch(tea.SetClipboard(secret), toast.Show(toast.Success, "Webhook secret copied."))
}

// Unlike the secret, the webhook URL can be read back at any time, so its copy key always works.
func (m Model) copyGithubURL() (Model, tea.Cmd) {
	url := m.github.WebhookURL
	if url == "" {
		return m, nil
	}
	m.githubURLCopied = true
	return m, tea.Batch(tea.SetClipboard(url), toast.Show(toast.Success, "Webhook URL copied."))
}

// onGithubSync does not reload the config: that would wipe a secret still revealed on screen.
func (m Model) onGithubSync(msg githubSyncMsg) (Model, tea.Cmd) {
	if msg.err {
		return m, toast.Show(toast.Error, githubErrorText(msg.status, msg.code, msg.reason))
	}
	m.github = msg.github
	if msg.github.SyncEnabled {
		return m, toast.Show(toast.Success, "GitHub sync resumed.")
	}
	return m, toast.Show(toast.Success, "GitHub sync paused — inbound events are ignored.")
}

func (m Model) onGithubSecret(msg githubSecretMsg) (Model, tea.Cmd) {
	if msg.err {
		return m, toast.Show(toast.Error, githubErrorText(msg.status, msg.code, msg.reason))
	}
	// no reload here: it would wipe the once-shown secret. A first-time connect is active, but rotating an
	// existing integration's secret must not flip its real sync state.
	sync := true
	if m.github.Connected {
		sync = m.github.SyncEnabled
	}
	m.githubSecret, m.githubSecretCopied, m.githubURLCopied = msg.secret, false, false
	m.github = domain.GithubIntegration{Connected: true, WebhookURL: msg.secret.WebhookURL, SyncEnabled: sync}
	m.githubLoaded = true
	m.configDetailScroll = clampScroll(m.configDetailScroll, m.configScrollMax())
	return m, toast.Show(toast.Success, "Webhook secret generated — press y to copy it now.")
}

func (m Model) onGithubAction(msg githubActionMsg) (Model, tea.Cmd) {
	if msg.err {
		return m, toast.Show(toast.Error, githubErrorText(msg.status, msg.code, msg.reason))
	}
	// the integration changed, so reload the GitHub section
	m.configRequested = true
	m.configReqGen++
	return m, tea.Batch(
		toast.Show(toast.Success, "GitHub disconnected."),
		loadConfig(m.deps, m.projectKey, m.configReqGen),
	)
}

type githubSecretMsg struct {
	secret domain.GithubSecret
	err    bool
	status int
	code   string
	reason string
}

type githubActionMsg struct {
	err    bool
	status int
	code   string
	reason string
}

type githubSyncMsg struct {
	github domain.GithubIntegration
	err    bool
	status int
	code   string
	reason string
}

func regenerateGithubSecretCmd(d deps.Deps, projectKey string) tea.Cmd {
	return func() tea.Msg {
		secret, err := d.Projects.RegenerateGithubSecret(context.Background(), projectKey)
		return githubSecretMsg{
			secret: secret, err: err != nil,
			status: statusOf(err), code: codeOf(err), reason: reasonOf(err),
		}
	}
}

func setGithubSyncCmd(d deps.Deps, projectKey string, enabled bool) tea.Cmd {
	return func() tea.Msg {
		gh, err := d.Projects.SetGithubSync(context.Background(), projectKey, enabled)
		return githubSyncMsg{
			github: gh, err: err != nil,
			status: statusOf(err), code: codeOf(err), reason: reasonOf(err),
		}
	}
}

func removeGithubCmd(d deps.Deps, projectKey string) tea.Cmd {
	return func() tea.Msg {
		err := d.Projects.RemoveGithubIntegration(context.Background(), projectKey)
		return githubActionMsg{err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func githubErrorText(status int, code, reason string) string {
	if m, ok := errmsg.OverrideParts(status, code); ok {
		return m
	}
	if status == http.StatusForbidden {
		return "You need the Manager role for that."
	}
	if reason != "" {
		return reason
	}
	return "Couldn't update the GitHub integration."
}

type configLoadedMsg struct {
	key     string
	gen     int
	project domain.Project
	err     bool

	github   domain.GithubIntegration
	githubOK bool // the GitHub status is a nice-to-have: a failure just hides that section

	deliveries   []domain.WebhookDelivery
	deliveriesOK bool // manager-only, so a plain member's 403 simply hides the log
}

// configDeliveryCount covers the pushes behind "why didn't my branch attach?" without paging.
const configDeliveryCount = 10

func loadConfig(d deps.Deps, projectKey string, gen int) tea.Cmd {
	return func() tea.Msg {
		p, err := d.Projects.GetProjectDetail(context.Background(), projectKey)
		msg := configLoadedMsg{key: projectKey, gen: gen, project: p, err: err != nil}
		// a GitHub status failure never blanks the tab
		if gh, e := d.Projects.GetGithubIntegration(context.Background(), projectKey); e == nil {
			msg.github, msg.githubOK = gh, true
		}
		// deliveries only mean anything once connected, and only a manager may read them
		if msg.githubOK && msg.github.Connected {
			if ds, e := d.Projects.ListGithubDeliveries(context.Background(), projectKey, configDeliveryCount); e == nil {
				msg.deliveries, msg.deliveriesOK = ds, true
			}
		}
		return msg
	}
}

type configActionDoneMsg struct {
	action string // "edit" | "archive" | "unarchive"
	err    bool
	status int
	code   string // the backend error code, for mapping a leaky code to friendlier copy
	reason string // the server's explanation on failure, so the toast can say why
}

func (m Model) onConfigActionDone(msg configActionDoneMsg) (Model, tea.Cmd) {
	if msg.err {
		return m, toast.Show(toast.Error, configActionErrorText(msg.action, msg.status, msg.code, msg.reason))
	}
	m.configRequested = true
	m.configReqGen++
	return m, tea.Batch(
		toast.Show(toast.Success, configActionOkText(msg.action)),
		loadConfig(m.deps, m.projectKey, m.configReqGen),
		func() tea.Msg { return nav.RefreshDashboardMsg{} }, // the change shows on the dashboard on return
	)
}

func updateProjectCmd(d deps.Deps, projectKey string, e domain.ProjectEdit) tea.Cmd {
	return func() tea.Msg {
		err := d.Projects.UpdateProject(context.Background(), projectKey, e)
		return configActionDoneMsg{action: "edit", err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func archiveProjectCmd(d deps.Deps, projectKey string) tea.Cmd {
	return func() tea.Msg {
		err := d.Projects.ArchiveProject(context.Background(), projectKey)
		return configActionDoneMsg{action: "archive", err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func unarchiveProjectCmd(d deps.Deps, projectKey string) tea.Cmd {
	return func() tea.Msg {
		err := d.Projects.UnarchiveProject(context.Background(), projectKey)
		return configActionDoneMsg{action: "unarchive", err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func configActionOkText(action string) string {
	switch action {
	case "edit":
		return "Project updated."
	case "archive":
		return "Project archived."
	case "unarchive":
		return "Project restored."
	}
	return "Done."
}

func configActionErrorText(action string, status int, code, reason string) string {
	if m, ok := errmsg.OverrideParts(status, code); ok {
		return m // connectivity, or a mapped leaky code (PROJECT_MEMBER_NOT_FOUND / PROJECT_MANAGER_REQUIRED / …)
	}
	switch status {
	case http.StatusForbidden:
		return "You need the Manager role for that."
	case http.StatusConflict:
		if action == "archive" {
			return "The project is already archived."
		}
		if action == "unarchive" {
			return "The project is not archived."
		}
	}
	if reason != "" {
		return reason // the server's explanation for the long tail (validation, not-found, …)
	}
	switch action {
	case "edit":
		return "Couldn't update the project."
	case "archive":
		return "Couldn't archive the project."
	case "unarchive":
		return "Couldn't restore the project."
	}
	return "Something went wrong."
}
