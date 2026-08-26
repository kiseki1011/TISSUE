package project

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// createReady loads a project with an issue-type catalog so the New issue form can open.
func createReady(t *testing.T) Model {
	t.Helper()
	m := loaded(t, 170, 44, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "First", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m.types = []domain.IssueTypeSummary{{ID: 5, Name: "Story", Color: "ANSI_BLUE", Hierarchy: "STANDARD"}}
	m.typesLoaded = true
	return m
}

func TestCreateOpensFromKey(t *testing.T) {
	m := createReady(t)
	m, _ = m.Update(press("n"))
	if !m.creating {
		t.Fatal("n should open the create form")
	}
	body := plain(m.View())
	for _, want := range []string{"New issue", "Type", "Story", "Title", "Priority", "Create"} {
		if !strings.Contains(body, want) {
			t.Errorf("create form missing %q:\n%s", want, body)
		}
	}
}

func TestCreateRequiresTypes(t *testing.T) {
	m := loaded(t, 170, 44, domain.IssuePage{Issues: issues(1), TotalElements: 1}) // no types loaded
	m, _ = m.Update(press("n"))
	if m.creating {
		t.Error("create should refuse until the issue types load")
	}
}

func TestNewButtonOpensCreate(t *testing.T) {
	m := createReady(t)
	click := clickZone(t, m, zoneNew)
	m, _ = m.Update(click)
	if !m.creating {
		t.Error("clicking +New should open the create form")
	}
}

// Priority defaults to P2.
func TestCreateFormSubmitEmitsValues(t *testing.T) {
	f := newCreateForm(testDeps(), []domain.IssueTypeSummary{{ID: 5, Name: "Story"}})
	f.title.SetValue("New thing")
	f, _ = f.focusOn(nfCreate)
	_, cmd := f.onKey(press("enter"))
	if cmd == nil {
		t.Fatal("submit should emit a command")
	}
	sub, ok := cmd().(createSubmittedMsg)
	if !ok {
		t.Fatalf("expected createSubmittedMsg, got %T", cmd())
	}
	if sub.v.title != "New thing" || sub.v.typeID != 5 || sub.v.priority != "P2" {
		t.Errorf("wrong submitted values: %+v", sub.v)
	}
}

func TestCreateFormEmptyTitleBlocks(t *testing.T) {
	f := newCreateForm(testDeps(), []domain.IssueTypeSummary{{ID: 5, Name: "Story"}})
	f, _ = f.focusOn(nfCreate)
	f, cmd := f.onKey(press("enter"))
	if f.titleErr == "" {
		t.Error("an empty title should set a validation error")
	}
	if cmd != nil {
		if _, ok := cmd().(createSubmittedMsg); ok {
			t.Error("an empty title must not submit")
		}
	}
}

func TestCreateFormCyclesType(t *testing.T) {
	f := newCreateForm(testDeps(), []domain.IssueTypeSummary{{ID: 5, Name: "Story"}, {ID: 6, Name: "Bug"}})
	f, _ = f.focusOn(nfType)
	f, _ = f.onKey(press("right"))
	if f.typeIx != 1 {
		t.Errorf("right should advance the type, got %d", f.typeIx)
	}
}

func TestCreateSubmitClosesAndFires(t *testing.T) {
	m := createReady(t)
	m, _ = m.Update(press("n"))
	var cmd tea.Cmd
	m, cmd = m.Update(createSubmittedMsg{v: createValues{typeID: 5, title: "X", priority: "P2"}})
	if m.creating {
		t.Error("submit should close the form")
	}
	if cmd == nil {
		t.Error("submit should fire the create command")
	}
}

func TestIssueCreatedReloadsList(t *testing.T) {
	m := createReady(t)
	gen := m.reqGen
	m, cmd := m.Update(IssueCreatedMsg{key: "ENG-9"})
	if !m.loading || m.reqGen == gen {
		t.Error("a successful create should reload the list from the top")
	}
	if cmd == nil {
		t.Error("a successful create should batch a reload and a toast")
	}
}

func TestIssueCreateErrorNoReload(t *testing.T) {
	m := createReady(t)
	gen := m.reqGen
	m, _ = m.Update(IssueCreatedMsg{err: true})
	if m.loading || m.reqGen != gen {
		t.Error("a failed create must not reload the list")
	}
}
