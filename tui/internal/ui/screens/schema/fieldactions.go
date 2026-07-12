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

// openFieldCreate opens the new-field modal for the selected issue type, seeded with the append
// position (one past the highest existing field position).
func (m Model) openFieldCreate() (Model, tea.Cmd, bool) {
	if m.kind != selType {
		return m, nil, false
	}
	t, ok := m.selectedType()
	if !ok {
		return m, nil, false
	}
	pos := 0
	if d, ok := m.typeDetail[t.ID]; ok {
		for _, f := range d.Fields {
			if f.Position+1 > pos {
				pos = f.Position + 1
			}
		}
	}
	m.cfield = newCreateFieldForm(m.deps, t.ID, pos)
	m.creatingField = true
	return m, m.cfield.Init(), true
}

// updateCreateField drives the open new-field modal. A successful create invalidates the type's
// cached fields and refetches them so the new field appears.
func (m Model) updateCreateField(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case fieldCreatedMsg:
		m.creatingField = false
		delete(m.typeDetail, msg.typeID)
		delete(m.detailFailed, msg.typeID)
		m.detailPending[msg.typeID] = true
		return m, loadDetail(m.deps, msg.typeID)
	case createFieldCancelledMsg:
		m.creatingField = false
		return m, nil
	case tea.KeyPressMsg:
		// esc closes the modal, unless the type dropdown is open — then it just closes that
		if msg.String() == "esc" && !m.cfield.submitting && !m.cfield.pickOpen {
			m.creatingField = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.cfield, cmd = m.cfield.Update(msg)
	return m, cmd
}

// deleteSelectedField opens a confirm dialog for the selected custom field (the x/d key). The
// actual delete fires only once the dialog is accepted.
func (m Model) deleteSelectedField() (Model, tea.Cmd, bool) {
	if m.kind != selType {
		return m, nil, false
	}
	e, ok := m.selectedTypeElem()
	if !ok || e.kind != elemField {
		return m, nil, false
	}
	t, ok := m.selectedType()
	if !ok {
		return m, nil, false
	}
	name := "this field"
	if d, ok := m.typeDetail[t.ID]; ok {
		for _, f := range d.Fields {
			if f.ID == e.id {
				name = "\"" + f.Name + "\""
			}
		}
	}
	m.pendingDeleteField = e.id
	m.confirm = newConfirmForm(m.deps, "Delete field",
		"Delete "+name+"? This removes the field from the issue type and cannot be undone.", "Delete")
	m.confirming = true
	return m, m.confirm.Init(), true
}

// clearPending closes the confirm dialog and forgets whatever delete it was guarding, so a fresh
// dialog never inherits a stale target.
func (m Model) clearPending() Model {
	m.confirming = false
	m.pendingDeleteField, m.pendingDeleteType, m.pendingDeleteWorkflow = 0, 0, 0
	return m
}

// updateConfirm drives the delete-confirm dialog. On accept it runs whichever delete is pending,
// keeping the dialog open (submitting) so a failure such as "in use" can be shown in place. A
// successful delete closes the dialog, refreshes the affected list, and raises a success toast.
func (m Model) updateConfirm(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case confirmAcceptedMsg:
		switch {
		case m.pendingDeleteField != 0:
			if t, ok := m.selectedType(); ok {
				return m, deleteFieldCmd(m.deps, t.ID, m.pendingDeleteField)
			}
		case m.pendingDeleteType != 0:
			if t, ok := m.selectedType(); ok {
				return m, deleteTypeCmd(m.deps, m.pendingDeleteType, t.Name)
			}
		case m.pendingDeleteWorkflow != 0:
			if w, ok := m.selectedWorkflow(); ok {
				return m, deleteWorkflowCmd(m.deps, m.pendingDeleteWorkflow, w.Name)
			}
		}
		return m.clearPending(), nil
	case confirmCancelledMsg:
		return m.clearPending(), nil
	case fieldDeletedMsg:
		m = m.clearPending()
		delete(m.typeDetail, msg.typeID)
		delete(m.detailFailed, msg.typeID)
		m.detailPending[msg.typeID] = true
		return m, tea.Batch(loadDetail(m.deps, msg.typeID), toast.Show(toast.Success, "Field deleted."))
	case fieldDeleteFailedMsg:
		m.confirm.submitting = false
		m.confirm.status = msg.message
		return m, nil
	case typeDeletedMsg:
		m = m.clearPending()
		return m, tea.Batch(load(m.deps), deletedToast("issue type", msg.name))
	case typeDeleteFailedMsg:
		m.confirm.submitting = false
		m.confirm.status = msg.message
		return m, nil
	case workflowDeletedMsg:
		m = m.clearPending()
		return m, tea.Batch(load(m.deps), deletedToast("workflow", msg.name))
	case workflowDeleteFailedMsg:
		m.confirm.submitting = false
		m.confirm.status = msg.message
		return m, nil
	case tea.KeyPressMsg:
		if msg.String() == "esc" && !m.confirm.submitting {
			return m.clearPending(), nil
		}
	}
	var cmd tea.Cmd
	m.confirm, cmd = m.confirm.Update(msg)
	return m, cmd
}

// reorderSelectedField moves the selected custom field up (delta -1) or down (delta +1), applying
// the new order optimistically and committing the full id list. It reports false when the move is
// not possible (no field selected, or already at an edge).
func (m Model) reorderSelectedField(delta int) (Model, tea.Cmd, bool) {
	if m.kind != selType {
		return m, nil, false
	}
	e, ok := m.selectedTypeElem()
	if !ok || e.kind != elemField {
		return m, nil, false
	}
	t, ok := m.selectedType()
	if !ok {
		return m, nil, false
	}
	d, ok := m.typeDetail[t.ID]
	if !ok {
		return m, nil, false
	}
	idx := -1
	for i, f := range d.Fields {
		if f.ID == e.id {
			idx = i
			break
		}
	}
	j := idx + delta
	if idx < 0 || j < 0 || j >= len(d.Fields) {
		return m, nil, false
	}
	// swap on a fresh slice so the cached order updates without mutating the shared backing array
	fields := make([]domain.IssueField, len(d.Fields))
	copy(fields, d.Fields)
	fields[idx], fields[j] = fields[j], fields[idx]
	d.Fields = fields
	m.typeDetail[t.ID] = d
	m.typeSel += delta // selection follows the moved field
	ids := make([]int, len(fields))
	for i, f := range fields {
		ids[i] = f.ID
	}
	m.revealSelectedTypeElem()
	return m, reorderFieldsCmd(m.deps, t.ID, ids), true
}

// selectedFieldHasOptions reports whether the selected type element is a SELECT_OPTION / CHECKLIST
// field, so the options shortcut is offered only where it applies.
func (m Model) selectedFieldHasOptions() bool {
	if m.kind != selType {
		return false
	}
	e, ok := m.selectedTypeElem()
	if !ok || e.kind != elemField {
		return false
	}
	t, ok := m.selectedType()
	if !ok {
		return false
	}
	if d, ok := m.typeDetail[t.ID]; ok {
		for _, f := range d.Fields {
			if f.ID == e.id {
				return canHaveOptions(f.Type)
			}
		}
	}
	return false
}

// openFieldOptions opens the options editor for the selected field, when that field is a
// SELECT_OPTION / CHECKLIST type. It reports false for any other selection.
func (m Model) openFieldOptions() (Model, tea.Cmd, bool) {
	if m.kind != selType {
		return m, nil, false
	}
	e, ok := m.selectedTypeElem()
	if !ok || e.kind != elemField {
		return m, nil, false
	}
	t, ok := m.selectedType()
	if !ok {
		return m, nil, false
	}
	d, ok := m.typeDetail[t.ID]
	if !ok {
		return m, nil, false
	}
	for _, f := range d.Fields {
		if f.ID == e.id {
			if !canHaveOptions(f.Type) {
				return m, nil, false
			}
			m.options = newOptionsForm(m.deps, t.ID, f.ID, f.Name, f.Options)
			m.optionsEditing = true
			return m, m.options.Init(), true
		}
	}
	return m, nil, false
}

// updateOptions drives the open options editor. A successful commit closes it and refetches the
// type's fields so the Details reflect the new option list. A failure keeps the editor open with the
// error and also refetches, so a partly-applied commit (some deletes/renames landed before an error)
// reseeds from the server instead of re-issuing the applied changes on the next Save.
func (m Model) updateOptions(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case optionsSavedMsg:
		m.optionsEditing = false
		delete(m.typeDetail, msg.typeID)
		delete(m.detailFailed, msg.typeID)
		m.detailPending[msg.typeID] = true
		return m, loadDetail(m.deps, msg.typeID)
	case optionsFailedMsg:
		m.options, _ = m.options.Update(msg) // surface the error, clear submitting
		id := m.options.typeID
		delete(m.typeDetail, id)
		delete(m.detailFailed, id)
		m.detailPending[id] = true
		return m, loadDetail(m.deps, id)
	case optionsCancelledMsg:
		m.optionsEditing = false
		return m, nil
	case tea.KeyPressMsg:
		// esc closes the modal, unless the add/rename prompt is open — then it just closes that
		if msg.String() == "esc" && !m.options.submitting && !m.options.inputOpen {
			m.optionsEditing = false
			return m, nil
		}
	}
	var cmd tea.Cmd
	m.options, cmd = m.options.Update(msg)
	return m, cmd
}

// reseedOptions rebuilds the open options editor from a freshly loaded type detail after a partial
// failure, so its baseline (orig) matches the server. The error message is preserved. If the field
// no longer exists the editor closes.
func (m *Model) reseedOptions(d domain.IssueTypeDetail) {
	for _, f := range d.Fields {
		if f.ID == m.options.fieldID {
			status := m.options.status
			m.options = newOptionsForm(m.deps, m.options.typeID, f.ID, f.Name, f.Options)
			m.options.status = status
			return
		}
	}
	m.optionsEditing = false
}

// ---- commands & messages ----

type fieldDeletedMsg struct{ typeID int }

type fieldDeleteFailedMsg struct{ message string }

type fieldsReorderedMsg struct{}

type fieldsReorderFailedMsg struct{ typeID int }

func deleteFieldCmd(d deps.Deps, typeID, fieldID int) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.DeleteIssueField(context.Background(), fieldID); err != nil {
			return fieldDeleteFailedMsg{message: deleteFieldErrorMessage(err)}
		}
		return fieldDeletedMsg{typeID: typeID}
	}
}

func reorderFieldsCmd(d deps.Deps, typeID int, ids []int) tea.Cmd {
	return func() tea.Msg {
		if err := d.Catalog.ReorderIssueFields(context.Background(), typeID, ids); err != nil {
			return fieldsReorderFailedMsg{typeID: typeID}
		}
		return fieldsReorderedMsg{}
	}
}

func deleteFieldErrorMessage(err error) string {
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "This field is in use by issues and cannot be deleted."
		case http.StatusForbidden:
			return "You do not have permission to delete this."
		}
	}
	return "Could not delete. Try again."
}
