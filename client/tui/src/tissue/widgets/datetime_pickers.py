"""textual-timepiece date/datetime pickers with a working Escape-to-collapse.

Upstream's overlay-close handler is misnamed, so it never fires while the
calendar grid holds focus — Escape there is swallowed and the dropdown never
closes. Each subclass catches the overlay's `Closed` message, collapses, and
parks focus on a plain control (no Escape binding of its own) so a further Escape
reaches the host modal's own "esc -> cancel" binding.

Leaf widgets (no screen imports), shared by the field-edit and create-issue forms.
"""

from __future__ import annotations

from textual import on
from textual_timepiece.pickers import (
    DateOverlay,
    DatePicker,
    DateTimeOverlay,
    DateTimePicker,
)


class FieldDatePicker(DatePicker):
    """A date-only picker whose overlay Escape actually collapses it."""

    @on(DateOverlay.Closed)
    def _on_overlay_closed(self, message: DateOverlay.Closed) -> None:
        message.stop()
        self.expanded = False
        toggle = self.query("#toggle-button")
        if toggle:
            toggle.first().focus()


class DueDateTimePicker(DateTimePicker):
    """A date+time picker whose overlay Escape actually collapses it."""

    @on(DateTimeOverlay.Closed)
    def _on_overlay_closed(self, message: DateTimeOverlay.Closed) -> None:
        message.stop()
        self.expanded = False
        toggle = self.query("#toggle-button")
        if toggle:
            toggle.first().focus()
