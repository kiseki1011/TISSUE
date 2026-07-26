package components

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"
)

// TitledBoxWeighted signals focus by border COLOUR, not weight: focused and unfocused draw the same
// rounded border, so toggling focus never shifts the layout and never thickens the border.
func TestTitledBoxWeighted(t *testing.T) {
	body := lipgloss.NewStyle().Padding(0, 1).Render("hello")
	col := lipgloss.Color("#7aa2f7")
	blur := TitledBoxWeighted("Name", body, col, false)
	focus := TitledBoxWeighted("Name", body, col, true)

	// same colour in and out of focus => identical render; focus never changes the border weight
	if blur != focus {
		t.Error("focus changed the border; TitledBoxWeighted must signal focus by colour only")
	}
	if blur != TitledBox("Name", body, col) {
		t.Error("TitledBoxWeighted must equal TitledBox (rounded, colour-only focus)")
	}

	plain := StripANSI(focus)
	if !strings.ContainsAny(plain, "╭╮╰╯") || strings.ContainsAny(plain, "┌┐└┘┏┓┗┛┃━") {
		t.Errorf("box is not rounded (or introduced heavy glyphs):\n%s", plain)
	}
}

// TitledRule is the "Details" look: a titled top rule and a bottom rule, NO side borders — yet it
// must occupy the exact same footprint as the boxed TitledBox so it drops into a boxed layout
// without shifting neighbours. It carries a right-inset subtitle and its rules always stay light
// (focus is shown by the rule colour, never by weight).
func TestTitledRule(t *testing.T) {
	body := lipgloss.NewStyle().Width(20).Render("hello\nworld")
	rule := TitledRule("Projects", "3/9", body, lipgloss.Color("#7aa2f7"))
	box := TitledBox("Projects", body, lipgloss.Color("#7aa2f7"))

	plain := StripANSI(rule)
	// no vertical/corner glyphs of any weight — the sides are blank
	if strings.ContainsAny(plain, "│┃╭╮╰╯┌┐└┘┏┓┗┛") {
		t.Errorf("TitledRule drew a side border or corner:\n%s", plain)
	}
	// the rules stay light — focus never thickens them
	if strings.ContainsAny(plain, "━┃┏┓┗┛") {
		t.Errorf("TitledRule used a heavy glyph; the rules must stay light:\n%s", plain)
	}
	// the title and subtitle appear on the rules
	lines := strings.Split(plain, "\n")
	if !strings.Contains(lines[0], "Projects") {
		t.Errorf("top rule missing the title: %q", lines[0])
	}
	if last := lines[len(lines)-1]; !strings.Contains(last, "3/9") {
		t.Errorf("bottom rule missing the subtitle: %q", last)
	}
	// identical footprint to the boxed version, so it drops in without reflowing the layout
	if lipgloss.Width(rule) != lipgloss.Width(box) || lipgloss.Height(rule) != lipgloss.Height(box) {
		t.Errorf("TitledRule footprint %dx%d != TitledBox %dx%d",
			lipgloss.Width(rule), lipgloss.Height(rule), lipgloss.Width(box), lipgloss.Height(box))
	}
}
