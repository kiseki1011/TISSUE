from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.coordinate import Coordinate
from textual.css.query import NoMatches
from textual.widgets import Button, DataTable, Input, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _SEARCH_DEBOUNCE
from tissue.screens.project_home.modals.create_issue_modal import CreateIssueModal
from tissue.screens.project_home.rendering import (
    _ISSUE_LIST_TITLE_WIDTH,
    _color_chip,
    _issue_list_rows,
)
from tissue.widgets.color_type import color_hex

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary

log = logging.getLogger(__name__)


class IssuesMixin(ProjectHomeBase):
    """The [1] Issues list.

    Handles:
        - Loading
        - Searching
        - Drawing
        - Coloring Status
        - Driving the detail
    """

    async def _load_issues(self, keyword: str | None = None) -> None:
        client = self.app.client
        if client is None:
            return
        self._issues_keyword = keyword
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                keyword=keyword,
                state_categories=self._filter.state_categories_arg(),
                priorities=self._filter.priorities_arg(),
                assignee_member_ids=self._filter.assignee_arg(),
                sprint_ids=self._filter.sprint_ids_arg(),
                current_sprint_only=self._filter.current_sprint_only_arg(),
            )
            self._issues = list(page.content or [])
            self._issues_total = page.total_elements or len(self._issues)
            self._issues_has_next = bool(page.has_next)
        except TissueApiError as error:
            log.debug("Hub: failed to load issues: %s", error)
            self._issues = []
            self._issues_total = 0
            self._issues_has_next = False
        self._issues_page = 0
        if not self._members:
            await self._load_members()
        await self._render_issues()
        self._refresh_box_chrome()
        if self._issues:
            self._select_issue(0)
        else:
            self._cancel_detail_timer()
            self.run_worker(
                self._reset_detail_pane(), exclusive=True, group="hub-detail"
            )

    def _member_names(self) -> dict[int, str]:
        """Member id to display name, for the Assignee column."""
        return {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._members
            if member.member_id is not None
        }

    async def _render_issues(self) -> None:
        # Remove the old table before mounting the new one, or its fixed id collides
        list_host = self.query_one("#hub-list-host")
        await list_host.remove_children()
        if not self._issues:
            await list_host.mount(Static("No issues.", classes="hub-list-empty"))
            return
        rows = _issue_list_rows(
            self._issues,
            self._state_colors,
            self.app.theme_variables,
            self._member_names(),
        )
        for index, row in enumerate(rows):
            row.insert(0, str(index + 1))
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
            rows,
            id="hub-issues-table",
            classes="hub-table",
        )
        await list_host.mount(table)
        # Scrollbar scrolling moves the viewport without a cursor move,
        # so watch the scroll position (to page in more)
        self.watch(table, "scroll_y", self._on_issues_scrolled, init=False)

    async def _load_more_issues(self) -> None:
        """Fetch the next page and append it to the [1] list."""
        if self._issues_loading_more or not self._issues_has_next:
            return
        client = self.app.client
        if client is None:
            return
        self._issues_loading_more = True
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                keyword=self._issues_keyword,
                state_categories=self._filter.state_categories_arg(),
                priorities=self._filter.priorities_arg(),
                assignee_member_ids=self._filter.assignee_arg(),
                sprint_ids=self._filter.sprint_ids_arg(),
                current_sprint_only=self._filter.current_sprint_only_arg(),
                page=self._issues_page + 1,
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load more issues: %s", error)
            self._issues_loading_more = False
            return
        new_issues = list(page.content or [])
        self._issues_page += 1
        self._issues_has_next = bool(page.has_next)
        if new_issues:
            self._issues.extend(new_issues)
            self._append_issue_rows(new_issues)
        self._issues_loading_more = False

    def _append_issue_rows(self, issues: list[IssueSummary]) -> None:
        try:
            table = self.query_one("#hub-issues-table", DataTable)
        except NoMatches:
            return
        start = len(self._issues) - len(issues)
        rows = _issue_list_rows(
            issues, self._state_colors, self.app.theme_variables, self._member_names()
        )
        for index, row in enumerate(rows):
            row.insert(0, str(start + index + 1))
        for row in rows:
            table.add_row(*row)
        self._recolor_status_table("#hub-issues-table", self._issues)

    def _on_issues_scrolled(self, _scroll_y: float) -> None:
        """Page in more when scrollbar scrolling nears the end."""
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
        """Build a state-id to color map from the project's workflows to color Status.

        If it fails we just skip it, the table statuses stay uncolored.
        """
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
        """Recolor Status cells of [1] Issues and [3] Agent Work once colors arrive.

        Both tables can be built before the color map is ready, so both need
        fixing up after the load. Done in place so it doesn't fight a load for the
        same table id.
        """
        self._recolor_status_table("#hub-issues-table", self._issues)
        self._recolor_status_table("#hub-agent-issues-table", self._agent_issues)

    def _recolor_status_table(self, table_id: str, issues: list[IssueSummary]) -> None:
        """Recolor the Status column in one table from `_state_colors`.

        Does nothing when the table isn't mounted yet. The Status column sits at a
        different spot per view, so we find it by its header label, not a fixed
        number.
        """
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
            # A reload may have made the table shorter since the color load,
            # so never touch a row past its end.
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
        """Open the right form for the view: issue, sprint, or add-member."""
        if self._view_mode == "sprints":
            self._open_create_sprint()
        elif self._view_mode == "members":
            self._open_add_member()
        else:
            self._open_create_issue()

    def _open_add_member(self) -> None:
        """Open the add-member search modal, reloading the list on a successful add."""
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
        """Open the create-issue form, then on success reload and select the new one."""
        self.app.push_screen(
            CreateIssueModal(project_key=self._project_key, members=self._members),
            self._on_issue_created,
        )

    def _is_project_manager(self) -> bool:
        """Whether the current user is a manager here, which gates sprint create.

        Returns False when the member list or profile isn't loaded yet.
        """
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
        """Set the create button's label and enabled state to match the [1] view."""
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
        # Shared "hub-list" group so the reload can't clash on the host.
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
        """Live search, restart the wait so the list filters when typing stops."""
        self._cancel_search_timer()
        if self._view_mode == "sprints":
            return
        self._search_timer = self.set_timer(_SEARCH_DEBOUNCE, self._run_search)

    @on(Input.Submitted, "#hub-search")
    def _on_search_submitted(self) -> None:
        """Enter searches right away, skipping the short wait."""
        self._cancel_search_timer()
        self._run_search()

    def _run_search(self) -> None:
        """Filter the active list by keyword.

        Members are filtered on our side so the full member list stays around for
        looking up names. Shared exclusive `hub-list` group so a search can't clash
        with a view switch.
        """
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
        """Set the search box to match the active view.

        Each view gets its own placeholder, and the box is disabled on the Sprints
        view since sprints aren't searchable.
        """
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
        # Expanded mode hides [2], so a deliberate Enter opens the detail as a modal.
        if focus_detail and self._expanded:
            self._open_issue_modal(issue_key)
            return
        # Moving the cursor waits a moment, Enter (focus_detail) draws right away.
        self._debounce_detail(
            lambda: self.run_worker(
                self._render_issue_detail(issue_key, focus_detail=focus_detail),
                exclusive=True,
                group="hub-detail",
            ),
            immediate=focus_detail,
        )

    def action_focus_issues(self) -> None:
        """Focus the mounted list table, or the host box when the view is empty.

        Keeps [1] reachable via 1 / CTRL+1 even when there's no table to focus.
        """
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
        """ctrl+s: add the focused [1] Issues issue to the project's active sprint.

        Acts only while the issues table holds focus. Notifies when there is no active
        sprint to add to.
        """
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
        """`/` (or Ctrl+/) jumps straight to the search input from the lists."""
        self.query_one("#hub-search", Input).focus()

    def action_leave_search(self) -> None:
        """Esc in the search box sends focus back to the list, bringing back the
        box-jump digits (1/2/3) that a focused Input would otherwise type."""
        focused = self.app.focused
        if focused is not None and focused.id == "hub-search":
            # Drop a live search that hasn't fired yet so it can't rebuild the list
            # and steal the focus we're about to set, just after we leave the box.
            self._cancel_search_timer()
            self.action_focus_issues()
