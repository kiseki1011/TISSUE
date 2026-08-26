package components

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"
)

var hierarchies = []string{"EPIC", "STANDARD", "SUBTASK", "MICROTASK"}

// Other render tests strip ANSI, so the chip's background is only observable here.
func TestHierarchyChipPaintsBackground(t *testing.T) {
	for _, h := range hierarchies {
		chip := HierarchyChip(h)
		if !strings.Contains(chip, "48;5;") {
			t.Errorf("%s chip has no background SGR: %q", h, chip)
		}
		if !strings.Contains(chip, h) {
			t.Errorf("%s chip lost its label: %q", h, chip)
		}
		if got, want := lipgloss.Width(chip), len(h)+2; got != want {
			t.Errorf("%s chip = %d cells, want %d (one space of padding each side)", h, got, want)
		}
	}
	if got := HierarchyChip(""); got != "" {
		t.Errorf("empty hierarchy = %q, want no chip", got)
	}
}

// Four levels the eye has to tell apart at a glance need four distinct colors.
func TestHierarchyColorsAreDistinct(t *testing.T) {
	seen := map[string]string{}
	for _, h := range hierarchies {
		key := lipgloss.NewStyle().Background(HierarchyColor(h)).Render("x")
		if prev, ok := seen[key]; ok {
			t.Errorf("%s and %s render the same background color", prev, h)
		}
		seen[key] = h
	}
}

// The ANSI theme paints Selection with color 8, so a chip using it vanishes into a selected row.
func TestHierarchyColorsAvoidANSIGray(t *testing.T) {
	gray := lipgloss.NewStyle().Background(lipgloss.ANSIColor(8)).Render("x")
	for _, h := range append(hierarchies, "SOMETHING_UNKNOWN") {
		if lipgloss.NewStyle().Background(HierarchyColor(h)).Render("x") == gray {
			t.Errorf("%s uses ANSI 8, which collides with the ANSI theme's Selection", h)
		}
	}
}

// Casing comes from whatever the API returns. An unknown value must still render.
func TestHierarchyChipTolerantOfInput(t *testing.T) {
	if HierarchyColor("epic") != HierarchyColor("EPIC") {
		t.Error("lowercase hierarchy does not resolve to its uppercase color")
	}
	if chip := HierarchyChip("FUTURE_LEVEL"); !strings.Contains(chip, "FUTURE_LEVEL") {
		t.Errorf("unknown hierarchy = %q, want a chip carrying the raw label", chip)
	}
}
