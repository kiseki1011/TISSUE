package project

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// detailBodyWith renders the detail content at width w in the given glyph mode.
func detailBodyWith(mode glyph.Mode, w int, d domain.IssueDetail) string {
	zone.NewGlobal()
	dp := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(mode)}
	m := New(dp, testKey, "Tissue")
	m, _ = m.Update(tea.WindowSizeMsg{Width: 160, Height: 60})
	return plain(m.detailContent(d, w))
}

// lineWith returns the first rendered line that contains sub.
func lineWith(t *testing.T, body, sub string) string {
	t.Helper()
	for _, line := range strings.Split(body, "\n") {
		if strings.Contains(line, sub) {
			return line
		}
	}
	t.Fatalf("no line contains %q in:\n%s", sub, body)
	return ""
}

// Children/reviewers/relations right-anchor their status in the shared colStatus column, so every label
// starts at w-colStatus. The parent is a fixed meta row, so it is excluded.
func TestDetailStatusColumnsAligned(t *testing.T) {
	const w = 90
	d := domain.IssueDetail{
		Key: "TIS-1", Title: "T",
		Parent:   &domain.IssueRef{Key: "PAR-1", TypeName: "Epic", StateLabel: "Active", StateCategory: "ACTIVE"},
		Children: []domain.IssueRef{{Key: "CHI-1", TypeName: "Task", StateLabel: "Done", StateCategory: "COMPLETED"}},
		Reviewers: []domain.Reviewer{
			{Name: "Rev", Status: "APPROVED"},
			{Name: "Rev2", Status: "CHANGES_REQUESTED"}, // the longest fixed status must still fit
		},
		Relations: []domain.IssueRelationGroup{{Kind: "Blocks", Items: []domain.RelatedIssue{
			{Key: "REL-1", Title: "Linked", StateLabel: "Aborted", StateCategory: "ABORTED"},
		}}},
	}
	body := detailBodyWith(glyph.Unicode, w, d)

	want := w - colStatus
	for label, key := range map[string]string{"Done": "CHI-1", "Approved": "Rev ", "Aborted": "REL-1"} {
		line := lineWith(t, body, key)
		col := lipgloss.Width(line[:strings.Index(line, label)])
		if col != want {
			t.Errorf("status %q on the %q row starts at column %d, want %d:\n%q", label, key, col, want, line)
		}
	}
	if !strings.Contains(body, "Changes requested") {
		t.Errorf("the review status column must fit %q:\n%s", "Changes requested", body)
	}
}

// A TEXT field renders its markdown then a blank line, so it reads apart from the next field.
func TestDetailTextFieldTrailingBlank(t *testing.T) {
	d := domain.IssueDetail{
		Key: "TIS-1", Title: "T",
		CustomFields: []domain.CustomField{
			{Label: "Notes", Type: "TEXT", Text: "the body"},
			{Label: "After", Type: "SHORT_TEXT", Text: "next"},
		},
	}
	lines := strings.Split(detailBodyWith(glyph.Unicode, 80, d), "\n")
	bodyIdx, afterIdx := -1, -1
	for i, line := range lines {
		if strings.Contains(line, "the body") {
			bodyIdx = i
		}
		if strings.Contains(line, "After") {
			afterIdx = i
		}
	}
	if bodyIdx < 0 || afterIdx < 0 {
		t.Fatalf("missing rows: body=%d after=%d\n%s", bodyIdx, afterIdx, strings.Join(lines, "\n"))
	}
	if strings.TrimSpace(lines[bodyIdx+1]) != "" {
		t.Errorf("a TEXT field must be followed by a blank line, got %q", lines[bodyIdx+1])
	}
	if afterIdx != bodyIdx+2 {
		t.Errorf("the next field should sit one blank line below the TEXT block: body=%d after=%d", bodyIdx, afterIdx)
	}
}

// A reply is preceded by a blank line and gutter-marked, one bar per nesting level.
func TestDetailReplyGutter(t *testing.T) {
	d := domain.IssueDetail{
		Key: "TIS-1", Title: "T", CommentCount: 1,
		Comments: []domain.IssueComment{{
			ID: 1, AuthorName: "Top", Content: "root body",
			Replies: []domain.IssueComment{{
				ID: 2, AuthorName: "Child", Content: "reply body",
				Replies: []domain.IssueComment{{ID: 3, AuthorName: "Grand", Content: "deep reply"}},
			}},
		}},
	}
	lines := strings.Split(detailBodyWith(glyph.Unicode, 80, d), "\n")
	rootIdx, replyIdx, deepIdx := -1, -1, -1
	for i, line := range lines {
		switch {
		case strings.Contains(line, "root body"):
			rootIdx = i
		case strings.Contains(line, "Child ·") || strings.Contains(line, "Child"):
			if replyIdx < 0 {
				replyIdx = i
			}
		case strings.Contains(line, "Grand"):
			deepIdx = i
		}
	}
	if rootIdx < 0 || replyIdx < 0 || deepIdx < 0 {
		t.Fatalf("missing comment rows: root=%d reply=%d deep=%d\n%s", rootIdx, replyIdx, deepIdx, strings.Join(lines, "\n"))
	}
	if strings.TrimSpace(lines[replyIdx-1]) != "" {
		t.Errorf("a reply must be preceded by a blank line, got %q", lines[replyIdx-1])
	}
	if !strings.HasPrefix(lines[replyIdx], "│ ") {
		t.Errorf("a first-level reply must carry one vertical bar, got %q", lines[replyIdx])
	}
	if !strings.HasPrefix(lines[deepIdx], "│ │ ") {
		t.Errorf("a second-level reply must carry two vertical bars, got %q", lines[deepIdx])
	}
}

// Meta rows and custom-field labels lead with a glyph in nerd mode and nothing on plain terminals.
func TestDetailGlyphs(t *testing.T) {
	d := domain.IssueDetail{
		Key: "TIS-1", Title: "T",
		CustomFields: []domain.CustomField{{Label: "Severity", Type: "SHORT_TEXT", Text: "High"}},
	}
	plainBody := detailBodyWith(glyph.Unicode, 80, d)
	nerdBody := detailBodyWith(glyph.Nerd, 80, d)

	// plain: the label starts the row, no leading glyph.
	if l := lineWith(t, plainBody, "Key"); !strings.HasPrefix(l, "Key") {
		t.Errorf("plain meta row should start with the bare label, got %q", l)
	}
	if l := lineWith(t, plainBody, "Severity"); !strings.HasPrefix(l, "Severity") {
		t.Errorf("plain custom-field label should start bare, got %q", l)
	}
	// nerd: the meta row's Key glyph and the SHORT_TEXT field's WholeWord glyph are present.
	g := glyph.New(glyph.Nerd)
	if !strings.Contains(nerdBody, g.Key) {
		t.Errorf("nerd meta row should carry the Key glyph %q:\n%s", g.Key, nerdBody)
	}
	if !strings.Contains(nerdBody, g.WholeWord) {
		t.Errorf("nerd custom-field label should carry its type glyph %q:\n%s", g.WholeWord, nerdBody)
	}
}

// Comment bodies go through the markdown pipeline, so inline syntax is formatted away, not shown raw.
func TestCommentRendersMarkdown(t *testing.T) {
	d := domain.IssueDetail{
		Key: "TIS-1", Title: "T", CommentCount: 1,
		Comments: []domain.IssueComment{{ID: 1, AuthorName: "A", Content: "has **bold** and `code`"}},
	}
	body := detailBodyWith(glyph.Unicode, 90, d)
	for _, raw := range []string{"**", "`"} {
		if strings.Contains(body, raw) {
			t.Errorf("comment markdown should be rendered, not raw %q:\n%s", raw, body)
		}
	}
	if !strings.Contains(body, "bold") || !strings.Contains(body, "code") {
		t.Errorf("comment text should survive markdown rendering:\n%s", body)
	}
}

// Every non-deleted comment carries a clickable Reply affordance. A deleted one does not.
func TestCommentReplyLinkShown(t *testing.T) {
	d := domain.IssueDetail{
		Key: "TIS-1", Title: "T", CommentCount: 2,
		Comments: []domain.IssueComment{
			{ID: 1, AuthorName: "Live", Content: "body"},
			{ID: 2, AuthorName: "Gone", Content: "x", Deleted: true},
		},
	}
	body := detailBodyWith(glyph.Unicode, 90, d)
	if live := lineWith(t, body, "Live"); !strings.Contains(live, "Reply") {
		t.Errorf("a live comment should show the Reply affordance, got %q", live)
	}
	if gone := lineWith(t, body, "Gone"); strings.Contains(gone, "Reply") {
		t.Errorf("a deleted comment should not show Reply, got %q", gone)
	}
}

// Clicking Reply opens the comment modal with that comment's inline composer open and focused.
func TestReplyClickOpensComposer(t *testing.T) {
	m := loaded(t, 160, 60, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(press("enter"))
	d := domain.IssueDetail{
		Key: m.viewKey, Title: "T", CommentCount: 1,
		Comments: []domain.IssueComment{{ID: 42, AuthorName: "Alice", Content: "hello"}},
	}
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: d})

	m, _ = m.Update(clickZone(t, m, commentReplyZone(42)))
	if !m.commenting {
		t.Fatal("clicking Reply should open the comment modal")
	}
	if m.commentUI.replyingTo != 42 || m.commentUI.focus != (commentFocus{42, partText}) {
		t.Errorf("the modal should open with comment 42's inline composer focused, got replyingTo=%d focus=%+v",
			m.commentUI.replyingTo, m.commentUI.focus)
	}
	if view, _, _ := m.commentModalView(); !strings.Contains(plain(view), "Reply to Alice") {
		t.Errorf("the inline composer should name the parent author:\n%s", plain(view))
	}

	// stay-open: submitting the reply keeps the modal up (the reply lands via refetch)
	m, cmd := m.Update(commentSubmittedMsg{content: "sure", parentID: 42})
	if !m.commenting {
		t.Error("stay-open: submitting the reply keeps the modal open")
	}
	if cmd == nil {
		t.Error("submitting the reply should run the create")
	}
}

// A very long author name (untrusted server text) must not widen the modal past the terminal, or the
// float overlay spills and corrupts the frame.
func TestReplyLongAuthorFitsFrame(t *testing.T) {
	m := loaded(t, 100, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(press("enter"))
	long := strings.Repeat("VeryLongAuthorName", 20)
	d := domain.IssueDetail{
		Key: m.viewKey, Title: "T", CommentCount: 1,
		Comments: []domain.IssueComment{{ID: 7, AuthorName: long, Content: "hi"}},
	}
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: d})
	m, _ = m.openCommentSection(7) // the same opener the Reply click uses
	for _, line := range strings.Split(plain(m.View()), "\n") {
		if lipgloss.Width(line) > m.width {
			t.Fatalf("the comment modal overflowed the frame: %d > %d:\n%q", lipgloss.Width(line), m.width, line)
		}
	}
}

// An unbreakable token in a comment body (markdown does not clamp those to the wrap width) must not
// widen the read-only thread pane past the terminal.
func TestCommentLongBodyFitsFrame(t *testing.T) {
	m := loaded(t, 100, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(press("enter"))
	longURL := "https://example.com/" + strings.Repeat("verylongpath", 20)
	d := domain.IssueDetail{
		Key: m.viewKey, Title: "T", CommentCount: 1,
		Comments: []domain.IssueComment{{ID: 9, AuthorName: "Al", Content: longURL}},
	}
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: d})
	m, _ = m.openCommentSection(0)
	for _, line := range strings.Split(plain(m.View()), "\n") {
		if lipgloss.Width(line) > m.width {
			t.Fatalf("a long comment body overflowed the modal frame: %d > %d:\n%q", lipgloss.Width(line), m.width, line)
		}
	}
}
