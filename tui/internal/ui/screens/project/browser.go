package project

import (
	"os/exec"
	"runtime"
	"strings"

	tea "charm.land/bubbletea/v2"
)

// openURLCmd opens an http(s) URL in the user's default browser off the update loop. It is used by the
// issue detail's clickable branch and commit links. A failure is swallowed: a link that will not open is
// a minor inconvenience, not worth interrupting the UI, and the URL text is still on screen.
func openURLCmd(url string) tea.Cmd {
	return func() tea.Msg {
		_ = openURL(url)
		return nil
	}
}

// openURL launches the platform browser for an http(s) URL. Non-http(s) input is ignored so a malformed
// or non-web link cannot be handed to the shell. Start (not Run) returns immediately without waiting.
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
