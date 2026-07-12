package theme

import lipgloss "charm.land/lipgloss/v2"

// Styles holds the finished styles the views render with.
// Views use these instead of touching Theme colors or calling lipgloss.NewStyle directly.
type Styles struct {
	Theme Theme

	Title lipgloss.Style
	Muted lipgloss.Style
	Error lipgloss.Style

	Dialog       lipgloss.Style
	Field        lipgloss.Style
	FieldFocused lipgloss.Style
	FieldError   lipgloss.Style
	Button       lipgloss.Style
	ButtonActive lipgloss.Style
	Hint         lipgloss.Style
}

// New builds the styles for a theme.
func New(t Theme) Styles {
	field := lipgloss.NewStyle().
		Border(lipgloss.RoundedBorder()).
		BorderForeground(t.Border).
		Padding(0, 1)

	button := lipgloss.NewStyle().
		Border(lipgloss.RoundedBorder()).
		BorderForeground(t.Border).
		Foreground(t.Muted).
		Padding(0, 2)

	return Styles{
		Theme:        t,
		Title:        lipgloss.NewStyle().Bold(true).Foreground(t.Primary),
		Muted:        lipgloss.NewStyle().Foreground(t.Muted),
		Error:        lipgloss.NewStyle().Foreground(t.Error),
		Dialog:       lipgloss.NewStyle().Border(lipgloss.RoundedBorder()).BorderForeground(t.Border).Padding(1, 3),
		Field:        field,
		FieldFocused: field.BorderForeground(t.Accent),
		FieldError:   field.BorderForeground(t.Error),
		Button:       button,
		ButtonActive: button.BorderForeground(t.Accent).Foreground(t.Accent).Bold(true),
		Hint:         lipgloss.NewStyle().Foreground(t.Muted).Italic(true),
	}
}
