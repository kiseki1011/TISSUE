package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// #7: the metadata edit form no longer carries the Content field (it moved to its own editor).
func TestEditFormHasNoContentField(t *testing.T) {
	m := editReady(t, sampleDetail())
	m, _ = m.Update(press("e"))
	if body := plain(m.editUI.View()); strings.Contains(body, "Content") {
		t.Errorf("the metadata edit form should not carry the Content field:\n%s", body)
	}
}

// #7: 'E' opens the standalone content editor prefilled from the loaded detail.
func TestContentEditorOpensPrefilled(t *testing.T) {
	m := editReady(t, sampleDetail()) // Content "# body"
	m, cmd := m.Update(press("E"))
	if !m.editingContent {
		t.Fatal("E should open the content editor")
	}
	if cmd == nil {
		t.Error("opening the content editor should start the input blink")
	}
	if got := m.contentUI.content.Value(); got != "# body" {
		t.Errorf("the content editor should prefill the issue body, got %q", got)
	}
	if body := plain(m.contentUI.View()); !strings.Contains(body, "Edit content") {
		t.Errorf("the content editor modal should render:\n%s", body)
	}
}

// #7: the preview toggle swaps the editable textarea for the rendered markdown, and back.
func TestContentEditorPreviewToggle(t *testing.T) {
	m := editReady(t, sampleDetail())
	m, _ = m.Update(press("E"))
	// the body is a raw textarea until preview is toggled
	if body := plain(m.contentUI.View()); strings.Contains(body, "Preview ─") && !strings.Contains(body, "Content ─") {
		t.Errorf("the editor should start in edit mode (Content box), got:\n%s", body)
	}
	m.contentUI, _ = m.contentUI.togglePreview()
	view := plain(m.contentUI.View())
	if !strings.Contains(view, "Preview ─") {
		t.Errorf("toggling preview should show a Preview box:\n%s", view)
	}
	if strings.Contains(view, "# body") {
		t.Errorf("preview should render the markdown, not show the raw '# body':\n%s", view)
	}
	m.contentUI, _ = m.contentUI.togglePreview()
	if view := plain(m.contentUI.View()); !strings.Contains(view, "Content ─") {
		t.Errorf("toggling back should return to the editable Content box:\n%s", view)
	}
}

// #7: an empty body previews a muted placeholder rather than a blank box.
func TestContentEditorPreviewEmpty(t *testing.T) {
	m := editReady(t, domain.IssueDetail{Title: "X", Priority: "P2"}) // no content
	m, _ = m.Update(press("E"))
	m.contentUI, _ = m.contentUI.togglePreview()
	if view := plain(m.contentUI.View()); !strings.Contains(view, "Nothing to preview.") {
		t.Errorf("an empty body should preview a placeholder:\n%s", view)
	}
}

// #7: saving a changed body optimistically updates the cached detail and fires the edit; an unchanged
// body reports no changes.
func TestContentEditorSaves(t *testing.T) {
	m := editReady(t, sampleDetail())
	m, _ = m.Update(press("E"))
	m, cmd := m.Update(contentSubmittedMsg{content: "## new body"})
	if m.editingContent {
		t.Error("saving should close the content editor")
	}
	if cmd == nil {
		t.Fatal("a changed body should run a save")
	}
	if d := m.details[m.viewKey]; d.Content != "## new body" {
		t.Errorf("the cached detail should optimistically show the new body, got %q", d.Content)
	}

	m2 := editReady(t, sampleDetail())
	m2, _ = m2.Update(press("E"))
	m2, cmd2 := m2.Update(contentSubmittedMsg{content: "# body"}) // unchanged
	if ts, ok := toastFrom(cmd2); !ok || !strings.Contains(ts.Text, "No changes") {
		t.Errorf("an unchanged body should report no changes, got %+v ok=%v", ts, ok)
	}
}

// #7: a background list reload (e.g. a search debounce) that lands while the content editor is open must
// not repoint viewKey, so the save still targets the issue being edited. Regression for the review's
// CONFIRMED finding.
func TestContentEditorPinsViewKeyAcrossReload(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", StateCategory: "ACTIVE"}, {Key: "ENG-2", StateCategory: "ACTIVE"}}, TotalElements: 2,
	})
	m, _ = m.Update(press("enter")) // focus ENG-1
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "One", Content: "# body"}})
	edited := m.viewKey
	m, _ = m.Update(press("E"))
	// a reload lands mid-edit and would otherwise reset the cursor to a different issue
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, page: domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-9", StateCategory: "ACTIVE"}, {Key: "ENG-1", StateCategory: "ACTIVE"}}, TotalElements: 2,
	}})
	if m.viewKey != edited {
		t.Fatalf("the content editor must pin viewKey to the edited issue across a reload, got %q want %q", m.viewKey, edited)
	}
	m, _ = m.Update(contentSubmittedMsg{content: "## changed"})
	if d := m.details[edited]; d.Content != "## changed" {
		t.Errorf("the save must land on the edited issue %q, got content %q", edited, d.Content)
	}
}

// #7: 'E' before the detail loads waits (a toast) rather than opening an empty editor.
func TestContentEditorWaitsForDetail(t *testing.T) {
	m := loaded(t, 120, 44, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1})
	m, _ = m.Update(press("enter")) // detail still loading
	m, cmd := m.Update(press("E"))
	if m.editingContent {
		t.Error("E must not open the content editor before the detail loads")
	}
	if cmd == nil {
		t.Error("E during load should surface a toast")
	}
}

// #7: clicking the Content section pen opens the content editor (the keyboard route is E).
func TestContentPenOpensEditor(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T", Content: "# body",
	}})
	m, _ = m.Update(clickZone(t, m, zoneEditContent))
	if !m.editingContent {
		t.Fatal("clicking the Content pen should open the content editor")
	}
}
