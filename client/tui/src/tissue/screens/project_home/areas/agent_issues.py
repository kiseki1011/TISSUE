from __future__ import annotations

import logging

from textual import on
from textual.widgets import DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _OPEN_STATE_CATEGORIES
from tissue.screens.project_home.issue_table import (
    AGENT_ISSUE_TABLE_ID,
    recolor_status_cells,
)
from tissue.screens.project_home.rendering import _issue_list_rows

log = logging.getLogger(__name__)


class AgentIssuesMixin(ProjectHomeBase):
    """The [1] Agent / Reviews contexts (my agents' work, issues I review)."""

    async def _load_agent_issues(self, keyword: str | None = None) -> None:
        await self._ensure_members_loaded()
        if self._ui.view_mode == "reviews":
            await self._load_requested_reviews(keyword)
        else:
            await self._load_agent_work(keyword)

    async def _load_agent_work(self, keyword: str | None) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            agents = await client.agents.list_my_agents()
        except TissueApiError as error:
            log.debug("Hub: failed to list agents: %s", error)
            agents = []
        self._agent_work.names = {
            agent.id: (agent.name or agent.username or "-")
            for agent in agents
            if agent.id is not None
        }
        agent_ids = [str(agent.id) for agent in agents if agent.id is not None]
        if not agent_ids:
            self._agent_work.issues = []
            await self._render_agent_issues(empty_hint="No agents yet.")
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                keyword=keyword,
                assignee_member_ids=agent_ids,
                **self._agent_issue_filter_kwargs(),
            )
            self._agent_work.issues = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load agent issues: %s", error)
            self._agent_work.issues = []
        await self._render_agent_issues(empty_hint="No agent work.")

    async def _load_requested_reviews(self, keyword: str | None) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                keyword=keyword,
                reviewer_member_ids=["me"],
                reviewer_statuses=self._filters.issue.reviewer_statuses_arg(),
                state_categories=_OPEN_STATE_CATEGORIES,
                **self._agent_issue_filter_kwargs(include_state=False),
            )
            self._agent_work.issues = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load requested reviews: %s", error)
            self._agent_work.issues = []
        await self._render_agent_issues(empty_hint="No issues awaiting your review.")

    def _agent_issue_filter_kwargs(self, *, include_state: bool = True) -> dict:
        if not self._filters.issue.apply_to_agent:
            return {}
        kwargs = {
            "priorities": self._filters.issue.priorities_arg(),
            "sprint_ids": self._filters.issue.sprint_ids_arg(),
            "current_sprint_only": self._filters.issue.current_sprint_only_arg(),
        }
        if include_state:
            kwargs["state_categories"] = self._filters.issue.state_categories_arg()
        return kwargs

    async def _render_agent_issues(self, *, empty_hint: str) -> None:
        panel = self._issue_list_panel()
        if panel is None:
            return
        if not self._agent_work.issues:
            await panel.replace_content([Static(empty_hint, classes="hub-list-empty")])
            self.run_worker(
                self._reset_detail_pane(), exclusive=True, group="hub-detail"
            )
            return
        member_names = {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._member_list.members
            if member.member_id is not None
        }
        member_names.update(self._agent_work.names)
        reviews = self._ui.view_mode == "reviews"
        rows = _issue_list_rows(
            self._agent_work.issues,
            self._state_colors,
            self.app.theme_variables,
            member_names,
            with_review_status=reviews,
            title_extra=self._title_extra(),
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
        await panel.replace_content(
            [
                _DashTable(
                    columns,
                    rows,
                    id="hub-agent-issues-table",
                    classes="hub-table",
                )
            ]
        )
        recolor_status_cells(
            panel, AGENT_ISSUE_TABLE_ID, self._agent_work.issues, self._state_colors
        )
        self._select_agent_issue(0)

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
        if not (0 <= row_index < len(self._agent_work.issues)):
            return
        issue_key = self._agent_work.issues[row_index].issue_key
        if issue_key is None:
            return
        if focus_detail and self._ui.expanded:
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
