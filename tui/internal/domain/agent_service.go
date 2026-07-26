package domain

import (
	"context"
	"fmt"
	"net/http"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// Agents are owner-scoped (the authenticated caller owns them). Tokens hang off an agent.
type AgentService struct {
	api *client.ClientWithResponses
}

func NewAgentService(api *client.ClientWithResponses) *AgentService {
	return &AgentService{api: api}
}

// ListAgents returns the caller's active agents.
func (s *AgentService) ListAgents(ctx context.Context) ([]Agent, error) {
	resp, err := s.api.ListMyAgentsWithResponse(ctx)
	if err != nil {
		return nil, fmt.Errorf("list agents: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, &APIError{Status: resp.StatusCode()}
	}
	out := make([]Agent, 0, len(*resp.JSON200))
	for _, a := range *resp.JSON200 {
		out = append(out, toAgent(&a))
	}
	return out, nil
}

// CreateAgent creates an agent and returns it. name must be letters/spaces only. declaredModel is
// optional. A 409 (duplicate name) or 403 (owner must be human) surfaces as an *APIError.
func (s *AgentService) CreateAgent(ctx context.Context, name, declaredModel string) (Agent, error) {
	body := client.CreateAgentRequest{Name: name}
	if declaredModel != "" {
		body.DeclaredModel = &declaredModel
	}
	resp, err := s.api.CreateAgentWithResponse(ctx, body)
	if err != nil {
		return Agent{}, fmt.Errorf("create agent: %w", err)
	}
	if resp.JSON201 == nil {
		return Agent{}, &APIError{Status: resp.StatusCode()}
	}
	return toAgent(resp.JSON201), nil
}

// DeactivateAgent soft-deletes an agent. Its tokens stop working immediately.
func (s *AgentService) DeactivateAgent(ctx context.Context, agentID int64) error {
	resp, err := s.api.DeactivateAgentWithResponse(ctx, agentID)
	if err != nil {
		return fmt.Errorf("deactivate agent: %w", err)
	}
	if resp.StatusCode() != http.StatusNoContent {
		return &APIError{Status: resp.StatusCode()}
	}
	return nil
}

// ListTokens returns an agent's tokens, including revoked and expired ones.
func (s *AgentService) ListTokens(ctx context.Context, agentID int64) ([]Token, error) {
	resp, err := s.api.ListAgentTokensWithResponse(ctx, agentID)
	if err != nil {
		return nil, fmt.Errorf("list tokens: %w", err)
	}
	if resp.JSON200 == nil {
		return nil, &APIError{Status: resp.StatusCode()}
	}
	out := make([]Token, 0, len(*resp.JSON200))
	for _, t := range *resp.JSON200 {
		out = append(out, toToken(&t))
	}
	return out, nil
}

// IssueToken issues a new token for an agent and returns its raw secret exactly once. ttlDays <= 0
// means no expiry. scope must be ScopeReadOnly or ScopeReadWrite.
func (s *AgentService) IssueToken(ctx context.Context, agentID int64, name, scope string, ttlDays int) (IssuedToken, error) {
	body := client.CreatePatRequest{Name: name, Scope: client.CreatePatRequestScope(scope)}
	if ttlDays > 0 {
		d := int32(ttlDays)
		body.TtlDays = &d
	}
	resp, err := s.api.IssueAgentTokenWithResponse(ctx, agentID, body)
	if err != nil {
		return IssuedToken{}, fmt.Errorf("issue token: %w", err)
	}
	if resp.JSON201 == nil || resp.JSON201.Token == nil {
		return IssuedToken{}, &APIError{Status: resp.StatusCode()}
	}
	return IssuedToken{Secret: *resp.JSON201.Token, Token: toToken(resp.JSON201.Pat)}, nil
}

// RevokeToken revokes one of an agent's tokens (idempotent server-side).
func (s *AgentService) RevokeToken(ctx context.Context, agentID, tokenID int64) error {
	resp, err := s.api.RevokeAgentTokenWithResponse(ctx, agentID, tokenID)
	if err != nil {
		return fmt.Errorf("revoke token: %w", err)
	}
	if resp.StatusCode() != http.StatusNoContent {
		return &APIError{Status: resp.StatusCode()}
	}
	return nil
}

func toAgent(a *client.AgentResponse) Agent {
	if a == nil {
		return Agent{}
	}
	return Agent{
		ID:            derefInt64to64(a.Id),
		Name:          deref(a.Name),
		Username:      deref(a.Username),
		DeclaredModel: deref(a.DeclaredModel),
		CreatedAt:     derefTime(a.CreatedAt),
	}
}

func toToken(t *client.PatResponse) Token {
	if t == nil {
		return Token{}
	}
	scope := ""
	if t.Scope != nil {
		scope = string(*t.Scope)
	}
	return Token{
		ID:         derefInt64to64(t.Id),
		Name:       deref(t.Name),
		Scope:      scope,
		Revoked:    derefBool(t.Revoked),
		CreatedAt:  derefTime(t.CreatedAt),
		ExpiresAt:  derefTime(t.ExpiresAt),
		LastUsedAt: derefTime(t.LastUsedAt),
	}
}

// Agent/token ids are int64 (member ids).
func derefInt64to64(p *int64) int64 {
	if p == nil {
		return 0
	}
	return *p
}
