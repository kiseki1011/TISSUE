// Package errmsg turns an API failure into a user-facing toast line. The server's own curated message
// (domain.ErrorReason) is shown by default. Only implementation-flavored codes are overridden.
package errmsg

import "github.com/kiseki1011/TISSUE/tui/internal/domain"

const connectivity = "Couldn't reach the server - check your connection and try again."

// codeMessages maps a backend error code (ProblemDetail "title") to a self-contained message. Keep it
// SMALL: only codes whose server detail is leaky. Everything else falls back to that detail.
var codeMessages = map[string]string{
	// These read as implementation-speak. A non-member hits PROJECT_MEMBER_NOT_FOUND on any write.
	"PROJECT_MEMBER_NOT_FOUND": "You're not a member of this project.",
	"PROJECT_MANAGER_REQUIRED": "You need the Manager role for that.",
	"PROJECT_JOIN_NOT_ALLOWED": "This project is private - ask a manager to add you.",
	// The raw details for these mean nothing to an end user.
	"OPTIMISTIC_LOCK_FAILED":   "This changed since you opened it — reopen and try again.",
	"DATA_INTEGRITY_VIOLATION": "That conflicts with existing data.",
	"UNEXPECTED_ERROR":         "Something went wrong on the server. Try again.",
}

// OverrideParts returns the connectivity line for a transport error (status 0) or the curated line for
// a mapped code. ok is false when the caller should use its own copy and/or the server detail.
func OverrideParts(status int, code string) (string, bool) {
	if status == 0 {
		return connectivity, true
	}
	if m, ok := codeMessages[code]; ok {
		return m, true
	}
	return "", false
}

// Override is OverrideParts for callers that still hold the error value.
func Override(err error) (string, bool) {
	if err == nil {
		return "", false
	}
	return OverrideParts(domain.ErrorStatus(err), domain.ErrorCode(err))
}

// Message resolves the toast line for a failed action. base names the action ("Could not create the
// issue."). Precedence: transport → connectivity, mapped code → its line, server detail → "base detail".
func Message(err error, base string) string {
	if err == nil {
		return base
	}
	if m, ok := Override(err); ok {
		return m
	}
	if r := domain.ErrorReason(err); r != "" {
		return base + " " + r
	}
	return base
}
