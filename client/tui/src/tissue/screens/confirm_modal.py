"""Generic yes/no confirmation dialog.

Dismisses `True` on confirm, `False` on cancel/Esc. Reusable for any action
that wants a guard (e.g. archiving a project, deleting a wiki document).
"""

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Static

from tissue.screens.base import TissueModal


class ConfirmModal(TissueModal[bool]):
    CSS_PATH = "confirm_modal.tcss"

    BINDINGS = [
        Binding("escape", "cancel", "cancel"),
    ]

    def __init__(
        self,
        *,
        title: str,
        message: str,
        confirm_label: str = "Confirm",
        danger: bool = False,
    ) -> None:
        super().__init__()
        self._title = title
        self._message = message
        self._confirm_label = confirm_label
        self._danger = danger

    def compose(self) -> ComposeResult:
        with Container(id="confirm-dialog", classes="dialog"):
            with Container(id="confirm-form"):
                yield Static(self._message, id="confirm-message", markup=False)
                yield Horizontal(
                    Button("Cancel", id="confirm-cancel-btn"),
                    Button(
                        self._confirm_label,
                        id="confirm-ok-btn",
                        classes="-btn-error" if self._danger else "-btn-success",
                    ),
                    id="confirm-buttons",
                )

    def on_mount(self) -> None:
        dialog = self.query_one("#confirm-dialog", Container)
        dialog.border_title = self._title
        dialog.border_subtitle = "Esc to cancel"
        # Default focus on Cancel so a stray Enter doesn't confirm the action.
        self.query_one("#confirm-cancel-btn", Button).focus()

    @on(Button.Pressed, "#confirm-ok-btn")
    def _on_confirm(self) -> None:
        self.dismiss(True)

    @on(Button.Pressed, "#confirm-cancel-btn")
    def _on_cancel(self) -> None:
        self.dismiss(False)

    def action_cancel(self) -> None:
        self.dismiss(False)
