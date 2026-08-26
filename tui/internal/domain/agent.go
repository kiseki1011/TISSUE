package domain

import "time"

// Mirrors the backend PatScope enum.
const (
	ScopeReadOnly  = "READ_ONLY"
	ScopeReadWrite = "READ_WRITE"
)

// AgentTypes mirrors the backend AgentType enum.
var AgentTypes = []string{"DEVELOPMENT", "PLANNING", "MANAGEMENT", "DESIGN", "QA", "GENERAL"}

// Agent is an AI/automation account owned by the caller (Member with memberType=AGENT).
// It inherits the owner's project memberships. There is no per-project scoping.
type Agent struct {
	ID          int64
	Name        string // letters and spaces only, unique among the owner's agents
	Username    string // server-generated global handle, like agent-alice-build
	AgentType   string // one of AgentTypes. "" when unset
	ModelID     int64  // catalog model id, 0 when none
	ModelName   string // "" when none
	ModelColor  string // ColorType enum name. "" when none
	Description string // "" when none
	CreatedAt   time.Time
}

// AiModel is a globally admin-managed catalog entry.
type AiModel struct {
	ID          int64
	Name        string
	Description string
	Color       string // ColorType name
}

// Token is a PAT issued to an agent. The raw secret is never returned after creation.
type Token struct {
	ID         int64
	Name       string
	Scope      string // ScopeReadOnly | ScopeReadWrite
	Revoked    bool
	CreatedAt  time.Time
	ExpiresAt  time.Time // zero = never expires
	LastUsedAt time.Time // zero = never used
}

func (t Token) IsUsable(now time.Time) bool {
	if t.Revoked {
		return false
	}
	if !t.ExpiresAt.IsZero() && !now.Before(t.ExpiresAt) {
		return false
	}
	return true
}

// IssuedToken carries the raw secret, available exactly once at creation.
type IssuedToken struct {
	Secret string
	Token  Token
}
