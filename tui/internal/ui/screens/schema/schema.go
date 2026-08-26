// Package schema is the global issue-type and workflow catalog tab.
package schema

import (
	"context"
	"fmt"
	"sort"
	"strings"

	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

const (
	paneTypes = iota
	paneWorkflows
	paneDetail
	paneCount
)

const (
	hInset          = 2
	colGap          = 1
	minWidth        = 60
	minHeight       = 12
	detailInsetL    = 2
	detailInsetR    = 2
	detailScrollbar = 1
	detailWheelStep = 2
	detailPadBottom = 1
	listPadX        = 2  // extra columns inside TitledBox's own 1-cell inset, so 3 in total
	listPadY        = 1  // blank rows above and below the list body
	rowHeight       = 2  // an "airy" data row: a blank separator line above its content line
	nameMinW        = 10 // the flex column never shrinks below this — extra columns drop instead
)

type selKind int

const (
	selType selKind = iota
	selWorkflow
)

// Kinds of selectable Details element, each paired with a backend id. elemNone = nothing selected.
const (
	elemNone = iota
	elemState
	elemTransition
	elemAddGuard // the clickable "+ Guard" affordance beneath a spine transition
	elemVcsEdit  // the "Edit" button in the VCS Automation section rule
	elemFlowEdit // the "Edit" button in the Flow section rule
	elemWfMeta   // the workflow's metadata block (its title/Edit pen)
	elemTypeMeta // the issue type's metadata block (its title/Edit pen)
	elemField    // one custom field row, by its field id
	elemAddField // the "+ Field" affordance beneath the fields list
)

// detailActions are the workflow Details "Edit" buttons, focus stops above the graph. They exist
// exactly when the graph is loaded, so the first graph element always follows them.
var detailActions = []wfElem{{elemVcsEdit, 0}, {elemFlowEdit, 0}}

type wfElem struct {
	kind int
	id   int
}

type Model struct {
	deps deps.Deps

	loading   bool
	err       error
	types     []domain.IssueTypeSummary
	workflows []domain.WorkflowSummary

	focus      int
	typeCursor int
	wfCursor   int
	kind       selKind
	typeSel    int // index into the selected issue type's elements (metadata, then fields)

	// lazy issue-type detail (fields), cached by id
	typeDetail    map[int]domain.IssueTypeDetail
	detailPending map[int]bool
	detailFailed  map[int]bool

	// lazy workflow detail (graph), cached by id
	wfDetail  map[int]domain.WorkflowDetail
	wfPending map[int]bool
	wfFailed  map[int]bool

	detailScroll int
	detailFor    string // selection key the scroll offset belongs to
	wfSel        int    // index into the selected workflow's elements (states then transitions)
	wfHover      wfElem // graph element under the mouse cursor, for hover highlight
	hoverAction  string // section-rule action button (zone id) under the cursor, "" = none

	listHoverPane int // paneTypes/paneWorkflows when a list row is hovered, -1 = none
	listHoverRow  int // hovered row index within that list, -1 = none

	editing bool
	edit    editForm

	fieldEditing bool
	field        fieldForm

	creatingField bool
	cfield        createFieldForm

	confirming            bool
	confirm               confirmForm
	pendingDeleteField    int // the field a pending delete confirm targets, 0 = none
	pendingDeleteType     int // the issue type a pending delete confirm targets, 0 = none
	pendingDeleteWorkflow int // the workflow a pending delete confirm targets, 0 = none

	optionsEditing bool
	options        optionsForm

	// a just-created select/checklist field whose options editor auto-opens on the next detail load
	// (the create call returns no id, so it is matched by name)
	pendingOptionsType int
	pendingOptionsName string

	creatingWorkflow bool
	cworkflow        createWorkflowForm

	creatingType bool
	ctype        createTypeForm

	guardsEditing bool
	guards        guardsForm

	vcsEditing bool
	vcs        vcsForm

	flowEditing bool
	flow        flowForm

	// wheel offset for a modal taller than the terminal. Reset whenever no modal is open.
	modalScroll int

	width  int
	height int
}

func New(d deps.Deps) Model {
	return Model{
		deps:          d,
		loading:       true,
		kind:          selType,
		listHoverPane: -1,
		listHoverRow:  -1,
		typeDetail:    map[int]domain.IssueTypeDetail{},
		detailPending: map[int]bool{},
		detailFailed:  map[int]bool{},
		wfDetail:      map[int]domain.WorkflowDetail{},
		wfPending:     map[int]bool{},
		wfFailed:      map[int]bool{},
	}
}

func (m Model) Init() tea.Cmd { return load(m.deps) }

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		m.clampDetailScroll()
		if view, ok := m.activeModalView(); ok {
			m = m.scrollModalBy(view, 0) // re-clamp a windowed modal's offset to the new height
		}
		return m, nil
	case LoadedMsg:
		m.loading = false
		m.err = msg.Err
		m.types, m.workflows = msg.Types, msg.Workflows
		return m, tea.Batch(m.syncSelection(), m.prefetchAll())
	case TypeDetailLoadedMsg:
		delete(m.detailPending, msg.ID)
		if msg.Err != nil {
			m.detailFailed[msg.ID] = true
		} else {
			m.typeDetail[msg.ID] = msg.Detail
		}
		if m.optionsEditing && msg.Err == nil && msg.ID == m.options.typeID {
			m.reseedOptions(msg.Detail)
		}
		if m.pendingOptionsType == msg.ID {
			name := m.pendingOptionsName
			m.pendingOptionsType, m.pendingOptionsName = 0, ""
			if msg.Err == nil {
				if mm, cmd, ok := m.openOptionsForNewField(msg.ID, name); ok {
					return mm, cmd
				}
			}
		}
		return m, nil
	case fieldsReorderedMsg:
		return m, nil
	case fieldsReorderFailedMsg:
		// the optimistic reorder may not match the server, so refetch to resync
		delete(m.typeDetail, msg.typeID)
		delete(m.detailFailed, msg.typeID)
		m.detailPending[msg.typeID] = true
		return m, loadDetail(m.deps, msg.typeID)
	case WorkflowDetailLoadedMsg:
		delete(m.wfPending, msg.ID)
		if msg.Err != nil {
			m.wfFailed[msg.ID] = true
		} else {
			m.wfDetail[msg.ID] = msg.Detail
		}
		m.wfSel = min(max(m.wfSel, 0), max(0, len(m.workflowElems())-1))
		m.clampDetailScroll()
		return m, nil
	}
	// while a modal floats, the wheel drives its window when it overflows the terminal. Checked only
	// for wheel events, so a keystroke never pays for rendering the modal.
	if wheel, isWheel := msg.(tea.MouseWheelMsg); isWheel {
		if view, ok := m.activeModalView(); ok && lipgloss.Height(view) > m.height {
			switch wheel.Button {
			case tea.MouseWheelUp:
				return m.scrollModalBy(view, -1), nil
			case tea.MouseWheelDown:
				return m.scrollModalBy(view, 1), nil
			}
		}
	}
	if mm, cmd, handled := m.routeModal(msg); handled {
		// after keyboard navigation, scroll a windowed modal so its focused control stays visible
		if _, isKey := msg.(tea.KeyPressMsg); isKey {
			mm = mm.followModalFocus()
		}
		return mm, cmd
	}
	m.modalScroll = 0 // no modal is floating — keep the next one's window starting at the top
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		return m.onKey(msg)
	case tea.MouseClickMsg:
		return m.onClick(msg)
	case tea.MouseMotionMsg:
		return m.onMotion(msg)
	case tea.MouseWheelMsg:
		return m.onWheel(msg)
	}
	return m, nil
}

func (m Model) updateGuards(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case guardsSavedMsg:
		m.guardsEditing = false
		delete(m.wfDetail, msg.wfID)
		delete(m.wfFailed, msg.wfID)
		m.wfPending[msg.wfID] = true
		return m, loadWorkflow(m.deps, msg.wfID)
	case guardsCancelledMsg:
		m.guardsEditing = false
		return m, nil
	case tea.KeyPressMsg:
		// esc closes the modal, unless the type dropdown is open — then it just closes that
		if msg.String() == "esc" && !m.guards.submitting && !m.guards.pickOpen {
			m.guardsEditing = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.guards, cmd = m.guards.Update(msg)
	return m, cmd
}

func (m Model) openGuards() (Model, tea.Cmd, bool) {
	e, ok := m.selectedElem()
	if !ok || e.kind != elemTransition {
		return m, nil, false
	}
	return m.openGuardsFor(e.id, false)
}

// openGuardsFor opens the guard editor. add lands straight on the add-guard dropdown.
func (m Model) openGuardsFor(transID int, add bool) (Model, tea.Cmd, bool) {
	if m.kind != selWorkflow {
		return m, nil, false
	}
	w, ok := m.selectedWorkflow()
	if !ok {
		return m, nil, false
	}
	d, ok := m.wfDetail[w.ID]
	if !ok {
		return m, nil, false
	}
	for _, tr := range d.Transitions {
		if tr.ID == transID {
			f := newGuardsForm(m.deps, w.ID, tr.ID, tr.Label, tr.Guards)
			if add {
				f = f.openAddPicker()
			}
			m.guards = f
			m.guardsEditing = true
			return m, m.guards.Init(), true
		}
	}
	return m, nil, false
}

func (m Model) updateVcs(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case vcsSavedMsg:
		m.vcsEditing = false
		delete(m.wfDetail, msg.wfID)
		delete(m.wfFailed, msg.wfID)
		m.wfPending[msg.wfID] = true
		return m, loadWorkflow(m.deps, msg.wfID)
	case vcsCancelledMsg:
		m.vcsEditing = false
		return m, nil
	case tea.KeyPressMsg:
		// esc closes the modal, unless a transition dropdown is open — then it just closes that
		if msg.String() == "esc" && !m.vcs.submitting && !m.vcs.pickOpen {
			m.vcsEditing = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.vcs, cmd = m.vcs.Update(msg)
	return m, cmd
}

func (m Model) openVcs() (Model, tea.Cmd, bool) {
	if m.kind != selWorkflow {
		return m, nil, false
	}
	w, ok := m.selectedWorkflow()
	if !ok {
		return m, nil, false
	}
	d, ok := m.wfDetail[w.ID]
	if !ok {
		return m, nil, false
	}
	m.vcs = newVcsForm(m.deps, w.ID, d.Transitions, d.States, d.VcsPrOpenedTransitionID, d.VcsPrMergedTransitionID)
	m.vcsEditing = true
	return m, m.vcs.Init(), true
}

func (m Model) updateFlow(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case flowSavedMsg:
		m.flowEditing = false
		delete(m.wfDetail, msg.wfID)
		delete(m.wfFailed, msg.wfID)
		m.wfPending[msg.wfID] = true
		return m, loadWorkflow(m.deps, msg.wfID)
	case flowCancelledMsg:
		m.flowEditing = false
		return m, nil
	case tea.KeyPressMsg:
		// esc closes the editor, unless a sub-form is open — then it just backs out of that level
		if msg.String() == "esc" && !m.flow.submitting && !m.flow.nodeOpen && !m.flow.edgeOpen {
			m.flowEditing = false
			return m, nil
		}
	case tea.MouseClickMsg:
		// a click on a diagram node/edge selects it. The action bar and empty space fall through below.
		if !m.flow.hasOverlay() && msg.Button == tea.MouseLeft {
			if e, ok := m.hitElem(msg); ok {
				if fi, ok := m.flow.focusForElem(e); ok {
					m.flow.focus = fi
					m.revealFlowSel()
					return m, nil
				}
			}
		}
	}
	var cmd tea.Cmd
	m.flow, cmd = m.flow.Update(msg)
	m.revealFlowSel() // keep the selected draft element scrolled into view as focus moves
	return m, cmd
}

func (m Model) openFlow() (Model, tea.Cmd, bool) {
	if m.kind != selWorkflow {
		return m, nil, false
	}
	w, ok := m.selectedWorkflow()
	if !ok {
		return m, nil, false
	}
	d, ok := m.wfDetail[w.ID]
	if !ok {
		return m, nil, false
	}
	m.flow = newFlowForm(m.deps, d)
	m.flow.focus = m.flow.firstStateFocus() // anchor on the diagram's initial state
	m.flowEditing = true
	m.detailScroll = 0 // enter at the top so the first state and the action bar are both in view
	return m, m.flow.Init(), true
}

func (m Model) activateDetailAction() (Model, tea.Cmd, bool) {
	e, ok := m.selectedElem()
	if !ok {
		return m, nil, false
	}
	switch e.kind {
	case elemWfMeta:
		return m.openWorkflowEdit()
	case elemVcsEdit:
		return m.openVcs()
	case elemFlowEdit:
		return m.openFlow()
	}
	return m, nil, false
}

func (m Model) actionFocused(kind int) bool {
	if m.focus != paneDetail {
		return false
	}
	e, ok := m.selectedElem()
	return ok && e.kind == kind
}

func (m Model) updateEdit(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case editSavedMsg:
		m.editing = false
		if m.edit.kind == editIssueType || m.edit.kind == editWorkflow {
			// name/color/description come from the catalog list, so refetch to reflect the edit. A
			// metadata edit leaves the graph/fields caches untouched.
			return m, load(m.deps)
		}
		delete(m.wfDetail, msg.wfID)
		delete(m.wfFailed, msg.wfID)
		m.wfPending[msg.wfID] = true
		return m, loadWorkflow(m.deps, msg.wfID)
	case editCancelledMsg:
		m.editing = false
		return m, nil
	case tea.KeyPressMsg:
		// esc closes the modal, unless the color grid is open — then it just closes that
		if msg.String() == "esc" && !m.edit.submitting && !m.edit.picking {
			m.editing = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.edit, cmd = m.edit.Update(msg)
	return m, cmd
}

// openTypeEdit edits issue type metadata. Hierarchy and workflow are fixed at creation.
func (m Model) openTypeEdit() (Model, tea.Cmd, bool) {
	if m.kind != selType {
		return m, nil, false
	}
	t, ok := m.selectedType()
	if !ok {
		return m, nil, false
	}
	m.edit = newEditForm(m.deps, editIssueType, t.ID, 0, "Edit Issue Type", t.Name, t.Color, t.Description, true)
	m.editing = true
	return m, m.edit.Init(), true
}

// openWorkflowEdit edits workflow metadata. Workflow color is being retired, so no color field is
// shown and the save omits it (UpdateWorkflow drops an empty color from the PATCH).
func (m Model) openWorkflowEdit() (Model, tea.Cmd, bool) {
	if m.kind != selWorkflow {
		return m, nil, false
	}
	w, ok := m.selectedWorkflow()
	if !ok {
		return m, nil, false
	}
	m.edit = newEditForm(m.deps, editWorkflow, w.ID, 0, "Edit Workflow", w.Name, "", w.Description, false)
	m.editing = true
	return m, m.edit.Init(), true
}

func (m Model) openMetaForPane() (Model, tea.Cmd, bool) {
	switch m.focus {
	case paneTypes:
		return m.openTypeEdit()
	case paneWorkflows:
		return m.openWorkflowEdit()
	}
	return m, nil, false
}

func (m Model) updateField(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case fieldSavedMsg:
		m.fieldEditing = false
		delete(m.typeDetail, msg.typeID)
		delete(m.detailFailed, msg.typeID)
		m.detailPending[msg.typeID] = true
		return m, loadDetail(m.deps, msg.typeID)
	case fieldCancelledMsg:
		m.fieldEditing = false
		return m, nil
	case tea.KeyPressMsg:
		if msg.String() == "esc" && !m.field.submitting {
			m.fieldEditing = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.field, cmd = m.field.Update(msg)
	return m, cmd
}

func (m Model) openFieldEdit(fieldID int) (Model, tea.Cmd, bool) {
	if m.kind != selType {
		return m, nil, false
	}
	t, ok := m.selectedType()
	if !ok {
		return m, nil, false
	}
	d, ok := m.typeDetail[t.ID]
	if !ok {
		return m, nil, false
	}
	for _, f := range d.Fields {
		if f.ID == fieldID {
			m.field = newFieldForm(m.deps, t.ID, f.ID, f.Name, f.Description, f.Required)
			m.fieldEditing = true
			return m, m.field.Init(), true
		}
	}
	return m, nil, false
}

// typeElems is the selected type's focus stops: metadata, each field in display order, then
// "+ Field". They appear only once the detail is loaded, so navigation never lands undrawn.
func (m Model) typeElems() []wfElem {
	elems := []wfElem{{elemTypeMeta, 0}}
	if t, ok := m.selectedType(); ok {
		if d, ok := m.typeDetail[t.ID]; ok {
			for _, f := range d.Fields {
				elems = append(elems, wfElem{elemField, f.ID})
			}
			elems = append(elems, wfElem{elemAddField, 0})
		}
	}
	return elems
}

func (m Model) selectedTypeElem() (wfElem, bool) {
	elems := m.typeElems()
	if m.typeSel < 0 || m.typeSel >= len(elems) {
		return wfElem{}, false
	}
	return elems[m.typeSel], true
}

func (m Model) editSelectedTypeElem() (Model, tea.Cmd, bool) {
	e, ok := m.selectedTypeElem()
	if !ok {
		return m, nil, false
	}
	switch e.kind {
	case elemTypeMeta:
		return m.openTypeEdit()
	case elemField:
		return m.openFieldEdit(e.id)
	case elemAddField:
		return m.openFieldCreate()
	}
	return m, nil, false
}

func (m Model) moveTypeElem(msg tea.KeyPressMsg) Model {
	n := len(m.typeElems())
	switch msg.String() {
	case "up", "k":
		m.typeSel--
	case "down", "j":
		m.typeSel++
	case "home", "g":
		m.typeSel = 0
	case "end", "G":
		m.typeSel = n - 1
	case "pgup":
		m.detailScroll -= m.detailViewHeight() - 1
		m.clampDetailScroll()
		return m
	case "pgdown":
		m.detailScroll += m.detailViewHeight() - 1
		m.clampDetailScroll()
		return m
	default:
		return m
	}
	m.typeSel = min(max(m.typeSel, 0), n-1)
	m.revealSelectedTypeElem()
	return m
}

func (m *Model) revealSelectedTypeElem() {
	e, ok := m.selectedTypeElem()
	if !ok {
		return
	}
	t, ok := m.selectedType()
	if !ok {
		return
	}
	_, rows := m.issueTypeDetailLines(t)
	r, ok := rows[e]
	if !ok {
		m.clampDetailScroll()
		return
	}
	viewH := m.detailViewHeight()
	if top := r - 1; top < m.detailScroll { // a row of margin above
		m.detailScroll = top
	}
	if bottom := r + 1; bottom > m.detailScroll+viewH {
		m.detailScroll = bottom - viewH
	}
	m.clampDetailScroll()
}

func (m Model) openEdit() (Model, tea.Cmd, bool) {
	if m.kind != selWorkflow {
		return m, nil, false
	}
	e, ok := m.selectedElem()
	if !ok {
		return m, nil, false
	}
	if e.kind == elemWfMeta {
		// metadata is editable before the graph loads, so handle it before the graph lookup
		return m.openWorkflowEdit()
	}
	w, ok := m.selectedWorkflow()
	if !ok {
		return m, nil, false
	}
	d, ok := m.wfDetail[w.ID]
	if !ok {
		return m, nil, false
	}
	switch e.kind {
	case elemState:
		for _, st := range d.States {
			if st.ID == e.id {
				m.edit = newEditForm(m.deps, editState, w.ID, st.ID, "Edit State", st.Label, st.Color, st.Description, true)
				m.editing = true
				return m, m.edit.Init(), true
			}
		}
	case elemTransition:
		for _, tr := range d.Transitions {
			if tr.ID == e.id {
				m.edit = newEditForm(m.deps, editTransition, w.ID, tr.ID, "Edit Transition", tr.Label, "", tr.Description, false)
				m.editing = true
				return m, m.edit.Init(), true
			}
		}
	}
	return m, nil, false
}

func (m Model) onKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	// the keyboard is now driving — drop any stale mouse-hover highlights
	m.wfHover, m.hoverAction = wfElem{}, ""
	m.listHoverPane, m.listHoverRow = -1, -1
	switch msg.String() {
	case "tab":
		return m.setFocus((m.focus + 1) % paneCount)
	case "shift+tab":
		return m.setFocus((m.focus - 1 + paneCount) % paneCount)
	}
	switch m.focus {
	case paneTypes, paneWorkflows:
		// common actions run straight from the list. →/l/enter drill into Details for the per-field
		// / per-state work that needs an element cursor.
		switch msg.String() {
		case "n":
			if mm, cmd, ok := m.openCreateForPane(); ok {
				return mm, cmd
			}
			return m, nil
		case "e":
			if mm, cmd, ok := m.openMetaForPane(); ok {
				return mm, cmd
			}
			return m, nil
		case "a":
			if m.focus == paneTypes {
				if mm, cmd, ok := m.openFieldCreate(); ok {
					return mm, cmd
				}
			}
			return m, nil
		case "v":
			if m.focus == paneWorkflows {
				if mm, cmd, ok := m.openVcs(); ok {
					return mm, cmd
				}
			}
			return m, nil
		case "f":
			if m.focus == paneWorkflows {
				if mm, cmd, ok := m.openFlow(); ok {
					return mm, cmd
				}
			}
			return m, nil
		case "d", "delete", "backspace":
			if mm, cmd, ok := m.deleteForPane(); ok {
				return mm, cmd
			}
			return m, nil
		case "right", "l", "enter":
			return m.setFocus(paneDetail)
		}
		return m.moveList(msg)
	case paneDetail:
		switch msg.String() {
		case "left", "h", "esc":
			if m.kind == selType {
				return m.setFocus(paneTypes)
			}
			return m.setFocus(paneWorkflows)
		case "enter", "space":
			if mm, cmd, ok := m.activateDetailAction(); ok {
				return mm, cmd
			}
			if m.kind == selType {
				if mm, cmd, ok := m.editSelectedTypeElem(); ok {
					return mm, cmd
				}
			}
		case "e":
			if m.kind == selType {
				if mm, cmd, ok := m.editSelectedTypeElem(); ok {
					return mm, cmd
				}
			}
			if mm, cmd, ok := m.openEdit(); ok {
				return mm, cmd
			}
		case "g":
			if mm, cmd, ok := m.openGuards(); ok {
				return mm, cmd
			}
		case "v":
			if mm, cmd, ok := m.openVcs(); ok {
				return mm, cmd
			}
		case "f":
			if mm, cmd, ok := m.openFlow(); ok {
				return mm, cmd
			}
		case "x", "delete", "backspace":
			if m.kind == selType {
				if mm, cmd, ok := m.deleteSelectedField(); ok {
					return mm, cmd
				}
			}
		case "shift+up":
			if m.kind == selType {
				if mm, cmd, ok := m.reorderSelectedField(-1); ok {
					return mm, cmd
				}
			}
		case "shift+down":
			if m.kind == selType {
				if mm, cmd, ok := m.reorderSelectedField(1); ok {
					return mm, cmd
				}
			}
		case "o":
			if m.kind == selType {
				if mm, cmd, ok := m.openFieldOptions(); ok {
					return mm, cmd
				}
			}
		}
		return m.scrollKey(msg), nil
	}
	return m, nil
}

// CapturingInput reports that a modal owns the keyboard, so the shell drops its tab-switch keys.
func (m Model) CapturingInput() bool {
	return m.editing || m.fieldEditing || m.creatingField || m.confirming || m.optionsEditing ||
		m.creatingWorkflow || m.creatingType || m.guardsEditing || m.vcsEditing || m.flowEditing
}

// moveList takes the cursor pointer from its own receiver so the mutation is kept.
func (m Model) moveList(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	cursor, n := &m.typeCursor, len(m.types)
	if m.focus == paneWorkflows {
		cursor, n = &m.wfCursor, len(m.workflows)
	}
	switch msg.String() {
	case "up", "k":
		if *cursor > 0 {
			*cursor--
		}
	case "down", "j":
		if *cursor < n-1 {
			*cursor++
		}
	case "home", "g":
		*cursor = 0
	case "end", "G":
		if n > 0 {
			*cursor = n - 1
		}
	default:
		return m, nil
	}
	return m, m.syncSelection()
}

func (m Model) scrollKey(msg tea.KeyPressMsg) Model {
	if m.kind == selWorkflow && len(m.workflowElems()) > 0 {
		return m.moveElem(msg)
	}
	if m.kind == selType {
		return m.moveTypeElem(msg)
	}
	switch msg.String() {
	case "up", "k":
		m.detailScroll--
	case "down", "j":
		m.detailScroll++
	case "pgup":
		m.detailScroll -= m.detailViewHeight() - 1
	case "pgdown":
		m.detailScroll += m.detailViewHeight() - 1
	case "home", "g":
		m.detailScroll = 0
	case "end", "G":
		m.detailScroll = m.detailScrollMax()
	}
	m.clampDetailScroll()
	return m
}

func (m Model) moveElem(msg tea.KeyPressMsg) Model {
	n := len(m.workflowElems())
	switch msg.String() {
	case "up", "k":
		m.wfSel--
	case "down", "j":
		m.wfSel++
	case "home", "g":
		m.wfSel = 0
	case "end", "G":
		m.wfSel = n - 1
	case "pgup":
		m.detailScroll -= m.detailViewHeight() - 1
		m.clampDetailScroll()
		return m
	case "pgdown":
		m.detailScroll += m.detailViewHeight() - 1
		m.clampDetailScroll()
		return m
	default:
		return m
	}
	m.wfSel = min(max(m.wfSel, 0), n-1)
	m.revealSelectedElem()
	return m
}

// workflowElems orders the workflow's focus stops by diagram row, so ↑/↓ walks it top-to-bottom.
func (m Model) workflowElems() []wfElem {
	w, ok := m.selectedWorkflow()
	if !ok {
		return nil
	}
	// the title's Edit pen is selectable before the graph loads, so metadata needs no wait
	elems := []wfElem{{elemWfMeta, 0}}
	d, ok := m.wfDetail[w.ID]
	if !ok {
		return elems
	}
	_, rows, _ := renderWorkflowGraph(d, m.deps.Styles, m.detailContentW(), wfElem{}, wfElem{}, true)
	elems = append(elems, detailActions...)
	for _, st := range d.States {
		elems = append(elems, wfElem{elemState, st.ID})
	}
	for _, tr := range d.Transitions {
		elems = append(elems, wfElem{elemTransition, tr.ID})
	}
	sort.SliceStable(elems, func(i, j int) bool { return elemNavRow(elems[i], rows) < elemNavRow(elems[j], rows) })
	return elems
}

// elemNavRow orders navigation: the section buttons render above the graph, then graph rows.
func elemNavRow(e wfElem, rows map[wfElem]int) int {
	switch e.kind {
	case elemWfMeta:
		return -3
	case elemVcsEdit:
		return -2
	case elemFlowEdit:
		return -1
	default:
		return rows[e]
	}
}

func (m Model) selectedElem() (wfElem, bool) {
	elems := m.workflowElems()
	if m.wfSel < 0 || m.wfSel >= len(elems) {
		return wfElem{}, false
	}
	return elems[m.wfSel], true
}

func (m *Model) revealSelectedElem() {
	e, ok := m.selectedElem()
	if !ok {
		return
	}
	r, ok := m.elemPanelRow(e)
	if !ok {
		m.clampDetailScroll()
		return
	}
	viewH := m.detailViewHeight()
	if top := r - 1; top < m.detailScroll { // a row of margin above
		m.detailScroll = top
	}
	if bottom := r + 3; bottom > m.detailScroll+viewH { // keep the whole box (3 rows) in view
		m.detailScroll = bottom - viewH
	}
	m.clampDetailScroll()
}

func (m Model) setFocus(target int) (Model, tea.Cmd) {
	m.focus = target
	switch target {
	case paneTypes:
		m.kind = selType
	case paneWorkflows:
		m.kind = selWorkflow
	}
	return m, m.syncSelection()
}

func (m *Model) syncSelection() tea.Cmd {
	if key := m.selectionKey(); key != m.detailFor {
		m.detailFor = key
		m.detailScroll = 0
		// default to the first graph element: the title pen and two section buttons precede it
		m.wfSel = len(detailActions) + 1
		// default an issue type's selection to its metadata block, so e edits the type on entry
		m.typeSel = 0
	}
	if m.kind == selType {
		if t, ok := m.selectedType(); ok {
			return m.fetchDetail(t.ID)
		}
	}
	if m.kind == selWorkflow {
		if w, ok := m.selectedWorkflow(); ok {
			return m.fetchWorkflow(w.ID)
		}
	}
	return nil
}

// prefetchAll loads every type's fields and workflow graph up front so Details never flashes
// empty. The per-id pending/failed guards dedupe the selected item's own fetch.
func (m *Model) prefetchAll() tea.Cmd {
	var cmds []tea.Cmd
	for _, t := range m.types {
		if c := m.fetchDetail(t.ID); c != nil {
			cmds = append(cmds, c)
		}
	}
	for _, w := range m.workflows {
		if c := m.fetchWorkflow(w.ID); c != nil {
			cmds = append(cmds, c)
		}
	}
	if len(cmds) == 0 {
		return nil
	}
	return tea.Batch(cmds...)
}

func (m *Model) fetchDetail(id int) tea.Cmd {
	if _, ok := m.typeDetail[id]; ok {
		return nil
	}
	if m.detailPending[id] || m.detailFailed[id] {
		return nil
	}
	m.detailPending[id] = true
	return loadDetail(m.deps, id)
}

func (m *Model) fetchWorkflow(id int) tea.Cmd {
	if _, ok := m.wfDetail[id]; ok {
		return nil
	}
	if m.wfPending[id] || m.wfFailed[id] {
		return nil
	}
	m.wfPending[id] = true
	return loadWorkflow(m.deps, id)
}

func (m Model) onClick(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	switch {
	case zone.Get("schema.types").InBounds(msg):
		_, y := zone.Get("schema.types").Pos(msg)
		if row, ok := rowAt(y, m.typeListTop(), len(m.types), m.typesInnerH()); ok {
			m.typeCursor = row
		}
		return m.setFocus(paneTypes)
	case zone.Get("schema.workflows").InBounds(msg):
		_, y := zone.Get("schema.workflows").Pos(msg)
		if row, ok := rowAt(y, m.wfListTop(), len(m.workflows), m.wfInnerH()); ok {
			m.wfCursor = row
		}
		return m.setFocus(paneWorkflows)
	case zone.Get("schema.detail").InBounds(msg):
		m.focus = paneDetail
		if z := zone.Get("schema.type.edit"); z != nil && z.InBounds(msg) {
			if mm, cmd, ok := m.openTypeEdit(); ok {
				return mm, cmd
			}
			return m, nil
		}
		if z := zone.Get("schema.wf.edit"); z != nil && z.InBounds(msg) {
			if mm, cmd, ok := m.openWorkflowEdit(); ok {
				return mm, cmd
			}
			return m, nil
		}
		if fid, ok := m.hitFieldPen(msg); ok {
			if mm, cmd, opened := m.openFieldEdit(fid); opened {
				return mm, cmd
			}
			return m, nil
		}
		if z := zone.Get("schema.field.new"); z != nil && z.InBounds(msg) {
			if mm, cmd, ok := m.openFieldCreate(); ok {
				return mm, cmd
			}
			return m, nil
		}
		if z := zone.Get("schema.vcs.edit"); z != nil && z.InBounds(msg) {
			if mm, cmd, ok := m.openVcs(); ok {
				return mm, cmd
			}
			return m, nil
		}
		if z := zone.Get("schema.flow.edit"); z != nil && z.InBounds(msg) {
			if mm, cmd, ok := m.openFlow(); ok {
				return mm, cmd
			}
			return m, nil
		}
		if e, ok := m.hitElem(msg); ok {
			m.wfHover = wfElem{}
			if e.kind == elemAddGuard {
				if mm, cmd, opened := m.openGuardsFor(e.id, true); opened {
					return mm, cmd
				}
				return m, nil
			}
			m.selectElemByRef(e)
			if mm, cmd, opened := m.openEdit(); opened {
				return mm, cmd
			}
		}
	}
	return m, nil
}

// hitFieldPen maps a mouse position to the custom field whose edit pen is under it, if any.
func (m Model) hitFieldPen(msg tea.MouseMsg) (int, bool) {
	if m.kind != selType {
		return 0, false
	}
	t, ok := m.selectedType()
	if !ok {
		return 0, false
	}
	d, ok := m.typeDetail[t.ID]
	if !ok {
		return 0, false
	}
	for _, f := range d.Fields {
		if z := zone.Get(fieldEditZone(f.ID)); z != nil && z.InBounds(msg) {
			return f.ID, true
		}
	}
	return 0, false
}

// onMotion tracks which graph element the cursor is over so the diagram can highlight it.
func (m Model) onMotion(msg tea.MouseMotionMsg) (Model, tea.Cmd) {
	m.hoverAction = ""
	for _, id := range sectionActionZones {
		if z := zone.Get(id); z != nil && z.InBounds(msg) {
			m.hoverAction = id
			break
		}
	}
	// a hovered per-field pen tints Secondary like the section-rule buttons
	if m.hoverAction == "" {
		if fid, ok := m.hitFieldPen(msg); ok {
			m.hoverAction = fieldEditZone(fid)
		}
	}
	hov := wfElem{}
	if e, ok := m.hitElem(msg); ok {
		hov = e
	}
	m.wfHover = hov

	m.listHoverPane, m.listHoverRow = -1, -1
	if z := zone.Get("schema.types"); z != nil && z.InBounds(msg) {
		_, y := z.Pos(msg)
		if row, ok := rowAt(y, m.typeListTop(), len(m.types), m.typesInnerH()); ok {
			m.listHoverPane, m.listHoverRow = paneTypes, row
		}
	} else if z := zone.Get("schema.workflows"); z != nil && z.InBounds(msg) {
		_, y := z.Pos(msg)
		if row, ok := rowAt(y, m.wfListTop(), len(m.workflows), m.wfInnerH()); ok {
			m.listHoverPane, m.listHoverRow = paneWorkflows, row
		}
	}
	return m, nil
}

// hoverRowFor is the hovered row index in pane, or -1.
func (m Model) hoverRowFor(pane int) int {
	if m.listHoverPane == pane {
		return m.listHoverRow
	}
	return -1
}

// sectionActionZones are the Details "Edit" buttons, in hover hit-test order.
var sectionActionZones = []string{"schema.type.edit", "schema.wf.edit", "schema.field.new", "schema.vcs.edit", "schema.flow.edit"}

// hitElem maps a Details mouse position through the scroll offset into the graph's cell grid.
func (m Model) hitElem(msg tea.MouseMsg) (wfElem, bool) {
	if m.kind != selWorkflow {
		return wfElem{}, false
	}
	z := zone.Get("schema.detail")
	if z == nil || !z.InBounds(msg) {
		return wfElem{}, false
	}
	px, py := z.Pos(msg)
	viewH := m.detailViewHeight()
	row := py - 1 // skip the top rule
	if row < 0 || row >= viewH {
		return wfElem{}, false
	}
	col := px - detailInsetL // skip the left inset
	if col < 0 || col >= m.detailContentW() {
		return wfElem{}, false
	}
	// while editing, hit-test the live draft graph — otherwise the cached read-only one
	hits, gStart, ok := m.workflowGraphGeometry()
	if m.flowEditing {
		hits, gStart, ok = m.flowGraphGeometry()
	}
	if !ok {
		return wfElem{}, false
	}
	voff := max(0, m.detailScroll) // kept clamped elsewhere, matching detailPanel's offset
	gr := voff + row - gStart      // graph-local row
	for e, rc := range hits {
		if gr >= rc.r0 && gr <= rc.r1 && col >= rc.c0 && col <= rc.c1 {
			return e, true
		}
	}
	return wfElem{}, false
}

// flowGraphGeometry is workflowGraphGeometry for the in-place editor: it hit-tests the live draft
// diagram, so clicks land on the element being edited rather than the last-saved graph.
func (m Model) flowGraphGeometry() (map[wfElem]grect, int, bool) {
	w, ok := m.selectedWorkflow()
	if !ok {
		return nil, 0, false
	}
	_, grows, hits := renderWorkflowGraph(m.flow.draftDetail(), m.deps.Styles, m.detailContentW(), wfElem{}, wfElem{}, false)
	if len(hits) == 0 {
		return nil, 0, false
	}
	_, dcRows := m.flowEditorLines(w, m.detailContentW())
	for e, dcRow := range dcRows {
		if _, isHit := hits[e]; isHit {
			return hits, dcRow - grows[e], true // gStart = detailContent row - graph-local row
		}
	}
	return nil, 0, false
}

// workflowGraphGeometry returns the element hit rectangles (graph-local) and the Details line
// holding graph row 0, derived from the element mapping so it stays in step with the renderer.
func (m Model) workflowGraphGeometry() (map[wfElem]grect, int, bool) {
	w, ok := m.selectedWorkflow()
	if !ok {
		return nil, 0, false
	}
	d, ok := m.wfDetail[w.ID]
	if !ok {
		return nil, 0, false
	}
	_, grows, hits := renderWorkflowGraph(d, m.deps.Styles, m.detailContentW(), wfElem{}, wfElem{}, true)
	if len(hits) == 0 {
		return nil, 0, false
	}
	_, dcRows := m.workflowDetailLines(w)
	for e, dcRow := range dcRows {
		if _, isHit := hits[e]; isHit {
			return hits, dcRow - grows[e], true // gStart = detailContent row - graph-local row
		}
	}
	return nil, 0, false
}

func (m *Model) selectElemByRef(e wfElem) {
	for i, el := range m.workflowElems() {
		if el == e {
			m.wfSel = i
			return
		}
	}
}

func (m Model) onWheel(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	if !zone.Get("schema.detail").InBounds(msg) {
		return m, nil
	}
	switch msg.Button {
	case tea.MouseWheelUp:
		m.detailScroll -= detailWheelStep
	case tea.MouseWheelDown:
		m.detailScroll += detailWheelStep
	default:
		return m, nil
	}
	m.clampDetailScroll()
	return m, nil
}

// rowAt maps a list-box-local Y to a data-row index. Y 0 is the top border, and each data row
// spans rowHeight lines, so a click on either line resolves to it.
func rowAt(localY, top, n, vis int) (int, bool) {
	bodyY := localY - 1 - listPadY // 0 = header row
	dataY := bodyY - 1             // 0 = first data row's blank separator
	if dataY < 0 {
		return 0, false
	}
	i := dataY / rowHeight
	if i < 0 || i >= vis {
		return 0, false
	}
	idx := top + i
	if idx < 0 || idx >= n {
		return 0, false
	}
	return idx, true
}

func (m Model) selectedType() (domain.IssueTypeSummary, bool) {
	if m.typeCursor < 0 || m.typeCursor >= len(m.types) {
		return domain.IssueTypeSummary{}, false
	}
	return m.types[m.typeCursor], true
}

func (m Model) selectedWorkflow() (domain.WorkflowSummary, bool) {
	if m.wfCursor < 0 || m.wfCursor >= len(m.workflows) {
		return domain.WorkflowSummary{}, false
	}
	return m.workflows[m.wfCursor], true
}

func (m Model) selectionKey() string {
	if m.kind == selWorkflow {
		if w, ok := m.selectedWorkflow(); ok {
			return fmt.Sprintf("wf:%d", w.ID)
		}
		return ""
	}
	if t, ok := m.selectedType(); ok {
		return fmt.Sprintf("type:%d", t.ID)
	}
	return ""
}

// activePane is the list feeding Details: the focused list, or the one the selection came from.
func (m Model) activePane() int {
	if m.focus == paneDetail {
		if m.kind == selWorkflow {
			return paneWorkflows
		}
		return paneTypes
	}
	return m.focus
}

func (m Model) View() string {
	if m.width == 0 {
		return ""
	}
	s := m.deps.Styles
	if m.width < minWidth || m.height < minHeight {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Muted.Render("Terminal too small"))
	}
	if m.loading {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Muted.Render("Loading catalogs…"))
	}
	if m.err != nil {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Error.Render("Failed to load catalogs."))
	}
	view := m.dashboard()
	switch {
	case m.editing:
		return m.overlayModal(view, m.edit.View())
	case m.fieldEditing:
		return m.overlayModal(view, m.field.View())
	case m.creatingField:
		return m.overlayModal(view, m.cfield.View())
	case m.confirming:
		return m.overlayModal(view, m.confirm.View())
	case m.optionsEditing:
		return m.overlayModal(view, m.options.View())
	case m.creatingWorkflow:
		return m.overlayModal(view, m.cworkflow.View())
	case m.creatingType:
		return m.overlayModal(view, m.ctype.View())
	case m.guardsEditing:
		return m.overlayModal(view, m.guards.View())
	case m.vcsEditing:
		return m.overlayModal(view, m.vcs.View())
	case m.flowEditing && m.flow.hasOverlay():
		// the structure editor lives in the Details panel — only its sub-forms float over it
		return m.overlayModal(view, m.flow.View())
	}
	return view
}

// dashboard lays the lists and Details side by side, or stacked on a narrow-and-tall terminal.
func (m Model) dashboard() string {
	if m.stacked() {
		stack := lipgloss.JoinVertical(lipgloss.Left, m.detailPanel(), m.leftColumn())
		return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, stack)
	}
	dash := lipgloss.JoinHorizontal(lipgloss.Top, m.leftColumn(), " ", m.detailPanel())
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, dash)
}

// overlayModal centers a modal over a dimmed dashboard, splicing it in by hand so the modal's
// click zones survive. A modal taller than the terminal is windowed with a scrollbar.
func (m Model) overlayModal(backdrop, modal string) string {
	bd := stripANSI(lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Top, backdrop))
	t := m.deps.Styles.Theme
	modal, _, _ = components.ScrollBox(modal, m.height, m.modalScroll, t.Primary, t.Border)
	mx := max(0, (m.width-lipgloss.Width(modal))/2)
	my := max(0, (m.height-lipgloss.Height(modal))/2)
	return overlayDim(bd, modal, mx, my, m.deps.Styles.Theme.Muted)
}

// activeModalView mirrors View's dispatch (a flow sub-form only floats while it has an overlay).
// false lets the wheel handler drive the dashboard instead of a modal window.
func (m Model) activeModalView() (string, bool) {
	switch {
	case m.editing:
		return m.edit.View(), true
	case m.fieldEditing:
		return m.field.View(), true
	case m.creatingField:
		return m.cfield.View(), true
	case m.confirming:
		return m.confirm.View(), true
	case m.optionsEditing:
		return m.options.View(), true
	case m.creatingWorkflow:
		return m.cworkflow.View(), true
	case m.creatingType:
		return m.ctype.View(), true
	case m.guardsEditing:
		return m.guards.View(), true
	case m.vcsEditing:
		return m.vcs.View(), true
	case m.flowEditing && m.flow.hasOverlay():
		return m.flow.View(), true
	}
	return "", false
}

// scrollModalBy moves the modal window offset, clamped to its scrollable range.
func (m Model) scrollModalBy(view string, delta int) Model {
	maxOff := max(0, lipgloss.Height(view)-m.height)
	m.modalScroll = min(max(m.modalScroll+delta, 0), maxOff)
	return m
}

// routeModal forwards a message to whichever modal is open, mirroring View's dispatch order.
// handled=false means no modal is floating and the event belongs to the dashboard.
func (m Model) routeModal(msg tea.Msg) (Model, tea.Cmd, bool) {
	switch {
	case m.editing:
		mm, cmd := m.updateEdit(msg)
		return mm, cmd, true
	case m.fieldEditing:
		mm, cmd := m.updateField(msg)
		return mm, cmd, true
	case m.creatingField:
		mm, cmd := m.updateCreateField(msg)
		return mm, cmd, true
	case m.confirming:
		mm, cmd := m.updateConfirm(msg)
		return mm, cmd, true
	case m.optionsEditing:
		mm, cmd := m.updateOptions(msg)
		return mm, cmd, true
	case m.creatingWorkflow:
		mm, cmd := m.updateCreateWorkflow(msg)
		return mm, cmd, true
	case m.creatingType:
		mm, cmd := m.updateCreateType(msg)
		return mm, cmd, true
	case m.guardsEditing:
		mm, cmd := m.updateGuards(msg)
		return mm, cmd, true
	case m.vcsEditing:
		mm, cmd := m.updateVcs(msg)
		return mm, cmd, true
	case m.flowEditing:
		mm, cmd := m.updateFlow(msg)
		return mm, cmd, true
	}
	return m, nil, false
}

// modalFocusRower is implemented by tall form modals that can report their focused control's row
// and height, so a windowed modal can scroll to it. Short modals get wheel-only scrolling.
type modalFocusRower interface {
	FocusRow() (row, height int, ok bool)
}

func (m Model) activeModalFocus() (row, height int, ok bool) {
	var f modalFocusRower
	switch {
	case m.creatingField:
		f = m.cfield
	case m.fieldEditing:
		f = m.field
	case m.editing:
		f = m.edit
	case m.creatingType:
		f = m.ctype
	case m.creatingWorkflow:
		f = m.cworkflow
	default:
		return 0, 0, false
	}
	return f.FocusRow()
}

// followModalFocus scrolls a windowed modal so the focused control sits inside the window. No-op
// when no modal reports a focus row or the modal already fits.
func (m Model) followModalFocus() Model {
	row, height, ok := m.activeModalFocus()
	if !ok {
		return m
	}
	view, vok := m.activeModalView()
	if !vok {
		return m
	}
	boxH := lipgloss.Height(view)
	if boxH <= m.height {
		return m
	}
	// ScrollBox shows interior box-lines [off+1, off+visible] — land the control flush at the near edge
	visible := m.height - 2
	off := m.modalScroll
	top, bottom := row, row+max(1, height)-1
	if top < 1+off {
		off = top - 1 // reveal the control's top flush at the first visible line
	} else if bottom > off+visible {
		off = bottom - visible // reveal its bottom flush at the last visible line
	}
	m.modalScroll = min(max(off, 0), boxH-m.height)
	return m
}

// panelWidths splits the dashboard 3:2, lists left and Details right, matching the Projects tab.
func (m Model) panelWidths() (left, right int) {
	if m.stacked() {
		full := m.width - 2*hInset
		return full, full
	}
	usable := m.width - 2*hInset - colGap
	left = usable * 3 / 5
	right = usable - left
	return left, right
}

const (
	// below this width, a tall-enough terminal stacks Details above the lists
	stackBelowW = 100
	stackMinH   = 24
)

func (m Model) stacked() bool {
	return components.StackVertically(m.width, m.height, stackBelowW, stackMinH)
}

// stackDetailH is the Details' top slice when stacked.
func (m Model) stackDetailH() int { return min(max(m.height*9/20, 8), m.height-8) }

// colHeight is the lists' height: full, or the bottom slice when stacked.
func (m Model) colHeight() int {
	if m.stacked() {
		return m.height - m.stackDetailH()
	}
	return m.height
}

// detailHeight is the full height side by side, or the top slice when stacked.
func (m Model) detailHeight() int {
	if m.stacked() {
		return m.stackDetailH()
	}
	return m.height
}

// typesHeight/workflowHeight split the catalog-list column 1:1.
func (m Model) typesHeight() int    { return m.colHeight() / 2 }
func (m Model) workflowHeight() int { return m.colHeight() - m.typesHeight() }

// typesInnerH / wfInnerH are the visible two-line data rows in each list box. They differ by one
// when m.height is odd, so rendering and hit-testing must use the same one or clicks misresolve.
func (m Model) typesInnerH() int { return listVis(m.typesHeight()) }

func (m Model) wfInnerH() int { return listVis(m.workflowHeight()) }

// listBodyH is the box height less its two borders and padBody's vertical padding.
func listBodyH(boxH int) int { return max(1, boxH-2-2*listPadY) }

// listVis is how many two-line data rows fit under the header inside listBodyH.
func listVis(boxH int) int { return max(0, (listBodyH(boxH)-1)/rowHeight) }

// listContentW is the row width: the box less borders, TitledBox's inset, and padding.
func (m Model) listContentW() int {
	leftW, _ := m.panelWidths()
	return max(1, leftW-4-2*listPadX)
}

func (m Model) typeListTop() int {
	return listTop(m.typeCursor, m.typesInnerH(), len(m.types))
}

func (m Model) wfListTop() int {
	return listTop(m.wfCursor, m.wfInnerH(), len(m.workflows))
}

func listTop(cursor, innerH, n int) int {
	top := 0
	if cursor >= innerH {
		top = cursor - innerH + 1
	}
	if maxTop := n - innerH; top > maxTop {
		top = maxTop
	}
	if top < 0 {
		top = 0
	}
	return top
}

func (m Model) leftColumn() string {
	t := m.deps.Styles.Theme

	typesBorder := t.Primary
	if m.focus == paneTypes {
		typesBorder = t.Accent
	}
	wfBorder := t.Primary
	if m.focus == paneWorkflows {
		wfBorder = t.Accent
	}

	contentW := m.listContentW()
	typesBody := m.tableView(typeCols(contentW), m.typeRow, m.typeCursor, m.hoverRowFor(paneTypes), len(m.types), m.typesHeight(), contentW, m.activePane() == paneTypes)
	wfBody := m.tableView(workflowCols(contentW), m.workflowRow, m.wfCursor, m.hoverRowFor(paneWorkflows), len(m.workflows), m.workflowHeight(), contentW, m.activePane() == paneWorkflows)

	typesBox := zone.Mark("schema.types", components.TitledRule(fmt.Sprintf("Issue Types (%d)", len(m.types)), "", typesBody, typesBorder))
	wfBox := zone.Mark("schema.workflows", components.TitledRule(fmt.Sprintf("Workflows (%d)", len(m.workflows)), "", wfBody, wfBorder))
	return lipgloss.JoinVertical(lipgloss.Left, typesBox, wfBox)
}

// tcol is a table column. width 0 marks the single flex column, capped by max, with the surplus
// handed to the grow column so a wide panel widens Workflow rather than stretching Name.
type tcol struct {
	title string
	width int
	max   int  // flex column only: cap its width
	grow  bool // absorbs the capped flex column's surplus
}

// cell kinds: styled text, a name with an optional color swatch, or a hierarchy badge.
const (
	cellText = iota
	cellName
	cellChip
)

type tcell struct {
	kind  int
	text  string         // cellText/cellName: the text. cellChip: the hierarchy value
	color string         // cellName: swatch color name, "" for none
	style lipgloss.Style // cellText/cellName: the non-banded text style
}

func typeCols(contentW int) []tcol {
	return fitColumns([]tcol{
		{title: "Name", width: 0, max: 18},
		{title: "Hierarchy", width: 14},
		{title: "Workflow", width: 14, grow: true},
		{title: "System", width: 6},
	}, contentW)
}

func workflowCols(contentW int) []tcol {
	return fitColumns([]tcol{
		{title: "Name", width: 18},
		{title: "Description", width: 0},
		{title: "System", width: 6},
	}, contentW)
}

func (m Model) typeRow(i int) []tcell {
	s := m.deps.Styles
	it := m.types[i]
	sys := "-"
	if it.SystemProvided {
		sys = "Yes"
	}
	name := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true)
	return []tcell{
		{kind: cellName, text: it.Name, color: it.Color, style: name},
		{kind: cellChip, text: it.Hierarchy},
		{kind: cellText, text: orDash(it.WorkflowName), style: s.Muted},
		{kind: cellText, text: sys, style: s.Muted},
	}
}

func (m Model) workflowRow(i int) []tcell {
	s := m.deps.Styles
	w := m.workflows[i]
	sys := "-"
	if w.SystemProvided {
		sys = "Yes"
	}
	name := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true)
	return []tcell{
		{kind: cellText, text: w.Name, style: name},
		{kind: cellText, text: orDash(w.Description), style: s.Muted},
		{kind: cellText, text: sys, style: s.Muted},
	}
}

// flexWidth is what is left for the flex column after the fixed columns and the gutters.
func flexWidth(cols []tcol, contentW int) int {
	fixed := 0
	for _, c := range cols {
		fixed += c.width
	}
	w := contentW - fixed - 2*(len(cols)-1)
	if w < 0 {
		w = 0
	}
	return w
}

// fitColumns drops trailing fixed columns (never the first, never the flex) until the flex column
// has at least nameMinW cells, so a narrow panel sheds columns rather than crushing the name.
func fitColumns(cols []tcol, contentW int) []tcol {
	for flexWidth(cols, contentW) < nameMinW {
		drop := -1
		for j := len(cols) - 1; j >= 1; j-- {
			if cols[j].width > 0 {
				drop = j
				break
			}
		}
		if drop < 0 {
			break
		}
		cols = append(cols[:drop], cols[drop+1:]...)
	}
	return cols
}

func colWidths(cols []tcol, contentW int) []int {
	flex := flexWidth(cols, contentW)
	grow := -1
	for i, c := range cols {
		if c.grow {
			grow = i
		}
	}
	surplus := 0
	ws := make([]int, len(cols))
	for i, c := range cols {
		if c.width != 0 {
			ws[i] = c.width
			continue
		}
		w := flex
		// cap the flex column and give the surplus to grow. Without a grow column the flex keeps it.
		if c.max > 0 && grow >= 0 && w > c.max {
			surplus = w - c.max
			w = c.max
		}
		ws[i] = w
	}
	if grow >= 0 {
		ws[grow] += surplus
	}
	return ws
}

// padTo left-aligns a rendered cell in a w-wide column.
func padTo(s string, w int) string {
	gap := w - lipgloss.Width(s)
	if gap < 0 {
		gap = 0
	}
	return s + strings.Repeat(" ", gap)
}

// renderCell clips a cell to its column. A banded row draws plain text so the band survives
// inner color resets.
func renderCell(c tcell, w int, banded bool) string {
	switch c.kind {
	case cellChip:
		if banded {
			return padTo(trunc(" "+c.text+" ", w), w)
		}
		return padTo(components.HierarchyChip(c.text), w)
	case cellName:
		prefix := ""
		if components.ColorSwatch(c.color) != "" {
			prefix = "██ "
		}
		txt := trunc(c.text, max(1, w-lipgloss.Width(prefix)))
		if banded {
			return padTo(prefix+txt, w)
		}
		styled := c.style.Render(txt)
		if prefix != "" {
			styled = components.ColorSwatch(c.color) + " " + styled
		}
		return padTo(styled, w)
	default:
		txt := trunc(c.text, w)
		if banded {
			return padTo(txt, w)
		}
		return padTo(c.style.Render(txt), w)
	}
}

// renderRow lays cells into columns with a two-space gutter. A banded row is wrapped whole.
func (m Model) renderRow(cells []tcell, cols []tcol, contentW int, banded bool, band lipgloss.Style) string {
	widths := colWidths(cols, contentW)
	parts := make([]string, len(cols))
	for i := range cols {
		parts[i] = renderCell(cells[i], widths[i], banded)
	}
	line := fitLine(strings.Join(parts, "  "), contentW)
	if banded {
		return band.Render(line)
	}
	return line
}

func (m Model) tableHeader(cols []tcol, contentW int) string {
	hdr := lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Text).Bold(true)
	widths := colWidths(cols, contentW)
	parts := make([]string, len(cols))
	for i, c := range cols {
		parts[i] = padTo(hdr.Render(trunc(c.title, widths[i])), widths[i])
	}
	return fitLine(strings.Join(parts, "  "), contentW)
}

func (m Model) selBand() lipgloss.Style {
	t := m.deps.Styles.Theme
	return lipgloss.NewStyle().Foreground(t.Text).Background(t.Selection).Bold(true)
}

// hoverBand highlights a hovered row. The ANSI theme has no background, so it tints the text.
func (m Model) hoverBand() lipgloss.Style {
	t := m.deps.Styles.Theme
	bg, ok := hoverBg(t)
	if !ok {
		return lipgloss.NewStyle().Foreground(t.Secondary)
	}
	return lipgloss.NewStyle().Foreground(t.Text).Background(bg)
}

// tableView renders a header row, then a window of airy data rows, selected/hovered rows banded.
func (m Model) tableView(cols []tcol, build func(int) []tcell, cursor, hoverRow, n, boxH, contentW int, highlight bool) string {
	s := m.deps.Styles
	if contentW < 1 {
		contentW = 1
	}
	bodyH := listBodyH(boxH)
	vis := listVis(boxH)
	blank := strings.Repeat(" ", contentW)
	lines := make([]string, 0, bodyH)
	lines = append(lines, m.tableHeader(cols, contentW))
	if n == 0 {
		lines = append(lines, "", fitLine(s.Muted.Render("None."), contentW))
	} else {
		top := listTop(cursor, vis, n)
		for i := 0; i < vis; i++ {
			lines = append(lines, "") // the airy blank separator above each row
			idx := top + i
			if idx >= n {
				lines = append(lines, blank)
				continue
			}
			sel := highlight && idx == cursor
			hov := idx == hoverRow && idx != cursor
			var band lipgloss.Style
			switch {
			case sel:
				band = m.selBand()
			case hov:
				band = m.hoverBand()
			}
			lines = append(lines, m.renderRow(build(idx), cols, contentW, sel || hov, band))
		}
	}
	for len(lines) < bodyH {
		lines = append(lines, blank)
	}
	return padBody(lines[:bodyH], contentW)
}

// padBody frames the rows with listPadX columns and listPadY blank rows.
func padBody(lines []string, contentW int) string {
	padX := strings.Repeat(" ", listPadX)
	blank := strings.Repeat(" ", contentW+2*listPadX)
	out := make([]string, 0, len(lines)+2*listPadY)
	for i := 0; i < listPadY; i++ {
		out = append(out, blank)
	}
	for _, ln := range lines {
		out = append(out, padX+ln+padX)
	}
	for i := 0; i < listPadY; i++ {
		out = append(out, blank)
	}
	return strings.Join(out, "\n")
}

func (m Model) detailPanel() string {
	_, rightW := m.panelWidths()
	t := m.deps.Styles.Theme
	totalRows := max(1, m.detailHeight()-2)
	viewH := m.detailViewHeight()
	contentW := m.detailContentW()

	lines := m.detailContent()
	voff := min(max(m.detailScroll, 0), max(0, len(lines)-viewH))
	bar := scrollbar(voff, len(lines), viewH, t.Primary, t.Muted)

	blank := strings.Repeat(" ", contentW)
	padL := strings.Repeat(" ", detailInsetL)
	padR := strings.Repeat(" ", detailInsetR)
	rows := make([]string, totalRows)
	for i := range rows {
		line, barCell := blank, " "
		if i < viewH {
			if di := voff + i; di < len(lines) {
				line = lines[di] // already padded to exactly contentW
			}
			barCell = bar[i]
		}
		rows[i] = padL + line + padR + barCell
	}

	border := t.Primary
	focused := m.focus == paneDetail
	if focused {
		border = t.Accent
	}
	top := ruleWithTitle("Details", rightW, border)
	bottom := lipgloss.NewStyle().Foreground(border).Render(strings.Repeat("─", rightW))
	panel := lipgloss.JoinVertical(lipgloss.Left, top, strings.Join(rows, "\n"), bottom)
	return zone.Mark("schema.detail", panel)
}

// detailContent is the Details body as lines, each padded to exactly contentW so the border and
// scrollbar stay aligned.
func (m Model) detailContent() []string {
	contentW := m.detailContentW()
	return strings.Split(lipgloss.NewStyle().Width(contentW).Render(m.detailBody()), "\n")
}

func (m Model) detailContentW() int {
	_, rightW := m.panelWidths()
	return max(1, rightW-detailInsetL-detailInsetR-detailScrollbar)
}

func (m Model) detailViewHeight() int { return max(1, m.detailHeight()-2-detailPadBottom) }

func (m Model) detailScrollMax() int {
	return max(0, len(m.detailContent())-m.detailViewHeight())
}

func (m *Model) clampDetailScroll() {
	m.detailScroll = min(max(m.detailScroll, 0), m.detailScrollMax())
}

// detailBody is the Details content, with a blank line under the top rule.
func (m Model) detailBody() string {
	return "\n" + m.detail()
}

func (m Model) detail() string {
	s := m.deps.Styles
	if m.kind == selWorkflow {
		w, ok := m.selectedWorkflow()
		if !ok {
			return s.Muted.Render("No workflow selected.")
		}
		return m.workflowDetail(w)
	}
	t, ok := m.selectedType()
	if !ok {
		return s.Muted.Render("No issue type selected.")
	}
	return m.issueTypeDetail(t)
}

func (m Model) issueTypeDetail(t domain.IssueTypeSummary) string {
	lines, _ := m.issueTypeDetailLines(t)
	return lipgloss.JoinVertical(lipgloss.Left, lines...)
}

// issueTypeDetailLines builds the issue type Details as lines plus the panel row each selectable
// element starts on. Rows are offset by one for detailBody's leading blank line.
func (m Model) issueTypeDetailLines(t domain.IssueTypeSummary) ([]string, map[wfElem]int) {
	s := m.deps.Styles
	g := m.deps.Glyphs
	contentW := m.detailContentW()
	rows := map[wfElem]int{}

	hierarchy := orDash(t.Hierarchy)
	if chip := components.HierarchyChip(t.Hierarchy); chip != "" {
		hierarchy = chip
	}
	metaSel := m.typeElemSelected(wfElem{elemTypeMeta, 0})
	rows[wfElem{elemTypeMeta, 0}] = 1 // first content line, after detailBody's leading blank
	lines := []string{
		m.titleWithEdit(t.Name, metaSel, contentW),
		"",
		metaRow(s, g.Or(g.Hierarchy, ""), "Hierarchy", hierarchy),
		metaRow(s, g.Or(g.Palette, ""), "Color", m.colorValue(t.Color)),
		metaRow(s, g.Or(g.Computer, ""), "System", yesNo(t.SystemProvided)),
		"",
		metaRow(s, g.Or(g.TransitionConnection, ""), "Workflow", orDash(t.WorkflowName)),
	}
	lines = append(lines, "", sectionRule(s, "Fields", contentW), "")
	lines = append(lines, m.fieldLines(t.ID, contentW, rows, len(lines))...)
	// the "+ Field" handle follows the fields once loaded, matching typeElems
	if _, loaded := m.typeDetail[t.ID]; loaded {
		lines = append(lines, "")
		rows[wfElem{elemAddField, 0}] = len(lines) + 1 // +1 for detailBody's leading blank line
		lines = append(lines, m.addFieldAffordance())
	}
	// close the fields area with a rule before the description
	lines = append(lines, "", closingRule(s, contentW), "")
	if t.Description != "" {
		lines = append(lines, t.Description)
	} else {
		lines = append(lines, s.Muted.Render("No description."))
	}
	return lines, rows
}

// titleWithEdit puts the title left and its edit pen right, accented while the block is selected.
func (m Model) titleWithEdit(name string, focused bool, contentW int) string {
	s := m.deps.Styles
	style := s.Title
	if focused {
		style = lipgloss.NewStyle().Foreground(s.Theme.Accent).Bold(true)
	}
	title := style.Render(name)
	if !m.deps.Mouse {
		return title // no Edit pen when the mouse is off (the focused block's e/enter still edits)
	}
	return rightAlignAction(title, m.typeEditButton(focused), contentW)
}

func (m Model) typeEditButton(focused bool) string {
	pen := m.deps.Glyphs.Or(m.deps.Glyphs.PenSquare, "Edit")
	return zone.Mark("schema.type.edit", m.penStyle("schema.type.edit", focused).Render(" "+pen+" "))
}

// workflowTitleWithEdit mirrors the issue type header: name/description are edited from the title.
func (m Model) workflowTitleWithEdit(name string, focused bool, contentW int) string {
	s := m.deps.Styles
	style := s.Title
	if focused {
		style = lipgloss.NewStyle().Foreground(s.Theme.Accent).Bold(true)
	}
	title := style.Render(name)
	if !m.deps.Mouse {
		return title // no Edit pen when the mouse is off (the focused block's e/enter still edits)
	}
	return rightAlignAction(title, m.wfEditButton(focused), contentW)
}

func (m Model) wfEditButton(focused bool) string {
	pen := m.deps.Glyphs.Or(m.deps.Glyphs.PenSquare, "Edit")
	return zone.Mark("schema.wf.edit", m.penStyle("schema.wf.edit", focused).Render(" "+pen+" "))
}

// typeElemSelected reports whether e is the selected element while Details is focused.
func (m Model) typeElemSelected(e wfElem) bool {
	if m.focus != paneDetail || m.kind != selType {
		return false
	}
	cur, ok := m.selectedTypeElem()
	return ok && cur == e
}

// penStyle colors a pen: Accent when selected, Secondary while hovered, Primary at rest.
func (m Model) penStyle(zoneID string, focused bool) lipgloss.Style {
	t := m.deps.Styles.Theme
	col := t.Primary
	switch {
	case focused:
		col = t.Accent
	case m.hoverAction == zoneID:
		col = t.Secondary
	}
	return lipgloss.NewStyle().Foreground(col).Bold(true)
}

// rightAlignAction is built by hand (not alignRow) so the action's click zone survives unclipped.
func rightAlignAction(left, action string, contentW int) string {
	aw := lipgloss.Width(action)
	left = trunc(left, max(1, contentW-aw-1))
	gap := max(1, contentW-lipgloss.Width(left)-aw)
	return left + strings.Repeat(" ", gap) + action
}

// colorValue is a swatch plus the color name, or a dash when unset.
func (m Model) colorValue(colorName string) string {
	swatch := components.ColorSwatch(colorName)
	if swatch == "" {
		return orDash("")
	}
	return swatch + " " + m.deps.Styles.Muted.Render(components.ColorLabel(colorName))
}

// fieldGlyph is empty on plain terminals, so they show the bare type tag.
func (m Model) fieldGlyph(fieldType string) string {
	return fieldTypeGlyph(m.deps.Glyphs, fieldType)
}

// fieldTags renders "<glyph> TYPE" padded to a common width, dropping the padding when narrow.
func (m Model) fieldTags(fields []domain.IssueField, contentW int) []string {
	tags := make([]string, len(fields))
	widest := 0
	for i, f := range fields {
		if f.Type == "" {
			continue
		}
		tags[i] = f.Type
		if gl := m.fieldGlyph(f.Type); gl != "" {
			tags[i] = gl + " " + f.Type
		}
		widest = max(widest, lipgloss.Width(tags[i]))
	}
	if widest*3 > contentW {
		return tags
	}
	for i, tag := range tags {
		if tag != "" {
			tags[i] = tag + strings.Repeat(" ", widest-lipgloss.Width(tag))
		}
	}
	return tags
}

// fieldLines renders each field as a header row (type tag, name, required, pen) with its
// description and options indented below. Header rows are recorded in rows for scroll-into-view.
func (m Model) fieldLines(typeID, contentW int, rows map[wfElem]int, base int) []string {
	s := m.deps.Styles
	if m.detailPending[typeID] {
		return []string{s.Muted.Render("Loading fields…")}
	}
	if m.detailFailed[typeID] {
		return []string{s.Muted.Render("Fields unavailable.")}
	}
	d, ok := m.typeDetail[typeID]
	if !ok {
		return []string{s.Muted.Render("Loading fields…")}
	}
	if len(d.Fields) == 0 {
		return []string{s.Muted.Render("No custom fields.")}
	}
	name := lipgloss.NewStyle().Foreground(s.Theme.Text).Bold(true)
	accent := lipgloss.NewStyle().Foreground(s.Theme.Accent).Bold(true)
	tags := m.fieldTags(d.Fields, contentW)
	var out []string
	for i, f := range d.Fields {
		if i > 0 {
			out = append(out, "")
		}
		sel := m.typeElemSelected(wfElem{elemField, f.ID})
		nameStyle := name
		if sel {
			nameStyle = accent
		}
		head := nameStyle.Render(f.Name)
		nameIndent := 0 // the column where the name starts, so its detail lines hang under it
		if tags[i] != "" {
			head = s.Muted.Render(tags[i]) + "  " + head
			nameIndent = lipgloss.Width(tags[i]) + 2
		}
		if f.Required {
			head += s.Muted.Render(" · required")
		}
		rows[wfElem{elemField, f.ID}] = base + len(out) + 1 // +1 for detailBody's leading blank
		out = append(out, rightAlignAction(head, m.fieldEditButton(f.ID, sel), contentW))
		// description and options hang under the name. Each emitted line already fits contentW, so
		// the panel's own Width wrap (which would shift the rows below) never triggers.
		if f.Description != "" {
			out = append(out, indentedWrap(s.Muted, f.Description, nameIndent, contentW)...)
		}
		if len(f.Options) > 0 {
			out = append(out, indentedWrap(s.Muted, "options: "+strings.Join(optionNames(f.Options), ", "), nameIndent, contentW)...)
		}
	}
	return out
}

// indentedWrap wraps text to hang under a label, every continuation at the same indent. Each line
// is at most contentW cells, so the Details panel never re-wraps it.
func indentedWrap(style lipgloss.Style, text string, indent, contentW int) []string {
	if indent > contentW-1 {
		indent = max(0, contentW-1)
	}
	avail := max(1, contentW-indent)
	pad := strings.Repeat(" ", indent)
	wrapped := style.Width(avail).Render(flatten(text))
	lines := strings.Split(wrapped, "\n")
	out := make([]string, len(lines))
	for i, ln := range lines {
		out[i] = pad + ln
	}
	return out
}

func (m Model) fieldEditButton(fieldID int, focused bool) string {
	if !m.deps.Mouse {
		return "" // no per-field edit pen when the mouse is off (the focused field's e/enter still edits)
	}
	zoneID := fieldEditZone(fieldID)
	pen := m.deps.Glyphs.Or(m.deps.Glyphs.PenSquare, "edit")
	return zone.Mark(zoneID, m.penStyle(zoneID, focused).Render(" "+pen+" "))
}

func fieldEditZone(id int) string { return "schema.field.edit." + itoa(id) }

// addFieldAffordance uses a plain "+" (no glyph) so its width is terminal-independent.
func (m Model) addFieldAffordance() string {
	sel := m.typeElemSelected(wfElem{elemAddField, 0})
	t := m.deps.Styles.Theme
	col, bold := t.Secondary, false
	switch {
	case sel:
		col, bold = t.Accent, true
	case m.hoverAction == "schema.field.new":
		col = t.Accent
	}
	return zone.Mark("schema.field.new", lipgloss.NewStyle().Foreground(col).Bold(bold).Render("+ Field"))
}

func optionNames(opts []domain.FieldOption) []string {
	names := make([]string, len(opts))
	for i, o := range opts {
		names[i] = o.Name
	}
	return names
}

// workflowHeader is the title + metadata shown above the graph, as individual lines.
func (m Model) workflowHeader(w domain.WorkflowSummary, contentW int) []string {
	s := m.deps.Styles
	g := m.deps.Glyphs
	rows := []string{
		m.workflowTitleWithEdit(w.Name, m.actionFocused(elemWfMeta), contentW),
		"",
		metaRow(s, g.Or(g.Computer, ""), "System", yesNo(w.SystemProvided)),
		"",
	}
	if w.Description != "" {
		rows = append(rows, strings.Split(w.Description, "\n")...)
	} else {
		rows = append(rows, s.Muted.Render("No description."))
	}
	return rows
}

// vcsLines shows which transition auto-fires when a linked PR is opened or merged.
func (m Model) vcsLines(d domain.WorkflowDetail, contentW int) []string {
	s := m.deps.Styles
	pr := m.deps.Glyphs.Or(m.deps.Glyphs.PullRequest, "")
	valW := max(1, contentW-metaLabelW)
	return []string{
		metaRow(s, pr, "PR opened", m.transitionLabel(d, d.VcsPrOpenedTransitionID, valW)),
		metaRow(s, pr, "PR merged", m.transitionLabel(d, d.VcsPrMergedTransitionID, valW)),
	}
}

// transitionLabel resolves a transition id to its name, or a muted dash when unset.
func (m Model) transitionLabel(d domain.WorkflowDetail, id, width int) string {
	if id != 0 {
		for _, tr := range d.Transitions {
			if tr.ID == id {
				return trunc(tr.Label, width)
			}
		}
	}
	return m.deps.Styles.Muted.Render("-")
}

// workflowDetail is the header, a Flow rule, and the diagram (or a fallback while it loads).
func (m Model) workflowDetail(w domain.WorkflowSummary) string {
	lines, _ := m.workflowDetailLines(w)
	return lipgloss.JoinVertical(lipgloss.Left, lines...)
}

// workflowDetailLines builds the workflow Details as lines (the header is pre-wrapped so the outer
// render never re-wraps and shifts row indices) plus each graph element's panel row.
func (m Model) workflowDetailLines(w domain.WorkflowSummary) ([]string, map[wfElem]int) {
	if m.flowEditing {
		return m.flowEditorLines(w, m.detailContentW())
	}
	s := m.deps.Styles
	contentW := m.detailContentW()
	// name/description are edited from the title pen, not these section rules
	const editLabel = "Edit"
	header := strings.Split(lipgloss.NewStyle().Width(contentW).Render(
		strings.Join(m.workflowHeader(w, contentW), "\n")), "\n")
	lines := append([]string{}, header...)
	rows := map[wfElem]int{}
	// the title (with its Edit pen) sits at the top, so selecting it scrolls the panel back up
	rows[wfElem{elemWfMeta, 0}] = 1 // +1 for detailBody's leading blank line
	// show VCS once the graph is loaded, so transition ids resolve to names
	if d, ok := m.wfDetail[w.ID]; ok {
		lines = append(lines, "")
		rows[wfElem{elemVcsEdit, 0}] = len(lines) + 1 // +1 for detailBody's leading blank line
		lines = append(lines, sectionRuleAction(s, "VCS Automation", editLabel, "schema.vcs.edit", contentW, m.hoverAction == "schema.vcs.edit", m.actionFocused(elemVcsEdit)), "")
		lines = append(lines, m.vcsLines(d, contentW)...)
	}
	flowRule := sectionRule(s, "Flow", contentW)
	if _, ok := m.wfDetail[w.ID]; ok {
		flowRule = sectionRuleAction(s, "Flow", editLabel, "schema.flow.edit", contentW, m.hoverAction == "schema.flow.edit", m.actionFocused(elemFlowEdit))
	}
	lines = append(lines, "")
	if _, ok := m.wfDetail[w.ID]; ok {
		rows[wfElem{elemFlowEdit, 0}] = len(lines) + 1 // +1 for detailBody's leading blank line
	}
	lines = append(lines, flowRule, "")
	switch {
	case m.wfPending[w.ID]:
		lines = append(lines, s.Muted.Render("Loading graph…"))
	case m.wfFailed[w.ID]:
		lines = append(lines, s.Muted.Render("Graph unavailable."))
	default:
		d, ok := m.wfDetail[w.ID]
		if !ok {
			lines = append(lines, s.Muted.Render("Loading graph…"))
			break
		}
		sel := wfElem{}
		if m.focus == paneDetail {
			sel, _ = m.selectedElem()
		}
		graphStart := len(lines)
		g, grows, _ := renderWorkflowGraph(d, s, contentW, sel, m.wfHover, true)
		lines = append(lines, g...)
		for e, r := range grows {
			rows[e] = graphStart + r + 1 // +1 for detailBody's leading blank line
		}
	}
	return lines, rows
}

// flowEditorLines renders the in-place structure editor into the Details panel: title, live draft
// diagram, action bar, status. The map gives each draft element's panel row for scroll-into-view.
func (m Model) flowEditorLines(w domain.WorkflowSummary, contentW int) ([]string, map[wfElem]int) {
	s := m.deps.Styles
	f := m.flow
	rows := map[wfElem]int{}
	head := strings.Split(lipgloss.NewStyle().Width(contentW).Render(lipgloss.JoinVertical(lipgloss.Left,
		s.Title.Render(w.Name),
		lipgloss.NewStyle().Foreground(s.Theme.Accent).Bold(true).Render("Editing structure"),
	)), "\n")
	lines := append([]string{}, head...)
	// the action bar sits above the diagram so it is visible before the tall graph scrolls it off
	lines = append(lines, "")
	lines = append(lines, f.actionBar(contentW)...)
	lines = append(lines, f.statusBlock(contentW)...)
	lines = append(lines, "")
	sel, _ := f.selElem()
	g, grows, _ := renderWorkflowGraph(f.draftDetail(), s, contentW, sel, wfElem{}, false)
	graphStart := len(lines)
	lines = append(lines, g...)
	for e, r := range grows {
		rows[e] = graphStart + r + 1 // +1 for detailBody's leading blank line
	}
	return lines, rows
}

// revealFlowSel keeps the editor's selected draft element scrolled into view.
func (m *Model) revealFlowSel() {
	w, ok := m.selectedWorkflow()
	if !ok {
		return
	}
	sel, ok := m.flow.selElem()
	if !ok {
		// a command is focused — the action bar sits at the top
		m.detailScroll = 0
		return
	}
	_, rows := m.flowEditorLines(w, m.detailContentW())
	r, ok := rows[sel]
	if !ok {
		m.clampDetailScroll()
		return
	}
	viewH := m.detailViewHeight()
	if top := r - 1; top < m.detailScroll { // a row of margin above
		m.detailScroll = top
	}
	if bottom := r + 3; bottom > m.detailScroll+viewH { // keep the whole box in view
		m.detailScroll = bottom - viewH
	}
	m.clampDetailScroll()
}

// elemPanelRow is the Details row a graph element starts on, for scroll-into-view.
func (m Model) elemPanelRow(e wfElem) (int, bool) {
	w, ok := m.selectedWorkflow()
	if !ok {
		return 0, false
	}
	_, rows := m.workflowDetailLines(w)
	r, ok := rows[e]
	return r, ok
}

// LoadedMsg carries the catalog fetch result. The shell routes it here even while another tab is
// active, so a background load is never lost.
type LoadedMsg struct {
	Types     []domain.IssueTypeSummary
	Workflows []domain.WorkflowSummary
	Err       error
}

// TypeDetailLoadedMsg carries one issue type's fields.
type TypeDetailLoadedMsg struct {
	ID     int
	Detail domain.IssueTypeDetail
	Err    error
}

// WorkflowDetailLoadedMsg carries one workflow's state/transition graph.
type WorkflowDetailLoadedMsg struct {
	ID     int
	Detail domain.WorkflowDetail
	Err    error
}

func load(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		ctx := context.Background()
		types, err := d.Catalog.ListIssueTypes(ctx)
		if err != nil {
			return LoadedMsg{Err: err}
		}
		workflows, err := d.Catalog.ListWorkflows(ctx)
		if err != nil {
			return LoadedMsg{Err: err}
		}
		return LoadedMsg{Types: types, Workflows: workflows}
	}
}

// Retheme swaps in new deps. The screen reads the theme fresh each render, so that is enough.
func (m Model) Retheme(d deps.Deps) Model {
	m.deps = d
	return m
}

// ThemeName lets the shell verify a live theme switch propagated here.
func (m Model) ThemeName() string { return m.deps.Styles.Theme.Name }

// HelpTitle / HelpAbout describe this screen in the app-level help modal.
func (m Model) HelpTitle() string { return "Schema" }

func (m Model) HelpAbout() string {
	return "Global issue types and workflows shared by every project. Edit an item's fields, " +
		"options, and workflow graph, or create and delete them."
}

func (m Model) HelpKeys() []key.Binding {
	if m.editing {
		return m.edit.HelpKeys()
	}
	if m.fieldEditing {
		return m.field.HelpKeys()
	}
	if m.creatingField {
		return m.cfield.HelpKeys()
	}
	if m.confirming {
		return m.confirm.HelpKeys()
	}
	if m.optionsEditing {
		return m.options.HelpKeys()
	}
	if m.creatingWorkflow {
		return m.cworkflow.HelpKeys()
	}
	if m.creatingType {
		return m.ctype.HelpKeys()
	}
	if m.guardsEditing {
		return m.guards.HelpKeys()
	}
	if m.vcsEditing {
		return m.vcs.HelpKeys()
	}
	if m.flowEditing {
		return m.flow.HelpKeys()
	}
	if m.focus == paneDetail {
		binds := []key.Binding{key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "select"))}
		if m.kind == selType {
			if _, ok := m.selectedType(); ok {
				if e, ok := m.selectedTypeElem(); ok && e.kind == elemAddField {
					binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "new field")))
				} else {
					binds = append(binds, key.NewBinding(key.WithKeys("e"), key.WithHelp("e", "edit")))
				}
				if e, ok := m.selectedTypeElem(); ok && e.kind == elemField {
					binds = append(binds,
						key.NewBinding(key.WithKeys("x"), key.WithHelp("x", "delete")),
						key.NewBinding(key.WithKeys("shift+up", "shift+down"), key.WithHelp("shift+↑/↓", "reorder")))
					if m.selectedFieldHasOptions() {
						binds = append(binds, key.NewBinding(key.WithKeys("o"), key.WithHelp("o", "options")))
					}
				}
			}
		}
		if m.kind == selWorkflow {
			if e, ok := m.selectedElem(); ok {
				switch e.kind {
				case elemVcsEdit, elemFlowEdit:
					binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "edit")))
				case elemState:
					binds = append(binds, key.NewBinding(key.WithKeys("e"), key.WithHelp("e", "edit")))
				case elemTransition:
					binds = append(binds,
						key.NewBinding(key.WithKeys("e"), key.WithHelp("e", "edit")),
						key.NewBinding(key.WithKeys("g"), key.WithHelp("g", "guards")))
				}
			}
			if w, ok := m.selectedWorkflow(); ok {
				if _, loaded := m.wfDetail[w.ID]; loaded {
					binds = append(binds,
						key.NewBinding(key.WithKeys("v"), key.WithHelp("v", "vcs")),
						key.NewBinding(key.WithKeys("f"), key.WithHelp("f", "edit flow")))
				}
			}
		}
		binds = append(binds, key.NewBinding(key.WithKeys("left", "esc"), key.WithHelp("←", "back")))
		return append(binds, key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "focus")))
	}
	// a list pane: item-level editors run from here, and →/enter drills into Details
	binds := []key.Binding{
		key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "select")),
		key.NewBinding(key.WithKeys("right", "enter"), key.WithHelp("→", "details")),
		key.NewBinding(key.WithKeys("e"), key.WithHelp("e", "edit")),
	}
	if m.focus == paneTypes {
		binds = append(binds, key.NewBinding(key.WithKeys("a"), key.WithHelp("a", "add field")))
	}
	if m.focus == paneWorkflows {
		binds = append(binds,
			key.NewBinding(key.WithKeys("v"), key.WithHelp("v", "vcs")),
			key.NewBinding(key.WithKeys("f"), key.WithHelp("f", "edit flow")))
	}
	binds = append(binds,
		key.NewBinding(key.WithKeys("n"), key.WithHelp("n", "new")),
		key.NewBinding(key.WithKeys("d"), key.WithHelp("d", "delete")))
	return append(binds, key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "focus")))
}

func loadDetail(d deps.Deps, id int) tea.Cmd {
	return func() tea.Msg {
		detail, err := d.Catalog.GetIssueType(context.Background(), id)
		return TypeDetailLoadedMsg{ID: id, Detail: detail, Err: err}
	}
}

func loadWorkflow(d deps.Deps, id int) tea.Cmd {
	return func() tea.Msg {
		detail, err := d.Catalog.GetWorkflow(context.Background(), id)
		return WorkflowDetailLoadedMsg{ID: id, Detail: detail, Err: err}
	}
}
