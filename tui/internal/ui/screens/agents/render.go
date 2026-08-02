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
	zoneNewAgent = "agents.new"
	zoneNewToken = "agents.token.new"

	rowHeight = 2 // each agent/token list row spans two lines (name + sub)
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
	left := clamp(m.width*38/100, 30, 46)
	right := m.width - left - 1
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

	dash := lipgloss.JoinHorizontal(lipgloss.Top, m.agentsPane(), " ", m.detailPane())
	view := lipgloss.PlaceHorizontal(m.width, lipgloss.Center, dash)

	switch {
	case m.creating:
		return m.overlayModal(view, m.create.View())
	case m.issuing:
		return m.overlayModal(view, m.issue.View())
	case m.revealing:
		return m.overlayModal(view, m.reveal.View())
	case m.confirming:
		return m.overlayModal(view, m.confirm.View())
	}
	return view
}

// The list windows to the selected row so a long list never overflows the pane.
func (m Model) agentsPane() string {
	t := m.deps.Styles.Theme
	leftW, _ := m.panelWidths()
	innerW := leftW - 4
	bodyH := max(1, m.height-2)
	rows := []string{m.newAffordance(zoneNewAgent, "New agent", "n", innerW), ""}

	if len(m.agents) == 0 {
		rows = append(rows, m.deps.Styles.Muted.Render("No agents yet. Press "+accent(t, "n")+" to create one."))
	} else {
		visible := max(1, (bodyH-2)/rowHeight) // two-row rows below the two-row affordance header
		top := listTop(m.cursor, visible, len(m.agents))
		for j := top; j < len(m.agents) && j < top+visible; j++ {
			rows = append(rows, zone.Mark(agentRowZone(j), m.agentRow(m.agents[j], j, innerW)))
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

func (m Model) agentRow(a domain.Agent, i, w int) string {
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
	return lipgloss.JoinVertical(lipgloss.Left, head, sub)
}

func (m Model) detailPane() string {
	t := m.deps.Styles.Theme
	_, rightW := m.panelWidths()
	innerW := rightW - 4
	border := t.Primary
	if m.focus == paneTokens {
		border = t.Accent
	}

	bodyH := max(1, m.height-2)
	a, ok := m.selectedAgent()
	if !ok {
		body := lipgloss.NewStyle().Width(innerW).Height(bodyH).MaxHeight(bodyH).Render(
			m.deps.Styles.Muted.Render("Select an agent to manage its tokens."))
		return components.TitledRule("Detail", "", body, border)
	}

	prefix := lipgloss.JoinVertical(lipgloss.Left,
		m.agentSummary(a, innerW), m.rule(innerW), "",
		m.newAffordance(zoneNewToken, "New token", "a", innerW), "")
	tokAvail := bodyH - lipgloss.Height(prefix)
	rows := append([]string{prefix}, m.tokensBlock(innerW, tokAvail)...)
	body := lipgloss.NewStyle().Width(innerW).Height(bodyH).MaxHeight(bodyH).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return zone.Mark("agents.detail", components.TitledRule("Detail", "", body, border))
}

// The project-inheritance note exists because agents have no per-project scoping.
func (m Model) agentSummary(a domain.Agent, w int) string {
	t := m.deps.Styles.Theme
	key := lipgloss.NewStyle().Foreground(t.Muted)
	val := lipgloss.NewStyle().Foreground(t.Text)
	line := func(k, v string) string {
		return key.Render(fmt.Sprintf("%-10s", k)) + val.Render(fit(v, w-10))
	}
	name := lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render(fit(a.Name, w))
	rows := []string{
		name,
		line("Handle", "@"+a.Username),
		line("Model", orDash(a.DeclaredModel)),
		line("Created", fmtDate(a.CreatedAt)),
		line("Projects", "inherits yours"),
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

// Windowed to the selected row so a long list never overflows the pane. avail is the rows left for this block.
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
		rows = append(rows, zone.Mark(tokenRowZone(j), m.tokenRow(m.tokens[j], j, now, w)))
	}
	return rows
}

func (m Model) tokenRow(tok domain.Token, i int, now time.Time, w int) string {
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

func (m Model) newAffordance(zoneID, label, hotkey string, w int) string {
	t := m.deps.Styles.Theme
	text := lipgloss.NewStyle().Foreground(t.Primary).Render("+ "+label) +
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
