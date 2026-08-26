package project

import (
	"context"
	"net/http"
	"strconv"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/errmsg"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// memberByID reads a member pinned by id, so an action survives the cursor moving on.
func (m Model) memberByID(id int64) (domain.ProjectMember, bool) {
	for _, mem := range m.members {
		if mem.MemberID == id {
			return mem, true
		}
	}
	return domain.ProjectMember{}, false
}

// openAddMemberPicker fetches the addable people, then opens a multi-select picker. The loading guard
// drops a repeat press so a double tap does not double-fetch.
func (m Model) openAddMemberPicker() (Model, tea.Cmd) {
	if m.memberCandidateLoading {
		return m, nil
	}
	m.memberCandidateGen++
	m.memberCandidateLoading = true
	return m, tea.Batch(
		toast.Show(toast.Info, "Loading people…"),
		loadMemberCandidates(m.deps, m.projectKey, m.memberCandidateGen),
	)
}

type memberCandidatesLoadedMsg struct {
	gen        int
	candidates []domain.ProjectMember
	err        bool
	status     int    // HTTP status on failure, so a 403 surfaces the manager-role hint
	reason     string // the server's explanation on failure, for the non-403 tail
}

func loadMemberCandidates(d deps.Deps, projectKey string, gen int) tea.Cmd {
	return func() tea.Msg {
		cs, err := d.Projects.ListMemberCandidates(context.Background(), projectKey, "")
		return memberCandidatesLoadedMsg{gen: gen, candidates: cs, err: err != nil, status: statusOf(err), reason: reasonOf(err)}
	}
}

// onMemberCandidatesLoaded opens the add picker once the candidates land. A superseded load, or one
// landing after the user moved on, is dropped so the picker cannot pop over what they are now using.
func (m Model) onMemberCandidatesLoaded(msg memberCandidatesLoadedMsg) (Model, tea.Cmd) {
	if msg.gen != m.memberCandidateGen {
		return m, nil
	}
	m.memberCandidateLoading = false
	if m.tab != tabMembers || m.CapturingInput() {
		return m, nil
	}
	if msg.err {
		if msg.status == http.StatusForbidden { // the candidate list is manager-gated, like the add itself
			return m, toast.Show(toast.Error, "You need the Manager role for that.")
		}
		return m, toast.Show(toast.Error, withReason("Couldn't load people to add.", msg.reason))
	}
	if len(msg.candidates) == 0 {
		return m, toast.Show(toast.Info, "Everyone is already a member.")
	}
	opts := make([]widgets.PickerOption, 0, len(msg.candidates))
	w := pickerMinW
	for _, c := range msg.candidates {
		label := memberCandidateLabel(c)
		opts = append(opts, widgets.PickerOption{Value: strconv.FormatInt(c.MemberID, 10), Label: label})
		if lw := lipgloss.Width(label) + 4; lw > w { // +4 for the "[ ] " checkbox before each label
			w = lw
		}
	}
	if w > pickerMaxW {
		w = pickerMaxW
	}
	m.picking = true
	m.pickKind = pickAddMember
	m.picker = widgets.NewMultiListPicker("Add members", opts, nil, assigneeRows, w)
	return m, nil
}

// memberCandidateLabel appends @username when it disambiguates two people sharing a display name.
func memberCandidateLabel(c domain.ProjectMember) string {
	label := flattenLine(c.Name())
	if c.Username != "" && c.Username != c.Name() {
		label += "  @" + c.Username
	}
	return label
}

// selectAddMembers adds the checked candidates in one batch. Enter with nothing checked just closes.
func (m Model) selectAddMembers() (Model, tea.Cmd) {
	values := m.picker.Selections()
	m.picking = false
	ids := make([]int64, 0, len(values))
	for _, v := range values {
		if id, err := strconv.ParseInt(v, 10, 64); err == nil {
			ids = append(ids, id)
		}
	}
	if len(ids) == 0 {
		return m, nil
	}
	return m, tea.Batch(
		toast.Show(toast.Info, "Adding…"),
		addMembersCmd(m.deps, m.projectKey, ids),
	)
}

// openMemberRolePicker opens on the member's current role, pinned by id so a roster reload cannot
// redirect the change.
func (m Model) openMemberRolePicker() (Model, tea.Cmd) {
	mem, ok := m.selectedMember()
	if !ok {
		return m, nil
	}
	m.memberActionID = mem.MemberID
	m.memberActionName = flattenLine(mem.Name())
	opts := []widgets.PickerOption{
		{Value: "MANAGER", Label: "Manager"},
		{Value: "MEMBER", Label: "Member"},
	}
	w := pickerMinW
	for _, o := range opts {
		if lw := lipgloss.Width(o.Label) + 2; lw > w {
			w = lw
		}
	}
	m.picking = true
	m.pickKind = pickMemberRole
	m.picker = widgets.NewListPicker("Set role", opts, mem.Role, pickerMaxRows, w)
	return m, nil
}

func (m Model) selectMemberRole() (Model, tea.Cmd) {
	opt, ok := m.picker.Selected()
	if !ok {
		return m, nil
	}
	m.picking = false
	if mem, ok := m.memberByID(m.memberActionID); ok && mem.Role == opt.Value {
		return m, toast.Show(toast.Info, "No change.")
	}
	return m, updateMemberRoleCmd(m.deps, m.projectKey, m.memberActionID, opt.Value)
}

// openMemberKickConfirm pins the target so a later roster reload cannot redirect the removal.
func (m Model) openMemberKickConfirm() (Model, tea.Cmd) {
	mem, ok := m.selectedMember()
	if !ok {
		return m, nil
	}
	m.memberActionID = mem.MemberID
	m.memberActionName = flattenLine(mem.Name())
	m.memberConfirming = true
	m.memberConfirmUI = widgets.NewConfirmForm(m.deps.Styles, "Remove member",
		"Remove "+m.memberActionName+" from this project? They lose access to it.", "Remove")
	return m, m.memberConfirmUI.Init()
}

func (m Model) updateMemberConfirm(msg tea.Msg) (Model, tea.Cmd) {
	switch msg.(type) {
	case widgets.ConfirmAcceptedMsg:
		m.memberConfirming = false
		return m, kickMemberCmd(m.deps, m.projectKey, m.memberActionID)
	case widgets.ConfirmCancelledMsg:
		m.memberConfirming = false
		return m, nil
	}
	var cmd tea.Cmd
	m.memberConfirmUI, cmd = m.memberConfirmUI.Update(msg)
	return m, cmd
}

// memberActionDoneMsg is the result of an add/role/kick command.
type memberActionDoneMsg struct {
	action string // "add" | "role" | "kick"
	count  int    // members added (add only)
	err    bool
	status int
	code   string // backend error code, for mapping a leaky code to friendlier copy
	reason string // the server's explanation on failure, so the toast can say why
}

func addMembersCmd(d deps.Deps, projectKey string, ids []int64) tea.Cmd {
	return func() tea.Msg {
		err := d.Projects.AddProjectMembers(context.Background(), projectKey, ids)
		return memberActionDoneMsg{action: "add", count: len(ids), err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func updateMemberRoleCmd(d deps.Deps, projectKey string, id int64, role string) tea.Cmd {
	return func() tea.Msg {
		err := d.Projects.UpdateMemberRole(context.Background(), projectKey, id, role)
		return memberActionDoneMsg{action: "role", count: 1, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

func kickMemberCmd(d deps.Deps, projectKey string, id int64) tea.Cmd {
	return func() tea.Msg {
		err := d.Projects.KickProjectMember(context.Background(), projectKey, id)
		return memberActionDoneMsg{action: "kick", count: 1, err: err != nil, status: statusOf(err), code: codeOf(err), reason: reasonOf(err)}
	}
}

// onMemberActionDone reloads the roster, plus the per-member stats when membership changed (a role change
// leaves the stats untouched). The stats gen bump drops any superseded stats load.
func (m Model) onMemberActionDone(msg memberActionDoneMsg) (Model, tea.Cmd) {
	if msg.err {
		return m, toast.Show(toast.Error, memberActionErrorText(msg.action, msg.status, msg.code, msg.reason))
	}
	// keep the highlight across the reload. Departed caches are pruned when the fresh roster lands, not
	// here: if the reload fails the member is still shown, with stale issues rather than a stuck spinner.
	m.memberRestoreID = m.selMemberID
	m.membersRequested = true
	cmds := []tea.Cmd{
		toast.Show(toast.Success, memberActionOkText(msg.action, msg.count)),
		loadMembers(m.deps, m.projectKey),
	}
	if msg.action != "role" {
		m.memberStatsRequested = true
		m.memberStatsErr = false
		m.memberStatsGen++
		cmds = append(cmds, loadMemberStats(m.deps, m.projectKey, m.memberStatsGen))
	}
	return m, tea.Batch(cmds...)
}

func memberActionOkText(action string, count int) string {
	switch action {
	case "add":
		if count == 1 {
			return "Member added."
		}
		return strconv.Itoa(count) + " members added."
	case "role":
		return "Role updated."
	case "kick":
		return "Member removed."
	}
	return "Done."
}

func memberActionErrorText(action string, status int, code, reason string) string {
	if code == "PROJECT_MEMBER_NOT_FOUND" && action != "add" {
		// here the code is about the TARGET member, not the caller, so the generic caller-scoped mapping
		// ("you're not a member") would mislead the acting manager
		return "That member is no longer in this project."
	}
	if m, ok := errmsg.OverrideParts(status, code); ok {
		return m // connectivity, or a mapped leaky code (PROJECT_MEMBER_NOT_FOUND / PROJECT_MANAGER_REQUIRED / …)
	}
	switch status {
	case http.StatusForbidden:
		// a change/remove 403 can also mean the target is a fellow manager, so it must not claim the actor
		// lacks a role they may well hold
		if action == "add" {
			return "You need the Manager role for that."
		}
		return "You don't have permission to change this member."
	case http.StatusConflict:
		if action == "add" {
			return "They are already a member of this project."
		}
	}
	if reason != "" {
		return reason // the server's explanation for the long tail (validation, not-found, …)
	}
	switch action {
	case "add":
		return "Couldn't add the member."
	case "role":
		return "Couldn't change the role."
	case "kick":
		return "Couldn't remove the member."
	}
	return "Something went wrong."
}
