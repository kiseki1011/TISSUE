package widgets

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

func confirmStyles() theme.Styles { return theme.New(theme.TokyoNight()) }

// The dialog defaults focus to Cancel so a stray enter never confirms a destructive action; the
// enter then emits a cancel, not an accept.
func TestConfirmDefaultsToCancel(t *testing.T) {
	f := NewConfirmForm(confirmStyles(), "Delete team", `Delete "Infra"?`, "Delete")
	f, cmd := f.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if cmd == nil {
		t.Fatal("enter on the default focus produced no message")
	}
	if _, ok := cmd().(ConfirmCancelledMsg); !ok {
		t.Errorf("enter on the default (Cancel) focus did not cancel (got %T)", cmd())
	}
	if f.Submitting {
		t.Error("a cancel wrongly entered the submitting state")
	}
}

// Toggling focus to the accept button and pressing enter enters the submitting state and asks the
// parent to run the command.
func TestConfirmAcceptSubmits(t *testing.T) {
	f := NewConfirmForm(confirmStyles(), "Delete team", `Delete "Infra"?`, "Delete")
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyLeft}) // Cancel -> Delete
	f, cmd := f.Update(tea.KeyPressMsg{Code: tea.KeyEnter})
	if !f.Submitting {
		t.Error("accept did not enter the submitting state")
	}
	if cmd == nil {
		t.Fatal("accept produced no command")
	}
	// the batch carries the accepted message plus the spinner tick
	if _, ok := cmd().(tea.BatchMsg); !ok {
		t.Errorf("accept did not batch its accepted message (got %T)", cmd())
	}
}

// Esc cancels the dialog, matching the advertised "esc cancel" hint and HelpKeys, regardless of
// which button is focused.
func TestConfirmEscCancels(t *testing.T) {
	f := NewConfirmForm(confirmStyles(), "Deactivate agent", `Deactivate "Bot"?`, "Deactivate")
	f, _ = f.Update(tea.KeyPressMsg{Code: tea.KeyLeft}) // focus the destructive button
	_, cmd := f.Update(tea.KeyPressMsg{Code: tea.KeyEscape})
	if cmd == nil {
		t.Fatal("esc produced no message")
	}
	if _, ok := cmd().(ConfirmCancelledMsg); !ok {
		t.Errorf("esc did not cancel (got %T)", cmd())
	}
}

// The dialog renders its title, message, and both buttons.
func TestConfirmView(t *testing.T) {
	f := NewConfirmForm(confirmStyles(), "Delete position", `Delete "Lead"?`, "Delete")
	view := f.View()
	for _, want := range []string{"Delete position", "Lead", "Delete", "Cancel"} {
		if !strings.Contains(view, want) {
			t.Errorf("confirm view missing %q", want)
		}
	}
}

// A parent-surfaced failure (Status set, Submitting cleared) is shown in place.
func TestConfirmSurfacesStatus(t *testing.T) {
	f := NewConfirmForm(confirmStyles(), "Delete team", `Delete "Infra"?`, "Delete")
	f.Submitting, f.Status = false, "You do not have permission."
	if !strings.Contains(f.View(), "permission") {
		t.Error("the failure status was not rendered")
	}
}
