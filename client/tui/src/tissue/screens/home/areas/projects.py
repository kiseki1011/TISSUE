from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.constants import (
    _PROJECT_KEY_WIDTH,
)
from tissue.screens.home.rendering import (
    _fit,
    _truncate,
    _visibility_label,
)
from tissue.screens.home.widgets import _DashTable
from tissue.util.datetime_fmt import format_date

log = logging.getLogger(__name__)


class ProjectsMixin(HomeScreenBase):
    """[2] Projects box: load/sort/pin/create/open projects."""

    def _projects_widgets(self) -> list[Widget]:
        if self._projects is None:
            return [Static("Loading…", classes="dashboard-muted")]
        if not self._projects:
            return [
                Static(
                    "No projects yet — press c to create.",
                    classes="dashboard-muted",
                )
            ]
        rows: list[list[str | Text]] = []
        for p in self._projects:
            marker = "📌 " if self._is_pinned(p.key) else ""
            cells = [
                _fit(p.key or "-", _PROJECT_KEY_WIDTH),
                marker + _truncate(p.title or "-"),
                _visibility_label(p.visibility),
                format_date(p.created_at),
            ]
            if p.archived:
                rows.append([Text(c, style="dim") for c in cells])
            else:
                rows.append([cells[0], Text(cells[1]), cells[2], cells[3]])
        return [
            _DashTable(
                [
                    ("Key", _PROJECT_KEY_WIDTH),
                    ("Title", None),
                    ("Visibility", 10),
                    ("Created", 10),
                ],
                rows,
                id="dash-projects",
                classes="dashboard-table",
            )
        ]

    async def _fetch_projects(self) -> None:
        """Load [2] Projects (including archived) into `_projects`, pinned first."""
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.projects.list_projects(size=100, include_archived=True)
            self._projects = list(page.content or [])
            self._sort_projects()
        except TissueApiError as e:
            log.debug("Dashboard: failed to load projects: %s", e)
            self._projects = []

    async def _render_projects(self, *, focus_key: str | None = None) -> None:
        """Re-render only the [2] Projects box (after a pin toggle or create).

        Rebuilds the table from scratch, so its cursor resets to row 0 and the
        freshly-mounted table posts RowHighlighted before focus lands (the
        has_focus-gated preview misses it). After mounting we restore the cursor
        to `focus_key` — whose index shifts when pinned projects float up — and
        drive the detail preview explicitly so it stays on the acted-on project.
        """
        try:
            box = self.query_one("#dash-projects-box")
        except NoMatches:
            return
        # Await the removal: the table has a fixed id, so mounting a new one
        # before the old is gone would raise DuplicateIds.
        await box.remove_children()
        await box.mount(*self._projects_widgets())
        self.call_after_refresh(self._after_projects_render, focus_key)

    def _after_projects_render(self, focus_key: str | None) -> None:
        self._focus_box("dash-projects-box")
        try:
            table = self.query_one("#dash-projects", DataTable)
        except NoMatches:
            return
        if not self._projects or not table.row_count:
            return
        row = 0
        if focus_key is not None:
            row = next(
                (i for i, p in enumerate(self._projects) if p.key == focus_key), 0
            )
            table.move_cursor(row=row, animate=False)
        self._select_project(row)

    def _pinned_keys(self) -> set[str]:
        server = self.app.config.state.current_server_url or ""
        return set(self.app.config.pinned_project_keys(server))

    def _is_pinned(self, key: str | None) -> bool:
        return bool(key) and key in self._pinned_keys()

    def _sort_projects(self) -> None:
        """Float pinned projects to the top, preserving server order otherwise."""
        if not self._projects:
            return
        pinned = self._pinned_keys()
        self._projects.sort(key=lambda p: (p.key or "") not in pinned)

    @on(DataTable.RowHighlighted, "#dash-projects")
    def _on_project_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_project(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-projects")
    def _on_project_selected(self, event: DataTable.RowSelected) -> None:
        self._open_project(event.cursor_row)

    def _select_project(self, idx: int) -> None:
        if self._projects and 0 <= idx < len(self._projects):
            self._render_project_detail(self._projects[idx], show_open_hint=True)

    def _open_project(self, idx: int) -> None:
        if not self._projects or not (0 <= idx < len(self._projects)):
            return
        project = self._projects[idx]
        if not project.key:
            return
        from tissue.screens.project_home.project_home import ProjectHomeScreen

        self.app.push_screen(ProjectHomeScreen(project.key, title=project.title))

    def _projects_box_focused(self) -> bool:
        return self._current_box_id() == "dash-projects-box"

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        """Show `c` / `p` in the footer only while the [2] Projects box is focused."""
        if action in ("create_project", "toggle_pin"):
            return self._projects_box_focused()
        # Every other action stays enabled; we deliberately don't delegate to
        # super() (this terminates the check_action chain for the dashboard).
        return True

    def action_create_project(self) -> None:
        if not self._projects_box_focused():
            return
        from tissue.screens.home.create_project_modal import CreateProjectModal

        self.app.push_screen(CreateProjectModal(), self._on_project_created)

    def _on_project_created(self, created_key: str | None) -> None:
        if created_key:
            self.run_worker(
                self._reload_projects(), exclusive=True, group="dashboard-projects"
            )

    async def _reload_projects(self) -> None:
        await self._fetch_projects()
        await self._render_projects()

    def action_toggle_pin(self) -> None:
        if not self._projects_box_focused() or not self._projects:
            return
        try:
            table = self.query_one("#dash-projects", DataTable)
        except NoMatches:
            return
        idx = table.cursor_row
        if not (0 <= idx < len(self._projects)):
            return
        key = self._projects[idx].key
        if not key:
            return
        server = self.app.config.state.current_server_url or ""
        self.app.config.toggle_pinned_project(server, key)
        self._sort_projects()
        self.run_worker(
            self._render_projects(focus_key=key),
            exclusive=True,
            group="dashboard-projects",
        )
