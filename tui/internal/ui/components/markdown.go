package components

import (
	"hash/fnv"
	"image/color"
	"strconv"
	"strings"
	"sync"

	"github.com/charmbracelet/glamour"
)

// glamour wraps to the given column, so a block rendered at width w is never wider than w. Renderers
// and rendered blocks are cached because View re-renders on every keystroke.
var (
	mdMu      sync.Mutex
	mdCache   = map[string]string{}
	mdRenders = map[string]*glamour.TermRenderer{}
)

// mdCacheCap bounds the cache so a long session cannot grow it without limit.
const mdCacheCap = 128

// Markdown renders GitHub-flavored markdown wrapped to width, falling back to the raw text on error.
// The result never carries a leading or trailing blank line.
func Markdown(text string, width int, dark bool) string {
	if width < 1 {
		width = 1
	}
	style := "light"
	if dark {
		style = "dark"
	}
	rk := style + "|" + strconv.Itoa(width)
	ck := rk + "|" + hashText(text)

	mdMu.Lock()
	defer mdMu.Unlock()
	if out, ok := mdCache[ck]; ok {
		return out
	}
	r := mdRenders[rk]
	if r == nil {
		var err error
		r, err = glamour.NewTermRenderer(glamour.WithStandardStyle(style), glamour.WithWordWrap(width))
		if err != nil {
			return text
		}
		mdRenders[rk] = r
	}
	out, err := r.Render(text)
	if err != nil {
		return text
	}
	out = strings.Trim(out, "\n")
	if len(mdCache) >= mdCacheCap {
		mdCache = map[string]string{}
	}
	mdCache[ck] = out
	return out
}

// IsDark treats an unset (NoColor) background as dark, the common terminal default.
func IsDark(bg color.Color) bool {
	r, g, b, a := bg.RGBA()
	if a == 0 {
		return true
	}
	lum := (0.299*float64(r) + 0.587*float64(g) + 0.114*float64(b)) / 65535.0
	return lum < 0.5
}

func hashText(s string) string {
	h := fnv.New64a()
	_, _ = h.Write([]byte(s))
	return strconv.FormatUint(h.Sum64(), 16)
}
