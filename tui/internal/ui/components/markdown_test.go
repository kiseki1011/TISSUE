package components

import (
	"strings"
	"testing"

	lipgloss "charm.land/lipgloss/v2"
)

// A rendered block never exceeds the requested width, so it drops into a fixed-width box unwrapped.
func TestMarkdownWidthBound(t *testing.T) {
	md := "# A heading\n\n" + strings.Repeat("some words that keep going and wrap around eventually ", 20) +
		"\n\n- a bullet item that is also quite long and should wrap within the width budget too"
	for _, w := range []int{30, 50, 72} {
		out := Markdown(md, w, true)
		for _, ln := range strings.Split(out, "\n") {
			if x := lipgloss.Width(ln); x > w {
				t.Errorf("width %d: line exceeds budget (%d): %q", w, x, ln)
			}
		}
	}
}

// The memo cache must not vary the result.
func TestMarkdownStable(t *testing.T) {
	md := "## Title\n\nbody **text** with `code`"
	first := Markdown(md, 60, true)
	for i := 0; i < 3; i++ {
		if got := Markdown(md, 60, true); got != first {
			t.Fatalf("render %d differs from the first", i)
		}
	}
	if strings.TrimSpace(first) == "" {
		t.Error("expected non-empty rendered output")
	}
}

// Empty markdown renders empty — the caller shows its own placeholder.
func TestMarkdownEmpty(t *testing.T) {
	if got := Markdown("", 40, true); strings.TrimSpace(got) != "" {
		t.Errorf("empty markdown should render empty, got %q", got)
	}
}

func TestIsDark(t *testing.T) {
	if !IsDark(lipgloss.Color("#1a1b26")) {
		t.Error("a near-black background should read as dark")
	}
	if IsDark(lipgloss.Color("#fafafa")) {
		t.Error("a near-white background should read as light")
	}
	if !IsDark(lipgloss.NoColor{}) {
		t.Error("an unset background should default to dark")
	}
}
