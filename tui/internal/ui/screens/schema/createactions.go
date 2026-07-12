package schema

import (
	tea "charm.land/bubbletea/v2"
)

// openCreateForPane opens the create modal for the focused list pane: a new workflow for the
// Workflows pane. The Issue Types pane's create modal is wired alongside it.
func (m Model) openCreateForPane() (Model, tea.Cmd, bool) {
	switch m.focus {
	case paneWorkflows:
		return m.openWorkflowCreate()
	case paneTypes:
		return m.openTypeCreate()
	}
	return m, nil, false
}

// openTypeCreate opens the new-issue-type modal, seeded with the workflow list to choose from.
func (m Model) openTypeCreate() (Model, tea.Cmd, bool) {
	m.ctype = newCreateTypeForm(m.deps, m.workflows)
	m.creatingType = true
	return m, m.ctype.Init(), true
}

// updateCreateType drives the open new-issue-type modal. A successful create closes it and reloads
// the catalog list so the new type appears.
func (m Model) updateCreateType(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case typeCreatedMsg:
		m.creatingType = false
		return m, load(m.deps)
	case createTypeCancelledMsg:
		m.creatingType = false
		return m, nil
	case tea.KeyPressMsg:
		// esc closes the modal, unless a picker (color grid or dropdown) is open — then it closes
		// only that
		if msg.String() == "esc" && !m.ctype.submitting && !m.ctype.picking && !m.ctype.pickOpen {
			m.creatingType = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.ctype, cmd = m.ctype.Update(msg)
	return m, cmd
}

// openWorkflowCreate opens the new-workflow modal seeded with a minimal valid starter graph.
func (m Model) openWorkflowCreate() (Model, tea.Cmd, bool) {
	m.cworkflow = newCreateWorkflowForm(m.deps)
	m.creatingWorkflow = true
	return m, m.cworkflow.Init(), true
}

// updateCreateWorkflow drives the open new-workflow modal. A successful create closes it and
// reloads the catalog list so the new workflow appears.
func (m Model) updateCreateWorkflow(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case workflowCreatedMsg:
		m.creatingWorkflow = false
		return m, load(m.deps)
	case createWorkflowCancelledMsg:
		m.creatingWorkflow = false
		return m, nil
	case tea.KeyPressMsg:
		// esc closes the modal, unless a sub-form (node/edge/color grid) is open — then it backs
		// out of that level only
		if msg.String() == "esc" && !m.cworkflow.submitting && !m.cworkflow.hasOverlay() {
			m.creatingWorkflow = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.cworkflow, cmd = m.cworkflow.Update(msg)
	return m, cmd
}
