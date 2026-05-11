from textual.app import ComposeResult
from textual.containers import Container
from textual.widgets import Footer, Header, Static

from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen


class HomeScreen(TissueScreen):
    def compose(self) -> ComposeResult:
        dialog = Container(
            Static(i18n.get("home_screen_placeholder")),
            id="home-dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("home_dialog_border_title")

        yield Header()
        yield dialog
        yield Footer()
