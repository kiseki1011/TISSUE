package project

import (
	"os/exec"
	"runtime"
	"strings"

	tea "charm.land/bubbletea/v2"
)

// openURLCmd opens a URL off the update loop. A failure is swallowed: the URL text is still on screen.
func openURLCmd(url string) tea.Cmd {
	return func() tea.Msg {
		_ = openURL(url)
		return nil
	}
}

// openURL launches the platform browser. Non-http(s) input is ignored so a malformed link cannot reach
// the shell. Start (not Run) avoids waiting on the browser.
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
