// Package project is the project drill-in screen: the issue list for one project.
package project

import (
	"context"

	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
)

const (
	minWidth  = 60
	minHeight = 10
	pageSize  = 50
)

type Model struct {
	deps       deps.Deps
	width      int
	height     int
	projectKey string
	title      string

	issues      []domain.IssueSummary
	cursor      int
	page        domain.IssuePage // metadata of the most recently loaded page (hasNext/totals)
	loading     bool             // first page in flight
	loadingMore bool             // a subsequent page in flight
	loadErr     bool
	reqGen      int // bumped on reload so a superseded in-flight load is ignored when it lands
}

func New(d deps.Deps, projectKey, title string) Model {
	return Model{deps: d, projectKey: projectKey, title: title, loading: true}
}

func (m Model) Init() tea.Cmd { return loadIssues(m.deps, m.projectKey, m.reqGen, 0, false) }

func (m Model) Retheme(d deps.Deps) Model {
	m.deps = d
	return m
}

// false for now — no modal yet, so the shell keeps its keys.
func (m Model) CapturingInput() bool { return false }

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		return m, nil

	case issuesLoadedMsg:
		if msg.key != m.projectKey || msg.gen != m.reqGen {
			return m, nil // a stale load from a previous project or a superseded reload
		}
		m.loading, m.loadingMore = false, false
		if msg.err {
			// a fresh-load failure replaces the body. An append (load-more) failure keeps the pages
			// already loaded and leaves page.HasNext set, so scrolling down retries.
			if !msg.append {
				m.loadErr = true
			}
			return m, nil
		}
		m.loadErr = false
		m.page = msg.page
		if msg.append {
			m.issues = append(m.issues, msg.page.Issues...)
		} else {
			m.issues = msg.page.Issues
			m.cursor = 0
		}
		if m.cursor >= len(m.issues) {
			m.cursor = max(0, len(m.issues)-1)
		}
		return m, nil

	case tea.KeyPressMsg:
		return m.onKey(msg)
	case tea.MouseClickMsg:
		return m.onClick(msg)
	case tea.MouseWheelMsg:
		switch msg.Button {
		case tea.MouseWheelUp:
			return m.moveCursor(-1)
		case tea.MouseWheelDown:
			return m.moveCursor(1)
		}
	}
	return m, nil
}

func (m Model) onKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "esc", "q", "backspace":
		return m, back
	case "up", "k":
		return m.moveCursor(-1)
	case "down", "j":
		return m.moveCursor(1)
	case "home", "g":
		m.cursor = 0
		return m, nil
	case "end", "G":
		m.cursor = max(0, len(m.issues)-1)
		return m, nil
	case "r":
		m.loading, m.loadErr, m.loadingMore = true, false, false
		m.reqGen++ // supersede any in-flight load so its late result is ignored
		return m, loadIssues(m.deps, m.projectKey, m.reqGen, 0, false)
	}
	return m, nil
}

// When the cursor reaches the end of the loaded issues with more pages available, requests the next
// page (appended).
func (m Model) moveCursor(delta int) (Model, tea.Cmd) {
	next := m.cursor + delta
	if next < 0 {
		next = 0
	}
	if next >= len(m.issues) {
		next = max(0, len(m.issues)-1)
		if m.page.HasNext && !m.loadingMore && len(m.issues) > 0 {
			m.loadingMore = true
			return m, loadIssues(m.deps, m.projectKey, m.reqGen, m.page.Page+1, true)
		}
	}
	m.cursor = next
	return m, nil
}

func (m Model) onClick(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	if zone.Get(zoneBack).InBounds(msg) {
		return m, back
	}
	for i := range m.issues {
		if zone.Get(issueRowZone(i)).InBounds(msg) {
			m.cursor = i
			return m, nil
		}
	}
	return m, nil
}

func (m Model) HelpKeys() []key.Binding {
	return []key.Binding{
		key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
		key.NewBinding(key.WithKeys("r"), key.WithHelp("r", "reload")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
	}
}

func max(a, b int) int {
	if a > b {
		return a
	}
	return b
}

type issuesLoadedMsg struct {
	key    string // the project the load was for, so a stale cross-project result is ignored
	gen    int    // the request generation, so a superseded reload's late result is ignored
	page   domain.IssuePage
	append bool
	err    bool
}

func back() tea.Msg { return nav.CloseProjectMsg{} }

func loadIssues(d deps.Deps, projectKey string, gen, page int, appendPage bool) tea.Cmd {
	return func() tea.Msg {
		p, err := d.Issues.SearchProjectIssues(context.Background(), projectKey, "", page, pageSize)
		return issuesLoadedMsg{key: projectKey, gen: gen, page: p, append: appendPage, err: err != nil}
	}
}
