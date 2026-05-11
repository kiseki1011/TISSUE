from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Horizontal
from textual.screen import ModalScreen
from textual.widgets import Button, Input

from tissue.widgets.bracket_button import BracketButton
from tissue.widgets.i18n_widgets import I18nButton, I18nContainer, I18nInput
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
        yield I18nContainer(
            Horizontal(
                Button("×", id="close_btn", classes="back-btn"),
                id="close-bar",
            ),
            I18nInput(
                placeholder_key="description_placeholder",
                title_key="description_title",
                value=self.default_value,
                id="description",
                classes="input-field",
            ),
            I18nButton(
                key="save_btn",
                id="save_btn",
                classes="-success",
            ),
            id="description-modal-dialog",
            title_key="description_title",
        )

    def on_mount(self) -> None:
        self.query_one("#description", I18nInput).focus()

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
