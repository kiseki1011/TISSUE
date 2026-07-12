// Package ui is the BubbleTea root model routing between screens, with shared theme, keys, and components.
package ui

import (
	"context"
	"log/slog"
	"strconv"
	"strings"

	"charm.land/bubbles/v2/help"
	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/connecting"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/home"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/login"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/oidcdevice"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/organization"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/schema"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

type screen int

const (
	screenConnecting screen = iota
	screenLogin
	screenOidcDevice
	screenHome
	screenSchema
	screenOrganization
)

// tabDef is one top-level tab shown in the header once authenticated.
type tabDef struct {
	screen screen
	label  string
	zone   string
}

// tabs is the header tab bar order: the project dashboard plus the global admin catalogs.
var tabs = []tabDef{
	{screenHome, "Projects", "app.tab.projects"},
	{screenSchema, "Schema", "app.tab.schema"},
	{screenOrganization, "Organization", "app.tab.organization"},
}

// Chrome band sizes.
// Each band sits one blank row away from the terminal edge and one from the content area.
const (
	padRows      = 1
	headerHeight = 1
	footerHeight = 2 // global(common) keys on the top line, screen-specific keys on the bottom
	leftInset    = 2
)

// App is the root model, owning shared dependencies and routing between screens.
type App struct {
	deps   deps.Deps
	screen screen
	width  int
	height int
	mouse  bool // when true, capture mouse so clicks can move focus
	help   help.Model
	toasts toast.Model    // bottom-right notification stack, shared across screens
	user   domain.Profile // authenticated member, populated after login

	connecting   connecting.Model
	login        login.Model
	oidcDevice   oidcdevice.Model
	home         home.Model
	schema       schema.Model
	organization organization.Model
}

// New builds the root model, starting on the connecting screen.
func New(d deps.Deps) App {
	t := d.Styles.Theme
	h := help.New()
	h.Styles.ShortKey = lipgloss.NewStyle().Foreground(t.Primary)
	h.Styles.ShortDesc = lipgloss.NewStyle().Foreground(t.Muted)
	h.Styles.ShortSeparator = lipgloss.NewStyle().Foreground(t.Border)
	h.Styles.Ellipsis = lipgloss.NewStyle().Foreground(t.Border)

	return App{
		deps:       d,
		screen:     screenConnecting,
		connecting: connecting.New(d),
		mouse:      true,
		help:       h,
		toasts:     toast.New(t, d.Glyphs),
	}
}

func (a App) Init() tea.Cmd {
	return a.connecting.Init()
}

func (a App) Update(msg tea.Msg) (tea.Model, tea.Cmd) {
	// a visible toast swallows mouse input landing on it, so the floating notification is opaque to
	// clicks/wheel rather than click-through to the widget it covers
	if mm, ok := msg.(tea.MouseMsg); ok && a.mouseOnToast(mm) {
		return a, nil
	}
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		switch msg.String() {
		case "ctrl+c":
			return a, tea.Quit
		case "ctrl+o":
			a.mouse = !a.mouse
			return a, nil
		case "ctrl+l":
			if a.tabNavActive() {
				return a.switchTab(a.stepTab(1))
			}
		case "ctrl+h":
			if a.tabNavActive() {
				return a.switchTab(a.stepTab(-1))
			}
		case "1", "2", "3":
			if a.tabNavActive() {
				if i := int(msg.String()[0] - '1'); i < len(tabs) {
					return a.switchTab(tabs[i].screen)
				}
			}
		}
	case tea.WindowSizeMsg:
		a.width, a.height = msg.Width, msg.Height
		a.help.SetWidth(msg.Width)
		return a.updateActive(tea.WindowSizeMsg{Width: msg.Width, Height: msg.Height - a.reservedRows()})
	case tea.MouseClickMsg:
		// a header-tab click switches tabs only when tab-nav is live; while a modal captures input
		// (e.g. a delete is submitting) the click yields to the screen, so a mid-action tab switch
		// cannot strand the in-flight result message on the wrong screen
		if a.tabNavActive() {
			if target, ok := a.tabAt(msg); ok {
				return a.switchTab(target)
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
	case nav.GoToHomeMsg:
		a.screen = screenHome
		a.home = home.New(a.deps, msg.Info, msg.Welcome)
		a.schema = schema.New(a.deps)
		a.organization = organization.New(a.deps)
		m, cmd := a.withSize(a.home.Init())
		// prefetch the Schema/Organization catalogs so they are ready when their tabs open
		return m, tea.Batch(cmd, fetchProfile(a.deps), a.schema.Init(), a.organization.Init())
	case profileLoadedMsg:
		a.user = msg.profile
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
	case organization.LoadedMsg:
		var cmd tea.Cmd
		a.organization, cmd = a.organization.Update(msg)
		return a, cmd
	case toast.ShowMsg, toast.ExpireMsg:
		// the notification stack is shell-owned; any screen raises a toast by emitting ShowMsg
		var cmd tea.Cmd
		a.toasts, cmd = a.toasts.Update(msg)
		return a, cmd
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
	case screenOrganization:
		a.organization, cmd = a.organization.Update(msg)
	}
	return a, cmd
}

// isTabScreen reports whether s is one of the authenticated top-level tab screens.
func (a App) isTabScreen(s screen) bool {
	for _, t := range tabs {
		if t.screen == s {
			return true
		}
	}
	return false
}

// stepTab returns the tab delta steps away from the current one, wrapping around.
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

// tabAt returns the tab whose header zone the click landed on.
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
	return a.updateActive(tea.WindowSizeMsg{Width: a.width, Height: a.height - a.reservedRows()})
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

// tabNavActive reports whether the global tab-switch keys should act right now.
func (a App) tabNavActive() bool {
	return a.isTabScreen(a.screen) && !a.activeCapturingInput()
}

// withSize forwards the current window size straight to the activated screen.
// It must not re-emit a WindowSizeMsg. The app's own handler would treat that as a terminal resize
// and overwrite the stored full size.
func (a App) withSize(initCmd tea.Cmd) (tea.Model, tea.Cmd) {
	if a.width == 0 {
		return a, initCmd
	}
	model, cmd := a.updateActive(tea.WindowSizeMsg{Width: a.width, Height: a.height - a.reservedRows()})
	return model, tea.Batch(initCmd, cmd)
}

func (a App) View() tea.View {
	var bands []string
	if header := a.headerView(); header != "" {
		bands = append(bands, "", header, "")
	}
	bands = append(bands, a.activeView())
	if footer := a.footerView(); footer != "" {
		bands = append(bands, "", footer, "")
	}
	content := lipgloss.JoinVertical(lipgloss.Left, bands...)
	// never emit more than the terminal
	content = lipgloss.NewStyle().MaxWidth(a.width).MaxHeight(a.height).Render(content)

	// float the notification stack into the bottom-right, after Scan has consumed the zone markers
	// so only SGR styling remains to preserve
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
	if a.isTabScreen(a.screen) {
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
// columns, then places insert; columns at and past x are covered by insert and dropped. ansi.Cut is
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
	case screenOrganization:
		return a.organization.View()
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
	case screenOrganization:
		return a.organization
	}
	return nil
}

// helpKeyer is implemented by screens that contribute context-specific key hints.
type helpKeyer interface {
	HelpKeys() []key.Binding
}

// reservedRows is the terminal rows the chrome takes: footer band always, plus the
// header band (brand + tab bar) on the authenticated tab screens.
func (a App) reservedRows() int {
	rows := padRows + footerHeight + padRows
	if a.isTabScreen(a.screen) {
		rows += padRows + headerHeight + padRows
	}
	return rows
}

// headerView draws "● Tissue Server · {url} · {user}" on the left and the tab bar on
// the right. It is shown only on the authenticated tab screens.
func (a App) headerView() string {
	if !a.isTabScreen(a.screen) || a.width == 0 {
		return ""
	}
	t := a.deps.Styles.Theme
	brand := lipgloss.NewStyle().Foreground(t.Success).Bold(true).Render(a.deps.Glyphs.Connected + " Tissue Server")
	muted := lipgloss.NewStyle().Foreground(t.Muted)
	left := brand + muted.Render(" · "+a.deps.Server)
	if a.user.Username != "" {
		left += muted.Render(" · " + a.user.Username)
	}
	right := a.tabBar()

	pad := leftInset + 1 // the header sits one column further in than the body
	gap := a.width - 2*pad - lipgloss.Width(left) - lipgloss.Width(right)
	if gap < 1 {
		gap = 1
	}
	line := strings.Repeat(" ", pad) + left + strings.Repeat(" ", gap) + right + strings.Repeat(" ", pad)
	return lipgloss.NewStyle().MaxWidth(a.width).Render(line)
}

// tabGlyph maps a tab to its nerd glyph; the fallback is empty so plain terminals show
// the label alone.
func (a App) tabGlyph(s screen) string {
	g := a.deps.Glyphs
	switch s {
	case screenHome:
		return g.Or(g.Project, "")
	case screenSchema:
		return g.Or(g.Workflow, "")
	case screenOrganization:
		return g.Or(g.People, "")
	}
	return ""
}

// tabBar renders the clickable top-level tabs; the active one is accented and underlined. Each tab
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
		if a.screen == tab.screen {
			style, numStyle = active, numActive
		}
		// the digit takes the accent only on the active tab (matching its label), but stays
		// un-underlined so the underline reads as the single active-tab marker
		cell := numStyle.Render(strconv.Itoa(i+1)+": ") + style.Render(content)
		cells = append(cells, zone.Mark(tab.zone, cell))
	}
	return lipgloss.JoinHorizontal(lipgloss.Top, cells...)
}

// footerView renders the two left-aligned key hint lines: global(common) keys on top, screen-specific below.
func (a App) footerView() string {
	if a.width == 0 {
		return ""
	}
	pad := strings.Repeat(" ", leftInset+1)
	top := pad + a.help.ShortHelpView(a.globalKeys())
	bottom := pad + a.help.ShortHelpView(a.screenKeys())
	return lipgloss.JoinVertical(lipgloss.Left, top, bottom)
}

func (a App) globalKeys() []key.Binding {
	mouse := "mouse: off"
	if a.mouse {
		mouse = "mouse: on"
	}
	binds := make([]key.Binding, 0, 4)
	if a.tabNavActive() {
		binds = append(binds, key.NewBinding(key.WithKeys("1", "2", "3", "ctrl+h", "ctrl+l"), key.WithHelp("1/2/3", "tab")))
	}
	return append(binds,
		key.NewBinding(key.WithKeys("?"), key.WithHelp("?", "help")),
		key.NewBinding(key.WithKeys("ctrl+o"), key.WithHelp("ctrl+o", mouse)),
		key.NewBinding(key.WithKeys("ctrl+c"), key.WithHelp("ctrl+c", "quit")),
	)
}

func (a App) screenKeys() []key.Binding {
	if k, ok := a.activeModel().(helpKeyer); ok {
		return k.HelpKeys()
	}
	return nil
}

type profileLoadedMsg struct{ profile domain.Profile }

// fetchProfile loads the authenticated member so the header can show the username.
func fetchProfile(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		p, err := d.Authed.Profile(context.Background())
		if err != nil {
			slog.Warn("load profile", "err", err)
			return nil
		}
		return profileLoadedMsg{profile: p}
	}
}
