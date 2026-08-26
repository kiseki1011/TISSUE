package domain

import (
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

func ptr[T any](v T) *T { return &v }

func TestTokenIsUsable(t *testing.T) {
	now := time.Date(2026, 8, 2, 12, 0, 0, 0, time.UTC)
	cases := []struct {
		name string
		tok  Token
		want bool
	}{
		{"active no expiry", Token{}, true},
		{"active future expiry", Token{ExpiresAt: now.Add(time.Hour)}, true},
		{"revoked", Token{Revoked: true}, false},
		{"expired", Token{ExpiresAt: now.Add(-time.Hour)}, false},
		{"revoked and future expiry", Token{Revoked: true, ExpiresAt: now.Add(time.Hour)}, false},
	}
	for _, c := range cases {
		if got := c.tok.IsUsable(now); got != c.want {
			t.Errorf("%s: IsUsable = %v, want %v", c.name, got, c.want)
		}
	}
}

// toAgent must be nil-safe on every optional field.
func TestToAgentMapping(t *testing.T) {
	created := time.Date(2026, 8, 1, 9, 0, 0, 0, time.UTC)
	agentType := client.AgentResponseAgentTypeDEVELOPMENT
	modelColor := client.AiModelSummaryColor("ANSI_BLUE")
	got := toAgent(&client.AgentResponse{
		Id: ptr(int64(7)), Name: ptr("Build Bot"), Username: ptr("agent-alice-build-bot"),
		AgentType:   &agentType,
		Model:       &client.AiModelSummary{Id: ptr(int64(3)), Name: ptr("claude-opus-4-8"), Color: &modelColor},
		Description: ptr("Reviews PRs"),
		CreatedAt:   ptr(created),
	})
	want := Agent{
		ID: 7, Name: "Build Bot", Username: "agent-alice-build-bot",
		AgentType: "DEVELOPMENT", ModelID: 3, ModelName: "claude-opus-4-8", ModelColor: "ANSI_BLUE",
		Description: "Reviews PRs", CreatedAt: created,
	}
	if got != want {
		t.Errorf("toAgent = %+v, want %+v", got, want)
	}
	// all-nil response must not panic
	if z := toAgent(&client.AgentResponse{}); z != (Agent{}) {
		t.Errorf("toAgent(empty) = %+v, want zero", z)
	}
}

// toToken must be nil-safe on timestamps and the revoked flag.
func TestToTokenMapping(t *testing.T) {
	rw := client.PatResponseScopeREADWRITE
	created := time.Date(2026, 7, 30, 8, 0, 0, 0, time.UTC)
	got := toToken(&client.PatResponse{
		Id: ptr(int64(11)), Name: ptr("ci"), Scope: &rw, Revoked: ptr(true), CreatedAt: ptr(created),
	})
	if got.ID != 11 || got.Name != "ci" || got.Scope != ScopeReadWrite || !got.Revoked || !got.CreatedAt.Equal(created) {
		t.Errorf("toToken = %+v", got)
	}
	if !got.ExpiresAt.IsZero() || !got.LastUsedAt.IsZero() {
		t.Errorf("nil timestamps should map to zero time, got expires=%v lastUsed=%v", got.ExpiresAt, got.LastUsedAt)
	}
	if z := toToken(&client.PatResponse{}); z.Scope != "" || z.ID != 0 {
		t.Errorf("toToken(empty) = %+v, want zero", z)
	}
}
