package project

import (
	"strings"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// openChildCreateForm opens the New issue form preset as a child: types restricted to the level below
// this issue, parent locked to it. It refuses a bottom-level issue, or one still loading.
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
	m.parentGen++ // drop any parent-candidate load still in flight from a prior session
	label := d.Key
	if d.Title != "" {
		label = d.Key + "  " + flattenLine(d.Title)
	}
	m.createUI = newChildCreateForm(m.deps, childTypes, d.Key, label)
	m2, cmd := m.requestCustomFields(int64(childTypes[0].ID))
	return m2, tea.Batch(m2.createUI.Init(), cmd)
}
