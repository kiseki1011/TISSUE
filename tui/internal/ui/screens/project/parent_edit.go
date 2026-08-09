package project

import (
	"context"
	"strings"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// hierarchyForType looks up an issue type's hierarchy level from the loaded catalog by display name.
// The detail carries only the type name, so the catalog (loaded for the filter/create flows) supplies
// the level needed to compute which issues may be this one's parent.
func (m Model) hierarchyForType(typeName string) (string, bool) {
	for _, t := range m.types {
		if t.Name == typeName {
			return t.Hierarchy, true
		}
	}
	return "", false
}

// openParentEditFromFormMsg asks the model to open the parent picker over the edit form; the edit
// form's Parent field emits it, mirroring the Due field's calendar (openDueEditMsg).
type openParentEditFromFormMsg struct{}

func openParentEditForm() tea.Msg { return openParentEditFromFormMsg{} }

// openParentEditPicker changes the current issue's parent. It is reached from the edit form's Parent
// field: it resolves the issue's hierarchy, then loads the eligible parent candidates (one level up)
// for a picker that also offers "None" to detach.
func (m Model) openParentEditPicker() (Model, tea.Cmd) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return m, toast.Show(toast.Info, "Still loading this issue…")
	}
	hier, ok := m.hierarchyForType(d.TypeName)
	if !ok {
		return m, toast.Show(toast.Info, "Loading issue types…")
	}
	parentHier, ok := parentHierarchy(hier)
	if !ok {
		return m, toast.Show(toast.Info, "Top-level issues have no parent.")
	}
	typeIDs := m.typeIDsAtHierarchy(parentHier)
	if len(typeIDs) == 0 {
		return m, toast.Show(toast.Info, "No "+strings.ToLower(parentHier)+" issue types to parent under.")
	}
	m.parentEditGen++
	return m, tea.Batch(
		toast.Show(toast.Info, "Loading parent issues…"),
		loadParentEditCandidates(m.deps, m.projectKey, m.viewKey, m.parentEditGen, typeIDs),
	)
}

type parentEditCandidatesLoadedMsg struct {
	gen        int    // the parentEditGen this load was for, so a superseded result is dropped
	key        string // the issue being edited, so a result stale after the user moved on is dropped
	candidates []domain.IssueSummary
	err        bool
}

// loadParentEditCandidates mirrors loadParentCandidates but routes its result to the Details edit flow.
func loadParentEditCandidates(d deps.Deps, projectKey, key string, gen int, typeIDs []int64) tea.Cmd {
	return func() tea.Msg {
		filter := domain.IssueFilter{
			StateCategories: []string{"INITIAL", "ACTIVE", "COMPLETED"},
			IssueTypeIDs:    typeIDs,
		}
		page, err := d.Issues.SearchProjectIssues(context.Background(), projectKey, filter, 0, parentCandidateLimit)
		return parentEditCandidatesLoadedMsg{gen: gen, key: key, candidates: page.Issues, err: err != nil}
	}
}

// onParentEditCandidates opens the parent picker with the loaded candidates. Self is excluded (an issue
// cannot be its own parent); "None (clear parent)" leads, in the error colour, to detach.
func (m Model) onParentEditCandidates(msg parentEditCandidatesLoadedMsg) (Model, tea.Cmd) {
	if msg.gen != m.parentEditGen || msg.key != m.viewKey {
		return m, nil // superseded, or the user moved to another issue while it loaded
	}
	if msg.err {
		return m, toast.Show(toast.Error, "Could not load parent issues.")
	}
	d, ok := m.details[m.viewKey]
	if !ok {
		return m, nil
	}
	var opts []widgets.PickerOption
	// only an optional-parent type (STANDARD) may detach; SUBTASK/MICROTASK require a parent, so offer no
	// "None" for them (mirroring the create flow, which gates its None option the same way)
	hier, _ := m.hierarchyForType(d.TypeName)
	if !parentRequired(hier) {
		opts = append(opts, widgets.PickerOption{Value: "", Label: "None (clear parent)", Color: m.deps.Styles.Theme.Error})
	}
	current := ""
	if d.Parent != nil {
		current = d.Parent.Key
	}
	w := pickerMinW
	for _, c := range msg.candidates {
		if c.Key == m.viewKey {
			continue // an issue cannot be its own parent
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
	if w > parentPickerMaxW {
		w = parentPickerMaxW
	}
	m.picking = true
	m.pickKind = pickParentEdit
	m.picker = widgets.NewSearchableListPicker("Set parent", opts, current, parentPickerRows, w)
	return m, nil
}

// selectParentEdit applies the chosen parent (or detaches on "None"), optimistically updating the cache
// and firing the change; the reload reconciles the parent's type/state (which the picker did not carry).
func (m Model) selectParentEdit() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	key := m.viewKey
	d, ok := m.details[key]
	if !ok {
		return m, nil
	}
	if opt.Value == "" { // detach
		if d.Parent == nil {
			return m, nil // already parentless, nothing to do
		}
		m.applyParent(key, nil)
		m.syncEditParent("") // reflect the detach in the still-open edit form's Parent row
		return m, removeParentCmd(m.deps, key)
	}
	if d.Parent != nil && d.Parent.Key == opt.Value {
		return m, nil // unchanged
	}
	m.applyParent(key, &domain.IssueRef{Key: opt.Value}) // minimal ref; the reload fills type/state
	m.syncEditParent(opt.Value)
	return m, assignParentCmd(m.deps, key, opt.Value)
}

// syncEditParent updates the open edit form's displayed parent after a pick, so the Parent row reflects
// the change once the picker closes and the form reappears. A no-op when the picker was not opened from
// the edit form.
func (m *Model) syncEditParent(parentKey string) {
	if m.editing {
		m.editUI.parentKey = parentKey
	}
}

// applyParent optimistically sets the cached detail's parent and bumps the load generation so an earlier
// in-flight refetch cannot clobber it. The list row carries no parent, so it is not patched.
func (m *Model) applyParent(key string, parent *domain.IssueRef) {
	d, ok := m.details[key]
	if !ok {
		return
	}
	m.detailGen[key]++
	d.Parent = parent
	m.details[key] = d
}

// ParentEditDoneMsg is exported so the app shell can route this background result back to the project
// screen even when the user has left the drill-in before it landed (so the toast still shows).
type ParentEditDoneMsg struct {
	key     string
	err     bool
	errText string // the resolved failure toast line (server reason / mapped code / fallback)
}

func assignParentCmd(d deps.Deps, key, parentKey string) tea.Cmd {
	return func() tea.Msg {
		err := d.Issues.AssignParent(context.Background(), key, parentKey)
		return ParentEditDoneMsg{key: key, err: err != nil, errText: errmsg.Message(err, "Could not update the parent.")}
	}
}

func removeParentCmd(d deps.Deps, key string) tea.Cmd {
	return func() tea.Msg {
		err := d.Issues.RemoveParent(context.Background(), key)
		return ParentEditDoneMsg{key: key, err: err != nil, errText: errmsg.Message(err, "Could not update the parent.")}
	}
}
