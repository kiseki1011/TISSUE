package schema

import (
	"context"
	"errors"
	"net/http"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// deleteForPane opens the delete-confirm dialog for the focused list pane's selected item, so d
// deletes an issue type or a workflow straight from the list.
func (m Model) deleteForPane() (Model, tea.Cmd, bool) {
	switch m.focus {
	case paneTypes:
		return m.deleteSelectedType()
	case paneWorkflows:
		return m.deleteSelectedWorkflow()
	}
	return m, nil, false
}

// deleteSelectedType opens a confirm dialog for the selected issue type (the d key on the Issue
// Types list). Deleting a global type affects every project, so the copy spells that out and the
// real delete fires only once the dialog is accepted.
func (m Model) deleteSelectedType() (Model, tea.Cmd, bool) {
	t, ok := m.selectedType()
	if !ok {
		return m, nil, false
	}
	m.pendingDeleteType = t.ID
	m.confirm = newConfirmForm(m.deps, "Delete issue type",
		"Delete \""+t.Name+"\"? This removes the issue type from every project and cannot be undone.", "Delete")
	m.confirming = true
	return m, m.confirm.Init(), true
}

// deleteSelectedWorkflow opens a confirm dialog for the selected workflow (the d key on the
// Workflows list). The real delete fires only once the dialog is accepted.
func (m Model) deleteSelectedWorkflow() (Model, tea.Cmd, bool) {
	w, ok := m.selectedWorkflow()
	if !ok {
		return m, nil, false
	}
	m.pendingDeleteWorkflow = w.ID
	m.confirm = newConfirmForm(m.deps, "Delete workflow",
		"Delete \""+w.Name+"\"? This removes the workflow and cannot be undone.", "Delete")
	m.confirming = true
	return m, m.confirm.Init(), true
}

// ---- commands & messages ----

type typeDeletedMsg struct{ name string }

type typeDeleteFailedMsg struct{ message string }

type workflowDeletedMsg struct{ name string }

type workflowDeleteFailedMsg struct{ message string }

func deleteTypeCmd(d deps.Deps, id int, name string) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.DeleteIssueType(context.Background(), id); err != nil {
			return typeDeleteFailedMsg{message: deleteCatalogErrorMessage(err, "issue type")}
		}
		return typeDeletedMsg{name: name}
	}
}

func deleteWorkflowCmd(d deps.Deps, id int, name string) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.DeleteWorkflow(context.Background(), id); err != nil {
			return workflowDeleteFailedMsg{message: deleteCatalogErrorMessage(err, "workflow")}
		}
		return workflowDeletedMsg{name: name}
	}
}

// deleteCatalogErrorMessage renders a delete failure for a catalog entity, naming the 409 in-use
// case (the common one for a global type/workflow still referenced by issues or types) and the
// permission case, with a generic fallback for everything else.
func deleteCatalogErrorMessage(err error, noun string) string {
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "This " + noun + " is still in use and cannot be deleted."
		case http.StatusForbidden:
			return "You do not have permission to delete this " + noun + "."
		}
	}
	return "Could not delete the " + noun + ". Try again."
}

// deletedToast is the success notification raised after a catalog entity is deleted.
func deletedToast(noun, name string) tea.Cmd {
	return toast.Show(toast.Success, "Deleted "+noun+" \""+name+"\".")
}
