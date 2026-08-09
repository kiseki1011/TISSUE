package widgets

import (
	"image/color"
	"strconv"
	"strings"

	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// PickerOption is one selectable row of a ListPicker: Value is applied on select, Label is shown, and
// Lead is optional pre-rendered decoration before the label, such as a color swatch. Color, when set,
// overrides the label's foreground (e.g. a destructive "Unassigned" shown in the error color).
type PickerOption struct {
	Value string
	Label string
	Lead  string
	Color color.Color
	// Notes are caveats printed under the label in the theme's warning color - what stands between the
	// user and this option (the guards a blocked workflow transition fails). They are part of the row:
	// a click anywhere in the block still selects the option, and long notes wrap rather than truncate.
	Notes []string
}

// ListPicker is a single-select dropdown, the list counterpart to ColorPicker (used inside a modal
// to choose one value instead of cycling with ←/→). Cursor is exported so a host's click handler
// can set it directly after a HitOption. When searchable, a "> " filter line sits above the rows and
// narrows them by a case-insensitive substring match.
type ListPicker struct {
	title      string
	all        []PickerOption // the full option set (searchable filters this into options)
	options    []PickerOption // the currently shown (filtered) options
	Cursor     int
	maxRows    int // a budget of body lines; without notes that is one option per line, as before
	width      int
	searchable bool
	search     textinput.Model
	checked    map[string]bool // non-nil switches on multi-select: space toggles, enter confirms Selections()
}

func NewListPicker(title string, opts []PickerOption, current string, maxRows, width int) ListPicker {
	p := ListPicker{title: title, all: opts, options: opts, maxRows: maxRows, width: width}
	p.setCurrent(current)
	return p
}

// NewSearchableListPicker is a ListPicker with a "> " filter line, for a long option set (project
// members). Typing narrows the list; arrows navigate the matches.
func NewSearchableListPicker(title string, opts []PickerOption, current string, maxRows, width int) ListPicker {
	ti := textinput.New()
	ti.Prompt = "> "
	ti.Placeholder = "type to filter"
	ti.SetWidth(max(4, width-4)) // room for the "> " prompt and a right margin inside the box
	ti.Focus()
	p := ListPicker{title: title, all: opts, options: opts, maxRows: maxRows, width: width, searchable: true, search: ti}
	p.setCurrent(current)
	return p
}

// NewMultiListPicker is a searchable multi-select: space toggles the row under the cursor (tracked in a
// checked set), enter confirms the whole selection via Selections(). preChecked seeds the initial set.
func NewMultiListPicker(title string, opts []PickerOption, preChecked []string, maxRows, width int) ListPicker {
	ti := textinput.New()
	ti.Prompt = "> "
	ti.Placeholder = "type to filter"
	ti.SetWidth(max(4, width-4))
	ti.Focus()
	checked := make(map[string]bool, len(preChecked))
	for _, v := range preChecked {
		checked[v] = true
	}
	p := ListPicker{title: title, all: opts, options: opts, maxRows: maxRows, width: width, searchable: true, search: ti, checked: checked}
	return p
}

// Multi reports whether this is a multi-select picker (space toggles rows, enter confirms Selections).
func (p ListPicker) Multi() bool { return p.checked != nil }

// Toggle flips the checked state of the option under the cursor. It copies the checked set so the
// value-receiver returns an independent picker (Elm semantics); a no-op on a single-select picker.
func (p ListPicker) Toggle() ListPicker {
	o, ok := p.Selected()
	if p.checked == nil || !ok {
		return p
	}
	next := make(map[string]bool, len(p.checked))
	for k, v := range p.checked {
		next[k] = v
	}
	next[o.Value] = !next[o.Value]
	p.checked = next
	return p
}

// Selections are the checked option values, the in-option ones first in the picker's defined order; nil
// on a single-select picker. A checked value with no matching option (e.g. pre-checked but absent from
// the option set) is still returned, so a multi-select never silently drops a selection it cannot render.
func (p ListPicker) Selections() []string {
	if p.checked == nil {
		return nil
	}
	seen := make(map[string]bool, len(p.checked))
	out := make([]string, 0, len(p.checked))
	for _, o := range p.all {
		if p.checked[o.Value] {
			out = append(out, o.Value)
			seen[o.Value] = true
		}
	}
	for v, on := range p.checked {
		if on && !seen[v] {
			out = append(out, v)
		}
	}
	return out
}

func (p *ListPicker) setCurrent(current string) {
	for i, o := range p.options {
		if o.Value == current {
			p.Cursor = i
			return
		}
	}
}

func (p ListPicker) Searchable() bool { return p.searchable }

func (p ListPicker) Move(delta int) ListPicker {
	if n := len(p.options); n > 0 {
		p.Cursor = (p.Cursor + delta + n) % n
	}
	return p
}

// Filter feeds a key to the search box and re-narrows the options. The cursor returns to the top
// match. A no-op on a non-searchable picker.
func (p ListPicker) Filter(msg tea.KeyPressMsg) ListPicker {
	if !p.searchable {
		return p
	}
	p.search, _ = p.search.Update(msg)
	query := strings.ToLower(strings.TrimSpace(p.search.Value()))
	if query == "" {
		p.options = p.all
	} else {
		filtered := make([]PickerOption, 0, len(p.all))
		for _, o := range p.all {
			if strings.Contains(strings.ToLower(o.Label), query) {
				filtered = append(filtered, o)
			}
		}
		p.options = filtered
	}
	p.Cursor = 0
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

const (
	// noteIndent is the columns a note is inset by, lining it up under the label rather than the cursor
	// marker, so the notes read as belonging to the row above them.
	noteIndent = 4
	// maxNoteRows caps one option's notes so a single pathological message cannot fill the box. The
	// window budget bounds the options it shows, but the option under the cursor is always drawn whole.
	maxNoteRows = 8
)

// ListPickerOptZone is exported so a host (or its tests) can resolve a click's coordinates to a row.
func ListPickerOptZone(i int) string { return "widgets.listpicker.opt." + strconv.Itoa(i) }

func (p ListPicker) HitOption(msg tea.MouseMsg) int {
	top, visible := p.window()
	for i := top; i < top+visible; i++ {
		if z := zone.Get(ListPickerOptZone(i)); z != nil && z.InBounds(msg) {
			return i
		}
	}
	return -1
}

// noteRows are an option's notes wrapped to the box width, one entry per line that will be drawn. It is
// the single source of an option's height, so what View renders and what the scroll maths budgeted for
// can never drift apart.
func (p ListPicker) noteRows(o PickerOption) []string {
	if len(o.Notes) == 0 {
		return nil
	}
	var out []string
	for _, note := range o.Notes {
		// a note is server text, so flatten it before wrapping rather than letting a stray newline
		// smuggle in a row nobody counted
		for _, line := range wrapNote(components.Flatten(note), max(1, p.width-noteIndent)) {
			if len(out) == maxNoteRows {
				out[len(out)-1] += "…"
				return out
			}
			out = append(out, line)
		}
	}
	return out
}

// wrapNote breaks a note onto lines no wider than w. The trailing pad lipgloss adds is stripped so the
// caller can color the text without painting a warning-colored bar across the empty right side.
func wrapNote(s string, w int) []string {
	lines := strings.Split(lipgloss.NewStyle().Width(w).Render(s), "\n")
	for i, line := range lines {
		lines[i] = strings.TrimRight(line, " ")
	}
	return lines
}

// hasNotes reports whether any shown option carries notes, which is what decides between the plain
// one-line-per-option layout and the taller one.
func (p ListPicker) hasNotes() bool {
	for _, o := range p.options {
		if len(o.Notes) > 0 {
			return true
		}
	}
	return false
}

// noteRowsFor is an option's note rows bounded by the line budget. window() always draws the option
// under the cursor whole, so without this one heavily-annotated option could push the box past the
// height its host floated it into - exactly what the budget exists to prevent.
func (p ListPicker) noteRowsFor(i int) []string {
	rows := p.noteRows(p.options[i])
	limit := max(0, p.maxRows-1)
	if len(rows) <= limit {
		return rows
	}
	rows = rows[:limit]
	if n := len(rows); n > 0 && !strings.HasSuffix(rows[n-1], "…") {
		rows[n-1] += "…"
	}
	return rows
}

func (p ListPicker) optionHeight(i int) int { return 1 + len(p.noteRowsFor(i)) }

// window is the run of options View draws and HitOption tests, as (top, count). Without notes an option
// is one line and this is the old min(maxRows, n) window unchanged. With notes it grows outward from the
// cursor until the next option would not fit the line budget, so the highlighted row stays on screen and
// the box stays inside the frame it is floated over.
func (p ListPicker) window() (int, int) {
	n := len(p.options)
	if n == 0 {
		return 0, 0
	}
	if !p.hasNotes() {
		return p.top(), min(p.maxRows, n)
	}
	cur := min(max(p.Cursor, 0), n-1)
	top, bottom, used := cur, cur, p.optionHeight(cur)
	for {
		grew := false
		if bottom+1 < n && used+p.optionHeight(bottom+1) <= p.maxRows {
			bottom++
			used += p.optionHeight(bottom)
			grew = true
		}
		if top > 0 && used+p.optionHeight(top-1) <= p.maxRows {
			top--
			used += p.optionHeight(top)
			grew = true
		}
		if !grew {
			return top, bottom - top + 1
		}
	}
}

func (p ListPicker) View(s theme.Styles) string {
	t := s.Theme
	n := len(p.options)
	top, visible := p.window()

	lines := make([]string, 0, visible+2)
	if p.searchable {
		lines = append(lines, components.FitLine(p.search.View(), p.width))
	}
	if n == 0 {
		lines = append(lines, s.Muted.Render(components.FitLine("  no matches", p.width)))
	}
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
		if o.Color != nil { // an explicitly-colored option keeps its color (the marker still shows the cursor)
			style = lipgloss.NewStyle().Foreground(o.Color).Bold(i == p.Cursor)
		}
		box := ""
		if p.checked != nil { // multi-select: a checkbox before each label
			if p.checked[o.Value] {
				box = lipgloss.NewStyle().Foreground(t.Success).Render("[x] ")
			} else {
				box = lipgloss.NewStyle().Foreground(t.Muted).Render("[ ] ")
			}
		}
		row := marker + box + style.Render(text)
		block := []string{components.FitLine(row, p.width)}
		note := lipgloss.NewStyle().Foreground(t.Warning)
		indent := strings.Repeat(" ", noteIndent)
		for _, line := range p.noteRowsFor(i) {
			block = append(block, components.FitLine(indent+note.Render(line), p.width))
		}
		// one zone for the whole block, so a click on a note picks the option it explains
		lines = append(lines, zone.Mark(ListPickerOptZone(i), lipgloss.JoinVertical(lipgloss.Left, block...)))
	}
	if n > visible { // position indicator when the list scrolls
		lines = append(lines, s.Muted.Render(components.FitLine("  "+strconv.Itoa(p.Cursor+1)+"/"+strconv.Itoa(n), p.width)))
	}
	body := lipgloss.JoinVertical(lipgloss.Left, lines...)
	return components.TitledBoxCentered(p.title, body, t.Primary)
}
