// Package toast provides a bottom-right notification stack, rendered and expired by the app shell.
// Any screen raises one by returning toast.Show(level, text) as a tea.Cmd; the shell owns the
// stack and its auto-dismiss timers, so screens stay unaware of where or how long toasts appear.
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

// Level is the severity of a toast, driving its color and icon.
type Level int

const (
	Info Level = iota
	Success
	Warning
	Error
)

// ttl is how long a toast stays before auto-dismissing.
const ttl = 4 * time.Second

// textWidth is the fixed inner text column of a toast; the border adds one inset and one rule
// on each side, so every toast in the stack lines up to the same width.
const textWidth = 32

// maxVisible caps the stack so a burst of toasts cannot fill the screen; the oldest scroll off.
const maxVisible = 4

// ShowMsg asks the shell to raise a toast. Screens emit it via Show.
type ShowMsg struct {
	Level Level
	Text  string
}

// ExpireMsg retires the toast with the given id once its ttl elapses. It is exported so the shell
// can route it back into the stack's Update.
type ExpireMsg struct{ ID int }

// Show returns a command that raises a toast from any screen.
func Show(level Level, text string) tea.Cmd {
	return func() tea.Msg { return ShowMsg{Level: level, Text: text} }
}

type item struct {
	id    int
	level Level
	text  string
}

// Model is the toast stack, presentation-only state owned by the app shell.
type Model struct {
	theme  theme.Theme
	glyphs glyph.Set
	items  []item
	seq    int
}

// New builds an empty stack bound to the given theme and glyph set.
func New(t theme.Theme, g glyph.Set) Model {
	return Model{theme: t, glyphs: g}
}

// Update handles a ShowMsg (raising a toast, returning its expiry timer) or an ExpireMsg (retiring
// one). It ignores every other message, so the shell can forward these two types blindly.
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

// push appends a toast and returns the tick command that will expire it after the ttl.
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

// remove drops the toast with the given id, returning a fresh slice.
func (m Model) remove(id int) []item {
	out := make([]item, 0, len(m.items))
	for _, it := range m.items {
		if it.id != id {
			out = append(out, it)
		}
	}
	return out
}

// Empty reports whether there is nothing to render, so the shell can skip the overlay entirely.
func (m Model) Empty() bool { return len(m.items) == 0 }

// View stacks the toasts into a right-aligned block (newest at the bottom, nearest the corner),
// or "" when empty. The shell composites the block into the bottom-right of the frame.
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

// box renders one toast: a rounded box tinted to its severity, titled with the severity glyph and
// label, wrapping the message to a fixed width.
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
