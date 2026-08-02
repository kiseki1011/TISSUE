package agents

import (
	"context"
	"errors"
	"image/color"
	"net/http"
	"regexp"
	"strings"
	"unicode/utf8"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// create-agent focus stops.
const (
	caName = iota
	caModel
	caSubmit
	caCancel
	caCount
)

const caFieldW = 38

// agentNamePattern mirrors the backend: letters (many scripts) and spaces only — no digits or
// punctuation.
var agentNamePattern = regexp.MustCompile(`^[A-Za-z\x{00C0}-\x{024F}\x{0370}-\x{03FF}\x{0400}-\x{04FF}\x{4E00}-\x{9FFF}\x{3040}-\x{30FF}\x{AC00}-\x{D7A3} ]+$`)

// createAgentForm is the "New Agent" modal: an owner-facing name and an optional declared model.
type createAgentForm struct {
	deps deps.Deps

	name    textinput.Model
	model   textinput.Model
	spinner spinner.Model
	focus   int
	hover   int

	nameErr    string
	status     string
	submitting bool
}

func newCreateAgentForm(d deps.Deps) createAgentForm {
	line := func(placeholder string) textinput.Model {
		in := textinput.New()
		in.Prompt = ""
		in.Placeholder = placeholder
		in.SetWidth(caFieldW)
		return in
	}
	f := createAgentForm{
		deps:    d,
		name:    line("Build Bot"),
		model:   line("claude-opus-4-8 (optional)"),
		spinner: spinner.New(),
		focus:   caName,
		hover:   -1,
	}
	f.name.Focus()
	return f
}

func (f createAgentForm) Init() tea.Cmd { return textinput.Blink }

func (f createAgentForm) Update(msg tea.Msg) (createAgentForm, tea.Cmd) {
	switch msg := msg.(type) {
	case createFailedMsg:
		f.submitting = false
		f.status = msg.message
		return f, nil
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
	return f.updateInputs(msg)
}

func (f createAgentForm) hitZone(msg tea.MouseMsg) int {
	switch {
	case zone.Get("agents.create.name").InBounds(msg):
		return caName
	case zone.Get("agents.create.model").InBounds(msg):
		return caModel
	case zone.Get("agents.create.submit").InBounds(msg):
		return caSubmit
	case zone.Get("agents.create.cancel").InBounds(msg):
		return caCancel
	}
	return -1
}

func (f createAgentForm) onClick(msg tea.MouseClickMsg) (createAgentForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	switch f.hitZone(msg) {
	case caName, caModel:
		return f.focusOn(f.hitZone(msg))
	case caSubmit:
		return f.submit()
	case caCancel:
		return f, cancelCreate
	}
	return f, nil
}

func (f createAgentForm) onKey(msg tea.KeyPressMsg) (createAgentForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	switch msg.String() {
	case "tab", "down":
		return f.moveFocus(1)
	case "shift+tab", "up":
		return f.moveFocus(-1)
	case "esc":
		return f, cancelCreate
	case "enter":
		switch f.focus {
		case caSubmit:
			return f.submit()
		case caCancel:
			return f, cancelCreate
		case caName, caModel:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

func (f createAgentForm) moveFocus(delta int) (createAgentForm, tea.Cmd) {
	return f.focusOn((f.focus + delta + caCount) % caCount)
}

func (f createAgentForm) focusOn(target int) (createAgentForm, tea.Cmd) {
	f.focus = target
	f.name.Blur()
	f.model.Blur()
	var cmd tea.Cmd
	switch target {
	case caName:
		cmd = f.name.Focus()
	case caModel:
		cmd = f.model.Focus()
	}
	return f, cmd
}

func (f createAgentForm) typeIntoFocused(msg tea.KeyPressMsg) (createAgentForm, tea.Cmd) {
	f.status = ""
	var cmd tea.Cmd
	switch f.focus {
	case caName:
		f.nameErr = ""
		f.name, cmd = f.name.Update(msg)
	case caModel:
		f.model, cmd = f.model.Update(msg)
	}
	return f, cmd
}

func (f createAgentForm) updateInputs(msg tea.Msg) (createAgentForm, tea.Cmd) {
	var nc, mc tea.Cmd
	f.name, nc = f.name.Update(msg)
	f.model, mc = f.model.Update(msg)
	return f, tea.Batch(nc, mc)
}

func (f createAgentForm) submit() (createAgentForm, tea.Cmd) {
	f.nameErr, f.status = "", ""
	name := strings.TrimSpace(f.name.Value())
	model := strings.TrimSpace(f.model.Value())

	switch n := utf8.RuneCountInString(name); {
	case name == "":
		f.nameErr = "Required field"
	case n < 2 || n > 35:
		f.nameErr = "2 to 35 characters"
	case !agentNamePattern.MatchString(name):
		f.nameErr = "Letters and spaces only"
	}
	if f.nameErr != "" {
		return f.focusOn(caName)
	}
	f.submitting = true
	return f, tea.Batch(createAgentCmd(f.deps, name, model), f.spinner.Tick)
}

func (f createAgentForm) View() string {
	rows := []string{
		f.field(caName, "Name", f.name.View(), f.nameErr),
		f.field(caModel, "Declared model", f.model.View(), ""),
	}
	switch {
	case f.submitting:
		rows = append(rows, lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Warning).Padding(0, 1).Render(f.spinner.View()+" Creating…"))
	case f.status != "":
		rows = append(rows, f.deps.Styles.Error.Width(caFieldW).Padding(0, 1).Render(f.status))
	}
	rows = append(rows, "", f.buttons())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("New Agent", body, f.deps.Styles.Theme.Primary)
}

func (f createAgentForm) field(which int, label, content, errMsg string) string {
	fixed := lipgloss.NewStyle().Width(caFieldW).MaxWidth(caFieldW).Height(1).MaxHeight(1).Render(content)
	box := components.TitledBoxWeighted(label, fixed, f.fieldBorderColor(which, errMsg), f.focus == which)
	box = zone.Mark(f.fieldZone(which), box)
	err := " "
	if errMsg != "" {
		err = f.deps.Styles.Error.Render(errMsg)
	}
	return lipgloss.JoinVertical(lipgloss.Left, box, lipgloss.NewStyle().Padding(0, 1).Render(err))
}

func (f createAgentForm) fieldZone(which int) string {
	switch which {
	case caName:
		return "agents.create.name"
	case caModel:
		return "agents.create.model"
	}
	return ""
}

func (f createAgentForm) fieldBorderColor(which int, errMsg string) color.Color {
	t := f.deps.Styles.Theme
	switch {
	case errMsg != "":
		return t.Error
	case f.focus == which:
		return t.Accent
	case f.hover == which:
		return t.Secondary
	default:
		return t.Primary
	}
}

func (f createAgentForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Create", "agents.create.submit", f.focus == caSubmit, f.hover == caSubmit),
		" ",
		f.button("Cancel", "agents.create.cancel", f.focus == caCancel, f.hover == caCancel),
	)
	return lipgloss.PlaceHorizontal(caFieldW+4, lipgloss.Right, group)
}

func (f createAgentForm) button(label, id string, focused, hovered bool) string {
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

func (f createAgentForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "confirm")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

type createFailedMsg struct{ message string }
type createCancelledMsg struct{}

func cancelCreate() tea.Msg { return createCancelledMsg{} }

func createAgentCmd(d deps.Deps, name, model string) tea.Cmd {
	return func() tea.Msg {
		a, err := d.Agents.CreateAgent(context.Background(), name, model)
		if err != nil {
			return createFailedMsg{message: createAgentError(err)}
		}
		return agentCreatedMsg{agent: a}
	}
}

func createAgentError(err error) string {
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "You already have an agent with that name."
		case http.StatusForbidden:
			return "Only human accounts can own agents."
		case http.StatusBadRequest:
			return "Invalid agent name."
		}
	}
	return "Could not create the agent. Try again."
}
