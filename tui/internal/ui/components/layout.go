package components

// StackVertically reports whether a list+detail screen should stack its panes (detail on top, list
// below) rather than place them side by side. It stacks when the width cannot host both columns
// comfortably (below minSideW) yet the terminal is tall enough (minStackH) to give each pane a usable
// vertical slice, so a narrow-and-tall terminal reads top-to-bottom instead of cramming two columns.
func StackVertically(w, h, minSideW, minStackH int) bool {
	return w < minSideW && h >= minStackH
}
