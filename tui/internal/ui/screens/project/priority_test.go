package project

import (
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// The reply gutter uses a section title's Text color, not the Muted it used before.
func TestCommentGutterUsesSectionTitleColor(t *testing.T) {
	m := New(testDeps(), testKey, "Tissue")
	th := m.deps.Styles.Theme

	got := m.commentGutter(1)
	wantText := lipgloss.NewStyle().Foreground(th.Text).Render("│ ")
	if got != wantText {
		t.Errorf("depth-1 gutter is not Text-colored:\n got  %q\n want %q", got, wantText)
	}
	if muted := lipgloss.NewStyle().Foreground(th.Muted).Render("│ "); got == muted {
		t.Error("gutter regressed to the muted color")
	}
	if m.commentGutter(0) != "" {
		t.Error("depth-0 comment should have no gutter")
	}
}

// P2 used to share the muted color of P3/P4. It now stands apart in Primary.
func TestPriorityColorP2IsDistinct(t *testing.T) {
	th := theme.TokyoNight()

	if got := priorityColor(th, "P2"); got != th.Primary {
		t.Errorf("P2 should be Primary, got %v", got)
	}
	if got := priorityColor(th, "P2"); got == th.Muted {
		t.Error("P2 must not share the muted low-priority color")
	}
	for _, low := range []string{"P3", "P4"} {
		if got := priorityColor(th, low); got != th.Muted {
			t.Errorf("%s should stay Muted, got %v", low, got)
		}
	}
	if priorityColor(th, "P0") != th.Error || priorityColor(th, "P1") != th.Warning {
		t.Error("P0/P1 colors regressed")
	}
}
