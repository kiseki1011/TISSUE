from __future__ import annotations

import logging

from textual import on
from textual.widgets import DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _OPEN_STATE_CATEGORIES
from tissue.screens.project_home.rendering import _issue_list_rows

log = logging.getLogger(__name__)


class AgentIssuesMixin(ProjectHomeBase):
    """The [3] Agent Work / Reviews box."""

    def _toggle_agent_mode(self) -> None:
        self._ui.agent_mode = "reviews" if self._ui.agent_mode == "work" else "work"
        self._persist_project_ui()
        self._refresh_box_chrome()
        keep = self._agent_box_has_focus()
        if keep:
            self._focus_agent_host()
        self.run_worker(
            self._load_agent_issues(focus_list=keep),
            exclusive=True,
            group="hub-agent",
        )

    def _agent_box_has_focus(self) -> bool:
        panel = self._agent_work_panel()
        return panel is not None and panel.has_focus_in(
            {"hub-agent-issues-table", "hub-agent-issues-host"}
        )

    def _focus_agent_host(self) -> None:
        panel = self._agent_work_panel()
        if panel is not None:
            panel.focus_host()

    async def _load_agent_issues(self, *, focus_list: bool = False) -> None:
        if self._ui.agent_mode == "reviews":
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
        self._agent_work.names = {
            agent.id: (agent.name or agent.username or "-")
            for agent in agents
            if agent.id is not None
        }
        agent_ids = [str(agent.id) for agent in agents if agent.id is not None]
        if not agent_ids:
            self._agent_work.issues = []
            await self._render_agent_issues(
                empty_hint="No agents yet.",
                focus_list=focus_list,
            )
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                assignee_member_ids=agent_ids,
                **self._agent_issue_filter_kwargs(),
            )
            self._agent_work.issues = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load agent issues: %s", error)
            self._agent_work.issues = []
        await self._render_agent_issues(
            empty_hint="No agent work.", focus_list=focus_list
        )

    async def _load_requested_reviews(self, *, focus_list: bool) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                reviewer_member_ids=["me"],
                reviewer_statuses=self._filters.issue.reviewer_statuses_arg(),
                state_categories=_OPEN_STATE_CATEGORIES,
                **self._agent_issue_filter_kwargs(include_state=False),
            )
            self._agent_work.issues = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load requested reviews: %s", error)
            self._agent_work.issues = []
        await self._render_agent_issues(
            empty_hint="No issues awaiting your review.", focus_list=focus_list
        )

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

    async def _render_agent_issues(
        self, *, empty_hint: str = "No agent work.", focus_list: bool = False
    ) -> None:
        panel = self._agent_work_panel()
        if panel is None:
            return
        if not self._agent_work.issues:
            await panel.replace_content([Static(empty_hint, classes="hub-muted")])
            if focus_list:
                self.action_focus_agent_issues()
            return
        member_names = {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._member_list.members
            if member.member_id is not None
        }
        member_names.update(self._agent_work.names)
        reviews = self._ui.agent_mode == "reviews"
        rows = _issue_list_rows(
            self._agent_work.issues,
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

    def action_focus_agent_issues(self) -> None:
        panel = self._agent_work_panel()
        if panel is None:
            return
        if panel.focus_first_table(("hub-agent-issues-table",)):
            return
        panel.focus_host()
