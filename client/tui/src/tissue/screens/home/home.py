from __future__ import annotations

import logging

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Grid, Vertical
from textual.widgets import Input
from textual_autocomplete import AutoComplete

from tissue.api.errors import TissueApiError
from tissue.screens.home.areas.details import DetailsMixin
from tissue.screens.home.areas.mywork import MyWorkMixin
from tissue.screens.home.areas.navigation import NavigationMixin
from tissue.screens.home.areas.projects import ProjectsMixin
from tissue.screens.home.areas.search import SearchMixin
from tissue.screens.home.constants import (
    _SEARCH_SIZE,
)
from tissue.screens.home.panels import (
    DashboardSearchBar,
    MyWorkPanel,
    ProjectsPanel,
    SearchResultsPanel,
)

log = logging.getLogger(__name__)


class HomeScreen(
    NavigationMixin,
    DetailsMixin,
    MyWorkMixin,
    SearchMixin,
    ProjectsMixin,
):
    """Dashboard landing screen."""

    CSS_PATH = "home.tcss"

    BINDINGS = [
        Binding("1", f"focus_box('{SearchResultsPanel.BOX_ID}')", show=False),
        Binding("2", f"focus_box('{MyWorkPanel.BOX_ID}')", show=False),
        Binding("3", f"focus_box('{ProjectsPanel.BOX_ID}')", show=False),
        Binding("ctrl+1", f"focus_box('{SearchResultsPanel.BOX_ID}')", show=False),
        Binding("ctrl+2", f"focus_box('{MyWorkPanel.BOX_ID}')", show=False),
        Binding("ctrl+3", f"focus_box('{ProjectsPanel.BOX_ID}')", show=False),
        Binding("h", "nav('h')", show=False),
        Binding("l", "nav('l')", show=False),
        Binding("c", "create_project", "create project"),
        Binding("p", "toggle_pin", "pin/unpin"),
        Binding("slash", "focus_search", "search", key_display="/"),
        Binding("ctrl+underscore,ctrl+slash", "focus_search", show=False),
        Binding("escape", "leave_search", show=False),
    ]

    _BOX_IDS = (
        SearchResultsPanel.BOX_ID,
        MyWorkPanel.BOX_ID,
        ProjectsPanel.BOX_ID,
    )

    def compose_content(self) -> ComposeResult:
        with Vertical(id="screen-body"):
            search = Input(
                placeholder="/project:<kw>   /issue:<kw>",
                id="dashboard-search",
            )
            search.border_title = "Search"
            yield DashboardSearchBar(search)
            yield AutoComplete(search, candidates=self._search_candidates)
            with Grid(id="dashboard-grid"):
                yield SearchResultsPanel(self._searched_widgets())
                yield self._detail_box()
                yield MyWorkPanel(self._mywork_widgets())
                yield ProjectsPanel(self._projects_widgets())

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_dashboard(), exclusive=True, group="dashboard-load")

    async def refresh_data(self) -> None:
        self._cancel_search_timer()
        self._search.invalidate()
        self._search.clear_results()
        await self._load_dashboard()

    async def _load_dashboard(self) -> None:
        client = self.app.client
        if client is None:
            return
        await self._fetch_projects()
        try:
            mywork_page = await client.issues.my_work(size=_SEARCH_SIZE)
            self._my_work.items = list(mywork_page.content or [])
        except TissueApiError as error:
            log.debug("Dashboard: failed to load my work: %s", error)
            self._my_work.items = []
        await self._load_header_stats()
        await self._load_state_colors()
        self.refresh(recompose=True)
        self.call_after_refresh(self._focus_after_load)

    async def _load_header_stats(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            projects = await client.projects.list_projects(
                size=1, include_archived=False
            )
            assigned = await client.issues.my_assigned_total(
                state_categories=["INITIAL", "ACTIVE"]
            )
        except TissueApiError as error:
            log.debug("Dashboard: failed to load header stats: %s", error)
            return
        count = projects.total_elements or 0
        self.set_top_bar_status(f"{count} projects · total {assigned} assigned")
