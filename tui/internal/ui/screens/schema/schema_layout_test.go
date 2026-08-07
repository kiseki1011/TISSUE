package schema

import (
	"fmt"
	"regexp"
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var csi = regexp.MustCompile("\\x1b\\[[0-9;]*[A-Za-z]")

func plain(s string) string { return csi.ReplaceAllString(zone.Scan(s), "") }

// fieldOptions builds a field's options from bare names (ids start at 1), for test fixtures.
func fieldOptions(names ...string) []domain.FieldOption {
	opts := make([]domain.FieldOption, len(names))
	for i, n := range names {
		opts[i] = domain.FieldOption{ID: i + 1, Name: n}
	}
	return opts
}

func mk(w, h, nTypes, nWf int, longNames bool) Model {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	m := New(d)
	m, _ = m.Update(tea.WindowSizeMsg{Width: w, Height: h})
	var ts []domain.IssueTypeSummary
	for i := 0; i < nTypes; i++ {
		name := fmt.Sprintf("T%d", i)
		if longNames {
			name = fmt.Sprintf("T%d An Extremely Long Issue Type Name That Exceeds The Column Width Easily", i)
		}
		ts = append(ts, domain.IssueTypeSummary{ID: i + 1, Name: name, Hierarchy: "STANDARD"})
	}
	var ws []domain.WorkflowSummary
	for i := 0; i < nWf; i++ {
		ws = append(ws, domain.WorkflowSummary{ID: 100 + i, Name: fmt.Sprintf("W%d", i)})
	}
	m, _ = m.Update(LoadedMsg{Types: ts, Workflows: ws})
	return m
}

func maxWidth(s string) int {
	w := 0
	for _, ln := range strings.Split(s, "\n") {
		w = max(w, lipgloss.Width(ln))
	}
	return w
}

// A list row must stay exactly one terminal line: long items must never wrap into extra rows — exercises BOTH the selected
// (cursor) row and the plain rows, and asserts the whole screen keeps its height budget.
func TestListItemNeverWraps(t *testing.T) {
	const w, h = 120, 25
	m := mk(w, h, 4, 3, true)
	if got := len(strings.Split(plain(m.leftColumn()), "\n")); got != h {
		t.Fatalf("leftColumn = %d rows, want m.height=%d (an item wrapped)", got, h)
	}
	if got := len(strings.Split(plain(m.View()), "\n")); got != h {
		t.Fatalf("View = %d rows, want m.height=%d", got, h)
	}
}

// An over-wide row does not wrap, it silently widens TitledBox, so the height assertions
// above cannot see it. Pin the width budget separately, or the row truncation can be
// deleted outright while the suite stays green.
func TestRenderedWidthStaysInBudget(t *testing.T) {
	for _, sz := range [][2]int{{120, 25}, {110, 24}, {80, 14}, {minWidth, minHeight}, {201, 41}} {
		w, h := sz[0], sz[1]
		m := mk(w, h, 6, 6, true)
		leftW, _ := m.panelWidths()
		if got := maxWidth(plain(m.leftColumn())); got != leftW {
			t.Errorf("%dx%d: leftColumn width = %d, want leftW=%d", w, h, got, leftW)
		}
		if got := maxWidth(plain(m.View())); got > w {
			t.Errorf("%dx%d: View width = %d, want <= %d", w, h, got, w)
		}
	}
}

// Field rows carry column alignment, so unlike the free-text description paragraph they
// must never reach the panel's wrap: a wrapped row shifts every line below it and its
// continuation loses the indent, reading as a separate field.
func TestFieldRowsFitContentWidth(t *testing.T) {
	for _, w := range []int{minWidth, 80, 100, 120, 200} {
		m := mk(w, 30, 3, 2, true)
		m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
			ID: 1, Name: "Epic",
			Fields: []domain.IssueField{
				{Name: "Acceptance Criteria That Runs Very Long Indeed", Type: "CHECKLIST", Required: true,
					Description: strings.Repeat("desc ", 30)},
				{Name: "Severity", Type: "SELECT_OPTION",
					Options: fieldOptions("blocker", "critical", "major", "minor", "trivial", "cosmetic", "wontfix")},
				{Name: "Untyped"},
			},
		}})
		contentW := m.detailContentW()
		for i, row := range m.fieldLines(1, contentW, map[wfElem]int{}, 0) {
			ln := plain(row)
			if strings.Contains(ln, "\n") {
				t.Fatalf("w=%d: field line %d spans multiple lines: %q", w, i, ln)
			}
			if got := lipgloss.Width(ln); got > contentW {
				t.Fatalf("w=%d: field line %d is %d cells, want <= %d: %q", w, i, got, contentW, ln)
			}
		}
	}
}

// Names and descriptions are admin free text and arrive unsanitized. An embedded newline
// survives ansi.Truncate, so lipgloss pads it into a SECOND row: the box grows past its
// height budget and every click below the offending item selects the wrong one.
func TestControlCharactersDoNotSplitRows(t *testing.T) {
	const w, h = 160, 25 // wide enough that the field description is not merely width-clipped
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	m := New(d)
	m, _ = m.Update(tea.WindowSizeMsg{Width: w, Height: h})
	m, _ = m.Update(LoadedMsg{
		Types: []domain.IssueTypeSummary{
			{ID: 1, Name: "Task\nBreaker", Hierarchy: "STANDARD"},
			{ID: 2, Name: "Tab\tbed", Hierarchy: "EPIC"},
		},
		Workflows: []domain.WorkflowSummary{{ID: 100, Name: "Multi\r\nLine"}},
	})
	if got := len(strings.Split(plain(m.leftColumn()), "\n")); got != h {
		t.Fatalf("leftColumn = %d rows, want m.height=%d (a control char split a row)", got, h)
	}

	m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
		ID: 1, Name: "Task\nBreaker",
		Fields: []domain.IssueField{{Name: "Repro\nSteps", Type: "TEXT", Description: "how to\nreproduce it"}},
	}})
	lines := m.fieldLines(1, m.detailContentW(), map[wfElem]int{}, 0)
	for i, ln := range lines {
		if strings.Contains(plain(ln), "\n") {
			t.Fatalf("field line %d spans multiple lines: %q", i, plain(ln))
		}
	}
	// the description must survive flattening (on its own indented line now), not be dropped
	if joined := plain(strings.Join(lines, " ")); !strings.Contains(joined, "reproduce it") {
		t.Fatalf("multi-line description was dropped from the field: %q", joined)
	}
}

// The Fields section renders "<TYPE>  <name>" with an edit pen flush right on the header line
// (no brackets around the type), and the description on its own indented line below.
func TestFieldRowFormat(t *testing.T) {
	m := mk(160, 30, 2, 1, false)
	m, _ = m.Update(TypeDetailLoadedMsg{ID: 1, Detail: domain.IssueTypeDetail{
		ID: 1, Name: "T0",
		Fields: []domain.IssueField{{ID: 7, Name: "Story Points", Type: "INTEGER", Description: "Relative effort"}},
	}})
	lines := m.fieldLines(1, m.detailContentW(), map[wfElem]int{}, 0)
	if len(lines) < 2 {
		t.Fatalf("field lines = %d, want at least 2 (header + description)", len(lines))
	}
	head := plain(lines[0])
	if strings.ContainsAny(head, "[]") {
		t.Errorf("field header still brackets the type: %q", head)
	}
	iType := strings.Index(head, "INTEGER")
	iName := strings.Index(head, "Story Points")
	if iType < 0 || iName < 0 || iType >= iName {
		t.Fatalf("field header order = type@%d name@%d, want type < name: %q", iType, iName, head)
	}
	if got := lipgloss.Width(head); got != m.detailContentW() {
		t.Errorf("field header = %d cells, want contentW=%d (pen not flush right)", got, m.detailContentW())
	}
	if desc := plain(lines[1]); !strings.Contains(desc, "Relative effort") {
		t.Errorf("description not on its own line: %q", desc)
	}
}

// The Details meta rows put a leading glyph in a fixed 12-cell label column. If a glyph is
// wide enough to overflow the label, detailRow's Width(12) wraps it to a second line, which
// pushes the whole Details panel down. Assert each glyph+label stays one line with the
// value flush at column 12.
func TestMetaRowGlyphsFitLabelColumn(t *testing.T) {
	zone.NewGlobal() // plain() -> zone.Scan needs the global manager, even with no marks
	s := theme.New(theme.TokyoNight())
	g := glyph.New(glyph.Nerd)
	cases := []struct{ glyph, label string }{
		{g.Or(g.Hierarchy, ""), "Hierarchy"}, // the longest label, tightest fit
		{g.Or(g.TransitionConnection, ""), "Workflow"},
		{g.Or(g.Computer, ""), "System"},
	}
	for _, c := range cases {
		row := plain(metaRow(s, c.glyph, c.label, "VALUE"))
		if strings.Contains(row, "\n") {
			t.Errorf("%s meta row wrapped: %q (glyph overflows the 12-cell label)", c.label, row)
			continue
		}
		if col := lipgloss.Width(row[:strings.Index(row, "VALUE")]); col != 15 {
			t.Errorf("%s value at column %d, want 15", c.label, col)
		}
	}
}

// The list is a Projects-style table: a header row, then airy (blank-line-above) data rows,
// all inset by the box padding. The padding is what the click hit-test offsets by, so a render
// that drops the header or the airy blank silently misaligns every row.
func TestListBoxPadding(t *testing.T) {
	const w, h = 120, 25
	// the requested geometry, spelled out rather than derived from the constants, so
	// zeroing listPadX/listPadY (or dropping the header/airy blank) fails this test
	const padX, padY = 3, 1
	m := mk(w, h, 4, 3, false)
	lines := strings.Split(plain(m.leftColumn()), "\n")
	if got := len(lines); got != h {
		t.Fatalf("leftColumn = %d rows, want m.height=%d", got, h)
	}

	// the row right after each panel's top rule is padding (a blank interior row). The Issue Types
	// and Workflows panels are borderless (TitledRule), so the side columns are blank spaces too.
	for _, top := range []int{0, m.typesHeight()} {
		if inner := strings.Trim(lines[top+1], "│┃ "); inner != "" {
			t.Fatalf("row after the top border should be padding, got %q", lines[top+1])
		}
	}

	// hasInset reports whether line, after its 1-cell side column and padX padding columns, begins
	// with want — the side column is a blank space (TitledRule is borderless), skipped either way.
	hasInset := func(line, want string) bool {
		r := []rune(line)
		if len(r) < 1+padX {
			return false
		}
		for i := 1; i <= padX; i++ {
			if r[i] != ' ' {
				return false
			}
		}
		return strings.HasPrefix(string(r[1+padX:]), want)
	}

	// the header row carries the column titles, inset by the padding
	header := lines[1+padY]
	if !hasInset(header, "Name") {
		t.Fatalf("types header = %q, want %d-col inset then Name", header, padX)
	}
	if !strings.Contains(header, "Hierarchy") {
		t.Errorf("types header missing the Hierarchy column: %q", header)
	}

	// the first data row sits one airy blank line below the header
	if first := lines[1+padY+1+1]; !hasInset(first, "T0") {
		t.Fatalf("first item row = %q, want %d-col inset then T0", first, padX)
	}

	if wfHeader := lines[m.typesHeight()+1+padY]; !hasInset(wfHeader, "Name") {
		t.Fatalf("workflows header = %q, want %d-col inset then Name", wfHeader, padX)
	}
	if wfFirst := lines[m.typesHeight()+1+padY+1+1]; !hasInset(wfFirst, "W0") {
		t.Fatalf("first workflow row = %q, want %d-col inset then W0", wfFirst, padX)
	}
}

// Click-to-select must resolve to the row actually painted there. Reads the real rendered
// workflows box, so it catches a render/hit-test height mismatch whatever helper each side
// uses. Each airy data row spans two lines (a blank separator above its content), and a click
// on either line must select it.
func TestWorkflowClickMatchesRenderedRow(t *testing.T) {
	const w, h = 120, 25 // odd height: typesHeight()=12 vs workflowHeight()=13
	m := mk(w, h, 3, 30, false)
	m.wfCursor = 25

	lines := strings.Split(plain(m.leftColumn()), "\n")
	wfTop := m.typesHeight() // the workflows box top-border line in the full column
	rowRe := regexp.MustCompile(`\bW(\d+)\b`)
	for i := 0; i < m.wfInnerH(); i++ {
		// content line of visible row i: border + padding + header + i airy rows + its blank
		contentY := 1 + listPadY + 1 + rowHeight*i + 1
		painted := lines[wfTop+contentY]
		mm := rowRe.FindStringSubmatch(painted)
		if mm == nil {
			continue // blank filler row past the data
		}
		idx, ok := rowAt(contentY, m.wfListTop(), len(m.workflows), m.wfInnerH())
		if !ok {
			t.Fatalf("row %d paints %q but is not clickable", i, strings.TrimSpace(painted))
		}
		if got := fmt.Sprintf("W%s", mm[1]); m.workflows[idx].Name != got {
			t.Fatalf("row %d paints %s but a click selects %s", i, got, m.workflows[idx].Name)
		}
		// the blank separator line above the content must resolve to the same row
		if r, ok := rowAt(contentY-1, m.wfListTop(), len(m.workflows), m.wfInnerH()); !ok || r != idx {
			t.Fatalf("blank line above row %d resolves to %d (ok=%v), want %d", i, r, ok, idx)
		}
	}
}
