package widgets

import (
	"strings"
	"testing"

	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// A single-select picker reports no multi state and a nil Selections; Toggle is a no-op.
func TestListPickerSingleSelectHasNoMulti(t *testing.T) {
	p := NewListPicker("Pick", listOpts, "1", 8, 20)
	if p.Multi() {
		t.Error("a single-select picker should not be multi")
	}
	if p.Selections() != nil {
		t.Errorf("single-select Selections should be nil, got %v", p.Selections())
	}
	if p.Toggle().Selections() != nil {
		t.Error("Toggle on a single-select picker must be a no-op")
	}
}

// A multi picker seeds its checked set from preChecked and returns them in option order.
func TestMultiListPickerSeedsAndOrders(t *testing.T) {
	p := NewMultiListPicker("Reviewers", listOpts, []string{"3", "1"}, 8, 24)
	if !p.Multi() {
		t.Fatal("NewMultiListPicker should be multi")
	}
	got := p.Selections()
	if len(got) != 2 || got[0] != "1" || got[1] != "3" {
		t.Errorf("Selections should be in option order [1 3], got %v", got)
	}
}

// Toggle flips the option under the cursor without mutating the original picker (Elm copy semantics).
func TestMultiListPickerToggleCopies(t *testing.T) {
	p := NewMultiListPicker("Reviewers", listOpts, nil, 8, 24) // cursor 0 = Alpha (value "1")
	toggled := p.Toggle()
	if got := toggled.Selections(); len(got) != 1 || got[0] != "1" {
		t.Errorf("toggling cursor 0 should check value 1, got %v", got)
	}
	if len(p.Selections()) != 0 {
		t.Error("Toggle must not mutate the original picker's checked set")
	}
	if got := toggled.Toggle().Selections(); len(got) != 0 {
		t.Errorf("re-toggling should uncheck, got %v", got)
	}
}

// A checked value with no matching option (pre-checked but absent from the option set) is still returned
// by Selections, so a multi-select never silently drops it - in-option values come first, in order.
func TestMultiListPickerCarriesUnlistedChecked(t *testing.T) {
	p := NewMultiListPicker("Reviewers", listOpts, []string{"2", "9"}, 8, 24) // "9" is not in listOpts
	got := p.Selections()
	set := map[string]bool{}
	for _, v := range got {
		set[v] = true
	}
	if !set["2"] || !set["9"] {
		t.Errorf("Selections should carry the unlisted checked value 9, got %v", got)
	}
	if len(got) == 0 || got[0] != "2" {
		t.Errorf("in-option checked values should come first in order, got %v", got)
	}
}

// The multi view renders a checkbox per row, ticked for checked options.
func TestMultiListPickerViewShowsCheckboxes(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewMultiListPicker("Reviewers", listOpts, []string{"2"}, 8, 24)
	view := zone.Scan(p.View(s))
	if !strings.Contains(view, "[x]") {
		t.Errorf("a checked option should render [x]:\n%s", view)
	}
	if !strings.Contains(view, "[ ]") {
		t.Errorf("unchecked options should render [ ]:\n%s", view)
	}
}
