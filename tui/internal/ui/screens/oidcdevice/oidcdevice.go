// Package oidcdevice runs the OIDC device authorization flow.
package oidcdevice

import (
	"context"
	"log/slog"
	"os/exec"
	"runtime"
	"strings"
	"time"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/spinner"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
)

type phase int

const (
	phaseStarting phase = iota
	phaseWaiting
	phaseDone
	phaseFailed
)

type Model struct {
	deps    deps.Deps
	info    domain.SystemInfo
	spinner spinner.Model

	device   domain.DeviceAuth
	interval int
	elapsed  int
	phase    phase
	errMsg   string
	notice   string

	width  int
	height int
}

func New(d deps.Deps, info domain.SystemInfo) Model {
	return Model{deps: d, info: info, spinner: spinner.New()}
}

func (m Model) Init() tea.Cmd {
	return tea.Batch(m.spinner.Tick, startDevice(m.deps))
}

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		return m, nil
	case tea.KeyPressMsg:
		return m.onKey(msg)
	case deviceStartedMsg:
		return m.onStarted(msg.device)
	case deviceFailedMsg:
		return m.fail(msg.message)
	case pollTickMsg:
		return m.onTick()
	case pollResultMsg:
		return m.onResult(msg)
	case spinner.TickMsg:
		var cmd tea.Cmd
		m.spinner, cmd = m.spinner.Update(msg)
		return m, cmd
	}
	return m, nil
}

func (m Model) onKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "esc":
		return m, gotoLogin(m.info)
	case "enter":
		if m.phase == phaseFailed {
			return m, gotoLogin(m.info)
		}
	case "y":
		if m.device.UserCode != "" {
			m.notice = "Code copied to clipboard"
			return m, tea.SetClipboard(m.device.UserCode)
		}
	}
	return m, nil
}

func (m Model) onStarted(device domain.DeviceAuth) (Model, tea.Cmd) {
	m.device = device
	m.interval = device.Interval
	if m.interval < 1 {
		m.interval = 5
	}
	m.phase = phaseWaiting
	return m, tea.Batch(openBrowser(device), pollAfter(m.interval))
}

func (m Model) onTick() (Model, tea.Cmd) {
	if m.phase != phaseWaiting {
		return m, nil
	}
	m.elapsed += m.interval
	if m.device.ExpiresIn > 0 && m.elapsed >= m.device.ExpiresIn {
		return m.fail("The code expired. Please try again.")
	}
	return m, poll(m.deps, m.device.DeviceCode)
}

func (m Model) onResult(msg pollResultMsg) (Model, tea.Cmd) {
	if m.phase != phaseWaiting {
		return m, nil
	}
	if msg.err != nil {
		slog.Debug("device poll failed", "err", msg.err)
		return m, pollAfter(m.interval) // transient, keep waiting
	}

	switch msg.poll.Status {
	case domain.DeviceComplete:
		m.phase = phaseDone
		return m, finishLogin(m.deps, m.info, msg.poll.Tokens)
	case domain.DeviceSlowDown:
		m.interval += 5
		return m, pollAfter(m.interval)
	case domain.DevicePending:
		return m, pollAfter(m.interval)
	case domain.DeviceDenied:
		return m.fail("Authorization was denied.")
	case domain.DeviceExpired:
		return m.fail("The code expired. Please try again.")
	default:
		return m.fail("Sign-in failed. Please try again.")
	}
}

func (m Model) fail(message string) (Model, tea.Cmd) {
	m.phase = phaseFailed
	m.errMsg = message
	return m, nil
}

func (m Model) View() string {
	s := m.deps.Styles

	var rows []string
	switch m.phase {
	case phaseStarting:
		rows = []string{m.spinner.View() + " Starting..."}
	case phaseWaiting:
		rows = []string{
			"Open this page and enter the code:",
			"",
			s.Title.Render(m.device.UserCode),
			"",
			s.Muted.Render(m.device.VerificationURI),
			"",
			m.spinner.View() + " Waiting for authorization...",
			"",
			s.Hint.Render("Only enter this code for a login you started."),
		}
		if m.notice != "" {
			rows = append(rows, s.Muted.Render(m.notice))
		}
	case phaseFailed:
		rows = []string{s.Error.Render(m.errMsg)}
	case phaseDone:
		rows = []string{s.Muted.Render("Signing in...")}
	}

	body := lipgloss.NewStyle().Padding(2, 1).Render(lipgloss.JoinVertical(lipgloss.Center, rows...))
	dialog := components.TitledBox("Sign in with "+m.idpLabel(), body, s.Theme.Accent)
	if m.width == 0 {
		return dialog
	}
	return lipgloss.Place(m.width, m.height, lipgloss.Center, lipgloss.Center, dialog)
}

func (m Model) HelpKeys() []key.Binding {
	switch m.phase {
	case phaseWaiting:
		return []key.Binding{
			key.NewBinding(key.WithKeys("y"), key.WithHelp("y", "copy code")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
		}
	case phaseFailed:
		return []key.Binding{
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "back")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
		}
	}
	return nil
}

func (m Model) idpLabel() string {
	if m.info.Setup.OIDC != nil && m.info.Setup.OIDC.ProviderName != "" {
		return m.info.Setup.OIDC.ProviderName
	}
	return "SSO"
}

type deviceStartedMsg struct{ device domain.DeviceAuth }

type deviceFailedMsg struct{ message string }

type pollTickMsg struct{}

type pollResultMsg struct {
	poll domain.DevicePoll
	err  error
}

func startDevice(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		device, err := d.Public.StartDeviceLogin(context.Background())
		if err != nil {
			slog.Warn("start device login", "err", err)
			return deviceFailedMsg{message: "Could not start sign-in. Please try again."}
		}
		if device.DeviceCode == "" || device.UserCode == "" || device.VerificationURI == "" {
			return deviceFailedMsg{message: "Could not start sign-in. Please try again."}
		}
		return deviceStartedMsg{device: device}
	}
}

func poll(d deps.Deps, deviceCode string) tea.Cmd {
	return func() tea.Msg {
		result, err := d.Public.PollDeviceLogin(context.Background(), deviceCode)
		return pollResultMsg{poll: result, err: err}
	}
}

func pollAfter(interval int) tea.Cmd {
	return tea.Tick(time.Duration(interval)*time.Second, func(time.Time) tea.Msg {
		return pollTickMsg{}
	})
}

func openBrowser(device domain.DeviceAuth) tea.Cmd {
	url := device.VerificationURIComplete
	if url == "" {
		url = device.VerificationURI
	}
	return func() tea.Msg {
		if err := openURL(url); err != nil {
			slog.Debug("open browser", "err", err)
		}
		return nil
	}
}

func finishLogin(d deps.Deps, info domain.SystemInfo, tokens domain.TokenPair) tea.Cmd {
	return func() tea.Msg {
		if err := d.Store.Save(d.Server, tokens); err != nil {
			slog.Warn("save tokens", "err", err)
		}
		d.Transport.SetTokens(tokens)
		return nav.GoToHomeMsg{Info: info}
	}
}

func gotoLogin(info domain.SystemInfo) tea.Cmd {
	return func() tea.Msg { return nav.GoToLoginMsg{Info: info} }
}

// openURL only opens http(s) URLs, since the address comes from the server.
func openURL(rawURL string) error {
	if !strings.HasPrefix(rawURL, "http://") && !strings.HasPrefix(rawURL, "https://") {
		return nil
	}

	var name string
	var args []string
	switch runtime.GOOS {
	case "darwin":
		name = "open"
	case "windows":
		name = "cmd"
		args = []string{"/c", "start", ""}
	default:
		name = "xdg-open"
	}
	args = append(args, rawURL)
	return exec.Command(name, args...).Start()
}
