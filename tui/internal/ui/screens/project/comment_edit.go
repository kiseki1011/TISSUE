package project

import (
	"context"
	"strings"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// isEditPart reports whether a focus part belongs to the inline edit composer, so esc can tell an open
// edit from an open reply when both hang off the same comment id.
func isEditPart(p commentPart) bool {
	return p == partEditText || p == partEditSave || p == partEditCancel
}

// commentEditable reports whether the caller may rewrite or remove this comment. It mirrors the server's
// rule - the author, or anyone who manages the project - minus the system-admin override, which the
// client cannot see; an admin acting on someone else's comment simply has no button and would have to
// use the API. Guessing wrong only costs a 403 the screen reports.
func (m Model) commentEditable(c domain.IssueComment) bool {
	if c.Deleted || c.ID == 0 || m.peeking {
		return false // a peeked issue is read-only, and a tombstone has nothing left to change
	}
	if m.viewer != "" && c.AuthorUsername == m.viewer {
		return true
	}
	return m.viewerManages()
}

// viewerManages reports whether the caller is a MANAGER of this project, read off the member roster the
// screen already prefetches (the project detail the screen holds does not carry the caller's role).
func (m Model) viewerManages() bool {
	if m.viewer == "" {
		return false
	}
	for _, mem := range m.members {
		if mem.Username == m.viewer {
			return mem.Role == "MANAGER"
		}
	}
	return false
}

// commentTreeHas reports whether id is c or one of its replies, so the edit composer can be emitted under
// the root block that contains the comment being edited.
func commentTreeHas(c domain.IssueComment, id int64) bool {
	if c.ID == id {
		return true
	}
	for _, r := range c.Replies {
		if commentTreeHas(r, id) {
			return true
		}
	}
	return false
}

// findComment returns the comment with this id anywhere in the thread.
func findComment(cs []domain.IssueComment, id int64) (domain.IssueComment, bool) {
	for _, c := range cs {
		if c.ID == id {
			return c, true
		}
		if got, ok := findComment(c.Replies, id); ok {
			return got, true
		}
	}
	return domain.IssueComment{}, false
}

// commentAuthorLabel names a comment's author for a composer title, falling back to a neutral word so the
// title is never blank.
func commentAuthorLabel(d domain.IssueDetail, id int64) string {
	if c, ok := findComment(d.Comments, id); ok && c.AuthorName != "" {
		return c.AuthorName + "'s comment"
	}
	return "comment"
}

// commentActionLink is one affordance in a comment's action row: muted, accenting when focused or hovered.
func (m Model) commentActionLink(id int64, part commentPart, label string) string {
	tok := commentFocus{id, part}
	col := m.deps.Styles.Theme.Muted
	if m.commentUI.focus == tok || m.commentUI.hover == tok {
		col = m.deps.Styles.Theme.Accent
	}
	return zone.Mark(commentZoneID(tok), lipgloss.NewStyle().Foreground(col).Render(label))
}

// loadMoreControl is the focusable "Load more" affordance under a partially-loaded thread, carrying the
// same count the read-only note shows so the user knows how much is left.
func (m Model) loadMoreControl(d domain.IssueDetail) string {
	tok := commentFocus{moreCommentID, partMore}
	t := m.deps.Styles.Theme
	label := "Load more  ·  " + strings.TrimPrefix(commentsMoreNote(d), "… ")
	if m.commentUI.loadingMore {
		label = "Loading…"
	}
	col := t.Muted
	if m.commentUI.focus == tok || m.commentUI.hover == tok {
		col = t.Accent
	}
	return zone.Mark(commentZoneID(tok), lipgloss.NewStyle().Foreground(col).Render(label))
}

// openInlineEdit opens (or re-focuses) the inline edit composer on comment id, prefilled with its body.
// Opening an edit closes any open reply composer: two open composers on one comment would be ambiguous.
func (m Model) openInlineEdit(id int64) (Model, tea.Cmd) {
	c, ok := findComment(m.details[m.viewKey].Comments, id)
	if !ok || !m.commentEditable(c) {
		return m, nil
	}
	if m.commentUI.editingID != id {
		m.commentUI.edit = newCommentArea("Edit your comment…")
		m.commentUI.edit.SetValue(c.Content)
		m.commentUI.editingID = id
		m.commentUI.editErr = ""
		m.commentUI.replyingTo = 0
	}
	return m.focusComment(commentFocus{id, partEditText})
}

// closeInlineEdit discards the edit composer and returns focus to the comment's Edit affordance.
func (m Model) closeInlineEdit() (Model, tea.Cmd) {
	id := m.commentUI.editingID
	m.commentUI.editingID = 0
	m.commentUI.editErr = ""
	return m.focusComment(commentFocus{id, partEdit})
}

// submitInlineEdit validates the edited body and sends it. An unchanged body is not resent: the server
// would stamp the comment "edited" for nothing.
func (m Model) submitInlineEdit() (Model, tea.Cmd) {
	id := m.commentUI.editingID
	if id == 0 || m.commentUI.sending != -1 {
		return m, nil
	}
	content := strings.TrimSpace(m.commentUI.edit.Value())
	if content == "" {
		m.commentUI.editErr = "Write something first"
		return m.focusComment(commentFocus{id, partEditText})
	}
	if c, ok := findComment(m.details[m.viewKey].Comments, id); ok && strings.TrimSpace(c.Content) == content {
		m, cmd := m.closeInlineEdit()
		return m, tea.Batch(cmd, toast.Show(toast.Info, "No changes."))
	}
	m.commentUI.sending = id // mark in flight synchronously so a rapid second Enter cannot double-send
	return m, updateCommentCmd(m.deps, m.viewKey, id, content, collectMentions(content, m.members))
}

// openCommentDelete floats a confirmation over the comment modal. Deleting is not reversible from the UI,
// so it is never a single keystroke.
func (m Model) openCommentDelete(id int64) (Model, tea.Cmd) {
	c, ok := findComment(m.details[m.viewKey].Comments, id)
	if !ok || !m.commentEditable(c) {
		return m, nil
	}
	m.commentDeleting = true
	m.commentDeleteID = id
	who := c.AuthorName
	if who == "" {
		who = "this comment"
	} else {
		who += "'s comment"
	}
	m.commentDeleteUI = widgets.NewConfirmForm(m.deps.Styles, "Delete comment",
		"Delete "+who+"? This cannot be undone.", "Delete")
	return m, m.commentDeleteUI.Init()
}

// updateCommentDelete drives the confirmation floated over the comment modal.
func (m Model) updateCommentDelete(msg tea.Msg) (Model, tea.Cmd) {
	switch msg.(type) {
	case widgets.ConfirmAcceptedMsg:
		m.commentDeleting = false
		return m, deleteCommentCmd(m.deps, m.viewKey, m.commentDeleteID)
	case widgets.ConfirmCancelledMsg:
		m.commentDeleting = false
		return m, nil
	}
	var cmd tea.Cmd
	m.commentDeleteUI, cmd = m.commentDeleteUI.Update(msg)
	return m, cmd
}

// loadMoreComments fetches the next page of the thread and appends it in place.
func (m Model) loadMoreComments() (Model, tea.Cmd) {
	if m.commentUI.loadingMore {
		return m, nil
	}
	d, ok := m.details[m.viewKey]
	if !ok || !d.CommentsHasMore {
		return m, nil
	}
	m.commentUI.loadingMore = true
	// the loaded roots are exactly the pages already consumed, so the next page index falls out of the
	// count rather than being tracked separately (a refetch resets both together)
	next := len(d.Comments) / domain.CommentPageSize
	return m, loadCommentPage(m.deps, m.viewKey, next)
}

// CommentEditDoneMsg is exported so the app shell routes an edit/delete result back to the project screen
// even if the user left the drill-in before it landed (so the toast still shows).
type CommentEditDoneMsg struct {
	key     string
	id      int64
	deleted bool
	err     bool
	errText string
}

func updateCommentCmd(d deps.Deps, key string, id int64, content string, mentions []string) tea.Cmd {
	return func() tea.Msg {
		if err := d.Issues.UpdateComment(context.Background(), key, id, content, mentions); err != nil {
			return CommentEditDoneMsg{key: key, id: id, err: true, errText: errmsg.Message(err, "Could not update the comment.")}
		}
		return CommentEditDoneMsg{key: key, id: id}
	}
}

func deleteCommentCmd(d deps.Deps, key string, id int64) tea.Cmd {
	return func() tea.Msg {
		if err := d.Issues.DeleteComment(context.Background(), key, id); err != nil {
			return CommentEditDoneMsg{key: key, id: id, deleted: true, err: true, errText: errmsg.Message(err, "Could not delete the comment.")}
		}
		return CommentEditDoneMsg{key: key, id: id, deleted: true}
	}
}

// commentPageLoadedMsg carries one "load more" page back to the screen.
type commentPageLoadedMsg struct {
	key     string // the issue the page was for, so a stale cross-issue result is ignored
	page    domain.IssueCommentPage
	err     bool
	errText string
}

func loadCommentPage(d deps.Deps, key string, page int) tea.Cmd {
	return func() tea.Msg {
		res, err := d.Issues.ListComments(context.Background(), key, page)
		if err != nil {
			return commentPageLoadedMsg{key: key, err: true, errText: errmsg.Message(err, "Could not load more comments.")}
		}
		return commentPageLoadedMsg{key: key, page: res}
	}
}

// onCommentEditDone reconciles an edit/delete result. The modal stays open on success so the refreshed
// thread shows the change in place; the detail is refetched either way, since a failure may still have
// applied (and the cached thread can no longer be trusted).
func (m Model) onCommentEditDone(msg CommentEditDoneMsg) (Model, tea.Cmd) {
	if m.commentUI.sending == msg.id {
		m.commentUI.sending = -1
	}
	if msg.err {
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
	}
	var focusCmd tea.Cmd
	if !msg.deleted && m.commentUI.editingID == msg.id {
		m, focusCmd = m.closeInlineEdit()
	}
	text := "Comment updated."
	if msg.deleted {
		text = "Comment deleted."
	}
	return m, tea.Batch(focusCmd, m.startDetailLoad(msg.key), toast.Show(toast.Success, text))
}

// onCommentPageLoaded appends a fetched page to the cached thread. The append is guarded on the page
// still belonging to the viewed issue, so a result that lands after the user moved on is dropped rather
// than grafted onto a different issue's comments.
func (m Model) onCommentPageLoaded(msg commentPageLoadedMsg) (Model, tea.Cmd) {
	m.commentUI.loadingMore = false
	if msg.err {
		return m, toast.Show(toast.Error, msg.errText)
	}
	d, ok := m.details[msg.key]
	if !ok {
		return m, nil
	}
	d.Comments = append(d.Comments, msg.page.Comments...)
	d.CommentsHasMore = msg.page.HasNext
	if msg.page.Total > 0 {
		d.CommentCount = msg.page.Total
	}
	m.details[msg.key] = d
	return m, nil
}
