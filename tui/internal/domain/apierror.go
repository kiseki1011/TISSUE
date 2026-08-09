package domain

import (
	"encoding/json"
	"errors"
	"fmt"
	"sort"
	"strings"
)

// APIError is a non-success HTTP response. Status tells rate limiting (429) apart from server errors;
// Code/Detail/Fields carry the server's RFC-7807 ProblemDetail so the UI can show the real reason
// instead of a generic "something went wrong".
type APIError struct {
	Status int
	Code   string            // ProblemDetail "title": a stable machine error code, e.g. "PROJECT_ARCHIVED"
	Detail string            // ProblemDetail "detail": a curated, user-safe human message
	Fields map[string]string // validation "errors" map (field -> message), present on 400s
}

func (e *APIError) Error() string {
	if e.Code != "" {
		return fmt.Sprintf("%s (status %d)", e.Code, e.Status)
	}
	return fmt.Sprintf("server returned status %d", e.Status)
}

// Reason is the user-facing explanation the server sent, or "" when the body carried nothing usable
// (a transport failure, an empty body, or a non-APIError).
func (e *APIError) Reason() string {
	// A validation error carries per-field messages; its detail is generic boilerplate ("Validation
	// failed for one or more fields"), which would just restate the action failure. Drop it and show
	// only the field messages - the actual information - with the DTO field name humanized so the toast
	// reads "Title: size must be between 2 and 50" rather than "…failed… — title: …".
	if len(e.Fields) > 0 {
		keys := make([]string, 0, len(e.Fields))
		for k := range e.Fields {
			keys = append(keys, k)
		}
		sort.Strings(keys) // a map has no order; sort so the message is stable
		parts := make([]string, 0, len(keys))
		for _, k := range keys {
			parts = append(parts, humanizeField(k)+": "+e.Fields[k])
		}
		return strings.Join(parts, "; ")
	}
	return strings.TrimSpace(e.Detail)
}

// humanizeField turns a camelCase DTO field name (title, issueTypeId, dueAt) into a readable label
// ("Title", "Issue type id", "Due at") for validation messages. ASCII DTO names only.
func humanizeField(name string) string {
	var b strings.Builder
	for i := 0; i < len(name); i++ {
		c := name[i]
		switch {
		case i == 0 && c >= 'a' && c <= 'z':
			b.WriteByte(c - 32) // capitalize the first letter
		case i > 0 && c >= 'A' && c <= 'Z':
			b.WriteByte(' ')
			b.WriteByte(c + 32) // start a new lowercase word at each interior capital
		default:
			b.WriteByte(c)
		}
	}
	return b.String()
}

// apiError returns nil for a 2xx status, else an *APIError carrying the parsed ProblemDetail body. Pass
// the response's StatusCode() and Body: apiError(resp.StatusCode(), resp.Body).
func apiError(status int, body []byte) error {
	if status >= 200 && status < 300 {
		return nil
	}
	return newAPIError(status, body)
}

// newAPIError builds an *APIError, best-effort parsing a ProblemDetail error body when one is present.
func newAPIError(status int, body []byte) *APIError {
	e := &APIError{Status: status}
	if len(body) == 0 {
		return e
	}
	var pd struct {
		Title  string            `json:"title"`
		Detail string            `json:"detail"`
		Errors map[string]string `json:"errors"`
	}
	if json.Unmarshal(body, &pd) == nil {
		e.Code, e.Detail, e.Fields = pd.Title, pd.Detail, pd.Errors
	}
	return e
}

// ErrorStatus unwraps a (possibly wrapped) *APIError and returns its HTTP status; a transport error
// stays 0.
func ErrorStatus(err error) int {
	var e *APIError
	if errors.As(err, &e) {
		return e.Status
	}
	return 0
}

// ErrorReason unwraps a (possibly wrapped) *APIError and returns the server's explanation, or "" when
// there is none (transport error, empty body, or a non-APIError).
func ErrorReason(err error) string {
	var e *APIError
	if errors.As(err, &e) {
		return e.Reason()
	}
	return ""
}

// ErrorCode unwraps a (possibly wrapped) *APIError and returns its machine error code (ProblemDetail
// "title"), or "" when there is none.
func ErrorCode(err error) string {
	var e *APIError
	if errors.As(err, &e) {
		return e.Code
	}
	return ""
}
