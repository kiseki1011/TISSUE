package schema

import (
	"context"
	"errors"
	"net/http"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
)

// Picker order. APPROVAL_REQUIRED is the only type with a parameter (min_approvals).
var guardTypes = []string{
	"ASSIGNEE_REQUIRED",
	"BLOCKING_ISSUE_RESOLVE_REQUIRED",
	"CHILD_ISSUES_RESOLVE_REQUIRED",
	"LINKED_BRANCH_REQUIRED",
	"APPROVAL_REQUIRED",
}

const guardApproval = "APPROVAL_REQUIRED"

func guardTypeLabel(t string) string {
	switch t {
	case "ASSIGNEE_REQUIRED":
		return "Assignee required"
	case "BLOCKING_ISSUE_RESOLVE_REQUIRED":
		return "No blocking issues"
	case "CHILD_ISSUES_RESOLVE_REQUIRED":
		return "Child issues resolved"
	case "LINKED_BRANCH_REQUIRED":
		return "Linked branch required"
	case guardApproval:
		return "Approval required"
	}
	return t
}

// guardRow is one editable guard. params is cloned from the backend so edits do not mutate the
// cached graph, and params other than min_approvals are preserved.
type guardRow struct {
	gtype  string
	params map[string]any
}

func (r guardRow) minApprovals() int { return minApprovalsOf(r.params) }

func (r *guardRow) setMinApprovals(n int) {
	if n < 1 {
		n = 1
	}
	if r.params == nil {
		r.params = map[string]any{}
	}
	r.params["min_approvals"] = n
}

func minApprovalsOf(params map[string]any) int {
	switch n := params["min_approvals"].(type) {
	case float64:
		return int(n)
	case int:
		return n
	case int32:
		return int(n)
	case int64:
		return int(n)
	}
	return 1
}

func cloneParams(m map[string]any) map[string]any {
	if len(m) == 0 {
		return nil
	}
	out := make(map[string]any, len(m))
	for k, v := range m {
		out[k] = v
	}
	return out
}

// guardsForm edits one transition's whole guard list, written back in a single PUT (the endpoint
// replaces all guards). A type may appear once, so the picker skips types used by another row.
type guardsForm struct {
	deps    deps.Deps
	wfID    int
	transID int
	transNm string

	rows       []guardRow
	focus      int // 0..len(rows)-1 rows, then Add, Save, Cancel
	spinner    spinner.Model
	status     string
	submitting bool

	pickOpen bool
	pick     picker // the dropdown, valid while pickOpen
	pickRow  int    // row whose type is being set, or -1 when adding a new guard
}

func newGuardsForm(d deps.Deps, wfID, transID int, transName string, guards []domain.WorkflowGuard) guardsForm {
	rows := make([]guardRow, 0, len(guards))
	for _, g := range guards {
		rows = append(rows, guardRow{gtype: g.Type, params: cloneParams(g.Params)})
	}
	return guardsForm{deps: d, wfID: wfID, transID: transID, transNm: transName, rows: rows, spinner: spinner.New(), focus: 0}
}

func (f guardsForm) Init() tea.Cmd { return nil }

func (f guardsForm) addIdx() int    { return len(f.rows) }
func (f guardsForm) saveIdx() int   { return len(f.rows) + 1 }
func (f guardsForm) cancelIdx() int { return len(f.rows) + 2 }
func (f guardsForm) lastIdx() int   { return len(f.rows) + 2 }
func (f guardsForm) onRow() bool    { return f.focus >= 0 && f.focus < len(f.rows) }

func (f guardsForm) Update(msg tea.Msg) (guardsForm, tea.Cmd) {
	switch msg := msg.(type) {
	case guardsFailedMsg:
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
	return f, nil
}

func (f guardsForm) onKey(msg tea.KeyPressMsg) (guardsForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	if f.pickOpen {
		return f.pickKey(msg), nil
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
	case "left", "h":
		if f.onRow() && f.rows[f.focus].gtype == guardApproval {
			f = f.cloneRows()
			f.rows[f.focus].setMinApprovals(f.rows[f.focus].minApprovals() - 1)
		}
	case "right", "l":
		if f.onRow() && f.rows[f.focus].gtype == guardApproval {
			f = f.cloneRows()
			f.rows[f.focus].setMinApprovals(f.rows[f.focus].minApprovals() + 1)
		}
	case "x", "delete", "backspace":
		if f.onRow() {
			return f.removeRow(f.focus), nil
		}
	case "enter", "space":
		switch {
		case f.onRow():
			return f.openTypePicker(f.focus), nil
		case f.focus == f.addIdx():
			return f.openAddPicker(), nil
		case f.focus == f.saveIdx():
			return f.submit()
		case f.focus == f.cancelIdx():
			return f, cancelGuards
		}
	}
	return f, nil
}

func (f guardsForm) pickKey(msg tea.KeyPressMsg) guardsForm {
	switch msg.String() {
	case "up", "k":
		f.pick = f.pick.move(-1)
	case "down", "j":
		f.pick = f.pick.move(1)
	case "enter", "space":
		return f.applyPick()
	case "esc":
		f.pickOpen = false
	}
	return f
}

// availableOptions are the guard types unused by other rows. except = that row's index, -1 when adding.
func (f guardsForm) availableOptions(except int) []pickerOption {
	used := f.usedTypes(except)
	var opts []pickerOption
	for _, t := range guardTypes {
		if !used[t] {
			opts = append(opts, pickerOption{value: t, label: guardTypeLabel(t)})
		}
	}
	return opts
}

func (f guardsForm) openAddPicker() guardsForm {
	opts := f.availableOptions(-1)
	if len(opts) == 0 {
		f.status = "All guard types are already added."
		return f
	}
	f.pick = newPicker("Add guard", opts, "", len(guardTypes), guardRowW)
	f.pickRow, f.pickOpen, f.status = -1, true, ""
	return f
}

func (f guardsForm) openTypePicker(i int) guardsForm {
	f.pick = newPicker("Guard type", f.availableOptions(i), f.rows[i].gtype, len(guardTypes), guardRowW)
	f.pickRow, f.pickOpen, f.status = i, true, ""
	return f
}

func (f guardsForm) applyPick() guardsForm {
	opt, ok := f.pick.selected()
	f.pickOpen = false
	if !ok {
		return f
	}
	f = f.cloneRows()
	if f.pickRow < 0 {
		row := guardRow{gtype: opt.value}
		if opt.value == guardApproval {
			row.setMinApprovals(1)
		}
		f.rows = append(f.rows, row)
		f.focus = len(f.rows) - 1
		return f
	}
	f.rows[f.pickRow].gtype = opt.value
	if opt.value == guardApproval && f.rows[f.pickRow].minApprovals() < 1 {
		f.rows[f.pickRow].setMinApprovals(1)
	}
	return f
}

// cloneRows copies the rows slice and its params so edits never reach another Model's cache.
func (f guardsForm) cloneRows() guardsForm {
	rows := make([]guardRow, len(f.rows))
	copy(rows, f.rows)
	for i := range rows {
		rows[i].params = cloneParams(rows[i].params)
	}
	f.rows = rows
	return f
}

func (f guardsForm) usedTypes(except int) map[string]bool {
	used := map[string]bool{}
	for i, r := range f.rows {
		if i != except {
			used[r.gtype] = true
		}
	}
	return used
}

func (f guardsForm) removeRow(i int) guardsForm {
	f = f.cloneRows()
	f.rows = append(f.rows[:i], f.rows[i+1:]...)
	if f.focus > len(f.rows) {
		f.focus = len(f.rows)
	}
	f.status = ""
	return f
}

func (f guardsForm) submit() (guardsForm, tea.Cmd) {
	if len(f.rows) == 0 {
		f.status = "Add at least one guard (clear all via the graph editor)."
		return f, nil
	}
	guards := make([]domain.GuardInput, len(f.rows))
	for i, r := range f.rows {
		g := domain.GuardInput{Type: r.gtype, Order: i + 1}
		if r.gtype == guardApproval {
			g.Params = map[string]any{"min_approvals": r.minApprovals()}
			for k, v := range r.params { // preserve advanced approval params we don't edit
				g.Params[k] = v
			}
			g.Params["min_approvals"] = r.minApprovals()
		}
		guards[i] = g
	}
	f.submitting = true
	f.status = ""
	return f, tea.Batch(saveGuards(f.deps, f.wfID, f.transID, guards), f.spinner.Tick)
}

func (f guardsForm) View() string {
	if f.pickOpen {
		return f.pick.View(f.deps.Styles)
	}
	t := f.deps.Styles.Theme
	rows := []string{f.deps.Styles.Muted.Render("Transition: ") + lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(f.transNm), ""}
	for i, r := range f.rows {
		rows = append(rows, f.rowLine(i, r))
	}
	rows = append(rows, f.addLine())
	switch {
	case f.submitting:
		rows = append(rows, "", lipgloss.NewStyle().Foreground(t.Warning).Render(f.spinner.View()+" Saving…"))
	case f.status != "":
		rows = append(rows, "", f.deps.Styles.Error.Render(f.status))
	}
	rows = append(rows, "", f.buttons(), "", f.hint())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("Guards", body, t.Primary)
}

const guardRowW = 42

func (f guardsForm) rowLine(i int, r guardRow) string {
	t := f.deps.Styles.Theme
	focused := f.focus == i
	marker, nameStyle := "  ", lipgloss.NewStyle().Foreground(t.Text)
	if focused {
		marker, nameStyle = lipgloss.NewStyle().Foreground(t.Accent).Render("▸ "), lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	}
	head := marker + nameStyle.Render(guardTypeLabel(r.gtype))
	tail := ""
	if r.gtype == guardApproval {
		num := f.deps.Styles.Muted.Render("min ")
		if focused {
			num += lipgloss.NewStyle().Foreground(t.Accent).Render("‹ ") + nameStyle.Render(itoa(r.minApprovals())) + lipgloss.NewStyle().Foreground(t.Accent).Render(" ›")
		} else {
			num += nameStyle.Render(itoa(r.minApprovals()))
		}
		tail = num
	}
	return zone.Mark(guardRowZone(i), alignRow(head, tail, guardRowW, lipgloss.NewStyle()))
}

func (f guardsForm) addLine() string {
	t := f.deps.Styles.Theme
	full := len(f.rows) >= len(guardTypes)
	style := lipgloss.NewStyle().Foreground(t.Muted)
	marker := "  "
	if f.focus == f.addIdx() {
		marker = lipgloss.NewStyle().Foreground(t.Accent).Render("▸ ")
		if !full {
			style = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		}
	}
	label := "+ Add guard"
	if full {
		label = "＋ (all types added)"
	}
	return zone.Mark("guards.add", marker+style.Render(label))
}

func (f guardsForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Save", "guards.save", f.focus == f.saveIdx()),
		" ",
		f.button("Cancel", "guards.cancel", f.focus == f.cancelIdx()),
	)
	return lipgloss.PlaceHorizontal(guardRowW+2, lipgloss.Right, group)
}

func (f guardsForm) button(label, id string, focused bool) string {
	t := f.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	if focused {
		borderCol, textCol, bold = t.Accent, t.Accent, true
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(id, components.TitledBoxWeighted("", body, borderCol, focused))
}

func (f guardsForm) hint() string {
	return hintBar(f.deps.Styles, "enter", "type", "←/→", "count", "x", "remove")
}

func guardRowZone(i int) string { return "guards.row." + itoa(i) }

func (f guardsForm) onClick(msg tea.MouseClickMsg) (guardsForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	if f.pickOpen {
		if i := f.pick.hitOption(msg); i >= 0 {
			f.pick.cursor = i
			return f.applyPick(), nil
		}
		return f, nil
	}
	for i := range f.rows {
		if zone.Get(guardRowZone(i)).InBounds(msg) {
			f.focus = i
			return f, nil
		}
	}
	switch {
	case zone.Get("guards.add").InBounds(msg):
		f.focus = f.addIdx()
		return f.openAddPicker(), nil
	case zone.Get("guards.save").InBounds(msg):
		f.focus = f.saveIdx()
		return f.submit()
	case zone.Get("guards.cancel").InBounds(msg):
		return f, cancelGuards
	}
	return f, nil
}

func (f guardsForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	if f.pickOpen {
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "back")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move"))}
	if f.onRow() {
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "type")))
		if f.rows[f.focus].gtype == guardApproval {
			binds = append(binds, key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "count")))
		}
		binds = append(binds, key.NewBinding(key.WithKeys("x"), key.WithHelp("x", "remove")))
	} else if f.focus == f.addIdx() {
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "add")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

func itoa(n int) string {
	if n == 0 {
		return "0"
	}
	neg := n < 0
	if neg {
		n = -n
	}
	var b []byte
	for n > 0 {
		b = append([]byte{byte('0' + n%10)}, b...)
		n /= 10
	}
	if neg {
		b = append([]byte{'-'}, b...)
	}
	return string(b)
}

type guardsSavedMsg struct{ wfID int }

type guardsFailedMsg struct{ message string }

type guardsCancelledMsg struct{}

func cancelGuards() tea.Msg { return guardsCancelledMsg{} }

func saveGuards(d deps.Deps, wfID, transID int, guards []domain.GuardInput) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.ConfigureTransitionGuards(context.Background(), wfID, transID, guards); err != nil {
			return guardsFailedMsg{message: guardsErrorMessage(err)}
		}
		return guardsSavedMsg{wfID: wfID}
	}
}

func guardsErrorMessage(err error) string {
	if m, ok := errmsg.Override(err); ok {
		return m // connectivity, or a leaky code mapped to friendlier copy
	}
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusBadRequest:
			return "Invalid guard configuration."
		case http.StatusConflict:
			return "Duplicate guard type."
		case http.StatusForbidden:
			return "You do not have permission to edit guards."
		}
	}
	if r := domain.ErrorReason(err); r != "" {
		return r // prefer the server's explanation over the generic line
	}
	return "Could not save guards. Try again."
}
