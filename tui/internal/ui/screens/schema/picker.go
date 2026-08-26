package schema

import (
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

type pickerOption struct {
	value string
	label string
	lead  string // optional decoration before the label, like a color swatch
}

// picker is a single-select dropdown used inside a modal instead of cycling options with ←/→.
type picker struct {
	title   string
	options []pickerOption
	cursor  int
	maxRows int
	width   int
}

func newPicker(title string, opts []pickerOption, current string, maxRows, width int) picker {
	p := picker{title: title, options: opts, maxRows: maxRows, width: width}
	for i, o := range opts {
		if o.value == current {
			p.cursor = i
		}
	}
	return p
}

func (p picker) move(delta int) picker {
	if n := len(p.options); n > 0 {
		p.cursor = (p.cursor + delta + n) % n
	}
	return p
}

func (p picker) selected() (pickerOption, bool) {
	if p.cursor < 0 || p.cursor >= len(p.options) {
		return pickerOption{}, false
	}
	return p.options[p.cursor], true
}

// top is the first visible option index, keeping the cursor within the window.
func (p picker) top() int {
	if len(p.options) <= p.maxRows {
		return 0
	}
	top := p.cursor - p.maxRows/2
	if top < 0 {
		top = 0
	}
	if hi := len(p.options) - p.maxRows; top > hi {
		top = hi
	}
	return top
}

func pickerOptZone(i int) string { return "picker.opt." + itoa(i) }

func (p picker) hitOption(msg tea.MouseMsg) int {
	visible := min(p.maxRows, len(p.options))
	top := p.top()
	for i := top; i < top+visible; i++ {
		if zone.Get(pickerOptZone(i)).InBounds(msg) {
			return i
		}
	}
	return -1
}

func (p picker) View(s theme.Styles) string {
	t := s.Theme
	n := len(p.options)
	visible := min(p.maxRows, n)
	top := p.top()

	lines := make([]string, 0, visible+1)
	for i := top; i < top+visible; i++ {
		o := p.options[i]
		text := o.label
		if o.lead != "" {
			text = o.lead + " " + o.label
		}
		marker, style := "  ", lipgloss.NewStyle().Foreground(t.Text)
		if i == p.cursor {
			marker = lipgloss.NewStyle().Foreground(t.Accent).Render("▸ ")
			style = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		}
		row := marker + style.Render(text)
		lines = append(lines, zone.Mark(pickerOptZone(i), fitLine(row, p.width)))
	}
	if n > visible { // position indicator when the list scrolls
		lines = append(lines, s.Muted.Render(fitLine("  "+itoa(p.cursor+1)+"/"+itoa(n), p.width)))
	}
	body := lipgloss.JoinVertical(lipgloss.Left, lines...)
	return components.TitledBoxCentered(p.title, body, t.Primary)
}
