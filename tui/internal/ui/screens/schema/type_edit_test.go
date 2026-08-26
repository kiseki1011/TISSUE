package schema

import (
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// typeDetailModel has issue types loaded and Details focused on the first type.
func typeDetailModel(t *testing.T) Model {
	t.Helper()
	m := mk(120, 30, 3, 2, false)
	m, _ = m.setFocus(paneDetail) // kind stays selType, T0 (id 1) is selected
	return m
}

// e opens the metadata editor seeded with the name and a color picker, no hierarchy/workflow.
func TestOpenTypeEditSeedsForm(t *testing.T) {
	m := typeDetailModel(t)
	m, _ = m.Update(pressE())
	if !m.editing || !m.CapturingInput() {
		t.Fatalf("issue type editor did not open (editing=%v capturing=%v)", m.editing, m.CapturingInput())
	}
	if m.edit.kind != editIssueType {
		t.Fatalf("kind = %v, want editIssueType", m.edit.kind)
	}
	if got := m.edit.name.Value(); got != "T0" {
		t.Errorf("name seeded %q, want T0", got)
	}
	if m.edit.colors == nil {
		t.Error("issue type editor should offer a color picker")
	}
}

// The issue type Details renders a clickable edit-pen zone on its title row.
func TestTypeEditButtonInPanel(t *testing.T) {
	m := typeDetailModel(t)
	_ = scanView(t, m.View(), "schema.type.edit")
	if z := zone.Get("schema.type.edit"); z == nil || z.IsZero() {
		t.Fatal("issue type Details has no edit-pen zone")
	}
}

// Clicking the title-row pen opens the metadata editor, like pressing e.
func TestTypeEditButtonClickOpens(t *testing.T) {
	m := typeDetailModel(t)
	_ = scanView(t, m.View(), "schema.type.edit")
	z := zone.Get("schema.type.edit")
	m, _ = m.Update(tea.MouseClickMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY, Button: tea.MouseLeft})
	if !m.editing || m.edit.kind != editIssueType {
		t.Fatalf("clicking the pen did not open the issue type editor (editing=%v kind=%v)", m.editing, m.edit.kind)
	}
}

// A type metadata save refetches the catalog without evicting the workflow graph cache.
func TestTypeEditSaveReloadsCatalog(t *testing.T) {
	m := typeDetailModel(t)
	m, _ = m.Update(pressE())
	m.wfDetail[1] = domain.WorkflowDetail{ID: 1} // a workflow cache entry that must survive
	m, cmd := m.Update(editSavedMsg{wfID: 1})    // wfID carries the issue type id here
	if m.editing {
		t.Fatal("save did not close the editor")
	}
	if _, ok := m.wfDetail[1]; !ok {
		t.Error("an issue type save wrongly invalidated the workflow cache")
	}
	if cmd == nil {
		t.Error("no catalog refetch was issued after the issue type save")
	}
}
