package project

import (
	"context"
	"image/color"
	"reflect"
	"strconv"
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
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// the edit form's focusable controls, in tab order. Story point sits on the Priority row (a tab stop only
// when the issue type permits a point). Content is edited in its own modal (E / the Content pen), not here.
const (
	efTitle = iota
	efParent
	efPriority
	efStoryPoint
	efDue
	efSave
	efCancel
)

const (
	editFieldW   = 60
	editContentH = 6
	// Priority and Story point share one line. Each titled box spends 4 non-content cells (two borders +
	// a one-cell inset on each side), so a full-width field's outer width is editFieldW+4. For the two
	// halves plus their one-cell gap to match that, the content halves must sum to editFieldW-5:
	// (L+4) + 1gap + (R+4) == editFieldW+4  ⇒  L+R == editFieldW-5.
	editHalfL = (editFieldW - 5) / 2
	editHalfR = editFieldW - 5 - editHalfL
)

// editForm is the "Edit issue" modal for an issue's common fields: title, priority, story point and due
// date. State and assignee have their own pickers; content has its own editor modal; summary is omitted.
type editForm struct {
	deps deps.Deps

	title            textinput.Model
	storyPoint       textinput.Model // numeric; only a tab stop when canUseStoryPoint
	canUseStoryPoint bool            // the issue type permits a story point; gates the field
	parentKey        string          // the current parent's key ("" = none); set by the shared parent picker
	canHaveParent    bool            // the issue type is not top-level; gates the Parent field (below Title)
	dueAt            time.Time       // the chosen due date (zero = none), set from the calendar picker
	dueSet           bool
	priorityIx       int

	// the issue type's custom fields, seeded from the issue's current values. origCustom is what each
	// field serialized to when the form opened, so the save can send only what actually changed - the
	// server merges the map, and re-sending an untouched field would stamp it as edited in the activity
	// log. Both are keyed by field id as a string, the wire's key type.
	customFields []customFieldInput
	origCustom   map[string]interface{}

	focus    int
	hover    int
	titleErr string
}

// focusIsArea reports whether the focused control is a multi-line TEXT custom field, whose enter/up/down
// are editing keys rather than form navigation (mirroring the create form).
func (f editForm) focusIsArea() bool {
	if !isCustomFocus(f.focus) {
		return false
	}
	i := customIndex(f.focus)
	return i >= 0 && i < len(f.customFields) && f.customFields[i].isArea()
}

func newEditForm(d deps.Deps, det domain.IssueDetail, canHaveParent bool) editForm {
	title := textinput.New()
	title.Prompt = ""
	title.SetWidth(editFieldW)
	title.CharLimit = 255
	title.SetValue(det.Title)

	sp := textinput.New()
	sp.Prompt = ""
	sp.SetWidth(editHalfR)
	sp.CharLimit = 5 // a point is a small whole number
	sp.Placeholder = "None"
	if det.StoryPoint > 0 {
		sp.SetValue(strconv.Itoa(det.StoryPoint))
	}

	parentKey := ""
	if det.Parent != nil {
		parentKey = det.Parent.Key
	}

	custom := make([]customFieldInput, 0, len(det.CustomFields))
	orig := make(map[string]interface{}, len(det.CustomFields))
	for _, cf := range det.CustomFields {
		in := newCustomFieldInput(cf.Definition()).seed(cf.Raw)
		// record the seeded value through the same serializer the save uses, so the diff compares like
		// with like rather than against the wire shape the detail happened to carry
		if v, present, _ := in.value(); present {
			orig[strconv.Itoa(in.field.ID)] = v
		}
		custom = append(custom, in)
	}

	f := editForm{
		deps: d, title: title, storyPoint: sp,
		canUseStoryPoint: det.CanUseStoryPoint,
		parentKey:        parentKey, canHaveParent: canHaveParent,
		dueAt: det.DueAt, dueSet: !det.DueAt.IsZero(),
		customFields: custom, origCustom: orig,
		priorityIx: max(0, indexOf(filterPriorities, det.Priority)), focus: efTitle, hover: -1,
	}
	f.title.Focus()
	return f
}

// setDue records a calendar pick (set=false clears the due date).
func (f editForm) setDue(v time.Time, set bool) editForm {
	f.dueAt, f.dueSet = v, set
	return f
}

func (f editForm) Init() tea.Cmd { return textinput.Blink }

// fields returns the focusable control ids in tab order. Story point is a tab stop only when the issue
// type permits a point (otherwise the field is not rendered).
func (f editForm) fields() []int {
	ids := []int{efTitle}
	if f.canHaveParent {
		ids = append(ids, efParent) // a picker-backed field directly below Title, like the create form
	}
	ids = append(ids, efPriority)
	if f.canUseStoryPoint {
		ids = append(ids, efStoryPoint)
	}
	ids = append(ids, efDue)
	for i := range f.customFields {
		ids = append(ids, nfCustomBase+i)
	}
	return append(ids, efSave, efCancel)
}

func (f editForm) Update(msg tea.Msg) (editForm, tea.Cmd) {
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

func (f editForm) onKey(msg tea.KeyPressMsg) (editForm, tea.Cmd) {
	switch msg.String() {
	case "esc":
		return f, cancelEditIssue
	case "tab":
		return f.moveFocus(1)
	case "shift+tab":
		return f.moveFocus(-1)
	case "up":
		if !f.focusIsArea() {
			return f.moveFocus(-1)
		}
	case "down":
		if !f.focusIsArea() {
			return f.moveFocus(1)
		}
	}
	if isCustomFocus(f.focus) {
		return f.customKey(msg)
	}
	switch msg.String() {
	case "left":
		if f.focus == efPriority {
			f.priorityIx = (f.priorityIx - 1 + len(filterPriorities)) % len(filterPriorities)
			return f, nil
		}
	case "right", "space":
		if f.focus == efPriority {
			f.priorityIx = (f.priorityIx + 1) % len(filterPriorities)
			return f, nil
		}
	case "enter":
		switch f.focus {
		case efSave:
			return f.submit()
		case efCancel:
			return f, cancelEditIssue
		case efDue:
			return f, openDueEdit // the model opens the calendar over the form
		case efParent:
			return f, openParentEditForm // the model opens the parent picker over the form
		default:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
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
	f.title.Blur()
	f.storyPoint.Blur()
	var cmd tea.Cmd
	switch id {
	case efTitle:
		cmd = f.title.Focus()
	case efStoryPoint:
		cmd = f.storyPoint.Focus()
	}
	for i := range f.customFields {
		f.customFields[i] = f.customFields[i].blur()
	}
	if i := customIndex(id); isCustomFocus(id) && i >= 0 && i < len(f.customFields) {
		f.customFields[i], cmd = f.customFields[i].focus()
	}
	return f, cmd
}

// customKey drives the focused custom field: a DATE/TIMESTAMP opens the calendar on enter and ignores
// typing, a multi-line TEXT takes enter as a newline, and anything else advances on enter. Mirrors the
// create form so the two modals do not diverge.
func (f editForm) customKey(msg tea.KeyPressMsg) (editForm, tea.Cmd) {
	i := customIndex(f.focus)
	if i < 0 || i >= len(f.customFields) {
		return f, nil
	}
	c := f.customFields[i]
	if c.isDate() {
		if msg.String() == "enter" {
			return f, openCustomDateEdit(i)
		}
		return f, nil
	}
	if msg.String() == "enter" && !c.isArea() {
		return f.moveFocus(1)
	}
	f.customFields[i], _ = f.customFields[i].handleKey(msg)
	return f, nil
}

// setCustomDate records a calendar pick on custom field i (set=false clears an optional field).
func (f editForm) setCustomDate(i int, v time.Time, set bool) editForm {
	if i >= 0 && i < len(f.customFields) {
		f.customFields[i] = f.customFields[i].setDate(v, set)
	}
	return f
}

func (f editForm) typeIntoFocused(msg tea.KeyPressMsg) (editForm, tea.Cmd) {
	var cmd tea.Cmd
	switch f.focus {
	case efTitle:
		f.titleErr = ""
		f.title, cmd = f.title.Update(msg)
	case efStoryPoint:
		if msg.Text != "" && !isDigits(msg.Text) {
			return f, nil // a story point is a whole number; drop non-digit character input
		}
		f.storyPoint, cmd = f.storyPoint.Update(msg)
	}
	return f, cmd
}

func (f editForm) updateInputs(msg tea.Msg) (editForm, tea.Cmd) {
	var tc, sc tea.Cmd
	f.title, tc = f.title.Update(msg)
	f.storyPoint, sc = f.storyPoint.Update(msg)
	return f, tea.Batch(tc, sc)
}

// isDigits reports whether s is non-empty and all ASCII digits, for the story point input filter.
func isDigits(s string) bool {
	if s == "" {
		return false
	}
	for _, r := range s {
		if r < '0' || r > '9' {
			return false
		}
	}
	return true
}

func (f editForm) submit() (editForm, tea.Cmd) {
	f.titleErr = ""
	title := strings.TrimSpace(f.title.Value())
	if title == "" {
		f.titleErr = "Required field"
		return f.focusOn(efTitle)
	}
	var due time.Time
	if f.dueSet {
		due = f.dueAt
	}
	// the input is digit-filtered, so a non-empty value always parses; empty means unset (0)
	sp := 0
	if v := strings.TrimSpace(f.storyPoint.Value()); v != "" {
		sp, _ = strconv.Atoi(v)
	}
	f, custom, ok := f.diffCustom()
	if !ok {
		return f, nil // a field reported a validation error, which is now rendered under it
	}
	return f, submitEditIssue(editValues{
		title:        title,
		priority:     filterPriorities[f.priorityIx],
		dueAt:        due,
		storyPoint:   sp,
		customFields: custom,
	})
}

// diffCustom validates every custom input and keeps only the values that differ from what the form
// opened with, so an untouched field is never resent (which would stamp it as edited in the activity
// log). A value that was set and is now empty becomes an explicit nil, which clears it. ok is false when
// a field failed validation - its message is set for rendering and the save is held back.
func (f editForm) diffCustom() (editForm, map[string]interface{}, bool) {
	out := map[string]interface{}{}
	ok := true
	for i := range f.customFields {
		f.customFields[i].err = ""
		v, present, errMsg := f.customFields[i].value()
		if errMsg != "" {
			f.customFields[i].err = errMsg
			ok = false
			continue
		}
		key := strconv.Itoa(f.customFields[i].field.ID)
		orig, had := f.origCustom[key]
		switch {
		case !present && had:
			out[key] = nil
		case present && (!had || !reflect.DeepEqual(orig, v)):
			out[key] = v
		}
	}
	return f, out, ok
}

func (f editForm) onClick(msg tea.MouseClickMsg) (editForm, tea.Cmd) {
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
		// a click on a checklist option or a cycle arrow acts on that control; anything else in the box
		// just focuses it (or, for a date field, opens the calendar)
		if c, consumed := f.customFields[i].clickAt(msg, editZone(id)); consumed {
			f.customFields[i] = c
			return f, nil
		}
		if f.customFields[i].isDate() {
			return f, openCustomDateEdit(i)
		}
		return f, nil
	}
	switch id {
	case efTitle, efPriority, efStoryPoint:
		return f.focusOn(id)
	case efParent:
		f, _ = f.focusOn(efParent)
		return f, openParentEditForm // clicking Parent opens the parent picker
	case efDue:
		f, _ = f.focusOn(efDue)
		return f, openDueEdit // clicking Due opens the calendar
	case efSave:
		return f.submit()
	case efCancel:
		return f, cancelEditIssue
	}
	return f, nil
}

func (f editForm) hitZone(msg tea.MouseMsg) int {
	for _, id := range f.fields() {
		if zone.Get(editZone(id)).InBounds(msg) {
			return id
		}
	}
	return -1
}

func (f editForm) View() string {
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("Edit issue", body, f.deps.Styles.Theme.Primary)
}

func (f editForm) body() string {
	rows := []string{f.field(efTitle, "Title", fixField(f.title.View(), 1), f.titleErr)}
	if f.canHaveParent {
		rows = append(rows, f.field(efParent, "Parent", f.parentContent(), ""))
	}
	rows = append(rows,
		f.priorityStoryRow(),
		f.field(efDue, "Due", f.dueContent(), ""),
	)
	rows = append(rows, f.customRows()...)
	rows = append(rows, "", f.buttons())
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// customRows renders the issue type's custom fields, in the order the detail listed them.
func (f editForm) customRows() []string {
	t := f.deps.Styles.Theme
	out := make([]string, 0, len(f.customFields))
	for i := range f.customFields {
		c := f.customFields[i]
		id := nfCustomBase + i
		out = append(out, f.field(id, c.label(), c.content(t, f.focus == id, editZone(id)), c.err))
	}
	return out
}

// priorityStoryRow lays Priority and Story point on one line (the point half only when the issue type
// permits it, in which case Priority takes the full width).
func (f editForm) priorityStoryRow() string {
	if !f.canUseStoryPoint {
		return f.field(efPriority, "Priority", f.priorityContent(editFieldW), "")
	}
	left := f.field(efPriority, "Priority", f.priorityContent(editHalfL), "")
	right := f.field(efStoryPoint, "Story point", fixFieldW(f.storyPoint.View(), editHalfR, 1), "")
	return lipgloss.JoinHorizontal(lipgloss.Top, left, " ", right)
}

// FocusRow reports the focused control's row (in the bordered View's coordinates) and height, so a
// windowed modal scrolls to keep it visible. +2 = top border + the padding row above the body. Priority
// and Story point share one row, so a focus on either maps to it.
func (f editForm) FocusRow() (int, int, bool) {
	const chromeTop = 2
	type frow struct {
		ids  []int
		view string
	}
	rows := []frow{{[]int{efTitle}, f.field(efTitle, "Title", fixField(f.title.View(), 1), f.titleErr)}}
	if f.canHaveParent {
		rows = append(rows, frow{[]int{efParent}, f.field(efParent, "Parent", f.parentContent(), "")})
	}
	rows = append(rows,
		frow{[]int{efPriority, efStoryPoint}, f.priorityStoryRow()},
		frow{[]int{efDue}, f.field(efDue, "Due", f.dueContent(), "")},
	)
	for i, view := range f.customRows() {
		rows = append(rows, frow{[]int{nfCustomBase + i}, view})
	}
	row := chromeTop
	for _, r := range rows {
		h := lipgloss.Height(r.view)
		if indexOfInt(r.ids, f.focus) >= 0 {
			return row, h, true
		}
		row += h
	}
	// the Save/Cancel buttons sit after every field plus the blank row before them
	return row + 1, lipgloss.Height(f.buttons()), true
}

// dueContent renders the Due field body: the chosen date, or a "Select…" hint. Accent when focused,
// matching the picker-backed Parent field. The field is set from the calendar, never typed.
func (f editForm) dueContent() string {
	t := f.deps.Styles.Theme
	col := t.Muted
	label := "Select…"
	if f.dueSet {
		label, col = formatDateOnly(f.dueAt), t.Text
	}
	if f.focus == efDue {
		col = t.Accent
	}
	return fixField(lipgloss.NewStyle().Foreground(col).Render(label), 1)
}

// parentContent renders the Parent field body: the current parent's key, or a "Select…" hint. Accent
// when focused, matching Due. The value is set from the shared parent picker, never typed.
func (f editForm) parentContent() string {
	t := f.deps.Styles.Theme
	col := t.Muted
	label := "Select…"
	if f.parentKey != "" {
		label, col = f.parentKey, t.Text
	}
	if f.focus == efParent {
		col = t.Accent
	}
	return fixField(lipgloss.NewStyle().Foreground(col).Render(label), 1)
}

func (f editForm) priorityContent(w int) string {
	t := f.deps.Styles.Theme
	p := filterPriorities[f.priorityIx]
	arrow := t.Muted
	if f.focus == efPriority {
		arrow = t.Accent
	}
	val := lipgloss.NewStyle().Foreground(priorityColor(t, p)).Bold(true).Render(p)
	body := lipgloss.NewStyle().Foreground(arrow).Render("‹ ") + val + lipgloss.NewStyle().Foreground(arrow).Render(" ›")
	return fixFieldW(body, w, 1)
}

func (f editForm) field(id int, label, content, errMsg string) string {
	box := components.TitledBoxWeighted(label, content, f.fieldBorderColor(id, errMsg), f.focus == id)
	box = zone.Mark(editZone(id), box)
	if errMsg != "" {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.deps.Styles.Error.Render(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
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

func (f editForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", efSave),
		" ",
		f.button("Cancel", efCancel),
	)
	return lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
}

func (f editForm) button(label string, id int) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case f.focus == id:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case f.hover == id:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(editZone(id), components.TitledBoxWeighted("", body, borderCol, f.focus == id))
}

func (f editForm) HelpKeys() []key.Binding {
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next"))}
	switch f.focus {
	case efPriority:
		binds = append(binds, key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "priority")))
	case efStoryPoint:
		binds = append(binds, key.NewBinding(key.WithKeys("0", "9"), key.WithHelp("0-9", "points")))
	}
	return append(binds,
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	)
}

func fixField(s string, h int) string { return fixFieldW(s, editFieldW, h) }

func fixFieldW(s string, w, h int) string {
	return lipgloss.NewStyle().Width(w).MaxWidth(w).Height(h).MaxHeight(h).Render(s)
}

func editZone(id int) string {
	if isCustomFocus(id) {
		return "project.edit.custom." + strconv.Itoa(customIndex(id))
	}
	switch id {
	case efTitle:
		return "project.edit.title"
	case efParent:
		return "project.edit.parent"
	case efPriority:
		return "project.edit.priority"
	case efStoryPoint:
		return "project.edit.storypoint"
	case efDue:
		return "project.edit.due"
	case efSave:
		return "project.edit.save"
	case efCancel:
		return "project.edit.cancel"
	}
	return ""
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

// editValues is the new common-field state the form emits on save (content lives in its own editor); the
// model diffs it against the loaded detail to send only what changed and to update the cache optimistically.
type editValues struct {
	title      string
	priority   string
	dueAt      time.Time // zero = no due date
	storyPoint int       // 0 = unset

	// customFields holds only the custom fields whose value changed, keyed by field id. A nil value is an
	// explicit clear. Empty means nothing to send.
	customFields map[string]interface{}
}

type editSubmittedMsg struct{ v editValues }

type editCancelledMsg struct{}

func cancelEditIssue() tea.Msg { return editCancelledMsg{} }

func submitEditIssue(v editValues) tea.Cmd {
	return func() tea.Msg { return editSubmittedMsg{v: v} }
}

// openEditForm opens the edit modal, prefilled from the loaded detail. It refuses while the detail is
// still loading (there would be nothing to prefill or diff against).
func (m Model) openEditForm() (Model, tea.Cmd) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return m, toast.Show(toast.Info, "Still loading this issue…")
	}
	m.editing = true
	m.editScroll = 0
	m.editBase = d // diff the save against this snapshot, not a cache a background refetch may change mid-edit
	m.editUI = newEditForm(m.deps, d, m.canHaveParent(d.TypeName))
	return m, m.editUI.Init()
}

// canHaveParent reports whether an issue of this type may be parented (it is not a top-level type and
// the issue-type catalog is loaded). It gates the edit form's Parent field, mirroring the picker's own
// guard: an EPIC (or an unresolved type) shows no Parent row.
func (m Model) canHaveParent(typeName string) bool {
	hier, ok := m.hierarchyForType(typeName)
	if !ok {
		return false
	}
	_, ok = parentHierarchy(hier)
	return ok
}

// updateEdit drives the open edit modal: submit/cancel close it, a wheel scrolls a modal too tall for
// the terminal, and anything else is forwarded to the form (then the window follows the focused field).
func (m Model) updateEdit(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case editCancelledMsg:
		m.editing = false
		return m, nil
	case editSubmittedMsg:
		return m.submitEdit(msg.v)
	case tea.MouseWheelMsg:
		if lipgloss.Height(m.editUI.View()) > m.height {
			switch msg.Button {
			case tea.MouseWheelUp:
				m.editScroll = clampScroll(m.editScroll-1, m.editScrollMax())
				return m, nil
			case tea.MouseWheelDown:
				m.editScroll = clampScroll(m.editScroll+1, m.editScrollMax())
				return m, nil
			}
		}
	}
	var cmd tea.Cmd
	m.editUI, cmd = m.editUI.Update(msg)
	return m.followEditFocus(), cmd
}

func (m Model) editScrollMax() int {
	return max(0, lipgloss.Height(m.editUI.View())-m.height)
}

// followEditFocus scrolls the windowed edit modal so the focused control stays visible, mirroring the
// schema dashboard's focus-follow. It is a no-op when the modal already fits the terminal.
func (m Model) followEditFocus() Model {
	row, height, ok := m.editUI.FocusRow()
	if !ok {
		return m
	}
	boxH := lipgloss.Height(m.editUI.View())
	if boxH <= m.height {
		return m
	}
	visible := m.height - 2 // ScrollBox shows interior box-lines [off+1, off+visible]
	off := m.editScroll
	top, bottom := row, row+max(1, height)-1
	if top < 1+off {
		off = top - 1
	} else if bottom > off+visible {
		off = bottom - visible
	}
	m.editScroll = min(max(off, 0), boxH-m.height)
	return m
}

// submitEdit sends only the fields that changed (a PATCH), updates the cache optimistically, and starts
// a background refetch to reconcile - matching the transition/assign flow. The diff is taken against the
// snapshot the form was built from (editBase), so a field the user never touched is never resent even if
// a background refetch changed the live cache while the form was open.
func (m Model) submitEdit(v editValues) (Model, tea.Cmd) {
	m.editing = false
	edit := diffEdit(m.editBase, v)
	if edit.Empty() {
		return m, toast.Show(toast.Info, "No changes.")
	}
	m.applyEdit(m.viewKey, edit) // optimistic: show the edited fields at once (a no-op if the cache was evicted)
	return m, editIssue(m.deps, m.viewKey, edit)
}

// diffEdit builds the PATCH: a field is included only when it differs from the open-time snapshot. The
// title is compared trimmed (the save trims it too), so opening and saving an unchanged issue whose
// stored title has surrounding whitespace is not treated as an edit. The due date is compared at day
// granularity (that is all the form edits). Summary and content are not edited here, so they are never sent.
func diffEdit(orig domain.IssueDetail, v editValues) domain.IssueEdit {
	var out domain.IssueEdit
	if v.title != strings.TrimSpace(orig.Title) {
		out.Title = &v.title
	}
	if v.priority != orig.Priority {
		out.Priority = &v.priority
	}
	if formatDateOnly(v.dueAt) != formatDateOnly(orig.DueAt) {
		if v.dueAt.IsZero() {
			out.ClearDue = true
		} else {
			out.DueAt = &v.dueAt
		}
	}
	// story point rides a separate endpoint; include it only when it actually changed (when the type
	// disallows it the form keeps the original value, so this stays a no-op)
	if v.storyPoint != orig.StoryPoint {
		sp := v.storyPoint
		out.StoryPoint = &sp
	}
	// the form already diffed these against its own open-time snapshot, so they arrive pre-filtered
	if len(v.customFields) > 0 {
		out.CustomFields = v.customFields
	}
	return out
}

// applyEdit writes only the changed fields into the cached detail (and its list row) at once and bumps
// the load generation so an earlier in-flight refetch cannot clobber this optimistic write. Applying
// only the diff (not every field) preserves any newer value a background refetch left in the cache for a
// field the user did not edit.
func (m *Model) applyEdit(key string, e domain.IssueEdit) {
	d, ok := m.details[key]
	if !ok {
		return
	}
	m.detailGen[key]++
	if e.Title != nil {
		d.Title = *e.Title
	}
	if e.Content != nil {
		d.Content = *e.Content
	}
	if e.Priority != nil {
		d.Priority = *e.Priority
	}
	if e.StoryPoint != nil {
		d.StoryPoint = *e.StoryPoint
	}
	switch {
	case e.ClearDue:
		d.DueAt = time.Time{}
	case e.DueAt != nil:
		d.DueAt = *e.DueAt
	}
	m.details[key] = d
	m.patchRow(key, d)
}

// EditDoneMsg is exported so the app shell can route this background result back to the project screen
// even when the user has left the drill-in before the edit landed (so the toast still shows).
type EditDoneMsg struct {
	key     string
	err     bool
	errText string // the resolved failure toast line (server reason / mapped code / fallback)
}

// editIssue applies the diff across the two endpoints it may span: the common-fields PATCH (only when a
// common field changed) and the separate story point endpoint (only when the point changed). It stops at
// the first error and reports one result the screen reconciles with a refetch.
func editIssue(d deps.Deps, key string, e domain.IssueEdit) tea.Cmd {
	return func() tea.Msg {
		if e.HasCommonFields() {
			if err := d.Issues.UpdateIssueCommonFields(context.Background(), key, e); err != nil {
				return EditDoneMsg{key: key, err: true, errText: errmsg.Message(err, "Could not save the issue.")}
			}
		}
		if e.StoryPoint != nil {
			if err := d.Issues.UpdateIssueStoryPoint(context.Background(), key, *e.StoryPoint); err != nil {
				return EditDoneMsg{key: key, err: true, errText: errmsg.Message(err, "Could not save the issue.")}
			}
		}
		if len(e.CustomFields) > 0 {
			if err := d.Issues.UpdateIssueCustomFields(context.Background(), key, e.CustomFields); err != nil {
				return EditDoneMsg{key: key, err: true, errText: errmsg.Message(err, "Could not save the issue.")}
			}
		}
		return EditDoneMsg{key: key}
	}
}
