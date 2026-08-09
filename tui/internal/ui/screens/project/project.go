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

// focusList is the zero value so a fresh model opens on the list. Tab cycles list -> search -> filter
// -> detail.
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
	pickRelationType // step 1 of adding a relation: choosing the relation type
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
	viewer     string // the caller's username, for spotting their own comments; "" until the profile lands
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
	relSource         string                   // the issue the add-relation flow started from, so it lands there even if the cursor moves mid-load
	relGen            int                      // bumped per relation-target candidate load, so a superseded/stale result is dropped

	dating       bool // a calendar picker is open over the create/edit form (setting a due date or a DATE/TIMESTAMP field)
	datePick     widgets.DatePicker
	dateTarget   dateTarget // which field the confirmed pick fills
	dateCustomIx int        // the custom-field index when dateTarget == dateCustom

	editing    bool // the edit form is open over the detail modal
	editUI     editForm
	editScroll int                // window offset for an edit form taller than the terminal
	editBase   domain.IssueDetail // the detail the form was built from, so the save diffs against the open-time snapshot (not a cache a background refetch may have changed mid-edit)

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
	parentGen     int // bumped when the create form opens or its parent picker is (re)requested, so a late/stale parent-candidate load is dropped
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

	// realtime (SSE): a new-issue event schedules one debounced silent list reload; other events patch the
	// affected issue in place. rtRestore{Key,Gen} preserve the selected issue across that reload.
	rtReloadSeq       int
	rtRestoreKey      string
	rtRestoreGen      int
	rtSprintReloadSeq int // debounce sequence for the silent sprint-list reload on a lifecycle SSE event

	// Sprints tab: the sprint list (left) plus, for the cursor's sprint, its detail (top right) and the
	// issues that belong to it (bottom right). Loaded lazily the first time the tab is opened.
	sprints            []domain.SprintSummary
	sprintCursor       int
	sprintPage         domain.SprintPage
	sprintsRequested   bool // the lazy load has been kicked off, so re-entering the tab does not refetch
	sprintsLoading     bool
	sprintsErr         bool
	sprintReqGen       int   // bumped per sprint-list reload so a superseded load's late result is ignored
	selSprintID        int64 // the sprint shown on the right (the cursor's sprint); 0 when none
	sprintDetailScroll int   // first visible line of the sprint detail panel (top right)
	sprintIssueScroll  int   // first visible line of the sprint issues panel (bottom right)

	// issues belonging to a sprint, cached by sprint id (SWR): re-selecting a sprint shows the cached
	// rows at once while a background refetch runs.
	sprintIssues        map[int64]domain.IssuePage
	sprintIssuesPending map[int64]bool
	sprintIssuesFailed  map[int64]bool
	sprintIssuesGen     map[int64]int
	// per-sprint report cache, loaded in lockstep with that sprint's issues so the summary stays in sync
	sprintReport        map[int64]domain.SprintReport
	sprintReportPending map[int64]bool
	sprintReportFailed  map[int64]bool
	sprintReportGen     map[int64]int

	// sprint actions (start / complete / edit). The start and edit-due date pickers reuse m.dating +
	// m.datePick via the dateSprintStart / dateSprintEditDue targets. sprintActionID pins the sprint an
	// open action modal is acting on; sprintRestoreID re-points the cursor after the post-action reload.
	sprintConfirming  bool // a yes/no sprint action confirmation (complete or cancel) is open
	sprintConfirmUI   widgets.ConfirmForm
	sprintConfirmKind string // which action the confirmation guards: "complete" | "cancel" | "delete" | "migrate"
	sprintEditing     bool
	sprintCreating    bool // the sprint modal is adding a sprint rather than editing the selected one
	sprintEditUI      sprintEditForm
	sprintEditScroll  int
	sprintEditBase    domain.SprintSummary // the sprint the edit form was built from, to diff the save against (not a live summary a background reload may change)
	sprintActionID    int64
	sprintRestoreID   int64

	// the project's current (ACTIVE) sprint, prefetched so the issue tab's "add to sprint" action knows
	// the target without loading the whole sprint list; nil when there is none. Refreshed by the sprint
	// list; currentSprintGen orders those refreshes so a late Init prefetch cannot clobber a fresher value.
	currentSprint    *domain.SprintSummary
	currentSprintGen int

	// migrate (Sprints tab, "m"): moves the current sprint's incomplete issues onward to the next
	// PLANNING sprint (all of them - the server carries over every unresolved issue). The source/target
	// are resolved when the action opens (not the selected row), then the source's incomplete issues are
	// fetched to show in a yes/no confirmation (reusing sprintConfirming with kind "migrate"); migrateGen
	// drops a superseded candidate load.
	migrateSourceID   int64
	migrateTargetID   int64
	migrateSourceName string
	migrateTargetName string
	migrateKeys       []string // the incomplete issue keys shown in the confirmation, sent with the request
	migrateGen        int
	migrateLoading    bool

	// Members tab: the roster reuses m.members (loaded for the assignee picker); memberCursor selects a
	// member and selMemberID points the right panel at them. Each member's open (INITIAL/ACTIVE) assigned
	// and reviewer issues are fetched together and cached SWR by member id, mirroring the sprint caches.
	// membersRequested is the in-flight guard: New starts it true because Init always prefetches the
	// roster, so opening the tab mid-prefetch cannot dispatch a duplicate load (whose late failure would
	// otherwise clobber the good roster into the error state).
	membersRequested   bool
	membersErr         bool
	memberCursor       int
	selMemberID        int64
	memberDetailScroll int
	memberWork         map[int64]memberWorkload
	memberWorkPending  map[int64]bool
	memberWorkFailed   map[int64]bool
	memberWorkGen      map[int64]int

	// per-member contribution stats, loaded once as a batch the first time the Members tab is opened and
	// keyed by member id; a member absent from the map (no assigned issues) renders as zeros.
	memberStats          map[int64]domain.MemberStats
	memberStatsRequested bool
	memberStatsLoaded    bool
	memberStatsErr       bool
	memberStatsGen       int

	// member management actions (Members tab): add (a), change role (r), remove/kick (d). The add flow
	// loads candidates asynchronously (memberCandidateGen drops a superseded/stale result, and the
	// loading guard prevents a double-fetch) then opens a multi-select picker. memberActionID pins the
	// member a role/kick action targets so a background roster reload cannot redirect it; memberActionName
	// carries that member's name into the confirmation message.
	memberCandidateGen     int
	memberCandidateLoading bool
	memberActionID         int64
	memberActionName       string
	memberRestoreID        int64 // re-select this member (by id) after a post-action roster reload, so a reorder cannot drift the highlight
	memberConfirming       bool  // the kick confirmation is open
	memberConfirmUI        widgets.ConfirmForm

	// Config tab: the project's editable settings (title/description/visibility/archived), loaded lazily
	// the first time the tab is opened and refreshed after an edit/archive action. The edit form and the
	// archive confirmation float over the settings panel.
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
	githubSecret       domain.GithubSecret      // a just-revealed webhook secret (shown once); Secret=="" means none
	githubSecretCopied bool                     // that secret has been copied, so the reveal can say so
	githubURLCopied    bool                     // the webhook URL has been copied, likewise
	deliveries         []domain.WebhookDelivery // recent inbound webhooks and how each was handled
	deliveriesLoaded   bool                     // false when unreadable (not a manager) or not connected

	// Stats tab: the project's issue-statistics snapshot (counts by state/priority/hierarchy plus the
	// headline totals), loaded lazily the first time the tab is opened and silently refreshed on re-entry
	// so the counts reflect issues added or resolved since it was last viewed. A read-only panel: no forms
	// float over it. statsLoading is the sole in-flight guard (there is no first-open-only gate; every open
	// refreshes), statsLoaded distinguishes the first-load skeleton from a background refresh.
	stats        domain.ProjectStats
	statsLoading bool
	statsLoaded  bool
	statsErr     bool
	statsReqGen  int // bumped per stats fetch so a superseded/stale load's late result is dropped
	statsScroll  int // first visible line of the overview when it overflows a short terminal
	// advanced stats loaded alongside the simple snapshot; each has an OK flag so a single failing section
	// (they are separate member-only endpoints) just hides itself rather than blanking the whole panel.
	aging    domain.AgingStats
	agingOK  bool
	cycle    domain.CycleTimeStats
	cycleOK  bool
	flow     domain.FlowStats
	flowOK   bool
	velocity domain.Velocity
	velocOK  bool

	// initial focus requested by the entry (e.g. drilling in from an Inbox notification): pendingIssue
	// opens that issue's read-only peek on the first layout; pendingSprintID lands on the Sprints tab.
	// Both are consumed once (cleared on apply) so a later resize does not re-trigger. navSprintID hands
	// the target sprint to the sprint-list load so it selects it (and loads its issues) on landing.
	pendingIssue    string
	pendingSprintID int64
	navSprintID     int64
}

// WithInitialFocus records a focus target to apply once the screen first lays out: issueKey opens that
// issue's read-only peek; sprintID (when issueKey is empty) lands on the Sprints tab with that sprint
// selected. Used by the Inbox to drill from a notification straight to its issue or sprint.
func (m Model) WithInitialFocus(issueKey string, sprintID int64) Model {
	m.pendingIssue = issueKey
	if issueKey == "" {
		m.pendingSprintID = sprintID
	}
	return m
}

// applyPendingFocus consumes a one-shot entry focus (from WithInitialFocus) on the first layout: it
// opens the requested issue's read-only peek, or lands on the Sprints tab with the requested sprint
// selected. It clears the pending fields so a later resize cannot re-trigger it.
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

// WithViewer records who is looking, so the screen can tell the caller's own comments from everyone
// else's. The profile arrives after login (and after a silent restore has already built this screen), so
// it is set separately rather than passed to New.
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
		membersRequested: true,                                         // Init prefetches the roster; guard against a duplicate load before it lands
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
	// turning the mouse off can strand focus on the now-hidden filter button; snap it back to the list.
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
		// the panels reflow to the new size, so keep both scroll offsets within the new bounds - else a
		// grown viewport (smaller max) leaves a stale offset that eats the first scroll key as a dead press
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
		// only the last cursor stop's timer prefetches; a scroll that kept moving is handled by its own tick
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
			// a fresh-load failure replaces the body. An append (load-more) failure keeps the pages
			// already loaded and leaves page.HasNext set, so scrolling down retries.
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
			// a realtime silent reload preserves the selected issue: restore the cursor to the CURRENT
			// selection (m.viewKey reflects any navigation the user did during the in-flight window - syncSelection
			// runs after this) if it is still in the page. rtRestoreKey/Gen only flag that this landing is that
			// silent reload, so a filter/search that replaced the list still lands on cursor 0.
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
		m, sel := m.syncSelection() // point the Details panel at the selected issue and load it
		if msg.append {
			// load-more grew the list below a resting cursor, and syncSelection no-ops because the
			// selection is unchanged - so the freshly appended rows now within span would never be
			// warmed until the next cursor move. Warm them here directly: a page arrival is already a
			// settled event (no debounce needed) and the cache/pending maps dedupe any overlap.
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
		m.patchRow(msg.key, msg.detail) // keep the list row in step with the freshly loaded detail
		if msg.key == m.viewKey {
			// a refetch may return shorter content, so keep the scroll offset within the new bounds; else
			// the next scroll key would jump the old overflow in one press instead of moving by one line
			m.detailScroll = clampScroll(m.detailScroll, m.detailScrollMax())
			if m.commenting {
				// stay-open: a posted comment just landed and grew the thread; keep the focused composer visible
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
		// success: the optimistic state already shows, so refetch in the background (no evict, no skeleton
		// flash). failure: the optimistic state was wrong, so evict it and refetch - the modal shows the
		// skeleton then the true state (or the "Failed to load" banner), never fabricated data.
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
				// a silent post-action refresh failed but the roster we were showing is still good: keep it
				// (a wiped roster plus the action's success toast would contradict each other) and just note
				// the hiccup, mirroring the sprint list's post-action refresh handling
				m.memberRestoreID = 0 // this reload will not consume it; drop it so a later reload cannot misfire
				return m, toast.Show(toast.Error, "Couldn't refresh members.")
			}
			m.membersErr = true // no roster to fall back on: surface the failure (reopening the tab retries)
			return m, nil
		}
		m.membersErr = false
		m.members = msg.members
		m.pruneMemberWork() // drop cached work for members no longer in the roster (e.g. one just kicked)
		// keep the highlight on the same member across a post-action reload (the roster query is unordered,
		// so a reorder must not drift the selection); a kicked member is gone, so this falls through to the
		// cursor clamp and re-selection below
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
			return m.syncMemberSelection() // re-points at the cursor's member (loading their work if it changed)
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
		// same as transitions: keep the optimistic assignee on success (background refetch reconciles),
		// evict on failure so the wrong assignee is not left on screen if the refetch also fails.
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
		// keep the modal open on failure so the reason stays in place beside the controls that produced it;
		// refetch either way, since a partial effect (verdict written, comment not) must not be guessed at
		if msg.err {
			m.reviewUI.sending = false
			m.reviewUI.status = msg.errText
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		m.reviewing = false
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, msg.text))

	case ReviewerDoneMsg:
		// refetch either way: on success to show the new roster, on failure because a partial diff may
		// have applied before the erroring call, so the cached roster can no longer be trusted.
		if msg.err {
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Reviewers updated."))

	case parentEditCandidatesLoadedMsg:
		return m.onParentEditCandidates(msg)

	case ParentEditDoneMsg:
		// refetch either way: on success to fill the new parent's type/state, on failure to revert the
		// optimistic write the reload could not otherwise undo.
		if msg.err {
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Parent updated."))

	case EditDoneMsg:
		// same as transitions/assigns: keep the optimistic edit on success (background refetch
		// reconciles), evict on failure so wrong values are not left on screen if the refetch also fails.
		if msg.err {
			delete(m.details, msg.key)
			return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Error, msg.errText))
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Issue updated."))

	case CommentDoneMsg:
		// a comment is additive, so there is nothing to roll back on failure. On success refetch the
		// detail (its BFF carries the comment thread) so the new comment appears; the cached detail stays
		// on screen meanwhile, so there is no skeleton flash.
		if m.commenting {
			return m.onCommentDoneWhileOpen(msg) // stay-open: clear the posted composer, keep the modal up
		}
		if msg.err {
			return m, toast.Show(toast.Error, msg.errText)
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, "Comment added."))

	case IssueDeletedMsg:
		// the delete dialog owns the in-flight/error state; route the result there so a failure shows in
		// place and a success closes the modal and drops the row (works even after the user left the drill-in)
		return m.updateDelete(msg)

	case IssueCreatedMsg:
		// the form already closed on submit. On success reload the list from the top so the new issue
		// appears (when it matches the active filter); on failure just surface the error.
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
		// the create form's Parent field asked to open the parent picker
		return m.openParentPicker()
	case openDueCreateMsg:
		return m.openDuePicker(dateDueCreate)
	case openDueEditMsg:
		return m.openDuePicker(dateDueEdit)
	case openParentEditFromFormMsg:
		return m.openParentEditPicker() // the edit form's Parent field asked to open the parent picker
	case openCustomDateMsg:
		return m.openCustomDatePicker(msg.index)
	case openCustomDateEditMsg:
		return m.openCustomDatePickerEdit(msg.index)
	case parentCandidatesLoadedMsg:
		return m.onParentCandidates(msg)
	case relationCandidatesLoadedMsg:
		return m.onRelationCandidates(msg)
	case relationDoneMsg:
		// nothing is applied optimistically, so there is nothing to roll back; on success refetch the detail
		// (its BFF carries the relations) so the change appears, and surface a rejection as a toast.
		if msg.err {
			return m, toast.Show(toast.Error, msg.errText)
		}
		text := "Relation added."
		if msg.removed {
			text = "Relation removed."
		}
		return m, tea.Batch(m.startDetailLoad(msg.key), toast.Show(toast.Success, text))
	case createTypeFieldsMsg:
		// the create form's type changed (or it opened): load that type's custom fields
		return m.requestCustomFields(msg.typeID)
	case typeFieldsLoadedMsg:
		return m.onTypeFieldsLoaded(msg)
	case RealtimeIssueEventMsg:
		// a live SSE issue event for this project: patch in place (background, regardless of open modals)
		return m.onRealtimeIssueEvent(msg)
	case realtimeReloadMsg:
		return m.onRealtimeReload(msg)
	case RealtimeSprintEventMsg:
		// a live SSE sprint event for this project: refresh the sprint list/caches in place (background)
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
		// a superseded load (the sprint list, which owns currentSprint once loaded, has since refreshed it)
		// or a best-effort prefetch failure: keep whatever the authoritative source last set
		if msg.gen != m.currentSprintGen || msg.err {
			return m, nil
		}
		m.currentSprint = msg.sprint
		return m, nil
	}

	// an open action form/picker owns the keyboard until it closes. The calendar and the parent picker
	// float over the create/edit form, so they take input ahead of updateCreate/updateEdit.
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

// onWheel scrolls the detail when the wheel is over the side panel (or whenever the read-only modal is
// open on a narrow terminal), and moves the list cursor otherwise.
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
	// each side panel scrolls independently by its own zone; the narrow read-only modal (no side zone)
	// routes the wheel to whichever view it is currently showing.
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

// updateFilter drives the open filter modal: applying commits the axes and reloads, cancelling drops
// it, a wheel scrolls the modal when it overflows, and anything else is forwarded to the form.
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
		m.rememberFilter() // persist the applied filter so re-opening the project restores it
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

// patchRow updates the list row for key from a freshly loaded detail, so a state or assignee change
// made in the modal shows in the list without a full reload.
func (m *Model) patchRow(key string, d domain.IssueDetail) {
	for i := range m.issues {
		if m.issues[i].Key == key {
			applyDetailToSummary(&m.issues[i], d)
			break
		}
	}
	m.patchSprintIssueRows(key, d) // keep the Sprints tab's cached issue lists in step too
}

// patchSprintIssueRows mirrors a detail change into every cached sprint issue list, so a transition or
// assignment made on the Issues tab shows in the Sprints tab's bottom panel without a refetch (that
// panel is a separate SWR cache keyed by sprint id, so patchRow alone would leave it stale).
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

// applyDetailToSummary copies the display fields a detail can change onto a list-row summary. The
// sprint field is intentionally left alone: IssueDetail carries no sprint, so patchRowSprintID owns it.
func applyDetailToSummary(it *domain.IssueSummary, d domain.IssueDetail) {
	it.Title = d.Title
	it.StateLabel = d.StateLabel
	it.StateCategory = d.StateCategory
	it.Priority = d.Priority
	it.AssigneeName = d.AssigneeName
	it.Assigned = d.AssigneeName != ""
}

// syncSelection points the Details panel at the cursor's issue, loading it (SWR: cached shows at once,
// otherwise a deduped fetch) and resetting the panel scroll. A no-op when the selection is unchanged.
func (m Model) syncSelection() (Model, tea.Cmd) {
	// while an action form/picker is open, keep viewKey pinned to the issue being acted upon, so a
	// background list reload (e.g. a search debounce that lands mid-edit) cannot repoint the target and
	// make a save/comment/transition land on a different issue
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
	if c := m.maybeLoadActivity(); c != nil { // load the activity too when its view is showing
		cmds = append(cmds, c)
	}
	cmds = append(cmds, m.armNeighborPrefetch()) // warm the rows around the cursor once it settles
	return m, tea.Batch(cmds...)
}

// startDetailLoad bumps the load generation for key and returns the fetch command, so a superseded
// in-flight load for the same issue is ignored when it lands (mirrors the issue list's reqGen).
func (m *Model) startDetailLoad(key string) tea.Cmd {
	m.detailGen[key]++
	m.detailsPending[key] = true
	m.detailsFailed[key] = false
	return loadDetail(m.deps, key, m.detailGen[key])
}

// armNeighborPrefetch bumps the prefetch generation and returns a debounced tick carrying it, so a fast
// scroll through many rows warms neighbors only at the row it finally rests on (mirrors the search
// debounce). Pointer receiver so the bump propagates through the value-receiver syncSelection.
func (m *Model) armNeighborPrefetch() tea.Cmd {
	m.prefetchSeq++
	seq := m.prefetchSeq
	return tea.Tick(prefetchDebounce, func(time.Time) tea.Msg { return prefetchDebounceMsg{seq: seq} })
}

// prefetchNeighbors warms the detail cache for the rows within prefetchSpan of the cursor, so scrolling
// onto one shows its detail without the skeleton flash. Cache-warm only: it fires the same deduped load
// a selection would, but never repoints viewKey, and IssueDetailLoadedMsg only touches the view when its
// key matches viewKey - so a landing prefetch is invisible until the cursor actually reaches that row.
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

// onDetailKey drives the Details/Activity panel while it is focused: esc/tab return to the list, R
// retries a failed load, and the scroll keys move the windowed body. The per-issue action keys
// (e/t/a/r/c/d/v) are handled ahead of the focus dispatch in onKey, so they work here too without being
// listed again.
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

// scrollActiveBy scrolls the view the keyboard currently drives - the Activity view when it is toggled
// on, else the Details view.
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

// retryActiveLoad refetches whichever view the keyboard drives, so r retries a failed Activity load
// when the Activity view is showing and a failed detail load otherwise.
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

// wheelClamp steps a scroll offset up or down by the wheel step, clamped to [0, maxOff].
func wheelClamp(cur int, btn tea.MouseButton, maxOff int) int {
	switch btn {
	case tea.MouseWheelUp:
		return clampScroll(cur-detailWheelStep, maxOff)
	case tea.MouseWheelDown:
		return clampScroll(cur+detailWheelStep, maxOff)
	}
	return clampScroll(cur, maxOff)
}

// onHover records the row or button under the cursor so it can be highlighted.
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
	// checked before the issue-row loop (as in onClick): in narrow mode the read-only modal floats over
	// the full-width list, whose row zones survive in the backdrop, so a reply link inside the modal
	// would otherwise match the row behind it and never highlight.
	if id, _, ok := m.hitCommentReply(msg); ok {
		m.hover = commentReplyZone(id)
		return m, nil
	}
	// inline edit pens, checked before the row loop for the same narrow-modal-over-list reason as replies.
	for _, z := range []string{zoneEditIssue, zoneEditState, zoneEditAssignee, zoneEditReviewers, zoneAddChild, zoneAddRelation, zoneEditContent} {
		if zone.Get(z).InBounds(msg) {
			m.hover = z
			return m, nil
		}
	}
	// linked-issue keys (parent/child/relation), also before the row loop: in narrow mode the detail modal
	// floats over the list, whose row zones survive in the backdrop and would otherwise match instead.
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
	// Per-issue actions act on the highlighted issue from the list or the Details panel, so the user need
	// not tab into Details first. They fire only from those two issue-viewing focuses - not from the
	// search box (letters must type into the query) nor the filter button (a control, whose help would
	// otherwise advertise nothing while the keys silently acted).
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

// issueAction handles the per-issue action keys shared across every non-search focus. ok is false when
// msg is not an action key, so the caller falls through to the focus-specific handler. Each opener
// guards on the selected issue (and its loaded detail), so acting from the list is safe.
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

// toggleActivity flips the Activity view, loading it lazily. Wide: it adds/removes the Activity column
// (or swaps it in for Details). Narrow: the Activity view is a read-only modal that only shows while
// Details is focused, so turning it on from the list also focuses Details to open that modal.
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
		// backspace is the deliberate leave-to-home. esc is intentionally NOT bound here: from the list
		// root it does nothing, so a reflex esc cannot escape the project (esc only steps back a level
		// from the search box, filter button, or Details focus).
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
			return m.setFocus(focusDetail) // the panel already shows this issue; focus it to act/scroll
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

// onFilterKey drives the search row's filter button while it is focused: enter/space opens the modal,
// tab cycles focus, and a digit still switches tabs (dropping back to the list).
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

// selectTab activates a sub-tab, kicking off its lazy load the first time it is opened. Only the
// Sprints tab loads on demand; the Issues tab was already loaded in Init.
func (m Model) selectTab(t projectTab) (Model, tea.Cmd) {
	m.tab = t
	if t != tabIssues && m.focus != focusList {
		// only the Issues tab has search/filter/detail focuses; keep the keyboard on the list elsewhere so
		// a focus left over from Issues does not swallow the other tabs' keys
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
		// reopening the tab retries a failed work load for the current member: in a single-member project
		// the selection never changes, so syncMemberSelection alone can never recover it (mirrors Sprints)
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
		// re-entering Config hides any one-time webhook secret from a prior reveal: the reload path clears it
		// via onConfigLoaded, but the already-loaded no-reload path would otherwise leave it on screen
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
		// reopening the tab also retries a failed issue load for the current sprint: in a single-sprint
		// project the selection never changes, so syncSprintSelection alone can never recover it
		if id := m.selSprintID; id != 0 {
			if _, cached := m.sprintIssues[id]; !cached && m.sprintIssuesFailed[id] && !m.sprintIssuesPending[id] {
				return m, m.startSprintIssuesLoad(id)
			}
		}
	}
	return m, nil
}

func (m Model) focusSearch() (Model, tea.Cmd) { return m.setFocus(focusSearch) }

// cycleFocus advances focus around the ring list -> search -> filter -> detail -> list. In narrow mode
// focusDetail means "the read-only modal is open", which only enter/click should trigger, so the ring
// skips it there (list -> search -> filter -> list); a single extra step always lands on a real cell.
func (m Model) cycleFocus(delta int) (Model, tea.Cmd) {
	next := m.focus
	// step in the requested direction, skipping any focus that is not a live Tab stop right now, so the
	// ring never lands on an invisible control. focusList is always available, so the loop terminates.
	for i := 0; i < int(focusCount); i++ {
		next = (next + projectFocus(delta) + focusCount) % focusCount
		if m.focusAvailable(next) {
			break
		}
	}
	return m.setFocus(next)
}

// focusAvailable reports whether target is a live Tab stop. The filter button is a click affordance
// hidden with the mouse off (the f key opens the filter instead), and in narrow mode Details is a modal
// opened by enter/click rather than a Tab stop.
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
	// In narrow mode the Details/Activity view is a modal shown only while Details is focused; leaving
	// that focus closes it, so drop the Activity toggle too. Otherwise showActivity lingers invisibly
	// true and the next v from the list toggles it back off with no visible effect (a dead first press).
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

// onSearchKey drives the search box while it is focused. esc/enter hand focus back to the list, tab
// advances to the filter button; every other key edits the query and (re)arms the debounce timer.
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

// When the cursor reaches the end of the loaded issues with more pages available, requests the next
// page (appended).
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
	m, sel := m.syncSelection() // the Details panel follows the cursor
	return m, tea.Batch(loadMore, sel)
}

// hitBranchLink reports whether a click landed on a branch name, commit hash or pull request in the loaded
// detail, and returns the command that opens its URL. Mouse-only and off during a peek, matching how the
// links render.
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
	// a click on a comment's Reply affordance opens the comment modal pre-targeted at that comment.
	// Checked before the panel zone, which spans the whole Details panel and would otherwise swallow
	// the click as a focus.
	if pid, _, ok := m.hitCommentReply(msg); ok {
		return m.openCommentSection(pid)
	}
	// a click on a linked issue's key (parent/child/relation) opens its read-only peek. Checked before the
	// panel zone, which spans the whole Details area and would otherwise swallow the click as a focus.
	if key, ok := m.hitPeek(msg); ok {
		return m.openPeek(key)
	}
	// a click on a branch name or commit hash opens that link in the browser. Checked before the panel
	// zone, which spans the whole Details area and would otherwise swallow the click as a focus.
	if cmd, ok := m.hitBranchLink(msg); ok {
		return m, cmd
	}
	// inline edit pens (mouse only), also checked before the panel zone. Each opens the same editor its
	// keyboard shortcut does, and each opener guards on the loaded detail so a click is safe.
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
	// any other click (a tab, the back affordance, a row) hands focus back to the list, so the keyboard
	// never stays routed to the search box or filter button the target no longer shows
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
				m, sel := m.syncSelection() // the Details panel/modal follows the clicked row
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

// hitCommentReply reports whether the mouse is over a rendered comment's Reply affordance, returning
// that comment's id and author so a reply can be threaded under it. It walks the loaded comment tree;
// a zone that is not currently rendered (scrolled out of view) simply never matches.
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

// HelpTitle names the help modal after the project being drilled into, so it reads "{project} · Help"
// like the top-level tabs do. Falls back to the project key, then a generic label, when the display
// title was not carried in (e.g. the restore-into-project path).
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
	// 1-4 tab, r reload and backspace back live on the footer's top (global) line via GlobalKeys; the
	// bottom line carries only the contextual list actions.
	var binds []key.Binding
	if m.tab == tabIssues {
		// enter (open Details) is omitted on purpose: it is the conventional "open the row" gesture and
		// would only crowd the footer. The key still works; it is just not advertised here or in Help.
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
		// the applicable lifecycle actions depend on the selected sprint's status
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
		// migrate acts on the current sprint (not the highlighted row), so it is offered whenever there is
		// an active sprint and a later planning sprint to move its incomplete issues into
		if m.currentSprint != nil && findNextPlanningSprint(m.sprints) != nil {
			binds = append(binds, key.NewBinding(key.WithKeys("m"), key.WithHelp("m", "migrate")))
		}
	}
	if m.tab == tabMembers {
		binds = append(binds,
			key.NewBinding(key.WithKeys("up", "down"), key.WithHelp("↑/↓", "move")),
			key.NewBinding(key.WithKeys("a"), key.WithHelp("a", "add")),
		)
		// role and remove act on the highlighted member, so they are offered only when one is selected
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
		// the copy keys are offered only when they have something to copy: the URL for as long as the
		// integration exists, the one-time secret only while its reveal is on screen
		if m.githubLoaded && m.github.WebhookURL != "" {
			binds = append(binds, key.NewBinding(key.WithKeys("u"), key.WithHelp("u", "copy URL")))
		}
		if m.githubSecret.Secret != "" {
			binds = append(binds, key.NewBinding(key.WithKeys("y"), key.WithHelp("y", "copy secret")))
		}
	}
	return binds
}

// GlobalKeys are the drill-in's navigation hints shown on the footer's top (global) line: its sub-tab
// switch, the list reload, and the leave key - kept apart from the per-issue actions below.
func (m Model) GlobalKeys() []key.Binding {
	binds := []key.Binding{
		key.NewBinding(key.WithKeys("1", "2", "3", "4", "5"), key.WithHelp("1-5", "tab")),
	}
	// reload and the leave key act only from the list focus (onListKey). The filter button focus - the
	// other non-capturing focus where this row shows - ignores them, so do not advertise inert keys there.
	if m.focus == focusList {
		if m.tab == tabIssues {
			binds = append(binds, key.NewBinding(key.WithKeys("R"), key.WithHelp("R", "reload")))
		}
		binds = append(binds, key.NewBinding(key.WithKeys("backspace"), key.WithHelp("backspace", "back")))
	}
	return binds
}

// issueActionBinds are the per-issue action key hints shared by the list and Details focus, since the
// actions work from both.
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
	// unlink is offered only when the viewed issue owns a relation it can actually drop: the inverse of a
	// directional relation has to be removed from the other issue, so advertising it here would mislead
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

// IssueDetailLoadedMsg is exported so the app shell can route this background result back to the
// project screen even when the user has left the drill-in before the fetch landed.
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
