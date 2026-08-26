package home

import (
	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

const (
	filterMembers = iota
	filterPrivate
	filterApply
	filterCancel
	filterItemCount
)

type filterForm struct {
	deps deps.Deps

	membersOnly bool
	hidePrivate bool
	focus       int
	hover       int // element under the cursor, -1 when none
}

func newFilterForm(d deps.Deps, membersOnly, hidePrivate bool) filterForm {
	return filterForm{
		deps:        d,
		membersOnly: membersOnly,
		hidePrivate: hidePrivate,
		focus:       filterMembers,
		hover:       -1,
	}
}

func (f filterForm) Init() tea.Cmd { return nil }

func (f filterForm) Update(msg tea.Msg) (filterForm, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.MouseMotionMsg:
		return f.onHover(msg)
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f, nil
}

func (f filterForm) onKey(msg tea.KeyPressMsg) (filterForm, tea.Cmd) {
	switch msg.String() {
	case "esc":
		return f, cancelFilter
	case "tab", "down":
		return f.moveFocus(1), nil
	case "shift+tab", "up":
		return f.moveFocus(-1), nil
	case "space":
		if f.focus == filterMembers || f.focus == filterPrivate {
			return f.toggle(f.focus), nil
		}
	case "enter":
		switch f.focus {
		case filterMembers, filterPrivate:
			return f.toggle(f.focus), nil
		case filterApply:
			return f, f.apply()
		case filterCancel:
			return f, cancelFilter
		}
	}
	return f, nil
}

func (f filterForm) onClick(msg tea.MouseClickMsg) (filterForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return f, nil
	}
	switch el := f.hitZone(msg); el {
	case filterMembers, filterPrivate:
		f.focus = el
		return f.toggle(el), nil
	case filterApply:
		return f, f.apply()
	case filterCancel:
		return f, cancelFilter
	}
	return f, nil
}

func (f filterForm) onHover(msg tea.MouseMotionMsg) (filterForm, tea.Cmd) {
	f.hover = f.hitZone(msg)
	return f, nil
}

func (f filterForm) hitZone(msg tea.MouseMsg) int {
	switch {
	case zone.Get(filterZoneID(filterMembers)).InBounds(msg):
		return filterMembers
	case zone.Get(filterZoneID(filterPrivate)).InBounds(msg):
		return filterPrivate
	case zone.Get("filter.apply").InBounds(msg):
		return filterApply
	case zone.Get("filter.cancel").InBounds(msg):
		return filterCancel
	}
	return -1
}

func (f filterForm) moveFocus(delta int) filterForm {
	f.focus = (f.focus + delta + filterItemCount) % filterItemCount
	return f
}

func (f filterForm) toggle(which int) filterForm {
	switch which {
	case filterMembers:
		f.membersOnly = !f.membersOnly
	case filterPrivate:
		f.hidePrivate = !f.hidePrivate
	}
	return f
}

func (f filterForm) apply() tea.Cmd {
	membersOnly, hidePrivate := f.membersOnly, f.hidePrivate
	return func() tea.Msg {
		return filterAppliedMsg{membersOnly: membersOnly, hidePrivate: hidePrivate}
	}
}

func (f filterForm) View() string {
	checks := []string{
		f.checkRow(filterMembers, "Joined projects only", f.membersOnly),
		f.checkRow(filterPrivate, "Hide private projects", f.hidePrivate),
	}
	w := lipgloss.Width(f.buttonGroup())
	for _, c := range checks {
		if cw := lipgloss.Width(c); cw > w {
			w = cw
		}
	}
	buttons := lipgloss.PlaceHorizontal(w, lipgloss.Right, f.buttonGroup())
	rows := append(checks, "", buttons)
	body := lipgloss.NewStyle().Padding(1, 1).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("Filters", body, f.deps.Styles.Theme.Primary)
}

func (f filterForm) checkRow(which int, label string, checked bool) string {
	t := f.deps.Styles.Theme
	g := f.deps.Glyphs
	box := "[ ]"
	if checked {
		box = "[" + lipgloss.NewStyle().Foreground(t.Success).Render(g.Or(g.Check, "x")) + "]"
	}
	col := t.Text
	switch {
	case f.focus == which:
		col = t.Accent
	case f.hover == which:
		col = t.Secondary
	}
	row := box + " " + lipgloss.NewStyle().Foreground(col).Render(label)
	return zone.Mark(filterZoneID(which), row)
}

func (f filterForm) buttonGroup() string {
	return lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Apply", "filter.apply", f.focus == filterApply, f.hover == filterApply),
		" ",
		f.button("Cancel", "filter.cancel", f.focus == filterCancel, f.hover == filterCancel),
	)
}

func (f filterForm) button(label, id string, focused, hovered bool) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case focused:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case hovered:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(id, components.TitledBoxWeighted("", body, borderCol, focused))
}

func filterZoneID(which int) string {
	switch which {
	case filterMembers:
		return "filter.members"
	case filterPrivate:
		return "filter.private"
	}
	return ""
}

func (f filterForm) HelpKeys() []key.Binding {
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("space"), key.WithHelp("space", "toggle")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "apply")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

type filterAppliedMsg struct {
	membersOnly bool
	hidePrivate bool
}

type filterCancelledMsg struct{}

func cancelFilter() tea.Msg { return filterCancelledMsg{} }
