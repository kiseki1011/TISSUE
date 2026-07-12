package schema

import (
	"image/color"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// confirm-modal focus stops.
const (
	cnfAccept = iota
	cnfCancel
)

const confirmW = 44

// confirmForm is a small yes/no dialog for a destructive action. It is presentational: it emits
// confirmAcceptedMsg / confirmCancelledMsg and the parent runs the real command, routing any
// failure back into status so the dialog can stay open and show it. On accept it flips to a
// submitting state (spinner) until the parent closes or fails it.
type confirmForm struct {
	deps    deps.Deps
	title   string
	message string
	accept  string // label for the confirm button, e.g. "Delete"

	focus      int
	hover      int
	spinner    spinner.Model
	submitting bool
	status     string
}

// newConfirmForm builds a dialog defaulting focus to Cancel, so a stray enter does not confirm a
// destructive action.
func newConfirmForm(d deps.Deps, title, message, acceptLabel string) confirmForm {
	return confirmForm{
		deps: d, title: title, message: message, accept: acceptLabel,
		spinner: spinner.New(), focus: cnfCancel, hover: -1,
	}
}

func (f confirmForm) Init() tea.Cmd { return nil }

func (f confirmForm) Update(msg tea.Msg) (confirmForm, tea.Cmd) {
	switch msg := msg.(type) {
	case spinner.TickMsg:
		var cmd tea.Cmd
		f.spinner, cmd = f.spinner.Update(msg)
		if !f.submitting {
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

func (f confirmForm) onKey(msg tea.KeyPressMsg) (confirmForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	switch msg.String() {
	case "tab", "shift+tab", "left", "right", "h", "l", "up", "down", "j", "k":
		if f.focus == cnfAccept {
			f.focus = cnfCancel
		} else {
			f.focus = cnfAccept
		}
	case "enter", " ":
		if f.focus == cnfAccept {
			return f.acceptNow()
		}
		return f, cancelConfirm
	}
	return f, nil
}

// acceptNow flips to the submitting state and tells the parent to run the real command.
func (f confirmForm) acceptNow() (confirmForm, tea.Cmd) {
	f.submitting, f.status = true, ""
	return f, tea.Batch(func() tea.Msg { return confirmAcceptedMsg{} }, f.spinner.Tick)
}

// ---- view ----

func (f confirmForm) View() string {
	t := f.deps.Styles.Theme
	rows := []string{
		lipgloss.NewStyle().Foreground(t.Text).Width(confirmW).Render(flatten(f.message)),
	}
	switch {
	case f.submitting:
		rows = append(rows, "", lipgloss.NewStyle().Foreground(t.Warning).Render(f.spinner.View()+" Working…"))
	case f.status != "":
		rows = append(rows, "", f.deps.Styles.Error.Width(confirmW).Render(f.status))
	}
	rows = append(rows, "", f.buttons(), "", f.hint())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered(f.title, body, t.Error)
}

func (f confirmForm) buttons() string {
	t := f.deps.Styles.Theme
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button(f.accept, "confirm.accept", t.Error, f.focus == cnfAccept, f.hover == cnfAccept),
		" ",
		f.button("Cancel", "confirm.cancel", t.Primary, f.focus == cnfCancel, f.hover == cnfCancel),
	)
	return lipgloss.PlaceHorizontal(confirmW, lipgloss.Right, group)
}

func (f confirmForm) button(label, id string, base color.Color, focused, hovered bool) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := base, t.Text, false
	switch {
	case focused:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case hovered:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(id, components.TitledBox("", body, borderCol))
}

func (f confirmForm) hint() string {
	return hintBar(f.deps.Styles, "←/→", "select", "enter", "confirm", "esc", "cancel")
}

func (f confirmForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "select")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "confirm")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

// ---- click routing ----

func (f confirmForm) hitZone(msg tea.MouseMsg) int {
	if zone.Get("confirm.accept").InBounds(msg) {
		return cnfAccept
	}
	if zone.Get("confirm.cancel").InBounds(msg) {
		return cnfCancel
	}
	return -1
}

func (f confirmForm) onClick(msg tea.MouseClickMsg) (confirmForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
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

// ---- messages ----

type confirmAcceptedMsg struct{}

type confirmCancelledMsg struct{}

func cancelConfirm() tea.Msg { return confirmCancelledMsg{} }
