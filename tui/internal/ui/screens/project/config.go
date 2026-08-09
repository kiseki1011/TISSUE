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

// loadProjectConfig kicks off the project-settings fetch the first time the Config tab is opened (and to
// retry after a failed load). Mirrors the sprint-list lazy load.
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
			// a silent post-action refresh failed but the settings we were showing are still good: keep them
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
	// a reload can shrink the body (e.g. a shortened/cleared description), so re-clamp the scroll offset -
	// else a stale offset eats the first scroll key as a dead press (mirrors the sprint/member handlers)
	m.configDetailScroll = clampScroll(m.configDetailScroll, m.configScrollMax())
	return m, nil
}

// onConfigKey drives the Config tab: edit the settings or toggle the archive state.
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

// openConfigEditForm opens the settings edit modal, prefilled from the loaded project.
func (m Model) openConfigEditForm() (Model, tea.Cmd) {
	if !m.configLoaded {
		return m, toast.Show(toast.Info, "Still loading the project settings…")
	}
	if m.project.Archived {
		// an archived project is read-only (the server rejects title/description edits), so editing is
		// gated behind restoring it first
		return m, toast.Show(toast.Info, "Restore the project first to edit its settings.")
	}
	m.configEditing = true
	m.configEditScroll = 0
	m.configEditBase = m.project // diff the save against this snapshot, not a value a background reload may change
	m.configEditUI = newConfigEditForm(m.deps, m.project)
	return m, m.configEditUI.Init()
}

// updateConfigEdit drives the open settings modal: submit/cancel close it, a wheel scrolls a modal too
// tall for the terminal, and anything else forwards to the form.
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

// followConfigEditFocus scrolls the windowed edit modal so the focused control stays visible, mirroring
// the sprint edit form. A no-op when the modal already fits the terminal.
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

// diffConfigEdit builds the PATCH: a field is included only when it differs from the loaded project. A
// description cleared to empty is sent as an explicit clear.
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

// toggleArchive archives an active project (behind a confirmation, since it makes the project and every
// item in it read-only) or restores an archived one directly (a safe, reversible action).
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

// updateConfigConfirm drives the open confirmation: accept fires the archive, or the GitHub disconnect
// when that is the pending action; cancel closes it.
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

// regenerateGithubSecret (re)issues the project's GitHub webhook secret, lazily creating the integration
// if needed. The secret comes back once, revealed in the GitHub section until the tab is reopened.
func (m Model) regenerateGithubSecret() (Model, tea.Cmd) {
	if !m.configLoaded {
		return m, toast.Show(toast.Info, "Still loading the project settings…")
	}
	return m, regenerateGithubSecretCmd(m.deps, m.projectKey)
}

// disconnectGithub confirms, then removes the project's GitHub integration.
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

// toggleGithubSync pauses or resumes acting on inbound webhooks. Unlike disconnecting, this keeps the
// secret valid, so resuming does not mean re-registering the webhook on GitHub.
func (m Model) toggleGithubSync() (Model, tea.Cmd) {
	if !m.configLoaded {
		return m, toast.Show(toast.Info, "Still loading the project settings…")
	}
	if !m.github.Connected {
		return m, toast.Show(toast.Info, "GitHub isn't connected.")
	}
	return m, setGithubSyncCmd(m.deps, m.projectKey, !m.github.SyncEnabled)
}

// copyGithubSecret puts the one-time webhook secret on the system clipboard. It is only bound while a
// secret is on screen: the value is unrecoverable once the reveal is gone, so a copy key that silently did
// nothing after a reload would be worse than no key at all.
func (m Model) copyGithubSecret() (Model, tea.Cmd) {
	secret := m.githubSecret.Secret
	if secret == "" {
		return m, nil
	}
	m.githubSecretCopied = true
	return m, tea.Batch(tea.SetClipboard(secret), toast.Show(toast.Success, "Webhook secret copied."))
}

// copyGithubURL puts the webhook endpoint on the system clipboard. Registering the integration in GitHub
// means pasting this and the secret into the same form, so the URL stays copyable for as long as the
// integration exists - unlike the secret, it can be read back at any time.
func (m Model) copyGithubURL() (Model, tea.Cmd) {
	url := m.github.WebhookURL
	if url == "" {
		return m, nil
	}
	m.githubURLCopied = true
	return m, tea.Batch(tea.SetClipboard(url), toast.Show(toast.Success, "Webhook URL copied."))
}

// onGithubSync applies the server's new integration status. It deliberately does not reload the config,
// which would wipe a secret still revealed on screen from a rotation in the same visit.
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
	// keep the reveal on screen (the secret is shown once) and reflect that the integration is now connected,
	// without a reload that would wipe the secret. A first-time connect is active; rotating the secret of an
	// existing integration must not silently flip its real sync state, so preserve it in that case.
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
	// the integration changed: reload the config so the GitHub section reflects the new status
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

// githubErrorText maps a failed GitHub action to a friendly toast: a mapped leaky code or connectivity
// message first, then a role hint, then the server's own reason, then a generic fallback.
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

// configDeliveryCount is how many recent deliveries the Config tab keeps. Enough to cover the pushes
// behind "why didn't my branch attach?", short enough to read without paging.
const configDeliveryCount = 10

func loadConfig(d deps.Deps, projectKey string, gen int) tea.Cmd {
	return func() tea.Msg {
		p, err := d.Projects.GetProjectDetail(context.Background(), projectKey)
		msg := configLoadedMsg{key: projectKey, gen: gen, project: p, err: err != nil}
		// the GitHub integration status loads alongside the settings; its failure never blanks the tab
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

// configActionDoneMsg is the result of an edit/archive/unarchive command. On success the settings are
// reloaded so the panel reflects the change; on failure the status maps to a helpful toast.
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
