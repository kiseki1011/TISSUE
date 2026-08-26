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

// relationTypeOptions are the forward types an issue can add (RELEVANT is labelled "Relates to").
var relationTypeOptions = []widgets.PickerOption{
	{Value: string(domain.RelationRelevant), Label: "Relates to"},
	{Value: string(domain.RelationBlocks), Label: "Blocks"},
	{Value: string(domain.RelationCauses), Label: "Causes"},
	{Value: string(domain.RelationDuplicates), Label: "Duplicates"},
}

// openRelationPicker starts the two-step add: the type here, the target once the candidates land.
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

// selectRelationType captures the source key now, so a cursor move cannot land the relation elsewhere.
func (m Model) selectRelationType() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.relPendingType = domain.RelationType(opt.Value)
	m.relSource = m.viewKey
	m.picking = false
	m.relGen++ // supersede an earlier candidate load so only this request opens the target picker
	return m, tea.Batch(
		toast.Show(toast.Info, "Loading issues…"),
		loadRelationCandidates(m.deps, m.projectKey, m.relSource, m.relGen),
	)
}

type relationCandidatesLoadedMsg struct {
	gen        int
	source     string // the issue the add started from, so a result stale after a cursor move is dropped
	candidates []domain.IssueSummary
	err        bool
}

// loadRelationCandidates searches this project only, though the backend allows cross-project by key.
func loadRelationCandidates(d deps.Deps, projectKey, source string, gen int) tea.Cmd {
	return func() tea.Msg {
		filter := domain.IssueFilter{StateCategories: []string{"INITIAL", "ACTIVE", "COMPLETED"}}
		page, err := d.Issues.SearchProjectIssues(context.Background(), projectKey, filter, 0, relCandidateLimit)
		return relationCandidatesLoadedMsg{gen: gen, source: source, candidates: page.Issues, err: err != nil}
	}
}

// onRelationCandidates drops self and already-linked issues, so a pick cannot obviously 400.
func (m Model) onRelationCandidates(msg relationCandidatesLoadedMsg) (Model, tea.Cmd) {
	if msg.gen != m.relGen || msg.source != m.viewKey {
		return m, nil // superseded, or the user moved to another issue while it loaded
	}
	if m.picking || m.editing || m.editingContent || m.commenting || m.deleting || m.dating || m.creating || m.filtering || m.peeking {
		return m, nil // the user started another action in the load window, so do not hijack it
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

func relationTargetTitle(rel domain.RelationType) string {
	for _, o := range relationTypeOptions {
		if o.Value == string(rel) {
			return o.Label + "…"
		}
	}
	return "Link issue"
}

// selectRelationTarget adds from m.relSource, not the live viewKey.
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
		// the backend gives a specific detail for a duplicate/cycle/self-link, which errmsg surfaces
		return relationDoneMsg{key: issueKey, err: err != nil, errText: errmsg.Message(err, "Could not add the relation.")}
	}
}

type relationRemoval struct {
	kind      string
	targetKey string
	title     string
}

// removableRelations skips inverse links (Blocked by, …): the server 404s unless asked on the owner.
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

// inverseOnlyRelations reports that every relation belongs to the other side.
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
		// the target may live in another project, so derive its project from its own key
		err := d.Issues.RemoveRelation(context.Background(), issueKey, domain.ProjectKeyOf(targetKey), targetKey)
		return relationDoneMsg{key: issueKey, removed: true, err: err != nil, errText: errmsg.Message(err, "Could not remove the relation.")}
	}
}
