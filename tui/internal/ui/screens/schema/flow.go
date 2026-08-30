package schema

import (
	"context"
	"errors"
	"image/color"
	"net/http"
	"sort"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
)

// navRefWidth is arbitrary: the vertical layout is width-independent, so nav order is unaffected.
const navRefWidth = 80

// flowState is one state in the working copy. key is a stable local handle ("s<id>" existing,
// "n<seq>" new) that transitions reference, so wiring survives reordering.
type flowState struct {
	id       int
	key      string
	name     string
	category string
	color    string
}

// flowTrans is one transition in the working copy. src/tgt hold flowState keys. guards is carried
// unedited so the preview can show it — a whole-graph replace preserves it server-side.
type flowTrans struct {
	id     int
	key    string
	name   string
	src    string
	tgt    string
	guards []domain.WorkflowGuard
}

// flowForm edits a copy of the whole graph and serializes it into one optimistic-locked replace.
type flowForm struct {
	deps    deps.Deps
	wfID    int
	version int

	states []flowState
	trans  []flowTrans
	seq    int

	focus      int
	hover      string // action-bar button zone under the cursor, "" = none
	status     string
	submitting bool
	spinner    spinner.Model

	nodeOpen    bool
	node        nodeForm
	editingNode int // index of the state row being edited, -1 when adding

	edgeOpen    bool
	edge        edgeForm
	editingEdge int // index of the transition row being edited, -1 when adding
}

func newFlowForm(d deps.Deps, wf domain.WorkflowDetail) flowForm {
	f := flowForm{deps: d, wfID: wf.ID, version: wf.Version, spinner: spinner.New(), editingNode: -1, editingEdge: -1}
	for _, st := range wf.States {
		f.states = append(f.states, flowState{id: st.ID, key: "s" + itoa(st.ID), name: st.Label, category: st.Category, color: st.Color})
	}
	for _, tr := range wf.Transitions {
		f.trans = append(f.trans, flowTrans{id: tr.ID, key: "t" + itoa(tr.ID), name: tr.Label, src: "s" + itoa(tr.SourceID), tgt: "s" + itoa(tr.TargetID), guards: tr.Guards})
	}
	return f
}

func (f flowForm) Init() tea.Cmd { return nil }

type flowItemKind int

const (
	fiState flowItemKind = iota
	fiAddState
	fiTrans
	fiAddTrans
	fiSave
	fiCancel
)

type flowItem struct {
	kind flowItemKind
	idx  int
}

// items is the flat focus order: states, transitions, then commands. The commands stay contiguous
// at the tail so navigating into one scrolls to the action bar instead of jumping mid-graph.
func (f flowForm) items() []flowItem {
	items := make([]flowItem, 0, len(f.states)+len(f.trans)+5)
	for i := range f.states {
		items = append(items, flowItem{fiState, i})
	}
	for i := range f.trans {
		items = append(items, flowItem{fiTrans, i})
	}
	return append(items, flowItem{fiAddState, 0}, flowItem{fiAddTrans, 0}, flowItem{fiSave, 0}, flowItem{fiCancel, 0})
}

func (f flowForm) cur() flowItem {
	items := f.items()
	if f.focus < 0 || f.focus >= len(items) {
		return flowItem{fiCancel, 0}
	}
	return items[f.focus]
}

func (f flowForm) stateFocus(i int) int { return i }
func (f flowForm) transFocus(i int) int { return len(f.states) + i }
func (f flowForm) addStateFocus() int   { return len(f.states) + len(f.trans) }
func (f flowForm) addTransFocus() int   { return len(f.states) + len(f.trans) + 1 }
func (f flowForm) saveFocus() int       { return len(f.states) + len(f.trans) + 2 }

// selElem maps the focused item to a draft element, matching draftDetail's index+1 numbering.
func (f flowForm) selElem() (wfElem, bool) {
	switch it := f.cur(); it.kind {
	case fiState:
		return wfElem{elemState, it.idx + 1}, true
	case fiTrans:
		return wfElem{elemTransition, it.idx + 1}, true
	default: // the buttons are not diagram elements
		return wfElem{}, false
	}
}

// focusForElem is the inverse of selElem: a clicked draft element back to its focus index.
func (f flowForm) focusForElem(e wfElem) (int, bool) {
	switch e.kind {
	case elemState:
		if i := e.id - 1; i >= 0 && i < len(f.states) {
			return f.stateFocus(i), true
		}
	case elemTransition:
		if i := e.id - 1; i >= 0 && i < len(f.trans) {
			return f.transFocus(i), true
		}
	}
	return 0, false
}

// navOrder is visual top-to-bottom order, so ↑/↓ read down the graph while focus stays draft-ordered.
func (f flowForm) navOrder() []int {
	items := f.items()
	_, rows, _ := renderWorkflowGraph(f.draftDetail(), f.deps.Styles, navRefWidth, wfElem{}, wfElem{}, false)
	elemOf := func(idx int) wfElem {
		if it := items[idx]; it.kind == fiState {
			return wfElem{elemState, it.idx + 1}
		}
		return wfElem{elemTransition, items[idx].idx + 1}
	}
	graph := make([]int, 0, len(items))
	cmds := make([]int, 0, len(items))
	for i, it := range items {
		if it.kind == fiState || it.kind == fiTrans {
			graph = append(graph, i)
		} else {
			cmds = append(cmds, i)
		}
	}
	sort.SliceStable(graph, func(a, b int) bool { return rows[elemOf(graph[a])] < rows[elemOf(graph[b])] })
	return append(graph, cmds...)
}

// firstStateFocus anchors the editor at the topmost drawn state, not the first in draft order.
func (f flowForm) firstStateFocus() int {
	for _, idx := range f.navOrder() {
		if f.items()[idx].kind == fiState {
			return idx
		}
	}
	return 0
}

// navStep moves the focus delta positions along the visual nav order, wrapping at both ends.
func (f flowForm) navStep(delta int) int {
	order := f.navOrder()
	pos := indexOfInt(order, f.focus)
	if pos < 0 {
		return f.focus
	}
	return order[(pos+delta+len(order))%len(order)]
}

// hasOverlay tells the screen to composite View() as a centered modal over the Details panel.
func (f flowForm) hasOverlay() bool { return f.nodeOpen || f.edgeOpen }

func (f flowForm) clampFocus() flowForm {
	if n := len(f.items()); f.focus >= n {
		f.focus = n - 1
	}
	if f.focus < 0 {
		f.focus = 0
	}
	return f
}

func (f flowForm) Update(msg tea.Msg) (flowForm, tea.Cmd) {
	switch msg := msg.(type) {
	case flowFailedMsg:
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
	return f, nil
}

func (f flowForm) hitButton(msg tea.MouseMsg) string {
	for _, id := range []string{"flow.addstate", "flow.addtrans", "flow.save", "flow.cancel"} {
		if zone.Get(id).InBounds(msg) {
			return id
		}
	}
	return ""
}

func (f flowForm) updateNode(msg tea.Msg) (flowForm, tea.Cmd) {
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

func (f flowForm) updateEdge(msg tea.Msg) (flowForm, tea.Cmd) {
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

func (f flowForm) onKey(msg tea.KeyPressMsg) (flowForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	f.hover = "" // the keyboard is driving — drop any stale mouse hover
	switch msg.String() {
	case "up", "k":
		f.focus = f.navStep(-1)
	case "down", "j":
		f.focus = f.navStep(1)
	case "tab":
		f.focus = f.navStep(1)
	case "shift+tab":
		f.focus = f.navStep(-1)
	case "left", "h":
		if it := f.cur(); it.kind == fiState {
			f.states[it.idx].category = cycle(stateCategories, f.states[it.idx].category, -1)
			f = f.enforceInitial(it.idx)
		}
	case "right", "l":
		if it := f.cur(); it.kind == fiState {
			f.states[it.idx].category = cycle(stateCategories, f.states[it.idx].category, 1)
			f = f.enforceInitial(it.idx)
		}
	case "a":
		f.node, f.editingNode, f.nodeOpen = newNodeForm(f.deps, "Add state", "", "ACTIVE", ""), -1, true
		return f, nil
	case "t":
		// a focused state pre-fills as the source, so "add from here" needs only a target
		src := ""
		if it := f.cur(); it.kind == fiState {
			src = f.states[it.idx].key
		}
		f.edge, f.editingEdge, f.edgeOpen = newEdgeForm(f.deps, "Add transition", true, "", src, "", f.states), -1, true
		return f, nil
	case "x", "delete", "backspace":
		return f.deleteCur(), nil
	case "ctrl+s":
		return f.submit()
	case "enter", "space":
		return f.activateCur()
	}
	return f, nil
}

func (f flowForm) activateCur() (flowForm, tea.Cmd) {
	switch it := f.cur(); it.kind {
	case fiState:
		st := f.states[it.idx]
		if st.id != 0 {
			return f, nil // existing state: recategorize inline, rename via the per-element editor
		}
		f.node, f.editingNode, f.nodeOpen = newNodeForm(f.deps, "Edit state", st.name, st.category, st.color), it.idx, true
	case fiAddState:
		f.node, f.editingNode, f.nodeOpen = newNodeForm(f.deps, "Add state", "", "ACTIVE", ""), -1, true
	case fiTrans:
		tr := f.trans[it.idx]
		f.edge, f.editingEdge, f.edgeOpen = newEdgeForm(f.deps, "Rewire transition", tr.id == 0, tr.name, tr.src, tr.tgt, f.states), it.idx, true
	case fiAddTrans:
		f.edge, f.editingEdge, f.edgeOpen = newEdgeForm(f.deps, "Add transition", true, "", "", "", f.states), -1, true
	case fiSave:
		return f.submit()
	case fiCancel:
		return f, cancelFlow
	}
	return f, nil
}

func (f flowForm) applyNode(name, category, color string) flowForm {
	if f.editingNode >= 0 && f.editingNode < len(f.states) {
		f.states[f.editingNode].name = name
		f.states[f.editingNode].category = category
		f.states[f.editingNode].color = color
		return f.enforceInitial(f.editingNode)
	}
	f.seq++
	f.states = append(f.states, flowState{key: "n" + itoa(f.seq), name: name, category: category, color: color})
	f.focus = f.stateFocus(len(f.states) - 1)
	return f.enforceInitial(len(f.states) - 1)
}

func (f flowForm) applyEdge(name, src, tgt string) flowForm {
	if f.editingEdge >= 0 && f.editingEdge < len(f.trans) {
		if f.trans[f.editingEdge].id == 0 {
			f.trans[f.editingEdge].name = name
		}
		f.trans[f.editingEdge].src, f.trans[f.editingEdge].tgt = src, tgt
		return f
	}
	f.seq++
	f.trans = append(f.trans, flowTrans{key: "e" + itoa(f.seq), name: name, src: src, tgt: tgt})
	f.focus = f.transFocus(len(f.trans) - 1)
	return f
}

// enforceInitial keeps exactly one INITIAL: every other one falls back to ACTIVE.
func (f flowForm) enforceInitial(i int) flowForm {
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

// deleteCur also drops every transition touching a deleted state, so nothing dangles in the payload.
func (f flowForm) deleteCur() flowForm {
	switch it := f.cur(); it.kind {
	case fiState:
		key := f.states[it.idx].key
		f.states = append(f.states[:it.idx], f.states[it.idx+1:]...)
		kept := make([]flowTrans, 0, len(f.trans))
		for _, tr := range f.trans {
			if tr.src != key && tr.tgt != key {
				kept = append(kept, tr)
			}
		}
		f.trans = kept
		f.status = ""
	case fiTrans:
		f.trans = append(f.trans[:it.idx], f.trans[it.idx+1:]...)
		f.status = ""
	default:
		return f
	}
	return f.clampFocus()
}

func (f flowForm) submit() (flowForm, tea.Cmd) {
	if msg := f.validate(); msg != "" {
		f.status = msg
		return f, nil
	}
	states, trans := f.buildInputs()
	f.submitting, f.status = true, ""
	return f, tea.Batch(saveFlow(f.deps, f.wfID, f.version, states, trans), f.spinner.Tick)
}

// buildInputs serializes into the whole-graph replace payload: existing nodes carry their id (a
// state sends only its category, the rest is preserved server-side), new nodes carry a temp key.
func (f flowForm) buildInputs() ([]domain.GraphStateInput, []domain.GraphTransitionInput) {
	states := make([]domain.GraphStateInput, 0, len(f.states))
	for _, st := range f.states {
		in := domain.GraphStateInput{Category: st.category}
		if st.id != 0 {
			in.ID = st.id
		} else {
			in.TempKey, in.Name, in.Color = st.key, st.name, st.color
		}
		states = append(states, in)
	}
	trans := make([]domain.GraphTransitionInput, 0, len(f.trans))
	for _, tr := range f.trans {
		in := domain.GraphTransitionInput{Source: f.ref(tr.src), Target: f.ref(tr.tgt)}
		if tr.id != 0 {
			in.ID = tr.id
		} else {
			in.TempKey, in.Name = tr.key, tr.name
		}
		trans = append(trans, in)
	}
	return states, trans
}

// ref resolves a local state key to a save-time node reference (existing id or temp key).
func (f flowForm) ref(key string) domain.GraphRef {
	for _, st := range f.states {
		if st.key == key {
			if st.id != 0 {
				return domain.GraphRef{ID: st.id}
			}
			return domain.GraphRef{TempKey: st.key}
		}
	}
	return domain.GraphRef{}
}

// validate runs cheap structural checks for a fast error. The backend still enforces connectivity,
// node caps, and issue migration on deleted states.
func (f flowForm) validate() string {
	if len(f.states) == 0 {
		return "Add at least one state."
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

// draftDetail renders the working copy into a throwaway WorkflowDetail for the graph renderer.
// Local keys become synthetic ids numbered by draft order, which selElem/focusForElem map back.
func (f flowForm) draftDetail() domain.WorkflowDetail {
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
		d.Transitions = append(d.Transitions, domain.WorkflowTransition{ID: i + 1, Label: tr.name, SourceID: ids[tr.src], TargetID: ids[tr.tgt], Guards: tr.guards})
	}
	return d
}

// View renders only the node/edge sub-forms. The screen draws the editor body into the Details
// panel, so with no sub-form open there is nothing to render here.
func (f flowForm) View() string {
	switch {
	case f.nodeOpen:
		return f.node.View()
	case f.edgeOpen:
		return f.edge.View()
	}
	return ""
}

// affordance renders one action-bar handle, zone-marked so a click routes to flowForm.onClick.
func (f flowForm) affordance(id, label string, focused bool, base color.Color) string {
	st := affordanceStyle(f.deps.Styles.Theme, base, focused, f.hover == id)
	return zone.Mark(id, st.Render(label))
}

// actionBar must stay within width: a line the Details panel wraps loses its zone markers.
func (f flowForm) actionBar(width int) []string {
	t := f.deps.Styles.Theme
	row := f.affordance("flow.addstate", "+ State", f.focus == f.addStateFocus(), t.Secondary) +
		"   " + f.affordance("flow.addtrans", "+ Transition", f.focus == f.addTransFocus(), t.Secondary) +
		"   " + f.affordance("flow.save", "Save", f.cur().kind == fiSave, t.Success) +
		"   " + f.affordance("flow.cancel", "Cancel", f.cur().kind == fiCancel, t.Error)
	return []string{sectionRule(f.deps.Styles, "Edit structure", width), "", row, ""}
}

func (f flowForm) statusBlock(width int) []string {
	s := f.deps.Styles
	// a wrapped hint would shift every diagram row out of step with the element-row map
	out := []string{trunc(f.hint(), width)}
	switch {
	case f.submitting:
		out = append(out, lipgloss.NewStyle().Foreground(s.Theme.Warning).Render(f.spinner.View()+" Saving…"))
	case f.status != "":
		out = append(out, strings.Split(s.Error.Width(width).Render(f.status), "\n")...)
	}
	return out
}

// hint is the contextual key row. Enter only edits a new state: existing ones recategorize inline.
func (f flowForm) hint() string {
	s := f.deps.Styles
	switch it := f.cur(); it.kind {
	case fiState:
		if f.states[it.idx].id == 0 {
			return hintBar(s, "enter", "edit", "←/→", "category", "x", "delete", "a", "add state", "t", "from here")
		}
		return hintBar(s, "←/→", "category", "x", "delete", "a", "add state", "t", "from here")
	case fiTrans:
		return hintBar(s, "enter", "rewire", "x", "delete")
	default:
		return hintBar(s, "enter", "select", "esc", "cancel")
	}
}

func (f flowForm) HelpKeys() []key.Binding {
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
		key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
		key.NewBinding(key.WithKeys("a"), key.WithHelp("a", "add state")),
		key.NewBinding(key.WithKeys("t"), key.WithHelp("t", "add transition")),
	}
	switch it := f.cur(); it.kind {
	case fiState:
		binds = append(binds, key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "category")))
		if f.states[it.idx].id == 0 {
			binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "edit")))
		}
		binds = append(binds, key.NewBinding(key.WithKeys("x"), key.WithHelp("x", "delete")))
	case fiTrans:
		binds = append(binds,
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "rewire")),
			key.NewBinding(key.WithKeys("x"), key.WithHelp("x", "delete")))
	case fiAddState, fiAddTrans:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "add")))
	default: // save and cancel add nothing beyond the two bindings below
	}
	binds = append(binds, key.NewBinding(key.WithKeys("ctrl+s"), key.WithHelp("ctrl+s", "save")))
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

// onClick handles the action bar only. The screen hit-tests diagram elements first, it owns geometry.
func (f flowForm) onClick(msg tea.MouseClickMsg) (flowForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	switch {
	case zone.Get("flow.addstate").InBounds(msg):
		f.focus = f.addStateFocus()
		return f.activateCur()
	case zone.Get("flow.addtrans").InBounds(msg):
		f.focus = f.addTransFocus()
		return f.activateCur()
	case zone.Get("flow.save").InBounds(msg):
		f.focus = f.saveFocus()
		return f.submit()
	case zone.Get("flow.cancel").InBounds(msg):
		return f, cancelFlow
	}
	return f, nil
}

type flowSavedMsg struct{ wfID int }

type flowFailedMsg struct{ message string }

type flowCancelledMsg struct{}

func cancelFlow() tea.Msg { return flowCancelledMsg{} }

func saveFlow(d deps.Deps, wfID, version int, states []domain.GraphStateInput, trans []domain.GraphTransitionInput) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.ReplaceWorkflowGraph(context.Background(), wfID, version, states, trans); err != nil {
			return flowFailedMsg{message: flowErrorMessage(err)}
		}
		return flowSavedMsg{wfID: wfID}
	}
}

func flowErrorMessage(err error) string {
	if m, ok := errmsg.Override(err); ok {
		return m // connectivity, or a leaky code mapped to friendlier copy
	}
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "This workflow changed since you opened it. Cancel and reopen to edit the latest."
		case http.StatusBadRequest:
			return "Invalid graph. Needs one initial and one completed state, every state reachable, and issues migrated off any deleted state."
		case http.StatusForbidden:
			return "You do not have permission to edit this workflow."
		}
	}
	if r := domain.ErrorReason(err); r != "" {
		return r // prefer the server's own explanation
	}
	return "Could not save the graph. Try again."
}
