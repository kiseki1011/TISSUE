package project

import (
	"context"
	"errors"
	"fmt"
	"net/http"

	tea "charm.land/bubbletea/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// openDeleteConfirm confirms deleting the viewed issue. The server soft-deletes into the trash, so the
// dialog promises a restore. Not optimistic: the row goes only once the server confirms.
func (m Model) openDeleteConfirm() (Model, tea.Cmd) {
	if m.viewKey == "" {
		return m, toast.Show(toast.Info, "No issue selected.")
	}
	name := m.viewKey
	if d, ok := m.details[m.viewKey]; ok && d.Title != "" {
		name = "\"" + flattenLine(d.Title) + "\""
	}
	message := "Delete " + name + "? It moves to the project's trash and can be restored later."
	if d, ok := m.details[m.viewKey]; ok && len(d.Children) > 0 {
		message += fmt.Sprintf(" It has %d sub-issue(s).", len(d.Children))
	}
	m.deleting = true
	m.deleteKey = m.viewKey // the exact issue the dialog is confirming, fixed for the delete command
	m.deleteUI = widgets.NewConfirmForm(m.deps.Styles, "Delete issue", message, "Delete")
	return m, m.deleteUI.Init()
}

func (m Model) updateDelete(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case widgets.ConfirmAcceptedMsg:
		return m, deleteIssueCmd(m.deps, m.deleteKey)
	case widgets.ConfirmCancelledMsg:
		m.deleting = false
		return m, nil
	case IssueDeletedMsg:
		if msg.err {
			// keep the dialog open with the reason (its own submit lock blocks navigation meanwhile)
			m.deleteUI.Submitting = false
			m.deleteUI.Status = msg.message
			return m, nil
		}
		m.deleting = false
		m.focus = focusList // the deleted issue's panel is gone, so drop back to the list
		m.removeIssue(msg.key)
		delete(m.details, msg.key)
		delete(m.detailsPending, msg.key)
		delete(m.detailsFailed, msg.key)
		m.detailGen[msg.key]++ // drop any still-in-flight detail load so it cannot resurrect the evicted cache
		m.viewKey = ""         // force the panel to re-point at the issue now under the cursor
		cmds := []tea.Cmd{toast.Show(toast.Success, "Issue deleted.")}
		if len(m.issues) == 0 && m.page.HasNext {
			// more pages exist server-side, so reload page 0 (moveCursor cannot page an empty list)
			m.loading, m.loadErr, m.loadingMore = true, false, false
			m.reqGen++
			cmds = append(cmds, loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false))
		} else {
			var sel tea.Cmd
			m, sel = m.syncSelection()
			cmds = append(cmds, sel)
		}
		return m, tea.Batch(cmds...)
	}
	var cmd tea.Cmd
	m.deleteUI, cmd = m.deleteUI.Update(msg)
	return m, cmd
}

func (m *Model) removeIssue(key string) {
	out := make([]domain.IssueSummary, 0, len(m.issues))
	for _, it := range m.issues {
		if it.Key != key {
			out = append(out, it)
		}
	}
	m.issues = out
	if m.cursor >= len(m.issues) {
		m.cursor = max(0, len(m.issues)-1)
	}
	if m.page.TotalElements > 0 {
		m.page.TotalElements--
	}
}

// IssueDeletedMsg is exported so the app shell can route it after the user has left the drill-in.
type IssueDeletedMsg struct {
	key     string
	err     bool
	message string // a friendly reason, shown in the dialog on failure
}

func deleteIssueCmd(d deps.Deps, key string) tea.Cmd {
	return func() tea.Msg {
		if err := d.Issues.DeleteIssue(context.Background(), key); err != nil {
			return IssueDeletedMsg{key: key, err: true, message: deleteErrorMessage(err)}
		}
		return IssueDeletedMsg{key: key}
	}
}

func deleteErrorMessage(err error) string {
	if m, ok := errmsg.Override(err); ok {
		return m // connectivity, or a leaky code mapped to friendlier copy
	}
	var apiErr *domain.APIError
	if errors.As(err, &apiErr) {
		switch apiErr.Status {
		case http.StatusForbidden:
			return "You do not have permission to delete this issue."
		case http.StatusConflict:
			return "This issue cannot be deleted (it may have sub-issues)."
		case http.StatusNotFound:
			return "This issue no longer exists."
		}
	}
	if r := domain.ErrorReason(err); r != "" {
		return r // prefer the server's explanation over the generic line
	}
	return "Could not delete the issue. Try again."
}
