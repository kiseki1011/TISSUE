package project

import (
	"sort"
	"strconv"
	"strings"
	"unicode"

	"charm.land/bubbles/v2/textarea"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
)

const (
	mentionMax     = 6  // rows offered in the autocomplete dropdown
	mentionSendCap = 50 // the server accepts at most this many mentioned usernames
)

// mentionState is the @-mention autocomplete popup attached to whichever composer holds keyboard focus.
// It is recomputed after every edit: active only while the cursor sits inside an "@token" that matches
// at least one project member.
type mentionState struct {
	active  bool
	query   string                 // the text typed after '@' (may be empty right after '@')
	matches []domain.ProjectMember // the filtered candidates, best first
	sel     int                    // the highlighted candidate
	hover   int                    // the mouse-hovered row, -1 when none
}

// commentMentionZoneID is a candidate row's bubblezone id, namespaced under the modal so it never
// collides with the reply/composer zones.
func commentMentionZoneID(i int) string {
	return "project.comment.modal.mention." + strconv.Itoa(i)
}

// mentionQuery inspects the focused composer and, if the cursor sits inside an "@token", returns the
// token's start column (the '@'), its line and cursor columns, and the query typed after '@'. The '@'
// counts only at the start of a word (line start or after whitespace), so "email@host" never triggers.
func mentionQuery(ta textarea.Model) (start, row, col int, query string, ok bool) {
	row = ta.Line()
	col = ta.Column()
	lines := strings.Split(ta.Value(), "\n")
	if row < 0 || row >= len(lines) {
		return 0, row, col, "", false
	}
	line := []rune(lines[row])
	if col < 0 {
		col = 0
	}
	if col > len(line) {
		col = len(line)
	}
	for i := col - 1; i >= 0; i-- {
		c := line[i]
		if c == '@' {
			if i == 0 || unicode.IsSpace(line[i-1]) {
				return i, row, col, string(line[i+1 : col]), true
			}
			return 0, row, col, "", false // '@' embedded in a word (e.g. an email) is not a mention
		}
		if unicode.IsSpace(c) {
			return 0, row, col, "", false // hit whitespace before any '@'
		}
	}
	return 0, row, col, "", false
}

// matchMembers ranks the project members against the typed query: username-prefix first, then
// display-name-prefix, then any substring. An empty query offers everyone. Members without a username
// are skipped (they cannot be mentioned). The result is capped to the dropdown height.
func matchMembers(query string, members []domain.ProjectMember) []domain.ProjectMember {
	q := strings.ToLower(strings.TrimSpace(query))
	type scored struct {
		m    domain.ProjectMember
		rank int
	}
	var out []scored
	for _, mem := range members {
		if mem.Username == "" {
			continue
		}
		u := strings.ToLower(mem.Username)
		n := strings.ToLower(mem.DisplayName)
		rank := -1
		switch {
		case q == "":
			rank = 3
		case strings.HasPrefix(u, q):
			rank = 0
		case strings.HasPrefix(n, q):
			rank = 1
		case strings.Contains(u, q) || strings.Contains(n, q):
			rank = 2
		}
		if rank >= 0 {
			out = append(out, scored{mem, rank})
		}
	}
	sort.SliceStable(out, func(i, j int) bool {
		if out[i].rank != out[j].rank {
			return out[i].rank < out[j].rank
		}
		return strings.ToLower(out[i].m.Username) < strings.ToLower(out[j].m.Username)
	})
	res := make([]domain.ProjectMember, 0, mentionMax)
	for _, s := range out {
		if len(res) >= mentionMax {
			break
		}
		res = append(res, s.m)
	}
	return res
}

// isUsernameRune is the character class a manually-typed @token may span. It errs wide (includes . _ -)
// so a token is read whole; trailing punctuation is stripped before matching.
func isUsernameRune(r rune) bool {
	return unicode.IsLetter(r) || unicode.IsDigit(r) || r == '_' || r == '.' || r == '-'
}

// collectMentions extracts the usernames actually mentioned in the submitted body, matched against the
// known members (case-insensitive) so only real, sendable handles are transmitted - the popup inserts
// clean space-terminated tokens, and a hand-typed handle is matched too. The canonical username is
// returned, de-duplicated in first-seen order and capped to the server limit.
func collectMentions(content string, members []domain.ProjectMember) []string {
	if len(members) == 0 {
		return nil
	}
	byName := make(map[string]string, len(members))
	for _, mem := range members {
		if mem.Username != "" {
			byName[strings.ToLower(mem.Username)] = mem.Username
		}
	}
	runes := []rune(content)
	seen := map[string]bool{}
	var out []string
	for i := 0; i < len(runes); i++ {
		if runes[i] != '@' {
			continue
		}
		if i > 0 && !unicode.IsSpace(runes[i-1]) {
			continue // '@' must open a word to be a mention
		}
		j := i + 1
		for j < len(runes) && isUsernameRune(runes[j]) {
			j++
		}
		raw := string(runes[i+1 : j])
		i = j - 1 // resume past this token
		if raw == "" {
			continue
		}
		// match the whole token first (a username may legitimately end in . _ -), then fall back to a
		// trailing-punctuation-trimmed form for a hand-typed token like "@bob."
		lc := strings.ToLower(raw)
		canon, ok := byName[lc]
		if !ok {
			lc = strings.ToLower(strings.TrimRight(raw, "._-"))
			canon, ok = byName[lc]
		}
		if ok && !seen[lc] {
			seen[lc] = true
			out = append(out, canon)
			if len(out) >= mentionSendCap {
				break
			}
		}
	}
	return out
}

// insertMention replaces the "@token" at the cursor with "@username " (space-terminated so the token
// reads as complete and the popup closes). It rebuilds the value in two halves and repositions the
// cursor by construction: setting the tail, then inserting the head at the start leaves the cursor
// exactly at the head/tail seam - right after the inserted mention - regardless of soft-wrapping.
func insertMention(ta textarea.Model, username string) textarea.Model {
	start, row, col, _, ok := mentionQuery(ta)
	if !ok {
		return ta
	}
	lines := strings.Split(ta.Value(), "\n")
	line := []rune(lines[row])
	if start < 0 || start > len(line) || col > len(line) {
		return ta
	}
	after := line[col:]
	// terminate the mention with a space so the token reads as complete. If the cursor already sits on a
	// space, fold that space into the head (the caret lands after it) rather than adding a second one: this
	// both avoids a doubled space and moves the caret past the token so the popup does not immediately reopen.
	replacement := "@" + username
	if len(after) > 0 && unicode.IsSpace(after[0]) {
		replacement += string(after[0])
		after = after[1:]
	} else {
		replacement += " "
	}
	headLast := string(line[:start]) + replacement
	tailFirst := string(after)
	head := strings.Join(append(append([]string{}, lines[:row]...), headLast), "\n")
	tail := strings.Join(append([]string{tailFirst}, lines[row+1:]...), "\n")
	ta.SetValue(tail)
	ta.MoveToBegin()
	ta.InsertString(head)
	return ta
}

// refreshMention recomputes the popup from the focused composer's current text and cursor. It is called
// after every forwarded input: the popup opens, filters, and closes as the user types. allowOpen gates
// OPENING - true only after a real text edit - so a cursor-only move or a cursor blink can close a stale
// popup (caret left the token) but can never re-open one the user dismissed with esc. Focus off the
// textarea, no active "@token", or no matches all clear it.
func (m Model) refreshMention(allowOpen bool) Model {
	f := m.commentUI
	if f.focus.part != partText {
		f.mention = mentionState{hover: -1}
		m.commentUI = f
		return m
	}
	ta := f.root
	if f.focus.id != 0 {
		ta = f.reply
	}
	_, _, _, query, ok := mentionQuery(ta)
	if !ok {
		f.mention = mentionState{hover: -1} // the caret left the token: always close
		m.commentUI = f
		return m
	}
	if !f.mention.active && !allowOpen {
		// the caret is inside a token but this was not an edit (a cursor move or a blink tick): keep the
		// popup closed, so an esc-dismissed popup stays dismissed until the user actually edits again.
		f.mention = mentionState{hover: -1}
		m.commentUI = f
		return m
	}
	matches := matchMembers(query, m.members)
	if len(matches) == 0 {
		f.mention = mentionState{hover: -1}
		m.commentUI = f
		return m
	}
	sel := f.mention.sel
	if f.mention.query != query || sel < 0 || sel >= len(matches) {
		sel = 0
	}
	f.mention = mentionState{active: true, query: query, matches: matches, sel: sel, hover: -1}
	m.commentUI = f
	return m
}

// mentionKey handles the popup's own keys while it is open: up/down (and ctrl+p/n) move the selection,
// enter/tab accept, esc dismisses (leaving the modal open). Any other key is left unhandled so it edits
// the composer normally (and re-filters the popup). handled reports whether the key was consumed.
func (m Model) mentionKey(msg tea.KeyPressMsg) (Model, tea.Cmd, bool) {
	n := len(m.commentUI.mention.matches)
	if n == 0 {
		return m, nil, false
	}
	switch msg.String() {
	case "up", "ctrl+p":
		m.commentUI.mention.sel = (m.commentUI.mention.sel - 1 + n) % n
		return m, nil, true
	case "down", "ctrl+n":
		m.commentUI.mention.sel = (m.commentUI.mention.sel + 1) % n
		return m, nil, true
	case "enter", "tab":
		mm, cmd := m.mentionAccept()
		return mm, cmd, true
	case "esc":
		m.commentUI.mention = mentionState{hover: -1}
		return m, nil, true
	}
	return m, nil, false
}

// mentionAccept inserts the selected candidate into the focused composer and closes the popup.
func (m Model) mentionAccept() (Model, tea.Cmd) {
	f := m.commentUI
	if !f.mention.active || len(f.mention.matches) == 0 {
		return m, nil
	}
	username := f.mention.matches[f.mention.sel].Username
	if f.focus.id == 0 {
		f.root = insertMention(f.root, username)
		f.rootErr = ""
	} else {
		f.reply = insertMention(f.reply, username)
		f.replyErr = ""
	}
	f.mention = mentionState{hover: -1}
	m.commentUI = f
	// the caret now sits after "@username ", so refresh finds no active token; pass allowOpen=false so it
	// stays closed even for the mid-line case where the caret lands just before a reused space
	m = m.refreshMention(false)
	return m.followCommentFocus(), nil
}

// mentionHitRow returns the candidate row under the mouse, if any.
func (m Model) mentionHitRow(msg tea.MouseMsg) (int, bool) {
	if !m.commentUI.mention.active {
		return 0, false
	}
	for i := range m.commentUI.mention.matches {
		if zone.Get(commentMentionZoneID(i)).InBounds(msg) {
			return i, true
		}
	}
	return 0, false
}

// mentionPopupLines renders the autocomplete dropdown as borderless, zone-marked rows sized to the
// composer width, so it appends flush under the focused textarea and each row stays clickable (the mark
// wraps a fixed-width cell, never routed through width-measuring that would strip it).
func (m Model) mentionPopupLines() []string {
	f := m.commentUI
	if !f.mention.active || len(f.mention.matches) == 0 {
		return nil
	}
	t := m.deps.Styles.Theme
	g := m.deps.Glyphs
	rows := make([]string, 0, len(f.mention.matches))
	for i, mem := range f.mention.matches {
		label := g.At + mem.Username
		if name := flattenLine(mem.DisplayName); name != "" && name != mem.Username {
			label += "  " + name
		}
		marker, style := "  ", lipgloss.NewStyle().Foreground(t.Muted)
		switch {
		case i == f.mention.sel:
			marker, style = "▸ ", lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		case i == f.mention.hover:
			style = lipgloss.NewStyle().Foreground(t.Text)
		}
		cell := lipgloss.NewStyle().Width(editFieldW).Render(style.Render(components.Trunc(marker+label, editFieldW)))
		rows = append(rows, zone.Mark(commentMentionZoneID(i), cell))
	}
	return rows
}
