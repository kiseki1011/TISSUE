package home

import (
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

func homeWith(t *testing.T, admin bool, projects ...domain.Project) Model {
	t.Helper()
	zone.NewGlobal()
	m := New(deps.Deps{
		Server: "srv", Config: &config.Config{},
		Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Nerd), Mouse: true,
	}, domain.SystemInfo{}, "")
	m, _ = m.Update(tea.WindowSizeMsg{Width: 140, Height: 30})
	m = m.WithAdmin(admin)
	m, _ = m.Update(ProjectsLoadedMsg{projects: projects})
	m.focus = focusList
	return m
}

func TestEnterMemberOpensDirectly(t *testing.T) {
	m := homeWith(t, false, domain.Project{Key: "ENG", Title: "Eng", Visibility: "PUBLIC", MyRole: "MEMBER"})
	m, cmd := m.Update(keyPress("enter"))
	if m.joining {
		t.Fatal("a member should open the project without a join modal")
	}
	if cmd == nil {
		t.Fatal("a member's enter should navigate")
	}
	if _, ok := cmd().(nav.OpenProjectMsg); !ok {
		t.Error("a member's enter should emit OpenProjectMsg")
	}
}

func TestEnterNonMemberPublicOffersJoin(t *testing.T) {
	m := homeWith(t, false, domain.Project{Key: "PUB", Title: "Pub", Visibility: "PUBLIC"})
	m, _ = m.Update(keyPress("enter"))
	if !m.joining {
		t.Fatal("a non-member on a public project should be offered a join")
	}
	if m.joinTarget.Key != "PUB" {
		t.Errorf("join target = %q, want PUB", m.joinTarget.Key)
	}
}

// A non-admin cannot self-join a PRIVATE project, so entry is blocked with a toast and no modal.
func TestEnterNonMemberPrivateBlockedForNonAdmin(t *testing.T) {
	m := homeWith(t, false, domain.Project{Key: "SEC", Title: "Sec", Visibility: "PRIVATE"})
	m, cmd := m.Update(keyPress("enter"))
	if m.joining {
		t.Fatal("a non-admin must not be offered self-join for a private project")
	}
	if cmd == nil {
		t.Error("a blocked private entry should show an explanatory toast")
	}
}

func TestEnterNonMemberPrivateAdminOffersJoin(t *testing.T) {
	m := homeWith(t, true, domain.Project{Key: "SEC", Title: "Sec", Visibility: "PRIVATE"})
	m, _ = m.Update(keyPress("enter"))
	if !m.joining {
		t.Fatal("a system admin should be offered self-join even for a private project")
	}
}

// Accepting HOLDS the modal open so the in-flight result cannot be dropped by navigating away.
func TestJoinAcceptHoldsModalAndFires(t *testing.T) {
	m := homeWith(t, false, domain.Project{Key: "PUB", Title: "Pub", Visibility: "PUBLIC"})
	m, _ = m.Update(keyPress("enter"))
	m, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if !m.joining {
		t.Error("accepting should keep the modal open (submitting) until the join returns")
	}
	if cmd == nil {
		t.Error("accepting should fire the join")
	}
	m, _ = m.Update(joinDoneMsg{project: domain.Project{Key: "PUB", Title: "Pub"}})
	if m.joining {
		t.Error("the join result should close the submitting modal")
	}
}

// An open join modal captures input, so app-level keys cannot switch away and strand the flow.
func TestJoiningCapturesInput(t *testing.T) {
	m := homeWith(t, false, domain.Project{Key: "PUB", Title: "Pub", Visibility: "PUBLIC"})
	m, _ = m.Update(keyPress("enter"))
	if !m.CapturingInput() {
		t.Error("an open join modal should capture input (block app-level tab/option keys)")
	}
}

func TestJoinCancelCloses(t *testing.T) {
	m := homeWith(t, false, domain.Project{Key: "PUB", Title: "Pub", Visibility: "PUBLIC"})
	m, _ = m.Update(keyPress("enter"))
	m, _ = m.Update(widgets.ConfirmCancelledMsg{})
	if m.joining {
		t.Error("cancelling should close the join confirm")
	}
}

// A successful join navigates into the project. A failure surfaces a toast instead.
func TestJoinDoneOutcomes(t *testing.T) {
	m := homeWith(t, false, domain.Project{Key: "PUB", Title: "Pub", Visibility: "PUBLIC"})
	_, okCmd := m.Update(joinDoneMsg{project: domain.Project{Key: "PUB", Title: "Pub"}})
	if okCmd == nil {
		t.Error("a successful join should batch navigation + a refresh")
	}
	_, errCmd := m.Update(joinDoneMsg{project: domain.Project{Key: "PUB"}, err: true, errText: "This project is private - ask a manager to add you."})
	if errCmd == nil {
		t.Error("a failed join should surface a toast")
	}
}
