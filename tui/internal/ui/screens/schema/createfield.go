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
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
)

// fieldTypes are the ten IssueFieldType values in the order the create picker lists them —
// the backend's declaration order, but with PERCENTAGE moved above SELECT_OPTION so the two
// option-carrying types (SELECT_OPTION, CHECKLIST) sit together at the tail.
var fieldTypes = []string{
	"TEXT", "SHORT_TEXT", "INTEGER", "DECIMAL", "TIMESTAMP",
	"DATE", "BOOLEAN", "PERCENTAGE", "SELECT_OPTION", "CHECKLIST",
}

// fieldTypeLabel is the human-readable name shown in the type picker.
func fieldTypeLabel(t string) string {
	switch t {
	case "TEXT":
		return "Text (multi-line)"
	case "SHORT_TEXT":
		return "Short text"
	case "INTEGER":
		return "Integer"
	case "DECIMAL":
		return "Decimal"
	case "TIMESTAMP":
		return "Timestamp"
	case "DATE":
		return "Date"
	case "BOOLEAN":
		return "Boolean"
	case "SELECT_OPTION":
		return "Select (one option)"
	case "PERCENTAGE":
		return "Percentage"
	case "CHECKLIST":
		return "Checklist (many options)"
	}
	return t
}

// fieldTypeGlyph maps a field type to its glyph, empty on plain terminals so the bare label shows.
func fieldTypeGlyph(g glyph.Set, fieldType string) string {
	switch fieldType {
	case "TEXT":
		return g.Or(g.SymbolString, "")
	case "SHORT_TEXT":
		return g.Or(g.WholeWord, "")
	case "SELECT_OPTION":
		return g.Or(g.SymbolEnum, "")
	case "CHECKLIST":
		return g.Or(g.Checklist, "")
	case "BOOLEAN":
		return g.Or(g.SymbolBoolean, "")
	case "DATE":
		return g.Or(g.Calendar, "")
	case "DECIMAL":
		return g.Or(g.Decimal, "")
	case "INTEGER":
		return g.Or(g.Number, "")
	case "PERCENTAGE":
		return g.Or(g.Percent, "")
	case "TIMESTAMP":
		return g.Or(g.Clock, "")
	}
	return ""
}

// canHaveOptions reports whether a field type carries a predefined option list (SELECT_OPTION /
// CHECKLIST), matching the backend's IssueFieldType.canHaveOptions.
func canHaveOptions(fieldType string) bool {
	return fieldType == "SELECT_OPTION" || fieldType == "CHECKLIST"
}

// create-field focus stops.
const (
	cffName = iota
	cffType
	cffRequired
	cffDesc
	cffSubmit
	cffCancel
)

// createFieldForm is the "New Field" modal for adding a custom field to an issue type. Unlike the
// edit form the type is chosen here (fixed thereafter), from a dropdown. Options are not collected
// here — a SELECT_OPTION / CHECKLIST field starts empty and its options are populated afterward
// through the options editor.
type createFieldForm struct {
	deps     deps.Deps
	typeID   int
	position int // the append position, computed at open

	name     textinput.Model
	ftype    string
	required bool
	desc     textarea.Model
	spinner  spinner.Model

	pickOpen bool
	pick     picker

	focus      int
	hover      int
	nameErr    string
	status     string
	submitting bool
}

func newCreateFieldForm(d deps.Deps, typeID, position int) createFieldForm {
	n := textinput.New()
	n.Prompt = ""
	n.SetWidth(editFieldW)
	n.CharLimit = 64

	da := textarea.New()
	da.Prompt = ""
	da.ShowLineNumbers = false
	da.CharLimit = 255
	da.SetWidth(editFieldW)
	da.SetHeight(editDescH)

	f := createFieldForm{
		deps: d, typeID: typeID, position: position,
		name: n, ftype: fieldTypes[0], desc: da, spinner: spinner.New(),
		focus: cffName, hover: -1,
	}
	f.name.Focus()
	return f
}

func (f createFieldForm) Init() tea.Cmd { return textinput.Blink }

func (f createFieldForm) fields() []int {
	return []int{cffName, cffType, cffRequired, cffDesc, cffSubmit, cffCancel}
}

func (f createFieldForm) Update(msg tea.Msg) (createFieldForm, tea.Cmd) {
	switch msg := msg.(type) {
	case createFieldFailedMsg:
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
		if !f.pickOpen {
			f.hover = f.hitZone(msg)
		}
		return f, nil
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f.updateInputs(msg)
}

func (f createFieldForm) onKey(msg tea.KeyPressMsg) (createFieldForm, tea.Cmd) {
	if f.submitting {
		return f, nil
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
		if f.focus != cffDesc {
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != cffDesc {
			return f.moveFocus(1)
		}
	case "left", "right", "space":
		if f.focus == cffRequired {
			f.required = !f.required
			return f, nil
		}
	case "enter":
		switch f.focus {
		case cffType:
			return f.openTypePicker(), nil
		case cffRequired:
			f.required = !f.required
			return f, nil
		case cffSubmit:
			return f.submit()
		case cffCancel:
			return f, cancelCreateField
		case cffName:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

func (f createFieldForm) pickKey(msg tea.KeyPressMsg) createFieldForm {
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

func (f createFieldForm) openTypePicker() createFieldForm {
	opts := make([]pickerOption, 0, len(fieldTypes))
	for _, t := range fieldTypes {
		// a trailing space on the glyph gives it one extra cell before the label (the picker adds
		// its own single space), matching the "glyph  label" spacing used elsewhere
		lead := fieldTypeGlyph(f.deps.Glyphs, t)
		if lead != "" {
			lead += " "
		}
		opts = append(opts, pickerOption{value: t, label: fieldTypeLabel(t), lead: lead})
	}
	f.pick = newPicker("Field type", opts, f.ftype, len(fieldTypes), editFieldW)
	f.pickOpen = true
	return f
}

func (f createFieldForm) applyPick() createFieldForm {
	opt, ok := f.pick.selected()
	f.pickOpen = false
	if ok {
		f.ftype = opt.value
	}
	return f
}

func (f createFieldForm) moveFocus(delta int) (createFieldForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f createFieldForm) focusOn(id int) (createFieldForm, tea.Cmd) {
	f.focus = id
	f.name.Blur()
	f.desc.Blur()
	var cmd tea.Cmd
	switch id {
	case cffName:
		cmd = f.name.Focus()
	case cffDesc:
		cmd = f.desc.Focus()
	}
	return f, cmd
}

func (f createFieldForm) typeIntoFocused(msg tea.KeyPressMsg) (createFieldForm, tea.Cmd) {
	f.status = ""
	var cmd tea.Cmd
	switch f.focus {
	case cffName:
		f.nameErr = ""
		f.name, cmd = f.name.Update(msg)
	case cffDesc:
		f.desc, cmd = f.desc.Update(msg)
	}
	return f, cmd
}

func (f createFieldForm) updateInputs(msg tea.Msg) (createFieldForm, tea.Cmd) {
	var nc, dc tea.Cmd
	f.name, nc = f.name.Update(msg)
	f.desc, dc = f.desc.Update(msg)
	return f, tea.Batch(nc, dc)
}

func (f createFieldForm) submit() (createFieldForm, tea.Cmd) {
	f.nameErr, f.status = "", ""
	name := strings.TrimSpace(f.name.Value())
	switch {
	case name == "":
		f.nameErr = "Required field"
		return f.focusOn(cffName)
	case len(name) > 64:
		f.nameErr = "64 characters max"
		return f.focusOn(cffName)
	}
	f.submitting = true
	return f, tea.Batch(
		createField(f.deps, f.typeID, name, strings.TrimSpace(f.desc.Value()), f.ftype, f.required, f.position),
		f.spinner.Tick,
	)
}

// ---- click routing ----

func (f createFieldForm) zoneID(id int) string {
	switch id {
	case cffName:
		return "createfield.name"
	case cffType:
		return "createfield.type"
	case cffRequired:
		return "createfield.required"
	case cffDesc:
		return "createfield.desc"
	case cffSubmit:
		return "createfield.save"
	case cffCancel:
		return "createfield.cancel"
	}
	return ""
}

func (f createFieldForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(f.zoneID(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f createFieldForm) onClick(msg tea.MouseClickMsg) (createFieldForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
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
	case cffName, cffDesc:
		return f.focusOn(id)
	case cffType:
		ff, _ := f.focusOn(cffType)
		return ff.openTypePicker(), nil
	case cffRequired:
		ff, _ := f.focusOn(cffRequired)
		ff.required = !ff.required
		return ff, nil
	case cffSubmit:
		return f.submit()
	case cffCancel:
		return f, cancelCreateField
	}
	return f, nil
}

// ---- view ----

func (f createFieldForm) View() string {
	if f.pickOpen {
		return f.pick.View(f.deps.Styles)
	}
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("New Field", body, f.deps.Styles.Theme.Primary)
}

func (f createFieldForm) body() string {
	rows := []string{
		f.field(cffName, "Name", fixEdit(f.name.View(), 1), f.nameErr),
		f.field(cffType, "Type", f.typeContent(), ""),
		f.field(cffRequired, "Required", f.requiredContent(), ""),
		f.field(cffDesc, "Description", fixEdit(f.desc.View(), editDescH), ""),
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

// typeContent shows the chosen field type as glyph + label with a hint that enter opens the picker.
func (f createFieldForm) typeContent() string {
	label := fieldTypeLabel(f.ftype)
	if gl := fieldTypeGlyph(f.deps.Glyphs, f.ftype); gl != "" {
		label = gl + " " + label
	}
	body := lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Text).Render(label)
	return alignRow(body, hintBar(f.deps.Styles, "enter", "▾"), editFieldW, lipgloss.NewStyle())
}

func (f createFieldForm) requiredContent() string {
	val := "No"
	if f.required {
		val = "Yes"
	}
	label := lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Text).Bold(true).Render(val)
	return alignRow(label, hintBar(f.deps.Styles, "space", "toggle"), editFieldW, lipgloss.NewStyle())
}

func (f createFieldForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBox(label, content, f.fieldBorderColor(id, errMsg))
	if zid := f.zoneID(id); zid != "" {
		box = zone.Mark(zid, box)
	}
	if id == cffName {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.errText(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f createFieldForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Create", "createfield.save", f.focus == cffSubmit, f.hover == cffSubmit),
		" ",
		f.button("Cancel", "createfield.cancel", f.focus == cffCancel, f.hover == cffCancel),
	)
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f createFieldForm) button(label, id string, focused, hovered bool) string {
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

func (f createFieldForm) fieldBorderColor(id int, errMsg string) color.Color {
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

func (f createFieldForm) errText(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f createFieldForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
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
	case cffType:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "pick type")))
	case cffRequired:
		binds = append(binds, key.NewBinding(key.WithKeys("space"), key.WithHelp("space", "toggle")))
	default:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "create")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

// ---- messages ----

type fieldCreatedMsg struct{ typeID int }

type createFieldFailedMsg struct{ message string }

type createFieldCancelledMsg struct{}

func cancelCreateField() tea.Msg { return createFieldCancelledMsg{} }

func createField(d deps.Deps, typeID int, name, desc, ftype string, required bool, position int) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.CreateIssueField(context.Background(), typeID, name, desc, ftype, required, position, nil); err != nil {
			return createFieldFailedMsg{message: editErrorMessage(err)}
		}
		return fieldCreatedMsg{typeID: typeID}
	}
}
