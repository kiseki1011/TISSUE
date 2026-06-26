from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.widget import Widget
from textual.widgets import Button

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.modals.transition_picker_modal import (
    TransitionPickerModal,
)
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.available_transition import AvailableTransition

log = logging.getLogger(__name__)


class TransitionsMixin(ProjectHomeBase):
    """The Status row's `⇄` button, picks a transition then performs it."""

    def _status_action(
        self,
        transitions: list[AvailableTransition],
        current_state_label: str,
        target_labels: dict[int, str],
    ) -> Widget | None:
        """The `⇄` transition button, or None when there are no transitions."""
        if not transitions:
            return None
        self._transition_current_label = current_state_label
        self._transition_target_labels = target_labels
        return TextButton("⇄", id="hub-transition-btn", classes="hub-row-action")

    @on(Button.Pressed, "#hub-transition-btn")
    def _on_transition_button(self) -> None:
        if not self._transitions_by_id:
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
        issue_key = self._detail_issue_key
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
        # Re-render the detail for the new state and the transitions you can now do.
        # `force=True`: just transitioned, so skip the cache and refetch.
        await self._render_issue_detail(issue_key, focus_detail=False, force=True)
