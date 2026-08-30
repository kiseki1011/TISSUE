package glyph

import "testing"

func TestParseMode(t *testing.T) {
	cases := map[string]Mode{
		"nerd":    Nerd,
		"NERD":    Nerd,
		"unicode": Unicode,
		"plain":   Unicode,
		" auto ":  Auto, //nolint:gocritic // the padding is the point: ParseMode trims before matching
		"":        Auto,
		"garbage": Auto,
	}
	for in, want := range cases {
		if got := ParseMode(in); got != want {
			t.Errorf("ParseMode(%q) = %v, want %v", in, got, want)
		}
	}
}

func TestNewResolvesByMode(t *testing.T) {
	nerd := New(Nerd)
	uni := New(Unicode)

	if nerd.Check == "" || uni.Check == "" {
		t.Fatal("Check must be set in both modes")
	}
	if nerd.Check == uni.Check {
		t.Errorf("Check should differ by mode: nerd=%q unicode=%q", nerd.Check, uni.Check)
	}

	// Connection glyphs are intentionally identical in both modes.
	if nerd.Connected != uni.Connected {
		t.Errorf("Connected should be identical in both modes: nerd=%q unicode=%q", nerd.Connected, uni.Connected)
	}
}

// Fallback mode leaves Priority empty so callers supply their own text through Or.
func TestPriorityGlyphResolvesByMode(t *testing.T) {
	nerd := New(Nerd)
	uni := New(Unicode)

	if nerd.Priority == "" {
		t.Error("Priority must resolve to a nerd glyph in nerd mode")
	}
	if uni.Priority != "" {
		t.Errorf("Priority must be empty in fallback mode so Or uses the override, got %q", uni.Priority)
	}

	if got := nerd.Or(nerd.Priority, "Pri"); got != nerd.Priority {
		t.Errorf("nerd Or should keep the glyph, got %q", got)
	}
	if got := uni.Or(uni.Priority, "Pri"); got != "Pri" {
		t.Errorf("fallback Or should use the \"Pri\" override, got %q", got)
	}
	if got := uni.Or(uni.Priority, ""); got != "" {
		t.Errorf("fallback Or with an empty override should be empty (Details use), got %q", got)
	}
}
