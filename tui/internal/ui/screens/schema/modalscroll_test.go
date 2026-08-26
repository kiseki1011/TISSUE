package schema

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
)

func openCreateFieldModal(t *testing.T) Model {
	t.Helper()
	m := typeFieldsModel(t)
	for i := 0; i < len(m.typeElems())-1; i++ {
		m = m.moveTypeElem(keyDown())
	}
	m, _ = m.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if !m.creatingField {
		t.Fatal("create-field modal did not open")
	}
	return m
}

// A modal taller than the terminal is windowed with a scrollbar. Closing it resets the offset.
func TestSchemaModalWindowsScrollsAndResets(t *testing.T) {
	m := openCreateFieldModal(t)
	view, ok := m.activeModalView()
	if !ok {
		t.Fatal("activeModalView reported no open modal")
	}
	full := lipgloss.Height(view)

	m.height = 14 // shorter than the New Field modal
	if !strings.ContainsAny(stripANSI(m.View()), "█░") {
		t.Fatalf("windowed schema modal has no scrollbar (full=%d, term=%d)", full, m.height)
	}

	m, _ = m.Update(tea.MouseWheelMsg{Button: tea.MouseWheelDown})
	m, _ = m.Update(tea.MouseWheelMsg{Button: tea.MouseWheelDown})
	if m.modalScroll == 0 {
		t.Error("wheel did not scroll the schema modal window")
	}

	// any dashboard event with no modal floating resets the offset
	m.creatingField = false
	m, _ = m.Update(tea.MouseMotionMsg{})
	if m.modalScroll != 0 {
		t.Errorf("offset not reset after the modal closed: %d", m.modalScroll)
	}
}

// A modal that fits is not windowed, so the wheel is left for the modal itself.
func TestSchemaModalNotWindowedWhenItFits(t *testing.T) {
	m := openCreateFieldModal(t)
	view, _ := m.activeModalView()
	m.height = lipgloss.Height(view) + 6 // ample room
	before := m.modalScroll
	m, _ = m.Update(tea.MouseWheelMsg{Button: tea.MouseWheelDown})
	if m.modalScroll != before {
		t.Error("wheel scrolled a modal that fits the terminal")
	}
}
