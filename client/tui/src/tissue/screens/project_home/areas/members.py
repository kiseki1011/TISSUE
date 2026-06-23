from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.widget import Widget
from textual.widgets import DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

log = logging.getLogger(__name__)


def _active_label(active: bool | None) -> str:
    """Yes / No, or '-' when unknown — `active` is an Optional bool, so don't
    conflate a missing value with an explicit False."""
    if active is None:
        return "-"
    return "Yes" if active else "No"


class MembersMixin(ProjectHomeBase):
    """The [1] box's Members view: the project's roster (one of the CTRL+T list
    views), with each member's read view (role / active / joined) in [2].

    `_load_members` also runs once at mount for assignee name resolution; the
    Members *view* reuses it, then renders the roster into the list host."""

    async def _load_members(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            self._members = await client.project_members.list_project_members(
                self._project_key
            )
        except TissueApiError as e:
            log.debug("Hub: failed to load members: %s", e)
            # Keep any roster already loaded rather than blanking it, so a failed
            # (re)load can't clobber data a prior successful load fetched (the
            # issues load and the members view both call this).
        # The roster carries the current user's role, which gates the manager-only
        # create button; refresh it now that the role is known.
        self._update_create_button()

    async def _load_members_list(self) -> None:
        # Refetch so the roster is fresh on each switch (mirrors issues/sprints);
        # the mount-time load only seeds name resolution before any view shows.
        await self._load_members()
        await self._render_members_list()
        # Seed the detail with the first member so [2] isn't stale on switch.
        if self._members:
            self._select_member(0)

    async def _render_members_list(self) -> None:
        box = self.query_one("#hub-list-host")
        await box.remove_children()
        if not self._members:
            await box.mount(Static("No members.", classes="hub-muted"))
            return
        rows: list[list[str | Text]] = [
            [
                Text(m.display_name or m.username or "-"),
                (m.role or "-").capitalize(),
                _active_label(m.active),
            ]
            for m in self._members
        ]
        await box.mount(
            _DashTable(
                [("Name", None), ("Role", 10), ("Active", 8)],
                rows,
                id="hub-members-table",
                classes="hub-table",
            )
        )

    @on(DataTable.RowHighlighted, "#hub-members-table")
    def _on_member_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_member(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-members-table")
    def _on_member_selected(self, event: DataTable.RowSelected) -> None:
        self._select_member(event.cursor_row, focus_detail=True)

    def _select_member(self, idx: int, *, focus_detail: bool = False) -> None:
        if not (0 <= idx < len(self._members)):
            return
        # Member detail has no modal; if expanded (Enter), leave full-width so the
        # [2] pane is visible instead of rendering into the hidden one.
        if focus_detail and self._expanded:
            self._ensure_not_expanded()
        member = self._members[idx]
        # Shares the issue-detail worker group so member / issue / sprint renders
        # never land in [2] concurrently; debounced like the other list views.
        self._debounce_detail(
            lambda: self.run_worker(
                self._render_member_detail(member, focus_detail=focus_detail),
                exclusive=True,
                group="hub-detail",
            ),
            immediate=focus_detail,
        )

    async def _render_member_detail(
        self, member: ProjectMemberSummary, *, focus_detail: bool
    ) -> None:
        # A member has no comments/activity: clear the issue key so any late issue
        # workers bail (they guard on _detail_issue_key), and clear the timeline
        # (as the sprint read view does).
        self._detail_issue_key = None
        await self._mount_detail(self._member_widgets(member))
        await self._clear_timeline()
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    def _member_widgets(self, m: ProjectMemberSummary) -> list[Widget]:
        """Member read view: display name, a blank line, then username / role /
        active / joined."""
        return [
            Static(
                m.display_name or m.username or "-",
                markup=False,
                classes="hub-detail-title",
            ),
            Static("", classes="hub-detail-spacer"),
            detail_row("Username", m.username or "-"),
            detail_row("Role", (m.role or "-").capitalize()),
            detail_row("Active", _active_label(m.active)),
            detail_row("Joined", format_relative(m.joined_at)),
        ]
