from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Horizontal
from textual.screen import ModalScreen
from textual.widgets import Button

from tissue.widgets.i18n_widgets import I18nButton, I18nContainer, I18nLabel


class LogoutConfirmModal(ModalScreen[bool]):
    CSS_PATH = ["css/_buttons.tcss", "css/logout_confirm.tcss"]

    BINDINGS = [
        Binding("escape", "no", show=False),
        Binding("left", "focus_previous", show=False),
        Binding("right", "focus_next", show=False),
    ]

    def compose(self) -> ComposeResult:
        yield I18nContainer(
            I18nLabel("logout_confirm_message", classes="confirm-message"),
            Horizontal(
                I18nButton(key="no_btn", id="no_btn", classes="-secondary"),
                I18nButton(key="yes_btn", id="yes_btn", classes="-success"),
                classes="button-row",
            ),
            id="logout-confirm-dialog",
            title_key="logout_confirm_title",
        )

    def on_mount(self) -> None:
        self.query_one("#no_btn", I18nButton).focus()

    def action_no(self) -> None:
        self.dismiss(False)

    def action_focus_previous(self) -> None:
        self.focus_previous()

    def action_focus_next(self) -> None:
        self.focus_next()

    @on(Button.Pressed, "#yes_btn")
    def on_yes(self) -> None:
        self.dismiss(True)

    @on(Button.Pressed, "#no_btn")
    def on_no(self) -> None:
        self.dismiss(False)
