package agents

import (
	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

const (
	rvCopy = iota
	rvClose
	rvCount
)

const revealW = 60

// revealModal shows a freshly issued token's raw secret exactly once. Closing it discards it from the UI.
type revealModal struct {
	deps   deps.Deps
	issued domain.IssuedToken
	focus  int
	hover  int
	copied bool
}

func newRevealModal(d deps.Deps, issued domain.IssuedToken) revealModal {
	return revealModal{deps: d, issued: issued, focus: rvCopy, hover: -1}
}

func (r revealModal) Update(msg tea.Msg) (revealModal, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		switch msg.String() {
		case "c", "y":
			r.copied = true
			return r, tea.SetClipboard(r.issued.Secret)
		case "tab", "left", "right", "shift+tab":
			r.focus = (r.focus + 1) % rvCount
			return r, nil
		case "enter", "space":
			if r.focus == rvCopy {
				r.copied = true
				return r, tea.SetClipboard(r.issued.Secret)
			}
			return r, closeReveal
		case "esc":
			return r, closeReveal
		}
	case tea.MouseMotionMsg:
		r.hover = r.hitZone(msg)
		return r, nil
	case tea.MouseClickMsg:
		if msg.Button != tea.MouseLeft {
			return r, nil
		}
		switch r.hitZone(msg) {
		case rvCopy:
			r.copied = true
			return r, tea.SetClipboard(r.issued.Secret)
		case rvClose:
			return r, closeReveal
		}
	}
	return r, nil
}

func (r revealModal) hitZone(msg tea.MouseMsg) int {
	switch {
	case zone.Get("agents.reveal.copy").InBounds(msg):
		return rvCopy
	case zone.Get("agents.reveal.close").InBounds(msg):
		return rvClose
	}
	return -1
}

func (r revealModal) View() string {
	t := r.deps.Styles.Theme
	// wrap every row to content width so a long secret or URL can't push the modal past revealW and clip.
	wrap := func(s string) string { return lipgloss.NewStyle().Width(revealW - 4).Render(s) }
	warn := wrap(lipgloss.NewStyle().Foreground(t.Warning).Bold(true).
		Render("Copy this now — it is shown once and cannot be retrieved again."))

	secretBox := components.TitledBox("Token", lipgloss.NewStyle().Foreground(t.Text).Width(revealW-4).Render(r.issued.Secret), t.Accent)

	key := lipgloss.NewStyle().Foreground(t.Muted)
	val := lipgloss.NewStyle().Foreground(t.Text)
	mcp := lipgloss.JoinVertical(lipgloss.Left,
		lipgloss.NewStyle().Foreground(t.Primary).Bold(true).Render("MCP connection"),
		wrap(key.Render("Endpoint  ")+val.Render(r.deps.Server+"/mcp/v1")),
		wrap(key.Render("Protocol  ")+val.Render("STREAMABLE")),
		wrap(key.Render("Scope     ")+val.Render(scopeLong(r.issued.Token.Scope))),
		key.Render("Header"),
		wrap(val.Render("Authorization: Bearer "+r.issued.Secret)),
	)

	rows := []string{warn, "", secretBox, ""}
	if r.copied {
		rows = append(rows, lipgloss.NewStyle().Foreground(t.Success).Render("Copied to clipboard."), "")
	}
	rows = append(rows, mcp, "", r.buttons())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("Token issued", body, t.Primary)
}

func (r revealModal) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		r.button("Copy", "agents.reveal.copy", r.focus == rvCopy, r.hover == rvCopy),
		" ",
		r.button("Close", "agents.reveal.close", r.focus == rvClose, r.hover == rvClose),
	)
	return lipgloss.PlaceHorizontal(revealW, lipgloss.Right, group)
}

func (r revealModal) button(label, id string, focused, hovered bool) string {
	t := r.deps.Styles.Theme
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case focused:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case hovered:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(id, components.TitledBoxWeighted("", body, borderCol, focused))
}

func (r revealModal) HelpKeys() []key.Binding {
	return []key.Binding{
		key.NewBinding(key.WithKeys("c"), key.WithHelp("c", "copy")),
		key.NewBinding(key.WithKeys("enter", "esc"), key.WithHelp("enter/esc", "close")),
	}
}

func scopeLong(scope string) string {
	switch scope {
	case domain.ScopeReadWrite:
		return "READ_WRITE (read and modify issues)"
	case domain.ScopeReadOnly:
		return "READ_ONLY (read only)"
	}
	return scope
}

type revealClosedMsg struct{}

func closeReveal() tea.Msg { return revealClosedMsg{} }
