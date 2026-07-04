from __future__ import annotations

import logging
from typing import TYPE_CHECKING, Any

from textual import on
from textual.widgets import DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.areas.issue_actions import IssueActionsMixin
from tissue.screens.project_home.areas.issue_search import IssueSearchMixin
from tissue.screens.project_home.constants import (
    _DETAIL_PREFETCH_AFTER,
    _DETAIL_PREFETCH_BEFORE,
    _ISSUE_VIEWS,
)
from tissue.screens.project_home.issue_table import (
    AGENT_ISSUE_TABLE_ID,
    ISSUE_LIST_FOCUS_TABLE_IDS,
    ISSUE_TABLE_ID,
    issue_table,
    numbered_issue_rows,
    recolor_status_cells,
)
from tissue.screens.project_home.rendering import _issue_list_rows
from tissue.screens.project_home.workflow_colors import load_state_colors

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.page_response_issue_summary import (
        PageResponseIssueSummary,
    )

log = logging.getLogger(__name__)


class IssuesMixin(IssueActionsMixin, IssueSearchMixin, ProjectHomeBase):
    """The [1] Issues list."""

    async def _load_issues(self, keyword: str | None = None) -> None:
        self._issue_list.keyword = keyword
        page = await self._fetch_issue_page(keyword=keyword)
        self._replace_issues(page)
        self._issue_list.page = 0
        if not self._member_list.members:
            await self._load_members()
        await self._render_issues()
        self._refresh_box_chrome()
        self._select_first_issue_or_reset_detail()

    async def _fetch_issue_page(
        self, *, keyword: str | None, page: int = 0
    ) -> PageResponseIssueSummary | None:
        client = self.app.client
        if client is None:
            return None
        try:
            return await client.issues.search_project_issues(
                self._project_key,
                keyword=keyword,
                state_categories=self._filters.issue.state_categories_arg(),
                priorities=self._filters.issue.priorities_arg(),
                assignee_member_ids=self._filters.issue.assignee_arg(),
                sprint_ids=self._filters.issue.sprint_ids_arg(),
                current_sprint_only=self._filters.issue.current_sprint_only_arg(),
                page=page,
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load issues: %s", error)
            return None

    def _replace_issues(self, page: PageResponseIssueSummary | None) -> None:
        if page is None:
            self._issue_list.issues = []
            self._issue_list.total = 0
            self._issue_list.has_next = False
            return
        self._issue_list.issues = list(page.content or [])
        self._issue_list.total = page.total_elements or len(self._issue_list.issues)
        self._issue_list.has_next = bool(page.has_next)

    def _select_first_issue_or_reset_detail(self) -> None:
        if self._issue_list.issues:
            self._select_issue(0)
            return
        self._cancel_detail_timer()
        self.run_worker(self._reset_detail_pane(), exclusive=True, group="hub-detail")

    def _member_names(self) -> dict[int, str]:
        return {
            member.member_id: (member.display_name or member.username or "-")
            for member in self._member_list.members
            if member.member_id is not None
        }

    def _sync_list_row(self, common: IssueCommonDetail) -> None:
        """Refresh the cached summary and [1] row after a detail-pane edit."""
        issue_key = common.issue_key
        if issue_key is None:
            return
        mode = self._ui.view_mode
        if mode == "issues":
            issues = self._issue_list.issues
            table_id = ISSUE_TABLE_ID
        elif mode in ("agent", "reviews"):
            issues = self._agent_work.issues
            table_id = AGENT_ISSUE_TABLE_ID
        else:
            return
        panel = self._issue_list_panel()
        if panel is None:
            return
        index = next(
            (i for i, summary in enumerate(issues) if summary.issue_key == issue_key),
            None,
        )
        if index is None:
            return
        self._apply_common_to_summary(issues[index], common)
        # Read the review-chip layout from the mounted table, not view_mode: a
        # stale revalidate can land mid-switch while the old table is still up.
        reviews = panel.table_column_index(table_id, "Review") is not None
        self._update_list_row_cells(table_id, index, issues[index], reviews=reviews)

    def _apply_common_to_summary(
        self, summary: IssueSummary, common: IssueCommonDetail
    ) -> None:
        summary.title = common.title
        summary.priority = common.priority
        summary.story_point = common.story_point
        summary.due_at = common.due_at
        state = common.current_state
        summary.current_state_id = state.id if state else None
        summary.current_state_label = state.display_name if state else None
        summary.current_state_category = state.category if state else None
        summary.assignee_member_id = (
            common.assignee.member_id if common.assignee else None
        )

    def _update_list_row_cells(
        self, table_id: str, index: int, summary: IssueSummary, *, reviews: bool
    ) -> None:
        panel = self._issue_list_panel()
        if panel is None:
            return
        member_names = self._member_names()
        if table_id == AGENT_ISSUE_TABLE_ID:
            member_names = {**member_names, **self._agent_work.names}
        row = _issue_list_rows(
            [summary],
            self._state_colors,
            self.app.theme_variables,
            member_names,
            with_review_status=reviews,
            title_extra=self._title_extra(),
        )[0]
        column_offset = 1 if table_id == ISSUE_TABLE_ID else 0
        for cell_index, value in enumerate(row):
            panel.update_table_cell(table_id, index, column_offset + cell_index, value)

    async def _render_issues(self) -> None:
        panel = self._issue_list_panel()
        if panel is None:
            return
        if not self._issue_list.issues:
            await panel.replace_content(
                [Static("No issues.", classes="hub-list-empty")]
            )
            return
        table = issue_table(
            self._issue_list.issues,
            self._state_colors,
            self.app.theme_variables,
            self._member_names(),
            title_extra=self._title_extra(),
        )
        await panel.replace_content([table])
        self.watch(table, "scroll_y", self._on_issues_scrolled, init=False)
        # Colors load in a separate worker; if they arrived while this table was
        # mid-mount, the state-color recolor no-op'd, so apply them now.
        self._recolor_status_table(ISSUE_TABLE_ID, self._issue_list.issues)

    async def _reflow_list_titles(self) -> None:
        """Re-render the [1] list so titles re-fit after an expand/collapse."""
        mode = self._ui.view_mode
        if mode not in _ISSUE_VIEWS:
            return
        panel = self._issue_list_panel()
        if panel is None:
            return
        table_id = ISSUE_TABLE_ID if mode == "issues" else AGENT_ISSUE_TABLE_ID
        table = panel.table(table_id)
        cursor = None if table is None else table.cursor_row
        had_focus = table is not None and table.has_focus
        if mode == "issues":
            if not self._issue_list.issues:
                return
            await self._render_issues()
        else:
            if not self._agent_work.issues:
                return
            await self._render_agent_issues(empty_hint="")
        new_table = panel.table(table_id)
        if new_table is None or cursor is None:
            return
        row = min(cursor, new_table.row_count - 1)
        if row < 0:
            return
        new_table.move_cursor(row=row)
        if mode == "issues":
            self._select_issue(row)
        else:
            self._select_agent_issue(row)
        if had_focus:
            new_table.focus()

    def _numbered_issue_rows(
        self, issues: list[IssueSummary], *, start: int = 0
    ) -> list[list[Any]]:
        return numbered_issue_rows(
            issues,
            self._state_colors,
            self.app.theme_variables,
            self._member_names(),
            start=start,
            title_extra=self._title_extra(),
        )

    async def _load_more_issues(self) -> None:
        if self._issue_list.loading_more or not self._issue_list.has_next:
            return
        self._issue_list.loading_more = True
        try:
            page = await self._fetch_issue_page(
                keyword=self._issue_list.keyword, page=self._issue_list.page + 1
            )
            if page is None:
                return
            self._append_issue_page(page)
        finally:
            self._issue_list.loading_more = False

    def _append_issue_page(self, page: PageResponseIssueSummary) -> None:
        new_issues = list(page.content or [])
        self._issue_list.page += 1
        self._issue_list.has_next = bool(page.has_next)
        if not new_issues:
            return
        self._issue_list.issues.extend(new_issues)
        self._append_issue_rows(new_issues)

    def _append_issue_rows(self, issues: list[IssueSummary]) -> None:
        panel = self._issue_list_panel()
        if panel is None:
            return
        start = len(self._issue_list.issues) - len(issues)
        rows = self._numbered_issue_rows(issues, start=start)
        if panel.add_table_rows(ISSUE_TABLE_ID, rows):
            self._recolor_status_table(ISSUE_TABLE_ID, self._issue_list.issues)

    def _on_issues_scrolled(self, _scroll_y: float) -> None:
        if self._issue_list.loading_more or not self._issue_list.has_next:
            return
        panel = self._issue_list_panel()
        if panel is None:
            return
        if panel.table_near_bottom(ISSUE_TABLE_ID, threshold=3):
            self.run_worker(
                self._load_more_issues(), exclusive=True, group="hub-load-more"
            )

    async def _load_state_colors(self) -> None:
        client = self.app.client
        if client is None:
            return
        self._state_colors = await load_state_colors(client, self._workflow_cache, log)
        self._recolor_table_status()

    def _recolor_table_status(self) -> None:
        self._recolor_status_table(ISSUE_TABLE_ID, self._issue_list.issues)
        self._recolor_status_table(AGENT_ISSUE_TABLE_ID, self._agent_work.issues)

    def _recolor_status_table(self, table_id: str, issues: list[IssueSummary]) -> None:
        panel = self._issue_list_panel()
        if panel is None:
            return
        recolor_status_cells(panel, table_id, issues, self._state_colors)

    @on(DataTable.RowHighlighted, f"#{ISSUE_TABLE_ID}")
    def _on_issue_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_issue(event.cursor_row)
            if (
                self._issue_list.has_next
                and not self._issue_list.loading_more
                and event.cursor_row >= len(self._issue_list.issues) - 5
            ):
                self.run_worker(
                    self._load_more_issues(), exclusive=True, group="hub-load-more"
                )

    @on(DataTable.RowSelected, f"#{ISSUE_TABLE_ID}")
    def _on_issue_selected(self, event: DataTable.RowSelected) -> None:
        self._select_issue(event.cursor_row, focus_detail=True)

    def _select_issue(self, index: int, *, focus_detail: bool = False) -> None:
        if not (0 <= index < len(self._issue_list.issues)):
            return
        issue_key = self._issue_list.issues[index].issue_key
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
        self._prefetch_nearby_issue_details(index)

    def _prefetch_nearby_issue_details(self, index: int) -> None:
        start = max(0, index - _DETAIL_PREFETCH_BEFORE)
        end = min(len(self._issue_list.issues), index + _DETAIL_PREFETCH_AFTER + 1)
        issue_keys = [
            issue.issue_key
            for offset, issue in enumerate(
                self._issue_list.issues[start:end], start=start
            )
            if offset != index
            and issue.issue_key is not None
            and issue.issue_key not in self._detail_state.cache
        ]
        if issue_keys:
            self.run_worker(
                self._prefetch_issue_details(issue_keys),
                exclusive=True,
                group="hub-detail-prefetch",
            )

    def action_focus_issues(self) -> None:
        panel = self._issue_list_panel()
        if panel is None:
            return
        if panel.focus_first_table(ISSUE_LIST_FOCUS_TABLE_IDS):
            return
        panel.focus_host()

    def action_add_to_sprint(self) -> None:
        """`s` toggles: remove if the issue is already in the active sprint."""
        issue_key = self._detail_state.issue_key
        if issue_key is None:
            return
        worker = (
            self._remove_issue_from_active_sprint(issue_key)
            if self._focused_in_active_sprint()
            else self._add_issue_to_active_sprint(issue_key)
        )
        self.run_worker(worker, exclusive=True, group="hub-add-sprint")
