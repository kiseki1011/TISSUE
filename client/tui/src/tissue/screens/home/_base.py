from __future__ import annotations

from typing import TYPE_CHECKING

from textual.css.query import NoMatches

from tissue.screens.base import RefreshableScreen
from tissue.screens.home.panels import DashboardBox, DashboardDetailPanel
from tissue.screens.home.state import (
    DashboardMyWorkState,
    DashboardProjectState,
    DashboardSearchState,
    DashboardWorkflowState,
)

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.project_summary import ProjectSummary
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
        self._projects = DashboardProjectState()
        self._my_work = DashboardMyWorkState()
        self._search = DashboardSearchState()
        self._workflows = DashboardWorkflowState()

    def _dashboard_box(self, box_id: str) -> DashboardBox | None:
        try:
            return self.query_one(f"#{box_id}", DashboardBox)
        except NoMatches:
            return None

    def _detail_panel(self) -> DashboardDetailPanel | None:
        try:
            return self.query_one(DashboardDetailPanel)
        except NoMatches:
            return None

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
        def _detail_box(self) -> DashboardDetailPanel: ...
        def _cancel_search_timer(self) -> None: ...
        def _searched_widgets(self) -> list[Widget]: ...
        def _projects_widgets(self) -> list[Widget]: ...
        def _mywork_widgets(self) -> list[Widget]: ...
        def _render_project_detail(
            self, project: ProjectSummary, *, show_open_hint: bool = False
        ) -> None: ...
        async def _render_issue_detail(self, issue_key: str) -> None: ...
        async def _load_state_colors(self) -> None: ...
