package components

import (
	"fmt"
	"time"
)

// HumanizeSince renders a compact "time ago" (e.g. "45m", "3d"). Zero renders "-", future clamps to "0s".
func HumanizeSince(t time.Time) string {
	if t.IsZero() {
		return "-"
	}
	return HumanizeDuration(time.Since(t))
}

// HumanizeDuration renders one unit off the s/m/h/d/w/mon/yr ladder. A negative duration clamps to "0s".
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
