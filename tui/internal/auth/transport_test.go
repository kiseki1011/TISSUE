package auth

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func TestTransport_RefreshesOn401(t *testing.T) {
	var seen []string
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		seen = append(seen, r.Header.Get("Authorization"))
		if r.Header.Get("Authorization") == "Bearer stale" {
			w.WriteHeader(http.StatusUnauthorized)
			return
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer srv.Close()

	var persisted domain.TokenPair
	refresh := func(_ context.Context, refreshToken string) (domain.TokenPair, error) {
		return domain.TokenPair{Access: "fresh", Refresh: refreshToken}, nil
	}
	tr := NewTransport(nil, refresh, func(tokens domain.TokenPair) { persisted = tokens })
	tr.SetTokens(domain.TokenPair{Access: "stale", Refresh: "r1"})

	resp, err := (&http.Client{Transport: tr}).Get(srv.URL)
	if err != nil {
		t.Fatalf("get: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		t.Fatalf("status = %d, want 200", resp.StatusCode)
	}
	if len(seen) != 2 || seen[0] != "Bearer stale" || seen[1] != "Bearer fresh" {
		t.Fatalf("bearer tokens seen = %v, want [stale fresh]", seen)
	}
	if persisted.Access != "fresh" {
		t.Fatalf("persisted = %+v, want access=fresh", persisted)
	}
}

func TestTransport_RefreshFailureReturns401(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusUnauthorized)
	}))
	defer srv.Close()

	refresh := func(context.Context, string) (domain.TokenPair, error) {
		return domain.TokenPair{}, errors.New("refresh rejected")
	}
	tr := NewTransport(nil, refresh, nil)
	tr.SetTokens(domain.TokenPair{Access: "stale", Refresh: "r1"})

	resp, err := (&http.Client{Transport: tr}).Get(srv.URL)
	if err != nil {
		t.Fatalf("get: %v", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusUnauthorized {
		t.Fatalf("status = %d, want 401", resp.StatusCode)
	}
}
