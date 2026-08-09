package project

import (
	"strings"
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// editReady opens the detail modal on ENG-1 and loads it with the given detail.
func editReady(t *testing.T, det domain.IssueDetail) Model {
	t.Helper()
	m := loaded(t, 120, 44, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: det.Title, StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m, _ = m.Update(press("enter"))
	det.Key = m.viewKey
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: det})
	return m
}

func sampleDetail() domain.IssueDetail {
	return domain.IssueDetail{
		Title: "Fix login", Summary: "short", Content: "# body", Priority: "P2",
		DueAt: time.Date(2026, 8, 15, 0, 0, 0, 0, time.UTC),
	}
}

// 'e' opens the edit form prefilled from the loaded detail.
func TestEditOpensPrefilled(t *testing.T) {
	m := editReady(t, sampleDetail())
	m, cmd := m.Update(press("e"))
	if !m.editing {
		t.Fatal("e should open the edit form")
	}
	if cmd == nil {
		t.Error("opening the edit form should start the input blink")
	}
	f := m.editUI
	if f.title.Value() != "Fix login" {
		t.Errorf("title not prefilled: %q", f.title.Value())
	}
	if filterPriorities[f.priorityIx] != "P2" {
		t.Errorf("priority not prefilled, got %q", filterPriorities[f.priorityIx])
	}
	if !f.dueSet || formatDateOnly(f.dueAt) != "2026-08-15" {
		t.Errorf("due not prefilled, got set=%v %q", f.dueSet, formatDateOnly(f.dueAt))
	}
	if body := plain(m.View()); !strings.Contains(body, "Edit issue") || !strings.Contains(body, "Fix login") {
		t.Errorf("edit form not rendered over the modal:\n%s", body)
	}
}

// Pressing 'e' before the detail loads reports it is loading rather than opening an empty form.
func TestEditWaitsForDetail(t *testing.T) {
	m := loaded(t, 120, 44, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1})
	m, _ = m.Update(press("enter")) // opens the modal but the detail is still loading (skeleton)
	m, cmd := m.Update(press("e"))
	if m.editing {
		t.Error("the edit form must not open before the detail loads")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Info {
		t.Errorf("expected an info toast while the detail is loading, got %+v ok=%v", ts, ok)
	}
}

// Saving with nothing changed sends no request and reports "No changes".
func TestEditNoChanges(t *testing.T) {
	m := editReady(t, sampleDetail())
	m, _ = m.Update(press("e"))
	// submit the same values the form was prefilled with
	m, cmd := m.Update(editSubmittedMsg{v: editValues{
		title: "Fix login", priority: "P2",
		dueAt: time.Date(2026, 8, 15, 0, 0, 0, 0, time.UTC),
	}})
	if m.editing {
		t.Error("saving should close the edit form")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Info || !strings.Contains(ts.Text, "No changes") {
		t.Errorf("an unchanged save should report no changes, got %+v ok=%v", ts, ok)
	}
}

// Saving a change updates the cached detail (and its list row) at once, before the server confirms.
func TestEditIsOptimistic(t *testing.T) {
	m := editReady(t, sampleDetail())
	oldGen := m.detailGen[m.viewKey]
	m, _ = m.Update(press("e"))
	m, cmd := m.Update(editSubmittedMsg{v: editValues{
		title: "Renamed", priority: "P0",
		dueAt: time.Date(2026, 8, 15, 0, 0, 0, 0, time.UTC),
	}})
	if m.editing {
		t.Error("saving should close the edit form")
	}
	if cmd == nil {
		t.Fatal("a changed save should run an update")
	}
	d := m.details[m.viewKey]
	if d.Title != "Renamed" || d.Priority != "P0" {
		t.Errorf("edit should optimistically update the cached detail, got %q/%q", d.Title, d.Priority)
	}
	if m.issues[0].Title != "Renamed" || m.issues[0].Priority != "P0" {
		t.Errorf("edit should optimistically patch the list row, got %q/%q", m.issues[0].Title, m.issues[0].Priority)
	}
	if m.detailGen[m.viewKey] == oldGen {
		t.Error("an optimistic edit should bump the detail generation")
	}
}

// A still-in-flight earlier refetch (old generation) cannot clobber the optimistic edit.
func TestEditBumpsGenGuard(t *testing.T) {
	m := editReady(t, sampleDetail())
	oldGen := m.detailGen[m.viewKey]
	m, _ = m.Update(press("e"))
	m, _ = m.Update(editSubmittedMsg{v: editValues{title: "Renamed", priority: "P2"}})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: oldGen, detail: domain.IssueDetail{Key: m.viewKey, Title: "STALE"}})
	if m.details[m.viewKey].Title != "Renamed" {
		t.Errorf("a stale (old-gen) refetch must not clobber the optimistic edit, got %q", m.details[m.viewKey].Title)
	}
}

func TestEditDoneRefetches(t *testing.T) {
	m := editReady(t, sampleDetail())
	m, cmd := m.Update(EditDoneMsg{key: m.viewKey})
	if _, cached := m.details[m.viewKey]; !cached {
		t.Error("a successful edit should keep the cached detail (SWR) so the modal does not flash a skeleton")
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("a background refetch should be pending after an edit")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Success {
		t.Errorf("expected a success toast, got %+v ok=%v", ts, ok)
	}
}

func TestEditDoneError(t *testing.T) {
	m := editReady(t, sampleDetail())
	m, cmd := m.Update(EditDoneMsg{key: m.viewKey, err: true})
	if _, cached := m.details[m.viewKey]; cached {
		t.Error("a failed edit should evict the optimistic detail (never persist values the server rejected)")
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("a refetch should be pending to restore the true values")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Error {
		t.Errorf("expected an error toast, got %+v ok=%v", ts, ok)
	}
}

// esc from the edit form returns to the detail modal without closing it.
func TestEditCancel(t *testing.T) {
	m := editReady(t, sampleDetail())
	m, _ = m.Update(press("e"))
	m, _ = m.Update(press("esc")) // form emits cancelEditIssue
	m, _ = m.Update(editCancelledMsg{})
	if m.editing {
		t.Error("esc should cancel the edit form")
	}
	if m.focus != focusDetail {
		t.Error("cancelling the edit should keep the detail modal open")
	}
}

func TestDiffEditOnlyChanged(t *testing.T) {
	orig := sampleDetail()
	orig.Key = "ENG-1"
	edit := diffEdit(orig, editValues{
		title: "Renamed", priority: orig.Priority, dueAt: orig.DueAt,
	})
	if edit.Title == nil || *edit.Title != "Renamed" {
		t.Errorf("changed title should be included, got %+v", edit.Title)
	}
	if edit.Summary != nil || edit.Content != nil || edit.Priority != nil || edit.DueAt != nil || edit.ClearDue {
		t.Errorf("unchanged fields should be omitted, got %+v", edit)
	}
}

func TestDiffEditClearsDue(t *testing.T) {
	orig := sampleDetail()
	edit := diffEdit(orig, editValues{
		title: orig.Title, priority: orig.Priority, // dueAt zero
	})
	if !edit.ClearDue || edit.DueAt != nil {
		t.Errorf("emptying the due date should clear it, got %+v", edit)
	}
}

// Priority cycles with left/right when the priority row is focused.
func TestEditPriorityCycles(t *testing.T) {
	f := newEditForm(testDeps(), domain.IssueDetail{Priority: "P2"}, false)
	f, _ = f.focusOn(efPriority)
	f, _ = f.onKey(press("right"))
	if filterPriorities[f.priorityIx] != "P3" {
		t.Errorf("right should advance the priority, got %q", filterPriorities[f.priorityIx])
	}
	f, _ = f.onKey(press("left"))
	f, _ = f.onKey(press("left"))
	if filterPriorities[f.priorityIx] != "P1" {
		t.Errorf("left should step the priority back, got %q", filterPriorities[f.priorityIx])
	}
}

// Submitting an empty title keeps the form open with an error rather than sending the edit.
func TestEditTitleRequired(t *testing.T) {
	f := newEditForm(testDeps(), domain.IssueDetail{Title: "X", Priority: "P2"}, false)
	f.title.SetValue("")
	f, _ = f.submit()
	if f.titleErr == "" || f.focus != efTitle {
		t.Errorf("an empty title should raise a required-field error and focus the title, got err=%q focus=%d", f.titleErr, f.focus)
	}
}

// An issue with no due date opens with the Due field unset (not formatDate's "-"), and the edit still
// submits. Regression for the HIGH finding, carried across the picker migration.
func TestEditNoDuePrefillEmpty(t *testing.T) {
	m := editReady(t, domain.IssueDetail{Title: "X", Priority: "P2"}) // no due date
	m, _ = m.Update(press("e"))
	if m.editUI.dueSet {
		t.Error("a due-less issue should open with no due date set")
	}
	_, cmd := m.editUI.submit()
	if cmd == nil {
		t.Error("a valid edit should submit")
	}
}

// The save diffs against the snapshot the form opened with, so a background refetch that changed an
// untouched field mid-edit is not reverted. Regression for the MEDIUM finding.
func TestEditDiffAgainstOpenSnapshot(t *testing.T) {
	m := editReady(t, sampleDetail()) // priority P2
	m, _ = m.Update(press("e"))       // editBase = the P2 snapshot
	gen := m.detailGen[m.viewKey]
	// a concurrent refetch lands while the form is open, changing an untouched field to P0
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: gen, detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Fix login", Summary: "short", Content: "# body", Priority: "P0",
		DueAt: sampleDetail().DueAt,
	}})
	// the user changed only the title; the form still holds the open-time priority (P2)
	m, _ = m.Update(editSubmittedMsg{v: editValues{
		title: "Renamed", priority: "P2", dueAt: sampleDetail().DueAt,
	}})
	if got := m.details[m.viewKey].Priority; got != "P0" {
		t.Errorf("an untouched priority must not clobber the concurrent change, got %q", got)
	}
	if got := m.details[m.viewKey].Title; got != "Renamed" {
		t.Errorf("the touched title should still apply, got %q", got)
	}
}

// Opening and saving an unchanged issue whose stored title has surrounding whitespace is not an
// edit (the save trims, and the diff compares trimmed). Regression for the LOW trim finding.
func TestEditTrimNoSpuriousDiff(t *testing.T) {
	orig := domain.IssueDetail{Title: " Fix ", Content: "# body", Priority: "P2"}
	edit := diffEdit(orig, editValues{title: "Fix", priority: "P2"})
	if !edit.Empty() {
		t.Errorf("trimming an untouched title must not produce a diff, got %+v", edit)
	}
}

// The Due field is set from the calendar, not typed: setDue records a pick, and clearing it (set=false)
// drops the due date so the save clears it.
func TestEditDueSetAndClear(t *testing.T) {
	f := newEditForm(testDeps(), domain.IssueDetail{Title: "X", Priority: "P2"}, false)
	f = f.setDue(time.Date(2026, 9, 1, 0, 0, 0, 0, time.UTC), true)
	if !f.dueSet || formatDateOnly(f.dueAt) != "2026-09-01" {
		t.Errorf("setDue should record the pick, got set=%v %q", f.dueSet, formatDateOnly(f.dueAt))
	}
	f = f.setDue(time.Time{}, false)
	if f.dueSet {
		t.Error("clearing should unset the due date")
	}
	_, cmd := f.submit()
	if cmd == nil {
		t.Error("a cleared due date is still a valid submit")
	}
}
