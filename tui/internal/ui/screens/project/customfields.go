package project

import (
	"context"
	"sort"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// createTypeFieldsMsg asks the model to (re)load the custom-field inputs for a type. The create form
// emits it when the type changes or when it opens.
type createTypeFieldsMsg struct{ typeID int64 }

func requestTypeFields(typeID int64) tea.Cmd {
	return func() tea.Msg { return createTypeFieldsMsg{typeID: typeID} }
}

type typeFieldsLoadedMsg struct {
	gen    int
	typeID int64
	fields []domain.IssueField
	err    bool
}

func loadTypeFields(d deps.Deps, typeID int64, gen int) tea.Cmd {
	return func() tea.Msg {
		det, err := d.Catalog.GetIssueType(context.Background(), int(typeID))
		return typeFieldsLoadedMsg{gen: gen, typeID: typeID, fields: det.Fields, err: err != nil}
	}
}

// requestCustomFields points the create form at a type's custom fields: from the per-type cache when
// available (cycling back to a type is then instant), else it clears the fields and fires a fetch.
func (m Model) requestCustomFields(typeID int64) (Model, tea.Cmd) {
	if !m.creating {
		return m, nil
	}
	if fields, ok := m.typeFields[typeID]; ok {
		m.createUI = m.createUI.withCustomFields(buildCustomInputs(fields))
		return m, nil
	}
	m.typeFieldsGen++
	m.createUI = m.createUI.startCustomLoad() // no custom rows, and submit waits, while loading
	return m, loadTypeFields(m.deps, typeID, m.typeFieldsGen)
}

// onTypeFieldsLoaded caches the fetched fields and, when the form is still open on that type and this is
// the latest request, builds their inputs.
func (m Model) onTypeFieldsLoaded(msg typeFieldsLoadedMsg) (Model, tea.Cmd) {
	if !msg.err {
		if m.typeFields == nil {
			m.typeFields = map[int64][]domain.IssueField{}
		}
		m.typeFields[msg.typeID] = msg.fields // cache even if the form has since closed
	}
	if !m.creating || msg.gen != m.typeFieldsGen {
		return m, nil
	}
	if msg.err {
		return m, toast.Show(toast.Error, "Could not load this type's fields.")
	}
	if t, ok := m.createUI.selectedType(); !ok || int64(t.ID) != msg.typeID {
		return m, nil // the user changed the type while this load was in flight
	}
	m.createUI = m.createUI.withCustomFields(buildCustomInputs(msg.fields))
	return m, nil
}

// buildCustomInputs makes an input per field, in the type's defined display order.
func buildCustomInputs(fields []domain.IssueField) []customFieldInput {
	sorted := append([]domain.IssueField(nil), fields...)
	sort.SliceStable(sorted, func(i, j int) bool { return sorted[i].Position < sorted[j].Position })
	out := make([]customFieldInput, 0, len(sorted))
	for _, f := range sorted {
		out = append(out, newCustomFieldInput(f))
	}
	return out
}
