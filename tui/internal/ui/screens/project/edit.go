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

// focusable controls, in tab order. Content is edited in its own modal (E), not here.
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
	// Priority and Story point share one line. Each titled box spends 4 non-content cells, so:
	// (L+4) + 1gap + (R+4) == editFieldW+4  ⇒  L+R == editFieldW-5.
	editHalfL = (editFieldW - 5) / 2
	editHalfR = editFieldW - 5 - editHalfL
)

// editForm edits an issue's common fields. State, assignee and content have their own modals.
type editForm struct {
	deps deps.Deps

	title            textinput.Model
	storyPoint       textinput.Model // numeric, only a tab stop when canUseStoryPoint
	canUseStoryPoint bool            // the issue type permits a story point
	parentKey        string          // "" = none, set by the shared parent picker
	canHaveParent    bool            // the issue type is not top-level, so a Parent field shows
	dueAt            time.Time       // zero = none, set from the calendar picker
	dueSet           bool
	priorityIx       int

	// origCustom is what each field serialized to at open, so the save sends only real changes (resending
	// an untouched field would stamp it as edited in the activity log). Keyed by field id as a string.
	customFields []customFieldInput
	origCustom   map[string]interface{}

	focus    int
	hover    int
	titleErr string
}

// focusIsArea reports whether focus is a multi-line TEXT field, where enter/up/down edit instead of navigating.
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
		// serialize the seed with the save's serializer, so the diff compares like with like
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

// fields returns the tab order. Story point is a stop only when the type permits a point.
func (f editForm) fields() []int {
	ids := []int{efTitle}
	if f.canHaveParent {
		ids = append(ids, efParent)
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
			return f, openDueEdit
		case efParent:
			return f, openParentEditForm
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

// customKey drives the focused custom field, mirroring the create form.
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
			return f, nil // a story point is a whole number, so drop non-digit input
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

// isDigits backs the story point input filter (empty is false).
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
	// digit-filtered input always parses. empty means unset (0)
	sp := 0
	if v := strings.TrimSpace(f.storyPoint.Value()); v != "" {
		sp, _ = strconv.Atoi(v)
	}
	f, custom, ok := f.diffCustom()
	if !ok {
		return f, nil // a field failed validation, now rendered under it
	}
	return f, submitEditIssue(editValues{
		title:        title,
		priority:     filterPriorities[f.priorityIx],
		dueAt:        due,
		storyPoint:   sp,
		customFields: custom,
	})
}

// diffCustom keeps only the values that differ from the form's open state, so an untouched field is never
// resent (that would stamp it as edited). A cleared value becomes an explicit nil. ok=false on a bad field.
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
		return f, openParentEditForm
	case efDue:
		f, _ = f.focusOn(efDue)
		return f, openDueEdit
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

// priorityStoryRow puts Priority and Story point on one line, Priority full-width without the point.
func (f editForm) priorityStoryRow() string {
	if !f.canUseStoryPoint {
		return f.field(efPriority, "Priority", f.priorityContent(editFieldW), "")
	}
	left := f.field(efPriority, "Priority", f.priorityContent(editHalfL), "")
	right := f.field(efStoryPoint, "Story point", fixFieldW(f.storyPoint.View(), editHalfR, 1), "")
	return lipgloss.JoinHorizontal(lipgloss.Top, left, " ", right)
}

// FocusRow reports the focused control's row and height in View coordinates, for scroll-into-view.
// chromeTop 2 = top border + the padding row. Priority and Story point share one row.
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
	// the buttons sit after every field plus the blank row
	return row + 1, lipgloss.Height(f.buttons()), true
}

// dueContent renders the Due field. The value comes from the calendar, never typed.
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

// parentContent renders the Parent field. The value comes from the parent picker, never typed.
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

// editValues is the common-field state emitted on save. The model diffs it against the loaded detail.
type editValues struct {
	title      string
	priority   string
	dueAt      time.Time // zero = no due date
	storyPoint int       // 0 = unset

	// only the changed custom fields, keyed by field id. A nil value is an explicit clear.
	customFields map[string]interface{}
}

type editSubmittedMsg struct{ v editValues }

type editCancelledMsg struct{}

func cancelEditIssue() tea.Msg { return editCancelledMsg{} }

func submitEditIssue(v editValues) tea.Cmd {
	return func() tea.Msg { return editSubmittedMsg{v: v} }
}

// openEditForm refuses while the detail is loading, since there is nothing to prefill or diff against.
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

// canHaveParent gates the Parent field, mirroring the picker's guard: an EPIC or an unresolved type
// (catalog not loaded) shows no Parent row.
func (m Model) canHaveParent(typeName string) bool {
	hier, ok := m.hierarchyForType(typeName)
	if !ok {
		return false
	}
	_, ok = parentHierarchy(hier)
	return ok
}

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

// followEditFocus scrolls the windowed modal to keep the focused control visible.
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

// submitEdit PATCHes only what changed, applies it optimistically, then refetches to reconcile. The diff
// is against editBase, so a field the user never touched is never resent.
func (m Model) submitEdit(v editValues) (Model, tea.Cmd) {
	m.editing = false
	edit := diffEdit(m.editBase, v)
	if edit.Empty() {
		return m, toast.Show(toast.Info, "No changes.")
	}
	m.applyEdit(m.viewKey, edit) // optimistic (a no-op if the cache was evicted)
	return m, editIssue(m.deps, m.viewKey, edit)
}

// diffEdit includes a field only when it differs from the open-time snapshot. The title is compared
// trimmed and the due date at day granularity. Summary and content are never sent from here.
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
	// story point rides a separate endpoint. A type that disallows it keeps the original, so this is a no-op.
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

// applyEdit writes only the changed fields into the cache and bumps the load generation, so an earlier
// in-flight refetch cannot clobber it. Diff-only, so a refetched value for an untouched field survives.
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

// EditDoneMsg is exported so the app shell can route it after the user has left the drill-in.
type EditDoneMsg struct {
	key     string
	err     bool
	errText string // the resolved failure toast line (server reason / mapped code / fallback)
}

// editIssue spans whichever endpoints the diff touches, stopping at the first error with one result.
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
