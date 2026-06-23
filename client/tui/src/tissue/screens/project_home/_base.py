from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.screens.base import RefreshableScreen

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
        self._members: list[ProjectMemberSummary] = []
        # [3] box: issues across this project assigned to agents the user owns,
        # plus an agent member-id -> display-name map for the Assignee column.
        self._agent_issues: list[IssueSummary] = []
        self._agent_names: dict[int, str] = {}
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
        # The [2] detail render is debounced so flying the cursor through a list
        # (holding ↓) doesn't fire a full render (several fetches + a mount) for
        # every row it passes over — only the row the cursor settles on renders.
        # Holds the pending settle timer (stopped/replaced on each new highlight).
        self._detail_timer: Timer | None = None
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
        async def _load_agent_issues(self) -> None: ...
        def action_focus_agent_issues(self) -> None: ...
        def _update_create_button(self) -> None: ...
        def _refresh_box_chrome(self) -> None: ...
        def _open_issue_modal(self, issue_key: str) -> None: ...
        def _ensure_not_expanded(self) -> None: ...
        def _debounce_detail(
            self, render: Callable[[], object], *, immediate: bool
        ) -> None: ...
        def _cancel_detail_timer(self) -> None: ...
        def _open_create_sprint(self) -> None: ...
        async def _load_sprints(self) -> None: ...
        async def _load_members(self) -> None: ...
        async def _load_members_list(self) -> None: ...
        async def _clear_timeline(self) -> None: ...
        def _set_view_chrome(self, mode: str) -> None: ...
        def _run_view_load(self, mode: str, *, focus_list: bool = False) -> None: ...
        def action_focus_issues(self) -> None: ...
        async def _render_issue_detail(
            self, issue_key: str, *, focus_detail: bool
        ) -> None: ...
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
