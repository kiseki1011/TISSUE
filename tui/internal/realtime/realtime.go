// Package realtime consumes the backend's user-scoped SSE stream
// (GET /api/v1/events/stream) and publishes connection state plus parsed domain events.
// The server holds the response open with ~15s `:keep-alive` comments, so a read watchdog
// (longer than the heartbeat) catches a silently dead connection and reconnects with backoff.
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

// The bearer token added by the shared auth transport scopes this to the member's projects.
const endpointPath = "/api/v1/events/stream"

const (
	// defaultReadTimeout must exceed the server's 15s heartbeat so a live-but-idle stream
	// is not read as dead.
	defaultReadTimeout = 40 * time.Second
	// defaultHeaderTimeout bounds connect->headers. The read watchdog only guards a live
	// stream, so without it a peer that never sends headers wedges us in Connecting.
	defaultHeaderTimeout = 15 * time.Second
	// defaultHealthyFor is how long a connection must survive before its drop resets the backoff.
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

// Event is one parsed SSE frame: the `event:` name plus the JSON `data:` payload.
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

type UpdateKind int

const (
	StateUpdate UpdateKind = iota
	EventUpdate
)

// Update is either a connection-state change or an arrived event. Gen echoes the session
// generation, so the shell can drop updates from a superseded session.
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

// Consumer streams SSE frames on a background goroutine. Single-use: New, Start, then Stop.
type Consumer struct {
	server string
	client *http.Client
	gen    int

	// timing knobs, defaulted in New and overridable in tests for speed
	headerTimeout  time.Duration
	readTimeout    time.Duration
	healthyFor     time.Duration
	backoffFloor   time.Duration
	backoffCeiling time.Duration

	ch     chan Update
	ctx    context.Context
	cancel context.CancelFunc
}

// New builds a Consumer for server's base URL. client's transport must attach the bearer token.
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

// Updates is the read side of the channel, closed when the Consumer's goroutine exits.
func (c *Consumer) Updates() <-chan Update { return c.ch }

// Start launches the connect/read/reconnect loop. Call once.
func (c *Consumer) Start() {
	c.ctx, c.cancel = context.WithCancel(context.Background())
	go c.run()
}

// Stop cancels the loop and closes the channel. A never-started Consumer is a no-op.
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

// stream reads one connection until it drops, reporting whether it lived long enough
// to reset the backoff.
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

	// bound the header-arrival phase, disarming the moment Do returns so the deadline never
	// applies to the streaming body read (the read watchdog guards that).
	headerGuard := time.AfterFunc(c.headerTimeout, cancel)
	resp, err := c.client.Do(req)
	headerGuard.Stop()
	if err != nil {
		slog.Debug("realtime: connect", "err", err)
		return false
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		// A 401 means the auth transport already failed to refresh. Every code backs off and retries.
		slog.Debug("realtime: non-200", "status", resp.StatusCode)
		return false
	}

	c.emit(Update{Kind: StateUpdate, State: Connected, Gen: c.gen})
	return c.read(reqCtx, cancel, resp.Body)
}

// read parses the SSE body until it ends. The watchdog cancels the request if no byte
// arrives within readTimeout, unblocking the scanner so the caller reconnects.
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
			// a comment line (":connected", ":keep-alive") — the watchdog reset above
			// already counted it as liveness
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

// emit drops the update if the Consumer is stopping, so a stalled reader cannot wedge
// the goroutine.
func (c *Consumer) emit(u Update) {
	select {
	case c.ch <- u:
	case <-c.ctx.Done():
	}
}

// splitField strips a single leading space from the value, per the SSE spec.
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

// parseEvent fails (ok=false) on data that is not the expected JSON, which the caller skips.
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
