package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"
)

// The color grid widget itself is unit-tested in the widgets package; this asserts the schema edit
// modal wires it in — opening the grid from a state's color field renders it over the dashboard.
func TestColorGridRendersInModal(t *testing.T) {
	m := selectElem(mkWorkflowModel(t), wfElem{elemState, 2}) // In Progress, has a color
	m, _ = m.Update(pressE())
	m.edit, _ = m.edit.focusOn(efColor)
	m.edit, _ = m.edit.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if !m.edit.picking {
		t.Fatal("enter on the color field did not open the grid")
	}
	if view := stripANSI(zone.Scan(m.View())); !strings.Contains(view, "Pick a color") {
		t.Error("the color grid is not shown over the dashboard")
	}
}
