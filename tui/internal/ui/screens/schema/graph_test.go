package schema

import (
	"fmt"
	"regexp"
	"strings"
	"testing"
	"unicode/utf8"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// graphLines renders the graph with nothing selected, for tests that only look at layout.
func graphLines(d domain.WorkflowDetail, s theme.Styles, w int) []string {
	lines, _, _ := renderWorkflowGraph(d, s, w, wfElem{}, wfElem{}, true)
	return lines
}

func exampleWorkflow() domain.WorkflowDetail {
	return domain.WorkflowDetail{
		ID: 1, Name: "Development", InitialStateID: 1,
		States: []domain.WorkflowState{
			{ID: 1, Label: "To Do", Category: "INITIAL", Color: "ANSI_BLUE"},
			{ID: 2, Label: "In Progress", Category: "ACTIVE", Color: "ANSI_YELLOW"},
			{ID: 3, Label: "In Review", Category: "ACTIVE", Color: "INDIGO"},
			{ID: 4, Label: "Done", Category: "COMPLETED", Color: "ANSI_GREEN"},
			{ID: 5, Label: "Cancelled", Category: "ABORTED", Color: "ANSI_BRIGHT_BLACK"},
		},
		Transitions: []domain.WorkflowTransition{
			{ID: 10, Label: "Start", SourceID: 1, TargetID: 2},
			{ID: 11, Label: "Submit", SourceID: 2, TargetID: 3, Guards: []domain.WorkflowGuard{{Type: "ASSIGNEE_REQUIRED"}}},
			{ID: 12, Label: "Approve", SourceID: 3, TargetID: 4, Guards: []domain.WorkflowGuard{{Type: "APPROVAL_REQUIRED", Params: map[string]any{"min_approvals": 2}}}},
			{ID: 13, Label: "Reject", SourceID: 3, TargetID: 2},
			{ID: 14, Label: "Abandon", SourceID: 2, TargetID: 5},
			{ID: 15, Label: "Reopen", SourceID: 5, TargetID: 1},
		},
	}
}

func koreanWorkflow() domain.WorkflowDetail {
	return domain.WorkflowDetail{
		ID: 1, Name: "개발 워크플로우", InitialStateID: 1,
		States: []domain.WorkflowState{
			{ID: 1, Label: "할 일", Category: "INITIAL", Color: "ANSI_BLUE"},
			{ID: 2, Label: "진행 중", Category: "ACTIVE", Color: "ANSI_YELLOW"},
			{ID: 3, Label: "검토 중", Category: "ACTIVE", Color: "INDIGO"},
			{ID: 4, Label: "완료", Category: "COMPLETED", Color: "ANSI_GREEN"},
			{ID: 5, Label: "취소됨", Category: "ABORTED", Color: "ANSI_BRIGHT_BLACK"},
		},
		Transitions: []domain.WorkflowTransition{
			{ID: 10, Label: "시작", SourceID: 1, TargetID: 2},
			{ID: 11, Label: "제출하기", SourceID: 2, TargetID: 3, Guards: []domain.WorkflowGuard{{Type: "ASSIGNEE_REQUIRED"}}},
			{ID: 12, Label: "승인", SourceID: 3, TargetID: 4, Guards: []domain.WorkflowGuard{{Type: "APPROVAL_REQUIRED", Params: map[string]any{"min_approvals": 2}}}},
			{ID: 13, Label: "반려하기", SourceID: 3, TargetID: 2},
			{ID: 14, Label: "포기", SourceID: 2, TargetID: 5},
			{ID: 15, Label: "다시 열기", SourceID: 5, TargetID: 1},
		},
	}
}

func labels(states []domain.WorkflowState) []string {
	out := make([]string, len(states))
	for i, st := range states {
		out[i] = st.Label
	}
	return out
}

// The happy path stays contiguous with terminals trailing, so the diagram reads straight down.
func TestOrderStatesHappyPathContiguous(t *testing.T) {
	got := orderStates(exampleWorkflow())
	want := []string{"To Do", "In Progress", "In Review", "Done", "Cancelled"}
	for i, st := range got {
		if st.Label != want[i] {
			t.Fatalf("state order = %v, want %v", labels(got), want)
		}
	}
}

// A line wider than the panel wraps in the Details renderer and shifts every line below it.
func TestGraphLinesFitWidth(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	const width = 64
	lines := graphLines(exampleWorkflow(), s, width)
	if len(lines) < 2 {
		t.Fatal("expected a multi-line graph")
	}
	for i, ln := range lines {
		if got := lipgloss.Width(ln); got > width {
			t.Fatalf("line %d width = %d, want <= %d: %q", i, got, width, stripANSI(ln))
		}
	}
}

// Transitions fanning into the same state must not stack their labels on one row.
func TestGraphFanInLabelsDoNotOverlap(t *testing.T) {
	d := domain.WorkflowDetail{
		ID: 1, Name: "Review", InitialStateID: 1,
		States: []domain.WorkflowState{
			{ID: 1, Label: "To Do", Category: "INITIAL", Color: "ANSI_BLUE"},
			{ID: 2, Label: "In Progress", Category: "ACTIVE", Color: "ANSI_YELLOW"},
			{ID: 3, Label: "In Review", Category: "ACTIVE", Color: "INDIGO"},
			{ID: 4, Label: "Done", Category: "COMPLETED", Color: "ANSI_GREEN"},
			{ID: 5, Label: "Cancelled", Category: "ABORTED", Color: "ANSI_BRIGHT_BLACK"},
			{ID: 6, Label: "Trash", Category: "ABORTED", Color: "ANSI_RED"},
		},
		Transitions: []domain.WorkflowTransition{
			{ID: 10, Label: "Start", SourceID: 1, TargetID: 2},
			{ID: 11, Label: "Submit", SourceID: 2, TargetID: 3},
			{ID: 12, Label: "Approve", SourceID: 3, TargetID: 4},
			{ID: 13, Label: "Cancel", SourceID: 2, TargetID: 5},
			{ID: 14, Label: "Reject to bin", SourceID: 3, TargetID: 5},
			{ID: 15, Label: "To trash", SourceID: 2, TargetID: 6},
		},
	}
	s := theme.New(theme.TokyoNight())
	lines, _, hits := renderWorkflowGraph(d, s, 80, wfElem{}, wfElem{}, true)
	// each routed transition's label is centered on its own lane, so no two share a center column
	cols := map[int]int{}
	for _, id := range []int{13, 14, 15} {
		rc, ok := hits[wfElem{elemTransition, id}]
		if !ok {
			t.Errorf("transition %d has no label rect", id)
			continue
		}
		mid := (rc.c0 + rc.c1) / 2
		if other, dup := cols[mid]; dup {
			t.Errorf("transitions %d and %d center on the same column %d", other, id, mid)
		}
		cols[mid] = id
	}
	// a multi-word name wraps at spaces, so every word still appears
	plain := stripANSI(strings.Join(lines, "\n"))
	for _, want := range []string{"Cancel", "Reject", "bin", "trash"} {
		if !strings.Contains(plain, want) {
			t.Errorf("label word %q missing (wrap dropped it?):\n%s", want, plain)
		}
	}
}

// accentedRunes returns the runes inside accent-colored runs, to check the accent reaches the arrows.
func accentedRunes(lines []string, accentSGR string) string {
	var out strings.Builder
	for _, ln := range lines {
		active := false
		for len(ln) > 0 {
			if ln[0] == 0x1b {
				end := strings.IndexByte(ln, 'm')
				if end < 0 {
					break
				}
				seq := ln[:end+1]
				switch {
				case strings.Contains(seq, "[0m") || seq == "\x1b[m":
					active = false
				case strings.Contains(seq, accentSGR):
					active = true
				default:
					active = false
				}
				ln = ln[end+1:]
				continue
			}
			r, size := utf8.DecodeRuneInString(ln)
			if active {
				out.WriteRune(r)
			}
			ln = ln[size:]
		}
	}
	return out.String()
}

// Selecting a transition accents its whole arrow path, not just its name.
func TestGraphSelectedTransitionHighlightsArrow(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	accent := "38;2;255;158;100" // TokyoNight accent (#ff9e64)
	// "Abandon" (id 14) is a jump: In Progress -> Cancelled, drawn through the right gutter
	sel, _, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{elemTransition, 14}, wfElem{}, true)
	got := accentedRunes(sel, accent)
	if !strings.ContainsAny(got, "│─└┐┌┘├┤┬┴┼▼▶◀↺") {
		t.Errorf("selected transition's arrow line is not accented (accented runes = %q)", got)
	}
}

// Selection reads from colour only: heavy glyphs would shift the whole diagram.
func TestGraphSelectedStateIsAccentOutlined(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	const width = 66
	accent := "38;2;255;158;100" // TokyoNight accent (#ff9e64)
	light := graphLines(exampleWorkflow(), s, width)
	if plain := stripANSI(strings.Join(light, "\n")); strings.ContainsAny(plain, "┏┓┗┛┃━┠┨┰┸") {
		t.Fatalf("unselected diagram already has heavy glyphs:\n%s", plain)
	}
	sel, _, _ := renderWorkflowGraph(exampleWorkflow(), s, width, wfElem{elemState, 2}, wfElem{}, true)
	// selection must never introduce heavy glyphs
	if plain := stripANSI(strings.Join(sel, "\n")); strings.ContainsAny(plain, "┏┓┗┛┃━┠┨┰┸") {
		t.Errorf("selecting a state introduced heavy glyphs (selection must be colour-only):\n%s", plain)
	}
	// the selected box's perimeter is painted accent
	if got := accentedRunes(sel, accent); !strings.ContainsAny(got, "┌┐└┘─│") {
		t.Errorf("selected state's box border is not accent-coloured (accented runes = %q)", got)
	}
	// width is preserved
	if w1, w2 := lipgloss.Width(light[0]), lipgloss.Width(sel[0]); w1 != w2 {
		t.Errorf("selecting a state changed the diagram width: %d vs %d", w1, w2)
	}
}

// Every state, guard, and routed transition must appear as a labeled arrow, not a list.
func TestGraphShowsStatesAndArrows(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	plain := stripANSI(strings.Join(graphLines(exampleWorkflow(), s, 66), "\n"))
	for _, want := range []string{
		"To Do", "In Progress", "In Review", "Done", "Cancelled", // states
		"Start", "Submit", "assignee", "Approve", "approval×2", // spine + guards
		"Reject", "Reopen", "Abandon", // routed edge labels, centered on their arrows
		"▼", "▶", "◀", // arrowheads: spine down, return into left, jump into right
	} {
		if !strings.Contains(plain, want) {
			t.Errorf("graph is missing %q\n%s", want, plain)
		}
	}
}

// Each extra guard takes its own row, so the spine arrow and the diagram grow taller.
func TestGraphListsMultipleGuards(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	oneGuard := graphLines(exampleWorkflow(), s, 66)

	multi := exampleWorkflow()
	multi.Transitions[2].Guards = []domain.WorkflowGuard{
		{Type: "APPROVAL_REQUIRED", Params: map[string]any{"min_approvals": 2}},
		{Type: "CHILD_ISSUES_RESOLVE_REQUIRED"},
		{Type: "LINKED_BRANCH_REQUIRED"},
	}
	multiLines := graphLines(multi, s, 66)
	plain := stripANSI(strings.Join(multiLines, "\n"))
	for _, g := range []string{"approval×2", "children", "branch"} {
		if !strings.Contains(plain, g) {
			t.Errorf("guard %q is not listed on its own row", g)
		}
	}
	if len(multiLines) <= len(oneGuard) {
		t.Fatalf("arrow did not grow for extra guards: 1-guard=%d rows, 3-guard=%d rows", len(oneGuard), len(multiLines))
	}
}

// A selected element reports its row (for scroll-into-view) and only it is painted accent.
func TestGraphHighlightsSelectedElement(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	accent := "38;2;255;158;100" // TokyoNight accent (#ff9e64)

	// select the "In Review" state (id 3)
	lines, rows, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{elemState, 3}, wfElem{}, true)
	if _, ok := rows[wfElem{elemState, 3}]; !ok {
		t.Fatal("selected state has no reported row")
	}
	joined := strings.Join(lines, "\n")
	if !strings.Contains(joined, accent) {
		t.Fatal("selected state is not painted in the accent color")
	}

	// selecting nothing must not paint accent anywhere
	plainLines, _, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{}, wfElem{}, true)
	if strings.Contains(strings.Join(plainLines, "\n"), accent) {
		t.Fatal("nothing selected, but the accent color appears")
	}

	// every state and transition must have a reported row for scroll-into-view
	_, rows2, _ := renderWorkflowGraph(exampleWorkflow(), s, 66, wfElem{}, wfElem{}, true)
	for _, id := range []int{1, 2, 3, 4, 5} {
		if _, ok := rows2[wfElem{elemState, id}]; !ok {
			t.Errorf("state %d has no reported row", id)
		}
	}
	for _, id := range []int{10, 11, 12, 13, 14, 15} {
		if _, ok := rows2[wfElem{elemTransition, id}]; !ok {
			t.Errorf("transition %d has no reported row", id)
		}
	}
}

// The diagram is centered, not flush left: equal margins on the top border row.
func TestGraphIsCentered(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	const width = 70
	first := stripANSI(graphLines(exampleWorkflow(), s, width)[0]) // To Do top border, no side lanes
	lead := len(first) - len(strings.TrimLeft(first, " "))
	trail := len(first) - len(strings.TrimRight(first, " "))
	if diff := lead - trail; diff < -1 || diff > 1 {
		t.Fatalf("diagram not centered: lead=%d trail=%d (%q)", lead, trail, first)
	}
}

// A colored state renders as a chip (a background behind the label), not a colored box border.
func TestGraphStateRendersColorChip(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	out := strings.Join(graphLines(exampleWorkflow(), s, 64), "\n")
	// a 48;2 or 48;5 background introducer means a chip was painted behind a label
	if !regexp.MustCompile(`\x1b\[[0-9;]*48[;:]`).MatchString(out) {
		t.Fatal("no state chip carries a background color (color drawn as border, not chip?)")
	}
}

// Hangul labels are display-width 2, so a renderer placing runes by index overflows the width.
// A very long Korean label must not panic the clipper either.
func TestGraphFitsWidthWithCJK(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	const width = 64
	for i, ln := range graphLines(koreanWorkflow(), s, width) {
		if got := lipgloss.Width(ln); got > width {
			t.Fatalf("CJK line %d width = %d, want <= %d: %q", i, got, width, stripANSI(ln))
		}
	}
	long := koreanWorkflow()
	long.Transitions[3].Label = strings.Repeat("가", 60)
	for i, ln := range graphLines(long, s, width) {
		if got := lipgloss.Width(ln); got > width {
			t.Fatalf("long-label line %d width = %d, want <= %d", i, got, width)
		}
	}
}

// Rows must be exactly the panel width at every scroll offset, or the border and scrollbar drift.
func TestWorkflowPanelRowsExactWidthCJK(t *testing.T) {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	m := New(d)
	m, _ = m.Update(tea.WindowSizeMsg{Width: 118, Height: 26})
	m, _ = m.Update(LoadedMsg{Workflows: []domain.WorkflowSummary{{ID: 1, Name: "개발"}}})
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(WorkflowDetailLoadedMsg{ID: 1, Detail: koreanWorkflow()})

	_, rightW := m.panelWidths()
	for off := 0; off <= m.detailScrollMax(); off++ {
		mm := m
		mm.detailScroll = off
		for i, ln := range strings.Split(stripANSI(zone.Scan(mm.detailPanel())), "\n") {
			if got := lipgloss.Width(ln); got != rightW {
				t.Fatalf("off=%d panel row %d = %d cells, want exactly rightW=%d: %q", off, i, got, rightW, ln)
			}
		}
	}
}

// The workflow flows into Details as ordinary vertical content that never exceeds the terminal width.
func TestWorkflowDetailFlowsVertically(t *testing.T) {
	zone.NewGlobal()
	d := deps.Deps{Styles: theme.New(theme.TokyoNight()), Glyphs: glyph.New(glyph.Unicode), Mouse: true}
	m := New(d)
	m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 20})
	m, _ = m.Update(LoadedMsg{Workflows: []domain.WorkflowSummary{{ID: 1, Name: "Development"}}})
	m, _ = m.setFocus(paneWorkflows)
	m, _ = m.Update(WorkflowDetailLoadedMsg{ID: 1, Detail: exampleWorkflow()})

	contentW := m.detailContentW()
	for i, ln := range m.detailContent() {
		if got := lipgloss.Width(ln); got != contentW {
			t.Fatalf("content line %d = %d cells, want contentW=%d: %q", i, got, contentW, stripANSI(ln))
		}
	}
	if m.detailScrollMax() <= 0 {
		t.Fatalf("the flow diagram should be taller than the panel, got scrollMax %d", m.detailScrollMax())
	}
	for i, ln := range strings.Split(stripANSI(zone.Scan(m.View())), "\n") {
		if got := lipgloss.Width(ln); got > m.width {
			t.Fatalf("View line %d = %d cells, want <= %d", i, got, m.width)
		}
	}
}

// Degenerate graphs must never panic and must still produce width-fitting output.
func TestGraphDegenerateInputs(t *testing.T) {
	s := theme.New(theme.TokyoNight())
	cases := map[string]domain.WorkflowDetail{
		"empty":       {ID: 1, Name: "x"},
		"single":      {ID: 1, InitialStateID: 1, States: []domain.WorkflowState{{ID: 1, Label: "Only", Category: "INITIAL"}}},
		"no-initial":  {ID: 1, InitialStateID: 999, States: []domain.WorkflowState{{ID: 1, Label: "A", Category: "ACTIVE"}, {ID: 2, Label: "B", Category: "COMPLETED"}}},
		"self-loop":   {ID: 1, InitialStateID: 1, States: []domain.WorkflowState{{ID: 1, Label: "A", Category: "INITIAL"}}, Transitions: []domain.WorkflowTransition{{ID: 1, Label: "loop", SourceID: 1, TargetID: 1}}},
		"unreachable": {ID: 1, InitialStateID: 1, States: []domain.WorkflowState{{ID: 1, Label: "A", Category: "INITIAL"}, {ID: 2, Label: "Island", Category: "COMPLETED"}}},
		"long-label":  {ID: 1, InitialStateID: 1, States: []domain.WorkflowState{{ID: 1, Label: strings.Repeat("Very Long State Name ", 3), Category: "INITIAL"}}},
		"back+jump":   {ID: 1, InitialStateID: 1, States: []domain.WorkflowState{{ID: 1, Label: "A", Category: "INITIAL"}, {ID: 2, Label: "B", Category: "ACTIVE"}, {ID: 3, Label: "C", Category: "COMPLETED"}, {ID: 4, Label: "X", Category: "ABORTED"}}, Transitions: []domain.WorkflowTransition{{ID: 1, Label: "go", SourceID: 1, TargetID: 2}, {ID: 2, Label: "ok", SourceID: 2, TargetID: 3}, {ID: 3, Label: "back", SourceID: 3, TargetID: 1}, {ID: 4, Label: "kill", SourceID: 2, TargetID: 4}}},
	}
	for _, width := range []int{20, 50} {
		for name, d := range cases {
			t.Run(fmt.Sprintf("%s@%d", name, width), func(t *testing.T) {
				lines := graphLines(d, s, width)
				if len(lines) == 0 {
					t.Fatalf("%s produced no output", name)
				}
				for i, ln := range lines {
					if lipgloss.Width(ln) > width {
						t.Fatalf("%s line %d exceeds width", name, i)
					}
				}
			})
		}
	}
}
