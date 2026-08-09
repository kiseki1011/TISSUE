package ui

import (
	"context"
	"time"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
)

// inboxPollInterval is how often the shell re-checks the unread badge. Notifications are not pushed
// over SSE, so this poll is the only signal that new items arrived; a light boolean GET keeps it cheap.
const inboxPollInterval = 60 * time.Second

// inboxUnreadMsg carries one unread-status poll result. reArm distinguishes the periodic loop (which
// schedules the next tick) from a one-shot refresh after a read action (which must not spawn a second
// loop). gen ties it to the session so a logged-out poll is dropped and its loop ends.
type inboxUnreadMsg struct {
	has   bool
	ok    bool
	reArm bool
	gen   int
}

// inboxRepollMsg fires when the poll interval elapses, kicking off the next unread check.
type inboxRepollMsg struct{ gen int }

// pollInboxUnread checks the unread status once. reArm=true belongs to the periodic loop; reArm=false
// is a one-shot refresh (after a read action) that updates the badge without re-arming the timer.
func pollInboxUnread(d deps.Deps, gen int, reArm bool) tea.Cmd {
	return func() tea.Msg {
		if d.Notifications == nil {
			return inboxUnreadMsg{gen: gen, reArm: reArm} // no service (as in tests): don't claim unread
		}
		has, err := d.Notifications.HasUnread(context.Background())
		return inboxUnreadMsg{has: has, ok: err == nil, reArm: reArm, gen: gen}
	}
}
