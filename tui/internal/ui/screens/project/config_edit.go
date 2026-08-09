package project

import (
	"image/color"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/textarea"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// the config edit form's focusable controls, in tab order.
const (
	cefTitle = iota
	cefDescription
	cefVisibility
	cefSave
	cefCancel
)

// project field limits, mirroring the create form (home) and the backend policy.
const (
	projectTitleMin = 2
	projectTitleMax = 60
	configDescH     = 5 // visible rows of the description textarea
)

// configEditForm is the "Edit project" modal for a project's title, description, and visibility.
// Archiving is a separate action.
type configEditForm struct {
	deps deps.Deps

	title       textinput.Model
	description textarea.Model
	visibility  string // "PUBLIC" | "PRIVATE"

	focus    int
	hover    int
	titleErr string
}

func newConfigEditForm(d deps.Deps, p domain.Project) configEditForm {
	title := textinput.New()
	title.Prompt = ""
	title.SetWidth(editFieldW)
	title.CharLimit = projectTitleMax
	title.SetValue(p.Title)

	description := textarea.New()
	description.Prompt = ""
	description.ShowLineNumbers = false
	description.CharLimit = 0 // no client cap; the server enforces its own limit (and rejects an overflow)
	description.Placeholder = "None"
	description.SetWidth(editFieldW)
	description.SetHeight(configDescH)
	description.SetValue(p.Description)

	vis := p.Visibility
	if vis != "PRIVATE" {
		vis = "PUBLIC" // default/normalise anything unexpected to the safer public-list default
	}
	f := configEditForm{
		deps: d, title: title, description: description, visibility: vis,
		focus: cefTitle, hover: -1,
	}
	f.title.Focus()
	return f
}

func (f configEditForm) Init() tea.Cmd { return textinput.Blink }

func (f configEditForm) fields() []int {
	return []int{cefTitle, cefDescription, cefVisibility, cefSave, cefCancel}
}

func (f configEditForm) Update(msg tea.Msg) (configEditForm, tea.Cmd) {
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

func (f configEditForm) onKey(msg tea.KeyPressMsg) (configEditForm, tea.Cmd) {
	switch msg.String() {
	case "esc":
		return f, cancelConfigEdit
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if f.focus != cefDescription { // in the textarea, up/down move the caret between lines
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != cefDescription {
			return f.moveFocus(1)
		}
	case "left", "right", "space":
		if f.focus == cefVisibility {
			return f.toggleVisibility(), nil
		}
	case "enter":
		switch f.focus {
		case cefSave:
			return f.submit()
		case cefCancel:
			return f, cancelConfigEdit
		case cefVisibility:
			return f.toggleVisibility(), nil
		case cefTitle:
			return f.moveFocus(1) // enter in the single-line title advances
		case cefDescription:
			return f.typeIntoFocused(msg) // a newline in the description
		}
	}
	return f.typeIntoFocused(msg)
}

func (f configEditForm) toggleVisibility() configEditForm {
	if f.visibility == "PUBLIC" {
		f.visibility = "PRIVATE"
	} else {
		f.visibility = "PUBLIC"
	}
	return f
}

func (f configEditForm) moveFocus(delta int) (configEditForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f configEditForm) focusOn(id int) (configEditForm, tea.Cmd) {
	f.focus = id
	f.title.Blur()
	f.description.Blur()
	var cmd tea.Cmd
	switch id {
	case cefTitle:
		cmd = f.title.Focus()
	case cefDescription:
		cmd = f.description.Focus()
	}
	return f, cmd
}

func (f configEditForm) typeIntoFocused(msg tea.KeyPressMsg) (configEditForm, tea.Cmd) {
	var cmd tea.Cmd
	switch f.focus {
	case cefTitle:
		f.titleErr = ""
		f.title, cmd = f.title.Update(msg)
	case cefDescription:
		f.description, cmd = f.description.Update(msg)
	}
	return f, cmd
}

func (f configEditForm) updateInputs(msg tea.Msg) (configEditForm, tea.Cmd) {
	var tc, dc tea.Cmd
	f.title, tc = f.title.Update(msg)
	f.description, dc = f.description.Update(msg)
	return f, tea.Batch(tc, dc)
}

func (f configEditForm) submit() (configEditForm, tea.Cmd) {
	f.titleErr = ""
	title := strings.TrimSpace(f.title.Value())
	if n := len([]rune(title)); n < projectTitleMin || n > projectTitleMax {
		f.titleErr = "2–60 characters"
		return f.focusOn(cefTitle)
	}
	return f, submitConfigEdit(configEditValues{
		title:       title,
		description: strings.TrimSpace(f.description.Value()),
		visibility:  f.visibility,
	})
}

func (f configEditForm) onClick(msg tea.MouseClickMsg) (configEditForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return f, nil
	}
	switch id := f.hitZone(msg); id {
	case cefTitle, cefDescription:
		return f.focusOn(id)
	case cefVisibility:
		f, _ = f.focusOn(cefVisibility)
		return f.toggleVisibility(), nil
	case cefSave:
		return f.submit()
	case cefCancel:
		return f, cancelConfigEdit
	}
	return f, nil
}

func (f configEditForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(configEditZone(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f configEditForm) View() string {
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("Edit project", body, f.deps.Styles.Theme.Primary)
}

func (f configEditForm) body() string {
	rows := []string{
		f.field(cefTitle, "Title", fixField(f.title.View(), 1), f.titleErr),
		f.field(cefDescription, "Description", fixField(f.description.View(), configDescH), ""),
		f.field(cefVisibility, "Visibility", f.visibilityContent(), ""),
		"", f.buttons(),
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// FocusRow reports the focused control's row and height in the bordered View, so a windowed modal
// scrolls to keep it visible (+2 = top border + the padding row above the body).
func (f configEditForm) FocusRow() (int, int, bool) {
	const chromeTop = 2
	rows := []struct {
		id   int
		view string
	}{
		{cefTitle, f.field(cefTitle, "Title", fixField(f.title.View(), 1), f.titleErr)},
		{cefDescription, f.field(cefDescription, "Description", fixField(f.description.View(), configDescH), "")},
		{cefVisibility, f.field(cefVisibility, "Visibility", f.visibilityContent(), "")},
	}
	row := chromeTop
	for _, r := range rows {
		h := lipgloss.Height(r.view)
		if r.id == f.focus {
			return row, h, true
		}
		row += h
	}
	return row + 1, lipgloss.Height(f.buttons()), true // Save/Cancel sit after the blank row
}

// visibilityContent renders the two options as a segmented toggle, the active one accented.
func (f configEditForm) visibilityContent() string {
	t := f.deps.Styles.Theme
	opt := func(label, value string) string {
		st := lipgloss.NewStyle().Foreground(t.Muted)
		if f.visibility == value {
			c := t.Text
			if f.focus == cefVisibility {
				c = t.Accent
			}
			st = lipgloss.NewStyle().Foreground(c).Bold(true)
		}
		return st.Render(label)
	}
	seg := opt("Public", "PUBLIC") + lipgloss.NewStyle().Foreground(t.Muted).Render("   ") + opt("Private", "PRIVATE")
	return fixField(seg, 1)
}

func (f configEditForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBoxWeighted(label, content, f.fieldBorderColor(id, errMsg), f.focus == id)
	box = zone.Mark(configEditZone(id), box)
	if errMsg != "" {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.deps.Styles.Error.Render(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f configEditForm) fieldBorderColor(id int, errMsg string) color.Color {
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

func (f configEditForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top, f.button("Save", cefSave), " ", f.button("Cancel", cefCancel))
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f configEditForm) button(label string, id int) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case f.focus == id:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case f.hover == id:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(configEditZone(id), components.TitledBoxWeighted("", body, borderCol, f.focus == id))
}

func (f configEditForm) HelpKeys() []key.Binding {
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

func configEditZone(id int) string {
	switch id {
	case cefTitle:
		return "project.config.edit.title"
	case cefDescription:
		return "project.config.edit.description"
	case cefVisibility:
		return "project.config.edit.visibility"
	case cefSave:
		return "project.config.edit.save"
	case cefCancel:
		return "project.config.edit.cancel"
	}
	return ""
}

// configEditValues is the project field state the form emits on save; the model diffs it against the
// loaded project to send only what changed.
type configEditValues struct {
	title       string
	description string
	visibility  string
}

type configEditSubmittedMsg struct{ v configEditValues }

type configEditCancelledMsg struct{}

func cancelConfigEdit() tea.Msg { return configEditCancelledMsg{} }
func submitConfigEdit(v configEditValues) tea.Cmd {
	return func() tea.Msg { return configEditSubmittedMsg{v: v} }
}
