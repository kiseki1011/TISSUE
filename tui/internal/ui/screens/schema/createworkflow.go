package schema

import (
	"context"
	"errors"
	"image/color"
	"net/http"
	"sort"
	"strings"
	"unicode/utf8"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
)

const cwFieldW = 48

// cwStructW matches a metadata field box's outer width (border + one-cell inset per side) so the
// Structure rule and draft graph line up with the boxes above.
const cwStructW = cwFieldW + 4

// cwDefaultColor stands in for the removed color picker — the backend is dropping the field.
const cwDefaultColor = "INDIGO"

// createWorkflowItemKind identifies one focus stop in the create-workflow modal.
type createWorkflowItemKind int

const (
	cwName createWorkflowItemKind = iota
	cwDesc
	cwState
	cwTrans
	cwAddState
	cwAddTrans
	cwSave
	cwCancel
)

type cwItem struct {
	kind createWorkflowItemKind
	idx  int
}

// createWorkflowForm is the "New Workflow" modal: metadata plus the whole starting graph, created
// in a single POST. Edited like the in-place flow editor, but every node is new (temp keys).
type createWorkflowForm struct {
	deps deps.Deps

	name  textinput.Model
	desc  textinput.Model
	color string

	states []flowState
	trans  []flowTrans
	seq    int

	focus      int
	hover      string // action-bar button zone under the cursor, "" = none
	nameErr    string
	status     string
	submitting bool
	spinner    spinner.Model

	nodeOpen    bool
	node        nodeForm
	editingNode int

	edgeOpen    bool
	edge        edgeForm
	editingEdge int
}

// newCreateWorkflowForm seeds the two states every workflow needs: INITIAL "To Do", COMPLETED "Done".
func newCreateWorkflowForm(d deps.Deps) createWorkflowForm {
	n := textinput.New()
	n.Prompt = ""
	n.CharLimit = 32
	n.SetWidth(cwFieldW)

	ds := textinput.New()
	ds.Prompt = ""
	ds.CharLimit = 255
	ds.SetWidth(cwFieldW)

	f := createWorkflowForm{
		deps: d, name: n, desc: ds, color: cwDefaultColor,
		spinner: spinner.New(), editingNode: -1, editingEdge: -1,
		states: []flowState{
			{key: "n1", name: "To Do", category: "INITIAL", color: "ANSI_BLUE"},
			{key: "n2", name: "Done", category: "COMPLETED", color: "ANSI_GREEN"},
		},
		seq: 2,
	}
	f.name.Focus()
	return f
}

func (f createWorkflowForm) Init() tea.Cmd { return textinput.Blink }

// items is the flat focus order, with the command affordances contiguous at the tail.
func (f createWorkflowForm) items() []cwItem {
	items := []cwItem{{cwName, 0}, {cwDesc, 0}}
	for i := range f.states {
		items = append(items, cwItem{cwState, i})
	}
	for i := range f.trans {
		items = append(items, cwItem{cwTrans, i})
	}
	return append(items, cwItem{cwAddState, 0}, cwItem{cwAddTrans, 0}, cwItem{cwSave, 0}, cwItem{cwCancel, 0})
}

func (f createWorkflowForm) cur() cwItem {
	items := f.items()
	if f.focus < 0 || f.focus >= len(items) {
		return cwItem{cwCancel, 0}
	}
	return items[f.focus]
}

func (f createWorkflowForm) indexOfKind(kind createWorkflowItemKind) int {
	for i, it := range f.items() {
		if it.kind == kind {
			return i
		}
	}
	return 0
}

func (f createWorkflowForm) addStateFocus() int { return f.indexOfKind(cwAddState) }
func (f createWorkflowForm) addTransFocus() int { return f.indexOfKind(cwAddTrans) }
func (f createWorkflowForm) saveFocus() int     { return f.indexOfKind(cwSave) }
func (f createWorkflowForm) cancelFocus() int   { return f.indexOfKind(cwCancel) }

// navOrder sorts graph elements by drawn row so navigation reads down the diagram rather than in raw
// draft order. Metadata first, commands last, mirroring the in-place flow editor.
func (f createWorkflowForm) navOrder() []int {
	items := f.items()
	_, rows, _ := renderWorkflowGraph(f.draftDetail(), f.deps.Styles, navRefWidth, wfElem{}, wfElem{}, false)
	elemOf := func(idx int) wfElem {
		if it := items[idx]; it.kind == cwState {
			return wfElem{elemState, it.idx + 1}
		}
		return wfElem{elemTransition, items[idx].idx + 1}
	}
	meta := make([]int, 0, 2)
	graph := make([]int, 0, len(items))
	cmds := make([]int, 0, len(items))
	for i, it := range items {
		switch it.kind {
		case cwState, cwTrans:
			graph = append(graph, i)
		case cwName, cwDesc:
			meta = append(meta, i)
		default:
			cmds = append(cmds, i)
		}
	}
	sort.SliceStable(graph, func(a, b int) bool { return rows[elemOf(graph[a])] < rows[elemOf(graph[b])] })
	order := make([]int, 0, len(items))
	order = append(order, meta...)
	order = append(order, graph...)
	return append(order, cmds...)
}

// selElem maps the focused item to a diagram element via the flow editor's synthetic ids (index+1).
func (f createWorkflowForm) selElem() (wfElem, bool) {
	switch it := f.cur(); it.kind {
	case cwState:
		return wfElem{elemState, it.idx + 1}, true
	case cwTrans:
		return wfElem{elemTransition, it.idx + 1}, true
	}
	return wfElem{}, false
}

func (f createWorkflowForm) clampFocus() createWorkflowForm {
	if n := len(f.items()); f.focus >= n {
		f.focus = n - 1
	}
	if f.focus < 0 {
		f.focus = 0
	}
	return f
}

func (f createWorkflowForm) hasOverlay() bool { return f.nodeOpen || f.edgeOpen }

func (f createWorkflowForm) Update(msg tea.Msg) (createWorkflowForm, tea.Cmd) {
	switch msg := msg.(type) {
	case createWorkflowFailedMsg:
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
	}
	if f.nodeOpen {
		return f.updateNode(msg)
	}
	if f.edgeOpen {
		return f.updateEdge(msg)
	}
	switch msg := msg.(type) {
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.MouseMotionMsg:
		f.hover = f.hitButton(msg)
		return f, nil
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f.updateInputs(msg)
}

func (f createWorkflowForm) hitButton(msg tea.MouseMsg) string {
	for _, id := range []string{"cw.addstate", "cw.addtrans", "cw.save", "cw.cancel"} {
		if zone.Get(id).InBounds(msg) {
			return id
		}
	}
	return ""
}

func (f createWorkflowForm) updateNode(msg tea.Msg) (createWorkflowForm, tea.Cmd) {
	var cmd tea.Cmd
	f.node, cmd = f.node.Update(msg)
	switch {
	case f.node.done:
		name, cat, col := f.node.result()
		f = f.applyNode(name, cat, col)
		f.nodeOpen = false
	case f.node.cancelled:
		f.nodeOpen = false
	}
	return f, cmd
}

func (f createWorkflowForm) updateEdge(msg tea.Msg) (createWorkflowForm, tea.Cmd) {
	var cmd tea.Cmd
	f.edge, cmd = f.edge.Update(msg)
	switch {
	case f.edge.done:
		name, src, tgt := f.edge.result()
		f = f.applyEdge(name, src, tgt)
		f.edgeOpen = false
	case f.edge.cancelled:
		f.edgeOpen = false
	}
	return f, cmd
}

func (f createWorkflowForm) updateInputs(msg tea.Msg) (createWorkflowForm, tea.Cmd) {
	var nc, dc tea.Cmd
	f.name, nc = f.name.Update(msg)
	f.desc, dc = f.desc.Update(msg)
	return f, tea.Batch(nc, dc)
}

func (f createWorkflowForm) onKey(msg tea.KeyPressMsg) (createWorkflowForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	f.hover = "" // the keyboard is driving — drop any stale mouse hover
	switch msg.String() {
	case "tab", "down":
		return f.move(1)
	case "shift+tab", "up":
		return f.move(-1)
	case "ctrl+s":
		return f.submit()
	}
	switch it := f.cur(); it.kind {
	case cwName, cwDesc:
		return f.typeInto(msg)
	case cwState:
		return f.stateKey(msg, it.idx)
	case cwTrans:
		return f.transKey(msg, it.idx)
	case cwAddState:
		switch msg.String() {
		case "enter", "space", "a":
			return f.openAddState(), nil
		case "t":
			return f.openAddTrans(""), nil
		}
	case cwAddTrans:
		switch msg.String() {
		case "enter", "space", "t":
			return f.openAddTrans(""), nil
		case "a":
			return f.openAddState(), nil
		}
	case cwSave:
		if msg.String() == "enter" || msg.String() == "space" {
			return f.submit()
		}
	case cwCancel:
		if msg.String() == "enter" || msg.String() == "space" {
			return f, cancelCreateWorkflow
		}
	}
	return f, nil
}

func (f createWorkflowForm) stateKey(msg tea.KeyPressMsg, idx int) (createWorkflowForm, tea.Cmd) {
	switch msg.String() {
	case "left", "h":
		f.states[idx].category = cycle(stateCategories, f.states[idx].category, -1)
		f = f.enforceInitial(idx)
	case "right", "l":
		f.states[idx].category = cycle(stateCategories, f.states[idx].category, 1)
		f = f.enforceInitial(idx)
	case "x", "delete", "backspace":
		return f.deleteState(idx), nil
	case "enter", "space":
		st := f.states[idx]
		f.node, f.editingNode, f.nodeOpen = newNodeForm(f.deps, "Edit state", st.name, st.category, st.color), idx, true
	case "a":
		return f.openAddState(), nil
	case "t":
		return f.openAddTrans(f.states[idx].key), nil
	}
	return f, nil
}

func (f createWorkflowForm) transKey(msg tea.KeyPressMsg, idx int) (createWorkflowForm, tea.Cmd) {
	switch msg.String() {
	case "enter", "space":
		tr := f.trans[idx]
		f.edge, f.editingEdge, f.edgeOpen = newEdgeForm(f.deps, "Rewire transition", true, tr.name, tr.src, tr.tgt, f.states), idx, true
	case "x", "delete", "backspace":
		f.trans = append(f.trans[:idx], f.trans[idx+1:]...)
		f = f.clampFocus()
	case "a":
		return f.openAddState(), nil
	case "t":
		return f.openAddTrans(""), nil
	}
	return f, nil
}

func (f createWorkflowForm) openAddState() createWorkflowForm {
	f.node, f.editingNode, f.nodeOpen = newNodeForm(f.deps, "Add state", "", "ACTIVE", ""), -1, true
	return f
}

func (f createWorkflowForm) openAddTrans(src string) createWorkflowForm {
	f.edge, f.editingEdge, f.edgeOpen = newEdgeForm(f.deps, "Add transition", true, "", src, "", f.states), -1, true
	return f
}

// move walks navOrder, so arrows and tab read down the diagram as the flow editor does.
func (f createWorkflowForm) move(delta int) (createWorkflowForm, tea.Cmd) {
	order := f.navOrder()
	if len(order) == 0 {
		return f, nil
	}
	pos := indexOfInt(order, f.focus)
	if pos < 0 {
		return f.focusOn(order[0])
	}
	return f.focusOn(order[(pos+delta+len(order))%len(order)])
}

func (f createWorkflowForm) focusOn(i int) (createWorkflowForm, tea.Cmd) {
	f.focus = i
	f.name.Blur()
	f.desc.Blur()
	var cmd tea.Cmd
	switch f.cur().kind {
	case cwName:
		cmd = f.name.Focus()
	case cwDesc:
		cmd = f.desc.Focus()
	}
	return f, cmd
}

func (f createWorkflowForm) typeInto(msg tea.KeyPressMsg) (createWorkflowForm, tea.Cmd) {
	if msg.String() == "enter" {
		return f.move(1)
	}
	f.status = ""
	var cmd tea.Cmd
	if f.cur().kind == cwName {
		f.nameErr = ""
		f.name, cmd = f.name.Update(msg)
	} else {
		f.desc, cmd = f.desc.Update(msg)
	}
	return f, cmd
}

func (f createWorkflowForm) applyNode(name, category, colorName string) createWorkflowForm {
	if f.editingNode >= 0 && f.editingNode < len(f.states) {
		f.states[f.editingNode].name = name
		f.states[f.editingNode].category = category
		f.states[f.editingNode].color = colorName
		return f.enforceInitial(f.editingNode)
	}
	f.seq++
	f.states = append(f.states, flowState{key: "n" + itoa(f.seq), name: name, category: category, color: colorName})
	f.focus = f.indexOfState(len(f.states) - 1)
	return f.enforceInitial(len(f.states) - 1)
}

func (f createWorkflowForm) applyEdge(name, src, tgt string) createWorkflowForm {
	if f.editingEdge >= 0 && f.editingEdge < len(f.trans) {
		f.trans[f.editingEdge].name = name
		f.trans[f.editingEdge].src, f.trans[f.editingEdge].tgt = src, tgt
		return f
	}
	f.seq++
	f.trans = append(f.trans, flowTrans{key: "e" + itoa(f.seq), name: name, src: src, tgt: tgt})
	f.focus = f.indexOfTrans(len(f.trans) - 1)
	return f
}

func (f createWorkflowForm) indexOfState(i int) int {
	for j, it := range f.items() {
		if it.kind == cwState && it.idx == i {
			return j
		}
	}
	return f.focus
}

func (f createWorkflowForm) indexOfTrans(i int) int {
	for j, it := range f.items() {
		if it.kind == cwTrans && it.idx == i {
			return j
		}
	}
	return f.focus
}

// enforceInitial keeps exactly one INITIAL: every other one falls back to ACTIVE.
func (f createWorkflowForm) enforceInitial(i int) createWorkflowForm {
	if i < 0 || i >= len(f.states) || f.states[i].category != "INITIAL" {
		return f
	}
	for j := range f.states {
		if j != i && f.states[j].category == "INITIAL" {
			f.states[j].category = "ACTIVE"
		}
	}
	return f
}

// deleteState removes a state and every transition touching it.
func (f createWorkflowForm) deleteState(idx int) createWorkflowForm {
	key := f.states[idx].key
	f.states = append(f.states[:idx], f.states[idx+1:]...)
	kept := make([]flowTrans, 0, len(f.trans))
	for _, tr := range f.trans {
		if tr.src != key && tr.tgt != key {
			kept = append(kept, tr)
		}
	}
	f.trans = kept
	f.status = ""
	return f.clampFocus()
}

// draftDetail renders the working copy into a throwaway WorkflowDetail for the graph renderer.
func (f createWorkflowForm) draftDetail() domain.WorkflowDetail {
	ids := make(map[string]int, len(f.states))
	var d domain.WorkflowDetail
	for i, st := range f.states {
		sid := i + 1
		ids[st.key] = sid
		d.States = append(d.States, domain.WorkflowState{ID: sid, Label: st.name, Category: st.category, Color: st.color})
		if st.category == "INITIAL" {
			d.InitialStateID = sid
		}
	}
	for i, tr := range f.trans {
		d.Transitions = append(d.Transitions, domain.WorkflowTransition{ID: i + 1, Label: tr.name, SourceID: ids[tr.src], TargetID: ids[tr.tgt]})
	}
	return d
}

func (f createWorkflowForm) validate() string {
	name := strings.TrimSpace(f.name.Value())
	switch n := utf8.RuneCountInString(name); {
	case n < 2:
		return "Name needs at least 2 characters."
	case n > 32:
		return "Name is at most 32 characters."
	}
	if len(f.states) == 0 {
		return "Add at least one state."
	}
	// state/transition names share the workflow's 2–32 bound. Check here so a bad name gets an
	// accurate message instead of the backend's generic graph rejection.
	for _, st := range f.states {
		if n := utf8.RuneCountInString(strings.TrimSpace(st.name)); n < 2 || n > 32 {
			return "State names must be 2–32 characters."
		}
	}
	for _, tr := range f.trans {
		if n := utf8.RuneCountInString(strings.TrimSpace(tr.name)); n < 2 || n > 32 {
			return "Transition names must be 2–32 characters."
		}
	}
	initial, completed := 0, 0
	for _, st := range f.states {
		switch st.category {
		case "INITIAL":
			initial++
		case "COMPLETED":
			completed++
		}
	}
	switch {
	case initial != 1:
		return "Exactly one initial state is required."
	case completed == 0:
		return "At least one completed state is required."
	case len(f.trans) == 0:
		return "Add at least one transition."
	}
	return ""
}

func (f createWorkflowForm) buildInputs() ([]domain.WorkflowStateCreate, []domain.WorkflowTransitionCreate) {
	states := make([]domain.WorkflowStateCreate, 0, len(f.states))
	for _, st := range f.states {
		states = append(states, domain.WorkflowStateCreate{TempKey: st.key, Name: st.name, Color: st.color, Category: st.category})
	}
	trans := make([]domain.WorkflowTransitionCreate, 0, len(f.trans))
	for _, tr := range f.trans {
		trans = append(trans, domain.WorkflowTransitionCreate{Name: tr.name, SourceTempKey: tr.src, TargetTempKey: tr.tgt})
	}
	return states, trans
}

func (f createWorkflowForm) submit() (createWorkflowForm, tea.Cmd) {
	if msg := f.validate(); msg != "" {
		f.status = msg
		if strings.HasPrefix(msg, "Name") {
			f.nameErr = msg
			return f.focusOn(f.indexOfKind(cwName))
		}
		return f, nil
	}
	states, trans := f.buildInputs()
	f.submitting, f.status = true, ""
	return f, tea.Batch(
		createWorkflow(f.deps, strings.TrimSpace(f.name.Value()), f.color, strings.TrimSpace(f.desc.Value()), states, trans),
		f.spinner.Tick,
	)
}

func (f createWorkflowForm) View() string {
	if f.nodeOpen {
		return f.node.View()
	}
	if f.edgeOpen {
		return f.edge.View()
	}
	body := lipgloss.NewStyle().Padding(1, 1).Render(f.body())
	return components.TitledBoxCentered("New Workflow", body, f.deps.Styles.Theme.Primary)
}

// FocusRow reports the focused item's row and height so a windowed modal can scroll it into view.
// chromeTop 2 = top border + the padding row. ok=false while a sub-form is open.
func (f createWorkflowForm) FocusRow() (int, int, bool) {
	if f.nodeOpen || f.edgeOpen {
		return 0, 0, false
	}
	s := f.deps.Styles
	const chromeTop = 2
	nameRow := f.field(cwName, "Name", fixCw(f.name.View(), 1), f.nameErr)
	descRow := f.field(cwDesc, "Description", fixCw(f.desc.View(), 1), "")
	nameH, descH := lipgloss.Height(nameRow), lipgloss.Height(descRow)
	rule := sectionRule(s, "Structure", cwStructW)
	// rows body() lays down before the action bar — measured off the same pieces, not counted
	beforeAction := lipgloss.Height(lipgloss.JoinVertical(lipgloss.Left, nameRow, descRow, "", rule, ""))

	switch it := f.cur(); it.kind {
	case cwName:
		return chromeTop, nameH, true
	case cwDesc:
		return chromeTop + nameH, descH, true
	case cwAddState, cwAddTrans, cwSave, cwCancel:
		return chromeTop + beforeAction, 1, true // the action bar is a single line
	case cwState, cwTrans:
		// everything body() stacks before the draft graph
		pre := []string{nameRow, descRow, "", rule, "", f.actionBar(), "", trunc(f.hint(), cwStructW), ""}
		pre = append(pre, f.statusLines()...)
		pre = append(pre, "")
		beforeGraph := lipgloss.Height(lipgloss.JoinVertical(lipgloss.Left, pre...))
		if sel, ok := f.selElem(); ok {
			_, grows, ghits := renderWorkflowGraph(f.draftDetail(), s, cwStructW, sel, wfElem{}, false)
			// the bounding rect covers a state's whole box and a wrapped transition label, not just the top row
			if rect, ok := ghits[sel]; ok {
				return chromeTop + beforeGraph + rect.r0, rect.r1 - rect.r0 + 1, true
			}
			if gr, ok := grows[sel]; ok {
				h := 1
				if sel.kind == elemState {
					h = 3
				}
				return chromeTop + beforeGraph + gr, h, true
			}
		}
		return chromeTop + beforeAction, 1, true // fallback: keep the action bar in view
	}
	return chromeTop, 1, true
}

func (f createWorkflowForm) body() string {
	s := f.deps.Styles
	rows := []string{
		f.field(cwName, "Name", fixCw(f.name.View(), 1), f.nameErr),
		f.field(cwDesc, "Description", fixCw(f.desc.View(), 1), ""),
		"",
		sectionRule(s, "Structure", cwStructW),
		"",
		f.actionBar(),
		"",
		trunc(f.hint(), cwStructW),
		"",
	}
	rows = append(rows, f.statusLines()...)
	rows = append(rows, "")
	sel, _ := f.selElem()
	g, _, _ := renderWorkflowGraph(f.draftDetail(), s, cwStructW, sel, wfElem{}, false)
	rows = append(rows, g...)
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// statusLines always occupies at least one line so the graph below keeps a stable position.
func (f createWorkflowForm) statusLines() []string {
	s := f.deps.Styles
	switch {
	case f.submitting:
		return []string{lipgloss.NewStyle().Foreground(s.Theme.Warning).Render(f.spinner.View() + " Creating…")}
	case f.status != "":
		return strings.Split(s.Error.Width(cwStructW).Render(f.status), "\n")
	}
	return []string{""}
}

// actionBar mirrors the in-place flow editor's one-line command row.
func (f createWorkflowForm) actionBar() string {
	t := f.deps.Styles.Theme
	return f.affordance("cw.addstate", "+ State", f.cur().kind == cwAddState, t.Secondary) +
		"   " + f.affordance("cw.addtrans", "+ Transition", f.cur().kind == cwAddTrans, t.Secondary) +
		"   " + f.affordance("cw.save", "Save", f.cur().kind == cwSave, t.Success) +
		"   " + f.affordance("cw.cancel", "Cancel", f.cur().kind == cwCancel, t.Error)
}

func (f createWorkflowForm) affordance(id, label string, focused bool, base color.Color) string {
	st := affordanceStyle(f.deps.Styles.Theme, base, focused, f.hover == id)
	return zone.Mark(id, st.Render(label))
}

func (f createWorkflowForm) hint() string {
	s := f.deps.Styles
	switch f.cur().kind {
	case cwState:
		return hintBar(s, "←/→", "category", "x", "delete", "a", "add state", "t", "from here")
	case cwTrans:
		return hintBar(s, "enter", "rewire", "x", "delete")
	case cwAddState, cwAddTrans:
		return hintBar(s, "enter", "add", "esc", "cancel")
	case cwSave, cwCancel:
		return hintBar(s, "enter", "select", "esc", "cancel")
	}
	return hintBar(s, "tab", "next", "a", "add state", "t", "add transition")
}

func (f createWorkflowForm) field(kind createWorkflowItemKind, label, content, errMsg string) string {
	box := components.TitledBoxWeighted(label, content, f.fieldBorderColor(kind, errMsg), f.cur().kind == kind)
	box = zone.Mark(cwFieldZone(kind), box)
	if kind == cwName {
		errLine := lipgloss.NewStyle().Padding(0, 1).Render(f.errText(errMsg))
		return lipgloss.JoinVertical(lipgloss.Left, box, errLine)
	}
	return box
}

func (f createWorkflowForm) fieldBorderColor(kind createWorkflowItemKind, errMsg string) color.Color {
	t := f.deps.Styles.Theme
	switch {
	case errMsg != "":
		return t.Error
	case f.cur().kind == kind:
		return t.Accent
	default:
		return t.Primary
	}
}

func (f createWorkflowForm) errText(msg string) string {
	if msg == "" {
		return " "
	}
	return f.deps.Styles.Error.Render(msg)
}

func (f createWorkflowForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	if f.nodeOpen {
		return f.node.HelpKeys()
	}
	if f.edgeOpen {
		return f.edge.HelpKeys()
	}
	binds := []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("a"), key.WithHelp("a", "add state")),
		key.NewBinding(key.WithKeys("t"), key.WithHelp("t", "add transition")),
		key.NewBinding(key.WithKeys("ctrl+s"), key.WithHelp("ctrl+s", "save")),
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

func fixCw(s string, h int) string {
	return lipgloss.NewStyle().Width(cwFieldW).MaxWidth(cwFieldW).Height(h).MaxHeight(h).Render(s)
}

func cwFieldZone(kind createWorkflowItemKind) string {
	switch kind {
	case cwName:
		return "cw.name"
	case cwDesc:
		return "cw.desc"
	}
	return ""
}

func (f createWorkflowForm) onClick(msg tea.MouseClickMsg) (createWorkflowForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	switch {
	case zone.Get("cw.name").InBounds(msg):
		return f.focusOn(f.indexOfKind(cwName))
	case zone.Get("cw.desc").InBounds(msg):
		return f.focusOn(f.indexOfKind(cwDesc))
	case zone.Get("cw.addstate").InBounds(msg):
		f, _ = f.focusOn(f.addStateFocus())
		return f.openAddState(), nil
	case zone.Get("cw.addtrans").InBounds(msg):
		f, _ = f.focusOn(f.addTransFocus())
		return f.openAddTrans(""), nil
	case zone.Get("cw.save").InBounds(msg):
		f, _ = f.focusOn(f.saveFocus())
		return f.submit()
	case zone.Get("cw.cancel").InBounds(msg):
		return f, cancelCreateWorkflow
	}
	return f, nil
}

type workflowCreatedMsg struct{}

type createWorkflowFailedMsg struct{ message string }

type createWorkflowCancelledMsg struct{}

func cancelCreateWorkflow() tea.Msg { return createWorkflowCancelledMsg{} }

func createWorkflow(d deps.Deps, name, colorName, desc string, states []domain.WorkflowStateCreate, trans []domain.WorkflowTransitionCreate) tea.Cmd {
	return func() tea.Msg {
		if _, err := d.Catalog.CreateWorkflow(context.Background(), name, colorName, desc, states, trans); err != nil {
			return createWorkflowFailedMsg{message: createWorkflowErrorMessage(err)}
		}
		return workflowCreatedMsg{}
	}
}

func createWorkflowErrorMessage(err error) string {
	if m, ok := errmsg.Override(err); ok {
		return m // connectivity, or a leaky code mapped to friendlier copy
	}
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "That workflow, state, or transition name is already taken."
		case http.StatusBadRequest:
			return "Invalid graph. Needs one initial and one completed state, every state reachable, and no dead ends."
		case http.StatusForbidden:
			return "You do not have permission to create workflows."
		}
	}
	if r := domain.ErrorReason(err); r != "" {
		return r // prefer the server's own explanation
	}
	return "Could not create the workflow. Try again."
}
