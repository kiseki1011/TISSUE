from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Static, TextArea

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal


class SprintGoalEditModal(TissueModal["bool | None"]):
    """Edit a sprint's Markdown goal in a multi-line editor (like the issue body).

    Closes with True after a save so the caller redraws, None on cancel.
    """

    CSS_PATH = "sprint_goal_edit_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(self, *, sprint_id: int, current_goal: str | None) -> None:
        super().__init__()
        self._sprint_id = sprint_id
        self._current_goal = current_goal or ""

    def compose(self) -> ComposeResult:
        with Container(id="sge-dialog", classes="dialog"):
            yield TextArea(self._current_goal, id="sge-editor", language="markdown")
            yield Static("", id="sge-status", classes="status-msg")
            with Horizontal(id="sge-actions"):
                yield Button("Cancel", id="sge-cancel", classes="-btn-error")
                yield Button("Save", id="sge-save", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#sge-dialog", Container)
        dialog.border_title = "Edit Goal"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#sge-editor", TextArea).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#sge-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    @on(Button.Pressed, "#sge-save")
    def _on_save(self, event: Button.Pressed) -> None:
        event.stop()
        self.run_worker(self._do_save(), exclusive=True, group="sge-save")

    async def _do_save(self) -> None:
        client = self.app.client
        if client is None:
            return
        goal = self.query_one("#sge-editor", TextArea).text.strip()
        if len(goal) > 255:
            self.query_one("#sge-status", Static).update(
                "Goal must be at most 255 characters."
            )
            return
        try:
            await client.sprints.update_sprint(self._sprint_id, goal=goal)
        except TissueApiError as error:
            self.query_one("#sge-status", Static).update(
                getattr(error, "detail", None) or str(error) or "Update failed."
            )
            return
        self.dismiss(True)
