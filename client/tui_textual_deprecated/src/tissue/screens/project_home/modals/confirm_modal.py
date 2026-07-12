from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Static

from tissue.screens.base import TissueModal


class ConfirmModal(TissueModal[bool | None]):
    """A small confirm / cancel dialog whose message reads in a warning tone.

    Dismisses True on confirm, None on cancel or Escape.
    """

    CSS_PATH = "confirm_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    def __init__(
        self,
        *,
        message: str,
        title: str = "Confirm",
        confirm_label: str = "Confirm",
    ) -> None:
        super().__init__()
        self._message = message
        self._title = title
        self._confirm_label = confirm_label

    def compose(self) -> ComposeResult:
        body = Container(
            Static(self._message, id="confirm-message"),
            Horizontal(
                Button("Cancel", id="confirm-cancel"),
                Button(self._confirm_label, id="confirm-ok", classes="-btn-error"),
                id="confirm-buttons",
            ),
            id="confirm-body",
        )
        dialog = Container(body, id="confirm-dialog", classes="dialog")
        dialog.border_title = self._title
        dialog.border_subtitle = "Esc to cancel"
        yield dialog

    def action_cancel(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#confirm-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    @on(Button.Pressed, "#confirm-ok")
    def _on_confirm(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(True)
