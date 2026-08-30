package inbox

import (
	"regexp"
	"strings"
	"testing"
	"time"

	tea "charm.land/bubbletea/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/glyph"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/theme"
)

var csi = regexp.MustCompile(`\x1b\[[0-9;]*[A-Za-z]`)

func plain(s string) string { return csi.ReplaceAllString(zone.Scan(s), "") }

func testDeps() deps.Deps {
	return deps.Deps{
		Server: "https://tissue.example.com",
		Styles: theme.New(theme.TokyoNight()),
		Glyphs: glyph.New(glyph.Unicode),
		Mouse:  true,
	}
}

func press(s string) tea.KeyPressMsg {
	if len(s) == 1 {
		return tea.KeyPressMsg{Code: rune(s[0]), Text: s}
	}
	return tea.KeyPressMsg{Text: s}
}

func sample() []domain.Notification {
	now := time.Now()
	return []domain.Notification{
		{
			ID: 1, Type: domain.NotifIssueAssigned, ActorName: "Alice",
			Data: map[string]string{"issueKey": "P-1"},
			Ref:  domain.EntityRef{IssueKey: "P-1", ProjectKey: "P"}, CreatedAt: now,
		},
		{
			ID: 2, Type: domain.NotifIssueMentioned, ActorName: "Bob", IsRead: true,
			Data: map[string]string{"issueKey": "P-2", "content": "hey"},
			Ref:  domain.EntityRef{IssueKey: "P-2", ProjectKey: "P"}, CreatedAt: now,
		},
	}
}

func loaded(items []domain.Notification, hasNext bool, cursor string) Model {
	zone.NewGlobal()
	m := New(testDeps())
	m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 30})
	m, _ = m.Update(NotificationsLoadedMsg{Gen: 0, Page: domain.NotificationPage{
		Items: items, HasNext: hasNext, NextCursor: cursor,
	}})
	return m
}

func TestLoadedShowsList(t *testing.T) {
	m := loaded(sample(), false, "")
	if m.loading {
		t.Error("still loading after load")
	}
	if len(m.items) != 2 {
		t.Fatalf("items = %d, want 2", len(m.items))
	}
	if m.cursor != 0 {
		t.Errorf("cursor = %d, want 0", m.cursor)
	}
	out := plain(m.View())
	if !strings.Contains(out, "Alice assigned P-1 to you") {
		t.Errorf("view missing first headline:\n%s", out)
	}
	if !strings.Contains(out, "Details") {
		t.Error("view missing detail pane")
	}
}

func TestLoadErrorEmpty(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps())
	m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 30})
	m, _ = m.Update(NotificationsLoadedMsg{Gen: 0, Err: true})
	if !m.loadErr {
		t.Fatal("loadErr not set on empty first-load failure")
	}
	if !strings.Contains(plain(m.View()), "Failed to load notifications") {
		t.Error("view does not surface the load error")
	}
}

// A load whose generation was superseded (by a toggle/refresh) is dropped.
func TestStaleLoadDropped(t *testing.T) {
	m := loaded(sample(), false, "")
	m, _ = m.Update(press("u")) // toggle bumps reqGen to 1 and clears the list
	if m.reqGen != 1 {
		t.Fatalf("reqGen = %d, want 1 after toggle", m.reqGen)
	}
	// a late gen-0 load must not repopulate the list
	m, _ = m.Update(NotificationsLoadedMsg{Gen: 0, Page: domain.NotificationPage{Items: sample()}})
	if len(m.items) != 0 {
		t.Errorf("stale load repopulated list: items = %d, want 0", len(m.items))
	}
	// the current gen-1 load applies
	m, _ = m.Update(NotificationsLoadedMsg{Gen: 1, Page: domain.NotificationPage{Items: sample()[:1]}})
	if len(m.items) != 1 {
		t.Errorf("current load ignored: items = %d, want 1", len(m.items))
	}
}

func TestToggleUnreadOnly(t *testing.T) {
	m := loaded(sample(), false, "")
	m, cmd := m.Update(press("u"))
	if !m.unreadOnly {
		t.Error("unreadOnly not set")
	}
	if !m.loading || len(m.items) != 0 {
		t.Errorf("toggle should clear+load: loading=%v items=%d", m.loading, len(m.items))
	}
	if cmd == nil {
		t.Error("toggle should fire a reload command")
	}
	if !strings.Contains(plain(m.View()), "Loading notifications") {
		t.Error("toggle should show the loading skeleton")
	}
}

func TestToggleMentionsOnly(t *testing.T) {
	m := loaded(sample(), false, "")
	m, cmd := m.Update(press("m"))
	if !m.mentionsOnly {
		t.Error("mentionsOnly not set")
	}
	if !m.loading || len(m.items) != 0 {
		t.Errorf("toggle should clear+load: loading=%v items=%d", m.loading, len(m.items))
	}
	if cmd == nil {
		t.Error("toggle should fire a reload command")
	}
}

// The list label reflects the active filters: mentions alone, and both filters stacked.
func TestMentionsFilterLabels(t *testing.T) {
	m := loaded(sample(), false, "")
	m, _ = m.Update(press("m")) // mentionsOnly on, reqGen -> 1, list cleared
	m, _ = m.Update(NotificationsLoadedMsg{Gen: 1, Page: domain.NotificationPage{Items: sample()[1:]}})
	if out := plain(m.View()); !strings.Contains(out, "Mentions") {
		t.Errorf("mentions filter should label the list 'Mentions':\n%s", out)
	}
	m, _ = m.Update(press("u")) // unreadOnly on as well, reqGen -> 2
	m, _ = m.Update(NotificationsLoadedMsg{Gen: 2, Page: domain.NotificationPage{Items: sample()[:1]}})
	if out := plain(m.View()); !strings.Contains(out, "Unread Mentions") {
		t.Errorf("both filters should label the list 'Unread Mentions':\n%s", out)
	}
}

// Enter opens the selected notification: it marks an unread item read (optimistically) and returns a
// batch (mark-read + navigate).
func TestEnterOpensAndMarksRead(t *testing.T) {
	m := loaded(sample(), false, "")
	m, cmd := m.Update(press("enter"))
	if !m.items[0].IsRead {
		t.Error("selected item not optimistically marked read")
	}
	if cmd == nil {
		t.Error("expected a mark-read + navigate command")
	}
}

// Enter on an already-read item still navigates but does not re-toggle its read state.
func TestEnterOnReadItemNavigatesOnly(t *testing.T) {
	m := loaded(sample(), false, "")
	m, _ = m.Update(press("j")) // move to the read item (index 1)
	if m.cursor != 1 {
		t.Fatalf("cursor = %d, want 1", m.cursor)
	}
	m, cmd := m.Update(press("enter"))
	if !m.items[1].IsRead {
		t.Error("already-read item should stay read")
	}
	if cmd == nil {
		t.Error("enter should still navigate to the item's target")
	}
}

// navCmdFor builds the drill-in message per notification kind.
func TestNavCmdFor(t *testing.T) {
	issue := domain.Notification{
		Type: domain.NotifIssueAssigned,
		Ref:  domain.EntityRef{ResourceType: "ISSUE", ProjectKey: "P", IssueKey: "P-1"},
	}
	if msg := runNav(t, navCmdFor(issue)); msg.ProjectKey != "P" || msg.IssueKey != "P-1" || msg.SprintID != 0 {
		t.Errorf("issue nav = %+v, want ProjectKey=P IssueKey=P-1", msg)
	}

	sprint := domain.Notification{
		Type: domain.NotifSprintStarted,
		Ref:  domain.EntityRef{ResourceType: "SPRINT", ProjectKey: "P", ResourceID: 42},
	}
	if msg := runNav(t, navCmdFor(sprint)); msg.ProjectKey != "P" || msg.IssueKey != "" || msg.SprintID != 42 {
		t.Errorf("sprint nav = %+v, want ProjectKey=P SprintID=42", msg)
	}

	role := domain.Notification{
		Type: domain.NotifProjectRoleChanged,
		Ref:  domain.EntityRef{ResourceType: "PROJECT_MEMBER", ProjectKey: "P"},
	}
	if msg := runNav(t, navCmdFor(role)); msg.ProjectKey != "P" || msg.IssueKey != "" || msg.SprintID != 0 {
		t.Errorf("role nav = %+v, want ProjectKey=P only", msg)
	}

	orphan := domain.Notification{Type: domain.NotifIssueDeleted} // no ref at all
	if navCmdFor(orphan) != nil {
		t.Error("a target-less notification should produce no nav command")
	}
}

func runNav(t *testing.T, cmd tea.Cmd) nav.OpenProjectMsg {
	t.Helper()
	if cmd == nil {
		t.Fatal("nil nav command")
	}
	msg, ok := cmd().(nav.OpenProjectMsg)
	if !ok {
		t.Fatalf("nav command produced %T, want nav.OpenProjectMsg", cmd())
	}
	return msg
}

func TestMarkAllReadOptimistic(t *testing.T) {
	m := loaded(sample(), false, "")
	m, cmd := m.Update(press("a"))
	for i, n := range m.items {
		if !n.IsRead {
			t.Errorf("item %d not marked read", i)
		}
	}
	if cmd == nil {
		t.Error("expected a mark-all-read command")
	}
}

func TestInfiniteScrollPullsNextPage(t *testing.T) {
	m := loaded(sample(), true, "cursor-2")
	m, cmd := m.Update(press("j")) // to index 1 (the last loaded row) → prefetch the next page
	if !m.loadingMore {
		t.Error("reaching the last row with more pages should start a load")
	}
	if cmd == nil {
		t.Error("expected a next-page command")
	}
	// the appended page grows the list and clears the loading-more flag
	m, _ = m.Update(NotificationsLoadedMsg{Gen: 0, Append: true, Page: domain.NotificationPage{
		Items: []domain.Notification{{ID: 3, Type: domain.NotifIssueDeleted, ActorName: "Cara"}},
	}})
	if m.loadingMore {
		t.Error("loadingMore not cleared after append")
	}
	if len(m.items) != 3 {
		t.Errorf("items = %d, want 3 after append", len(m.items))
	}
}

// A refresh yields to an in-flight load rather than racing it.
func TestRefreshYieldsWhileLoading(t *testing.T) {
	zone.NewGlobal()
	m := New(testDeps()) // loading = true, nothing loaded yet
	m, _ = m.Update(tea.WindowSizeMsg{Width: 120, Height: 30})
	_, cmd := m.Update(RefreshMsg{})
	if cmd != nil {
		t.Error("refresh should no-op while a load is in flight")
	}
}

func TestRefreshBumpsGenWhenIdle(t *testing.T) {
	m := loaded(sample(), false, "")
	m, cmd := m.Update(RefreshMsg{})
	if m.reqGen != 1 {
		t.Errorf("reqGen = %d, want 1 after refresh", m.reqGen)
	}
	if cmd == nil {
		t.Error("refresh should fire a reload command")
	}
	// SWR: the current list stays visible until the fresh page lands
	if len(m.items) != 2 {
		t.Errorf("refresh should keep the current list: items = %d, want 2", len(m.items))
	}
}

func TestMarkedAllEmitsToastAndBadge(t *testing.T) {
	m := loaded(sample(), false, "")
	_, cmd := m.Update(MarkedAllMsg{})
	if cmd == nil {
		t.Fatal("MarkedAllMsg should emit a toast + badge-refresh batch")
	}
}

// A refresh fences infinite scroll: it clears pagination so a scroll during the reload window cannot
// append the pre-refresh cursor onto the fresh list, while keeping the current items visible (SWR).
func TestRefreshFencesPagination(t *testing.T) {
	m := loaded(sample(), true, "cursor-2")
	m, cmd := m.Update(RefreshMsg{})
	if m.hasNext || m.nextCursor != "" {
		t.Errorf("refresh should fence pagination: hasNext=%v nextCursor=%q", m.hasNext, m.nextCursor)
	}
	if len(m.items) != 2 {
		t.Errorf("refresh should keep items visible (SWR): items=%d", len(m.items))
	}
	if cmd == nil {
		t.Error("refresh should fire a reload command")
	}
	// with pagination fenced, a scroll-to-bottom cannot start an append
	m, cmd = m.Update(press("j"))
	if m.loadingMore || cmd != nil {
		t.Error("no append should fire while pagination is fenced during a refresh")
	}
}

// A failed mark action reconciles the optimistic write by re-pulling the first page (authoritative read
// flags) and re-checking the badge, not just toasting.
func TestActionFailedReconciles(t *testing.T) {
	m := loaded(sample(), true, "cursor-2")
	m, _ = m.Update(press("enter")) // optimistic mark-read on item 0
	gen := m.reqGen
	m, cmd := m.Update(ActionFailedMsg{Text: "nope"})
	if m.reqGen != gen+1 {
		t.Errorf("reqGen = %d, want %d (a reload supersedes)", m.reqGen, gen+1)
	}
	if m.hasNext {
		t.Error("failed-action reload should fence pagination")
	}
	if cmd == nil {
		t.Fatal("failed action should reload + toast + re-check badge")
	}
}

func TestCapturingInputListFalse(t *testing.T) {
	m := loaded(sample(), false, "")
	if m.CapturingInput() {
		t.Error("the list itself should not capture input")
	}
}

func prefsSample() []domain.NotificationPref {
	return []domain.NotificationPref{
		{Type: domain.NotifIssueAssigned, Channel: domain.ChannelEmail, Enabled: true},
		{Type: domain.NotifIssueMentioned, Channel: domain.ChannelEmail, Enabled: false},
	}
}

func TestOpenPrefsLoadsAndCaptures(t *testing.T) {
	m := loaded(sample(), false, "")
	m, cmd := m.Update(press("p"))
	if !m.prefsOpen || !m.prefsLoading {
		t.Fatalf("prefs not opening: open=%v loading=%v", m.prefsOpen, m.prefsLoading)
	}
	if cmd == nil {
		t.Error("expected a prefs-load command")
	}
	if !m.CapturingInput() {
		t.Error("the open prefs modal should capture input")
	}
}

func TestPrefsLoadAndToggle(t *testing.T) {
	m := loaded(sample(), false, "")
	m, _ = m.Update(press("p"))
	m, _ = m.Update(prefsLoadedMsg{rows: prefsSample()})
	if m.prefsLoading || len(m.prefsRows) != 2 {
		t.Fatalf("prefs not loaded: loading=%v rows=%d", m.prefsLoading, len(m.prefsRows))
	}
	m, cmd := m.Update(press("enter")) // toggle row 0 (enabled -> disabled)
	if m.prefsRows[0].Enabled {
		t.Error("first pref should toggle off")
	}
	if cmd == nil {
		t.Error("toggle should persist to the server")
	}
	if !strings.Contains(plain(m.View()), "Email notifications") {
		t.Error("prefs modal not rendered over the list")
	}
}

func TestPrefsSaveFailedReverts(t *testing.T) {
	m := loaded(sample(), false, "")
	m, _ = m.Update(press("p"))
	m, _ = m.Update(prefsLoadedMsg{rows: prefsSample()})
	m, _ = m.Update(press("enter")) // optimistic off for row 0
	m, cmd := m.Update(prefsSaveFailedMsg{notifType: domain.NotifIssueAssigned, prev: true})
	if !m.prefsRows[0].Enabled {
		t.Error("a rejected save should revert the optimistic toggle")
	}
	if cmd == nil {
		t.Error("expected an error toast")
	}
}

func TestPrefsCloseEsc(t *testing.T) {
	m := loaded(sample(), false, "")
	m, _ = m.Update(press("p"))
	m, _ = m.Update(press("esc"))
	if m.prefsOpen {
		t.Error("esc should close the prefs modal")
	}
}
