package components

import (
	"testing"
	"time"
)

func TestHumanizeSince(t *testing.T) {
	now := time.Now()
	cases := []struct {
		name string
		t    time.Time
		want string
	}{
		{"zero is a dash", time.Time{}, "-"},
		{"future clamps to 0s", now.Add(2 * time.Hour), "0s"},
		{"seconds", now.Add(-30 * time.Second), "30s"},
		{"minutes", now.Add(-45 * time.Minute), "45m"},
		{"hours", now.Add(-3 * time.Hour), "3h"},
		{"days", now.Add(-5 * 24 * time.Hour), "5d"},
		{"weeks", now.Add(-2 * 7 * 24 * time.Hour), "2w"},
		{"months", now.Add(-3 * 30 * 24 * time.Hour), "3mon"},
		{"years", now.Add(-2 * 365 * 24 * time.Hour), "2yr"},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := HumanizeSince(c.t); got != c.want {
				t.Errorf("HumanizeSince(%v) = %q, want %q", c.t, got, c.want)
			}
		})
	}
}
