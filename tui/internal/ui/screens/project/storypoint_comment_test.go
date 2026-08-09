package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// spDetail is a sample detail whose type permits (or not) a story point, seeded with a point value.
func spDetail(canUse bool, point int) domain.IssueDetail {
	d := sampleDetail()
	d.CanUseStoryPoint = canUse
	d.StoryPoint = point
	return d
}

// #6: when the type permits a point, the edit form shows a Story point field and it is a tab stop.
func TestEditStoryPointShownWhenAllowed(t *testing.T) {
	m := editReady(t, spDetail(true, 5))
	m, _ = m.Update(press("e"))
	if !strings.Contains(plain(m.editUI.View()), "Story point") {
		t.Errorf("the edit form should show a Story point field when the type allows it:\n%s", plain(m.editUI.View()))
	}
	if indexOfInt(m.editUI.fields(), efStoryPoint) < 0 {
		t.Error("story point should be a tab stop when the type allows it")
	}
}

// #6: when the type disallows a point, the field is hidden and is not a tab stop (so the UI can never hit
// STORY_POINT_NOT_ALLOWED).
func TestEditStoryPointHiddenWhenDisallowed(t *testing.T) {
	m := editReady(t, spDetail(false, 0))
	m, _ = m.Update(press("e"))
	if strings.Contains(plain(m.editUI.View()), "Story point") {
		t.Errorf("the edit form must hide the Story point field when the type disallows it:\n%s", plain(m.editUI.View()))
	}
	if indexOfInt(m.editUI.fields(), efStoryPoint) >= 0 {
		t.Error("story point must not be a tab stop when the type disallows it")
	}
}

// #6: the Priority + Story point row must be exactly as wide as every other edit field, so the modal's
// right border stays flush (the two boxed halves + gap == a full field's outer width). Regression for the
// review's CONFIRMED finding.
func TestEditStoryPointRowAligned(t *testing.T) {
	m := editReady(t, spDetail(true, 3))
	m, _ = m.Update(press("e"))
	f := m.editUI
	full := lipgloss.Width(f.field(efDue, "Due", f.dueContent(), ""))
	pair := lipgloss.Width(f.priorityStoryRow())
	if pair != full {
		t.Errorf("the Priority/Story point row width %d must match a full field width %d", pair, full)
	}
}

// #6: the form prefills the current point and a changed point flows through to an optimistic cache update
// and a save command.
func TestEditStoryPointSaves(t *testing.T) {
	m := editReady(t, spDetail(true, 5))
	m, _ = m.Update(press("e"))
	if got := m.editUI.storyPoint.Value(); got != "5" {
		t.Errorf("the story point field should prefill the current point, got %q", got)
	}
	m, cmd := m.Update(editSubmittedMsg{v: editValues{
		title: "Fix login", priority: "P2", storyPoint: 8,
	}})
	if cmd == nil {
		t.Fatal("a changed story point should run a save")
	}
	if d := m.details[m.viewKey]; d.StoryPoint != 8 {
		t.Errorf("the cached detail should optimistically show the new point, got %d", d.StoryPoint)
	}
}

// #6: the story point field ignores non-digit character input (a whole number only).
func TestEditStoryPointDigitsOnly(t *testing.T) {
	m := editReady(t, spDetail(true, 0))
	m, _ = m.Update(press("e"))
	m.editUI, _ = m.editUI.focusOn(efStoryPoint)
	m.editUI, _ = m.editUI.onKey(press("a")) // a letter is dropped
	m.editUI, _ = m.editUI.onKey(press("7")) // a digit is kept
	if got := m.editUI.storyPoint.Value(); got != "7" {
		t.Errorf("the story point field should accept digits and drop letters, got %q", got)
	}
}

// diffEdit includes the story point only when it changed.
func TestDiffEditStoryPoint(t *testing.T) {
	orig := domain.IssueDetail{Title: "T", Priority: "P2", StoryPoint: 5}
	same := diffEdit(orig, editValues{title: "T", priority: "P2", storyPoint: 5})
	if same.StoryPoint != nil {
		t.Errorf("an unchanged story point must not appear in the diff, got %v", *same.StoryPoint)
	}
	changed := diffEdit(orig, editValues{title: "T", priority: "P2", storyPoint: 0})
	if changed.StoryPoint == nil || *changed.StoryPoint != 0 {
		t.Errorf("clearing the story point (to 0) should be sent explicitly, got %v", changed.StoryPoint)
	}
}

// #comment: a comment header shows "{name} (@{username})".
func TestCommentAuthorWithUsername(t *testing.T) {
	d := domain.IssueDetail{
		Key: "TIS-1", Title: "T", CommentCount: 1,
		Comments: []domain.IssueComment{{ID: 1, AuthorName: "Kim Seungki", AuthorUsername: "seungki", Content: "hi"}},
	}
	line := lineWith(t, detailBodyWith(0, 90, d), "Kim Seungki")
	if !strings.Contains(line, "Kim Seungki (@seungki)") {
		t.Errorf("the comment header should read \"{name} (@{username})\", got %q", line)
	}
}

// #comment: the @handle is omitted when the server sends no username, or when it merely duplicates the
// name (a display-name fallback), so the header does not read "name (@name)".
func TestCommentAuthorUsernameOmitted(t *testing.T) {
	for _, c := range []domain.IssueComment{
		{ID: 1, AuthorName: "Solo", Content: "x"},                         // no username
		{ID: 2, AuthorName: "dupe", AuthorUsername: "dupe", Content: "x"}, // username == name
	} {
		d := domain.IssueDetail{Key: "TIS-1", Title: "T", CommentCount: 1, Comments: []domain.IssueComment{c}}
		line := lineWith(t, detailBodyWith(0, 90, d), c.AuthorName)
		if strings.Contains(line, "(@") {
			t.Errorf("no redundant/empty @handle should show, got %q", line)
		}
	}
}
