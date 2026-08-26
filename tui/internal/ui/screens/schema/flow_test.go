package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

// flowDetailModel loads a versioned example workflow with the Details pane focused.
func flowDetailModel(t *testing.T) Model {
	t.Helper()
	m := mkWorkflowModel(t)
	d := exampleWorkflow()
	d.Version = 7
	m, _ = m.Update(WorkflowDetailLoadedMsg{ID: 1, Detail: d})
	return m
}

// flowModel opens the graph-structure editor over the versioned example workflow.
func flowModel(t *testing.T) Model {
	t.Helper()
	m := flowDetailModel(t)
	mm, _, ok := m.openFlow()
	if !ok {
		t.Fatal("openFlow returned ok=false")
	}
	return mm
}

func keyLeft() tea.KeyPressMsg  { return tea.KeyPressMsg{Code: tea.KeyLeft} }
func keyRight() tea.KeyPressMsg { return tea.KeyPressMsg{Code: tea.KeyRight} }
func keyX() tea.KeyPressMsg     { return tea.KeyPressMsg{Code: 'x', Text: "x"} }

func TestOpenFlowSeedsGraph(t *testing.T) {
	m := flowModel(t)
	if !m.flowEditing || !m.CapturingInput() {
		t.Fatalf("editor not open/capturing (editing=%v capturing=%v)", m.flowEditing, m.CapturingInput())
	}
	if len(m.flow.states) != 5 || len(m.flow.trans) != 6 {
		t.Fatalf("seeded %d states, %d transitions; want 5/6", len(m.flow.states), len(m.flow.trans))
	}
	if m.flow.version != 7 {
		t.Errorf("version = %d, want 7", m.flow.version)
	}
	if m.flow.states[0].category != "INITIAL" {
		t.Errorf("first state category = %q, want INITIAL", m.flow.states[0].category)
	}
}

// Entering resets the scroll inherited from the read view and anchors on the initial state.
func TestOpenFlowFocusesFirstState(t *testing.T) {
	m := flowDetailModel(t)
	m.detailScroll = 50 // an inherited scroll from browsing the read view
	mm, _, ok := m.openFlow()
	if !ok {
		t.Fatal("openFlow returned ok=false")
	}
	e, ok := mm.flow.selElem()
	if !ok || e.kind != elemState || e.id != 1 { // To Do is the initial state, draft id -> 1
		t.Errorf("editor opened on %+v (ok=%v), want the initial state (elemState id 1)", e, ok)
	}
	if mm.detailScroll != 0 {
		t.Errorf("editor did not reset the scroll to the top: %d", mm.detailScroll)
	}
}

func TestOpenFlowOnlyForLoadedWorkflow(t *testing.T) {
	m := mkWorkflowModel(t)
	delete(m.wfDetail, 1)
	if _, _, ok := m.openFlow(); ok {
		t.Error("flow editor opened before the graph was loaded")
	}
}

// Cycling a category to INITIAL demotes the previous initial state.
func TestFlowCategoryCycleEnforcesSingleInitial(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.stateFocus(1) // In Progress, ACTIVE
	f, _ = f.onKey(keyLeft()) // ACTIVE -> INITIAL
	if f.states[1].category != "INITIAL" {
		t.Fatalf("focused state category = %q, want INITIAL", f.states[1].category)
	}
	if f.states[0].category != "ACTIVE" {
		t.Errorf("previous initial not demoted: %q", f.states[0].category)
	}
	f, _ = f.onKey(keyRight()) // back to ACTIVE
	if f.states[1].category != "ACTIVE" {
		t.Errorf("category = %q, want ACTIVE", f.states[1].category)
	}
}

func TestFlowDeleteStateCascadesTransitions(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.stateFocus(1) // In Progress (id 2)
	f = f.deleteCur()
	if len(f.states) != 4 {
		t.Fatalf("states = %d, want 4", len(f.states))
	}
	if len(f.trans) != 2 { // 4 of the 6 transitions touch In Progress
		t.Fatalf("transitions = %d, want 2", len(f.trans))
	}
	for _, tr := range f.trans {
		if tr.src == "s2" || tr.tgt == "s2" {
			t.Errorf("transition still references the deleted state: %+v", tr)
		}
	}
}

func TestFlowDeleteTransition(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.transFocus(0)
	f, _ = f.onKey(keyX())
	if len(f.trans) != 5 || len(f.states) != 5 {
		t.Fatalf("after delete: %d transitions, %d states; want 5/5", len(f.trans), len(f.states))
	}
}

func TestFlowAddTransitionAppends(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.addTransFocus()
	f, _ = f.activateCur()
	if !f.edgeOpen || !f.edge.isNew {
		t.Fatalf("add did not open a new edge form (open=%v new=%v)", f.edgeOpen, f.edge.isNew)
	}
	f.edge.name.SetValue("Escalate")
	f.edge.srcKey, f.edge.tgtKey = "s2", "s4"
	f.edge.focus = egSave
	f, _ = f.updateEdge(keyEnter())
	if f.edgeOpen {
		t.Fatal("edge form did not close after save")
	}
	if len(f.trans) != 7 {
		t.Fatalf("transitions = %d, want 7", len(f.trans))
	}
	got := f.trans[6]
	if got.name != "Escalate" || got.src != "s2" || got.tgt != "s4" || got.id != 0 || got.key == "" {
		t.Errorf("new transition = %+v", got)
	}
}

// Rewiring changes the endpoints but keeps the transition's id and name.
func TestFlowRewireExistingTransition(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.transFocus(0) // Start (id 10): s1 -> s2
	f, _ = f.activateCur()
	if !f.edgeOpen || f.edge.isNew {
		t.Fatalf("rewire opened wrong form (open=%v new=%v)", f.edgeOpen, f.edge.isNew)
	}
	f.edge.tgtKey = "s3"
	f.edge.focus = egSave
	f, _ = f.updateEdge(keyEnter())
	if f.trans[0].tgt != "s3" || f.trans[0].id != 10 {
		t.Errorf("rewired transition = %+v, want id 10 -> s3", f.trans[0])
	}
}

func TestFlowAddStateAppends(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.addStateFocus()
	f, _ = f.activateCur()
	if !f.nodeOpen {
		t.Fatal("add state did not open the node form")
	}
	f.node.name.SetValue("Blocked")
	f.node.category = "ACTIVE"
	f.node.focus = nfSave
	f, _ = f.updateNode(keyEnter())
	if f.nodeOpen {
		t.Fatal("node form did not close after save")
	}
	if len(f.states) != 6 {
		t.Fatalf("states = %d, want 6", len(f.states))
	}
	got := f.states[5]
	if got.name != "Blocked" || got.id != 0 || got.key == "" {
		t.Errorf("new state = %+v", got)
	}
}

func TestFlowAddStateRequiresName(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.addStateFocus()
	f, _ = f.activateCur()
	f.node.name.SetValue("")
	f.node.focus = nfSave
	f, _ = f.updateNode(keyEnter())
	if !f.nodeOpen {
		t.Fatal("empty-name node should keep the form open")
	}
	if len(f.states) != 5 {
		t.Errorf("states = %d, want 5 (nothing added)", len(f.states))
	}
}

func TestFlowBuildInputsRefsNewNodes(t *testing.T) {
	f := flowModel(t).flow
	f.states = append(f.states, flowState{key: "n1", name: "Blocked", category: "ACTIVE", color: "ANSI_RED"})
	f.trans = append(f.trans, flowTrans{key: "e1", name: "Block", src: "s1", tgt: "n1"})

	states, trans := f.buildInputs()
	if len(states) != 6 || len(trans) != 7 {
		t.Fatalf("built %d states, %d transitions; want 6/7", len(states), len(trans))
	}
	if states[0].ID != 1 || states[0].TempKey != "" || states[0].Name != "" {
		t.Errorf("existing state input = %+v, want id-only", states[0])
	}
	ns := states[5]
	if ns.ID != 0 || ns.TempKey != "n1" || ns.Name != "Blocked" || ns.Color != "ANSI_RED" {
		t.Errorf("new state input = %+v", ns)
	}
	nt := trans[6]
	if nt.ID != 0 || nt.TempKey != "e1" || nt.Name != "Block" {
		t.Errorf("new transition input = %+v", nt)
	}
	if nt.Source.ID != 1 || nt.Source.TempKey != "" {
		t.Errorf("source ref = %+v, want id 1", nt.Source)
	}
	if nt.Target.TempKey != "n1" || nt.Target.ID != 0 {
		t.Errorf("target ref = %+v, want temp key n1", nt.Target)
	}
}

// The example workflow is valid as loaded, so it submits.
func TestFlowSubmitIssuesSave(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.saveFocus()
	f, cmd := f.submit()
	if !f.submitting || cmd == nil {
		t.Fatalf("valid graph did not submit (submitting=%v cmd=%v)", f.submitting, cmd == nil)
	}
}

func TestFlowSubmitRejectsInvalid(t *testing.T) {
	f := flowModel(t).flow
	f.states[3].category = "ACTIVE" // Done was the only COMPLETED state
	f, cmd := f.submit()
	if f.submitting || cmd != nil {
		t.Fatal("invalid graph should not submit")
	}
	if f.status == "" {
		t.Error("expected a validation message")
	}
}

func TestFlowEditButtonOpensEditor(t *testing.T) {
	m := flowDetailModel(t)
	_ = scanView(t, m.View(), "schema.flow.edit")
	z := zone.Get("schema.flow.edit")
	m, _ = m.Update(tea.MouseClickMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY, Button: tea.MouseLeft})
	if !m.flowEditing {
		t.Fatal("clicking the Flow Edit button did not open the editor")
	}
}

func TestFlowEditButtonHover(t *testing.T) {
	m := flowDetailModel(t)
	_ = scanView(t, m.View(), "schema.flow.edit")
	z := zone.Get("schema.flow.edit")
	base := m.View()

	hovered, _ := m.Update(tea.MouseMotionMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY})
	if hovered.hoverAction != "schema.flow.edit" {
		t.Fatalf("hoverAction = %q, want schema.flow.edit", hovered.hoverAction)
	}
	if hovered.View() == base {
		t.Error("hovering the Edit button did not change its appearance")
	}

	away, _ := m.Update(tea.MouseMotionMsg{X: 0, Y: 0})
	if away.hoverAction != "" {
		t.Errorf("hoverAction not cleared off the button: %q", away.hoverAction)
	}
}

// esc closes an open sub-form first, then the editor.
func TestFlowEscClosesSubformThenEditor(t *testing.T) {
	m := flowModel(t)
	m.flow.focus = m.flow.addStateFocus()
	m.flow, _ = m.flow.activateCur()
	if !m.flow.nodeOpen {
		t.Fatal("sub-form did not open")
	}
	m, _ = m.Update(pressEsc())
	if !m.flowEditing || m.flow.nodeOpen {
		t.Fatalf("esc should close only the sub-form (editing=%v nodeOpen=%v)", m.flowEditing, m.flow.nodeOpen)
	}
	m, _ = m.Update(pressEsc())
	if m.flowEditing {
		t.Fatal("esc did not close the editor")
	}
}

func TestFlowSaveInvalidatesAndRefetches(t *testing.T) {
	m := flowModel(t)
	m, cmd := m.Update(flowSavedMsg{wfID: 1})
	if m.flowEditing {
		t.Fatal("save did not close the editor")
	}
	if _, ok := m.wfDetail[1]; ok {
		t.Error("cached graph not invalidated after save")
	}
	if !m.wfPending[1] || cmd == nil {
		t.Error("no refetch issued after save")
	}
}

func TestFlowShortcutOpensEditor(t *testing.T) {
	m := flowDetailModel(t)
	m, _ = m.Update(keyRune('f'))
	if !m.flowEditing {
		t.Fatal("f did not open the flow editor")
	}
}

// The draft diagram is drawn from the working copy, so unsaved states and wiring show up.
func TestFlowDraftDetailReflectsEdits(t *testing.T) {
	f := flowModel(t).flow
	f.states = append(f.states, flowState{key: "n1", name: "Blocked", category: "ACTIVE", color: "ANSI_RED"})
	f.trans = append(f.trans, flowTrans{key: "e1", name: "Block", src: "s2", tgt: "n1"})
	d := f.draftDetail()
	blockedID := 0
	for _, st := range d.States {
		if st.Label == "Blocked" {
			blockedID = st.ID
		}
	}
	if blockedID == 0 {
		t.Fatal("draftDetail missing the newly added state")
	}
	wired := false
	for _, tr := range d.Transitions {
		if tr.Label == "Block" && tr.TargetID == blockedID {
			wired = true
		}
	}
	if !wired {
		t.Error("draftDetail did not wire the new transition to the new state")
	}
}

// The editor diagram drops "+ Guard" (guards are edited from the read view) but still shows guards.
func TestFlowEditorHidesAddGuard(t *testing.T) {
	m := flowModel(t)
	panel := plain(m.detailPanel())
	if strings.Contains(panel, addGuardText) {
		t.Errorf("editor diagram should not show the %q affordance:\n%s", addGuardText, panel)
	}
	if !strings.Contains(panel, "assignee") { // the example's Submit transition carries an assignee guard
		t.Errorf("editor diagram did not show existing transition guards:\n%s", panel)
	}
}

// A long editor status wraps inside the Details panel instead of widening the dashboard.
func TestFlowLongStatusDoesNotWiden(t *testing.T) {
	m := flowModel(t)
	baseW := lipgloss.Width(m.View())
	m.flow.status = strings.Repeat("this is a long error message ", 6)
	if w := lipgloss.Width(m.View()); w != baseW {
		t.Errorf("long status changed the view width: %d, want %d", w, baseW)
	}
}

// The structure editor draws into the Details panel, not a centered modal.
func TestFlowEditsInPlace(t *testing.T) {
	m := flowModel(t)
	if !m.flowEditing || !m.CapturingInput() {
		t.Fatalf("editor not active (editing=%v capturing=%v)", m.flowEditing, m.CapturingInput())
	}
	if m.flow.hasOverlay() {
		t.Fatal("a freshly opened editor should have no floating overlay")
	}
	panel := plain(m.detailPanel())
	for _, want := range []string{"Editing structure", "+ State", "+ Transition", "Save", "Cancel"} {
		if !strings.Contains(panel, want) {
			t.Errorf("in-place editor panel missing %q:\n%s", want, panel)
		}
	}
}

// The action-bar handles are clickable zones inside the Details panel — mouse without a modal.
func TestFlowActionBarZonesInPanel(t *testing.T) {
	m := flowModel(t)
	view := m.View()
	for _, id := range []string{"flow.addstate", "flow.addtrans", "flow.save", "flow.cancel"} {
		if z := zone.Get(id); z == nil || z.IsZero() {
			_ = scanView(t, view, id)
			if z := zone.Get(id); z == nil || z.IsZero() {
				t.Errorf("action zone %q not present in the panel", id)
			}
		}
	}
}

// Clicking a draft node/edge moves the selection without opening a modal.
func TestFlowClickSelectsElement(t *testing.T) {
	m := flowModel(t)
	view := scanView(t, m.View(), "schema.detail")
	x, y, ok := locate(view, "In Progress") // draft state index 1
	if !ok {
		t.Fatal("could not find the In Progress state in the editor diagram")
	}
	m, _ = m.Update(tea.MouseClickMsg{X: x + 1, Y: y, Button: tea.MouseLeft})
	if m.flow.hasOverlay() {
		t.Fatal("clicking a node opened a sub-form; expected select only")
	}
	if it := m.flow.cur(); it.kind != fiState || it.idx != 1 {
		t.Errorf("focus after click = %+v, want the In Progress state (fiState idx 1)", it)
	}

	view = scanView(t, m.View(), "schema.detail")
	if x, y, ok := locate(view, "Reject"); ok { // a transition label (draft transition index 3)
		m, _ = m.Update(tea.MouseClickMsg{X: x + 1, Y: y, Button: tea.MouseLeft})
		if it := m.flow.cur(); it.kind != fiTrans {
			t.Errorf("clicking a transition label selected %+v, want a transition", it)
		}
	}
}

func TestFlowSelElemTracksFocus(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.stateFocus(2)
	if e, ok := f.selElem(); !ok || e != (wfElem{elemState, 3}) {
		t.Errorf("state focus -> %+v (ok=%v), want {elemState 3}", e, ok)
	}
	f.focus = f.transFocus(0)
	if e, ok := f.selElem(); !ok || e != (wfElem{elemTransition, 1}) {
		t.Errorf("transition focus -> %+v (ok=%v), want {elemTransition 1}", e, ok)
	}
	f.focus = f.saveFocus()
	if _, ok := f.selElem(); ok {
		t.Error("a command item should map to no diagram element")
	}
}

// ↑/↓ walk in visual diagram order (interleaved by row), not all states then all transitions.
func TestFlowNavFollowsDiagramOrder(t *testing.T) {
	f := flowModel(t).flow
	f.focus = f.stateFocus(0) // To Do, the initial state at the top of the diagram
	f.focus = f.navStep(1)
	if it := f.cur(); it.kind != fiTrans {
		t.Fatalf("down from the top state selected %+v, want the transition below it", it)
	}
	order := f.navOrder()
	if last := f.items()[order[len(order)-1]]; last.kind != fiCancel {
		t.Errorf("nav order does not end at the command handles: last = %+v", last)
	}
}

func TestFlowNavWrapsBothWays(t *testing.T) {
	f := flowModel(t).flow
	order := f.navOrder()
	first, last := order[0], order[len(order)-1]

	f.focus = last
	if got := f.navStep(1); got != first {
		t.Errorf("down from the last item -> %d, want wrap to first %d", got, first)
	}
	f.focus = first
	if got := f.navStep(-1); got != last {
		t.Errorf("up from the first item -> %d, want wrap to last %d", got, last)
	}
}

// t pre-fills a focused state as the new transition's source.
func TestFlowAddShortcuts(t *testing.T) {
	f := flowModel(t).flow
	fa, _ := f.onKey(keyRune('a'))
	if !fa.nodeOpen || fa.editingNode != -1 {
		t.Errorf("a did not open the add-state form (open=%v editing=%d)", fa.nodeOpen, fa.editingNode)
	}

	f.focus = f.stateFocus(1) // In Progress (key s2)
	ft, _ := f.onKey(keyRune('t'))
	if !ft.edgeOpen || !ft.edge.isNew {
		t.Fatalf("t did not open a new-transition form (open=%v new=%v)", ft.edgeOpen, ft.edge.isNew)
	}
	if ft.edge.srcKey != "s2" {
		t.Errorf("t from a focused state seeded source %q, want s2", ft.edge.srcKey)
	}
}
