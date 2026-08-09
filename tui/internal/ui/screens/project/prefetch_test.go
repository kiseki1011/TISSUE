package project

import (
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// A settled cursor warms the detail cache for the rows within prefetchSpan on each side, leaving the
// cursor's own row (loaded via selection) and rows outside the span untouched.
func TestPrefetchWarmsNeighbors(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(9), TotalElements: 9})
	m.cursor = 4 // park in the middle so the span reaches both ways

	m, _ = m.Update(prefetchDebounceMsg{seq: m.prefetchSeq})

	for _, i := range []int{2, 3, 5, 6} { // cursor ± {1,2}
		if !m.detailsPending[m.issues[i].Key] {
			t.Errorf("neighbor row %d (%s) was not prefetched", i, m.issues[i].Key)
		}
	}
	if m.detailsPending[m.issues[4].Key] {
		t.Error("the cursor's own row must be loaded by selection, not by the neighbor prefetch")
	}
	for _, i := range []int{7, 8} { // outside the span
		if m.detailsPending[m.issues[i].Key] {
			t.Errorf("row %d is outside prefetchSpan and must not be prefetched", i)
		}
	}
}

// A prefetch tick from an earlier cursor stop is dropped: only the latest generation prefetches, so a
// fast scroll does not fire a warm at every row it passed through.
func TestPrefetchStaleSeqIgnored(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(9), TotalElements: 9})
	m.cursor = 4

	m, cmd := m.Update(prefetchDebounceMsg{seq: m.prefetchSeq - 1}) // a superseded tick

	if cmd != nil {
		t.Error("a stale prefetch tick should be a no-op")
	}
	for _, i := range []int{2, 3, 5, 6} {
		if m.detailsPending[m.issues[i].Key] {
			t.Errorf("stale tick still prefetched neighbor row %d", i)
		}
	}
}

// Prefetch reuses the detail cache's dedupe: a neighbor already cached (or in flight) is skipped, while
// a cold neighbor gets a fresh load (its generation is bumped).
func TestPrefetchDedupesCachedNeighbor(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(9), TotalElements: 9})
	m.cursor = 4

	cached := m.issues[3].Key // an already-cached neighbor
	m.details[cached] = domain.IssueDetail{Key: cached}
	genBefore := m.detailGen[cached]

	cold := m.issues[5].Key // a cold neighbor
	if m.detailGen[cold] != 0 {
		t.Fatalf("cold neighbor should start at generation 0, got %d", m.detailGen[cold])
	}

	m, _ = m.Update(prefetchDebounceMsg{seq: m.prefetchSeq})

	if m.detailsPending[cached] {
		t.Error("a cached neighbor must not be re-fetched")
	}
	if m.detailGen[cached] != genBefore {
		t.Errorf("cached neighbor's generation moved (%d -> %d); it was re-loaded", genBefore, m.detailGen[cached])
	}
	if !m.detailsPending[cold] || m.detailGen[cold] != 1 {
		t.Errorf("cold neighbor was not loaded: pending=%v gen=%d", m.detailsPending[cold], m.detailGen[cold])
	}
}

// At a load-more boundary the newly appended rows within span are warmed immediately: syncSelection
// no-ops on the unchanged selection, so the append branch must prefetch them directly - otherwise
// scrolling onto the next page shows the skeleton flash the feature exists to prevent (adversarial
// review finding, confirmed by two lenses).
func TestPrefetchWarmsAppendedRows(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(3), TotalElements: 6, HasNext: true})
	m, _ = m.Update(press("down")) // cursor 0 -> 1
	m, _ = m.Update(press("down")) // cursor 1 -> 2 (the last loaded row)
	if m.cursor != 2 {
		t.Fatalf("setup: cursor should rest at the last row, got %d", m.cursor)
	}

	more := []domain.IssueSummary{
		{Key: "MORE-1", Title: "m1", StateCategory: "ACTIVE"},
		{Key: "MORE-2", Title: "m2", StateCategory: "ACTIVE"},
		{Key: "MORE-3", Title: "m3", StateCategory: "ACTIVE"},
	}
	m, _ = m.Update(issuesLoadedMsg{key: testKey, gen: m.reqGen, append: true, page: domain.IssuePage{Issues: more}})

	if m.cursor != 2 {
		t.Fatalf("append should not move the resting cursor, got %d", m.cursor)
	}
	for _, key := range []string{"MORE-1", "MORE-2"} { // cursor+1, cursor+2 - just appended, within span
		if !m.detailsPending[key] {
			t.Errorf("appended in-span row %s was not prefetched at the load-more boundary", key)
		}
	}
	if m.detailsPending["MORE-3"] { // cursor+3, outside span
		t.Error("row outside prefetchSpan must not be prefetched on append")
	}
}

// Scrolling to a new row arms the debounce: the prefetch generation advances and a tick command is
// scheduled, so the warm fires once the cursor settles.
func TestScrollArmsPrefetch(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(9), TotalElements: 9})
	before := m.prefetchSeq

	m, cmd := m.Update(press("down"))

	if m.cursor != 1 {
		t.Fatalf("down did not move the cursor: %d", m.cursor)
	}
	if m.prefetchSeq <= before {
		t.Errorf("moving to a new row did not arm the prefetch debounce: %d -> %d", before, m.prefetchSeq)
	}
	if cmd == nil {
		t.Error("moving the cursor did not schedule the debounce tick")
	}
}
