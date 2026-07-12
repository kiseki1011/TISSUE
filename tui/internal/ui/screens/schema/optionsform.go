package schema

import (
	"context"
	"errors"
	"net/http"
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
)

// optRow is one editable option. id 0 marks a new option not yet on the server.
type optRow struct {
	id   int
	name string
}

// maxFieldOptions mirrors the backend's per-field option cap.
const maxFieldOptions = 50

const optionRowW = 40

// optionsForm is the options editor for one SELECT_OPTION / CHECKLIST field. It edits the option
// list locally (add / rename / remove) and, on Save, diffs against the field's original options and
// fires the individual option endpoints in one command (delete, then rename, then add) since the
// backend has no whole-list replace for options.
type optionsForm struct {
	deps    deps.Deps
	typeID  int
	fieldID int
	fieldNm string

	orig []domain.FieldOption // server state at open, for the save diff
	rows []optRow

	focus int // 0..len(rows)-1 rows, then Add, Save, Cancel

	inputOpen bool // the add/rename text prompt is open, replacing the list
	input     textinput.Model
	inputIdx  int // -1 = adding, else the row being renamed
	inputErr  string

	spinner    spinner.Model
	status     string
	submitting bool
}

func newOptionsForm(d deps.Deps, typeID, fieldID int, fieldName string, options []domain.FieldOption) optionsForm {
	rows := make([]optRow, len(options))
	for i, o := range options {
		rows[i] = optRow{id: o.ID, name: o.Name}
	}
	in := textinput.New()
	in.Prompt = ""
	in.CharLimit = 64
	in.SetWidth(optionRowW)
	return optionsForm{
		deps: d, typeID: typeID, fieldID: fieldID, fieldNm: fieldName,
		orig: append([]domain.FieldOption(nil), options...), rows: rows,
		input: in, inputIdx: -1, spinner: spinner.New(), focus: 0,
	}
}

func (f optionsForm) Init() tea.Cmd { return nil }

func (f optionsForm) addIdx() int    { return len(f.rows) }
func (f optionsForm) saveIdx() int   { return len(f.rows) + 1 }
func (f optionsForm) cancelIdx() int { return len(f.rows) + 2 }
func (f optionsForm) lastIdx() int   { return len(f.rows) + 2 }
func (f optionsForm) onRow() bool    { return f.focus >= 0 && f.focus < len(f.rows) }

func (f optionsForm) Update(msg tea.Msg) (optionsForm, tea.Cmd) {
	switch msg := msg.(type) {
	case optionsFailedMsg:
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
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	if f.inputOpen {
		var cmd tea.Cmd
		f.input, cmd = f.input.Update(msg)
		return f, cmd
	}
	return f, nil
}

func (f optionsForm) onKey(msg tea.KeyPressMsg) (optionsForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	if f.inputOpen {
		return f.inputKey(msg)
	}
	switch msg.String() {
	case "up", "k":
		if f.focus > 0 {
			f.focus--
		}
	case "down", "j":
		if f.focus < f.lastIdx() {
			f.focus++
		}
	case "tab":
		f.focus = (f.focus + 1) % (f.lastIdx() + 1)
	case "shift+tab":
		f.focus = (f.focus - 1 + f.lastIdx() + 1) % (f.lastIdx() + 1)
	case "x", "delete", "backspace":
		if f.onRow() {
			return f.removeRow(f.focus), nil
		}
	case "enter", " ":
		switch {
		case f.onRow():
			return f.openRename(f.focus), nil
		case f.focus == f.addIdx():
			return f.openAdd(), nil
		case f.focus == f.saveIdx():
			return f.submit()
		case f.focus == f.cancelIdx():
			return f, cancelOptions
		}
	}
	return f, nil
}

// inputKey drives the open add/rename text prompt.
func (f optionsForm) inputKey(msg tea.KeyPressMsg) (optionsForm, tea.Cmd) {
	switch msg.String() {
	case "enter":
		return f.commitInput(), nil
	case "esc":
		f.inputOpen, f.inputErr = false, ""
		return f, nil
	}
	f.inputErr = ""
	var cmd tea.Cmd
	f.input, cmd = f.input.Update(msg)
	return f, cmd
}

func (f optionsForm) openAdd() optionsForm {
	if len(f.rows) >= maxFieldOptions {
		f.status = "This field already has the maximum of 50 options."
		return f
	}
	f.inputOpen, f.inputIdx, f.inputErr, f.status = true, -1, "", ""
	f.input.SetValue("")
	f.input.Focus()
	return f
}

func (f optionsForm) openRename(i int) optionsForm {
	f.inputOpen, f.inputIdx, f.inputErr, f.status = true, i, "", ""
	f.input.SetValue(f.rows[i].name)
	f.input.Focus()
	return f
}

// commitInput folds the prompt's text back into the row list (a new row or a rename), rejecting a
// blank or duplicate name so the backend never 400/409s on it.
func (f optionsForm) commitInput() optionsForm {
	name := strings.TrimSpace(f.input.Value())
	switch {
	case name == "":
		f.inputErr = "Required"
		return f
	case len(name) > 64:
		f.inputErr = "64 characters max"
		return f
	}
	if f.duplicateName(name, f.inputIdx) {
		f.inputErr = "That option already exists."
		return f
	}
	if f.inputIdx < 0 {
		f.rows = append(f.rows, optRow{name: name})
		f.focus = len(f.rows) - 1
	} else {
		f.rows[f.inputIdx].name = name
	}
	f.inputOpen, f.inputErr = false, ""
	return f
}

// duplicateName reports whether name collides case-insensitively with another row (the backend's
// option uniqueness is on the normalized name).
func (f optionsForm) duplicateName(name string, except int) bool {
	norm := strings.ToLower(strings.TrimSpace(name))
	for i, r := range f.rows {
		if i != except && strings.ToLower(strings.TrimSpace(r.name)) == norm {
			return true
		}
	}
	return false
}

func (f optionsForm) removeRow(i int) optionsForm {
	f.rows = append(f.rows[:i], f.rows[i+1:]...)
	if f.focus > len(f.rows) {
		f.focus = len(f.rows)
	}
	f.status = ""
	return f
}

// diff compares the edited rows against the original options and reports the deletions (by id),
// renames (rows keeping their id with a changed name), and additions (new names). A case-only
// rename is dropped from renames since the backend rejects it as a duplicate.
func (f optionsForm) diff() (dels []int, renames []optRow, adds []string) {
	origByID := map[int]string{}
	for _, o := range f.orig {
		origByID[o.ID] = o.Name
	}
	kept := map[int]bool{}
	for _, r := range f.rows {
		if r.id == 0 {
			adds = append(adds, r.name)
			continue
		}
		kept[r.id] = true
		if old, ok := origByID[r.id]; ok && !sameNormalized(old, r.name) {
			renames = append(renames, r)
		}
	}
	for _, o := range f.orig {
		if !kept[o.ID] {
			dels = append(dels, o.ID)
		}
	}
	return dels, renames, adds
}

// submit commits the option diff in one command (delete, then rename, then add). Nothing changed
// closes the modal without a network call. It collects the set of names to steer temp rename names
// clear of, so a swap or cycle of option names commits without an intermediate collision.
func (f optionsForm) submit() (optionsForm, tea.Cmd) {
	f.status = ""
	dels, renames, adds := f.diff()
	if len(dels) == 0 && len(renames) == 0 && len(adds) == 0 {
		return f, cancelOptions
	}
	avoid := map[string]bool{}
	for _, o := range f.orig {
		avoid[strings.ToLower(strings.TrimSpace(o.Name))] = true
	}
	for _, r := range f.rows {
		avoid[strings.ToLower(strings.TrimSpace(r.name))] = true
	}
	f.submitting = true
	return f, tea.Batch(commitOptions(f.deps, f.typeID, f.fieldID, dels, renames, adds, avoid), f.spinner.Tick)
}

// sameNormalized reports whether two names are equal after trim+lowercase, so a case-only rename
// (which the backend rejects) is treated as no change.
func sameNormalized(a, b string) bool {
	return strings.ToLower(strings.TrimSpace(a)) == strings.ToLower(strings.TrimSpace(b))
}

// ---- view ----

func (f optionsForm) View() string {
	t := f.deps.Styles.Theme
	if f.inputOpen {
		return f.inputView()
	}
	rows := []string{f.deps.Styles.Muted.Render("Field: ") + lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(f.fieldNm), ""}
	if len(f.rows) == 0 {
		rows = append(rows, f.deps.Styles.Muted.Render("No options yet."))
	}
	for i, r := range f.rows {
		rows = append(rows, f.rowLine(i, r))
	}
	rows = append(rows, f.addLine())
	switch {
	case f.submitting:
		rows = append(rows, "", lipgloss.NewStyle().Foreground(t.Warning).Render(f.spinner.View()+" Saving…"))
	case f.status != "":
		rows = append(rows, "", f.deps.Styles.Error.Width(optionRowW).Render(f.status))
	}
	rows = append(rows, "", f.buttons(), "", f.hint())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("Options", body, t.Primary)
}

func (f optionsForm) inputView() string {
	t := f.deps.Styles.Theme
	title := "Add option"
	if f.inputIdx >= 0 {
		title = "Rename option"
	}
	border := t.Accent
	if f.inputErr != "" {
		border = t.Error
	}
	rows := []string{
		components.TitledBox("Name", fixOption(f.input.View()), border),
		lipgloss.NewStyle().Padding(0, 1).Render(f.errText(f.inputErr)),
		"",
		hintBar(f.deps.Styles, "enter", "save", "esc", "back"),
	}
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered(title, body, t.Primary)
}

func (f optionsForm) rowLine(i int, r optRow) string {
	t := f.deps.Styles.Theme
	marker, style := "  ", lipgloss.NewStyle().Foreground(t.Text)
	if f.focus == i {
		marker, style = lipgloss.NewStyle().Foreground(t.Accent).Render("▸ "), lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	}
	tail := ""
	if r.id == 0 {
		tail = f.deps.Styles.Muted.Render("new")
	}
	head := marker + style.Render(orDash(r.name))
	return zone.Mark(optionRowZone(i), alignRow(head, tail, optionRowW, lipgloss.NewStyle()))
}

func (f optionsForm) addLine() string {
	t := f.deps.Styles.Theme
	full := len(f.rows) >= maxFieldOptions
	style, marker := lipgloss.NewStyle().Foreground(t.Muted), "  "
	if f.focus == f.addIdx() {
		marker = lipgloss.NewStyle().Foreground(t.Accent).Render("▸ ")
		if !full {
			style = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		}
	}
	label := "+ Add option"
	if full {
		label = "＋ (option limit reached)"
	}
	return zone.Mark("options.add", marker+style.Render(label))
}

func (f optionsForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", "options.save", f.focus == f.saveIdx()),
		" ",
		f.button("Cancel", "options.cancel", f.focus == f.cancelIdx()),
	)
	return lipgloss.PlaceHorizontal(optionRowW+2, lipgloss.Right, group)
}

func (f optionsForm) button(label, id string, focused bool) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	if focused {
		borderCol, textCol, bold = t.Accent, t.Accent, true
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(id, components.TitledBox("", body, borderCol))
}

func (f optionsForm) hint() string {
	return hintBar(f.deps.Styles, "enter", "rename", "x", "remove", "esc", "cancel")
}

func (f optionsForm) errText(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f optionsForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	if f.inputOpen {
		return []key.Binding{
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move"))}
	if f.onRow() {
		binds = append(binds,
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "rename")),
			key.NewBinding(key.WithKeys("x"), key.WithHelp("x", "remove")))
	} else if f.focus == f.addIdx() {
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "add")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

// ---- click routing ----

func optionRowZone(i int) string { return "options.row." + itoa(i) }

func (f optionsForm) onClick(msg tea.MouseClickMsg) (optionsForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting || f.inputOpen {
		return f, nil
	}
	for i := range f.rows {
		if zone.Get(optionRowZone(i)).InBounds(msg) {
			f.focus = i
			return f.openRename(i), nil
		}
	}
	switch {
	case zone.Get("options.add").InBounds(msg):
		f.focus = f.addIdx()
		return f.openAdd(), nil
	case zone.Get("options.save").InBounds(msg):
		f.focus = f.saveIdx()
		return f.submit()
	case zone.Get("options.cancel").InBounds(msg):
		return f, cancelOptions
	}
	return f, nil
}

func fixOption(s string) string {
	return lipgloss.NewStyle().Width(optionRowW).MaxWidth(optionRowW).Height(1).MaxHeight(1).Render(s)
}

// ---- messages ----

type optionsSavedMsg struct{ typeID int }

type optionsFailedMsg struct{ message string }

type optionsCancelledMsg struct{}

func cancelOptions() tea.Msg { return optionsCancelledMsg{} }

func commitOptions(d deps.Deps, typeID, fieldID int, dels []int, renames []optRow, adds []string, avoid map[string]bool) tea.Cmd {
	return func() tea.Msg {
		ctx := context.Background()
		for _, id := range dels {
			if err := d.Catalog.DeleteFieldOption(ctx, fieldID, id); err != nil {
				return optionsFailedMsg{message: optionsErrorMessage(err)}
			}
		}
		// Rename in two phases so a swap or cycle of names never hits an intermediate duplicate: move
		// every renamed option to a throwaway name clear of all current and final names, then to its
		// target. The backend enforces uniqueness at each PATCH, so a one-pass rename could 409.
		if len(renames) > 0 {
			temp := tempRenameNames(len(renames), avoid)
			for i, r := range renames {
				if err := d.Catalog.RenameFieldOption(ctx, fieldID, r.id, temp[i]); err != nil {
					return optionsFailedMsg{message: optionsErrorMessage(err)}
				}
			}
			for _, r := range renames {
				if err := d.Catalog.RenameFieldOption(ctx, fieldID, r.id, r.name); err != nil {
					return optionsFailedMsg{message: optionsErrorMessage(err)}
				}
			}
		}
		for _, name := range adds {
			if err := d.Catalog.AddFieldOption(ctx, fieldID, name); err != nil {
				return optionsFailedMsg{message: optionsErrorMessage(err)}
			}
		}
		return optionsSavedMsg{typeID: typeID}
	}
}

// tempRenameNames returns n distinct throwaway option names, each clear of the avoid set (the
// current and final option names, normalized lowercase), for the first phase of a two-phase rename.
func tempRenameNames(n int, avoid map[string]bool) []string {
	out := make([]string, 0, n)
	k := 0
	for len(out) < n {
		t := "tmp" + itoa(k)
		k++
		if !avoid[strings.ToLower(t)] {
			out = append(out, t)
		}
	}
	return out
}

func optionsErrorMessage(err error) string {
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "An option name is duplicated or in use. Some changes may have applied."
		case http.StatusBadRequest:
			return "Invalid option. Some changes may have applied."
		case http.StatusForbidden:
			return "You do not have permission to edit options."
		}
	}
	return "Could not save options. Some changes may have applied."
}
