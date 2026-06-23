from __future__ import annotations

import logging

from textual import on
from textual.coordinate import Coordinate
from textual.css.query import NoMatches
from textual.widgets import Button, DataTable, Input, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.create_issue_modal import CreateIssueModal
from tissue.screens.project_home.rendering import _color_chip, _issue_list_rows
from tissue.widgets.color_type import color_hex

log = logging.getLogger(__name__)


class IssuesMixin(ProjectHomeBase):
    """The [1] Issues list: load/search/render the project's issues, tint each
    row's Status with its workflow colour, and drive the detail on selection."""

    async def _load_issues(self, keyword: str | None = None) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key, keyword=keyword
            )
            self._issues = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load issues: %s", e)
            self._issues = []
        # The Assignee column resolves member ids to names, so make sure the
        # roster is loaded before the first render (it loads concurrently at
        # mount; this just guarantees ordering, and no-ops once populated).
        if not self._members:
            await self._load_members()
        await self._render_issues()
        # Seed the detail pane with the first issue so it isn't blank on open.
        if self._issues:
            self._select_issue(0)

    async def _render_issues(self) -> None:
        # The two list views (Issues / Sprints) swap inside #hub-list-host; the
        # toggle row above it is mount-once. The table has a fixed id, so the old
        # one must be gone before the new mounts (else DuplicateIds) — await it.
        box = self.query_one("#hub-list-host")
        await box.remove_children()
        if not self._issues:
            await box.mount(Static("No issues.", classes="hub-muted"))
            return
        member_names = {
            m.member_id: (m.display_name or m.username or "-")
            for m in self._members
            if m.member_id is not None
        }
        rows = _issue_list_rows(
            self._issues,
            self._state_colors,
            self.app.theme_variables,
            member_names,
        )
        await box.mount(
            _DashTable(
                [
                    ("Key", 9),
                    ("Title", None),
                    ("Status", 11),
                    ("Priority", 8),
                    ("Points", 6),
                    ("Due", 12),
                    ("Assignee", 14),
                ],
                rows,
                id="hub-issues-table",
                classes="hub-table",
            )
        )

    async def _load_state_colors(self) -> None:
        """Build a state-id -> colour map from the project's workflows so the
        issues table can tint each Status with its workflow-defined colour.

        Best-effort: a failure just leaves table statuses uncoloured (the detail
        pane colours its Status straight from the issue's own state, so it never
        depends on this map)."""
        client = self.app.client
        if client is None:
            return
        try:
            summaries = await client.workflows.list_workflows()
        except TissueApiError as e:
            log.debug("Hub: failed to list workflows: %s", e)
            return
        colors: dict[int, str] = {}
        for summary in summaries:
            workflow_id = summary.id
            if workflow_id is None:
                continue
            workflow = self._workflow_cache.get(workflow_id)
            if workflow is None:
                try:
                    workflow = await client.workflows.get_workflow(workflow_id)
                except TissueApiError as e:
                    log.debug("Hub: failed to load workflow %s: %s", workflow_id, e)
                    continue
                self._workflow_cache[workflow_id] = workflow
            for s in workflow.states or []:
                if s.id is not None and s.color:
                    hex_color = color_hex(s.color)
                    if hex_color:
                        colors[s.id] = hex_color
        self._state_colors = colors
        self._recolor_table_status()

    def _recolor_table_status(self) -> None:
        """Repaint each Status cell in place once colours are known. In-place
        (`update_cell_at`) rather than a rebuild, so it never fights a concurrent
        issues load for the table id, and leaves the cursor where it is.

        No-op when the table isn't mounted yet — that load will already read the
        now-populated colour map when it builds the rows."""
        try:
            table = self.query_one("#hub-issues-table", DataTable)
        except NoMatches:
            return
        for row, issue in enumerate(self._issues):
            state_id = issue.current_state_id
            if state_id is None:
                continue
            hex_color = self._state_colors.get(state_id)
            if hex_color:
                table.update_cell_at(
                    Coordinate(row, 2),
                    _color_chip(issue.current_state_label or "-", hex_color),
                )

    @on(Button.Pressed, "#hub-new-issue")
    def _on_new_issue(self) -> None:
        """Open the create-issue form; on success, reload and select the new one."""
        self.app.push_screen(
            CreateIssueModal(project_key=self._project_key, members=self._members),
            self._on_issue_created,
        )

    def _on_issue_created(self, issue_key: str | None) -> None:
        if not issue_key:
            return
        # The new issue belongs in the Issues list — make it the active view, then
        # reload and select it (shared "hub-list" group, so no race on the host).
        self._set_view_chrome("issues")
        self.run_worker(
            self._reload_and_select(issue_key), exclusive=True, group="hub-list"
        )

    async def _reload_and_select(self, issue_key: str) -> None:
        await self._load_issues()
        for idx, issue in enumerate(self._issues):
            if issue.issue_key == issue_key:
                self._select_issue(idx)
                return

    @on(Input.Submitted, "#hub-search")
    def _on_search(self, event: Input.Submitted) -> None:
        keyword = event.value.strip() or None
        # Search filters issues — make the Issues view active so the toggle
        # chrome and #hub-list-host stay coherent even when searching from the
        # Sprints view. Shares the single `hub-list` group (see _switch_view).
        self._set_view_chrome("issues")
        self.run_worker(self._load_issues(keyword), exclusive=True, group="hub-list")

    @on(DataTable.RowHighlighted, "#hub-issues-table")
    def _on_issue_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_issue(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-issues-table")
    def _on_issue_selected(self, event: DataTable.RowSelected) -> None:
        self._select_issue(event.cursor_row, focus_detail=True)

    def _select_issue(self, idx: int, *, focus_detail: bool = False) -> None:
        if not (0 <= idx < len(self._issues)):
            return
        issue_key = self._issues[idx].issue_key
        if issue_key is None:
            return
        self.run_worker(
            self._render_issue_detail(issue_key, focus_detail=focus_detail),
            exclusive=True,
            group="hub-detail",
        )

    def action_focus_issues(self) -> None:
        """Focus whichever list table is mounted (Issues / Sprints / Members), or
        the host container when the active view is empty (no table) so [1] stays
        reachable via 1 / CTRL+1."""
        for table_id in (
            "#hub-issues-table",
            "#hub-sprints-table",
            "#hub-members-table",
        ):
            try:
                self.query_one(table_id, DataTable).focus()
                return
            except NoMatches:
                continue
        try:
            self.query_one("#hub-list-host").focus()
        except NoMatches:
            pass

    def action_focus_search(self) -> None:
        """ctrl+/ — jump straight to the search input from anywhere on the screen."""
        self.query_one("#hub-search", Input).focus()
