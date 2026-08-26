// Package nav holds the messages that screens emit to request navigation
package nav

import "github.com/kiseki1011/TISSUE/tui/internal/domain"

type GoToLoginMsg struct {
	Info domain.SystemInfo
}

type GoToHomeMsg struct {
	Info    domain.SystemInfo
	Welcome string
	// Restore marks a silent session restore (relaunch re-authed from a saved token), so the shell
	// deep-links back into the last-open project instead of landing on the dashboard.
	Restore bool
}

type GoToOidcDeviceMsg struct {
	Info domain.SystemInfo
}

type OpenProjectMsg struct {
	ProjectKey string
	Title      string
	// IssueKey opens that issue's read-only peek on entry (the Inbox drills in this way). SprintID, with
	// IssueKey empty, lands on the Sprints sub-tab. Both zero drills into the issue list.
	IssueKey string
	SprintID int64
}

type CloseProjectMsg struct{}

// RefreshDashboardMsg reloads the home dashboard, so an edit made inside a drill-in shows on return.
type RefreshDashboardMsg struct{}
