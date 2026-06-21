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

log = logging.getLogger(__name__)


class HomeScreen(
    NavigationMixin,
    DetailsMixin,
    MyWorkMixin,
    SearchMixin,
    ProjectsMixin,
):
    """Dashboard landing screen.

    Top is a search bar (/project: /issue:)
    [1] Searched Items  | Details
    [2] My Work         | (Details, row-span 2)
    [2] My Work         | [3] Projects
    """

    CSS_PATH = "home.tcss"

    # Number keys jump to a box.
    # h/l cycle through the boxes (1→2→3→1).
    # j/k (and the arrows) move rows inside the focused table.
    # c / p create a project / toggle pin while the [3] Projects box is focused.
    BINDINGS = [
        Binding("1", "focus_box('dash-searched')", show=False),
        Binding("2", "focus_box('dash-mywork')", show=False),
        Binding("3", "focus_box('dash-projects-box')", show=False),
        # ctrl+digit does the same jump but also works while the search input has
        # focus (a plain digit is typed into the input there, never reaching the
        # screen binding — see Textual's focused-widget-first key dispatch).
        Binding("ctrl+1", "focus_box('dash-searched')", show=False),
        Binding("ctrl+2", "focus_box('dash-mywork')", show=False),
        Binding("ctrl+3", "focus_box('dash-projects-box')", show=False),
        Binding("h", "nav('h')", show=False),
        Binding("l", "nav('l')", show=False),
        Binding("c", "create_project", "create project"),
        Binding("p", "toggle_pin", "pin/unpin"),
        # ctrl+/ — terminals send it as ctrl+underscore (0x1F); the kitty keyboard
        # protocol sends it as ctrl+slash. Bind both, display as ctrl+/.
        Binding(
            "ctrl+underscore,ctrl+slash",
            "focus_search",
            "search",
            key_display="ctrl+/",
        ),
    ]

    _BOX_IDS = (
        "dash-searched",
        "dash-mywork",
        "dash-projects-box",
    )

    def top_bar_breadcrumb(self) -> str:
        return "Dashboard"

    def compose_content(self) -> ComposeResult:
        with Vertical(id="screen-body"):
            search = Input(
                placeholder="/project:<kw>   /issue:<kw>",
                id="dashboard-search",
            )
            search.border_title = "Search"
            yield search
            yield AutoComplete(search, candidates=self._search_candidates)
            with Grid(id="dashboard-grid"):
                yield self._box(
                    "[1] Searched Items", "dash-searched", self._searched_widgets()
                )
                yield self._detail_box()
                yield self._box("[2] My Work", "dash-mywork", self._mywork_widgets())
                yield self._box(
                    "[3] Projects", "dash-projects-box", self._projects_widgets()
                )

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_dashboard(), exclusive=True, group="dashboard-load")

    async def refresh_data(self) -> None:
        self._cancel_search_timer()  # drop any pending live-search keystroke
        self._search_gen += 1  # invalidate any in-flight search
        # A full refresh recomposes (clearing the search input), so clear the
        # search results too — otherwise the Searched Items box would keep stale
        # (highlighted) results while the input reads empty.
        self._search_results = None
        self._search_type = None
        self._search_keyword = ""
        self._searched_table_kind = None  # recompose drops the mounted table
        await self._load_dashboard()

    async def _load_dashboard(self) -> None:
        client = self.app.client
        if client is None:
            return
        # [2] Projects
        await self._fetch_projects()
        # [3] My Work
        try:
            mywork_page = await client.issues.my_work(size=_SEARCH_SIZE)
            self._my_work = list(mywork_page.content or [])
        except TissueApiError as e:
            log.debug("Dashboard: failed to load my work: %s", e)
            self._my_work = []
        self.refresh(recompose=True)
        # Land on a data box so the nav keys (1-4/h/l/j/k) work right away
        self.call_after_refresh(self._focus_after_load)
