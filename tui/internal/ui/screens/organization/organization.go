// Package organization is the global team and position catalog tab.
// This slice is read-only; admin editing lands later.
package organization

import (
	"context"
	"fmt"
	"strings"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const (
	inset     = 2  // blank columns on each side of the content
	colGap    = 2  // gap between the two catalog columns
	minWidth  = 56 // below this the two columns cannot fit
	minHeight = 8
)

type Model struct {
	deps deps.Deps

	loading   bool
	err       error
	teams     []domain.TeamSummary
	positions []domain.PositionSummary

	width  int
	height int
}

// New builds the Organization tab in its loading state.
func New(d deps.Deps) Model { return Model{deps: d, loading: true} }

func (m Model) Init() tea.Cmd { return load(m.deps) }

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
	case LoadedMsg:
		m.loading = false
		m.err = msg.Err
		m.teams, m.positions = msg.Teams, msg.Positions
	}
	return m, nil
}

func (m Model) View() string {
	if m.width == 0 {
		return ""
	}
	s := m.deps.Styles
	if m.width < minWidth || m.height < minHeight {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Muted.Render("Terminal too small"))
	}
	if m.loading {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Muted.Render("Loading catalogs…"))
	}
	if m.err != nil {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Error.Render("Failed to load catalogs."))
	}

	innerW := m.width - 2*inset
	leftW := (innerW - colGap) / 2
	rightW := innerW - colGap - leftW

	left := m.column(fmt.Sprintf("Teams (%d)", len(m.teams)), m.teamRows(), leftW, m.height)
	right := m.column(fmt.Sprintf("Positions (%d)", len(m.positions)), m.positionRows(), rightW, m.height)

	row := lipgloss.JoinHorizontal(lipgloss.Top, left, strings.Repeat(" ", colGap), right)
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, row)
}

// column renders one catalog as a titled box of the given outer size, clipping the rows
// to the body height with a "+N more" marker.
func (m Model) column(title string, rows []string, w, h int) string {
	s := m.deps.Styles
	contentW := w - 4 // border(2) + inset(2)
	bodyH := h - 2    // top and bottom border
	if bodyH < 1 {
		bodyH = 1
	}
	if len(rows) > bodyH {
		rows = append(rows[:bodyH-1:bodyH-1], s.Muted.Render(fmt.Sprintf("… +%d more", len(rows)-(bodyH-1))))
	}
	lines := make([]string, bodyH)
	for i := range lines {
		if i < len(rows) {
			// truncate first: lipgloss Width() wraps, which would push the box past bodyH
			lines[i] = lipgloss.NewStyle().Width(contentW).Render(ansi.Truncate(rows[i], contentW, "…"))
		} else {
			lines[i] = strings.Repeat(" ", contentW)
		}
	}
	return components.TitledBox(title, strings.Join(lines, "\n"), s.Theme.Primary)
}

func (m Model) teamRows() []string {
	s := m.deps.Styles
	if len(m.teams) == 0 {
		return []string{s.Muted.Render("No teams.")}
	}
	return catalogRows(s, len(m.teams), func(i int) (string, string) {
		return m.teams[i].Name, m.teams[i].Description
	})
}

func (m Model) positionRows() []string {
	s := m.deps.Styles
	if len(m.positions) == 0 {
		return []string{s.Muted.Render("No positions.")}
	}
	return catalogRows(s, len(m.positions), func(i int) (string, string) {
		return m.positions[i].Name, m.positions[i].Description
	})
}

// catalogRows renders "name · description" rows for a simple name/description catalog.
func catalogRows(s theme.Styles, n int, at func(int) (name, desc string)) []string {
	name := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true)
	rows := make([]string, 0, n)
	for i := 0; i < n; i++ {
		nm, desc := at(i)
		meta := ""
		if desc != "" {
			meta = " · " + desc
		}
		rows = append(rows, name.Render(nm)+s.Muted.Render(meta))
	}
	return rows
}

// LoadedMsg carries the catalog fetch result. The app shell routes it to this screen
// even while another tab is active, so a background load never gets lost.
type LoadedMsg struct {
	Teams     []domain.TeamSummary
	Positions []domain.PositionSummary
	Err       error
}

func load(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		ctx := context.Background()
		teams, err := d.Catalog.ListTeams(ctx)
		if err != nil {
			return LoadedMsg{Err: err}
		}
		positions, err := d.Catalog.ListPositions(ctx)
		if err != nil {
			return LoadedMsg{Err: err}
		}
		return LoadedMsg{Teams: teams, Positions: positions}
	}
}
