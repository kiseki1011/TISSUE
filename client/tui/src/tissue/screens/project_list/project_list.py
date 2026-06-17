from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Vertical
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import (
    Button,
    Checkbox,
    DataTable,
    Input,
    LoadingIndicator,
    Static,
)

from tissue.api.errors import ConnectionFailed, ServerError, TissueApiError
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.screens.base import RefreshableScreen
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.search_bar import SearchBar
from tissue.widgets.table_detail_split_view import Column, TableDetailSplitView
from tissue.widgets.text_button import TextButton

log = logging.getLogger(__name__)


class ProjectListScreen(RefreshableScreen):
    """Project picker: list + detail split view.

    Search, an archived filter, client-side pinning, archive/unarchive and
    create. Reached from the dashboard and the command palette ("Projects").
    """

    CSS_PATH = "project_list.tcss"

    def __init__(self) -> None:
        super().__init__()
        self._projects: list[ProjectSummary] | None = None
        self._project_error: str | None = None
        self._project_query = ""
        self._include_archived = False
        self._selected_project: ProjectSummary | None = None

    def top_bar_breadcrumb(self) -> str:
        return "Projects"

    def compose_content(self) -> ComposeResult:
        with Container(id="screen-body"):
            yield from self._compose_body()

    def _compose_body(self) -> ComposeResult:
        if self._project_error is not None:
            yield self._status_panel(
                Static(self._project_error, classes="status-error")
            )
            return
        if self._projects is None:
            yield self._status_panel(LoadingIndicator())
            return
        # Truly empty (no projects AND no active search/filter) → first-run CTA.
        no_filter = not self._project_query and not self._include_archived
        if not self._projects and no_filter:
            yield self._status_panel(
                Static("No projects yet", classes="status-title"),
                Static(
                    "You don't have any projects yet. Create your first one?",
                    classes="status-sub",
                ),
                Button(
                    "+ New project",
                    id="project-create-btn",
                    classes="-btn-success",
                ),
            )
            return
        with Vertical(id="project-main"):
            toggle = Checkbox(
                "Archived",
                value=self._include_archived,
                id="project-archived-toggle",
            )
            yield SearchBar(
                toggle,
                input_id="project-search",
                placeholder="Search projects…",
                value=self._project_query,
            )
            if not self._projects:
                yield Static("No matching projects.", classes="detail-empty")
            else:
                yield TableDetailSplitView(
                    columns=[
                        Column("status", "", 3),
                        Column("key", "Key", 12),
                        Column("title", "Title"),
                        Column("visibility", "Visibility", 12),
                        Column("archived", "Archived", 10),
                    ],
                    row_builder=self._row,
                    detail_renderer=self._render_detail,
                    items=self._projects,
                    id="project-split",
                    table_title="Projects",
                    detail_title="Details",
                )
        if self._projects:
            self.call_after_refresh(self._focus_table)
        else:  # no matches — keep focus in the search box to refine/clear
            self.call_after_refresh(self._focus_project_search)

    def _status_panel(self, *children: Widget) -> Vertical:
        """Bordered panel for the non-loaded states (loading / error / empty),
        so chrome stays consistent with the loaded split view."""
        panel = Vertical(*children, classes="panel project-status")
        panel.border_title = "Projects"
        return panel

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    async def refresh_data(self) -> None:
        await self._load_projects()

    async def _load_projects(self) -> None:
        client = self.app.client
        if client is None:
            log.error("Project list load attempted but TissueClient is not set")
            return
        # Launched as an exclusive worker (group "project-load"), which cancels
        # any in-flight load so the newest query/filter always wins.
        try:
            page = await client.projects.list_projects(
                size=100,
                keyword=self._project_query or None,
                include_archived=self._include_archived,
            )
        except ConnectionFailed:
            self._set_project_error("Cannot reach server. Press r to retry.")
            return
        except ServerError:
            self._set_project_error("Server error. Press r to retry.")
            return
        except TissueApiError as e:
            log.warning("Failed to load projects: %s", e)
            self._set_project_error("Failed to load projects. Press r to retry.")
            return

        self._project_error = None
        self._projects = list(page.content or [])
        self._sort_projects()
        self.refresh(recompose=True)

    def _set_project_error(self, message: str) -> None:
        self._project_error = message
        self.refresh(recompose=True)
        self.app.notify(message, severity="error", timeout=5)

    def _row(self, idx: int, project: ProjectSummary) -> list[str | Text]:
        archived = bool(project.archived)
        pin = "📌" if self._is_pinned(project.key) else ""
        key = project.key or "-"
        title = project.title or "-"
        visibility = self._visibility_label(project.visibility)
        archived_label = "ARCHIVED" if archived else "-"
        if archived:  # de-emphasize the whole row
            return [
                Text(pin, style="dim"),
                Text(key, style="dim"),
                Text(title, style="dim"),
                Text(visibility, style="dim"),
                Text(archived_label, style="dim"),
            ]
        return [pin, key, Text(title), visibility, archived_label]

    def _render_detail(
        self, project: ProjectSummary | None, content: Container, actions: Container
    ) -> None:
        self._selected_project = project
        content.remove_children()
        if project is None:
            content.mount(Static("No project selected.", classes="detail-empty"))
        else:
            content.mount(self._build_detail(project))
        # Buttons are mounted once (stable ids); per-row state is synced below to
        # avoid remove/mount churn (and DuplicateIds) on every row highlight.
        if not actions.children:
            actions.mount(
                TextButton("+ New project", id="project-create-btn"),
                TextButton("Pin", id="project-pin-btn"),
                TextButton("Archive", id="project-archive-btn"),
            )
        self._sync_project_action_buttons(project)

    def _sync_project_action_buttons(self, project: ProjectSummary | None) -> None:
        try:
            pin_btn = self.query_one("#project-pin-btn", Button)
            arch_btn = self.query_one("#project-archive-btn", Button)
        except NoMatches:
            return
        if project is None or not project.key:
            pin_btn.disabled = True
            arch_btn.disabled = True
            return
        pin_btn.disabled = False
        arch_btn.disabled = False
        pin_btn.label = "Unpin" if self._is_pinned(project.key) else "Pin"
        arch_btn.label = "Unarchive" if project.archived else "Archive"

    def _build_detail(self, project: ProjectSummary) -> Vertical:
        return Vertical(
            Static(project.title or "-", markup=False, classes="detail-title"),
            detail_row("Key", project.key or "-"),
            detail_row("Visibility", self._visibility_label(project.visibility)),
            detail_row("Created", format_relative(project.created_at)),
            detail_row("Updated", format_relative(project.last_updated_at)),
            Static(
                project.description or "No description.",
                markup=False,
                classes="detail-desc",
            ),
            Static("Press Enter to open", classes="detail-hint"),
            classes="detail-body",
        )

    def _focus_table(self) -> None:
        try:
            self.query_one("#project-split").query_one(DataTable).focus()
        except NoMatches:
            pass

    def _focus_project_search(self) -> None:
        try:
            self.query_one("#project-search", Input).focus()
        except NoMatches:
            pass

    @on(DataTable.RowSelected)
    def _on_row_selected(self, event: DataTable.RowSelected) -> None:
        idx = event.cursor_row
        if self._projects and 0 <= idx < len(self._projects):
            self._open_project(self._projects[idx].key)

    def _open_project(self, project_key: str | None) -> None:
        if not project_key:
            return
        from tissue.screens.project_home.project_home import ProjectHomeScreen

        title = next(
            (p.title for p in (self._projects or []) if p.key == project_key), None
        )
        self.app.push_screen(ProjectHomeScreen(project_key, title=title))

    @on(Button.Pressed, "#project-create-btn")
    def _on_create_button(self) -> None:
        self._open_create_modal()

    def _open_create_modal(self) -> None:
        from tissue.screens.home.create_project_modal import CreateProjectModal

        self.app.push_screen(CreateProjectModal(), self._on_project_created)

    def _on_project_created(self, created_key: str | None) -> None:
        if created_key:
            self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @on(Input.Submitted, "#project-search")
    def _on_project_search(self, event: Input.Submitted) -> None:
        self._project_query = event.value.strip()
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @on(Checkbox.Changed, "#project-archived-toggle")
    def _on_archived_toggle(self, event: Checkbox.Changed) -> None:
        self._include_archived = event.value
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @on(Button.Pressed, "#project-pin-btn")
    def _on_pin_button(self) -> None:
        project = self._selected_project
        if project is not None and project.key:
            self._toggle_pin(project.key)

    @on(Button.Pressed, "#project-archive-btn")
    def _on_archive_button(self) -> None:
        project = self._selected_project
        if project is None or not project.key:
            return
        key = project.key
        if project.archived:  # unarchive only reveals it again → no confirm needed
            self.run_worker(
                self._toggle_archive(key, False),
                exclusive=True,
                group="project-archive",
            )
            return

        from tissue.screens.confirm_modal import ConfirmModal

        def _after(confirmed: bool | None) -> None:
            if confirmed:
                self.run_worker(
                    self._toggle_archive(key, True),
                    exclusive=True,
                    group="project-archive",
                )

        self.app.push_screen(
            ConfirmModal(
                title="Archive project",
                message=(
                    f"Archive {key}? It will be hidden from the list until you "
                    "turn on the “Archived” filter."
                ),
                confirm_label="Archive",
            ),
            _after,
        )

    # ---- project pin (client-side) / archive ---------------------------

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

    def _toggle_pin(self, key: str) -> None:
        server = self.app.config.state.current_server_url or ""
        self.app.config.toggle_pinned_project(server, key)
        self._sort_projects()
        self.refresh(recompose=True)

    async def _toggle_archive(self, key: str, archive: bool) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            if archive:
                await client.projects.archive_project(key)
            else:
                await client.projects.unarchive_project(key)
        except TissueApiError as e:
            log.warning("Failed to set archived=%s for %s: %s", archive, key, e)
            self.app.notify("Couldn't change the archive state.", severity="error")
            return
        if archive:
            self.app.notify(
                "Project archived — hidden until you enable the “Archived” filter."
            )
        else:
            self.app.notify("Project unarchived.")
        self.run_worker(self._load_projects(), exclusive=True, group="project-load")

    @staticmethod
    def _visibility_label(visibility: str | None) -> str:
        if not visibility:
            return "-"
        labels = {
            "public": "Public",
            "private": "Private",
        }
        label = labels.get(visibility.lower())
        if label is None:  # unknown enum → readable fallback
            return visibility.replace("_", " ").title()
        return label
