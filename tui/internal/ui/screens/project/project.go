// Package project is the project drill-in screen: the issue list for one project.
package project

import (
	"context"
	"strings"
	"time"

	"charm.land/bubbles/v2/key"
	"charm.land/bubbles/v2/textinput"
	tea "charm.land/bubbletea/v2"
	lipgloss "charm.land/lipgloss/v2"
	zone "github.com/lrstanley/bubblezone/v2"

	"github.com/kiseki1011/TISSUE/tui/internal/domain"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/deps"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/nav"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/toast"
	"github.com/kiseki1011/TISSUE/tui/internal/ui/widgets"
)

const (
	minHeight      = 10
	pageSize       = 50
	hInset         = 2                      // blank columns between the content and each terminal edge
	searchRowH     = 3                      // top border, input, bottom border
	searchDebounce = 300 * time.Millisecond // wait for typing to settle before hitting the server

	prefetchDebounce = 150 * time.Millisecond // wait for the cursor to settle before warming neighbor details
	prefetchSpan     = 2                      // rows on each side of the cursor to prefetch
)

type projectFocus int

// focusList is the zero value so a fresh model opens on the list.
const (
	focusList projectFocus = iota
	focusSearch
	focusFilter
	focusDetail
	focusCount
)

type projectTab int

const (
	tabIssues projectTab = iota
	tabSprints
	tabStats
	tabMembers
	tabConfig
)

// pickKind is which picker is open over the detail modal, so its selection is routed correctly.
type pickKind int

const (
	pickTransition pickKind = iota
	pickAssignee
	pickParent       // choosing a parent issue in the create form
	pickReviewers    // multi-select of the issue's reviewers
	pickParentEdit   // changing an existing issue's parent from the Details panel
	pickRelationType // step 1 of adding a relation
	pickRelationTarget
	pickRelationRemove    // choosing which of the viewed issue's relations to unlink
	pickSprintRemoveIssue // choosing which of a sprint's issues to remove from it (Sprints tab)
	pickAddMember         // multi-select of candidates to add to the project (Members tab)
	pickMemberRole        // choosing a member's new project role (Members tab)
)

var projectTabs = []struct {
	tab   projectTab
	label string
	zone  string
}{
	{tabIssues, "Issues", "project.tab.issues"},
	{tabSprints, "Sprints", "project.tab.sprints"},
	{tabStats, "Stats", "project.tab.stats"},
	{tabMembers, "Members", "project.tab.members"},
	{tabConfig, "Config", "project.tab.config"},
}

type Model struct {
	deps       deps.Deps
	width      int
	height     int
	viewer     string // the caller's username, for spotting their own comments. "" until the profile lands
	projectKey string
	title      string
	tab        projectTab
	filter     domain.IssueFilter
	focus      projectFocus
	search     textinput.Model
	searchSeq  int // bumped per keystroke so only the last one's debounce actually searches

	filtering   bool
	filterUI    filterForm
	modalScroll int // wheel offset for a filter modal that overflows a short terminal

	types       []domain.IssueTypeSummary // the global issue-type catalog, for the filter modal
	typesLoaded bool

	viewKey      string // the issue shown in the Details panel (the cursor's issue)
	detailScroll int    // first visible line of the Details panel's windowed body

	showActivity   bool // the Activity view is toggled on (a 3rd column when wide, else swapped for Details)
	activityScroll int  // first visible line of the Activity view's windowed body

	picking           bool // a picker (transition or assignee) is open over the detail modal
	pickKind          pickKind
	picker            widgets.ListPicker
	pickerTransitions []domain.IssueTransition // the picker's transitions, for canExecute/reason lookup
	reviewerBase      []domain.Reviewer        // the issue's reviewers when the reviewers picker opened, to diff the confirm against
	relPendingType    domain.RelationType      // the relation type chosen in step 1, applied when the target is picked in step 2
	relSource         string                   // the issue the add-relation flow started from, so a cursor move cannot redirect it
	relGen            int                      // bumped per relation-target candidate load, so a superseded/stale result is dropped

	dating       bool // a calendar picker is open over the create/edit form (setting a due date or a DATE/TIMESTAMP field)
	datePick     widgets.DatePicker
	dateTarget   dateTarget // which field the confirmed pick fills
	dateCustomIx int        // the custom-field index when dateTarget == dateCustom

	editing    bool // the edit form is open over the detail modal
	editUI     editForm
	editScroll int                // window offset for an edit form taller than the terminal
	editBase   domain.IssueDetail // the detail the form was built from, so the save diffs against the open-time snapshot

	commentDeleting bool // a delete confirmation floats over the comment modal
	commentDeleteUI widgets.ConfirmForm
	commentDeleteID int64 // the comment the confirmation guards, pinned so a later focus move cannot redirect it

	editingContent bool // the standalone content editor is open over the detail modal
	contentUI      contentForm
	contentScroll  int // window offset for a content editor taller than the terminal

	reviewing    bool // the review modal (submit a verdict / ask reviewers again) is open
	reviewUI     reviewForm
	reviewScroll int    // window offset for a review modal taller than the terminal
	contentBase  string // the content the editor was built from, so the save diffs against the open-time snapshot

	commenting    bool // the comment composer is open over the detail modal
	commentUI     commentForm
	commentScroll int // window offset for a composer taller than the terminal

	creating      bool // the new-issue form is open
	createUI      createForm
	createScroll  int // window offset for a create form taller than the terminal
	parentGen     int // bumped per parent-candidate request, so a late/stale load is dropped
	parentEditGen int // bumped per Details parent-edit picker request, so a superseded candidate load is dropped

	typeFields    map[int64][]domain.IssueField // custom-field definitions cached per issue type (for the create form)
	typeFieldsGen int                           // bumped per type-fields request so a superseded/stale load is dropped

	deleting  bool // the delete confirmation is open over the detail modal
	deleteUI  widgets.ConfirmForm
	deleteKey string // the issue the confirmation is guarding

	peeking    bool   // a read-only "peek" modal for a linked issue (parent/child/relation) is open
	peekKey    string // the issue being peeked, keyed into the same detail cache as the main view
	peekScroll int    // window offset for a peek taller than the terminal

	members       []domain.ProjectMember // active members, for the assignee picker
	membersLoaded bool
	// issue details cached by key (SWR): a re-open shows the cached copy at once
	details        map[string]domain.IssueDetail
	detailsPending map[string]bool
	detailsFailed  map[string]bool
	detailGen      map[string]int // per-key load generation, so a superseded detail load's late result is dropped

	// issue activity cached by key (SWR), mirroring the detail cache
	activities        map[string]domain.IssueActivityPage
	activitiesPending map[string]bool
	activitiesFailed  map[string]bool
	activityGen       map[string]int

	issues          []domain.IssueSummary
	cursor          int
	page            domain.IssuePage // metadata of the most recently loaded page (hasNext/totals)
	loading         bool             // first page in flight
	loadingMore     bool             // a subsequent page in flight
	loadErr         bool
	morePagesLoaded bool   // a load-more has appended beyond the first page (a live-reload would strand those)
	reqGen          int    // bumped on reload so a superseded in-flight load is ignored when it lands
	prefetchSeq     int    // bumped when the selection changes so only the last cursor stop's debounce prefetches
	hover           string // zone id of the row/button under the cursor, "" when none

	// realtime (SSE): rtRestore{Key,Gen} preserve the selection across a debounced silent reload.
	rtReloadSeq       int
	rtRestoreKey      string
	rtRestoreGen      int
	rtSprintReloadSeq int // debounce sequence for the silent sprint-list reload on a lifecycle SSE event

	// Sprints tab: the sprint list plus, for the cursor's sprint, its detail and issues. Lazy-loaded.
	sprints            []domain.SprintSummary
	sprintCursor       int
	sprintPage         domain.SprintPage
	sprintsRequested   bool // the lazy load has been kicked off, so re-entering the tab does not refetch
	sprintsLoading     bool
	sprintsErr         bool
	sprintReqGen       int   // bumped per sprint-list reload so a superseded load's late result is ignored
	selSprintID        int64 // the sprint shown on the right (the cursor's sprint). 0 when none
	sprintDetailScroll int   // first visible line of the sprint detail panel (top right)
	sprintIssueScroll  int   // first visible line of the sprint issues panel (bottom right)

	// issues belonging to a sprint, cached by sprint id (SWR): cached rows show while a refetch runs.
	sprintIssues        map[int64]domain.IssuePage
	sprintIssuesPending map[int64]bool
	sprintIssuesFailed  map[int64]bool
	sprintIssuesGen     map[int64]int
	// per-sprint report cache, loaded in lockstep with that sprint's issues so the summary stays in sync
	sprintReport        map[int64]domain.SprintReport
	sprintReportPending map[int64]bool
	sprintReportFailed  map[int64]bool
	sprintReportGen     map[int64]int

	// sprintActionID pins an open action modal's target. sprintRestoreID re-points the cursor after reload.
	sprintConfirming  bool // a yes/no sprint action confirmation (complete or cancel) is open
	sprintConfirmUI   widgets.ConfirmForm
	sprintConfirmKind string // which action the confirmation guards: "complete" | "cancel" | "delete" | "migrate"
	sprintEditing     bool
	sprintCreating    bool // the sprint modal is adding a sprint rather than editing the selected one
	sprintEditUI      sprintEditForm
	sprintEditScroll  int
	sprintEditBase    domain.SprintSummary // the sprint the edit form was built from, to diff the save against
	sprintActionID    int64
	sprintRestoreID   int64

	// the current (ACTIVE) sprint, prefetched for "add to sprint". currentSprintGen drops a late prefetch.
	currentSprint    *domain.SprintSummary
	currentSprintGen int

	// migrate ("m") acts on the current sprint and the next PLANNING one, not the highlighted row.
	migrateSourceID   int64
	migrateTargetID   int64
	migrateSourceName string
	migrateTargetName string
	migrateKeys       []string // the incomplete issue keys shown in the confirmation, sent with the request
	migrateGen        int
	migrateLoading    bool

	// Members tab: the roster reuses m.members. membersRequested starts true (Init prefetches).
	membersRequested   bool
	membersErr         bool
	memberCursor       int
	selMemberID        int64
	memberDetailScroll int
	memberWork         map[int64]memberWorkload
	memberWorkPending  map[int64]bool
	memberWorkFailed   map[int64]bool
	memberWorkGen      map[int64]int

	// per-member contribution stats, one batch load keyed by member id. An absent member renders as zeros.
	memberStats          map[int64]domain.MemberStats
	memberStatsRequested bool
	memberStatsLoaded    bool
	memberStatsErr       bool
	memberStatsGen       int

	// member actions: add (a), role (r), kick (d). memberActionID pins the target against a roster reload.
	memberCandidateGen     int
	memberCandidateLoading bool
	memberActionID         int64
	memberActionName       string
	memberRestoreID        int64 // re-select this member after a post-action reload, so a reorder cannot drift the highlight
	memberConfirming       bool  // the kick confirmation is open
	memberConfirmUI        widgets.ConfirmForm

	// Config tab: the project's editable settings, lazy-loaded on first open and refreshed after an edit.
	project            domain.Project
	configRequested    bool
	configLoading      bool
	configLoaded       bool
	configErr          bool
	configReqGen       int
	configDetailScroll int
	configEditing      bool
	configEditUI       configEditForm
	configEditScroll   int
	configEditBase     domain.Project // the project the edit form was built from, to diff the save against
	configConfirming   bool           // the archive/disconnect confirmation is open
	configConfirmUI    widgets.ConfirmForm
	githubConfirming   bool                     // the open confirm is a GitHub disconnect (not an archive)
	github             domain.GithubIntegration // GitHub webhook integration status, loaded with the config
	githubLoaded       bool
	githubSecret       domain.GithubSecret      // a just-revealed webhook secret (shown once). Secret=="" means none
	githubSecretCopied bool                     // that secret has been copied, so the reveal can say so
	githubURLCopied    bool                     // the webhook URL has been copied, likewise
	deliveries         []domain.WebhookDelivery // recent inbound webhooks and how each was handled
	deliveriesLoaded   bool                     // false when unreadable (not a manager) or not connected

	// Stats tab: refreshed on every open. statsLoaded separates the first-load skeleton from a refresh.
	stats        domain.ProjectStats
	statsLoading bool
	statsLoaded  bool
	statsErr     bool
	statsReqGen  int // bumped per stats fetch so a superseded/stale load's late result is dropped
	statsScroll  int // first visible line of the overview when it overflows a short terminal
	// advanced stats: a per-section OK flag (separate endpoints), so one failure hides only that section.
	aging    domain.AgingStats
	agingOK  bool
	cycle    domain.CycleTimeStats
	cycleOK  bool
	flow     domain.FlowStats
	flowOK   bool
	velocity domain.Velocity
	velocOK  bool

	// one-shot entry focus from the Inbox. Consumed on apply, so a resize cannot re-trigger it.
	pendingIssue    string
	pendingSprintID int64
	navSprintID     int64
}

// WithInitialFocus records a first-layout focus: peek issueKey, else land on sprintID's tab.
func (m Model) WithInitialFocus(issueKey string, sprintID int64) Model {
	m.pendingIssue = issueKey
	if issueKey == "" {
		m.pendingSprintID = sprintID
	}
	return m
}

// applyPendingFocus consumes the one-shot entry focus, clearing it so a resize cannot re-trigger.
func (m Model) applyPendingFocus() (Model, tea.Cmd) {
	if key := m.pendingIssue; key != "" {
		m.pendingIssue, m.pendingSprintID = "", 0
		return m.openPeek(key)
	}
	if id := m.pendingSprintID; id != 0 {
		m.pendingIssue, m.pendingSprintID = "", 0
		m.navSprintID = id // the sprint-list load selects this sprint (and loads its issues) on landing
		return m.selectTab(tabSprints)
	}
	return m, nil
}

// WithViewer records the caller's username. The profile arrives after New, so it is set separately.
func (m Model) WithViewer(username string) Model {
	m.viewer = username
	return m
}

func New(d deps.Deps, projectKey, title string) Model {
	search := textinput.New()
	search.Prompt = ""
	search.Placeholder = ""
	return Model{
		deps: d, projectKey: projectKey, title: title, loading: true,
		membersRequested: true,                                         // Init prefetches the roster, so guard against a duplicate load
		filter:           initialFilter(d, projectKey), search: search, // restore the last-applied filter for this project
		details:             map[string]domain.IssueDetail{},
		detailsPending:      map[string]bool{},
		detailsFailed:       map[string]bool{},
		detailGen:           map[string]int{},
		activities:          map[string]domain.IssueActivityPage{},
		activitiesPending:   map[string]bool{},
		activitiesFailed:    map[string]bool{},
		activityGen:         map[string]int{},
		sprintIssues:        map[int64]domain.IssuePage{},
		sprintIssuesPending: map[int64]bool{},
		sprintIssuesFailed:  map[int64]bool{},
		sprintIssuesGen:     map[int64]int{},
		sprintReport:        map[int64]domain.SprintReport{},
		sprintReportPending: map[int64]bool{},
		sprintReportFailed:  map[int64]bool{},
		sprintReportGen:     map[int64]int{},
		memberWork:          map[int64]memberWorkload{},
		memberWorkPending:   map[int64]bool{},
		memberWorkFailed:    map[int64]bool{},
		memberWorkGen:       map[int64]int{},
		memberStats:         map[int64]domain.MemberStats{},
	}
}

func (m Model) Init() tea.Cmd {
	return tea.Batch(
		loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false),
		loadIssueTypes(m.deps),                                      // prefetch the catalog so the filter modal opens ready
		loadMembers(m.deps, m.projectKey),                           // prefetch members so the assignee picker opens ready
		loadCurrentSprint(m.deps, m.projectKey, m.currentSprintGen), // prefetch the active sprint so "add to sprint" knows its target
	)
}

func (m Model) Retheme(d deps.Deps) Model {
	m.deps = d
	// turning the mouse off can strand focus on the now-hidden filter button, so snap back to the list.
	if !m.focusAvailable(m.focus) {
		m.focus = focusList
		m.search.Blur()
	}
	return m
}

func (m Model) CapturingInput() bool {
	return m.focus == focusSearch || m.focus == focusDetail || m.filtering ||
		m.editing || m.editingContent || m.commenting || m.deleting || m.picking || m.creating || m.dating ||
		m.reviewing ||
		m.sprintEditing || m.sprintConfirming || m.memberConfirming || m.configEditing || m.configConfirming
}

func (m Model) Update(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case tea.WindowSizeMsg:
		m.width, m.height = msg.Width, msg.Height
		m.search.SetWidth(m.searchInputWidth())
		// the panels reflow, so re-clamp - a stale offset eats the first scroll key as a dead press
		m.detailScroll = clampScroll(m.detailScroll, m.detailScrollMax())
		m.activityScroll = clampScroll(m.activityScroll, m.activityScrollMax())
		if m.peeking {
			m.peekScroll = clampScroll(m.peekScroll, m.peekScrollMax())
		}
		m.sprintDetailScroll = clampScroll(m.sprintDetailScroll, m.sprintDetailScrollMax())
		m.sprintIssueScroll = clampScroll(m.sprintIssueScroll, m.sprintIssueScrollMax())
		m.memberDetailScroll = clampScroll(m.memberDetailScroll, m.memberDetailScrollMax())
		m.configDetailScroll = clampScroll(m.configDetailScroll, m.configScrollMax())
		m.statsScroll = clampScroll(m.statsScroll, m.statsScrollMax())
		return m.applyPendingFocus()

	case searchDebounceMsg:
		// only the most recent keystroke's timer actually searches, so fast typing hits the server once
		if msg.seq != m.searchSeq {
			return m, nil
		}
		keyword := strings.TrimSpace(m.search.Value())
		if keyword == m.filter.Keyword {
			return m, nil
		}
		m.filter.Keyword = keyword
		m.loading, m.loadErr, m.loadingMore = true, false, false
		m.reqGen++
		return m, loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false)

	case prefetchDebounceMsg:
		// only the last cursor stop's timer prefetches
		if msg.seq != m.prefetchSeq {
			return m, nil
		}
		return m.prefetchNeighbors()

	case issuesLoadedMsg:
		if msg.key != m.projectKey || msg.gen != m.reqGen {
			return m, nil // a stale load from a previous project or a superseded reload
		}
		m.loading, m.loadingMore = false, false
		if msg.err {
			// an append failure keeps the loaded pages and leaves page.HasNext set, so scrolling retries.
			if !msg.append {
				m.loadErr = true
			}
			return m, nil
		}
		m.loadErr = false
		m.page = msg.page
		if msg.append {
			m.issues = append(m.issues, msg.page.Issues...)
			m.morePagesLoaded = true
		} else {
			m.issues = msg.page.Issues
			m.morePagesLoaded = false
			m.cursor = 0
			// a silent realtime reload restores the CURRENT selection, including mid-flight navigation.
			if m.rtRestoreKey != "" && msg.gen == m.rtRestoreGen {
				if idx := m.indexOfIssue(m.viewKey); idx >= 0 {
					m.cursor = idx
				}
			}
			m.rtRestoreKey = ""
		}
		if m.cursor >= len(m.issues) {
			m.cursor = max(0, len(m.issues)-1)
		}
		m, sel := m.syncSelection()
		if msg.append {
			// syncSelection no-ops on an unchanged selection, so warm the appended rows here.
			m, pf := m.prefetchNeighbors()
			return m, tea.Batch(sel, pf)
		}
		return m, sel

	case issueTypesLoadedMsg:
		// a failed load still flips typesLoaded so the filter modal stops showing "Loading…"
		m.typesLoaded = true
		if !msg.err {
			m.types = msg.types
		}
		if m.filtering {
			// fill in the Type rows of an already-open modal without dropping in-progress toggles
			m.filterUI = m.filterUI.withTypes(m.types, m.typesLoaded)
		}
		return m, nil

	case IssueDetailLoadedMsg:
		if msg.gen != m.detailGen[msg.key] {
			return m, nil // a superseded detail load (an earlier fetch) landed late
		}
		delete(m.detailsPending, msg.key)
		if msg.err {
			m.detailsFailed[msg.key] = true
			return m, nil
		}
		m.detailsFailed[msg.key] = false
		m.details[msg.key] = msg.detail
		m.patchRow(msg.key, msg.detail)
		if msg.key == m.viewKey {
			// a refetch may return shorter content, so re-clamp - else the next scroll key jumps in one press
			m.detailScroll = clampScroll(m.detailScroll, m.detailScrollMax())
			if m.commenting {
				// a posted comment just landed and grew the thread, so keep the focused composer visible
				m = m.followCommentFocus()
			}
		}
		return m, nil

	case ActivitiesLoadedMsg:
		if msg.gen != m.activityGen[msg.key] {
			return m, nil // a superseded activity load landed late
		}
		delete(m.activitiesPending, msg.key)
		if msg.err {
			m.activitiesFailed[msg.key] = true
			return m, nil
		}
		m.activitiesFailed[msg.key] = false
		m.activities[msg.key] = msg.page
		if msg.key == m.viewKey {
			m.activityScroll = clampScroll(m.activityScroll, m.activityScrollMax())
		}
		return m, nil

	case TransitionDoneMsg:
		// keep the optimistic state on success, evict it on failure - never leave fabricated data on screen.
		if msg.err {
			delete(m.details, msg.key)
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Moved to "+msg.target+"."))

	case membersLoadedMsg:
		m.membersRequested = false
		m.membersLoaded = true
		if msg.err {
			if len(m.members) > 0 {
				// the refresh failed but the roster on screen is still good: a wipe would contradict the success toast
				m.memberRestoreID = 0 // dropped here, so a later reload cannot misfire on it
				return m, toast.Show(toast.Error, "Couldn't refresh members.")
			}
			m.membersErr = true // no roster to fall back on: surface the failure (reopening the tab retries)
			return m, nil
		}
		m.membersErr = false
		m.members = msg.members
		m.pruneMemberWork()
		// keep the highlight on the same member across a post-action reload (the roster is unordered)
		if id := m.memberRestoreID; id != 0 {
			m.memberRestoreID = 0
			for i := range m.members {
				if m.members[i].MemberID == id {
					m.memberCursor = i
					break
				}
			}
		}
		if m.memberCursor >= len(m.members) {
			m.memberCursor = max(0, len(m.members)-1) // a kick may have shrunk the roster past the cursor
		}
		if m.tab == tabMembers {
			return m.syncMemberSelection()
		}
		return m, nil

	case memberWorkLoadedMsg:
		return m.onMemberWorkLoaded(msg)

	case memberCandidatesLoadedMsg:
		return m.onMemberCandidatesLoaded(msg)

	case memberActionDoneMsg:
		return m.onMemberActionDone(msg)

	case configLoadedMsg:
		return m.onConfigLoaded(msg)

	case statsLoadedMsg:
		return m.onStatsLoaded(msg)

	case configActionDoneMsg:
		return m.onConfigActionDone(msg)
	case githubSecretMsg:
		return m.onGithubSecret(msg)
	case githubActionMsg:
		return m.onGithubAction(msg)
	case githubSyncMsg:
		return m.onGithubSync(msg)

	case memberStatsLoadedMsg:
		if msg.gen != m.memberStatsGen {
			return m, nil // a superseded stats load landed late
		}
		if msg.err {
			m.memberStatsErr = true
			return m, nil
		}
		m.memberStatsErr = false
		m.memberStatsLoaded = true
		m.memberStats = map[int64]domain.MemberStats{}
		for _, st := range msg.stats {
			m.memberStats[st.MemberID] = st
		}
		return m, nil

	case AssignDoneMsg:
		// same as transitions: keep the optimistic assignee on success, evict it on failure.
		if msg.err {
			delete(m.details, msg.key)
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		text := "Unassigned."
		if msg.assignee != "" {
			text = "Assigned to " + msg.assignee + "."
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, text))

	case CommentEditDoneMsg:
		return m.onCommentEditDone(msg)

	case commentPageLoadedMsg:
		return m.onCommentPageLoaded(msg)

	case ReviewDoneMsg:
		// keep the modal open on failure. refetch either way: a partial effect must not be guessed at.
		if msg.err {
			m.reviewUI.sending = false
			m.reviewUI.status = msg.errText
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		m.reviewing = false
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, msg.text))

	case ReviewerDoneMsg:
		// refetch either way: a partial diff may have applied before the erroring call.
		if msg.err {
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Reviewers updated."))

	case parentEditCandidatesLoadedMsg:
		return m.onParentEditCandidates(msg)

	case ParentEditDoneMsg:
		// refetch either way: on failure it reverts the optimistic write.
		if msg.err {
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Parent updated."))

	case EditDoneMsg:
		// same as transitions/assigns: keep the optimistic edit on success, evict it on failure.
		if msg.err {
			delete(m.details, msg.key)
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Issue updated."))

	case CommentDoneMsg:
		// a comment is additive, so nothing to roll back. the refetch pulls the thread from the detail BFF.
		if m.commenting {
			return m.onCommentDoneWhileOpen(msg) // stay-open: clear the posted composer, keep the modal up
		}
		if msg.err {
			return m, toast.Show(toast.Error, msg.errText)
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Comment added."))

	case IssueDeletedMsg:
		// the delete dialog owns the in-flight/error state, so route the result there
		return m.updateDelete(msg)

	case IssueCreatedMsg:
		// the form already closed on submit. reload from the top so the new issue appears if it matches.
		if msg.err {
			return m, toast.Show(toast.Error, msg.errText)
		}
		m.loading, m.loadErr, m.loadingMore = true, false, false
		m.reqGen++ // supersede any in-flight load so its late result cannot clobber the reload
		return m, tea.Batch(
			toast.Show(toast.Success, "Created "+msg.key),
			loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false),
		)

	case createParentPickMsg:
		return m.openParentPicker()
	case openDueCreateMsg:
		return m.openDuePicker(dateDueCreate)
	case openDueEditMsg:
		return m.openDuePicker(dateDueEdit)
	case openParentEditFromFormMsg:
		return m.openParentEditPicker()
	case openCustomDateMsg:
		return m.openCustomDatePicker(msg.index)
	case openCustomDateEditMsg:
		return m.openCustomDatePickerEdit(msg.index)
	case parentCandidatesLoadedMsg:
		return m.onParentCandidates(msg)
	case relationCandidatesLoadedMsg:
		return m.onRelationCandidates(msg)
	case relationDoneMsg:
		// nothing optimistic to roll back. the refetch pulls the new relations from the detail BFF.
		if msg.err {
			return m, toast.Show(toast.Error, msg.errText)
		}
		text := "Relation added."
		if msg.removed {
			text = "Relation removed."
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, text))
	case createTypeFieldsMsg:
		return m.requestCustomFields(msg.typeID)
	case typeFieldsLoadedMsg:
		return m.onTypeFieldsLoaded(msg)
	case RealtimeIssueEventMsg:
		return m.onRealtimeIssueEvent(msg)
	case realtimeReloadMsg:
		return m.onRealtimeReload(msg)
	case RealtimeSprintEventMsg:
		return m.onRealtimeSprintEvent(msg)
	case realtimeSprintReloadMsg:
		return m.onRealtimeSprintReload(msg)
	case sprintsLoadedMsg:
		return m.onSprintsLoaded(msg)
	case sprintIssuesLoadedMsg:
		return m.onSprintIssuesLoaded(msg)
	case sprintReportLoadedMsg:
		return m.onSprintReportLoaded(msg)
	case sprintActionDoneMsg:
		return m.onSprintActionDone(msg)
	case sprintIssuesChangedMsg:
		return m.onSprintIssuesChanged(msg)
	case migrateCandidatesLoadedMsg:
		return m.onMigrateCandidatesLoaded(msg)
	case sprintMigrateDoneMsg:
		return m.onSprintMigrateDone(msg)
	case currentSprintLoadedMsg:
		// superseded or failed: keep whatever the sprint list (the authority once loaded) last set
		if msg.gen != m.currentSprintGen || msg.err {
			return m, nil
		}
		m.currentSprint = msg.sprint
		return m, nil
	}

	// an open form/picker owns the keyboard. the calendar and pickers float over it, so they come first.
	if m.peeking {
		return m.updatePeek(msg)
	}
	if m.dating {
		return m.updateDatePicker(msg)
	}
	if m.picking {
		return m.updatePicker(msg)
	}
	if m.creating {
		return m.updateCreate(msg)
	}
	if m.filtering {
		return m.updateFilter(msg)
	}
	if m.editing {
		return m.updateEdit(msg)
	}
	if m.editingContent {
		return m.updateContentEditor(msg)
	}
	if m.reviewing {
		return m.updateReview(msg)
	}
	if m.commenting {
		return m.updateComment(msg)
	}
	if m.deleting {
		return m.updateDelete(msg)
	}
	if m.sprintEditing {
		return m.updateSprintEdit(msg)
	}
	if m.sprintConfirming {
		return m.updateSprintConfirm(msg)
	}
	if m.memberConfirming {
		return m.updateMemberConfirm(msg)
	}
	if m.configEditing {
		return m.updateConfigEdit(msg)
	}
	if m.configConfirming {
		return m.updateConfigConfirm(msg)
	}
	switch msg := msg.(type) {
	case tea.KeyPressMsg:
		return m.onKey(msg)
	case tea.MouseClickMsg:
		return m.onClick(msg)
	case tea.MouseMotionMsg:
		return m.onHover(msg)
	case tea.MouseWheelMsg:
		return m.onWheel(msg)
	}
	return m, nil
}

// onWheel scrolls the panel under the pointer (or the narrow read-only modal), else moves the cursor.
func (m Model) onWheel(msg tea.MouseWheelMsg) (Model, tea.Cmd) {
	if m.tab == tabSprints {
		return m.onSprintWheel(msg)
	}
	if m.tab == tabMembers {
		return m.onMemberWheel(msg)
	}
	if m.tab == tabConfig {
		return m.onConfigWheel(msg)
	}
	if m.tab == tabStats {
		return m.onStatsWheel(msg)
	}
	// the narrow read-only modal has no side zone, so it routes the wheel to the view it is showing.
	if zone.Get(zoneActivity).InBounds(msg) {
		m.activityScroll = wheelClamp(m.activityScroll, msg.Button, m.activityScrollMax())
		return m, nil
	}
	if zone.Get(zoneDetail).InBounds(msg) {
		m.detailScroll = wheelClamp(m.detailScroll, msg.Button, m.detailScrollMax())
		return m, nil
	}
	if m.narrow() && m.focus == focusDetail {
		if m.showActivity {
			m.activityScroll = wheelClamp(m.activityScroll, msg.Button, m.activityScrollMax())
		} else {
			m.detailScroll = wheelClamp(m.detailScroll, msg.Button, m.detailScrollMax())
		}
		return m, nil
	}
	switch msg.Button {
	case tea.MouseWheelUp:
		return m.moveCursor(-1)
	case tea.MouseWheelDown:
		return m.moveCursor(1)
	}
	return m, nil
}

func (m Model) updateFilter(msg tea.Msg) (Model, tea.Cmd) {
	switch msg := msg.(type) {
	case filterAppliedMsg:
		m.filtering = false
		m.filter.StateCategories = msg.states
		m.filter.Priorities = msg.priorities
		m.filter.IssueTypeIDs = msg.typeIDs
		m.filter.AssigneeMe = msg.assigneeMe
		m.filter.ReviewerMe = msg.reviewerMe
		m.filter.ReviewerStatuses = msg.reviewerSts
		// the search keyword lives in the search box and is left untouched, so the two axes combine
		m.rememberFilter()
		m.loading, m.loadErr, m.loadingMore = true, false, false
		m.reqGen++
		return m, loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false)
	case filterCancelledMsg:
		m.filtering = false
		return m, nil
	case tea.MouseWheelMsg:
		if view := m.filterUI.View(); lipgloss.Height(view) > m.height {
			switch msg.Button {
			case tea.MouseWheelUp:
				return m.scrollModalBy(view, -1), nil
			case tea.MouseWheelDown:
				return m.scrollModalBy(view, 1), nil
			}
		}
	}
	var cmd tea.Cmd
	m.filterUI, cmd = m.filterUI.Update(msg)
	return m, cmd
}

func (m Model) scrollModalBy(view string, delta int) Model {
	maxOff := max(0, lipgloss.Height(view)-m.height)
	m.modalScroll = min(max(m.modalScroll+delta, 0), maxOff)
	return m
}

func (m Model) openFilter() (Model, tea.Cmd) {
	m.filtering = true
	m.modalScroll = 0
	m.search.Blur()
	m.focus = focusList // the list regains focus once the modal closes
	m.filterUI = newFilterForm(m.deps, m.filter, m.types, m.typesLoaded)
	return m, m.filterUI.Init()
}

func (m Model) selectedIssue() (domain.IssueSummary, bool) {
	if m.cursor < 0 || m.cursor >= len(m.issues) {
		return domain.IssueSummary{}, false
	}
	return m.issues[m.cursor], true
}

// patchRow updates the list row from a fresh detail, so a modal edit shows without a reload.
func (m *Model) patchRow(key string, d domain.IssueDetail) {
	for i := range m.issues {
		if m.issues[i].Key == key {
			applyDetailToSummary(&m.issues[i], d)
			break
		}
	}
	m.patchSprintIssueRows(key, d)
}

// patchSprintIssueRows mirrors a detail change into the sprint caches, which patchRow leaves stale.
func (m *Model) patchSprintIssueRows(key string, d domain.IssueDetail) {
	for id := range m.sprintIssues {
		pg := m.sprintIssues[id] // pg.Issues shares its backing array with the map's copy, so element writes persist
		for i := range pg.Issues {
			if pg.Issues[i].Key == key {
				applyDetailToSummary(&pg.Issues[i], d)
				break
			}
		}
	}
}

// applyDetailToSummary leaves Sprint alone: IssueDetail carries none, so patchRowSprintID owns it.
func applyDetailToSummary(it *domain.IssueSummary, d domain.IssueDetail) {
	it.Title = d.Title
	it.StateLabel = d.StateLabel
	it.StateCategory = d.StateCategory
	it.Priority = d.Priority
	it.AssigneeName = d.AssigneeName
	it.Assigned = d.AssigneeName != ""
}

// syncSelection points the Details panel at the cursor's issue (SWR load). No-op if unchanged.
func (m Model) syncSelection() (Model, tea.Cmd) {
	// keep viewKey pinned under a form/picker, so a reload cannot land the save on a different issue
	if m.editing || m.editingContent || m.commenting || m.deleting || m.picking || m.reviewing {
		return m, nil
	}
	it, ok := m.selectedIssue()
	if !ok {
		m.viewKey = ""
		return m, nil
	}
	if it.Key == m.viewKey {
		return m, nil
	}
	m.viewKey = it.Key
	m.detailScroll = 0
	m.activityScroll = 0
	var cmds []tea.Cmd
	if _, cached := m.details[it.Key]; !cached && !m.detailsPending[it.Key] {
		cmds = append(cmds, m.startDetailLoad(it.Key))
	}
	if c := m.maybeLoadActivity(); c != nil {
		cmds = append(cmds, c)
	}
	cmds = append(cmds, m.armNeighborPrefetch())
	return m, tea.Batch(cmds...)
}

// startDetailLoad bumps the key's generation, so a superseded in-flight load is ignored when it lands.
func (m *Model) startDetailLoad(key string) tea.Cmd {
	m.detailGen[key]++
	m.detailsPending[key] = true
	m.detailsFailed[key] = false
	return loadDetail(m.deps, key, m.detailGen[key])
}

// armNeighborPrefetch debounces, so a fast scroll warms neighbors only where it rests.
// Pointer receiver, so the bump propagates through the value-receiver syncSelection.
func (m *Model) armNeighborPrefetch() tea.Cmd {
	m.prefetchSeq++
	seq := m.prefetchSeq
	return tea.Tick(prefetchDebounce, func(time.Time) tea.Msg { return prefetchDebounceMsg{seq: seq} })
}

// prefetchNeighbors warms the detail cache around the cursor. It never repoints viewKey.
func (m Model) prefetchNeighbors() (Model, tea.Cmd) {
	var cmds []tea.Cmd
	for d := 1; d <= prefetchSpan; d++ {
		for _, i := range []int{m.cursor - d, m.cursor + d} {
			if i < 0 || i >= len(m.issues) {
				continue
			}
			key := m.issues[i].Key
			if key == "" {
				continue
			}
			if _, cached := m.details[key]; cached || m.detailsPending[key] {
				continue // already loaded or in flight - the gen/pending maps dedupe for us
			}
			cmds = append(cmds, m.startDetailLoad(key))
		}
	}
	return m, tea.Batch(cmds...)
}

// onDetailKey drives the focused Details/Activity panel. The per-issue action keys run earlier, in onKey.
func (m Model) onDetailKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "esc", "q", "backspace":
		return m.setFocus(focusList)
	case "tab":
		return m.cycleFocus(1)
	case "shift+tab":
		return m.cycleFocus(-1)
	case "R":
		if m.viewKey != "" {
			return m, m.retryActiveLoad()
		}
	case "up", "k":
		m.scrollActiveBy(-1)
	case "down", "j":
		m.scrollActiveBy(1)
	case "pgup":
		m.scrollActiveBy(-m.activePage())
	case "pgdown":
		m.scrollActiveBy(m.activePage())
	case "home", "g":
		m.setActiveScroll(0)
	case "end", "G":
		m.setActiveScroll(1 << 30) // clamps to the active view's max
	}
	return m, nil
}

// scrollActiveBy scrolls whichever view the keyboard drives: Activity when toggled on, else Details.
func (m *Model) scrollActiveBy(delta int) {
	if m.showActivity {
		m.activityScroll = clampScroll(m.activityScroll+delta, m.activityScrollMax())
		return
	}
	m.detailScroll = clampScroll(m.detailScroll+delta, m.detailScrollMax())
}

func (m *Model) setActiveScroll(v int) {
	if m.showActivity {
		m.activityScroll = clampScroll(v, m.activityScrollMax())
		return
	}
	m.detailScroll = clampScroll(v, m.detailScrollMax())
}

// retryActiveLoad refetches whichever view the keyboard drives (Activity when showing, else detail).
func (m *Model) retryActiveLoad() tea.Cmd {
	if m.showActivity {
		if m.activitiesPending[m.viewKey] {
			return nil
		}
		return m.startActivityLoad(m.viewKey)
	}
	if m.detailsPending[m.viewKey] {
		return nil
	}
	return m.startDetailLoad(m.viewKey)
}

const detailWheelStep = 2

func clampScroll(v, maxOff int) int { return min(max(v, 0), maxOff) }

func wheelClamp(cur int, btn tea.MouseButton, maxOff int) int {
	switch btn {
	case tea.MouseWheelUp:
		return clampScroll(cur-detailWheelStep, maxOff)
	case tea.MouseWheelDown:
		return clampScroll(cur+detailWheelStep, maxOff)
	}
	return clampScroll(cur, maxOff)
}

func (m Model) onHover(msg tea.MouseMotionMsg) (Model, tea.Cmd) {
	m.hover = ""
	if zone.Get(zoneSearch).InBounds(msg) {
		m.hover = zoneSearch
		return m, nil
	}
	if zone.Get(zoneFilter).InBounds(msg) {
		m.hover = zoneFilter
		return m, nil
	}
	if zone.Get(zoneNew).InBounds(msg) {
		m.hover = zoneNew
		return m, nil
	}
	if zone.Get(zoneBack).InBounds(msg) {
		m.hover = zoneBack
		return m, nil
	}
	for _, pt := range projectTabs {
		if zone.Get(pt.zone).InBounds(msg) {
			m.hover = pt.zone
			return m, nil
		}
	}
	// before the row loop: in narrow mode the backdrop list's row zones would swallow the reply link.
	if id, _, ok := m.hitCommentReply(msg); ok {
		m.hover = commentReplyZone(id)
		return m, nil
	}
	// inline edit pens, before the row loop for the same narrow-modal reason.
	for _, z := range []string{zoneEditIssue, zoneEditState, zoneEditAssignee, zoneEditReviewers, zoneAddChild, zoneAddRelation, zoneEditContent} {
		if zone.Get(z).InBounds(msg) {
			m.hover = z
			return m, nil
		}
	}
	// linked-issue keys, also before the row loop for the same narrow-modal reason.
	if z, ok := m.hoverPeekZone(msg); ok {
		m.hover = z
		return m, nil
	}
	if m.tab == tabIssues {
		for i := range m.issues {
			if zone.Get(issueRowZone(i)).InBounds(msg) {
				m.hover = issueRowZone(i)
				return m, nil
			}
		}
	}
	if m.tab == tabSprints {
		for i := range m.sprints {
			if zone.Get(sprintRowZone(i)).InBounds(msg) {
				m.hover = sprintRowZone(i)
				return m, nil
			}
		}
	}
	if m.tab == tabMembers {
		for i := range m.members {
			if zone.Get(memberRowZone(i)).InBounds(msg) {
				m.hover = memberRowZone(i)
				return m, nil
			}
		}
	}
	return m, nil
}

func (m Model) onKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	// list and Details focus only - in the search box letters must type into the query.
	if m.tab == tabIssues && (m.focus == focusList || m.focus == focusDetail) {
		if nm, cmd, ok := m.issueAction(msg); ok {
			return nm, cmd
		}
	}
	switch m.focus {
	case focusSearch:
		return m.onSearchKey(msg)
	case focusFilter:
		return m.onFilterKey(msg)
	case focusDetail:
		return m.onDetailKey(msg)
	default:
		return m.onListKey(msg)
	}
}

// issueAction returns ok=false for a key it does not own, so the caller falls through to its focus handler.
func (m Model) issueAction(msg tea.KeyPressMsg) (Model, tea.Cmd, bool) {
	switch msg.String() {
	case "e":
		nm, cmd := m.openEditForm()
		return nm, cmd, true
	case "E":
		nm, cmd := m.openContentEditor()
		return nm, cmd, true
	case "t":
		nm, cmd := m.openTransitionPicker()
		return nm, cmd, true
	case "a":
		nm, cmd := m.openAssigneePicker()
		return nm, cmd, true
	case "r":
		nm, cmd := m.openReviewersPicker()
		return nm, cmd, true
	case "L":
		nm, cmd := m.openRelationPicker()
		return nm, cmd, true
	case "U":
		nm, cmd := m.openRelationRemovePicker()
		return nm, cmd, true
	case "c":
		nm, cmd := m.openCommentForm()
		return nm, cmd, true
	case "d":
		nm, cmd := m.openDeleteConfirm()
		return nm, cmd, true
	case "v":
		nm, cmd := m.toggleActivity()
		return nm, cmd, true
	case "V":
		nm, cmd := m.openReviewForm()
		return nm, cmd, true
	case "s":
		nm, cmd := m.addSelectedIssueToSprint()
		return nm, cmd, true
	}
	return m, nil, false
}

// toggleActivity loads lazily. Narrow: Activity is a modal, so turning it on focuses Details too.
func (m Model) toggleActivity() (Model, tea.Cmd) {
	m.showActivity = !m.showActivity
	m.activityScroll = 0
	if m.narrow() && m.showActivity && m.focus != focusDetail {
		m, _ = m.setFocus(focusDetail) // open the read-only modal so the toggled-on Activity view is visible
	}
	if m.showActivity {
		return m, m.maybeLoadActivity()
	}
	return m, nil
}

func (m Model) onListKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	m.hover = "" // the keyboard is driving now, so drop any stale mouse-hover highlight
	switch msg.String() {
	case "backspace":
		// esc is intentionally NOT bound here, so a reflex press cannot escape the project.
		return m, back
	case "1", "2", "3", "4", "5":
		return m.switchTab(msg)
	}
	if m.tab == tabSprints {
		return m.onSprintListKey(msg)
	}
	if m.tab == tabMembers {
		return m.onMemberListKey(msg)
	}
	if m.tab == tabConfig {
		return m.onConfigKey(msg)
	}
	if m.tab == tabStats {
		return m.onStatsKey(msg)
	}
	if m.tab != tabIssues {
		return m, nil
	}
	switch msg.String() {
	case "tab":
		return m.cycleFocus(1)
	case "shift+tab":
		return m.cycleFocus(-1)
	case "/":
		return m.focusSearch()
	case "f":
		return m.openFilter()
	case "n":
		return m.openCreateForm()
	case "enter":
		if _, ok := m.selectedIssue(); ok {
			return m.setFocus(focusDetail) // the panel already shows this issue, so focus it to act/scroll
		}
		return m, nil
	case "up", "k":
		return m.moveCursor(-1)
	case "down", "j":
		return m.moveCursor(1)
	case "home", "g":
		m.cursor = 0
		return m.syncSelection()
	case "end", "G":
		m.cursor = max(0, len(m.issues)-1)
		return m.syncSelection()
	case "R":
		m.loading, m.loadErr, m.loadingMore = true, false, false
		m.reqGen++ // supersede any in-flight load so its late result is ignored
		return m, loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, 0, false)
	}
	return m, nil
}

func (m Model) onFilterKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "tab":
		return m.cycleFocus(1)
	case "shift+tab":
		return m.cycleFocus(-1)
	case "/":
		return m.focusSearch()
	case "esc":
		m.focus = focusList
		return m, nil
	case "enter", "space":
		return m.openFilter()
	case "1", "2", "3", "4", "5":
		m, cmd := m.switchTab(msg)
		m.focus = focusList
		return m, cmd
	}
	return m, nil
}

func (m Model) switchTab(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	if i := int(msg.String()[0] - '1'); i >= 0 && i < len(projectTabs) {
		return m.selectTab(projectTabs[i].tab)
	}
	return m, nil
}

// selectTab activates a sub-tab, kicking off its lazy load on first open (Issues: Init loaded it).
func (m Model) selectTab(t projectTab) (Model, tea.Cmd) {
	m.tab = t
	if t != tabIssues && m.focus != focusList {
		// only Issues has search/filter/detail focuses, so a leftover focus would swallow other tabs' keys
		m.search.Blur()
		m.focus = focusList
	}
	if t == tabMembers {
		var cmds []tea.Cmd
		// the per-member stats are one batch call, loaded once (and retried on failure) alongside the roster
		if !m.memberStatsRequested || m.memberStatsErr {
			m.memberStatsRequested = true
			m.memberStatsErr = false
			m.memberStatsGen++
			cmds = append(cmds, loadMemberStats(m.deps, m.projectKey, m.memberStatsGen))
		}
		if !m.membersRequested && (!m.membersLoaded || m.membersErr) {
			m.membersRequested = true
			cmds = append(cmds, loadMembers(m.deps, m.projectKey))
			return m, tea.Batch(cmds...)
		}
		// with a single member the selection never changes, so only reopening can retry a failed work load
		if id := m.selMemberID; id != 0 {
			if _, cached := m.memberWork[id]; !cached && m.memberWorkFailed[id] && !m.memberWorkPending[id] {
				cmds = append(cmds, m.startMemberWorkLoad(id))
				return m, tea.Batch(cmds...)
			}
		}
		m, cmd := m.syncMemberSelection()
		cmds = append(cmds, cmd)
		return m, tea.Batch(cmds...)
	}
	if t == tabConfig {
		// hide a prior one-time webhook secret: the no-reload path would otherwise leave it on screen
		m.githubSecret, m.githubSecretCopied, m.githubURLCopied = domain.GithubSecret{}, false, false
		if !m.configRequested || (m.configErr && !m.configLoading) {
			return m.loadProjectConfig()
		}
		return m, nil
	}
	if t == tabStats {
		return m.openStatsTab()
	}
	if t == tabSprints {
		if !m.sprintsRequested {
			return m.loadSprintList()
		}
		// with a single sprint the selection never changes, so only reopening can retry a failed issue load
		if id := m.selSprintID; id != 0 {
			if _, cached := m.sprintIssues[id]; !cached && m.sprintIssuesFailed[id] && !m.sprintIssuesPending[id] {
				return m, m.startSprintIssuesLoad(id)
			}
		}
	}
	return m, nil
}

func (m Model) focusSearch() (Model, tea.Cmd) { return m.setFocus(focusSearch) }

// cycleFocus rings list -> search -> filter -> detail. Narrow skips detail (enter/click opens it).
func (m Model) cycleFocus(delta int) (Model, tea.Cmd) {
	next := m.focus
	// skip focuses that are not live Tab stops. focusList is always available, so the loop terminates.
	for i := 0; i < int(focusCount); i++ {
		next = (next + projectFocus(delta) + focusCount) % focusCount
		if m.focusAvailable(next) {
			break
		}
	}
	return m.setFocus(next)
}

func (m Model) focusAvailable(target projectFocus) bool {
	switch target {
	case focusFilter:
		return m.deps.Mouse
	case focusDetail:
		return !m.narrow()
	}
	return true
}

func (m Model) setFocus(target projectFocus) (Model, tea.Cmd) {
	// narrow: leaving Details closes the modal, so drop showActivity - else the next v is a dead press.
	if m.narrow() && m.focus == focusDetail && target != focusDetail {
		m.showActivity = false
	}
	m.focus = target
	m.hover = ""
	if target == focusSearch {
		return m, m.search.Focus()
	}
	m.search.Blur()
	return m, nil
}

func (m Model) onSearchKey(msg tea.KeyPressMsg) (Model, tea.Cmd) {
	switch msg.String() {
	case "esc", "enter":
		m.search.Blur()
		m.focus = focusList
		return m, nil
	case "tab":
		return m.cycleFocus(1)
	case "shift+tab":
		return m.cycleFocus(-1)
	}
	var cmd tea.Cmd
	m.search, cmd = m.search.Update(msg)
	m.searchSeq++
	seq := m.searchSeq
	return m, tea.Batch(cmd, tea.Tick(searchDebounce, func(time.Time) tea.Msg { return searchDebounceMsg{seq: seq} }))
}

// moveCursor moves the cursor, appending the next page when it runs past the loaded issues.
func (m Model) moveCursor(delta int) (Model, tea.Cmd) {
	next := m.cursor + delta
	if next < 0 {
		next = 0
	}
	var loadMore tea.Cmd
	if next >= len(m.issues) {
		next = max(0, len(m.issues)-1)
		if m.page.HasNext && !m.loadingMore && len(m.issues) > 0 {
			m.loadingMore = true
			loadMore = loadIssues(m.deps, m.projectKey, m.filter, m.reqGen, m.page.Page+1, true)
		}
	}
	m.cursor = next
	m, sel := m.syncSelection()
	return m, tea.Batch(loadMore, sel)
}

// hitBranchLink is mouse-only and off during a peek, matching how the links render.
func (m Model) hitBranchLink(msg tea.MouseClickMsg) (tea.Cmd, bool) {
	if !m.deps.Mouse || m.peeking || m.viewKey == "" {
		return nil, false
	}
	d, ok := m.details[m.viewKey]
	if !ok {
		return nil, false
	}
	for i, b := range d.Branches {
		if b.BranchURL != "" && zone.Get(branchZone(i)).InBounds(msg) {
			return openURLCmd(b.BranchURL), true
		}
		if b.LatestCommitURL != "" && zone.Get(branchCommitZone(i)).InBounds(msg) {
			return openURLCmd(b.LatestCommitURL), true
		}
	}
	for i, pr := range d.PullRequests {
		if pr.URL != "" && zone.Get(pullRequestZone(i)).InBounds(msg) {
			return openURLCmd(pr.URL), true
		}
	}
	return nil, false
}

func (m Model) onClick(msg tea.MouseClickMsg) (Model, tea.Cmd) {
	if msg.Button != tea.MouseLeft {
		return m, nil
	}
	if zone.Get(zoneSearch).InBounds(msg) {
		return m.focusSearch()
	}
	if zone.Get(zoneFilter).InBounds(msg) {
		return m.openFilter()
	}
	if zone.Get(zoneNew).InBounds(msg) {
		return m.openCreateForm()
	}
	// before the panel zone, which spans all of Details and would swallow this click as a focus.
	if pid, _, ok := m.hitCommentReply(msg); ok {
		return m.openCommentSection(pid)
	}
	// linked-issue keys open a peek. Checked before the panel zone for the same swallow reason.
	if key, ok := m.hitPeek(msg); ok {
		return m.openPeek(key)
	}
	// branch and commit links open in the browser. Checked before the panel zone for the same reason.
	if cmd, ok := m.hitBranchLink(msg); ok {
		return m, cmd
	}
	// inline edit pens (mouse only), also before the panel zone. Each opener guards on the loaded detail.
	if zone.Get(zoneEditIssue).InBounds(msg) {
		return m.openEditForm()
	}
	if zone.Get(zoneEditState).InBounds(msg) {
		return m.openTransitionPicker()
	}
	if zone.Get(zoneEditAssignee).InBounds(msg) {
		return m.openAssigneePicker()
	}
	if zone.Get(zoneEditReviewers).InBounds(msg) {
		return m.openReviewersPicker()
	}
	if zone.Get(zoneAddChild).InBounds(msg) {
		return m.openChildCreateForm()
	}
	if zone.Get(zoneAddRelation).InBounds(msg) {
		return m.openRelationPicker()
	}
	if zone.Get(zoneEditContent).InBounds(msg) {
		return m.openContentEditor()
	}
	// clicking the Details or Activity panel focuses the detail area (so its scroll and action keys apply)
	if zone.Get(zoneDetail).InBounds(msg) || zone.Get(zoneActivity).InBounds(msg) {
		return m.setFocus(focusDetail)
	}
	// any other click hands focus back to the list, so the keyboard is not left on a hidden control
	if m.focus != focusList {
		m.search.Blur()
		m.focus = focusList
	}
	if zone.Get(zoneBack).InBounds(msg) {
		return m, back
	}
	for _, pt := range projectTabs {
		if zone.Get(pt.zone).InBounds(msg) {
			return m.selectTab(pt.tab)
		}
	}
	if m.tab == tabSprints {
		for i := range m.sprints {
			if zone.Get(sprintRowZone(i)).InBounds(msg) {
				m.sprintCursor = i
				return m.syncSprintSelection()
			}
		}
		return m, nil
	}
	if m.tab == tabMembers {
		for i := range m.members {
			if zone.Get(memberRowZone(i)).InBounds(msg) {
				m.memberCursor = i
				return m.syncMemberSelection()
			}
		}
		return m, nil
	}
	if m.tab == tabIssues {
		for i := range m.issues {
			if zone.Get(issueRowZone(i)).InBounds(msg) {
				m.cursor = i
				m, sel := m.syncSelection()
				if m.narrow() {
					// no side panel to reveal, so a click opens the read-only detail modal on that issue
					mf, foc := m.setFocus(focusDetail)
					return mf, tea.Batch(sel, foc)
				}
				return m, sel
			}
		}
	}
	return m, nil
}

// hitCommentReply: a zone scrolled out of view is not rendered, so it never matches.
func (m Model) hitCommentReply(msg tea.MouseMsg) (int64, string, bool) {
	d, ok := m.details[m.viewKey]
	if !ok {
		return 0, "", false
	}
	var walk func(cs []domain.IssueComment) (int64, string, bool)
	walk = func(cs []domain.IssueComment) (int64, string, bool) {
		for _, c := range cs {
			if !c.Deleted && c.ID != 0 && zone.Get(commentReplyZone(c.ID)).InBounds(msg) {
				return c.ID, c.AuthorName, true
			}
			if id, a, hit := walk(c.Replies); hit {
				return id, a, hit
			}
		}
		return 0, "", false
	}
	return walk(d.Comments)
}

// HelpTitle names the help modal after the project, falling back to its key when no title came in.
func (m Model) HelpTitle() string {
	if m.title != "" {
		return m.title
	}
	if m.projectKey != "" {
		return m.projectKey
	}
	return "Project"
}

func (m Model) HelpAbout() string {
	return "This project's issues. Browse and filter the list, open an issue to see its details, and edit " +
		"its fields, comments and relationships."
}

func (m Model) HelpKeys() []key.Binding {
	if m.peeking {
		binds := []key.Binding{key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "scroll"))}
		if m.detailsFailed[m.peekKey] {
			binds = append(binds, key.NewBinding(key.WithKeys("R"), key.WithHelp("R", "retry")))
		}
		return append(binds, key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "close")))
	}
	if m.dating {
		return m.dateHelpKeys()
	}
	if m.picking {
		if m.picker.Multi() {
			return []key.Binding{
				key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
				key.NewBinding(key.WithKeys("space"), key.WithHelp("space", "toggle")),
				key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "save")),
				key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
			}
		}
		return []key.Binding{
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "select")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "cancel")),
		}
	}
	if m.creating {
		return m.createUI.HelpKeys()
	}
	if m.editing {
		return m.editUI.HelpKeys()
	}
	if m.editingContent {
		return m.contentUI.HelpKeys()
	}
	if m.reviewing {
		return m.reviewUI.HelpKeys()
	}
	if m.commenting {
		if m.commentDeleting {
			return m.commentDeleteUI.HelpKeys()
		}
		return m.commentHelpKeys()
	}
	if m.deleting {
		return m.deleteUI.HelpKeys()
	}
	if m.sprintEditing {
		return m.sprintEditUI.HelpKeys()
	}
	if m.sprintConfirming {
		return m.sprintConfirmUI.HelpKeys()
	}
	if m.memberConfirming {
		return m.memberConfirmUI.HelpKeys()
	}
	if m.configEditing {
		return m.configEditUI.HelpKeys()
	}
	if m.configConfirming {
		return m.configConfirmUI.HelpKeys()
	}
	if m.filtering {
		return m.filterUI.HelpKeys()
	}
	if m.focus == focusDetail {
		binds := m.issueActionBinds()
		return append(binds,
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "scroll")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "list")),
		)
	}
	if m.focus == focusSearch {
		return []key.Binding{key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "done"))}
	}
	if m.focus == focusFilter {
		return []key.Binding{
			key.NewBinding(key.WithKeys("enter"), key.WithHelp("enter", "filter")),
			key.NewBinding(key.WithKeys("tab"), key.WithHelp("tab", "focus")),
			key.NewBinding(key.WithKeys("esc"), key.WithHelp("esc", "list")),
		}
	}
	// tab, reload and back live on the footer's global line (GlobalKeys), so only contextual keys here.
	var binds []key.Binding
	if m.tab == tabIssues {
		// enter (open Details) still works, it is just not advertised - it would only crowd the footer.
		binds = append(binds,
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
		)
		binds = append(binds, m.issueActionBinds()...) // actions work on the highlighted issue from the list too
		binds = append(binds,
			key.NewBinding(key.WithKeys("n"), key.WithHelp("n", "new")),
			key.NewBinding(key.WithKeys("f"), key.WithHelp("f", "filter")),
			key.NewBinding(key.WithKeys("/"), key.WithHelp("/", "search")),
		)
	}
	if m.tab == tabSprints {
		binds = append(binds,
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			// new needs no selection, so it is offered even on an empty tab
			key.NewBinding(key.WithKeys("n"), key.WithHelp("n", "new")),
		)
		if sp, ok := m.selectedSprint(); ok {
			switch sp.Status {
			case "PLANNING":
				binds = append(binds, key.NewBinding(key.WithKeys("s"), key.WithHelp("s", "start")))
			case "ACTIVE":
				binds = append(binds, key.NewBinding(key.WithKeys("c"), key.WithHelp("c", "complete")))
			case "CANCELLED":
				// the server only deletes a cancelled sprint, so the key is advertised only where it works
				binds = append(binds, key.NewBinding(key.WithKeys("d"), key.WithHelp("d", "delete")))
			}
			if sp.Status == "PLANNING" || sp.Status == "ACTIVE" {
				binds = append(binds,
					key.NewBinding(key.WithKeys("e"), key.WithHelp("e", "edit")),
					key.NewBinding(key.WithKeys("x"), key.WithHelp("x", "cancel")),
				)
				if p, ok := m.sprintIssues[sp.ID]; ok && len(p.Issues) > 0 {
					binds = append(binds, key.NewBinding(key.WithKeys("r"), key.WithHelp("r", "remove issue")))
				}
			}
		}
		// migrate acts on the current sprint, not the highlighted row
		if m.currentSprint != nil && findNextPlanningSprint(m.sprints) != nil {
			binds = append(binds, key.NewBinding(key.WithKeys("m"), key.WithHelp("m", "migrate")))
		}
	}
	if m.tab == tabMembers {
		binds = append(binds,
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("a"), key.WithHelp("a", "add")),
		)
		// role and remove need a highlighted member
		if _, ok := m.selectedMember(); ok {
			binds = append(binds,
				key.NewBinding(key.WithKeys("r"), key.WithHelp("r", "role")),
				key.NewBinding(key.WithKeys("d"), key.WithHelp("d", "remove")),
			)
		}
	}
	if m.tab == tabStats && m.statsLoaded {
		binds = append(binds, key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "scroll")))
	}
	if m.tab == tabConfig && m.configLoaded {
		archiveLabel := "archive"
		if m.project.Archived {
			archiveLabel = "restore"
		}
		binds = append(binds,
			key.NewBinding(key.WithKeys("e"), key.WithHelp("e", "edit")),
			key.NewBinding(key.WithKeys("a"), key.WithHelp("a", archiveLabel)),
		)
		// offered only when there is something to copy (the secret only while its reveal is on screen)
		if m.githubLoaded && m.github.WebhookURL != "" {
			binds = append(binds, key.NewBinding(key.WithKeys("u"), key.WithHelp("u", "copy URL")))
		}
		if m.githubSecret.Secret != "" {
			binds = append(binds, key.NewBinding(key.WithKeys("y"), key.WithHelp("y", "copy secret")))
		}
	}
	return binds
}

// GlobalKeys are the navigation hints on the footer's top line, kept apart from the per-issue actions.
func (m Model) GlobalKeys() []key.Binding {
	binds := []key.Binding{
		key.NewBinding(key.WithKeys("1", "2", "3", "4", "5"), key.WithHelp("1-5", "tab")),
	}
	// reload and back act only from the list focus, so do not advertise them as inert on the filter button.
	if m.focus == focusList {
		if m.tab == tabIssues {
			binds = append(binds, key.NewBinding(key.WithKeys("R"), key.WithHelp("R", "reload")))
		}
		binds = append(binds, key.NewBinding(key.WithKeys("backspace"), key.WithHelp("backspace", "back")))
	}
	return binds
}

// issueActionBinds are the per-issue action hints shared by the list and Details focus.
func (m Model) issueActionBinds() []key.Binding {
	binds := []key.Binding{
		key.NewBinding(key.WithKeys("e"), key.WithHelp("e", "edit")),
		key.NewBinding(key.WithKeys("E"), key.WithHelp("E", "content")),
		key.NewBinding(key.WithKeys("t"), key.WithHelp("t", "transition")),
		key.NewBinding(key.WithKeys("a"), key.WithHelp("a", "assign")),
		key.NewBinding(key.WithKeys("r"), key.WithHelp("r", "reviewers")),
		key.NewBinding(key.WithKeys("L"), key.WithHelp("L", "link")),
		key.NewBinding(key.WithKeys("c"), key.WithHelp("c", "comment")),
		key.NewBinding(key.WithKeys("d"), key.WithHelp("d", "delete")),
		key.NewBinding(key.WithKeys("v"), key.WithHelp("v", m.activityHelp())),
		key.NewBinding(key.WithKeys("V"), key.WithHelp("V", "review")),
		key.NewBinding(key.WithKeys("s"), key.WithHelp("s", "add to sprint")),
	}
	// unlink is offered only for relations this issue owns - an inverse must be dropped from the other one
	if len(m.removableRelations()) > 0 {
		binds = append(binds, key.NewBinding(key.WithKeys("U"), key.WithHelp("U", "unlink")))
	}
	return binds
}

func max(a, b int) int {
	if a > b {
		return a
	}
	return b
}

type issuesLoadedMsg struct {
	key    string // the project the load was for, so a stale cross-project result is ignored
	gen    int    // the request generation, so a superseded reload's late result is ignored
	page   domain.IssuePage
	append bool
	err    bool
}

type searchDebounceMsg struct{ seq int }

// prefetchDebounceMsg fires after the cursor has held still long enough to warm the neighbor details.
type prefetchDebounceMsg struct{ seq int }

type issueTypesLoadedMsg struct {
	types []domain.IssueTypeSummary
	err   bool
}

func loadIssueTypes(d deps.Deps) tea.Cmd {
	return func() tea.Msg {
		types, err := d.Catalog.ListIssueTypes(context.Background())
		return issueTypesLoadedMsg{types: types, err: err != nil}
	}
}

// IssueDetailLoadedMsg is exported so the app shell can route a late result back after the user left.
type IssueDetailLoadedMsg struct {
	key    string
	gen    int
	detail domain.IssueDetail
	err    bool
}

func loadDetail(d deps.Deps, key string, gen int) tea.Cmd {
	return func() tea.Msg {
		det, err := d.Issues.GetIssueDetail(context.Background(), key)
		return IssueDetailLoadedMsg{key: key, gen: gen, detail: det, err: err != nil}
	}
}

func back() tea.Msg { return nav.CloseProjectMsg{} }

func loadIssues(d deps.Deps, projectKey string, filter domain.IssueFilter, gen, page int, appendPage bool) tea.Cmd {
	return func() tea.Msg {
		p, err := d.Issues.SearchProjectIssues(context.Background(), projectKey, filter, page, pageSize)
		return issuesLoadedMsg{key: projectKey, gen: gen, page: p, append: appendPage, err: err != nil}
	}
}
