package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

func openDetailOn(t *testing.T, w, h int, page domain.IssuePage) Model {
	t.Helper()
	m := loaded(t, w, h, page)
	m, _ = m.Update(press("enter")) // focus the selected row's Details panel
	return m
}

func TestEnterFocusesDetail(t *testing.T) {
	m := loaded(t, 120, 40, domain.IssuePage{Issues: issues(3), TotalElements: 3})
	if m.viewKey == "" {
		t.Error("the panel should target the selected issue after a load")
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("selecting an issue should start a detail fetch")
	}
	m, _ = m.Update(press("enter"))
	if m.focus != focusDetail {
		t.Fatal("enter should focus the Details panel")
	}
	if !m.CapturingInput() {
		t.Error("a focused Details panel should capture input")
	}
}

func TestDetailSkeletonWhileLoading(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(2), TotalElements: 2})
	if body := plain(m.View()); !strings.Contains(body, "Loading issue") {
		t.Errorf("an unloaded detail should render the skeleton:\n%s", body)
	}
}

func TestDetailLoadedRendersContent(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Fix the redirect loop", StateLabel: "In Progress", StateCategory: "ACTIVE",
		Priority: "P1", AssigneeName: "Hong Gildong", Content: "The full body.",
	}})
	body := plain(m.View())
	for _, want := range []string{"Fix the redirect loop", "In Progress", "Hong Gildong", "The full body.", "Content"} {
		if !strings.Contains(body, want) {
			t.Errorf("detail body missing %q:\n%s", want, body)
		}
	}
}

func TestDetailCacheSkipsRefetch(t *testing.T) {
	m := openDetailOn(t, 120, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "Cached"}})
	m, _ = m.Update(press("esc"))
	if m.focus != focusList {
		t.Fatal("esc should return focus to the list")
	}
	m, cmd := m.Update(press("enter")) // re-focus the same (cached) issue
	if m.focus != focusDetail {
		t.Fatal("enter should focus the Details panel")
	}
	if cmd != nil {
		t.Error("a cached issue should not trigger another fetch")
	}
}

func TestDetailFailedShowsRetry(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], err: true})
	if body := plain(m.View()); !strings.Contains(body, "Failed to load") {
		t.Errorf("a failed detail should surface the error:\n%s", body)
	}
	_, cmd := m.Update(press("R"))
	if cmd == nil {
		t.Error("R should retry a failed detail load")
	}
}

// A lone carriage return would reset the cursor to column 0 and corrupt the frame.
func TestDetailSanitizesControlChars(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "Line one\rstill line one", Content: "First line\r\nsecond\rthird\nfourth",
	}})
	body := plain(m.View())
	if strings.ContainsRune(body, '\r') {
		t.Error("the rendered frame must not carry a carriage return")
	}
	if !strings.Contains(body, "First line") || !strings.Contains(body, "fourth") {
		t.Errorf("real newlines should survive sanitization:\n%s", body)
	}
}

// A meta value longer than the value column is clipped, not wrapped to column 0.
func TestDetailMetaValueClipped(t *testing.T) {
	m := openDetailOn(t, 80, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	longName := strings.Repeat("verylongname ", 12)
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T", AssigneeName: longName,
	}})
	for _, line := range strings.Split(plain(m.View()), "\n") {
		if lipgloss.Width(line) > m.width {
			t.Errorf("no line should exceed the terminal width, got %d:\n%q", lipgloss.Width(line), line)
		}
	}
}

// Regression: an out-of-order detail load must not revert the cache (missing generation guard).
func TestDetailStaleLoadDropped(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, StateLabel: "New"}})

	staleGen := m.detailGen[m.viewKey]
	m, _ = m.Update(press("R")) // a refetch bumps the generation
	freshGen := m.detailGen[m.viewKey]
	if freshGen == staleGen {
		t.Fatal("a refetch should bump the detail generation")
	}

	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: staleGen, detail: domain.IssueDetail{Key: m.viewKey, StateLabel: "Stale"}})
	if got := m.details[m.viewKey].StateLabel; got == "Stale" {
		t.Errorf("a superseded detail load overwrote the cache: %q", got)
	}
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: freshGen, detail: domain.IssueDetail{Key: m.viewKey, StateLabel: "Fresh"}})
	if got := m.details[m.viewKey].StateLabel; got != "Fresh" {
		t.Errorf("the current detail load should apply, got %q", got)
	}
}

func TestDetailScrolls(t *testing.T) {
	m := openDetailOn(t, 100, 12, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	long := strings.Repeat("A paragraph of body text.\n", 60)
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "T", Content: long}})
	if m.detailScrollMax() == 0 {
		t.Fatal("a long detail should be scrollable at this height")
	}
	m, _ = m.Update(press("down"))
	if m.detailScroll != 1 {
		t.Errorf("down should scroll by one, got %d", m.detailScroll)
	}
	m, _ = m.Update(press("end"))
	if m.detailScroll != m.detailScrollMax() {
		t.Errorf("end should jump to the bottom, got %d want %d", m.detailScroll, m.detailScrollMax())
	}
	m, _ = m.Update(press("up"))
	if m.detailScroll != m.detailScrollMax()-1 {
		t.Errorf("up should scroll back by one, got %d", m.detailScroll)
	}
}

// Regression: a background reload must not repoint viewKey while a form is open (cross-issue write).
func TestSelectionPinnedWhileActing(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: []domain.IssueSummary{
		{Key: "ENG-1", Title: "A", StateCategory: "ACTIVE"}, {Key: "ENG-2", Title: "B", StateCategory: "ACTIVE"},
	}, TotalElements: 2})
	m, _ = m.Update(IssueDetailLoadedMsg{key: "ENG-1", gen: m.detailGen["ENG-1"], detail: domain.IssueDetail{Key: "ENG-1", Title: "A"}})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(press("e"))
	if !m.editing || m.viewKey != "ENG-1" {
		t.Fatalf("precondition: editing ENG-1, got editing=%v viewKey=%q", m.editing, m.viewKey)
	}
	// a background reload puts a different issue at row 0
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, page: domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-9", Title: "Z", StateCategory: "ACTIVE"}}, TotalElements: 1,
	}})
	if m.viewKey != "ENG-1" {
		t.Errorf("viewKey must stay pinned to the edited issue while a form is open, got %q", m.viewKey)
	}
}

// Regression: an unbounded comment author name spilled the narrow modal past the terminal width.
func TestNarrowModalWrapsLongCommentAuthor(t *testing.T) {
	m := openDetailOn(t, 100, 30, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	longAuthor := strings.Repeat("verylongauthorname", 10) // far past the 100-column terminal
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T", CommentCount: 1,
		Comments: []domain.IssueComment{{AuthorName: longAuthor, Content: "body"}},
	}})
	for _, line := range strings.Split(plain(m.View()), "\n") {
		if lipgloss.Width(line) > m.width {
			t.Errorf("no line may exceed the terminal width, got %d:\n%q", lipgloss.Width(line), line)
		}
	}
}

// Regression: a refetch that shortens the detail must re-clamp the scroll offset.
func TestDetailScrollReclampedOnShrink(t *testing.T) {
	m := openDetailOn(t, 100, 12, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	long := strings.Repeat("a line of body text\n", 80)
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "T", Content: long}})
	m, _ = m.Update(press("end")) // scroll to the old bottom
	if m.detailScroll == 0 {
		t.Fatal("precondition: the long detail should be scrolled off zero")
	}
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "T", Content: "short"}})
	if m.detailScroll > m.detailScrollMax() {
		t.Errorf("scroll offset %d exceeds the new max %d after the content shrank (not re-clamped)", m.detailScroll, m.detailScrollMax())
	}
}

// Narrow Tab must skip focusDetail (there it means "modal open"), so tabbing cannot pop the modal.
func TestTabRingSkipsDetailByLayout(t *testing.T) {
	narrow := loaded(t, 100, 40, domain.IssuePage{Issues: issues(2), TotalElements: 2})
	narrow, _ = narrow.Update(press("tab")) // list -> search
	narrow, _ = narrow.Update(press("tab")) // search -> filter
	narrow, _ = narrow.Update(press("tab")) // filter -> (skip detail) -> list
	if narrow.focus != focusList {
		t.Errorf("the narrow Tab ring should skip focusDetail and land on the list, got %v", narrow.focus)
	}

	wide := loaded(t, 160, 40, domain.IssuePage{Issues: issues(2), TotalElements: 2})
	wide, _ = wide.Update(press("tab")) // list -> search
	wide, _ = wide.Update(press("tab")) // search -> filter
	wide, _ = wide.Update(press("tab")) // filter -> detail
	if wide.focus != focusDetail {
		t.Errorf("the wide Tab ring should reach focusDetail (the side panel), got %v", wide.focus)
	}
}

// Wide always shows the side panel. Narrow has no room, so the detail only appears as a modal.
func TestNarrowDetailIsModalOnly(t *testing.T) {
	page := domain.IssuePage{Issues: issues(1), TotalElements: 1}

	wide := loaded(t, 160, 40, page)
	if !strings.Contains(plain(wide.View()), "Loading issue") {
		t.Error("the wide layout should always show the Details panel (skeleton before focus)")
	}

	narrow := loaded(t, 100, 40, page)
	if strings.Contains(plain(narrow.View()), "Loading issue") {
		t.Error("the narrow layout should not show the detail before the modal is opened")
	}
	narrow, _ = narrow.Update(press("enter"))
	if narrow.focus != focusDetail {
		t.Fatal("enter should open/focus the detail")
	}
	if !strings.Contains(plain(narrow.View()), "Loading issue") {
		t.Error("the narrow read-only modal should show the detail once opened")
	}
}

func TestNarrowModalEscClosesToList(t *testing.T) {
	m := loaded(t, 100, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(press("enter"))
	if m.focus != focusDetail {
		t.Fatal("enter should open the modal")
	}
	m, _ = m.Update(press("esc"))
	if m.focus != focusList {
		t.Fatal("esc should close the modal back to the list")
	}
	if strings.Contains(plain(m.View()), "Loading issue") {
		t.Error("closing the modal should hide the detail again")
	}
}

// A narrow row click opens the modal (no side panel to reveal). A wide one keeps focus on the list.
func TestRowClickFocusByLayout(t *testing.T) {
	narrow := loaded(t, 100, 40, domain.IssuePage{Issues: issues(3), TotalElements: 3})
	narrow, _ = narrow.Update(clickZone(t, narrow, issueRowZone(1)))
	if narrow.cursor != 1 {
		t.Errorf("a row click should select that row, got cursor=%d", narrow.cursor)
	}
	if narrow.focus != focusDetail {
		t.Errorf("a narrow row click should open the detail modal, got focus=%v", narrow.focus)
	}

	wide := loaded(t, 160, 40, domain.IssuePage{Issues: issues(3), TotalElements: 3})
	wide, _ = wide.Update(clickZone(t, wide, issueRowZone(1)))
	if wide.cursor != 1 {
		t.Errorf("a row click should select that row, got cursor=%d", wide.cursor)
	}
	if wide.focus != focusList {
		t.Errorf("a wide row click should keep focus on the list, got focus=%v", wide.focus)
	}
}

// Regression: with no selection, c and d must be inert rather than firing on an empty key.
func TestActionsGuardedOnEmptySelection(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: nil, TotalElements: 0})
	if m.viewKey != "" {
		t.Fatalf("precondition: no selection, got viewKey=%q", m.viewKey)
	}
	m, _ = m.Update(press("tab")) // list -> search
	m, _ = m.Update(press("tab")) // search -> filter
	m, _ = m.Update(press("tab")) // filter -> detail (no selection)
	if m.focus != focusDetail {
		t.Fatalf("precondition: focus should reach the panel, got %v", m.focus)
	}
	mc, ccmd := m.Update(press("c"))
	if mc.commenting {
		t.Error("c must not open the composer with no issue selected")
	}
	if ts, ok := toastFrom(ccmd); !ok || ts.Level != toast.Info {
		t.Errorf("c on an empty selection should info-toast, got %+v ok=%v", ts, ok)
	}
	md, dcmd := m.Update(press("d"))
	if md.deleting {
		t.Error("d must not open the delete dialog with no issue selected")
	}
	if ts, ok := toastFrom(dcmd); !ok || ts.Level != toast.Info {
		t.Errorf("d on an empty selection should info-toast, got %+v ok=%v", ts, ok)
	}
}
