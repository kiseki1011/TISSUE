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
    """The [3] box, which toggles (CTRL+T while focused) between two modes:

    - "work": issues in this project assigned to agents the user owns (the
      Assignee column resolves to the owning agent's name).
    - "reviews": issues where the current user is a requested reviewer.

    Mirrors the [1] Issues list (same columns/rendering); selecting a row drives
    the same [2] Details pane (shared `hub-detail` group)."""

    def _toggle_agent_mode(self) -> None:
        """Flip [3] between Agent Work and Requested reviews (CTRL+T on [3])."""
        self._agent_mode = "reviews" if self._agent_mode == "work" else "work"
        self._refresh_box_chrome()
        # The table is about to be removed; park focus on the persistent host (as
        # the [1] toggle does) so it doesn't flicker to the search bar, then the
        # reload re-focuses the new table.
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
            await self._render_agent_issues(
                empty_hint="No agents yet — create one to delegate work.",
                focus_list=focus_list,
            )
            return
        # The agent-assignee filter always applies; the issue filter's state/priority/
        # sprint narrowing only piggybacks when the user ticked "apply to [3]".
        apply = self._filter.apply_to_agent
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                assignee_member_ids=agent_ids,
                state_categories=self._filter.state_categories_arg() if apply else None,
                priorities=self._filter.priorities_arg() if apply else None,
                sprint_ids=self._filter.sprint_ids_arg() if apply else None,
                current_sprint_only=(
                    self._filter.current_sprint_only_arg() if apply else None
                ),
            )
            self._agent_issues = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load agent issues: %s", e)
            self._agent_issues = []
        await self._render_agent_issues(
            empty_hint="No agent work.", focus_list=focus_list
        )

    async def _load_requested_reviews(self, *, focus_list: bool) -> None:
        """Issues where the current user is a requested reviewer. `reviewer_member_ids=
        ["me"]` is resolved to the current member server-side; the filter's
        `reviewer_statuses` (set via the ⚙ modal's "My review status" section) further
        narrows it to reviews in those statuses (empty = any status).

        State is fixed to INITIAL+ACTIVE (open work) and is NOT user-filterable here —
        you only review issues still in flight, never completed/aborted ones."""
        client = self.app.client
        if client is None:
            return
        apply = self._filter.apply_to_agent
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                reviewer_member_ids=["me"],
                # The review-status filter is reviews-specific, so it always applies
                # (independent of the "apply to [3]" priority/sprint narrowing).
                reviewer_statuses=self._filter.reviewer_statuses_arg(),
                # Always open-only: the ⚙ state filter is ignored in reviews mode.
                state_categories=_OPEN_STATE_CATEGORIES,
                priorities=self._filter.priorities_arg() if apply else None,
                sprint_ids=self._filter.sprint_ids_arg() if apply else None,
                current_sprint_only=(
                    self._filter.current_sprint_only_arg() if apply else None
                ),
            )
            self._agent_issues = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load requested reviews: %s", e)
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
        # Resolve the assignee to the owning agent's name (agents may not be in the
        # human roster), falling back to the project roster, then "-".
        member_names = {
            m.member_id: (m.display_name or m.username or "-")
            for m in self._members
            if m.member_id is not None
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
        # In reviews mode the last column is the human Assignee, not an agent, and a
        # leading "Review" column shows the caller's own status on each issue.
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
        # Highlights (cursor moving) debounce; Enter (focus_detail) renders now.
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
