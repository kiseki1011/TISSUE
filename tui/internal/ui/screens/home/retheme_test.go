package home

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/config"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// Retheme must rebuild the table rows, not only its styles: project rows embed theme colors (key,
// pin, visibility, archived) into the cell strings at build time, so a plain SetStyles would leave
// the visible rows painted in the old palette.
func TestRethemeReembedsRowColors(t *testing.T) {
	m := testModel(t)
	var cmd tea.Cmd
	m, cmd = m.Update(tea.WindowSizeMsg{Width: 130, Height: 30})
	_ = cmd
	if len(m.table.Rows()) == 0 {
		t.Fatal("no rows were built after sizing the table")
	}
	before := joinRows(m)

	m = m.Retheme(deps.Deps{
		Server: "srv", Config: &config.Config{},
		Styles: theme.New(theme.Dracula()), Glyphs: glyph.New(glyph.Nerd),
	})
	if joinRows(m) == before {
		t.Error("Retheme did not re-embed the new theme colors into the table rows")
	}
	if m.ThemeName() != "dracula" {
		t.Errorf("ThemeName after retheme = %q, want dracula", m.ThemeName())
	}
}

func joinRows(m Model) string {
	var b strings.Builder
	for _, r := range m.table.Rows() {
		for _, c := range r {
			b.WriteString(c)
			b.WriteByte('|')
		}
		b.WriteByte('\n')
	}
	return b.String()
}
