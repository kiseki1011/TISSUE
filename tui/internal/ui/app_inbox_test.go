package ui

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/inbox"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func inboxTestApp() App {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://x"}
	a := New(d)
	a.screen = screenHome
	a.width, a.height = 120, 32
	a.sessionGen = 1
	return a
}

// A matching-generation unread poll updates the badge and, for the periodic path, re-arms the timer.
func TestInboxUnreadUpdatesBadgeAndRearms(t *testing.T) {
	a := inboxTestApp()
	m, cmd := a.Update(inboxUnreadMsg{has: true, ok: true, reArm: true, gen: 1})
	if !m.(App).inboxUnread {
		t.Error("badge not set from an unread poll")
	}
	if cmd == nil {
		t.Error("periodic poll should re-arm the timer")
	}
}

// A one-shot refresh (after a read action) updates the badge but does not re-arm a second loop.
func TestInboxUnreadOneShotNoRearm(t *testing.T) {
	a := inboxTestApp()
	a.inboxUnread = true
	m, cmd := a.Update(inboxUnreadMsg{has: false, ok: true, reArm: false, gen: 1})
	if m.(App).inboxUnread {
		t.Error("badge not cleared")
	}
	if cmd != nil {
		t.Error("one-shot refresh should not re-arm")
	}
}

// A poll from a superseded session is dropped and ends its loop.
func TestInboxUnreadStaleGenDropped(t *testing.T) {
	a := inboxTestApp()
	a.inboxUnread = false
	m, cmd := a.Update(inboxUnreadMsg{has: true, ok: true, reArm: true, gen: 0})
	if m.(App).inboxUnread {
		t.Error("stale-gen poll should not touch the badge")
	}
	if cmd != nil {
		t.Error("stale-gen poll should not re-arm")
	}
}

// A failed poll (ok=false) leaves the last known badge state but still re-arms so it retries.
func TestInboxUnreadFailedKeepsStateRearms(t *testing.T) {
	a := inboxTestApp()
	a.inboxUnread = true
	m, cmd := a.Update(inboxUnreadMsg{has: false, ok: false, reArm: true, gen: 1})
	if !m.(App).inboxUnread {
		t.Error("a failed poll should not clear the badge")
	}
	if cmd == nil {
		t.Error("a failed poll should still re-arm to retry")
	}
}

// A read change in the inbox triggers an authoritative badge re-check.
func TestInboxReadChangedTriggersPoll(t *testing.T) {
	a := inboxTestApp()
	_, cmd := a.Update(inbox.ReadChangedMsg{})
	if cmd == nil {
		t.Error("ReadChangedMsg should fire a badge re-check")
	}
}

// The repoll tick from a stale session stops the loop.
func TestInboxRepollStaleGenStops(t *testing.T) {
	a := inboxTestApp()
	_, cmd := a.Update(inboxRepollMsg{gen: 0})
	if cmd != nil {
		t.Error("stale repoll should not continue the loop")
	}
}

// The Inbox tab wears an unread dot only when there is unread mail.
func TestTabBarUnreadDot(t *testing.T) {
	zone.NewGlobal()
	a := inboxTestApp()
	a.inboxUnread = true
	if bar := stripCSI(a.tabBar()); !strings.Contains(bar, "Inbox") || !strings.Contains(bar, "●") {
		t.Errorf("unread tab bar missing Inbox dot:\n%s", bar)
	}
	a.inboxUnread = false
	if bar := stripCSI(a.tabBar()); strings.Contains(bar, "●") {
		t.Errorf("read tab bar should have no dot:\n%s", bar)
	}
}

// The 4th tab digit switches to the Inbox. Driven from the Schema tab, whose zero-value model does not
// capture input (the zero home model defaults to a focused search field, which would swallow the digit).
func TestFourthTabDigitSwitchesToInbox(t *testing.T) {
	zone.NewGlobal()
	a := inboxTestApp()
	a.screen = screenSchema
	m, _ := a.Update(press("4"))
	if got := m.(App).screen; got != screenInbox {
		t.Errorf("screen = %v after pressing 4, want screenInbox", got)
	}
}

// Drilling into a project from the Inbox remembers the Inbox as the origin, so closing the drill-in
// (esc) returns to the Inbox rather than the Projects dashboard.
func TestDrillFromInboxReturnsToInbox(t *testing.T) {
	zone.NewGlobal()
	a := inboxTestApp()
	a.screen = screenInbox
	m, _ := a.Update(nav.OpenProjectMsg{ProjectKey: "P", IssueKey: "P-1"})
	app := m.(App)
	if app.screen != screenProject {
		t.Fatalf("screen = %v, want screenProject", app.screen)
	}
	if app.projectOrigin != screenInbox {
		t.Errorf("projectOrigin = %v, want screenInbox", app.projectOrigin)
	}
	m2, _ := app.Update(nav.CloseProjectMsg{})
	if got := m2.(App).screen; got != screenInbox {
		t.Errorf("screen after close = %v, want screenInbox (the origin)", got)
	}
}

func press(s string) tea.KeyPressMsg {
	if len(s) == 1 {
		return tea.KeyPressMsg{Code: rune(s[0]), Text: s}
	}
	return tea.KeyPressMsg{Text: s}
}
