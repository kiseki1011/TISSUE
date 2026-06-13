"""Prompt shown when the project list is empty, offering to create one."""

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Static

from tissue.screens.base import TissueModal


class EmptyProjectsModal(TissueModal[bool | None]):
    """Asks whether to create the first project. Dismisses True to create."""

    CSS_PATH = "empty_projects_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def compose(self) -> ComposeResult:
        message = Static(
            "You don't have any projects yet. Create your first one?",
            classes="prompt",
            id="empty_projects_message",
        )
        buttons = Horizontal(
            Button(
                "Not now",
                id="empty_projects_no_btn",
            ),
            Button(
                "Create",
                id="empty_projects_yes_btn",
                classes="-btn-success",
            ),
            id="empty-projects-buttons",
        )
        form = Container(message, buttons, id="empty-projects-form")
        dialog = Container(form, id="empty-projects-dialog", classes="dialog")
        dialog.border_title = "No projects yet"
        dialog.border_subtitle = "Esc to cancel"
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
