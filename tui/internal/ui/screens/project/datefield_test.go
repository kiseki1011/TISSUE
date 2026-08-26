package project

import (
	"strings"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// runCmd executes a command's message back through Update, so a form's open-picker request lands.
func runCmd(m Model, cmd tea.Cmd) Model {
	if cmd == nil {
		return m
	}
	m, _ = m.Update(cmd())
	return m
}

// Due is set from the calendar, never typed.
func TestCreateDueOpensCalendarAndConfirms(t *testing.T) {
	m := createReady(t)
	m, _ = m.Update(press("n"))
	m.createUI, _ = m.createUI.focusOn(nfDue)

	m, cmd := m.Update(press("enter"))
	m = runCmd(m, cmd)
	if !m.dating || m.dateTarget != dateDueCreate {
		t.Fatalf("Due enter should open the create calendar, dating=%v target=%v", m.dating, m.dateTarget)
	}
	if body := plain(m.View()); !strings.Contains(body, "OK") || !strings.Contains(body, "Su") {
		t.Errorf("the calendar should float over the form:\n%s", body)
	}

	m, _ = m.Update(press("enter")) // confirm the default (today)
	if m.dating {
		t.Error("confirming should close the calendar")
	}
	if !m.createUI.dueSet {
		t.Error("confirming should set the due date")
	}
}

func TestCreateDueClear(t *testing.T) {
	m := createReady(t)
	m, _ = m.Update(press("n"))
	m.createUI = m.createUI.setDue(time.Date(2026, 9, 1, 0, 0, 0, 0, time.UTC), true)
	m.createUI, _ = m.createUI.focusOn(nfDue)

	m, cmd := m.Update(press("enter"))
	m = runCmd(m, cmd)
	m, _ = m.Update(press("delete"))
	if m.dating {
		t.Error("clearing should close the calendar")
	}
	if m.createUI.dueSet {
		t.Error("delete should clear the due date")
	}
}

func TestEditDueOpensPrefilledAndMoves(t *testing.T) {
	m := editReady(t, sampleDetail()) // due 2026-08-15
	m, _ = m.Update(press("e"))
	m.editUI, _ = m.editUI.focusOn(efDue)

	m, cmd := m.Update(press("enter"))
	m = runCmd(m, cmd)
	if !m.dating || m.dateTarget != dateDueEdit {
		t.Fatalf("Due enter should open the edit calendar, dating=%v target=%v", m.dating, m.dateTarget)
	}
	if got := m.datePick.Value(); got.Format("2006-01-02") != "2026-08-15" {
		t.Errorf("the calendar should prefill from the issue's due, got %s", got.Format("2006-01-02"))
	}

	m, _ = m.Update(press("right")) // 15 -> 16
	m, _ = m.Update(press("enter"))
	if formatDateOnly(m.editUI.dueAt) != "2026-08-16" {
		t.Errorf("the moved day should apply, got %s", formatDateOnly(m.editUI.dueAt))
	}
}

// createWithDateFields opens the create form with a DATE and a TIMESTAMP custom field loaded.
func createWithDateFields(t *testing.T) Model {
	t.Helper()
	m := createReady(t)
	m.typeFields = map[int64][]domain.IssueField{5: {
		{ID: 20, Name: "When", Type: "DATE", Position: 1},
		{ID: 21, Name: "At", Type: "TIMESTAMP", Position: 2},
	}}
	m, _ = m.Update(press("n"))
	return m
}

func TestCustomDateFieldsOpenCalendar(t *testing.T) {
	m := createWithDateFields(t)
	if len(m.createUI.customFields) != 2 {
		t.Fatalf("expected 2 custom fields, got %d", len(m.createUI.customFields))
	}

	m.createUI, _ = m.createUI.focusOn(nfCustomBase) // the DATE field
	m, cmd := m.Update(press("enter"))
	m = runCmd(m, cmd)
	if !m.dating || m.dateTarget != dateCustom || m.dateCustomIx != 0 || m.datePick.WithTime() {
		t.Fatalf("DATE field should open a date-only calendar, dating=%v ix=%d timed=%v", m.dating, m.dateCustomIx, m.datePick.WithTime())
	}
	m, _ = m.Update(press("enter")) // confirm today
	if !m.createUI.customFields[0].dateSet {
		t.Error("confirming should set the DATE field")
	}
	if v, present, errMsg := m.createUI.customFields[0].value(); !present || errMsg != "" || v == "" {
		t.Errorf("a set DATE field should serialize, got v=%v present=%v err=%q", v, present, errMsg)
	}

	m.createUI, _ = m.createUI.focusOn(nfCustomBase + 1) // the TIMESTAMP field
	m, cmd = m.Update(press("enter"))
	m = runCmd(m, cmd)
	if !m.datePick.WithTime() {
		t.Error("TIMESTAMP field should open a timed calendar")
	}
}
