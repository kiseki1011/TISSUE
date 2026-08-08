package realtime

import (
	"fmt"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
	"time"
)

const issueFrame = `{"type":"ISSUE_TRANSITIONED","projectKey":"ENG","issueKey":"ENG-1","actorMemberId":42,"occurredAt":"2026-08-08T00:00:00Z","data":{"newStateId":3,"newStateName":"Done"}}`

// fast lowers the timing knobs so reconnect/backoff/watchdog behaviour is testable
// in milliseconds rather than seconds.
func fast(c *Consumer) {
	c.backoffFloor = 5 * time.Millisecond
	c.backoffCeiling = 20 * time.Millisecond
	c.headerTimeout = 60 * time.Millisecond
	c.readTimeout = 60 * time.Millisecond
	c.healthyFor = time.Hour // treat every drop as unhealthy so backoff stays at the floor
}

func writeSSE(w http.ResponseWriter, f http.Flusher, s string) {
	fmt.Fprint(w, s)
	f.Flush()
}

// collect reads updates until want is satisfied, failing on close/timeout.
func collect(t *testing.T, c *Consumer, want func([]Update) bool) []Update {
	t.Helper()
	var got []Update
	deadline := time.After(3 * time.Second)
	for {
		if want(got) {
			return got
		}
		select {
		case u, ok := <-c.Updates():
			if !ok {
				if want(got) {
					return got
				}
				t.Fatalf("channel closed before predicate satisfied; got states %v", states(got))
			}
			got = append(got, u)
		case <-deadline:
			t.Fatalf("timed out; got states %v", states(got))
		}
	}
}

func states(us []Update) []string {
	var out []string
	for _, u := range us {
		if u.Kind == StateUpdate {
			out = append(out, u.State.String())
		} else {
			out = append(out, "event:"+u.Event.Type)
		}
	}
	return out
}

func countState(us []Update, s State) int {
	n := 0
	for _, u := range us {
		if u.Kind == StateUpdate && u.State == s {
			n++
		}
	}
	return n
}

func firstEvent(us []Update) (Event, bool) {
	for _, u := range us {
		if u.Kind == EventUpdate {
			return u.Event, true
		}
	}
	return Event{}, false
}

// A live connection reports Connecting then Connected, and a data frame is parsed
// into a fully-populated Event carrying the session generation.
func TestConnectAndParseEvent(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		f := w.(http.Flusher)
		w.Header().Set("Content-Type", "text/event-stream")
		writeSSE(w, f, ":connected\n\n")
		writeSSE(w, f, "event: issue\nid: evt-1\ndata: "+issueFrame+"\n\n")
		<-r.Context().Done() // hold open until the client disconnects
	}))
	defer srv.Close()

	c := New(srv.URL, srv.Client(), 7)
	fast(c)
	c.Start()
	defer c.Stop()

	got := collect(t, c, func(us []Update) bool { _, ok := firstEvent(us); return ok })

	if got[0].Kind != StateUpdate || got[0].State != Connecting {
		t.Errorf("first update should be Connecting, got %v", states(got))
	}
	if countState(got, Connected) != 1 {
		t.Errorf("expected one Connected state, got %v", states(got))
	}
	ev, _ := firstEvent(got)
	if ev.Category != "issue" || ev.ID != "evt-1" || ev.Type != "ISSUE_TRANSITIONED" {
		t.Errorf("event header mis-parsed: %+v", ev)
	}
	if ev.ProjectKey != "ENG" || ev.IssueKey != "ENG-1" || ev.ActorMemberID != 42 {
		t.Errorf("event fields mis-parsed: %+v", ev)
	}
	if ev.Data["newStateName"] != "Done" {
		t.Errorf("event data payload mis-parsed: %+v", ev.Data)
	}
	if ev.OccurredAt != "2026-08-08T00:00:00Z" {
		t.Errorf("occurredAt mis-parsed: %q", ev.OccurredAt)
	}
	for _, u := range got {
		if u.Gen != 7 {
			t.Fatalf("update did not carry the session generation: %+v", u)
		}
	}
}

// When the connection drops, the consumer reports Disconnected and reconnects,
// receiving the next connection's events.
func TestReconnectAfterDrop(t *testing.T) {
	var conns atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		f := w.(http.Flusher)
		w.Header().Set("Content-Type", "text/event-stream")
		n := conns.Add(1)
		if n == 1 {
			writeSSE(w, f, "event: issue\nid: a\ndata: "+issueFrame+"\n\n")
			return // drop the connection
		}
		writeSSE(w, f, "event: sprint\nid: b\ndata: {\"type\":\"SPRINT_STARTED\",\"projectKey\":\"ENG\",\"issueKey\":null,\"data\":{\"sprintId\":9}}\n\n")
		<-r.Context().Done()
	}))
	defer srv.Close()

	c := New(srv.URL, srv.Client(), 1)
	fast(c)
	c.Start()
	defer c.Stop()

	// wait until we have seen two Connected states (the reconnect) and the second event
	got := collect(t, c, func(us []Update) bool {
		sawSprint := false
		for _, u := range us {
			if u.Kind == EventUpdate && u.Event.Type == "SPRINT_STARTED" {
				sawSprint = true
			}
		}
		return countState(us, Connected) >= 2 && sawSprint
	})

	if countState(got, Disconnected) < 1 {
		t.Errorf("a drop should surface Disconnected: %v", states(got))
	}
	// the sprint event has a null issueKey — it must parse to an empty IssueKey
	for _, u := range got {
		if u.Kind == EventUpdate && u.Event.Type == "SPRINT_STARTED" && u.Event.IssueKey != "" {
			t.Errorf("null issueKey should parse empty, got %q", u.Event.IssueKey)
		}
	}
}

// A non-200 response backs off and retries rather than surfacing a bogus Connected.
func TestNon200BacksOffThenReconnects(t *testing.T) {
	var conns atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if conns.Add(1) == 1 {
			w.WriteHeader(http.StatusInternalServerError)
			return
		}
		f := w.(http.Flusher)
		w.Header().Set("Content-Type", "text/event-stream")
		writeSSE(w, f, "event: issue\nid: c\ndata: "+issueFrame+"\n\n")
		<-r.Context().Done()
	}))
	defer srv.Close()

	c := New(srv.URL, srv.Client(), 1)
	fast(c)
	c.Start()
	defer c.Stop()

	got := collect(t, c, func(us []Update) bool { _, ok := firstEvent(us); return ok })

	// the first attempt (500) must not report Connected; the Connected we see is the retry
	firstConnectedAt, firstEventAt := -1, -1
	for i, u := range got {
		if u.Kind == StateUpdate && u.State == Connected && firstConnectedAt < 0 {
			firstConnectedAt = i
		}
		if u.Kind == EventUpdate && firstEventAt < 0 {
			firstEventAt = i
		}
	}
	if countState(got, Disconnected) < 1 {
		t.Errorf("the 500 attempt should surface Disconnected before the retry: %v", states(got))
	}
	if firstConnectedAt < 0 || firstConnectedAt > firstEventAt {
		t.Errorf("Connected should precede the event: %v", states(got))
	}
}

// A silent connection (no heartbeat) is detected by the read watchdog and reconnected.
func TestWatchdogReconnectsSilentConnection(t *testing.T) {
	var conns atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		f := w.(http.Flusher)
		w.Header().Set("Content-Type", "text/event-stream")
		f.Flush() // send 200 headers so the client reports Connected
		if conns.Add(1) == 1 {
			<-r.Context().Done() // never send a byte: the watchdog must fire
			return
		}
		writeSSE(w, f, "event: issue\nid: d\ndata: "+issueFrame+"\n\n")
		<-r.Context().Done()
	}))
	defer srv.Close()

	c := New(srv.URL, srv.Client(), 1)
	fast(c) // readTimeout 60ms
	c.Start()
	defer c.Stop()

	got := collect(t, c, func(us []Update) bool { _, ok := firstEvent(us); return ok })
	if countState(got, Connected) < 2 {
		t.Errorf("watchdog should have forced a reconnect (2 Connected): %v", states(got))
	}
	if countState(got, Disconnected) < 1 {
		t.Errorf("the dead connection should surface Disconnected: %v", states(got))
	}
}

// A peer that accepts the socket but never sends response headers is bounded by the header-arrival
// deadline (not just the post-headers read watchdog) and reconnects.
func TestHeaderTimeoutReconnectsStalledConnect(t *testing.T) {
	var conns atomic.Int32
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if conns.Add(1) == 1 {
			<-r.Context().Done() // never write headers: Do() must not block past headerTimeout
			return
		}
		f := w.(http.Flusher)
		w.Header().Set("Content-Type", "text/event-stream")
		writeSSE(w, f, "event: issue\nid: e\ndata: "+issueFrame+"\n\n")
		<-r.Context().Done()
	}))
	defer srv.Close()

	c := New(srv.URL, srv.Client(), 1)
	fast(c) // headerTimeout 60ms
	c.Start()
	defer c.Stop()

	got := collect(t, c, func(us []Update) bool { _, ok := firstEvent(us); return ok })
	if countState(got, Disconnected) < 1 {
		t.Errorf("the stalled connect should surface Disconnected before the retry: %v", states(got))
	}
	if countState(got, Connected) < 1 {
		t.Errorf("the retry should reach Connected: %v", states(got))
	}
}

// Stop ends the goroutine and closes the channel.
func TestStopClosesChannel(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		f := w.(http.Flusher)
		w.Header().Set("Content-Type", "text/event-stream")
		writeSSE(w, f, ":connected\n\n")
		<-r.Context().Done()
	}))
	defer srv.Close()

	c := New(srv.URL, srv.Client(), 1)
	fast(c)
	c.Start()
	collect(t, c, func(us []Update) bool { return countState(us, Connected) >= 1 })
	c.Stop()

	closed := false
	deadline := time.After(2 * time.Second)
	for !closed {
		select {
		case _, ok := <-c.Updates():
			if !ok {
				closed = true
			}
		case <-deadline:
			t.Fatal("channel was not closed after Stop")
		}
	}
}

func TestSplitField(t *testing.T) {
	cases := []struct{ line, field, value string }{
		{"event: issue", "event", "issue"},
		{"data:{\"a\":1}", "data", "{\"a\":1}"}, // no space after colon
		{"data: hello", "data", "hello"},        // one leading space stripped
		{"data:  hello", "data", " hello"},      // only one space stripped
		{"id: 42", "id", "42"},
		{"bare", "bare", ""},
	}
	for _, tc := range cases {
		f, v := splitField(tc.line)
		if f != tc.field || v != tc.value {
			t.Errorf("splitField(%q) = (%q,%q), want (%q,%q)", tc.line, f, v, tc.field, tc.value)
		}
	}
}

func TestParseEventMultilineAndBadData(t *testing.T) {
	// SSE joins multiple data: lines with \n; the payload is still valid JSON.
	ev, ok := parseEvent("issue", "x", "{\n\"type\":\"ISSUE_CREATED\",\n\"projectKey\":\"ENG\"\n}")
	if !ok || ev.Type != "ISSUE_CREATED" || ev.ProjectKey != "ENG" {
		t.Errorf("multiline data mis-parsed: %+v ok=%v", ev, ok)
	}
	if _, ok := parseEvent("issue", "x", "not json"); ok {
		t.Error("non-JSON data should be rejected")
	}
}
