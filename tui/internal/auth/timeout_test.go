package auth

import (
	"context"
	"io"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
	"time"
)

func TestTimeoutTransport_TimesOutHungRequest(t *testing.T) {
	release := make(chan struct{})
	srv := httptest.NewServer(http.HandlerFunc(func(_ http.ResponseWriter, _ *http.Request) {
		<-release // never respond until the test lets it
	}))
	defer srv.Close()
	defer close(release)

	tr := NewTimeoutTransport(nil, 50*time.Millisecond)

	start := time.Now()
	resp, err := (&http.Client{Transport: tr}).Get(srv.URL)
	elapsed := time.Since(start)

	if err == nil {
		resp.Body.Close()
		t.Fatal("expected a timeout error, got nil")
	}
	if elapsed > time.Second {
		t.Fatalf("request took %v, expected it to time out near 50ms", elapsed)
	}
}

func TestTimeoutTransport_NormalResponseReadsFully(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("hello world"))
	}))
	defer srv.Close()

	tr := NewTimeoutTransport(nil, time.Second)

	resp, err := (&http.Client{Transport: tr}).Get(srv.URL)
	if err != nil {
		t.Fatalf("get: %v", err)
	}
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Fatalf("read body: %v", err)
	}
	if err := resp.Body.Close(); err != nil {
		t.Fatalf("close: %v", err)
	}
	if string(body) != "hello world" {
		t.Fatalf("body = %q, want %q", body, "hello world")
	}
}

// A slow-but-progressing body must not be cut so long as it finishes within the window: the
// deadline is tied to Close, not to RoundTrip returning.
func TestTimeoutTransport_SlowBodyWithinWindowIsNotCut(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		flusher, _ := w.(http.Flusher)
		w.WriteHeader(http.StatusOK)
		for i := 0; i < 3; i++ {
			_, _ = w.Write([]byte("chunk"))
			if flusher != nil {
				flusher.Flush()
			}
			time.Sleep(20 * time.Millisecond)
		}
	}))
	defer srv.Close()

	tr := NewTimeoutTransport(nil, time.Second)

	resp, err := (&http.Client{Transport: tr}).Get(srv.URL)
	if err != nil {
		t.Fatalf("get: %v", err)
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Fatalf("read body: %v", err)
	}
	if string(body) != "chunkchunkchunk" {
		t.Fatalf("body = %q, want three chunks", body)
	}
}

func TestTimeoutTransport_RespectsExistingDeadline(t *testing.T) {
	var wrappedCtxHadDeadline atomic.Bool
	spy := roundTripFunc(func(req *http.Request) (*http.Response, error) {
		_, ok := req.Context().Deadline()
		wrappedCtxHadDeadline.Store(ok)
		return &http.Response{StatusCode: http.StatusOK, Body: http.NoBody}, nil
	})
	tr := NewTimeoutTransport(spy, time.Hour)

	// caller supplies its own (shorter) deadline
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Millisecond)
	defer cancel()
	req, _ := http.NewRequestWithContext(ctx, http.MethodGet, "http://example.test", nil)

	resp, err := tr.RoundTrip(req)
	if err != nil {
		t.Fatalf("roundtrip: %v", err)
	}
	_ = resp.Body.Close()

	if !wrappedCtxHadDeadline.Load() {
		t.Fatal("existing caller deadline should have been passed through unchanged")
	}
	// the passed-through body is the bare one, not wrapped (nothing to cancel)
	if _, wrapped := resp.Body.(*cancelBody); wrapped {
		t.Fatal("body should not be wrapped when a deadline already exists")
	}
}

type roundTripFunc func(*http.Request) (*http.Response, error)

func (f roundTripFunc) RoundTrip(req *http.Request) (*http.Response, error) { return f(req) }
