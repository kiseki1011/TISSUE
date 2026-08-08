package ui

import (
	"net/http"
	"strings"
	"testing"

	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/realtime"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
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

// applyRealtime folds a matching-generation update into the model and re-arms the wait; an update from
// a superseded session is dropped and does not re-arm.
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

// Without an authed transport (as in tests that never log in) startRealtime is a no-op: no consumer,
// no command, and the indicator stays disconnected.
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

// stopRealtime resets the indicator to disconnected.
func TestStopRealtimeResetsIndicator(t *testing.T) {
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode)}
	a := New(d)
	a.rtState = realtime.Connected
	a.stopRealtime()
	if a.rtState != realtime.Disconnected {
		t.Errorf("stopRealtime should reset the indicator, got %v", a.rtState)
	}
}
