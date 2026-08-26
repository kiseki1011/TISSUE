package project

import (
	"strings"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func sampleActivity() domain.IssueActivityPage {
	at := time.Date(2026, 1, 2, 9, 0, 0, 0, time.UTC) // fixed past date -> absolute relative-time format
	return domain.IssueActivityPage{HasNext: true, Items: []domain.IssueActivity{
		{ID: 2, Type: "ISSUE_UPDATED", ActorID: 5, OccurredAt: at, Changes: []domain.ActivityChange{
			{Field: "content"}, {Field: "priority", From: "P2", To: "P0"},
		}},
		{ID: 1, Type: "ISSUE_CREATED", ActorID: 0, OccurredAt: at}, // system actor, no changes
	}}
}

func activityReady(t *testing.T, w, h int, page domain.IssueActivityPage) Model {
	t.Helper()
	m := loaded(t, w, h, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(press("enter"))
	m.members = []domain.ProjectMember{{MemberID: 5, DisplayName: "Alice"}}
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "T"}})
	m, _ = m.Update(press("v"))
	m, _ = m.Update(ActivitiesLoadedMsg{key: m.viewKey, gen: m.activityGen[m.viewKey], page: page})
	return m
}

func TestActivityToggleAndLoad(t *testing.T) {
	m := loaded(t, 170, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey}})
	if m.showActivity {
		t.Fatal("the activity view should start hidden")
	}
	var cmd tea.Cmd
	m, cmd = m.Update(press("v"))
	if !m.showActivity {
		t.Fatal("v should toggle the activity view on")
	}
	if !m.activitiesPending[m.viewKey] || cmd == nil {
		t.Error("toggling the activity view on should start a load")
	}
	m, _ = m.Update(press("v"))
	if m.showActivity {
		t.Error("v should toggle the activity view back off")
	}
}

// A wide terminal gives Activity its own third column.
func TestActivityThreeColumn(t *testing.T) {
	m := activityReady(t, 170, 40, sampleActivity())
	if !m.threeCol() {
		t.Fatal("a wide terminal should give the activity view its own column")
	}
	body := plain(m.View())
	if !strings.Contains(body, "Details") || !strings.Contains(body, "Activity") {
		t.Errorf("both panels should show in three-column mode:\n%s", body)
	}
	if !strings.Contains(body, "No comments yet") {
		t.Errorf("the Details column should still render alongside Activity:\n%s", body)
	}
}

// Below the three-column width the Activity view swaps in for Details.
func TestActivitySwap(t *testing.T) {
	m := activityReady(t, 140, 40, sampleActivity())
	if m.threeCol() || m.narrow() {
		t.Fatalf("140 should be a two-column swap: threeCol=%v narrow=%v", m.threeCol(), m.narrow())
	}
	body := plain(m.View())
	if !strings.Contains(body, "Activity") || !strings.Contains(body, "Content updated") {
		t.Errorf("the activity view should replace the detail panel:\n%s", body)
	}
	if strings.Contains(body, "No comments yet") {
		t.Errorf("the Details body should be swapped out for Activity:\n%s", body)
	}
}

// Details is wider than the Activity rail (3:2), and columns plus gaps exactly fill the inner width.
func TestActivityColumnRatio(t *testing.T) {
	m := activityReady(t, 200, 40, sampleActivity())
	if !m.threeCol() {
		t.Fatal("200 should be three-column")
	}
	list, detail, activity := m.panelWidths()
	if detail <= activity {
		t.Errorf("Details should be wider than Activity, got detail=%d activity=%d", detail, activity)
	}
	if got := list + 1 + detail + 1 + activity; got != m.innerWidth() {
		t.Errorf("columns + gaps should fill the inner width: %d != %d", got, m.innerWidth())
	}
}

// In narrow mode the toggle swaps the read-only modal to Activity.
func TestActivityNarrowModal(t *testing.T) {
	m := activityReady(t, 110, 40, sampleActivity())
	if !m.narrow() {
		t.Fatal("110 should be narrow")
	}
	body := plain(m.View())
	if !strings.Contains(body, "Activity") || !strings.Contains(body, "Content updated") {
		t.Errorf("the narrow modal should show the activity view:\n%s", body)
	}
}

func TestActivityContentRendering(t *testing.T) {
	body := plain(activityReady(t, 170, 40, sampleActivity()).View())
	for _, want := range []string{"●", "Updated", "Created", "Alice", "Content updated", "Priority: P2 → P0", "2026-01-02", "earlier activity not shown"} {
		if !strings.Contains(body, want) {
			t.Errorf("activity body missing %q:\n%s", want, body)
		}
	}
	if strings.Contains(body, "System") {
		t.Errorf("a system actor should be omitted, not labelled:\n%s", body)
	}
}

// A long line wraps and keeps the timeline connector, never overflowing the column.
func TestActivityWraps(t *testing.T) {
	long := strings.Repeat("verylongword ", 30)
	page := domain.IssueActivityPage{Items: []domain.IssueActivity{
		{ID: 1, Type: "ISSUE_UPDATED", ActorID: 5, Changes: []domain.ActivityChange{{Field: "summary", From: "old", To: long}}},
	}}
	m := activityReady(t, 170, 40, page)
	lines := strings.Split(plain(m.activityContent(page, 40)), "\n")
	wrapped := 0
	for _, line := range lines {
		if lipgloss.Width(line) > 40 {
			t.Fatalf("a line overflowed the column: %d > 40: %q", lipgloss.Width(line), line)
		}
		if strings.HasPrefix(line, "│ ") {
			wrapped++
		}
	}
	if wrapped < 2 {
		t.Errorf("the long line should wrap onto connector-prefixed continuations, got %d:\n%s", wrapped, strings.Join(lines, "\n"))
	}
}

// Scroll keys drive the Activity offset while it shows, leaving the Details scroll untouched.
func TestActivityScrollRouting(t *testing.T) {
	var items []domain.IssueActivity
	for i := 0; i < 30; i++ {
		items = append(items, domain.IssueActivity{ID: int64(i), Type: "ISSUE_COMMENT_ADDED", ActorID: 5})
	}
	m := activityReady(t, 170, 12, domain.IssueActivityPage{Items: items})
	if m.activityScrollMax() == 0 {
		t.Fatal("the fixture should overflow the short panel")
	}
	m, _ = m.Update(press("j"))
	if m.activityScroll == 0 {
		t.Error("j should scroll the activity view")
	}
	if m.detailScroll != 0 {
		t.Error("scrolling the activity view must not move the detail scroll")
	}
}

// A resize must re-clamp activityScroll, else the first scroll key after it is a dead press.
func TestActivityScrollReclampOnResize(t *testing.T) {
	var items []domain.IssueActivity
	for i := 0; i < 30; i++ {
		items = append(items, domain.IssueActivity{ID: int64(i), Type: "ISSUE_COMMENT_ADDED", ActorID: 5})
	}
	m := activityReady(t, 170, 16, domain.IssueActivityPage{Items: items})
	m.activityScroll = m.activityScrollMax() // scrolled to the bottom
	m, _ = m.Update(tea.WindowSizeMsg{Width: 170, Height: 44})
	if m.activityScroll > m.activityScrollMax() {
		t.Errorf("resize must re-clamp activityScroll: %d > max %d", m.activityScroll, m.activityScrollMax())
	}
}

// activityDetails follows the deprecated client's rules for each change shape.
func TestActivityDetails(t *testing.T) {
	a := domain.IssueActivity{
		Changes: []domain.ActivityChange{
			{Field: "content", From: "x", To: "y"},
			{Field: "priority", From: "P2", To: "P0"},
			{Field: "assignee", From: "", To: "Alice"},
			{Field: "dueAt", From: "2026-01-01", To: ""},
			{Field: "summary", From: "", To: ""}, // neither -> skipped
		},
		Data: []domain.ActivityData{
			{Key: "issueKey", Value: "TIS-1"}, // skipped
			{Key: "branch", Value: "feature/x"},
		},
	}
	got := activityDetails(a, nil)
	want := []string{
		"Content updated",
		"Priority: P2 → P0",
		"Assignee: Alice",
		"Due at: 2026-01-01 (cleared)",
		"Branch: feature/x",
	}
	if len(got) != len(want) {
		t.Fatalf("got %d lines %q, want %d %q", len(got), got, len(want), want)
	}
	for i := range want {
		if got[i] != want[i] {
			t.Errorf("line %d = %q, want %q", i, got[i], want[i])
		}
	}
}

func TestActivityLabel(t *testing.T) {
	cases := map[string]string{
		"ISSUE_CREATED":               "Created",
		"ISSUE_COMMENT_ADDED":         "Comment added",
		"ISSUE_WORKFLOW_TRANSITIONED": "Workflow transitioned",
		"SPRINT_STARTED":              "Sprint started",
		"":                            "Activity",
		"ISSUE_":                      "Activity", // reduces to empty, must not panic
	}
	for in, want := range cases {
		if got := activityLabel(in); got != want {
			t.Errorf("activityLabel(%q)=%q, want %q", in, got, want)
		}
	}
}

func TestChangeLabelCustomField(t *testing.T) {
	names := map[string]string{"42": "severity"}
	if got := changeLabel("customFields.42", names); got != "Severity" {
		t.Errorf("a loaded custom field should resolve to its label, got %q", got)
	}
	if got := changeLabel("customFields.99", names); got != "Custom field 99" {
		t.Errorf("an unknown custom field should fall back, got %q", got)
	}
	if got := changeLabel("priority", names); got != "Priority" {
		t.Errorf("a regular field should humanize, got %q", got)
	}
}

func TestHumanizeKey(t *testing.T) {
	cases := map[string]string{
		"priority":     "Priority",
		"storyPoint":   "Story point",
		"assigneeName": "Assignee",
		"dueAt":        "Due at",
		"parentKey":    "Parent",
	}
	for in, want := range cases {
		if got := humanizeKey(in); got != want {
			t.Errorf("humanizeKey(%q)=%q, want %q", in, got, want)
		}
	}
}

func TestMemberLabel(t *testing.T) {
	m := loaded(t, 170, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m.members = []domain.ProjectMember{{MemberID: 5, DisplayName: "Alice"}}
	if got := m.memberLabel(0); got != "" {
		t.Errorf("the system actor should be omitted, got %q", got)
	}
	if got := m.memberLabel(5); got != "Alice" {
		t.Errorf("a known actor should resolve to its name, got %q", got)
	}
	if got := m.memberLabel(99); got != "" {
		t.Errorf("an unknown actor should be omitted, got %q", got)
	}
}
