from __future__ import annotations

import logging

from textual import on
from textual.css.query import NoMatches
from textual.widgets import DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _OPEN_STATE_CATEGORIES
from tissue.screens.project_home.rendering import _issue_list_rows

log = logging.getLogger(__name__)


class AgentIssuesMixin(ProjectHomeBase):
    """The [3] box, switching (CTRL+T while focused) between two modes.

    Modes:
        - "work", issues in this project assigned to agents the user owns
          (the Assignee column shows the owning agent's name).
        - "reviews", issues where the current user is a requested reviewer.

    Works like the [1] Issues list, and clicking a row updates the same [2]
    Details pane (shared `hub-detail` group).
    """

    def _toggle_agent_mode(self) -> None:
        """Flip [3] between Agent Work and Requested reviews (CTRL+T on [3])."""
        self._agent_mode = "reviews" if self._agent_mode == "work" else "work"
        self._refresh_box_chrome()
        # Move focus to the always-present host before the table is removed, so
        # focus doesn't jump to the search bar. The reload re-focuses the new table.
        focused = self.app.focused
        keep = focused is not None and focused.id in (
            "hub-agent-issues-table",
            "hub-agent-issues-host",
        )
        if keep:
            try:
                self.query_one("#hub-agent-issues-host").focus()
            except NoMatches:
                pass
        self.run_worker(
            self._load_agent_issues(focus_list=keep),
            exclusive=True,
            group="hub-agent",
        )

    async def _load_agent_issues(self, *, focus_list: bool = False) -> None:
        if self._agent_mode == "reviews":
            await self._load_requested_reviews(focus_list=focus_list)
        else:
            await self._load_agent_work(focus_list=focus_list)

    async def _load_agent_work(self, *, focus_list: bool) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            agents = await client.agents.list_my_agents()
        except TissueApiError as error:
            log.debug("Hub: failed to list agents: %s", error)
            agents = []
        self._agent_names = {
            agent.id: (agent.name or agent.username or "-")
            for agent in agents
            if agent.id is not None
        }
        agent_ids = [str(agent.id) for agent in agents if agent.id is not None]
        if not agent_ids:
            self._agent_issues = []
            await self._render_agent_issues(
                empty_hint="No agents yet — create one to delegate work.",
                focus_list=focus_list,
            )
            return
        # The agent-assignee filter always applies. The issue filter's
        # state/priority/sprint limits are added only when the user ticked
        # "apply to [3]".
        apply_issue_filter = self._filter.apply_to_agent
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                assignee_member_ids=agent_ids,
                state_categories=self._filter.state_categories_arg()
                if apply_issue_filter
                else None,
                priorities=self._filter.priorities_arg()
                if apply_issue_filter
                else None,
                sprint_ids=self._filter.sprint_ids_arg()
                if apply_issue_filter
                else None,
                current_sprint_only=(
                    self._filter.current_sprint_only_arg()
                    if apply_issue_filter
                    else None
                ),
            )
            self._agent_issues = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load agent issues: %s", error)
            self._agent_issues = []
        await self._render_agent_issues(
            empty_hint="No agent work.", focus_list=focus_list
        )

    async def _load_requested_reviews(self, *, focus_list: bool) -> None:
        """Issues where the current user is a requested reviewer.

        `reviewer_member_ids=["me"]` means the current member, worked out on
        the server. State is fixed to INITIAL+ACTIVE because you only review
        issues still being worked on, never completed or aborted ones.
        """
        client = self.app.client
        if client is None:
            return
        apply_issue_filter = self._filter.apply_to_agent
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                reviewer_member_ids=["me"],
                # Only used for reviews, so it always applies, ignoring "apply to [3]".
                reviewer_statuses=self._filter.reviewer_statuses_arg(),
                state_categories=_OPEN_STATE_CATEGORIES,
                priorities=self._filter.priorities_arg()
                if apply_issue_filter
                else None,
                sprint_ids=self._filter.sprint_ids_arg()
                if apply_issue_filter
                else None,
                current_sprint_only=(
                    self._filter.current_sprint_only_arg()
                    if apply_issue_filter
                    else None
                ),
            )
            self._agent_issues = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load requested reviews: %s", error)
            self._agent_issues = []
        await self._render_agent_issues(
            empty_hint="No issues awaiting your review.", focus_list=focus_list
        )

    async def _render_agent_issues(
        self, *, empty_hint: str = "No agent work.", focus_list: bool = False
    ) -> None:
        try:
            box = self.query_one("#hub-agent-issues-host")
        except NoMatches:
            return
        await box.remove_children()
        if not self._agent_issues:
            await box.mount(Static(empty_hint, classes="hub-muted"))
            if focus_list:
                self.action_focus_agent_issues()
            return
        # Look up the assignee's name, trying each source in order:
        # - The owning agent (agents may not be in the people list)
        # - The project member list
        # - `"-"` as a last resort
        member_names = {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._members
            if member.member_id is not None
        }
        member_names.update(self._agent_names)
        reviews = self._agent_mode == "reviews"
        rows = _issue_list_rows(
            self._agent_issues,
            self._state_colors,
            self.app.theme_variables,
            member_names,
            with_review_status=reviews,
        )
        last_col = "Assignee" if reviews else "Agent"
        columns: list[tuple[str, int | None]] = []
        if reviews:
            columns.append(("Review", 9))
        columns.extend(
            [
                ("Key", 10),
                ("Type", 10),
                ("Title", None),
                ("Status", 13),
                ("Priority", 8),
                ("Points", 6),
                ("Due", 12),
                (last_col, 14),
            ]
        )
        await box.mount(
            _DashTable(
                columns,
                rows,
                id="hub-agent-issues-table",
                classes="hub-table",
            )
        )
        if focus_list:
            self.action_focus_agent_issues()

    @on(DataTable.RowHighlighted, "#hub-agent-issues-table")
    def _on_agent_issue_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_agent_issue(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-agent-issues-table")
    def _on_agent_issue_selected(self, event: DataTable.RowSelected) -> None:
        self._select_agent_issue(event.cursor_row, focus_detail=True)

    def _select_agent_issue(
        self, row_index: int, *, focus_detail: bool = False
    ) -> None:
        if not (0 <= row_index < len(self._agent_issues)):
            return
        issue_key = self._agent_issues[row_index].issue_key
        if issue_key is None:
            return
        # Expanded mode hides [2], so pressing Enter opens the detail as a modal.
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

    def action_focus_agent_issues(self) -> None:
        """Focus the [3] agent-issues table, or its host when empty so [3] stays
        reachable via 3 / CTRL+3."""
        try:
            self.query_one("#hub-agent-issues-table", DataTable).focus()
            return
        except NoMatches:
            pass
        try:
            self.query_one("#hub-agent-issues-host").focus()
        except NoMatches:
            pass
