package project

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
)

// The list header shows the Priority column as a compact glyph whose text fallback (unicode mode) is
// "Pri", not the full word "Priority".
func TestListHeaderPriorityLabel(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	h := plain(m.headerRow(18))
	if !strings.Contains(h, "Pri") || strings.Contains(h, "Priority") {
		t.Errorf("the list header should show the compact 'Pri' fallback, not 'Priority':\n%s", h)
	}
}

// The Type cell paints the name on a background colour chip (not a side swatch) when the backend gave
// the type a colour, and falls back to plain text otherwise.
func TestTypeCellChip(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	colored := m.typeCell("Bug", "INDIGO", 12) // hex colour -> truecolor background SGR
	if !strings.Contains(colored, "48;2;") {
		t.Errorf("a coloured type should paint a background chip, got %q", colored)
	}
	pc := plain(colored)
	if !strings.Contains(pc, "Bug") || strings.Contains(pc, "██") {
		t.Errorf("the chip should show the name with no side swatch, got %q", pc)
	}
	if got := lipgloss.Width(colored); got != 12 {
		t.Errorf("the Type cell must be exactly the column width, got %d want 12", got)
	}
	noColor := m.typeCell("Bug", "", 12)
	if strings.Contains(noColor, "48;2;") {
		t.Errorf("a type with no colour should have no background chip, got %q", noColor)
	}
}

// progressBar fills the fraction of the width and leaves the rest as track cells, clamping out-of-range.
func TestProgressBar(t *testing.T) {
	th := testDeps().Styles.Theme
	bar := plain(progressBar(50, 10, th.Primary, th.Muted))
	if got := strings.Count(bar, "█"); got != 5 {
		t.Errorf("50%% of width 10 should fill 5 cells, got %d (%q)", got, bar)
	}
	if got := strings.Count(bar, "░"); got != 5 {
		t.Errorf("50%% of width 10 should leave 5 track cells, got %d (%q)", got, bar)
	}
	if got := strings.Count(plain(progressBar(150, 8, th.Primary, th.Muted)), "█"); got != 8 {
		t.Errorf("an over-100 pct should clamp to a full bar, got %d filled", got)
	}
	if got := strings.Count(plain(progressBar(-5, 8, th.Primary, th.Muted)), "█"); got != 0 {
		t.Errorf("a negative pct should clamp to empty, got %d filled", got)
	}
}

// A loaded detail renders the progress as a bar plus the percentage.
func TestDetailProgressBar(t *testing.T) {
	m := openDetailOn(t, 160, 40, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T", Progress: 40,
	}})
	body := plain(m.View())
	if !strings.Contains(body, "40%") {
		t.Errorf("detail should show the progress number:\n%s", body)
	}
	if !strings.Contains(body, "░") {
		t.Errorf("detail should show a progress bar (track cells):\n%s", body)
	}
}

// The Details Type row paints the type name on a background colour chip.
func TestDetailTypeChip(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	chip := m.typeValue("Bug", "INDIGO")
	if !strings.Contains(chip, "48;2;") {
		t.Errorf("the Details Type value should be a background chip, got %q", chip)
	}
	if pc := plain(chip); !strings.Contains(pc, "Bug") || strings.Contains(pc, "██") {
		t.Errorf("the chip should show the name with no side swatch, got %q", pc)
	}
}

// The progress bar is capped short (<= 12 cells) so it does not dominate the value column.
func TestProgressBarCapped(t *testing.T) {
	m := loaded(t, 160, 40, domain.IssuePage{})
	val := plain(m.progressValue(50, 100)) // a wide value column
	cells := strings.Count(val, "█") + strings.Count(val, "░")
	if cells > 12 {
		t.Errorf("the progress bar should cap at 12 cells, got %d (%q)", cells, val)
	}
}

// The widened Priority column and the background Type chips must not overflow the list at any width,
// from the narrow full-width floor through the tight side-by-side boundary.
func TestListRowsFitWidthWithPriorityColumn(t *testing.T) {
	rows := make([]domain.IssueSummary, 5)
	for i := range rows {
		rows[i] = domain.IssueSummary{
			Key: "TIS-" + string(rune('1'+i)), TypeName: "Subtask", TypeColor: "INDIGO",
			Title: "A reasonably long issue title", StateLabel: "In Progress", StateCategory: "ACTIVE", Priority: "P1",
		}
	}
	for _, w := range []int{80, 100, 128, 140, 160} {
		m := loaded(t, w, 40, domain.IssuePage{Issues: rows, TotalElements: 5})
		for _, line := range strings.Split(plain(m.View()), "\n") {
			if lipgloss.Width(line) > m.width {
				t.Errorf("at width %d a line exceeds the terminal (%d):\n%q", w, lipgloss.Width(line), line)
			}
		}
	}
}

// A long, coloured type in the narrow read-only detail modal must stay within the terminal width (the
// chip is truncated ANSI-safely, not overflowed).
func TestNarrowModalTypeChipFits(t *testing.T) {
	m := openDetailOn(t, 100, 30, domain.IssuePage{Issues: issues(1), TotalElements: 1})
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, Title: "T", TypeName: strings.Repeat("LongType", 20), TypeColor: "INDIGO",
	}})
	for _, line := range strings.Split(plain(m.View()), "\n") {
		if lipgloss.Width(line) > m.width {
			t.Errorf("a coloured type chip overflowed the modal: line width %d > %d:\n%q", lipgloss.Width(line), m.width, line)
		}
	}
}
