package project

import (
	"context"
	"strconv"
	"strings"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/textarea"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/components"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

const commentContentH = 6

// commentPart identifies which part of a focusable comment control is active.
type commentPart int

const (
	partReply      commentPart = iota // a root comment's Reply button
	partText                          // a composer's textarea
	partSubmit                        // a composer's Submit button
	partCancel                        // a composer's Cancel button
	partEdit                          // a comment's Edit affordance
	partDelete                        // a comment's Delete affordance
	partEditText                      // the open edit composer's textarea
	partEditSave                      // the open edit composer's Save button
	partEditCancel                    // the open edit composer's Cancel button
	partMore                          // the thread's "Load more" control
)

// moreCommentID is the pseudo-comment the "Load more" control hangs off, so it can live in the same
// focus ring without colliding with a real comment (or with the root composer's id 0).
const moreCommentID int64 = -2

// commentFocus is the modal's focused control: a comment id (0 = the persistent bottom root composer)
// plus which part of it. A reply button is {commentID, partReply}; an inline reply composer's textarea
// is {commentID, partText}; the root composer's textarea is {0, partText}.
type commentFocus struct {
	id   int64
	part commentPart
}

// noCommentFocus is the sentinel for "no hover" (id -1 is never a real comment).
var noCommentFocus = commentFocus{id: -1}

// commentForm holds the comment modal's live input state. The thread itself is read from the loaded
// detail at render time (so a background refetch updates it in place); this struct owns only the two
// composers, the focus, and the id of the comment whose inline reply composer is open.
type commentForm struct {
	deps        deps.Deps
	root        textarea.Model // the persistent bottom composer (a new root comment)
	reply       textarea.Model // the inline reply composer, valid while replyingTo != 0
	replyingTo  int64          // the comment with an open inline reply composer (0 = none)
	focus       commentFocus
	hover       commentFocus
	editingID   int64          // the comment with an open inline edit composer (0 = none)
	edit        textarea.Model // the inline edit composer, valid while editingID != 0
	rootErr     string
	replyErr    string
	editErr     string
	loadingMore bool         // a "load more" page is in flight
	sending     int64        // the parentID of an in-flight submit (0 = root, >0 = reply), or -1 when idle
	mention     mentionState // the @-mention autocomplete popup for the focused composer
}

func newCommentArea(placeholder string) textarea.Model {
	ta := textarea.New()
	ta.Prompt = ""
	ta.ShowLineNumbers = false
	ta.CharLimit = 0
	ta.SetWidth(editFieldW)
	ta.SetHeight(commentContentH)
	ta.Placeholder = placeholder
	return ta
}

func newCommentForm(d deps.Deps) commentForm {
	return commentForm{
		deps:    d,
		root:    newCommentArea("Write a comment…"),
		focus:   commentFocus{id: 0, part: partText}, // start in the root composer
		hover:   noCommentFocus,
		sending: -1,
		mention: mentionState{hover: -1},
	}
}

// openCommentForm opens the comment modal on the bottom root composer (the 'c' action).
func (m Model) openCommentForm() (Model, tea.Cmd) { return m.openCommentSection(0) }

// openCommentSection opens the comment modal. targetID != 0 (e.g. a Details "Reply" click) pre-opens the
// inline reply composer under that comment and focuses it. It needs only the issue key, so it works even
// while the detail body (and its thread) is still loading - then only the root composer shows.
func (m Model) openCommentSection(targetID int64) (Model, tea.Cmd) {
	if m.viewKey == "" {
		return m, toast.Show(toast.Info, "No issue selected.")
	}
	f := newCommentForm(m.deps)
	m.commenting = true
	m.commentScroll = 0
	if targetID != 0 && m.commentReplyable(targetID) {
		f.reply = newCommentArea("Write a reply…")
		f.replyingTo = targetID
		f.focus = commentFocus{id: targetID, part: partText}
	}
	m.commentUI = f
	return m.focusComment(m.commentUI.focus) // (re)focus the right textarea and scroll it into view
}

// commentReplyable reports whether id is a live (non-deleted) comment in the loaded thread.
func (m Model) commentReplyable(id int64) bool {
	for _, c := range m.details[m.viewKey].Comments {
		if c.ID == id && !c.Deleted {
			return true
		}
	}
	return false
}

// commentFocusRing is the modal's focusable controls in visual order: each live root comment's Reply
// button (and, when open, its inline composer's text/submit/cancel), then the bottom root composer's
// text/submit/cancel. Rebuilt each time so opening/closing an inline composer updates navigation.
func (m Model) commentFocusRing() []commentFocus {
	d := m.details[m.viewKey]
	var ring []commentFocus
	var walk func(cs []domain.IssueComment, depth int)
	walk = func(cs []domain.IssueComment, depth int) {
		for _, c := range cs {
			// a deleted/tombstone comment carries no affordances of its own, but its replies survive it
			if c.ID != 0 && !c.Deleted {
				if depth == 0 {
					ring = append(ring, commentFocus{c.ID, partReply}) // the server caps nesting at 1
				}
				if m.commentEditable(c) {
					ring = append(ring, commentFocus{c.ID, partEdit}, commentFocus{c.ID, partDelete})
				}
				if m.commentUI.editingID == c.ID {
					ring = append(ring, commentFocus{c.ID, partEditText},
						commentFocus{c.ID, partEditSave}, commentFocus{c.ID, partEditCancel})
				}
				if depth == 0 && m.commentUI.replyingTo == c.ID {
					ring = append(ring, commentFocus{c.ID, partText},
						commentFocus{c.ID, partSubmit}, commentFocus{c.ID, partCancel})
				}
			}
			walk(c.Replies, depth+1)
		}
	}
	walk(d.Comments, 0)
	if d.CommentsHasMore {
		ring = append(ring, commentFocus{moreCommentID, partMore})
	}
	return append(ring, commentFocus{0, partText}, commentFocus{0, partSubmit}, commentFocus{0, partCancel})
}

func indexOfCommentFocus(ring []commentFocus, f commentFocus) int {
	for i, r := range ring {
		if r == f {
			return i
		}
	}
	return -1
}

func (m Model) moveCommentFocus(delta int) (Model, tea.Cmd) {
	ring := m.commentFocusRing()
	idx := indexOfCommentFocus(ring, m.commentUI.focus)
	if idx < 0 {
		idx = 0
	}
	return m.focusComment(ring[(idx+delta+len(ring))%len(ring)])
}

// focusComment moves focus to target, giving keyboard input to the matching textarea (and blurring the
// others), then scrolls the modal so the focused control is visible.
func (m Model) focusComment(target commentFocus) (Model, tea.Cmd) {
	m.commentUI.focus = target
	m.commentUI.mention = mentionState{hover: -1} // moving focus dismisses any open mention popup
	m.commentUI.root.Blur()
	m.commentUI.reply.Blur()
	m.commentUI.edit.Blur()
	var cmd tea.Cmd
	switch {
	case target.part == partEditText:
		cmd = m.commentUI.edit.Focus()
	case target.part == partText && target.id == 0:
		cmd = m.commentUI.root.Focus()
	case target.part == partText:
		cmd = m.commentUI.reply.Focus()
	}
	return m.followCommentFocus(), cmd
}

// openInlineReply opens (or re-focuses) the inline reply composer under comment id.
func (m Model) openInlineReply(id int64) (Model, tea.Cmd) {
	if m.commentUI.replyingTo != id {
		m.commentUI.reply = newCommentArea("Write a reply…")
		m.commentUI.replyingTo = id
		m.commentUI.replyErr = ""
	}
	return m.focusComment(commentFocus{id, partText})
}

// closeInlineReply discards the inline reply composer and returns focus to its Reply button.
func (m Model) closeInlineReply() (Model, tea.Cmd) {
	id := m.commentUI.replyingTo
	m.commentUI.replyingTo = 0
	m.commentUI.replyErr = ""
	return m.focusComment(commentFocus{id, partReply})
}

// submitComposer validates and submits the root (id 0) or inline reply (id != 0) composer.
func (m Model) submitComposer(id int64) (Model, tea.Cmd) {
	if m.commentUI.sending != -1 {
		return m, nil // a submit is already in flight; ignore a double-submit
	}
	if id == 0 {
		content := strings.TrimSpace(m.commentUI.root.Value())
		if content == "" {
			m.commentUI.rootErr = "Write something first"
			return m.focusComment(commentFocus{0, partText})
		}
		m.commentUI.sending = 0 // mark in flight synchronously so a rapid second Enter cannot double-post
		return m, submitComment(0, content)
	}
	content := strings.TrimSpace(m.commentUI.reply.Value())
	if content == "" {
		m.commentUI.replyErr = "Write something first"
		return m.focusComment(commentFocus{id, partText})
	}
	m.commentUI.sending = id
	return m, submitComment(id, content)
}

// updateComment drives the open modal: submit/cancel close it, a wheel scrolls a modal too tall for the
// terminal, mouse and keys route to the focused control, and anything else is forwarded to that composer.
func (m Model) updateComment(msg tea.Msg) (Model, tea.Cmd) {
	// the delete confirmation floats on top and owns input while it is up
	if m.commentDeleting {
		return m.updateCommentDelete(msg)
	}
	switch msg := msg.(type) {
	case CommentEditDoneMsg:
		return m.onCommentEditDone(msg)
	case commentPageLoadedMsg:
		return m.onCommentPageLoaded(msg)
	case commentCancelledMsg:
		m.commenting = false
		return m, nil
	case commentSubmittedMsg:
		// stay open: keep the modal up and mark which composer is in flight, so CommentDoneMsg clears the
		// right one and the refetched thread shows the new comment in place (see onCommentDoneWhileOpen).
		m.commentUI.sending = msg.parentID
		// resolve @mentions from the submitted body against the known members; only real handles are sent
		return m, createComment(m.deps, m.viewKey, msg.parentID, msg.content, collectMentions(msg.content, m.members))
	case tea.MouseWheelMsg:
		return m.scrollComment(msg)
	case tea.MouseClickMsg:
		return m.commentClick(msg)
	case tea.MouseMotionMsg:
		if i, ok := m.mentionHitRow(msg); ok {
			m.commentUI.mention.hover = i
			m.commentUI.hover = noCommentFocus
			return m, nil
		}
		m.commentUI.mention.hover = -1
		m.commentUI.hover = m.commentHitZone(msg)
		return m, nil
	case tea.KeyPressMsg:
		return m.commentKey(msg)
	}
	return m.forwardToComposer(msg)
}

func (m Model) scrollComment(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	view, _, _ := m.commentModalView()
	if lipgloss.Height(view) <= m.height {
		return m, nil
	}
	switch msg.Button {
	case tea.MouseWheelUp:
		m.commentScroll = clampScroll(m.commentScroll-1, m.commentScrollMax())
	case tea.MouseWheelDown:
		m.commentScroll = clampScroll(m.commentScroll+1, m.commentScrollMax())
	}
	return m, nil
}

func (m Model) commentKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	// the mention popup owns navigation/accept/dismiss keys while open; everything else (typing,
	// backspace, cursor moves) falls through to edit the composer and re-filter the popup.
	if m.commentUI.mention.active {
		if mm, cmd, handled := m.mentionKey(msg); handled {
			return mm, cmd
		}
	}
	f := m.commentUI
	switch msg.String() {
	case "esc":
		// esc closes an open inline composer (back to the affordance it came from); otherwise the modal
		if f.editingID != 0 && f.focus.id == f.editingID && isEditPart(f.focus.part) {
			return m.closeInlineEdit()
		}
		if f.replyingTo != 0 && f.focus.id == f.replyingTo && !isEditPart(f.focus.part) {
			return m.closeInlineReply()
		}
		return m, cancelComment
	case "tab":
		return m.moveCommentFocus(1)
	case "shift+tab":
		return m.moveCommentFocus(-1)
	case "up":
		if f.focus.part != partText {
			return m.moveCommentFocus(-1)
		}
	case "down":
		if f.focus.part != partText {
			return m.moveCommentFocus(1)
		}
	case "enter":
		switch f.focus.part {
		case partReply:
			return m.openInlineReply(f.focus.id)
		case partSubmit:
			return m.submitComposer(f.focus.id)
		case partCancel:
			if f.focus.id == 0 {
				return m, cancelComment
			}
			return m.closeInlineReply()
		case partEdit:
			return m.openInlineEdit(f.focus.id)
		case partDelete:
			return m.openCommentDelete(f.focus.id)
		case partEditSave:
			return m.submitInlineEdit()
		case partEditCancel:
			return m.closeInlineEdit()
		case partMore:
			return m.loadMoreComments()
		case partText, partEditText:
			return m.forwardToComposer(msg) // a newline in the body
		}
	}
	return m.forwardToComposer(msg)
}

// forwardToComposer routes input to the focused textarea (a no-op when a button/Reply is focused), then
// recomputes the @-mention popup from the new text/cursor so it opens, filters, and closes as you type.
// Only a real text edit may open the popup (a cursor move or a blink tick can just close a stale one);
// after refreshing, the modal re-follows the focused block so a popup that grew it scrolls into view.
func (m Model) forwardToComposer(msg tea.Msg) (Model, tea.Cmd) {
	if m.commentUI.focus.part == partEditText {
		before := m.commentUI.edit.Value()
		m.commentUI.editErr = ""
		var cmd tea.Cmd
		m.commentUI.edit, cmd = m.commentUI.edit.Update(msg)
		return m.refreshMention(m.commentUI.edit.Value() != before).followCommentFocus(), cmd
	}
	if m.commentUI.focus.part != partText {
		return m, nil
	}
	var cmd tea.Cmd
	var edited bool
	if m.commentUI.focus.id == 0 {
		before := m.commentUI.root.Value()
		m.commentUI.rootErr = ""
		m.commentUI.root, cmd = m.commentUI.root.Update(msg)
		edited = m.commentUI.root.Value() != before
	} else {
		before := m.commentUI.reply.Value()
		m.commentUI.replyErr = ""
		m.commentUI.reply, cmd = m.commentUI.reply.Update(msg)
		edited = m.commentUI.reply.Value() != before
	}
	return m.refreshMention(edited).followCommentFocus(), cmd
}

func (m Model) commentClick(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	if i, ok := m.mentionHitRow(msg); ok {
		m.commentUI.mention.sel = i
		return m.mentionAccept()
	}
	tok := m.commentHitZone(msg)
	if tok == noCommentFocus {
		return m, nil
	}
	switch tok.part {
	case partReply:
		return m.openInlineReply(tok.id)
	case partSubmit:
		return m.submitComposer(tok.id)
	case partCancel:
		if tok.id == 0 {
			return m, cancelComment
		}
		return m.closeInlineReply()
	case partEdit:
		return m.openInlineEdit(tok.id)
	case partDelete:
		return m.openCommentDelete(tok.id)
	case partEditSave:
		return m.submitInlineEdit()
	case partEditCancel:
		return m.closeInlineEdit()
	case partMore:
		return m.loadMoreComments()
	default: // partText / partEditText
		return m.focusComment(tok)
	}
}

func (m Model) commentHitZone(msg tea.MouseMsg) commentFocus {
	for _, tok := range m.commentFocusRing() {
		if zone.Get(commentZoneID(tok)).InBounds(msg) {
			return tok
		}
	}
	return noCommentFocus
}

// commentZoneID is a modal control's bubblezone id. The "modal" segment keeps these distinct from the
// Details panel's own per-comment reply zones, which survive in the dimmed backdrop under the modal.
func commentZoneID(tok commentFocus) string {
	if tok.part == partReply {
		return "project.comment.modal.reply." + strconv.FormatInt(tok.id, 10)
	}
	return "project.comment.modal." + strconv.FormatInt(tok.id, 10) + "." + strconv.Itoa(int(tok.part))
}

// commentHelpKeys are the footer hints for the modal, tailored to the focused control.
func (m Model) commentHelpKeys() []key.Binding {
	if m.commentUI.mention.active {
		// the popup owns these keys while open, so advertise its bindings instead of the composer's
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "select")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "mention")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "dismiss")),
		}
	}
	binds := []key.Binding{key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "move"))}
	switch m.commentUI.focus.part {
	case partReply:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "reply")))
	case partText:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "newline")))
	case partSubmit:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "submit")))
	case partCancel:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "close")))
	case partEdit:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "edit")))
	case partDelete:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "delete")))
	case partEditText:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "newline")))
	case partEditSave:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")))
	case partEditCancel:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "discard")))
	case partMore:
		binds = append(binds, key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "load more")))
	}
	return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "close")))
}

// commentModalView boxes the modal body into the centered "Comments" modal and reports the focused
// control's row and height (in box coordinates) so the window can scroll it into view.
func (m Model) commentModalView() (string, int, int) {
	body, row, h := m.commentModalBody()
	boxed := components.TitledBoxCentered("Comments",
		lipgloss.NewStyle().Padding(1, 1).Render(body), m.deps.Styles.Theme.Primary)
	return boxed, row, h
}

// commentModalBody builds the modal body - the comment thread (each root comment with a Reply button and,
// when open, an inline reply composer) followed by the persistent root composer - and, in the same pass,
// records the focused control's row/height so render and scroll never drift apart.
func (m Model) commentModalBody() (string, int, int) {
	f := m.commentUI
	d := m.details[m.viewKey]
	s := m.deps.Styles

	var lines []string
	focusRow, focusH := 0, 0
	const base = 2 // top border + the padding row above the body (box coordinates)
	emit := func(block []string, focused bool) {
		if focused {
			focusRow, focusH = base+len(lines), len(block)
		}
		lines = append(lines, block...)
	}
	// the comment markdown is untrusted and glamour does not clamp long/unbreakable tokens to the wrap
	// width, so fold each thread block to the modal width or the floated frame could overflow.
	fold := func(s string) []string {
		return strings.Split(lipgloss.NewStyle().Width(editFieldW).Render(s), "\n")
	}
	// emitComposer draws one composer (textarea + Save/Cancel). The parts are passed in so the same block
	// serves the reply/root composers and the inline edit composer without their zones colliding.
	emitComposer := func(id int64, ta textarea.Model, errMsg, label, saveLabel string, textPart, savePart, cancelPart commentPart) {
		textFocused := f.focus == commentFocus{id, textPart}
		border := s.Theme.Primary
		if textFocused {
			border = s.Theme.Accent
		}
		// clamp the (untrusted) label so a long reply-to author cannot widen the box title past the frame
		box := components.TitledBoxWeighted(components.Trunc(label, editFieldW), fixField(ta.View(), commentContentH), border, textFocused)
		box = zone.Mark(commentZoneID(commentFocus{id, textPart}), box)
		boxLines := strings.Split(box, "\n") // the composer box is a fixed width; never fold (it would clip the border)
		if textFocused && f.mention.active {
			// the autocomplete dropdown rides under the textarea, inside the focused block so it scrolls into view
			boxLines = append(boxLines, m.mentionPopupLines()...)
		}
		emit(boxLines, textFocused)
		if errMsg != "" {
			emit([]string{lipgloss.NewStyle().Padding(0, 1).Render(s.Error.Render(errMsg))}, false)
		}
		group := lipgloss.JoinHorizontal(lipgloss.Top,
			m.composerButton(saveLabel, id, savePart), " ", m.composerButton("Cancel", id, cancelPart))
		buttons := lipgloss.PlaceHorizontal(editFieldW+4, lipgloss.Right, group)
		btnFocused := f.focus == commentFocus{id, savePart} || f.focus == commentFocus{id, cancelPart}
		emit(strings.Split(buttons, "\n"), btnFocused)
	}

	if len(d.Comments) == 0 {
		emit([]string{s.Muted.Render("No comments yet.")}, false)
	}
	for i, c := range d.Comments {
		if i > 0 {
			emit([]string{""}, false)
		}
		block := fold(strings.Join(m.commentRows(c, editFieldW, 0, m.modalCommentActions), "\n"))
		if len(block) > 0 {
			// the Reply button rides the header (first line); focusing it scrolls to that line
			emit(block[:1], f.focus == commentFocus{c.ID, partReply})
			emit(block[1:], false)
		}
		// Render the inline composer only for a live reply target, using the SAME predicate as the focus
		// ring and the reply buttons (c.ID != 0 && !c.Deleted) so render and navigation never disagree.
		// The c.ID != 0 guard is critical: replyingTo's default sentinel is 0, so without it an ID==0
		// comment (server omitted commentId) would match and emit the zero-value reply textarea - whose
		// View() panics - and double-mark the root composer's {0,*} zones.
		if c.ID != 0 && !c.Deleted && f.replyingTo == c.ID {
			emit([]string{""}, false)
			emitComposer(c.ID, f.reply, f.replyErr, "Reply to "+replyToAuthor(d, c.ID), "Submit",
				partText, partSubmit, partCancel)
		}
		// The edit composer follows the whole root block, even when a nested reply is the one being
		// edited: the block is rendered in one pass, so there is no seam to inject it at. Its title names
		// the author so it is never ambiguous which comment is open.
		if f.editingID != 0 && commentTreeHas(c, f.editingID) {
			emit([]string{""}, false)
			emitComposer(f.editingID, f.edit, f.editErr, "Edit "+commentAuthorLabel(d, f.editingID), "Save",
				partEditText, partEditSave, partEditCancel)
		}
	}
	if d.CommentsHasMore {
		emit([]string{""}, false)
		more := m.loadMoreControl(d)
		emit([]string{more}, f.focus == commentFocus{moreCommentID, partMore})
	}
	// the persistent root composer at the bottom
	emit([]string{""}, false)
	emitComposer(0, f.root, f.rootErr, "Add a comment", "Submit", partText, partSubmit, partCancel)

	return strings.Join(lines, "\n"), focusRow, focusH
}

func commentsMoreNote(d domain.IssueDetail) string {
	return "… " + strconv.Itoa(d.CommentCount-len(d.Comments)) + " more (showing " +
		strconv.Itoa(len(d.Comments)) + " of " + strconv.Itoa(d.CommentCount) + ")"
}

// modalCommentActions is the per-comment affordance row inside the modal: Reply on a root comment, and
// Edit/Delete on any comment the caller may change. Each is keyboard-focusable and clickable, accenting
// when focused or hovered, in zones namespaced apart from the Details panel's reply links.
func (m Model) modalCommentActions(c domain.IssueComment, depth int) string {
	if c.Deleted || c.ID == 0 {
		return ""
	}
	g := m.deps.Glyphs
	var out string
	if depth == 0 { // the server caps nesting at 1, so only a root comment can be replied to
		out += "  " + m.commentActionLink(c.ID, partReply, g.Reply+" Reply")
	}
	if m.commentEditable(c) {
		out += "  " + m.commentActionLink(c.ID, partEdit, g.Or(g.Pen+" ", "")+"Edit")
		out += "  " + m.commentActionLink(c.ID, partDelete, g.Or(g.Trash+" ", "")+"Delete")
	}
	return out
}

func (m Model) composerButton(label string, id int64, part commentPart) string {
	t := m.deps.Styles.Theme
	focused := m.commentUI.focus == commentFocus{id, part}
	hovered := m.commentUI.hover == commentFocus{id, part}
	borderCol, textCol, bold := t.Primary, t.Text, false
	switch {
	case focused:
		borderCol, textCol, bold = t.Accent, t.Accent, true
	case hovered:
		borderCol = t.Secondary
	}
	body := lipgloss.NewStyle().Foreground(textCol).Bold(bold).Render(label)
	return zone.Mark(commentZoneID(commentFocus{id, part}), components.TitledBoxWeighted("", body, borderCol, focused))
}

func (m Model) commentScrollMax() int {
	view, _, _ := m.commentModalView()
	return max(0, lipgloss.Height(view)-m.height)
}

// followCommentFocus scrolls the windowed modal so the focused control stays visible, mirroring the edit
// modal. It is a no-op when the modal already fits the terminal.
func (m Model) followCommentFocus() Model {
	view, row, height := m.commentModalView()
	if height == 0 {
		return m
	}
	boxH := lipgloss.Height(view)
	if boxH <= m.height {
		m.commentScroll = 0
		return m
	}
	visible := m.height - 2 // ScrollBox shows interior box-lines [off+1, off+visible]
	off := m.commentScroll
	top, bottom := row, row+max(1, height)-1
	if top < 1+off {
		off = top - 1
	} else if bottom > off+visible {
		off = bottom - visible
	}
	m.commentScroll = min(max(off, 0), boxH-m.height)
	return m
}

// onCommentDoneWhileOpen handles a create result while the modal is still open (stay-open). On success
// it clears the posted composer - the bottom composer for another root comment, or the inline composer
// which closes - and refetches so the new comment appears in the live thread. On failure it keeps the
// typed text so the user can retry.
func (m Model) onCommentDoneWhileOpen(msg CommentDoneMsg) (Model, tea.Cmd) {
	parent := m.commentUI.sending
	m.commentUI.sending = -1
	if msg.err {
		return m, toast.Show(toast.Error, msg.errText)
	}
	var focusCmd tea.Cmd
	label := "Comment added."
	if parent > 0 { // a reply: close its inline composer (the reply now lives under the parent)
		label = "Reply added."
		if m.commentUI.replyingTo == parent { // still on it (the user did not move on during the send)
			m, focusCmd = m.closeInlineReply()
		}
	} else { // a root comment: clear the bottom composer (its content was posted)
		m.commentUI.root.SetValue("")
		m.commentUI.rootErr = ""
		// only refocus the root composer if the user is still there; if they opened an inline reply during
		// the send, leave their focus/scroll alone (mirrors the reply branch's replyingTo guard)
		if m.commentUI.focus.id == 0 {
			m, focusCmd = m.focusComment(commentFocus{0, partText})
		}
	}
	return m, tea.Batch(focusCmd, m.startDetailLoad(msg.key), toast.Show(toast.Success, label))
}

// replyToAuthor is the (flattened) author of the comment being replied to, for the inline composer label.
func replyToAuthor(d domain.IssueDetail, id int64) string {
	for _, c := range d.Comments {
		if c.ID == id {
			if n := flattenLine(c.AuthorName); n != "" {
				return n
			}
			return "comment"
		}
	}
	return "comment"
}

type commentSubmittedMsg struct {
	content  string
	parentID int64 // 0 for a top-level comment, else the parent comment id
}

type commentCancelledMsg struct{}

func cancelComment() tea.Msg { return commentCancelledMsg{} }

func submitComment(parentID int64, content string) tea.Cmd {
	return func() tea.Msg { return commentSubmittedMsg{content: content, parentID: parentID} }
}

// CommentDoneMsg is exported so the app shell can route this background result back to the project
// screen even when the user has left the drill-in before the comment landed (so the toast still shows).
type CommentDoneMsg struct {
	key     string
	err     bool
	errText string // the resolved failure toast line (server reason / mapped code / fallback)
}

func createComment(d deps.Deps, key string, parentID int64, content string, mentions []string) tea.Cmd {
	return func() tea.Msg {
		var err error
		if parentID != 0 {
			err = d.Issues.CreateReply(context.Background(), key, parentID, content, mentions)
		} else {
			err = d.Issues.CreateComment(context.Background(), key, content, mentions)
		}
		return CommentDoneMsg{key: key, err: err != nil, errText: errmsg.Message(err, "Could not add the comment.")}
	}
}
