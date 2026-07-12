package components

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
)

// The backend ColorType enum (com.tissue.shared.enums.ColorType) is a plain identifier set
// carried over from the Textual client; it tells us which color a caller picked, not how to
// draw it ("Let the client render the color based on the enum value"). We map the sixteen
// ANSI_* names to real ANSI indexes so those swatches follow the terminal's own palette,
// exactly as Textual rendered them, and give the extended names their hex.
var ansiColorIndex = map[string]int{
	"ANSI_BLACK": 0, "ANSI_RED": 1, "ANSI_GREEN": 2, "ANSI_YELLOW": 3,
	"ANSI_BLUE": 4, "ANSI_MAGENTA": 5, "ANSI_CYAN": 6, "ANSI_WHITE": 7,
	"ANSI_BRIGHT_BLACK": 8, "ANSI_BRIGHT_RED": 9, "ANSI_BRIGHT_GREEN": 10, "ANSI_BRIGHT_YELLOW": 11,
	"ANSI_BRIGHT_BLUE": 12, "ANSI_BRIGHT_MAGENTA": 13, "ANSI_BRIGHT_CYAN": 14, "ANSI_BRIGHT_WHITE": 15,
}

// extendedColorHex holds the non-ANSI names. The two backend entries missing their '#'
// (DARKORANGE, LIGHTGREEN) are corrected here.
var extendedColorHex = map[string]string{
	"PINK": "#FFC0CB", "MAROON": "#800000", "RED": "#FF0000", "ORANGERED": "#FF4500",
	"DARKORANGE": "#FF8C00", "LIMEGREEN": "#32CD32", "LIGHTGREEN": "#90EE90", "LIGHTYELLOW": "#FFFFE0",
	"MEDIUMBLUE": "#0000CD", "MIDNIGHTBLUE": "#191970", "INDIGO": "#4B0082", "MAGENTA": "#FF00FF",
	"BROWN": "#A52A2A", "TAN": "#D2B48C",
}

// colorTypeOrder lists every ColorType in a stable order (the sixteen ANSI names, then the
// fourteen extended names), so a color picker can cycle through them deterministically.
var colorTypeOrder = []string{
	"ANSI_BLACK", "ANSI_RED", "ANSI_GREEN", "ANSI_YELLOW", "ANSI_BLUE", "ANSI_MAGENTA",
	"ANSI_CYAN", "ANSI_WHITE", "ANSI_BRIGHT_BLACK", "ANSI_BRIGHT_RED", "ANSI_BRIGHT_GREEN",
	"ANSI_BRIGHT_YELLOW", "ANSI_BRIGHT_BLUE", "ANSI_BRIGHT_MAGENTA", "ANSI_BRIGHT_CYAN",
	"ANSI_BRIGHT_WHITE", "PINK", "MAROON", "RED", "ORANGERED", "DARKORANGE", "LIMEGREEN",
	"LIGHTGREEN", "LIGHTYELLOW", "MEDIUMBLUE", "MIDNIGHTBLUE", "INDIGO", "MAGENTA", "BROWN", "TAN",
}

// ColorTypeNames returns every ColorType name in a stable order, for a color picker.
func ColorTypeNames() []string { return colorTypeOrder }

// IssueColor resolves a ColorType name to a renderable color. ok is false for an empty or
// unknown name, so callers can omit the swatch.
func IssueColor(name string) (color.Color, bool) {
	key := strings.ToUpper(strings.TrimSpace(name))
	if key == "" {
		return nil, false
	}
	if i, ok := ansiColorIndex[key]; ok {
		return lipgloss.ANSIColor(i), true //nolint:gosec // index is 0..15
	}
	if hex, ok := extendedColorHex[key]; ok {
		return lipgloss.Color(hex), true
	}
	return nil, false
}

// ColorSwatch renders a two-cell filled block in the issue type's color, or nothing when
// the color is empty or unrecognized.
func ColorSwatch(name string) string {
	c, ok := IssueColor(name)
	if !ok {
		return ""
	}
	return lipgloss.NewStyle().Foreground(c).Render("██")
}

// contrastText picks black or white — whichever stays legible on bg. It measures perceived
// luminance from the color's own RGBA (ANSIColor resolves to the standard palette, so the
// same formula covers both ANSI and hex), then flips above a mid threshold. #000/#fff are
// used rather than ANSI 0/15 so the text is true black/white regardless of the terminal's
// palette.
func contrastText(bg color.Color) color.Color {
	r, g, b, _ := bg.RGBA()
	lum := (0.299*float64(r) + 0.587*float64(g) + 0.114*float64(b)) / 257.0 // RGBA is 0..65535
	if lum > 128 {
		return lipgloss.Color("#000000")
	}
	return lipgloss.Color("#ffffff")
}

// ChipColors resolves a ColorType name to a background color and a foreground that contrasts
// against it. ok is false for an empty or unknown color, so callers can fall back to plain
// text. Callers that render onto a cell grid need the colors, not a pre-styled string.
func ChipColors(name string) (bg, fg color.Color, ok bool) {
	bg, ok = IssueColor(name)
	if !ok {
		return nil, nil, false
	}
	return bg, contrastText(bg), true
}

// ColorChip renders text as a badge painted in the ColorType's color with a foreground that
// contrasts against it. ok is false for an empty or unknown color, so callers can fall back
// to plain text instead of a swatch.
func ColorChip(name, text string) (string, bool) {
	bg, fg, ok := ChipColors(name)
	if !ok {
		return "", false
	}
	return lipgloss.NewStyle().
		Foreground(fg).
		Background(bg).
		Bold(true).
		Render(" " + text + " "), true
}

// ColorLabel is the human-readable form of a ColorType name: "ANSI_BRIGHT_RED" ->
// "ANSI Bright Red", "INDIGO" -> "Indigo".
func ColorLabel(name string) string {
	words := strings.Split(strings.TrimSpace(name), "_")
	for i, w := range words {
		if w == "" || w == "ANSI" {
			continue
		}
		words[i] = strings.ToUpper(w[:1]) + strings.ToLower(w[1:])
	}
	return strings.Join(words, " ")
}
