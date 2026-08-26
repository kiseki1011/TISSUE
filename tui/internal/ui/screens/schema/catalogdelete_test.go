package schema

import (
	"net/http"
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

func keyD() tea.KeyPressMsg { return tea.KeyPressMsg{Code: 'd', Text: "d"} }

// callSafe swallows the panic a reload cmd throws on mk's nil Catalog.
func callSafe(c tea.Cmd) (msg tea.Msg) {
	defer func() { _ = recover() }()
	if c != nil {
		msg = c()
	}
	return
}

// toastFromBatch scans the whole batch: tea.Batch does not guarantee command order.
func toastFromBatch(t *testing.T, cmd tea.Cmd) toast.ShowMsg {
	t.Helper()
	if cmd == nil {
		t.Fatal("delete success returned no command")
	}
	batch, ok := cmd().(tea.BatchMsg)
	if !ok {
		t.Fatalf("delete success was not a batch, got %T", cmd())
	}
	for _, c := range batch {
		if ts, ok := callSafe(c).(toast.ShowMsg); ok {
			return ts
		}
	}
	t.Fatal("delete-success batch carried no toast")
	return toast.ShowMsg{}
}

func TestDeleteWorkflowOpensConfirm(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyD())
	if !m.confirming || m.pendingDeleteWorkflow == 0 {
		t.Fatalf("d did not open the workflow delete confirm (confirming=%v pending=%d)",
			m.confirming, m.pendingDeleteWorkflow)
	}
	if m.pendingDeleteType != 0 || m.pendingDeleteField != 0 {
		t.Errorf("an unrelated delete target was set (type=%d field=%d)",
			m.pendingDeleteType, m.pendingDeleteField)
	}
}

// The dialog stays open after accept so an in-place failure ("in use") can still be shown.
func TestDeleteWorkflowAcceptRunsDelete(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyD())
	m, cmd := m.Update(confirmAcceptedMsg{})
	if cmd == nil {
		t.Fatal("accepting the confirm did not run a delete command")
	}
	if !m.confirming {
		t.Error("the dialog closed before the delete resolved")
	}
}

func TestWorkflowDeletedReloadsAndToasts(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyD())
	m, cmd := m.Update(workflowDeletedMsg{name: "W0"})
	if m.confirming || m.pendingDeleteWorkflow != 0 {
		t.Errorf("a successful delete did not close/clear the confirm (confirming=%v pending=%d)",
			m.confirming, m.pendingDeleteWorkflow)
	}
	ts := toastFromBatch(t, cmd)
	if ts.Level != toast.Success {
		t.Errorf("toast level = %v, want Success", ts.Level)
	}
	if !strings.Contains(ts.Text, "workflow") || !strings.Contains(ts.Text, "W0") {
		t.Errorf("toast text = %q, want it to name the deleted workflow W0", ts.Text)
	}
}

func TestWorkflowDeleteFailureKeepsOpen(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(keyD())
	m, _ = m.Update(workflowDeleteFailedMsg{message: "in use"})
	if !m.confirming || m.confirm.Status != "in use" {
		t.Errorf("failure not surfaced in place (confirming=%v status=%q)", m.confirming, m.confirm.Status)
	}
}

func TestDeleteTypeOpensConfirm(t *testing.T) {
	m := mk(120, 30, 3, 2, false) // focus defaults to the Issue Types pane
	m, _ = m.Update(keyD())
	if !m.confirming || m.pendingDeleteType == 0 {
		t.Fatalf("d did not open the type delete confirm (confirming=%v pending=%d)",
			m.confirming, m.pendingDeleteType)
	}
}

func TestTypeDeletedReloadsAndToasts(t *testing.T) {
	m := mk(120, 30, 3, 2, false)
	m, _ = m.Update(keyD())
	m, cmd := m.Update(typeDeletedMsg{name: "T0"})
	if m.confirming || m.pendingDeleteType != 0 {
		t.Errorf("a successful type delete did not close/clear the confirm (confirming=%v pending=%d)",
			m.confirming, m.pendingDeleteType)
	}
	ts := toastFromBatch(t, cmd)
	if ts.Level != toast.Success {
		t.Errorf("toast level = %v, want Success", ts.Level)
	}
	if !strings.Contains(ts.Text, "issue type") || !strings.Contains(ts.Text, "T0") {
		t.Errorf("toast text = %q, want it to name the deleted issue type T0", ts.Text)
	}
}

func TestDeleteErrorMessageNamesInUse(t *testing.T) {
	err := &domain.APIError{Status: http.StatusConflict}
	if got := deleteCatalogErrorMessage(err, "workflow"); !strings.Contains(got, "in use") {
		t.Errorf("409 message = %q, want it to mention the entity is in use", got)
	}
	forbidden := &domain.APIError{Status: http.StatusForbidden}
	if got := deleteCatalogErrorMessage(forbidden, "issue type"); !strings.Contains(got, "permission") {
		t.Errorf("403 message = %q, want it to mention permission", got)
	}
}
