from __future__ import annotations

import logging

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.modals.transition_picker_modal import (
    TransitionPickerModal,
)

log = logging.getLogger(__name__)


class TransitionsMixin(ProjectHomeBase):
    """Issue status transitions (the issue branch of the `t` action)."""

    def _transition_issue(self) -> None:
        if self._detail_state.issue_key is None or not self._transitions_by_id:
            return
        self.app.push_screen(
            TransitionPickerModal(
                list(self._transitions_by_id.values()),
                self._transition_current_label,
                self._transition_target_labels,
            ),
            self._on_transition_picked,
        )

    def _on_transition_picked(self, transition_id: int | None) -> None:
        issue_key = self._detail_state.issue_key
        if transition_id is None or issue_key is None:
            return
        self.run_worker(
            self._perform_transition(issue_key, transition_id),
            exclusive=True,
            group="hub-detail",
        )

    async def _perform_transition(self, issue_key: str, transition_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.perform_transition(issue_key, transition_id)
        except TissueApiError as error:
            log.debug("Hub: transition failed for %s: %s", issue_key, error)
            self.app.notify("Transition failed.", severity="error")
            return
        await self._render_issue_detail(issue_key, focus_detail=False, force=True)
