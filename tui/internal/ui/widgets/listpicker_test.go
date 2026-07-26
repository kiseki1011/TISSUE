package widgets

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var listOpts = []PickerOption{
	{Value: "1", Label: "Alpha"}, {Value: "2", Label: "Beta"}, {Value: "3", Label: "Gamma"},
}

// The picker seeds its cursor at the option whose Value matches current and reports it as selected.
func TestListPickerSeedsCurrent(t *testing.T) {
	p := NewListPicker("Pick", listOpts, "2", 8, 20)
	if p.Cursor != 1 {
		t.Fatalf("seeded cursor %d, want 1 (Beta)", p.Cursor)
	}
	if got, _ := p.Selected(); got.Label != "Beta" {
		t.Errorf("selected = %q, want Beta", got.Label)
	}
}

// Move wraps around both ends of the list.
func TestListPickerMoveWraps(t *testing.T) {
	p := NewListPicker("Pick", listOpts, "1", 8, 20) // cursor 0
	if got := p.Move(-1).Cursor; got != 2 {
		t.Errorf("up from the top wrapped to %d, want 2", got)
	}
	if got := p.Move(1).Move(1).Move(1).Cursor; got != 0 {
		t.Errorf("three downs from the top wrapped to %d, want 0", got)
	}
}

// The View lists every option's label inside a titled box.
func TestListPickerViewListsOptions(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewListPicker("Themes", listOpts, "1", 8, 20)
	view := zone.Scan(p.View(s))
	for _, want := range []string{"Themes", "Alpha", "Beta", "Gamma"} {
		if !strings.Contains(view, want) {
			t.Errorf("view missing %q:\n%s", want, view)
		}
	}
}

// Each option is a click zone; clicking one resolves to its index.
func TestListPickerClickHitsOption(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewListPicker("Pick", listOpts, "1", 8, 20)
	const target = 2
	settleZone(t, p.View(s), ListPickerOptZone(target))
	z := zone.Get(ListPickerOptZone(target))
	click := tea.MouseClickMsg{X: z.StartX, Y: z.StartY, Button: tea.MouseLeft}
	if got := p.HitOption(click); got != target {
		t.Fatalf("HitOption = %d, want %d", got, target)
	}
}
