package toast

import (
	"fmt"
	"regexp"
	"strings"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var csi = regexp.MustCompile(`\x1b\[[0-9;]*[A-Za-z]`)

func plain(s string) string { return csi.ReplaceAllString(s, "") }

func newModel() Model { return New(theme.TokyoNight(), glyph.New(glyph.Unicode)) }

func TestShowRaisesToast(t *testing.T) {
	m := newModel()
	m, cmd := m.Update(ShowMsg{Level: Success, Text: "Saved changes"})
	if m.Empty() {
		t.Fatal("stack is empty right after a ShowMsg")
	}
	if cmd == nil {
		t.Error("no expiry timer was scheduled for the new toast")
	}
	if !strings.Contains(plain(m.View()), "Saved changes") {
		t.Errorf("toast text not rendered:\n%s", plain(m.View()))
	}
}

func TestExpireRemovesMatchingToast(t *testing.T) {
	m := newModel()
	m, _ = m.Update(ShowMsg{Level: Info, Text: "first"})  // id 1
	m, _ = m.Update(ShowMsg{Level: Info, Text: "second"}) // id 2
	m, _ = m.Update(ExpireMsg{ID: 1})
	v := plain(m.View())
	if strings.Contains(v, "first") {
		t.Error("the expired toast is still shown")
	}
	if !strings.Contains(v, "second") {
		t.Error("expiring id 1 removed the wrong toast")
	}
}

func TestStackCapsAtMaxVisible(t *testing.T) {
	m := newModel()
	for i := 0; i < maxVisible+3; i++ {
		m, _ = m.Update(ShowMsg{Level: Info, Text: fmt.Sprintf("toast-%d", i)})
	}
	if len(m.items) != maxVisible {
		t.Fatalf("stack length = %d, want the %d cap", len(m.items), maxVisible)
	}
	if strings.Contains(plain(m.View()), "toast-0") {
		t.Error("oldest toast was not dropped once the cap was exceeded")
	}
}

func TestEmptyStackRendersNothing(t *testing.T) {
	m := newModel()
	if !m.Empty() {
		t.Error("a fresh stack is not Empty()")
	}
	if m.View() != "" {
		t.Errorf("empty stack rendered %q, want an empty string", m.View())
	}
}

func TestSeverityLabels(t *testing.T) {
	cases := map[Level]string{Success: "Success", Warning: "Warning", Error: "Error", Info: "Info"}
	for level, label := range cases {
		m := newModel()
		m, _ = m.Update(ShowMsg{Level: level, Text: "body"})
		if !strings.Contains(plain(m.View()), label) {
			t.Errorf("level %d did not render the %q label", level, label)
		}
	}
}
