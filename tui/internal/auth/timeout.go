package auth

import (
	"context"
	"io"
	"net/http"
	"time"
)

type TimeoutTransport struct {
	base    http.RoundTripper
	timeout time.Duration
}

// NewTimeoutTransport wraps base (nil uses http.DefaultTransport).
func NewTimeoutTransport(base http.RoundTripper, timeout time.Duration) *TimeoutTransport {
	if base == nil {
		base = http.DefaultTransport
	}
	return &TimeoutTransport{base: base, timeout: timeout}
}

func (t *TimeoutTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	if _, hasDeadline := req.Context().Deadline(); hasDeadline {
		return t.base.RoundTrip(req) // caller already bounded this request
	}

	ctx, cancel := context.WithTimeout(req.Context(), t.timeout)
	resp, err := t.base.RoundTrip(req.WithContext(ctx))
	if err != nil {
		cancel()
		return nil, err
	}
	if resp.Body == nil {
		resp.Body = http.NoBody
	}
	resp.Body = &cancelBody{ReadCloser: resp.Body, cancel: cancel}
	return resp, nil
}

// cancelBody releases the request context once the body is closed, keeping the deadline alive for
// the duration of the read rather than firing when RoundTrip returns.
type cancelBody struct {
	io.ReadCloser
	cancel context.CancelFunc
}

func (b *cancelBody) Close() error {
	err := b.ReadCloser.Close()
	b.cancel()
	return err
}
