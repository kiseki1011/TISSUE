package components

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
)

// fg is per-chip because terminals render 4/5 dark and 6/7 light — white-on-cyan is under 3:1.
type hierarchyChip struct{ bg, fg lipgloss.ANSIColor }

// ANSI indexes so the chips follow the terminal's own palette, not the active theme. None is index
// 8 — the ANSI theme uses it for Selection, so that chip would vanish into a selected row.
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

// HierarchyChip renders nothing for an empty hierarchy, so callers can place it unconditionally.
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
