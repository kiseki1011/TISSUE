// Package ui is the BubbleTea root model routing between screens, with shared theme, keys, and components.
package ui

import (
	"context"
	"log/slog"
	"net/http"
	"strconv"
	"strings"
	"time"

	"charm.land/bubbles/v2/help"
	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/realtime"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/agents"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/connecting"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/home"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/inbox"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/login"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/oidcdevice"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/project"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/schema"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

type screen int

const (
	screenConnecting screen = iota
	screenLogin
	screenOidcDevice
	screenHome
	screenSchema
	screenAgents
	screenInbox
	screenProject // a drill-in from the Projects tab, not a top-level tab
)

// tabDef is one top-level tab shown in the header once authenticated.
type tabDef struct {
	screen screen
	label  string
	zone   string
}

// tabs is the header tab bar order: the project dashboard plus the global schema catalog.
var tabs = []tabDef{
	{screenHome, "Projects", "app.tab.projects"},
	{screenSchema, "Schema", "app.tab.schema"},
	{screenAgents, "Agents", "app.tab.agents"},
	{screenInbox, "Inbox", "app.tab.inbox"},
}

// Chrome band sizes.
// Each band sits one blank row away from the terminal edge and one from the content area.
const (
	padRows      = 1
	headerHeight = 1
	footerHeight = 2 // the footer band's minimum: global(common) keys on top, screen-specific below
	leftInset    = 2
	// headerCompactW is the width at and below which the header sheds its optional affordances (the server
	// url, the username, and the drill-in's "‹ Projects" back link) so the brand and the tabs still fit.
	headerCompactW = 130
)

// App is the root model, owning shared dependencies and routing between screens.
type App struct {
	deps        deps.Deps
	screen      screen
	width       int
	height      int
	mouse       bool   // when true, capture mouse so clicks can move focus
	hoverTab    string // zone of the header tab under the cursor, "" when none
	help        help.Model
	toasts      toast.Model    // bottom-right notification stack, shared across screens
	modal       appModal       // app-level overlay (help, ...), nil when none is open
	modalScroll int            // wheel scroll offset when the open modal is taller than the terminal
	user        domain.Profile // authenticated member, populated after login
	sessionGen  int            // bumped on login/logout so a stale in-flight profile fetch is ignored

	rt      *realtime.Consumer // user-scoped SSE stream, live only while authenticated
	rtState realtime.State     // last connection state, drives the header indicator

	inboxUnread bool // whether the caller has unread notifications, driving the Inbox tab's badge

	// sentSize is the body size last handed to the active screen. The footer's wrapped height is part of
	// the row budget, so a hint row coming or going has to re-issue it even without a terminal resize.
	sentSize tea.WindowSizeMsg

	// projectOrigin is the tab to return to when the project drill-in closes (esc). It is the screen the
	// OpenProjectMsg was emitted from, so drilling in from the Inbox returns to the Inbox, and from the
	// Projects dashboard returns there. Defaults to the dashboard (the silent-restore deep-link path).
	projectOrigin screen

	connecting connecting.Model
	login      login.Model
	oidcDevice oidcdevice.Model
	home       home.Model
	schema     schema.Model
	agents     agents.Model
	inbox      inbox.Model
	project    project.Model
}

// newHelpModel builds the footer/help-modal renderer styled to the theme. It is rebuilt on a live
// theme change, so the short (footer) and full (help modal) views both restyle.
func newHelpModel(t theme.Theme) help.Model {
	h := help.New()
	keyS := lipgloss.NewStyle().Foreground(t.Primary)
	descS := lipgloss.NewStyle().Foreground(t.Muted)
	sepS := lipgloss.NewStyle().Foreground(t.Border)
	h.Styles.ShortKey, h.Styles.FullKey = keyS, keyS
	h.Styles.ShortDesc, h.Styles.FullDesc = descS, descS
	h.Styles.ShortSeparator, h.Styles.FullSeparator = sepS, sepS
	h.Styles.Ellipsis = sepS
	return h
}

func New(d deps.Deps) App {
	t := d.Styles.Theme
	return App{
		deps:          d,
		screen:        screenConnecting,
		connecting:    connecting.New(d),
		mouse:         d.Mouse,
		help:          newHelpModel(t),
		toasts:        toast.New(t, d.Glyphs),
		projectOrigin: screenHome,
	}
}

func (a App) Init() tea.Cmd {
	return a.connecting.Init()
}

// appModal is an app-level overlay (help now, settings later) shown centered over a dimmed backdrop.
// While one is open it owns the keyboard and its own mouse. The host dismisses it on a click outside
// its box or a modalClosedMsg the modal emits (such as on esc).
type appModal interface {
	Update(tea.Msg) (appModal, tea.Cmd)
	View() string
	HelpKeys() []key.Binding
}

// describer is optionally implemented by a screen to introduce itself in the help modal.
type describer interface {
	HelpTitle() string
	HelpAbout() string
}

type modalClosedMsg struct{}

// closeModal is the command a modal returns to ask the host to dismiss it.
func closeModal() tea.Msg { return modalClosedMsg{} }

// Update runs the message through the app, then reconciles the layout: the footer's height depends on
// which hints the new state advertises, so a message that changes them also changes the body's budget.
func (a App) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	return reflow(a.update(msg))
}

// reflow re-issues the body size when the chrome's height no longer matches what the active screen was
// last told. Screens are sized by message, so a footer that grew a wrapped row has to hand the rows back.
// It settles in one pass because no screen's hints depend on the height it was granted; a hint gated on
// the body overflowing would make the two chase each other (a one-row flicker, since this is single-shot).
func reflow(model tea.Model, cmd tea.Cmd) (tea.Model, tea.Cmd) {
	a, ok := model.(App)
	if !ok || a.width == 0 || a.bodySize() == a.sentSize {
		return model, cmd
	}
	next, resized := a.resize()
	return next, tea.Batch(cmd, resized)
}

// bodySize is the room left for the active screen once the chrome bands are taken out.
func (a App) bodySize() tea.WindowSizeMsg {
	return tea.WindowSizeMsg{Width: a.width, Height: a.height - a.reservedRows()}
}

// resize hands the current body size to the active screen and records it. Every path that sizes a screen
// goes through here so reflow always compares against what was really sent.
func (a App) resize() (tea.Model, tea.Cmd) {
	a.sentSize = a.bodySize()
	return a.updateActive(a.sentSize)
}

func (a App) update(msg tea.Msg) (tea.Model, tea.Cmd) {
	// a visible toast swallows mouse input landing on it, so the floating notification is opaque to
	// clicks/wheel rather than click-through to the widget it covers
	if mm, ok := msg.(tea.MouseMsg); ok && a.mouseOnToast(mm) {
		return a, nil
	}
	// ctrl+c always quits, even while a modal owns the keyboard below
	if k, ok := msg.(tea.KeyPressMsg); ok && k.String() == "ctrl+c" {
		return a, tea.Quit
	}
	// an open modal owns interactive input. Background messages (resize, loads, toasts) still flow
	// through to the normal handling below so the inert screen stays current
	if a.modal != nil {
		switch msg.(type) {
		case tea.KeyPressMsg, tea.MouseClickMsg, tea.MouseWheelMsg, tea.MouseMotionMsg:
			return a.updateModal(msg)
		}
	}
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		switch msg.String() {
		case "?":
			if a.optionsNavActive() {
				return a.openHelp()
			}
		case ",":
			if a.optionsNavActive() {
				return a.openOptions()
			}
		case "ctrl+l":
			if a.tabNavActive() {
				return a.switchTab(a.stepTab(1))
			}
		case "ctrl+h":
			if a.tabNavActive() {
				return a.switchTab(a.stepTab(-1))
			}
		case "1", "2", "3", "4":
			if a.tabNavActive() {
				if i := int(msg.String()[0] - '1'); i < len(tabs) {
					return a.switchTab(tabs[i].screen)
				}
			}
		}
	case tea.WindowSizeMsg:
		a.width, a.height = msg.Width, msg.Height
		a.help.SetWidth(msg.Width)
		if a.modal != nil {
			a.modal, _ = a.modal.Update(msg) // let the modal re-size its own viewport
			a = a.scrollModalBy(0)           // re-clamp the window offset to the new height
		}
		return a.resize()
	case modalClosedMsg:
		a.modal, a.modalScroll = nil, 0
		return a, nil
	case themeSelectedMsg:
		return a.applyTheme(msg.name)
	case iconsSelectedMsg:
		return a.applyIcons(msg.mode)
	case mouseSelectedMsg:
		return a.applyMouse(msg.on)
	case realtimeUpdateMsg:
		return a.applyRealtime(msg.u)
	case logoutMsg:
		// revoke + clear tokens in the background. The probe runs once that completes (loggedOutMsg)
		a.modal, a.modalScroll = nil, 0
		a.user = domain.Profile{}
		a.sessionGen++
		a.stopRealtime() // drop the SSE stream so a logged-out session streams nothing
		a.screen = screenConnecting
		a.connecting = connecting.New(a.deps)
		return a, logoutCmd(a.deps)
	case loggedOutMsg:
		// tokens are revoked server-side and cleared locally. The re-probe now routes to login
		return a.withSize(a.connecting.Init())
	case tea.MouseClickMsg:
		// a header-tab click switches tabs only when tab-nav is live. While a modal captures input
		// (for example a delete is submitting) the click yields to the screen, so a mid-action tab switch
		// cannot strand the in-flight result message on the wrong screen
		if a.tabNavActive() {
			if target, ok := a.tabAt(msg); ok {
				return a.switchTab(target)
			}
		}
		return a.updateActive(msg)
	case tea.MouseMotionMsg:
		// track the hovered header tab for its highlight, then let the active screen hover too
		a.hoverTab = ""
		if a.tabNavActive() {
			for _, tb := range tabs {
				if zone.Get(tb.zone).InBounds(msg) {
					a.hoverTab = tb.zone
					break
				}
			}
		}
		return a.updateActive(msg)
	case nav.GoToLoginMsg:
		a.screen = screenLogin
		a.login = login.New(a.deps, msg.Info)
		return a.withSize(a.login.Init())
	case nav.GoToOidcDeviceMsg:
		a.screen = screenOidcDevice
		a.oidcDevice = oidcdevice.New(a.deps, msg.Info)
		return a.withSize(a.oidcDevice.Init())
	case nav.OpenProjectMsg:
		// drill into a project's issues. The origin tab (Projects dashboard, or the Inbox when drilling from
		// a notification) is remembered so esc returns there; its state is preserved underneath
		a.projectOrigin = a.screen
		a.screen = screenProject
		a.project = project.New(a.deps, msg.ProjectKey, msg.Title).
			WithViewer(a.user.Username).WithInitialFocus(msg.IssueKey, msg.SprintID)
		a.rememberLastProject(msg.ProjectKey) // so the next silent restore lands back here
		return a.withSize(a.project.Init())
	case nav.CloseProjectMsg:
		// return to the origin tab (dashboard or Inbox) and re-lay it to the current size. The origin is now
		// the last screen, so forget the project pointer
		a.clearLastProject()
		return a.switchTab(a.projectOrigin)
	case nav.RefreshDashboardMsg:
		// a project mutation that changes how the project reads on the dashboard (settings edit) asks the
		// dashboard to silently reload, so the change shows when the user returns to it
		var cmd tea.Cmd
		a.home, cmd = a.home.Update(home.RefreshMsg{})
		return a, cmd
	case nav.GoToHomeMsg:
		a.screen = screenHome
		a.sessionGen++            // a fresh session, ignore any prior login's in-flight profile fetch
		a.user = domain.Profile{} // start blank so the new session never inherits the prior identity
		a.home = home.New(a.deps, msg.Info, msg.Welcome)
		a.schema = schema.New(a.deps)
		a.agents = agents.New(a.deps)
		a.inbox = inbox.New(a.deps)
		a.inboxUnread = false
		rtCmd := a.startRealtime() // open the user-scoped SSE stream for this session
		// prefetch the Schema catalog, the agents list, and the inbox so those tabs are ready when opened;
		// home loads its projects list in the background so it is ready even when we deep-link past it. The
		// unread poll starts its self-arming loop so the Inbox badge lights even before the tab is opened.
		inits := []tea.Cmd{
			rtCmd, fetchProfile(a.deps, a.sessionGen), a.home.Init(), a.schema.Init(), a.agents.Init(),
			a.inbox.Init(), pollInboxUnread(a.deps, a.sessionGen, true),
		}
		if msg.Restore {
			// a silent restore lands back on the last-open project without flashing the dashboard: set
			// the project active up front so the first render is already the project. Home stays
			// initialized underneath, so esc returns to a ready list
			if key := a.lastProjectKey(); key != "" {
				a.screen = screenProject
				a.project = project.New(a.deps, key, "").WithViewer(a.user.Username)
				inits = append(inits, a.project.Init())
			}
		} else {
			// a fresh login lands on the dashboard and forgets the pointer, so a different user signing in
			// on the same server does not inherit the previous user's last project
			a.clearLastProject()
		}
		// size whichever screen is now active (home, or the restored project). When we deep-linked, home
		// is sized on esc via CloseProjectMsg, so it needs no sizing here
		return a.withSize(tea.Batch(inits...))
	case profileLoadedMsg:
		if msg.gen == a.sessionGen { // drop a profile that belongs to a superseded session
			a.user = msg.profile
			a.home = a.home.WithAdmin(msg.profile.IsAdmin()) // a system admin may self-join a PRIVATE project
			// a silent restore builds the project screen before the profile lands, so hand it over now too
			a.project = a.project.WithViewer(msg.profile.Username)
		}
		return a, nil
	case schema.LoadedMsg:
		// route the background catalog load to the Schema screen even while another tab is active
		var cmd tea.Cmd
		a.schema, cmd = a.schema.Update(msg)
		return a, cmd
	case schema.TypeDetailLoadedMsg:
		var cmd tea.Cmd
		a.schema, cmd = a.schema.Update(msg)
		return a, cmd
	case schema.WorkflowDetailLoadedMsg:
		var cmd tea.Cmd
		a.schema, cmd = a.schema.Update(msg)
		return a, cmd
	case agents.AgentsLoadedMsg, agents.TokensLoadedMsg, agents.ModelsLoadedMsg:
		// route the background agents prefetch to the Agents screen even while another tab is active
		var cmd tea.Cmd
		a.agents, cmd = a.agents.Update(msg)
		return a, cmd
	case inbox.NotificationsLoadedMsg:
		// route the background inbox prefetch (and any refresh/append landing while another tab is active)
		var cmd tea.Cmd
		a.inbox, cmd = a.inbox.Update(msg)
		return a, cmd
	case inbox.ReadChangedMsg:
		// a read action in the inbox changed unread state: re-check the badge authoritatively (one-shot, so
		// it does not spawn a second poll loop)
		return a, pollInboxUnread(a.deps, a.sessionGen, false)
	case inbox.MarkedAllMsg, inbox.ActionFailedMsg:
		// deliver a mark-all/action-failed result to the inbox even if the user left the tab before it
		// landed, so its toast and badge re-check (and the failed-action reconcile) still fire
		var cmd tea.Cmd
		a.inbox, cmd = a.inbox.Update(msg)
		return a, cmd
	case inboxUnreadMsg:
		if msg.gen != a.sessionGen {
			return a, nil // a re-login/logout superseded this poll; end its loop
		}
		if msg.ok {
			a.inboxUnread = msg.has
		}
		if msg.reArm {
			return a, tea.Tick(inboxPollInterval, func(time.Time) tea.Msg { return inboxRepollMsg{gen: msg.gen} })
		}
		return a, nil
	case inboxRepollMsg:
		if msg.gen != a.sessionGen {
			return a, nil // stale session; stop polling
		}
		return a, pollInboxUnread(a.deps, msg.gen, true)
	case home.ProjectsLoadedMsg, home.ProjectsErrMsg, home.StatsLoadedMsg, home.StatsErrMsg:
		// home's projects-list and stats prefetch run in the background. Deliver their results to home
		// even while the user has drilled into a project - on a silent restore we deep-link straight past
		// the dashboard, so its own list load would otherwise land on the project screen and be dropped,
		// stranding the dashboard on "Projects (loading)" (and the stats pending state on Details)
		var cmd tea.Cmd
		a.home, cmd = a.home.Update(msg)
		return a, cmd
	case project.IssueDetailLoadedMsg, project.TransitionDoneMsg, project.AssignDoneMsg, project.ReviewerDoneMsg, project.ReviewDoneMsg, project.ParentEditDoneMsg, project.EditDoneMsg, project.CommentDoneMsg, project.CommentEditDoneMsg, project.IssueDeletedMsg, project.IssueCreatedMsg:
		// deliver a project detail/transition/assign/reviewer/review/parent/edit/comment/delete result to the
		// (preserved) project model even if the user left the drill-in before it landed, so the toast still fires
		var cmd tea.Cmd
		a.project, cmd = a.project.Update(msg)
		return a, cmd
	case toast.ShowMsg, toast.ExpireMsg:
		// the notification stack is shell-owned. Any screen raises a toast by emitting ShowMsg
		var cmd tea.Cmd
		a.toasts, cmd = a.toasts.Update(msg)
		return a, cmd
	case optionsPositionsLoaded, optionsPositionFailed:
		// the options modal owns async position-picker state. Deliver the result if it is still open
		if a.modal == nil {
			return a, nil
		}
		var cmd tea.Cmd
		a.modal, cmd = a.modal.Update(msg)
		return a, cmd
	case optionsPositionSet:
		// let the modal reflect the change, and refresh the cached profile so a reopen shows it too.
		// If the modal has already closed (for example a logout raced the result), do nothing — refreshing
		// would repopulate the just-cleared profile.
		if a.modal == nil {
			return a, nil
		}
		var cmd tea.Cmd
		a.modal, cmd = a.modal.Update(msg)
		return a, tea.Batch(cmd, fetchProfile(a.deps, a.sessionGen))
	}

	return a.updateActive(msg)
}

func (a App) updateActive(msg tea.Msg) (tea.Model, tea.Cmd) {
	var cmd tea.Cmd
	switch a.screen {
	case screenConnecting:
		a.connecting, cmd = a.connecting.Update(msg)
	case screenLogin:
		a.login, cmd = a.login.Update(msg)
	case screenOidcDevice:
		a.oidcDevice, cmd = a.oidcDevice.Update(msg)
	case screenHome:
		a.home, cmd = a.home.Update(msg)
	case screenSchema:
		a.schema, cmd = a.schema.Update(msg)
	case screenAgents:
		a.agents, cmd = a.agents.Update(msg)
	case screenInbox:
		a.inbox, cmd = a.inbox.Update(msg)
	case screenProject:
		a.project, cmd = a.project.Update(msg)
	}
	return a, cmd
}

func (a App) isTabScreen(s screen) bool {
	for _, t := range tabs {
		if t.screen == s {
			return true
		}
	}
	return false
}

// hasChrome reports whether the screen wears the app header/footer bands. The top-level tabs do, and
// so does the project drill-in, which supplies its own header-right tabs via HeaderInfo.
func (a App) hasChrome(s screen) bool {
	return a.isTabScreen(s) || s == screenProject
}

// headerInfoer is implemented by a screen that draws its own header-right content (its own tab bar)
// in place of the app's top-level tabs. compact asks it to drop optional affordances (its back link) on
// a narrow header, matching the app's own hiding of the username.
type headerInfoer interface {
	HeaderInfo(compact bool) string
}

func (a App) stepTab(delta int) screen {
	idx := 0
	for i, t := range tabs {
		if t.screen == a.screen {
			idx = i
			break
		}
	}
	n := len(tabs)
	return tabs[(idx+delta+n)%n].screen
}

func (a App) tabAt(msg tea.MouseClickMsg) (screen, bool) {
	if msg.Button != tea.MouseLeft {
		return 0, false
	}
	for _, t := range tabs {
		if zone.Get(t.zone).InBounds(msg) {
			return t.screen, true
		}
	}
	return 0, false
}

// switchTab activates target and forwards the current size so it lays out immediately.
func (a App) switchTab(target screen) (tea.Model, tea.Cmd) {
	if a.screen == target {
		return a, nil
	}
	a.screen = target
	if a.width == 0 {
		return a, nil
	}
	model, cmd := a.resize()
	if target == screenInbox {
		// silently re-pull the inbox each time it is entered; notifications are poll-only, so the boot
		// prefetch would otherwise go stale between visits
		app := model.(App)
		var rcmd tea.Cmd
		app.inbox, rcmd = app.inbox.Update(inbox.RefreshMsg{})
		return app, tea.Batch(cmd, rcmd)
	}
	return model, cmd
}

// inputCapturer is implemented by a tab screen that is currently owning the keyboard
// (a focused text field or an open modal). While it captures, global tab-switch keys
// (1/2/3, ctrl+h/l) yield to it so digits type into fields instead of switching tabs.
type inputCapturer interface {
	CapturingInput() bool
}

func (a App) activeCapturingInput() bool {
	if c, ok := a.activeModel().(inputCapturer); ok {
		return c.CapturingInput()
	}
	return false
}

// tabNavActive reports whether the global tab-switch keys should act right now. An open app modal
// owns the keyboard, so tab-nav (and its footer hint) yields to it.
func (a App) tabNavActive() bool {
	return a.isTabScreen(a.screen) && !a.activeCapturingInput() && a.modal == nil
}

// optionsNavActive reports whether the app-level Options/help overlays may open right now. Unlike
// tabNavActive it also covers the project drill-in, which wears the app chrome and can host the same
// overlays; the tab-switch digits stay on tabNavActive so they never collide with the project's own
// 1-4 sub-tabs.
func (a App) optionsNavActive() bool {
	return a.hasChrome(a.screen) && !a.activeCapturingInput() && a.modal == nil
}

// withSize forwards the current window size straight to the activated screen.
// It must not re-emit a WindowSizeMsg. The app's own handler would treat that as a terminal resize
// and overwrite the stored full size.
func (a App) withSize(initCmd tea.Cmd) (tea.Model, tea.Cmd) {
	if a.width == 0 {
		return a, initCmd
	}
	model, cmd := a.resize()
	return model, tea.Batch(initCmd, cmd)
}

func (a App) View() tea.View {
	var bands []string
	if header := a.headerView(); header != "" {
		bands = append(bands, "", header, "")
	}
	bands = append(bands, a.activeView())
	if footer := a.footerView(); footer != "" {
		// clipped to the band's budget, so on a terminal too small to hold every wrapped hint the row
		// that goes is a hint rather than the body or the bottom margin
		bands = append(bands, "", lipgloss.NewStyle().MaxHeight(a.footerRows()).Render(footer), "")
	}
	content := lipgloss.JoinVertical(lipgloss.Left, bands...)
	// never emit more than the terminal
	content = lipgloss.NewStyle().MaxWidth(a.width).MaxHeight(a.height).Render(content)

	// a modal dims the (inert) backdrop and floats centered. It is spliced in BEFORE Scan so its own
	// click zones register, unlike the toasts which sit on top after Scan
	if a.modal != nil {
		content = a.overlayModalDim(content, a.modalView())
	}
	v := tea.NewView(a.overlayToasts(zone.Scan(content)))
	v.AltScreen = true
	v.BackgroundColor = a.deps.Styles.Theme.Background
	v.ForegroundColor = a.deps.Styles.Theme.Text
	if a.mouse {
		// screens receive hover motion without a held button
		v.MouseMode = tea.MouseModeAllMotion
	}
	return v
}

// toastBottomGap is how many rows the notification stack floats above the terminal's last row, so
// it settles into the bottom-right corner over the footer band's blank right side.
const toastBottomGap = 1

// toastLayout places the notification stack in terminal coordinates: left column x, top and bottom
// rows (inclusive), and width w. It anchors the stack's bottom just above the last row, keeps the
// header/tab band clear, and reports ok=false when nothing is showing. overlayToasts (rendering)
// and mouseOnToast (input) share it so the visible box and the swallowed input rectangle agree.
func (a App) toastLayout() (x, top, bottom, w int, ok bool) {
	if a.toasts.Empty() || a.width == 0 || a.height == 0 {
		return 0, 0, 0, 0, false
	}
	stack := a.toasts.View()
	if stack == "" {
		return 0, 0, 0, 0, false
	}
	w = lipgloss.Width(stack)
	h := strings.Count(stack, "\n") + 1
	x = a.width - w - leftInset
	if x < 0 {
		x = 0
	}
	bottom = a.height - 1 - toastBottomGap
	minRow := 0
	if a.hasChrome(a.screen) {
		minRow = padRows + headerHeight + padRows // never paint over the header band
	}
	top = bottom - h + 1
	if top < minRow {
		top = minRow
	}
	if bottom < top {
		return 0, 0, 0, 0, false
	}
	return x, top, bottom, w, true
}

// overlayToasts splices the notification stack into the bottom-right of the already-scanned frame.
// It runs after zone.Scan, so the frame carries no zone markers — only SGR escapes, which spliceAt
// preserves on the untouched left of each covered row, leaving every other screen's clicks intact.
func (a App) overlayToasts(frame string) string {
	x, top, bottom, _, ok := a.toastLayout()
	if !ok {
		return frame
	}
	stackLines := strings.Split(a.toasts.View(), "\n")
	lines := strings.Split(frame, "\n")
	// paint bottom-up so that, if the stack is taller than the space, the OLDEST (top) lines clip
	// first and the newest — nearest the corner — always survive
	for i := len(stackLines) - 1; i >= 0; i-- {
		row := bottom - (len(stackLines) - 1 - i)
		if row < top || row >= len(lines) {
			continue
		}
		// clamp to the terminal width in case a stack wider than the screen was placed at x=0
		lines[row] = ansi.Truncate(spliceAt(lines[row], x, stackLines[i]), a.width, "")
	}
	return strings.Join(lines, "\n")
}

// spliceAt keeps the visible columns [0,x) of line with their SGR styling intact, pads to exactly x
// columns, then places insert. Columns at and past x are covered by insert and dropped. ansi.Cut is
// grapheme- and SGR-aware — the same measure lipgloss laid the frame out with — so the cut lands on
// the same column the frame was built to, even across emoji / VS16 / ZWJ clusters.
func spliceAt(line string, x int, insert string) string {
	left := ansi.Cut(line, 0, x)
	pad := x - ansi.StringWidth(left)
	if pad < 0 {
		pad = 0
	}
	// reset before the padding so an unclosed span from the cut cannot tint the pad spaces
	return left + "\x1b[0m" + strings.Repeat(" ", pad) + insert
}

// mouseOnToast reports whether a mouse event lands within the visible toast stack, so the shell can
// swallow it instead of routing it click-through to the covered widget.
func (a App) mouseOnToast(msg tea.MouseMsg) bool {
	x, top, bottom, w, ok := a.toastLayout()
	if !ok {
		return false
	}
	m := msg.Mouse()
	return m.Y >= top && m.Y <= bottom && m.X >= x && m.X < x+w
}

// openHelp raises the help modal for the active screen, seeded with the screen's self-description
// (when it offers one) and the live global + screen key bindings, so the shortcut list can never
// drift from what the footer already advertises.
func (a App) openHelp() (tea.Model, tea.Cmd) {
	title, about := "Help", ""
	if d, ok := a.activeModel().(describer); ok {
		title, about = d.HelpTitle(), d.HelpAbout()
	}
	a.modal, a.modalScroll = newHelpModal(a.deps.Styles.Theme, a.help, title, about, a.globalKeys(), a.screenKeys(), a.width, a.height), 0
	return a, nil
}

// openOptions raises the settings modal (server + account info, live theme switch, logout).
func (a App) openOptions() (tea.Model, tea.Cmd) {
	m, cmd := newOptionsModal(a.deps, a.user)
	a.modal, a.modalScroll = m, 0
	return a, cmd
}

// applyTheme switches the whole app to the named theme: it restyles the shell (header/footer/help,
// toasts) and every persistent screen in place — no state is lost — and persists the choice.
func (a App) applyTheme(name string) (tea.Model, tea.Cmd) {
	a.deps.Styles = theme.New(theme.ByName(name))
	if a.deps.Config != nil {
		a.deps.Config.Theme = name
		// best-effort: a failed persist still applies for this session, but surface it like the
		// codebase's other best-effort saves
		if err := a.deps.Config.Save(); err != nil {
			slog.Warn("save theme", "name", name, "err", err)
		}
	}
	t := a.deps.Styles.Theme
	a.help = newHelpModel(t)
	a.toasts = a.toasts.Retheme(t)
	a.home = a.home.Retheme(a.deps)
	a.schema = a.schema.Retheme(a.deps)
	a.agents = a.agents.Retheme(a.deps)
	a.inbox = a.inbox.Retheme(a.deps)
	a.project = a.project.Retheme(a.deps)
	return a, nil
}

// applyIcons switches the glyph set live and persists the choice, mirroring applyTheme. The header
// reads deps.Glyphs on each render, so only the cached consumers (toasts, screens) need refreshing.
func (a App) applyIcons(mode string) (tea.Model, tea.Cmd) {
	a.deps.Glyphs = glyph.New(glyph.ParseMode(mode))
	a.deps.Icons = mode
	if a.deps.Config != nil {
		a.deps.Config.Icons = mode
		if err := a.deps.Config.Save(); err != nil {
			slog.Warn("save icons", "mode", mode, "err", err)
		}
	}
	a.toasts = a.toasts.Reglyph(a.deps.Glyphs)
	a.home = a.home.Retheme(a.deps)
	a.schema = a.schema.Retheme(a.deps)
	a.agents = a.agents.Retheme(a.deps)
	a.inbox = a.inbox.Retheme(a.deps)
	a.project = a.project.Retheme(a.deps)
	return a, nil
}

// applyMouse toggles mouse capture and persists the choice. The View reads a.mouse to set the mouse
// mode, and the screens read deps.Mouse (refreshed via Retheme) to hide their click-only affordances.
func (a App) applyMouse(on bool) (tea.Model, tea.Cmd) {
	a.mouse = on
	a.deps.Mouse = on
	if !on {
		a.hoverTab = "" // no motion events arrive while the mouse is off, so clear any stuck highlight
	}
	if a.deps.Config != nil {
		a.deps.Config.Mouse = mouseSetting(on)
		if err := a.deps.Config.Save(); err != nil {
			slog.Warn("save mouse setting", "on", on, "err", err)
		}
	}
	a.home = a.home.Retheme(a.deps)
	a.schema = a.schema.Retheme(a.deps)
	a.agents = a.agents.Retheme(a.deps)
	a.inbox = a.inbox.Retheme(a.deps)
	a.project = a.project.Retheme(a.deps)
	return a, nil
}

func mouseSetting(on bool) string {
	if on {
		return "on"
	}
	return "off"
}

// lastProjectKey is the project to restore for the current server, or "" when none is saved (or config
// is unavailable, as in tests).
func (a App) lastProjectKey() string {
	if a.deps.Config == nil {
		return ""
	}
	return a.deps.Config.LastProjectFor(a.deps.Server)
}

// rememberLastProject persists the drilled-into project so the next silent restore returns to it. It is
// best-effort: a failed write still works for this session, matching the other config saves.
func (a App) rememberLastProject(key string) {
	if a.deps.Config == nil {
		return
	}
	if err := a.deps.Config.SetLastProject(a.deps.Server, key); err != nil {
		slog.Warn("save last project", "key", key, "err", err)
	}
}

// clearLastProject forgets the saved project (on a return to the dashboard or a fresh login).
func (a App) clearLastProject() { a.rememberLastProject("") }

type loggedOutMsg struct{}

// logoutCmd revokes the session server-side so a token that survives a failed local clear cannot be
// traded back for a fresh pair (the connecting re-probe's refresh would 401 → login), then clears the
// local tokens and transport and signals the shell to re-probe.
func logoutCmd(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		ctx := context.Background()
		if d.Authed != nil {
			if err := d.Authed.Logout(ctx); err != nil {
				slog.Warn("server-side logout", "err", err)
			}
		}
		if d.Store != nil {
			if err := d.Store.Clear(d.Server); err != nil {
				slog.Warn("clear stored tokens", "err", err)
			}
		}
		if d.Transport != nil {
			d.Transport.Clear()
		}
		return loggedOutMsg{}
	}
}

// realtimeUpdateMsg carries one connection-state change or event off the SSE consumer.
type realtimeUpdateMsg struct{ u realtime.Update }

// startRealtime (re)opens the user-scoped SSE stream for the current session and returns the command
// that waits for its first update. Any prior stream is stopped first. It no-ops without an authed
// transport (as in tests), leaving the indicator disconnected.
func (a *App) startRealtime() tea.Cmd {
	if a.deps.Transport == nil || a.deps.Server == "" {
		return nil
	}
	if a.rt != nil {
		a.rt.Stop()
	}
	a.rtState = realtime.Connecting
	c := realtime.New(a.deps.Server, &http.Client{Transport: a.deps.Transport}, a.sessionGen)
	a.rt = c
	c.Start()
	return waitForRealtime(c)
}

// stopRealtime tears the stream down (on logout) and resets the indicator.
func (a *App) stopRealtime() {
	if a.rt != nil {
		a.rt.Stop()
		a.rt = nil
	}
	a.rtState = realtime.Disconnected
}

// applyRealtime folds one consumer update into the model and re-arms the wait. Updates from a
// superseded session (a re-login/logout bumped the generation) are dropped without re-arming, ending
// that stream's command chain.
func (a App) applyRealtime(u realtime.Update) (tea.Model, tea.Cmd) {
	if a.rt == nil || u.Gen != a.sessionGen {
		return a, nil
	}
	switch u.Kind {
	case realtime.StateUpdate:
		a.rtState = u.State
	case realtime.EventUpdate:
		if u.Event.Category == "notification" {
			cmds := []tea.Cmd{waitForRealtime(a.rt), pollInboxUnread(a.deps, a.sessionGen, false)}
			if a.screen == screenInbox {
				var cmd tea.Cmd
				a.inbox, cmd = a.inbox.Update(inbox.RefreshMsg{})
				cmds = append(cmds, cmd)
			}
			return a, tea.Batch(cmds...)
		}
		// route an event to the drilled-in project screen when it is showing that event's project; the
		// screen folds it into its list/detail/sprints in place. Other screens have no consumer yet.
		if a.screen == screenProject && u.Event.ProjectKey == a.project.ProjectKey() {
			switch u.Event.Category {
			case "issue":
				var cmd tea.Cmd
				a.project, cmd = a.project.Update(project.RealtimeIssueEventMsg{Type: u.Event.Type, IssueKey: u.Event.IssueKey})
				return a, tea.Batch(cmd, waitForRealtime(a.rt))
			case "sprint":
				var cmd tea.Cmd
				a.project, cmd = a.project.Update(project.RealtimeSprintEventMsg{
					Type:      u.Event.Type,
					SprintID:  sprintIDFromEvent(u.Event.Data),
					IssueKeys: issueKeysFromEvent(u.Event.Data),
				})
				return a, tea.Batch(cmd, waitForRealtime(a.rt))
			}
		}
		slog.Debug("realtime event", "category", u.Event.Category, "type", u.Event.Type,
			"project", u.Event.ProjectKey, "issue", u.Event.IssueKey)
	}
	return a, waitForRealtime(a.rt)
}

// sprintIDFromEvent pulls data.sprintId (a JSON number, so float64 in the decoded map) from a sprint
// event's payload; 0 when absent.
func sprintIDFromEvent(data map[string]any) int64 {
	if f, ok := data["sprintId"].(float64); ok {
		return int64(f)
	}
	return 0
}

// issueKeysFromEvent pulls data.issueKeys (a JSON string array) from a sprint event's payload; it is set
// only for SPRINT_ISSUES_ADDED / SPRINT_ISSUES_REMOVED. nil when absent or malformed.
func issueKeysFromEvent(data map[string]any) []string {
	raw, ok := data["issueKeys"].([]any)
	if !ok {
		return nil
	}
	keys := make([]string, 0, len(raw))
	for _, v := range raw {
		if s, ok := v.(string); ok {
			keys = append(keys, s)
		}
	}
	return keys
}

// waitForRealtime blocks on the consumer's next update and re-arms itself after each one — the standard
// BubbleTea pattern for consuming a channel. A closed channel (the consumer was stopped) yields nil,
// ending the chain harmlessly.
func waitForRealtime(c *realtime.Consumer) tea.Cmd {
	return func() tea.Msg {
		u, ok := <-c.Updates()
		if !ok {
			return nil
		}
		return realtimeUpdateMsg{u: u}
	}
}

// updateModal routes interactive input to the open modal. A left click outside the modal's box
// dismisses it. Everything else is the modal's to handle (it emits modalClosedMsg to close).
func (a App) updateModal(msg tea.Msg) (tea.Model, tea.Cmd) {
	if click, ok := msg.(tea.MouseClickMsg); ok {
		if click.Button == tea.MouseLeft && !a.pointInModal(click) {
			a.modal, a.modalScroll = nil, 0
			return a, nil
		}
	}
	// when the modal overflows the terminal the wheel drives its window. Otherwise it belongs to the
	// modal's own scroll (for example the help viewport)
	if wheel, ok := msg.(tea.MouseWheelMsg); ok && a.modalWindowed() {
		switch wheel.Button {
		case tea.MouseWheelUp:
			return a.scrollModalBy(-1), nil
		case tea.MouseWheelDown:
			return a.scrollModalBy(1), nil
		}
	}
	var cmd tea.Cmd
	a.modal, cmd = a.modal.Update(msg)
	return a, cmd
}

// modalBox renders the modal — windowed to the terminal height with a scrollbar when it is too tall —
// and returns it with its centered top-left origin (mx,my). overlayModalDim (rendering) and
// pointInModal (input) share it so the visible box and its click-through rectangle agree.
func (a App) modalBox() (view string, mx, my, w, h int) {
	view = a.modalView()
	w, h = lipgloss.Width(view), lipgloss.Height(view)
	mx = max(0, (a.width-w)/2)
	my = max(0, (a.height-h)/2)
	return
}

// modalView is the open modal's rendered box, windowed to the terminal height with a scrollbar drawn
// in its right border when it overflows (self-sizing modals that already fit are returned untouched).
func (a App) modalView() string {
	view := a.modal.View()
	if a.height <= 0 {
		return view
	}
	t := a.deps.Styles.Theme
	windowed, _, _ := components.ScrollBox(view, a.height, a.modalScroll, t.Primary, t.Border)
	return windowed
}

// modalWindowed reports whether the open modal is taller than the terminal, so wheel scrolling drives
// the window instead of the modal's own handler.
func (a App) modalWindowed() bool {
	return a.modal != nil && a.height > 0 && lipgloss.Height(a.modal.View()) > a.height
}

func (a App) scrollModalBy(delta int) App {
	if a.modal == nil {
		return a
	}
	maxOff := lipgloss.Height(a.modal.View()) - a.height // interior overflow rows
	a.modalScroll = min(max(a.modalScroll+delta, 0), max(maxOff, 0))
	return a
}

func (a App) pointInModal(msg tea.MouseMsg) bool {
	if a.modal == nil {
		return false
	}
	_, mx, my, w, h := a.modalBox()
	m := msg.Mouse()
	return m.X >= mx && m.X < mx+w && m.Y >= my && m.Y < my+h
}

// overlayModalDim dims the whole (inert) backdrop and splices the centered modal over it. It runs
// before zone.Scan, so the modal's own markers survive to register. The backdrop is stripped, so its
// now-stale zones drop out and cannot be clicked behind the modal.
func (a App) overlayModalDim(backdrop, modal string) string {
	_, mx, my, w, _ := a.modalBox()
	dim := lipgloss.NewStyle().Foreground(a.deps.Styles.Theme.Muted)
	src := strings.Split(backdrop, "\n")
	modalLines := strings.Split(modal, "\n")
	// grow past the (height-clamped) backdrop if the modal extends below it, so every modal row — and
	// its zone markers — is emitted for the scan and pointInModal cannot claim an unrendered row
	total := len(src)
	if my+len(modalLines) > total {
		total = my + len(modalLines)
	}
	out := make([]string, total)
	for i := 0; i < total; i++ {
		plain := ""
		if i < len(src) {
			plain = ansi.Strip(src[i]) // drop backdrop SGR + zone markers — it is inert under the modal
		}
		row := i - my
		if row < 0 || row >= len(modalLines) {
			out[i] = dim.Render(plain)
			continue
		}
		left := ansi.Cut(plain, 0, mx)
		if p := mx - ansi.StringWidth(left); p > 0 {
			left += strings.Repeat(" ", p)
		}
		right := ansi.Cut(plain, mx+w, a.width)
		out[i] = dim.Render(left) + modalLines[row] + dim.Render(right)
	}
	return strings.Join(out, "\n")
}

func (a App) activeView() string {
	switch a.screen {
	case screenConnecting:
		return a.connecting.View()
	case screenLogin:
		return a.login.View()
	case screenOidcDevice:
		return a.oidcDevice.View()
	case screenHome:
		return a.home.View()
	case screenSchema:
		return a.schema.View()
	case screenAgents:
		return a.agents.View()
	case screenInbox:
		return a.inbox.View()
	case screenProject:
		return a.project.View()
	}
	return ""
}

func (a App) activeModel() any {
	switch a.screen {
	case screenConnecting:
		return a.connecting
	case screenLogin:
		return a.login
	case screenOidcDevice:
		return a.oidcDevice
	case screenHome:
		return a.home
	case screenSchema:
		return a.schema
	case screenAgents:
		return a.agents
	case screenInbox:
		return a.inbox
	case screenProject:
		return a.project
	}
	return nil
}

// helpKeyer is implemented by screens that contribute context-specific key hints.
type helpKeyer interface {
	HelpKeys() []key.Binding
}

// globalKeyer is optionally implemented by a screen to contribute nav hints to the footer's top
// (global) line - the project drill-in surfaces its sub-tab switch and reload there.
type globalKeyer interface {
	GlobalKeys() []key.Binding
}

// reservedRows is the terminal rows the chrome takes: footer band always, plus the
// header band on the screens that wear chrome.
func (a App) reservedRows() int {
	rows := padRows + a.footerRows() + padRows
	if a.hasChrome(a.screen) {
		rows += padRows + headerHeight + padRows
	}
	return rows
}

// footerRows is the footer band's real height - two rows, or more once a hint group wraps on a narrow
// terminal. The extra rows are charged to the body rather than left to overflow, so the bottom margin
// survives and the wrapped hints are not clipped off the terminal. The charge stops one row short of
// taking the body entirely: past that the footer's own tail is clipped instead, which on a terminal
// that small is the lesser loss.
func (a App) footerRows() int {
	if a.width == 0 {
		return footerHeight
	}
	return min(max(footerHeight, lipgloss.Height(a.footerView())), max(footerHeight, a.footerBudget()))
}

// footerBudget is the most rows the footer may claim: the terminal less its margins, the header band
// where one is worn, and one row for the body itself.
func (a App) footerBudget() int {
	rows := a.height - padRows - padRows - 1
	if a.hasChrome(a.screen) {
		rows -= padRows + headerHeight + padRows
	}
	return rows
}

// headerView draws "● Tissue Server · {url} · {user}" on the left. The right side is the app's
// top-level tabs, or a screen's own HeaderInfo when it supplies one (the project drill-in's tabs).
func (a App) headerView() string {
	if !a.hasChrome(a.screen) || a.width == 0 {
		return ""
	}
	t := a.deps.Styles.Theme
	brand := a.connectionBrand()
	muted := lipgloss.NewStyle().Foreground(t.Muted)
	// a narrow header sheds its optional bits (the server url and username here, the drill-in's back link
	// via compact) so the brand and the tabs still fit without colliding.
	compact := a.width <= headerCompactW
	left := brand
	if !compact {
		left += muted.Render(" · " + a.deps.Server)
		if a.user.Username != "" {
			left += muted.Render(" · " + a.user.Username)
		}
	}
	right := a.tabBar()
	if h, ok := a.activeModel().(headerInfoer); ok {
		right = h.HeaderInfo(compact)
	}

	pad := leftInset + 1 // the header sits one column further in than the body
	gap := a.width - 2*pad - lipgloss.Width(left) - lipgloss.Width(right)
	if gap < 1 {
		gap = 1
	}
	line := strings.Repeat(" ", pad) + left + strings.Repeat(" ", gap) + right + strings.Repeat(" ", pad)
	return lipgloss.NewStyle().MaxWidth(a.width).Render(line)
}

// connectionBrand renders the "● Tissue Server" label, its dot glyph and colour reflecting the live
// SSE connection state: connected (success ●), connecting (warning ◐), disconnected (error ✕).
func (a App) connectionBrand() string {
	t := a.deps.Styles.Theme
	g := a.deps.Glyphs
	dot, col := g.Disconnected, t.Error
	switch a.rtState {
	case realtime.Connected:
		dot, col = g.Connected, t.Success
	case realtime.Connecting:
		dot, col = g.Connecting, t.Warning
	}
	return lipgloss.NewStyle().Foreground(col).Bold(true).Render(dot + " Tissue Server")
}

// tabGlyph maps a tab to its nerd glyph. The fallback is empty so plain terminals show
// the label alone.
func (a App) tabGlyph(s screen) string {
	g := a.deps.Glyphs
	switch s {
	case screenHome:
		return g.Or(g.Project, "")
	case screenSchema:
		return g.Or(g.Workflow, "")
	case screenAgents:
		return g.Or(g.Robot, "")
	case screenInbox:
		return g.Or(g.Bell, "")
	}
	return ""
}

// tabBar renders the clickable top-level tabs. The active one is accented and underlined. Each tab
// carries its 1/2/3 switch key as a muted digit prefix, surfacing the shortcut at the tab itself
// instead of only in the footer help.
func (a App) tabBar() string {
	t := a.deps.Styles.Theme
	active := lipgloss.NewStyle().Foreground(t.Accent).Bold(true).Underline(true)
	inactive := lipgloss.NewStyle().Foreground(t.Muted)
	numActive := lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	numInactive := lipgloss.NewStyle().Foreground(t.Muted)
	cells := make([]string, 0, len(tabs)*2)
	for i, tab := range tabs {
		if i > 0 {
			cells = append(cells, "  ")
		}
		content := tab.label
		if gl := a.tabGlyph(tab.screen); gl != "" {
			content = gl + " " + tab.label
		}
		style, numStyle := inactive, numInactive
		switch {
		case a.screen == tab.screen:
			style, numStyle = active, numActive
		case a.hoverTab == tab.zone:
			style = lipgloss.NewStyle().Foreground(t.Secondary) // hovered inactive tab brightens
		}
		// the digit takes the accent only on the active tab (matching its label), but stays
		// un-underlined so the underline reads as the single active-tab marker
		cell := numStyle.Render(strconv.Itoa(i+1)+": ") + style.Render(content)
		// the Inbox tab wears an unread dot (the backend exposes only has-unread, not a count)
		if tab.screen == screenInbox && a.inboxUnread {
			cell += " " + lipgloss.NewStyle().Foreground(t.Warning).Render("●")
		}
		cells = append(cells, zone.Mark(tab.zone, cell))
	}
	return lipgloss.JoinHorizontal(lipgloss.Top, cells...)
}

// footerView renders the two left-aligned key hint groups: global(common) keys on top, screen-specific
// below. Each group wraps onto extra lines rather than ellipsizing, so a long hint row on a narrow
// terminal stays fully readable. The wrapped rows are charged to the body (see footerRows), which keeps
// the bottom margin; only once the footer would take the body's last row does its own tail get clipped.
func (a App) footerView() string {
	if a.width == 0 {
		return ""
	}
	pad := leftInset + 1
	budget := max(1, a.width-pad)
	top := indentEach(a.wrapHelp(a.globalKeys(), budget), pad)
	bottom := indentEach(a.wrapHelp(a.screenKeys(), budget), pad)
	return lipgloss.JoinVertical(lipgloss.Left, top, bottom)
}

// wrapHelp renders key-hint bindings as "key desc" cells joined by the help separator, packing them onto
// as many lines as needed to fit width (rather than ellipsizing like help.ShortHelpView). A single cell
// wider than width still gets its own line rather than being dropped. Styling matches the footer help so
// the wrapped rows read identically to the single-line ones.
func (a App) wrapHelp(binds []key.Binding, width int) string {
	sep := a.help.Styles.ShortSeparator.Inline(true).Render(a.help.ShortSeparator)
	sepW := lipgloss.Width(sep)
	var lines []string
	var cur strings.Builder
	curW := 0
	for _, kb := range binds {
		if !kb.Enabled() {
			continue
		}
		cell := a.help.Styles.ShortKey.Inline(true).Render(kb.Help().Key) + " " +
			a.help.Styles.ShortDesc.Inline(true).Render(kb.Help().Desc)
		cw := lipgloss.Width(cell)
		switch {
		case curW == 0:
			cur.WriteString(cell)
			curW = cw
		case curW+sepW+cw <= width:
			cur.WriteString(sep + cell)
			curW += sepW + cw
		default:
			lines = append(lines, cur.String())
			cur.Reset()
			cur.WriteString(cell)
			curW = cw
		}
	}
	if curW > 0 {
		lines = append(lines, cur.String())
	}
	return strings.Join(lines, "\n")
}

// indentEach prefixes every line of s with n spaces (an empty s stays empty, so a blank hint group keeps
// its blank line rather than gaining stray padding).
func indentEach(s string, n int) string {
	if s == "" {
		return ""
	}
	pad := strings.Repeat(" ", n)
	lines := strings.Split(s, "\n")
	for i := range lines {
		lines[i] = pad + lines[i]
	}
	return strings.Join(lines, "\n")
}

func (a App) globalKeys() []key.Binding {
	binds := make([]key.Binding, 0, 6)
	// a screen may contribute its own top-line nav keys (the project drill-in's sub-tabs + reload). They
	// hide while it captures input, so the row never advertises a key the focused field has swallowed.
	if !a.activeCapturingInput() {
		if g, ok := a.activeModel().(globalKeyer); ok {
			binds = append(binds, g.GlobalKeys()...)
		}
	}
	// the global tab-switch stays on tab screens only, so its digits never clash with the project's 1-4.
	if a.tabNavActive() {
		binds = append(binds,
			key.NewBinding(key.WithKeys("1", "2", "3", "4", "ctrl+h", "ctrl+l"), key.WithHelp("1/2/3/4", "tab")),
		)
	}
	// help and options also open on the project drill-in, so their hint follows optionsNavActive; each is
	// gated to the same condition its handler is, so the footer never advertises an inert shortcut.
	if a.optionsNavActive() {
		binds = append(binds,
			key.NewBinding(key.WithKeys("?"), key.WithHelp("?", "help")),
			key.NewBinding(key.WithKeys(","), key.WithHelp(",", "options")),
		)
	}
	return append(binds,
		key.NewBinding(key.WithKeys("ctrl+c"), key.WithHelp("ctrl+c", "quit")),
	)
}

func (a App) screenKeys() []key.Binding {
	if a.modal != nil {
		return a.modal.HelpKeys()
	}
	if k, ok := a.activeModel().(helpKeyer); ok {
		return k.HelpKeys()
	}
	return nil
}

type profileLoadedMsg struct {
	profile domain.Profile
	gen     int // the session generation this fetch was issued for
}

// fetchProfile loads the authenticated member so the header can show the username. It is stamped with
// the session generation so a result that arrives after a logout/re-login is ignored.
func fetchProfile(d deps.Deps, gen int) tea.Cmd {
	return func() tea.Msg {
		p, err := d.Authed.Profile(context.Background())
		if err != nil {
			slog.Warn("load profile", "err", err)
			return nil
		}
		return profileLoadedMsg{profile: p, gen: gen}
	}
}
