package schema

import (
	"context"
	"image/color"
	"strings"
	"unicode/utf8"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	"charm.land/bubbles/v2/textarea"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// issueHierarchies are the four IssueHierarchy values, top to bottom.
var issueHierarchies = []string{"EPIC", "STANDARD", "SUBTASK", "MICROTASK"}

// create-type focus stops.
const (
	ctName = iota
	ctColor
	ctHierarchy
	ctWorkflow
	ctDesc
	ctSubmit
	ctCancel
)

// createTypeForm is the "New Issue Type" modal. Name, color, hierarchy, and workflow are set here;
// hierarchy and workflow are fixed at creation (they cannot be edited afterward). The icon is not
// collected — the backend defaults it and it is unused in the TUI.
type createTypeForm struct {
	deps deps.Deps

	name      textinput.Model
	color     string
	colors    []string
	hierarchy string
	workflows []domain.WorkflowSummary
	wfID      int
	desc      textarea.Model
	spinner   spinner.Model

	picking   bool // color grid open
	cpick     colorPicker
	pickOpen  bool // hierarchy/workflow dropdown open
	pick      picker
	pickField int

	focus      int
	hover      int
	nameErr    string
	status     string
	submitting bool
}

func newCreateTypeForm(d deps.Deps, workflows []domain.WorkflowSummary) createTypeForm {
	n := textinput.New()
	n.Prompt = ""
	n.CharLimit = 50
	n.SetWidth(editFieldW)

	da := textarea.New()
	da.Prompt = ""
	da.ShowLineNumbers = false
	da.CharLimit = 255
	da.SetWidth(editFieldW)
	da.SetHeight(editDescH)

	f := createTypeForm{
		deps: d, name: n, desc: da, spinner: spinner.New(),
		colors: components.ColorTypeNames(), color: "ANSI_BLUE",
		hierarchy: "STANDARD", workflows: workflows,
		focus: ctName, hover: -1,
	}
	if len(workflows) > 0 {
		f.wfID = workflows[0].ID
	}
	f.name.Focus()
	return f
}

func (f createTypeForm) Init() tea.Cmd { return textinput.Blink }

func (f createTypeForm) fields() []int {
	return []int{ctName, ctColor, ctHierarchy, ctWorkflow, ctDesc, ctSubmit, ctCancel}
}

func (f createTypeForm) Update(msg tea.Msg) (createTypeForm, tea.Cmd) {
	switch msg := msg.(type) {
	case createTypeFailedMsg:
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
		if f.picking {
			if i := f.cpick.hitCell(msg); i >= 0 {
				f.cpick.cursor = i
			}
			return f, nil
		}
		if !f.pickOpen {
			f.hover = f.hitZone(msg)
		}
		return f, nil
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f.updateInputs(msg)
}

func (f createTypeForm) onKey(msg tea.KeyPressMsg) (createTypeForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	if f.picking {
		return f.colorKey(msg), nil
	}
	if f.pickOpen {
		return f.pickKey(msg), nil
	}
	switch msg.String() {
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if f.focus != ctDesc {
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != ctDesc {
			return f.moveFocus(1)
		}
	case "enter":
		switch f.focus {
		case ctColor:
			return f.openColorPicker(), nil
		case ctHierarchy:
			return f.openHierarchyPicker(), nil
		case ctWorkflow:
			return f.openWorkflowPicker(), nil
		case ctSubmit:
			return f.submit()
		case ctCancel:
			return f, cancelCreateType
		case ctName:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

func (f createTypeForm) colorKey(msg tea.KeyPressMsg) createTypeForm {
	switch msg.String() {
	case "left", "h":
		f.cpick = f.cpick.move(-1, 0)
	case "right", "l":
		f.cpick = f.cpick.move(1, 0)
	case "up", "k":
		f.cpick = f.cpick.move(0, -1)
	case "down", "j":
		f.cpick = f.cpick.move(0, 1)
	case "enter", " ":
		if name, ok := f.cpick.selected(); ok {
			f.color = name
		}
		f.picking = false
	case "esc":
		f.picking = false
	}
	return f
}

func (f createTypeForm) pickKey(msg tea.KeyPressMsg) createTypeForm {
	switch msg.String() {
	case "up", "k":
		f.pick = f.pick.move(-1)
	case "down", "j":
		f.pick = f.pick.move(1)
	case "enter", " ":
		return f.applyPick()
	case "esc":
		f.pickOpen = false
	}
	return f
}

func (f createTypeForm) openColorPicker() createTypeForm {
	f.cpick = newColorPicker("Pick a color", f.color, colorGridCols)
	f.picking = true
	return f
}

func (f createTypeForm) openHierarchyPicker() createTypeForm {
	opts := make([]pickerOption, 0, len(issueHierarchies))
	for _, h := range issueHierarchies {
		opts = append(opts, pickerOption{value: h, label: h})
	}
	f.pick = newPicker("Hierarchy", opts, f.hierarchy, len(issueHierarchies), editFieldW)
	f.pickField, f.pickOpen = ctHierarchy, true
	return f
}

func (f createTypeForm) openWorkflowPicker() createTypeForm {
	opts := make([]pickerOption, 0, len(f.workflows))
	for _, w := range f.workflows {
		opts = append(opts, pickerOption{value: itoa(w.ID), label: w.Name})
	}
	f.pick = newPicker("Workflow", opts, itoa(f.wfID), 8, editFieldW)
	f.pickField, f.pickOpen = ctWorkflow, true
	return f
}

func (f createTypeForm) applyPick() createTypeForm {
	opt, ok := f.pick.selected()
	f.pickOpen = false
	if !ok {
		return f
	}
	if f.pickField == ctWorkflow {
		f.wfID = atoiSafe(opt.value)
	} else {
		f.hierarchy = opt.value
	}
	return f
}

func (f createTypeForm) moveFocus(delta int) (createTypeForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f createTypeForm) focusOn(id int) (createTypeForm, tea.Cmd) {
	f.focus = id
	f.name.Blur()
	f.desc.Blur()
	var cmd tea.Cmd
	switch id {
	case ctName:
		cmd = f.name.Focus()
	case ctDesc:
		cmd = f.desc.Focus()
	}
	return f, cmd
}

func (f createTypeForm) typeIntoFocused(msg tea.KeyPressMsg) (createTypeForm, tea.Cmd) {
	f.status = ""
	var cmd tea.Cmd
	switch f.focus {
	case ctName:
		f.nameErr = ""
		f.name, cmd = f.name.Update(msg)
	case ctDesc:
		f.desc, cmd = f.desc.Update(msg)
	}
	return f, cmd
}

func (f createTypeForm) updateInputs(msg tea.Msg) (createTypeForm, tea.Cmd) {
	var nc, dc tea.Cmd
	f.name, nc = f.name.Update(msg)
	f.desc, dc = f.desc.Update(msg)
	return f, tea.Batch(nc, dc)
}

func (f createTypeForm) submit() (createTypeForm, tea.Cmd) {
	f.nameErr, f.status = "", ""
	name := strings.TrimSpace(f.name.Value())
	switch n := utf8.RuneCountInString(name); {
	case n < 2:
		f.nameErr = "At least 2 characters"
		return f.focusOn(ctName)
	case n > 50:
		f.nameErr = "50 characters max"
		return f.focusOn(ctName)
	}
	if f.wfID == 0 {
		f.status = "Pick a workflow (create one first if the list is empty)."
		return f, nil
	}
	f.submitting = true
	return f, tea.Batch(
		createType(f.deps, name, strings.TrimSpace(f.desc.Value()), f.color, f.hierarchy, f.wfID),
		f.spinner.Tick,
	)
}

// ---- view ----

func (f createTypeForm) View() string {
	if f.picking {
		return f.cpick.View(f.deps.Styles)
	}
	if f.pickOpen {
		return f.pick.View(f.deps.Styles)
	}
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("New Issue Type", body, f.deps.Styles.Theme.Primary)
}

func (f createTypeForm) body() string {
	rows := []string{
		f.field(ctName, "Name", fixEdit(f.name.View(), 1), f.nameErr),
		f.field(ctColor, "Color", f.colorContent(), ""),
		f.field(ctHierarchy, "Hierarchy", f.hierarchyContent(), ""),
		f.field(ctWorkflow, "Workflow", f.workflowContent(), ""),
		f.field(ctDesc, "Description", fixEdit(f.desc.View(), editDescH), ""),
	}
	switch {
	case f.submitting:
		rows = append(rows, lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Warning).Padding(0, 1).Render(f.spinner.View()+" Creating…"))
	case f.status != "":
		rows = append(rows, f.deps.Styles.Error.Width(editFieldW).Padding(0, 1).Render(f.status))
	}
	rows = append(rows, "", f.buttons())
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

func (f createTypeForm) colorContent() string {
	body := components.ColorLabel(f.color)
	if sw := components.ColorSwatch(f.color); sw != "" {
		body = sw + " " + body
	}
	return alignRow(body, hintBar(f.deps.Styles, "enter", "▾"), editFieldW, lipgloss.NewStyle())
}

func (f createTypeForm) hierarchyContent() string {
	body := f.hierarchy
	if chip := components.HierarchyChip(f.hierarchy); chip != "" {
		body = chip
	}
	return alignRow(body, hintBar(f.deps.Styles, "enter", "▾"), editFieldW, lipgloss.NewStyle())
}

func (f createTypeForm) workflowContent() string {
	value := f.deps.Styles.Muted.Render("Select…")
	for _, w := range f.workflows {
		if w.ID == f.wfID {
			value = lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Text).Render(trunc(w.Name, editFieldW-8))
		}
	}
	return alignRow(value, hintBar(f.deps.Styles, "enter", "▾"), editFieldW, lipgloss.NewStyle())
}

func (f createTypeForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBox(label, content, f.fieldBorderColor(id, errMsg))
	if zid := f.zoneID(id); zid != "" {
		box = zone.Mark(zid, box)
	}
	if id == ctName {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.errText(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f createTypeForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Create", "createtype.save", f.focus == ctSubmit, f.hover == ctSubmit),
		" ",
		f.button("Cancel", "createtype.cancel", f.focus == ctCancel, f.hover == ctCancel),
	)
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f createTypeForm) button(label, id string, focused, hovered bool) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case focused:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case hovered:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(id, components.TitledBox("", body, borderCol))
}

func (f createTypeForm) fieldBorderColor(id int, errMsg string) color.Color {
	t := f.deps.Styles.Theme
	switch {
	case errMsg != "":
		return t.Error
	case f.focus == id:
		return t.Accent
	case f.hover == id:
		return t.Secondary
	default:
		return t.Primary
	}
}

func (f createTypeForm) errText(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f createTypeForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	if f.picking {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down", "left", "right"), key.WithHelp("↑↓←→", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	if f.pickOpen {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	switch f.focus {
	case ctColor, ctHierarchy, ctWorkflow:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "choose")))
	default:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "create")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

// ---- click routing ----

func (f createTypeForm) zoneID(id int) string {
	switch id {
	case ctName:
		return "createtype.name"
	case ctColor:
		return "createtype.color"
	case ctHierarchy:
		return "createtype.hierarchy"
	case ctWorkflow:
		return "createtype.workflow"
	case ctDesc:
		return "createtype.desc"
	case ctSubmit:
		return "createtype.save"
	case ctCancel:
		return "createtype.cancel"
	}
	return ""
}

func (f createTypeForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(f.zoneID(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f createTypeForm) onClick(msg tea.MouseClickMsg) (createTypeForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	if f.picking {
		if i := f.cpick.hitCell(msg); i >= 0 {
			f.cpick.cursor = i
			if name, ok := f.cpick.selected(); ok {
				f.color = name
			}
			f.picking = false
		}
		return f, nil
	}
	if f.pickOpen {
		if i := f.pick.hitOption(msg); i >= 0 {
			f.pick.cursor = i
			return f.applyPick(), nil
		}
		return f, nil
	}
	switch id := f.hitZone(msg); id {
	case ctName, ctDesc:
		return f.focusOn(id)
	case ctColor:
		ff, _ := f.focusOn(ctColor)
		return ff.openColorPicker(), nil
	case ctHierarchy:
		ff, _ := f.focusOn(ctHierarchy)
		return ff.openHierarchyPicker(), nil
	case ctWorkflow:
		ff, _ := f.focusOn(ctWorkflow)
		return ff.openWorkflowPicker(), nil
	case ctSubmit:
		return f.submit()
	case ctCancel:
		return f, cancelCreateType
	}
	return f, nil
}

// atoiSafe parses a small non-negative integer, returning 0 on any error.
func atoiSafe(s string) int {
	n := 0
	for _, r := range s {
		if r < '0' || r > '9' {
			return 0
		}
		n = n*10 + int(r-'0')
	}
	return n
}

// ---- messages ----

type typeCreatedMsg struct{}

type createTypeFailedMsg struct{ message string }

type createTypeCancelledMsg struct{}

func cancelCreateType() tea.Msg { return createTypeCancelledMsg{} }

func createType(d deps.Deps, name, desc, colorName, hierarchy string, workflowID int) tea.Cmd {
	return func() tea.Msg {
		if _, err := d.Catalog.CreateIssueType(context.Background(), name, desc, colorName, hierarchy, workflowID); err != nil {
			return createTypeFailedMsg{message: editErrorMessage(err)}
		}
		return typeCreatedMsg{}
	}
}
