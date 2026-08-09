package project

import (
	"context"
	"strings"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// The 4-level issue hierarchy, top to bottom. A parent must sit exactly one level above its child; a
// SUBTASK or MICROTASK cannot be created standalone (its parent is required).
var hierarchyLevel = map[string]int{"EPIC": 0, "STANDARD": 1, "SUBTASK": 2, "MICROTASK": 3}

var hierarchyByLevel = map[int]string{0: "EPIC", 1: "STANDARD", 2: "SUBTASK", 3: "MICROTASK"}

const (
	parentCandidateLimit = 100
	parentPickerRows     = 10
	parentPickerMaxW     = 72 // wider than the transition/assignee pickers - parent labels carry a title
)

// parentHierarchy is the hierarchy one level above hier (a valid parent's level), or ok=false when hier
// is top-level (EPIC) or unknown, so no parent is possible.
func parentHierarchy(hier string) (string, bool) {
	lvl, ok := hierarchyLevel[hier]
	if !ok || lvl == 0 {
		return "", false
	}
	return hierarchyByLevel[lvl-1], true
}

// parentRequired reports whether an issue of this hierarchy must have a parent (SUBTASK / MICROTASK).
func parentRequired(hier string) bool {
	return hierarchyLevel[hier] >= 2
}

// childHierarchy is the hierarchy one level below hier (the level this issue's children sit at), or
// ok=false when hier is the bottom level (MICROTASK) or unknown, so no child issue is possible.
func childHierarchy(hier string) (string, bool) {
	lvl, ok := hierarchyLevel[hier]
	if !ok || lvl >= 3 {
		return "", false
	}
	return hierarchyByLevel[lvl+1], true
}

// typeIDsAtHierarchy is the ids of the loaded issue types sitting at the given hierarchy level - the
// eligible parent types to filter the candidate search by.
func (m Model) typeIDsAtHierarchy(hier string) []int64 {
	var ids []int64
	for _, t := range m.types {
		if t.Hierarchy == hier {
			ids = append(ids, int64(t.ID))
		}
	}
	return ids
}

// typesAtHierarchy is the loaded issue types sitting at the given hierarchy level - the types a child
// issue created under a parent of the level above may take.
func (m Model) typesAtHierarchy(hier string) []domain.IssueTypeSummary {
	var out []domain.IssueTypeSummary
	for _, t := range m.types {
		if t.Hierarchy == hier {
			out = append(out, t)
		}
	}
	return out
}

// createParentPickMsg is emitted by the create form's Parent field to ask the model to open the picker.
type createParentPickMsg struct{}

func openParentPickerCmd() tea.Msg { return createParentPickMsg{} }

type parentCandidatesLoadedMsg struct {
	gen        int    // the parentGen this load was issued for, so a stale/superseded result is dropped
	hier       string // the parent hierarchy this load was for, so a result stale after a type change is dropped
	candidates []domain.IssueSummary
	err        bool
}

// loadParentCandidates searches the project for issues of the eligible parent types. Passing the type
// ids as the filter makes the backend return only candidates, so no client-side filtering is needed.
func loadParentCandidates(d deps.Deps, projectKey, parentHier string, gen int, typeIDs []int64) tea.Cmd {
	return func() tea.Msg {
		filter := domain.IssueFilter{
			StateCategories: []string{"INITIAL", "ACTIVE", "COMPLETED"},
			IssueTypeIDs:    typeIDs,
		}
		page, err := d.Issues.SearchProjectIssues(context.Background(), projectKey, filter, 0, parentCandidateLimit)
		return parentCandidatesLoadedMsg{gen: gen, hier: parentHier, candidates: page.Issues, err: err != nil}
	}
}

// openParentPicker starts the candidate load for the create form's selected type, or explains why no
// parent applies (a top-level type, or no eligible parent types exist).
func (m Model) openParentPicker() (Model, tea.Cmd) {
	if !m.creating {
		return m, nil
	}
	parentHier, ok := parentHierarchy(m.createUI.selectedHierarchy())
	if !ok {
		return m, toast.Show(toast.Info, "Top-level issues have no parent.")
	}
	typeIDs := m.typeIDsAtHierarchy(parentHier)
	if len(typeIDs) == 0 {
		return m, toast.Show(toast.Info, "No "+strings.ToLower(parentHier)+" issue types to parent under.")
	}
	m.parentGen++ // supersede any earlier in-flight parent load so only this request can open the picker
	return m, tea.Batch(
		toast.Show(toast.Info, "Loading parent issues…"),
		loadParentCandidates(m.deps, m.projectKey, parentHier, m.parentGen, typeIDs),
	)
}

// onParentCandidates opens the parent picker with the loaded candidates, or reports why it cannot. An
// optional parent (STANDARD child) gets a leading "None" option to clear the selection.
func (m Model) onParentCandidates(msg parentCandidatesLoadedMsg) (Model, tea.Cmd) {
	if !m.creating {
		return m, nil // the form closed while the candidates were loading
	}
	// drop a superseded/stale load: one from a prior form session or an earlier activation (a reopened
	// form or a double-press must not pop an unsolicited picker or reset an open one)
	if msg.gen != m.parentGen {
		return m, nil
	}
	// drop a result whose parent level no longer matches the selected type (the user changed the type
	// while these candidates were loading), else the picker would offer parents of the wrong hierarchy
	if cur, ok := parentHierarchy(m.createUI.selectedHierarchy()); !ok || cur != msg.hier {
		return m, nil
	}
	if msg.err {
		return m, toast.Show(toast.Error, "Could not load parent issues.")
	}
	var opts []widgets.PickerOption
	if !parentRequired(m.createUI.selectedHierarchy()) {
		opts = append(opts, widgets.PickerOption{Value: "", Label: "None"})
	}
	w := pickerMinW
	for _, c := range msg.candidates {
		label := c.Key
		if c.Title != "" {
			label = c.Key + "  " + flattenLine(c.Title)
		}
		opts = append(opts, widgets.PickerOption{Value: c.Key, Label: label})
		if lw := lipgloss.Width(label) + 2; lw > w {
			w = lw
		}
	}
	if len(opts) == 0 {
		return m, toast.Show(toast.Info, "No eligible parent issues.")
	}
	if w > parentPickerMaxW {
		w = parentPickerMaxW
	}
	m.picking = true
	m.pickKind = pickParent
	m.picker = widgets.NewSearchableListPicker("Set parent", opts, m.createUI.parentKey, parentPickerRows, w)
	return m, nil
}

// selectParent applies the highlighted candidate as the create form's parent and closes the picker.
func (m Model) selectParent() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	m.createUI = m.createUI.withParent(opt.Value, opt.Label)
	return m, nil
}
