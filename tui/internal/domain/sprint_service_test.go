package domain

import (
	"context"
	"encoding/json"
	"net/http"
	"testing"
)

func TestCreateSprintSendsTitleAndGoal(t *testing.T) {
	svc, req, body := sprintServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"projectKey":"PROJ","sprintId":42}`))
	})

	id, err := svc.CreateSprint(context.Background(), "PROJ", "Sprint 7", "ship the importer")
	if err != nil {
		t.Fatalf("CreateSprint: %v", err)
	}
	if id != 42 {
		t.Errorf("id = %d, want the created sprint 42", id)
	}
	if req.Method != http.MethodPost {
		t.Errorf("method = %s, want POST", req.Method)
	}

	var sent map[string]any
	if err := json.Unmarshal(*body, &sent); err != nil {
		t.Fatalf("decoding the request body %q: %v", *body, err)
	}
	if sent["title"] != "Sprint 7" || sent["goal"] != "ship the importer" {
		t.Errorf("body = %v, want the title and goal", sent)
	}
}

// Goal is optional. Sending "" would set an empty goal rather than leaving it unset, so the field is
// omitted entirely when the user left it blank.
func TestCreateSprintOmitsAnEmptyGoal(t *testing.T) {
	svc, _, body := sprintServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusCreated)
		_, _ = w.Write([]byte(`{"sprintId":1}`))
	})

	if _, err := svc.CreateSprint(context.Background(), "PROJ", "Sprint 7", ""); err != nil {
		t.Fatalf("CreateSprint: %v", err)
	}
	var sent map[string]any
	if err := json.Unmarshal(*body, &sent); err != nil {
		t.Fatalf("decoding the request body %q: %v", *body, err)
	}
	if _, present := sent["goal"]; present {
		t.Errorf("an empty goal should be omitted, got %v", sent)
	}
}

func TestDeleteSprintCallsTheEndpoint(t *testing.T) {
	svc, req, _ := sprintServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	})

	if err := svc.DeleteSprint(context.Background(), 7); err != nil {
		t.Fatalf("DeleteSprint: %v", err)
	}
	if req.Method != http.MethodDelete {
		t.Errorf("method = %s, want DELETE", req.Method)
	}
	if want := "/api/v1/sprints/7"; req.URL.Path != want {
		t.Errorf("path = %s, want %s", req.URL.Path, want)
	}
}

// The server refuses to delete a sprint that is not CANCELLED; that refusal must surface as an error the
// screen can turn into a reason, not be swallowed.
func TestDeleteSprintSurfacesTheServerRefusal(t *testing.T) {
	svc, _, _ := sprintServiceOn(t, func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/problem+json")
		w.WriteHeader(http.StatusBadRequest)
		_, _ = w.Write([]byte(`{"detail":"Only a cancelled sprint can be deleted","code":"SPRINT_NOT_CANCELLED"}`))
	})

	err := svc.DeleteSprint(context.Background(), 7)
	if err == nil {
		t.Fatal("a 400 should be reported as an error")
	}
	if got := ErrorReason(err); got != "Only a cancelled sprint can be deleted" {
		t.Errorf("the server's reason should survive for the toast, got %q", got)
	}
}
