package schema

import "github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"

// Aliases keep this package's forms and their many call sites unqualified — see widgets/colorpicker.go.

type colorPicker = widgets.ColorPicker

const colorGridCols = widgets.ColorGridCols

func newColorPicker(title, current string, cols int) colorPicker {
	return widgets.NewColorPicker(title, current, cols)
}
