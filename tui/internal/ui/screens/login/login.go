// Package login is the login screen.
package login

import (
	"context"
	"errors"
	"image/color"
	"log/slog"
	"net/http"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/brand"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
)

const (
	focusIdentifier = iota
	focusPassword
	focusSubmit
	focusCount
)

const formWidth = 40

type Model struct {
	deps deps.Deps
	info domain.SystemInfo

	identifier textinput.Model
	password   textinput.Model
	focus      int
	hover      string // zone id under the cursor, for hover highlight

	idErr      string
	pwErr      string
	credErr    bool // `error` color for fields on a rejected login
	status     string
	submitting bool

	width  int
	height int
}

func New(d deps.Deps, info domain.SystemInfo) Model {
	id := textinput.New()
	id.Prompt = ""
	id.Placeholder = "username"
	if info.Setup.EmailRequired {
		id.Placeholder = "you@example.com"
	}
	id.SetWidth(formWidth - 4)

	pw := textinput.New()
	pw.Prompt = ""
	pw.Placeholder = "password"
	pw.EchoMode = textinput.EchoPassword
	pw.SetWidth(formWidth - 4)

	m := Model{deps: d, info: info, identifier: id, password: pw, focus: focusIdentifier}
	if !info.Setup.IsOIDC() {
		m.identifier.Focus()
	}
	return m
}

func (m Model) Init() tea.Cmd {
	if m.info.Setup.IsOIDC() {
		return nil
	}
	return textinput.Blink
}

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		return m, nil
	case tea.KeyPressMsg:
		return m.onKey(msg)
	case tea.MouseClickMsg:
		return m.onClick(msg)
	case tea.MouseMotionMsg:
		return m.onHover(msg)
	case loginFailedMsg:
		return m.onLoginFailed(msg)
	case loggedInMsg:
		return m, gotoHome(m.info, msg.welcome)
	}
	return m.updateInputs(msg)
}

func (m Model) onKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	m.hover = "" // the keyboard is now driving, so drop any stale hover highlight
	if m.info.Setup.IsOIDC() {
		if msg.String() == "enter" {
			return m, gotoOidc(m.info)
		}
		return m, nil
	}
	if m.submitting {
		return m, nil
	}

	switch msg.String() {
	case "tab", "down":
		return m.moveFocus(1)
	case "shift+tab", "up":
		return m.moveFocus(-1)
	case "enter":
		if m.focus == focusIdentifier {
			return m.moveFocus(1)
		}
		return m.submit()
	}
	return m.typeIntoFocused(msg)
}

func (m Model) moveFocus(delta int) (Model, tea.Cmd) {
	return m.focusOn((m.focus + delta + focusCount) % focusCount)
}

func (m Model) focusOn(target int) (Model, tea.Cmd) {
	m.focus = target
	m.identifier.Blur()
	m.password.Blur()

	var cmd tea.Cmd
	switch target {
	case focusIdentifier:
		cmd = m.identifier.Focus()
	case focusPassword:
		cmd = m.password.Focus()
	}
	return m, cmd
}

func (m Model) onClick(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	if m.info.Setup.IsOIDC() {
		if zone.Get("login.oidc").InBounds(msg) {
			return m, gotoOidc(m.info)
		}
		return m, nil
	}
	if m.submitting {
		return m, nil
	}
	switch {
	case zone.Get("login.identifier").InBounds(msg):
		return m.focusOn(focusIdentifier)
	case zone.Get("login.password").InBounds(msg):
		return m.focusOn(focusPassword)
	case zone.Get("login.submit").InBounds(msg):
		return m.submit()
	}
	return m, nil
}

func (m Model) onHover(msg tea.MouseMotionMsg) (Model, tea.Cmd) {
	m.hover = ""
	button := "login.submit"
	if m.info.Setup.IsOIDC() {
		button = "login.oidc"
	}
	if zone.Get(button).InBounds(msg) {
		m.hover = button
	}
	return m, nil
}

func (m Model) typeIntoFocused(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	m.credErr = false
	m.status = ""

	var cmd tea.Cmd
	switch m.focus {
	case focusIdentifier:
		m.idErr = ""
		m.identifier, cmd = m.identifier.Update(msg)
	case focusPassword:
		m.pwErr = ""
		m.password, cmd = m.password.Update(msg)
	}
	return m, cmd
}

func (m Model) updateInputs(msg tea.Msg) (Model, tea.Cmd) {
	var idCmd, pwCmd tea.Cmd
	m.identifier, idCmd = m.identifier.Update(msg)
	m.password, pwCmd = m.password.Update(msg)
	return m, tea.Batch(idCmd, pwCmd)
}

func (m Model) submit() (Model, tea.Cmd) {
	m.idErr, m.pwErr, m.credErr, m.status = "", "", false, ""

	identifier := strings.TrimSpace(m.identifier.Value())
	password := m.password.Value()

	valid := true
	if identifier == "" {
		m.idErr = "Required field"
		valid = false
	}
	if password == "" {
		m.pwErr = "Required field"
		valid = false
	}
	if !valid {
		return m, nil
	}

	m.submitting = true
	m.status = "Logging in..."
	return m, login(m.deps, identifier, password)
}

func (m Model) onLoginFailed(msg loginFailedMsg) (Model, tea.Cmd) {
	m.submitting = false
	m.status = ""
	slog.Warn("login failed", "err", msg.err)

	if errors.Is(msg.err, domain.ErrInvalidCredentials) {
		m.credErr = true
		m.pwErr = "Invalid credentials"
		return m, nil
	}

	var apiErr *domain.APIError
	switch {
	case errors.As(msg.err, &apiErr) && apiErr.Status == http.StatusTooManyRequests:
		m.status = "Too many login attempts. Please wait and try again."
	case errors.As(msg.err, &apiErr):
		m.status = "Server error. Please try again later."
	default:
		m.status = "Cannot reach the server. Check the URL and network."
	}
	return m, nil
}

func (m Model) View() string {
	s := m.deps.Styles
	form := m.localDialog()
	if m.info.Setup.IsOIDC() {
		form = m.oidcDialog()
	}
	body := lipgloss.JoinVertical(lipgloss.Center,
		brand.RenderVertical(s.Theme.Primary),
		"",
		s.Hint.Render("Server: "+m.deps.Server),
		"",
		form,
	)
	return m.place(body)
}

func (m Model) localDialog() string {
	identifierLabel := "Username"
	if m.info.Setup.EmailRequired {
		identifierLabel = "Email"
	}

	rows := []string{
		m.field(focusIdentifier, identifierLabel, m.identifier.View()),
		m.errorLine(m.idErr),
		m.field(focusPassword, "Password", m.password.View()),
		m.errorLine(m.pwErr),
		m.button(),
	}
	if m.status != "" {
		rows = append(rows, "", m.deps.Styles.Error.Render(m.status))
	}
	return lipgloss.JoinVertical(lipgloss.Left, rows...)
}

func (m Model) oidcDialog() string {
	s := m.deps.Styles
	col := s.Theme.Primary
	if m.hover == "login.oidc" {
		col = s.Theme.Secondary
	}
	action := zone.Mark("login.oidc", lipgloss.NewStyle().
		Foreground(col).
		Bold(true).
		Padding(0, 1).
		Border(lipgloss.NormalBorder(), false, false, true, false).
		BorderForeground(col).
		Render("Login with "+m.idpLabel()))
	rows := []string{
		action,
		"",
		s.Hint.Render("press enter to continue"),
	}
	return lipgloss.JoinVertical(lipgloss.Center, rows...)
}

// content is pre-sized to a fixed width so every field lines up.
func (m Model) field(which int, title, content string) string {
	body := lipgloss.NewStyle().Width(formWidth - 4).MaxWidth(formWidth - 4).MaxHeight(1).Render(content)
	box := components.TitledBoxWeighted(title, body, m.fieldBorderColor(which), m.focus == which)
	if id := fieldZoneID(which); id != "" {
		box = zone.Mark(id, box)
	}
	return box
}

func fieldZoneID(which int) string {
	switch which {
	case focusIdentifier:
		return "login.identifier"
	case focusPassword:
		return "login.password"
	}
	return ""
}

func (m Model) fieldBorderColor(which int) color.Color {
	t := m.deps.Styles.Theme
	switch {
	case m.hasFieldError(which):
		return t.Error
	case m.focus == which:
		return t.Accent
	default:
		return t.Border
	}
}

func (m Model) hasFieldError(which int) bool {
	return m.credErr ||
		(which == focusIdentifier && m.idErr != "") ||
		(which == focusPassword && m.pwErr != "")
}

func (m Model) button() string {
	label := "Login"
	if m.submitting {
		label = "Logging in..."
	}
	t := m.deps.Styles.Theme
	borderCol, textCol, bold := t.Border, t.Muted, false
	switch {
	case m.submitting:
	case m.focus == focusSubmit:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case m.hover == "login.submit":
		borderCol, textCol = t.Secondary, t.Secondary
	}
	body := lipgloss.NewStyle().Width(formWidth - 4).Align(lipgloss.Center).Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark("login.submit", components.TitledBoxWeighted("", body, borderCol, m.focus == focusSubmit))
}

// errorLine keeps a blank row so the form does not jump as messages appear and clear.
func (m Model) errorLine(msg string) string {
	if msg == "" {
		return " "
	}
	return m.deps.Styles.Error.Render(msg)
}

func (m Model) place(dialog string) string {
	if m.width == 0 {
		return dialog
	}
	return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center, dialog)
}

func (m Model) idpLabel() string {
	if m.info.Setup.OIDC != nil && m.info.Setup.OIDC.ProviderName != "" {
		return m.info.Setup.OIDC.ProviderName
	}
	return "SSO"
}

func (m Model) HelpKeys() []key.Binding {
	if m.info.Setup.IsOIDC() {
		return []key.Binding{
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "continue")),
		}
	}
	if m.submitting {
		return nil
	}
	return []key.Binding{
		key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "next field")),
		key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "submit")),
	}
}

type loginFailedMsg struct{ err error }

type loggedInMsg struct{ welcome string }

func login(d deps.Deps, identifier, password string) tea.Cmd {
	return func() tea.Msg {
		tokens, err := d.Public.Login(context.Background(), identifier, password)
		if err != nil {
			return loginFailedMsg{err: err}
		}
		if err := d.Store.Save(d.Server, tokens); err != nil {
			slog.Warn("save tokens", "err", err)
		}
		d.Transport.SetTokens(tokens)
		return loggedInMsg{welcome: identifier}
	}
}

func gotoHome(info domain.SystemInfo, welcome string) tea.Cmd {
	return func() tea.Msg { return nav.GoToHomeMsg{Info: info, Welcome: welcome} }
}

func gotoOidc(info domain.SystemInfo) tea.Cmd {
	return func() tea.Msg { return nav.GoToOidcDeviceMsg{Info: info} }
}
