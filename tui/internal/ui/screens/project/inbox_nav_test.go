package project

import (
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// WithInitialFocus(issueKey) opens that issue's read-only peek on the first layout, then is consumed so
// a later resize does not re-open it.
func TestWithInitialFocusOpensIssuePeek(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps(), testKey, "Proj").WithInitialFocus(testKey+"-9", 0)
	m, cmd := m.Update(tea.WindowSizeMsg{Width: 120, Height: 30})
	if !m.peeking || m.peekKey != testKey+"-9" {
		t.Fatalf("peek not opened: peeking=%v key=%q", m.peeking, m.peekKey)
	}
	if cmd == nil {
		t.Error("expected a detail-load command for the peeked issue")
	}
	m.peeking, m.peekKey = false, "" // a later resize must not re-open it
	m, _ = m.Update(tea.WindowSizeMsg{Width: 100, Height: 28})
	if m.peeking {
		t.Error("pending issue focus should be consumed after the first layout")
	}
}

// WithInitialFocus(sprintID) lands on the Sprints tab and, once the sprint list loads, selects the
// notified sprint (loading its issues).
func TestWithInitialFocusSelectsSprint(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps(), testKey, "Proj").WithInitialFocus("", 42)
	m, cmd := m.Update(tea.WindowSizeMsg{Width: 120, Height: 30})
	if m.tab != tabSprints {
		t.Fatalf("tab = %v, want tabSprints", m.tab)
	}
	if m.navSprintID != 42 {
		t.Errorf("navSprintID = %d, want 42", m.navSprintID)
	}
	if cmd == nil {
		t.Error("expected a sprint-list load command")
	}
	m, _ = m.Update(sprintsLoadedMsg{key: testKey, gen: m.sprintReqGen, page: domain.SprintPage{Sprints: []domain.SprintSummary{
		{ID: 10, Key: "S-10", Title: "A", Status: "PLANNING"},
		{ID: 42, Key: "S-42", Title: "B", Status: "ACTIVE"},
	}}})
	if m.navSprintID != 0 {
		t.Error("navSprintID should be consumed on the sprint-list load")
	}
	if m.selSprintID != 42 {
		t.Errorf("selSprintID = %d, want 42 (notified sprint selected)", m.selSprintID)
	}
}

// When the notified sprint is not in the loaded page, the drill-in still lands on the Sprints tab and
// selects the default (newest) sprint rather than crashing or pinning the wrong one as the target.
func TestWithInitialFocusSprintNotInPage(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps(), testKey, "Proj").WithInitialFocus("", 999)
	m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 30})
	m, _ = m.Update(sprintsLoadedMsg{key: testKey, gen: m.sprintReqGen, page: domain.SprintPage{Sprints: []domain.SprintSummary{
		{ID: 10, Key: "S-10", Title: "A", Status: "ACTIVE"},
		{ID: 8, Key: "S-8", Title: "B", Status: "PLANNING"},
	}}})
	if m.tab != tabSprints {
		t.Errorf("tab = %v, want tabSprints", m.tab)
	}
	if m.navSprintID != 0 {
		t.Error("navSprintID should be consumed even when the target is absent")
	}
	if m.selSprintID != 10 {
		t.Errorf("selSprintID = %d, want 10 (default newest, target 999 absent)", m.selSprintID)
	}
}
