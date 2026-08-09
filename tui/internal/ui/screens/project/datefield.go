package project

import (
	"time"

	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// dateTarget is which field a confirmed calendar pick fills.
type dateTarget int

const (
	dateDueCreate     dateTarget = iota // the create form's Due field
	dateDueEdit                         // the edit form's Due field
	dateCustom                          // a DATE/TIMESTAMP custom field in the create form (by dateCustomIx)
	dateCustomEdit                      // the same, in the edit form
	dateSprintStart                     // the required due date that starts a sprint (fires the start command)
	dateSprintEditDue                   // the sprint edit form's Due field
)

// datePickerW is the calendar modal's content width (a touch wider than the natural grid for margin).
const datePickerW = 24

// The create/edit forms emit these to ask the model to open the calendar picker over them; the model
// owns the overlay, mirroring the parent-picker flow.
type openDueCreateMsg struct{}
type openDueEditMsg struct{}
type openCustomDateMsg struct{ index int }
type openCustomDateEditMsg struct{ index int }

func openDueCreate() tea.Msg       { return openDueCreateMsg{} }
func openDueEdit() tea.Msg         { return openDueEditMsg{} }
func openCustomDate(i int) tea.Cmd { return func() tea.Msg { return openCustomDateMsg{index: i} } }
func openCustomDateEdit(i int) tea.Cmd {
	return func() tea.Msg { return openCustomDateEditMsg{index: i} }
}

// openDuePicker opens the calendar over the create or edit form to set its (date-only, clearable) Due.
func (m Model) openDuePicker(target dateTarget) (Model, tea.Cmd) {
	var initial time.Time
	switch target {
	case dateDueCreate:
		if m.createUI.dueSet {
			initial = m.createUI.dueAt
		}
	case dateDueEdit:
		if m.editUI.dueSet {
			initial = m.editUI.dueAt
		}
	}
	m.dating = true
	m.dateTarget = target
	m.datePick = widgets.NewDatePicker("Due date", initial, false, true, datePickerW)
	return m, nil
}

// openCustomDatePickerEdit is openCustomDatePicker for the edit form's custom fields.
func (m Model) openCustomDatePickerEdit(i int) (Model, tea.Cmd) {
	if i < 0 || i >= len(m.editUI.customFields) {
		return m, nil
	}
	c := m.editUI.customFields[i]
	if !c.isDate() {
		return m, nil
	}
	m.dating = true
	m.dateTarget = dateCustomEdit
	m.dateCustomIx = i
	withTime := c.field.Type == "TIMESTAMP"
	m.datePick = widgets.NewDatePicker(c.field.Name, c.dateInitial(), withTime, true, datePickerW)
	return m, nil
}

// openCustomDatePicker opens the calendar over the create form to set a DATE (date-only) or TIMESTAMP
// (with time) custom field. A required field offers no Clear.
func (m Model) openCustomDatePicker(i int) (Model, tea.Cmd) {
	if i < 0 || i >= len(m.createUI.customFields) {
		return m, nil
	}
	c := m.createUI.customFields[i]
	if !c.isDate() {
		return m, nil
	}
	m.dating = true
	m.dateTarget = dateCustom
	m.dateCustomIx = i
	withTime := c.field.Type == "TIMESTAMP"
	m.datePick = widgets.NewDatePicker(c.field.Name, c.dateInitial(), withTime, !c.field.Required, datePickerW)
	return m, nil
}

// updateDatePicker drives the open calendar: esc cancels, enter confirms the selection, delete clears an
// optional field, a click picks a day (a date-only day click also confirms) or nudges the month/time,
// and any other key navigates the grid or steps the time.
func (m Model) updateDatePicker(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		switch msg.String() {
		case "esc":
			m.dating = false
			return m, nil
		case "enter":
			return m.confirmDate(m.datePick.Value(), true)
		case "delete":
			if m.datePick.AllowClear() {
				return m.confirmDate(time.Time{}, false)
			}
			return m, nil
		}
		m.datePick = m.datePick.Update(msg)
		return m, nil
	case tea.MouseClickMsg:
		if msg.Button != tea.MouseLeft {
			return m, nil
		}
		if t, ok := m.datePick.HitDay(msg); ok {
			m.datePick = m.datePick.SetSel(t)
			if !m.datePick.WithTime() { // a date-only day click both selects and confirms
				return m.confirmDate(m.datePick.Value(), true)
			}
			return m, nil
		}
		if delta, ok := m.datePick.HitMonthNav(msg); ok {
			m.datePick = m.datePick.MoveMonth(delta)
			return m, nil
		}
		if np, ok := m.datePick.HitTimeSegment(msg); ok {
			m.datePick = np
			return m, nil
		}
		if m.datePick.HitClear(msg) {
			return m.confirmDate(time.Time{}, false)
		}
		if m.datePick.HitConfirm(msg) {
			return m.confirmDate(m.datePick.Value(), true)
		}
	}
	return m, nil
}

// dateHelpKeys are the footer hints while the calendar is open: grid/month navigation, the time stepper
// (timed fields only), select, an optional clear, and cancel.
func (m Model) dateHelpKeys() []key.Binding {
	binds := []key.Binding{
		key.NewBinding(key.WithKeys("left", "right", "up", "down"), key.WithHelp("←/→/↑/↓", "day")),
		key.NewBinding(key.WithKeys("pgup", "pgdown"), key.WithHelp("PgUp/PgDn", "month")),
	}
	if m.datePick.WithTime() {
		binds = append(binds, key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "time")))
	}
	binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")))
	if m.datePick.AllowClear() {
		binds = append(binds, key.NewBinding(key.WithKeys("delete"), key.WithHelp("del", "clear")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")))
}

// confirmDate closes the picker and writes the pick back into the field it was opened for (set=false
// clears an optional field).
func (m Model) confirmDate(v time.Time, set bool) (Model, tea.Cmd) {
	m.dating = false
	switch m.dateTarget {
	case dateDueCreate:
		m.createUI = m.createUI.setDue(v, set)
	case dateDueEdit:
		m.editUI = m.editUI.setDue(v, set)
	case dateCustom:
		if i := m.dateCustomIx; i >= 0 && i < len(m.createUI.customFields) {
			m.createUI.customFields[i] = m.createUI.customFields[i].setDate(v, set)
		}
	case dateCustomEdit:
		m.editUI = m.editUI.setCustomDate(m.dateCustomIx, v, set)
	case dateSprintStart:
		// the due date is required and non-clearable, so a confirm always starts the sprint
		return m, startSprintCmd(m.deps, m.sprintActionID, v)
	case dateSprintEditDue:
		m.sprintEditUI = m.sprintEditUI.setDue(v, set)
	}
	return m, nil
}
