package widgets

import (
	"regexp"
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var csi = regexp.MustCompile("\\x1b\\[[0-9;]*[A-Za-z]")

// stripStyles drops the color escapes and zone markers, leaving the text the user reads.
func stripStyles(s string) string { return csi.ReplaceAllString(zone.Scan(s), "") }

// An explicit Color survives into the rendered label (a destructive "Unassigned").
func TestListPickerOptionColor(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	opts := []PickerOption{
		{Value: "1", Label: "Alice"},
		{Value: "", Label: "Unassigned", Color: s.Theme.Error},
	}
	p := NewListPicker("Assign", opts, "1", 8, 24) // cursor seeds on Alice, so Unassigned is not highlighted
	view := p.View(s)
	want := lipgloss.NewStyle().Foreground(s.Theme.Error).Bold(false).Render("Unassigned")
	if !strings.Contains(view, want) {
		t.Errorf("a colored option should render its label in the error color:\n%s", view)
	}
}

var listOpts = []PickerOption{
	{Value: "1", Label: "Alpha"}, {Value: "2", Label: "Beta"}, {Value: "3", Label: "Gamma"},
}

func TestListPickerSeedsCurrent(t *testing.T) {
	p := NewListPicker("Pick", listOpts, "2", 8, 20)
	if p.Cursor != 1 {
		t.Fatalf("seeded cursor %d, want 1 (Beta)", p.Cursor)
	}
	if got, _ := p.Selected(); got.Label != "Beta" {
		t.Errorf("selected = %q, want Beta", got.Label)
	}
}

func TestListPickerMoveWraps(t *testing.T) {
	p := NewListPicker("Pick", listOpts, "1", 8, 20) // cursor 0
	if got := p.Move(-1).Cursor; got != 2 {
		t.Errorf("up from the top wrapped to %d, want 2", got)
	}
	if got := p.Move(1).Move(1).Move(1).Cursor; got != 0 {
		t.Errorf("three downs from the top wrapped to %d, want 0", got)
	}
}

func TestListPickerViewListsOptions(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewListPicker("Themes", listOpts, "1", 8, 20)
	view := zone.Scan(p.View(s))
	for _, want := range []string{"Themes", "Alpha", "Beta", "Gamma"} {
		if !strings.Contains(view, want) {
			t.Errorf("view missing %q:\n%s", want, view)
		}
	}
}

func TestListPickerClickHitsOption(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewListPicker("Pick", listOpts, "1", 8, 20)
	const target = 2
	settleZone(t, p.View(s), ListPickerOptZone(target))
	z := zone.Get(ListPickerOptZone(target))
	click := tea.MouseClickMsg{X: z.StartX, Y: z.StartY, Button: tea.MouseLeft}
	if got := p.HitOption(click); got != target {
		t.Fatalf("HitOption = %d, want %d", got, target)
	}
}

func keyRune(r rune) tea.KeyPressMsg { return tea.KeyPressMsg{Code: r, Text: string(r)} }

// Filtering is a case-insensitive substring match. Navigation wraps within the matches.
func TestListPickerFilters(t *testing.T) {
	opts := []PickerOption{
		{Value: "1", Label: "Hong"}, {Value: "2", Label: "Kim Younghee"},
		{Value: "3", Label: "Kim Cheolsu"}, {Value: "4", Label: "Lee"},
	}
	p := NewSearchableListPicker("Assign", opts, "", 8, 30)
	if !p.Searchable() {
		t.Fatal("NewSearchableListPicker should be searchable")
	}
	for _, r := range "kim" {
		p = p.Filter(keyRune(r))
	}
	if got, _ := p.Selected(); got.Label != "Kim Younghee" {
		t.Errorf("after filtering 'kim', top match = %q, want Kim Younghee", got.Label)
	}
	p = p.Move(1)
	if got, _ := p.Selected(); got.Label != "Kim Cheolsu" {
		t.Errorf("move should land on the second match, got %q", got.Label)
	}
	p = p.Move(1)
	if got, _ := p.Selected(); got.Label != "Kim Younghee" {
		t.Errorf("move should wrap within the 2 matches, got %q", got.Label)
	}
	if NewListPicker("Pick", opts, "", 8, 30).Searchable() {
		t.Error("NewListPicker should not be searchable")
	}
}

// Notes print under their own option, in the warning color.
func TestListPickerNotesRenderUnderTheOption(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewListPicker("Move", []PickerOption{
		{Value: "1", Label: "Start"},
		{Value: "2", Label: "Resolve", Notes: []string{"needs an assignee", "blocked by ENG-4"}},
	}, "1", 8, 40)

	view := p.View(s)
	plain := stripStyles(view)
	for _, want := range []string{"needs an assignee", "blocked by ENG-4"} {
		if !strings.Contains(plain, want) {
			t.Errorf("a note should render under its option, missing %q:\n%s", want, plain)
		}
	}
	if !strings.Contains(view, lipgloss.NewStyle().Foreground(s.Theme.Warning).Render("needs an assignee")) {
		t.Errorf("notes should be drawn in the warning color:\n%s", view)
	}
	// order: the note follows its own option, not the one after it
	lines := strings.Split(plain, "\n")
	label, note := indexOfLine(lines, "Resolve"), indexOfLine(lines, "needs an assignee")
	if label < 0 || note != label+1 {
		t.Errorf("a note should sit directly under its label (label=%d note=%d):\n%s", label, note, plain)
	}
	if noted := indexOfLine(lines, "Start"); noted >= 0 && noted != label-1 {
		t.Errorf("an option with no notes should take a single row:\n%s", plain)
	}
}

// A long note wraps instead of being clipped. A half-shown guard message is worse than useless.
func TestListPickerNoteWrapsInsteadOfTruncating(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewListPicker("Move", []PickerOption{
		{Value: "1", Label: "Resolve", Notes: []string{"blocked by unresolved issues: ENG-4, ENG-9, ENG-12, ENG-31"}},
	}, "1", 8, 32)

	plain := stripStyles(p.View(s))
	if strings.Contains(plain, "…") {
		t.Errorf("a long note must wrap, not ellipsize:\n%s", plain)
	}
	for _, want := range []string{"blocked by unresolved", "ENG-31"} {
		if !strings.Contains(plain, want) {
			t.Errorf("the wrap dropped %q:\n%s", want, plain)
		}
	}
	if got := strings.Count(plain, "ENG-31"); got != 1 {
		t.Errorf("the tail should appear once, got %d:\n%s", got, plain)
	}
}

// A click on a caveat picks the option it explains, not whatever is underneath.
func TestListPickerClickOnANoteHitsItsOption(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewListPicker("Move", []PickerOption{
		{Value: "1", Label: "Start"},
		{Value: "2", Label: "Resolve", Notes: []string{"needs an assignee", "blocked by ENG-4"}},
	}, "1", 8, 40)

	settleZone(t, p.View(s), ListPickerOptZone(1))
	z := zone.Get(ListPickerOptZone(1))
	if rows := z.EndY - z.StartY + 1; rows != 3 {
		t.Errorf("the zone should span the label and both notes, got %d rows", rows)
	}
	// the last row of the block is a note, not the label
	if got := p.HitOption(tea.MouseClickMsg{X: z.StartX + 4, Y: z.EndY, Button: tea.MouseLeft}); got != 1 {
		t.Errorf("a click on a note should select its option, got %d", got)
	}
}

// indexOfLine is the first rendered line containing sub, or -1.
func indexOfLine(lines []string, sub string) int {
	for i, l := range lines {
		if strings.Contains(l, sub) {
			return i
		}
	}
	return -1
}

// maxRows is a budget of lines, not of options, so notes cannot outgrow the space the host allowed.
func TestListPickerNotesRespectTheRowBudget(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	var opts []PickerOption
	for _, v := range []string{"1", "2", "3", "4"} {
		opts = append(opts, PickerOption{Value: v, Label: "Move " + v, Notes: []string{"needs " + v, "also " + v}})
	}
	p := NewListPicker("Move", opts, "1", 6, 40)

	lines := strings.Split(stripStyles(p.View(s)), "\n")
	body := len(lines) - 3 // two borders and the n/total indicator
	if body > 6 {
		t.Errorf("the body should stay inside the 6-line budget, got %d:\n%s", body, strings.Join(lines, "\n"))
	}
	if !strings.Contains(stripStyles(p.View(s)), "Move 1") {
		t.Error("the option under the cursor must stay inside the window")
	}
	if !strings.Contains(stripStyles(p.View(s)), "1/4") {
		t.Error("a windowed list should show its position, counting options rather than lines")
	}
}

// Guards the plain path: a note-free picker still windows one line per option, as it always did.
func TestListPickerWithoutNotesIsOneLinePerOption(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	p := NewListPicker("Pick", listOpts, "1", 8, 20)

	if lines := strings.Split(stripStyles(p.View(s)), "\n"); len(lines) != len(listOpts)+2 {
		t.Errorf("a note-free picker should be one row per option plus its borders, got %d lines:\n%s",
			len(lines), strings.Join(lines, "\n"))
	}
	top, visible := p.window()
	if top != 0 || visible != len(listOpts) {
		t.Errorf("window() = (%d, %d), want the whole list", top, visible)
	}
}

// Notes are capped, and the cut is marked so the reader knows there is more.
func TestListPickerNotesAreCapped(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	notes := make([]string, 20)
	for i := range notes {
		notes[i] = "condition " + string(rune('a'+i))
	}
	p := NewListPicker("Move", []PickerOption{{Value: "1", Label: "Resolve", Notes: notes}}, "1", 40, 40)

	plain := stripStyles(p.View(s))
	if body := len(strings.Split(plain, "\n")) - 2; body > 1+maxNoteRows {
		t.Errorf("an option should render at most %d note rows, got %d:\n%s", maxNoteRows, body-1, plain)
	}
	if !strings.Contains(plain, "…") {
		t.Errorf("a truncated note list should say so:\n%s", plain)
	}
}

// The cursor's option is drawn whole, so its notes are bounded too or the box outgrows its host.
func TestListPickerCursorOptionCannotExceedTheBudget(t *testing.T) {
	zone.NewGlobal()
	s := theme.New(theme.TokyoNight())
	notes := make([]string, 10)
	for i := range notes {
		notes[i] = "condition " + string(rune('a'+i))
	}
	const budget = 5
	p := NewListPicker("Move", []PickerOption{
		{Value: "1", Label: "Resolve", Notes: notes},
		{Value: "2", Label: "Start"},
	}, "1", budget, 40)

	lines := strings.Split(stripStyles(p.View(s)), "\n")
	if body := len(lines) - 3; body > budget { // two borders and the n/total indicator
		t.Errorf("one option overflowed the %d-line budget with %d rows:\n%s", budget, body, strings.Join(lines, "\n"))
	}
	if _, visible := p.window(); visible < 1 {
		t.Error("the option under the cursor must still be drawn")
	}
	if !strings.Contains(stripStyles(p.View(s)), "…") {
		t.Error("notes cut by the budget should say there is more")
	}
}
