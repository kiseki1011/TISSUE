package agents

import (
	"image/color"
	"strconv"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

const (
	agentFieldW  = 38
	agentDescMax = 255 // mirrors the backend DESCRIPTION_MAX_LENGTH
)

const agentPickRows = 8

func agentTypeOptions() []widgets.PickerOption {
	opts := make([]widgets.PickerOption, len(domain.AgentTypes))
	for i, t := range domain.AgentTypes {
		opts[i] = widgets.PickerOption{Value: t, Label: titleCase(t)}
	}
	return opts
}

func agentModelOptions(models []domain.AiModel) []widgets.PickerOption {
	opts := make([]widgets.PickerOption, 0, len(models)+1)
	opts = append(opts, widgets.PickerOption{Value: "0", Label: "None"})
	for _, m := range models {
		opts = append(opts, widgets.PickerOption{
			Value: strconv.FormatInt(m.ID, 10),
			Label: m.Name,
			Lead:  components.ColorSwatch(m.Color),
		})
	}
	return opts
}

func modelName(models []domain.AiModel, id int64) string {
	if id == 0 {
		return "None"
	}
	for _, m := range models {
		if m.ID == id {
			return m.Name
		}
	}
	return "None"
}

// withModel keeps the agent's current model in the list, so a stale catalog cannot silently clear it.
func withModel(models []domain.AiModel, id int64, name, color string) []domain.AiModel {
	if id == 0 {
		return models
	}
	for _, m := range models {
		if m.ID == id {
			return models
		}
	}
	return append(append([]domain.AiModel{}, models...), domain.AiModel{ID: id, Name: name, Color: color})
}

func dropdownContent(t theme.Theme, value string, w int) string {
	hint := lipgloss.NewStyle().Foreground(t.Muted).Render("enter ▾")
	inner := max(1, w-lipgloss.Width(hint)-1)
	left := lipgloss.NewStyle().Foreground(t.Text).Render(fit(value, inner))
	gap := max(1, w-lipgloss.Width(left)-lipgloss.Width(hint))
	return left + strings.Repeat(" ", gap) + hint
}

func fieldBorderColor(t theme.Theme, focused, hovered, hasErr bool) color.Color {
	switch {
	case hasErr:
		return t.Error
	case focused:
		return t.Accent
	case hovered:
		return t.Secondary
	default:
		return t.Primary
	}
}

func formButton(t theme.Theme, label, zoneID string, focused, hovered bool) string {
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case focused:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case hovered:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(zoneID, components.TitledBoxWeighted("", body, borderCol, focused))
}

// titleCase renders an ALL-CAPS enum for display (DEVELOPMENT -> Development), keeping QA uppercase.
func titleCase(s string) string {
	if len(s) <= 2 {
		return s
	}
	lower := []rune(s)
	for i := 1; i < len(lower); i++ {
		if lower[i] >= 'A' && lower[i] <= 'Z' {
			lower[i] += 'a' - 'A'
		}
	}
	return string(lower)
}
