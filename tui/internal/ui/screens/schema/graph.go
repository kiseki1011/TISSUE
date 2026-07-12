package schema

import (
	"fmt"
	"image/color"
	"sort"
	"strings"

	lipgloss "charm.land/lipgloss/v2"
	"github.com/charmbracelet/x/ansi"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

// A top-to-bottom workflow renderer. States stack vertically down the center as color-chip
// boxes, ordered by distance from the initial state (terminals trailing). A transition
// between two states that land adjacent in that order is the spine arrow joining their boxes.
// Every other transition is routed as an arrow through a side gutter: returns (to an earlier
// state) run up the left, jumps (to a later state, e.g. cancelling into an ABORTED state) run
// down the right, each in its own lane so nested arrows never cross. The whole diagram is
// drawn on a display-cell canvas, so CJK/Hangul labels never skew it and line crossings merge
// into the right box-drawing junction.

// ---- box-drawing canvas ----

// contRune marks the trailing display cell of a wide (2-cell) rune so the grid stays one
// display cell per column even with CJK labels. It emits nothing.
const contRune = '\x00'

// direction bits for box-drawing line merging.
const (
	bU = 1 << iota
	bD
	bL
	bR
)

var boxRune = map[int]rune{
	bU | bD: '│', bL | bR: '─',
	bU | bR: '└', bU | bL: '┘', bD | bR: '┌', bD | bL: '┐',
	bU | bD | bR: '├', bU | bD | bL: '┤', bL | bR | bD: '┬', bL | bR | bU: '┴',
	bU | bD | bL | bR: '┼',
	bU:                '╵', bD: '╷', bL: '╴', bR: '╶',
}

// boxBits is the reverse of boxRune, so an existing glyph merges with new segments and a
// crossing resolves to the right junction.
var boxBits = func() map[rune]int {
	m := map[rune]int{}
	for bits, r := range boxRune {
		m[r] = bits
	}
	return m
}()

type canvas struct {
	ch   [][]rune
	fg   [][]color.Color
	bg   [][]color.Color
	bold [][]bool
	w, h int
	// rec, when non-nil, collects the (row,col) of every connector/arrowhead cell drawn, so the
	// selected transition's whole path can be recolored after the graph is assembled.
	rec *[][2]int
}

func newCanvas(w, h int) *canvas {
	if w < 1 {
		w = 1
	}
	if h < 1 {
		h = 1
	}
	c := &canvas{w: w, h: h, ch: make([][]rune, h), fg: make([][]color.Color, h), bg: make([][]color.Color, h), bold: make([][]bool, h)}
	for r := 0; r < h; r++ {
		c.ch[r] = make([]rune, w)
		c.fg[r] = make([]color.Color, w)
		c.bg[r] = make([]color.Color, w)
		c.bold[r] = make([]bool, w)
		for x := 0; x < w; x++ {
			c.ch[r][x] = ' '
		}
	}
	return c
}

func (c *canvas) in(r, x int) bool { return r >= 0 && r < c.h && x >= 0 && x < c.w }

func (c *canvas) setCell(r, x int, ch rune, fg, bg color.Color, bold bool) {
	if !c.in(r, x) {
		return
	}
	c.ch[r][x], c.fg[r][x], c.bg[r][x], c.bold[r][x] = ch, fg, bg, bold
}

// text writes a foreground-only run in display-cell space (a wide rune takes two columns).
func (c *canvas) text(r, x int, s string, fg color.Color, bold bool) {
	for _, ch := range s {
		w := lipgloss.Width(string(ch))
		if w < 1 {
			w = 1
		}
		c.setCell(r, x, ch, fg, nil, bold)
		for k := 1; k < w; k++ {
			c.setCell(r, x+k, contRune, fg, nil, bold)
		}
		x += w
	}
}

// chip writes a background-painted, bold run — a color chip whose background continues under
// the trailing cell of a wide rune.
func (c *canvas) chip(r, x int, s string, fg, bg color.Color) {
	for _, ch := range s {
		w := lipgloss.Width(string(ch))
		if w < 1 {
			w = 1
		}
		c.setCell(r, x, ch, fg, bg, true)
		for k := 1; k < w; k++ {
			c.setCell(r, x+k, contRune, fg, bg, true)
		}
		x += w
	}
}

// line merges box-drawing bits into a cell so crossings and junctions resolve correctly.
func (c *canvas) line(r, x, bits int) {
	if !c.in(r, x) {
		return
	}
	if eb, ok := boxBits[c.ch[r][x]]; ok {
		bits |= eb
	}
	if g, ok := boxRune[bits]; ok {
		c.setCell(r, x, g, nil, nil, false)
		c.record(r, x)
	}
}

// record notes a cell for the active transition path, when recording is on.
func (c *canvas) record(r, x int) {
	if c.rec != nil {
		*c.rec = append(*c.rec, [2]int{r, x})
	}
}

func (c *canvas) hrun(r, x0, x1 int) {
	if x0 > x1 {
		x0, x1 = x1, x0
	}
	for x := x0; x <= x1; x++ {
		c.line(r, x, bL|bR)
	}
}

func (c *canvas) vrun(x, r0, r1 int) {
	if r0 > r1 {
		r0, r1 = r1, r0
	}
	for r := r0; r <= r1; r++ {
		c.line(r, x, bU|bD)
	}
}

// arrow places a single arrowhead glyph without merging, so it stays a solid mark.
func (c *canvas) arrow(r, x int, ch rune) {
	c.setCell(r, x, ch, nil, nil, false)
	c.record(r, x)
}

// setFG recolors a cell's foreground, leaving its rune and background untouched.
func (c *canvas) setFG(r, x int, fg color.Color) {
	if c.in(r, x) {
		c.fg[r][x] = fg
	}
}

// lines emits the canvas as styled strings, grouping consecutive cells by (fg, bg, bold) so
// chips keep their colors and structure stays muted. Continuation cells are dropped from the
// run because the wide rune before each already spans both display cells.
func (c *canvas) lines(s theme.Styles) []string {
	structural := lipgloss.NewStyle().Foreground(s.Theme.Border)
	out := make([]string, c.h)
	for r := 0; r < c.h; r++ {
		var b strings.Builder
		i := 0
		for i < c.w {
			fg, bg, bold := c.fg[r][i], c.bg[r][i], c.bold[r][i]
			j := i
			for j < c.w && sameColor(c.fg[r][j], fg) && sameColor(c.bg[r][j], bg) && c.bold[r][j] == bold {
				j++
			}
			run := strings.ReplaceAll(string(c.ch[r][i:j]), string(contRune), "")
			if fg == nil && bg == nil {
				b.WriteString(structural.Render(run))
			} else {
				st := lipgloss.NewStyle()
				if fg != nil {
					st = st.Foreground(fg)
				}
				if bg != nil {
					st = st.Background(bg)
				}
				b.WriteString(st.Bold(bold).Render(run))
			}
			i = j
		}
		out[r] = b.String()
	}
	return out
}

func sameColor(a, b color.Color) bool {
	if a == nil || b == nil {
		return a == nil && b == nil
	}
	return a == b
}

// ---- ordering ----

func categoryOrder(cat string) int {
	switch cat {
	case "INITIAL":
		return 0
	case "ACTIVE":
		return 1
	case "COMPLETED":
		return 2
	case "ABORTED":
		return 3
	}
	return 4
}

func isTerminal(cat string) bool { return cat == "COMPLETED" || cat == "ABORTED" }

// orderStates returns the states top-to-bottom: non-terminal states by distance from the
// initial state, then terminal states (COMPLETED before ABORTED), so the happy path stays a
// contiguous line and cancel/done sink to the bottom.
func orderStates(d domain.WorkflowDetail) []domain.WorkflowState {
	adj := map[int][]int{}
	for _, tr := range d.Transitions {
		adj[tr.SourceID] = append(adj[tr.SourceID], tr.TargetID)
	}
	const unreached = 1 << 20
	dist := map[int]int{}
	queue := []int{d.InitialStateID}
	dist[d.InitialStateID] = 0
	for len(queue) > 0 {
		n := queue[0]
		queue = queue[1:]
		for _, t := range adj[n] {
			if _, seen := dist[t]; !seen {
				dist[t] = dist[n] + 1
				queue = append(queue, t)
			}
		}
	}
	rank := func(id int) int {
		if d, ok := dist[id]; ok {
			return d
		}
		return unreached
	}
	states := append([]domain.WorkflowState(nil), d.States...)
	sort.SliceStable(states, func(i, j int) bool {
		ti, tj := isTerminal(states[i].Category), isTerminal(states[j].Category)
		if ti != tj {
			return tj // non-terminal first
		}
		ri, rj := rank(states[i].ID), rank(states[j].ID)
		ci, cj := categoryOrder(states[i].Category), categoryOrder(states[j].Category)
		if ti { // both terminal: COMPLETED before ABORTED, then by rank
			if ci != cj {
				return ci < cj
			}
			if ri != rj {
				return ri < rj
			}
		} else { // both non-terminal: by rank, then category
			if ri != rj {
				return ri < rj
			}
			if ci != cj {
				return ci < cj
			}
		}
		return states[i].ID < states[j].ID
	})
	return states
}

// guardLabel shortens a guard for the diagram.
func guardLabel(g domain.WorkflowGuard) string {
	switch g.Type {
	case "APPROVAL_REQUIRED":
		if n, ok := g.Params["min_approvals"]; ok {
			return fmt.Sprintf("approval×%v", n)
		}
		return "approval"
	case "ASSIGNEE_REQUIRED":
		return "assignee"
	case "BLOCKING_ISSUE_RESOLVE_REQUIRED":
		return "not-blocked"
	case "CHILD_ISSUES_RESOLVE_REQUIRED":
		return "children"
	case "LINKED_BRANCH_REQUIRED":
		return "branch"
	}
	return strings.ToLower(g.Type)
}

// guardsInline is the comma-joined guard labels, shown after a transition as "· a, b".
func guardsInline(guards []domain.WorkflowGuard) string {
	if len(guards) == 0 {
		return ""
	}
	parts := make([]string, len(guards))
	for i, g := range guards {
		parts[i] = guardLabel(g)
	}
	return strings.Join(parts, ", ")
}

// clip truncates s to at most w display cells, ANSI- and wide-rune-aware.
func clip(s string, w int) string {
	if w < 1 {
		w = 1
	}
	return ansi.Truncate(s, w, "…")
}

// assignLanes gives each [lo,hi] row span the innermost free lane (0 = closest to the boxes),
// processing the narrowest spans first so nested arrows stack outward without crossing.
func assignLanes(spans [][2]int) []int {
	lanes := make([]int, len(spans))
	order := make([]int, len(spans))
	for i := range order {
		order[i] = i
	}
	sort.SliceStable(order, func(a, b int) bool {
		return spans[order[a]][1]-spans[order[a]][0] < spans[order[b]][1]-spans[order[b]][0]
	})
	var occupied [][][2]int
	for _, idx := range order {
		lo, hi := spans[idx][0], spans[idx][1]
		placed := -1
		for l := range occupied {
			free := true
			for _, sp := range occupied[l] {
				if lo <= sp[1] && sp[0] <= hi {
					free = false
					break
				}
			}
			if free {
				placed = l
				break
			}
		}
		if placed < 0 {
			placed = len(occupied)
			occupied = append(occupied, nil)
		}
		occupied[placed] = append(occupied[placed], [2]int{lo, hi})
		lanes[idx] = placed
	}
	return lanes
}

// ---- layout ----

const (
	gvGap      = 4  // rows between stacked boxes (the spine arrow's length)
	gvGutter   = 10 // columns reserved on each side for routed arrows
	gvStub     = 4  // horizontal cells a routed arrow protrudes before turning onto its lane
	gvLaneStep = 4  // columns between adjacent routing lanes
)

// addGuardText labels the clickable affordance beneath a spine transition's guards.
const addGuardText = "+ Guard"

type gedge struct {
	tr       domain.WorkflowTransition
	src, tgt int // order positions
}

type glabel struct {
	r, x int
	s    string
	fg   color.Color
	bold bool
}

// grect is an element's clickable/hoverable rectangle in graph-local cell coordinates
// (inclusive bounds), used to hit-test the mouse and to paint the hover highlight.
type grect struct{ r0, r1, c0, c1 int }

// labelRectL is the cell span a left-anchored label occupies once clipped to w.
func labelRectL(r, x int, text string, w int) grect {
	tw := max(1, lipgloss.Width(clip(text, w)))
	return grect{r, r, x, x + tw - 1}
}

// mixColors linearly blends a toward b by ratio in [0,1].
func mixColors(a, b color.Color, ratio float64) color.Color {
	ar, ag, ab, _ := a.RGBA()
	br, bg, bb, _ := b.RGBA()
	blend := func(x, y uint32) uint8 {
		return uint8(float64(x>>8)*(1-ratio) + float64(y>>8)*ratio)
	}
	return color.RGBA{R: blend(ar, br), G: blend(ag, bg), B: blend(ab, bb), A: 0xff}
}

// hoverBg is the subtle background tint for a hovered element, or ok=false on the ANSI theme,
// which follows the terminal's own colors and has no real surface to tint.
func hoverBg(t theme.Theme) (color.Color, bool) {
	if _, noBg := t.Background.(lipgloss.NoColor); noBg {
		return nil, false
	}
	return mixColors(t.Selection, t.Background, 0.5), true
}

// applyHover gives the hovered element a soft highlight: a background tint behind a box or a
// transition label, keeping any color chip; on the ANSI theme it brightens the foreground
// instead, since there is no real surface to tint.
func applyHover(c *canvas, hits map[wfElem]grect, hov wfElem, t theme.Theme) {
	if hov.kind == elemNone {
		return
	}
	rc, ok := hits[hov]
	if !ok {
		return
	}
	bg, useBg := hoverBg(t)
	for r := rc.r0; r <= rc.r1; r++ {
		for x := rc.c0; x <= rc.c1; x++ {
			if !c.in(r, x) || c.bg[r][x] != nil {
				continue // leave color chips untouched
			}
			if useBg {
				if c.fg[r][x] == nil {
					c.fg[r][x] = t.Border // keep the box outline visible under the tint
				}
				c.bg[r][x] = bg
			} else if ch := c.ch[r][x]; ch != ' ' && ch != contRune {
				c.fg[r][x] = t.Secondary
			}
		}
	}
}

// renderWorkflowGraph draws the workflow as a centered, top-to-bottom diagram no wider than
// width. Every line is exactly width display cells, so it scrolls with the Details panel. sel
// marks the currently selected state or transition (drawn in the accent color); the returned
// map gives each element's top row so the panel can scroll it into view. addGuard draws the
// clickable "+ Guard" affordance under each spine transition — on in the editable Details view,
// off in the read-only preview.
func renderWorkflowGraph(d domain.WorkflowDetail, s theme.Styles, width int, sel, hov wfElem, addGuard bool) ([]string, map[wfElem]int, map[wfElem]grect) {
	if width < 12 {
		width = 12
	}
	rows := map[wfElem]int{}
	hits := map[wfElem]grect{}
	if len(d.States) == 0 {
		return []string{s.Muted.Render("No states.")}, rows, hits
	}
	states := orderStates(d)
	pos := map[int]int{}
	for i, st := range states {
		pos[st.ID] = i
	}

	spine := make([]*domain.WorkflowTransition, len(states))
	var backs, skips, selfs []gedge
	for k := range d.Transitions {
		tr := d.Transitions[k]
		sp, okS := pos[tr.SourceID]
		tp, okT := pos[tr.TargetID]
		if !okS || !okT {
			continue
		}
		switch {
		case sp == tp:
			selfs = append(selfs, gedge{tr, sp, tp})
		case tp == sp+1:
			spine[sp] = &d.Transitions[k]
		case tp > sp+1:
			skips = append(skips, gedge{tr, sp, tp})
		default:
			backs = append(backs, gedge{tr, sp, tp})
		}
	}
	hasSide := len(backs)+len(skips)+len(selfs) > 0

	// box width fits the widest "chip  category", capped so the gutters keep room to route
	innerNeed := 3
	for _, st := range states {
		innerNeed = max(innerNeed, lipgloss.Width(st.Label)+2+minGap+lipgloss.Width(st.Category))
	}
	maxInner := width - 4
	if hasSide {
		maxInner = width - 4 - 2*gvGutter
	}
	if maxInner < 3 {
		maxInner = 3
	}
	boxInner := min(innerNeed, maxInner)
	boxW := boxInner + 4
	boxX := (width - boxW) / 2
	centerCol := boxX + boxW/2

	// the gap after each box holds the spine arrow and its labels; it grows so a guarded
	// transition can stack, below a leading blank row, its name, one row per guard, and
	// (when editable) a "+ Guard" affordance, all clear of the arrowhead
	guardBase := 3
	if addGuard {
		guardBase = 4
	}
	rowTop := make([]int, len(states))
	for i := 1; i < len(states); i++ {
		gap := gvGap
		if spine[i-1] != nil {
			gap = max(gvGap, len(spine[i-1].Guards)+guardBase)
		}
		rowTop[i] = rowTop[i-1] + 3 + gap
	}
	midRow := func(i int) int { return rowTop[i] + 1 }
	totalH := rowTop[len(states)-1] + 3

	backSpans := make([][2]int, len(backs))
	for i, e := range backs {
		backSpans[i] = [2]int{midRow(e.tgt), midRow(e.src)}
	}
	backLane := assignLanes(backSpans)
	skipSpans := make([][2]int, len(skips))
	for i, e := range skips {
		skipSpans[i] = [2]int{midRow(e.src), midRow(e.tgt)}
	}
	skipLane := assignLanes(skipSpans)

	c := newCanvas(width, totalH)
	for i, st := range states {
		drawBox(c, rowTop[i], boxX, boxInner, st, s)
		rows[wfElem{elemState, st.ID}] = rowTop[i]
		hits[wfElem{elemState, st.ID}] = grect{rowTop[i], rowTop[i] + 2, boxX, boxX + boxW - 1}
	}

	var labels []glabel
	edgeText := func(tr domain.WorkflowTransition) string {
		t := tr.Label
		if g := guardsInline(tr.Guards); g != "" {
			t += " · " + g
		}
		return t
	}
	// edgeFG colors a transition's label: accent+bold when it is the selected element.
	edgeFG := func(id int) (color.Color, bool) {
		if sel.kind == elemTransition && sel.id == id {
			return s.Theme.Accent, true
		}
		return s.Theme.Text, false
	}
	placeL := func(r, x int, text string, w int, fg color.Color, bold bool) {
		if w >= 1 && text != "" {
			labels = append(labels, glabel{r: r, x: x, s: clip(text, w), fg: fg, bold: bold})
		}
	}
	// placeWrapped renders a routed transition's name as horizontal text centered on its lane at
	// the arrow's vertical middle, wrapping at spaces so a multi-word name stacks into short lines
	// that read left-to-right and fit the gutter (each line kept narrow enough not to spill into
	// the neighboring lane). It returns the block's extent for hit-testing, or ok=false when there
	// is no room. lo..hi bound the arrow's own vertical run.
	placeWrapped := func(mid, laneCol, lo, hi int, name string, fg color.Color, bold bool) (grect, bool) {
		words := strings.Fields(name)
		if len(words) == 0 || hi < lo {
			return grect{}, false
		}
		n := min(len(words), hi-lo+1)
		start := mid - (n-1)/2
		if start+n-1 > hi {
			start = hi - (n - 1)
		}
		if start < lo {
			start = lo
		}
		c0, c1 := laneCol, laneCol
		for i := 0; i < n; i++ {
			w := clip(words[i], 2*gvLaneStep-1)
			ww := lipgloss.Width(w)
			x := laneCol - (ww-1)/2
			labels = append(labels, glabel{r: start + i, x: x, s: w, fg: fg, bold: bold})
			c0, c1 = min(c0, x), max(c1, x+ww-1)
		}
		return grect{start, start + n - 1, c0, c1}, true
	}

	// while drawing the selected transition, its connector/arrowhead cells are collected so its
	// whole path can be recolored to match its accent label once the graph is assembled
	var selCells [][2]int
	markSel := func(id int) {
		if sel.kind == elemTransition && sel.id == id {
			c.rec = &selCells
		} else {
			c.rec = nil
		}
	}

	// spine: a downward arrow whose head touches the next box; the name and, beneath it, one
	// row per guard stack beside the arrow. The label band runs a little past the box into the
	// gutter but stops one cell short of the innermost right lane, so it never hits a jump arrow.
	rightEdge := boxX + boxW + 5
	if len(skips) > 0 {
		rightEdge = boxX + boxW - 1 + gvStub - 1
	}
	spineW := max(1, rightEdge-(centerCol+2)+1)
	for i := 0; i < len(states)-1; i++ {
		tr := spine[i]
		if tr == nil {
			continue
		}
		markSel(tr.ID)
		c.line(rowTop[i]+2, centerCol, bD)
		top, bot := rowTop[i]+3, rowTop[i+1]-1
		c.vrun(centerCol, top, bot-1)
		c.arrow(bot, centerCol, '▼')
		// the name sits one row below the box (a blank gap row above it), then one guard per
		// row, then a "+ Guard" affordance the user can click to add a guard
		nameRow := top + 1
		nameFG, _ := edgeFG(tr.ID)
		placeL(nameRow, centerCol+2, tr.Label, spineW, nameFG, true) // the name is always bold
		rows[wfElem{elemTransition, tr.ID}] = nameRow
		hits[wfElem{elemTransition, tr.ID}] = labelRectL(nameRow, centerCol+2, tr.Label, spineW)
		for gi, g := range tr.Guards {
			placeL(nameRow+1+gi, centerCol+2, guardLabel(g), spineW, s.Theme.Muted, false)
		}
		if addGuard {
			addRow := nameRow + 1 + len(tr.Guards)
			placeL(addRow, centerCol+2, addGuardText, spineW, s.Theme.Secondary, false)
			hits[wfElem{elemAddGuard, tr.ID}] = labelRectL(addRow, centerCol+2, addGuardText, spineW)
		}
	}

	// returns: up the left gutter, head touching the target's left side; the name rides the lane
	for i, e := range backs {
		markSel(e.tr.ID)
		laneCol := max(1, boxX-gvStub-backLane[i]*gvLaneStep)
		sr, tr := midRow(e.src), midRow(e.tgt) // tr < sr
		c.line(sr, boxX, bL)                   // ┤ exit source left
		c.hrun(sr, laneCol+1, boxX-1)
		c.line(sr, laneCol, bU|bR) // └
		c.vrun(laneCol, tr+1, sr-1)
		c.line(tr, laneCol, bD|bR) // ┌
		if laneCol+1 <= boxX-2 {   // only when there is room before the arrowhead
			c.hrun(tr, laneCol+1, boxX-2)
		}
		c.arrow(tr, boxX-1, '▶') // head touches target left
		fg, bold := edgeFG(e.tr.ID)
		rows[wfElem{elemTransition, e.tr.ID}] = (sr + tr) / 2
		if rc, ok := placeWrapped((sr+tr)/2, laneCol, tr+1, sr-1, e.tr.Label, fg, bold); ok {
			hits[wfElem{elemTransition, e.tr.ID}] = rc
			rows[wfElem{elemTransition, e.tr.ID}] = rc.r0
		}
	}

	// jumps: down the right gutter, head touching the target's right side; the name rides the lane
	for i, e := range skips {
		markSel(e.tr.ID)
		laneCol := min(width-2, boxX+boxW-1+gvStub+skipLane[i]*gvLaneStep)
		sr, tr := midRow(e.src), midRow(e.tgt) // sr < tr
		rb := boxX + boxW - 1
		c.line(sr, rb, bR) // ├ exit source right
		c.hrun(sr, rb+1, laneCol-1)
		c.line(sr, laneCol, bD|bL) // ┐
		c.vrun(laneCol, sr+1, tr-1)
		c.line(tr, laneCol, bU|bL) // ┘
		if rb+2 <= laneCol-1 {     // only when there is room before the arrowhead
			c.hrun(tr, rb+2, laneCol-1)
		}
		c.arrow(tr, rb+1, '◀') // head touches target right
		fg, bold := edgeFG(e.tr.ID)
		rows[wfElem{elemTransition, e.tr.ID}] = (sr + tr) / 2
		if rc, ok := placeWrapped((sr+tr)/2, laneCol, sr+1, tr-1, e.tr.Label, fg, bold); ok {
			hits[wfElem{elemTransition, e.tr.ID}] = rc
			rows[wfElem{elemTransition, e.tr.ID}] = rc.r0
		}
	}

	// self-loops: a small hook on the right of the box
	for _, e := range selfs {
		markSel(e.tr.ID)
		r, rb := midRow(e.src), boxX+boxW-1
		c.line(r, rb, bR)
		c.arrow(r, rb+1, '↺')
		fg, bold := edgeFG(e.tr.ID)
		placeL(r, rb+3, edgeText(e.tr), width-(rb+3), fg, bold)
		rows[wfElem{elemTransition, e.tr.ID}] = r
		hits[wfElem{elemTransition, e.tr.ID}] = labelRectL(r, rb+3, edgeText(e.tr), width-(rb+3))
	}

	c.rec = nil
	for _, l := range labels {
		c.text(l.r, l.x, l.s, l.fg, l.bold)
	}
	applyHover(c, hits, hov, s.Theme)
	// paint the selected transition's whole arrow path in the accent color, so it reads as one
	// highlighted unit with its (already accent) label
	for _, cell := range selCells {
		c.setFG(cell[0], cell[1], s.Theme.Accent)
	}
	// outline the selected state last, so its accent border survives any arrow that merged
	// into it while the boxes were drawn
	if sel.kind == elemState {
		if i, ok := pos[sel.id]; ok {
			recolorBorder(c, rowTop[i], boxX, boxW, s.Theme.Accent)
		}
	}
	return c.lines(s), rows, hits
}

// recolorBorder repaints a box's border cells (its perimeter) in fg without changing the
// runes, so a selected state can be outlined in the accent color.
func recolorBorder(c *canvas, top, x, boxW int, fg color.Color) {
	for xx := x; xx < x+boxW; xx++ {
		c.setFG(top, xx, fg)
		c.setFG(top+2, xx, fg)
	}
	for r := top; r <= top+2; r++ {
		c.setFG(r, x, fg)
		c.setFG(r, x+boxW-1, fg)
	}
}

// drawBox renders one state node onto the canvas: a bordered box with the label as a color
// chip and the category muted and right-aligned inside it.
func drawBox(c *canvas, top, x, boxInner int, st domain.WorkflowState, s theme.Styles) {
	boxW := boxInner + 4
	c.line(top, x, bD|bR)
	c.line(top, x+boxW-1, bD|bL)
	c.hrun(top, x+1, x+boxW-2)
	c.line(top+2, x, bU|bR)
	c.line(top+2, x+boxW-1, bU|bL)
	c.hrun(top+2, x+1, x+boxW-2)
	c.line(top+1, x, bU|bD)
	c.line(top+1, x+boxW-1, bU|bD)

	catW := lipgloss.Width(st.Category)
	label := clip(st.Label, max(1, boxInner-catW-minGap-2))
	if bg, fg, ok := components.ChipColors(st.Color); ok {
		c.chip(top+1, x+2, " "+label+" ", fg, bg)
	} else {
		c.text(top+1, x+2, " "+label+" ", s.Theme.Text, true)
	}
	c.text(top+1, x+2+boxInner-catW, st.Category, s.Theme.Muted, false)
}
