package glyph

import "testing"

func TestParseMode(t *testing.T) {
	cases := map[string]Mode{
		"nerd":    Nerd,
		"NERD":    Nerd,
		"unicode": Unicode,
		"plain":   Unicode,
		" auto ":  Auto,
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

	// Check differs between the two modes.
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
