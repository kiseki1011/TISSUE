// Package nav holds the messages that screens emit to request navigation
package nav

import "github.com/kiseki1011/TISSUE/tui/internal/domain"

// GoToLoginMsg asks the router to show the login screen.
// Info carries the server configuration the login form needs.
type GoToLoginMsg struct {
	Info domain.SystemInfo
}

// GoToHomeMsg asks the router to show the post-login home screen.
type GoToHomeMsg struct {
	Info    domain.SystemInfo
	Welcome string
}

// GoToOidcDeviceMsg asks the router to start the OIDC device login flow.
type GoToOidcDeviceMsg struct {
	Info domain.SystemInfo
}
