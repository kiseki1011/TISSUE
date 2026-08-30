package agents

import (
	"fmt"
	"strconv"
	"strings"
	"time"

	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const (
	zoneNewAgent  = "agents.new"
	zoneNewToken  = "agents.token.new"
	zoneAgentEdit = "agents.edit" // the "Edit" pen beside the selected agent's name in Details

	rowHeight = 2 // each agent/token list row spans two lines (name + sub)

	hInset = 2 // left/right padding, so the dashboard lines up with the other tabs
	colGap = 1 // the blank column between the two panes
)

func agentRowZone(i int) string { return "agents.row." + strconv.Itoa(i) }
func tokenRowZone(i int) string { return "agents.token.row." + strconv.Itoa(i) }

// listTop is the first visible index that keeps cursor within a window of visible rows.
func listTop(cursor, visible, n int) int {
	top := 0
	if cursor >= visible {
		top = cursor - visible + 1
	}
	if maxTop := n - visible; top > maxTop {
		top = maxTop
	}
	if top < 0 {
		top = 0
	}
	return top
}

func (m Model) panelWidths() (int, int) {
	usable := m.width - 2*hInset - colGap
	left := clamp(usable*38/100, 30, 46)
	right := usable - left
	if right < 20 {
		right = 20
	}
	return left, right
}

func (m Model) View() string {
	if m.width == 0 {
		return ""
	}
	s := m.deps.Styles
	if m.width < minWidth || m.height < minHeight {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Muted.Render("Terminal too small"))
	}
	if m.loading {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Muted.Render("Loading agents…"))
	}
	if m.loadErr {
		return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center,
			s.Error.Render("Failed to load agents."))
	}

	view := m.dashboard()

	switch {
	case m.creating:
		return m.overlayModal(view, m.create.View())
	case m.editing:
		return m.overlayModal(view, m.edit.View())
	case m.issuing:
		return m.overlayModal(view, m.issue.View())
	case m.revealing:
		return m.overlayModal(view, m.reveal.View())
	case m.confirming:
		return m.overlayModal(view, m.confirm.View())
	}
	return view
}

// dashboard lays list and detail side by side, or stacked (detail above) when narrow and tall.
func (m Model) dashboard() string {
	if m.layout() == layoutStacked {
		w, detailH := m.stackWidth(), m.stackDetailH()
		stack := lipgloss.JoinVertical(lipgloss.Left, m.detailPane(w, detailH), m.agentsPane(w, m.height-detailH))
		return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, stack)
	}
	leftW, rightW := m.panelWidths()
	dash := lipgloss.JoinHorizontal(lipgloss.Top, m.agentsPane(leftW, m.height), " ", m.detailPane(rightW, m.height))
	return lipgloss.PlaceHorizontal(m.width, lipgloss.Center, dash)
}

func (m Model) layout() layoutKind {
	if components.StackVertically(m.width, m.height, stackBelowW, stackMinH) {
		return layoutStacked
	}
	return layoutSide
}

// stackWidth is the single-column width, capped so a very wide terminal keeps a readable measure.
func (m Model) stackWidth() int { return clamp(m.width-2*hInset, 24, 84) }

// stackDetailH gives the detail the top slice when stacked, leaving the rest for the list below.
func (m Model) stackDetailH() int { return clamp(m.height*45/100, 10, m.height-6) }

// The list windows to the selected row so a long list never overflows the pane.
func (m Model) agentsPane(w, h int) string {
	t := m.deps.Styles.Theme
	innerW := w - 4
	bodyH := max(1, h-2)
	rows := []string{""}
	headerH := 1 // the leading blank row
	if m.deps.Mouse {
		// the "+ New agent" click affordance is hidden when the mouse is off (n does the same)
		rows = append(rows, m.newAffordance(zoneNewAgent, "New agent", "n", innerW, m.hover == zoneNewAgent), "")
		headerH = 3
	}

	if len(m.agents) == 0 {
		rows = append(rows, m.deps.Styles.Muted.Render("No agents yet. Press "+accent(t, "n")+" to create one."))
	} else {
		visible := max(1, (bodyH-headerH)/rowHeight) // rows below the header block
		top := listTop(m.cursor, visible, len(m.agents))
		for j := top; j < len(m.agents) && j < top+visible; j++ {
			rows = append(rows, zone.Mark(agentRowZone(j), m.agentRow(m.agents[j], j, innerW, m.hover == agentRowZone(j))))
		}
	}
	body := lipgloss.NewStyle().Width(innerW).Height(bodyH).MaxHeight(bodyH).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	border := t.Primary
	if m.focus == paneAgents {
		border = t.Accent
	}
	title := fmt.Sprintf("Agents (%d)", len(m.agents))
	return zone.Mark("agents.pane", components.TitledRule(title, "", body, border))
}

func (m Model) agentRow(a domain.Agent, i, w int, hovered bool) string {
	t := m.deps.Styles.Theme
	nameStyle := lipgloss.NewStyle().Foreground(t.Text)
	if i == m.cursor {
		if m.focus == paneAgents {
			nameStyle = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		} else {
			nameStyle = lipgloss.NewStyle().Foreground(t.Secondary).Bold(true)
		}
	}
	robot := lipgloss.NewStyle().Foreground(t.Muted).Render(m.deps.Glyphs.Or(m.deps.Glyphs.Robot, "*"))
	head := robot + " " + nameStyle.Render(fit(a.Name, w-2))
	sub := lipgloss.NewStyle().Foreground(t.Muted).Render(fit("  @"+a.Username, w))
	if hovered && i != m.cursor {
		band := m.hoverBand()
		head, sub = band.Width(w).Render(head), band.Width(w).Render(sub)
	}
	return lipgloss.JoinVertical(lipgloss.Left, head, sub)
}

// hoverBand dims a hovered row. The ANSI theme has no background to dim, so it tints the text.
func (m Model) hoverBand() lipgloss.Style {
	t := m.deps.Styles.Theme
	if _, noBg := t.Background.(lipgloss.NoColor); noBg {
		return lipgloss.NewStyle().Foreground(t.Secondary)
	}
	return lipgloss.NewStyle().Foreground(t.Text).Background(components.MixColors(t.Selection, t.Background, 0.5))
}

func (m Model) detailPane(w, h int) string {
	border := m.deps.Styles.Theme.Primary
	if m.focus == paneTokens {
		border = m.deps.Styles.Theme.Accent
	}
	return zone.Mark("agents.detail", components.TitledRule("Details", "", m.detailContent(w-4, max(1, h-2)), border))
}

// detailContent is the inner block shared by the side-by-side and the stacked pane, sized to innerW x bodyH.
func (m Model) detailContent(innerW, bodyH int) string {
	a, ok := m.selectedAgent()
	if !ok {
		return lipgloss.NewStyle().Width(innerW).Height(bodyH).MaxHeight(bodyH).Render(
			m.deps.Styles.Muted.Render("Select an agent to manage its tokens."))
	}
	parts := []string{m.agentSummary(a, innerW), ""}
	if m.deps.Mouse {
		// the "+ New token" click affordance is hidden when the mouse is off (a does the same)
		parts = append(parts, m.newAffordance(zoneNewToken, "New token", "a", innerW, m.hover == zoneNewToken), "")
	}
	prefix := lipgloss.JoinVertical(lipgloss.Left, parts...)
	tokAvail := bodyH - lipgloss.Height(prefix)
	rows := append([]string{prefix}, m.tokensBlock(innerW, tokAvail)...)
	return lipgloss.NewStyle().Width(innerW).Height(bodyH).MaxHeight(bodyH).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
}

func (m Model) agentSummary(a domain.Agent, w int) string {
	t := m.deps.Styles.Theme
	g := m.deps.Glyphs
	key := lipgloss.NewStyle().Foreground(t.Muted)
	val := lipgloss.NewStyle().Foreground(t.Text)
	const labelW = 11 // glyph + two spaces + fixed-width field name, matching other tabs' Details
	row := func(glyph, k, v string) string {
		if glyph != "" {
			k = glyph + "  " + k
		}
		return key.Render(fit(k, labelW)) + val.Render(fit(v, max(1, w-labelW)))
	}
	lines := []string{
		"",
		m.agentTitle(a, w),
		"",
		row(g.Or(g.At, ""), "Handle", "@"+a.Username),
		row(g.Or(g.Tag, ""), "Type", orDash(titleCase(a.AgentType))),
		row(g.Or(g.Flash, ""), "Model", orDash(a.ModelName)),
		row(g.Or(g.Calendar, ""), "Created", fmtDate(a.CreatedAt)),
		"",
	}
	// A long description reads as a wrapped block rather than a truncated fixed-width row.
	if a.Description != "" {
		lines = append(lines, val.Width(w).Render(a.Description), "")
	}
	lines = append(lines, m.rule(w))
	return lipgloss.JoinVertical(lipgloss.Left, lines...)
}

func (m Model) agentTitle(a domain.Agent, w int) string {
	t := m.deps.Styles.Theme
	nameStyle := lipgloss.NewStyle().Foreground(t.Primary).Bold(true)
	if !m.deps.Mouse {
		return nameStyle.Render(fit(a.Name, w)) // no Edit pen when the mouse is off (e does the same)
	}
	pen := m.deps.Glyphs.Or(m.deps.Glyphs.PenSquare, "Edit")
	penColor := t.Primary
	if m.hover == zoneAgentEdit {
		penColor = t.Secondary
	}
	penStr := zone.Mark(zoneAgentEdit, lipgloss.NewStyle().Foreground(penColor).Render(" "+pen+" "))
	nameW := max(1, w-lipgloss.Width(penStr))
	name := nameStyle.Render(fit(a.Name, nameW))
	gap := max(0, w-lipgloss.Width(name)-lipgloss.Width(penStr))
	return name + strings.Repeat(" ", gap) + penStr
}

// Windowed to the selected row. avail is the lines left for this block.
func (m Model) tokensBlock(w, avail int) []string {
	t := m.deps.Styles.Theme
	head := lipgloss.NewStyle().Foreground(t.Muted).Bold(true).Render("Tokens")
	rows := []string{head, ""}
	switch {
	case m.tokensLoading:
		return append(rows, m.deps.Styles.Muted.Render("Loading…"))
	case m.tokensErr:
		return append(rows, m.deps.Styles.Error.Render("Failed to load tokens."))
	case len(m.tokens) == 0:
		return append(rows, m.deps.Styles.Muted.Render("No tokens. Press "+accent(t, "a")+" to issue one."))
	}
	visible := max(1, (avail-2)/rowHeight) // two-row rows below the two-row "Tokens" header
	top := listTop(m.tokenCursor, visible, len(m.tokens))
	now := time.Now()
	for j := top; j < len(m.tokens) && j < top+visible; j++ {
		rows = append(rows, zone.Mark(tokenRowZone(j), m.tokenRow(m.tokens[j], j, now, w, m.hover == tokenRowZone(j))))
	}
	return rows
}

func (m Model) tokenRow(tok domain.Token, i int, now time.Time, w int, hovered bool) string {
	t := m.deps.Styles.Theme
	nameStyle := lipgloss.NewStyle().Foreground(t.Text)
	if i == m.tokenCursor && m.focus == paneTokens {
		nameStyle = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
	} else if i == m.tokenCursor {
		nameStyle = lipgloss.NewStyle().Foreground(t.Secondary).Bold(true)
	}
	keyGlyph := lipgloss.NewStyle().Foreground(t.Muted).Render(m.deps.Glyphs.Or(m.deps.Glyphs.Key, "-"))
	scope := scopeLabel(tok.Scope)
	status := m.tokenStatus(tok, now)
	head := keyGlyph + " " + nameStyle.Render(fit(tok.Name, w-16)) + "  " +
		lipgloss.NewStyle().Foreground(t.Muted).Render(scope) + "  " + status
	sub := lipgloss.NewStyle().Foreground(t.Muted).Render(fit(
		fmt.Sprintf("  used %s · expires %s", fmtRelative(tok.LastUsedAt, now), fmtExpiry(tok.ExpiresAt)), w))
	if hovered && (i != m.tokenCursor || m.focus != paneTokens) {
		band := m.hoverBand()
		head, sub = band.Width(w).Render(head), band.Width(w).Render(sub)
	}
	return lipgloss.JoinVertical(lipgloss.Left, head, sub)
}

func (m Model) tokenStatus(tok domain.Token, now time.Time) string {
	t := m.deps.Styles.Theme
	switch {
	case tok.Revoked:
		return lipgloss.NewStyle().Foreground(t.Error).Render("revoked")
	case !tok.ExpiresAt.IsZero() && !now.Before(tok.ExpiresAt):
		return lipgloss.NewStyle().Foreground(t.Warning).Render("expired")
	default:
		return lipgloss.NewStyle().Foreground(t.Success).Render("active")
	}
}

func (m Model) newAffordance(zoneID, label, hotkey string, w int, hovered bool) string {
	t := m.deps.Styles.Theme
	labelColor := t.Primary
	if hovered {
		labelColor = t.Secondary
	}
	text := lipgloss.NewStyle().Foreground(labelColor).Render("+ "+label) +
		lipgloss.NewStyle().Foreground(t.Muted).Render("  ("+hotkey+")")
	return zone.Mark(zoneID, lipgloss.NewStyle().Width(w).Render(text))
}

func (m Model) rule(w int) string {
	if w < 0 {
		w = 0
	}
	return lipgloss.NewStyle().Foreground(m.deps.Styles.Theme.Border).Render(strings.Repeat("─", w))
}

// accent highlights a hotkey inside a hint sentence.
func accent(t theme.Theme, s string) string {
	return lipgloss.NewStyle().Foreground(t.Accent).Bold(true).Render(s)
}

func scopeLabel(scope string) string {
	switch scope {
	case domain.ScopeReadWrite:
		return "RW"
	case domain.ScopeReadOnly:
		return "RO"
	}
	return "?"
}

func orDash(s string) string {
	if s == "" {
		return "—"
	}
	return s
}

func fmtDate(tm time.Time) string {
	if tm.IsZero() {
		return "—"
	}
	return tm.Local().Format("2006-01-02")
}

func fmtExpiry(tm time.Time) string {
	if tm.IsZero() {
		return "never"
	}
	return tm.Local().Format("2006-01-02")
}

func fmtRelative(tm, now time.Time) string {
	if tm.IsZero() {
		return "never"
	}
	d := now.Sub(tm)
	switch {
	case d < time.Minute:
		return "just now"
	case d < time.Hour:
		return fmt.Sprintf("%dm ago", int(d.Minutes()))
	case d < 24*time.Hour:
		return fmt.Sprintf("%dh ago", int(d.Hours()))
	default:
		return fmt.Sprintf("%dd ago", int(d.Hours()/24))
	}
}

// fit pads or truncates s to w so rows align in a fixed column.
func fit(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return lipgloss.NewStyle().Width(w).MaxWidth(w).Render(components.Flatten(s))
}
