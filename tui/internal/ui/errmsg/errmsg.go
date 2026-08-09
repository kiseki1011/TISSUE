// Package errmsg turns an API failure into a user-facing toast line. It sits on top of the domain's
// ProblemDetail parsing (domain.APIError): most failures already carry a curated, user-safe server
// message (domain.ErrorReason), which is shown by default. This package adds a SMALL override for the
// handful of error codes whose server message is implementation-flavored (e.g. "Project member not
// found") or where a canonical, self-contained line reads better than the raw detail.
package errmsg

import "github.com/kiseki1011/TISSUE/tui/internal/domain"

const connectivity = "Couldn't reach the server - check your connection and try again."

// codeMessages maps a backend error code (ProblemDetail "title") to a self-contained user message.
// Keep this SMALL and deliberate: only codes whose server detail is leaky, or clearly worse than a
// curated line. Anything not listed falls back to the server's own detail, which is already user-safe.
var codeMessages = map[string]string{
	// membership / permission — "Project member not found" / "Requires project manager role" read as
	// implementation-speak; a non-member hits PROJECT_MEMBER_NOT_FOUND on any write in a project they
	// have not joined.
	"PROJECT_MEMBER_NOT_FOUND": "You're not a member of this project.",
	"PROJECT_MANAGER_REQUIRED": "You need the Manager role for that.",
	"PROJECT_JOIN_NOT_ALLOWED": "This project is private - ask a manager to add you.",
	// concurrency / infrastructure — the raw details mean nothing to an end user.
	"OPTIMISTIC_LOCK_FAILED":   "This changed since you opened it — reopen and try again.",
	"DATA_INTEGRITY_VIOLATION": "That conflicts with existing data.",
	"UNEXPECTED_ERROR":         "Something went wrong on the server. Try again.",
}

// OverrideParts returns a mapped message for a failure described by its HTTP status and error code: a
// connectivity line for a transport error (status 0), or the curated line for a mapped code. ok is false
// when nothing applies and the caller should use its own curated copy and/or the server detail.
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

// Message resolves the full toast line for a failed action. base is a self-contained fallback that names
// the action ("Could not create the issue."). Precedence:
//
//	transport error → connectivity line
//	mapped code     → its canonical message (self-contained)
//	server detail   → "<base> <detail>"   (base gives the action context; detail the reason)
//	nothing         → base
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
