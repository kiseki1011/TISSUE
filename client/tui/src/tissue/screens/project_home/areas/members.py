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
from tissue.screens.project_home.member_filter import DEFAULT_MEMBER_FILTER
from tissue.screens.project_home.rendering import _issue_rows
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row

if TYPE_CHECKING:
    from tissue.api.client import TissueClient
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

_ELEVATED_SYSTEM_ROLES = ("ADMIN", "SUPER_ADMIN")

log = logging.getLogger(__name__)


def _active_label(active: bool | None) -> str:
    if active is None:
        return "-"
    return "Yes" if active else "No"


def member_read_view(
    member: ProjectMemberSummary, *, title_class: str, spacer_class: str
) -> list[Widget]:
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
    """Members list and member detail view."""

    async def _load_members(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            self._member_list.members = (
                await client.project_members.list_project_members(self._project_key)
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load members: %s", error)
        self._update_create_button()

    async def _ensure_members_loaded(self) -> None:
        if not self._member_list.members:
            await self._load_members()

    async def _load_members_list(self, keyword: str | None = None) -> None:
        await self._load_members()
        await self._render_members_list(keyword)
        if self._member_list.displayed:
            self._select_member(0)

    async def _render_members_list(self, keyword: str | None = None) -> None:
        panel = self._issue_list_panel()
        if panel is None:
            return
        members = self._filtered_members(keyword)
        self._member_list.displayed = members
        if not members:
            await panel.replace_content(
                [Static(self._empty_member_text(keyword), classes="hub-list-empty")]
            )
            return
        await panel.replace_content(
            [
                _DashTable(
                    [("#", None), ("Name", None), ("Role", 10), ("Active", 8)],
                    self._member_rows(members),
                    id="hub-members-table",
                    classes="hub-table",
                )
            ]
        )

    def _filtered_members(self, keyword: str | None) -> list[ProjectMemberSummary]:
        members = self._member_list.members
        if keyword:
            keyword_folded = keyword.casefold()
            members = [
                member
                for member in members
                if keyword_folded in (member.display_name or "").casefold()
                or keyword_folded in (member.username or "").casefold()
            ]
        return [member for member in members if self._filters.member.matches(member)]

    def _empty_member_text(self, keyword: str | None) -> str:
        has_filter = bool(keyword) or self._filters.member != DEFAULT_MEMBER_FILTER
        return "No matching members." if has_filter else "No members."

    def _member_rows(
        self, members: list[ProjectMemberSummary]
    ) -> list[list[str | Text]]:
        return [
            [
                str(index + 1),
                Text(member.display_name or member.username or "-"),
                (member.role or "-").capitalize(),
                _active_label(member.active),
            ]
            for index, member in enumerate(members)
        ]

    @on(DataTable.RowHighlighted, "#hub-members-table")
    def _on_member_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_member(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-members-table")
    def _on_member_selected(self, event: DataTable.RowSelected) -> None:
        self._select_member(event.cursor_row, focus_detail=True)

    def _select_member(self, row_index: int, *, focus_detail: bool = False) -> None:
        if not (0 <= row_index < len(self._member_list.displayed)):
            return
        member = self._member_list.displayed[row_index]
        if focus_detail and self._ui.expanded:
            self._open_member_modal(member)
            return
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
        self._detail_state.issue_key = None
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
            self._focus_detail_body()

    def _open_member_modal(self, member: ProjectMemberSummary) -> None:
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
