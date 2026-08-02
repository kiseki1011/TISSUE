package ui

import (
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

func newOptModal(user domain.Profile) optionsModal {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Server: "http://x"}
	m, _ := newOptionsModal(d, user) // the position-load cmd is not run (no Catalog in tests)
	return m
}

// drive feeds a sequence of messages through the modal and returns the resulting concrete model.
func drive(m optionsModal, msgs ...tea.Msg) optionsModal {
	var am appModal = m
	for _, msg := range msgs {
		am, _ = am.Update(msg)
	}
	return am.(optionsModal)
}

var samplePositions = []domain.PositionSummary{
	{ID: 10, Name: "Backend"}, {ID: 11, Name: "Frontend"}, {ID: 12, Name: "Design"},
}

// tab wraps Info → Settings → Account → Info, and ←/→ (with h/l) switch sections the same way.
func TestOptionsSectionNav(t *testing.T) {
	m := newOptModal(domain.Profile{})
	if m.section != sectionInfo {
		t.Fatalf("initial section = %d, want Info", m.section)
	}
	m = drive(m, optKeyTab())
	if m.section != sectionSettings {
		t.Errorf("after one tab = %d, want Settings", m.section)
	}
	m = drive(m, optKeyTab(), optKeyTab())
	if m.section != sectionInfo {
		t.Errorf("tab did not wrap back to Info: %d", m.section)
	}
	// arrows switch sections too
	m = drive(m, keyPress("right"))
	if m.section != sectionSettings {
		t.Errorf("right did not advance the section: %d", m.section)
	}
	m = drive(m, keyPress("l"))
	if m.section != sectionAccount {
		t.Errorf("l did not advance the section: %d", m.section)
	}
	m = drive(m, keyPress("left"))
	if m.section != sectionSettings {
		t.Errorf("left did not step back a section: %d", m.section)
	}
	m = drive(m, keyPress("h"), keyPress("h"))
	if m.section != sectionAccount {
		t.Errorf("h did not wrap back around: %d", m.section)
	}
}

// On Settings, enter opens the theme list popup; it lists the themes and is seeded to the applied one.
func TestThemePickerOpens(t *testing.T) {
	m := newOptModal(domain.Profile{})
	m = drive(m, optKeyTab(), optKeyEnter()) // Settings -> open theme popup
	if m.picking != pickerTheme {
		t.Fatalf("enter on the theme control did not open the theme popup: picking=%d", m.picking)
	}
	view := stripCSI(m.View())
	for _, want := range []string{"Theme", "Tokyo Night", "Dracula", "Gruvbox", "ANSI"} {
		if !strings.Contains(view, want) {
			t.Errorf("theme popup missing %q:\n%s", want, view)
		}
	}
}

// Moving to a theme and pressing enter applies it (themeSelectedMsg), repaints the modal, and closes
// the popup.
func TestThemePickerApplies(t *testing.T) {
	m := newOptModal(domain.Profile{})
	m = drive(m, optKeyTab(), optKeyEnter(), keyPress("down")) // Settings, open, tokyo-night -> dracula
	var am appModal = m
	am, cmd := am.Update(optKeyEnter())
	m = am.(optionsModal)
	if m.picking != pickerNone {
		t.Error("applying the theme did not close the popup")
	}
	if cmd == nil {
		t.Fatal("applying the theme emitted no command")
	}
	msg, ok := cmd().(themeSelectedMsg)
	if !ok || msg.name != "dracula" {
		t.Fatalf("apply did not select dracula (got %#v)", cmd())
	}
	if m.theme.Name != "dracula" {
		t.Errorf("the modal did not repaint in the new theme: %q", m.theme.Name)
	}
}

// esc backs out of a popup to the options body without applying anything.
func TestPickerEscReturnsToBody(t *testing.T) {
	m := newOptModal(domain.Profile{})
	m = drive(m, optKeyTab(), optKeyEnter()) // open theme popup
	if m.picking != pickerTheme {
		t.Fatal("theme popup did not open")
	}
	m = drive(m, keyPress("esc"))
	if m.picking != pickerNone {
		t.Errorf("esc did not close the popup: picking=%d", m.picking)
	}
	if m.theme.Name != "tokyo-night" {
		t.Errorf("esc changed the theme: %q", m.theme.Name)
	}
}

// On Account, enter opens the position popup once the list has loaded; it offers a "none" row plus
// every position and is seeded to the applied one.
func TestPositionPickerOpensAndLists(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 11}) // Frontend current
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab(), optKeyEnter())
	if m.picking != pickerPosition {
		t.Fatalf("enter did not open the position popup: picking=%d", m.picking)
	}
	if got, _ := m.pick.Selected(); got.Label != "Frontend" {
		t.Errorf("popup not seeded to the applied position: %q", got.Label)
	}
	view := stripCSI(m.View())
	for _, want := range []string{"Position", "(none)", "Backend", "Frontend", "Design"} {
		if !strings.Contains(view, want) {
			t.Errorf("position popup missing %q:\n%s", want, view)
		}
	}
}

// Choosing a different position issues the set command and enters the saving state, without
// optimistically rewriting the applied position (that waits for the result).
func TestPositionPickerApply(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 10}) // Backend current
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab(), optKeyEnter())
	m = drive(m, keyPress("down")) // none? no — seeded to Backend(index 1); down -> Frontend(index 2)
	if got, _ := m.pick.Selected(); got.Label != "Frontend" {
		t.Fatalf("cursor not on Frontend: %q", got.Label)
	}
	var am appModal = m
	am, cmd := am.Update(optKeyEnter())
	m = am.(optionsModal)
	if !m.positionSaving {
		t.Error("apply did not enter the saving state")
	}
	if cmd == nil {
		t.Error("apply produced no set command")
	}
	if m.picking != pickerNone {
		t.Error("apply did not close the popup")
	}
	if m.user.PositionID != 10 {
		t.Errorf("apply optimistically changed the applied position: %d", m.user.PositionID)
	}
}

// Choosing the already-applied position is a no-op: no command, no saving state.
func TestPositionPickerApplySameNoop(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 11}) // Frontend current, seeded under the cursor
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab(), optKeyEnter())
	var am appModal = m
	am, cmd := am.Update(optKeyEnter()) // enter on the already-applied Frontend
	if cmd != nil {
		t.Error("re-selecting the applied position fired a command")
	}
	if am.(optionsModal).positionSaving {
		t.Error("re-selecting the applied position entered the saving state")
	}
}

// The position popup cannot be opened unless the position is editable — not while the list is loading
// or failed to load (nothing to choose, so enter must not issue an unintended clear), and not while a
// save is already in flight (which would double-submit).
func TestPositionPickerBlockedUnlessEditable(t *testing.T) {
	// still loading
	m := newOptModal(domain.Profile{PositionID: 10})
	if m.positionEditable() {
		t.Error("position reported editable while still loading")
	}
	m = drive(m, optKeyTab(), optKeyTab(), optKeyEnter())
	if m.picking != pickerNone {
		t.Error("position popup opened while the list was still loading")
	}
	// failed to load
	m2 := newOptModal(domain.Profile{PositionID: 10})
	m2 = drive(m2, optionsPositionsLoaded{err: &domain.APIError{Status: 403}}, optKeyTab(), optKeyTab())
	if m2.positionEditable() {
		t.Error("position reported editable while the list was unavailable")
	}
	m2 = drive(m2, optKeyEnter())
	if m2.picking != pickerNone {
		t.Error("position popup opened while the list was unavailable")
	}
	// a save in flight
	m3 := newOptModal(domain.Profile{PositionID: 10})
	m3 = drive(m3, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab())
	m3.positionSaving = true
	if m3.positionEditable() {
		t.Error("position reported editable while a save was in flight")
	}
	m3 = drive(m3, optKeyEnter())
	if m3.picking != pickerNone {
		t.Error("position popup opened while a save was in flight")
	}
}

// A successful set clears saving, updates the applied position from the message, and toasts.
func TestPositionSetMsgUpdatesUser(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 10})
	m.positionSaving = true
	var am appModal = m
	am, cmd := am.Update(optionsPositionSet{positionID: 11, name: "Frontend"})
	m = am.(optionsModal)
	if m.positionSaving {
		t.Error("set did not clear the saving state")
	}
	if m.user.PositionID != 11 || m.user.PositionName != "Frontend" {
		t.Errorf("applied position not updated: %+v", m.user)
	}
	if _, ok := cmd().(toast.ShowMsg); !ok {
		t.Errorf("set did not toast (got %T)", cmd())
	}
}

// A failed set clears saving and surfaces the error both inline and as a toast.
func TestPositionFailedMsg(t *testing.T) {
	m := newOptModal(domain.Profile{})
	m.positionSaving = true
	var am appModal = m
	am, cmd := am.Update(optionsPositionFailed{message: "Not allowed."})
	m = am.(optionsModal)
	if m.positionSaving {
		t.Error("failure did not clear the saving state")
	}
	if m.positionStatus != "Not allowed." {
		t.Errorf("failure not surfaced: %q", m.positionStatus)
	}
	if _, ok := cmd().(toast.ShowMsg); !ok {
		t.Errorf("failure did not toast (got %T)", cmd())
	}
}

// Switching sections drops a stale picker error so it does not follow the user.
func TestPositionErrorClearsOnSectionSwitch(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 10})
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab())
	m = drive(m, optionsPositionFailed{message: "Not allowed."})
	if m.positionStatus == "" {
		t.Fatal("precondition: expected a sticky error status")
	}
	m = drive(m, keyPress("right")) // move to another section
	if m.positionStatus != "" {
		t.Errorf("switching sections did not clear the stale error: %q", m.positionStatus)
	}
}

// positionSetArgs resolves a picker value into the setOptionPosition arguments: "" clears, a numeric
// value selects that position (with its resolved name), and an unparseable value is a no-op.
func TestPositionSetArgs(t *testing.T) {
	// clear
	id, name, cleared, ok := positionSetArgs("", samplePositions)
	if id != nil || name != "" || !cleared || !ok {
		t.Errorf(`clear: got (%v,%q,%v,%v), want (nil,"",true,true)`, id, name, cleared, ok)
	}
	// select a known position
	id, name, cleared, ok = positionSetArgs("11", samplePositions)
	if id == nil || *id != 11 || name != "Frontend" || cleared || !ok {
		t.Errorf(`select 11: got (%v,%q,%v,%v), want (&11,"Frontend",false,true)`, id, name, cleared, ok)
	}
	// a valid id absent from the list still sends the id, with an empty name
	id, name, _, ok = positionSetArgs("999", samplePositions)
	if id == nil || *id != 999 || name != "" || !ok {
		t.Errorf(`select 999: got (%v,%q,ok=%v), want (&999,"",true)`, id, name, ok)
	}
	// unparseable -> no-op
	if _, _, _, ok = positionSetArgs("abc", samplePositions); ok {
		t.Error("unparseable value should not be ok")
	}
}

// Choosing the "— (none)" row clears the position: it issues a set command and enters the saving
// state, without optimistically clearing the applied position.
func TestPositionPickerClear(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 10}) // Backend current
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab(), optKeyEnter())
	m = drive(m, keyPress("up")) // Backend(index 1) -> none(index 0)
	if got, _ := m.pick.Selected(); got.Value != "" {
		t.Fatalf("cursor not on the none row: value=%q", got.Value)
	}
	var am appModal = m
	am, cmd := am.Update(optKeyEnter())
	m = am.(optionsModal)
	if !m.positionSaving {
		t.Error("clearing did not enter the saving state")
	}
	if cmd == nil {
		t.Error("clearing produced no command")
	}
	if m.user.PositionID != 10 {
		t.Errorf("clearing optimistically changed the applied position: %d", m.user.PositionID)
	}
}

// A successful clear (optionsPositionSet{cleared:true}) zeroes the applied position and toasts the
// clear text.
func TestPositionClearedMsgUpdatesUser(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 10, PositionName: "Backend"})
	m.positionSaving = true
	var am appModal = m
	am, cmd := am.Update(optionsPositionSet{positionID: 0, name: "", cleared: true})
	m = am.(optionsModal)
	if m.user.PositionID != 0 || m.user.PositionName != "" {
		t.Errorf("clear did not zero the applied position: %+v", m.user)
	}
	msg, ok := cmd().(toast.ShowMsg)
	if !ok {
		t.Fatalf("clear did not toast (got %T)", cmd())
	}
	if msg.Text != "Cleared your position." {
		t.Errorf("clear toast text = %q, want the cleared message", msg.Text)
	}
}

// Clicking a row inside an open popup applies it, the same as enter (theme popup: no Catalog needed).
func TestPopupRowClickApplies(t *testing.T) {
	m := newOptModal(domain.Profile{})
	m = drive(m, optKeyTab(), optKeyEnter()) // Settings -> open theme popup (seeded tokyo-night)
	if m.picking != pickerTheme {
		t.Fatal("theme popup did not open")
	}
	zone.Scan(m.View())
	z := settleZone(t, widgets.ListPickerOptZone(1)) // the Dracula row
	var am appModal = m
	am, cmd := am.Update(tea.MouseClickMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY, Button: tea.MouseLeft})
	m = am.(optionsModal)
	if m.picking != pickerNone {
		t.Error("clicking a row did not close the popup")
	}
	if cmd == nil {
		t.Fatal("clicking a row emitted no command")
	}
	if msg, ok := cmd().(themeSelectedMsg); !ok || msg.name != "dracula" {
		t.Errorf("row click did not apply the clicked theme (got %#v)", cmd())
	}
	if m.theme.Name != "dracula" {
		t.Errorf("row click did not repaint the modal: %q", m.theme.Name)
	}
}

// The Account section renders the team and the applied position as peer field rows (same shape), with
// the position carrying the "value ▾" dropdown affordance once it is editable.
func TestAccountRendersPositionField(t *testing.T) {
	m := newOptModal(domain.Profile{TeamName: "Platform", PositionID: 10})
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab())
	view := stripCSI(m.View())
	for _, want := range []string{"Profile", "Team", "Platform", "Position", "Backend", "▾"} {
		if !strings.Contains(view, want) {
			t.Errorf("Account view missing %q:\n%s", want, view)
		}
	}
}

// While a save is in flight the Position row withholds the dropdown (no double-submit) and shows a
// saving status line instead.
func TestPositionRowSavingHidesDropdown(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 10})
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab())
	m.positionSaving = true
	view := stripCSI(m.View())
	if strings.Contains(view, "▾") {
		t.Error("the position dropdown is shown while a save is in flight")
	}
	if !strings.Contains(view, "saving…") {
		t.Errorf("no saving status shown:\n%s", view)
	}
}

// A near-full-width position value must not wrap: the value and its dropdown are concatenated
// (marker-safe) rather than laid out with Style.Width()/PlaceHorizontal on marker-bearing content,
// which would shred the row on the lipgloss marker-width trap.
func TestPositionRowLongValueStaysSingleLine(t *testing.T) {
	long := strings.Repeat("Xy", 18) // 36 cols -> row ~50, at optionsWidth (50)
	m := newOptModal(domain.Profile{PositionID: 10, PositionName: long})
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab())
	row := m.positionRow()
	if strings.Contains(row, "\n") {
		t.Errorf("position row wrapped to multiple lines (marker-unsafe layout):\n%q", row)
	}
	if !strings.Contains(row, "▾") {
		t.Error("the long-value row dropped the dropdown affordance")
	}
}

// Clicking the position dropdown zone opens the picker (the mouse counterpart to enter on the control).
func TestPositionControlClickOpensPicker(t *testing.T) {
	m := newOptModal(domain.Profile{PositionID: 10})
	m = drive(m, optionsPositionsLoaded{positions: samplePositions}, optKeyTab(), optKeyTab())
	zone.Scan(m.View())
	z := settleZone(t, "opt.pos")
	var am appModal = m
	am, _ = am.Update(tea.MouseClickMsg{X: (z.StartX + z.EndX) / 2, Y: z.StartY, Button: tea.MouseLeft})
	if am.(optionsModal).picking != pickerPosition {
		t.Error("clicking the pen did not open the position picker")
	}
}

// The theme popup shows a palette-preview panel beside the list, titled with — and coloured to — the
// currently highlighted theme, so the palette follows the cursor.
func TestThemePickerShowsPalette(t *testing.T) {
	m := newOptModal(domain.Profile{})
	m = drive(m, optKeyTab(), optKeyEnter()) // open theme popup, tokyo-night highlighted
	view := stripCSI(m.View())
	for _, want := range []string{"Tokyo Night", "Accent", "Primary", "Success", "Muted"} {
		if !strings.Contains(view, want) {
			t.Errorf("theme palette preview missing %q:\n%s", want, view)
		}
	}
	// the preview follows the cursor, not the applied theme: moving to Dracula makes it the previewed
	// theme (asserted directly — "Dracula" is in the list on every view, so a substring check would not
	// discriminate) and titles the panel accordingly.
	m = drive(m, keyPress("down"))
	if got := m.pickedTheme().Name; got != "dracula" {
		t.Errorf("previewed theme did not follow the cursor: %q, want dracula", got)
	}
	if c := strings.Count(stripCSI(m.View()), "Dracula"); c != 2 { // list row + panel title
		t.Errorf("Dracula appears %d times, want 2 (list row + preview title)", c)
	}
}
