package errmsg

import (
	"errors"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// A leaky/implementation-flavored code is replaced by its canonical, self-contained line, ignoring base.
func TestMessageMapsLeakyCode(t *testing.T) {
	err := &domain.APIError{Status: 404, Code: "PROJECT_MEMBER_NOT_FOUND", Detail: "Project member not found"}
	got := Message(err, "Could not create the issue.")
	if got != "You're not a member of this project." {
		t.Errorf("a leaky code should map to friendlier copy, got %q", got)
	}
}

// An unmapped code with a good server detail falls back to "<base> <detail>" - action context + reason.
func TestMessagePrefixesServerDetail(t *testing.T) {
	err := &domain.APIError{Status: 409, Code: "ISSUE_ALREADY_ASSIGNED", Detail: "The issue is already assigned to another member"}
	got := Message(err, "Could not change the assignee.")
	want := "Could not change the assignee. The issue is already assigned to another member"
	if got != want {
		t.Errorf("Message = %q, want %q", got, want)
	}
}

// A transport error (no status) reads as connectivity, whatever the base.
func TestMessageTransportIsConnectivity(t *testing.T) {
	got := Message(errors.New("dial tcp: connection refused"), "Could not save the issue.")
	if got != connectivity {
		t.Errorf("a transport error should read as connectivity, got %q", got)
	}
}

// With no code and no detail (e.g. an empty error body), Message falls back to the base line.
func TestMessageFallsBackToBase(t *testing.T) {
	err := &domain.APIError{Status: 500}
	if got := Message(err, "Could not save the issue."); got != "Could not save the issue." {
		t.Errorf("a body-less failure should fall back to base, got %q", got)
	}
}

// Override exposes only the top tier (connectivity + mapped code) for callers that keep their own copy.
func TestOverrideParts(t *testing.T) {
	if m, ok := OverrideParts(0, "ANYTHING"); !ok || m != connectivity {
		t.Errorf("status 0 should override to connectivity, got %q ok=%v", m, ok)
	}
	if m, ok := OverrideParts(403, "PROJECT_MANAGER_REQUIRED"); !ok || m != "You need the Manager role for that." {
		t.Errorf("a mapped code should override, got %q ok=%v", m, ok)
	}
	if _, ok := OverrideParts(404, "ISSUE_NOT_FOUND"); ok {
		t.Error("an unmapped code should not override (caller uses its own copy / the server detail)")
	}
	// a PRIVATE project's self-join rejection maps to a member-facing line, not "not allowed"
	if m, ok := OverrideParts(403, "PROJECT_JOIN_NOT_ALLOWED"); !ok || m != "This project is private - ask a manager to add you." {
		t.Errorf("a private-join rejection should map, got %q ok=%v", m, ok)
	}
}
