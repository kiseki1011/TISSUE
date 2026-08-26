// Package connecting shows a spinner while it reaches the server.
package connecting

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"time"

	"charm.land/bubbles/v2/spinner"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/brand"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
)

const (
	maxAttempts = 5
	retryDelay  = time.Second
)

type Model struct {
	deps      deps.Deps
	spinner   spinner.Model
	status    string
	attempt   int
	restoring bool
	err       error
	width     int
	height    int
}

func New(d deps.Deps) Model {
	return Model{
		deps:    d,
		spinner: spinner.New(),
		status:  "Connecting to " + d.Server,
		attempt: 1,
	}
}

func (m Model) Init() tea.Cmd {
	return tea.Batch(m.spinner.Tick, tryConnect(m.deps, 1))
}

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		return m, nil
	case attemptResultMsg:
		return m.onAttempt(msg)
	case retryMsg:
		m.attempt = msg.attempt
		return m, tryConnect(m.deps, msg.attempt)
	case connectedMsg:
		return m.onConnected(msg)
	case restoredMsg:
		return m.onRestored(msg)
	case spinner.TickMsg:
		var cmd tea.Cmd
		m.spinner, cmd = m.spinner.Update(msg)
		return m, cmd
	}
	return m, nil
}

func (m Model) onAttempt(msg attemptResultMsg) (Model, tea.Cmd) {
	if msg.err == nil {
		return m, finishConnect(m.deps, msg.info)
	}
	if msg.attempt < maxAttempts {
		return m, retryAfter(msg.attempt + 1)
	}
	m.err = msg.err
	return m, nil
}

func (m Model) onConnected(msg connectedMsg) (Model, tea.Cmd) {
	if !msg.hasSession {
		return m, gotoLogin(msg.info)
	}
	m.restoring = true
	m.status = "Restoring session"
	return m, restore(m.deps, msg.info, msg.refresh)
}

func (m Model) onRestored(msg restoredMsg) (Model, tea.Cmd) {
	if msg.ok {
		return m, gotoHome(msg.info, "")
	}
	return m, gotoLogin(msg.info)
}

func (m Model) View() string {
	if m.width == 0 {
		return ""
	}
	logo := brand.RenderVertical(m.deps.Styles.Theme.Primary)

	var body string
	if m.err != nil {
		body = lipgloss.JoinVertical(lipgloss.Center,
			m.deps.Styles.Error.Render(connectErrorMessage(m.err)),
			m.deps.Styles.Muted.Render(m.deps.Server),
			"",
			m.deps.Styles.Hint.Render("press ctrl+c to quit"),
		)
	} else {
		lines := []string{m.spinner.View() + " " + m.status}
		if !m.restoring {
			lines = append(lines, m.deps.Styles.Muted.Render(fmt.Sprintf("(%d/%d)", m.attempt, maxAttempts)))
		}
		body = lipgloss.JoinVertical(lipgloss.Center, lines...)
	}

	content := lipgloss.JoinVertical(lipgloss.Center, logo, "", body)
	return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center, content)
}

func connectErrorMessage(err error) string {
	var apiErr *domain.APIError
	if !errors.As(err, &apiErr) {
		return "Cannot reach server. Check the URL and network."
	}
	switch {
	case apiErr.Status >= 500:
		return "Server returned an error. Try again later."
	case apiErr.Status == http.StatusOK || apiErr.Status == http.StatusNotFound:
		return "Not a Tissue server."
	default:
		return "Connection failed."
	}
}

type attemptResultMsg struct {
	attempt int
	info    domain.SystemInfo
	err     error
}

type retryMsg struct{ attempt int }

type connectedMsg struct {
	info       domain.SystemInfo
	refresh    string
	hasSession bool
}

type restoredMsg struct {
	ok   bool
	info domain.SystemInfo
}

func tryConnect(d deps.Deps, attempt int) tea.Cmd {
	return func() tea.Msg {
		info, err := d.Public.SystemInfo(context.Background())
		return attemptResultMsg{attempt: attempt, info: info, err: err}
	}
}

func retryAfter(next int) tea.Cmd {
	return tea.Tick(retryDelay, func(time.Time) tea.Msg {
		return retryMsg{attempt: next}
	})
}

func finishConnect(d deps.Deps, info domain.SystemInfo) tea.Cmd {
	return func() tea.Msg {
		if err := d.Config.SetServer(d.Server); err != nil {
			slog.Warn("save server url", "err", err)
		}
		tokens, ok, err := d.Store.Load(d.Server)
		if err != nil {
			slog.Warn("read stored tokens", "err", err)
			ok = false
		}
		return connectedMsg{info: info, refresh: tokens.Refresh, hasSession: ok}
	}
}

func restore(d deps.Deps, info domain.SystemInfo, refresh string) tea.Cmd {
	return func() tea.Msg {
		tokens, err := d.Public.Refresh(context.Background(), refresh)
		if err != nil {
			if clearErr := d.Store.Clear(d.Server); clearErr != nil {
				slog.Warn("clear dead tokens", "err", clearErr)
			}
			return restoredMsg{ok: false, info: info}
		}
		if err := d.Store.Save(d.Server, tokens); err != nil {
			slog.Warn("save restored tokens", "err", err)
		}
		d.Transport.SetTokens(tokens)
		return restoredMsg{ok: true, info: info}
	}
}

func gotoLogin(info domain.SystemInfo) tea.Cmd {
	return func() tea.Msg { return nav.GoToLoginMsg{Info: info} }
}

// gotoHome sets Restore so the shell deep-links back into the last-open project.
func gotoHome(info domain.SystemInfo, welcome string) tea.Cmd {
	return func() tea.Msg { return nav.GoToHomeMsg{Info: info, Welcome: welcome, Restore: true} }
}
