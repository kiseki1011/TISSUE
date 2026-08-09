// Package realtime consumes the backend's user-scoped SSE stream
// (GET /api/v1/events/stream) and surfaces connection-state changes and parsed
// domain events to the UI. It is deliberately UI-agnostic: the Bubble Tea shell
// adapts Updates() into its own messages.
//
// The stream is "an HTTP request that never ends": the server holds the response
// open, pushing `event:`/`id:`/`data:` frames plus `:keep-alive` comments every
// ~15s. The Consumer reads frames until the connection drops, then reconnects
// with exponential backoff. A read watchdog (longer than the heartbeat) detects a
// silently dead connection and forces a reconnect.
package realtime

import (
	"bufio"
	"context"
	"encoding/json"
	"io"
	"log/slog"
	"net/http"
	"strings"
	"time"
)

// endpointPath is the user-scoped SSE stream. The caller's bearer token (added by
// the shared auth transport) scopes it to that member's projects.
const endpointPath = "/api/v1/events/stream"

const (
	// defaultReadTimeout must exceed the server's 15s heartbeat so a live-but-idle
	// stream is not mistaken for a dead one. If no byte (frame or comment) arrives
	// within this window the connection is treated as dead and reconnected.
	defaultReadTimeout = 40 * time.Second
	// defaultHeaderTimeout bounds the phase between the socket being established and
	// the response headers arriving. The read watchdog only guards a live stream, so
	// without this a peer that accepts the connection but never sends headers (an
	// overloaded backend, a half-open proxy) would wedge us in Connecting forever.
	defaultHeaderTimeout = 15 * time.Second
	// defaultHealthyFor is how long a connection must survive before its drop resets
	// the backoff. A connection that dies immediately keeps backing off; one that
	// held for a while starts the next backoff from the floor.
	defaultHealthyFor     = 5 * time.Second
	defaultBackoffFloor   = 1 * time.Second
	defaultBackoffCeiling = 30 * time.Second
	updateChanDepth       = 16
)

// State is the connection state surfaced to the header indicator.
type State int

const (
	Disconnected State = iota
	Connecting
	Connected
)

func (s State) String() string {
	switch s {
	case Connecting:
		return "connecting"
	case Connected:
		return "connected"
	default:
		return "disconnected"
	}
}

// Event is one parsed SSE frame. Category is the SSE `event:` name ("issue",
// "sprint", or "notification"); the rest come from the JSON `data:` payload (RealtimeMessage).
type Event struct {
	Category      string         // SSE event name: "issue" | "sprint" | "notification"
	ID            string         // SSE id: the domain event UUID
	Type          string         // data.type: ISSUE_CREATED, SPRINT_STARTED, ...
	ProjectKey    string         // data.projectKey
	IssueKey      string         // data.issueKey (empty for sprint events)
	ActorMemberID int64          // data.actorMemberId (0 if absent)
	OccurredAt    string         // data.occurredAt (ISO instant)
	Data          map[string]any // data.data: per-type extra fields
}

// UpdateKind distinguishes the two things a Consumer surfaces.
type UpdateKind int

const (
	StateUpdate UpdateKind = iota
	EventUpdate
)

// Update is one item read off the Consumer's channel: either a connection-state
// change or an arrived event. Gen echoes the session generation the Consumer was
// created with, so the shell can drop updates from a superseded session.
type Update struct {
	Kind  UpdateKind
	State State
	Event Event
	Gen   int
}

// wireMessage mirrors the backend RealtimeMessage record (data: payload).
type wireMessage struct {
	Type          string         `json:"type"`
	ProjectKey    string         `json:"projectKey"`
	IssueKey      *string        `json:"issueKey"`
	ActorMemberID *int64         `json:"actorMemberId"`
	OccurredAt    string         `json:"occurredAt"`
	Data          map[string]any `json:"data"`
}

// Consumer streams SSE frames from one server on a background goroutine and
// publishes Updates on a channel. It is single-use: New, Start, then Stop.
type Consumer struct {
	server string
	client *http.Client
	gen    int

	// timing knobs; defaulted in New, overridable in tests for speed
	headerTimeout  time.Duration
	readTimeout    time.Duration
	healthyFor     time.Duration
	backoffFloor   time.Duration
	backoffCeiling time.Duration

	ch     chan Update
	ctx    context.Context
	cancel context.CancelFunc
}

// New builds a Consumer for server (its base URL, e.g. http://host:8080) using
// client, whose transport must attach the bearer token. gen is echoed on every
// Update so the shell can ignore a stale session's updates.
func New(server string, client *http.Client, gen int) *Consumer {
	return &Consumer{
		server:         strings.TrimRight(server, "/") + endpointPath,
		client:         client,
		gen:            gen,
		headerTimeout:  defaultHeaderTimeout,
		readTimeout:    defaultReadTimeout,
		healthyFor:     defaultHealthyFor,
		backoffFloor:   defaultBackoffFloor,
		backoffCeiling: defaultBackoffCeiling,
		ch:             make(chan Update, updateChanDepth),
	}
}

// Updates is the read side of the channel. It is closed when the Consumer's
// goroutine exits (after Stop or an unrecoverable context cancel), so a pending
// receive yields the zero Update with ok=false.
func (c *Consumer) Updates() <-chan Update { return c.ch }

// Start launches the connect/read/reconnect loop. Call once.
func (c *Consumer) Start() {
	c.ctx, c.cancel = context.WithCancel(context.Background())
	go c.run()
}

// Stop cancels the loop. The goroutine emits no further updates and closes the
// channel. Safe to call once; a nil-cancel (never started) Consumer is a no-op.
func (c *Consumer) Stop() {
	if c.cancel != nil {
		c.cancel()
	}
}

func (c *Consumer) run() {
	defer close(c.ch)
	backoff := c.backoffFloor
	for {
		if c.ctx.Err() != nil {
			return
		}
		c.emit(Update{Kind: StateUpdate, State: Connecting, Gen: c.gen})

		healthy := c.stream()

		if c.ctx.Err() != nil {
			return
		}
		c.emit(Update{Kind: StateUpdate, State: Disconnected, Gen: c.gen})

		if healthy {
			backoff = c.backoffFloor
		}
		select {
		case <-c.ctx.Done():
			return
		case <-time.After(backoff):
		}
		backoff = min(backoff*2, c.backoffCeiling)
	}
}

// stream opens one connection and reads frames until it drops. It returns whether
// the connection was healthy long enough to reset the backoff.
func (c *Consumer) stream() (healthy bool) {
	reqCtx, cancel := context.WithCancel(c.ctx)
	defer cancel()

	req, err := http.NewRequestWithContext(reqCtx, http.MethodGet, c.server, nil)
	if err != nil {
		slog.Debug("realtime: build request", "err", err)
		return false
	}
	req.Header.Set("Accept", "text/event-stream")
	req.Header.Set("Cache-Control", "no-cache")

	// bound the header-arrival phase, then disarm the moment Do returns so the deadline never applies to
	// the streaming body read (the read watchdog guards that instead). Without this, a peer that accepts
	// the socket but never sends headers would block Do forever and wedge us in Connecting.
	headerGuard := time.AfterFunc(c.headerTimeout, cancel)
	resp, err := c.client.Do(req)
	headerGuard.Stop()
	if err != nil {
		slog.Debug("realtime: connect", "err", err)
		return false
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		// A 401 here means the auth transport already tried (and failed) to refresh:
		// the session is dead. Other codes are server-side. Both back off and retry.
		slog.Debug("realtime: non-200", "status", resp.StatusCode)
		return false
	}

	c.emit(Update{Kind: StateUpdate, State: Connected, Gen: c.gen})
	return c.read(reqCtx, cancel, resp.Body)
}

// read parses the SSE body until it ends. A watchdog cancels the request (via
// cancel) if no byte arrives within readTimeout, unblocking the scanner so the
// caller reconnects.
func (c *Consumer) read(ctx context.Context, cancel context.CancelFunc, body io.Reader) (healthy bool) {
	start := time.Now()
	watchdog := time.AfterFunc(c.readTimeout, cancel)
	defer watchdog.Stop()

	scanner := bufio.NewScanner(body)
	scanner.Buffer(make([]byte, 0, 64*1024), 1024*1024)

	var name, id string
	var data strings.Builder
	for scanner.Scan() {
		watchdog.Reset(c.readTimeout)
		line := scanner.Text()
		switch {
		case line == "":
			if data.Len() > 0 {
				if ev, ok := parseEvent(name, id, data.String()); ok {
					c.emit(Update{Kind: EventUpdate, Event: ev, Gen: c.gen})
				}
			}
			name, id, data = "", "", strings.Builder{}
		case strings.HasPrefix(line, ":"):
			// comment (":connected" on subscribe, ":keep-alive" heartbeat) — the
			// watchdog reset above already counted it as liveness
		default:
			field, value := splitField(line)
			switch field {
			case "event":
				name = value
			case "id":
				id = value
			case "data":
				if data.Len() > 0 {
					data.WriteByte('\n')
				}
				data.WriteString(value)
			}
		}
		if ctx.Err() != nil {
			break
		}
	}
	return time.Since(start) > c.healthyFor
}

// emit delivers an update, or drops it if the Consumer is stopping (so a stalled
// reader can never wedge the goroutine).
func (c *Consumer) emit(u Update) {
	select {
	case c.ch <- u:
	case <-c.ctx.Done():
	}
}

// splitField parses one SSE line into field/value, stripping a single leading
// space from the value per the SSE spec. A line with no colon is a field whose
// value is empty.
func splitField(line string) (field, value string) {
	i := strings.IndexByte(line, ':')
	if i < 0 {
		return line, ""
	}
	value = line[i+1:]
	if strings.HasPrefix(value, " ") {
		value = value[1:]
	}
	return line[:i], value
}

// parseEvent turns a dispatched frame into an Event. It fails (ok=false) when the
// data is not the expected JSON, which the caller skips.
func parseEvent(name, id, data string) (Event, bool) {
	var msg wireMessage
	if err := json.Unmarshal([]byte(data), &msg); err != nil {
		slog.Debug("realtime: bad data frame", "err", err)
		return Event{}, false
	}
	ev := Event{
		Category:   name,
		ID:         id,
		Type:       msg.Type,
		ProjectKey: msg.ProjectKey,
		OccurredAt: msg.OccurredAt,
		Data:       msg.Data,
	}
	if msg.IssueKey != nil {
		ev.IssueKey = *msg.IssueKey
	}
	if msg.ActorMemberID != nil {
		ev.ActorMemberID = *msg.ActorMemberID
	}
	return ev, true
}
