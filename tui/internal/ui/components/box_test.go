package components

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"
)

// Focus is signalled by border COLOUR, not weight, so toggling it never shifts the layout.
func TestTitledBoxWeighted(t *testing.T) {
	body := lipgloss.NewStyle().Padding(0, 1).Render("hello")
	col := lipgloss.Color("#7aa2f7")
	blur := TitledBoxWeighted("Name", body, col, false)
	focus := TitledBoxWeighted("Name", body, col, true)

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

// TitledRule has no side borders yet must occupy the same footprint as TitledBox, so it drops into
// a boxed layout without shifting neighbours. Its rules stay light — focus shows in the colour.
func TestTitledRule(t *testing.T) {
	body := lipgloss.NewStyle().Width(20).Render("hello\nworld")
	rule := TitledRule("Projects", "3/9", body, lipgloss.Color("#7aa2f7"))
	box := TitledBox("Projects", body, lipgloss.Color("#7aa2f7"))

	plain := StripANSI(rule)
	if strings.ContainsAny(plain, "│┃╭╮╰╯┌┐└┘┏┓┗┛") {
		t.Errorf("TitledRule drew a side border or corner:\n%s", plain)
	}
	if strings.ContainsAny(plain, "━┃┏┓┗┛") {
		t.Errorf("TitledRule used a heavy glyph; the rules must stay light:\n%s", plain)
	}
	lines := strings.Split(plain, "\n")
	if !strings.Contains(lines[0], "Projects") {
		t.Errorf("top rule missing the title: %q", lines[0])
	}
	if last := lines[len(lines)-1]; !strings.Contains(last, "3/9") {
		t.Errorf("bottom rule missing the subtitle: %q", last)
	}
	if lipgloss.Width(rule) != lipgloss.Width(box) || lipgloss.Height(rule) != lipgloss.Height(box) {
		t.Errorf("TitledRule footprint %dx%d != TitledBox %dx%d",
			lipgloss.Width(rule), lipgloss.Height(rule), lipgloss.Width(box), lipgloss.Height(box))
	}
}
