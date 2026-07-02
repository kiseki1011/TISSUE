from __future__ import annotations

from typing import Any

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.widgets import Button, Input, Static, TextArea
from textual_timepiece.pickers import DateTimePicker
from whenever import Instant, PlainDateTime

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.widgets.datetime_pickers import DueDateTimePicker


def sprint_field_edits(
    *, title: str, goal: str, due_at: str, original: dict[str, str], show_due: bool
) -> dict[str, Any]:
    """The changed sprint fields as update_sprint kwargs, omitting unchanged."""
    edits: dict[str, Any] = {}
    if title != original.get("title", ""):
        edits["title"] = title
    if goal != original.get("goal", ""):
        edits["goal"] = goal
    if show_due and due_at and due_at != original.get("dueAt", ""):
        edits["due_at"] = due_at
    return edits


class EditSprintModal(TissueModal[bool | None]):
    """Edit a sprint's fields in one form, saving only what changed."""

    CSS_PATH = "edit_sprint_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self, *, sprint_id: int, current: dict[str, str], show_due: bool = False
    ) -> None:
        super().__init__()
        self._sprint_id = sprint_id
        self._current = current
        self._show_due = show_due

    def compose(self) -> ComposeResult:
        with Container(id="esm-dialog", classes="dialog"):
            with VerticalScroll(id="esm-scroll"), Vertical(id="esm-form"):
                title_input = Input(
                    value=self._current.get("title", ""), id="esm-title"
                )
                title_input.border_title = "Title"
                yield title_input

                goal = TextArea(self._current.get("goal", ""), id="esm-goal")
                goal.border_title = "Goal"
                yield goal

                if self._show_due:
                    due = DueDateTimePicker(value=self._initial_due(), id="esm-due")
                    due.border_title = "Due"
                    yield due
            yield Static("", id="esm-status")
            with Horizontal(id="esm-actions"):
                yield Button("Cancel", id="esm-cancel", classes="-btn-error")
                yield Button("Save", id="esm-save", classes="-btn-success")

    def _initial_due(self) -> PlainDateTime | None:
        raw = self._current.get("dueAt", "")
        if not raw:
            return None
        try:
            return Instant.parse_iso(raw).to_system_tz().to_plain()
        except ValueError:
            return None

    def on_mount(self) -> None:
        dialog = self.query_one("#esm-dialog", Container)
        dialog.border_title = "Edit Sprint"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#esm-title", Input).focus()

    @on(Button.Pressed, "#esm-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#esm-save")
    def _on_save(self) -> None:
        self.run_worker(self._do_save(), exclusive=True, group="esm-save")

    def _error(self, message: str) -> None:
        self.query_one("#esm-status", Static).update(message)

    def _due_value(self) -> str:
        picked = self.query_one("#esm-due", DateTimePicker).datetime
        if picked is None:
            return ""
        return picked.assume_system_tz().to_instant().format_iso()

    async def _do_save(self) -> None:
        client = self.app.client
        if client is None:
            return
        title = self.query_one("#esm-title", Input).value.strip()
        if not (2 <= len(title) <= 50):
            self._error("Title must be 2-50 characters.")
            return
        edits = sprint_field_edits(
            title=title,
            goal=self.query_one("#esm-goal", TextArea).text,
            due_at=self._due_value() if self._show_due else "",
            original=self._current,
            show_due=self._show_due,
        )
        if not edits:
            self.dismiss(None)
            return
        try:
            await client.sprints.update_sprint(self._sprint_id, **edits)
        except TissueApiError as error:
            self._error(
                getattr(error, "detail", None) or str(error) or "Update failed."
            )
            return
        self.dismiss(True)
