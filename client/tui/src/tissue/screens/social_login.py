from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.screen import ModalScreen

from tissue.i18n.manager import i18n
from tissue.widgets.social_button import SocialButton


class SocialLoginModal(ModalScreen[str | None]):
    CSS_PATH = "css/social_login.tcss"

    BINDINGS = [
        Binding("escape", "close", "Close"),
        Binding("down", "nav_down", show=False, priority=True),
        Binding("up", "nav_up", show=False, priority=True),
        Binding("j", "nav_down", show=False, priority=True),
        Binding("k", "nav_up", show=False, priority=True),
    ]

    def __init__(self, providers: list[str]) -> None:
        super().__init__()
        self.providers = providers

    def compose(self) -> ComposeResult:
        yield Container(
            *(SocialButton(p) for p in self.providers),
            id="social-modal-dialog",
        )

    def on_mount(self) -> None:
        social_modal_dialog = self.query_one("#social-modal-dialog", Container)
        title = i18n.get("social_modal_border_title")
        social_modal_dialog.border_title = title

        buttons = list(self.query(SocialButton))
        if buttons:
            buttons[0].focus()

    def action_nav_down(self) -> None:
        self.focus_next()

    def action_nav_up(self) -> None:
        self.focus_previous()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(SocialButton.Pressed)
    def on_social_pressed(self, event: SocialButton.Pressed) -> None:
        self.dismiss(event.provider)
