package agents

import (
	"context"
	"errors"
	"net/http"
	"regexp"
	"strconv"
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
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

const (
	caName = iota
	caType
	caModel
	caDesc
	caSubmit
	caCancel
	caCount
)

// agentNamePattern mirrors the backend rule: letters (many scripts) and spaces, no digits.
var agentNamePattern = regexp.MustCompile(`^[A-Za-z\x{00C0}-\x{024F}\x{0370}-\x{03FF}\x{0400}-\x{04FF}\x{4E00}-\x{9FFF}\x{3040}-\x{30FF}\x{AC00}-\x{D7A3} ]+$`)

type createAgentForm struct {
	deps deps.Deps

	name      textinput.Model
	agentType string
	models    []domain.AiModel
	modelID   int64
	desc      textinput.Model
	spinner   spinner.Model
	focus     int
	hover     int

	picking   bool
	pick      widgets.ListPicker
	pickField int

	nameErr    string
	status     string
	submitting bool
}

func newCreateAgentForm(d deps.Deps, models []domain.AiModel) createAgentForm {
	line := func(placeholder string) textinput.Model {
		in := textinput.New()
		in.Prompt = ""
		in.Placeholder = placeholder
		in.SetWidth(agentFieldW)
		return in
	}
	desc := line("What is this agent for? (optional)")
	desc.CharLimit = agentDescMax
	f := createAgentForm{
		deps:      d,
		name:      line("Build Bot"),
		agentType: "GENERAL",
		models:    models,
		modelID:   0,
		desc:      desc,
		spinner:   spinner.New(),
		focus:     caName,
		hover:     -1,
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
		if !f.picking {
			f.hover = f.hitZone(msg)
		}
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
	case zone.Get("agents.create.type").InBounds(msg):
		return caType
	case zone.Get("agents.create.model").InBounds(msg):
		return caModel
	case zone.Get("agents.create.desc").InBounds(msg):
		return caDesc
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
	if f.picking {
		if i := f.pick.HitOption(msg); i >= 0 {
			f.pick.Cursor = i
			return f.applyPick(), nil
		}
		return f, nil
	}
	switch f.hitZone(msg) {
	case caName, caDesc:
		return f.focusOn(f.hitZone(msg))
	case caType:
		ff, _ := f.focusOn(caType)
		return ff.openPicker(caType), nil
	case caModel:
		ff, _ := f.focusOn(caModel)
		return ff.openPicker(caModel), nil
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
	if f.picking {
		return f.pickKey(msg), nil
	}
	switch msg.String() {
	case "tab", "down":
		return f.moveFocus(1)
	case "shift+tab", "up":
		return f.moveFocus(-1)
	case "esc":
		return f, cancelCreate
	case "enter", "space":
		switch f.focus {
		case caType:
			return f.openPicker(caType), nil
		case caModel:
			return f.openPicker(caModel), nil
		case caSubmit:
			return f.submit()
		case caCancel:
			return f, cancelCreate
		default:
			if msg.String() == "enter" {
				return f.moveFocus(1)
			}
		}
	}
	return f.typeIntoFocused(msg)
}

func (f createAgentForm) pickKey(msg tea.KeyPressMsg) createAgentForm {
	switch msg.String() {
	case "up", "k":
		f.pick = f.pick.Move(-1)
	case "down", "j":
		f.pick = f.pick.Move(1)
	case "enter", "space":
		return f.applyPick()
	case "esc":
		f.picking = false
	}
	return f
}

func (f createAgentForm) openPicker(field int) createAgentForm {
	f.picking, f.pickField = true, field
	if field == caType {
		f.pick = widgets.NewListPicker("Type", agentTypeOptions(), f.agentType, agentPickRows, agentFieldW)
	} else {
		f.pick = widgets.NewListPicker("Model", agentModelOptions(f.models), strconv.FormatInt(f.modelID, 10), agentPickRows, agentFieldW)
	}
	return f
}

func (f createAgentForm) applyPick() createAgentForm {
	opt, ok := f.pick.Selected()
	f.picking = false
	if ok {
		if f.pickField == caType {
			f.agentType = opt.Value
		} else {
			id, _ := strconv.ParseInt(opt.Value, 10, 64)
			f.modelID = id
		}
	}
	return f
}

func (f createAgentForm) moveFocus(delta int) (createAgentForm, tea.Cmd) {
	return f.focusOn((f.focus + delta + caCount) % caCount)
}

func (f createAgentForm) focusOn(target int) (createAgentForm, tea.Cmd) {
	f.focus = target
	f.name.Blur()
	f.desc.Blur()
	var cmd tea.Cmd
	switch target {
	case caName:
		cmd = f.name.Focus()
	case caDesc:
		cmd = f.desc.Focus()
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
	case caDesc:
		f.desc, cmd = f.desc.Update(msg)
	}
	return f, cmd
}

func (f createAgentForm) updateInputs(msg tea.Msg) (createAgentForm, tea.Cmd) {
	var nc, dc tea.Cmd
	f.name, nc = f.name.Update(msg)
	f.desc, dc = f.desc.Update(msg)
	return f, tea.Batch(nc, dc)
}

func (f createAgentForm) submit() (createAgentForm, tea.Cmd) {
	f.nameErr, f.status = "", ""
	name := strings.TrimSpace(f.name.Value())

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
	cmd := createAgentCmd(f.deps, name, f.agentType, f.modelID, strings.TrimSpace(f.desc.Value()))
	return f, tea.Batch(cmd, f.spinner.Tick)
}

func (f createAgentForm) View() string {
	if f.picking {
		return f.pick.View(f.deps.Styles)
	}
	t := f.deps.Styles.Theme
	rows := []string{
		f.field(caName, "Name", f.name.View(), f.nameErr),
		f.field(caType, "Type", dropdownContent(t, titleCase(f.agentType), agentFieldW), ""),
		f.field(caModel, "Model", dropdownContent(t, modelName(f.models, f.modelID), agentFieldW), ""),
		f.field(caDesc, "Description", f.desc.View(), ""),
	}
	switch {
	case f.submitting:
		rows = append(rows, lipgloss.NewStyle().Foreground(t.Warning).Padding(0, 1).Render(f.spinner.View()+" Creating…"))
	case f.status != "":
		rows = append(rows, f.deps.Styles.Error.Width(agentFieldW).Padding(0, 1).Render(f.status))
	}
	rows = append(rows, "", f.buttons())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("New Agent", body, t.Primary)
}

func (f createAgentForm) field(which int, label, content, errMsg string) string {
	t := f.deps.Styles.Theme
	fixed := lipgloss.NewStyle().Width(agentFieldW).MaxWidth(agentFieldW).Height(1).MaxHeight(1).Render(content)
	box := components.TitledBoxWeighted(label, fixed, fieldBorderColor(t, f.focus == which, f.hover == which, errMsg != ""), f.focus == which)
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
	case caType:
		return "agents.create.type"
	case caModel:
		return "agents.create.model"
	case caDesc:
		return "agents.create.desc"
	}
	return ""
}

func (f createAgentForm) buttons() string {
	t := f.deps.Styles.Theme
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		formButton(t, "Create", "agents.create.submit", f.focus == caSubmit, f.hover == caSubmit),
		" ",
		formButton(t, "Cancel", "agents.create.cancel", f.focus == caCancel, f.hover == caCancel),
	)
	return lipgloss.PlaceHorizontal(agentFieldW+4, lipgloss.Right, group)
}

func (f createAgentForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	if f.picking {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	switch f.focus {
	case caType, caModel:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "pick")))
	default:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "confirm")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

type (
	createFailedMsg    struct{ message string }
	createCancelledMsg struct{}
)

func cancelCreate() tea.Msg { return createCancelledMsg{} }

func createAgentCmd(d deps.Deps, name, agentType string, modelID int64, description string) tea.Cmd {
	return func() tea.Msg {
		a, err := d.Agents.CreateAgent(context.Background(), name, agentType, modelID, description)
		if err != nil {
			return createFailedMsg{message: createAgentError(err)}
		}
		return agentCreatedMsg{agent: a}
	}
}

func createAgentError(err error) string {
	if m, ok := errmsg.Override(err); ok {
		return m // connectivity, or a leaky code mapped to friendlier copy
	}
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
	if r := domain.ErrorReason(err); r != "" {
		return r // prefer the server's explanation over the generic line
	}
	return "Could not create the agent. Try again."
}
