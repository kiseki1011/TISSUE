// Package home is the post-login dashboard
package home

import (
	"context"
	"fmt"
	"log/slog"
	"sort"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/table"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// Focus order for Tab:
// search -> filter -> plus -> list -> detail -> back
const (
	focusSearch = iota
	focusFilter
	focusPlus
	focusList
	focusDetail
	focusCount
)

const (
	searchRowH = 3 // top border, input, bottom border
	hInset     = 2 // blank columns between the content and each terminal edge
	rowHeight  = 2 // lines each table row occupies (one blank separator above the content)
	minHeight  = 12
	// repoColMinWidth is the narrowest terminal that still shows the Repository column.
	// At or below 130 it is dropped so the Title has room, since it only shows a placeholder.
	repoColMinWidth = 131
	// withRepoColCount is the table column count when the Repository column is shown.
	withRepoColCount = 6
)

type Model struct {
	deps deps.Deps
	info domain.SystemInfo

	projects []domain.Project
	loading  bool
	err      error
	status   string

	table    table.Model
	search   textinput.Model
	focus    int
	hover    string // dashboard zone under the cursor, for hover highlight
	hoverRow int    // hovered project row, -1 when the cursor is off the rows

	creating bool
	create   createForm

	filtering bool
	filter    filterForm

	filterMembersOnly bool // show only projects the caller has joined
	filterHidePrivate bool // hide PRIVATE-visibility projects

	// project stats, cached by project Key (selection re-sorts, so index is unstable)
	stats        map[string]domain.ProjectStats
	statsPending map[string]bool
	statsFailed  map[string]bool
	statsQueue   []string // keys awaiting background prefetch

	listTop int // index of the first visible row

	detailScroll int    // first visible line of the Details body
	detailKey    string // project the scroll offset belongs to

	width  int
	height int
}

// New builds the dashboard for the connected server.
func New(d deps.Deps, info domain.SystemInfo, _ string) Model {
	act, vis, arch := columnTitles(d.Glyphs)
	t := table.New(
		table.WithColumns(projectColumns(60, act, vis, arch, true)),
		table.WithFocused(true),
		table.WithStyles(tableStyles(d)),
	)

	search := textinput.New()
	search.Prompt = ""
	search.Placeholder = ""

	return Model{
		deps:         d,
		info:         info,
		loading:      true,
		table:        t,
		search:       search,
		focus:        focusList,
		hoverRow:     -1,
		stats:        map[string]domain.ProjectStats{},
		statsPending: map[string]bool{},
		statsFailed:  map[string]bool{},
	}
}

func (m Model) Init() tea.Cmd {
	return loadProjects(m.deps)
}

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		m.relayout()
		m.clampDetailScroll()
		if m.creating {
			m.create = m.create.resize(m.modalBodyHeight())
		}
		return m, nil
	case projectsLoadedMsg:
		m.loading, m.err, m.projects = false, nil, msg.projects
		m.rebuildRows()
		// The window sizes before the list loads, so the initial relayout ran SetRows on an empty
		// list and drove the table cursor to -1 (SetRows clamps to len-1). With rows present now,
		// land the default selection on the top row — the topmost pinned project, or the topmost
		// project when none are pinned, since visibleProjects floats pins up.
		if m.table.Cursor() < 0 && len(m.visibleProjects()) > 0 {
			m.table.SetCursor(0)
			m.ensureCursorVisible()
		}
		return m, m.fetchStats()
	case projectsErrMsg:
		m.loading, m.err = false, msg.err
		return m, nil
	case statsLoadedMsg:
		delete(m.statsPending, msg.key)
		m.stats[msg.key] = msg.stats
		return m, m.nextStatsPrefetch() // keep the prefetch pipeline full
	case statsErrMsg:
		delete(m.statsPending, msg.key)
		m.statsFailed[msg.key] = true
		slog.Warn("load project stats", "key", msg.key, "err", msg.err)
		return m, m.nextStatsPrefetch()
	}

	if m.creating {
		return m.updateCreate(msg)
	}
	if m.filtering {
		return m.updateFilter(msg)
	}
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		return m.onKey(msg)
	case tea.MouseClickMsg:
		return m.onClick(msg)
	case tea.MouseMotionMsg:
		return m.onHover(msg)
	case tea.MouseWheelMsg:
		return m.onWheel(msg)
	}
	return m.forwardToFocused(msg)
}

func (m Model) updateCreate(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case createSubmittedMsg:
		m.creating = false
		m.status = "Created " + msg.key
		m.loading = true
		return m, loadProjects(m.deps)
	case createCancelledMsg:
		m.creating = false
		return m, nil
	case tea.KeyPressMsg:
		if msg.String() == "esc" && !m.create.submitting {
			m.creating = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.create, cmd = m.create.Update(msg)
	return m, cmd
}

func (m Model) updateFilter(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case filterAppliedMsg:
		m.filtering = false
		m.filterMembersOnly = msg.membersOnly
		m.filterHidePrivate = msg.hidePrivate
		// the narrowed list may no longer include the selected row; keep the cursor in range
		if n := len(m.visibleProjects()); n == 0 {
			m.table.SetCursor(0)
		} else if m.table.Cursor() >= n {
			m.table.SetCursor(n - 1)
		}
		m.rebuildRows()
		return m, m.maybeFetchStats()
	case filterCancelledMsg:
		m.filtering = false
		return m, nil
	}
	var cmd tea.Cmd
	m.filter, cmd = m.filter.Update(msg)
	return m, cmd
}

// onKey routes keys to the focused element. Tab cycles search -> filter -> plus ->
// list -> detail.
func (m Model) onKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	// The keyboard is now driving, so drop any stale mouse-hover highlight
	if m.hoverRow >= 0 {
		m.hoverRow = -1
		m.rebuildRows()
	}
	switch m.focus {
	case focusSearch:
		return m.onKeySearch(msg)
	case focusFilter:
		return m.onKeyButton(msg, Model.openFilter)
	case focusPlus:
		return m.onKeyButton(msg, Model.openCreate)
	case focusDetail:
		return m.onKeyDetail(msg)
	default:
		return m.onKeyList(msg)
	}
}

func (m Model) onKeySearch(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "tab":
		return m.cycleFocus(1)
	case "shift+tab":
		return m.cycleFocus(-1)
	case "enter", "down", "esc":
		return m.focusList()
	}
	var cmd tea.Cmd
	m.search, cmd = m.search.Update(msg)
	m.rebuildRows()
	return m, tea.Batch(cmd, m.maybeFetchStats())
}

// onKeyButton handles the filter + "+" buttons
// Tab for focus change, Enter/Space activates.
func (m Model) onKeyButton(msg tea.KeyPressMsg, activate func(Model) (Model, tea.Cmd)) (Model, tea.Cmd) {
	switch msg.String() {
	case "tab":
		return m.cycleFocus(1)
	case "shift+tab":
		return m.cycleFocus(-1)
	case "/":
		return m.focusSearch()
	case "esc":
		return m.focusList()
	case "enter", " ", "space":
		return activate(m)
	}
	return m, nil
}

func (m Model) onKeyList(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "/":
		return m.focusSearch()
	case "tab":
		return m.cycleFocus(1)
	case "shift+tab":
		return m.cycleFocus(-1)
	case "up":
		if m.table.Cursor() == 0 {
			return m.focusSearch()
		}
	case "c":
		return m.openCreate()
	case "p":
		return m.togglePin()
	}

	var cmd tea.Cmd
	m.table, cmd = m.table.Update(msg)
	m.rebuildRows() // re-mark the newly selected row so the highlight stays whole
	return m, tea.Batch(cmd, m.maybeFetchStats())
}

// onKeyDetail scrolls the Details body and hands off focus.
func (m Model) onKeyDetail(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "tab":
		return m.cycleFocus(1)
	case "shift+tab":
		return m.cycleFocus(-1)
	case "/":
		return m.focusSearch()
	case "esc":
		return m.focusList()
	case "up", "k":
		m.scrollDetail(-1)
	case "down", "j":
		m.scrollDetail(1)
	case "pgup":
		m.scrollDetail(-(m.detailBodyHeight() - 1))
	case "pgdown":
		m.scrollDetail(m.detailBodyHeight() - 1)
	case "home", "g":
		m.detailScroll = 0
	case "end", "G":
		m.detailScroll = m.detailScrollMax()
	}
	return m, nil
}

func (m *Model) scrollDetail(delta int) {
	m.detailScroll += delta
	m.clampDetailScroll()
}

// setFocus moves focus to target, focusing the search input only when it lands there.
func (m Model) setFocus(target int) (Model, tea.Cmd) {
	m.focus = target
	if target == focusSearch {
		m.status = ""
		return m, m.search.Focus()
	}
	m.search.Blur()
	return m, nil
}

// cycleFocus advances the focus by delta around the Tab order.
func (m Model) cycleFocus(delta int) (Model, tea.Cmd) {
	return m.setFocus((m.focus + delta + focusCount) % focusCount)
}

func (m Model) focusSearch() (Model, tea.Cmd) { return m.setFocus(focusSearch) }

func (m Model) focusList() (Model, tea.Cmd) { return m.setFocus(focusList) }

func (m Model) focusDetail() (Model, tea.Cmd) { return m.setFocus(focusDetail) }

func (m Model) togglePin() (Model, tea.Cmd) {
	p, ok := m.selectedProject()
	if !ok {
		return m, nil
	}
	if err := m.deps.Config.TogglePin(m.deps.Server, p.Key); err != nil {
		slog.Warn("toggle pin", "err", err)
	}
	cursor := m.table.Cursor()
	m.rebuildRows()
	m.table.SetCursor(cursor)
	return m, m.maybeFetchStats()
}

// onClick routes a left click to zones marked in the view.
func (m Model) onClick(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	switch {
	case zone.Get("home.plus").InBounds(msg):
		return m.openCreate()
	case zone.Get("home.filter").InBounds(msg):
		return m.openFilter()
	case zone.Get("home.search").InBounds(msg):
		return m.focusSearch()
	case zone.Get("home.list").InBounds(msg):
		return m.clickList(msg)
	case zone.Get("home.detail").InBounds(msg):
		return m.focusDetail()
	}
	return m, nil
}

// clickList focuses the project list and selects the clicked row
func (m Model) clickList(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	m.focus = focusList
	m.search.Blur()
	_, localY := zone.Get("home.list").Pos(msg)
	if row, ok := m.rowAt(localY); ok {
		m.table.SetCursor(row)
		m.rebuildRows() // re-mark the newly selected row and keep it in view
	}
	return m, m.maybeFetchStats()
}

// rowAt maps a click's list-box-local Y to a visible project index
func (m Model) rowAt(localY int) (int, bool) {
	const preRows = 3
	dataY := localY - preRows
	if dataY < 0 {
		return 0, false
	}
	visIdx := dataY / rowHeight
	if visIdx >= m.maxVisibleRows() {
		return 0, false
	}
	row := m.listTop + visIdx
	if row < 0 || row >= len(m.visibleProjects()) {
		return 0, false
	}
	return row, true
}

// onHover tracks the button/box (and, for the list, the row) the cursor is over so the view can highlight
func (m Model) onHover(msg tea.MouseMotionMsg) (Model, tea.Cmd) {
	hover, hoverRow := "", -1
	switch {
	case zone.Get("home.plus").InBounds(msg):
		hover = "home.plus"
	case zone.Get("home.filter").InBounds(msg):
		hover = "home.filter"
	case zone.Get("home.search").InBounds(msg):
		hover = "home.search"
	case zone.Get("home.list").InBounds(msg):
		hover = "home.list"
		_, localY := zone.Get("home.list").Pos(msg)
		if row, ok := m.rowAt(localY); ok {
			hoverRow = row
		}
	}
	changed := hoverRow != m.hoverRow
	m.hover, m.hoverRow = hover, hoverRow
	if changed {
		m.rebuildRows() // restyle the row now under the cursor
	}
	return m, nil
}

const detailWheelStep = 2

// onWheel scrolls the Details body when the wheel turns over it.
func (m Model) onWheel(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	if !zone.Get("home.detail").InBounds(msg) {
		return m, nil
	}
	switch msg.Button {
	case tea.MouseWheelUp:
		m.detailScroll -= detailWheelStep
	case tea.MouseWheelDown:
		m.detailScroll += detailWheelStep
	default:
		return m, nil
	}
	m.clampDetailScroll()
	return m, nil
}

// clampDetailScroll keeps the Details scroll offset within its scrollable range.
func (m *Model) clampDetailScroll() {
	m.detailScroll = min(max(m.detailScroll, 0), m.detailScrollMax())
}

// detailScrollMax is the largest scroll offset that still shows content, i.e. the
// wrapped line count beyond what one body height can display.
func (m Model) detailScrollMax() int {
	wrapped := lipgloss.NewStyle().Width(m.detailContentW()).Render(m.detailBody())
	return max(0, strings.Count(wrapped, "\n")+1-m.detailBodyHeight())
}

// openCreate shows the create modal
func (m Model) openCreate() (Model, tea.Cmd) {
	m.creating = true
	m.create = newCreateForm(m.deps, m.modalBodyHeight())
	m.status = ""
	return m, m.create.Init()
}

// openFilter shows the list-filter modal, seeded with the active filters.
func (m Model) openFilter() (Model, tea.Cmd) {
	m.filtering = true
	m.filter = newFilterForm(m.deps, m.filterMembersOnly, m.filterHidePrivate)
	m.status = ""
	return m, m.filter.Init()
}

// modalBodyHeight is the rows the create modal's scrollable body can use
func (m Model) modalBodyHeight() int {
	h := m.height - 4
	if h < 3 {
		h = 3
	}
	return h
}

func (m Model) forwardToFocused(msg tea.Msg) (Model, tea.Cmd) {
	if m.focus == focusSearch {
		var cmd tea.Cmd
		m.search, cmd = m.search.Update(msg)
		return m, cmd
	}
	var cmd tea.Cmd
	m.table, cmd = m.table.Update(msg)
	m.rebuildRows() // re-mark the newly selected row so the highlight stays whole
	return m, tea.Batch(cmd, m.maybeFetchStats())
}

// maybeFetchStats loads the selected project's stats (unless cached, in flight, or
// failed) and resets the Details scroll when the selection moves to another project.
func (m *Model) maybeFetchStats() tea.Cmd {
	p, ok := m.selectedProject()
	if !ok {
		return nil
	}
	if p.Key != m.detailKey {
		m.detailKey = p.Key
		m.detailScroll = 0
	}
	return m.fetchStatsFor(p.Key)
}

// how many stats requests run at once during the background prefetch
const statsPrefetchWorkers = 4

// fetchStats requests the selected project's stats first, then starts the bounded background prefetch
// of every other project's stats
func (m *Model) fetchStats() tea.Cmd {
	m.statsQueue = m.statsQueue[:0]
	for _, p := range m.projects {
		m.statsQueue = append(m.statsQueue, p.Key)
	}
	cmds := []tea.Cmd{m.maybeFetchStats()}
	for i := 0; i < statsPrefetchWorkers; i++ {
		cmds = append(cmds, m.nextStatsPrefetch())
	}
	return tea.Batch(cmds...)
}

// nextStatsPrefetch dequeues the next project that still needs stats and requests
// them, or returns nil when the queue is drained.
func (m *Model) nextStatsPrefetch() tea.Cmd {
	for len(m.statsQueue) > 0 {
		key := m.statsQueue[0]
		m.statsQueue = m.statsQueue[1:]
		if cmd := m.fetchStatsFor(key); cmd != nil {
			return cmd
		}
	}
	return nil
}

// fetchStatsFor requests one project's stats unless they are already cached, in
// flight, or failed.
func (m *Model) fetchStatsFor(key string) tea.Cmd {
	if _, cached := m.stats[key]; cached {
		return nil
	}
	if m.statsPending[key] || m.statsFailed[key] {
		return nil
	}
	m.statsPending[key] = true
	return loadStats(m.deps, key)
}

// relayout re-sizes the project table to the current terminal width.
func (m *Model) relayout() {
	if m.width < m.minWidth() || m.height < minHeight {
		return
	}
	leftW, _ := m.panelWidths()
	// -4 for the box border and inset, -2 more for the inner padding added in panels.
	tableW := leftW - 6
	act, vis, arch := columnTitles(m.deps.Glyphs)
	showRepo := m.showRepo()
	cols := projectColumns(tableW, act, vis, arch, showRepo)
	rows := m.projectRows(showRepo)
	// Every table setter re-renders eagerly, and renderRow indexes cols[i] while ranging
	// a row's cells, so a row must never be longer than the columns mid-swap. Crossing
	// the Repository threshold changes the count: when it shrinks, install the narrower
	// rows first; when it grows (or is unchanged), install the wider columns first.
	if len(cols) < len(m.table.Columns()) {
		m.table.SetRows(rows)
		m.table.SetColumns(cols)
	} else {
		m.table.SetColumns(cols)
		m.table.SetRows(rows)
	}
	m.table.SetWidth(tableW)
	m.table.SetHeight(len(rows)*rowHeight + 1)
	// leave room for the fixed search glyph and its trailing space
	iconW := lipgloss.Width(m.deps.Glyphs.Search) + 1
	m.search.SetWidth(m.searchBoxW() - 4 - iconW)
	m.ensureCursorVisible()
}

// rebuildRows repopulates the table rows to match the currently installed columns.
// It keys the Repository cell off the actual column count, not showRepo(width): a
// resize to a too-short terminal makes relayout early-return, so the width can cross
// the Repository threshold while the columns stay put. Rows must equal the columns or
// SetRows' eager render panics (renderRow indexes cols[i] while ranging a row's cells).
func (m *Model) rebuildRows() {
	if len(m.table.Columns()) == 0 {
		return
	}
	rows := m.projectRows(len(m.table.Columns()) == withRepoColCount)
	m.table.SetRows(rows)
	// Size the table to all rows and scroll it ourselves in listView; the widget's
	// line-based scrolling mishandles our multi-line rows.
	m.table.SetHeight(len(rows)*rowHeight + 1)
	m.ensureCursorVisible()
}

// projectRows builds the table rows for the visible projects. showRepo controls whether
// each row carries the Repository cell and must match the installed column set.
func (m Model) projectRows(showRepo bool) []table.Row {
	visible := m.visibleProjects()
	cursor := m.table.Cursor()
	rows := make([]table.Row, 0, len(visible))
	s := m.deps.Styles
	g := m.deps.Glyphs
	muted := lipgloss.NewStyle().Foreground(s.Theme.Muted)
	keyStyle := lipgloss.NewStyle().Foreground(s.Theme.Primary)
	pinStyle := lipgloss.NewStyle().Foreground(s.Theme.Warning)
	// Status glyphs render bold so they match the bold header and selected row
	success := lipgloss.NewStyle().Foreground(s.Theme.Success).Bold(true)
	mutedGlyph := lipgloss.NewStyle().Foreground(s.Theme.Muted).Bold(true)
	const dash = "-"
	for i, p := range visible {
		pinned := m.deps.Config.IsPinned(m.deps.Server, p.Key)
		vis := g.Or(visGlyph(g, p.Visibility), visibilityLabel(p.Visibility))
		if vis == "" {
			vis = dash
		}
		arch := dash
		if p.Archived {
			arch = g.Or(g.ArchiveCheck, "YES")
		}
		act := humanizeSince(effectiveActivity(p))
		var cells []string
		switch {
		case i == cursor, i == m.hoverRow:
			// Both rows sit under a solid background (selection / hover), so leave the
			// cells uncolored to keep inner color resets from punching holes in it.
			title := p.Title
			if pinned {
				title = g.Pin + " " + title
			}
			cells = []string{act, p.Key, title, dash, vis, arch}
		case p.Archived:
			title := p.Title
			if pinned {
				title = g.Pin + " " + title
			}
			cells = []string{muted.Render(act), muted.Render(p.Key), muted.Render(title), muted.Render(dash), mutedGlyph.Render(vis), success.Render(arch)}
		default:
			title := p.Title
			if pinned {
				title = pinStyle.Render(g.Pin) + " " + title
			}
			visCell := lipgloss.NewStyle().Foreground(visColor(s, p.Visibility)).Bold(true).Render(vis)
			cells = []string{muted.Render(act), keyStyle.Render(p.Key), title, muted.Render(dash), visCell, muted.Render(arch)}
		}
		if !showRepo {
			// drop the Repository cell (index 3) to match the trimmed column set
			cells = append(cells[:3], cells[4:]...)
		}
		rows = append(rows, cells)
	}
	return rows
}

// maxVisibleRows is how many whole rows fit in the list body.
func (m Model) maxVisibleRows() int {
	// panelHeight - box(2) - inner padding(2) - header(1), floored to whole rows.
	dataLines := m.panelHeight() - 5
	if dataLines < rowHeight {
		return 1
	}
	return dataLines / rowHeight
}

// ensureCursorVisible scrolls listTop the minimum needed to keep the cursor row on screen.
func (m *Model) ensureCursorVisible() {
	vis := m.maxVisibleRows()
	c := m.table.Cursor()
	if c < m.listTop {
		m.listTop = c
	} else if c >= m.listTop+vis {
		m.listTop = c - vis + 1
	}
	if maxTop := len(m.visibleProjects()) - vis; m.listTop > maxTop {
		m.listTop = maxTop
	}
	if m.listTop < 0 {
		m.listTop = 0
	}
}

// listView renders the table's header + window of rows around listTop,
// padded to the list body height so the box stays rectangular.
func (m Model) listView() string {
	lines := strings.Split(m.table.View(), "\n")
	header, data := lines[0], lines[1:]

	region := m.panelHeight() - 4 // lines for header + data window inside the padding
	if region < 1 {
		region = 1
	}
	show := m.maxVisibleRows() * rowHeight
	start := m.listTop * rowHeight
	if start > len(data) {
		start = len(data)
	}
	end := start + show
	if end > len(data) {
		end = len(data)
	}

	body := append([]string{header}, data[start:end]...)
	t := m.deps.Styles.Theme
	sel := lipgloss.NewStyle().Foreground(t.Text).Background(t.Selection).Bold(true)
	m.paintRow(body, start, end, m.table.Cursor(), sel)
	// The hovered row gets a dimmer tone, and only when it is not the selected one.
	if m.hoverRow >= 0 && m.hoverRow != m.table.Cursor() {
		m.paintRow(body, start, end, m.hoverRow, m.hoverStyle())
	}
	for len(body) < region {
		body = append(body, "")
	}
	return strings.Join(body[:region], "\n")
}

// paintRow shades only the row's content line (the last line of its rowHeight-line
// block; the blank separator sits above it) so the highlight is a single line.
func (m Model) paintRow(body []string, start, end, row int, style lipgloss.Style) {
	di := row*rowHeight + (rowHeight - 1)
	if di < start || di >= end {
		return
	}
	if bi := 1 + (di - start); bi >= 0 && bi < len(body) {
		body[bi] = style.Render(body[bi])
	}
}

// hoverStyle is the dimmer highlight for the hovered (non-selected) row.
func (m Model) hoverStyle() lipgloss.Style {
	t := m.deps.Styles.Theme
	if _, noBg := t.Background.(lipgloss.NoColor); noBg {
		// The ANSI theme follows the terminal's own colors and has no real background
		// to dim, so tint the text instead of fabricating a background color.
		return lipgloss.NewStyle().Foreground(t.Secondary)
	}
	return lipgloss.NewStyle().Foreground(t.Text).Background(mixColors(t.Selection, t.Background, 0.5))
}

// visibleProjects applies the search query and filters, floating pinned projects to the top.
func (m Model) visibleProjects() []domain.Project {
	query := strings.ToLower(strings.TrimSpace(m.search.Value()))
	var pinned, rest []domain.Project
	for _, p := range m.projects {
		if query != "" && !matchesQuery(p, query) {
			continue
		}
		if m.filterMembersOnly && p.MyRole == "" {
			continue
		}
		if m.filterHidePrivate && strings.EqualFold(p.Visibility, "PRIVATE") {
			continue
		}
		if m.deps.Config.IsPinned(m.deps.Server, p.Key) {
			pinned = append(pinned, p)
		} else {
			rest = append(rest, p)
		}
	}
	byRecentActivity(pinned)
	byRecentActivity(rest)
	return append(pinned, rest...)
}

// byRecentActivity orders projects by most-recent-activity first.
func byRecentActivity(ps []domain.Project) {
	sort.SliceStable(ps, func(i, j int) bool {
		return effectiveActivity(ps[i]).After(effectiveActivity(ps[j]))
	})
}

func (m Model) selectedProject() (domain.Project, bool) {
	visible := m.visibleProjects()
	i := m.table.Cursor()
	if i < 0 || i >= len(visible) {
		return domain.Project{}, false
	}
	return visible[i], true
}

func (m Model) View() string {
	if m.width == 0 {
		return ""
	}
	if m.width < m.minWidth() || m.height < minHeight {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			m.deps.Styles.Muted.Render("Terminal too small"))
	}

	if m.creating || m.filtering {
		return m.modalView()
	}
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, m.dashboard())
}

func (m Model) dashboard() string {
	left := lipgloss.JoinVertical(lipgloss.Left, m.searchRow(), m.listPanel())
	return lipgloss.JoinHorizontal(lipgloss.Top, left, " ", m.detailPanel())
}

// modalView centers the active modal over the dimmed dashboard, splicing it in by
// hand (not lipgloss.NewCompositor, which drops zone marks) so it stays clickable.
func (m Model) modalView() string {
	backdrop := stripANSI(lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Top, m.dashboard()))
	modal := m.create.View()
	if m.filtering {
		modal = m.filter.View()
	}
	mx := max(0, (m.width-lipgloss.Width(modal))/2)
	my := max(0, (m.height-lipgloss.Height(modal))/2)
	return overlayDim(backdrop, modal, mx, my, m.deps.Styles.Theme.Muted)
}

const (
	plusButtonW   = 5
	filterButtonW = 5
)

// searchBoxW is the outer width of the search box, sized so the search row (search
// box + filter + create buttons) exactly fills the list column below it.
func (m Model) searchBoxW() int {
	leftW, _ := m.panelWidths()
	return leftW - plusButtonW - filterButtonW - 2 // two one-cell gaps between the three
}

func (m Model) searchRow() string {
	t := m.deps.Styles.Theme
	boxW := m.searchBoxW()

	searchBorder := t.Muted
	switch {
	case m.focus == focusSearch:
		searchBorder = t.Accent
	case m.hover == "home.search":
		searchBorder = t.Secondary
	}
	// The glyph lives outside the input so its size never changes with focus.
	icon := lipgloss.NewStyle().Foreground(t.Muted).Render(m.deps.Glyphs.Search)
	inner := icon + " " + m.search.View()
	inputBody := lipgloss.NewStyle().Width(boxW - 4).MaxWidth(boxW - 4).MaxHeight(1).Render(inner)
	searchBox := zone.Mark("home.search", components.TitledBox("Search", inputBody, searchBorder))

	return lipgloss.JoinHorizontal(lipgloss.Top, searchBox, " ", m.filterButton(), " ", m.plusButton())
}

// filterButton opens the filter modal; it glows accent while focused or while any
// filter is active.
func (m Model) filterButton() string {
	t := m.deps.Styles.Theme
	col := t.Muted
	switch {
	case m.focus == focusFilter:
		col = t.Accent
	case m.hover == "home.filter":
		col = t.Secondary
	case m.filterMembersOnly || m.filterHidePrivate:
		col = t.Accent
	}
	g := m.deps.Glyphs.Or(m.deps.Glyphs.Filter, "F")
	return zone.Mark("home.filter", components.TitledBox("", lipgloss.NewStyle().Foreground(col).Bold(true).Render(g), col))
}

// plusButton opens the create modal.
func (m Model) plusButton() string {
	t := m.deps.Styles.Theme
	col := t.Muted
	switch {
	case m.focus == focusPlus:
		col = t.Accent
	case m.hover == "home.plus":
		col = t.Secondary
	}
	return zone.Mark("home.plus", components.TitledBox("", lipgloss.NewStyle().Foreground(col).Bold(true).Render("+"), col))
}

// listPanel is the projects box. Its width matches the search row stacked above it.
func (m Model) listPanel() string {
	t := m.deps.Styles.Theme
	listBorder := t.Primary
	if m.focus == focusList {
		listBorder = t.Accent
	}
	listBody := lipgloss.NewStyle().Padding(1, 1).Render(m.listView())
	return zone.Mark("home.list", components.TitledBoxSub(m.listTitle(), m.listCounter(), listBody, listBorder))
}

const (
	detailInsetL     = 2 // blank columns left of the Details body
	detailInsetR     = 1 // blank column between the body and the scrollbar
	detailScrollbarW = 1 // rightmost column reserved for the scrollbar
	detailPadBottom  = 1 // blank rows kept above the bottom rule
)

// detailPanel is the Details column: a titled top rule and a bottom rule with no
// side borders, spanning the full height, with a right-edge scrollbar when the
// content overflows the visible body.
func (m Model) detailPanel() string {
	_, rightW := m.panelWidths()
	t := m.deps.Styles.Theme
	totalRows := m.detailBodyRows() // rows between the top and bottom rules
	viewH := m.detailBodyHeight()   // visible content rows (total minus the bottom pad)
	contentW := m.detailContentW()

	lines := strings.Split(lipgloss.NewStyle().Width(contentW).Render(m.detailBody()), "\n")
	off := min(max(m.detailScroll, 0), max(0, len(lines)-viewH))
	bar := scrollbarColumn(off, len(lines), viewH, t)

	blank := strings.Repeat(" ", contentW)
	padL := strings.Repeat(" ", detailInsetL)
	padR := strings.Repeat(" ", detailInsetR)
	rows := make([]string, totalRows)
	for i := range rows {
		line, barCell := blank, " "
		if i < viewH { // the trailing rows stay blank as bottom padding
			if di := off + i; di < len(lines) {
				line = lines[di]
			}
			barCell = bar[i]
		}
		rows[i] = padL + line + padR + barCell
	}

	border := t.Primary
	if m.focus == focusDetail {
		border = t.Accent
	}
	top := ruleWithTitle("Details", rightW, border)
	bottom := lipgloss.NewStyle().Foreground(border).Render(strings.Repeat("─", rightW))
	panel := lipgloss.JoinVertical(lipgloss.Left, top, strings.Join(rows, "\n"), bottom)
	return zone.Mark("home.detail", panel)
}

// detailBody is the Details content with a blank line under the top rule
func (m Model) detailBody() string {
	return "\n" + m.detail()
}

// detailBodyRows is the number of rows between the top and bottom rules
func (m Model) detailBodyRows() int {
	return max(1, m.height-2)
}

// detailBodyHeight is the visible content height
func (m Model) detailBodyHeight() int {
	return max(1, m.detailBodyRows()-detailPadBottom)
}

// detailContentW is the Details text width after the insets and scrollbar column
func (m Model) detailContentW() int {
	_, rightW := m.panelWidths()
	return max(1, rightW-detailInsetL-detailInsetR-detailScrollbarW)
}

func (m Model) listTitle() string {
	if m.loading {
		return "Projects (loading)"
	}
	return fmt.Sprintf("Projects (%d)", len(m.visibleProjects()))
}

// listCounter is the "{focused position}/{count}" for the list box bottom edge.
// Empty when there is nothing to show.
func (m Model) listCounter() string {
	count := len(m.visibleProjects())
	if count == 0 {
		return ""
	}
	pos := m.table.Cursor() + 1
	if pos > count {
		pos = count
	}
	return fmt.Sprintf("%d/%d", pos, count)
}

func (m Model) detail() string {
	s := m.deps.Styles
	if m.err != nil {
		return s.Error.Render("Failed to load projects.")
	}
	p, ok := m.selectedProject()
	if !ok {
		return s.Muted.Render("No project selected.")
	}

	g := m.deps.Glyphs
	key := lipgloss.NewStyle().Foreground(s.Theme.Primary).Bold(true).Render(p.Key)
	// g.Or(..., "") shows the glyph in nerd mode and nothing in fallback
	row := func(icon, name, value string) string {
		if icon != "" {
			name = icon + "  " + name
		}
		return detailRow(s, name, value)
	}
	rows := []string{
		s.Title.Render(p.Title),
		"",
		row(g.Or(g.Key, ""), "Key", key),
		row(g.Or(g.Eye, ""), "Visibility", visibilityLabel(p.Visibility)),
		row(g.Or(g.Cabinet, ""), "Archived", yesNo(p.Archived)),
		row(g.Or(g.Calendar, ""), "Created", formatDate(p.CreatedAt)),
		row(g.Or(g.Calendar, ""), "Updated", formatDate(p.UpdatedAt)),
		row(g.Or(g.People, ""), "Members", fmt.Sprintf("%d", p.MemberCount)),
		row(g.Or(g.AccountBadge, ""), "Role", roleValue(s, p.MyRole)),
		"",
		row(g.Or(g.Git, ""), "Repository", s.Muted.Render("-")),
		"",
	}

	rows = append(rows, m.statsRows(p.Key)...)

	if p.Description != "" {
		rows = append(rows, p.Description)
	} else {
		rows = append(rows, s.Muted.Render("No description."))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// statsRows is the Details stats block for a project.
// The cached stats, a loading line while a fetch is in flight, or an unavailable line on failure.
// Nil until a fetch starts.
func (m Model) statsRows(key string) []string {
	s := m.deps.Styles
	contentW := m.detailContentW()

	var body []string
	switch {
	case hasStats(m.stats, key):
		body = statsBlock(s, m.deps.Glyphs, m.stats[key], contentW)
	case m.statsPending[key]:
		body = []string{s.Muted.Render("Loading stats…")}
	case m.statsFailed[key]:
		body = []string{s.Muted.Render("Stats unavailable.")}
	default:
		return nil
	}
	// close the stats area with a rule before the description
	return append(body, "", sectionRule(s, contentW), "")
}

func hasStats(stats map[string]domain.ProjectStats, key string) bool {
	_, ok := stats[key]
	return ok
}

// innerWidth is the content width after the side insets.
func (m Model) innerWidth() int {
	return m.width - 2*hInset
}

// panelWidths splits the inner width into a 3:2 list/detail pair with a one-cell gap.
func (m Model) panelWidths() (left, right int) {
	usable := m.innerWidth() - 1
	left = usable * 3 / 5
	right = usable - left
	return left, right
}

const (
	// titleColFloor is the Title width at which the table starts to truncate; below it
	// the columns overflow. It locates where the six-column (Repository) layout renders.
	titleColFloor = 6
	// titleColComfort keeps the Title readable at the narrowest rendered width. It sets
	// the five-column floor (about 110 cells in nerd mode).
	titleColComfort = 25
)

// minWidth is the smallest terminal width that still renders the dashboard. Below the
// Repository threshold the table drops to five columns, so the floor follows that
// narrower layout with a comfortable Title (nerd glyph headers pack tighter than
// fallback words, so nerd mode floors lower). But if the six-column layout would not
// yet fit by the width it reappears at, hold the floor there instead so the band where
// Repository shows is never rendered broken.
func (m Model) minWidth() int {
	if floor6 := m.tableFloorWidth(true, titleColFloor); floor6 > repoColMinWidth {
		return floor6
	}
	return m.tableFloorWidth(false, titleColComfort)
}

// tableFloorWidth is the smallest terminal width at which the project table's Title
// column still reaches titleMin cells for the given column set. It inverts the
// panelWidths -> tableW -> projectColumns chain.
func (m Model) tableFloorWidth(showRepo bool, titleMin int) int {
	_, vis, arch := columnTitles(m.deps.Glyphs)
	visW, archW := glyphColW(vis), glyphColW(arch)
	ncols, fixed := 5, activityColW+10+visW+archW // activity + key + vis + arch
	if showRepo {
		ncols, fixed = 6, activityColW+10+21+visW+archW // + repository
	}
	tableMin := titleMin + 2*ncols + fixed // projectColumns: titleW = tableW - 2*ncols - fixed
	leftMin := tableMin + 6                // relayout: tableW = leftW - 6
	usable := (leftMin*5 + 2) / 3          // panelWidths: leftW = usable*3/5, inverted and ceil'd
	return usable + 5                      // innerWidth = width - 4; usable = innerWidth - 1
}

func (m Model) panelHeight() int {
	return m.height - searchRowH
}

// showRepo reports whether the Repository column fits; it is dropped on narrow terminals.
func (m Model) showRepo() bool {
	return m.width >= repoColMinWidth
}

// CapturingInput reports whether the screen owns the keyboard (search field focused or a
// modal open), so the app shell suppresses its global tab-switch keys.
func (m Model) CapturingInput() bool {
	return m.focus == focusSearch || m.creating || m.filtering
}

func (m Model) HelpKeys() []key.Binding {
	if m.creating {
		return m.create.HelpKeys()
	}
	if m.filtering {
		return m.filter.HelpKeys()
	}
	if m.focus == focusSearch {
		return []key.Binding{
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "apply")),
			key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "list")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
		}
	}
	if m.focus == focusDetail {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "scroll")),
			key.NewBinding(key.WithKeys("pgup", "pgdown"), key.WithHelp("PgUp/PgDn", "page")),
			key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "focus")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "list")),
		}
	}
	if m.focus == focusFilter || m.focus == focusPlus {
		return []key.Binding{
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "open")),
			key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "focus")),
			key.NewBinding(key.WithKeys("/"), key.WithHelp("/", "search")),
		}
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "focus")),
		key.NewBinding(key.WithKeys("/"), key.WithHelp("/", "search")),
		key.NewBinding(key.WithKeys("c"), key.WithHelp("c", "new project")),
		key.NewBinding(key.WithKeys("p"), key.WithHelp("p", "pin/unpin")),
	}
}

type projectsLoadedMsg struct{ projects []domain.Project }

type projectsErrMsg struct{ err error }

func loadProjects(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		projects, err := d.Projects.ListProjects(context.Background(), true)
		if err != nil {
			return projectsErrMsg{err: err}
		}
		return projectsLoadedMsg{projects: projects}
	}
}

type statsLoadedMsg struct {
	key   string
	stats domain.ProjectStats
}

type statsErrMsg struct {
	key string
	err error
}

func loadStats(d deps.Deps, key string) tea.Cmd {
	return func() tea.Msg {
		stats, err := d.Projects.GetProjectStats(context.Background(), key)
		if err != nil {
			return statsErrMsg{key: key, err: err}
		}
		return statsLoadedMsg{key: key, stats: stats}
	}
}
