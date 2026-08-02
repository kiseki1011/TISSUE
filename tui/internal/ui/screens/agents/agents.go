// Package agents is the Agents tab: manage the caller's agents and their PATs for MCP.
package agents

import (
	"context"

	"charm.land/bubbles/v2/key"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

const (
	minWidth  = 60
	minHeight = 12
)

type pane int

const (
	paneAgents pane = iota
	paneTokens
)

// confirmKind records what an open confirm dialog will do when accepted, so one shared ConfirmForm
// serves both destructive actions.
type confirmKind int

const (
	confirmNone confirmKind = iota
	confirmDeactivate
	confirmRevoke
)

type Model struct {
	deps   deps.Deps
	width  int
	height int

	agents  []domain.Agent
	cursor  int
	loading bool
	loadErr bool

	tokens        []domain.Token
	tokensAgent   int64 // id of the agent whose tokens are loaded (0 = none)
	tokenCursor   int
	tokensLoading bool
	tokensErr     bool

	focus pane

	// modals (only one open at a time)
	creating       bool
	create         createAgentForm
	issuing        bool
	issue          issueTokenForm
	revealing      bool
	reveal         revealModal
	confirming     bool
	confirm        widgets.ConfirmForm
	confirmKind    confirmKind
	confirmAgentID int64
	confirmTokenID int64

	modalScroll int // wheel offset when the open modal is taller than the terminal
}

// New loads nothing until Init runs.
func New(d deps.Deps) Model {
	return Model{deps: d, loading: true, focus: paneAgents}
}

func (m Model) Init() tea.Cmd { return loadAgents(m.deps) }

func (m Model) Retheme(d deps.Deps) Model {
	m.deps = d
	return m
}

// CapturingInput reports that a modal owns the keyboard, so the app shell suppresses its global
// tab-switch keys while the user types.
func (m Model) CapturingInput() bool {
	return m.creating || m.issuing || m.revealing || m.confirming
}

func (m Model) selectedAgent() (domain.Agent, bool) {
	if m.cursor < 0 || m.cursor >= len(m.agents) {
		return domain.Agent{}, false
	}
	return m.agents[m.cursor], true
}

func (m Model) selectedToken() (domain.Token, bool) {
	if m.tokenCursor < 0 || m.tokenCursor >= len(m.tokens) {
		return domain.Token{}, false
	}
	return m.tokens[m.tokenCursor], true
}

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		m.modalScroll = m.clampModalScroll(m.modalScroll) // a resize may reveal or hide modal rows
		return m, nil

	case AgentsLoadedMsg:
		m.loading, m.loadErr = false, msg.Err
		m.agents = msg.Agents
		if m.cursor >= len(m.agents) {
			m.cursor = max(0, len(m.agents)-1)
		}
		return m, m.selectTokens()

	case TokensLoadedMsg:
		if msg.AgentID != m.selectedAgentID() {
			return m, nil // a stale load for an agent no longer selected
		}
		m.tokensLoading, m.tokensErr = false, msg.Err
		m.tokens = msg.Tokens
		if m.tokenCursor >= len(m.tokens) {
			m.tokenCursor = max(0, len(m.tokens)-1)
		}
		return m, nil

	case agentCreatedMsg:
		m.creating = false
		return m, tea.Batch(loadAgents(m.deps), toast.Show(toast.Success, "Agent \""+msg.agent.Name+"\" created."))
	case createCancelledMsg:
		m.creating = false
		return m, nil

	case agentDeactivatedMsg:
		m.confirming, m.confirmKind = false, confirmNone
		m.tokensAgent = 0 // force a token reload for whatever becomes selected
		return m, tea.Batch(loadAgents(m.deps), toast.Show(toast.Success, "Agent deactivated."))

	case tokenIssuedMsg:
		m.issuing = false
		m.revealing, m.modalScroll = true, 0
		m.reveal = newRevealModal(m.deps, msg.issued)
		return m, tea.Batch(m.reloadTokens(), toast.Show(toast.Success, "Token issued."))
	case issueCancelledMsg:
		m.issuing = false
		return m, nil
	case revealClosedMsg:
		m.revealing = false
		return m, nil

	case tokenRevokedMsg:
		m.confirming, m.confirmKind = false, confirmNone
		return m, tea.Batch(m.reloadTokens(), toast.Show(toast.Success, "Token revoked."))

	case actionFailedMsg:
		// a confirm-driven action failed: keep the dialog open and show the reason in place
		if m.confirming {
			m.confirm.Submitting = false
			m.confirm.Status = msg.message
		}
		return m, nil

	case widgets.ConfirmAcceptedMsg:
		return m.runConfirmedAction()
	case widgets.ConfirmCancelledMsg:
		m.confirming, m.confirmKind = false, confirmNone
		return m, nil
	}

	// a wheel over a too-tall modal scrolls its window rather than reaching the form
	if wheel, ok := msg.(tea.MouseWheelMsg); ok && m.anyModalOpen() {
		delta := 1
		if wheel.Button == tea.MouseWheelUp {
			delta = -1
		}
		m.modalScroll = m.clampModalScroll(m.modalScroll + delta)
		return m, nil
	}

	switch {
	case m.creating:
		var cmd tea.Cmd
		m.create, cmd = m.create.Update(msg)
		return m, cmd
	case m.issuing:
		var cmd tea.Cmd
		m.issue, cmd = m.issue.Update(msg)
		return m, cmd
	case m.revealing:
		var cmd tea.Cmd
		m.reveal, cmd = m.reveal.Update(msg)
		return m, cmd
	case m.confirming:
		var cmd tea.Cmd
		m.confirm, cmd = m.confirm.Update(msg)
		return m, cmd
	}

	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		return m.onKey(msg)
	case tea.MouseClickMsg:
		return m.onClick(msg)
	}
	return m, nil
}

func (m Model) onKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "tab":
		return m.togglePane(), nil
	case "left", "h":
		m.focus = paneAgents
		return m, nil
	case "right", "l":
		if m.tokensAgent != 0 {
			m.focus = paneTokens
		}
		return m, nil
	case "up", "k":
		return m.moveCursor(-1)
	case "down", "j":
		return m.moveCursor(1)
	case "n":
		return m.openCreate()
	case "a":
		return m.openIssueToken()
	case "d", "x":
		return m.openDelete()
	}
	return m, nil
}

func (m Model) togglePane() Model {
	if m.focus == paneAgents && m.tokensAgent != 0 {
		m.focus = paneTokens
	} else {
		m.focus = paneAgents
	}
	return m
}

func (m Model) moveCursor(delta int) (Model, tea.Cmd) {
	if m.focus == paneTokens {
		m.tokenCursor = clamp(m.tokenCursor+delta, 0, len(m.tokens)-1)
		return m, nil
	}
	before := m.cursor
	m.cursor = clamp(m.cursor+delta, 0, len(m.agents)-1)
	if m.cursor != before {
		return m, m.selectTokens()
	}
	return m, nil
}

func (m Model) openCreate() (Model, tea.Cmd) {
	m.creating, m.modalScroll = true, 0
	m.create = newCreateAgentForm(m.deps)
	return m, m.create.Init()
}

func (m Model) openIssueToken() (Model, tea.Cmd) {
	a, ok := m.selectedAgent()
	if !ok {
		return m, nil
	}
	m.issuing, m.modalScroll = true, 0
	m.issue = newIssueTokenForm(m.deps, a)
	return m, m.issue.Init()
}

// openDelete opens a confirm dialog for the focused pane's destructive action: deactivate the
// selected agent, or revoke the selected token.
func (m Model) openDelete() (Model, tea.Cmd) {
	s := m.deps.Styles
	m.modalScroll = 0
	if m.focus == paneTokens {
		tok, ok := m.selectedToken()
		a, aok := m.selectedAgent()
		if !ok || !aok {
			return m, nil
		}
		m.confirming, m.confirmKind = true, confirmRevoke
		m.confirmAgentID, m.confirmTokenID = a.ID, tok.ID
		m.confirm = widgets.NewConfirmForm(s, "Revoke token",
			"Revoke token \""+tok.Name+"\"? Any client using it stops working immediately.", "Revoke")
		return m, nil
	}
	a, ok := m.selectedAgent()
	if !ok {
		return m, nil
	}
	m.confirming, m.confirmKind = true, confirmDeactivate
	m.confirmAgentID = a.ID
	m.confirm = widgets.NewConfirmForm(s, "Deactivate agent",
		"Deactivate \""+a.Name+"\"? Its tokens stop working immediately and it cannot be restored.", "Deactivate")
	return m, nil
}

func (m Model) runConfirmedAction() (Model, tea.Cmd) {
	switch m.confirmKind {
	case confirmDeactivate:
		return m, deactivateAgent(m.deps, m.confirmAgentID)
	case confirmRevoke:
		return m, revokeToken(m.deps, m.confirmAgentID, m.confirmTokenID)
	}
	return m, nil
}

func (m Model) onClick(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	if zone.Get(zoneNewAgent).InBounds(msg) {
		return m.openCreate()
	}
	if zone.Get(zoneNewToken).InBounds(msg) {
		return m.openIssueToken()
	}
	for i := range m.agents {
		if zone.Get(agentRowZone(i)).InBounds(msg) {
			before := m.cursor
			m.cursor, m.focus = i, paneAgents
			if m.cursor != before {
				return m, m.selectTokens()
			}
			return m, nil
		}
	}
	for i := range m.tokens {
		if zone.Get(tokenRowZone(i)).InBounds(msg) {
			m.tokenCursor, m.focus = i, paneTokens
			return m, nil
		}
	}
	return m, nil
}

func (m Model) selectedAgentID() int64 {
	if a, ok := m.selectedAgent(); ok {
		return a.ID
	}
	return 0
}

// selectTokens loads the selected agent's tokens if they are not already loaded.
func (m *Model) selectTokens() tea.Cmd {
	id := m.selectedAgentID()
	if id == 0 {
		m.tokens, m.tokensAgent, m.tokenCursor = nil, 0, 0
		return nil
	}
	if id == m.tokensAgent {
		return nil
	}
	m.tokens, m.tokensAgent, m.tokenCursor = nil, id, 0
	m.tokensLoading, m.tokensErr = true, false
	return loadTokens(m.deps, id)
}

// reloadTokens forces a reload of the currently selected agent's tokens (after issue/revoke).
func (m *Model) reloadTokens() tea.Cmd {
	id := m.selectedAgentID()
	if id == 0 {
		return nil
	}
	m.tokensAgent = id
	m.tokensLoading, m.tokensErr = true, false
	return loadTokens(m.deps, id)
}

func (m Model) HelpKeys() []key.Binding {
	if m.creating {
		return m.create.HelpKeys()
	}
	if m.issuing {
		return m.issue.HelpKeys()
	}
	if m.revealing {
		return m.reveal.HelpKeys()
	}
	if m.confirming {
		return m.confirm.HelpKeys()
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "pane")),
		key.NewBinding(key.WithKeys("n"), key.WithHelp("n", "new agent")),
		key.NewBinding(key.WithKeys("a"), key.WithHelp("a", "add token")),
		key.NewBinding(key.WithKeys("d"), key.WithHelp("d", "deactivate/revoke")),
	}
}

func (m Model) anyModalOpen() bool {
	return m.creating || m.issuing || m.revealing || m.confirming
}

// activeModalView renders the open modal, mirroring View's dispatch, so the host can measure and
// window it.
func (m Model) activeModalView() string {
	switch {
	case m.creating:
		return m.create.View()
	case m.issuing:
		return m.issue.View()
	case m.revealing:
		return m.reveal.View()
	case m.confirming:
		return m.confirm.View()
	}
	return ""
}

func (m Model) clampModalScroll(off int) int {
	if !m.anyModalOpen() {
		return 0
	}
	maxOff := lipgloss.Height(m.activeModalView()) - m.height
	return clamp(off, 0, max(0, maxOff))
}

// overlayModal mirrors the schema tab's modal overlay: dimmed backdrop, centered, scrollbar-windowed when too tall.
func (m Model) overlayModal(backdrop, modal string) string {
	t := m.deps.Styles.Theme
	bd := components.StripANSI(lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Top, backdrop))
	modal, _, _ = components.ScrollBox(modal, m.height, m.modalScroll, t.Primary, t.Border)
	mx := max(0, (m.width-lipgloss.Width(modal))/2)
	my := max(0, (m.height-lipgloss.Height(modal))/2)
	return components.OverlayDim(bd, modal, mx, my, t.Muted)
}

func clamp(v, lo, hi int) int {
	if hi < lo {
		return lo
	}
	if v < lo {
		return lo
	}
	if v > hi {
		return hi
	}
	return v
}

// AgentsLoadedMsg carries the caller's agents (or an error flag). Exported so the app shell can route
// a background prefetch to this screen while another tab is active.
type AgentsLoadedMsg struct {
	Agents []domain.Agent
	Err    bool
}

// TokensLoadedMsg carries one agent's tokens. Exported for the same prefetch-routing reason.
type TokensLoadedMsg struct {
	AgentID int64
	Tokens  []domain.Token
	Err     bool
}

type agentCreatedMsg struct{ agent domain.Agent }
type agentDeactivatedMsg struct{}
type tokenIssuedMsg struct{ issued domain.IssuedToken }
type tokenRevokedMsg struct{}
type actionFailedMsg struct{ message string }

func loadAgents(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		agents, err := d.Agents.ListAgents(context.Background())
		return AgentsLoadedMsg{Agents: agents, Err: err != nil}
	}
}

func loadTokens(d deps.Deps, agentID int64) tea.Cmd {
	return func() tea.Msg {
		tokens, err := d.Agents.ListTokens(context.Background(), agentID)
		return TokensLoadedMsg{AgentID: agentID, Tokens: tokens, Err: err != nil}
	}
}

func deactivateAgent(d deps.Deps, agentID int64) tea.Cmd {
	return func() tea.Msg {
		if err := d.Agents.DeactivateAgent(context.Background(), agentID); err != nil {
			return actionFailedMsg{message: "Could not deactivate the agent. Try again."}
		}
		return agentDeactivatedMsg{}
	}
}

func revokeToken(d deps.Deps, agentID, tokenID int64) tea.Cmd {
	return func() tea.Msg {
		if err := d.Agents.RevokeToken(context.Background(), agentID, tokenID); err != nil {
			return actionFailedMsg{message: "Could not revoke the token. Try again."}
		}
		return tokenRevokedMsg{}
	}
}
