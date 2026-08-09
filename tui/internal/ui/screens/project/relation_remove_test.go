package project

import (
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// relationModel loads TIS-1 with the given relation groups and the Details panel open on it.
func relationModel(t *testing.T, groups []domain.IssueRelationGroup) Model {
	t.Helper()
	m := loaded(t, 120, 40, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "TIS-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "X", Relations: groups,
	}})
	return m
}

func outgoing(kind string, keys ...string) domain.IssueRelationGroup {
	g := domain.IssueRelationGroup{Kind: kind, Removable: true}
	for _, k := range keys {
		g.Items = append(g.Items, domain.RelatedIssue{Key: k, Title: "T " + k})
	}
	return g
}

func inverse(kind string, keys ...string) domain.IssueRelationGroup {
	g := outgoing(kind, keys...)
	g.Removable = false
	return g
}

func TestUnlinkPickerListsRemovableRelations(t *testing.T) {
	m := relationModel(t, []domain.IssueRelationGroup{
		outgoing("Blocks", "TIS-2"),
		outgoing("Related to", "TIS-3"),
	})
	m, _ = m.Update(press("U"))

	if !m.picking || m.pickKind != pickRelationRemove {
		t.Fatalf("U should open the unlink picker (picking=%v kind=%v)", m.picking, m.pickKind)
	}
	if m.relSource != "TIS-1" {
		t.Errorf("the source should be pinned to the viewed issue, got %q", m.relSource)
	}
	view := plain(m.picker.View(m.deps.Styles))
	for _, want := range []string{"Blocks", "TIS-2", "Related to", "TIS-3"} {
		if !strings.Contains(view, want) {
			t.Errorf("the picker should list %q:\n%s", want, view)
		}
	}
}

// A directional relation lives on its source, so its inverse cannot be dropped from this side: offering
// it would produce RELATION_NOT_FOUND against a link the user can plainly see.
func TestUnlinkSkipsInverseDirectionalRelations(t *testing.T) {
	m := relationModel(t, []domain.IssueRelationGroup{
		inverse("Blocked by", "TIS-9"),
		outgoing("Blocks", "TIS-2"),
	})

	got := m.removableRelations()
	if len(got) != 1 || got[0].targetKey != "TIS-2" {
		t.Errorf("only the outgoing relation should be removable, got %+v", got)
	}
}

// When every link belongs to the other side, say so rather than claiming there are none.
func TestUnlinkExplainsWhenOnlyInverseRelationsExist(t *testing.T) {
	m := relationModel(t, []domain.IssueRelationGroup{inverse("Blocked by", "TIS-9")})
	if !m.inverseOnlyRelations() {
		t.Fatal("an inverse-only issue should be recognised as such")
	}
	m, cmd := m.Update(press("U"))
	if m.picking {
		t.Error("there is nothing this issue can unlink, so no picker should open")
	}
	if cmd == nil {
		t.Fatal("the refusal should explain itself")
	}
}

func TestUnlinkRefusedWithNoRelations(t *testing.T) {
	m := relationModel(t, nil)
	m, _ = m.Update(press("U"))
	if m.picking {
		t.Error("an issue with no relations should not open the unlink picker")
	}
	if m.inverseOnlyRelations() {
		t.Error("no relations at all is not the inverse-only case")
	}
}

// A peeked issue is read-only, so it offers no unlink.
func TestUnlinkNotOfferedWhilePeeking(t *testing.T) {
	m := relationModel(t, []domain.IssueRelationGroup{outgoing("Blocks", "TIS-2")})
	m.peeking = true
	if got := m.removableRelations(); len(got) != 0 {
		t.Errorf("a peeked issue should offer nothing to unlink, got %+v", got)
	}
}

func TestUnlinkHintOnlyWhenSomethingIsRemovable(t *testing.T) {
	with := relationModel(t, []domain.IssueRelationGroup{outgoing("Blocks", "TIS-2")})
	if !hasKey(with.HelpKeys(), "U") {
		t.Error("the footer should offer U when a relation can be dropped")
	}

	without := relationModel(t, []domain.IssueRelationGroup{inverse("Blocked by", "TIS-9")})
	if hasKey(without.HelpKeys(), "U") {
		t.Error("the footer should not advertise U when nothing here can be dropped")
	}
}

func TestUnlinkSelectionFiresTheRemoval(t *testing.T) {
	m := relationModel(t, []domain.IssueRelationGroup{outgoing("Blocks", "TIS-2")})
	m, _ = m.Update(press("U"))
	m, cmd := m.Update(press("enter"))

	if m.picking {
		t.Error("choosing should close the picker")
	}
	if cmd == nil {
		t.Fatal("choosing should fire the remove command")
	}
}

// The add and remove paths share one result message, so the copy must not claim the wrong action.
func TestRelationToastNamesTheAction(t *testing.T) {
	m := relationModel(t, []domain.IssueRelationGroup{outgoing("Blocks", "TIS-2")})

	_, cmd := m.Update(relationDoneMsg{key: "TIS-1", removed: true})
	ts, ok := toastFrom(cmd)
	if !ok {
		t.Fatal("a successful removal should toast")
	}
	if ts.Text != "Relation removed." {
		t.Errorf("toast = %q, want \"Relation removed.\"", ts.Text)
	}

	_, cmd = m.Update(relationDoneMsg{key: "TIS-1"})
	if ts, ok := toastFrom(cmd); !ok || ts.Text != "Relation added." {
		t.Errorf("the add path should still say added, got %q", ts.Text)
	}
}
