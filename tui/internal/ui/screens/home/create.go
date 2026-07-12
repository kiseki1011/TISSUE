package home

import (
	"context"
	"errors"
	"image/color"
	"net/http"
	"regexp"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	"charm.land/bubbles/v2/textarea"
	"charm.land/bubbles/v2/textinput"
	"charm.land/bubbles/v2/viewport"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

const (
	createKey = iota
	createTitle
	createDesc
	createSubmit
	createCancel
	createFieldCount
)

const (
	createFieldW  = 40 // field input body width
	descFieldRows = 4  // visible rows in description textarea
)

var projectKeyPattern = regexp.MustCompile(`^[A-Z]+[0-9]*$`)

// createForm is the "New Project" modal. Its fields live in a viewport so the modal
// scrolls when the terminal is too short to show them all.
type createForm struct {
	deps deps.Deps

	key     textinput.Model
	title   textinput.Model
	desc    textarea.Model
	spinner spinner.Model
	focus   int
	hover   int // field or button under the cursor for hover highlight, -1 when none

	keyErr     string
	titleErr   string
	descErr    string
	status     string
	submitting bool

	vp    viewport.Model
	bodyH int // rows available for the scrollable body
}

func newCreateForm(d deps.Deps, bodyH int) createForm {
	line := func(placeholder string) textinput.Model {
		in := textinput.New()
		in.Prompt = ""
		in.Placeholder = placeholder
		in.SetWidth(createFieldW)
		return in
	}
	desc := textarea.New()
	desc.Placeholder = "optional"
	desc.Prompt = ""
	desc.ShowLineNumbers = false
	desc.CharLimit = 255
	desc.SetWidth(createFieldW)
	desc.SetHeight(descFieldRows)

	f := createForm{
		deps:    d,
		key:     line("MYPROJ"),
		title:   line("My project"),
		desc:    desc,
		spinner: spinner.New(),
		focus:   createKey,
		hover:   -1,
		vp:      viewport.New(),
		bodyH:   bodyH,
	}
	f.key.Focus()
	return f.sync()
}

func (f createForm) Init() tea.Cmd { return textinput.Blink }

func (f createForm) Update(msg tea.Msg) (createForm, tea.Cmd) {
	before := f.focus
	f, cmd := f.route(msg)
	f = f.sync()
	// Follow focus into view when it moves, but leave wheel scrolling alone.
	if f.focus != before {
		f.vp.EnsureVisible(f.focusLine(), 0, 0)
	}
	return f, cmd
}

func (f createForm) route(msg tea.Msg) (createForm, tea.Cmd) {
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
	case tea.MouseWheelMsg:
		var cmd tea.Cmd
		f.vp, cmd = f.vp.Update(msg)
		return f, cmd
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.MouseMotionMsg:
		return f.onHover(msg)
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f.updateInputs(msg)
}

// hitZone returns the field or button under the cursor, or -1 for none. Elements
// scrolled out of the viewport carry no zone this frame, so they never match.
func (f createForm) hitZone(msg tea.MouseMsg) int {
	switch {
	case zone.Get(fieldZoneID(createKey)).InBounds(msg):
		return createKey
	case zone.Get(fieldZoneID(createTitle)).InBounds(msg):
		return createTitle
	case zone.Get(fieldZoneID(createDesc)).InBounds(msg):
		return createDesc
	case zone.Get("create.submit").InBounds(msg):
		return createSubmit
	case zone.Get("create.cancel").InBounds(msg):
		return createCancel
	}
	return -1
}

func (f createForm) onClick(msg tea.MouseClickMsg) (createForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	switch el := f.hitZone(msg); el {
	case createKey, createTitle, createDesc:
		return f.focusOn(el)
	case createSubmit:
		return f.submit()
	case createCancel:
		return f, cancelCreate
	}
	return f, nil
}

func (f createForm) onHover(msg tea.MouseMotionMsg) (createForm, tea.Cmd) {
	f.hover = f.hitZone(msg)
	return f, nil
}

// resize updates the height available to the body when the terminal is resized
// while the modal is open, keeping the focused field in view.
func (f createForm) resize(bodyH int) createForm {
	f.bodyH = bodyH
	f = f.sync()
	f.vp.EnsureVisible(f.focusLine(), 0, 0)
	return f
}

// sync rebuilds the scrollable body and sizes the viewport to it (capped at bodyH).
// SetContent keeps the current scroll offset, so it is safe to call on every update.
func (f createForm) sync() createForm {
	content := f.body()
	f.vp.SetContent(content)
	f.vp.SetWidth(lipgloss.Width(content))
	h := lipgloss.Height(content)
	if h > f.bodyH {
		h = f.bodyH
	}
	if h < 1 {
		h = 1
	}
	f.vp.SetHeight(h)
	// SetContent clamps against the old height and SetHeight not at all, so re-clamp
	// here or a stale offset strands the body below the fold when the viewport grows.
	f.vp.SetYOffset(f.vp.YOffset())
	return f
}

// onKey routes navigation and submit. In the description textarea, up/down and enter
// belong to the textarea, so they fall through to typeIntoFocused.
func (f createForm) onKey(msg tea.KeyPressMsg) (createForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	switch msg.String() {
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if f.focus != createDesc {
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != createDesc {
			return f.moveFocus(1)
		}
	case "enter":
		switch f.focus {
		case createSubmit:
			return f.submit()
		case createCancel:
			return f, cancelCreate
		case createKey, createTitle:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

func (f createForm) moveFocus(delta int) (createForm, tea.Cmd) {
	return f.focusOn((f.focus + delta + createFieldCount) % createFieldCount)
}

func (f createForm) focusOn(target int) (createForm, tea.Cmd) {
	f.focus = target
	f.key.Blur()
	f.title.Blur()
	f.desc.Blur()

	var cmd tea.Cmd
	switch target {
	case createKey:
		cmd = f.key.Focus()
	case createTitle:
		cmd = f.title.Focus()
	case createDesc:
		cmd = f.desc.Focus()
	}
	return f, cmd
}

func (f createForm) typeIntoFocused(msg tea.KeyPressMsg) (createForm, tea.Cmd) {
	f.status = ""
	var cmd tea.Cmd
	switch f.focus {
	case createKey:
		f.keyErr = ""
		f.key, cmd = f.key.Update(msg)
		f.key.SetValue(strings.ToUpper(f.key.Value()))
	case createTitle:
		f.titleErr = ""
		f.title, cmd = f.title.Update(msg)
	case createDesc:
		f.descErr = ""
		f.desc, cmd = f.desc.Update(msg)
	}
	return f, cmd
}

func (f createForm) updateInputs(msg tea.Msg) (createForm, tea.Cmd) {
	var kc, tc, dc tea.Cmd
	f.key, kc = f.key.Update(msg)
	f.title, tc = f.title.Update(msg)
	f.desc, dc = f.desc.Update(msg)
	return f, tea.Batch(kc, tc, dc)
}

func (f createForm) submit() (createForm, tea.Cmd) {
	f.keyErr, f.titleErr, f.descErr, f.status = "", "", "", ""
	key := strings.ToUpper(strings.TrimSpace(f.key.Value()))
	title := strings.TrimSpace(f.title.Value())
	desc := strings.TrimSpace(f.desc.Value())

	valid := true
	switch {
	case key == "":
		f.keyErr = "Required field"
		valid = false
	case len(key) < 2 || len(key) > 10:
		f.keyErr = "2 to 10 characters"
		valid = false
	case !projectKeyPattern.MatchString(key):
		f.keyErr = "Letters then digits (e.g. TIS)"
		valid = false
	}
	switch {
	case title == "":
		f.titleErr = "Required field"
		valid = false
	case len(title) < 2 || len(title) > 60:
		f.titleErr = "2 to 60 characters"
		valid = false
	}
	if len(desc) > 255 {
		f.descErr = "255 characters max"
		valid = false
	}
	if !valid {
		// Focus the first invalid field so Update scrolls its error into view
		switch {
		case f.keyErr != "":
			return f.focusOn(createKey)
		case f.titleErr != "":
			return f.focusOn(createTitle)
		case f.descErr != "":
			return f.focusOn(createDesc)
		}
		return f, nil
	}

	f.submitting = true
	f.status = ""
	return f, tea.Batch(createProject(f.deps, key, title, desc), f.spinner.Tick)
}

func (f createForm) View() string {
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.withScrollbar(f.vp.View()))
	return components.TitledBoxCentered("New Project", body, f.deps.Styles.Theme.Primary)
}

// body assembles the scrollable form content
func (f createForm) body() string {
	rows := []string{
		f.field(createKey, "Key", fixBody(f.key.View(), 1), f.keyErr),
		f.field(createTitle, "Title", fixBody(f.title.View(), 1), f.titleErr),
		f.field(createDesc, "Description", fixBody(f.desc.View(), descFieldRows), f.descErr),
	}
	if f.submitting {
		creating := f.spinner.View() + " Creating..."
		rows = append(rows, lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Warning).Padding(0, 1).Render(creating))
	} else if f.status != "" {
		rows = append(rows, f.deps.Styles.Error.Padding(0, 1).Render(f.status))
	}
	rows = append(rows, "", f.buttons())
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// focusLine is the body line where the focused element starts, so the viewport can scroll it into view
func (f createForm) focusLine() int {
	keyH := lipgloss.Height(f.field(createKey, "Key", fixBody(f.key.View(), 1), f.keyErr))
	titleH := lipgloss.Height(f.field(createTitle, "Title", fixBody(f.title.View(), 1), f.titleErr))
	descH := lipgloss.Height(f.field(createDesc, "Description", fixBody(f.desc.View(), descFieldRows), f.descErr))
	switch f.focus {
	case createKey:
		return 0
	case createTitle:
		return keyH
	case createDesc:
		return keyH + titleH
	default: // the buttons row
		line := keyH + titleH + descH
		if f.submitting || f.status != "" {
			line++
		}
		return line + 1 // the blank row before the buttons
	}
}

// withScrollbar reserves a one-column right gutter on every line and draws the scrollbar in it when
// the content overflows. The gutter is always present so the modal width stays constant whether
// or not the scrollbar is shown.
func (f createForm) withScrollbar(view string) string {
	lines := strings.Split(view, "\n")
	h := len(lines)
	total := f.vp.TotalLineCount()
	overflow := total > h

	thumb, pos := 0, 0
	if overflow {
		if thumb = h * h / total; thumb < 1 {
			thumb = 1
		}
		if maxOff := total - h; maxOff > 0 {
			pos = f.vp.YOffset() * (h - thumb) / maxOff
		}
	}

	t := f.deps.Styles.Theme
	thumbCell := lipgloss.NewStyle().Foreground(t.Primary).Render("█")
	trackCell := lipgloss.NewStyle().Foreground(t.Border).Render("░")
	for i := range lines {
		cell := " "
		if overflow {
			if cell = trackCell; i >= pos && i < pos+thumb {
				cell = thumbCell
			}
		}
		lines[i] += " " + cell
	}
	return strings.Join(lines, "\n")
}

func fixBody(s string, h int) string {
	return lipgloss.NewStyle().Width(createFieldW).MaxWidth(createFieldW).Height(h).MaxHeight(h).Render(s)
}

func (f createForm) field(which int, label, content, errMsg string) string {
	box := components.TitledBox(label, content, f.fieldBorderColor(which, errMsg))
	if id := fieldZoneID(which); id != "" {
		box = zone.Mark(id, box)
	}
	errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.errorLine(errMsg))
	return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
}

// fieldZoneID is the click zone for an editable field, empty for the button rows.
func fieldZoneID(which int) string {
	switch which {
	case createKey:
		return "create.key"
	case createTitle:
		return "create.title"
	case createDesc:
		return "create.desc"
	}
	return ""
}

// buttons draws the Create and Cancel actions, each sized to its label, grouped at
// the bottom right under the fields.
func (f createForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Create", "create.submit", f.focus == createSubmit, f.hover == createSubmit),
		" ",
		f.button("Cancel", "create.cancel", f.focus == createCancel, f.hover == createCancel),
	)
	return lipgloss.PlaceHorizontal(createFieldW+4, lipgloss.Right, group)
}

func (f createForm) button(label, id string, focused, hovered bool) string {
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

func (f createForm) fieldBorderColor(which int, errMsg string) color.Color {
	t := f.deps.Styles.Theme
	switch {
	case errMsg != "":
		return t.Error
	case f.focus == which:
		return t.Accent
	case f.hover == which:
		return t.Secondary
	default:
		return t.Primary
	}
}

func (f createForm) errorLine(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f createForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "confirm")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

type createSubmittedMsg struct{ key string }

type createFailedMsg struct{ message string }

type createCancelledMsg struct{}

func cancelCreate() tea.Msg { return createCancelledMsg{} }

func createProject(d deps.Deps, key, title, desc string) tea.Cmd {
	return func() tea.Msg {
		newKey, err := d.Projects.CreateProject(context.Background(), key, title, desc)
		if err != nil {
			return createFailedMsg{message: createErrorMessage(err)}
		}
		return createSubmittedMsg{key: newKey}
	}
}

func createErrorMessage(err error) string {
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "That project key is already taken."
		case http.StatusBadRequest:
			return "Invalid project details."
		}
	}
	return "Could not create the project. Try again."
}
