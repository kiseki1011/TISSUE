package project

import (
	"context"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

const (
	relCandidateLimit = 100
	relPickerRows     = 10
	relPickerMaxW     = 72
)

// relationTypeOptions is the forward relation types a source issue can add, labelled with the display
// verb the Details section uses (RELEVANT reads "Relates to").
var relationTypeOptions = []widgets.PickerOption{
	{Value: string(domain.RelationRelevant), Label: "Relates to"},
	{Value: string(domain.RelationBlocks), Label: "Blocks"},
	{Value: string(domain.RelationCauses), Label: "Causes"},
	{Value: string(domain.RelationDuplicates), Label: "Duplicates"},
}

// openRelationPicker starts adding a relation from the viewed issue: first pick the relation type, then
// (after the type is chosen) pick the target issue. Guards on the loaded detail so it acts on a real issue.
func (m Model) openRelationPicker() (Model, tea.Cmd) {
	if _, ok := m.details[m.viewKey]; !ok {
		return m, toast.Show(toast.Info, "Open the issue first.")
	}
	w := pickerMinW
	for _, o := range relationTypeOptions {
		if lw := lipgloss.Width(o.Label) + 2; lw > w {
			w = lw
		}
	}
	m.picking = true
	m.pickKind = pickRelationType
	m.picker = widgets.NewListPicker("Add relation", relationTypeOptions, "", pickerMaxRows, w)
	return m, nil
}

// selectRelationType records the chosen type and the source issue, then loads the target-issue candidates
// for the second step. The source key is captured now and carried through the load so the relation cannot
// land on a different issue if the cursor moves (repointing m.viewKey) while the candidates are in flight.
func (m Model) selectRelationType() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.relPendingType = domain.RelationType(opt.Value)
	m.relSource = m.viewKey
	m.picking = false
	m.relGen++ // supersede any earlier in-flight candidate load so only this request opens the target picker
	return m, tea.Batch(
		toast.Show(toast.Info, "Loading issues…"),
		loadRelationCandidates(m.deps, m.projectKey, m.relSource, m.relGen),
	)
}

type relationCandidatesLoadedMsg struct {
	gen        int
	source     string // the issue the add was initiated from, so a result stale after the cursor moved is dropped
	candidates []domain.IssueSummary
	err        bool
}

// loadRelationCandidates searches the current project for issues to link (same-project only for now; the
// backend also permits cross-project by key, deferred). Aborted issues are excluded.
func loadRelationCandidates(d deps.Deps, projectKey, source string, gen int) tea.Cmd {
	return func() tea.Msg {
		filter := domain.IssueFilter{StateCategories: []string{"INITIAL", "ACTIVE", "COMPLETED"}}
		page, err := d.Issues.SearchProjectIssues(context.Background(), projectKey, filter, 0, relCandidateLimit)
		return relationCandidatesLoadedMsg{gen: gen, source: source, candidates: page.Issues, err: err != nil}
	}
}

// onRelationCandidates opens the target picker, dropping the viewed issue itself and any already-linked
// issues so a click cannot obviously 400 (self-link / duplicate). It abandons the result if it was
// superseded, if the cursor moved to another issue, or if another modal action opened while it loaded -
// so the picker never pops against the wrong issue or clobbers a flow the user has since started.
func (m Model) onRelationCandidates(msg relationCandidatesLoadedMsg) (Model, tea.Cmd) {
	if msg.gen != m.relGen || msg.source != m.viewKey {
		return m, nil // superseded, or the user moved to another issue while it loaded
	}
	if m.picking || m.editing || m.editingContent || m.commenting || m.deleting || m.dating || m.creating || m.filtering || m.peeking {
		return m, nil // the user started another action in the load window; do not hijack it
	}
	if msg.err {
		return m, toast.Show(toast.Error, "Could not load issues.")
	}
	linked := map[string]bool{}
	if d, ok := m.details[m.viewKey]; ok {
		for _, g := range d.Relations {
			for _, it := range g.Items {
				linked[it.Key] = true
			}
		}
	}
	var opts []widgets.PickerOption
	w := pickerMinW
	for _, c := range msg.candidates {
		if c.Key == m.viewKey || linked[c.Key] {
			continue
		}
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
		return m, toast.Show(toast.Info, "No eligible issues to link.")
	}
	if w > relPickerMaxW {
		w = relPickerMaxW
	}
	m.picking = true
	m.pickKind = pickRelationTarget
	m.picker = widgets.NewSearchableListPicker(relationTargetTitle(m.relPendingType), opts, "", relPickerRows, w)
	return m, nil
}

// relationTargetTitle is the target picker's title, naming the verb chosen in the first step.
func relationTargetTitle(rel domain.RelationType) string {
	for _, o := range relationTypeOptions {
		if o.Value == string(rel) {
			return o.Label + "…"
		}
	}
	return "Link issue"
}

// selectRelationTarget adds the chosen relation and closes the picker; the result refetches the detail.
// The source is the key captured when the type was chosen (m.relSource), not the live m.viewKey, so the
// relation always lands on the issue the user started from.
func (m Model) selectRelationTarget() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	return m, tea.Batch(
		toast.Show(toast.Info, "Adding relation…"),
		addRelation(m.deps, m.relSource, m.projectKey, opt.Value, m.relPendingType),
	)
}

type relationDoneMsg struct {
	key     string
	removed bool // an unlink rather than a link, so the success toast can say which
	err     bool
	errText string // the resolved failure toast line (server reason / mapped code / fallback)
}

func addRelation(d deps.Deps, issueKey, targetProjectKey, targetKey string, rel domain.RelationType) tea.Cmd {
	return func() tea.Msg {
		err := d.Issues.AddRelation(context.Background(), issueKey, targetProjectKey, targetKey, rel)
		// the backend returns a specific detail for a duplicate/cycle/self-link, which errmsg surfaces
		return relationDoneMsg{key: issueKey, err: err != nil, errText: errmsg.Message(err, "Could not add the relation.")}
	}
}

// relationRemoval is one unlinkable relation on the viewed issue: the display verb plus the issue at the
// other end. The picker's value is the target key, which is all the remove call needs.
type relationRemoval struct {
	kind      string
	targetKey string
	title     string
}

// removableRelations are the viewed issue's relations that it can actually drop. The inverse of a
// directional relation (Blocked by / Caused by / Duplicated by) lives on the other issue, so it is left
// out: asking to remove it here would return RELATION_NOT_FOUND against a link the user can plainly see.
func (m Model) removableRelations() []relationRemoval {
	d, ok := m.details[m.viewKey]
	if !ok || m.peeking {
		return nil // a peeked issue is read-only
	}
	var out []relationRemoval
	for _, g := range d.Relations {
		if !g.Removable {
			continue
		}
		for _, it := range g.Items {
			out = append(out, relationRemoval{kind: g.Kind, targetKey: it.Key, title: it.Title})
		}
	}
	return out
}

// inverseOnlyRelations reports whether the issue has relations but every one of them belongs to the other
// side, so the "nothing to unlink" message can say where to go instead of implying there are none.
func (m Model) inverseOnlyRelations() bool {
	d, ok := m.details[m.viewKey]
	if !ok {
		return false
	}
	for _, g := range d.Relations {
		if !g.Removable && len(g.Items) > 0 {
			return true
		}
	}
	return false
}

// openRelationRemovePicker opens a picker of the relations the viewed issue can unlink.
func (m Model) openRelationRemovePicker() (Model, tea.Cmd) {
	if _, ok := m.details[m.viewKey]; !ok {
		return m, toast.Show(toast.Info, "Open the issue first.")
	}
	removable := m.removableRelations()
	if len(removable) == 0 {
		if m.inverseOnlyRelations() {
			return m, toast.Show(toast.Info, "These links are owned by the other issue - remove them from there.")
		}
		return m, toast.Show(toast.Info, "No linked issues to remove.")
	}

	opts := make([]widgets.PickerOption, 0, len(removable))
	w := pickerMinW
	for _, r := range removable {
		label := r.kind + "  " + r.targetKey
		if r.title != "" {
			label += "  " + flattenLine(r.title)
		}
		opts = append(opts, widgets.PickerOption{Value: r.targetKey, Label: label})
		if lw := lipgloss.Width(label) + 2; lw > w {
			w = lw
		}
	}
	if w > relPickerMaxW {
		w = relPickerMaxW
	}
	m.relSource = m.viewKey // pin the source so a cursor move cannot redirect the removal
	m.picking = true
	m.pickKind = pickRelationRemove
	m.picker = widgets.NewListPicker("Remove relation", opts, "", relPickerRows, w)
	return m, nil
}

// selectRelationRemove unlinks the chosen relation; the result refetches the detail.
func (m Model) selectRelationRemove() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	return m, tea.Batch(
		toast.Show(toast.Info, "Removing relation…"),
		removeRelation(m.deps, m.relSource, opt.Value),
	)
}

func removeRelation(d deps.Deps, issueKey, targetKey string) tea.Cmd {
	return func() tea.Msg {
		// the target may live in another project (a link added through the API), so its project is derived
		// from its own key rather than assumed to be the one being browsed
		err := d.Issues.RemoveRelation(context.Background(), issueKey, domain.ProjectKeyOf(targetKey), targetKey)
		return relationDoneMsg{key: issueKey, removed: true, err: err != nil, errText: errmsg.Message(err, "Could not remove the relation.")}
	}
}
