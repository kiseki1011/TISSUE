package components

import (
	"fmt"
	"time"
)

// HumanizeSince renders a compact "time ago" for t: a single unit on a s/m/h/d/w/mon/yr ladder
// (e.g. "45m", "3d", "11mon"). A zero time renders "-"; a future time clamps to "0s".
func HumanizeSince(t time.Time) string {
	if t.IsZero() {
		return "-"
	}
	return HumanizeDuration(time.Since(t))
}

// HumanizeDuration renders a compact single-unit duration on the same s/m/h/d/w/mon/yr ladder as
// HumanizeSince (e.g. "45m", "3d", "11mon"). A negative duration clamps to "0s".
func HumanizeDuration(d time.Duration) string {
	if d < 0 {
		d = 0
	}
	switch {
	case d < time.Minute:
		return fmt.Sprintf("%ds", int(d.Seconds()))
	case d < time.Hour:
		return fmt.Sprintf("%dm", int(d.Minutes()))
	case d < 24*time.Hour:
		return fmt.Sprintf("%dh", int(d.Hours()))
	case d < 7*24*time.Hour:
		return fmt.Sprintf("%dd", int(d.Hours())/24)
	case d < 30*24*time.Hour:
		return fmt.Sprintf("%dw", int(d.Hours())/(24*7))
	case d < 365*24*time.Hour:
		return fmt.Sprintf("%dmon", int(d.Hours())/(24*30))
	default:
		return fmt.Sprintf("%dyr", int(d.Hours())/(24*365))
	}
}
