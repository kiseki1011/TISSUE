package agents

import (
	"context"
	"errors"
	"image/color"
	"net/http"
	"strconv"
	"strings"
	"unicode/utf8"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
)

// issue-token focus stops.
const (
	itName = iota
	itScope
	itTTL
	itSubmit
	itCancel
	itCount
)

const itFieldW = 38

type issueTokenForm struct {
	deps    deps.Deps
	agent   domain.Agent
	name    textinput.Model
	ttl     textinput.Model
	scope   string // domain.ScopeReadOnly | domain.ScopeReadWrite
	spinner spinner.Model
	focus   int
	hover   int

	nameErr    string
	ttlErr     string
	status     string
	submitting bool
}

func newIssueTokenForm(d deps.Deps, agent domain.Agent) issueTokenForm {
	line := func(placeholder string) textinput.Model {
		in := textinput.New()
		in.Prompt = ""
		in.Placeholder = placeholder
		in.SetWidth(itFieldW)
		return in
	}
	f := issueTokenForm{
		deps:    d,
		agent:   agent,
		name:    line("ci-runner"),
		ttl:     line("blank = never expires"),
		scope:   domain.ScopeReadWrite,
		spinner: spinner.New(),
		focus:   itName,
		hover:   -1,
	}
	f.name.Focus()
	return f
}

func (f issueTokenForm) Init() tea.Cmd { return textinput.Blink }

func (f issueTokenForm) Update(msg tea.Msg) (issueTokenForm, tea.Cmd) {
	switch msg := msg.(type) {
	case issueFailedMsg:
		f.submitting = false
		f.status = msg.message
		return f, nil
	case spinner.TickMsg:
		var cmd tea.Cmd
		f.spinner, cmd = f.spinner.Update(msg)
		if !f.submitting {
			cmd = nil
		}
		return f, cmd
	case tea.MouseClickMsg:
		return f.onClick(msg)
	case tea.MouseMotionMsg:
		f.hover = f.hitZone(msg)
		return f, nil
	case tea.KeyPressMsg:
		return f.onKey(msg)
	}
	return f.updateInputs(msg)
}

func (f issueTokenForm) hitZone(msg tea.MouseMsg) int {
	switch {
	case zone.Get("agents.token.name").InBounds(msg):
		return itName
	case zone.Get("agents.token.scope").InBounds(msg):
		return itScope
	case zone.Get("agents.token.ttl").InBounds(msg):
		return itTTL
	case zone.Get("agents.token.submit").InBounds(msg):
		return itSubmit
	case zone.Get("agents.token.cancel").InBounds(msg):
		return itCancel
	}
	return -1
}

func (f issueTokenForm) onClick(msg tea.MouseClickMsg) (issueTokenForm, tea.Cmd) {
	if msg.Button != tea.MouseLeft || f.submitting {
		return f, nil
	}
	switch f.hitZone(msg) {
	case itName, itTTL:
		return f.focusOn(f.hitZone(msg))
	case itScope:
		f, _ = f.focusOn(itScope)
		f.scope = toggleScope(f.scope)
		return f, nil
	case itSubmit:
		return f.submit()
	case itCancel:
		return f, cancelIssue
	}
	return f, nil
}

func (f issueTokenForm) onKey(msg tea.KeyPressMsg) (issueTokenForm, tea.Cmd) {
	if f.submitting {
		return f, nil
	}
	switch msg.String() {
	case "tab", "down":
		return f.moveFocus(1)
	case "shift+tab", "up":
		return f.moveFocus(-1)
	case "esc":
		return f, cancelIssue
	case "left", "right", "space":
		if f.focus == itScope {
			f.scope = toggleScope(f.scope)
			return f, nil
		}
	case "enter":
		switch f.focus {
		case itSubmit:
			return f.submit()
		case itCancel:
			return f, cancelIssue
		default:
			return f.moveFocus(1)
		}
	}
	return f.typeIntoFocused(msg)
}

func (f issueTokenForm) moveFocus(delta int) (issueTokenForm, tea.Cmd) {
	return f.focusOn((f.focus + delta + itCount) % itCount)
}

func (f issueTokenForm) focusOn(target int) (issueTokenForm, tea.Cmd) {
	f.focus = target
	f.name.Blur()
	f.ttl.Blur()
	var cmd tea.Cmd
	switch target {
	case itName:
		cmd = f.name.Focus()
	case itTTL:
		cmd = f.ttl.Focus()
	}
	return f, cmd
}

func (f issueTokenForm) typeIntoFocused(msg tea.KeyPressMsg) (issueTokenForm, tea.Cmd) {
	f.status = ""
	var cmd tea.Cmd
	switch f.focus {
	case itName:
		f.nameErr = ""
		f.name, cmd = f.name.Update(msg)
	case itTTL:
		f.ttlErr = ""
		f.ttl, cmd = f.ttl.Update(msg)
	}
	return f, cmd
}

func (f issueTokenForm) updateInputs(msg tea.Msg) (issueTokenForm, tea.Cmd) {
	var nc, tc tea.Cmd
	f.name, nc = f.name.Update(msg)
	f.ttl, tc = f.ttl.Update(msg)
	return f, tea.Batch(nc, tc)
}

func (f issueTokenForm) submit() (issueTokenForm, tea.Cmd) {
	f.nameErr, f.ttlErr, f.status = "", "", ""
	name := strings.TrimSpace(f.name.Value())
	ttlStr := strings.TrimSpace(f.ttl.Value())

	ttl := 0
	switch {
	case name == "":
		f.nameErr = "Required field"
	case utf8.RuneCountInString(name) > 50:
		f.nameErr = "50 characters max"
	}
	if ttlStr != "" {
		n, err := strconv.Atoi(ttlStr)
		if err != nil || n < 1 || n > 365 {
			f.ttlErr = "1 to 365, or blank"
		} else {
			ttl = n
		}
	}
	switch {
	case f.nameErr != "":
		return f.focusOn(itName)
	case f.ttlErr != "":
		return f.focusOn(itTTL)
	}
	f.submitting = true
	return f, tea.Batch(issueTokenCmd(f.deps, f.agent.ID, name, f.scope, ttl), f.spinner.Tick)
}

func (f issueTokenForm) View() string {
	t := f.deps.Styles.Theme
	rows := []string{
		f.field(itName, "Name", f.name.View(), f.nameErr),
		f.scopeField(),
		f.field(itTTL, "Expires in days", f.ttl.View(), f.ttlErr),
	}
	switch {
	case f.submitting:
		rows = append(rows, lipgloss.NewStyle().Foreground(t.Warning).Padding(0, 1).Render(f.spinner.View()+" Issuing…"))
	case f.status != "":
		rows = append(rows, f.deps.Styles.Error.Width(itFieldW).Padding(0, 1).Render(f.status))
	}
	rows = append(rows, "", f.buttons())
	body := lipgloss.NewStyle().Padding(1, 2).Render(lipgloss.JoinVertical(lipgloss.Left, rows...))
	return components.TitledBoxCentered("Issue token · "+f.agent.Name, body, t.Primary)
}

func (f issueTokenForm) field(which int, label, content, errMsg string) string {
	fixed := lipgloss.NewStyle().Width(itFieldW).MaxWidth(itFieldW).Height(1).MaxHeight(1).Render(content)
	box := zone.Mark(f.fieldZone(which), components.TitledBoxWeighted(label, fixed, f.fieldBorderColor(which, errMsg), f.focus == which))
	err := " "
	if errMsg != "" {
		err = f.deps.Styles.Error.Render(errMsg)
	}
	return lipgloss.JoinVertical(lipgloss.Left, box, lipgloss.NewStyle().Padding(0, 1).Render(err))
}

func (f issueTokenForm) scopeField() string {
	t := f.deps.Styles.Theme
	opt := func(label, val string) string {
		st := lipgloss.NewStyle().Foreground(t.Muted)
		if f.scope == val {
			st = lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		}
		return st.Render(label)
	}
	content := opt("Read only", domain.ScopeReadOnly) + lipgloss.NewStyle().Foreground(t.Border).Render("   /   ") + opt("Read + write", domain.ScopeReadWrite)
	hint := lipgloss.NewStyle().Foreground(t.Muted).Render("  ←/→")
	fixed := lipgloss.NewStyle().Width(itFieldW).MaxWidth(itFieldW).Height(1).MaxHeight(1).Render(content + hint)
	box := zone.Mark("agents.token.scope", components.TitledBoxWeighted("Scope", fixed, f.fieldBorderColor(itScope, ""), f.focus == itScope))
	return lipgloss.JoinVertical(lipgloss.Left, box, lipgloss.NewStyle().Padding(0, 1).Render(" "))
}

func (f issueTokenForm) fieldZone(which int) string {
	switch which {
	case itName:
		return "agents.token.name"
	case itTTL:
		return "agents.token.ttl"
	}
	return ""
}

func (f issueTokenForm) fieldBorderColor(which int, errMsg string) color.Color {
	t := f.deps.Styles.Theme
	switch {
	case errMsg != "":
		return t.Error
	case f.focus == which:
		return t.Accent
	case f.hover == which:
		return t.Secondary
	default:
		return t.Primary
	}
}

func (f issueTokenForm) buttons() string {
	group := lipgloss.JoinHorizontal(lipgloss.Top,
		f.button("Issue", "agents.token.submit", f.focus == itSubmit, f.hover == itSubmit),
		" ",
		f.button("Cancel", "agents.token.cancel", f.focus == itCancel, f.hover == itCancel),
	)
	return lipgloss.PlaceHorizontal(itFieldW+4, lipgloss.Right, group)
}

func (f issueTokenForm) button(label, id string, focused, hovered bool) string {
	t := f.deps.Styles.Theme
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

func (f issueTokenForm) HelpKeys() []key.Binding {
	if f.submitting {
		return nil
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next")),
		key.NewBinding(key.WithKeys("left", "right"), key.WithHelp("←/→", "scope")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "confirm")),
		key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
	}
}

func toggleScope(s string) string {
	if s == domain.ScopeReadWrite {
		return domain.ScopeReadOnly
	}
	return domain.ScopeReadWrite
}

type (
	issueFailedMsg    struct{ message string }
	issueCancelledMsg struct{}
)

func cancelIssue() tea.Msg { return issueCancelledMsg{} }

func issueTokenCmd(d deps.Deps, agentID int64, name, scope string, ttlDays int) tea.Cmd {
	return func() tea.Msg {
		issued, err := d.Agents.IssueToken(context.Background(), agentID, name, scope, ttlDays)
		if err != nil {
			return issueFailedMsg{message: issueTokenError(err)}
		}
		return tokenIssuedMsg{issued: issued}
	}
}

func issueTokenError(err error) string {
	if m, ok := errmsg.Override(err); ok {
		return m // connectivity, or a leaky code mapped to friendlier copy
	}
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusConflict:
			return "This agent already has a token with that name."
		case http.StatusNotFound:
			return "That agent no longer exists."
		case http.StatusBadRequest:
			return "Invalid token details."
		}
	}
	if r := domain.ErrorReason(err); r != "" {
		return r // prefer the server's explanation over the generic line
	}
	return "Could not issue the token. Try again."
}
