package components

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
)

// fg is per-chip, not one constant, because terminals render 4/5 dark and 6/7 light,
// and white-on-cyan measures under 3:1 on most palettes.
type hierarchyChip struct{ bg, fg lipgloss.ANSIColor }

// Uses ANSI indexes so the chips follow the terminal's own palette rather than the
// active theme.
//
// None of these is index 8: the ANSI theme uses 8 for Selection, Surface, Muted and
// Border, so a chip painted with it would vanish into a selected row.
var hierarchyChips = map[string]hierarchyChip{
	"EPIC":      {bg: 5, fg: 15},
	"STANDARD":  {bg: 4, fg: 15},
	"SUBTASK":   {bg: 6, fg: 0},
	"MICROTASK": {bg: 7, fg: 0},
}

var unknownChip = hierarchyChip{bg: 7, fg: 0}

func chipFor(hierarchy string) hierarchyChip {
	if c, ok := hierarchyChips[strings.ToUpper(hierarchy)]; ok {
		return c
	}
	return unknownChip
}

func HierarchyColor(hierarchy string) color.Color { return chipFor(hierarchy).bg }

// An empty hierarchy renders nothing, so callers can drop it into a layout unconditionally.
func HierarchyChip(hierarchy string) string {
	if hierarchy == "" {
		return ""
	}
	c := chipFor(hierarchy)
	return lipgloss.NewStyle().
		Foreground(c.fg).
		Background(c.bg).
		Bold(true).
		Render(" " + hierarchy + " ")
}
