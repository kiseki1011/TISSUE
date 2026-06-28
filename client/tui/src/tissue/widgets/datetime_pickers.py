from __future__ import annotations

from textual import on
from textual.message import Message
from textual_timepiece.pickers import (
    DateOverlay,
    DatePicker,
    DateTimeOverlay,
    DateTimePicker,
)


def _collapse_to_toggle(picker: DatePicker | DateTimePicker, message: Message) -> None:
    """Collapse the picker and focus its toggle button.

    The upstream overlay-close handler is misnamed, so it never fires while the
    calendar grid holds focus, and Escape there is swallowed. Moving focus to the
    toggle button (which has no Escape binding) lets a further Escape reach the
    host modal's own cancel binding.
    """
    message.stop()
    picker.expanded = False
    toggle = picker.query("#toggle-button")
    if toggle:
        toggle.first().focus()


class FieldDatePicker(DatePicker):
    """A date-only picker whose overlay Escape collapses the dropdown."""

    @on(DateOverlay.Closed)
    def _on_overlay_closed(self, message: DateOverlay.Closed) -> None:
        _collapse_to_toggle(self, message)


class DueDateTimePicker(DateTimePicker):
    """A date+time picker whose overlay Escape collapses the dropdown."""

    @on(DateTimeOverlay.Closed)
    def _on_overlay_closed(self, message: DateTimeOverlay.Closed) -> None:
        _collapse_to_toggle(self, message)
