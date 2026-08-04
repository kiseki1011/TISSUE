package agents

import (
	"image/color"

	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const (
	agentFieldW  = 38
	agentDescMax = 255 // mirrors the backend DESCRIPTION_MAX_LENGTH
)

// picker is a horizontal option cycler used for the type and model fields in the agent forms. The
// label is what the user sees; the form maps the index back to a concrete value.
type picker struct {
	labels []string
	idx    int
}

func (p *picker) cycle(delta int) {
	if n := len(p.labels); n > 0 {
		p.idx = (p.idx + delta%n + n) % n
	}
}

func (p picker) label() string {
	if p.idx < 0 || p.idx >= len(p.labels) {
		return ""
	}
	return p.labels[p.idx]
}

// newTypePicker builds a picker over the agent types, starting on current (GENERAL when empty).
func newTypePicker(current string) picker {
	if current == "" {
		current = "GENERAL"
	}
	p := picker{labels: make([]string, len(domain.AgentTypes))}
	for i, t := range domain.AgentTypes {
		p.labels[i] = titleCase(t)
		if t == current {
			p.idx = i
		}
	}
	return p
}

func typeValue(p picker) string {
	if p.idx < 0 || p.idx >= len(domain.AgentTypes) {
		return "GENERAL"
	}
	return domain.AgentTypes[p.idx]
}

// newModelPicker builds a picker over "None" followed by the catalog, starting on selectedID.
func newModelPicker(models []domain.AiModel, selectedID int64) picker {
	p := picker{labels: make([]string, 0, len(models)+1)}
	p.labels = append(p.labels, "None")
	for i, m := range models {
		p.labels = append(p.labels, m.Name)
		if m.ID == selectedID {
			p.idx = i + 1
		}
	}
	return p
}

// modelValue returns the selected catalog id (0 = None).
func modelValue(models []domain.AiModel, p picker) int64 {
	if p.idx <= 0 || p.idx-1 >= len(models) {
		return 0
	}
	return models[p.idx-1].ID
}

// withModel ensures the list contains the agent's current model, so editing preserves it even when
// the catalog is stale or failed to load (otherwise saving would silently clear the model).
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

// cyclerContent renders a picker's current value flanked by ‹ › arrows, padded to width w.
func cyclerContent(t theme.Theme, value string, w int) string {
	arrow := lipgloss.NewStyle().Foreground(t.Muted)
	inner := max(1, w-4)
	val := lipgloss.NewStyle().Foreground(t.Text).Render(fit(value, inner))
	return arrow.Render("‹") + " " + val + " " + arrow.Render("›")
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

// formButton renders one modal button (Create/Save/Cancel) as a clickable bordered box.
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

// titleCase renders an ALL-CAPS enum name for display (DEVELOPMENT -> Development), keeping short
// acronyms like QA uppercase.
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
