package inbox

import (
	"context"
	"strconv"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// openPrefs reloads the settings on every open, so a change made elsewhere is reflected.
func (m Model) openPrefs() (Model, tea.Cmd) {
	m.prefsOpen = true
	m.prefsLoading = true
	m.prefsErr = false
	m.prefsCursor = 0
	return m, loadPrefs(m.deps)
}

func (m Model) closePrefs() (Model, tea.Cmd) {
	m.prefsOpen = false
	return m, nil
}

func (m Model) onPrefsKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "esc", "p":
		return m.closePrefs()
	case "up", "k":
		m.prefsCursor = clamp(m.prefsCursor-1, 0, len(m.prefsRows)-1)
	case "down", "j":
		m.prefsCursor = clamp(m.prefsCursor+1, 0, len(m.prefsRows)-1)
	case "home", "g":
		m.prefsCursor = 0
	case "end", "G":
		m.prefsCursor = max(0, len(m.prefsRows)-1)
	case "space", "enter":
		return m.togglePref()
	}
	return m, nil
}

func (m Model) togglePref() (Model, tea.Cmd) {
	if m.prefsCursor < 0 || m.prefsCursor >= len(m.prefsRows) {
		return m, nil
	}
	row := m.prefsRows[m.prefsCursor]
	next := !row.Enabled
	m.prefsRows[m.prefsCursor].Enabled = next // optimistic (shared slice backing array persists)
	return m, savePref(m.deps, row.Type, row.Channel, next)
}

// emailPrefs keeps only EMAIL-channel rows (the only channel today), in the backend's order.
func emailPrefs(rows []domain.NotificationPref) []domain.NotificationPref {
	out := make([]domain.NotificationPref, 0, len(rows))
	for _, r := range rows {
		if r.Channel == domain.ChannelEmail {
			out = append(out, r)
		}
	}
	return out
}

type prefsLoadedMsg struct {
	rows []domain.NotificationPref
	err  bool
}

// prefsSaveFailedMsg reverts the optimistic toggle for one type when the server rejects it.
type prefsSaveFailedMsg struct {
	notifType string
	prev      bool
}

func loadPrefs(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		rows, err := d.Notifications.GetPreferences(context.Background())
		return prefsLoadedMsg{rows: rows, err: err != nil}
	}
}

func savePref(d deps.Deps, notifType, channel string, enabled bool) tea.Cmd {
	return func() tea.Msg {
		if err := d.Notifications.UpdatePreference(context.Background(), notifType, channel, enabled); err != nil {
			return prefsSaveFailedMsg{notifType: notifType, prev: !enabled}
		}
		return nil // the optimistic toggle already reflects the new state
	}
}

const prefsModalW = 44

func (m Model) prefsModalView() string {
	t := m.deps.Styles.Theme
	title := lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render("Email notifications")
	sub := lipgloss.NewStyle().Foreground(t.Muted).Width(prefsModalW).Render(
		"Choose which events email you. The in-app inbox always keeps every notification.")
	var body string
	switch {
	case m.prefsLoading:
		body = lipgloss.NewStyle().Foreground(t.Muted).Render("Loading…")
	case m.prefsErr:
		body = lipgloss.NewStyle().Foreground(t.Error).Render("Failed to load settings.")
	default:
		body = m.prefsRowsView()
	}
	help := lipgloss.NewStyle().Foreground(t.Muted).Render("space toggle · esc close")
	content := lipgloss.JoinVertical(lipgloss.Left, title, sub, "", body, "", help)
	return lipgloss.NewStyle().Border(lipgloss.RoundedBorder()).BorderForeground(t.Border).Padding(1, 2).Render(content)
}

func (m Model) prefsRowsView() string {
	t := m.deps.Styles.Theme
	// 12 = border, padding, title, sub and help rows
	avail := max(1, m.height-12)
	visible := max(1, min(len(m.prefsRows), avail))
	top := listTop(m.prefsCursor, visible, len(m.prefsRows))
	lines := make([]string, 0, visible+1)
	for i := top; i < len(m.prefsRows) && i < top+visible; i++ {
		lines = append(lines, m.prefsRow(m.prefsRows[i], i))
	}
	if visible < len(m.prefsRows) {
		lines = append(lines, lipgloss.NewStyle().Foreground(t.Muted).Render(
			"  "+strconv.Itoa(m.prefsCursor+1)+"/"+strconv.Itoa(len(m.prefsRows))))
	}
	return lipgloss.JoinVertical(lipgloss.Left, lines...)
}

func (m Model) prefsRow(p domain.NotificationPref, i int) string {
	t := m.deps.Styles.Theme
	box := lipgloss.NewStyle().Foreground(t.Muted).Render("[ ]")
	if p.Enabled {
		box = lipgloss.NewStyle().Foreground(t.Success).Render("[x]")
	}
	nameStyle := lipgloss.NewStyle().Foreground(t.Text)
	if i == m.prefsCursor {
		nameStyle = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	}
	return box + " " + nameStyle.Render(fit(domain.HumanizeNotificationType(p.Type), prefsModalW-4))
}

func (m Model) overlayModal(backdrop, modal string) string {
	t := m.deps.Styles.Theme
	bd := components.StripANSI(lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Top, backdrop))
	mx := max(0, (m.width-lipgloss.Width(modal))/2)
	my := max(0, (m.height-lipgloss.Height(modal))/2)
	return components.OverlayDim(bd, modal, mx, my, t.Muted)
}
