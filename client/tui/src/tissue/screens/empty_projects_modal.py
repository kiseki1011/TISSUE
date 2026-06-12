"""Prompt shown when the project list is empty, offering to create one."""

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Static

from tissue.i18n.manager import i18n
from tissue.screens.base import TissueModal


class EmptyProjectsModal(TissueModal[bool | None]):
    """Asks whether to create the first project. Dismisses True to create."""

    CSS_PATH = "empty_projects_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def compose(self) -> ComposeResult:
        message = Static(
            i18n.get("project_empty_message"),
            classes="prompt",
            id="empty_projects_message",
        )
        buttons = Horizontal(
            Button(
                i18n.get("project_empty_no_btn"),
                id="empty_projects_no_btn",
            ),
            Button(
                i18n.get("project_empty_yes_btn"),
                id="empty_projects_yes_btn",
                classes="-btn-success",
            ),
            id="empty-projects-buttons",
        )
        form = Container(message, buttons, id="empty-projects-form")
        dialog = Container(form, id="empty-projects-dialog", classes="dialog")
        dialog.border_title = i18n.get("project_empty_title")
        dialog.border_subtitle = i18n.get("workspace_create_modal_close_hint")
        yield dialog

    def on_mount(self) -> None:
        self.query_one("#empty_projects_yes_btn", Button).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#empty_projects_no_btn")
    def _on_no(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#empty_projects_yes_btn")
    def _on_yes(self) -> None:
        self.dismiss(True)
