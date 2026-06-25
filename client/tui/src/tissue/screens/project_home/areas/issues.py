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
from tissue.screens.project_home.rendering import _color_chip, _issue_list_rows
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
        except TissueApiError as error:
            log.debug("Hub: failed to load issues: %s", error)
            self._issues = []
        # The Assignee column needs the member list. Load it on first use, and
        # do nothing once it is already there.
        if not self._members:
            await self._load_members()
        await self._render_issues()
        # When the list is empty, clear [2] in the shared detail group so it can't
        # keep showing a now-hidden issue or draw an old one at the wrong time.
        if self._issues:
            self._select_issue(0)
        else:
            self._cancel_detail_timer()
            self.run_worker(
                self._reset_detail_pane(), exclusive=True, group="hub-detail"
            )

    async def _render_issues(self) -> None:
        # The table id is fixed, so wait for the old one to be removed before
        # mounting the new one, or we get a DuplicateIds error.
        list_host = self.query_one("#hub-list-host")
        await list_host.remove_children()
        if not self._issues:
            await list_host.mount(Static("No issues.", classes="hub-muted"))
            return
        member_names = {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._members
            if member.member_id is not None
        }
        rows = _issue_list_rows(
            self._issues,
            self._state_colors,
            self.app.theme_variables,
            member_names,
        )
        await list_host.mount(
            _DashTable(
                [
                    ("Key", 10),
                    ("Type", 10),
                    ("Title", None),
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
        """Create the right thing for the view, issue on Issues, sprint on Sprints."""
        if self._view_mode == "sprints":
            self._open_create_sprint()
        elif self._view_mode == "members":
            # Member-add isn't built yet, so the button is disabled in this view.
            return
        else:
            self._open_create_issue()

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
            create_button.disabled = True
            create_button.tooltip = "Add member (coming soon)"
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
