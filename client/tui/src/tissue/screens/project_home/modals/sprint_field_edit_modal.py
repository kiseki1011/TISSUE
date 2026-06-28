from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Input, Static
from textual_timepiece.pickers import DateTimePicker
from whenever import Instant, PlainDateTime

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.widgets.datetime_pickers import DueDateTimePicker as _DueDateTimePicker

_LABELS = {"title": "Title", "dueAt": "Due date"}
_FIELD_CLASS = {"title": "-title", "dueAt": "-datetime"}


class SprintFieldEditModal(TissueModal[bool | None]):
    """Edit one sprint field."""

    CSS_PATH = "sprint_field_edit_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self, *, sprint_id: int, field: str, current_value: str | None
    ) -> None:
        super().__init__()
        self._sprint_id = sprint_id
        self._field = field
        self._current_value = current_value or ""

    def compose(self) -> ComposeResult:
        dialog_classes = f"dialog {_FIELD_CLASS.get(self._field, '')}".strip()
        with Container(id="sfe-dialog", classes=dialog_classes):
            if self._field == "dueAt":
                yield _DueDateTimePicker(
                    value=self._initial_datetime(), id="sfe-datetime"
                )
            else:
                yield Input(value=self._current_value, id="sfe-input")
            yield Static("", id="sfe-status", classes="status-msg")
            with Horizontal(classes="sfe-actions"):
                yield Button("Cancel", id="sfe-cancel", classes="-btn-error")
                yield Button("Save", id="sfe-save", classes="-btn-success")

    def _initial_datetime(self) -> PlainDateTime | None:
        """The server stores a UTC time. The user edits in their local time."""
        if not self._current_value:
            return None
        try:
            return Instant.parse_iso(self._current_value).to_system_tz().to_plain()
        except ValueError:
            return None

    def on_mount(self) -> None:
        dialog = self.query_one("#sfe-dialog", Container)
        dialog.border_title = f"Edit {_LABELS.get(self._field, self._field)}"
        dialog.border_subtitle = "Esc to cancel"
        if self._field == "dueAt":
            picker = self.query_one("#sfe-datetime", DateTimePicker)
            self.watch(picker, "expanded", self._on_picker_expanded)
            self.call_after_refresh(self._open_due_picker)
        else:
            self.query_one("#sfe-input", Input).focus()

    def _open_due_picker(self) -> None:
        self.query_one("#sfe-datetime", DateTimePicker).expanded = True

    def _on_picker_expanded(self, expanded: bool) -> None:
        self.query_one("#sfe-dialog", Container).set_class(bool(expanded), "-expanded")

    @on(Button.Pressed, "#sfe-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Input.Submitted, "#sfe-input")
    def _on_submit(self) -> None:
        self._save()

    @on(Button.Pressed, "#sfe-save")
    def _on_save(self) -> None:
        self._save()

    def _save(self) -> None:
        self.run_worker(self._do_save(), exclusive=True, group="sfe-save")

    def _value(self) -> str:
        if self._field == "dueAt":
            picked = self.query_one("#sfe-datetime", DateTimePicker).datetime
            if picked is None:
                return ""
            return picked.assume_system_tz().to_instant().format_iso()
        return self.query_one("#sfe-input", Input).value.strip()

    def _error(self, message: str) -> None:
        self.query_one("#sfe-status", Static).update(message)

    async def _do_save(self) -> None:
        client = self.app.client
        if client is None:
            return
        value = self._value()
        if self._field == "title" and not (2 <= len(value) <= 50):
            self._error("Title must be 2-50 characters.")
            return
        if self._field == "dueAt" and not value:
            self._error("Pick a due date.")
            return
        try:
            await self._apply(client, value)
        except TissueApiError as error:
            self._error(
                getattr(error, "detail", None) or str(error) or "Update failed."
            )
            return
        self.dismiss(True)

    async def _apply(self, client, value: str) -> None:
        sprints = client.sprints
        sprint_id = self._sprint_id
        if self._field == "title":
            await sprints.update_sprint(sprint_id, title=value)
        elif self._field == "dueAt":
            await sprints.update_sprint(sprint_id, due_at=value)
