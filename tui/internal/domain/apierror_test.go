package domain

import (
	"fmt"
	"testing"
)

// apiError returns nil for any 2xx and a populated *APIError otherwise, parsing the ProblemDetail body.
func TestApiErrorParsesProblemDetail(t *testing.T) {
	if err := apiError(200, []byte(`{"title":"OK"}`)); err != nil {
		t.Errorf("a 2xx status should not be an error, got %v", err)
	}
	body := []byte(`{"type":"about:blank","title":"PROJECT_ARCHIVED","status":400,"detail":"The project is archived"}`)
	err := apiError(400, body)
	if err == nil {
		t.Fatal("a 400 should be an error")
	}
	if got := ErrorStatus(err); got != 400 {
		t.Errorf("status = %d, want 400", got)
	}
	if got := ErrorCode(err); got != "PROJECT_ARCHIVED" {
		t.Errorf("code = %q, want PROJECT_ARCHIVED", got)
	}
	if got := ErrorReason(err); got != "The project is archived" {
		t.Errorf("reason = %q, want the detail line", got)
	}
}

// A body-less or unparseable failure still yields an *APIError carrying the status, with an empty reason.
func TestApiErrorWithoutUsableBody(t *testing.T) {
	err := apiError(503, nil)
	if err == nil {
		t.Fatal("a 503 should be an error")
	}
	if got := ErrorStatus(err); got != 503 {
		t.Errorf("status = %d, want 503", got)
	}
	if got := ErrorReason(err); got != "" {
		t.Errorf("a body-less failure should have no reason, got %q", got)
	}
	// unparseable body: still an APIError, still no reason (best-effort parse fails silently)
	if got := ErrorReason(apiError(500, []byte("<html>oops</html>"))); got != "" {
		t.Errorf("an unparseable body should yield no reason, got %q", got)
	}
}

// A validation error drops the generic boilerplate detail and shows only the per-field messages, sorted
// for stability, with camelCase field names humanized.
func TestReasonJoinsValidationFields(t *testing.T) {
	body := []byte(`{"title":"VALIDATION_FAILED","status":400,"detail":"Validation failed for one or more fields","errors":{"issueTypeId":"must not be null","title":"size must be between 2 and 50"}}`)
	got := ErrorReason(apiError(400, body))
	want := "Issue type id: must not be null; Title: size must be between 2 and 50"
	if got != want {
		t.Errorf("reason = %q, want %q", got, want)
	}
}

// ErrorStatus/ErrorReason/ErrorCode unwrap a wrapped APIError (errors.As), and no-op on a plain error.
func TestErrorHelpersUnwrapAndDegrade(t *testing.T) {
	inner := apiError(404, []byte(`{"title":"ISSUE_NOT_FOUND","detail":"Issue not found"}`))
	wrapped := fmt.Errorf("create issue: %w", inner)
	if got := ErrorStatus(wrapped); got != 404 {
		t.Errorf("wrapped status = %d, want 404", got)
	}
	if got := ErrorReason(wrapped); got != "Issue not found" {
		t.Errorf("wrapped reason = %q, want the detail", got)
	}
	plain := fmt.Errorf("dial tcp: connection refused")
	if got := ErrorStatus(plain); got != 0 {
		t.Errorf("a transport error has status 0, got %d", got)
	}
	if got := ErrorReason(plain); got != "" {
		t.Errorf("a transport error has no reason, got %q", got)
	}
}
