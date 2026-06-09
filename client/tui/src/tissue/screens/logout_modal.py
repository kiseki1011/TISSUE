"""Confirmation modal shown before tearing down the current session."""

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Static

from tissue.i18n.manager import i18n
from tissue.screens.base import TissueModal


class LogoutModal(TissueModal[bool | None]):
    """Confirmation before logging out."""

    CSS_PATH = "logout_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def compose(self) -> ComposeResult:
        warning = Static(
            i18n.get("logout_warning"),
            classes="warning",
            id="logout_warning",
        )
        buttons = Horizontal(
            Button(
                i18n.get("logout_cancel_btn"),
                id="logout_cancel_btn",
            ),
            Button(
                i18n.get("logout_confirm_btn"),
                id="logout_confirm_btn",
                classes="-btn-warning",
            ),
            id="logout-buttons",
        )
        form = Container(
            warning,
            buttons,
            id="logout-form",
        )
        dialog = Container(
            form,
            id="logout-dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("logout_title")
        dialog.border_subtitle = i18n.get("workspace_create_modal_close_hint")
        yield dialog

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#logout_cancel_btn")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#logout_confirm_btn")
    def _on_confirm(self) -> None:
        self.dismiss(True)
