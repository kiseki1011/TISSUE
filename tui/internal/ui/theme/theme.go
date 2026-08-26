// Package theme defines the named color palettes the UI renders with.
package theme

import (
	"image/color"

	lipgloss "charm.land/lipgloss/v2"
)

var hex = lipgloss.Color

type Theme struct {
	Name string

	Background color.Color
	Surface    color.Color
	Text       color.Color
	Muted      color.Color
	Primary    color.Color
	Secondary  color.Color
	Accent     color.Color
	Success    color.Color
	Warning    color.Color
	Error      color.Color
	Border     color.Color
	Selection  color.Color
}

// Names lists the built-in themes in menu order.
func Names() []string {
	return []string{"tokyo-night", "dracula", "gruvbox", "ansi"}
}

// ByName returns the named theme, falling back to the default when unknown.
func ByName(name string) Theme {
	switch name {
	case "dracula":
		return Dracula()
	case "gruvbox":
		return Gruvbox()
	case "ansi":
		return ANSI()
	default:
		return TokyoNight()
	}
}

// TokyoNight is the default dark theme.
func TokyoNight() Theme {
	return Theme{
		Name:       "tokyo-night",
		Background: hex("#1a1b26"),
		Surface:    hex("#24283b"),
		Text:       hex("#c0caf5"),
		Muted:      hex("#565f89"),
		Primary:    hex("#7aa2f7"),
		Secondary:  hex("#7dcfff"),
		// warm orange for focus: lavender (#bb9af7) sat too close to the cool Primary/Secondary
		Accent:    hex("#ff9e64"),
		Success:   hex("#9ece6a"),
		Warning:   hex("#e0af68"),
		Error:     hex("#f7768e"),
		Border:    hex("#414868"),
		Selection: hex("#33467c"),
	}
}

func Dracula() Theme {
	return Theme{
		Name:       "dracula",
		Background: hex("#282a36"),
		Surface:    hex("#44475a"),
		Text:       hex("#f8f8f2"),
		Muted:      hex("#6272a4"),
		Primary:    hex("#bd93f9"),
		Secondary:  hex("#8be9fd"),
		Accent:     hex("#ff79c6"),
		Success:    hex("#50fa7b"),
		Warning:    hex("#f1fa8c"),
		Error:      hex("#ff5555"),
		Border:     hex("#44475a"),
		Selection:  hex("#44475a"),
	}
}

func Gruvbox() Theme {
	return Theme{
		Name:       "gruvbox",
		Background: hex("#282828"),
		Surface:    hex("#3c3836"),
		Text:       hex("#ebdbb2"),
		Muted:      hex("#928374"),
		Primary:    hex("#83a598"),
		Secondary:  hex("#d3869b"),
		Accent:     hex("#fabd2f"),
		Success:    hex("#b8bb26"),
		Warning:    hex("#fe8019"),
		Error:      hex("#fb4934"),
		Border:     hex("#504945"),
		Selection:  hex("#504945"),
	}
}

// ANSI maps every role to a base ANSI color (0-15) so the palette follows the terminal's own theme.
// Background and Text stay NoColor to keep the terminal's default surface and foreground.
func ANSI() Theme {
	return Theme{
		Name:       "ansi",
		Background: lipgloss.NoColor{},
		Surface:    lipgloss.ANSIColor(8),
		Text:       lipgloss.NoColor{},
		Muted:      lipgloss.ANSIColor(8),
		Primary:    lipgloss.ANSIColor(4),
		Secondary:  lipgloss.ANSIColor(6),
		// bright yellow (11) for focus: no true orange in the 16-colour palette
		Accent:    lipgloss.ANSIColor(11),
		Success:   lipgloss.ANSIColor(2),
		Warning:   lipgloss.ANSIColor(3),
		Error:     lipgloss.ANSIColor(1),
		Border:    lipgloss.ANSIColor(8),
		Selection: lipgloss.ANSIColor(8),
	}
}
