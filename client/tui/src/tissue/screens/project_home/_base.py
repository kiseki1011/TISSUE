from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.screens.base import RefreshableScreen
from tissue.screens.project_home.issue_filter import (
    DEFAULT_ISSUE_FILTER,
    IssueFilter,
)

if TYPE_CHECKING:
    from collections.abc import Callable

    from textual.timer import Timer
    from textual.widget import Widget

    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.comment_detail_response import (
        CommentDetailResponse,
    )
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )
    from tissue.api.generated.models.sprint_summary import SprintSummary
    from tissue.api.generated.models.workflow_detail import WorkflowDetail
    from tissue.app import TissueApp


class ProjectHomeBase(RefreshableScreen):
    """Shared base for the ProjectHomeScreen area mixins.

    Holds the screen's shared state (`__init__`) and, under `TYPE_CHECKING`, the
    cross-area method contract each mixin type-checks against. Real bodies live on
    whichever mixin owns the area; every mixin inherits this base.
    """

    if TYPE_CHECKING:
        app: TissueApp

    def __init__(self, project_key: str, title: str | None = None) -> None:
        super().__init__()
        self._project_key = project_key
        self._title = title
        self._issues: list[IssueSummary] = []
        # The active filter for the [1] Issues list (state/priority/assignee/sprint).
        # Defaults to non-terminal issues; edited via the ⚙ filter modal.
        self._filter: IssueFilter = DEFAULT_ISSUE_FILTER
        self._members: list[ProjectMemberSummary] = []
        # The members currently shown in the [1] table — the full roster, or a
        # client-side keyword-filtered subset when searching. `self._members` stays
        # the full roster (assignee/actor name resolution depends on it); row
        # selection indexes THIS list so the cursor and the table never desync.
        self._displayed_members: list[ProjectMemberSummary] = []
        # [3] box: issues across this project assigned to agents the user owns,
        # plus an agent member-id -> display-name map for the Assignee column.
        self._agent_issues: list[IssueSummary] = []
        self._agent_names: dict[int, str] = {}
        # [3] toggles (CTRL+T while focused) between "work" (agent-assigned issues)
        # and "reviews" (issues where the current user is a requested reviewer).
        self._agent_mode = "work"
        # Left-column layout (F3/F4): which stacked box is collapsed to a sliver
        # ("issues-box" / "agent-box" / None), and whether the column is expanded
        # to full width (hiding [2], in which case Enter opens the detail modal).
        self._collapsed_box: str | None = None
        self._expanded = False
        # The [1] box toggles between the issues list and the sprints list; the
        # sprints are loaded lazily the first time that view is shown.
        self._view_mode = "issues"
        self._sprints: list[SprintSummary] = []
        # Sprint read view (F6): the open sprint's id and the two issue lists it
        # shows — its assigned issues, and the open issues that can be added to it —
        # stashed so the ↑/↓ transfer buttons can map a cursor row to an issue key.
        self._sprint_detail_id: int | None = None
        self._sprint_detail_issues: list[IssueSummary] = []
        self._sprint_open_issues: list[IssueSummary] = []
        self._detail_issue_key: str | None = None
        self._detail_assigned = False
        # Reviewers of the issue currently in [2], stashed when it renders: the
        # member ids (for the picker diff + request-review) and the assignee id
        # (excluded from the reviewer picker — the assignee can't be a reviewer).
        self._detail_reviewer_ids: list[int] = []
        self._detail_assignee_id: int | None = None
        # Serialises reviewer mutations: a reviewer add/remove/request-review is in
        # flight, so ignore further reviewer clicks until it settles (they run in
        # their own worker group so a [2] re-render can't cancel them mid-sequence).
        self._reviewer_busy = False
        # The issue + reviewer set the open reviewer picker edits, snapshotted when
        # it opens so applying its result diffs against THIS baseline even if the
        # detail re-renders or switches issue while the picker is up.
        self._reviewer_picker_issue: str | None = None
        self._reviewer_picker_baseline: list[int] = []
        # Parent/children hierarchy of the issue in [2], fetched separately (not on
        # the detail DTO). `_detail_hierarchy` is the issue's own level (resolved via
        # the type catalog) — it gates which +/✕ controls show. `_detail_children`
        # feeds the child picker's exclude set. `_issue_type_hierarchy` caches the
        # catalog (type id -> hierarchy). `_hierarchy_busy` serialises hierarchy
        # mutations; `_hier_picker_issue` snapshots which issue a picker edits.
        self._detail_hierarchy: str | None = None
        self._detail_children: list[IssueIdentifierResponse] = []
        self._issue_type_hierarchy: dict[int, str] = {}
        self._hierarchy_busy = False
        self._hier_picker_issue: str | None = None
        # Comment reply target: the root comment id a new comment replies to (None =
        # a top-level comment). Set when a comment's ↳ Reply is pressed, cleared on
        # submit/cancel. `_reply_targets` maps each root comment id to its author
        # label, for the "Replying to @…" banner above the input.
        self._reply_to: int | None = None
        self._reply_targets: dict[int, str] = {}
        # Bumped on every comment-thread rebuild. A post worker captures it at
        # submit and skips its optimistic mount if the thread was rebuilt meanwhile
        # (that rebuild re-fetched the comments, so the new one is already shown).
        self._comment_gen: int = 0
        # The [2] detail render is debounced so flying the cursor through a list
        # (holding ↓) doesn't fire a full render (several fetches + a mount) for
        # every row it passes over — only the row the cursor settles on renders.
        # Holds the pending settle timer (stopped/replaced on each new highlight).
        self._detail_timer: Timer | None = None
        # Live search is debounced too: the pending search timer (fires once typing
        # in #hub-search pauses). The keyword filters whichever list view is active.
        self._search_timer: Timer | None = None
        # Current values of the editable detail fields, stashed when the detail
        # renders so a field-edit modal can prefill (field name -> string).
        self._edit_current: dict[str, str] = {}
        # Workflow graphs are cached by id; they barely change and several issues
        # share one — fetched only to label transitions with their target state.
        self._workflow_cache: dict[int, WorkflowDetail] = {}
        self._transitions_by_id: dict[int, AvailableTransition] = {}
        # Labels for the transition picker modal, stashed when the detail renders.
        self._transition_current_label = "-"
        self._transition_target_labels: dict[int, str] = {}
        # state-id -> #rrggbb, harvested from the project's workflows so the
        # issues table can tint each Status with its workflow-defined colour.
        self._state_colors: dict[int, str] = {}
        # Custom-field metadata stashed when the detail renders, so a ✎ edit opens
        # the right modal: field id -> its current value info, and field id -> its
        # selectable options (SELECT_OPTION / CHECKLIST).
        self._detail_custom_fields: dict[int, CustomFieldValueInfo] = {}
        self._detail_field_options: dict[int, list[FieldOptionDetail]] = {}

    if TYPE_CHECKING:
        # Cross-area methods: implemented by the mixin that owns the area, called
        # from others. Declared here so every mixin type-checks against them.
        async def _load_issues(self, keyword: str | None = None) -> None: ...
        async def _load_agent_issues(self, *, focus_list: bool = False) -> None: ...
        def _toggle_agent_mode(self) -> None: ...
        def action_focus_agent_issues(self) -> None: ...
        def _current_hub_box(self) -> str | None: ...
        def _update_create_button(self) -> None: ...
        def _refresh_box_chrome(self) -> None: ...
        def _open_issue_modal(self, issue_key: str) -> None: ...
        def _debounce_detail(
            self, render: Callable[[], object], *, immediate: bool
        ) -> None: ...
        def _cancel_detail_timer(self) -> None: ...
        def _open_create_sprint(self) -> None: ...
        async def _load_sprints(self) -> None: ...
        async def _ensure_sprints_loaded(self) -> None: ...
        async def _load_members(self) -> None: ...
        async def _ensure_members_loaded(self) -> None: ...
        async def _load_members_list(self, keyword: str | None = None) -> None: ...
        async def _render_members_list(self, keyword: str | None = None) -> None: ...
        def _update_search_input(self) -> None: ...
        def _cancel_search_timer(self) -> None: ...
        async def _clear_timeline(self) -> None: ...
        def _set_view_chrome(self, mode: str) -> None: ...
        def _run_view_load(self, mode: str, *, focus_list: bool = False) -> None: ...
        def action_focus_issues(self) -> None: ...
        def action_focus_detail(self) -> None: ...
        async def _render_issue_detail(
            self, issue_key: str, *, focus_detail: bool
        ) -> None: ...
        async def _reset_detail_pane(self) -> None: ...
        async def _mount_detail(self, widgets: list[Widget]) -> None: ...
        async def _load_activity(self, issue_key: str) -> None: ...
        def _status_action(
            self,
            transitions: list[AvailableTransition],
            current_state_label: str,
            target_labels: dict[int, str],
        ) -> Widget | None: ...
        def _comment_section(
            self, comments: list[CommentDetailResponse]
        ) -> list[Widget]: ...
        def _reviewer_section(self, d: IssueCommonDetail) -> list[Widget]: ...
        def _refresh_detail(self, issue_key: str) -> None: ...
        async def _ensure_issue_type_hierarchy(self) -> None: ...
        def _hierarchy_section(
            self,
            d: IssueCommonDetail,
            parent: IssueIdentifierResponse | None,
            children: list[IssueIdentifierResponse],
        ) -> list[Widget]: ...
