from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home._base import HomeScreenBase

if TYPE_CHECKING:
    from tissue.api.generated.models.project_summary import ProjectSummary
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
    """[2] Projects box that loads, sorts, pins, creates, and opens projects."""

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
        for project in self._projects:
            marker = "📌 " if self._is_pinned(project.key) else ""
            cells = [
                _fit(project.key or "-", _PROJECT_KEY_WIDTH),
                marker + _truncate(project.title or "-"),
                _visibility_label(project.visibility),
                format_date(project.created_at),
            ]
            if project.archived:
                rows.append([Text(cell, style="dim") for cell in cells])
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
        """Load [2] Projects including archived into `_projects`, pinned first."""
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.projects.list_projects(size=100, include_archived=True)
            self._projects = list(page.content or [])
            self._sort_projects()
        except TissueApiError as error:
            log.debug("Dashboard: failed to load projects: %s", error)
            self._projects = []

    async def _render_projects(self, *, focus_key: str | None = None) -> None:
        """Re-render only the [2] Projects box after a pin toggle or create.

        Rebuilding from scratch resets the cursor to row 0, and the new table
        posts RowHighlighted before focus lands (the preview ignores it, since
        it only fires when the table has focus). So after mounting we restore the
        cursor to `focus_key` (whose index shifts when pinned projects move up)
        and drive the detail preview ourselves to keep it on the acted-on
        project.
        """
        try:
            box = self.query_one("#dash-projects-box")
        except NoMatches:
            return
        # The table has a fixed id, so mounting a new one before the old is gone
        # raises DuplicateIds.
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
                (
                    index
                    for index, project in enumerate(self._projects)
                    if project.key == focus_key
                ),
                0,
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
        self._projects.sort(key=lambda project: (project.key or "") not in pinned)

    @on(DataTable.RowHighlighted, "#dash-projects")
    def _on_project_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_project(event.cursor_row)

    @on(DataTable.RowSelected, "#dash-projects")
    def _on_project_selected(self, event: DataTable.RowSelected) -> None:
        self._open_project(event.cursor_row)

    def _select_project(self, index: int) -> None:
        if self._projects and 0 <= index < len(self._projects):
            # Drop a pending issue-detail render (from My Work / search) so a slow
            # one can't land on top of this project's detail after a box switch.
            self.workers.cancel_group(self, "dash-detail")
            self._render_project_detail(self._projects[index], show_open_hint=True)

    def _open_project(self, index: int) -> None:
        if not self._projects or not (0 <= index < len(self._projects)):
            return
        project = self._projects[index]
        if not project.key:
            return
        # Ensure membership off the UI thread, joining a PUBLIC project the user
        # isn't yet in. The hub only opens once entry is granted.
        self.run_worker(
            self._enter_project(project), exclusive=True, group="open-project"
        )

    async def _enter_project(self, project: ProjectSummary) -> None:
        if self.app.client is None or not project.key:
            return
        name = project.title or project.key
        if not await self._ensure_membership(project.key, name):
            return
        from tissue.screens.project_home.project_home import ProjectHomeScreen

        self.app.push_screen(ProjectHomeScreen(project.key, title=project.title))

    async def _ensure_membership(self, project_key: str, name: str) -> bool:
        """Whether the user may enter the project.

        Returns True when already a member or after a successful auto-join.
        Notifies and returns False when a join is refused, for a PRIVATE project
        the user has no access to. Membership is probed with a 1-row member list,
        which the server 404s for a non-member. The join-permission check is the
        one that decides, including the system-admin override, so we never
        pre-decide from visibility.
        """
        client = self.app.client
        if client is None:
            return False
        try:
            await client.project_members.list_project_members(project_key, size=1)
            return True
        except TissueApiError as error:
            if error.status != 404:
                # Couldn't determine membership on a transient or other error, so
                # let them in rather than false-bounce. The hub surfaces its own
                # load errors.
                return True
        # A 404 from the probe means not a member, so try to join.
        try:
            await client.project_members.join_project(project_key)
        except TissueApiError as error:
            if error.status == 403:
                message = f"{name} is private — you don't have access."
            elif error.status is None:
                # No status means a connection/timeout failure, not an access
                # denial, so don't imply a permissions problem.
                message = f"Couldn't reach the server to join {name}."
            else:
                detail = error.detail or "please try again"
                message = f"Couldn't join {name}: {detail}."
            self.app.notify(message, severity="error")
            return False
        self.app.notify(f"Joined {name}.")
        return True

    def _projects_box_focused(self) -> bool:
        return self._current_box_id() == "dash-projects-box"

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        """Show `c` / `p` in the footer only while the [2] Projects box is focused."""
        if action in ("create_project", "toggle_pin"):
            return self._projects_box_focused()
        # Every other action stays enabled. We deliberately don't delegate to
        # super(), which would end the check_action chain for the dashboard.
        return True

    def action_create_project(self) -> None:
        if not self._projects_box_focused():
            return
        from tissue.screens.home.modals.create_project_modal import CreateProjectModal

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
        index = table.cursor_row
        if not (0 <= index < len(self._projects)):
            return
        key = self._projects[index].key
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
