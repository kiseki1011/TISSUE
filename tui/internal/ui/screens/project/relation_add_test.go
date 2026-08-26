package project

import (
	"errors"
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
)

// relationReady seeds a detail with an existing "Blocks" link to ENG-9, for the exclusion cases.
func relationReady(t *testing.T) Model {
	t.Helper()
	m := editReady(t, domain.IssueDetail{
		Key: "ENG-1", Title: "Root", StateLabel: "Active", StateCategory: "ACTIVE",
		Relations: []domain.IssueRelationGroup{
			{Kind: "Blocks", Items: []domain.RelatedIssue{{Key: "ENG-9", Title: "Blocked", StateCategory: "ACTIVE"}}},
		},
	})
	return m
}

func TestRelationTypePickerOpens(t *testing.T) {
	m := relationReady(t)
	m, _ = m.Update(press("L"))
	if !m.picking || m.pickKind != pickRelationType {
		t.Fatalf("L should open the relation type picker, got picking=%v kind=%v", m.picking, m.pickKind)
	}
	view := plain(m.picker.View(m.deps.Styles))
	for _, want := range []string{"Relates to", "Blocks", "Causes", "Duplicates"} {
		if !strings.Contains(view, want) {
			t.Errorf("relation type picker missing %q:\n%s", want, view)
		}
	}
}

func TestRelationTypeSelectionLoadsCandidates(t *testing.T) {
	m := relationReady(t)
	m, _ = m.Update(press("L"))
	m.picker = m.picker.Move(1) // "Blocks"
	before := m.relGen

	m, cmd := m.pickSelect()

	if m.relPendingType != domain.RelationBlocks {
		t.Errorf("selected relation type not recorded: %q", m.relPendingType)
	}
	if m.picking {
		t.Error("the type picker should close before the target load")
	}
	if m.relGen <= before || cmd == nil {
		t.Errorf("selecting a type should bump relGen and load candidates: gen %d->%d cmd=%v", before, m.relGen, cmd != nil)
	}
}

// Excluding self and already-linked issues avoids an obvious 400.
func TestRelationCandidatesExcludeSelfAndLinked(t *testing.T) {
	m := relationReady(t)
	m.relPendingType = domain.RelationBlocks
	m.relSource = m.viewKey // ENG-1
	m.relGen = 7

	m, _ = m.Update(relationCandidatesLoadedMsg{gen: 7, source: m.viewKey, candidates: []domain.IssueSummary{
		{Key: "ENG-1", Title: "Root"},    // self
		{Key: "ENG-9", Title: "Blocked"}, // already linked
		{Key: "ENG-5", Title: "Free"},    // eligible
	}})

	if !m.picking || m.pickKind != pickRelationTarget {
		t.Fatalf("candidates should open the target picker, got picking=%v kind=%v", m.picking, m.pickKind)
	}
	view := plain(m.picker.View(m.deps.Styles))
	if !strings.Contains(view, "ENG-5") {
		t.Errorf("eligible issue ENG-5 should be offered:\n%s", view)
	}
	for _, excluded := range []string{"ENG-1", "ENG-9"} {
		if strings.Contains(view, excluded) {
			t.Errorf("%s should be excluded (self or already-linked):\n%s", excluded, view)
		}
	}
}

func TestRelationCandidatesStaleDropped(t *testing.T) {
	m := relationReady(t)
	m.relGen = 7
	m, cmd := m.Update(relationCandidatesLoadedMsg{gen: 6, candidates: []domain.IssueSummary{{Key: "ENG-5"}}})
	if m.picking || cmd != nil {
		t.Error("a stale candidate load should be a no-op")
	}
}

func TestRelationTargetAddsAndRefetches(t *testing.T) {
	m := relationReady(t)
	m.relPendingType = domain.RelationBlocks
	m.relSource = m.viewKey
	m, _ = m.Update(relationCandidatesLoadedMsg{gen: m.relGen, source: m.viewKey, candidates: []domain.IssueSummary{{Key: "ENG-5", Title: "Free"}}})
	if !m.picking || m.pickKind != pickRelationTarget {
		t.Fatalf("candidates should have opened the target picker: picking=%v kind=%v", m.picking, m.pickKind)
	}

	_, cmd := m.pickSelect() // select ENG-5
	if cmd == nil {
		t.Fatal("selecting a target should fire the add command")
	}

	m2, cmd2 := m.Update(relationDoneMsg{key: "ENG-1", err: false})
	if !m2.detailsPending["ENG-1"] || cmd2 == nil {
		t.Error("a successful relation add should refetch the source detail")
	}
}

// The add command resolves errText via errmsg: the server's reason, or connectivity for a transport error.
func TestRelationAddErrorText(t *testing.T) {
	dup := &domain.APIError{Status: 400, Detail: "A relation already exists between these two issues"}
	if got := errmsg.Message(dup, "Could not add the relation."); !strings.Contains(got, "already exists") {
		t.Errorf("a server reason should be surfaced, got %q", got)
	}
	if got := errmsg.Message(errors.New("dial tcp"), "Could not add the relation."); !strings.Contains(got, "reach the server") {
		t.Errorf("a transport error should read as connectivity, got %q", got)
	}
}

// Guards a silent wrong-source add when the cursor repoints m.viewKey mid-load.
func TestRelationHonorsSourceAcrossCursorMove(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: []domain.IssueSummary{
		{Key: "ENG-1", Title: "A", StateCategory: "ACTIVE"},
		{Key: "ENG-2", Title: "B", StateCategory: "ACTIVE"},
	}, TotalElements: 2})
	m.details["ENG-1"] = domain.IssueDetail{Key: "ENG-1", Title: "A"}
	m.details["ENG-2"] = domain.IssueDetail{Key: "ENG-2", Title: "B"}
	// stay on the list focus so a later "down" moves the cursor instead of scrolling Details

	m, _ = m.Update(press("L")) // relation type picker on ENG-1
	source := m.viewKey         // ENG-1
	m, cmd := m.pickSelect()    // choose "Relates to", firing the candidate load
	if m.relSource != source || cmd == nil {
		t.Fatalf("selecting a type should capture the source: relSource=%q", m.relSource)
	}

	// the cursor moves to ENG-2 before the candidates land, repointing viewKey
	m, _ = m.Update(press("down"))
	if m.viewKey != "ENG-2" {
		t.Fatalf("setup: cursor should have moved to ENG-2, got %q", m.viewKey)
	}

	// the stale candidate load (source ENG-1 != current viewKey ENG-2) must be dropped, not open a picker
	m, _ = m.Update(relationCandidatesLoadedMsg{gen: m.relGen, source: source, candidates: []domain.IssueSummary{{Key: "ENG-5"}}})
	if m.picking {
		t.Error("a candidate load whose source no longer matches the cursor must be dropped")
	}
}
