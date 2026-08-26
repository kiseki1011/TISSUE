package project

import (
	"strconv"
	"strings"

	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// The axes the filter exposes, in display order. The backend's sprint axes are deliberately left out.
var (
	filterStates     = []string{"INITIAL", "ACTIVE", "COMPLETED", "ABORTED"}
	filterPriorities = []string{"P0", "P1", "P2", "P3", "P4"}
	// the backend only honours these alongside a named reviewer, hence the nesting under that row
	filterReviewStatuses = []string{"PENDING", "APPROVED", "CHANGES_REQUESTED"}
)

// filterKind groups a focusable row so toggling and applying know what it means.
type filterKind int

const (
	kindState filterKind = iota
	kindPriority
	kindType
	kindAssignee
	kindReviewer
	kindReviewStatus
	kindApply
	kindCancel
)

// filterItem is one focusable row: a checkbox (state/priority/type/assignee) or a button.
type filterItem struct {
	kind  filterKind
	label string
	value string // enum value for a state or priority row
	id    int64  // catalog id for a type row
}

// filterForm is the issue filter modal: a flat list of checkboxes plus Apply/Cancel.
type filterForm struct {
	deps deps.Deps

	states     map[string]bool
	priorities map[string]bool
	types      map[int64]bool
	assigneeMe bool
	reviewerMe bool
	reviewSel  map[string]bool

	typesLoaded bool
	items       []filterItem // focusable rows in display order
	focus       int
	hover       int // item under the cursor, -1 when none
}

func newFilterForm(d deps.Deps, f domain.IssueFilter, types []domain.IssueTypeSummary, typesLoaded bool) filterForm {
	states := map[string]bool{}
	for _, s := range f.StateCategories {
		states[s] = true
	}
	priorities := map[string]bool{}
	for _, p := range f.Priorities {
		priorities[p] = true
	}
	typeSel := map[int64]bool{}
	for _, id := range f.IssueTypeIDs {
		typeSel[id] = true
	}
	reviewSel := map[string]bool{}
	for _, st := range f.ReviewerStatuses {
		reviewSel[st] = true
	}

	return filterForm{
		deps: d, states: states, priorities: priorities, types: typeSel,
		assigneeMe: f.AssigneeMe, reviewerMe: f.ReviewerMe, reviewSel: reviewSel,
		typesLoaded: typesLoaded, items: buildFilterItems(types), hover: -1,
	}
}

func buildFilterItems(types []domain.IssueTypeSummary) []filterItem {
	items := make([]filterItem, 0, len(filterStates)+len(filterPriorities)+len(types)+3)
	for _, s := range filterStates {
		items = append(items, filterItem{kind: kindState, label: stateLabel(s), value: s})
	}
	for _, p := range filterPriorities {
		items = append(items, filterItem{kind: kindPriority, label: p, value: p})
	}
	for _, t := range types {
		items = append(items, filterItem{kind: kindType, label: t.Name, id: int64(t.ID)})
	}
	items = append(items,
		filterItem{kind: kindAssignee, label: "Assigned to me"},
		filterItem{kind: kindReviewer, label: "I am a reviewer"},
	)
	for _, st := range filterReviewStatuses {
		items = append(items, filterItem{kind: kindReviewStatus, label: reviewStatusLabel(st), value: st})
	}
	return append(items,
		filterItem{kind: kindApply, label: "Apply"},
		filterItem{kind: kindCancel, label: "Cancel"},
	)
}

// withTypes fills the type rows from a late catalog load, so a modal opened first does not stay on "Loading…".
func (f filterForm) withTypes(types []domain.IssueTypeSummary, loaded bool) filterForm {
	f.typesLoaded = loaded
	f.items = buildFilterItems(types)
	if f.focus >= len(f.items) {
		f.focus = len(f.items) - 1
	}
	f.hover = -1
	return f
}

func (f filterForm) Init() tea.Cmd { return nil }

func (f filterForm) Update(msg tea.Msg) (filterForm, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.MouseMotionMsg:
		return f.onHover(msg)
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f, nil
}

func (f filterForm) onKey(msg tea.KeyPressMsg) (filterForm, tea.Cmd) {
	f.hover = -1 // the keyboard is driving now, so drop any stale mouse-hover highlight
	switch msg.String() {
	case "esc":
		return f, cancelFilter
	case "tab", "down":
		return f.moveFocus(1), nil
	case "shift+tab", "up":
		return f.moveFocus(-1), nil
	case "enter", "space":
		return f.activate(f.focus)
	}
	return f, nil
}

func (f filterForm) onClick(msg tea.MouseClickMsg) (filterForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return f, nil
	}
	if i := f.hitZone(msg); i >= 0 {
		f.focus = i
		return f.activate(i)
	}
	return f, nil
}

func (f filterForm) onHover(msg tea.MouseMotionMsg) (filterForm, tea.Cmd) {
	f.hover = f.hitZone(msg)
	return f, nil
}

func (f filterForm) hitZone(msg tea.MouseMsg) int {
	for i := range f.items {
		if zone.Get(filterItemZone(i)).InBounds(msg) {
			return i
		}
	}
	return -1
}

func (f filterForm) moveFocus(delta int) filterForm {
	n := len(f.items)
	if n == 0 {
		return f
	}
	f.focus = (f.focus + delta + n) % n
	return f
}

// activate toggles a row or fires a button. The maps are shared by reference, so a toggle survives the value copy.
func (f filterForm) activate(i int) (filterForm, tea.Cmd) {
	if i < 0 || i >= len(f.items) {
		return f, nil
	}
	switch it := f.items[i]; it.kind {
	case kindState:
		f.states[it.value] = !f.states[it.value]
	case kindPriority:
		f.priorities[it.value] = !f.priorities[it.value]
	case kindType:
		f.types[it.id] = !f.types[it.id]
	case kindAssignee:
		f.assigneeMe = !f.assigneeMe
	case kindReviewer:
		f.reviewerMe = !f.reviewerMe
		if !f.reviewerMe {
			// with no reviewer the server silently ignores these, so do not leave them checked
			clear(f.reviewSel)
		}
	case kindReviewStatus:
		f.reviewSel[it.value] = !f.reviewSel[it.value]
		if f.reviewSel[it.value] {
			f.reviewerMe = true // a status on its own is inert server-side, so checking one implies the reviewer
		}
	case kindApply:
		return f, f.apply()
	case kindCancel:
		return f, cancelFilter
	}
	return f, nil
}

func (f filterForm) apply() tea.Cmd {
	msg := filterAppliedMsg{
		states:      selectedInOrder(filterStates, f.states),
		priorities:  selectedInOrder(filterPriorities, f.priorities),
		assigneeMe:  f.assigneeMe,
		reviewerMe:  f.reviewerMe,
		reviewerSts: selectedInOrder(filterReviewStatuses, f.reviewSel),
	}
	for _, it := range f.items {
		if it.kind == kindType && f.types[it.id] {
			msg.typeIDs = append(msg.typeIDs, it.id)
		}
	}
	return func() tea.Msg { return msg }
}

// selectedInOrder keeps the canonical display order, so an applied filter is toggle-order independent.
func selectedInOrder(all []string, sel map[string]bool) []string {
	var out []string
	for _, v := range all {
		if sel[v] {
			out = append(out, v)
		}
	}
	return out
}

func (f filterForm) indexOfKind(k filterKind) int {
	for i, it := range f.items {
		if it.kind == k {
			return i
		}
	}
	return -1
}

func (f filterForm) View() string {
	rows := []string{f.sectionHead("State")}
	rows = append(rows, f.rowsOfKind(kindState)...)
	rows = append(rows, "", f.sectionHead("Priority"))
	rows = append(rows, f.rowsOfKind(kindPriority)...)
	rows = append(rows, "", f.sectionHead("Type"))
	if typeRows := f.rowsOfKind(kindType); len(typeRows) > 0 {
		rows = append(rows, typeRows...)
	} else if !f.typesLoaded {
		rows = append(rows, f.deps.Styles.Muted.Render("Loading…"))
	} else {
		rows = append(rows, f.deps.Styles.Muted.Render("No issue types."))
	}
	if i := f.indexOfKind(kindAssignee); i >= 0 {
		rows = append(rows, "", f.checkRow(i, f.items[i].label, f.assigneeMe))
	}
	rows = append(rows, "", f.sectionHead("Review"))
	if i := f.indexOfKind(kindReviewer); i >= 0 {
		rows = append(rows, f.checkRow(i, f.items[i].label, f.reviewerMe))
	}
	rows = append(rows, f.rowsOfKind(kindReviewStatus)...)

	buttons := f.buttonGroup()
	w := lipgloss.Width(buttons)
	for _, r := range rows {
		if rw := lipgloss.Width(r); rw > w {
			w = rw
		}
	}
	rows = append(rows, "", lipgloss.PlaceHorizontal(w, lipgloss.Right, buttons))

	body := lipgloss.NewStyle().Padding(1, 1).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("Filters", body, f.deps.Styles.Theme.Primary)
}

func (f filterForm) sectionHead(label string) string {
	return lipgloss.NewStyle().Foreground(f.deps.Styles.Theme.Text).Bold(true).Render(label)
}

func (f filterForm) rowsOfKind(k filterKind) []string {
	var out []string
	for i, it := range f.items {
		if it.kind != k {
			continue
		}
		checked := false
		switch k {
		case kindState:
			checked = f.states[it.value]
		case kindPriority:
			checked = f.priorities[it.value]
		case kindType:
			checked = f.types[it.id]
		case kindReviewStatus:
			checked = f.reviewSel[it.value]
		}
		label := it.label
		if k == kindReviewStatus {
			label = "  " + label // indented: these narrow the reviewer row above rather than standing alone
		}
		out = append(out, f.checkRow(i, label, checked))
	}
	return out
}

func (f filterForm) checkRow(i int, label string, checked bool) string {
	t := f.deps.Styles.Theme
	g := f.deps.Glyphs
	box := "[ ]"
	if checked {
		box = "[" + lipgloss.NewStyle().Foreground(t.Success).Render(g.Or(g.Check, "x")) + "]"
	}
	col := t.Text
	switch {
	case f.focus == i:
		col = t.Accent
	case f.hover == i:
		col = t.Secondary
	}
	row := box + " " + lipgloss.NewStyle().Foreground(col).Render(label)
	return zone.Mark(filterItemZone(i), row)
}

func (f filterForm) buttonGroup() string {
	return lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Apply", f.indexOfKind(kindApply)),
		" ",
		f.button("Cancel", f.indexOfKind(kindCancel)),
	)
}

func (f filterForm) button(label string, i int) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case f.focus == i:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case f.hover == i:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(filterItemZone(i), components.TitledBoxWeighted("", body, borderCol, f.focus == i))
}

func (f filterForm) HelpKeys() []key.Binding {
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("space"), key.WithHelp("space", "toggle")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "apply")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

// stateLabel title-cases a state category (INITIAL -> Initial) for display.
func stateLabel(cat string) string {
	if cat == "" {
		return ""
	}
	return cat[:1] + strings.ToLower(cat[1:])
}

func filterItemZone(i int) string { return "project.filter." + strconv.Itoa(i) }

type filterAppliedMsg struct {
	states      []string
	priorities  []string
	typeIDs     []int64
	assigneeMe  bool
	reviewerMe  bool
	reviewerSts []string
}

// reviewStatusLabel matches how the detail panel labels a reviewer's state.
func reviewStatusLabel(status string) string {
	switch status {
	case "APPROVED":
		return "Approved"
	case "CHANGES_REQUESTED":
		return "Changes requested"
	default:
		return "Pending"
	}
}

type filterCancelledMsg struct{}

func cancelFilter() tea.Msg { return filterCancelledMsg{} }
