package agents

import (
	"context"
	"strconv"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// edit-agent focus stops (the name is immutable, so it is shown but not editable).
const (
	eaType = iota
	eaModel
	eaDesc
	eaSubmit
	eaCancel
	eaCount
)

type editAgentForm struct {
	deps deps.Deps

	agentID   int64
	agentName string

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

	status     string
	submitting bool
}

func newEditAgentForm(d deps.Deps, agent domain.Agent, models []domain.AiModel) editAgentForm {
	desc := textinput.New()
	desc.Prompt = ""
	desc.Placeholder = "What is this agent for? (optional)"
	desc.SetWidth(agentFieldW)
	desc.CharLimit = agentDescMax
	desc.SetValue(agent.Description)
	models = withModel(models, agent.ModelID, agent.ModelName, agent.ModelColor)
	agentType := agent.AgentType
	if agentType == "" {
		agentType = "GENERAL"
	}
	f := editAgentForm{
		deps:      d,
		agentID:   agent.ID,
		agentName: agent.Name,
		agentType: agentType,
		models:    models,
		modelID:   agent.ModelID,
		desc:      desc,
		spinner:   spinner.New(),
		focus:     eaType,
		hover:     -1,
	}
	return f
}

func (f editAgentForm) Init() tea.Cmd { return textinput.Blink }

func (f editAgentForm) Update(msg tea.Msg) (editAgentForm, tea.Cmd) {
	switch msg := msg.(type) {
	case editFailedMsg:
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
	var cmd tea.Cmd
	f.desc, cmd = f.desc.Update(msg)
	return f, cmd
}

func (f editAgentForm) hitZone(msg tea.MouseMsg) int {
	switch {
	case zone.Get("agents.edit.type").InBounds(msg):
		return eaType
	case zone.Get("agents.edit.model").InBounds(msg):
		return eaModel
	case zone.Get("agents.edit.desc").InBounds(msg):
		return eaDesc
	case zone.Get("agents.edit.submit").InBounds(msg):
		return eaSubmit
	case zone.Get("agents.edit.cancel").InBounds(msg):
		return eaCancel
	}
	return -1
}

func (f editAgentForm) onClick(msg tea.MouseClickMsg) (editAgentForm, tea.Cmd) {
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
	case eaDesc:
		return f.focusOn(eaDesc)
	case eaType:
		ff, _ := f.focusOn(eaType)
		return ff.openPicker(eaType), nil
	case eaModel:
		ff, _ := f.focusOn(eaModel)
		return ff.openPicker(eaModel), nil
	case eaSubmit:
		return f.submit()
	case eaCancel:
		return f, cancelEdit
	}
	return f, nil
}

func (f editAgentForm) onKey(msg tea.KeyPressMsg) (editAgentForm, tea.Cmd) {
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
		return f, cancelEdit
	case "enter", "space":
		switch f.focus {
		case eaType:
			return f.openPicker(eaType), nil
		case eaModel:
			return f.openPicker(eaModel), nil
		case eaSubmit:
			return f.submit()
		case eaCancel:
			return f, cancelEdit
		default:
			if msg.String() == "enter" {
				return f.moveFocus(1)
			}
		}
	}
	if f.focus == eaDesc {
		f.status = ""
		var cmd tea.Cmd
		f.desc, cmd = f.desc.Update(msg)
		return f, cmd
	}
	return f, nil
}

func (f editAgentForm) pickKey(msg tea.KeyPressMsg) editAgentForm {
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

func (f editAgentForm) openPicker(field int) editAgentForm {
	f.picking, f.pickField = true, field
	if field == eaType {
		f.pick = widgets.NewListPicker("Type", agentTypeOptions(), f.agentType, agentPickRows, agentFieldW)
	} else {
		f.pick = widgets.NewListPicker("Model", agentModelOptions(f.models), strconv.FormatInt(f.modelID, 10), agentPickRows, agentFieldW)
	}
	return f
}

func (f editAgentForm) applyPick() editAgentForm {
	opt, ok := f.pick.Selected()
	f.picking = false
	if ok {
		if f.pickField == eaType {
			f.agentType = opt.Value
		} else {
			id, _ := strconv.ParseInt(opt.Value, 10, 64)
			f.modelID = id
		}
	}
	return f
}

func (f editAgentForm) moveFocus(delta int) (editAgentForm, tea.Cmd) {
	return f.focusOn((f.focus + delta + eaCount) % eaCount)
}

func (f editAgentForm) focusOn(target int) (editAgentForm, tea.Cmd) {
	f.focus = target
	f.desc.Blur()
	if target == eaDesc {
		return f, f.desc.Focus()
	}
	return f, nil
}

func (f editAgentForm) submit() (editAgentForm, tea.Cmd) {
	f.status = ""
	f.submitting = true
	cmd := updateAgentCmd(f.deps, f.agentID, f.agentType, f.modelID, strings.TrimSpace(f.desc.Value()))
	return f, tea.Batch(cmd, f.spinner.Tick)
}

func (f editAgentForm) View() string {
	if f.picking {
		return f.pick.View(f.deps.Styles)
	}
	t := f.deps.Styles.Theme
	header := lipgloss.NewStyle().Foreground(t.Muted).Padding(0, 1).Render("Editing " +
		lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render(f.agentName))
	rows := []string{
		header,
		"",
		f.field(eaType, "Type", dropdownContent(t, titleCase(f.agentType), agentFieldW)),
		f.field(eaModel, "Model", dropdownContent(t, modelName(f.models, f.modelID), agentFieldW)),
		f.field(eaDesc, "Description", f.desc.View()),
	}
	switch {
	case f.submitting:
		rows = append(rows, lipgloss.NewStyle().Foreground(t.Warning).Padding(0, 1).Render(f.spinner.View()+" Saving…"))
	case f.status != "":
		rows = append(rows, f.deps.Styles.Error.Width(agentFieldW).Padding(0, 1).Render(f.status))
	}
	rows = append(rows, "", f.buttons())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("Edit Agent", body, t.Primary)
}

func (f editAgentForm) field(which int, label, content string) string {
	t := f.deps.Styles.Theme
	fixed := lipgloss.NewStyle().Width(agentFieldW).MaxWidth(agentFieldW).Height(1).MaxHeight(1).Render(content)
	box := components.TitledBoxWeighted(label, fixed, fieldBorderColor(t, f.focus == which, f.hover == which, false), f.focus == which)
	box = zone.Mark(f.fieldZone(which), box)
	return lipgloss.JoinVertical(lipgloss.Left, box, lipgloss.NewStyle().Padding(0, 1).Render(" "))
}

func (f editAgentForm) fieldZone(which int) string {
	switch which {
	case eaType:
		return "agents.edit.type"
	case eaModel:
		return "agents.edit.model"
	case eaDesc:
		return "agents.edit.desc"
	}
	return ""
}

func (f editAgentForm) buttons() string {
	t := f.deps.Styles.Theme
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		formButton(t, "Save", "agents.edit.submit", f.focus == eaSubmit, f.hover == eaSubmit),
		" ",
		formButton(t, "Cancel", "agents.edit.cancel", f.focus == eaCancel, f.hover == eaCancel),
	)
	return lipgloss.PlaceHorizontal(agentFieldW+4, lipgloss.Right, group)
}

func (f editAgentForm) HelpKeys() []key.Binding {
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
	case eaType, eaModel:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "pick")))
	default:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "confirm")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

type (
	agentUpdatedMsg  struct{}
	editCancelledMsg struct{}
	editFailedMsg    struct{ message string }
)

func cancelEdit() tea.Msg { return editCancelledMsg{} }

func updateAgentCmd(d deps.Deps, agentID int64, agentType string, modelID int64, description string) tea.Cmd {
	return func() tea.Msg {
		if err := d.Agents.UpdateAgent(context.Background(), agentID, agentType, modelID, description); err != nil {
			return editFailedMsg{message: "Could not save the agent. Try again."}
		}
		return agentUpdatedMsg{}
	}
}
