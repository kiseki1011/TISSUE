package widgets

import (
	"image/color"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const (
	cnfAccept = iota
	cnfCancel
)

const confirmW = 44

// ConfirmForm is a yes/no dialog for a destructive action. It only emits Accepted/Cancelled: the parent
// runs the command and routes a failure back into Status, so the dialog stays open and shows it.
type ConfirmForm struct {
	styles  theme.Styles
	title   string
	message string
	accept  string // label for the confirm button, such as "Delete"

	focus   int
	hover   int
	spinner spinner.Model

	Submitting bool
	Status     string
}

// NewConfirmForm defaults focus to Cancel, so a stray enter does not confirm a destructive action.
func NewConfirmForm(s theme.Styles, title, message, acceptLabel string) ConfirmForm {
	return ConfirmForm{
		styles: s, title: title, message: message, accept: acceptLabel,
		spinner: spinner.New(), focus: cnfCancel, hover: -1,
	}
}

func (f ConfirmForm) Init() tea.Cmd { return nil }

func (f ConfirmForm) Update(msg tea.Msg) (ConfirmForm, tea.Cmd) {
	switch msg := msg.(type) {
	case spinner.TickMsg:
		var cmd tea.Cmd
		f.spinner, cmd = f.spinner.Update(msg)
		if !f.Submitting {
			cmd = nil
		}
		return f, cmd
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.MouseMotionMsg:
		f.hover = f.hitZone(msg)
		return f, nil
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f, nil
}

func (f ConfirmForm) onKey(msg tea.KeyPressMsg) (ConfirmForm, tea.Cmd) {
	if f.Submitting {
		return f, nil
	}
	switch msg.String() {
	case "tab", "shift+tab", "left", "right", "h", "l", "up", "down", "j", "k":
		if f.focus == cnfAccept {
			f.focus = cnfCancel
		} else {
			f.focus = cnfAccept
		}
	case "enter", "space":
		if f.focus == cnfAccept {
			return f.acceptNow()
		}
		return f, cancelConfirm
	case "esc":
		return f, cancelConfirm
	}
	return f, nil
}

func (f ConfirmForm) acceptNow() (ConfirmForm, tea.Cmd) {
	f.Submitting, f.Status = true, ""
	return f, tea.Batch(func() tea.Msg { return ConfirmAcceptedMsg{} }, f.spinner.Tick)
}

func (f ConfirmForm) View() string {
	t := f.styles.Theme
	rows := []string{
		lipgloss.NewStyle().Foreground(t.Text).Width(confirmW).Render(components.Flatten(f.message)),
	}
	switch {
	case f.Submitting:
		rows = append(rows, "", lipgloss.NewStyle().Foreground(t.Warning).Render(f.spinner.View()+" Working…"))
	case f.Status != "":
		rows = append(rows, "", f.styles.Error.Width(confirmW).Render(f.Status))
	}
	rows = append(rows, "", f.buttons(), "", f.hint())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered(f.title, body, t.Error)
}

func (f ConfirmForm) buttons() string {
	t := f.styles.Theme
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button(f.accept, "widgets.confirm.accept", t.Error, f.focus == cnfAccept, f.hover == cnfAccept),
		" ",
		f.button("Cancel", "widgets.confirm.cancel", t.Primary, f.focus == cnfCancel, f.hover == cnfCancel),
	)
	return lipgloss.PlaceHorizontal(confirmW, lipgloss.Right, group)
}

func (f ConfirmForm) button(label, id string, base color.Color, focused, hovered bool) string {
	t := f.styles.Theme
	borderCol, textCol, bold := base, t.Text, false
	switch {
	case focused:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case hovered:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(id, components.TitledBoxWeighted("", body, borderCol, focused))
}

func (f ConfirmForm) hint() string {
	return components.HintBar(f.styles, "←/→", "select", "enter", "confirm", "esc", "cancel")
}

func (f ConfirmForm) HelpKeys() []key.Binding {
	if f.Submitting {
		return nil
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "select")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "confirm")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

func (f ConfirmForm) hitZone(msg tea.MouseMsg) int {
	if zone.Get("widgets.confirm.accept").InBounds(msg) {
		return cnfAccept
	}
	if zone.Get("widgets.confirm.cancel").InBounds(msg) {
		return cnfCancel
	}
	return -1
}

func (f ConfirmForm) onClick(msg tea.MouseClickMsg) (ConfirmForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.Submitting {
		return f, nil
	}
	switch f.hitZone(msg) {
	case cnfAccept:
		f.focus = cnfAccept
		return f.acceptNow()
	case cnfCancel:
		return f, cancelConfirm
	}
	return f, nil
}

type ConfirmAcceptedMsg struct{}

type ConfirmCancelledMsg struct{}

func cancelConfirm() tea.Msg { return ConfirmCancelledMsg{} }
