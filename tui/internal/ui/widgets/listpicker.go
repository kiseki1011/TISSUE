package widgets

import (
	"strconv"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// PickerOption is one selectable row of a ListPicker: Value is applied on select, Label is shown, and
// Lead is optional pre-rendered decoration before the label, such as a color swatch.
type PickerOption struct {
	Value string
	Label string
	Lead  string
}

// ListPicker is a single-select dropdown, the list counterpart to ColorPicker (used inside a modal
// to choose one value instead of cycling with ←/→). Cursor is exported so a host's click handler
// can set it directly after a HitOption.
type ListPicker struct {
	title   string
	options []PickerOption
	Cursor  int
	maxRows int
	width   int
}

func NewListPicker(title string, opts []PickerOption, current string, maxRows, width int) ListPicker {
	p := ListPicker{title: title, options: opts, maxRows: maxRows, width: width}
	for i, o := range opts {
		if o.Value == current {
			p.Cursor = i
		}
	}
	return p
}

func (p ListPicker) Move(delta int) ListPicker {
	if n := len(p.options); n > 0 {
		p.Cursor = (p.Cursor + delta + n) % n
	}
	return p
}

func (p ListPicker) Selected() (PickerOption, bool) {
	if p.Cursor < 0 || p.Cursor >= len(p.options) {
		return PickerOption{}, false
	}
	return p.options[p.Cursor], true
}

func (p ListPicker) top() int {
	if len(p.options) <= p.maxRows {
		return 0
	}
	top := p.Cursor - p.maxRows/2
	if top < 0 {
		top = 0
	}
	if hi := len(p.options) - p.maxRows; top > hi {
		top = hi
	}
	return top
}

// ListPickerOptZone is exported so a host (or its tests) can resolve a click's coordinates to a row.
func ListPickerOptZone(i int) string { return "widgets.listpicker.opt." + strconv.Itoa(i) }

func (p ListPicker) HitOption(msg tea.MouseMsg) int {
	visible := min(p.maxRows, len(p.options))
	top := p.top()
	for i := top; i < top+visible; i++ {
		if z := zone.Get(ListPickerOptZone(i)); z != nil && z.InBounds(msg) {
			return i
		}
	}
	return -1
}

func (p ListPicker) View(s theme.Styles) string {
	t := s.Theme
	n := len(p.options)
	visible := min(p.maxRows, n)
	top := p.top()

	lines := make([]string, 0, visible+1)
	for i := top; i < top+visible; i++ {
		o := p.options[i]
		text := o.Label
		if o.Lead != "" {
			text = o.Lead + " " + o.Label
		}
		marker, style := "  ", lipgloss.NewStyle().Foreground(t.Text)
		if i == p.Cursor {
			marker = lipgloss.NewStyle().Foreground(t.Accent).Render("▸ ")
			style = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		}
		row := marker + style.Render(text)
		lines = append(lines, zone.Mark(ListPickerOptZone(i), components.FitLine(row, p.width)))
	}
	if n > visible { // position indicator when the list scrolls
		lines = append(lines, s.Muted.Render(components.FitLine("  "+strconv.Itoa(p.Cursor+1)+"/"+strconv.Itoa(n), p.width)))
	}
	body := lipgloss.JoinVertical(lipgloss.Left, lines...)
	return components.TitledBoxCentered(p.title, body, t.Primary)
}
