package components

import "testing"

func TestStackVertically(t *testing.T) {
	cases := []struct {
		w, h, minSide, minStack int
		want                    bool
	}{
		{120, 24, 90, 20, false}, // wide enough to sit side by side
		{70, 30, 90, 20, true},   // narrow and tall stacks
		{70, 14, 90, 20, false},  // narrow but too short to stack usefully
		{90, 40, 90, 20, false},  // exactly the side threshold is not below it
		{89, 20, 90, 20, true},   // one below the threshold, just tall enough
	}
	for _, c := range cases {
		if got := StackVertically(c.w, c.h, c.minSide, c.minStack); got != c.want {
			t.Errorf("StackVertically(%d,%d,%d,%d) = %v, want %v", c.w, c.h, c.minSide, c.minStack, got, c.want)
		}
	}
}
