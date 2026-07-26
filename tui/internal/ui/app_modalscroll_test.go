package ui

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
)

// On a terminal too short for the options modal, the modal is windowed to the terminal height with a
// scrollbar, and the wheel scrolls the window (revealing the logout button at the bottom).
func TestOptionsModalWindowsAndScrolls(t *testing.T) {
	a := optionsApp(nil)
	a.width, a.height = 60, 12 // shorter than the Account section needs
	m, _ := a.Update(keyComma())
	app := m.(App)
	m, _ = app.Update(optKeyTab())     // Info -> Settings
	m, _ = m.(App).Update(optKeyTab()) // Settings -> Account (tallest)
	app = m.(App)

	if !app.modalWindowed() {
		t.Fatalf("options modal not windowed at height 12 (full=%d)", lipgloss.Height(app.modal.View()))
	}
	if h := lipgloss.Height(app.modalView()); h != 12 {
		t.Errorf("windowed modal height = %d, want 12", h)
	}
	if !strings.ContainsAny(stripCSI(app.modalView()), "█░") {
		t.Error("windowed modal has no scrollbar")
	}

	// wheel down advances the window; it clamps at the bottom (revealing the logout button) and does
	// not scroll past it
	var am tea.Model = app
	for i := 0; i < 20; i++ {
		am, _ = am.(App).Update(tea.MouseWheelMsg{Button: tea.MouseWheelDown})
	}
	app2 := am.(App)
	if app2.modalScroll == 0 {
		t.Error("wheel down did not advance the modal scroll offset")
	}
	bottomOff := app2.modalScroll
	if !strings.Contains(stripCSI(app2.modalView()), "Log out") {
		t.Error("scrolling to the bottom did not reveal the logout button")
	}
	// one more wheel-down must not move past the clamped bottom
	am, _ = app2.Update(tea.MouseWheelMsg{Button: tea.MouseWheelDown})
	if off := am.(App).modalScroll; off != bottomOff {
		t.Errorf("scrolled past the bottom: offset = %d, want %d", off, bottomOff)
	}
	// wheel up all the way clamps at the top
	for i := 0; i < 20; i++ {
		am, _ = am.(App).Update(tea.MouseWheelMsg{Button: tea.MouseWheelUp})
	}
	if off := am.(App).modalScroll; off != 0 {
		t.Errorf("wheel up did not clamp to the top: offset = %d", off)
	}
}

// A modal that fits the terminal is not windowed, so its own scroll (e.g. the help viewport) keeps
// the wheel and no host scrollbar is drawn.
func TestModalNotWindowedWhenItFits(t *testing.T) {
	a := optionsApp(nil)
	a.width, a.height = 100, 40 // ample room
	m, _ := a.Update(keyComma())
	app := m.(App)
	if app.modalWindowed() {
		t.Error("modal reported windowed on a tall terminal")
	}
	if app.modalView() != app.modal.View() {
		t.Error("a fitting modal must render unchanged (no host scrollbar)")
	}
}
