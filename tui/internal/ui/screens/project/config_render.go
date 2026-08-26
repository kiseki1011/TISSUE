package project

import (
	"fmt"
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const zoneConfigDetail = "project.config.detail"

// configView is the Config tab: a centered settings panel, with any form floating over a dimmed copy.
func (m Model) configView() string {
	base := m.configTab()
	t := m.deps.Styles.Theme
	switch {
	case m.configEditing:
		form, _, _ := components.ScrollBox(m.configEditUI.View(), m.height, m.configEditScroll, t.Primary, t.Border)
		return m.floatOver(base, form)
	case m.configConfirming:
		return m.floatOver(base, m.configConfirmUI.View())
	}
	return base
}

func (m Model) configWidth() int { return m.innerWidth() }

func (m Model) configTab() string {
	panel := m.configDetailPanel(m.configWidth(), m.height)
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, panel)
}

func (m Model) configDetailPanel(w, h int) string {
	return m.windowedPanel("Settings", zoneConfigDetail, m.configBody, m.configDetailScroll, w, h, false)
}

func (m Model) configScrollMax() int {
	cw := m.sprintPanelContentW(m.configWidth())
	lines := lipgloss.Height(lipgloss.NewStyle().Width(cw).Render("\n" + m.configBody(cw)))
	return max(0, lines-max(1, m.height-2-detailPadBottom))
}

// configMinColumnW is the width below which a column stops being readable and the two stack.
const (
	configColumnGap  = 4
	configMinColumnW = 44
)

// configBody puts what the project is on the left and how it connects to a repository on the right:
// they are read for different reasons. A terminal too narrow to split stacks them.
func (m Model) configBody(w int) string {
	s := m.deps.Styles
	switch {
	case !m.configLoaded && !m.configErr:
		return s.Muted.Render("Loading settings…")
	case m.configErr:
		return s.Error.Render("Failed to load the project settings. Reopen the tab to retry.")
	}

	colW := (w - configColumnGap) / 2
	if colW < configMinColumnW {
		rows := m.configGeneralRows(w)
		rows = append(rows, "")
		rows = append(rows, m.configVcsRows(w)...)
		return lipgloss.JoinVertical(lipgloss.Left, rows...)
	}

	col := lipgloss.NewStyle().Width(colW)
	left := col.Render(lipgloss.JoinVertical(lipgloss.Left, m.configGeneralRows(colW)...))
	right := col.Render(lipgloss.JoinVertical(lipgloss.Left, m.configVcsRows(colW)...))

	return lipgloss.JoinHorizontal(lipgloss.Top, left, strings.Repeat(" ", configColumnGap), right)
}

// configGeneralRows is the left column: identity fields and description.
func (m Model) configGeneralRows(w int) []string {
	s := m.deps.Styles
	t := s.Theme
	p := m.project
	g := m.deps.Glyphs
	labelW := 14
	row := func(icon, label, value string) string {
		if icon != "" {
			label = icon + "  " + label
		}
		return lipgloss.NewStyle().Foreground(t.Muted).Width(labelW).Render(label) + components.Trunc(value, max(1, w-labelW))
	}

	visLabel := "Public"
	if p.Visibility == "PRIVATE" {
		visLabel = "Private"
	}
	statusLabel, statusColor := "Active", t.Success
	if p.Archived {
		statusLabel, statusColor = "Archived · read-only", t.Muted
	}

	title := lipgloss.NewStyle().Foreground(t.Text).Bold(true).Render(orDash(flattenLine(p.Title)))
	status := lipgloss.NewStyle().Foreground(statusColor).Render(statusLabel)

	rows := []string{
		sectionRule(s, "Project", w), "",
		title, "",
		row(g.Or(g.Key, ""), "Key", p.Key),
		row(g.Or(g.Eye, ""), "Visibility", visLabel),
		row(g.Or(g.ArchiveCheck, ""), "Status", status),
	}
	if !p.CreatedAt.IsZero() {
		rows = append(rows, row(g.Or(g.Calendar, ""), "Created", formatLocalDay(p.CreatedAt)))
	}
	if !p.UpdatedAt.IsZero() {
		rows = append(rows, row(g.Or(g.Calendar, ""), "Updated", formatLocalDay(p.UpdatedAt)))
	}
	rows = append(rows, "", sectionRule(s, "Description", w), "")
	if desc := strings.TrimSpace(p.Description); desc != "" {
		rows = append(rows, lipgloss.NewStyle().Foreground(t.Text).Width(w).Render(desc))
	} else {
		rows = append(rows, s.Muted.Render("None"))
	}
	return rows
}

// configVcsRows is the right column: the repository connection, then what it has sent.
func (m Model) configVcsRows(w int) []string {
	rows := m.githubSection(w)
	if !m.githubLoaded || !m.github.Connected {
		return rows
	}
	return append(rows, m.deliveriesSection(w)...)
}

// deliveriesSection answers "the push happened, so why is nothing linked?", which is why an ignored
// delivery shows its reason as prominently as a successful one shows its result.
func (m Model) deliveriesSection(w int) []string {
	s := m.deps.Styles
	t := s.Theme
	out := []string{"", sectionRule(s, "Recent deliveries", w), ""}
	if !m.deliveriesLoaded {
		return append(out, s.Muted.Render("Only a project manager can read the delivery log."))
	}
	if len(m.deliveries) == 0 {
		return append(out, s.Muted.Render("Nothing received yet."))
	}
	for i, d := range m.deliveries {
		if i > 0 {
			out = append(out, "")
		}
		out = append(out, deliveryHeadline(m.deps.Styles, d, w))
		if detail := flattenLine(d.Detail); detail != "" {
			out = append(out, lipgloss.NewStyle().Foreground(t.Muted).Width(w).Render("  "+detail))
		}
	}
	return out
}

func deliveryHeadline(s theme.Styles, d domain.WebhookDelivery, w int) string {
	t := s.Theme
	label, color := deliveryOutcome(t, d)
	when := lipgloss.NewStyle().Foreground(t.Muted).Render(formatRelative(d.ReceivedAt))
	event := lipgloss.NewStyle().Foreground(t.Text).Render(components.Trunc(d.EventType, max(1, w/3)))
	return when + " " + event + "  " + lipgloss.NewStyle().Foreground(color).Render(label)
}

// deliveryOutcome maps a delivery status to how it reads. A deliberate skip is not an error colour:
// ignoring an event Tissue does not act on is normal, and red would bury the real failures.
func deliveryOutcome(t theme.Theme, d domain.WebhookDelivery) (string, color.Color) {
	switch d.Status {
	case "PROCESSED":
		return "applied", t.Success
	case "IGNORED":
		return "skipped", t.Muted
	case "FAILED":
		return fmt.Sprintf("retrying (%d)", d.AttemptCount), t.Warning
	case "DEAD":
		return "gave up", t.Error
	default:
		return "pending", t.Secondary
	}
}

// copyHint keeps the copy key visible after copying, so the value can be copied again.
func copyHint(s theme.Styles, key, label string, copied bool) string {
	hint := s.Muted.Render(key + ": " + label)
	if copied {
		hint += lipgloss.NewStyle().Foreground(s.Theme.Success).Render("  copied")
	}
	return hint
}

// githubSection renders the project's GitHub webhook integration. A status that could not load reads
// as unavailable rather than hiding the section silently.
func (m Model) githubSection(w int) []string {
	s := m.deps.Styles
	t := s.Theme
	g := m.deps.Glyphs
	labelW := 14
	statusRow := func(value string) string {
		return lipgloss.NewStyle().Foreground(t.Muted).Width(labelW).Render(g.Or(g.Github, "")+"  Status") + value
	}
	out := []string{sectionRule(s, "GitHub", w), ""}
	if !m.githubLoaded {
		return append(out, s.Muted.Render("Integration status unavailable."))
	}
	if !m.github.Connected {
		return append(out,
			statusRow(lipgloss.NewStyle().Foreground(t.Muted).Render("Not connected")),
			"",
			s.Muted.Render("Press g to generate a webhook secret and connect a repository."))
	}
	syncLabel, syncColor := "syncing", t.Success
	if !m.github.SyncEnabled {
		syncLabel, syncColor = "paused", t.Warning
	}
	status := lipgloss.NewStyle().Foreground(t.Success).Render("Connected · ") +
		lipgloss.NewStyle().Foreground(syncColor).Render(syncLabel)
	out = append(out, statusRow(status), "",
		lipgloss.NewStyle().Foreground(t.Muted).Render("Webhook URL"),
		lipgloss.NewStyle().Foreground(t.Text).Width(w).Render(orDash(m.github.WebhookURL)))
	// each hint sits under the value it copies, so it is clear which key takes which
	if m.github.WebhookURL != "" {
		out = append(out, copyHint(s, "u", "copy URL", m.githubURLCopied))
	}
	if sec := m.githubSecret.Secret; sec != "" {
		out = append(out, "",
			lipgloss.NewStyle().Foreground(t.Warning).Bold(true).Render("Secret (shown once — copy it now):"),
			lipgloss.NewStyle().Foreground(t.Text).Width(w).Render(sec),
			copyHint(s, "y", "copy secret", m.githubSecretCopied))
	}
	syncAction := "s: pause sync"
	if !m.github.SyncEnabled {
		syncAction = "s: resume sync"
	}
	return append(out, "", s.Muted.Render("g: rotate secret · "+syncAction+" · x: disconnect"))
}
