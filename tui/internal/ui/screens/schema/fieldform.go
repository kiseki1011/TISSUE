package schema

import (
	"context"
	"image/color"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	"charm.land/bubbles/v2/textarea"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

const (
	ffName = iota
	ffDesc
	ffRequired
	ffSubmit
	ffCancel
)

// fieldForm edits one custom field's metadata. Type is fixed at creation and options go through
// their own endpoints.
type fieldForm struct {
	deps    deps.Deps
	typeID  int // owning type, for cache invalidation on save
	fieldID int

	name     textinput.Model
	desc     textarea.Model
	required bool
	spinner  spinner.Model

	focus      int
	hover      int
	nameErr    string
	status     string
	submitting bool
}

func newFieldForm(d deps.Deps, typeID, fieldID int, name, desc string, required bool) fieldForm {
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

	f := fieldForm{
		deps: d, typeID: typeID, fieldID: fieldID, required: required,
		name: n, desc: da, spinner: spinner.New(), focus: ffName, hover: -1,
	}
	f.name.Focus()
	return f
}

func (f fieldForm) Init() tea.Cmd { return textinput.Blink }

func (f fieldForm) fields() []int { return []int{ffName, ffDesc, ffRequired, ffSubmit, ffCancel} }

func (f fieldForm) Update(msg tea.Msg) (fieldForm, tea.Cmd) {
	switch msg := msg.(type) {
	case fieldFailedMsg:
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

func (f fieldForm) onKey(msg tea.KeyPressMsg) (fieldForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	switch msg.String() {
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if f.focus != ffDesc {
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != ffDesc {
			return f.moveFocus(1)
		}
	case "left", "right", "space":
		if f.focus == ffRequired {
			f.required = !f.required
			return f, nil
		}
	case "enter":
		switch f.focus {
		case ffSubmit:
			return f.submit()
		case ffCancel:
			return f, cancelField
		case ffRequired:
			f.required = !f.required
			return f, nil
		case ffName:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

func (f fieldForm) moveFocus(delta int) (fieldForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f fieldForm) focusOn(id int) (fieldForm, tea.Cmd) {
	f.focus = id
	f.name.Blur()
	f.desc.Blur()
	var cmd tea.Cmd
	switch id {
	case ffName:
		cmd = f.name.Focus()
	case ffDesc:
		cmd = f.desc.Focus()
	}
	return f, cmd
}

func (f fieldForm) typeIntoFocused(msg tea.KeyPressMsg) (fieldForm, tea.Cmd) {
	f.status = ""
	var cmd tea.Cmd
	switch f.focus {
	case ffName:
		f.nameErr = ""
		f.name, cmd = f.name.Update(msg)
	case ffDesc:
		f.desc, cmd = f.desc.Update(msg)
	}
	return f, cmd
}

func (f fieldForm) updateInputs(msg tea.Msg) (fieldForm, tea.Cmd) {
	var nc, dc tea.Cmd
	f.name, nc = f.name.Update(msg)
	f.desc, dc = f.desc.Update(msg)
	return f, tea.Batch(nc, dc)
}

func (f fieldForm) submit() (fieldForm, tea.Cmd) {
	f.nameErr, f.status = "", ""
	name := strings.TrimSpace(f.name.Value())
	switch {
	case name == "":
		f.nameErr = "Required field"
		return f.focusOn(ffName)
	case len(name) > 100:
		f.nameErr = "100 characters max"
		return f.focusOn(ffName)
	}
	f.submitting = true
	return f, tea.Batch(saveField(f.deps, f.typeID, f.fieldID, name, strings.TrimSpace(f.desc.Value()), f.required), f.spinner.Tick)
}

func (f fieldForm) zoneID(id int) string {
	switch id {
	case ffName:
		return "field.name"
	case ffDesc:
		return "field.desc"
	case ffRequired:
		return "field.required"
	case ffSubmit:
		return "field.save"
	case ffCancel:
		return "field.cancel"
	}
	return ""
}

func (f fieldForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(f.zoneID(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f fieldForm) onClick(msg tea.MouseClickMsg) (fieldForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	switch id := f.hitZone(msg); id {
	case ffName, ffDesc:
		return f.focusOn(id)
	case ffRequired:
		ff, _ := f.focusOn(ffRequired)
		ff.required = !ff.required
		return ff, nil
	case ffSubmit:
		return f.submit()
	case ffCancel:
		return f, cancelField
	}
	return f, nil
}

func (f fieldForm) View() string {
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("Edit Field", body, f.deps.Styles.Theme.Primary)
}

// FocusRow reports the focused row and height so a windowed modal can scroll it into view.
// chromeTop 2 = top border + the padding row.
func (f fieldForm) FocusRow() (int, int, bool) {
	const chromeTop = 2
	nameH := lipgloss.Height(f.field(ffName, "Name", fixEdit(f.name.View(), 1), f.nameErr))
	descH := lipgloss.Height(f.field(ffDesc, "Description", fixEdit(f.desc.View(), editDescH), ""))
	reqH := lipgloss.Height(f.field(ffRequired, "Required", f.requiredContent(), ""))
	switch f.focus {
	case ffName:
		return chromeTop, nameH, true
	case ffDesc:
		return chromeTop + nameH, descH, true
	case ffRequired:
		return chromeTop + nameH + descH, reqH, true
	default: // the Save/Cancel buttons row
		line := nameH + descH + reqH
		if f.submitting || f.status != "" {
			line++
		}
		line++ // the blank row before the buttons
		return chromeTop + line, lipgloss.Height(f.buttons()), true
	}
}

func (f fieldForm) body() string {
	rows := []string{
		f.field(ffName, "Name", fixEdit(f.name.View(), 1), f.nameErr),
		f.field(ffDesc, "Description", fixEdit(f.desc.View(), editDescH), ""),
		f.field(ffRequired, "Required", f.requiredContent(), ""),
	}
	switch {
	case f.submitting:
		rows = append(rows, lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Warning).Padding(0, 1).Render(f.spinner.View()+" Saving…"))
	case f.status != "":
		rows = append(rows, f.deps.Styles.Error.Padding(0, 1).Render(f.status))
	}
	rows = append(rows, "", f.buttons())
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

func (f fieldForm) requiredContent() string {
	t := f.deps.Styles.Theme
	val := "No"
	if f.required {
		val = "Yes"
	}
	label := lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(val)
	return alignRow(label, hintBar(f.deps.Styles, "space", "toggle"), editFieldW, lipgloss.NewStyle())
}

func (f fieldForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBoxWeighted(label, content, f.fieldBorderColor(id, errMsg), f.focus == id)
	if zid := f.zoneID(id); zid != "" {
		box = zone.Mark(zid, box)
	}
	if id == ffName {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.errText(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f fieldForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", "field.save", f.focus == ffSubmit, f.hover == ffSubmit),
		" ",
		f.button("Cancel", "field.cancel", f.focus == ffCancel, f.hover == ffCancel),
	)
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f fieldForm) button(label, id string, focused, hovered bool) string {
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

func (f fieldForm) fieldBorderColor(id int, errMsg string) color.Color {
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

func (f fieldForm) errText(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f fieldForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	switch f.focus {
	case ffRequired:
		binds = append(binds, key.NewBinding(key.WithKeys("space"), key.WithHelp("space", "toggle")))
	default:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

type fieldSavedMsg struct{ typeID int }

type fieldFailedMsg struct{ message string }

type fieldCancelledMsg struct{}

func cancelField() tea.Msg { return fieldCancelledMsg{} }

func saveField(d deps.Deps, typeID, fieldID int, name, desc string, required bool) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.UpdateIssueField(context.Background(), fieldID, name, desc, required); err != nil {
			return fieldFailedMsg{message: editErrorMessage(err)}
		}
		return fieldSavedMsg{typeID: typeID}
	}
}
