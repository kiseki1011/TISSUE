package project

import (
	"net/http"
	"strings"
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// configTabModel is a loaded model sitting on the Config tab with the given project settings already
// loaded, for exercising the tab without a network load.
func configTabModel(t *testing.T, p domain.Project) Model {
	t.Helper()
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabConfig
	m.configRequested = true
	m.configLoaded = true
	m.project = p
	m.title = p.Title
	return m
}

// Opening the Config tab (key 5) for the first time kicks off the settings load; reopening it once
// loaded does not refetch.
func TestConfigLazyLoad(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m, cmd := m.Update(press("5"))
	if m.tab != tabConfig {
		t.Fatalf("key 5 should select the Config tab, got %v", m.tab)
	}
	if !m.configRequested || cmd == nil {
		t.Fatalf("opening Config should load the settings: requested=%v cmd=%v", m.configRequested, cmd != nil)
	}
	m, _ = m.Update(configLoadedMsg{key: m.projectKey, gen: m.configReqGen, project: domain.Project{Key: "PROJ", Title: "P"}})
	_, cmd = m.Update(press("5"))
	if cmd != nil {
		t.Error("reopening Config once loaded should not refetch")
	}
}

// The Config view renders the loaded settings: key, visibility, status, and the description.
func TestConfigRendersSettings(t *testing.T) {
	m := configTabModel(t, domain.Project{
		Key: "PROJ", Title: "My Project", Description: "A thing", Visibility: "PRIVATE", Archived: false,
	})
	out := plain(m.View())
	for _, want := range []string{"Settings", "PROJ", "Visibility", "Private", "Status", "Active", "Description", "A thing"} {
		if !strings.Contains(out, want) {
			t.Errorf("config view missing %q:\n%s", want, out)
		}
	}
}

// An archived project reads as archived (and read-only) and shows None for an empty description.
func TestConfigRendersArchivedAndEmptyDescription(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "P", Visibility: "PUBLIC", Archived: true})
	out := plain(m.View())
	if !strings.Contains(out, "Archived") || !strings.Contains(out, "read-only") {
		t.Errorf("archived project should read Archived and read-only:\n%s", out)
	}
	if !strings.Contains(out, "None") {
		t.Errorf("an empty description should render None:\n%s", out)
	}
}

// Editing is gated on an archived (read-only) project: pressing e does not open the form, it explains why.
func TestConfigArchivedBlocksEdit(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "P", Archived: true})
	m, cmd := m.Update(press("e"))
	if m.configEditing {
		t.Error("editing an archived project should be blocked")
	}
	if cmd == nil {
		t.Error("blocking the edit should surface an explanatory toast")
	}
}

// Restoring is offered (not blocked) on an archived project: pressing a fires the unarchive directly.
func TestConfigArchivedAllowsRestore(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "P", Archived: true})
	m, cmd := m.Update(press("a"))
	if m.configConfirming || cmd == nil {
		t.Errorf("restoring an archived project should fire directly: confirming=%v cmd=%v", m.configConfirming, cmd != nil)
	}
}

// Editing the settings and saving a changed field fires the update; the modal closes.
func TestConfigEditSubmit(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Old", Visibility: "PUBLIC"})
	m, _ = m.Update(press("e"))
	if !m.configEditing {
		t.Fatal("pressing e should open the edit form")
	}
	m, cmd := m.Update(configEditSubmittedMsg{v: configEditValues{title: "New", description: "", visibility: "PUBLIC"}})
	if m.configEditing {
		t.Error("submitting should close the edit form")
	}
	if cmd == nil {
		t.Error("a changed title should fire the update")
	}
}

// The edit form renders its multi-line description (a textarea) and the visibility toggle.
func TestConfigEditFormRenders(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "P", Description: "line one\nline two", Visibility: "PRIVATE"})
	m, _ = m.Update(press("e"))
	if !m.configEditing {
		t.Fatal("pressing e should open the edit form")
	}
	out := plain(m.View())
	for _, want := range []string{"Edit project", "Description", "line one", "line two", "Public", "Private"} {
		if !strings.Contains(out, want) {
			t.Errorf("edit form missing %q:\n%s", want, out)
		}
	}
}

// Archiving an active project asks for confirmation; accepting fires the archive, cancelling closes it.
func TestConfigArchiveConfirmFlow(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "P", Archived: false})
	m, _ = m.Update(press("a"))
	if !m.configConfirming {
		t.Fatal("archiving an active project should ask for confirmation")
	}
	if !m.CapturingInput() {
		t.Error("an open archive confirmation should capture input")
	}
	accepted, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if accepted.configConfirming || cmd == nil {
		t.Errorf("accepting should close the dialog and fire the archive: confirming=%v cmd=%v", accepted.configConfirming, cmd != nil)
	}
	cancelled, cmd := m.Update(widgets.ConfirmCancelledMsg{})
	if cancelled.configConfirming || cmd != nil {
		t.Errorf("cancelling should close the dialog without a command: confirming=%v cmd=%v", cancelled.configConfirming, cmd != nil)
	}
}

// Restoring an archived project is a direct, unconfirmed action (it is reversible and safe).
func TestConfigUnarchiveDirect(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "P", Archived: true})
	m, cmd := m.Update(press("a"))
	if m.configConfirming {
		t.Error("restoring should not ask for confirmation")
	}
	if cmd == nil {
		t.Error("restoring should fire the unarchive directly")
	}
}

// A successful action reloads the settings so the panel reflects the change.
func TestConfigActionDoneReloads(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "P"})
	gen := m.configReqGen
	m, cmd := m.onConfigActionDone(configActionDoneMsg{action: "edit"})
	if !m.configRequested || m.configReqGen != gen+1 || cmd == nil {
		t.Errorf("a successful action should reload the settings: requested=%v gen=%d->%d cmd=%v",
			m.configRequested, gen, m.configReqGen, cmd != nil)
	}
}

// A failed post-action refresh keeps the settings already on screen rather than wiping them with an
// error alongside the action's success toast.
func TestConfigReloadFailureKeepsSettings(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Kept"})
	m, cmd := m.Update(configLoadedMsg{key: m.projectKey, gen: m.configReqGen, err: true})
	if m.configErr {
		t.Error("a failed refresh with settings in hand should not raise the error state")
	}
	if m.project.Title != "Kept" {
		t.Error("a failed refresh should keep the shown settings")
	}
	if cmd == nil {
		t.Error("a failed refresh should still note the hiccup with a toast")
	}
}

// A reload re-clamps the settings-panel scroll, so a body that shrank (e.g. a cleared description) does
// not leave a stale offset that eats the first scroll key.
func TestConfigReloadClampsScroll(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "P"})
	m.configDetailScroll = 20 // beyond any real offset for this short body
	m, _ = m.Update(configLoadedMsg{key: m.projectKey, gen: m.configReqGen, project: domain.Project{Key: "PROJ", Title: "P"}})
	if m.configDetailScroll != 0 {
		t.Errorf("a reload should clamp the scroll back into range, got %d", m.configDetailScroll)
	}
}

// A landed edit updates the drill-in header title to match.
func TestConfigLoadedSyncsTitle(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Old"})
	m, _ = m.Update(configLoadedMsg{key: m.projectKey, gen: m.configReqGen, project: domain.Project{Key: "PROJ", Title: "Renamed"}})
	if m.title != "Renamed" {
		t.Errorf("the header title should follow an edited project title, got %q", m.title)
	}
}

func TestDiffConfigEdit(t *testing.T) {
	orig := domain.Project{Title: "Old", Description: "desc", Visibility: "PUBLIC"}
	if !diffConfigEdit(orig, configEditValues{title: "Old", description: "desc", visibility: "PUBLIC"}).Empty() {
		t.Error("an unchanged form should produce an empty edit")
	}
	if e := diffConfigEdit(orig, configEditValues{title: "New", description: "desc", visibility: "PUBLIC"}); e.Title == nil || *e.Title != "New" {
		t.Error("a changed title should be included")
	}
	if e := diffConfigEdit(orig, configEditValues{title: "Old", description: "", visibility: "PUBLIC"}); !e.ClearDescription {
		t.Error("clearing the description should send an explicit clear")
	}
	if e := diffConfigEdit(orig, configEditValues{title: "Old", description: "desc", visibility: "PRIVATE"}); e.Visibility == nil || *e.Visibility != "PRIVATE" {
		t.Error("a changed visibility should be included")
	}
}

// The visibility toggle flips between the two values, and the edit form rejects an out-of-range title.
func TestConfigEditFormBehaviour(t *testing.T) {
	f := newConfigEditForm(testDeps(), domain.Project{Title: "Ok", Visibility: "PUBLIC"})
	if f.toggleVisibility().visibility != "PRIVATE" {
		t.Error("toggling from PUBLIC should give PRIVATE")
	}
	// a too-short title fails validation: the form flags the error and refocuses Title (rather than
	// emitting a submit)
	f.title.SetValue("a")
	f, _ = f.submit()
	if f.titleErr == "" {
		t.Error("a too-short title should set the title error")
	}
	if f.focus != cefTitle {
		t.Error("a rejected submit should refocus the Title field")
	}
}

func TestConfigActionErrorText(t *testing.T) {
	cases := []struct {
		action string
		status int
		code   string
		reason string
		want   string
	}{
		{"edit", 0, "", "", "Couldn't reach the server - check your connection and try again."},
		{"edit", http.StatusForbidden, "", "", "You need the Manager role for that."},
		{"archive", http.StatusConflict, "", "", "The project is already archived."},
		{"unarchive", http.StatusConflict, "", "", "The project is not archived."},
		{"edit", http.StatusBadRequest, "", "", "Couldn't update the project."},
		// a server reason on the long tail is shown verbatim instead of the generic line
		{"edit", http.StatusBadRequest, "", "Title must not be blank.", "Title must not be blank."},
		// a friendly override still wins over the reason for the well-known statuses
		{"edit", http.StatusForbidden, "", "Requires project manager role", "You need the Manager role for that."},
		// a leaky code is mapped to friendlier copy (non-member editing an archived-project's settings)
		{"edit", http.StatusNotFound, "PROJECT_MEMBER_NOT_FOUND", "Project member not found", "You're not a member of this project."},
	}
	for _, c := range cases {
		if got := configActionErrorText(c.action, c.status, c.code, c.reason); got != c.want {
			t.Errorf("configActionErrorText(%q, %d, %q, %q) = %q, want %q", c.action, c.status, c.code, c.reason, got, c.want)
		}
	}
}

// The Config footer offers edit and the archive/restore toggle labelled for the current state.
func TestConfigHelpKeys(t *testing.T) {
	active := configTabModel(t, domain.Project{Key: "PROJ", Archived: false})
	if got := helpDesc(active, "a"); got != "archive" {
		t.Errorf("an active project should offer 'archive', got %q", got)
	}
	archived := configTabModel(t, domain.Project{Key: "PROJ", Archived: true})
	if got := helpDesc(archived, "a"); got != "restore" {
		t.Errorf("an archived project should offer 'restore', got %q", got)
	}
}

func helpDesc(m Model, keyName string) string {
	for _, b := range m.HelpKeys() {
		if b.Help().Key == keyName {
			return b.Help().Desc
		}
	}
	return ""
}

// guard: configView renders without panicking while the settings are still loading.
func TestConfigLoadingView(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	m.tab = tabConfig
	m.configRequested = true
	if out := plain(m.View()); !strings.Contains(out, "Loading") {
		t.Errorf("a loading Config tab should show a loading note:\n%s", out)
	}
}

// configTabModelAt is configTabModel at a chosen terminal size, for exercising the responsive layout.
func configTabModelAt(t *testing.T, w, h int, p domain.Project) Model {
	t.Helper()
	m := loaded(t, w, h, domain.IssuePage{})
	m.tab = tabConfig
	m.configRequested = true
	m.configLoaded = true
	m.project = p
	m.title = p.Title
	return m
}

// On a wide terminal the project fields and the GitHub section sit side by side, so a line carries both.
func TestConfigTwoColumnLayout(t *testing.T) {
	m := configTabModelAt(t, 160, 40, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true, WebhookURL: "https://ex.co/webhook"}

	if line := lineWithBoth(plain(m.View()), "Project", "GitHub"); line == "" {
		t.Errorf("the Project and GitHub columns should share their header line:\n%s", plain(m.View()))
	}
}

// Too narrow to split, the columns stack: the same two labels then land on different lines.
func TestConfigStacksWhenNarrow(t *testing.T) {
	m := configTabModelAt(t, 80, 40, domain.Project{Key: "PROJ", Title: "Proj"}) // the narrowest terminal the screen renders at
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true, WebhookURL: "https://ex.co/webhook"}

	out := plain(m.View())
	if line := lineWithBoth(out, "Project", "GitHub"); line != "" {
		t.Errorf("a narrow terminal should stack the columns, got a shared line %q", line)
	}
	for _, want := range []string{"Project", "Visibility", "GitHub"} {
		if !strings.Contains(out, want) {
			t.Errorf("stacking must not drop %q:\n%s", want, out)
		}
	}
}

// lineWithBoth returns the first line containing both needles, or "" when none does.
func lineWithBoth(body, a, b string) string {
	for _, ln := range strings.Split(body, "\n") {
		if strings.Contains(ln, a) && strings.Contains(ln, b) {
			return ln
		}
	}
	return ""
}

// connectedConfigModel is a Config tab with GitHub connected, for the delivery-log cases.
func connectedConfigModel(t *testing.T) Model {
	t.Helper()
	m := configTabModelAt(t, 160, 40, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true, WebhookURL: "https://ex.co/webhook"}
	return m
}

// Each delivery shows when it arrived, its event type, an outcome, and the reason - the reason being the
// point, since a skipped delivery is where "my push did nothing" gets explained.
func TestConfigDeliveriesRender(t *testing.T) {
	m := connectedConfigModel(t)
	m.deliveriesLoaded = true
	m.deliveries = []domain.WebhookDelivery{
		{EventType: "push", Status: "PROCESSED", Detail: "Linked branch feature/x to PROJ-12", ReceivedAt: time.Now()},
		{EventType: "push", Status: "IGNORED", Detail: "No issue key found in: refs/heads/fix-login", ReceivedAt: time.Now()},
	}
	out := plain(m.View())
	for _, want := range []string{
		"Recent deliveries", "push", "applied", "Linked branch feature/x to PROJ-12",
		"skipped", "No issue key found in: refs/heads/fix-login",
	} {
		if !strings.Contains(out, want) {
			t.Errorf("delivery log missing %q:\n%s", want, out)
		}
	}
}

// A failing delivery says it is still being retried and how many attempts it has cost; one past its budget
// reads as given up, so the two are not confused.
func TestConfigDeliveriesRetryAndDead(t *testing.T) {
	m := connectedConfigModel(t)
	m.deliveriesLoaded = true
	m.deliveries = []domain.WebhookDelivery{
		{EventType: "push", Status: "FAILED", AttemptCount: 2, Detail: "connection refused", ReceivedAt: time.Now()},
		{EventType: "push", Status: "DEAD", AttemptCount: 5, Detail: "connection refused", ReceivedAt: time.Now()},
	}
	out := plain(m.View())
	for _, want := range []string{"retrying (2)", "gave up"} {
		if !strings.Contains(out, want) {
			t.Errorf("delivery log missing %q:\n%s", want, out)
		}
	}
}

// A member who may not read the log is told so, rather than being shown an empty list that would read as
// "nothing ever arrived".
func TestConfigDeliveriesUnreadable(t *testing.T) {
	m := connectedConfigModel(t)
	m.deliveriesLoaded = false
	out := plain(m.View())
	if !strings.Contains(out, "Only a project manager") {
		t.Errorf("an unreadable log should say why:\n%s", out)
	}
	if strings.Contains(out, "Nothing received yet") {
		t.Error("an unreadable log must not claim nothing arrived")
	}
}

// A connected integration that has genuinely received nothing says so.
func TestConfigDeliveriesEmpty(t *testing.T) {
	m := connectedConfigModel(t)
	m.deliveriesLoaded = true
	m.deliveries = nil
	if out := plain(m.View()); !strings.Contains(out, "Nothing received yet") {
		t.Errorf("an empty log should say so:\n%s", out)
	}
}

// With nothing connected there is no log to speak of, so the section stays away entirely.
func TestConfigDeliveriesHiddenWhenNotConnected(t *testing.T) {
	m := configTabModelAt(t, 160, 40, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: false}
	if out := plain(m.View()); strings.Contains(out, "Recent deliveries") {
		t.Errorf("a disconnected project should not show a delivery log:\n%s", out)
	}
}

// The Config tab shows a GitHub section: when not connected it invites generating a secret.
func TestConfigGithubNotConnected(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: false}
	out := plain(m.View())
	for _, want := range []string{"GitHub", "Not connected", "generate a webhook secret"} {
		if !strings.Contains(out, want) {
			t.Errorf("GitHub section missing %q:\n%s", want, out)
		}
	}
}

// When connected the section shows the status, sync state and the webhook URL to register in GitHub.
func TestConfigGithubConnected(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true, WebhookURL: "https://ex.co/webhook"}
	out := plain(m.View())
	for _, want := range []string{"GitHub", "Connected", "syncing", "https://ex.co/webhook", "rotate secret", "disconnect"} {
		if !strings.Contains(out, want) {
			t.Errorf("connected GitHub section missing %q:\n%s", want, out)
		}
	}
}

// A generated secret is revealed once (with the connection now shown as connected) and surfaces a toast.
func TestConfigGithubSecretReveal(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m, cmd := m.Update(githubSecretMsg{secret: domain.GithubSecret{WebhookURL: "https://ex.co/webhook", Secret: "s3cr3t-value"}})
	if !m.github.Connected {
		t.Error("a generated secret should mark the integration connected")
	}
	if m.githubSecret.Secret != "s3cr3t-value" {
		t.Errorf("secret not stored for reveal: %q", m.githubSecret.Secret)
	}
	if cmd == nil {
		t.Error("a successful generate should surface a toast")
	}
	out := plain(m.View())
	for _, want := range []string{"shown once", "s3cr3t-value"} {
		if !strings.Contains(out, want) {
			t.Errorf("secret reveal missing %q:\n%s", want, out)
		}
	}
}

// The hint names the action the key will perform, so a paused integration offers to resume rather than
// repeating the state already shown above it.
func TestConfigGithubSyncHintFollowsState(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true

	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true}
	if out := plain(m.View()); !strings.Contains(out, "s: pause sync") {
		t.Errorf("a syncing integration should offer to pause:\n%s", out)
	}

	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: false}
	out := plain(m.View())
	for _, want := range []string{"paused", "s: resume sync"} {
		if !strings.Contains(out, want) {
			t.Errorf("a paused integration should show %q:\n%s", want, out)
		}
	}
}

// Pressing s fires the toggle; the request flips whatever the current state is.
func TestConfigGithubSyncToggleKey(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true}
	if _, cmd := m.Update(press("s")); cmd == nil {
		t.Error("pressing s should fire the sync toggle command")
	}
}

// Toggling when GitHub is not connected reports that instead of calling the server.
func TestConfigGithubSyncToggleNotConnected(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: false}
	m2, cmd := m.Update(press("s"))
	if cmd == nil {
		t.Error("the press should still surface an informational toast")
	}
	if m2.github.Connected {
		t.Error("a toggle with nothing connected must not mark the integration connected")
	}
}

// The server's new status is applied without reloading, so a secret revealed earlier in the same visit
// stays on screen.
func TestConfigGithubSyncKeepsRevealedSecret(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m, _ = m.Update(githubSecretMsg{secret: domain.GithubSecret{WebhookURL: "https://ex.co/webhook", Secret: "s3cr3t-value"}})

	m, cmd := m.Update(githubSyncMsg{github: domain.GithubIntegration{
		Connected: true, SyncEnabled: false, WebhookURL: "https://ex.co/webhook",
	}})

	if m.github.SyncEnabled {
		t.Error("the server's paused status should be applied")
	}
	if cmd == nil {
		t.Error("a successful toggle should surface a toast")
	}
	if out := plain(m.View()); !strings.Contains(out, "s3cr3t-value") {
		t.Errorf("the one-time secret should survive a sync toggle:\n%s", out)
	}
}

// A failed toggle leaves the shown status alone rather than flipping it optimistically.
func TestConfigGithubSyncFailureKeepsState(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true}

	m, cmd := m.Update(githubSyncMsg{err: true, status: 403})

	if !m.github.SyncEnabled {
		t.Error("a failed toggle must not change the shown sync state")
	}
	if cmd == nil {
		t.Error("a failed toggle should surface an error toast")
	}
}

// Pressing g on the Config tab fires the (re)generate command.
func TestConfigGithubRegenerateKey(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	_, cmd := m.Update(press("g"))
	if cmd == nil {
		t.Error("pressing g should fire the regenerate-secret command")
	}
}

// Disconnect is confirmed first, then fires the remove; the confirm is flagged as a GitHub action so it
// does not archive the project by mistake.
func TestConfigGithubDisconnectConfirmed(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true}
	m, _ = m.Update(press("x"))
	if !m.configConfirming || !m.githubConfirming {
		t.Fatalf("x should open the disconnect confirmation: confirming=%v github=%v", m.configConfirming, m.githubConfirming)
	}
	m, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if m.configConfirming || m.githubConfirming {
		t.Error("accepting should close the confirmation")
	}
	if cmd == nil {
		t.Error("accepting the disconnect should fire the remove command")
	}
}

// Pressing x when GitHub is not connected does nothing destructive (no confirmation opens).
func TestConfigGithubDisconnectNotConnected(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: false}
	m, _ = m.Update(press("x"))
	if m.configConfirming || m.githubConfirming {
		t.Error("x with no connection should not open a disconnect confirmation")
	}
}

// A revealed one-time secret is hidden once the Config tab is left and reopened, even when the reopen does
// not refetch (the settings were already loaded) — the secret must not linger on screen for the session.
func TestConfigGithubSecretClearedOnReopen(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m, _ = m.Update(githubSecretMsg{secret: domain.GithubSecret{WebhookURL: "https://ex.co/webhook", Secret: "s3cr3t-value"}})
	if !strings.Contains(plain(m.View()), "s3cr3t-value") {
		t.Fatal("precondition: the secret should be revealed right after generating")
	}
	m, _ = m.Update(press("1")) // leave for the Issues tab
	m, _ = m.Update(press("5")) // reopen Config (already loaded: no refetch)
	if m.githubSecret.Secret != "" {
		t.Errorf("reopening Config should clear the one-time secret, still have %q", m.githubSecret.Secret)
	}
	if strings.Contains(plain(m.View()), "s3cr3t-value") {
		t.Errorf("the one-time secret must not linger after reopening the tab:\n%s", plain(m.View()))
	}
}

// Rotating the secret of an integration whose sync is paused must not mislabel it as syncing.
func TestConfigGithubRotatePreservesPausedSync(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: false, WebhookURL: "https://ex.co/webhook"}
	m, _ = m.Update(githubSecretMsg{secret: domain.GithubSecret{WebhookURL: "https://ex.co/webhook", Secret: "rotated"}})
	if m.github.SyncEnabled {
		t.Error("rotating a paused integration's secret should keep it paused, not flip it to syncing")
	}
}

// While a one-time secret is on screen the reveal advertises the copy key; pressing it copies the value
// and the hint turns into a confirmation.
func TestConfigGithubSecretCopy(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m, _ = m.Update(githubSecretMsg{secret: domain.GithubSecret{WebhookURL: "https://ex.co/webhook", Secret: "s3cr3t-value"}})
	if out := plain(m.View()); !strings.Contains(out, "y: copy secret") {
		t.Errorf("a revealed secret should advertise the copy key:\n%s", out)
	}

	m, cmd := m.Update(press("y"))
	if !m.githubSecretCopied {
		t.Error("pressing y should mark the secret copied")
	}
	if cmd == nil {
		t.Error("pressing y should emit a clipboard command")
	}
	out := plain(m.View())
	if !strings.Contains(out, "copied") {
		t.Errorf("a copied secret should confirm it:\n%s", out)
	}
	if !strings.Contains(out, "y: copy secret") {
		t.Errorf("the copy key should stay visible so the value can be copied again:\n%s", out)
	}
}

// The webhook URL and the secret are pasted into the same GitHub form, so each carries its own copy key.
// The URL's works for as long as the integration exists, not only during a reveal.
func TestConfigGithubURLCopy(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true, WebhookURL: "https://ex.co/webhook"}
	if out := plain(m.View()); !strings.Contains(out, "u: copy URL") {
		t.Errorf("a connected integration should advertise the URL copy key:\n%s", out)
	}
	if got := helpDesc(m, "u"); got != "copy URL" {
		t.Errorf("the URL copy key should be in help, got %q", got)
	}

	m, cmd := m.Update(press("u"))
	if !m.githubURLCopied {
		t.Error("pressing u should mark the URL copied")
	}
	if cmd == nil {
		t.Error("pressing u should emit a clipboard command")
	}
	if out := plain(m.View()); !strings.Contains(out, "copied") {
		t.Errorf("a copied URL should confirm it:\n%s", out)
	}
}

// With no integration there is no URL to copy, so the key is neither advertised nor acted on.
func TestConfigURLCopyKeyNeedsIntegration(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: false}
	if got := helpDesc(m, "u"); got != "" {
		t.Errorf("no integration means no URL copy key, got %q", got)
	}
	m, cmd := m.Update(press("u"))
	if cmd != nil || m.githubURLCopied {
		t.Error("u should do nothing with no webhook URL")
	}
}

// The copy key is only meaningful while a secret is revealed, so it is neither advertised nor acted on
// otherwise - a key that silently did nothing would read as a failed copy.
func TestConfigCopyKeyOnlyWhileRevealed(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m.github = domain.GithubIntegration{Connected: true, SyncEnabled: true, WebhookURL: "https://ex.co/webhook"}
	if got := helpDesc(m, "y"); got != "" {
		t.Errorf("no secret is revealed, so no copy key should be offered, got %q", got)
	}
	m, cmd := m.Update(press("y"))
	if cmd != nil {
		t.Error("y should do nothing with no secret on screen")
	}
	if m.githubSecretCopied {
		t.Error("nothing was copied")
	}

	m, _ = m.Update(githubSecretMsg{secret: domain.GithubSecret{WebhookURL: "https://ex.co/webhook", Secret: "s3cr3t-value"}})
	if got := helpDesc(m, "y"); got != "copy secret" {
		t.Errorf("a revealed secret should offer the copy key in help, got %q", got)
	}
}

// Leaving and re-entering the tab drops the reveal, so the copied confirmation must not outlive it and
// suggest a value that is no longer on screen is still to hand.
func TestConfigCopiedResetsWithReveal(t *testing.T) {
	m := configTabModel(t, domain.Project{Key: "PROJ", Title: "Proj"})
	m.githubLoaded = true
	m, _ = m.Update(githubSecretMsg{secret: domain.GithubSecret{Secret: "s3cr3t-value"}})
	m, _ = m.Update(press("y"))

	m.tab = tabIssues
	m, _ = m.switchTab(press("5"))
	if m.githubSecret.Secret != "" || m.githubSecretCopied {
		t.Error("re-entering Config should drop both the reveal and its copied flag")
	}
}
