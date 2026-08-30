// Package inbox is the Inbox tab: the caller's personal, member-scoped notification feed.
package inbox

import (
	"context"

	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

const (
	minWidth  = 60
	minHeight = 12

	notifPageSize = 20 // matches the backend's default page size
)

type Model struct {
	deps   deps.Deps
	width  int
	height int

	items      []domain.Notification
	cursor     int
	loading    bool // first-page load in flight with nothing to show yet (skeleton)
	loadErr    bool
	hasNext    bool
	nextCursor string

	loadingMore bool // a next-page (append) fetch is in flight
	// reqGen supersedes a stale first-page load: a filter toggle or refresh bumps it, so a slow earlier
	// load landing late is dropped. Append loads carry the same gen.
	reqGen int

	unreadOnly   bool
	mentionsOnly bool   // server-side @mention filter (types=[ISSUE_MENTIONED]), toggled with "m"
	hover        string // zone id of the row under the cursor, "" when none

	// preferences modal ("p"). The backend exposes only the EMAIL channel (the in-app inbox is always
	// kept), so each row is one notification type's email on/off.
	prefsOpen    bool
	prefsRows    []domain.NotificationPref
	prefsCursor  int
	prefsLoading bool
	prefsErr     bool
}

// New starts empty. Init loads the first page.
func New(d deps.Deps) Model {
	return Model{deps: d, loading: true}
}

func (m Model) Init() tea.Cmd {
	return loadNotifications(m.deps, m.unreadOnly, m.mentionsOnly, "", m.reqGen, false)
}

func (m Model) Retheme(d deps.Deps) Model {
	m.deps = d
	return m
}

// CapturingInput reports whether the prefs modal owns the keyboard, so global tab-switch keys yield.
func (m Model) CapturingInput() bool { return m.prefsOpen }

func (m Model) selected() (domain.Notification, bool) {
	if m.cursor < 0 || m.cursor >= len(m.items) {
		return domain.Notification{}, false
	}
	return m.items[m.cursor], true
}

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	// the prefs modal owns interactive input while open. background loads still flow through below
	if m.prefsOpen {
		switch msg := msg.(type) {
		case tea.KeyPressMsg:
			return m.onPrefsKey(msg)
		case tea.MouseClickMsg, tea.MouseWheelMsg, tea.MouseMotionMsg:
			return m, nil
		}
	}
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		return m, nil

	case prefsLoadedMsg:
		m.prefsLoading = false
		if msg.err {
			m.prefsErr = true
			return m, nil
		}
		m.prefsErr = false
		m.prefsRows = emailPrefs(msg.rows)
		if m.prefsCursor >= len(m.prefsRows) {
			m.prefsCursor = max(0, len(m.prefsRows)-1)
		}
		return m, nil

	case prefsSaveFailedMsg:
		for i := range m.prefsRows {
			if m.prefsRows[i].Type == msg.notifType {
				m.prefsRows[i].Enabled = msg.prev // revert the optimistic toggle the server rejected
				break
			}
		}
		return m, toast.Show(toast.Error, "Couldn't update the setting.")

	case NotificationsLoadedMsg:
		return m.onLoaded(msg)

	case RefreshMsg:
		// notifications are poll-only, so without a re-pull on tab entry the list goes stale after the boot
		// prefetch. yields to an in-flight load.
		if m.loading || m.loadingMore {
			return m, nil
		}
		return m.reloadFirstPage()

	case MarkedAllMsg:
		return m, tea.Batch(
			toast.Show(toast.Success, "All notifications marked as read."),
			emitReadChanged(),
		)

	case ActionFailedMsg:
		// the server rejected an optimistic read: re-pull page 1 for the authoritative flags
		m2, cmd := m.reloadFirstPage()
		return m2, tea.Batch(toast.Show(toast.Error, msg.Text), emitReadChanged(), cmd)

	case tea.KeyPressMsg:
		return m.onKey(msg)
	case tea.MouseClickMsg:
		return m.onClick(msg)
	case tea.MouseWheelMsg:
		return m.onWheel(msg)
	case tea.MouseMotionMsg:
		return m.onHover(msg)
	}
	return m, nil
}

func (m Model) onLoaded(msg NotificationsLoadedMsg) (Model, tea.Cmd) {
	if msg.Gen != m.reqGen {
		return m, nil // a filter toggle or refresh superseded this load
	}
	if msg.Append {
		m.loadingMore = false
		if msg.Err {
			return m, nil // a failed page-append is non-fatal
		}
		m.items = append(m.items, msg.Page.Items...)
		m.hasNext, m.nextCursor = msg.Page.HasNext, msg.Page.NextCursor
		return m, nil
	}
	m.loading, m.loadingMore = false, false
	if msg.Err {
		if len(m.items) == 0 {
			m.loadErr = true // a first load with nothing already shown: surface the failure
		}
		return m, nil // otherwise SWR: keep the prior list visible on a refresh failure
	}
	m.loadErr = false
	m.items = msg.Page.Items
	m.hasNext, m.nextCursor = msg.Page.HasNext, msg.Page.NextCursor
	m.cursor = 0 // a fresh first page lands the cursor on the newest item
	return m, nil
}

func (m Model) onKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	m.hover = "" // the keyboard is driving now, so drop any stale mouse-hover highlight
	switch msg.String() {
	case "up", "k":
		return m.moveCursor(-1)
	case "down", "j":
		return m.moveCursor(1)
	case "home", "g":
		m.cursor = 0
		return m, nil
	case "end", "G":
		return m.moveCursor(len(m.items)) // clamps to the last row, pulling another page if one exists
	case "enter":
		return m.openSelected()
	case "a":
		return m.markAllRead()
	case "u":
		return m.toggleUnreadOnly()
	case "m":
		return m.toggleMentionsOnly()
	case "p":
		return m.openPrefs()
	}
	return m, nil
}

func (m Model) moveCursor(delta int) (Model, tea.Cmd) {
	if len(m.items) == 0 {
		return m, nil
	}
	m.cursor = clamp(m.cursor+delta, 0, len(m.items)-1)
	// infinite scroll: the last loaded row pulls the next page
	if m.cursor >= len(m.items)-1 && m.hasNext && !m.loadingMore {
		m.loadingMore = true
		return m, loadNotifications(m.deps, m.unreadOnly, m.mentionsOnly, m.nextCursor, m.reqGen, true)
	}
	return m, nil
}

// openSelected marks the notification read and drills into its target, if it has one.
func (m Model) openSelected() (Model, tea.Cmd) {
	n, ok := m.selected()
	if !ok {
		return m, nil
	}
	var cmds []tea.Cmd
	if !n.IsRead {
		m.items[m.cursor].IsRead = true // the slice backing array is shared, so this persists past the value copy
		cmds = append(cmds, markReadCmd(m.deps, n.ID))
	}
	if nc := navCmdFor(n); nc != nil {
		cmds = append(cmds, nc)
	}
	return m, tea.Batch(cmds...)
}

// navCmdFor drills into the notification's target: issue peek, sprint tab, or project issue list.
// Nil when there is no project reference.
func navCmdFor(n domain.Notification) tea.Cmd {
	projectKey := n.Ref.ProjectKey
	if projectKey == "" {
		projectKey = n.Data["projectKey"]
	}
	if projectKey == "" {
		return nil
	}
	issueKey := n.Ref.IssueKey
	if issueKey == "" {
		issueKey = n.Data["issueKey"]
	}
	msg := nav.OpenProjectMsg{ProjectKey: projectKey, Title: projectKey, IssueKey: issueKey}
	if issueKey == "" && n.Ref.ResourceType == "SPRINT" {
		msg.SprintID = n.Ref.ResourceID
	}
	return func() tea.Msg { return msg }
}

// markAllRead marks every notification read optimistically, then on the server.
func (m Model) markAllRead() (Model, tea.Cmd) {
	if len(m.items) == 0 {
		return m, nil
	}
	for i := range m.items {
		m.items[i].IsRead = true
	}
	return m, markAllReadCmd(m.deps)
}

// reloadFirstPage re-pulls page 1 with a fresh generation and clears the cursor. Without that clear, a
// mid-reload scroll would append the pre-reload cursor's page under the new gen, grafting a stale page.
func (m Model) reloadFirstPage() (Model, tea.Cmd) {
	m.reqGen++
	m.hasNext, m.nextCursor = false, ""
	return m, loadNotifications(m.deps, m.unreadOnly, m.mentionsOnly, "", m.reqGen, false)
}

// toggleUnreadOnly reloads from the top with a fresh generation so a slower prior load cannot
// repopulate the other filter's list.
func (m Model) toggleUnreadOnly() (Model, tea.Cmd) {
	m.unreadOnly = !m.unreadOnly
	m.items, m.cursor, m.hasNext, m.nextCursor = nil, 0, false, ""
	m.loading, m.loadErr, m.loadingMore = true, false, false
	m.reqGen++
	return m, loadNotifications(m.deps, m.unreadOnly, m.mentionsOnly, "", m.reqGen, false)
}

func (m Model) toggleMentionsOnly() (Model, tea.Cmd) {
	m.mentionsOnly = !m.mentionsOnly
	m.items, m.cursor, m.hasNext, m.nextCursor = nil, 0, false, ""
	m.loading, m.loadErr, m.loadingMore = true, false, false
	m.reqGen++
	return m, loadNotifications(m.deps, m.unreadOnly, m.mentionsOnly, "", m.reqGen, false)
}

func (m Model) onClick(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	for i := range m.items {
		if zone.Get(rowZone(i)).InBounds(msg) {
			m.cursor = i
			return m, nil
		}
	}
	return m, nil
}

func (m Model) onWheel(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	switch msg.Button {
	case tea.MouseWheelUp:
		return m.moveCursor(-1)
	case tea.MouseWheelDown:
		return m.moveCursor(1)
	}
	return m, nil
}

func (m Model) onHover(msg tea.MouseMotionMsg) (Model, tea.Cmd) {
	m.hover = ""
	for i := range m.items {
		if zone.Get(rowZone(i)).InBounds(msg) {
			m.hover = rowZone(i)
			break
		}
	}
	return m, nil
}

func (m Model) HelpTitle() string { return "Inbox" }

func (m Model) HelpAbout() string {
	return "Your personal notification feed — assignments, mentions, reviews, status changes, and sprint " +
		"activity. Notifications are polled, not pushed live. Mark items read as you go."
}

func (m Model) HelpKeys() []key.Binding {
	if m.prefsOpen {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("space"), key.WithHelp("space", "toggle email")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "close")),
		}
	}
	filter := "unread only"
	if m.unreadOnly {
		filter = "show all"
	}
	mentions := "mentions only"
	if m.mentionsOnly {
		mentions = "all types"
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "open")),
		key.NewBinding(key.WithKeys("a"), key.WithHelp("a", "mark all read")),
		key.NewBinding(key.WithKeys("u"), key.WithHelp("u", filter)),
		key.NewBinding(key.WithKeys("m"), key.WithHelp("m", mentions)),
		key.NewBinding(key.WithKeys("p"), key.WithHelp("p", "settings")),
	}
}

func clamp(v, lo, hi int) int {
	if hi < lo {
		return lo
	}
	if v < lo {
		return lo
	}
	if v > hi {
		return hi
	}
	return v
}

// NotificationsLoadedMsg carries a first page (Append=false) or a next page. Exported so the app shell
// can route the boot prefetch here while another tab is active.
type NotificationsLoadedMsg struct {
	Gen    int
	Page   domain.NotificationPage
	Err    bool
	Append bool
}

// RefreshMsg asks the inbox to silently re-pull its first page. Sent by the app shell on tab entry.
type RefreshMsg struct{}

// ReadChangedMsg tells the app shell to re-check the unread badge. Handled by the app, not here.
type ReadChangedMsg struct{}

// MarkedAllMsg and ActionFailedMsg are routed at the app level so their toast and badge refresh survive
// the user leaving the tab before the request completes.
type (
	MarkedAllMsg    struct{}
	ActionFailedMsg struct{ Text string }
)

func emitReadChanged() tea.Cmd {
	return func() tea.Msg { return ReadChangedMsg{} }
}

func loadNotifications(d deps.Deps, unreadOnly, mentionsOnly bool, cursor string, gen int, more bool) tea.Cmd {
	return func() tea.Msg {
		page, err := d.Notifications.List(context.Background(), unreadOnly, mentionsOnly, cursor, notifPageSize)
		return NotificationsLoadedMsg{Gen: gen, Page: page, Err: err != nil, Append: more}
	}
}

func markReadCmd(d deps.Deps, id int64) tea.Cmd {
	return func() tea.Msg {
		if err := d.Notifications.MarkRead(context.Background(), id); err != nil {
			return ActionFailedMsg{Text: "Couldn't mark as read."}
		}
		return ReadChangedMsg{}
	}
}

func markAllReadCmd(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		if err := d.Notifications.MarkAllRead(context.Background()); err != nil {
			return ActionFailedMsg{Text: "Couldn't mark all as read."}
		}
		return MarkedAllMsg{}
	}
}
