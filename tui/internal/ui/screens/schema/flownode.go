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
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// node sub-form focus zones.
const (
	nfName = iota
	nfCategory
	nfColor
	nfSave
	nfCancel
)

const nodeFieldW = 40

// stateCategories are the four StateCategory values in the order the sub-form cycles them.
var stateCategories = []string{"INITIAL", "ACTIVE", "COMPLETED", "ABORTED"}

func categoryColor(t theme.Theme, category string) color.Color {
	switch category {
	case "INITIAL":
		return t.Primary
	case "COMPLETED":
		return t.Success
	case "ABORTED":
		return t.Error
	default:
		return t.Text
	}
}

// nodeForm creates or edits a single NEW state's fields — name, category, color — before it is
// folded into the graph editor. It never touches the backend. The graph editor serializes every
// node together in one whole-graph replace. done/cancelled tell the graph editor how the sub-form closed.
type nodeForm struct {
	deps  deps.Deps
	title string

	name     textinput.Model
	category string
	colors   []string
	colorIx  int

	focus   int
	hover   int
	nameErr string

	picking bool
	cpick   colorPicker

	done      bool
	cancelled bool
}

func newNodeForm(d deps.Deps, title, name, category, colorName string) nodeForm {
	n := textinput.New()
	n.Prompt = ""
	n.SetWidth(nodeFieldW)
	n.SetValue(name)
	colors := components.ColorTypeNames()
	f := nodeForm{
		deps: d, title: title, name: n, category: category, colors: colors,
		colorIx: max(0, indexOf(colors, strings.ToUpper(strings.TrimSpace(colorName)))),
		focus:   nfName, hover: -1,
	}
	if f.category == "" {
		f.category = "ACTIVE"
	}
	f.name.Focus()
	return f
}

func (f nodeForm) result() (name, category, colorName string) {
	return strings.TrimSpace(f.name.Value()), f.category, f.colors[f.colorIx]
}

func (f nodeForm) fields() []int { return []int{nfName, nfCategory, nfColor, nfSave, nfCancel} }

func (f nodeForm) Update(msg tea.Msg) (nodeForm, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.MouseMotionMsg:
		if f.picking {
			if i := f.cpick.HitCell(msg); i >= 0 {
				f.cpick.Cursor = i
			}
			return f, nil
		}
		f.hover = f.hitZone(msg)
		return f, nil
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	var cmd tea.Cmd
	f.name, cmd = f.name.Update(msg)
	return f, cmd
}

func (f nodeForm) onKey(msg tea.KeyPressMsg) (nodeForm, tea.Cmd) {
	if f.picking {
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
	case "left", "h":
		if f.focus == nfCategory {
			f.category = cycle(stateCategories, f.category, -1)
		}
		return f, nil
	case "right", "l":
		if f.focus == nfCategory {
			f.category = cycle(stateCategories, f.category, 1)
		}
		return f, nil
	case "enter":
		switch f.focus {
		case nfColor:
			return f.openColorPicker(), nil
		case nfSave:
			return f.submit()
		case nfCancel:
			f.cancelled = true
			return f, nil
		case nfName:
			return f.moveFocus(1)
		}
	case "esc":
		f.cancelled = true
		return f, nil
	}
	return f.typeIntoName(msg)
}

func (f nodeForm) pickKey(msg tea.KeyPressMsg) nodeForm {
	switch msg.String() {
	case "left", "h":
		f.cpick = f.cpick.Move(-1, 0)
	case "right", "l":
		f.cpick = f.cpick.Move(1, 0)
	case "up", "k":
		f.cpick = f.cpick.Move(0, -1)
	case "down", "j":
		f.cpick = f.cpick.Move(0, 1)
	case "enter", "space":
		return f.applyColor()
	case "esc":
		f.picking = false
	}
	return f
}

func (f nodeForm) openColorPicker() nodeForm {
	f.cpick = newColorPicker("Pick a color", f.colors[f.colorIx], colorGridCols)
	f.picking = true
	return f
}

func (f nodeForm) applyColor() nodeForm {
	if name, ok := f.cpick.Selected(); ok {
		if i := indexOf(f.colors, name); i >= 0 {
			f.colorIx = i
		}
	}
	f.picking = false
	return f
}

func (f nodeForm) moveFocus(delta int) (nodeForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f nodeForm) focusOn(id int) (nodeForm, tea.Cmd) {
	f.focus = id
	f.name.Blur()
	var cmd tea.Cmd
	if id == nfName {
		cmd = f.name.Focus()
	}
	return f, cmd
}

func (f nodeForm) typeIntoName(msg tea.KeyPressMsg) (nodeForm, tea.Cmd) {
	if f.focus != nfName {
		return f, nil
	}
	f.nameErr = ""
	var cmd tea.Cmd
	f.name, cmd = f.name.Update(msg)
	return f, cmd
}

func (f nodeForm) submit() (nodeForm, tea.Cmd) {
	name := strings.TrimSpace(f.name.Value())
	switch {
	case name == "":
		f.nameErr = "Required field"
		return f.focusOn(nfName)
	case len(name) > 100:
		f.nameErr = "100 characters max"
		return f.focusOn(nfName)
	}
	f.done = true
	return f, nil
}

func (f nodeForm) View() string {
	if f.picking {
		return f.cpick.View(f.deps.Styles)
	}
	rows := []string{
		f.field(nfName, "Name", fixNode(f.name.View(), 1), f.nameErr),
		f.field(nfCategory, "Category", f.categoryContent(), ""),
		f.field(nfColor, "Color", f.colorContent(), ""),
		"",
		f.buttons(),
	}
	body := lipgloss.NewStyle().Padding(1, 1).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered(f.title, body, f.deps.Styles.Theme.Primary)
}

func (f nodeForm) categoryContent() string {
	t := f.deps.Styles.Theme
	label := lipgloss.NewStyle().Foreground(categoryColor(t, f.category)).Bold(true).Render(f.category)
	if f.focus == nfCategory {
		left := lipgloss.NewStyle().Foreground(t.Accent).Render("‹ ")
		right := lipgloss.NewStyle().Foreground(t.Accent).Render(" ›")
		return alignRow(left+label+right, hintBar(f.deps.Styles, "←/→", ""), nodeFieldW, lipgloss.NewStyle())
	}
	return alignRow("  "+label, "", nodeFieldW, lipgloss.NewStyle())
}

func (f nodeForm) colorContent() string {
	name := f.colors[f.colorIx]
	body := components.ColorLabel(name)
	if sw := components.ColorSwatch(name); sw != "" {
		body = sw + " " + body
	}
	return alignRow(body, hintBar(f.deps.Styles, "enter", "▾"), nodeFieldW, lipgloss.NewStyle())
}

func (f nodeForm) field(id int, label, content, errMsg string) string {
	box := zone.Mark(nodeZone(id), components.TitledBoxWeighted(label, content, f.fieldBorderColor(id, errMsg), f.focus == id))
	if id == nfName {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.errText(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f nodeForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", "flow.node.save", f.focus == nfSave, f.hover == nfSave),
		" ",
		f.button("Cancel", "flow.node.cancel", f.focus == nfCancel, f.hover == nfCancel),
	)
	return lipgloss.PlaceHorizontal(nodeFieldW+4, lipgloss.Right, group)
}

func (f nodeForm) button(label, id string, focused, hovered bool) string {
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

func (f nodeForm) fieldBorderColor(id int, errMsg string) color.Color {
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

func (f nodeForm) errText(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f nodeForm) HelpKeys() []key.Binding {
	if f.picking {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down", "left", "right"), key.WithHelp("↑↓←→", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	switch f.focus {
	case nfCategory:
		binds = append(binds, key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "category")))
	case nfColor:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "pick color")))
	default:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")))
}

func nodeZone(id int) string {
	switch id {
	case nfName:
		return "flow.node.name"
	case nfCategory:
		return "flow.node.category"
	case nfColor:
		return "flow.node.color"
	case nfSave:
		return "flow.node.save"
	case nfCancel:
		return "flow.node.cancel"
	}
	return ""
}

func (f nodeForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(nodeZone(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f nodeForm) onClick(msg tea.MouseClickMsg) (nodeForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return f, nil
	}
	if f.picking {
		if i := f.cpick.HitCell(msg); i >= 0 {
			f.cpick.Cursor = i
			return f.applyColor(), nil
		}
		return f, nil
	}
	switch id := f.hitZone(msg); id {
	case nfName, nfCategory:
		return f.focusOn(id)
	case nfColor:
		ff, _ := f.focusOn(nfColor)
		return ff.openColorPicker(), nil
	case nfSave:
		return f.submit()
	case nfCancel:
		f.cancelled = true
		return f, nil
	}
	return f, nil
}

func fixNode(s string, h int) string {
	return lipgloss.NewStyle().Width(nodeFieldW).MaxWidth(nodeFieldW).Height(h).MaxHeight(h).Render(s)
}

func cycle(vals []string, cur string, delta int) string {
	i := indexOf(vals, cur)
	if i < 0 {
		i = 0
	}
	return vals[(i+delta+len(vals))%len(vals)]
}
