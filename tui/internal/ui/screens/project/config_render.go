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

// configView is the Config tab: a single centered panel of the project's settings. An open edit form or
// the archive confirmation floats over a dimmed copy, mirroring issuesView.
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

// configWidth uses the full inner width so the settings panel fills the tab horizontally.
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

// configColumnGap separates the two settings columns; configMinColumnW is the width below which a column
// stops being readable and the two are stacked instead.
const (
	configColumnGap  = 4
	configMinColumnW = 44
)

// configBody lays the project's settings out in two columns: what the project *is* on the left, and how it
// connects to a repository on the right. The two are read for different reasons - the left when checking
// the project, the right when an integration misbehaves - so keeping them side by side means neither is
// buried under the other. A terminal too narrow to split stacks them instead.
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

// configGeneralRows is the left column: what the project is - its identity fields and description.
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

// configVcsRows is the right column: how the project connects to a repository, then what that repository
// has actually sent.
func (m Model) configVcsRows(w int) []string {
	rows := m.githubSection(w)
	if !m.githubLoaded || !m.github.Connected {
		return rows
	}
	return append(rows, m.deliveriesSection(w)...)
}

// deliveriesSection lists recent inbound webhooks. It exists to answer "the push happened, so why is
// nothing linked?", which is why a delivery that was deliberately ignored shows its reason as prominently
// as a successful one shows its result. Absent for a non-manager, who may not read the log.
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

// deliveryHeadline is a delivery's first line: when it arrived, its event type, and a coloured outcome.
func deliveryHeadline(s theme.Styles, d domain.WebhookDelivery, w int) string {
	t := s.Theme
	label, color := deliveryOutcome(t, d)
	when := lipgloss.NewStyle().Foreground(t.Muted).Render(formatRelative(d.ReceivedAt))
	event := lipgloss.NewStyle().Foreground(t.Text).Render(components.Trunc(d.EventType, max(1, w/3)))
	return when + " " + event + "  " + lipgloss.NewStyle().Foreground(color).Render(label)
}

// deliveryOutcome maps a delivery status to how it should read. A deliberate skip is deliberately not an
// error colour: ignoring an event Tissue does not act on is normal, and colouring it red would bury the
// failures that do need attention.
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

// copyHint is the line under a copyable value: the key that copies it, plus a confirmation once it has
// been copied. The key stays visible afterwards so the value can be copied again - a hint replaced by its
// own confirmation would take the affordance away with it.
func copyHint(s theme.Styles, key, label string, copied bool) string {
	hint := s.Muted.Render(key + ": " + label)
	if copied {
		hint += lipgloss.NewStyle().Foreground(s.Theme.Success).Render("  copied")
	}
	return hint
}

// githubSection renders the project's GitHub webhook integration: its connection and sync status, the
// webhook URL to register in GitHub, a just-generated secret (shown once), a copy key beside each, and
// the g/s/x action hints. When
// the status could not load it reads unavailable rather than hiding the section silently.
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
	// each copy hint sits under the value it copies rather than on the action line below: registering the
	// integration in GitHub means pasting both, and a hint beside its value says which key takes which
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
