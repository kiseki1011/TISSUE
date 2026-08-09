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

// reviewerRows is the visible row budget of the reviewers picker, like the assignee picker (a project
// can have many members; the filter narrows them and the window scrolls the rest).
const reviewerRows = 10

// openReviewersPicker opens a multi-select of the project's active members, pre-checked with the issue's
// current reviewers. Space toggles, enter confirms; the roster change is applied as a diff of per-member
// add/remove calls (the backend has no bulk set).
func (m Model) openReviewersPicker() (Model, tea.Cmd) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return m, toast.Show(toast.Info, "Still loading this issue…")
	}
	if !m.membersLoaded {
		return m, toast.Show(toast.Info, "Loading members…")
	}
	if len(m.members) == 0 {
		return m, toast.Show(toast.Info, "No members to add as reviewers.")
	}

	opts := make([]widgets.PickerOption, 0, len(m.members))
	w := pickerMinW
	for _, mem := range m.members {
		label := mem.Name()
		opts = append(opts, widgets.PickerOption{Value: strconv.FormatInt(mem.MemberID, 10), Label: label})
		if lw := lipgloss.Width(label) + 6; lw > w { // room for the cursor marker + "[ ] " checkbox
			w = lw
		}
	}
	if w > pickerMaxW {
		w = pickerMaxW
	}

	preChecked := make([]string, 0, len(d.Reviewers))
	for _, rv := range d.Reviewers {
		preChecked = append(preChecked, strconv.FormatInt(rv.MemberID, 10))
	}

	m.picking = true
	m.pickKind = pickReviewers
	m.reviewerBase = append([]domain.Reviewer(nil), d.Reviewers...) // snapshot to diff the confirm against
	m.picker = widgets.NewMultiListPicker("Set reviewers", opts, preChecked, reviewerRows, w)
	return m, nil
}

// confirmReviewers diffs the checked set against the issue's current reviewers and fires the add/remove
// calls. A no-op change just closes the picker.
func (m Model) confirmReviewers() (Model, tea.Cmd) {
	desired := map[int64]bool{}
	for _, v := range m.picker.Selections() {
		if id, err := strconv.ParseInt(v, 10, 64); err == nil {
			desired[id] = true
		}
	}
	current := map[int64]bool{}
	for _, rv := range m.reviewerBase {
		current[rv.MemberID] = true
	}
	add, remove := reviewerDiff(desired, current)

	m.picking = false
	if len(add) == 0 && len(remove) == 0 {
		return m, nil // nothing changed
	}
	return m, applyReviewers(m.deps, m.viewKey, add, remove)
}

// reviewerDiff is the per-member add/remove needed to move the roster from current to desired.
func reviewerDiff(desired, current map[int64]bool) (add, remove []int64) {
	for id := range desired {
		if !current[id] {
			add = append(add, id)
		}
	}
	for id := range current {
		if !desired[id] {
			remove = append(remove, id)
		}
	}
	return add, remove
}

// ReviewerDoneMsg is exported so the app shell routes this background result back to the project screen
// even if the user left the drill-in before it landed (so the toast still shows).
type ReviewerDoneMsg struct {
	key     string
	err     bool
	errText string // the resolved failure toast line (server reason / mapped code / fallback)
}

// applyReviewers runs the per-member add then remove calls in one command, stopping at the first error,
// and reports a single result the screen reconciles with a detail refetch.
func applyReviewers(d deps.Deps, key string, add, remove []int64) tea.Cmd {
	return func() tea.Msg {
		for _, id := range add {
			if err := d.Issues.AddReviewer(context.Background(), key, id); err != nil {
				return ReviewerDoneMsg{key: key, err: true, errText: errmsg.Message(err, "Could not update reviewers.")}
			}
		}
		for _, id := range remove {
			if err := d.Issues.RemoveReviewer(context.Background(), key, id); err != nil {
				return ReviewerDoneMsg{key: key, err: true, errText: errmsg.Message(err, "Could not update reviewers.")}
			}
		}
		return ReviewerDoneMsg{key: key}
	}
}
