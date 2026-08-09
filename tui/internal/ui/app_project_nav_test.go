package ui

import (
	"strings"
	"testing"

	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/screens/project"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func projectNavApp(t *testing.T) App {
	t.Helper()
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://localhost:8080"}
	a := New(d)
	a.screen = screenProject
	a.project = project.New(d, "ENG", "Engineering")
	a.user = domain.Profile{Username: "admin"}
	a.width, a.height = 120, 24
	return a
}

// Options and help open from the project drill-in (it wears chrome), even though it is not a top-level
// tab - the tab-switch digits stay reserved for the project's own 1-4 sub-tabs.
func TestProjectScreenOpensOptionsAndHelp(t *testing.T) {
	a := projectNavApp(t)
	if !a.optionsNavActive() {
		t.Fatal("optionsNavActive should be true on the project screen at the list root")
	}
	m, _ := a.Update(keyPress(","))
	if m.(App).modal == nil {
		t.Error(", did not open the Options modal on the project screen")
	}

	a = projectNavApp(t)
	m, _ = a.Update(keyPress("?"))
	app := m.(App)
	if app.modal == nil {
		t.Fatal("? did not open the help modal on the project screen")
	}
	// The drill-in names its help after the project ("{project} · Help"), not the bare "Help · Help" the
	// default title produced before the screen implemented describer.
	view := stripCSI(app.modal.View())
	if !strings.Contains(view, "Engineering") {
		t.Errorf("project help modal title is not the project name:\n%s", view)
	}
	if strings.Contains(view, "Help · Help") {
		t.Errorf("project help modal fell back to the default title:\n%s", view)
	}
}

// The project drill-in's footer top line carries its sub-tab switch + reload and the options/help hints,
// but not the app's own 1/2/3 tab switch (whose digits belong to the project's sub-tabs here).
func TestProjectScreenGlobalFooter(t *testing.T) {
	a := projectNavApp(t)
	descs := map[string]bool{}
	for _, b := range a.globalKeys() {
		descs[b.Help().Desc] = true
		if b.Help().Key == "1/2/3" {
			t.Error("the app 1/2/3 tab switch must not show on the project screen")
		}
	}
	for _, want := range []string{"tab", "reload", "back", "help", "options", "quit"} {
		if !descs[want] {
			t.Errorf("project global footer missing %q (have %v)", want, descs)
		}
	}
}
