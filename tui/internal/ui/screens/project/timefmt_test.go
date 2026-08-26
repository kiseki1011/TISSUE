package project

import (
	"strings"
	"testing"
	"time"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// withLocalZone pins time.Local for the rest of the test so timezone-dependent formatting can be
// asserted identically on any machine - without it these tests would only be meaningful when the
// developer (or CI) happens to sit in the right offset. No test in this package runs in parallel, so
// the global is safe to swap. Restores on cleanup, LIFO, so repeated calls unwind correctly.
func withLocalZone(t *testing.T, offsetHours int) {
	t.Helper()
	prev := time.Local
	time.Local = time.FixedZone("TEST", offsetHours*3600)
	t.Cleanup(func() { time.Local = prev })
}

// An instant is shown as the day it falls on where the viewer sits, not the day it falls on in UTC.
func TestFormatLocalDayUsesViewerZone(t *testing.T) {
	// 23:30Z is already the next day in Seoul (+09) while still the same day in New York (-05)
	inst := time.Date(2026, 8, 23, 23, 30, 0, 0, time.UTC)

	withLocalZone(t, 9)
	if got := formatLocalDay(inst); got != "2026-08-24" {
		t.Errorf("a +09 viewer should see the 24th, got %s", got)
	}
	withLocalZone(t, -5)
	if got := formatLocalDay(inst); got != "2026-08-23" {
		t.Errorf("a -05 viewer should see the 23rd, got %s", got)
	}
}

// A due date rides the wire as UTC midnight of the chosen day and must read as that day everywhere:
// localizing it west of Greenwich would land on the day before, quietly moving every deadline.
func TestFormatDateOnlyIsZoneInvariant(t *testing.T) {
	due := time.Date(2026, 8, 30, 0, 0, 0, 0, time.UTC)
	for _, off := range []int{9, 0, -5, -11} {
		withLocalZone(t, off)
		if got := formatDateOnly(due); got != "2026-08-30" {
			t.Errorf("offset %+d: a due date must not shift, got %s", off, got)
		}
	}
}

// The relative form carries a wall clock, so it must be the viewer's wall clock - the case that made
// this worth fixing is a Seoul morning reading back as "yesterday at 23:30".
func TestFormatRelativeUsesLocalWallClock(t *testing.T) {
	withLocalZone(t, 9)
	// far enough back to miss the today/yesterday branches, so the absolute form is what gets asserted
	base := time.Now().UTC().AddDate(0, 0, -20)
	inst := time.Date(base.Year(), base.Month(), base.Day(), 23, 30, 0, 0, time.UTC)

	got := formatRelative(inst)
	want := inst.AddDate(0, 0, 1).Format("2006-01-02") + " 08:30"
	if got != want {
		t.Errorf("expected the +09 wall clock %q, got %q", want, got)
	}
	if strings.Contains(got, "23:30") {
		t.Errorf("the UTC clock leaked into the display: %s", got)
	}
}

// The two helpers are not interchangeable, and the detail panel is where a mix-up would show. Under a
// negative offset each classification produces a different day from the other, so a swapped call site
// fails here rather than shipping a deadline that drifted a day.
func TestDetailDueAndCreatedUseTheRightClock(t *testing.T) {
	withLocalZone(t, -5)
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key:   m.viewKey,
		Title: "Wire it up",
		// a due date: UTC midnight, and localizing it would read 2026-08-29
		DueAt: time.Date(2026, 8, 30, 0, 0, 0, 0, time.UTC),
		// an instant: UTC calls this the 24th, but a -05 viewer was still on the 23rd
		CreatedAt: time.Date(2026, 8, 24, 2, 0, 0, 0, time.UTC),
	}})
	body := plain(m.View())
	if !strings.Contains(body, "2026-08-30") {
		t.Errorf("the due date should hold its chosen day:\n%s", body)
	}
	if !strings.Contains(body, "2026-08-23") {
		t.Errorf("created should read as the viewer's day:\n%s", body)
	}
	if strings.Contains(body, "2026-08-29") || strings.Contains(body, "2026-08-24") {
		t.Errorf("a date was rendered with the wrong clock:\n%s", body)
	}
}
