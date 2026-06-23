from __future__ import annotations

import logging

from textual import on
from textual.css.query import NoMatches
from textual.widgets import DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.rendering import _issue_list_rows

log = logging.getLogger(__name__)


class AgentIssuesMixin(ProjectHomeBase):
    """The [3] box: issues in this project assigned to agents the user owns.

    Mirrors the [1] Issues list (same columns/rendering) so the two read alike;
    the Assignee column resolves to the owning agent's name. Selecting a row drives
    the same [2] Details pane as the issues list (shared `hub-detail` group)."""

    async def _load_agent_issues(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            agents = await client.agents.list_my_agents()
        except TissueApiError as e:
            log.debug("Hub: failed to list agents: %s", e)
            agents = []
        self._agent_names = {
            a.id: (a.name or a.username or "-") for a in agents if a.id is not None
        }
        agent_ids = [str(a.id) for a in agents if a.id is not None]
        if not agent_ids:
            # No agents owned -> nothing to assign; leave a hint instead of a table.
            self._agent_issues = []
            await self._render_agent_issues(no_agents=True)
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key, assignee_member_ids=agent_ids
            )
            self._agent_issues = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load agent issues: %s", e)
            self._agent_issues = []
        await self._render_agent_issues()

    async def _render_agent_issues(self, *, no_agents: bool = False) -> None:
        try:
            box = self.query_one("#hub-agent-issues-host")
        except NoMatches:
            return
        await box.remove_children()
        if no_agents:
            await box.mount(
                Static(
                    "No agents yet — create one to delegate work.",
                    classes="hub-muted",
                )
            )
            return
        if not self._agent_issues:
            await box.mount(Static("No agent work.", classes="hub-muted"))
            return
        # Resolve the assignee to the owning agent's name (agents may not be in the
        # human roster), falling back to the project roster, then "-".
        member_names = {
            m.member_id: (m.display_name or m.username or "-")
            for m in self._members
            if m.member_id is not None
        }
        member_names.update(self._agent_names)
        rows = _issue_list_rows(
            self._agent_issues,
            self._state_colors,
            self.app.theme_variables,
            member_names,
        )
        await box.mount(
            _DashTable(
                [
                    ("Key", 10),
                    ("Title", None),
                    ("Status", 13),
                    ("Priority", 8),
                    ("Points", 6),
                    ("Due", 12),
                    ("Agent", 14),
                ],
                rows,
                id="hub-agent-issues-table",
                classes="hub-table",
            )
        )

    @on(DataTable.RowHighlighted, "#hub-agent-issues-table")
    def _on_agent_issue_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_agent_issue(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-agent-issues-table")
    def _on_agent_issue_selected(self, event: DataTable.RowSelected) -> None:
        self._select_agent_issue(event.cursor_row, focus_detail=True)

    def _select_agent_issue(self, idx: int, *, focus_detail: bool = False) -> None:
        if not (0 <= idx < len(self._agent_issues)):
            return
        issue_key = self._agent_issues[idx].issue_key
        if issue_key is None:
            return
        # Expanded mode hides [2]; an explicit Enter opens the detail as a modal.
        if focus_detail and self._expanded:
            self._open_issue_modal(issue_key)
            return
        self.run_worker(
            self._render_issue_detail(issue_key, focus_detail=focus_detail),
            exclusive=True,
            group="hub-detail",
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
