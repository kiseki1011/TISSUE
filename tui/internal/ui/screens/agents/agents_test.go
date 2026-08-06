package agents

import (
	"regexp"
	"strings"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

var csi = regexp.MustCompile("\\x1b\\[[0-9;]*[A-Za-z]")

func plain(s string) string { return csi.ReplaceAllString(zone.Scan(s), "") }

func testDeps() deps.Deps {
	return deps.Deps{
		Server: "https://tissue.example.com",
		Styles: theme.New(theme.TokyoNight()),
		Glyphs: glyph.New(glyph.Unicode),
	}
}

// twoAgents builds a loaded model with two agents (ids 1,2), sized and with the global zone manager
// reset so View/Scan work.
func twoAgents(t *testing.T) Model {
	t.Helper()
	zone.NewGlobal()
	m := New(testDeps())
	m, _ = m.Update(tea.WindowSizeMsg{Width: 100, Height: 24})
	m, _ = m.Update(AgentsLoadedMsg{Agents: []domain.Agent{
		{ID: 1, Name: "Build Bot", Username: "agent-a-build"},
		{ID: 2, Name: "Reviewer", Username: "agent-a-review"},
	}})
	return m
}

func keyStr(s string) tea.KeyPressMsg {
	if len(s) == 1 {
		return tea.KeyPressMsg{Code: rune(s[0]), Text: s}
	}
	return tea.KeyPressMsg{Text: s}
}

// Loading agents selects the first and asks to load its tokens.
func TestAgentsLoadedSelectsFirst(t *testing.T) {
	m := New(testDeps())
	m, cmd := m.Update(AgentsLoadedMsg{Agents: []domain.Agent{{ID: 1, Name: "A"}, {ID: 2, Name: "B"}}})
	if m.loading {
		t.Error("still loading after AgentsLoadedMsg")
	}
	if m.cursor != 0 {
		t.Errorf("cursor = %d, want 0", m.cursor)
	}
	if m.tokensAgent != 1 {
		t.Errorf("tokensAgent = %d, want 1 (first agent)", m.tokensAgent)
	}
	if cmd == nil {
		t.Error("expected a token-load command for the first agent")
	}
}

// A load error flips loadErr and the view says so.
func TestAgentsLoadError(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps())
	m, _ = m.Update(tea.WindowSizeMsg{Width: 100, Height: 24})
	m, _ = m.Update(AgentsLoadedMsg{Err: true})
	if !m.loadErr {
		t.Fatal("loadErr not set")
	}
	if !strings.Contains(plain(m.View()), "Failed to load agents") {
		t.Error("view does not surface the load error")
	}
}

// Moving the cursor to a different agent re-targets the token pane and requests that agent's tokens.
func TestMoveCursorLoadsNewAgentTokens(t *testing.T) {
	m := twoAgents(t)
	m, cmd := m.Update(keyStr("j")) // down to agent 2
	if m.cursor != 1 {
		t.Fatalf("cursor = %d, want 1", m.cursor)
	}
	if m.tokensAgent != 2 {
		t.Errorf("tokensAgent = %d, want 2", m.tokensAgent)
	}
	if cmd == nil {
		t.Error("expected a token-load command after selecting a new agent")
	}
}

// Opening a modal makes the screen capture input so the shell yields its tab keys.
func TestOpenCreateCapturesInput(t *testing.T) {
	m := twoAgents(t)
	if m.CapturingInput() {
		t.Fatal("capturing input before any modal is open")
	}
	m, _ = m.Update(keyStr("n"))
	if !m.creating {
		t.Fatal("pressing n did not open the create modal")
	}
	if !m.CapturingInput() {
		t.Error("CapturingInput should be true while the create modal is open")
	}
}

// The create-agent form rejects an empty name, rejects digits, and accepts a letters/spaces name.
func TestCreateAgentValidation(t *testing.T) {
	f := newCreateAgentForm(testDeps(), nil)
	if _, _ = f.submit(); f.nameErr == "" {
		// submit returns a copy; re-run capturing it
	}
	f2, _ := f.submit()
	if f2.nameErr == "" {
		t.Error("empty name should be rejected")
	}

	f.name.SetValue("Bot 3000") // contains digits
	f3, _ := f.submit()
	if !strings.Contains(f3.nameErr, "Letters and spaces") {
		t.Errorf("digit name error = %q, want letters/spaces message", f3.nameErr)
	}

	f.name.SetValue("Build Bot")
	f4, cmd := f.submit()
	if f4.nameErr != "" {
		t.Errorf("valid name rejected: %q", f4.nameErr)
	}
	if !f4.submitting || cmd == nil {
		t.Error("a valid submit should enter the submitting state and run a command")
	}
}

// A successful create closes the modal and reloads the list.
func TestAgentCreatedClosesModal(t *testing.T) {
	m := twoAgents(t)
	m, _ = m.Update(keyStr("n"))
	m, cmd := m.Update(agentCreatedMsg{agent: domain.Agent{ID: 9, Name: "New"}})
	if m.creating {
		t.Error("create modal still open after agentCreatedMsg")
	}
	if cmd == nil {
		t.Error("expected a reload+toast command after creating an agent")
	}
}

// The issue-token form defaults to READ_WRITE, toggles scope with ←/→, and validates ttl bounds.
func TestIssueTokenScopeAndTtl(t *testing.T) {
	f := newIssueTokenForm(testDeps(), domain.Agent{ID: 1, Name: "Bot"})
	if f.scope != domain.ScopeReadWrite {
		t.Errorf("default scope = %q, want READ_WRITE", f.scope)
	}
	f, _ = f.focusOn(itScope)
	f, _ = f.onKey(tea.KeyPressMsg{Code: tea.KeyLeft})
	if f.scope != domain.ScopeReadOnly {
		t.Errorf("after toggle scope = %q, want READ_ONLY", f.scope)
	}

	f.name.SetValue("ci")
	f.ttl.SetValue("400") // out of range
	f2, _ := f.submit()
	if f2.ttlErr == "" {
		t.Error("ttl 400 should be rejected (max 365)")
	}

	f.ttl.SetValue("30")
	f3, cmd := f.submit()
	if f3.ttlErr != "" || f3.nameErr != "" {
		t.Errorf("valid token rejected: name=%q ttl=%q", f3.nameErr, f3.ttlErr)
	}
	if !f3.submitting || cmd == nil {
		t.Error("valid submit should enter submitting and run a command")
	}
}

// Issuing a token opens the reveal modal carrying the raw secret exactly once.
func TestTokenIssuedOpensReveal(t *testing.T) {
	m := twoAgents(t)
	m, _ = m.Update(keyStr("a")) // open issue modal for agent 1
	if !m.issuing {
		t.Fatal("issue modal did not open")
	}
	m, _ = m.Update(tokenIssuedMsg{issued: domain.IssuedToken{Secret: "tissue_pat_secret", Token: domain.Token{Scope: domain.ScopeReadWrite}}})
	if m.issuing {
		t.Error("issue modal still open after tokenIssuedMsg")
	}
	if !m.revealing {
		t.Fatal("reveal modal did not open")
	}
	if !strings.Contains(plain(m.reveal.View()), "tissue_pat_secret") {
		t.Error("reveal modal does not show the raw secret")
	}
	if !strings.Contains(plain(m.reveal.View()), "/mcp/v1") {
		t.Error("reveal modal does not show the MCP endpoint")
	}
}

// Copying in the reveal modal emits a clipboard command and marks it copied.
func TestRevealCopy(t *testing.T) {
	r := newRevealModal(testDeps(), domain.IssuedToken{Secret: "s3cret"})
	r, cmd := r.Update(keyStr("c"))
	if !r.copied {
		t.Error("copied flag not set")
	}
	if cmd == nil {
		t.Error("expected a SetClipboard command")
	}
}

// 'd' on the agents pane arms a deactivate confirm; accepting it runs the deactivate command.
func TestDeactivateConfirmFlow(t *testing.T) {
	m := twoAgents(t)
	m, _ = m.Update(keyStr("d"))
	if !m.confirming || m.confirmKind != confirmDeactivate {
		t.Fatalf("deactivate confirm not armed: confirming=%v kind=%v", m.confirming, m.confirmKind)
	}
	if m.confirmAgentID != 1 {
		t.Errorf("confirmAgentID = %d, want 1", m.confirmAgentID)
	}
	_, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if cmd == nil {
		t.Error("accepting the confirm should run the deactivate command")
	}
}

// 'd' while the tokens pane is focused arms a revoke confirm targeting the selected token.
func TestRevokeConfirmFlow(t *testing.T) {
	m := twoAgents(t)
	m, _ = m.Update(TokensLoadedMsg{AgentID: 1, Tokens: []domain.Token{{ID: 50, Name: "ci", Scope: domain.ScopeReadWrite}}})
	m.focus = paneTokens
	m, _ = m.Update(keyStr("d"))
	if !m.confirming || m.confirmKind != confirmRevoke {
		t.Fatalf("revoke confirm not armed: confirming=%v kind=%v", m.confirming, m.confirmKind)
	}
	if m.confirmAgentID != 1 || m.confirmTokenID != 50 {
		t.Errorf("confirm targets agent=%d token=%d, want 1/50", m.confirmAgentID, m.confirmTokenID)
	}
}

// A confirm-driven action failure keeps the dialog open and shows the reason in place.
func TestActionFailureKeepsConfirmOpen(t *testing.T) {
	m := twoAgents(t)
	m, _ = m.Update(keyStr("d"))
	m, _ = m.Update(actionFailedMsg{message: "boom"})
	if !m.confirming {
		t.Error("confirm dialog closed on failure; it should stay open")
	}
	if m.confirm.Status != "boom" {
		t.Errorf("confirm status = %q, want the failure message", m.confirm.Status)
	}
}

// Token status labels reflect revoked / expired / active state.
func TestTokenStatusLabels(t *testing.T) {
	zone.NewGlobal()
	m := twoAgents(t)
	now := time.Now()
	m, _ = m.Update(TokensLoadedMsg{AgentID: 1, Tokens: []domain.Token{
		{ID: 1, Name: "live", Scope: domain.ScopeReadWrite},
		{ID: 2, Name: "gone", Scope: domain.ScopeReadOnly, Revoked: true},
		{ID: 3, Name: "old", Scope: domain.ScopeReadOnly, ExpiresAt: now.Add(-time.Hour)},
	}})
	view := plain(m.View())
	for _, want := range []string{"active", "revoked", "expired"} {
		if !strings.Contains(view, want) {
			t.Errorf("view missing token status %q", want)
		}
	}
}

// Name length is validated in characters, not bytes, so multi-byte scripts the regex allows (e.g.
// Hangul) are measured the same way the backend measures them.
func TestCreateAgentNameLengthIsRuneBased(t *testing.T) {
	f := newCreateAgentForm(testDeps(), nil)
	// 12 Hangul characters = 36 bytes but 12 runes — within the 35-char limit, must be accepted.
	f.name.SetValue("가나다라마바사아자차카타")
	f2, _ := f.submit()
	if f2.nameErr != "" {
		t.Errorf("a 12-character Hangul name was rejected: %q", f2.nameErr)
	}
	// a single Hangul character is 3 bytes but 1 rune — must be rejected as too short.
	f.name.SetValue("봇")
	f3, _ := f.submit()
	if f3.nameErr == "" {
		t.Error("a 1-character name should be rejected as too short")
	}
}

// A list longer than the pane windows to the selected row: the pane never exceeds the height budget,
// and the selected agent's row stays rendered (reachable), not scrolled off.
func TestAgentListWindowsToSelection(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps())
	const h = 16
	m, _ = m.Update(tea.WindowSizeMsg{Width: 100, Height: h})
	var many []domain.Agent
	for i := 0; i < 40; i++ {
		many = append(many, domain.Agent{ID: int64(i + 1), Name: "Agent " + string(rune('A'+i%26)), Username: "agent-x"})
	}
	m, _ = m.Update(AgentsLoadedMsg{Agents: many})
	m.cursor = 39 // last agent, far past the fold

	leftW, _ := m.panelWidths()
	pane := plain(m.agentsPane(leftW, m.height))
	if got := len(strings.Split(pane, "\n")); got > h {
		t.Errorf("agents pane = %d rows, want <= height %d (list overflowed)", got, h)
	}
	// the selected row carries its click zone this frame, proving it is within the window
	m.View()
	for i := 0; i < 1000; i++ {
		if _ = zone.Scan(m.agentsPane(leftW, m.height)); zone.Get(agentRowZone(39)) != nil {
			break
		}
	}
	if zone.Get(agentRowZone(39)) == nil {
		t.Error("the selected (last) agent row is not rendered/clickable — it scrolled out of the window")
	}
}

// The view renders without panicking across a range of sizes, including below the minimum.
func TestViewNoPanic(t *testing.T) {
	for _, sz := range [][2]int{{40, 8}, {60, 12}, {70, 40}, {100, 24}, {200, 50}} {
		zone.NewGlobal()
		m := New(testDeps())
		m, _ = m.Update(tea.WindowSizeMsg{Width: sz[0], Height: sz[1]})
		m, _ = m.Update(AgentsLoadedMsg{Agents: []domain.Agent{{ID: 1, Name: "A", Username: "agent-a"}}})
		if got := plain(m.View()); got == "" && sz[0] > 0 {
			t.Errorf("%dx%d: empty view", sz[0], sz[1])
		}
	}
}
