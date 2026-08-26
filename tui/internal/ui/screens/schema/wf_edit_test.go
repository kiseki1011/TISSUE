package schema

import (
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

// The workflow editor seeds name/description and offers no color field.
func TestOpenWorkflowEditSeedsForm(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemWfMeta, 0})
	m, _ = m.Update(pressE())
	if !m.editing || !m.CapturingInput() {
		t.Fatalf("workflow editor did not open (editing=%v capturing=%v)", m.editing, m.CapturingInput())
	}
	if m.edit.kind != editWorkflow {
		t.Fatalf("kind = %v, want editWorkflow", m.edit.kind)
	}
	if got := m.edit.name.Value(); got != "Development" {
		t.Errorf("name seeded %q, want Development", got)
	}
	if m.edit.colors != nil {
		t.Error("the workflow editor should not offer a color picker")
	}
}

func TestWorkflowMetaEnterOpensEditor(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemWfMeta, 0})
	m, _ = m.Update(keyEnter())
	if !m.editing || m.edit.kind != editWorkflow {
		t.Fatalf("enter on the workflow metadata did not open the editor (editing=%v kind=%v)", m.editing, m.edit.kind)
	}
}

func TestWorkflowEditButtonClickOpens(t *testing.T) {
	m := mkWorkflowModel(t)
	_ = scanView(t, m.View(), "schema.wf.edit")
	z := zone.Get("schema.wf.edit")
	if z == nil || z.IsZero() {
		t.Fatal("workflow Details has no edit-pen zone")
	}
	m, _ = m.Update(tea.MouseClickMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY, Button: tea.MouseLeft})
	if !m.editing || m.edit.kind != editWorkflow {
		t.Fatalf("clicking the pen did not open the workflow editor (editing=%v kind=%v)", m.editing, m.edit.kind)
	}
}

// The metadata pen sits above the section buttons.
func TestWorkflowMetaNavigableAboveButtons(t *testing.T) {
	m := mkWorkflowModel(t)
	m, _ = m.Update(keyUp()) // Flow
	m, _ = m.Update(keyUp()) // VCS
	m, _ = m.Update(keyUp()) // workflow metadata
	if e, _ := m.selectedElem(); e.kind != elemWfMeta {
		t.Fatalf("up from the VCS button selected %+v, want the workflow metadata", e)
	}
}

// The metadata pen is selectable before the graph loads, so editing does not wait on the diagram.
func TestWorkflowMetaSelectableBeforeGraph(t *testing.T) {
	m := mkWorkflowModel(t)
	delete(m.wfDetail, 1) // drop the loaded graph
	elems := m.workflowElems()
	if len(elems) != 1 || elems[0].kind != elemWfMeta {
		t.Fatalf("without a graph, elems = %+v, want just the workflow metadata", elems)
	}
}

// A metadata save refetches the catalog without evicting the graph cache.
func TestWorkflowMetaSaveReloadsCatalog(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemWfMeta, 0})
	m, _ = m.Update(pressE())
	m, cmd := m.Update(editSavedMsg{wfID: 1})
	if m.editing {
		t.Fatal("save did not close the editor")
	}
	if _, ok := m.wfDetail[1]; !ok {
		t.Error("a workflow metadata save wrongly invalidated the graph cache")
	}
	if cmd == nil {
		t.Error("no catalog refetch was issued after the workflow metadata save")
	}
}

// The workflow metadata editor omits the color, so a save preserves the workflow's current color.
func TestWorkflowEditOmitsColor(t *testing.T) {
	f := newEditForm(optionsDeps(), editWorkflow, 1, 0, "Edit Workflow", "Development", "", "desc", false)
	f.name.SetValue("Renamed")
	f, cmd := f.submit()
	if !f.submitting || cmd == nil {
		t.Fatal("the workflow edit did not submit")
	}
	if f.colors != nil {
		t.Error("the workflow editor must not carry color options")
	}
}
