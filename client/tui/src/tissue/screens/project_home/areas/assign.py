from __future__ import annotations

import logging

from textual import on
from textual.widgets import Button

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.modals.member_picker_modal import (
    UNASSIGN,
    MemberPickerModal,
)

log = logging.getLogger(__name__)


class AssignMixin(ProjectHomeBase):
    """Set or clear who the [2] issue is assigned to, using the member picker."""

    @on(Button.Pressed, "#hub-assignee-edit")
    def _on_assignee_pressed(self) -> None:
        if self._detail_issue_key is None:
            return
        self.app.push_screen(
            MemberPickerModal(self._members, assigned=self._detail_assigned),
            self._on_member_picked,
        )

    def _on_member_picked(self, result: int | None) -> None:
        issue_key = self._detail_issue_key
        if result is None or issue_key is None:
            return
        worker = (
            self._unassign(issue_key)
            if result == UNASSIGN
            else self._assign(issue_key, result)
        )
        self.run_worker(worker, exclusive=True, group="hub-detail")

    async def _assign(self, issue_key: str, member_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.assign_issue(issue_key, member_id)
        except TissueApiError as error:
            log.debug("Hub: assign failed for %s: %s", issue_key, error)
            self.app.notify("Assign failed.", severity="error")
            return
        await self._render_issue_detail(issue_key, focus_detail=False, force=True)

    async def _unassign(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.unassign_issue(issue_key)
        except TissueApiError as error:
            log.debug("Hub: unassign failed for %s: %s", issue_key, error)
            self.app.notify("Unassign failed.", severity="error")
            return
        await self._render_issue_detail(issue_key, focus_detail=False, force=True)
