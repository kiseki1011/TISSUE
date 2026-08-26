package ui

import (
	"context"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// inboxPollInterval paces the unread-badge poll. Notifications are not pushed, so polling is the only signal.
const inboxPollInterval = 60 * time.Second

// inboxUnreadMsg carries one unread-status poll result. reArm separates the periodic loop from a
// one-shot refresh that must not spawn a second loop. gen drops a poll from a logged-out session.
type inboxUnreadMsg struct {
	has   bool
	ok    bool
	reArm bool
	gen   int
}

// inboxRepollMsg fires when the poll interval elapses, kicking off the next unread check.
type inboxRepollMsg struct{ gen int }

// pollInboxUnread checks the unread status once. reArm=false updates the badge without re-arming.
func pollInboxUnread(d deps.Deps, gen int, reArm bool) tea.Cmd {
	return func() tea.Msg {
		if d.Notifications == nil {
			return inboxUnreadMsg{gen: gen, reArm: reArm} // no service (as in tests): don't claim unread
		}
		has, err := d.Notifications.HasUnread(context.Background())
		return inboxUnreadMsg{has: has, ok: err == nil, reArm: reArm, gen: gen}
	}
}
