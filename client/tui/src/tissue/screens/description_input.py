from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.screen import ModalScreen
from textual.widgets import Button, Input

from tissue.i18n.manager import i18n
from tissue.widgets.bracket_button import BracketButton
from tissue.widgets.modal_input import ModalInput


class DescriptionInputModal(ModalScreen[str | None]):
    CSS_PATH = ["css/_buttons.tcss", "css/description_input.tcss"]

    BINDINGS = [
        Binding("escape", "close", "close"),
        Binding("down", "nav_down", show=False, priority=True),
        Binding("up", "nav_up", show=False, priority=True),
    ]

    def __init__(self, default_value: str = "") -> None:
        super().__init__()
        self.default_value = default_value

    def compose(self) -> ComposeResult:
        yield Container(
            Horizontal(
                Button("\u00d7", id="close_btn", classes="back-btn"),
                id="close-bar",
            ),
            ModalInput(
                placeholder=i18n.get("description_placeholder"),
                value=self.default_value,
                id="description",
                classes="input-field",
            ),
            BracketButton(
                i18n.get("save_btn"),
                id="save_btn",
                classes="-success",
            ),
            id="description-modal-dialog",
        )

    def on_mount(self) -> None:
        dialog = self.query_one("#description-modal-dialog", Container)
        dialog.border_title = i18n.get("description_title")
        self.query_one("#description", ModalInput).border_title = i18n.get(
            "description_title"
        )
        self.query_one("#description", ModalInput).focus()

    def action_nav_down(self) -> None:
        self.focus_next()

    def action_nav_up(self) -> None:
        self.focus_previous()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#close_btn")
    def on_close_pressed(self) -> None:
        self.action_close()

    @on(Input.Submitted, "#description")
    @on(BracketButton.Pressed, "#save_btn")
    def on_save(self) -> None:
        value = self.query_one("#description", ModalInput).value.strip()
        self.dismiss(value)
