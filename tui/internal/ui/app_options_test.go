package ui

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/auth"
	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/schema"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// fakeStore records the server whose tokens were cleared on logout.
type fakeStore struct{ cleared string }

func (f *fakeStore) Load(string) (domain.TokenPair, bool, error) {
	return domain.TokenPair{}, false, nil
}
func (f *fakeStore) Save(string, domain.TokenPair) error { return nil }
func (f *fakeStore) Clear(server string) error           { f.cleared = server; return nil }

var _ auth.TokenStore = (*fakeStore)(nil)

func keyComma() tea.KeyPressMsg    { return tea.KeyPressMsg{Code: ','} }
func optKeyTab() tea.KeyPressMsg   { return tea.KeyPressMsg{Code: tea.KeyTab} }
func optKeyEnter() tea.KeyPressMsg { return tea.KeyPressMsg{Code: tea.KeyEnter} }

func optionsApp(store auth.TokenStore) App {
	zone.NewGlobal()
	d := deps.Deps{
		Styles: theme.New(theme.TokyoNight()),
		Glyphs: glyph.New(glyph.Unicode),
		Server: "http://localhost:8080",
		Store:  store,
		// Config nil so applyTheme does not touch the user's real config file during tests
	}
	a := New(d)
	a.screen = screenSchema
	a.schema = schema.New(d)
	a.user = domain.Profile{Name: "Hong Gil Dong", Username: "admin", Email: "admin@tissue.com", Role: "ADMIN"}
	a.width, a.height = 100, 32
	return a
}

// comma (,) opens the options modal; the Info section shows the server and the Account section shows the
// caller's identity.
func TestCommaOpensOptions(t *testing.T) {
	a := optionsApp(nil)
	m, _ := a.Update(keyComma())
	app := m.(App)
	if _, ok := app.modal.(optionsModal); !ok {
		t.Fatalf(", did not open the options modal (got %T)", app.modal)
	}
	if info := stripCSI(app.modal.View()); !strings.Contains(info, "Options") || !strings.Contains(info, "http://localhost:8080") {
		t.Errorf("Info section missing the server:\n%s", info)
	}
	m, _ = app.Update(optKeyTab())     // Info -> Settings
	m, _ = m.(App).Update(optKeyTab()) // Settings -> Account
	acct := stripCSI(m.(App).modal.View())
	for _, want := range []string{"admin", "admin@tissue.com", "Admin", "Team", "Position"} {
		if !strings.Contains(acct, want) {
			t.Errorf("Account section missing %q:\n%s", want, acct)
		}
	}
}

// Picking a theme from the popup applies it app-wide (deps restyled) and repaints the modal.
func TestOptionsThemeSwitchApplies(t *testing.T) {
	a := optionsApp(nil)
	m, _ := a.Update(keyComma())
	m, _ = m.(App).Update(optKeyTab())      // Info -> Settings (theme control)
	m, _ = m.(App).Update(optKeyEnter())    // open the theme popup (seeded tokyo-night)
	m, _ = m.(App).Update(keyPress("down")) // tokyo-night -> dracula
	m, cmd := m.(App).Update(optKeyEnter()) // apply the highlighted theme
	if cmd == nil {
		t.Fatal("picking the theme emitted no command")
	}
	m, _ = m.(App).Update(cmd()) // deliver the themeSelectedMsg
	app := m.(App)
	if got := app.deps.Styles.Theme.Name; got != "dracula" {
		t.Errorf("theme not applied app-wide: %q, want dracula", got)
	}
	if om, ok := app.modal.(optionsModal); !ok || om.theme.Name != "dracula" {
		t.Errorf("the modal did not repaint in the new theme")
	}
	// the switch must propagate to every persistent screen, not just deps + the modal
	if app.home.ThemeName() != "dracula" || app.schema.ThemeName() != "dracula" {
		t.Errorf("theme did not reach all screens: home=%q schema=%q",
			app.home.ThemeName(), app.schema.ThemeName())
	}
}

// A theme switch is written to the config file so it survives a restart.
func TestOptionsThemeSwitchPersists(t *testing.T) {
	tmp := t.TempDir()
	t.Setenv("HOME", tmp)            // os.UserConfigDir on darwin
	t.Setenv("XDG_CONFIG_HOME", tmp) // os.UserConfigDir on linux
	cfg, err := config.Load()
	if err != nil {
		t.Fatalf("load config: %v", err)
	}
	zone.NewGlobal()
	d := deps.Deps{
		Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode),
		Server: "http://localhost:8080", Config: cfg,
	}
	a := New(d)
	a.screen = screenSchema
	a.schema = schema.New(d)
	a.width, a.height = 100, 32

	m, _ := a.Update(keyComma())
	m, _ = m.(App).Update(optKeyTab())      // Info -> Settings
	m, _ = m.(App).Update(optKeyEnter())    // open the theme popup
	m, _ = m.(App).Update(keyPress("down")) // tokyo-night -> dracula
	m, cmd := m.(App).Update(optKeyEnter()) // apply
	m.(App).Update(cmd())                   // applyTheme -> Config.Save()

	reloaded, err := config.Load()
	if err != nil {
		t.Fatalf("reload config: %v", err)
	}
	if reloaded.Theme != "dracula" {
		t.Errorf("theme not persisted to disk: %q, want dracula", reloaded.Theme)
	}
}

// Mouse clicks on the theme control (Settings) open its list popup, and on the logout button (Account)
// drop the session — the same effects as the keyboard.
func TestOptionsClickZones(t *testing.T) {
	a := optionsApp(nil)
	m, _ := a.Update(keyComma())
	m, _ = m.(App).Update(optKeyTab()) // Info -> Settings so the theme control renders
	app := m.(App)
	_ = app.View() // composite + scan the modal so its click zones register

	themeZone := settleZone(t, "opt.theme")
	m3, _ := app.Update(tea.MouseClickMsg{X: (themeZone.StartX + themeZone.EndX) / 2, Y: themeZone.StartY, Button: tea.MouseLeft})
	if om, ok := m3.(App).modal.(optionsModal); !ok || om.picking != pickerTheme {
		t.Errorf("clicking the theme control did not open the theme popup")
	}

	m2, _ := app.Update(optKeyTab()) // Settings -> Account so the logout button renders
	app2 := m2.(App)
	_ = app2.View()
	logoutZone := settleZone(t, "opt.logout")
	_, cmd := app2.Update(tea.MouseClickMsg{X: (logoutZone.StartX + logoutZone.EndX) / 2, Y: logoutZone.StartY, Button: tea.MouseLeft})
	if cmd == nil {
		t.Fatal("clicking the logout button emitted no command")
	}
	if _, ok := cmd().(logoutMsg); !ok {
		t.Errorf("logout click did not emit logoutMsg (got %T)", cmd())
	}
}

// In the Account section, Down moves focus from the position picker to the logout button; Enter drops
// the session and returns to the connecting screen, clearing the stored tokens.
func TestOptionsLogout(t *testing.T) {
	fs := &fakeStore{}
	a := optionsApp(fs)
	m, _ := a.Update(keyComma())
	m, _ = m.(App).Update(optKeyTab())      // Info -> Settings
	m, _ = m.(App).Update(optKeyTab())      // Settings -> Account (focus: position)
	m, _ = m.(App).Update(keyPress("down")) // position -> logout
	m, cmd := m.(App).Update(optKeyEnter()) // logout button emits its cmd
	if cmd == nil {
		t.Fatal("enter on the logout button emitted no command")
	}
	m, cmd = m.(App).Update(cmd()) // deliver logoutMsg -> navigates + returns the async logout cmd
	app := m.(App)
	if app.screen != screenConnecting {
		t.Errorf("logout did not return to the connecting screen: %v", app.screen)
	}
	if app.modal != nil {
		t.Error("logout left the modal open")
	}
	if app.user.Username != "" {
		t.Error("logout did not clear the cached profile")
	}
	// running the async logout command revokes + clears the stored tokens
	if cmd == nil {
		t.Fatal("logout did not run a revoke/clear command")
	}
	if _, ok := cmd().(loggedOutMsg); !ok {
		t.Error("the logout command did not report completion")
	}
	if fs.cleared != "http://localhost:8080" {
		t.Errorf("logout did not clear the server's tokens: %q", fs.cleared)
	}
}
