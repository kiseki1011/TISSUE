// Package toast provides a bottom-right notification stack, rendered and expired by the app shell.
package toast

import (
	"image/color"
	"time"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

type Level int

const (
	Info Level = iota
	Success
	Warning
	Error
)

const ttl = 4 * time.Second

// textWidth is fixed so every toast in the stack lines up to the same width.
const textWidth = 32

// maxVisible caps the stack so a burst of toasts cannot fill the screen. The oldest scroll off.
const maxVisible = 4

type ShowMsg struct {
	Level Level
	Text  string
}

// ExpireMsg is exported so the shell can route it back into the stack's Update.
type ExpireMsg struct{ ID int }

func Show(level Level, text string) tea.Cmd {
	return func() tea.Msg { return ShowMsg{Level: level, Text: text} }
}

type item struct {
	id    int
	level Level
	text  string
}

type Model struct {
	theme  theme.Theme
	glyphs glyph.Set
	items  []item
	seq    int
}

func New(t theme.Theme, g glyph.Set) Model {
	return Model{theme: t, glyphs: g}
}

// Update ignores every other message, so the shell can forward these two types blindly.
func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case ShowMsg:
		return m.push(msg.Level, msg.Text)
	case ExpireMsg:
		m.items = m.remove(msg.ID)
		return m, nil
	}
	return m, nil
}

func (m Model) push(level Level, text string) (Model, tea.Cmd) {
	m.seq++
	id := m.seq
	// copy-on-append so the value receiver never mutates a shared backing array
	items := append(append([]item(nil), m.items...), item{id: id, level: level, text: text})
	if len(items) > maxVisible {
		items = items[len(items)-maxVisible:]
	}
	m.items = items
	return m, tea.Tick(ttl, func(time.Time) tea.Msg { return ExpireMsg{ID: id} })
}

func (m Model) remove(id int) []item {
	out := make([]item, 0, len(m.items))
	for _, it := range m.items {
		if it.id != id {
			out = append(out, it)
		}
	}
	return out
}

func (m Model) Empty() bool { return len(m.items) == 0 }

func (m Model) Retheme(t theme.Theme) Model {
	m.theme = t
	return m
}

func (m Model) Reglyph(g glyph.Set) Model {
	m.glyphs = g
	return m
}

// View stacks the toasts right-aligned, newest at the bottom, or "" when empty.
func (m Model) View() string {
	if len(m.items) == 0 {
		return ""
	}
	boxes := make([]string, 0, len(m.items)*2)
	for i, it := range m.items {
		if i > 0 {
			boxes = append(boxes, "") // a blank gutter between stacked toasts
		}
		boxes = append(boxes, m.box(it))
	}
	return lipgloss.JoinVertical(lipgloss.Right, boxes...)
}

func (m Model) box(it item) string {
	c := m.color(it.level)
	title := lipgloss.NewStyle().Foreground(c).Bold(true).Render(m.glyph(it.level) + " " + m.label(it.level))
	text := lipgloss.NewStyle().Foreground(m.theme.Text).Width(textWidth).Render(it.text)
	body := lipgloss.JoinVertical(lipgloss.Left, title, text)
	return components.TitledBox("", body, c)
}

func (m Model) color(l Level) color.Color {
	switch l {
	case Success:
		return m.theme.Success
	case Warning:
		return m.theme.Warning
	case Error:
		return m.theme.Error
	default:
		return m.theme.Primary
	}
}

func (m Model) glyph(l Level) string {
	g := m.glyphs
	switch l {
	case Success:
		return g.Or(g.Check, "✓")
	case Warning:
		return g.Or(g.Warning, "!")
	case Error:
		return g.Or(g.Cross, "✗")
	default:
		return g.Or(g.Bell, "i")
	}
}

func (m Model) label(l Level) string {
	switch l {
	case Success:
		return "Success"
	case Warning:
		return "Warning"
	case Error:
		return "Error"
	default:
		return "Info"
	}
}
