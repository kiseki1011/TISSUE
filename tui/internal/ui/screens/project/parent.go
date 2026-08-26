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

// The 4-level issue hierarchy, top to bottom. A parent sits exactly one level above its child, and a
// SUBTASK or MICROTASK cannot be created standalone.
var hierarchyLevel = map[string]int{"EPIC": 0, "STANDARD": 1, "SUBTASK": 2, "MICROTASK": 3}

var hierarchyByLevel = map[int]string{0: "EPIC", 1: "STANDARD", 2: "SUBTASK", 3: "MICROTASK"}

const (
	parentCandidateLimit = 100
	parentPickerRows     = 10
	parentPickerMaxW     = 72 // wider than the transition/assignee pickers - parent labels carry a title
)

// parentHierarchy is the level above hier, ok=false for top-level (EPIC) or unknown.
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

// childHierarchy is the level below hier, ok=false for the bottom level (MICROTASK) or unknown.
func childHierarchy(hier string) (string, bool) {
	lvl, ok := hierarchyLevel[hier]
	if !ok || lvl >= 3 {
		return "", false
	}
	return hierarchyByLevel[lvl+1], true
}

// typeIDsAtHierarchy is the loaded type ids at a level - the eligible parent types to filter by.
func (m Model) typeIDsAtHierarchy(hier string) []int64 {
	var ids []int64
	for _, t := range m.types {
		if t.Hierarchy == hier {
			ids = append(ids, int64(t.ID))
		}
	}
	return ids
}

// typesAtHierarchy is the loaded types at a level - what a child under the level above may take.
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

// loadParentCandidates filters by the eligible parent type ids, so the backend returns only candidates.
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

// openParentPicker loads the candidates for the selected type, or explains why no parent applies.
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

// onParentCandidates opens the picker. An optional parent (STANDARD child) gets a leading "None".
func (m Model) onParentCandidates(msg parentCandidatesLoadedMsg) (Model, tea.Cmd) {
	if !m.creating {
		return m, nil // the form closed while the candidates were loading
	}
	// a load from a prior form session or an earlier activation must not pop an unsolicited picker
	if msg.gen != m.parentGen {
		return m, nil
	}
	// the type changed while these candidates loaded, so they are of the wrong hierarchy
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

func (m Model) selectParent() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	m.createUI = m.createUI.withParent(opt.Value, opt.Label)
	return m, nil
}
