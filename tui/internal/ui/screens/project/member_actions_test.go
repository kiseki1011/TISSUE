package project

import (
	"net/http"
	"testing"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

// "a" starts the candidate load, guarded against a double press. The picker opens only once they land.
func TestAddMemberFlowOpensPicker(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m, cmd := m.Update(press("a"))
	if !m.memberCandidateLoading || cmd == nil {
		t.Fatalf("pressing a should start the candidate load: loading=%v cmd=%v", m.memberCandidateLoading, cmd != nil)
	}
	gen := m.memberCandidateGen
	m2, cmd2 := m.Update(press("a"))
	if cmd2 != nil || m2.memberCandidateGen != gen {
		t.Errorf("a repeat add press while loading should be a no-op, got cmd=%v gen=%d->%d", cmd2 != nil, gen, m2.memberCandidateGen)
	}
	m, _ = m.Update(memberCandidatesLoadedMsg{gen: gen, candidates: []domain.ProjectMember{
		{MemberID: 9, DisplayName: "Nia", Username: "nia"},
	}})
	if !m.picking || m.pickKind != pickAddMember {
		t.Fatalf("landing candidates should open the add-member picker: picking=%v kind=%v", m.picking, m.pickKind)
	}
	if !m.picker.Multi() {
		t.Error("the add-member picker should be multi-select")
	}
}

func TestAddMemberStaleGenDropped(t *testing.T) {
	m := membersTabModel(t, memberList(1))
	m, _ = m.Update(press("a")) // memberCandidateGen -> 1
	m, _ = m.Update(memberCandidatesLoadedMsg{gen: 0, candidates: []domain.ProjectMember{{MemberID: 9}}})
	if m.picking {
		t.Error("a stale-generation candidate load should not open the picker")
	}
}

// Candidates landing after the user left the Members tab must not steal the screen.
func TestAddMemberCandidatesDroppedOffTab(t *testing.T) {
	m := membersTabModel(t, memberList(1))
	m, _ = m.Update(press("a"))
	gen := m.memberCandidateGen
	m.tab = tabIssues // the user navigated away while the candidates loaded
	m, _ = m.Update(memberCandidatesLoadedMsg{gen: gen, candidates: []domain.ProjectMember{{MemberID: 9}}})
	if m.picking {
		t.Error("candidates landing off the Members tab should not open the picker")
	}
	if m.memberCandidateLoading {
		t.Error("the loading guard should still clear even when the result is dropped")
	}
}

// An empty candidate set closes the flow with a note instead of an empty picker.
func TestAddMemberNoCandidates(t *testing.T) {
	m := membersTabModel(t, memberList(1))
	m, _ = m.Update(press("a"))
	m, cmd := m.Update(memberCandidatesLoadedMsg{gen: m.memberCandidateGen, candidates: nil})
	if m.picking {
		t.Error("no candidates should not open a picker")
	}
	if cmd == nil {
		t.Error("no candidates should still surface a toast")
	}
}

// Confirming with rows checked fires the batch add. With nothing checked it just closes.
func TestSelectAddMembers(t *testing.T) {
	m := membersTabModel(t, memberList(1))
	m.picking = true
	m.pickKind = pickAddMember
	m.picker = widgets.NewMultiListPicker("Add members", []widgets.PickerOption{
		{Value: "7", Label: "Ada"}, {Value: "8", Label: "Ben"},
	}, []string{"7", "8"}, 10, 30)
	m, cmd := m.selectAddMembers()
	if m.picking {
		t.Error("selecting should close the picker")
	}
	if cmd == nil {
		t.Error("checked candidates should fire the batch add")
	}

	empty := membersTabModel(t, memberList(1))
	empty.picking = true
	empty.pickKind = pickAddMember
	empty.picker = widgets.NewMultiListPicker("Add members", []widgets.PickerOption{{Value: "7", Label: "Ada"}}, nil, 10, 30)
	empty, cmd = empty.selectAddMembers()
	if empty.picking {
		t.Error("confirming with nothing checked should close the picker")
	}
	if cmd != nil {
		t.Error("confirming with nothing checked should not fire an add")
	}
}

// The role picker opens pinned to the member, cursor on their current role. The same role is a no-op.
func TestMemberRolePicker(t *testing.T) {
	m := membersTabModel(t, memberList(2)) // member 1 = MANAGER, 2 = MEMBER
	m.memberCursor = 1
	m, _ = m.syncMemberSelection()
	m, _ = m.openMemberRolePicker()
	if !m.picking || m.pickKind != pickMemberRole || m.memberActionID != 2 {
		t.Fatalf("role picker should open pinned to member 2: picking=%v kind=%v id=%d", m.picking, m.pickKind, m.memberActionID)
	}
	sel, _ := m.picker.Selected()
	if sel.Value != "MEMBER" {
		t.Errorf("cursor should start on the member's current role, got %q", sel.Value)
	}

	// the same role still closes the picker, emitting only the informational toast (no PATCH)
	same, cmd := m.selectMemberRole()
	if same.picking {
		t.Error("selecting a role should close the picker")
	}
	if cmd == nil {
		t.Error("selecting the same role should still surface a 'no change' toast")
	}
}

// Kick opens a confirmation pinned to that member. Accepting fires the remove, cancelling does not.
func TestKickConfirmFlow(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m, _ = m.openMemberKickConfirm()
	if !m.memberConfirming || m.memberActionID != 1 {
		t.Fatalf("kick should open a confirmation pinned to member 1: confirming=%v id=%d", m.memberConfirming, m.memberActionID)
	}
	if !m.CapturingInput() {
		t.Error("an open kick confirmation should capture input")
	}
	accepted, cmd := m.Update(widgets.ConfirmAcceptedMsg{})
	if accepted.memberConfirming || cmd == nil {
		t.Errorf("accepting should close the dialog and fire the remove: confirming=%v cmd=%v", accepted.memberConfirming, cmd != nil)
	}
	cancelled, cmd := m.Update(widgets.ConfirmCancelledMsg{})
	if cancelled.memberConfirming || cmd != nil {
		t.Errorf("cancelling should close the dialog without a command: confirming=%v cmd=%v", cancelled.memberConfirming, cmd != nil)
	}
}

// A membership change reloads roster and stats, a role change only the roster. Selection is pinned.
func TestMemberActionDoneReloads(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m.selMemberID = 2
	m.memberStatsGen = 3
	add, cmd := m.onMemberActionDone(memberActionDoneMsg{action: "add", count: 2})
	if !add.membersRequested || !add.memberStatsRequested || add.memberStatsGen != 4 || cmd == nil {
		t.Errorf("add should reload roster+stats: members=%v stats=%v gen=%d cmd=%v",
			add.membersRequested, add.memberStatsRequested, add.memberStatsGen, cmd != nil)
	}
	if add.memberRestoreID != 2 {
		t.Errorf("the current selection should be pinned for restore, got %d", add.memberRestoreID)
	}

	role := membersTabModel(t, memberList(2))
	role.memberStatsGen = 3
	role, cmd = role.onMemberActionDone(memberActionDoneMsg{action: "role", count: 1})
	if !role.membersRequested || cmd == nil {
		t.Error("role change should reload the roster")
	}
	if role.memberStatsRequested || role.memberStatsGen != 3 {
		t.Errorf("role change should not touch the stats: requested=%v gen=%d", role.memberStatsRequested, role.memberStatsGen)
	}
}

// A failed action surfaces a toast and does not trigger a reload.
func TestMemberActionDoneError(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m.membersRequested = false
	m, cmd := m.onMemberActionDone(memberActionDoneMsg{action: "kick", err: true, status: http.StatusForbidden})
	if cmd == nil {
		t.Error("a failed action should surface a toast")
	}
	if m.membersRequested {
		t.Error("a failed action should not trigger a roster reload")
	}
}

// After a kick shrinks the roster below the cursor, the reload clamps the cursor back into range.
func TestKickReloadClampsCursor(t *testing.T) {
	m := membersTabModel(t, memberList(3))
	m.memberCursor = 2 // on the last member
	m, _ = m.Update(membersLoadedMsg{members: memberList(1)})
	if m.memberCursor != 0 {
		t.Errorf("cursor should clamp into the shrunk roster, got %d", m.memberCursor)
	}
}

// A fresh roster prunes departed members' cached work, so re-adding that id cannot show pre-kick issues.
func TestReloadPrunesDepartedMemberWork(t *testing.T) {
	m := membersTabModel(t, memberList(3))
	m.memberWork[3] = memberWorkload{assigned: domain.IssuePage{TotalElements: 3}}
	m.memberWorkFailed[3] = true
	m.memberWork[2] = memberWorkload{assigned: domain.IssuePage{TotalElements: 5}}
	m, _ = m.Update(membersLoadedMsg{members: memberList(2)}) // roster [1,2] - member 3 is gone
	if _, ok := m.memberWork[3]; ok {
		t.Error("a departed member's cached work should be pruned on reload")
	}
	if m.memberWorkFailed[3] {
		t.Error("a departed member's failed flag should be pruned on reload")
	}
	if m.memberWork[2].assigned.TotalElements != 5 {
		t.Error("a member still in the roster should keep their cached work")
	}
}

// Cache eviction is deferred to the reload: if it fails, the kept member shows stale issues rather than
// a stuck spinner.
func TestKickReloadFailureKeepsWork(t *testing.T) {
	m := membersTabModel(t, memberList(3))
	m.memberCursor = 2
	m.selMemberID = 3
	m.memberActionID = 3
	m.memberWork[3] = memberWorkload{assigned: domain.IssuePage{TotalElements: 2}}
	m, _ = m.onMemberActionDone(memberActionDoneMsg{action: "kick", count: 1})
	if _, ok := m.memberWork[3]; !ok {
		t.Fatal("kick should not eagerly evict — eviction is deferred to a successful reload")
	}
	m, _ = m.Update(membersLoadedMsg{err: true}) // reload failed: roster kept, cache kept
	if _, ok := m.memberWork[3]; !ok {
		t.Error("a failed post-kick reload should keep the cached work so the panel is not a stuck spinner")
	}
}

// A failed reload drops the restore id, so it cannot yank the highlight during a later reload.
func TestReloadFailureClearsRestoreID(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m.memberRestoreID = 2
	m, _ = m.Update(membersLoadedMsg{err: true})
	if m.memberRestoreID != 0 {
		t.Errorf("a failed refresh should drop the restore id, got %d", m.memberRestoreID)
	}
}

// Reload re-selects by id, so a reordered roster cannot drift the highlight to another member.
func TestMemberReloadRestoresSelectionByID(t *testing.T) {
	m := membersTabModel(t, memberList(3))
	m.memberCursor = 1
	m.selMemberID = 2
	m.memberRestoreID = 2
	reordered := []domain.ProjectMember{{MemberID: 3, Username: "c"}, {MemberID: 1, Username: "a"}, {MemberID: 2, Username: "b"}}
	m, _ = m.Update(membersLoadedMsg{members: reordered})
	if m.memberCursor != 2 {
		t.Errorf("cursor should follow member 2 to its new index 2, got %d", m.memberCursor)
	}
	if m.memberRestoreID != 0 {
		t.Error("the restore id should be consumed once applied")
	}
}

// A failed refresh keeps the on-screen roster, not an error banner contradicting the success toast.
func TestMembersReloadFailureKeepsRoster(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	m, cmd := m.Update(membersLoadedMsg{err: true})
	if len(m.members) != 2 {
		t.Errorf("a failed refresh should keep the shown roster, got %d members", len(m.members))
	}
	if m.membersErr {
		t.Error("a failed refresh with a roster in hand should not raise the error banner")
	}
	if cmd == nil {
		t.Error("a failed refresh should still note the hiccup with a toast")
	}
}

// A 403 on the manager-gated candidate load shows the manager-role hint, not the generic error.
func TestAddMemberForbiddenClearsGuard(t *testing.T) {
	m := membersTabModel(t, memberList(1))
	m, _ = m.Update(press("a"))
	m, cmd := m.Update(memberCandidatesLoadedMsg{gen: m.memberCandidateGen, err: true, status: http.StatusForbidden})
	if m.picking {
		t.Error("a forbidden candidate load should not open the picker")
	}
	if m.memberCandidateLoading {
		t.Error("the loading guard should clear on a forbidden result")
	}
	if cmd == nil {
		t.Error("a forbidden candidate load should surface a toast")
	}
}

func TestMemberActionErrorText(t *testing.T) {
	cases := []struct {
		action string
		status int
		code   string
		reason string
		want   string
	}{
		{"add", 0, "", "", "Couldn't reach the server - check your connection and try again."},
		{"add", http.StatusForbidden, "", "", "You need the Manager role for that."},
		{"kick", http.StatusForbidden, "", "", "You don't have permission to change this member."},
		{"role", http.StatusForbidden, "", "", "You don't have permission to change this member."},
		{"add", http.StatusConflict, "", "", "They are already a member of this project."},
		{"role", http.StatusConflict, "", "", "Couldn't change the role."},
		{"add", http.StatusBadRequest, "", "", "Couldn't add the member."},
		{"kick", http.StatusBadRequest, "", "", "Couldn't remove the member."},
		// a server reason on the long tail is shown verbatim instead of the generic line
		{"role", http.StatusBadRequest, "", "Role must be MANAGER or MEMBER.", "Role must be MANAGER or MEMBER."},
		// a mapped code (PROJECT_MANAGER_REQUIRED 403) wins with its canonical copy
		{"add", http.StatusForbidden, "PROJECT_MANAGER_REQUIRED", "Requires project manager role", "You need the Manager role for that."},
		// PROJECT_MEMBER_NOT_FOUND in role/kick is about the TARGET, not the acting manager
		{"kick", http.StatusNotFound, "PROJECT_MEMBER_NOT_FOUND", "Project member not found", "That member is no longer in this project."},
		{"role", http.StatusNotFound, "PROJECT_MEMBER_NOT_FOUND", "Project member not found", "That member is no longer in this project."},
	}
	for _, c := range cases {
		if got := memberActionErrorText(c.action, c.status, c.code, c.reason); got != c.want {
			t.Errorf("memberActionErrorText(%q, %d, %q, %q) = %q, want %q", c.action, c.status, c.code, c.reason, got, c.want)
		}
	}
}

func TestMemberActionOkText(t *testing.T) {
	cases := []struct {
		action string
		count  int
		want   string
	}{
		{"add", 1, "Member added."},
		{"add", 3, "3 members added."},
		{"role", 1, "Role updated."},
		{"kick", 1, "Member removed."},
	}
	for _, c := range cases {
		if got := memberActionOkText(c.action, c.count); got != c.want {
			t.Errorf("memberActionOkText(%q, %d) = %q, want %q", c.action, c.count, got, c.want)
		}
	}
}

// The Members footer advertises add always and role/remove only when a member is selected.
func TestMembersHelpKeysAdvertiseActions(t *testing.T) {
	m := membersTabModel(t, memberList(2))
	keys := map[string]bool{}
	for _, b := range m.HelpKeys() {
		keys[b.Help().Key] = true
	}
	for _, k := range []string{"a", "r", "d"} {
		if !keys[k] {
			t.Errorf("Members footer should advertise %q", k)
		}
	}

	empty := membersTabModel(t, nil)
	keys = map[string]bool{}
	for _, b := range empty.HelpKeys() {
		keys[b.Help().Key] = true
	}
	if !keys["a"] {
		t.Error("add should be offered even with an empty roster")
	}
	if keys["r"] || keys["d"] {
		t.Error("role/remove should not be offered when no member is selected")
	}
}
