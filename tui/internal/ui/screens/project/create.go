package project

import (
	"context"
	"image/color"
	"strconv"
	"strings"
	"time"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/textarea"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// focusable controls, in tab order.
const (
	nfType = iota
	nfParent
	nfTitle
	nfSummary
	nfContent
	nfPriority
	nfDue
	nfCreate
	nfCancel
)

// nfCustomBase is the focus-id base for dynamic custom-field rows, kept above the static ids so the
// two ranges never collide.
const nfCustomBase = 100

func isCustomFocus(id int) bool { return id >= nfCustomBase }
func customIndex(id int) int    { return id - nfCustomBase }

// focusIsArea reports whether focus is a multi-line TEXT field, where enter/up/down edit instead of navigating.
func (f createForm) focusIsArea() bool {
	if !isCustomFocus(f.focus) {
		return false
	}
	i := customIndex(f.focus)
	return i >= 0 && i < len(f.customFields) && f.customFields[i].isArea()
}

type createForm struct {
	deps   deps.Deps
	types  []domain.IssueTypeSummary
	typeIx int

	customFields  []customFieldInput // the selected type's custom-field inputs, loaded lazily
	customLoading bool               // a type's fields are being fetched, so submit waits for them

	title      textinput.Model
	summary    textinput.Model
	content    textarea.Model
	dueAt      time.Time // zero = none, set from the calendar picker
	dueSet     bool
	priorityIx int

	parentKey   string // chosen parent issue key, "" = none
	parentLabel string // picker label, for display
	// lockedParent fixes the Parent field for a child-create: the type cycle is pre-restricted to the child
	// level, the parent survives type cycles, and the field is neither focusable nor a picker.
	lockedParent bool

	focus     int
	hover     int
	titleErr  string
	parentErr string
}

// setDue records a calendar pick (set=false clears the due date).
func (f createForm) setDue(v time.Time, set bool) createForm {
	f.dueAt, f.dueSet = v, set
	return f
}

func (f createForm) selectedType() (domain.IssueTypeSummary, bool) {
	if len(f.types) == 0 {
		return domain.IssueTypeSummary{}, false
	}
	return f.types[f.typeIx], true
}

// selectedHierarchy is the cycled type's hierarchy level (EPIC/STANDARD/SUBTASK/MICROTASK), or "".
func (f createForm) selectedHierarchy() string {
	if t, ok := f.selectedType(); ok {
		return t.Hierarchy
	}
	return ""
}

// withParent records the picked parent (an empty key clears it) and drops the parent error.
func (f createForm) withParent(key, label string) createForm {
	f.parentKey, f.parentErr = key, ""
	if key == "" {
		f.parentLabel = ""
	} else {
		f.parentLabel = label
	}
	return f
}

func newCreateForm(d deps.Deps, types []domain.IssueTypeSummary) createForm {
	title := textinput.New()
	title.Prompt = ""
	title.SetWidth(editFieldW)
	title.CharLimit = 255

	summary := textinput.New()
	summary.Prompt = ""
	summary.SetWidth(editFieldW)
	summary.CharLimit = 2000 // SUMMARY_MAX
	summary.Placeholder = "Optional"

	content := textarea.New()
	content.Prompt = ""
	content.ShowLineNumbers = false
	content.CharLimit = 0 // issue bodies can be long markdown
	content.SetWidth(editFieldW)
	content.SetHeight(editContentH)

	f := createForm{
		deps: d, types: types,
		title: title, summary: summary, content: content,
		priorityIx: max(0, indexOf(filterPriorities, "P2")), // default P2, the backend's middle priority
		focus:      nfTitle, hover: -1,                      // start in Title for immediate typing (Type has a default)
	}
	f.title.Focus()
	return f
}

// newChildCreateForm presets the form to add a child: types limited to one level below, parent locked.
func newChildCreateForm(d deps.Deps, types []domain.IssueTypeSummary, parentKey, parentLabel string) createForm {
	f := newCreateForm(d, types)
	f.parentKey, f.parentLabel, f.lockedParent = parentKey, parentLabel, true
	return f
}

func (f createForm) Init() tea.Cmd { return textinput.Blink }

func (f createForm) fields() []int {
	ids := []int{nfType}
	if !f.lockedParent { // a locked parent is not a tab stop or a click target
		ids = append(ids, nfParent)
	}
	ids = append(ids, nfTitle, nfSummary, nfContent, nfPriority, nfDue)
	for i := range f.customFields {
		ids = append(ids, nfCustomBase+i)
	}
	return append(ids, nfCreate, nfCancel)
}

// withCustomFields swaps in a type's resolved inputs, pulling focus off a now-removed custom row.
func (f createForm) withCustomFields(inputs []customFieldInput) createForm {
	f.customFields = inputs
	f.customLoading = false
	if isCustomFocus(f.focus) && customIndex(f.focus) >= len(inputs) {
		f.focus = nfType
	}
	return f
}

// startCustomLoad marks a fetch in flight so submit waits for the new type's required-field checks.
func (f createForm) startCustomLoad() createForm {
	f.customFields = nil
	f.customLoading = true
	if isCustomFocus(f.focus) {
		f.focus = nfType
	}
	return f
}

func (f createForm) Update(msg tea.Msg) (createForm, tea.Cmd) {
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

func (f createForm) onKey(msg tea.KeyPressMsg) (createForm, tea.Cmd) {
	// navigation applies regardless of the focused field's kind
	switch msg.String() {
	case "esc":
		return f, cancelCreateIssue
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if f.focus != nfContent && !f.focusIsArea() {
			return f.moveFocus(-1)
		}
	case "down":
		if f.focus != nfContent && !f.focusIsArea() {
			return f.moveFocus(1)
		}
	}
	// a focused custom field owns its own keys (DATE opens the calendar, a TEXT area keeps enter for editing).
	if isCustomFocus(f.focus) {
		i := customIndex(f.focus)
		if i < 0 || i >= len(f.customFields) {
			return f, nil
		}
		c := f.customFields[i]
		if c.isDate() {
			if msg.String() == "enter" {
				return f, openCustomDate(i)
			}
			return f, nil // a date field has no typing
		}
		if msg.String() == "enter" && !c.isArea() {
			return f.moveFocus(1)
		}
		f.customFields[i], _ = f.customFields[i].handleKey(msg)
		return f, nil
	}
	switch msg.String() {
	case "left":
		switch f.focus {
		case nfType:
			return f.cycleTypeReload(-1)
		case nfPriority:
			f.priorityIx = (f.priorityIx - 1 + len(filterPriorities)) % len(filterPriorities)
			return f, nil
		}
	case "right", "space":
		switch f.focus {
		case nfType:
			return f.cycleTypeReload(1)
		case nfPriority:
			f.priorityIx = (f.priorityIx + 1) % len(filterPriorities)
			return f, nil
		}
	case "enter":
		switch f.focus {
		case nfCreate:
			return f.submit()
		case nfCancel:
			return f, cancelCreateIssue
		case nfParent:
			return f, openParentPickerCmd // the model opens the picker (or explains why none applies)
		case nfDue:
			return f, openDueCreate
		case nfContent:
			return f.typeIntoFocused(msg) // a newline in the body
		default:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

func (f createForm) cycleTypeReload(delta int) (createForm, tea.Cmd) {
	f = f.cycleType(delta)
	f = f.startCustomLoad()
	return f, f.requestFieldsCmd()
}

func (f createForm) requestFieldsCmd() tea.Cmd {
	if t, ok := f.selectedType(); ok {
		return requestTypeFields(int64(t.ID))
	}
	return nil
}

// cycleType clears any picked parent, since a different type changes which hierarchy level a parent must
// come from. A locked parent keeps its own, because the cycle is pre-restricted to one level.
func (f createForm) cycleType(delta int) createForm {
	if len(f.types) == 0 {
		return f
	}
	f.typeIx = (f.typeIx + delta + len(f.types)) % len(f.types)
	if f.lockedParent {
		return f
	}
	return f.withParent("", "")
}

func (f createForm) moveFocus(delta int) (createForm, tea.Cmd) {
	fs := f.fields()
	cur := indexOfInt(fs, f.focus)
	if cur < 0 {
		cur = 0
	}
	return f.focusOn(fs[(cur+delta+len(fs))%len(fs)])
}

func (f createForm) focusOn(id int) (createForm, tea.Cmd) {
	f.focus = id
	f.title.Blur()
	f.summary.Blur()
	f.content.Blur()
	for i := range f.customFields {
		f.customFields[i] = f.customFields[i].blur()
	}
	var cmd tea.Cmd
	switch {
	case id == nfTitle:
		cmd = f.title.Focus()
	case id == nfSummary:
		cmd = f.summary.Focus()
	case id == nfContent:
		cmd = f.content.Focus()
	case isCustomFocus(id):
		if i := customIndex(id); i >= 0 && i < len(f.customFields) {
			f.customFields[i], cmd = f.customFields[i].focus()
		}
	}
	return f, cmd
}

func (f createForm) typeIntoFocused(msg tea.KeyPressMsg) (createForm, tea.Cmd) {
	var cmd tea.Cmd
	switch f.focus {
	case nfTitle:
		f.titleErr = ""
		f.title, cmd = f.title.Update(msg)
	case nfSummary:
		f.summary, cmd = f.summary.Update(msg)
	case nfContent:
		f.content, cmd = f.content.Update(msg)
	}
	return f, cmd
}

func (f createForm) updateInputs(msg tea.Msg) (createForm, tea.Cmd) {
	var tc, sc, cc tea.Cmd
	f.title, tc = f.title.Update(msg)
	f.summary, sc = f.summary.Update(msg)
	f.content, cc = f.content.Update(msg)
	return f, tea.Batch(tc, sc, cc)
}

func (f createForm) submit() (createForm, tea.Cmd) {
	if f.customLoading {
		// wait for the type's required-field checks rather than eat a confusing server-side rejection
		return f, toast.Show(toast.Info, "Loading this type's fields…")
	}
	f.titleErr, f.parentErr = "", ""
	title := strings.TrimSpace(f.title.Value())
	if title == "" {
		f.titleErr = "Required field"
		return f.focusOn(nfTitle)
	}
	if parentRequired(f.selectedHierarchy()) && f.parentKey == "" {
		f.parentErr = "Pick a parent (required for this type)"
		return f.focusOn(nfParent)
	}
	var due time.Time
	if f.dueSet {
		due = f.dueAt
	}
	for i := range f.customFields {
		f.customFields[i].err = ""
	}
	custom := map[string]interface{}{}
	for i := range f.customFields {
		val, present, errMsg := f.customFields[i].value()
		if errMsg != "" {
			f.customFields[i].err = errMsg
			return f.focusOn(nfCustomBase + i)
		}
		if present {
			custom[strconv.Itoa(f.customFields[i].field.ID)] = val
		}
	}
	var typeID int64
	if len(f.types) > 0 {
		typeID = int64(f.types[f.typeIx].ID)
	}
	return f, submitCreateIssue(createValues{
		typeID:       typeID,
		title:        title,
		summary:      strings.TrimSpace(f.summary.Value()),
		content:      f.content.Value(),
		priority:     filterPriorities[f.priorityIx],
		dueAt:        due,
		parentKey:    f.parentKey,
		customFields: custom,
	})
}

func (f createForm) onClick(msg tea.MouseClickMsg) (createForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return f, nil
	}
	id := f.hitZone(msg)
	if isCustomFocus(id) {
		f, _ = f.focusOn(id)
		i := customIndex(id)
		if i < 0 || i >= len(f.customFields) {
			return f, nil
		}
		// a click on a checklist option or a cycle arrow acts on that control, not the field as a whole
		if c, consumed := f.customFields[i].clickAt(msg, createZone(id)); consumed {
			f.customFields[i] = c
			return f, nil
		}
		if f.customFields[i].isDate() {
			return f, openCustomDate(i)
		}
		return f, nil
	}
	switch id {
	case nfType, nfTitle, nfSummary, nfContent, nfPriority:
		return f.focusOn(id)
	case nfDue:
		f, _ = f.focusOn(nfDue)
		return f, openDueCreate
	case nfParent:
		f, _ = f.focusOn(nfParent)
		return f, openParentPickerCmd
	case nfCreate:
		return f.submit()
	case nfCancel:
		return f, cancelCreateIssue
	}
	return f, nil
}

func (f createForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(createZone(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f createForm) View() string {
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("New issue", body, f.deps.Styles.Theme.Primary)
}

// formRow pairs a rendered field row with its focus id, so body() and FocusRow() agree on order and height.
type formRow struct {
	id   int
	view string
}

func (f createForm) rows() []formRow {
	t := f.deps.Styles.Theme
	rows := []formRow{
		{nfType, f.field(nfType, "Type", f.typeContent(), "")},
		{nfParent, f.field(nfParent, "Parent", f.parentContent(), f.parentErr)},
		{nfTitle, f.field(nfTitle, "Title", fixField(f.title.View(), 1), f.titleErr)},
		{nfSummary, f.field(nfSummary, "Summary", fixField(f.summary.View(), 1), "")},
		{nfContent, f.field(nfContent, "Content", fixField(f.content.View(), editContentH), "")},
		{nfPriority, f.field(nfPriority, "Priority", f.priorityContent(), "")},
		{nfDue, f.field(nfDue, "Due", f.dueContent(), "")},
	}
	for i := range f.customFields {
		c := f.customFields[i]
		id := nfCustomBase + i
		rows = append(rows, formRow{id, f.field(id, c.label(), c.content(t, f.focus == id, createZone(id)), c.err)})
	}
	return rows
}

func (f createForm) body() string {
	rows := f.rows()
	views := make([]string, 0, len(rows)+2)
	for _, r := range rows {
		views = append(views, r.view)
	}
	views = append(views, "", f.buttons())
	return lipgloss.JoinVertical(lipgloss.Left, views...)
}

// FocusRow reports the focused control's row and height in View coordinates, for scroll-into-view.
// chromeTop 2 = top border + the padding row above the body.
func (f createForm) FocusRow() (int, int, bool) {
	const chromeTop = 2
	row := chromeTop
	for _, r := range f.rows() {
		h := lipgloss.Height(r.view)
		if r.id == f.focus {
			return row, h, true
		}
		row += h
	}
	return row + 1, lipgloss.Height(f.buttons()), true // the buttons sit after every field plus the blank row
}

// typeContent is the type cycle "‹ Story ›", the name tinted with the type's color.
func (f createForm) typeContent() string {
	t := f.deps.Styles.Theme
	name := "-"
	tint := t.Text
	if len(f.types) > 0 {
		it := f.types[f.typeIx]
		name = it.Name
		if c, ok := components.IssueColor(it.Color); ok {
			tint = c
		}
	}
	arrow := t.Muted
	if f.focus == nfType {
		arrow = t.Accent
	}
	val := lipgloss.NewStyle().Foreground(tint).Bold(true).Render(name)
	body := lipgloss.NewStyle().Foreground(arrow).Render("‹ ") + val + lipgloss.NewStyle().Foreground(arrow).Render(" ›")
	return fixField(body, 1)
}

// parentContent shows the picked parent, or a hint reflecting the type's hierarchy (top-level types take
// none, SUBTASK/MICROTASK require one). A locked parent renders plainly, since it cannot change here.
func (f createForm) parentContent() string {
	t := f.deps.Styles.Theme
	if f.lockedParent {
		label := f.parentLabel
		if label == "" {
			label = f.parentKey
		}
		return fixField(lipgloss.NewStyle().Foreground(t.Text).Render(components.Trunc(flattenLine(label), editFieldW)), 1)
	}
	hier := f.selectedHierarchy()
	if _, ok := parentHierarchy(hier); !ok {
		return fixField(lipgloss.NewStyle().Foreground(t.Muted).Render("Top-level (no parent)"), 1)
	}
	if f.parentKey != "" {
		label := f.parentLabel
		if label == "" {
			label = f.parentKey
		}
		return fixField(lipgloss.NewStyle().Foreground(t.Text).Render(components.Trunc(flattenLine(label), editFieldW)), 1)
	}
	hint := "‹ Select… › (optional)"
	if parentRequired(hier) {
		hint = "‹ Select… › (required)"
	}
	col := t.Muted
	if f.focus == nfParent {
		col = t.Accent
	}
	return fixField(lipgloss.NewStyle().Foreground(col).Render(hint), 1)
}

// dueContent shows the chosen date or a "Select…" hint. Due is set from the calendar, never typed.
func (f createForm) dueContent() string {
	t := f.deps.Styles.Theme
	col := t.Muted
	label := "Select…"
	if f.dueSet {
		label, col = formatDateOnly(f.dueAt), t.Text
	}
	if f.focus == nfDue {
		col = t.Accent
	}
	return fixField(lipgloss.NewStyle().Foreground(col).Render(label), 1)
}

func (f createForm) priorityContent() string {
	t := f.deps.Styles.Theme
	p := filterPriorities[f.priorityIx]
	arrow := t.Muted
	if f.focus == nfPriority {
		arrow = t.Accent
	}
	val := lipgloss.NewStyle().Foreground(priorityColor(t, p)).Bold(true).Render(p)
	body := lipgloss.NewStyle().Foreground(arrow).Render("‹ ") + val + lipgloss.NewStyle().Foreground(arrow).Render(" ›")
	return fixField(body, 1)
}

func (f createForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBoxWeighted(label, content, f.fieldBorderColor(id, errMsg), f.focus == id)
	box = zone.Mark(createZone(id), box)
	if errMsg != "" {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.deps.Styles.Error.Render(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f createForm) fieldBorderColor(id int, errMsg string) color.Color {
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

func (f createForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Create", nfCreate),
		" ",
		f.button("Cancel", nfCancel),
	)
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f createForm) button(label string, id int) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case f.focus == id:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case f.hover == id:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(createZone(id), components.TitledBoxWeighted("", body, borderCol, f.focus == id))
}

func (f createForm) HelpKeys() []key.Binding {
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	switch {
	case f.focus == nfType:
		binds = append(binds, key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "type")))
	case f.focus == nfPriority:
		binds = append(binds, key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "priority")))
	case isCustomFocus(f.focus):
		if i := customIndex(f.focus); i >= 0 && i < len(f.customFields) {
			if h := f.customFields[i].cfHelp(); h != "" {
				binds = append(binds, key.NewBinding(key.WithKeys("left", "right", "space"), key.WithHelp("", h)))
			}
		}
	}
	return append(binds,
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "create")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	)
}

func createZone(id int) string {
	if isCustomFocus(id) {
		return "project.create.custom." + strconv.Itoa(customIndex(id))
	}
	switch id {
	case nfType:
		return "project.create.type"
	case nfParent:
		return "project.create.parent"
	case nfTitle:
		return "project.create.title"
	case nfSummary:
		return "project.create.summary"
	case nfContent:
		return "project.create.content"
	case nfPriority:
		return "project.create.priority"
	case nfDue:
		return "project.create.due"
	case nfCreate:
		return "project.create.create"
	case nfCancel:
		return "project.create.cancel"
	}
	return ""
}

// createValues is the new-issue payload the form emits on submit.
type createValues struct {
	typeID       int64
	title        string
	summary      string
	content      string
	priority     string
	dueAt        time.Time // zero = no due date
	parentKey    string    // "" = no parent
	customFields map[string]interface{}
}

type createSubmittedMsg struct{ v createValues }

type createCancelledMsg struct{}

func cancelCreateIssue() tea.Msg { return createCancelledMsg{} }

func submitCreateIssue(v createValues) tea.Cmd {
	return func() tea.Msg { return createSubmittedMsg{v: v} }
}

// IssueCreatedMsg carries a create result. Exported so the app shell can route it after the user has left
// the drill-in.
type IssueCreatedMsg struct {
	key     string
	err     bool
	errText string // the resolved failure toast line (server reason / mapped code / fallback)
}

func createIssue(d deps.Deps, projectKey string, v createValues) tea.Cmd {
	return func() tea.Msg {
		key, err := d.Issues.CreateIssue(context.Background(), projectKey, domain.CreateIssueInput{
			IssueTypeID: v.typeID, Title: v.title, Summary: v.summary, Content: v.content,
			Priority: v.priority, DueAt: v.dueAt, ParentKey: v.parentKey, CustomFields: v.customFields,
		})
		return IssueCreatedMsg{key: key, err: err != nil, errText: errmsg.Message(err, "Could not create the issue.")}
	}
}

// openCreateForm refuses until the issue-type catalog (prefetched in Init) has loaded.
func (m Model) openCreateForm() (Model, tea.Cmd) {
	if len(m.types) == 0 {
		return m, toast.Show(toast.Info, "Loading issue types…")
	}
	m.creating = true
	m.createScroll = 0
	m.parentGen++ // drop any parent-candidate load still in flight from a prior session
	m.createUI = newCreateForm(m.deps, m.types)
	m2, cmd := m.requestCustomFields(int64(m.types[0].ID))
	return m2, tea.Batch(m2.createUI.Init(), cmd)
}

func (m Model) updateCreate(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case createCancelledMsg:
		m.creating = false
		return m, nil
	case createSubmittedMsg:
		return m.submitCreate(msg.v)
	case tea.MouseWheelMsg:
		if lipgloss.Height(m.createUI.View()) > m.height {
			switch msg.Button {
			case tea.MouseWheelUp:
				m.createScroll = clampScroll(m.createScroll-1, m.createScrollMax())
				return m, nil
			case tea.MouseWheelDown:
				m.createScroll = clampScroll(m.createScroll+1, m.createScrollMax())
				return m, nil
			}
		}
	}
	var cmd tea.Cmd
	m.createUI, cmd = m.createUI.Update(msg)
	return m.followCreateFocus(), cmd
}

func (m Model) createScrollMax() int {
	return max(0, lipgloss.Height(m.createUI.View())-m.height)
}

// followCreateFocus scrolls the windowed modal to keep the focused control visible.
func (m Model) followCreateFocus() Model {
	row, height, ok := m.createUI.FocusRow()
	if !ok {
		return m
	}
	boxH := lipgloss.Height(m.createUI.View())
	if boxH <= m.height {
		return m
	}
	visible := m.height - 2 // ScrollBox shows interior box-lines [off+1, off+visible]
	off := m.createScroll
	top, bottom := row, row+max(1, height)-1
	if top < 1+off {
		off = top - 1
	} else if bottom > off+visible {
		off = bottom - visible
	}
	m.createScroll = min(max(off, 0), boxH-m.height)
	return m
}

// submitCreate is not optimistic, unlike edit/transition: the row needs the server-assigned key.
func (m Model) submitCreate(v createValues) (Model, tea.Cmd) {
	m.creating = false
	return m, createIssue(m.deps, m.projectKey, v)
}
