from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.widget import Widget
from textual.widgets import DataTable, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.rendering import _issue_rows
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

# System roles worth surfacing on a member (USER is the unremarkable default).
_ELEVATED_SYSTEM_ROLES = ("ADMIN", "SUPER_ADMIN")

log = logging.getLogger(__name__)


def _active_label(active: bool | None) -> str:
    """Yes / No, or '-' when unknown — `active` is an Optional bool, so don't
    conflate a missing value with an explicit False."""
    if active is None:
        return "-"
    return "Yes" if active else "No"


def member_read_view(
    m: ProjectMemberSummary, *, title_class: str, spacer_class: str
) -> list[Widget]:
    """Member read view: display name, a blank line, then username / email / role /
    (system role, only when elevated) / active / joined. Shared by the hub's [2]
    detail pane and the expanded-mode MemberDetailModal so the two can't drift;
    callers pass their own CSS classes."""
    widgets: list[Widget] = [
        Static(m.display_name or m.username or "-", markup=False, classes=title_class),
        Static("", classes=spacer_class),
        detail_row("Username", m.username or "-"),
        detail_row("Email", m.email or "-"),
        detail_row("Role", (m.role or "-").capitalize()),
    ]
    # System role (SUPER_ADMIN/ADMIN/USER) is orthogonal to the project role; surface
    # it only when elevated, since USER is the unremarkable default for most members.
    system_role = (m.system_role or "").upper()
    if system_role in _ELEVATED_SYSTEM_ROLES:
        widgets.append(detail_row("System role", system_role.replace("_", " ").title()))
    widgets.extend(
        [
            detail_row("Active", _active_label(m.active)),
            detail_row("Joined", format_relative(m.joined_at)),
        ]
    )
    return widgets


def member_issue_section(
    label: str,
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    *,
    table_id: str,
    title_class: str,
    muted_class: str,
) -> list[Widget]:
    """A '<label> (n)' section title + a read-only issues table (or a muted 'None.'
    when empty). Used for the Assigned / Reviewing issue lists in a member's detail."""
    widgets: list[Widget] = [Static(f"{label} ({len(issues)})", classes=title_class)]
    if issues:
        widgets.append(
            _DashTable(
                [("Key", 10), ("Title", None), ("Status", 11), ("Priority", 8)],
                _issue_rows(issues, state_colors, theme_variables),
                id=table_id,
                classes="hub-table hub-sprint-issues",
            )
        )
    else:
        widgets.append(Static("None.", classes=muted_class))
    return widgets


async def fetch_member_issues(
    client: TissueClient | None, project_key: str, member_id: int | None
) -> tuple[list[IssueSummary], list[IssueSummary]]:
    """A member's (assigned, reviewing) issues — two separate searches (the backend
    filters by assignee and by reviewer independently). Best-effort: a failed search
    yields an empty list. Shared by the hub's [2] detail and the MemberDetailModal."""
    assigned: list[IssueSummary] = []
    reviewing: list[IssueSummary] = []
    if client is None or member_id is None:
        return assigned, reviewing
    member_ids = [str(member_id)]
    try:
        page = await client.issues.search_project_issues(
            project_key, assignee_member_ids=member_ids
        )
        assigned = list(page.content or [])
    except TissueApiError as e:
        log.debug("Hub: failed to load assigned issues for %s: %s", member_id, e)
    try:
        page = await client.issues.search_project_issues(
            project_key, reviewer_member_ids=member_ids
        )
        reviewing = list(page.content or [])
    except TissueApiError as e:
        log.debug("Hub: failed to load reviewing issues for %s: %s", member_id, e)
    return assigned, reviewing


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

    async def _ensure_members_loaded(self) -> None:
        """Populate `self._members` if it isn't already — for the filter modal's
        Assignee picker, which can open before the Issues view has loaded the roster.
        No-op once present (the roster is the full set; the Members view filters a
        copy for display, so this never clobbers a search)."""
        if not self._members:
            await self._load_members()

    async def _load_members_list(self, keyword: str | None = None) -> None:
        # Refetch so the roster is fresh on each switch (mirrors issues/sprints);
        # the mount-time load only seeds name resolution before any view shows.
        await self._load_members()
        await self._render_members_list(keyword)
        # Seed the detail with the first shown member so [2] isn't stale on switch.
        if self._displayed_members:
            self._select_member(0)

    async def _render_members_list(self, keyword: str | None = None) -> None:
        box = self.query_one("#hub-list-host")
        await box.remove_children()
        # Filter the FULL roster client-side for display; `self._members` stays whole
        # (name resolution depends on it). `_displayed_members` is what the table
        # shows and what row selection indexes — the two never desync.
        members = self._members
        if keyword:
            kw = keyword.casefold()
            members = [
                m
                for m in members
                if kw in (m.display_name or "").casefold()
                or kw in (m.username or "").casefold()
            ]
        self._displayed_members = members
        if not members:
            placeholder = "No matching members." if keyword else "No members."
            await box.mount(Static(placeholder, classes="hub-muted"))
            return
        rows: list[list[str | Text]] = [
            [
                Text(m.display_name or m.username or "-"),
                (m.role or "-").capitalize(),
                _active_label(m.active),
            ]
            for m in members
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
        if not (0 <= idx < len(self._displayed_members)):
            return
        member = self._displayed_members[idx]
        # Expanded mode hides [2]; an explicit Enter pops the detail as a modal
        # (mirrors the issues list).
        if focus_detail and self._expanded:
            self._open_member_modal(member)
            return
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
        # Members have no activity timeline at all — hide the column (and its
        # separating line) so the member fields take the full detail width.
        self.add_class("-no-timeline")
        widgets = member_read_view(
            member, title_class="hub-detail-title", spacer_class="hub-detail-spacer"
        )
        # The member's issues across the project: assigned to them, and where they're
        # a reviewer. Two separate searches (lazy, on selection).
        assigned, reviewing = await fetch_member_issues(
            self.app.client, self._project_key, member.member_id
        )
        widgets.append(Rule())
        widgets.extend(
            member_issue_section(
                "Assigned",
                assigned,
                self._state_colors,
                self.app.theme_variables,
                table_id="hub-member-assigned",
                title_class="hub-section-title",
                muted_class="hub-muted",
            )
        )
        widgets.extend(
            member_issue_section(
                "Reviewing",
                reviewing,
                self._state_colors,
                self.app.theme_variables,
                table_id="hub-member-reviewing",
                title_class="hub-section-title",
                muted_class="hub-muted",
            )
        )
        await self._mount_detail(widgets)
        await self._clear_timeline()
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    def _open_member_modal(self, member: ProjectMemberSummary) -> None:
        """Pop a read-only member detail modal (expanded mode, where [2] is hidden)."""
        from tissue.screens.project_home.modals.member_detail_modal import (
            MemberDetailModal,
        )

        self.app.push_screen(
            MemberDetailModal(
                member=member,
                project_key=self._project_key,
                state_colors=self._state_colors,
            )
        )
