from __future__ import annotations

import logging
from typing import TYPE_CHECKING, Any

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.widgets import Button, Input, Select, Static, TextArea
from textual_timepiece.pickers import DateTimePicker
from whenever import Instant, PlainDateTime

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.widgets.custom_field_input import UNSET, CustomFieldInput
from tissue.widgets.datetime_pickers import DueDateTimePicker

if TYPE_CHECKING:
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail

log = logging.getLogger(__name__)

_PRIORITIES = ["P0", "P1", "P2", "P3", "P4"]


def common_field_edits(
    *, title: str, priority: str, content: str, due_at: str, original: dict[str, str]
) -> dict[str, Any]:
    """The changed common fields as update_common_fields kwargs, omitting unchanged."""
    edits: dict[str, Any] = {}
    if title != original.get("title", ""):
        edits["title"] = title
    if priority != original.get("priority", ""):
        edits["priority"] = priority
    if content != original.get("content", ""):
        edits["content"] = content
    if due_at != original.get("dueAt", ""):
        if due_at:
            edits["due_at"] = due_at
        else:
            edits["clear_due_at"] = True
    return edits


class EditIssueModal(TissueModal[bool | None]):
    """Edit an issue's fields in one form, saving only what changed."""

    CSS_PATH = "edit_issue_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self,
        *,
        issue_key: str,
        current: dict[str, str],
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
        show_story_point: bool = True,
    ) -> None:
        super().__init__()
        self._issue_key = issue_key
        self._current = current
        self._custom_fields = custom_fields
        self._options_by_field = options_by_field
        self._show_story_point = show_story_point
        self._cf_baseline: dict[int, Any] = {}

    def compose(self) -> ComposeResult:
        with Container(id="eim-dialog"):
            with VerticalScroll(id="eim-scroll"), Vertical(id="eim-form"):
                title_input = Input(
                    value=self._current.get("title", ""), id="eim-title"
                )
                title_input.border_title = "Title"
                yield title_input

                priority = self._current.get("priority") or "P2"
                select = Select(
                    [(value, value) for value in _PRIORITIES],
                    value=priority if priority in _PRIORITIES else "P2",
                    allow_blank=False,
                    id="eim-priority",
                )
                select.border_title = "Priority"
                yield select

                due_picker = DueDateTimePicker(value=self._initial_due(), id="eim-due")
                due_picker.border_title = "Due"
                yield due_picker

                if self._show_story_point:
                    story_point = Input(
                        value=self._current.get("storyPoint", ""),
                        placeholder="integer (empty clears)",
                        id="eim-sp",
                    )
                    story_point.border_title = "Story points"
                    yield story_point

                content = TextArea(self._current.get("content", ""), id="eim-content")
                content.border_title = "Description"
                yield content

                yield from self._custom_field_inputs()
            yield Static("", id="eim-status")
            with Horizontal(id="eim-actions"):
                yield Button("Cancel", id="eim-cancel", classes="-btn-error")
                yield Button("Save", id="eim-save", classes="-btn-success")

    def _custom_field_inputs(self) -> ComposeResult:
        for field in self._custom_fields:
            if field.field_id is None:
                continue
            label = field.field_label or "Field"
            yield CustomFieldInput(
                field_id=field.field_id,
                label=label[:1].upper() + label[1:],
                ftype=field.issue_field_type or "TEXT",
                required=bool(field.required),
                options=self._options_by_field.get(field.field_id, []),
                value=field.value,
            )

    def _initial_due(self) -> PlainDateTime | None:
        raw = self._current.get("dueAt", "")
        if not raw:
            return None
        try:
            return Instant.parse_iso(raw).to_system_tz().to_plain()
        except ValueError:
            return None

    def on_mount(self) -> None:
        dialog = self.query_one("#eim-dialog", Container)
        dialog.border_title = f"Edit {self._issue_key}"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#eim-title", Input).focus()
        self.call_after_refresh(self._snapshot_custom_fields)

    def _snapshot_custom_fields(self) -> None:
        """Record each field's value at open time, so save sends only edits."""
        for field_input in self.query(CustomFieldInput):
            try:
                self._cf_baseline[field_input.field_id] = field_input.get_value()
            except ValueError:
                self._cf_baseline[field_input.field_id] = UNSET

    @on(Button.Pressed, "#eim-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#eim-save")
    def _on_save(self) -> None:
        self.run_worker(self._do_save(), exclusive=True, group="eim-save")

    def _error(self, message: str) -> None:
        self.query_one("#eim-status", Static).update(message)

    def _due_value(self) -> str:
        picked = self.query_one("#eim-due", DateTimePicker).datetime
        if picked is None:
            return ""
        return picked.assume_system_tz().to_instant().format_iso()

    def _changed_custom_fields(self) -> dict[str, Any]:
        changed: dict[str, Any] = {}
        for field_input in self.query(CustomFieldInput):
            new_value = field_input.get_value()
            if new_value is UNSET:
                continue
            if new_value != self._cf_baseline.get(field_input.field_id):
                changed[str(field_input.field_id)] = new_value
        return changed

    async def _do_save(self) -> None:
        client = self.app.client
        if client is None:
            return
        title = self.query_one("#eim-title", Input).value.strip()
        if not (2 <= len(title) <= 50):
            self._error("Title must be 2-50 characters.")
            return
        priority = str(self.query_one("#eim-priority", Select).value)
        content = self.query_one("#eim-content", TextArea).text
        common = common_field_edits(
            title=title,
            priority=priority,
            content=content,
            due_at=self._due_value(),
            original=self._current,
        )

        story_point_changed = False
        story_point: int | None = None
        if self._show_story_point:
            entered = self.query_one("#eim-sp", Input).value.strip()
            if entered and not entered.isdigit():
                self._error("Story points must be a non-negative integer.")
                return
            if entered != self._current.get("storyPoint", ""):
                story_point_changed = True
                story_point = int(entered) if entered else None

        try:
            changed_custom = self._changed_custom_fields()
        except ValueError as error:
            self._error(str(error))
            return

        if not (common or story_point_changed or changed_custom):
            self.dismiss(None)
            return

        try:
            if common:
                await client.issues.update_common_fields(self._issue_key, **common)
            if story_point_changed:
                await client.issues.update_story_point(self._issue_key, story_point)
            if changed_custom:
                await client.issues.update_custom_fields(
                    self._issue_key, changed_custom
                )
        except TissueApiError as error:
            self._error(
                getattr(error, "detail", None) or str(error) or "Update failed."
            )
            return
        self.dismiss(True)
