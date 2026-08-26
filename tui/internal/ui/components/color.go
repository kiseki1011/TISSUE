package components

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
)

// The backend ColorType enum names a color but not how to draw it. ANSI_* map to real ANSI indexes
// so swatches follow the terminal's own palette. The extended names get their hex below.
var ansiColorIndex = map[string]int{
	"ANSI_BLACK": 0, "ANSI_RED": 1, "ANSI_GREEN": 2, "ANSI_YELLOW": 3,
	"ANSI_BLUE": 4, "ANSI_MAGENTA": 5, "ANSI_CYAN": 6, "ANSI_WHITE": 7,
	"ANSI_BRIGHT_BLACK": 8, "ANSI_BRIGHT_RED": 9, "ANSI_BRIGHT_GREEN": 10, "ANSI_BRIGHT_YELLOW": 11,
	"ANSI_BRIGHT_BLUE": 12, "ANSI_BRIGHT_MAGENTA": 13, "ANSI_BRIGHT_CYAN": 14, "ANSI_BRIGHT_WHITE": 15,
}

// The two backend entries missing their '#' (DARKORANGE, LIGHTGREEN) are corrected here.
var extendedColorHex = map[string]string{
	"PINK": "#FFC0CB", "MAROON": "#800000", "RED": "#FF0000", "ORANGERED": "#FF4500",
	"DARKORANGE": "#FF8C00", "LIMEGREEN": "#32CD32", "LIGHTGREEN": "#90EE90", "LIGHTYELLOW": "#FFFFE0",
	"MEDIUMBLUE": "#0000CD", "MIDNIGHTBLUE": "#191970", "INDIGO": "#4B0082", "MAGENTA": "#FF00FF",
	"BROWN": "#A52A2A", "TAN": "#D2B48C",
}

// Stable picker order: the ANSI names first, then the extended ones.
var colorTypeOrder = []string{
	"ANSI_BLACK", "ANSI_RED", "ANSI_GREEN", "ANSI_YELLOW", "ANSI_BLUE", "ANSI_MAGENTA",
	"ANSI_CYAN", "ANSI_WHITE", "ANSI_BRIGHT_BLACK", "ANSI_BRIGHT_RED", "ANSI_BRIGHT_GREEN",
	"ANSI_BRIGHT_YELLOW", "ANSI_BRIGHT_BLUE", "ANSI_BRIGHT_MAGENTA", "ANSI_BRIGHT_CYAN",
	"ANSI_BRIGHT_WHITE", "PINK", "MAROON", "RED", "ORANGERED", "DARKORANGE", "LIMEGREEN",
	"LIGHTGREEN", "LIGHTYELLOW", "MEDIUMBLUE", "MIDNIGHTBLUE", "INDIGO", "MAGENTA", "BROWN", "TAN",
}

func ColorTypeNames() []string { return colorTypeOrder }

// IssueColor resolves a backend ColorType name. ok is false for empty/unknown so callers can omit
// the swatch.
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

func ColorSwatch(name string) string {
	c, ok := IssueColor(name)
	if !ok {
		return ""
	}
	return lipgloss.NewStyle().Foreground(c).Render("██")
}

// RGBA luminance covers both ANSI and hex with one formula. #000/#fff rather than ANSI 0/15 so the
// text stays true black/white whatever the terminal's palette.
func contrastText(bg color.Color) color.Color {
	r, g, b, _ := bg.RGBA()
	lum := (0.299*float64(r) + 0.587*float64(g) + 0.114*float64(b)) / 257.0 // RGBA is 0..65535
	if lum > 128 {
		return lipgloss.Color("#000000")
	}
	return lipgloss.Color("#ffffff")
}

// ChipColors serves callers painting onto a cell grid that need the colors, not a styled string.
func ChipColors(name string) (bg, fg color.Color, ok bool) {
	bg, ok = IssueColor(name)
	if !ok {
		return nil, nil, false
	}
	return bg, contrastText(bg), true
}

// ColorChip returns a styled chip. ok is false for empty/unknown so callers fall back to plain text.
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

// ColorLabel humanizes a ColorType name: "ANSI_BRIGHT_RED" -> "ANSI Bright Red".
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
