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
	// Restore is set on a silent session restore (an app relaunch that re-authed from a saved token),
	// telling the shell to deep-link back into the last-open project. A fresh login leaves it false so
	// the session lands on the dashboard.
	Restore bool
}

type GoToOidcDeviceMsg struct {
	Info domain.SystemInfo
}

type OpenProjectMsg struct {
	ProjectKey string
	Title      string
	// IssueKey, when set, opens that issue's read-only detail (a peek) on entry — used by the Inbox to
	// drill from a notification straight to its issue. SprintID, when set (and IssueKey empty), lands on
	// the Sprints sub-tab with that sprint selected. Both zero drills into the project's issue list.
	IssueKey string
	SprintID int64
}

type CloseProjectMsg struct{}

// RefreshDashboardMsg asks the app to silently reload the home dashboard's projects list. A project
// drill-in fires it after a mutation that changes how the project reads on the dashboard (a settings
// edit), so the change shows on the list when the user returns to it.
type RefreshDashboardMsg struct{}
