package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

func mentionMembers() []domain.ProjectMember {
	return []domain.ProjectMember{
		{MemberID: 1, Username: "alice", DisplayName: "Alice Kim"},
		{MemberID: 2, Username: "bob", DisplayName: "Bob Lee"},
		{MemberID: 3, Username: "albert", DisplayName: "Albert"},
	}
}

// mentionReady opens the comment modal on the root composer with members loaded, ready for '@'.
func mentionReady(t *testing.T) Model {
	t.Helper()
	m := editReady(t, domain.IssueDetail{Title: "Root", StateLabel: "Active", StateCategory: "ACTIVE"})
	m.members = mentionMembers()
	m.membersLoaded = true
	m, _ = m.Update(press("c"))
	if !m.commenting || m.commentUI.focus != (commentFocus{0, partText}) {
		t.Fatalf("setup: c should open the comment modal on the root composer, got commenting=%v focus=%+v", m.commenting, m.commentUI.focus)
	}
	return m
}

// typeInto types s one rune at a time, as real keystrokes would.
func typeInto(m Model, s string) Model {
	for _, r := range s {
		m, _ = m.Update(press(string(r)))
	}
	return m
}

// mentionQuery finds the "@token" at the cursor, but only when '@' opens a word.
func TestMentionQueryDetectsToken(t *testing.T) {
	ta := newCommentArea("")
	ta.SetValue("hey @al")
	start, row, col, q, ok := mentionQuery(ta)
	if !ok || q != "al" || start != 4 || row != 0 || col != 7 {
		t.Fatalf("expected token @al at 4, got ok=%v q=%q start=%d row=%d col=%d", ok, q, start, row, col)
	}

	ta.SetValue("bare @")
	if _, _, _, q, ok := mentionQuery(ta); !ok || q != "" {
		t.Errorf("a lone '@' should be an active, empty query: ok=%v q=%q", ok, q)
	}

	ta.SetValue("mail me@host") // '@' embedded in a word is not a mention
	if _, _, _, _, ok := mentionQuery(ta); ok {
		t.Error("an embedded '@' (email) must not trigger a mention")
	}

	ta.SetValue("@al done") // cursor at end sits past a space, so no active token
	if _, _, _, _, ok := mentionQuery(ta); ok {
		t.Error("a token completed by whitespace must not stay active at end of line")
	}
}

// username-prefix outranks display-name-prefix. An empty query offers all, no-username is skipped, capped.
func TestMatchMembers(t *testing.T) {
	members := []domain.ProjectMember{
		{Username: "alice", DisplayName: "Alice"},
		{Username: "bob", DisplayName: "Bob"},
		{Username: "carol", DisplayName: "Alberto"}, // display-name prefix "al"
		{Username: "", DisplayName: "Ghost"},        // no username -> unmentionable
	}
	got := matchMembers("al", members)
	if len(got) != 2 {
		t.Fatalf("query 'al' should match alice (username) and carol (display), got %d: %+v", len(got), got)
	}
	if got[0].Username != "alice" {
		t.Errorf("username-prefix should rank first, got %q", got[0].Username)
	}

	all := matchMembers("", members)
	if len(all) != 3 {
		t.Errorf("empty query should offer the 3 mentionable members, got %d", len(all))
	}
	for _, mem := range all {
		if mem.Username == "" {
			t.Error("a member without a username must never be offered")
		}
	}

	many := make([]domain.ProjectMember, 20)
	for i := range many {
		many[i] = domain.ProjectMember{Username: "user" + string(rune('a'+i))}
	}
	if capped := matchMembers("user", many); len(capped) != mentionMax {
		t.Errorf("the dropdown should cap at %d rows, got %d", mentionMax, len(capped))
	}
}

// insertMention swaps the token and leaves the caret right after it, single-line and across lines.
func TestInsertMention(t *testing.T) {
	ta := newCommentArea("")
	ta.SetValue("hi @al world")
	ta.SetCursorColumn(6) // just past "@al", on the existing space
	ta = insertMention(ta, "alice")
	if ta.Value() != "hi @alice world" { // the existing space is reused, not doubled
		t.Errorf("single-line insert wrong: %q", ta.Value())
	}
	if ta.Line() != 0 || ta.Column() != 10 { // after "hi @alice " - past the reused space, so the popup closes
		t.Errorf("cursor should sit after the inserted mention and its space, got row=%d col=%d", ta.Line(), ta.Column())
	}

	ta.SetValue("line1\nhey @b\nline3")
	ta.CursorUp()         // from the last line up to "hey @b"
	ta.SetCursorColumn(6) // end of "hey @b"
	ta = insertMention(ta, "bob")
	if ta.Value() != "line1\nhey @bob \nline3" {
		t.Errorf("multi-line insert wrong: %q", ta.Value())
	}
	if ta.Line() != 1 || ta.Column() != 9 {
		t.Errorf("multi-line cursor wrong, got row=%d col=%d", ta.Line(), ta.Column())
	}

	// no active token at the cursor -> unchanged
	ta.SetValue("no mention here")
	ta.MoveToEnd()
	if got := insertMention(ta, "alice"); got.Value() != "no mention here" {
		t.Errorf("insert with no active token should be a no-op, got %q", got.Value())
	}
}

// Only real handles are sent: case-insensitive, de-duplicated, email '@' ignored, punctuation trimmed.
func TestCollectMentions(t *testing.T) {
	members := mentionMembers()

	got := collectMentions("hey @alice and @bob!", members)
	if strings.Join(got, ",") != "alice,bob" {
		t.Errorf("expected alice,bob, got %v", got)
	}
	if dup := collectMentions("@alice @alice", members); len(dup) != 1 {
		t.Errorf("a repeated mention should be sent once, got %v", dup)
	}
	if ci := collectMentions("ping @Alice.", members); len(ci) != 1 || ci[0] != "alice" {
		t.Errorf("case-insensitive match with trailing punctuation should yield canonical alice, got %v", ci)
	}
	if none := collectMentions("mail test@alice.com", members); len(none) != 0 {
		t.Errorf("an embedded '@' (email) must not be read as a mention, got %v", none)
	}
	if unknown := collectMentions("@nobody here", members); len(unknown) != 0 {
		t.Errorf("an unknown handle is not sent, got %v", unknown)
	}
}

// Typing '@' opens the popup. More typing narrows it to the matching members.
func TestMentionPopupOpensAndFilters(t *testing.T) {
	m := mentionReady(t)
	m = typeInto(m, "@")
	if !m.commentUI.mention.active {
		t.Fatal("typing '@' should open the mention popup")
	}
	m = typeInto(m, "al")
	view := plain(m.View())
	if !strings.Contains(view, "alice") || !strings.Contains(view, "albert") {
		t.Errorf("the popup should offer the 'al' matches:\n%s", view)
	}
	if strings.Contains(view, "bob") {
		t.Errorf("a non-matching member should be filtered out:\n%s", view)
	}
}

// enter accepts the highlighted candidate instead of submitting the comment or adding a newline.
func TestMentionEnterAcceptsWithoutSubmitting(t *testing.T) {
	m := mentionReady(t)
	m = typeInto(m, "@bo") // a single match: bob
	if !m.commentUI.mention.active {
		t.Fatal("setup: the popup should be open")
	}
	m, _ = m.Update(press("enter"))
	if got := m.commentUI.root.Value(); got != "@bob " {
		t.Errorf("enter should insert the mention, got %q", got)
	}
	if m.commentUI.mention.active {
		t.Error("accepting should close the popup")
	}
	if !m.commenting || m.commentUI.sending != -1 {
		t.Errorf("accepting a mention must not submit the comment: commenting=%v sending=%d", m.commenting, m.commentUI.sending)
	}
}

// esc dismisses only the popup. The modal and the typed text stay.
func TestMentionEscDismissesPopupOnly(t *testing.T) {
	m := mentionReady(t)
	m = typeInto(m, "@bo")
	m, _ = m.Update(press("esc"))
	if m.commentUI.mention.active {
		t.Error("esc should dismiss the popup")
	}
	if !m.commenting {
		t.Error("esc on the popup must not close the whole modal")
	}
	if m.commentUI.root.Value() != "@bo" {
		t.Errorf("the typed text should survive a popup dismiss, got %q", m.commentUI.root.Value())
	}
}

// down moves the popup selection, not the textarea cursor.
func TestMentionArrowMovesSelection(t *testing.T) {
	m := mentionReady(t)
	m = typeInto(m, "@a") // alice + albert (albert sorts first on the username tiebreak)
	if len(m.commentUI.mention.matches) < 2 {
		t.Fatalf("setup: expected at least two candidates, got %d", len(m.commentUI.mention.matches))
	}
	second := m.commentUI.mention.matches[1].Username
	m, _ = m.Update(press("down"))
	if m.commentUI.mention.sel != 1 {
		t.Errorf("down should advance the selection, got %d", m.commentUI.mention.sel)
	}
	m, _ = m.Update(press("enter"))
	if got := m.commentUI.root.Value(); got != "@"+second+" " {
		t.Errorf("down then enter should accept the second candidate %q, got %q", second, got)
	}
}

// Clicking a candidate row accepts it.
func TestMentionClickAccepts(t *testing.T) {
	m := mentionReady(t)
	m = typeInto(m, "@bo") // single match bob at row 0
	click := clickZone(t, m, commentMentionZoneID(0))
	m, _ = m.Update(click)
	if got := m.commentUI.root.Value(); got != "@bob " {
		t.Errorf("clicking a candidate should insert it, got %q", got)
	}
}

// A mid-line accept reuses the existing space and lands the caret past it, so the popup does not reopen.
func TestInsertMentionMidLineLandsPastSpace(t *testing.T) {
	ta := newCommentArea("")
	ta.SetValue("see @bo here")
	ta.SetCursorColumn(7) // just past "@bo", on the existing space
	ta = insertMention(ta, "bob")
	if ta.Value() != "see @bob here" { // the space is reused, not doubled
		t.Errorf("mid-line insert wrong: %q", ta.Value())
	}
	if _, _, _, _, ok := mentionQuery(ta); ok {
		t.Error("after a mid-line accept the caret must sit past the token so no mention is re-detected")
	}
}

// esc dismisses durably: a blink or cursor move must not reopen the popup, an actual edit may.
func TestMentionEscStaysDismissed(t *testing.T) {
	m := mentionReady(t)
	m = typeInto(m, "@bob")
	m, _ = m.Update(press("esc"))
	if m.commentUI.mention.active {
		t.Fatal("esc should dismiss the popup")
	}
	if m.refreshMention(false).commentUI.mention.active {
		t.Error("a non-edit refresh (blink/cursor move) reopened an esc-dismissed popup")
	}
	if !m.refreshMention(true).commentUI.mention.active {
		t.Error("an edit refresh should reopen the popup while the caret is in a matching token")
	}
}

// A cursor move after esc goes through forwardToComposer but does not reopen the dismissed popup.
func TestMentionCursorMoveAfterEscDoesNotReopen(t *testing.T) {
	m := mentionReady(t)
	m = typeInto(m, "@bob")
	m, _ = m.Update(press("esc"))
	m, _ = m.Update(press("left")) // a cursor-only move (not an edit)
	if m.commentUI.mention.active {
		t.Error("a cursor move after esc must not reopen the popup")
	}
}

// When the modal overflows the terminal, opening the popup scrolls it into the visible window.
func TestMentionPopupScrollsIntoView(t *testing.T) {
	m := loaded(t, 120, 12, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "Root", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m.members = mentionMembers()
	m.membersLoaded = true
	comments := make([]domain.IssueComment, 8) // a long thread overflows the 12-row terminal
	for i := range comments {
		comments[i] = domain.IssueComment{ID: int64(i + 1), AuthorName: "U", Content: "a comment line"}
	}
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{
		key: m.viewKey, gen: m.detailGen[m.viewKey],
		detail: domain.IssueDetail{Key: m.viewKey, Title: "Root", StateLabel: "Active", StateCategory: "ACTIVE", CommentCount: len(comments), Comments: comments},
	})
	m, _ = m.Update(press("c"))

	m = typeInto(m, "@")
	if !m.commentUI.mention.active {
		t.Fatal("typing '@' should open the popup")
	}
	if view := plain(m.View()); !strings.Contains(view, "alice") {
		t.Errorf("the popup should scroll into the visible window, not render past the bottom:\n%s", view)
	}
}

// A username ending in '_' survives: the whole token matches before the punctuation-trim fallback.
func TestCollectMentionsKeepsPunctuationUsername(t *testing.T) {
	members := []domain.ProjectMember{{Username: "john_", DisplayName: "John"}, {Username: "bob"}}
	got := collectMentions("hi @john_ and @bob.", members)
	if strings.Join(got, ",") != "john_,bob" {
		t.Errorf("a username ending in '_' must not be trimmed away, and a hand-typed '@bob.' still matches; got %v", got)
	}
}

// The footer advertises the popup's own keys while it is open.
func TestMentionHelpKeysWhileOpen(t *testing.T) {
	m := mentionReady(t)
	m = typeInto(m, "@a")
	var labels []string
	for _, b := range m.commentHelpKeys() {
		labels = append(labels, b.Help().Desc)
	}
	joined := strings.Join(labels, " ")
	if !strings.Contains(joined, "select") || !strings.Contains(joined, "mention") || !strings.Contains(joined, "dismiss") {
		t.Errorf("popup help should advertise select/mention/dismiss, got %v", labels)
	}
}
