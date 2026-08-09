package project

import (
	"strings"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// openChildCreateForm opens the New issue form preset to create a child of the issue in the Details
// panel: the type cycle is restricted to the one hierarchy level below this issue, and the parent is
// locked to it. It mirrors openCreateForm's readiness guards (the detail and the type catalog must have
// loaded) and refuses when this issue can have no children (a bottom-level MICROTASK, or no child types).
func (m Model) openChildCreateForm() (Model, tea.Cmd) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return m, toast.Show(toast.Info, "Still loading this issue…")
	}
	hier, ok := m.hierarchyForType(d.TypeName)
	if !ok {
		return m, toast.Show(toast.Info, "Loading issue types…")
	}
	childHier, ok := childHierarchy(hier)
	if !ok {
		return m, toast.Show(toast.Info, "This issue can't have child issues.")
	}
	childTypes := m.typesAtHierarchy(childHier)
	if len(childTypes) == 0 {
		return m, toast.Show(toast.Info, "No "+strings.ToLower(childHier)+" issue types to create.")
	}

	m.creating = true
	m.createScroll = 0
	m.parentGen++ // a fresh form session: drop any parent-candidate load still in flight from a prior one
	label := d.Key
	if d.Title != "" {
		label = d.Key + "  " + flattenLine(d.Title)
	}
	m.createUI = newChildCreateForm(m.deps, childTypes, d.Key, label)
	// load the first restricted type's custom fields (from cache when available)
	m2, cmd := m.requestCustomFields(int64(childTypes[0].ID))
	return m2, tea.Batch(m2.createUI.Init(), cmd)
}
