package schema

import (
	"image/color"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// edge sub-form focus zones.
const (
	egName = iota
	egSource
	egTarget
	egSave
	egCancel
)

// edgeForm creates a new transition or rewires an existing one before it is folded into the
// graph editor. Source and target are chosen from the editor's current states. The name is
// editable only for a new transition (an existing one is renamed via the per-element editor,
// since a whole-graph replace preserves its name). It never touches the backend.
type edgeForm struct {
	deps  deps.Deps
	title string
	isNew bool

	name   textinput.Model
	srcKey string
	tgtKey string
	states []flowState

	focus   int
	hover   int
	nameErr string

	pickOpen  bool
	pick      picker
	pickField int

	done      bool
	cancelled bool
}

func newEdgeForm(d deps.Deps, title string, isNew bool, name, srcKey, tgtKey string, states []flowState) edgeForm {
	n := textinput.New()
	n.Prompt = ""
	n.SetWidth(nodeFieldW)
	n.SetValue(name)
	f := edgeForm{
		deps: d, title: title, isNew: isNew,
		name: n, srcKey: srcKey, tgtKey: tgtKey, states: states,
		focus: egSource, hover: -1,
	}
	if isNew {
		f.focus = egName
		f.name.Focus()
	}
	return f
}

func (f edgeForm) result() (name, srcKey, tgtKey string) {
	return strings.TrimSpace(f.name.Value()), f.srcKey, f.tgtKey
}

func (f edgeForm) fields() []int {
	if f.isNew {
		return []int{egName, egSource, egTarget, egSave, egCancel}
	}
	return []int{egSource, egTarget, egSave, egCancel}
}

func (f edgeForm) Update(msg tea.Msg) (edgeForm, tea.Cmd) {
	switch msg := msg.(type) {
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
	var cmd tea.Cmd
	f.name, cmd = f.name.Update(msg)
	return f, cmd
}

func (f edgeForm) onKey(msg tea.KeyPressMsg) (edgeForm, tea.Cmd) {
	if f.pickOpen {
		return f.pickKey(msg), nil
	}
	switch msg.String() {
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		return f.moveFocus(-1)
	case "down":
		return f.moveFocus(1)
	case "enter":
		switch f.focus {
		case egSource, egTarget:
			return f.openPicker(f.focus), nil
		case egSave:
			return f.submit()
		case egCancel:
			f.cancelled = true
			return f, nil
		case egName:
			return f.moveFocus(1)
		}
	case "esc":
		f.cancelled = true
		return f, nil
	}
	return f.typeIntoName(msg)
}

func (f edgeForm) pickKey(msg tea.KeyPressMsg) edgeForm {
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

func (f edgeForm) openPicker(field int) edgeForm {
	opts := make([]pickerOption, 0, len(f.states))
	for _, st := range f.states {
		opts = append(opts, pickerOption{value: st.key, label: st.name, lead: components.ColorSwatch(st.color)})
	}
	title, cur := "Source state", f.srcKey
	if field == egTarget {
		title, cur = "Target state", f.tgtKey
	}
	f.pick = newPicker(title, opts, cur, 8, nodeFieldW)
	f.pickField, f.pickOpen = field, true
	return f
}

func (f edgeForm) applyPick() edgeForm {
	opt, ok := f.pick.selected()
	f.pickOpen = false
	if !ok {
		return f
	}
	if f.pickField == egTarget {
		f.tgtKey = opt.value
	} else {
		f.srcKey = opt.value
	}
	return f
}

func (f edgeForm) moveFocus(delta int) (edgeForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f edgeForm) focusOn(id int) (edgeForm, tea.Cmd) {
	f.focus = id
	f.name.Blur()
	var cmd tea.Cmd
	if id == egName {
		cmd = f.name.Focus()
	}
	return f, cmd
}

func (f edgeForm) typeIntoName(msg tea.KeyPressMsg) (edgeForm, tea.Cmd) {
	if f.focus != egName {
		return f, nil
	}
	f.nameErr = ""
	var cmd tea.Cmd
	f.name, cmd = f.name.Update(msg)
	return f, cmd
}

func (f edgeForm) submit() (edgeForm, tea.Cmd) {
	if f.isNew {
		name := strings.TrimSpace(f.name.Value())
		switch {
		case name == "":
			f.nameErr = "Required field"
			return f.focusOn(egName)
		case len(name) > 100:
			f.nameErr = "100 characters max"
			return f.focusOn(egName)
		}
	}
	if f.srcKey == "" || f.tgtKey == "" {
		f.nameErr = ""
		return f, nil // both endpoints are required — leave the form open
	}
	f.done = true
	return f, nil
}

func (f edgeForm) stateName(key string) string {
	for _, st := range f.states {
		if st.key == key {
			return st.name
		}
	}
	return ""
}

func (f edgeForm) stateColor(key string) string {
	for _, st := range f.states {
		if st.key == key {
			return st.color
		}
	}
	return ""
}

func (f edgeForm) View() string {
	if f.pickOpen {
		return f.pick.View(f.deps.Styles)
	}
	var rows []string
	if f.isNew {
		rows = append(rows, f.field(egName, "Name", fixNode(f.name.View(), 1), f.nameErr))
	} else {
		rows = append(rows, f.nameDisplay())
	}
	rows = append(rows,
		f.endpoint(egSource, "Source", f.srcKey),
		f.endpoint(egTarget, "Target", f.tgtKey),
		"",
		f.buttons(),
	)
	body := lipgloss.NewStyle().Padding(1, 1).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered(f.title, body, f.deps.Styles.Theme.Primary)
}

// nameDisplay shows an existing transition's name read-only, since a rewire preserves it.
func (f edgeForm) nameDisplay() string {
	t := f.deps.Styles.Theme
	name := strings.TrimSpace(f.name.Value())
	if name == "" {
		name = "(unnamed)"
	}
	content := alignRow(lipgloss.NewStyle().Foreground(t.Text).Render(name), f.deps.Styles.Muted.Render("rename via edit"), nodeFieldW, lipgloss.NewStyle())
	return components.TitledBox("Name", content, t.Border)
}

func (f edgeForm) endpoint(id int, label, key string) string {
	t := f.deps.Styles.Theme
	value := f.deps.Styles.Muted.Render("Select…")
	if name := f.stateName(key); name != "" {
		swatch := components.ColorSwatch(f.stateColor(key))
		value = lipgloss.NewStyle().Foreground(t.Text).Render(name)
		if swatch != "" {
			value = swatch + " " + value
		}
	}
	content := alignRow(value, hintBar(f.deps.Styles, "enter", "▾"), nodeFieldW, lipgloss.NewStyle())
	return zone.Mark(edgeZone(id), components.TitledBoxWeighted(label, content, f.fieldBorderColor(id), f.focus == id))
}

func (f edgeForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", "flow.edge.save", f.focus == egSave, f.hover == egSave),
		" ",
		f.button("Cancel", "flow.edge.cancel", f.focus == egCancel, f.hover == egCancel),
	)
	return lipgloss.PlaceHorizontal(nodeFieldW+4, lipgloss.Right, group)
}

func (f edgeForm) button(label, id string, focused, hovered bool) string {
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

func (f edgeForm) field(id int, label, content, errMsg string) string {
	borderCol := f.fieldBorderColor(id)
	if errMsg != "" {
		borderCol = f.deps.Styles.Theme.Error
	}
	box := zone.Mark(edgeZone(id), components.TitledBoxWeighted(label, content, borderCol, f.focus == id))
	if id == egName {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.errText(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f edgeForm) fieldBorderColor(id int) color.Color {
	t := f.deps.Styles.Theme
	switch {
	case f.focus == id:
		return t.Accent
	case f.hover == id:
		return t.Secondary
	default:
		return t.Primary
	}
}

func (f edgeForm) errText(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f edgeForm) HelpKeys() []key.Binding {
	if f.pickOpen {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	switch f.focus {
	case egSource, egTarget:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "choose")))
	case egSave:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")))
}

func edgeZone(id int) string {
	switch id {
	case egName:
		return "flow.edge.name"
	case egSource:
		return "flow.edge.source"
	case egTarget:
		return "flow.edge.target"
	case egSave:
		return "flow.edge.save"
	case egCancel:
		return "flow.edge.cancel"
	}
	return ""
}

func (f edgeForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(edgeZone(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f edgeForm) onClick(msg tea.MouseClickMsg) (edgeForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
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
	case egName:
		return f.focusOn(id)
	case egSource, egTarget:
		ff, _ := f.focusOn(id)
		return ff.openPicker(id), nil
	case egSave:
		return f.submit()
	case egCancel:
		f.cancelled = true
		return f, nil
	}
	return f, nil
}
