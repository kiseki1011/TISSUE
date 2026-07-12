package auth

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"sync"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// RefreshFunc exchanges a refresh token for a new pair. It must use an unauthenticated client so
// a failed refresh does not recurse back through Transport.
type RefreshFunc func(ctx context.Context, refreshToken string) (domain.TokenPair, error)

// Transport adds the bearer token to each request and on a 401, refreshes once and retries.
// It runs outside the BubbleTea loop, so it guards its tokens with a mutex.
type Transport struct {
	base    http.RoundTripper
	refresh RefreshFunc
	persist func(domain.TokenPair)

	mu     sync.Mutex
	tokens domain.TokenPair
}

// NewTransport wraps base (nil uses http.DefaultTransport).
// `persist` when set, is called with refreshed tokens so they can be saved.
func NewTransport(base http.RoundTripper, refresh RefreshFunc, persist func(domain.TokenPair)) *Transport {
	if base == nil {
		base = http.DefaultTransport
	}
	return &Transport{base: base, refresh: refresh, persist: persist}
}

// SetTokens replaces the current tokens after a login or restore.
func (t *Transport) SetTokens(tokens domain.TokenPair) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.tokens = tokens
}

// Clear drops the current tokens after logout.
func (t *Transport) Clear() {
	t.SetTokens(domain.TokenPair{})
}

func (t *Transport) RoundTrip(req *http.Request) (*http.Response, error) {
	access := t.accessToken()
	resp, err := t.send(req, access)
	if err != nil || resp.StatusCode != http.StatusUnauthorized {
		return resp, err
	}

	fresh, err := t.tryRefresh(req.Context(), access)
	if err != nil {
		slog.Debug("token refresh failed", "err", err)
		return resp, nil // hand 401 back so caller re-authenticates
	}
	_ = resp.Body.Close()
	return t.send(req, fresh)
}

func (t *Transport) accessToken() string {
	t.mu.Lock()
	defer t.mu.Unlock()
	return t.tokens.Access
}

// send clones the request (RoundTripper must not mutate the original) and attaches the bearer token,
// restoring the body so the request can be retried.
func (t *Transport) send(req *http.Request, access string) (*http.Response, error) {
	clone := req.Clone(req.Context())
	if req.Body != nil && req.GetBody != nil {
		body, err := req.GetBody()
		if err != nil {
			return nil, err
		}
		clone.Body = body
	}
	if access != "" {
		clone.Header.Set("Authorization", "Bearer "+access)
	}
	return t.base.RoundTrip(clone)
}

func (t *Transport) tryRefresh(ctx context.Context, staleAccess string) (string, error) {
	t.mu.Lock()
	defer t.mu.Unlock()

	// another request may have refreshed while this one waited for the lock
	if t.tokens.Access != "" && t.tokens.Access != staleAccess {
		return t.tokens.Access, nil
	}
	if t.tokens.Refresh == "" {
		return "", errors.New("no refresh token")
	}

	tokens, err := t.refresh(ctx, t.tokens.Refresh)
	if err != nil {
		return "", err
	}
	t.tokens = tokens
	if t.persist != nil {
		t.persist(tokens)
	}
	return tokens.Access, nil
}
