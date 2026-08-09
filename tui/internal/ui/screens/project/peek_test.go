package project

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// withChildDetail loads a page of one issue and gives that issue a cached detail with a clickable child,
// so the Details panel renders the child's peek link.
func withChildDetail(t *testing.T, childKey string) (Model, string) {
	t.Helper()
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	key := m.issues[0].Key
	m.details[key] = domain.IssueDetail{
		Key:      key,
		Title:    "Root issue",
		Children: []domain.IssueRef{{Key: childKey, StateCategory: "ACTIVE", TypeName: "Task"}},
	}
	return m, key
}

// A linked issue's key cell is a peek link only when it is clickable: not while a peek is already open
// (the reused body must render no nested links) and not with the mouse off.
func TestPeekLinkGating(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})

	if got := m.peekLink("c", "TIS-9", "X"); got == "X" {
		t.Error("a clickable linked issue should be zone-marked")
	}
	if got := m.peekLink("c", "", "X"); got != "X" {
		t.Error("an empty key must not be marked")
	}
	peeking := m
	peeking.peeking = true
	if got := peeking.peekLink("c", "TIS-9", "X"); got != "X" {
		t.Error("no nested peek links while a peek is open")
	}
}

// The peek body is read-only: the reused detail body renders none of its edit affordances.
func TestPeekSuppressesEditAffordances(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	live := m.replyLink(domain.IssueComment{ID: 5}, 0)
	m.peeking = true
	if m.penAction("z", "x", "x") != "" {
		t.Error("edit pens must be suppressed in the peek modal")
	}
	if m.addButton("z", "Child") != "" {
		t.Error("add buttons must be suppressed in the peek modal")
	}
	if live == "" {
		t.Fatal("setup: reply link should be present when not peeking")
	}
	if m.replyLink(domain.IssueComment{ID: 5}, 0) != "" {
		t.Error("comment reply links must be suppressed in the peek modal")
	}
}

// Clicking a child's key opens its read-only peek, targeting that child.
func TestPeekOpensFromChildClick(t *testing.T) {
	m, _ := withChildDetail(t, "TIS-CHILD")

	click := clickZone(t, m, peekZone("c", "TIS-CHILD"))
	m, _ = m.Update(click)

	if !m.peeking || m.peekKey != "TIS-CHILD" {
		t.Fatalf("child click did not open the peek: peeking=%v key=%q", m.peeking, m.peekKey)
	}
}

// esc closes the peek; while it is open the per-issue action keys are swallowed (it owns the keyboard).
func TestPeekEscClosesAndOwnsKeyboard(t *testing.T) {
	m, _ := withChildDetail(t, "TIS-CHILD")
	m, _ = m.Update(clickZone(t, m, peekZone("c", "TIS-CHILD")))

	m, _ = m.Update(press("e")) // would open the edit form if it leaked past the peek guard
	if !m.peeking || m.editing {
		t.Fatalf("peek did not own the keyboard: peeking=%v editing=%v", m.peeking, m.editing)
	}

	m, _ = m.Update(press("esc"))
	if m.peeking {
		t.Error("esc did not close the peek")
	}
}

// Opening a peek for an uncached issue starts a deduped load; a cached target opens without refetching.
func TestPeekLoadIsCacheAware(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})

	m, cmd := m.openPeek("TIS-COLD")
	if cmd == nil || !m.detailsPending["TIS-COLD"] {
		t.Errorf("peeking an uncached issue must start a load: cmd=%v pending=%v", cmd != nil, m.detailsPending["TIS-COLD"])
	}

	m.details["TIS-WARM"] = domain.IssueDetail{Key: "TIS-WARM"}
	_, cmd = m.openPeek("TIS-WARM")
	if cmd != nil {
		t.Error("peeking a cached issue must not refetch")
	}
}

// The peek reads from the shared detail cache: a load that lands while the peek is open fills the body.
func TestPeekShowsLandedDetail(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.openPeek("TIS-COLD")

	if got := plain(m.peekBody(80)); !strings.Contains(got, "Loading") && !strings.Contains(got, "…") {
		// the exact skeleton text may vary; the point is the body is not yet the real content
		if strings.Contains(got, "Landed title") {
			t.Fatal("peek showed content before the load landed")
		}
	}

	m, _ = m.Update(IssueDetailLoadedMsg{key: "TIS-COLD", gen: m.detailGen["TIS-COLD"], detail: domain.IssueDetail{Key: "TIS-COLD", Title: "Landed title"}})

	if got := plain(m.peekBody(80)); !strings.Contains(got, "Landed title") {
		t.Errorf("peek did not render the landed detail:\n%s", got)
	}
}

// A failed peek load offers an in-place retry: the body shows the hint and R re-fires the load (adversarial
// review finding - the modal was a dead-end with no retry).
func TestPeekFailedLoadRetries(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.openPeek("TIS-COLD")
	m, _ = m.Update(IssueDetailLoadedMsg{key: "TIS-COLD", gen: m.detailGen["TIS-COLD"], err: true})

	if !m.detailsFailed["TIS-COLD"] {
		t.Fatal("setup: the peek load should have failed")
	}
	if got := plain(m.peekBody(80)); !strings.Contains(got, "retry") {
		t.Errorf("failed peek body should show a retry hint:\n%s", got)
	}
	m, cmd := m.Update(press("R"))
	if cmd == nil || !m.detailsPending["TIS-COLD"] {
		t.Error("R did not re-fire the failed peek load")
	}
	if !m.peeking {
		t.Error("retry should keep the peek open")
	}
}

// A resize while peeking re-clamps peekScroll, so the next scroll key is not eaten as a dead press
// (adversarial review finding - the WindowSizeMsg clamp omitted peekScroll).
func TestPeekScrollReclampedOnResize(t *testing.T) {
	m := loaded(t, 120, 12, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	// a peeked issue whose body overflows a short terminal
	m.details["TIS-BIG"] = domain.IssueDetail{Key: "TIS-BIG", Title: "Big", Content: strings.Repeat("line\n\n", 40)}
	m, _ = m.openPeek("TIS-BIG")
	m.peekScroll = m.peekScrollMax()
	if m.peekScroll == 0 {
		t.Fatal("setup: the peek should overflow so it can scroll")
	}

	m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 60}) // grow the terminal: max shrinks
	if m.peekScroll > m.peekScrollMax() {
		t.Errorf("peekScroll %d left stale above the new max %d after resize", m.peekScroll, m.peekScrollMax())
	}
}

// The same issue key in two sections (a parent that is also a relation target) marks distinct zones, so
// the higher link is not a dead click (adversarial review finding - shared zone id).
func TestPeekZoneDistinctPerSection(t *testing.T) {
	if peekZone("p", "TIS-DUP") == peekZone("r", "TIS-DUP") {
		t.Error("a key in two sections must map to distinct peek zones")
	}
}

// A hovered issue link takes the focus Accent; at rest it keeps its base colour, and a read-only peek's
// own links never highlight.
func TestPeekKeyColorHover(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	th := m.deps.Styles.Theme

	if got := m.peekKeyColor("c", "K", th.Primary); got != th.Primary {
		t.Error("an un-hovered link should keep its base colour")
	}
	hov := m
	hov.hover = peekZone("c", "K")
	if got := hov.peekKeyColor("c", "K", th.Primary); got != th.Accent {
		t.Error("a hovered link should take the focus Accent")
	}
	hov.peeking = true
	if got := hov.peekKeyColor("c", "K", th.Primary); got != th.Primary {
		t.Error("a peek is read-only, so its own links must not highlight")
	}
}

// Details affordances (edit pens and "+ …" buttons) use the focus Accent on hover, not Secondary.
func TestAffordColorHover(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	th := m.deps.Styles.Theme

	if got := m.affordColor(zoneAddRelation); got != th.Primary {
		t.Error("an un-hovered affordance should be Primary")
	}
	m.hover = zoneAddRelation
	if got := m.affordColor(zoneAddRelation); got != th.Accent {
		t.Error("a hovered affordance should use the focus Accent")
	}
	if th.Accent == th.Secondary {
		t.Skip("theme Accent and Secondary coincide; cannot assert the switch")
	}
	if got := m.affordColor(zoneAddRelation); got == th.Secondary {
		t.Error("hover must no longer use Secondary")
	}
}
