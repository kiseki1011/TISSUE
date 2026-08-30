package project

import (
	"strconv"
	"strings"
	"testing"

	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
)

// callSafe runs a command, recovering from the nil-service panics that test deps would cause (loadDetail
// / performTransition hit a nil client), so a batch can be inspected for the toast it also carries.
func callSafe(c tea.Cmd) (msg tea.Msg) {
	defer func() { _ = recover() }()
	if c != nil {
		msg = c()
	}
	return
}

// toastFrom pulls a toast.ShowMsg out of a command, whether it is the command itself or one leg of a batch.
func toastFrom(cmd tea.Cmd) (toast.ShowMsg, bool) {
	if cmd == nil {
		return toast.ShowMsg{}, false
	}
	if ts, ok := callSafe(cmd).(toast.ShowMsg); ok {
		return ts, true
	}
	if batch, ok := cmd().(tea.BatchMsg); ok {
		for _, c := range batch {
			if ts, ok := callSafe(c).(toast.ShowMsg); ok {
				return ts, true
			}
		}
	}
	return toast.ShowMsg{}, false
}

// detailWith opens the detail modal on ENG-1 and loads it with the given transitions.
func detailWith(t *testing.T, trs []domain.IssueTransition) Model {
	t.Helper()
	m := loaded(t, 120, 30, domain.IssuePage{
		Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
	})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{Key: m.viewKey, Title: "X", Transitions: trs}})
	return m
}

func TestTransitionPickerOpens(t *testing.T) {
	m := detailWith(t, []domain.IssueTransition{
		{ID: 1, Label: "Start review", TargetLabel: "In Review", CanExecute: true},
		{ID: 2, Label: "Resolve", TargetLabel: "Done", CanExecute: true},
	})
	m, cmd := m.Update(press("t"))
	if !m.picking {
		t.Fatal("t should open the transition picker")
	}
	if cmd != nil {
		t.Error("opening the picker should not run a command")
	}
	if body := plain(m.View()); !strings.Contains(body, "Move issue") || !strings.Contains(body, "Start review") {
		t.Errorf("picker not rendered over the modal:\n%s", body)
	}
}

func TestTransitionNoneAvailable(t *testing.T) {
	m := detailWith(t, nil)
	m, cmd := m.Update(press("t"))
	if m.picking {
		t.Error("with no transitions the picker should not open")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Info {
		t.Errorf("expected an info toast, got %+v ok=%v", ts, ok)
	}
}

func TestTransitionExecutes(t *testing.T) {
	m := detailWith(t, []domain.IssueTransition{{ID: 7, Label: "Resolve", TargetLabel: "Done", CanExecute: true}})
	m, _ = m.Update(press("t"))
	m, cmd := m.Update(press("enter"))
	if m.picking {
		t.Error("selecting a transition should close the picker")
	}
	if cmd == nil {
		t.Fatal("selecting an executable transition should run it")
	}
}

func TestTransitionBlockedWarns(t *testing.T) {
	m := detailWith(t, []domain.IssueTransition{
		{ID: 1, Label: "Start", TargetLabel: "Doing", CanExecute: true},
		{ID: 2, Label: "Abandon", TargetLabel: "Dropped", CanExecute: false, BlockedReasons: []string{"needs approval"}},
	})
	m, _ = m.Update(press("t"))
	m, _ = m.Update(press("down")) // move onto the blocked transition
	m, cmd := m.Update(press("enter"))
	if !m.picking {
		t.Error("selecting a blocked transition should keep the picker open")
	}
	ts, ok := toastFrom(cmd)
	if !ok || ts.Level != toast.Warning || !strings.Contains(ts.Text, "approval") {
		t.Errorf("a blocked transition should warn with its reason, got %+v ok=%v", ts, ok)
	}
}

func TestTransitionDoneRefetches(t *testing.T) {
	m := detailWith(t, []domain.IssueTransition{{ID: 1, Label: "x", CanExecute: true}})
	m, cmd := m.Update(TransitionDoneMsg{key: m.viewKey, target: "Done"})
	if _, cached := m.details[m.viewKey]; !cached {
		t.Error("a transition should keep the cached detail (SWR) so the modal does not flash a skeleton")
	}
	if !m.detailsPending[m.viewKey] {
		t.Error("a background refetch should be pending after a transition")
	}
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Success || !strings.Contains(ts.Text, "Done") {
		t.Errorf("expected a success toast naming the target, got %+v ok=%v", ts, ok)
	}
}

// Selecting a transition updates the cached detail (and its row) at once, before the server confirms,
// so the modal never flashes a skeleton.
func TestTransitionIsOptimistic(t *testing.T) {
	m := detailWith(t, []domain.IssueTransition{
		{ID: 1, Label: "Resolve", TargetLabel: "Done", TargetCategory: "COMPLETED", CanExecute: true},
	})
	m, _ = m.Update(press("t"))
	m, _ = m.Update(press("enter"))
	d := m.details[m.viewKey]
	if d.StateLabel != "Done" || d.StateCategory != "COMPLETED" {
		t.Errorf("transition should optimistically update the state, got %q/%q", d.StateLabel, d.StateCategory)
	}
	if len(d.Transitions) != 0 {
		t.Error("the stale transitions should be cleared until the refetch refills them")
	}
	if m.issues[0].StateCategory != "COMPLETED" {
		t.Errorf("the list row should be patched too, got %q", m.issues[0].StateCategory)
	}
}

func TestTransitionDoneError(t *testing.T) {
	m := detailWith(t, nil)
	m, cmd := m.Update(TransitionDoneMsg{key: m.viewKey, err: true})
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Error {
		t.Errorf("expected an error toast, got %+v ok=%v", ts, ok)
	}
}

// A failed transition evicts the optimistic state so a state the server never entered is not left on screen.
func TestTransitionErrorEvictsOptimistic(t *testing.T) {
	m := loaded(t, 120, 30, domain.IssuePage{Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: m.viewKey, gen: m.detailGen[m.viewKey], detail: domain.IssueDetail{
		Key: m.viewKey, StateLabel: "In Progress", StateCategory: "ACTIVE",
		Transitions: []domain.IssueTransition{{ID: 1, Label: "Resolve", TargetLabel: "Done", TargetCategory: "COMPLETED", CanExecute: true}},
	}})
	m, _ = m.Update(press("t"))
	m, _ = m.Update(press("enter")) // optimistic: Done
	if m.details[m.viewKey].StateLabel != "Done" {
		t.Fatal("precondition: the optimistic state should be Done")
	}
	m, _ = m.Update(TransitionDoneMsg{key: m.viewKey, err: true})
	if _, cached := m.details[m.viewKey]; cached {
		t.Error("a failed transition should evict the optimistic state (never persist a state the server rejected)")
	}
}

// A freshly loaded detail patches its list row so a state/assignee change shows without a full reload.
func TestPatchRowFromDetail(t *testing.T) {
	m := loaded(t, 120, 24, domain.IssuePage{Issues: []domain.IssueSummary{
		{Key: "ENG-1", Title: "A", StateLabel: "Open", StateCategory: "INITIAL", Priority: "P3"},
	}, TotalElements: 1})
	m, _ = m.Update(press("enter"))
	m, _ = m.Update(IssueDetailLoadedMsg{key: "ENG-1", gen: m.detailGen["ENG-1"], detail: domain.IssueDetail{
		Key: "ENG-1", StateLabel: "In Progress", StateCategory: "ACTIVE", Priority: "P1", AssigneeName: "Kim",
	}})
	got := m.issues[0]
	if got.StateLabel != "In Progress" || got.StateCategory != "ACTIVE" || got.Priority != "P1" || got.AssigneeName != "Kim" || !got.Assigned {
		t.Errorf("row not patched from detail: %+v", got)
	}
}

// The picker spells out every guard a blocked transition fails, so the user reads what is missing at a
// glance instead of selecting each blocked move to be told one reason at a time.
func TestTransitionPickerListsEveryBlockingCondition(t *testing.T) {
	m := detailWith(t, []domain.IssueTransition{
		{ID: 1, Label: "Start", TargetLabel: "In Progress", CanExecute: true},
		{ID: 2, Label: "Resolve", TargetLabel: "Done", CanExecute: false, BlockedReasons: []string{
			"An assignee is required.", "Blocked by unresolved issues: ENG-4",
		}},
	})
	m, _ = m.Update(press("t"))

	body := plain(m.View())
	for _, want := range []string{"An assignee is required.", "Blocked by unresolved issues: ENG-4"} {
		if !strings.Contains(body, want) {
			t.Errorf("the picker should list the blocking condition %q:\n%s", want, body)
		}
	}
	lines := strings.Split(body, "\n")
	move, cond := lineIndex(lines, "Resolve → Done"), lineIndex(lines, "An assignee is required.")
	if move < 0 || cond != move+1 {
		t.Errorf("a condition should sit under its own transition (move=%d cond=%d):\n%s", move, cond, body)
	}
}

// An executable transition carries no conditions: its guards passed, and the server does not report the
// ones it silently satisfied - so a clean row must not gain a phantom warning line.
func TestTransitionPickerLeavesExecutableMovesUnannotated(t *testing.T) {
	if notes := blockedNotes(domain.IssueTransition{
		ID: 1, Label: "Start", CanExecute: true,
		BlockedReasons: []string{"stale reason"},
	}); notes != nil {
		t.Errorf("an executable transition should carry no conditions, got %v", notes)
	}
}

// A transition the server reports as blocked without saying why still says so, rather than rendering a
// bare "(blocked)" with nothing under it.
func TestTransitionBlockedWithoutAReasonStillExplains(t *testing.T) {
	m := detailWith(t, []domain.IssueTransition{
		{ID: 1, Label: "Resolve", TargetLabel: "Done", CanExecute: false},
	})
	m, _ = m.Update(press("t"))
	if body := plain(m.View()); !strings.Contains(body, "This transition is blocked.") {
		t.Errorf("a reasonless block should still be explained:\n%s", body)
	}

	_, cmd := m.Update(press("enter"))
	if ts, ok := toastFrom(cmd); !ok || ts.Level != toast.Warning || !strings.Contains(ts.Text, "blocked") {
		t.Errorf("selecting it should still warn, got %+v ok=%v", ts, ok)
	}
}

// lineIndex is the first rendered line containing sub, or -1.
func lineIndex(lines []string, sub string) int {
	for i, l := range lines {
		if strings.Contains(l, sub) {
			return i
		}
	}
	return -1
}

// The picker is floated over the screen, not scrolled inside it, so whatever falls past the frame is
// simply never drawn. Guard conditions make the box tall, so its budget follows the terminal.
func TestTransitionPickerFitsTheFrame(t *testing.T) {
	var trs []domain.IssueTransition
	for i := 1; i <= 6; i++ {
		n := strconv.Itoa(i)
		trs = append(trs, domain.IssueTransition{
			ID: int64(i), Label: "Move" + n, TargetLabel: "State" + n,
			BlockedReasons: []string{
				"Reason A" + n + ": blocked by unresolved issues [ENG-4, ENG-9, ENG-12, ENG-31]",
				"Reason B" + n + ": an assignee is required before this can be resolved.",
			},
		})
	}
	for _, height := range []int{10, 20, 40} {
		m := loaded(t, 120, height, domain.IssuePage{
			Issues: []domain.IssueSummary{{Key: "ENG-1", Title: "X", StateCategory: "ACTIVE"}}, TotalElements: 1,
		})
		m, _ = m.Update(press("enter"))
		m, _ = m.Update(IssueDetailLoadedMsg{
			key: m.viewKey, gen: m.detailGen[m.viewKey],
			detail: domain.IssueDetail{Key: m.viewKey, Title: "X", Transitions: trs},
		})
		m, _ = m.Update(press("t"))

		if got := lipgloss.Height(m.picker.View(m.deps.Styles)); got > height {
			t.Errorf("height %d: the picker is %d rows, so %d would be clipped away unseen", height, got, got-height)
		}
	}
}
