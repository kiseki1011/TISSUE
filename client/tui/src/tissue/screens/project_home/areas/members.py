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

# USER is the plain default, so only these roles are shown on a member.
_ELEVATED_SYSTEM_ROLES = ("ADMIN", "SUPER_ADMIN")

log = logging.getLogger(__name__)


def _active_label(active: bool | None) -> str:
    """None means unknown ('-'), which is not the same as False."""
    if active is None:
        return "-"
    return "Yes" if active else "No"


def member_read_view(
    member: ProjectMemberSummary, *, title_class: str, spacer_class: str
) -> list[Widget]:
    """Build the member detail, shared so the pane and modal match.

    Used by the hub's [2] detail pane and the MemberDetailModal.
    """
    widgets: list[Widget] = [
        Static(
            member.display_name or member.username or "-",
            markup=False,
            classes=title_class,
        ),
        Static("", classes=spacer_class),
        detail_row("Username", member.username or "-"),
        detail_row("Email", member.email or "-"),
        detail_row("Role", (member.role or "-").capitalize()),
    ]
    system_role = (member.system_role or "").upper()
    if system_role in _ELEVATED_SYSTEM_ROLES:
        widgets.append(detail_row("System role", system_role.replace("_", " ").title()))
    widgets.extend(
        [
            detail_row("Active", _active_label(member.active)),
            detail_row("Joined", format_relative(member.joined_at)),
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
    """Section title plus a read-only issues table, or a grey 'None.' if empty."""
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
    """A member's (assigned, reviewing) issues.

    A failed search is skipped and returns an empty list for that side.
    """
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
    except TissueApiError as error:
        log.debug("Hub: failed to load assigned issues for %s: %s", member_id, error)
    try:
        page = await client.issues.search_project_issues(
            project_key, reviewer_member_ids=member_ids
        )
        reviewing = list(page.content or [])
    except TissueApiError as error:
        log.debug("Hub: failed to load reviewing issues for %s: %s", member_id, error)
    return assigned, reviewing


class MembersMixin(ProjectHomeBase):
    """The [1] box's Members view, a member list with each member shown in [2].

    `_load_members` also runs once at mount to look up assignee names, and the
    Members view reuses it before drawing the list.
    """

    async def _load_members(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            self._members = await client.project_members.list_project_members(
                self._project_key
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load members: %s", error)
            # Keep any member list already loaded, so a failed (re)load can't wipe
            # out data an earlier good load fetched.
        # The member list carries the current user's role, which controls the
        # manager-only create button, so refresh now that the role is known.
        self._update_create_button()

    async def _ensure_members_loaded(self) -> None:
        """Fill in `self._members` if not loaded yet.

        The filter modal's Assignee picker can open before the Issues view has
        loaded the member list.
        """
        if not self._members:
            await self._load_members()

    async def _load_members_list(self, keyword: str | None = None) -> None:
        # Refetch so the member list is fresh on each switch, like issues/sprints.
        await self._load_members()
        await self._render_members_list(keyword)
        if self._displayed_members:
            self._select_member(0)

    async def _render_members_list(self, keyword: str | None = None) -> None:
        box = self.query_one("#hub-list-host")
        await box.remove_children()
        # `self._members` stays whole so we can look up names, while
        # `_displayed_members` is the filtered part that row clicks index into.
        members = self._members
        if keyword:
            keyword_folded = keyword.casefold()
            members = [
                member
                for member in members
                if keyword_folded in (member.display_name or "").casefold()
                or keyword_folded in (member.username or "").casefold()
            ]
        self._displayed_members = members
        if not members:
            placeholder = "No matching members." if keyword else "No members."
            await box.mount(Static(placeholder, classes="hub-muted"))
            return
        rows: list[list[str | Text]] = [
            [
                Text(member.display_name or member.username or "-"),
                (member.role or "-").capitalize(),
                _active_label(member.active),
            ]
            for member in members
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

    def _select_member(self, row_index: int, *, focus_detail: bool = False) -> None:
        if not (0 <= row_index < len(self._displayed_members)):
            return
        member = self._displayed_members[row_index]
        # Expanded mode hides [2], so pressing Enter opens the detail as a modal.
        if focus_detail and self._expanded:
            self._open_member_modal(member)
            return
        # Shares the issue-detail worker group so member, issue and sprint draws
        # never end up in [2] at the same time.
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
        # Clear the issue key so any late issue workers stop (they check
        # _detail_issue_key). A member has no comments or activity.
        self._detail_issue_key = None
        # Members have no timeline, so hide that column and its divider line and
        # let the member fields take the full detail width.
        self.add_class("-no-timeline")
        widgets = member_read_view(
            member, title_class="hub-detail-title", spacer_class="hub-detail-spacer"
        )
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
        """Open a read-only member detail modal (expanded mode, where [2] is hidden)."""
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
