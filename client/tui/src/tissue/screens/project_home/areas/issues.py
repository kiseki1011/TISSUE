from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.coordinate import Coordinate
from textual.css.query import NoMatches
from textual.widgets import Button, DataTable, Input, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import (
    _DETAIL_PREFETCH_AFTER,
    _DETAIL_PREFETCH_BEFORE,
    _SEARCH_DEBOUNCE,
)
from tissue.screens.project_home.modals.create_issue_modal import CreateIssueModal
from tissue.screens.project_home.rendering import (
    _ISSUE_LIST_TITLE_WIDTH,
    _color_chip,
    _issue_list_rows,
)
from tissue.widgets.color_type import color_hex

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.page_response_issue_summary import (
        PageResponseIssueSummary,
    )

log = logging.getLogger(__name__)


class IssuesMixin(ProjectHomeBase):
    """The [1] Issues list."""

    async def _load_issues(self, keyword: str | None = None) -> None:
        self._issues_keyword = keyword
        page = await self._fetch_issue_page(keyword=keyword)
        self._replace_issues(page)
        self._issues_page = 0
        if not self._members:
            await self._load_members()
        await self._render_issues()
        self._refresh_box_chrome()
        self._select_first_issue_or_reset_detail()

    async def _fetch_issue_page(
        self, *, keyword: str | None, page: int = 0
    ) -> PageResponseIssueSummary | None:
        client = self.app.client
        if client is None:
            return None
        try:
            return await client.issues.search_project_issues(
                self._project_key,
                keyword=keyword,
                state_categories=self._filter.state_categories_arg(),
                priorities=self._filter.priorities_arg(),
                assignee_member_ids=self._filter.assignee_arg(),
                sprint_ids=self._filter.sprint_ids_arg(),
                current_sprint_only=self._filter.current_sprint_only_arg(),
                page=page,
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load issues: %s", error)
            return None

    def _replace_issues(self, page: PageResponseIssueSummary | None) -> None:
        if page is None:
            self._issues = []
            self._issues_total = 0
            self._issues_has_next = False
            return
        self._issues = list(page.content or [])
        self._issues_total = page.total_elements or len(self._issues)
        self._issues_has_next = bool(page.has_next)

    def _select_first_issue_or_reset_detail(self) -> None:
        if self._issues:
            self._select_issue(0)
            return
        self._cancel_detail_timer()
        self.run_worker(self._reset_detail_pane(), exclusive=True, group="hub-detail")

    def _member_names(self) -> dict[int, str]:
        return {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._members
            if member.member_id is not None
        }

    async def _render_issues(self) -> None:
        list_host = self.query_one("#hub-list-host")
        await list_host.remove_children()
        if not self._issues:
            await list_host.mount(Static("No issues.", classes="hub-list-empty"))
            return
        table = _DashTable(
            [
                ("#", None),
                ("Key", 10),
                ("Type", 10),
                ("Title", _ISSUE_LIST_TITLE_WIDTH),
                ("Status", 13),
                ("Priority", 8),
                ("Points", 6),
                ("Due", 12),
                ("Assignee", 14),
            ],
            self._numbered_issue_rows(self._issues),
            id="hub-issues-table",
            classes="hub-table",
        )
        await list_host.mount(table)
        self.watch(table, "scroll_y", self._on_issues_scrolled, init=False)

    def _numbered_issue_rows(
        self, issues: list[IssueSummary], *, start: int = 0
    ) -> list[list[str | Text]]:
        rows = _issue_list_rows(
            issues,
            self._state_colors,
            self.app.theme_variables,
            self._member_names(),
        )
        for index, row in enumerate(rows):
            row.insert(0, str(start + index + 1))
        return rows

    async def _load_more_issues(self) -> None:
        if self._issues_loading_more or not self._issues_has_next:
            return
        self._issues_loading_more = True
        try:
            page = await self._fetch_issue_page(
                keyword=self._issues_keyword, page=self._issues_page + 1
            )
            if page is None:
                return
            self._append_issue_page(page)
        finally:
            self._issues_loading_more = False

    def _append_issue_page(self, page: PageResponseIssueSummary) -> None:
        new_issues = list(page.content or [])
        self._issues_page += 1
        self._issues_has_next = bool(page.has_next)
        if not new_issues:
            return
        self._issues.extend(new_issues)
        self._append_issue_rows(new_issues)

    def _append_issue_rows(self, issues: list[IssueSummary]) -> None:
        try:
            table = self.query_one("#hub-issues-table", DataTable)
        except NoMatches:
            return
        start = len(self._issues) - len(issues)
        for row in self._numbered_issue_rows(issues, start=start):
            table.add_row(*row)
        self._recolor_status_table("#hub-issues-table", self._issues)

    def _on_issues_scrolled(self, _scroll_y: float) -> None:
        if self._issues_loading_more or not self._issues_has_next:
            return
        try:
            table = self.query_one("#hub-issues-table", DataTable)
        except NoMatches:
            return
        if table.max_scroll_y > 0 and table.scroll_offset.y >= table.max_scroll_y - 3:
            self.run_worker(
                self._load_more_issues(), exclusive=True, group="hub-load-more"
            )

    async def _load_state_colors(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            summaries = await client.workflows.list_workflows()
        except TissueApiError as error:
            log.debug("Hub: failed to list workflows: %s", error)
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
                except TissueApiError as error:
                    log.debug("Hub: failed to load workflow %s: %s", workflow_id, error)
                    continue
                self._workflow_cache[workflow_id] = workflow
            for state in workflow.states or []:
                if state.id is not None and state.color:
                    hex_color = color_hex(state.color)
                    if hex_color:
                        colors[state.id] = hex_color
        self._state_colors = colors
        self._recolor_table_status()

    def _recolor_table_status(self) -> None:
        self._recolor_status_table("#hub-issues-table", self._issues)
        self._recolor_status_table("#hub-agent-issues-table", self._agent_issues)

    def _recolor_status_table(self, table_id: str, issues: list[IssueSummary]) -> None:
        try:
            table = self.query_one(table_id, DataTable)
        except NoMatches:
            return
        status_col = next(
            (
                index
                for index, column in enumerate(table.columns.values())
                if str(column.label) == "Status"
            ),
            None,
        )
        if status_col is None:
            return
        for row, issue in enumerate(issues):
            if row >= table.row_count:
                break
            state_id = issue.current_state_id
            if state_id is None:
                continue
            hex_color = self._state_colors.get(state_id)
            if hex_color:
                table.update_cell_at(
                    Coordinate(row, status_col),
                    _color_chip(issue.current_state_label or "-", hex_color),
                )

    @on(Button.Pressed, "#hub-new-issue")
    def _on_create_pressed(self) -> None:
        if self._view_mode == "sprints":
            self._open_create_sprint()
        elif self._view_mode == "members":
            self._open_add_member()
        else:
            self._open_create_issue()

    def _open_add_member(self) -> None:
        from tissue.screens.project_home.modals.member_add_modal import MemberAddModal

        self.app.push_screen(
            MemberAddModal(project_key=self._project_key), self._on_members_added
        )

    def _on_members_added(self, added: bool | None) -> None:
        if added:
            self.run_worker(
                self._load_members_list(self._search_keyword()),
                exclusive=True,
                group="hub-list",
            )

    def _open_create_issue(self) -> None:
        self.app.push_screen(
            CreateIssueModal(project_key=self._project_key, members=self._members),
            self._on_issue_created,
        )

    def _is_project_manager(self) -> bool:
        client = self.app.client
        profile = client.account.cached_profile if client is not None else None
        username = profile.username if profile is not None else None
        if not username:
            return False
        for member in self._members:
            if member.username == username:
                return (member.role or "").upper() == "MANAGER"
        return False

    def _update_create_button(self) -> None:
        try:
            create_button = self.query_one("#hub-new-issue", Button)
        except NoMatches:
            return
        mode = self._view_mode
        if mode == "sprints":
            create_button.label = "S"
            manager = self._is_project_manager()
            create_button.disabled = not manager
            create_button.tooltip = "New sprint" if manager else "Requires manager role"
        elif mode == "members":
            create_button.label = "+"
            manager = self._is_project_manager()
            create_button.disabled = not manager
            create_button.tooltip = "Add member" if manager else "Requires manager role"
        else:
            create_button.label = "+"
            create_button.disabled = False
            create_button.tooltip = "New issue"

    def _on_issue_created(self, issue_key: str | None) -> None:
        if not issue_key:
            return
        self._set_view_chrome("issues")
        self.run_worker(
            self._reload_and_select(issue_key), exclusive=True, group="hub-list"
        )

    async def _reload_and_select(self, issue_key: str) -> None:
        await self._load_issues()
        for index, issue in enumerate(self._issues):
            if issue.issue_key == issue_key:
                self._select_issue(index)
                return

    @on(Input.Changed, "#hub-search")
    def _on_search_changed(self) -> None:
        self._cancel_search_timer()
        if self._view_mode == "sprints":
            return
        self._search_timer = self.set_timer(_SEARCH_DEBOUNCE, self._run_search)

    @on(Input.Submitted, "#hub-search")
    def _on_search_submitted(self) -> None:
        self._cancel_search_timer()
        self._run_search()

    def _run_search(self) -> None:
        try:
            keyword = self.query_one("#hub-search", Input).value.strip() or None
        except NoMatches:
            return
        if self._view_mode == "members":
            self.run_worker(
                self._render_members_list(keyword), exclusive=True, group="hub-list"
            )
        elif self._view_mode == "sprints":
            return
        else:
            self.run_worker(
                self._load_issues(keyword), exclusive=True, group="hub-list"
            )

    def _cancel_search_timer(self) -> None:
        if self._search_timer is not None:
            self._search_timer.stop()
            self._search_timer = None

    def _update_search_input(self) -> None:
        try:
            search_input = self.query_one("#hub-search", Input)
        except NoMatches:
            return
        if self._view_mode == "sprints":
            search_input.disabled = True
            search_input.placeholder = "Search unavailable for sprints"
        else:
            search_input.disabled = False
            search_input.placeholder = (
                "Search members…" if self._view_mode == "members" else "Search issues…"
            )

    @on(DataTable.RowHighlighted, "#hub-issues-table")
    def _on_issue_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_issue(event.cursor_row)
            if (
                self._issues_has_next
                and not self._issues_loading_more
                and event.cursor_row >= len(self._issues) - 5
            ):
                self.run_worker(
                    self._load_more_issues(), exclusive=True, group="hub-load-more"
                )

    @on(DataTable.RowSelected, "#hub-issues-table")
    def _on_issue_selected(self, event: DataTable.RowSelected) -> None:
        self._select_issue(event.cursor_row, focus_detail=True)

    def _select_issue(self, index: int, *, focus_detail: bool = False) -> None:
        if not (0 <= index < len(self._issues)):
            return
        issue_key = self._issues[index].issue_key
        if issue_key is None:
            return
        if focus_detail and self._expanded:
            self._open_issue_modal(issue_key)
            return

        self._debounce_detail(
            lambda: self.run_worker(
                self._render_issue_detail(issue_key, focus_detail=focus_detail),
                exclusive=True,
                group="hub-detail",
            ),
            immediate=focus_detail,
        )
        self._prefetch_nearby_issue_details(index)

    def _prefetch_nearby_issue_details(self, index: int) -> None:
        start = max(0, index - _DETAIL_PREFETCH_BEFORE)
        end = min(len(self._issues), index + _DETAIL_PREFETCH_AFTER + 1)
        issue_keys = [
            issue.issue_key
            for offset, issue in enumerate(self._issues[start:end], start=start)
            if offset != index
            and issue.issue_key is not None
            and issue.issue_key not in self._detail_cache
        ]
        if issue_keys:
            self.run_worker(
                self._prefetch_issue_details(issue_keys),
                exclusive=True,
                group="hub-detail-prefetch",
            )

    def action_focus_issues(self) -> None:
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

    def action_add_to_sprint(self) -> None:
        try:
            table = self.query_one("#hub-issues-table", DataTable)
        except NoMatches:
            return
        if not table.has_focus:
            return
        row = table.cursor_row
        if not (0 <= row < len(self._issues)):
            return
        issue_key = self._issues[row].issue_key
        if not issue_key:
            return
        self.run_worker(
            self._add_issue_to_active_sprint(issue_key),
            exclusive=True,
            group="hub-add-sprint",
        )

    def action_focus_search(self) -> None:
        self.query_one("#hub-search", Input).focus()

    def action_leave_search(self) -> None:
        focused = self.app.focused
        if focused is not None and focused.id == "hub-search":
            self._cancel_search_timer()
            self.action_focus_issues()
