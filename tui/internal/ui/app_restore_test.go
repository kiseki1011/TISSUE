package ui

import (
	"strings"
	"testing"

	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/home"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

const restoreServer = "http://localhost:8080"

// newRestoreApp builds an app whose config lives in a throwaway dir, so the last-project pointer can be
// persisted and reloaded without touching the user's real config file.
func newRestoreApp(t *testing.T) (App, *config.Config) {
	t.Helper()
	tmp := t.TempDir()
	t.Setenv("HOME", tmp)
	t.Setenv("XDG_CONFIG_HOME", tmp)
	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("load config: %v", err)
	}
	zone.NewGlobal()
	d := deps.Deps{
		Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode),
		Server: restoreServer, Config: cfg,
	}
	a := New(d)
	a.width, a.height = 120, 32
	return a, cfg
}

func reloadConfig(t *testing.T) *config.Config {
	t.Helper()
	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("reload config: %v", err)
	}
	return cfg
}

// A silent session restore with a saved project deep-links straight into it, skipping the dashboard.
func TestRestoreDeepLinksLastProject(t *testing.T) {
	a, cfg := newRestoreApp(t)
	if err := cfg.SetLastProject(restoreServer, "ENG"); err != nil {
		t.Fatalf("seed: %v", err)
	}
	m, _ := a.Update(nav.GoToHomeMsg{Info: domain.SystemInfo{}, Restore: true})
	if got := m.(App).screen; got != screenProject {
		t.Errorf("a restore with a saved project should land on the project screen, got %v", got)
	}
}

// A silent restore with nothing saved lands on the dashboard.
func TestRestoreNoSavedProjectStaysHome(t *testing.T) {
	a, _ := newRestoreApp(t)
	m, _ := a.Update(nav.GoToHomeMsg{Info: domain.SystemInfo{}, Restore: true})
	if got := m.(App).screen; got != screenHome {
		t.Errorf("a restore with no saved project should land on the dashboard, got %v", got)
	}
}

// A fresh login lands on the dashboard and forgets any saved pointer, so the next user on the same
// server does not inherit the previous one's last project.
func TestFreshLoginLandsHomeAndForgets(t *testing.T) {
	a, cfg := newRestoreApp(t)
	if err := cfg.SetLastProject(restoreServer, "ENG"); err != nil {
		t.Fatalf("seed: %v", err)
	}
	m, _ := a.Update(nav.GoToHomeMsg{Info: domain.SystemInfo{}, Restore: false})
	if got := m.(App).screen; got != screenHome {
		t.Errorf("a fresh login should land on the dashboard, got %v", got)
	}
	if got := reloadConfig(t).LastProjectFor(restoreServer); got != "" {
		t.Errorf("a fresh login should forget the last project, got %q", got)
	}
}

// When a silent restore deep-links past the dashboard into a project, home's background projects-list
// load still lands: the app routes ProjectsLoadedMsg to home even though the project screen is active.
// Otherwise home stays stuck on "Projects (loading)" after the user escapes back.
// A project settings edit fires RefreshDashboardMsg; the app routes it to the dashboard, which silently
// reloads so the change shows on the list when the user returns.
func TestRefreshDashboardMsgReloadsHome(t *testing.T) {
	a, _ := newRestoreApp(t)
	a.width, a.height = 170, 44
	m, _ := a.Update(nav.GoToHomeMsg{Info: domain.SystemInfo{}})
	m, _ = m.(App).Update(home.ProjectsLoadedMsg{}) // dashboard is loaded (not spinning)

	m, cmd := m.(App).Update(nav.RefreshDashboardMsg{})
	if cmd == nil {
		t.Fatal("RefreshDashboardMsg should trigger a dashboard reload")
	}
	if view := stripCSI(m.(App).View().Content); !strings.Contains(view, "Projects (loading)") {
		t.Errorf("the dashboard should be reloading after a refresh signal:\n%s", view)
	}
}

func TestRestoreRoutesHomeProjectsLoadWhileDeepLinked(t *testing.T) {
	a, cfg := newRestoreApp(t)
	a.width, a.height = 170, 44 // large enough that home renders its list rather than "too small"
	if err := cfg.SetLastProject(restoreServer, "ENG"); err != nil {
		t.Fatalf("seed: %v", err)
	}
	m, _ := a.Update(nav.GoToHomeMsg{Info: domain.SystemInfo{}, Restore: true})
	if m.(App).screen != screenProject {
		t.Fatalf("restore should deep-link into the project")
	}

	// home's Init() load resolves while the project screen is active - it must still reach home
	m, _ = m.(App).Update(home.ProjectsLoadedMsg{})

	// escape back to the dashboard, which sizes home and renders it
	m, _ = m.(App).Update(nav.CloseProjectMsg{})
	if m.(App).screen != screenHome {
		t.Fatalf("esc should return to the dashboard")
	}
	view := stripCSI(m.(App).View().Content)
	if strings.Contains(view, "Projects (loading)") {
		t.Errorf("the dashboard is stuck loading - the projects-list result was dropped:\n%s", view)
	}
	if !strings.Contains(view, "Projects (0)") {
		t.Errorf("the dashboard should show the loaded (empty) projects list:\n%s", view)
	}
}

// Drilling into a project remembers it, and returning to the dashboard forgets it.
func TestOpenAndCloseProjectPointer(t *testing.T) {
	a, _ := newRestoreApp(t)
	a.screen = screenHome

	m, _ := a.Update(nav.OpenProjectMsg{ProjectKey: "ENG", Title: "Engineering"})
	if got := m.(App).screen; got != screenProject {
		t.Fatalf("opening a project should drill in, got %v", got)
	}
	if got := reloadConfig(t).LastProjectFor(restoreServer); got != "ENG" {
		t.Errorf("opening a project should remember it, got %q", got)
	}

	m, _ = m.(App).Update(nav.CloseProjectMsg{})
	if got := m.(App).screen; got != screenHome {
		t.Fatalf("closing a project should return to the dashboard, got %v", got)
	}
	if got := reloadConfig(t).LastProjectFor(restoreServer); got != "" {
		t.Errorf("returning to the dashboard should forget the project, got %q", got)
	}
}
