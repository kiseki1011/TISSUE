package schema

import (
	tea "charm.land/bubbletea/v2"
)

func (m Model) openCreateForPane() (Model, tea.Cmd, bool) {
	switch m.focus {
	case paneWorkflows:
		return m.openWorkflowCreate()
	case paneTypes:
		return m.openTypeCreate()
	}
	return m, nil, false
}

func (m Model) openTypeCreate() (Model, tea.Cmd, bool) {
	m.ctype = newCreateTypeForm(m.deps, m.workflows)
	m.creatingType = true
	return m, m.ctype.Init(), true
}

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

func (m Model) openWorkflowCreate() (Model, tea.Cmd, bool) {
	m.cworkflow = newCreateWorkflowForm(m.deps)
	m.creatingWorkflow = true
	return m, m.cworkflow.Init(), true
}

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
