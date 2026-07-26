// Package nav holds the messages that screens emit to request navigation
package nav

import "github.com/kiseki1011/TISSUE/tui/internal/domain"

// Info carries the server configuration the login form needs.
type GoToLoginMsg struct {
	Info domain.SystemInfo
}

type GoToHomeMsg struct {
	Info    domain.SystemInfo
	Welcome string
}

type GoToOidcDeviceMsg struct {
	Info domain.SystemInfo
}

type OpenProjectMsg struct {
	ProjectKey string
	Title      string
}

type CloseProjectMsg struct{}
