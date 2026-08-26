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

// The project drill-in wears the app chrome but supplies its own header-right tabs, keeping the app's
// left brand/server/user. Its tab keys stay live because the app's tab-nav yields to it.
func TestProjectScreenHeaderChrome(t *testing.T) {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Server: "http://localhost:8080"}
	a := New(d)
	a.screen = screenProject
	a.project = project.New(d, "ENG", "Engineering")
	a.user = domain.Profile{Username: "admin"}
	a.width, a.height = 140, 24 // wide enough to stay non-compact so the full header shows

	if !a.hasChrome(screenProject) {
		t.Fatal("the project screen should wear the header/footer chrome")
	}
	if a.tabNavActive() {
		t.Error("app tab-nav must yield on the project screen so its own 1-4 keys own the digits")
	}

	h := stripCSI(a.headerView())
	// app-owned left, project-owned right
	for _, want := range []string{"Tissue Server", "http://localhost:8080", "admin", "‹ Projects", "Issues", "Config"} {
		if !strings.Contains(h, want) {
			t.Errorf("project header missing %q:\n%s", want, h)
		}
	}
	if strings.Contains(h, "Schema") || strings.Contains(h, "Agents") {
		t.Errorf("the project header should show project tabs, not the app tabs:\n%s", h)
	}
}
