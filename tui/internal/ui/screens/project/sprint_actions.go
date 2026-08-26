package project

import (
	"context"
	"errors"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// sortSprintsRecentFirst orders by id, which is monotonic with creation. The ACTIVE sprint is flagged in
// the row rather than pinned, since a just-created upcoming sprint is legitimately newer.
func sortSprintsRecentFirst(in []domain.SprintSummary) []domain.SprintSummary {
	out := append([]domain.SprintSummary(nil), in...)
	sort.SliceStable(out, func(i, j int) bool { return out[i].ID > out[j].ID })
	return out
}

func indexOfSprint(sprints []domain.SprintSummary, id int64) int {
	for i := range sprints {
		if sprints[i].ID == id {
			return i
		}
	}
	return -1
}

// isCurrentSprint: exactly the ACTIVE sprint, of which there is at most one.
func isCurrentSprint(sp domain.SprintSummary) bool { return sp.Status == "ACTIVE" }

func sprintName(sp domain.SprintSummary) string {
	if t := strings.TrimSpace(sp.Title); t != "" {
		return "\"" + flattenLine(t) + "\""
	}
	return sp.Key
}

// openSprintStart reuses m.dating via the dateSprintStart target, which fires the start on confirm.
func (m Model) openSprintStart() (Model, tea.Cmd) {
	sp, ok := m.selectedSprint()
	if !ok {
		return m, nil
	}
	if sp.Status != "PLANNING" {
		return m, toast.Show(toast.Info, "Only a planning sprint can be started.")
	}
	m.sprintActionID = sp.ID
	m.dating = true
	m.dateTarget = dateSprintStart
	// default a two-week sprint. the due date must be in the future (the server stamps the start at now)
	m.datePick = widgets.NewDatePicker("Due date", time.Now().AddDate(0, 0, 14), false, false, datePickerW)
	return m, nil
}

func (m Model) openSprintComplete() (Model, tea.Cmd) {
	sp, ok := m.selectedSprint()
	if !ok {
		return m, nil
	}
	if sp.Status != "ACTIVE" {
		return m, toast.Show(toast.Info, "Only the active sprint can be completed.")
	}
	return m.openSprintConfirm(sp, "complete", "Complete sprint",
		"Complete "+sprintName(sp)+"? Its issues must all be resolved.", "Complete")
}

func (m Model) openSprintCancel() (Model, tea.Cmd) {
	sp, ok := m.selectedSprint()
	if !ok {
		return m, nil
	}
	if sp.Status != "PLANNING" && sp.Status != "ACTIVE" {
		return m, toast.Show(toast.Info, "This sprint can't be cancelled.")
	}
	return m.openSprintConfirm(sp, "cancel", "Cancel sprint",
		"Cancel "+sprintName(sp)+"? This abandons the sprint.", "Confirm")
}

// openSprintConfirm pins the target sprint so a later cursor move cannot redirect the action.
func (m Model) openSprintConfirm(sp domain.SprintSummary, kind, title, message, accept string) (Model, tea.Cmd) {
	m.sprintActionID = sp.ID
	m.sprintConfirming = true
	m.sprintConfirmKind = kind
	m.sprintConfirmUI = widgets.NewConfirmForm(m.deps.Styles, title, message, accept)
	return m, m.sprintConfirmUI.Init()
}

// openSprintEditForm refuses a closed (completed/cancelled) sprint.
func (m Model) openSprintEditForm() (Model, tea.Cmd) {
	sp, ok := m.selectedSprint()
	if !ok {
		return m, nil
	}
	if sp.Status == "COMPLETED" || sp.Status == "CANCELLED" {
		return m, toast.Show(toast.Info, "A closed sprint can't be edited.")
	}
	m.sprintActionID = sp.ID
	m.sprintEditing = true
	m.sprintEditScroll = 0
	m.sprintEditBase = sp // diff the save against this snapshot, not a live summary a background reload may change
	m.sprintEditUI = newSprintEditForm(m.deps, sp)
	return m, m.sprintEditUI.Init()
}

// openSprintCreateForm needs no selection: an empty tab is exactly when a first sprint is needed.
func (m Model) openSprintCreateForm() (Model, tea.Cmd) {
	m.sprintEditing = true
	m.sprintCreating = true
	m.sprintEditScroll = 0
	m.sprintEditUI = newSprintCreateForm(m.deps)
	return m, m.sprintEditUI.Init()
}

// The server deletes only a CANCELLED sprint, so the guard says why rather than letting the call fail.
func (m Model) openSprintDelete() (Model, tea.Cmd) {
	sp, ok := m.selectedSprint()
	if !ok {
		return m, nil
	}
	if sp.Status != "CANCELLED" {
		return m, toast.Show(toast.Info, "Only a cancelled sprint can be deleted. Cancel it first (x).")
	}
	return m.openSprintConfirm(sp, "delete", "Delete sprint",
		"Delete "+sprintName(sp)+"? This cannot be undone.", "Delete")
}

func (m Model) openSprintEditDuePicker() (Model, tea.Cmd) {
	var initial time.Time
	if m.sprintEditUI.dueSet {
		initial = m.sprintEditUI.dueAt
	}
	m.dating = true
	m.dateTarget = dateSprintEditDue
	m.datePick = widgets.NewDatePicker("Due date", initial, false, true, datePickerW)
	return m, nil
}

func (m Model) updateSprintConfirm(msg tea.Msg) (Model, tea.Cmd) {
	switch msg.(type) {
	case widgets.ConfirmAcceptedMsg:
		m.sprintConfirming = false
		switch m.sprintConfirmKind {
		case "cancel":
			return m, cancelSprintCmd(m.deps, m.sprintActionID)
		case "delete":
			return m, deleteSprintCmd(m.deps, m.sprintActionID)
		case "migrate":
			return m, migrateIssuesCmd(m.deps, m.migrateSourceID, m.migrateTargetID, m.migrateKeys)
		}
		return m, completeSprintCmd(m.deps, m.sprintActionID)
	case widgets.ConfirmCancelledMsg:
		m.sprintConfirming = false
		return m, nil
	}
	var cmd tea.Cmd
	m.sprintConfirmUI, cmd = m.sprintConfirmUI.Update(msg)
	return m, cmd
}

func (m Model) openSprintRemoveIssuePicker() (Model, tea.Cmd) {
	sp, ok := m.selectedSprint()
	if !ok {
		return m, nil
	}
	if sp.Status == "COMPLETED" || sp.Status == "CANCELLED" {
		return m, toast.Show(toast.Info, "A closed sprint's issues can't be changed.")
	}
	page, ok := m.sprintIssues[sp.ID]
	if !ok || len(page.Issues) == 0 {
		return m, toast.Show(toast.Info, "This sprint has no issues to remove.")
	}
	opts := make([]widgets.PickerOption, 0, len(page.Issues))
	w := pickerMinW
	for _, it := range page.Issues {
		label := it.Key + "  " + flattenLine(it.Title)
		opts = append(opts, widgets.PickerOption{Value: it.Key, Label: label})
		if lw := lipgloss.Width(label) + 2; lw > w {
			w = lw
		}
	}
	if w > pickerMaxW {
		w = pickerMaxW
	}
	m.sprintActionID = sp.ID
	m.picking = true
	m.pickKind = pickSprintRemoveIssue
	m.picker = widgets.NewSearchableListPicker("Remove from sprint", opts, "", assigneeRows, w)
	return m, nil
}

func (m Model) selectSprintRemoveIssue() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	return m, removeSprintIssueCmd(m.deps, m.sprintActionID, opt.Value)
}

func (m Model) addSelectedIssueToSprint() (Model, tea.Cmd) {
	it, ok := m.selectedIssue()
	if !ok {
		return m, toast.Show(toast.Info, "No issue selected.")
	}
	cur := m.currentSprint
	if cur == nil {
		return m, toast.Show(toast.Info, "No active sprint to add to.")
	}
	if it.SprintID == cur.ID {
		return m, toast.Show(toast.Info, "Already in the current sprint.")
	}
	return m, addIssueToSprintCmd(m.deps, cur.ID, it.Key, it.SprintID)
}

func (m Model) updateSprintEdit(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case sprintEditCancelledMsg:
		m.sprintEditing, m.sprintCreating = false, false
		return m, nil
	case sprintEditSubmittedMsg:
		return m.submitSprintEdit(msg.v)
	case openSprintEditDueMsg:
		return m.openSprintEditDuePicker()
	case tea.MouseWheelMsg:
		if lipgloss.Height(m.sprintEditUI.View()) > m.height {
			switch msg.Button {
			case tea.MouseWheelUp:
				m.sprintEditScroll = clampScroll(m.sprintEditScroll-1, m.sprintEditScrollMax())
				return m, nil
			case tea.MouseWheelDown:
				m.sprintEditScroll = clampScroll(m.sprintEditScroll+1, m.sprintEditScrollMax())
				return m, nil
			}
		}
	}
	var cmd tea.Cmd
	m.sprintEditUI, cmd = m.sprintEditUI.Update(msg)
	return m.followSprintEditFocus(), cmd
}

func (m Model) sprintEditScrollMax() int {
	return max(0, lipgloss.Height(m.sprintEditUI.View())-m.height)
}

func (m Model) followSprintEditFocus() Model {
	row, height, ok := m.sprintEditUI.FocusRow()
	if !ok {
		return m
	}
	boxH := lipgloss.Height(m.sprintEditUI.View())
	if boxH <= m.height {
		return m
	}
	visible := m.height - 2
	off := m.sprintEditScroll
	top, bottom := row, row+max(1, height)-1
	if top < 1+off {
		off = top - 1
	} else if bottom > off+visible {
		off = bottom - visible
	}
	m.sprintEditScroll = min(max(off, 0), boxH-m.height)
	return m
}

// submitSprintEdit PATCHes only what changed, diffed against the snapshot the form was built from - not a
// live summary a background reload may have changed mid-edit.
func (m Model) submitSprintEdit(v sprintEditValues) (Model, tea.Cmd) {
	m.sprintEditing = false
	if m.sprintCreating {
		m.sprintCreating = false
		return m, createSprintCmd(m.deps, m.projectKey, v.title, v.goal)
	}
	edit := diffSprintEdit(m.sprintEditBase, v)
	if edit.Empty() {
		return m, toast.Show(toast.Info, "No changes.")
	}
	return m, updateSprintCmd(m.deps, m.sprintActionID, edit)
}

// diffSprintEdit includes a field only when it differs. The due date compares at day granularity.
func diffSprintEdit(orig domain.SprintSummary, v sprintEditValues) domain.SprintEdit {
	var out domain.SprintEdit
	if v.title != strings.TrimSpace(orig.Title) {
		out.Title = &v.title
	}
	if v.goal != strings.TrimSpace(orig.Goal) { // v.goal is already trimmed, so trim both sides to compare
		out.Goal = &v.goal
	}
	if formatDateOnly(v.dueAt) != formatDateOnly(orig.DueAt) {
		if !v.dueSet {
			out.ClearDue = true
		} else {
			out.DueAt = &v.dueAt
		}
	}
	return out
}

type sprintActionDoneMsg struct {
	action string // "start" | "complete" | "cancel" | "edit" | "create" | "delete"
	id     int64
	err    bool
	status int
	code   string // backend error code, mapped to friendlier copy
	reason string // the server's explanation, so the toast can say why
}

func (m Model) onSprintActionDone(msg sprintActionDoneMsg) (Model, tea.Cmd) {
	if msg.err {
		return m, toast.Show(toast.Error, sprintActionErrorText(msg.action, msg.status, msg.code, msg.reason))
	}
	// reload the list silently and re-point the cursor at the same sprint. its issues are unchanged
	m.sprintRestoreID = msg.id
	m.sprintReqGen++
	return m, tea.Batch(
		toast.Show(toast.Success, sprintActionOkText(msg.action)),
		loadSprints(m.deps, m.projectKey, m.sprintReqGen),
	)
}

func sprintActionOkText(action string) string {
	switch action {
	case "start":
		return "Sprint started."
	case "complete":
		return "Sprint completed."
	case "cancel":
		return "Sprint cancelled."
	case "edit":
		return "Sprint updated."
	case "create":
		return "Sprint created."
	case "delete":
		return "Sprint deleted."
	}
	return "Done."
}

func sprintActionErrorText(action string, status int, code, reason string) string {
	if m, ok := errmsg.OverrideParts(status, code); ok {
		return m // connectivity, or a mapped leaky code (PROJECT_MEMBER_NOT_FOUND / PROJECT_MANAGER_REQUIRED / …)
	}
	if status == http.StatusForbidden {
		return "You need the Manager role for that."
	}
	switch action {
	case "start":
		if status == http.StatusConflict {
			return "Another sprint is already active."
		}
		if reason != "" {
			return reason
		}
		return "Couldn't start the sprint - the due date must be in the future."
	case "complete":
		if reason != "" {
			return reason
		}
		return "Couldn't complete the sprint - resolve or move its open issues first."
	case "cancel":
		return withReason("Couldn't cancel the sprint.", reason)
	case "edit":
		return withReason("Couldn't update the sprint.", reason)
	case "create":
		return withReason("Couldn't create the sprint.", reason)
	case "delete":
		if reason != "" {
			return reason
		}
		return "Couldn't delete the sprint - only a cancelled sprint can be deleted."
	}
	return "Something went wrong."
}

func startSprintCmd(d deps.Deps, id int64, dueAt time.Time) tea.Cmd {
	return func() tea.Msg {
		err := d.Sprints.StartSprint(context.Background(), id, dueAt)
		return sprintActionDoneMsg{action: "start", id: id, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func completeSprintCmd(d deps.Deps, id int64) tea.Cmd {
	return func() tea.Msg {
		err := d.Sprints.CompleteSprint(context.Background(), id)
		return sprintActionDoneMsg{action: "complete", id: id, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

// createSprintCmd adds the sprint and reports its new id, so the reloaded list can land the cursor on it.
func createSprintCmd(d deps.Deps, projectKey, title, goal string) tea.Cmd {
	return func() tea.Msg {
		id, err := d.Sprints.CreateSprint(context.Background(), projectKey, title, goal)
		return sprintActionDoneMsg{action: "create", id: id, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func deleteSprintCmd(d deps.Deps, id int64) tea.Cmd {
	return func() tea.Msg {
		err := d.Sprints.DeleteSprint(context.Background(), id)
		return sprintActionDoneMsg{action: "delete", id: id, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func cancelSprintCmd(d deps.Deps, id int64) tea.Cmd {
	return func() tea.Msg {
		err := d.Sprints.CancelSprint(context.Background(), id)
		return sprintActionDoneMsg{action: "cancel", id: id, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

// sprintIssuesChangedMsg is the result of moving an issue in or out of a sprint.
type sprintIssuesChangedMsg struct {
	sprintID     int64
	prevSprintID int64 // the issue's sprint before an add (0 = backlog), so its stale cache is evicted too
	issueKey     string
	added        bool
	err          bool
	status       int
	code         string // backend error code, mapped to friendlier copy
	reason       string // the server's explanation, so the toast can say why
}

func addIssueToSprintCmd(d deps.Deps, sprintID int64, issueKey string, prevSprintID int64) tea.Cmd {
	return func() tea.Msg {
		err := d.Sprints.AddSprintIssues(context.Background(), sprintID, []string{issueKey})
		return sprintIssuesChangedMsg{sprintID: sprintID, prevSprintID: prevSprintID, issueKey: issueKey, added: true, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func removeSprintIssueCmd(d deps.Deps, sprintID int64, issueKey string) tea.Cmd {
	return func() tea.Msg {
		err := d.Sprints.RemoveSprintIssues(context.Background(), sprintID, []string{issueKey})
		return sprintIssuesChangedMsg{sprintID: sprintID, issueKey: issueKey, added: false, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

// onSprintIssuesChanged evicts or refetches the caches that can now disagree with the server.
func (m Model) onSprintIssuesChanged(msg sprintIssuesChangedMsg) (Model, tea.Cmd) {
	if msg.err {
		return m, toast.Show(toast.Error, sprintIssuesErrorText(msg.added, msg.status, msg.code, msg.reason))
	}
	ok := "Removed " + msg.issueKey + " from the sprint."
	if msg.added {
		ok = "Added " + msg.issueKey + " to the sprint."
	}
	cmds := []tea.Cmd{toast.Show(toast.Success, ok)}

	// keep the row's sprint in step so the "already in the current sprint" guard is right without a reload
	newSprint := msg.sprintID
	if !msg.added {
		newSprint = 0
	}
	m.patchRowSprintID(msg.issueKey, newSprint)

	cmds = append(cmds, m.evictSprintIssues(msg.sprintID)...)
	// an add may have moved the issue out of another sprint, whose cached list is now stale too
	if msg.added && msg.prevSprintID != 0 && msg.prevSprintID != msg.sprintID {
		cmds = append(cmds, m.evictSprintIssues(msg.prevSprintID)...)
	}
	// refetch the Details panel's issue if it is the one that moved
	if msg.issueKey != "" && msg.issueKey == m.viewKey {
		if _, cached := m.details[msg.issueKey]; cached {
			cmds = append(cmds, m.startDetailLoad(msg.issueKey))
		}
	}
	return m, tea.Batch(cmds...)
}

// evictSprintIssues drops a sprint's cached issue list. The selected sprint refetches now, otherwise the
// eviction only supersedes any in-flight load so a later re-select refetches.
func (m *Model) evictSprintIssues(sprintID int64) []tea.Cmd {
	delete(m.sprintIssues, sprintID)
	delete(m.sprintIssuesFailed, sprintID)
	if sprintID == m.selSprintID {
		return []tea.Cmd{m.startSprintIssuesLoad(sprintID)}
	}
	m.sprintIssuesGen[sprintID]++
	delete(m.sprintIssuesPending, sprintID)
	return nil
}

// patchRowSprintID reflects an add/remove immediately (patchRow cannot: IssueDetail has no sprint field).
func (m *Model) patchRowSprintID(key string, sprintID int64) {
	if key == "" {
		return
	}
	for i := range m.issues {
		if m.issues[i].Key == key {
			m.issues[i].SprintID = sprintID
			return
		}
	}
}

func sprintIssuesErrorText(added bool, status int, code, reason string) string {
	if m, ok := errmsg.OverrideParts(status, code); ok {
		return m
	}
	if status == http.StatusBadRequest {
		return "The sprint is closed, so its issues can't be changed."
	}
	if reason != "" {
		return reason
	}
	if added {
		return "Couldn't add the issue to the sprint."
	}
	return "Couldn't remove the issue from the sprint."
}

func findActiveSprint(sprints []domain.SprintSummary) *domain.SprintSummary {
	for i := range sprints {
		if sprints[i].Status == "ACTIVE" {
			sp := sprints[i]
			return &sp
		}
	}
	return nil
}

// migrateMaxCandidates matches the server's per-request migrate limit. A larger sprint is truncated.
const migrateMaxCandidates = 100

// findNextPlanningSprint is the oldest (lowest-id) PLANNING sprint - ids are monotonic with creation, and
// a sprint planned before the active one started is still a valid onward target. nil when there is none.
func findNextPlanningSprint(sprints []domain.SprintSummary) *domain.SprintSummary {
	var next *domain.SprintSummary
	for i := range sprints {
		sp := sprints[i]
		if sp.Status != "PLANNING" {
			continue
		}
		if next == nil || sp.ID < next.ID {
			s := sp
			next = &s
		}
	}
	return next
}

// openMigrate resolves the onward target (the next PLANNING sprint) and fetches the incomplete issues so
// the confirmation can list them. It always acts on the current sprint, never the highlighted row.
func (m Model) openMigrate() (Model, tea.Cmd) {
	if m.migrateLoading {
		return m, nil // a candidate load is already in flight, so ignore the repeat press
	}
	if m.currentSprint == nil {
		return m, toast.Show(toast.Info, "No active sprint to migrate from.")
	}
	target := findNextPlanningSprint(m.sprints)
	if target == nil {
		return m, toast.Show(toast.Info, "No planning sprint to migrate into - create one first.")
	}
	m.migrateSourceID = m.currentSprint.ID
	m.migrateTargetID = target.ID
	m.migrateSourceName = sprintName(*m.currentSprint)
	m.migrateTargetName = sprintName(*target)
	m.migrateGen++
	m.migrateLoading = true
	return m, loadMigrateCandidates(m.deps, m.projectKey, m.currentSprint.ID, m.migrateGen)
}

type migrateCandidatesLoadedMsg struct {
	gen       int
	issues    []domain.IssueSummary
	truncated bool
	err       bool
}

// loadMigrateCandidates fetches the incomplete issues, the only ones the server migrates.
func loadMigrateCandidates(d deps.Deps, projectKey string, sourceID int64, gen int) tea.Cmd {
	return func() tea.Msg {
		f := domain.IssueFilter{SprintIDs: []int64{sourceID}, StateCategories: []string{"INITIAL", "ACTIVE"}}
		p, err := d.Issues.SearchProjectIssues(context.Background(), projectKey, f, 0, migrateMaxCandidates)
		return migrateCandidatesLoadedMsg{gen: gen, issues: p.Issues, truncated: p.HasNext, err: err != nil}
	}
}

// onMigrateCandidatesLoaded opens the confirmation listing what carries over. Stale loads are dropped.
func (m Model) onMigrateCandidatesLoaded(msg migrateCandidatesLoadedMsg) (Model, tea.Cmd) {
	if msg.gen != m.migrateGen {
		return m, nil
	}
	m.migrateLoading = false
	// the confirmation opens asynchronously, so the user may have moved on. Never steal the screen: drop the
	// result rather than pop a dialog over whatever they are now interacting with.
	if m.tab != tabSprints || m.CapturingInput() {
		return m, nil
	}
	if msg.err {
		return m, toast.Show(toast.Error, "Couldn't load the sprint's issues.")
	}
	if len(msg.issues) == 0 {
		return m, toast.Show(toast.Info, "No incomplete issues to migrate.")
	}
	m.migrateKeys = make([]string, 0, len(msg.issues))
	for _, it := range msg.issues {
		m.migrateKeys = append(m.migrateKeys, it.Key)
	}
	m.sprintConfirming = true
	m.sprintConfirmKind = "migrate"
	m.sprintConfirmUI = widgets.NewConfirmForm(m.deps.Styles, "Migrate issues",
		migrateConfirmMessage(m.migrateSourceName, m.migrateTargetName, msg.issues, msg.truncated), "Migrate")
	return m, m.sprintConfirmUI.Init()
}

// migrateConfirmMessage summarises the carry-over: count, source, target, and a capped key list.
// ConfirmForm flattens newlines, so the keys are comma-joined and wrap within the dialog width.
func migrateConfirmMessage(sourceName, targetName string, issues []domain.IssueSummary, truncated bool) string {
	const maxKeys = 12
	noun := "issues"
	if len(issues) == 1 {
		noun = "issue"
	}
	count := strconv.Itoa(len(issues))
	if truncated {
		count += "+" // more exist than were fetched. the server still moves them all
	}
	keys := make([]string, 0, min(len(issues), maxKeys))
	for i, it := range issues {
		if i >= maxKeys {
			break
		}
		keys = append(keys, it.Key)
	}
	list := strings.Join(keys, ", ")
	if len(issues) > maxKeys {
		list += ", +" + strconv.Itoa(len(issues)-maxKeys) + " more"
	}
	return "Move " + count + " incomplete " + noun + " from " + sourceName + " to " + targetName + ": " + list + "."
}

type sprintMigrateDoneMsg struct {
	sourceID int64
	targetID int64
	keys     []string
	err      bool
	status   int
	code     string // backend error code, mapped to friendlier copy
	reason   string // the server's explanation, so the toast can say why
}

// migrateIssuesCmd carries the source sprint's incomplete issues over to the target. keys is not sent -
// the server moves them all - and rides along only so the success handler can repoint those list rows.
func migrateIssuesCmd(d deps.Deps, sourceID, targetID int64, keys []string) tea.Cmd {
	return func() tea.Msg {
		err := d.Sprints.MigrateSprintIssues(context.Background(), sourceID, targetID)
		return sprintMigrateDoneMsg{sourceID: sourceID, targetID: targetID, keys: keys, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func (m Model) onSprintMigrateDone(msg sprintMigrateDoneMsg) (Model, tea.Cmd) {
	if msg.err {
		return m, toast.Show(toast.Error, migrateErrorText(msg.status, msg.code, msg.reason))
	}
	noun := "issue"
	if len(msg.keys) != 1 {
		noun = "issues"
	}
	cmds := []tea.Cmd{toast.Show(toast.Success, "Migrated "+strconv.Itoa(len(msg.keys))+" "+noun+" to the next sprint.")}
	for _, k := range msg.keys {
		m.patchRowSprintID(k, msg.targetID)
	}
	// both sprints' issue lists changed, so evict (and refetch the selected one)
	cmds = append(cmds, m.evictSprintIssues(msg.sourceID)...)
	cmds = append(cmds, m.evictSprintIssues(msg.targetID)...)
	// refetch the Details panel's issue if it was migrated
	if m.viewKey != "" {
		for _, k := range msg.keys {
			if k == m.viewKey {
				if _, cached := m.details[k]; cached {
					cmds = append(cmds, m.startDetailLoad(k))
				}
				break
			}
		}
	}
	return m, tea.Batch(cmds...)
}

func migrateErrorText(status int, code, reason string) string {
	if m, ok := errmsg.OverrideParts(status, code); ok {
		return m
	}
	switch status {
	case http.StatusForbidden:
		return "You need the manager role to migrate issues."
	case http.StatusBadRequest:
		return "A sprint is closed, so issues can't be migrated."
	}
	return withReason("Couldn't migrate the issues.", reason)
}

type currentSprintLoadedMsg struct {
	gen    int // the currentSprint load generation, so a superseded prefetch is ignored
	sprint *domain.SprintSummary
	err    bool
}

func loadCurrentSprint(d deps.Deps, projectKey string, gen int) tea.Cmd {
	return func() tea.Msg {
		sp, err := d.Sprints.CurrentSprint(context.Background(), projectKey)
		return currentSprintLoadedMsg{gen: gen, sprint: sp, err: err != nil}
	}
}

func updateSprintCmd(d deps.Deps, id int64, e domain.SprintEdit) tea.Cmd {
	return func() tea.Msg {
		err := d.Sprints.UpdateSprint(context.Background(), id, e)
		return sprintActionDoneMsg{action: "edit", id: id, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

// statusOf pulls the HTTP status from an APIError (errors.As unwraps). A transport error stays 0.
func statusOf(err error) int {
	var ae *domain.APIError
	if errors.As(err, &ae) {
		return ae.Status
	}
	return 0
}

// reasonOf is the server's human explanation, or "" for a transport or body-less failure.
func reasonOf(err error) string { return domain.ErrorReason(err) }

// codeOf is the backend error code (ProblemDetail title), which errmsg maps to friendlier copy.
func codeOf(err error) string { return domain.ErrorCode(err) }

// withReason appends the server's explanation when there is one, so a failure always names its cause.
func withReason(base, reason string) string {
	if reason == "" {
		return base
	}
	return base + " " + reason
}
