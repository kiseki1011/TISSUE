package domain

import (
	"io"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/pkg/client"
)

// stubAPI wires a generated client to a stub server, returning the client plus pointers to the last
// request and body it received, so a test can assert on the wire shape a service produces.
func stubAPI(t *testing.T, handler http.HandlerFunc) (*client.ClientWithResponses, *http.Request, *[]byte) {
	t.Helper()
	var gotReq http.Request
	var gotBody []byte
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		gotReq = *r
		gotBody, _ = io.ReadAll(r.Body)
		handler(w, r)
	}))
	t.Cleanup(srv.Close)

	api, err := client.NewClientWithResponses(srv.URL)
	if err != nil {
		t.Fatalf("building the client: %v", err)
	}
	return api, &gotReq, &gotBody
}

func sprintServiceOn(t *testing.T, handler http.HandlerFunc) (*SprintService, *http.Request, *[]byte) {
	t.Helper()
	api, req, body := stubAPI(t, handler)
	return NewSprintService(api), req, body
}

func issueServiceOn(t *testing.T, handler http.HandlerFunc) (*IssueService, *http.Request, *[]byte) {
	t.Helper()
	api, req, body := stubAPI(t, handler)
	return NewIssueService(api), req, body
}
