from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.widgets import Footer, Header, Static

from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen


class SignupScreen(TissueScreen):
    BINDINGS = [
        Binding("escape", "back", "back"),
    ]

    def compose(self) -> ComposeResult:
        dialog = Container(
            Static(i18n.get("signup_screen_placeholder")),
            id="signup-dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("signup_dialog_border_title")

        yield Header()
        yield dialog
        yield Footer()

    def action_back(self) -> None:
        self.app.pop_screen()
