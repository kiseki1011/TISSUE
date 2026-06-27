from __future__ import annotations

from typing import TYPE_CHECKING

from tissue.screens.base import RefreshableScreen

if TYPE_CHECKING:
    from textual.containers import Vertical, VerticalScroll
    from textual.timer import Timer
    from textual.widget import Widget

    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.project_summary import ProjectSummary
    from tissue.api.generated.models.workflow_detail import WorkflowDetail
    from tissue.app import TissueApp


class HomeScreenBase(RefreshableScreen):
    """Shared base for the HomeScreen area mixins.

    Holds the screen's shared state (`__init__`) and, under `TYPE_CHECKING`, the
    cross-area method contract each mixin type-checks against. Real bodies live on
    whichever mixin owns the area, and every mixin inherits this base.
    """

    if TYPE_CHECKING:
        app: TissueApp
        # The box ids in nav order. The real value lives on HomeScreen.
        _BOX_IDS: tuple[str, ...]

    def __init__(self) -> None:
        super().__init__()
        self._projects: list[ProjectSummary] | None = None
        self._my_work: list[IssueSummary] | None = None
        self._search_type: str | None = None
        self._search_results: list[ProjectSummary] | list[IssueSummary] | None = None
        # The keyword behind the current results, for title highlighting.
        self._search_keyword = ""
        # Goes up by one on every search/reset/refresh so a slow in-flight search
        # whose result lands late can't clobber a newer search or a reset.
        self._search_gen = 0
        self._search_timer: Timer | None = None
        # The search kind ("project"/"issue") whose table is currently mounted in
        # the Searched Items box, or None when a placeholder Static is shown. Lets
        # `_render_searched` refill rows in place when only the rows changed (same
        # kind means same columns) instead of remounting the whole table, which
        # would make the column header flicker on every keystroke.
        self._searched_table_kind: str | None = None
        # Maps state-id to #rrggbb, collected from every workflow so the dashboard's
        # issue tables (My Work + issue search) can tint each Status with its
        # workflow-defined color, the same colors the project hub uses.
        self._state_colors: dict[int, str] = {}
        # Workflow graphs cached by id, fetched only to collect the state colors
        # above. They barely change and several issues share one.
        self._workflow_cache: dict[int, WorkflowDetail] = {}

    if TYPE_CHECKING:
        # Cross-area methods, each implemented by the mixin that owns the area and
        # called from others. Declared here so every mixin type-checks against them.
        async def _fetch_projects(self) -> None: ...
        def _focus_after_load(self) -> None: ...
        def _focus_box(self, box_id: str) -> None: ...
        def _select_project(self, index: int) -> None: ...
        def _select_mywork(self, index: int) -> None: ...
        def _select_searched(self, idx: int) -> None: ...
        def _preview_focused_row(self, box_id: str, row: int) -> None: ...
        def _current_box_id(self) -> str | None: ...
        def _box(self, title: str, box_id: str, children: list[Widget]) -> Vertical: ...
        def _detail_box(self) -> VerticalScroll: ...
        def _cancel_search_timer(self) -> None: ...
        def _searched_widgets(self) -> list[Widget]: ...
        def _projects_widgets(self) -> list[Widget]: ...
        def _mywork_widgets(self) -> list[Widget]: ...
        def _render_project_detail(
            self, project: ProjectSummary, *, show_open_hint: bool = False
        ) -> None: ...
        async def _render_issue_detail(self, issue_key: str) -> None: ...
        async def _load_state_colors(self) -> None: ...
