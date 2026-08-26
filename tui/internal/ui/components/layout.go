package components

// StackVertically reports whether a list+detail screen should stack its panes: the width cannot host
// both columns (below minSideW) yet the terminal is tall enough (minStackH) for a usable slice each.
func StackVertically(w, h, minSideW, minStackH int) bool {
	return w < minSideW && h >= minStackH
}
