package project

import (
	"context"
	"strconv"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

const (
	pickerMaxRows = 8
	pickerMinW    = 24
	pickerMaxW    = 56
	// transitionMaxW is wider than a plain picker's: a blocked transition lists the guard conditions it
	// fails underneath it, and those read as sentences rather than labels.
	transitionMaxW = 72
)

// openTransitionPicker builds the picker from the current issue's available transitions. Blocked
// transitions are listed too, with the guard conditions they fail spelled out underneath, so the user
// reads what is missing in the picker instead of having to select each one to be told.
func (m Model) openTransitionPicker() (Model, tea.Cmd) {
	d, ok := m.details[m.viewKey]
	if !ok || len(d.Transitions) == 0 {
		return m, toast.Show(toast.Info, "No transitions available.")
	}

	opts := make([]widgets.PickerOption, len(d.Transitions))
	w := pickerMinW
	for i, tr := range d.Transitions {
		label := tr.Label
		if tr.TargetLabel != "" {
			label = tr.Label + " → " + tr.TargetLabel
		}
		if !tr.CanExecute {
			label += "  (blocked)"
		}
		opts[i] = widgets.PickerOption{Value: strconv.FormatInt(tr.ID, 10), Label: label, Notes: blockedNotes(tr)}
		if lw := lipgloss.Width(label) + 2; lw > w {
			w = lw
		}
		// a note wraps rather than truncates, so it only widens the box up to the cap
		for _, note := range opts[i].Notes {
			if nw := lipgloss.Width(note) + 4; nw > w {
				w = nw
			}
		}
	}
	if w > transitionMaxW {
		w = transitionMaxW
	}

	m.picking = true
	m.pickKind = pickTransition
	m.pickerTransitions = d.Transitions
	m.picker = widgets.NewListPicker("Move issue", opts, "", m.pickerRows(), w)
	return m, nil
}

// updatePicker drives whichever picker is open: esc cancels, arrows move, enter selects, and a click
// selects the row it lands on. A searchable picker sends every other key to its filter; a plain one
// also takes vim-style j/k/q. The selection is routed by pickKind.
func (m Model) updatePicker(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		switch msg.String() {
		case "esc":
			m.picking = false
			return m, nil
		case "up":
			m.picker = m.picker.Move(-1)
			return m, nil
		case "down":
			m.picker = m.picker.Move(1)
			return m, nil
		case "enter":
			return m.pickSelect()
		case "space":
			if m.picker.Multi() {
				m.picker = m.picker.Toggle() // multi-select: space ticks the row under the cursor
				return m, nil
			}
			// single-select: fall through so a searchable picker types the space into its filter
		}
		if m.picker.Searchable() {
			m.picker = m.picker.Filter(msg) // any other key edits the filter
			return m, nil
		}
		switch msg.String() {
		case "q":
			m.picking = false
		case "k":
			m.picker = m.picker.Move(-1)
		case "j":
			m.picker = m.picker.Move(1)
		}
		return m, nil
	case tea.MouseClickMsg:
		if msg.Button == tea.MouseLeft {
			if i := m.picker.HitOption(msg); i >= 0 {
				m.picker.Cursor = i
				if m.picker.Multi() {
					m.picker = m.picker.Toggle() // multi-select: a click ticks the row, staying open
					return m, nil
				}
				return m.pickSelect()
			}
		}
	}
	return m, nil
}

// pickSelect applies the highlighted option according to which picker is open.
func (m Model) pickSelect() (Model, tea.Cmd) {
	switch m.pickKind {
	case pickAssignee:
		return m.selectAssignee()
	case pickParent:
		return m.selectParent()
	case pickReviewers:
		return m.confirmReviewers()
	case pickParentEdit:
		return m.selectParentEdit()
	case pickRelationType:
		return m.selectRelationType()
	case pickRelationTarget:
		return m.selectRelationTarget()
	case pickRelationRemove:
		return m.selectRelationRemove()
	case pickSprintRemoveIssue:
		return m.selectSprintRemoveIssue()
	case pickAddMember:
		return m.selectAddMembers()
	case pickMemberRole:
		return m.selectMemberRole()
	default:
		return m.selectTransition()
	}
}

// pickerRows is the transition picker's body budget. The picker is floated over the screen rather than
// scrolled inside it, so anything past the frame is simply not drawn - and a blocked transition's
// conditions make the box tall enough for that to matter. The 3 rows are its two borders and the
// position indicator that appears once the options do not all fit.
func (m Model) pickerRows() int {
	return max(3, m.height-3)
}

// blockedNotes are the guard conditions listed under a blocked transition. An executable one gets none:
// its guards all passed, and the server does not report the ones it silently satisfied.
func blockedNotes(tr domain.IssueTransition) []string {
	if tr.CanExecute {
		return nil
	}
	if len(tr.BlockedReasons) == 0 {
		return []string{"This transition is blocked."}
	}
	return tr.BlockedReasons
}

// selectTransition runs the highlighted transition, or explains why a blocked one cannot run.
func (m Model) selectTransition() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	id, err := strconv.ParseInt(opt.Value, 10, 64)
	if err != nil {
		return m, nil
	}
	var chosen domain.IssueTransition
	for _, tr := range m.pickerTransitions {
		if tr.ID == id {
			chosen = tr
			break
		}
	}
	if !chosen.CanExecute {
		// the picker already lists them all; the toast answers the click, so it names the first
		return m, toast.Show(toast.Warning, blockedNotes(chosen)[0])
	}
	m.picking = false
	m.applyTransition(m.viewKey, chosen) // optimistic: show the new state at once
	return m, performTransition(m.deps, m.viewKey, id, chosen.TargetLabel)
}

// applyTransition optimistically moves the cached detail (and its list row) to a transition's target
// state. The available transitions change with the state, so they are cleared until the background
// refetch refills them. It bumps the load generation so an earlier in-flight refetch cannot clobber
// this optimistic write.
func (m *Model) applyTransition(key string, tr domain.IssueTransition) {
	d, ok := m.details[key]
	if !ok {
		return
	}
	m.detailGen[key]++
	d.StateLabel = tr.TargetLabel
	d.StateCategory = tr.TargetCategory
	d.Transitions = nil
	m.details[key] = d
	m.patchRow(key, d)
}

// TransitionDoneMsg is exported so the app shell can route this background result back to the project
// screen even when the user has left the drill-in before the transition landed (so the toast still shows).
type TransitionDoneMsg struct {
	key     string
	target  string
	err     bool
	errText string // the resolved failure toast line (server reason / mapped code / fallback)
}

func performTransition(d deps.Deps, key string, id int64, target string) tea.Cmd {
	return func() tea.Msg {
		err := d.Issues.PerformTransition(context.Background(), key, id)
		return TransitionDoneMsg{key: key, target: target, err: err != nil, errText: errmsg.Message(err, "Could not move the issue.")}
	}
}
