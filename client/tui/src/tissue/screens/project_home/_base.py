from __future__ import annotations

import asyncio
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
    from tissue.api.generated.models.issue_detail_view import IssueDetailView
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_relations_detail import (
        IssueRelationsDetail,
    )
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )
    from tissue.api.generated.models.sprint_summary import SprintSummary
    from tissue.api.generated.models.workflow_detail import WorkflowDetail
    from tissue.app import TissueApp


class ProjectHomeBase(RefreshableScreen):
    """Shared state and shared methods for the ProjectHomeScreen mixins.

    Each mixin holds the real methods for one area. They are listed here
    (under TYPE_CHECKING) so one mixin can call another and still pass type
    checks.
    """

    if TYPE_CHECKING:
        app: TissueApp

    def __init__(self, project_key: str, title: str | None = None) -> None:
        super().__init__()
        self._project_key = project_key
        self._title = title
        self._issues: list[IssueSummary] = []
        self._filter: IssueFilter = DEFAULT_ISSUE_FILTER
        self._issues_keyword: str | None = None
        self._issues_total = 0
        self._issues_page = 0
        self._issues_has_next = False
        self._issues_loading_more = False
        self._members: list[ProjectMemberSummary] = []
        # The full member list stays whole so we can look up names. This holds
        # the part the [1] table shows (filtered while searching), and row
        # clicks index into it, so the cursor and the table never get out of
        # sync.
        self._displayed_members: list[ProjectMemberSummary] = []
        self._agent_issues: list[IssueSummary] = []
        self._agent_names: dict[int, str] = {}
        # CTRL+T switches this while [3] is focused.
        #   - "work" is issues assigned to the user's agents
        #   - "reviews" is issues waiting for the user to review
        self._agent_mode = "work"
        # Which stacked box is shrunk to a thin strip, if any.
        self._collapsed_box: str | None = None
        # When expanded, [2] is hidden and Enter opens the detail modal instead.
        self._expanded = False
        self._view_mode = "issues"
        self._sprints: list[SprintSummary] = []
        # Saved so the up/down buttons can tell which issue a cursor row is.
        self._sprint_detail_id: int | None = None
        self._sprint_detail_issues: list[IssueSummary] = []
        self._sprint_open_issues: list[IssueSummary] = []
        self._detail_issue_key: str | None = None
        # Loaded detail bundles kept by issue key.
        # Revisit shows the cached one at once, then a background refetch corrects
        # anything edited.
        # An edit on this screen passes force=True to skip and refresh the cache.
        self._detail_cache: dict[str, IssueDetailView] = {}
        # Runs the [2] detail remove+mount one at a time so two fast renders
        # can't interleave and clash on a shared id. Paired with a shield in
        # _mount_detail so a cancelled render can't leave the pane half-swapped.
        self._detail_mount_lock = asyncio.Lock()
        self._detail_assigned = False
        # Saved when [2] is drawn. The assignee is left out of the reviewer
        # picker, because the backend won't let the assignee be a reviewer too.
        self._detail_reviewer_ids: list[int] = []
        self._detail_assignee_id: int | None = None
        # Reviewer changes run in their own worker group, so redrawing [2]
        # can't cancel them halfway. This flag makes overlapping clicks run one
        # by one.
        self._reviewer_busy = False
        # Saved when the reviewer picker opens, so its result is compared with
        # this set even if [2] redraws or switches issue while it is open.
        self._reviewer_picker_issue: str | None = None
        self._reviewer_picker_baseline: list[int] = []
        # The [2] issue's parent and children, loaded on their own and not part
        # of the issue data.
        #   - _hierarchy_busy makes changes run one at a time
        #   - _hier_picker_issue remembers which issue a picker is editing
        self._detail_hierarchy: str | None = None
        self._detail_children: list[IssueIdentifierResponse] = []
        self._issue_type_hierarchy: dict[int, str] = {}
        self._hierarchy_busy = False
        self._hier_picker_issue: str | None = None
        # The [2] issue's relations, loaded on their own. Same busy flag and
        # remembered-issue pattern as the hierarchy above.
        self._detail_relations: IssueRelationsDetail | None = None
        self._relations_busy = False
        self._rel_picker_issue: str | None = None
        # Which comment a new comment replies to, None for a top-level comment.
        # _reply_targets maps a top comment id to its author name, for the
        # "Replying to" banner.
        self._reply_to: int | None = None
        self._reply_targets: dict[int, str] = {}
        # Goes up by one each time the comment list is rebuilt. The posting
        # task remembers this number and skips adding its comment early if the
        # list was rebuilt in between, since the rebuild already shows it.
        self._comment_gen: int = 0
        # Short wait timers. Moving the cursor fast through a list, or typing in
        # the search box, only does its work after you pause.
        self._detail_timer: Timer | None = None
        self._search_timer: Timer | None = None
        # Current field values, saved so an edit modal can fill them in.
        self._edit_current: dict[str, str] = {}
        # Saved workflows, keyed by id, loaded only to show each transition's
        # target state.
        self._workflow_cache: dict[int, WorkflowDetail] = {}
        self._transitions_by_id: dict[int, AvailableTransition] = {}
        self._transition_current_label = "-"
        self._transition_target_labels: dict[int, str] = {}
        # state id -> #rrggbb, so the issues table can color each Status with
        # the color set in its workflow.
        self._state_colors: dict[int, str] = {}
        # Custom-field info saved so a ✎ opens the right editor.
        self._detail_custom_fields: dict[int, CustomFieldValueInfo] = {}
        self._detail_field_options: dict[int, list[FieldOptionDetail]] = {}

    if TYPE_CHECKING:

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
            self, issue_key: str, *, focus_detail: bool, force: bool = False
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
        def _reviewer_section(self, detail: IssueCommonDetail) -> list[Widget]: ...
        def _refresh_detail(self, issue_key: str) -> None: ...
        async def _ensure_issue_type_hierarchy(self) -> None: ...
        def _hierarchy_section(
            self,
            detail: IssueCommonDetail,
            parent: IssueIdentifierResponse | None,
            children: list[IssueIdentifierResponse],
        ) -> list[Widget]: ...
        def _relations_section(self, detail: IssueCommonDetail) -> list[Widget]: ...
