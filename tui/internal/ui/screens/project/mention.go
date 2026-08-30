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

// mentionState is the @-mention autocomplete popup for the focused composer. Recomputed after every edit:
// active only while the caret sits in an "@token" matching at least one member.
type mentionState struct {
	active  bool
	query   string                 // the text typed after '@' (may be empty right after '@')
	matches []domain.ProjectMember // the filtered candidates, best first
	sel     int                    // the highlighted candidate
	hover   int                    // the mouse-hovered row, -1 when none
}

// commentMentionZoneID namespaces a row's zone id under the modal so it cannot collide.
func commentMentionZoneID(i int) string {
	return "project.comment.modal.mention." + strconv.Itoa(i)
}

// mentionQuery returns the '@' column, the caret's line/column and the query typed after '@'. The '@'
// counts only at a word start (line start or after whitespace), so "email@host" never triggers.
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

// matchMembers ranks by username-prefix, then display-name-prefix, then substring. An empty query offers
// everyone, a member without a username cannot be mentioned, and the result is capped to the dropdown.
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

// isUsernameRune errs wide (includes . _ -) so a token is read whole. Trailing punctuation is stripped later.
func isUsernameRune(r rune) bool {
	return unicode.IsLetter(r) || unicode.IsDigit(r) || r == '_' || r == '.' || r == '-'
}

// collectMentions returns the canonical usernames mentioned in the body, matched case-insensitively
// against known members so only sendable handles are sent. De-duplicated, capped to the server limit.
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
		// whole token first (a username may end in . _ -), then a punctuation-trimmed fallback for "@bob."
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

// insertMention replaces the "@token" at the caret with "@username " (space-terminated so the popup
// closes). Setting the tail then inserting the head leaves the caret at the seam, whatever the wrapping.
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
	// if the caret already sits on a space, fold it into the head instead of adding a second one: no doubled
	// space, and the caret lands past the token so the popup does not reopen
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

// refreshMention recomputes the popup after every forwarded input. allowOpen gates OPENING (true only
// after a real edit), so a cursor move or a blink cannot reopen a popup the user dismissed with esc.
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
		// inside a token but not an edit (cursor move or blink tick): stay closed so an esc dismissal sticks
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

// mentionKey handles the popup's keys: up/down (ctrl+p/n) move, enter/tab accept, esc dismisses. Any
// other key is left unhandled so it edits the composer and re-filters.
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
	// allowOpen=false keeps it closed in the mid-line case where the caret lands just before a reused space
	m = m.refreshMention(false)
	return m.followCommentFocus(), nil
}

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

// mentionPopupLines renders the dropdown as borderless zone-marked rows at composer width. The mark wraps
// a fixed-width cell, never routed through width-measuring, which would strip it.
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
		switch i {
		case f.mention.sel:
			marker, style = "▸ ", lipgloss.NewStyle().Foreground(t.Accent).Bold(true)
		case f.mention.hover:
			style = lipgloss.NewStyle().Foreground(t.Text)
		}
		cell := lipgloss.NewStyle().Width(editFieldW).Render(style.Render(components.Trunc(marker+label, editFieldW)))
		rows = append(rows, zone.Mark(commentMentionZoneID(i), cell))
	}
	return rows
}
