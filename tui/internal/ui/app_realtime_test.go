package ui

import (
	"net/http"
	"strings"
	"testing"

	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/realtime"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/inbox"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/project"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// The header dot reflects the live SSE connection state: ● connected, ◐ connecting, ✕ disconnected.
func TestHeaderConnectionIndicator(t *testing.T) {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://x"}
	a := New(d)
	a.screen = screenHome
	a.width, a.height = 100, 32

	cases := []struct {
		state realtime.State
		glyph string
	}{
		{realtime.Connected, "\u25cf"},    // ●
		{realtime.Connecting, "\u25d0"},   // ◐
		{realtime.Disconnected, "\u2715"}, // ✕
	}
	for _, tc := range cases {
		a.rtState = tc.state
		h := stripCSI(a.headerView())
		if !strings.Contains(h, tc.glyph) {
			t.Errorf("state %v: header missing indicator glyph %q:\n%s", tc.state, tc.glyph, h)
		}
		if !strings.Contains(h, "Tissue Server") {
			t.Errorf("state %v: header missing brand label:\n%s", tc.state, h)
		}
	}
}

// A matching-gen update is folded in and re-arms. A superseded session's update is dropped.
func TestApplyRealtimeGenGating(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode)}
	a := New(d)
	a.sessionGen = 5
	a.rt = realtime.New("http://x", &http.Client{}, 5) // non-nil consumer, not started

	m, cmd := a.applyRealtime(realtime.Update{Kind: realtime.StateUpdate, State: realtime.Connected, Gen: 5})
	if got := m.(App).rtState; got != realtime.Connected {
		t.Errorf("matching-gen state update not applied: got %v", got)
	}
	if cmd == nil {
		t.Error("matching-gen update should re-arm the wait")
	}

	a.rtState = realtime.Connecting
	m2, cmd2 := a.applyRealtime(realtime.Update{Kind: realtime.StateUpdate, State: realtime.Connected, Gen: 4})
	if got := m2.(App).rtState; got != realtime.Connecting {
		t.Errorf("stale-gen update should be dropped, rtState changed to %v", got)
	}
	if cmd2 != nil {
		t.Error("stale-gen update should not re-arm")
	}
}

// Without an authed transport startRealtime is a no-op and the indicator stays disconnected.
func TestStartRealtimeNoTransportNoop(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://x"}
	a := New(d) // Transport nil
	cmd := a.startRealtime()
	if cmd != nil {
		t.Error("startRealtime without a transport should return nil")
	}
	if a.rt != nil {
		t.Error("no consumer should be created without a transport")
	}
	if a.rtState != realtime.Disconnected {
		t.Errorf("indicator should stay disconnected, got %v", a.rtState)
	}
}

func TestStopRealtimeResetsIndicator(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode)}
	a := New(d)
	a.rtState = realtime.Connected
	a.stopRealtime()
	if a.rtState != realtime.Disconnected {
		t.Errorf("stopRealtime should reset the indicator, got %v", a.rtState)
	}
}

// An issue event routes to the project screen only when screen and project key match, otherwise it
// falls to the log path. Every case still re-arms, and stale-gen events are dropped.
func TestApplyRealtimeRoutesIssueEvent(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode)}
	a := New(d)
	a.sessionGen = 7
	a.rt = realtime.New("http://x", &http.Client{}, 7)
	a.screen = screenProject
	a.project = project.New(d, "ENG", "Eng")

	evt := func(pk string) realtime.Update {
		return realtime.Update{Kind: realtime.EventUpdate, Gen: 7,
			Event: realtime.Event{Category: "issue", Type: "ISSUE_CREATED", ProjectKey: pk, IssueKey: pk + "-1"}}
	}

	m, cmd := a.applyRealtime(evt("ENG"))
	if cmd == nil {
		t.Error("a routed issue event should re-arm the wait")
	}
	if got := m.(App).project.ProjectKey(); got != "ENG" {
		t.Errorf("routing must not swap the project, got %q", got)
	}

	if _, cmd := a.applyRealtime(evt("OTHER")); cmd == nil {
		t.Error("an event for another project should still re-arm")
	}

	if _, cmd := a.applyRealtime(realtime.Update{Kind: realtime.EventUpdate, Gen: 6,
		Event: realtime.Event{Category: "issue", Type: "ISSUE_CREATED", ProjectKey: "ENG"}}); cmd != nil {
		t.Error("a stale-gen event should not re-arm")
	}
}

// A sprint event for the drilled-in project routes to it. One for another project still re-arms.
func TestApplyRealtimeRoutesSprintEvent(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode)}
	a := New(d)
	a.sessionGen = 7
	a.rt = realtime.New("http://x", &http.Client{}, 7)
	a.screen = screenProject
	a.project = project.New(d, "ENG", "Eng")

	evt := func(pk string) realtime.Update {
		return realtime.Update{Kind: realtime.EventUpdate, Gen: 7, Event: realtime.Event{
			Category: "sprint", Type: "SPRINT_STARTED", ProjectKey: pk,
			Data: map[string]any{"sprintId": float64(3)}}}
	}

	m, cmd := a.applyRealtime(evt("ENG"))
	if cmd == nil {
		t.Error("a routed sprint event should re-arm the wait")
	}
	if got := m.(App).project.ProjectKey(); got != "ENG" {
		t.Errorf("routing must not swap the project, got %q", got)
	}
	if _, cmd := a.applyRealtime(evt("OTHER")); cmd == nil {
		t.Error("a sprint event for another project should still re-arm")
	}
}

// A notification event re-polls the badge on any screen and re-arms. On the Inbox it also refreshes
// the list. A stale-generation event is dropped.
func TestApplyRealtimeNotificationRepolls(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://x"}
	a := New(d)
	a.sessionGen = 9
	a.rt = realtime.New("http://x", &http.Client{}, 9)
	a.screen = screenHome

	notif := realtime.Update{Kind: realtime.EventUpdate, Gen: 9, Event: realtime.Event{
		Category: "notification", Type: "NOTIFICATION_CREATED", ProjectKey: "ENG"}}
	if _, cmd := a.applyRealtime(notif); cmd == nil {
		t.Error("a notification event should re-poll the badge and re-arm")
	}

	// on the Inbox the branch also refreshes the list - it must not panic and still re-arms
	a.screen = screenInbox
	a.inbox = inbox.New(d)
	if _, cmd := a.applyRealtime(notif); cmd == nil {
		t.Error("a notification event on the Inbox screen should re-poll + refresh + re-arm")
	}

	stale := realtime.Update{Kind: realtime.EventUpdate, Gen: 8, Event: realtime.Event{
		Category: "notification", Type: "NOTIFICATION_CREATED"}}
	if _, cmd := a.applyRealtime(stale); cmd != nil {
		t.Error("a stale-gen notification event should be dropped without re-arming")
	}
}

// The extractors read data.sprintId (a JSON number) and data.issueKeys, and are safe on nil payloads.
func TestSprintEventDataExtraction(t *testing.T) {
	data := map[string]any{"sprintId": float64(42), "issueKeys": []any{"ENG-1", "ENG-2"}}
	if got := sprintIDFromEvent(data); got != 42 {
		t.Errorf("sprintIDFromEvent = %d, want 42", got)
	}
	if got := issueKeysFromEvent(data); len(got) != 2 || got[0] != "ENG-1" || got[1] != "ENG-2" {
		t.Errorf("issueKeysFromEvent = %v, want [ENG-1 ENG-2]", got)
	}
	if got := sprintIDFromEvent(nil); got != 0 {
		t.Errorf("sprintIDFromEvent(nil) = %d, want 0", got)
	}
	if got := issueKeysFromEvent(map[string]any{}); got != nil {
		t.Errorf("issueKeysFromEvent(empty) = %v, want nil", got)
	}
}
