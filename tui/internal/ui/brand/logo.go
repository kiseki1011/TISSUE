package brand

import (
	"image/color"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
)

const Logo = `███      ███     ███
 ███     ███
  ███  ███████  ████   █████  █████ ███  ███   ██████
   ███   ███     ███  ███    ███    ███  ███  ███  ███
  ███    ███     ███  ██████ ██████ ███  ███ ████████
 ███     ███     ███     ███    ███ ███  ███  ███
███       ████ ██████ █████  █████   ██████    ██████`

// RenderVertical paints Logo with a top-to-bottom gradient fading base toward black.
// Lines are padded to the widest so the block stays rectangular when centered.
func RenderVertical(base color.Color) string {
	lines := strings.Split(Logo, "\n")

	width := 0
	for _, ln := range lines {
		if w := lipgloss.Width(ln); w > width {
			width = w
		}
	}

	black := color.RGBA{A: 0xff}
	painted := make([]string, len(lines))
	for i, ln := range lines {
		t := 0.0
		if len(lines) > 1 {
			t = float64(i) / float64(len(lines)-1)
		}
		fg := blend(base, black, 0.55*t)
		painted[i] = lipgloss.NewStyle().Foreground(fg).Width(width).Render(ln)
	}
	return strings.Join(painted, "\n")
}

func blend(a, b color.Color, t float64) color.Color {
	ar, ag, ab, _ := a.RGBA()
	br, bg, bb, _ := b.RGBA()
	blend := func(x, y uint32) uint8 {
		return uint8(float64(x>>8)*(1-t) + float64(y>>8)*t)
	}
	return color.RGBA{R: blend(ar, br), G: blend(ag, bg), B: blend(ab, bb), A: 0xff}
}
