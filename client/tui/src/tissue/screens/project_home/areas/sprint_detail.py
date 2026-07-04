from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.widgets import Button, DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.modals.confirm_modal import ConfirmModal
from tissue.screens.project_home.modals.edit_sprint_modal import EditSprintModal
from tissue.screens.project_home.modals.sprint_transition_modal import (
    SprintTransitionModal,
)
from tissue.screens.project_home.sprint_rendering import sprint_detail_widgets

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.sprint_detail import SprintDetail
    from tissue.api.generated.models.sprint_summary import SprintSummary

log = logging.getLogger(__name__)


class SprintDetailMixin(ProjectHomeBase):
    """Loads and mutates the sprint detail shown in [2]."""

    async def _render_sprint_detail(
        self, sprint_id: int, *, focus_detail: bool
    ) -> None:
        if not self._start_sprint_detail():
            return

        sprint = await self._load_sprint(sprint_id)
        if sprint is None:
            return
        issues = await self._load_sprint_issues(sprint_id)
        await self._ensure_members_loaded()
        self._store_sprint_detail(sprint_id, sprint, issues)
        self.refresh_bindings()

        await self._mount_detail(self._sprint_widgets(sprint, issues))
        await self._clear_timeline()
        if focus_detail:
            self._focus_detail_body()

    def _start_sprint_detail(self) -> bool:
        if self.app.client is None:
            return False
        self._detail_state.issue_key = None
        self.add_class("-no-timeline")
        return True

    async def _load_sprint(self, sprint_id: int) -> SprintDetail | None:
        client = self.app.client
        if client is None:
            return None
        try:
            return await client.sprints.get_sprint(sprint_id)
        except TissueApiError as error:
            log.debug("Hub: failed to load sprint %s: %s", sprint_id, error)
            await self._mount_detail(
                [Static("Couldn't load sprint.", classes="hub-muted")]
            )
            await self._clear_timeline()
            return None

    async def _load_sprint_issues(self, sprint_id: int) -> list[IssueSummary]:
        client = self.app.client
        if client is None:
            return []
        try:
            page = await client.issues.search_project_issues(
                self._project_key, sprint_ids=[sprint_id]
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load issues for sprint %s: %s", sprint_id, error)
            return []
        return list(page.content or [])

    def _store_sprint_detail(
        self, sprint_id: int, sprint: SprintDetail, issues: list[IssueSummary]
    ) -> None:
        self._sprint_state.detail_id = sprint_id
        self._sprint_state.detail_issues = issues
        self._sprint_state.detail_status = (sprint.status or "").upper()
        self._sprint_state.edit_current = {
            "title": sprint.title or "",
            "goal": sprint.goal or "",
            "dueAt": sprint.due_at.isoformat() if sprint.due_at else "",
        }

    def _sprint_widgets(
        self, sprint: SprintDetail, issues: list[IssueSummary]
    ) -> list[Widget]:
        return sprint_detail_widgets(
            sprint,
            issues,
            self._state_colors,
            self.app.theme_variables,
            title_class="hub-detail-title",
            content_class="hub-content",
            muted_class="hub-muted",
            issue_title_class="hub-open-title",
            issue_empty_title_class="hub-section-title",
            issue_table_id="hub-sprint-issues-table",
            issue_table_classes="hub-table hub-sprint-issues",
            with_issue_remove=True,
        )

    @on(Button.Pressed, "#hub-sprint-remove-issue")
    def _on_sprint_remove_issue(self) -> None:
        self._remove_focused_sprint_issue()

    def action_remove_from_sprint(self) -> None:
        self._remove_focused_sprint_issue()

    def _remove_focused_sprint_issue(self) -> None:
        issue_key = self._focused_sprint_issue_key()
        if issue_key is None:
            self.app.notify("Focus an issue to remove.", severity="warning")
            return
        self.app.push_screen(
            ConfirmModal(
                message=f"⚠ Remove {issue_key} from this sprint?",
                title="Remove issue",
                confirm_label="Remove",
            ),
            lambda confirmed: self._on_remove_issue_confirmed(issue_key, confirmed),
        )

    @on(DataTable.RowSelected, "#hub-sprint-issues-table")
    def _on_sprint_issue_enter(self, event: DataTable.RowSelected) -> None:
        event.stop()
        row = event.cursor_row
        if 0 <= row < len(self._sprint_state.detail_issues):
            issue_key = self._sprint_state.detail_issues[row].issue_key
            if issue_key:
                self._open_issue_modal(issue_key)

    def _focused_sprint_issue_key(self) -> str | None:
        panel = self._detail_panel()
        if panel is None:
            return None
        row = panel.table_cursor_row("hub-sprint-issues-table")
        if row is None:
            return None
        if not (0 <= row < len(self._sprint_state.detail_issues)):
            return None
        return self._sprint_state.detail_issues[row].issue_key

    def _on_remove_issue_confirmed(
        self, issue_key: str, confirmed: bool | None
    ) -> None:
        if not confirmed or self._sprint_state.detail_id is None:
            return
        self.run_worker(
            self._remove_sprint_issue(self._sprint_state.detail_id, issue_key),
            exclusive=True,
            group="hub-detail",
        )

    async def _remove_sprint_issue(self, sprint_id: int, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.sprints.remove_sprint_issues(sprint_id, [issue_key])
        except TissueApiError as error:
            self.app.notify(
                f"Couldn't remove {issue_key}: {error.detail or 'please try again'}",
                severity="error",
            )
            return
        await self._render_sprint_detail(sprint_id, focus_detail=False)

    def _edit_sprint(self) -> None:
        if self._sprint_state.detail_id is None:
            return
        self.app.push_screen(
            EditSprintModal(
                sprint_id=self._sprint_state.detail_id,
                current=dict(self._sprint_state.edit_current),
                show_due=(self._sprint_state.detail_status or "") == "ACTIVE",
            ),
            self._on_sprint_edited,
        )

    def _transition_sprint(self) -> None:
        if (
            self._sprint_state.detail_id is None
            or self._sprint_state.detail_status is None
        ):
            return
        self.app.push_screen(
            SprintTransitionModal(
                sprint_id=self._sprint_state.detail_id,
                status=self._sprint_state.detail_status,
            ),
            self._on_sprint_edited,
        )

    def _on_sprint_edited(self, updated: bool | None) -> None:
        if not updated or self._sprint_state.detail_id is None:
            return
        self._sprint_state.by_id = None
        self.run_worker(
            self._render_sprint_detail(
                self._sprint_state.detail_id, focus_detail=False
            ),
            exclusive=True,
            group="hub-detail",
        )

    async def _ensure_sprint_index(self) -> None:
        if self._sprint_state.by_id is not None:
            return
        client = self.app.client
        if client is None:
            self._sprint_state.by_id = {}
            return
        try:
            page = await client.sprints.list_project_sprints(self._project_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load sprint index: %s", error)
            self._sprint_state.by_id = {}
            return
        self._sprint_state.by_id = {
            sprint.id: sprint
            for sprint in (page.content or [])
            if sprint.id is not None
        }

    def _active_sprint(self) -> SprintSummary | None:
        for sprint in (self._sprint_state.by_id or {}).values():
            if (sprint.status or "").upper() == "ACTIVE":
                return sprint
        return None

    async def _add_issue_to_active_sprint(self, issue_key: str) -> None:
        await self._ensure_sprint_index()
        active = self._active_sprint()
        if active is None or active.id is None:
            self.app.notify("No active sprint to add to.", severity="warning")
            return
        client = self.app.client
        if client is None:
            return
        try:
            await client.sprints.add_sprint_issues(active.id, [issue_key])
        except TissueApiError as error:
            self.app.notify(
                f"Couldn't add {issue_key}: {error.detail or 'please try again'}",
                severity="error",
            )
            return
        self.app.notify(
            f"Added {issue_key} to {active.sprint_key or 'the active sprint'}."
        )
        self._reflect_issue_sprint_change(issue_key, active.id)

    async def _remove_issue_from_active_sprint(self, issue_key: str) -> None:
        await self._ensure_sprint_index()
        active = self._active_sprint()
        if active is None or active.id is None:
            self.app.notify("No active sprint to remove from.", severity="warning")
            return
        client = self.app.client
        if client is None:
            return
        try:
            await client.sprints.remove_sprint_issues(active.id, [issue_key])
        except TissueApiError as error:
            self.app.notify(
                f"Couldn't remove {issue_key}: {error.detail or 'please try again'}",
                severity="error",
            )
            return
        self.app.notify(
            f"Removed {issue_key} from {active.sprint_key or 'the active sprint'}."
        )
        self._reflect_issue_sprint_change(issue_key, None)

    def _reflect_issue_sprint_change(
        self, issue_key: str, sprint_id: int | None
    ) -> None:
        for summary in (*self._issue_list.issues, *self._agent_work.issues):
            if summary.issue_key == issue_key:
                summary.sprint_id = sprint_id
        if self._detail_state.issue_key == issue_key:
            self._refresh_detail(issue_key)

    async def _clear_timeline(self) -> None:
        panel = self._activity_panel()
        if panel is not None:
            await panel.clear()
