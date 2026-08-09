package project

import (
	"image/color"
	"strings"
	"time"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// the sprint edit form's focusable controls, in tab order.
const (
	sefTitle = iota
	sefGoal
	sefDue
	sefSave
	sefCancel
)

// sprint field limits, mirroring the backend's SprintConstraintPolicy.
const (
	sprintTitleMin = 2
	sprintTitleMax = 50
	sprintGoalMax  = 255
)

// sprintEditForm is the sprint title/goal/due modal, serving both "New sprint" and "Edit sprint".
// Creating drops the Due field: a new sprint starts in PLANNING and its due date is chosen when it is
// started, so offering it here would collect a value the create call cannot send. Status changes go
// through the separate start/complete actions.
type sprintEditForm struct {
	deps     deps.Deps
	creating bool

	title  textinput.Model
	goal   textinput.Model
	dueAt  time.Time // the chosen due date (zero = none), set from the calendar picker
	dueSet bool

	focus    int
	hover    int
	titleErr string
}

func newSprintEditForm(d deps.Deps, sp domain.SprintSummary) sprintEditForm {
	title := textinput.New()
	title.Prompt = ""
	title.SetWidth(editFieldW)
	title.CharLimit = sprintTitleMax
	title.SetValue(sp.Title)

	goal := textinput.New()
	goal.Prompt = ""
	goal.SetWidth(editFieldW)
	goal.CharLimit = sprintGoalMax
	goal.Placeholder = "None"
	goal.SetValue(sp.Goal)

	f := sprintEditForm{
		deps: d, title: title, goal: goal,
		dueAt: sp.DueAt, dueSet: !sp.DueAt.IsZero(),
		focus: sefTitle, hover: -1,
	}
	f.title.Focus()
	return f
}

// newSprintCreateForm is the same modal with empty fields and no Due row, for adding a sprint.
func newSprintCreateForm(d deps.Deps) sprintEditForm {
	f := newSprintEditForm(d, domain.SprintSummary{})
	f.creating = true
	return f
}

// setDue records a calendar pick (set=false clears the due date).
func (f sprintEditForm) setDue(v time.Time, set bool) sprintEditForm {
	f.dueAt, f.dueSet = v, set
	return f
}

func (f sprintEditForm) Init() tea.Cmd { return textinput.Blink }

func (f sprintEditForm) fields() []int {
	if f.creating {
		return []int{sefTitle, sefGoal, sefSave, sefCancel}
	}
	return []int{sefTitle, sefGoal, sefDue, sefSave, sefCancel}
}

func (f sprintEditForm) Update(msg tea.Msg) (sprintEditForm, tea.Cmd) {
	switch msg := msg.(type) {
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

func (f sprintEditForm) onKey(msg tea.KeyPressMsg) (sprintEditForm, tea.Cmd) {
	switch msg.String() {
	case "esc":
		return f, cancelSprintEdit
	case "tab", "down":
		return f.moveFocus(1)
	case "shift+tab", "up":
		return f.moveFocus(-1)
	case "enter":
		switch f.focus {
		case sefSave:
			return f.submit()
		case sefCancel:
			return f, cancelSprintEdit
		case sefDue:
			return f, openSprintEditDue // the model opens the calendar over the form
		default:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

func (f sprintEditForm) moveFocus(delta int) (sprintEditForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f sprintEditForm) focusOn(id int) (sprintEditForm, tea.Cmd) {
	f.focus = id
	f.title.Blur()
	f.goal.Blur()
	var cmd tea.Cmd
	switch id {
	case sefTitle:
		cmd = f.title.Focus()
	case sefGoal:
		cmd = f.goal.Focus()
	}
	return f, cmd
}

func (f sprintEditForm) typeIntoFocused(msg tea.KeyPressMsg) (sprintEditForm, tea.Cmd) {
	var cmd tea.Cmd
	switch f.focus {
	case sefTitle:
		f.titleErr = ""
		f.title, cmd = f.title.Update(msg)
	case sefGoal:
		f.goal, cmd = f.goal.Update(msg)
	}
	return f, cmd
}

func (f sprintEditForm) updateInputs(msg tea.Msg) (sprintEditForm, tea.Cmd) {
	var tc, gc tea.Cmd
	f.title, tc = f.title.Update(msg)
	f.goal, gc = f.goal.Update(msg)
	return f, tea.Batch(tc, gc)
}

func (f sprintEditForm) submit() (sprintEditForm, tea.Cmd) {
	f.titleErr = ""
	title := strings.TrimSpace(f.title.Value())
	if len([]rune(title)) < sprintTitleMin {
		f.titleErr = "At least 2 characters"
		return f.focusOn(sefTitle)
	}
	var due time.Time
	if f.dueSet {
		due = f.dueAt
	}
	return f, submitSprintEdit(sprintEditValues{
		title:  title,
		goal:   strings.TrimSpace(f.goal.Value()),
		dueAt:  due,
		dueSet: f.dueSet,
	})
}

func (f sprintEditForm) onClick(msg tea.MouseClickMsg) (sprintEditForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return f, nil
	}
	switch id := f.hitZone(msg); id {
	case sefTitle, sefGoal:
		return f.focusOn(id)
	case sefDue:
		f, _ = f.focusOn(sefDue)
		return f, openSprintEditDue
	case sefSave:
		return f.submit()
	case sefCancel:
		return f, cancelSprintEdit
	}
	return f, nil
}

func (f sprintEditForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(sprintEditZone(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f sprintEditForm) View() string {
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered(f.boxTitle(), body, f.deps.Styles.Theme.Primary)
}

func (f sprintEditForm) boxTitle() string {
	if f.creating {
		return "New sprint"
	}
	return "Edit sprint"
}

func (f sprintEditForm) body() string {
	rows := f.fieldRows()
	return lipgloss.JoinVertical(lipgloss.Left, append(rows, "", f.buttons())...)
}

// fieldRows are the modal's input rows in display order, so the view and the scroll maths cannot drift.
func (f sprintEditForm) fieldRows() []string {
	rows := []string{
		f.field(sefTitle, "Title", fixField(f.title.View(), 1), f.titleErr),
		f.field(sefGoal, "Goal", fixField(f.goal.View(), 1), ""),
	}
	if f.creating {
		return rows
	}
	return append(rows, f.field(sefDue, "Due", f.dueContent(), ""))
}

// FocusRow reports the focused control's row and height in the bordered View, so a windowed modal
// scrolls to keep it visible (+2 = top border + the padding row above the body).
func (f sprintEditForm) FocusRow() (int, int, bool) {
	const chromeTop = 2
	ids := f.fields() // same order as fieldRows, minus the two buttons at the end
	row := chromeTop
	for i, view := range f.fieldRows() {
		h := lipgloss.Height(view)
		if ids[i] == f.focus {
			return row, h, true
		}
		row += h
	}
	return row + 1, lipgloss.Height(f.buttons()), true // the buttons sit after the blank row
}

func (f sprintEditForm) dueContent() string {
	t := f.deps.Styles.Theme
	col := t.Muted
	label := "Select…"
	if f.dueSet {
		label, col = formatDateOnly(f.dueAt), t.Text
	}
	if f.focus == sefDue {
		col = t.Accent
	}
	return fixField(lipgloss.NewStyle().Foreground(col).Render(label), 1)
}

func (f sprintEditForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBoxWeighted(label, content, f.fieldBorderColor(id, errMsg), f.focus == id)
	box = zone.Mark(sprintEditZone(id), box)
	if errMsg != "" {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.deps.Styles.Error.Render(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f sprintEditForm) fieldBorderColor(id int, errMsg string) color.Color {
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

func (f sprintEditForm) buttons() string {
	save := "Save"
	if f.creating {
		save = "Create"
	}
	group := lipgloss.JoinHorizontal(lipgloss.Top, f.button(save, sefSave), " ", f.button("Cancel", sefCancel))
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f sprintEditForm) button(label string, id int) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case f.focus == id:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case f.hover == id:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(sprintEditZone(id), components.TitledBoxWeighted("", body, borderCol, f.focus == id))
}

func (f sprintEditForm) HelpKeys() []key.Binding {
	save := "save"
	if f.creating {
		save = "create"
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", save)),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

func sprintEditZone(id int) string {
	switch id {
	case sefTitle:
		return "project.sprint.edit.title"
	case sefGoal:
		return "project.sprint.edit.goal"
	case sefDue:
		return "project.sprint.edit.due"
	case sefSave:
		return "project.sprint.edit.save"
	case sefCancel:
		return "project.sprint.edit.cancel"
	}
	return ""
}

// sprintEditValues is the sprint field state the form emits on save; the model diffs it against the
// selected sprint to send only what changed.
type sprintEditValues struct {
	title  string
	goal   string
	dueAt  time.Time
	dueSet bool
}

type sprintEditSubmittedMsg struct{ v sprintEditValues }

type sprintEditCancelledMsg struct{}

type openSprintEditDueMsg struct{}

func cancelSprintEdit() tea.Msg  { return sprintEditCancelledMsg{} }
func openSprintEditDue() tea.Msg { return openSprintEditDueMsg{} }
func submitSprintEdit(v sprintEditValues) tea.Cmd {
	return func() tea.Msg { return sprintEditSubmittedMsg{v: v} }
}
