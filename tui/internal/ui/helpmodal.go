package ui

import (
	"fmt"
	"strings"

	"charm.land/bubbles/v2/help"
	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/viewport"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// helpModalWidth caps the box so long descriptions do not sprawl across a wide terminal.
const helpModalWidth = 72

// helpModal is a read-only overlay: what the active screen is for, plus every shortcut available on
// it. The shortcut list is the screen's own live key bindings (the same ones the footer draws), so
// it can never drift. Content taller than the terminal scrolls in a viewport.
type helpModal struct {
	theme  theme.Theme
	help   help.Model
	vp     viewport.Model
	title  string
	about  string
	global []key.Binding
	screen []key.Binding
	scroll bool // content overflowed and the viewport is scrollable
}

func newHelpModal(t theme.Theme, h help.Model, title, about string, global, screen []key.Binding, termW, termH int) helpModal {
	m := helpModal{theme: t, help: h, title: title, about: about, global: global, screen: screen, vp: viewport.New()}
	return m.layout(termW, termH)
}

func (m helpModal) layout(termW, termH int) helpModal {
	boxW := helpModalWidth
	if boxW > termW-8 {
		boxW = termW - 8
	}
	if boxW < 30 {
		boxW = 30
	}
	// per side: border cell + TitledBox inset + the extra padding below = 3, so 6 across
	contentW := boxW - 6
	if contentW < 10 {
		contentW = 10
	}
	m.help.SetWidth(contentW)

	content := m.content(contentW)
	// leave room for the box border (2), the padding (2), the footer hint block (2), and margins (4)
	maxH := termH - 10
	if maxH < 3 {
		maxH = 3
	}
	h := lipgloss.Height(content)
	m.scroll = h > maxH
	if m.scroll {
		h = maxH
	}
	m.vp.SetWidth(contentW)
	m.vp.SetHeight(h)
	m.vp.SetContent(content)
	m.vp.SetYOffset(m.vp.YOffset()) // re-clamp the offset against the new height
	return m
}

func (m helpModal) content(width int) string {
	parts := make([]string, 0, 3)
	if m.about != "" {
		parts = append(parts, lipgloss.NewStyle().Foreground(m.theme.Muted).Width(width).Render(m.about))
	}
	if len(m.global) > 0 {
		parts = append(parts, m.section("Global", m.global))
	}
	if len(m.screen) > 0 {
		parts = append(parts, m.section(m.title, m.screen))
	}
	return strings.Join(parts, "\n\n")
}

// FullHelpView of a single group renders one column of "key  desc" rows.
func (m helpModal) section(label string, binds []key.Binding) string {
	head := lipgloss.NewStyle().Foreground(m.theme.Text).Bold(true).Render(label)
	return head + "\n" + m.help.FullHelpView([][]key.Binding{binds})
}

func (m helpModal) Update(msg tea.Msg) (appModal, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		return m.layout(msg.Width, msg.Height), nil
	case tea.KeyPressMsg:
		switch msg.String() {
		case "esc", "?", "q":
			return m, closeModal
		}
	}
	var cmd tea.Cmd
	m.vp, cmd = m.vp.Update(msg)
	return m, cmd
}

func (m helpModal) View() string {
	hint := "esc close"
	if m.scroll {
		hint = fmt.Sprintf("↑/↓ scroll · %d%% · %s", int(m.vp.ScrollPercent()*100), hint)
	}
	footer := lipgloss.NewStyle().Foreground(m.theme.Muted).Render(hint)
	body := lipgloss.NewStyle().Padding(1).Render(lipgloss.JoinVertical(lipgloss.Left, m.vp.View(), "", footer))
	return components.TitledBoxCentered(m.title+" · Help", body, m.theme.Accent)
}

func (m helpModal) HelpKeys() []key.Binding {
	binds := make([]key.Binding, 0, 2)
	if m.scroll {
		binds = append(binds, key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "scroll")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "close")))
}
