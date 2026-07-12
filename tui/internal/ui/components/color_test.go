package components

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"
)

// Every backend ColorType name must resolve, or its issue types render without a swatch.
// This list mirrors com.tissue.shared.enums.ColorType.
var allColorTypes = []string{
	"ANSI_BLACK", "ANSI_RED", "ANSI_GREEN", "ANSI_YELLOW", "ANSI_BLUE", "ANSI_MAGENTA",
	"ANSI_CYAN", "ANSI_WHITE", "ANSI_BRIGHT_BLACK", "ANSI_BRIGHT_RED", "ANSI_BRIGHT_GREEN",
	"ANSI_BRIGHT_YELLOW", "ANSI_BRIGHT_BLUE", "ANSI_BRIGHT_MAGENTA", "ANSI_BRIGHT_CYAN",
	"ANSI_BRIGHT_WHITE", "PINK", "MAROON", "RED", "ORANGERED", "DARKORANGE", "LIMEGREEN",
	"LIGHTGREEN", "LIGHTYELLOW", "MEDIUMBLUE", "MIDNIGHTBLUE", "INDIGO", "MAGENTA", "BROWN", "TAN",
}

func TestEveryColorTypeResolves(t *testing.T) {
	for _, name := range allColorTypes {
		if _, ok := IssueColor(name); !ok {
			t.Errorf("ColorType %q does not resolve to a color", name)
		}
		if ColorSwatch(name) == "" {
			t.Errorf("ColorType %q renders no swatch", name)
		}
	}
}

func TestUnknownAndEmptyColorHaveNoSwatch(t *testing.T) {
	for _, name := range []string{"", "  ", "NOT_A_COLOR"} {
		if _, ok := IssueColor(name); ok {
			t.Errorf("%q should not resolve", name)
		}
		if got := ColorSwatch(name); got != "" {
			t.Errorf("%q swatch = %q, want empty", name, got)
		}
	}
}

// The extended-color hexes must be well-formed; the backend stores two of them without a
// leading '#', so this pins the correction.
func TestExtendedColorsAreValidHex(t *testing.T) {
	for name, hex := range extendedColorHex {
		if !strings.HasPrefix(hex, "#") || len(hex) != 7 {
			t.Errorf("%s hex %q is malformed", name, hex)
		}
	}
}

func TestColorLabelHumanizes(t *testing.T) {
	cases := map[string]string{
		"ANSI_BRIGHT_RED": "ANSI Bright Red",
		"INDIGO":          "Indigo",
		"ORANGERED":       "Orangered",
	}
	for in, want := range cases {
		if got := ColorLabel(in); got != want {
			t.Errorf("ColorLabel(%q) = %q, want %q", in, got, want)
		}
	}
}

// A chip paints the text on the color as a background, and picks a foreground that stays
// legible: black on light colors, white on dark ones.
func TestColorChipContrastsText(t *testing.T) {
	const black = "38;2;0;0;0"
	const white = "38;2;255;255;255"
	cases := map[string]string{
		"LIGHTYELLOW":       black, // very light
		"PINK":              black,
		"ANSI_BRIGHT_WHITE": black,
		"LIMEGREEN":         black,
		"INDIGO":            white, // very dark
		"MIDNIGHTBLUE":      white,
		"ANSI_BLUE":         white,
		"MAROON":            white,
	}
	for name, wantFG := range cases {
		chip, ok := ColorChip(name, "x")
		if !ok {
			t.Errorf("%s produced no chip", name)
			continue
		}
		if !strings.Contains(chip, "48") { // a background SGR must be present
			t.Errorf("%s chip has no background: %q", name, chip)
		}
		if !strings.Contains(chip, wantFG) {
			t.Errorf("%s chip foreground = %q, want it to contain %q", name, chip, wantFG)
		}
	}
}

func TestColorChipUnknownFallsBack(t *testing.T) {
	for _, name := range []string{"", "NOPE"} {
		if chip, ok := ColorChip(name, "x"); ok || chip != "" {
			t.Errorf("ColorChip(%q) = (%q, %v), want (\"\", false)", name, chip, ok)
		}
	}
}

// ANSI names must map to ANSI palette indexes (so swatches follow the terminal theme), not
// to a hardcoded hex.
func TestANSIColorsUsePalette(t *testing.T) {
	c, ok := IssueColor("ANSI_RED")
	if !ok {
		t.Fatal("ANSI_RED did not resolve")
	}
	if _, isANSI := c.(lipgloss.ANSIColor); !isANSI {
		t.Errorf("ANSI_RED = %T, want lipgloss.ANSIColor", c)
	}
}
