from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import DataTable, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.rendering import _fit, _truncate
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _VIEW_CYCLE, _VIEW_LABELS
from tissue.screens.project_home.rendering import _issue_rows, _sprint_status_chip
from tissue.util.datetime_fmt import format_date, format_relative
from tissue.widgets.detail_row import detail_row

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.sprint_detail import SprintDetail

log = logging.getLogger(__name__)


class SprintsMixin(ProjectHomeBase):
    """The [1] box's Sprints view: the sprint list (one of the CTRL+T list views)
    plus the sprint read view (meta + its issues) in [2]. Also owns the CTRL+T
    cycle that swaps the [1] list between Issues / Sprints / Members."""

    def action_toggle_list(self) -> None:
        # Keep focus on [1] across the swap when it holds focus now. The focused
        # table is about to be removed (→ focus would jump to the search bar and
        # flicker), so park focus on the persistent, always-focusable host first;
        # the load then focuses the new table (or leaves it on the host when the
        # next view is empty and has no table).
        focused = self.app.focused
        keep_focus = focused is not None and focused.id in (
            "hub-issues-table",
            "hub-sprints-table",
            "hub-members-table",
            "hub-list-host",
        )
        if keep_focus:
            try:
                self.query_one("#hub-list-host").focus()
            except NoMatches:
                pass
        i = _VIEW_CYCLE.index(self._view_mode)
        self._switch_view(
            _VIEW_CYCLE[(i + 1) % len(_VIEW_CYCLE)], focus_list=keep_focus
        )

    def _set_view_chrome(self, mode: str) -> None:
        """Reflect the active list view in the [1] box border title (the only
        chrome — the cycle is keyboard-driven via CTRL+T) and record `_view_mode`,
        WITHOUT loading. Any path that swaps #hub-list-host (toggle, search) calls
        this first so the title can never disagree with what's shown. The title
        names the current view and hints the next one in the cycle."""
        self._view_mode = mode
        i = _VIEW_CYCLE.index(mode)
        nxt = _VIEW_CYCLE[(i + 1) % len(_VIEW_CYCLE)]
        self.query_one(
            "#hub-issues-box"
        ).border_title = f"[1] {_VIEW_LABELS[mode]}  (CTRL+T: {_VIEW_LABELS[nxt]})"

    def _switch_view(self, mode: str, *, focus_list: bool = False) -> None:
        """Flip the [1] box to another list view, then (re)load it. All list
        loads share the single exclusive `hub-list` worker group, so a switch
        cancels any in-flight load and only one table is ever mounted into
        #hub-list-host. `focus_list` re-focuses the new table once it mounts."""
        if self._view_mode == mode:
            return
        self._set_view_chrome(mode)
        self._run_view_load(mode, focus_list=focus_list)

    def _run_view_load(self, mode: str, *, focus_list: bool = False) -> None:
        """Spawn the loader for `mode` in the shared exclusive `hub-list` group.
        When `focus_list`, focus the [1] table once it's mounted so a keyboard
        toggle keeps focus on [1] instead of dropping it to the search bar."""
        self.run_worker(
            self._load_view(mode, focus_list), exclusive=True, group="hub-list"
        )

    async def _load_view(self, mode: str, focus_list: bool) -> None:
        if mode == "sprints":
            await self._load_sprints()
        elif mode == "members":
            await self._load_members_list()
        else:
            await self._load_issues()
        if focus_list:
            self.action_focus_issues()

    async def _load_sprints(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.sprints.list_project_sprints(self._project_key)
            self._sprints = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load sprints: %s", e)
            self._sprints = []
        await self._render_sprints()
        # Seed the detail with the first sprint so [2] isn't stale on switch.
        if self._sprints:
            self._select_sprint(0)

    async def _render_sprints(self) -> None:
        box = self.query_one("#hub-list-host")
        await box.remove_children()
        if not self._sprints:
            await box.mount(Static("No sprints.", classes="hub-muted"))
            return
        rows: list[list[str | Text]] = [
            [
                _fit(s.sprint_key or "-", 9),
                Text(_truncate(s.title or "-", 20)),
                _sprint_status_chip(self.app.theme_variables, s.status, pad=False),
                format_date(s.due_at),
            ]
            for s in self._sprints
        ]
        await box.mount(
            _DashTable(
                [("Key", 9), ("Title", None), ("Status", 11), ("Due", 11)],
                rows,
                id="hub-sprints-table",
                classes="hub-table",
            )
        )

    @on(DataTable.RowHighlighted, "#hub-sprints-table")
    def _on_sprint_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_sprint(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-sprints-table")
    def _on_sprint_selected(self, event: DataTable.RowSelected) -> None:
        self._select_sprint(event.cursor_row, focus_detail=True)

    def _select_sprint(self, idx: int, *, focus_detail: bool = False) -> None:
        if not (0 <= idx < len(self._sprints)):
            return
        sprint_id = self._sprints[idx].id
        if sprint_id is None:
            return
        # Shares the issue-detail worker group so the two never render into [2]
        # concurrently.
        self.run_worker(
            self._render_sprint_detail(sprint_id, focus_detail=focus_detail),
            exclusive=True,
            group="hub-detail",
        )

    async def _render_sprint_detail(
        self, sprint_id: int, *, focus_detail: bool
    ) -> None:
        client = self.app.client
        if client is None:
            return
        # Sprint detail replaces the issue detail; clear the issue key so any
        # late-arriving comment/activity workers for a prior issue bail (they
        # guard on _detail_issue_key) instead of clobbering the sprint view.
        self._detail_issue_key = None
        try:
            sprint = await client.sprints.get_sprint(sprint_id)
        except TissueApiError as e:
            log.debug("Hub: failed to load sprint %s: %s", sprint_id, e)
            await self._mount_detail(
                [Static("Couldn't load sprint.", classes="hub-muted")]
            )
            await self._clear_timeline()
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key, sprint_ids=[sprint_id]
            )
            issues = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load issues for sprint %s: %s", sprint_id, e)
            issues = []
        await self._mount_detail(self._sprint_widgets(sprint, issues))
        # Sprints have no activity timeline; clear whatever the last issue left.
        await self._clear_timeline()
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    def _sprint_widgets(
        self, s: SprintDetail, issues: list[IssueSummary]
    ) -> list[Widget]:
        """Sprint read view: title, meta rows, then its issues (reusing the same
        row rendering as the [1] list) under an `Issues (N)` heading."""
        widgets: list[Widget] = [
            Static(s.title or "-", markup=False, classes="hub-detail-title"),
            detail_row("Key", s.sprint_key or "-"),
            detail_row(
                "Status", _sprint_status_chip(self.app.theme_variables, s.status)
            ),
            detail_row(
                "Number", "-" if s.sprint_number is None else str(s.sprint_number)
            ),
            detail_row("Goal", (s.goal or "").strip() or "-"),
            detail_row("Started", format_relative(s.started_at)),
            detail_row("Due", format_relative(s.due_at)),
            detail_row("Completed", format_relative(s.completed_at)),
            detail_row("Created", format_relative(s.created_at)),
            Rule(),
            Static(f"Issues ({len(issues)})", classes="hub-section-title"),
        ]
        if not issues:
            widgets.append(Static("No issues.", classes="hub-muted"))
            return widgets
        rows = _issue_rows(issues, self._state_colors, self.app.theme_variables)
        widgets.append(
            _DashTable(
                [("Key", 9), ("Title", None), ("Status", 9), ("Priority", 8)],
                rows,
                id="hub-sprint-issues-table",
                classes="hub-table hub-sprint-issues",
            )
        )
        return widgets

    async def _clear_timeline(self) -> None:
        try:
            box = self.query_one("#hub-detail-timeline-inner")
        except NoMatches:
            return
        await box.remove_children()
