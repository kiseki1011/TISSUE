from __future__ import annotations

import logging

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Input, Select, Static
from textual_timepiece.pickers import DateTimePicker
from whenever import Instant, PlainDateTime

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.widgets.datetime_pickers import DueDateTimePicker as _DueDateTimePicker

log = logging.getLogger(__name__)


_PRIORITIES = ["P0", "P1", "P2", "P3", "P4"]
_LABELS = {
    "title": "Title",
    "priority": "Priority",
    "dueAt": "Due date",
    "storyPoint": "Story points",
}
# Per-field dialog modifier class, so the CSS can size each editor to its content
# (a wide calendar for dueAt, a narrow Select for priority, etc.).
_FIELD_CLASS = {
    "title": "-title",
    "priority": "-priority",
    "dueAt": "-datetime",
    "storyPoint": "-sp",
}
_PLACEHOLDERS = {
    "storyPoint": "integer (empty clears)",
}


class IssueFieldEditModal(TissueModal[bool | None]):
    """Edit one editable issue field (title / priority / due date / story points).

    `priority` is a `Select` of P0-P4, `dueAt` a `DateTimePicker` (edited in local
    time, stored as a UTC instant) whose calendar auto-opens on mount and offers a
    `Clear` button, and the rest a free `Input` (empty story points clears). Saves
    via `IssueService`, then dismisses `True` so the caller re-renders, or `None` on
    cancel.
    """

    CSS_PATH = "issue_field_edit_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self, *, issue_key: str, field: str, current_value: str | None
    ) -> None:
        super().__init__()
        self._issue_key = issue_key
        self._field = field
        self._current = current_value or ""

    def compose(self) -> ComposeResult:
        # A per-field modifier class sizes the dialog (and, for dueAt, lets the CSS
        # grow it while the tall calendar/time overlay is open).
        dialog_classes = f"dialog {_FIELD_CLASS.get(self._field, '')}".strip()
        with Container(id="ife-dialog", classes=dialog_classes):
            if self._field == "priority":
                initial = self._current if self._current in _PRIORITIES else "P2"
                yield Select(
                    [(p, p) for p in _PRIORITIES],
                    value=initial,
                    allow_blank=False,
                    id="ife-select",
                )
            elif self._field == "dueAt":
                yield _DueDateTimePicker(
                    value=self._initial_datetime(), id="ife-datetime"
                )
            else:
                yield Input(
                    value=self._current,
                    placeholder=_PLACEHOLDERS.get(self._field, ""),
                    id="ife-input",
                )
            yield Static("", id="ife-status", classes="status-msg")
            with Horizontal(classes="ife-actions"):
                if self._field == "dueAt":
                    yield Button("Clear", id="ife-clear", classes="-btn-warning")
                yield Button("Cancel", id="ife-cancel", classes="-btn-error")
                yield Button("Save", id="ife-save", classes="-btn-success")

    def _initial_datetime(self) -> PlainDateTime | None:
        """Parse the stashed ISO instant into a local wall-clock datetime for the
        picker (the server stores a UTC instant; the user edits in local time)."""
        if not self._current:
            return None
        try:
            return Instant.parse_iso(self._current).to_system_tz().to_plain()
        except ValueError:
            return None

    def on_mount(self) -> None:
        dialog = self.query_one("#ife-dialog", Container)
        dialog.border_title = f"Edit {_LABELS.get(self._field, self._field)}"
        dialog.border_subtitle = "Esc to cancel"
        if self._field == "dueAt":
            # Grow the dialog while the calendar/time overlay is open so it stays
            # inside the box, and auto-open it (after a refresh, so the DOM has
            # settled) — saving the user the extra click to expand the dropdown.
            picker = self.query_one("#ife-datetime", DateTimePicker)
            self.watch(picker, "expanded", self._on_picker_expanded)
            self.call_after_refresh(self._open_due_picker)
        else:
            target = self.query("#ife-select") or self.query("#ife-input")
            if target:
                target.first().focus()

    def _open_due_picker(self) -> None:
        self.query_one("#ife-datetime", DateTimePicker).expanded = True

    def _on_picker_expanded(self, expanded: bool) -> None:
        self.query_one("#ife-dialog", Container).set_class(bool(expanded), "-expanded")

    @on(Button.Pressed, "#ife-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#ife-clear")
    def _on_clear(self) -> None:
        """Clear the due date: blank the picker, then save (-> clear_due_at)."""
        self.query_one("#ife-datetime", DateTimePicker).datetime = None
        self._save()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Input.Submitted, "#ife-input")
    def _on_submit(self) -> None:
        self._save()

    @on(Button.Pressed, "#ife-save")
    def _on_save(self) -> None:
        self._save()

    def _save(self) -> None:
        self.run_worker(self._do_save(), exclusive=True, group="ife-save")

    def _value(self) -> str:
        if self._field == "priority":
            value = self.query_one("#ife-select", Select).value
            return "" if value is Select.BLANK else str(value)
        if self._field == "dueAt":
            picked = self.query_one("#ife-datetime", DateTimePicker).datetime
            if picked is None:
                return ""
            # Local wall-clock -> UTC instant (ISO-8601) for the server.
            return picked.assume_system_tz().to_instant().format_iso()
        return self.query_one("#ife-input", Input).value.strip()

    def _error(self, message: str) -> None:
        self.query_one("#ife-status", Static).update(message)

    async def _do_save(self) -> None:
        client = self.app.client
        if client is None:
            return
        value = self._value()
        # Mirror the backend's IssueConstraintPolicy.TITLE_MAX_LENGTH (50).
        if self._field == "title" and not (2 <= len(value) <= 50):
            self._error("Title must be 2-50 characters.")
            return
        if self._field == "storyPoint" and value and not value.isdigit():
            self._error("Story points must be a non-negative integer.")
            return
        try:
            await self._apply(client, value)
        except TissueApiError as e:
            self._error(getattr(e, "detail", None) or str(e) or "Update failed.")
            return
        self.dismiss(True)

    async def _apply(self, client, value: str) -> None:
        issues = client.issues
        key = self._issue_key
        if self._field == "title":
            await issues.update_common_fields(key, title=value)
        elif self._field == "priority":
            await issues.update_common_fields(key, priority=value)
        elif self._field == "dueAt":
            if value:
                await issues.update_common_fields(key, due_at=value)
            else:
                await issues.update_common_fields(key, clear_due_at=True)
        elif self._field == "storyPoint":
            await issues.update_story_point(key, int(value) if value else None)
