package schema

import (
	"context"
	"errors"
	"image/color"
	"net/http"
	"strings"

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

// editKind selects which metadata endpoint a save hits.
type editKind int

const (
	editState editKind = iota
	editTransition
	editWorkflow
	editIssueType
)

// fields present in the edit modal; efColor is skipped for transitions.
const (
	efName = iota
	efColor
	efDesc
	efSubmit
	efCancel
)

const (
	editFieldW = 40
	editDescH  = 3
)

// editForm is the "Edit …" modal for a single graph element's metadata. Only name, color
// (states/workflow) and description are editable here — category and wiring are not, so they
// have no fields. On save it fires the matching PATCH and closes.
type editForm struct {
	deps   deps.Deps
	kind   editKind
	wfID   int
	elemID int
	title  string

	name    textinput.Model
	desc    textarea.Model
	colors  []string // color options; nil for a kind without a color field
	colorIx int
	spinner spinner.Model

	picking bool        // the color swatch grid is open
	cpick   colorPicker // the grid, valid while picking

	focus      int
	hover      int
	nameErr    string
	status     string
	submitting bool
}

// newEditForm builds the modal seeded with the element's current values. withColor adds the
// color picker (states and workflows have one; transitions do not).
func newEditForm(d deps.Deps, kind editKind, wfID, elemID int, title, name, colorName, desc string, withColor bool) editForm {
	n := textinput.New()
	n.Prompt = ""
	n.SetWidth(editFieldW)
	n.SetValue(name)

	da := textarea.New()
	da.Prompt = ""
	da.ShowLineNumbers = false
	da.CharLimit = 255
	da.SetWidth(editFieldW)
	da.SetHeight(editDescH)
	da.SetValue(desc)

	f := editForm{
		deps: d, kind: kind, wfID: wfID, elemID: elemID, title: title,
		name: n, desc: da, spinner: spinner.New(), focus: efName, hover: -1,
	}
	if withColor {
		f.colors = components.ColorTypeNames()
		f.colorIx = max(0, indexOf(f.colors, strings.ToUpper(strings.TrimSpace(colorName))))
	}
	f.name.Focus()
	return f
}

func (f editForm) Init() tea.Cmd { return textinput.Blink }

// fields returns the field ids present for this kind, in tab order.
func (f editForm) fields() []int {
	fs := []int{efName}
	if f.colors != nil {
		fs = append(fs, efColor)
	}
	return append(fs, efDesc, efSubmit, efCancel)
}

func (f editForm) Update(msg tea.Msg) (editForm, tea.Cmd) { return f.route(msg) }

func (f editForm) route(msg tea.Msg) (editForm, tea.Cmd) {
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
		if f.picking {
			if i := f.cpick.hitCell(msg); i >= 0 {
				f.cpick.cursor = i
			}
			return f, nil
		}
		f.hover = f.hitZone(msg)
		return f, nil
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f.updateInputs(msg)
}

func (f editForm) onKey(msg tea.KeyPressMsg) (editForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	if f.picking {
		return f.pickKey(msg), nil
	}
	switch msg.String() {
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if f.focus != efDesc {
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != efDesc {
			return f.moveFocus(1)
		}
	case "enter":
		switch f.focus {
		case efSubmit:
			return f.submit()
		case efCancel:
			return f, cancelEdit
		case efColor:
			return f.openColorPicker(), nil
		case efName:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

// pickKey drives the open color grid: arrows/hjkl move, enter selects, esc closes it.
func (f editForm) pickKey(msg tea.KeyPressMsg) editForm {
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
		return f.applyColor()
	case "esc":
		f.picking = false
	}
	return f
}

func (f editForm) openColorPicker() editForm {
	f.cpick = newColorPicker("Pick a color", f.colors[f.colorIx], colorGridCols)
	f.picking = true
	return f
}

// applyColor commits the grid's highlighted swatch as the form's color and closes the grid.
func (f editForm) applyColor() editForm {
	if name, ok := f.cpick.selected(); ok {
		if i := indexOf(f.colors, name); i >= 0 {
			f.colorIx = i
		}
	}
	f.picking = false
	return f
}

func (f editForm) moveFocus(delta int) (editForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f editForm) focusOn(id int) (editForm, tea.Cmd) {
	f.focus = id
	f.name.Blur()
	f.desc.Blur()
	var cmd tea.Cmd
	switch id {
	case efName:
		cmd = f.name.Focus()
	case efDesc:
		cmd = f.desc.Focus()
	}
	return f, cmd
}

func (f editForm) typeIntoFocused(msg tea.KeyPressMsg) (editForm, tea.Cmd) {
	f.status = ""
	var cmd tea.Cmd
	switch f.focus {
	case efName:
		f.nameErr = ""
		f.name, cmd = f.name.Update(msg)
	case efDesc:
		f.desc, cmd = f.desc.Update(msg)
	}
	return f, cmd
}

func (f editForm) updateInputs(msg tea.Msg) (editForm, tea.Cmd) {
	var nc, dc tea.Cmd
	f.name, nc = f.name.Update(msg)
	f.desc, dc = f.desc.Update(msg)
	return f, tea.Batch(nc, dc)
}

func (f editForm) submit() (editForm, tea.Cmd) {
	f.nameErr, f.status = "", ""
	name := strings.TrimSpace(f.name.Value())
	switch {
	case name == "":
		f.nameErr = "Required field"
		return f.focusOn(efName)
	case len(name) > 100:
		f.nameErr = "100 characters max"
		return f.focusOn(efName)
	}
	color := ""
	if f.colors != nil {
		color = f.colors[f.colorIx]
	}
	f.submitting = true
	return f, tea.Batch(saveEdit(f.deps, f.kind, f.wfID, f.elemID, name, color, strings.TrimSpace(f.desc.Value())), f.spinner.Tick)
}

// ---- click routing ----

func (f editForm) zoneID(id int) string {
	switch id {
	case efName:
		return "edit.name"
	case efColor:
		return "edit.color"
	case efDesc:
		return "edit.desc"
	case efSubmit:
		return "edit.save"
	case efCancel:
		return "edit.cancel"
	}
	return ""
}

func (f editForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range []int{efName, efColor, efDesc, efSubmit, efCancel} {
		if zone.Get(f.zoneID(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f editForm) onClick(msg tea.MouseClickMsg) (editForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	if f.picking {
		if i := f.cpick.hitCell(msg); i >= 0 {
			f.cpick.cursor = i
			return f.applyColor(), nil
		}
		return f, nil
	}
	switch id := f.hitZone(msg); id {
	case efName, efDesc:
		return f.focusOn(id)
	case efColor:
		ff, _ := f.focusOn(efColor)
		return ff.openColorPicker(), nil // clicking the color field opens the swatch grid
	case efSubmit:
		return f.submit()
	case efCancel:
		return f, cancelEdit
	}
	return f, nil
}

// ---- view ----

func (f editForm) View() string {
	if f.picking {
		return f.cpick.View(f.deps.Styles)
	}
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered(f.title, body, f.deps.Styles.Theme.Primary)
}

func (f editForm) body() string {
	rows := []string{f.field(efName, "Name", fixEdit(f.name.View(), 1), f.nameErr)}
	if f.colors != nil {
		rows = append(rows, f.field(efColor, "Color", f.colorContent(), ""))
	}
	rows = append(rows, f.field(efDesc, "Description", fixEdit(f.desc.View(), editDescH), ""))
	switch {
	case f.submitting:
		rows = append(rows, lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Warning).Padding(0, 1).Render(f.spinner.View()+" Saving…"))
	case f.status != "":
		rows = append(rows, f.deps.Styles.Error.Padding(0, 1).Render(f.status))
	}
	rows = append(rows, "", f.buttons())
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// colorContent shows the current color as a swatch + name, with a hint that enter opens the
// swatch grid to change it.
func (f editForm) colorContent() string {
	name := f.colors[f.colorIx]
	body := components.ColorLabel(name)
	if sw := components.ColorSwatch(name); sw != "" {
		body = sw + " " + body
	}
	hint := hintBar(f.deps.Styles, "enter", "▾")
	return alignRow(body, hint, editFieldW, lipgloss.NewStyle())
}

func (f editForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBox(label, content, f.fieldBorderColor(id, errMsg))
	if zid := f.zoneID(id); zid != "" {
		box = zone.Mark(zid, box)
	}
	if id == efName {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.errText(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f editForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", "edit.save", f.focus == efSubmit, f.hover == efSubmit),
		" ",
		f.button("Cancel", "edit.cancel", f.focus == efCancel, f.hover == efCancel),
	)
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f editForm) button(label, id string, focused, hovered bool) string {
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

func (f editForm) fieldBorderColor(id int, errMsg string) color.Color {
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

func (f editForm) errText(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f editForm) HelpKeys() []key.Binding {
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
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	if f.focus == efColor {
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "pick color")))
	} else {
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

func fixEdit(s string, h int) string {
	return lipgloss.NewStyle().Width(editFieldW).MaxWidth(editFieldW).Height(h).MaxHeight(h).Render(s)
}

func indexOf(ss []string, v string) int {
	for i, s := range ss {
		if s == v {
			return i
		}
	}
	return -1
}

func indexOfInt(is []int, v int) int {
	for i, n := range is {
		if n == v {
			return i
		}
	}
	return -1
}

// ---- messages ----

type editSavedMsg struct{ wfID int }

type editFailedMsg struct{ message string }

type editCancelledMsg struct{}

func cancelEdit() tea.Msg { return editCancelledMsg{} }

func saveEdit(d deps.Deps, kind editKind, wfID, elemID int, name, color, desc string) tea.Cmd {
	return func() tea.Msg {
		var err error
		switch kind {
		case editState:
			err = d.Catalog.UpdateWorkflowState(context.Background(), wfID, elemID, name, color, desc)
		case editTransition:
			err = d.Catalog.UpdateWorkflowTransition(context.Background(), wfID, elemID, name, desc)
		case editWorkflow:
			err = d.Catalog.UpdateWorkflow(context.Background(), wfID, name, color, desc)
		case editIssueType:
			// wfID carries the issue type id here (the edit form is shared across entity kinds)
			err = d.Catalog.UpdateIssueType(context.Background(), wfID, name, color, desc)
		}
		if err != nil {
			return editFailedMsg{message: editErrorMessage(err)}
		}
		return editSavedMsg{wfID: wfID}
	}
}

func editErrorMessage(err error) string {
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "That name is already taken."
		case http.StatusBadRequest:
			return "Invalid details."
		case http.StatusForbidden:
			return "You do not have permission to edit this."
		}
	}
	return "Could not save. Try again."
}
