"""A small dialog that creates a sprint: a required title and an optional goal,
submitted together. Opened from the hub's context-aware create button while the
[1] list shows the Sprints view (PROJECT_MANAGER only)."""

from __future__ import annotations

import logging

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical
from textual.widgets import Button, Input, Static, TextArea

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal

log = logging.getLogger(__name__)


class CreateSprintModal(TissueModal[int | None]):
    """Create a sprint in one short form.

    Dismisses with the new sprint's id on success, or None on cancel.
    """

    CSS_PATH = "create_sprint_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(self, *, project_key: str) -> None:
        super().__init__()
        self._project_key = project_key
        # True while a create POST is in flight — blocks a duplicate submit.
        self._submitting = False

    def compose(self) -> ComposeResult:
        with Container(id="csm-dialog", classes="dialog"):
            with Vertical(id="csm-form"):
                title = Input(
                    placeholder="2-50 characters", max_length=50, id="csm-title"
                )
                title.border_title = "Title *"
                yield title
                goal = TextArea(id="csm-goal")
                goal.border_title = "Goal"
                yield goal
            yield Static("", id="csm-status", classes="status-msg")
            with Horizontal(id="csm-actions"):
                yield Button("Cancel", id="csm-cancel", classes="-btn-error")
                yield Button("Create", id="csm-create", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#csm-dialog", Container)
        dialog.border_title = "New sprint"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#csm-title", Input).focus()

    @on(Button.Pressed, "#csm-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#csm-create")
    def _on_create(self) -> None:
        # Synchronous double-submit guard (a second press while the POST is in
        # flight would create a duplicate sprint — cancelling can't un-send it).
        if self._submitting:
            return
        self._submitting = True
        self.run_worker(self._do_create(), group="csm-create")

    def _error(self, message: str) -> None:
        self.query_one("#csm-status", Static).update(message)

    async def _do_create(self) -> None:
        try:
            client = self.app.client
            if client is None:
                return
            title = self.query_one("#csm-title", Input).value.strip()
            if not (2 <= len(title) <= 50):
                self._error("Title must be 2-50 characters.")
                return
            goal = self.query_one("#csm-goal", TextArea).text.strip() or None
            if goal is not None and len(goal) > 255:
                self._error("Goal must be 255 characters or fewer.")
                return
            try:
                result = await client.sprints.create_sprint(
                    self._project_key, title=title, goal=goal
                )
            except TissueApiError as e:
                self._error(getattr(e, "detail", None) or str(e) or "Create failed.")
                return
            self.dismiss(result.sprint_id)
        finally:
            self._submitting = False
