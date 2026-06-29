from __future__ import annotations

from typing import TYPE_CHECKING

from textual.css.query import NoMatches

from tissue.screens.base import RefreshableScreen
from tissue.screens.project_home.panels import (
    AgentWorkPanel,
    IssueDetailPanel,
    IssueListPanel,
)
from tissue.screens.project_home.state import (
    AgentWorkState,
    IssueCommentState,
    IssueDetailState,
    IssueHierarchyState,
    IssueListState,
    IssueRelationState,
    IssueReviewState,
    ProjectHomeFilters,
    ProjectHomeUiState,
    ProjectMemberListState,
    SprintState,
)

if TYPE_CHECKING:
    from collections.abc import Callable

    from textual.timer import Timer
    from textual.widget import Widget

    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.comment_detail_response import (
        CommentDetailResponse,
    )
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.sprint_summary import SprintSummary
    from tissue.api.generated.models.workflow_detail import WorkflowDetail
    from tissue.app import TissueApp


class ProjectHomeBase(RefreshableScreen):
    """Shared state for ProjectHome mixins."""

    if TYPE_CHECKING:
        app: TissueApp

    def __init__(self, project_key: str, title: str | None = None) -> None:
        super().__init__()
        self._project_key = project_key
        self._title = title
        self._init_list_state()
        self._init_sprint_state()
        self._init_detail_state()
        self._init_review_state()
        self._init_hierarchy_state()
        self._init_relation_state()
        self._init_comment_state()
        self._init_timer_state()
        self._init_rendering_state()

    def _init_list_state(self) -> None:
        self._filters = ProjectHomeFilters()
        self._issue_list = IssueListState()
        self._member_list = ProjectMemberListState()
        self._agent_work = AgentWorkState()
        self._ui = ProjectHomeUiState()

    def _init_sprint_state(self) -> None:
        self._sprint_state = SprintState()

    def _init_detail_state(self) -> None:
        self._detail_state = IssueDetailState()

    def _init_review_state(self) -> None:
        self._review_state = IssueReviewState()

    def _init_hierarchy_state(self) -> None:
        self._hierarchy_state = IssueHierarchyState()

    def _init_relation_state(self) -> None:
        self._relation_state = IssueRelationState()

    def _init_comment_state(self) -> None:
        self._comment_state = IssueCommentState()

    def _init_timer_state(self) -> None:
        self._detail_timer: Timer | None = None
        self._search_timer: Timer | None = None

    def _init_rendering_state(self) -> None:
        self._workflow_cache: dict[int, WorkflowDetail] = {}
        self._transitions_by_id: dict[int, AvailableTransition] = {}
        self._transition_current_label = "-"
        self._transition_target_labels: dict[int, str] = {}
        self._state_colors: dict[int, str] = {}

    def _detail_panel(self) -> IssueDetailPanel | None:
        try:
            return self.query_one(IssueDetailPanel)
        except NoMatches:
            return None

    def _issue_list_panel(self) -> IssueListPanel | None:
        try:
            return self.query_one(IssueListPanel)
        except NoMatches:
            return None

    def _agent_work_panel(self) -> AgentWorkPanel | None:
        try:
            return self.query_one(AgentWorkPanel)
        except NoMatches:
            return None

    if TYPE_CHECKING:

        async def _load_issues(self, keyword: str | None = None) -> None: ...
        async def _load_agent_issues(self, *, focus_list: bool = False) -> None: ...
        def _toggle_agent_mode(self) -> None: ...
        def action_focus_agent_issues(self) -> None: ...
        def _current_hub_box(self) -> str | None: ...
        def _update_create_button(self) -> None: ...
        def _update_filter_button(self) -> None: ...
        def _search_keyword(self) -> str | None: ...
        def _refresh_box_chrome(self) -> None: ...
        def _restore_project_ui(self) -> None: ...
        def _persist_project_ui(self) -> None: ...
        def _open_issue_modal(self, issue_key: str) -> None: ...
        def _select_issue(self, index: int, *, focus_detail: bool = False) -> None: ...
        def _debounce_detail(
            self, render: Callable[[], object], *, immediate: bool
        ) -> None: ...
        def _cancel_detail_timer(self) -> None: ...
        def _open_create_sprint(self) -> None: ...
        def _is_project_manager(self) -> bool: ...
        async def _render_sprint_detail(
            self, sprint_id: int, *, focus_detail: bool
        ) -> None: ...
        async def _load_sprints(self) -> None: ...
        async def _ensure_sprints_loaded(self) -> None: ...
        async def _ensure_sprint_index(self) -> None: ...
        def _active_sprint(self) -> SprintSummary | None: ...
        async def _add_issue_to_active_sprint(self, issue_key: str) -> None: ...
        async def _load_members(self) -> None: ...
        async def _ensure_members_loaded(self) -> None: ...
        async def _load_members_list(self, keyword: str | None = None) -> None: ...
        async def _render_members_list(self, keyword: str | None = None) -> None: ...
        def _select_member(
            self, row_index: int, *, focus_detail: bool = False
        ) -> None: ...
        def _update_search_input(self) -> None: ...
        def _cancel_search_timer(self) -> None: ...
        async def _clear_timeline(self) -> None: ...
        def _set_view_chrome(self, mode: str) -> None: ...
        def _run_view_load(self, mode: str, *, focus_list: bool = False) -> None: ...
        def action_focus_issues(self) -> None: ...
        def action_focus_detail(self) -> None: ...
        def _focus_detail_body(self) -> None: ...
        async def _render_issue_detail(
            self, issue_key: str, *, focus_detail: bool, force: bool = False
        ) -> None: ...
        async def _prefetch_issue_details(self, issue_keys: list[str]) -> None: ...
        async def _reset_detail_pane(self) -> None: ...
        async def _mount_detail(self, widgets: list[Widget]) -> None: ...
        async def _load_activity(self, issue_key: str) -> None: ...
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
