from textual.app import ComposeResult
from textual.containers import Container
from textual.widgets import Footer, Header, Static

from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen


class LoginScreen(TissueScreen):
    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Static(i18n.get("login_screen_placeholder")),
            id="dialog",
        )
        yield Footer()

    def on_mount(self) -> None:
        dialog = self.query_one("#dialog", Container)
        dialog.border_title = i18n.get("login_screen_title")
        server_host = self.app.client.host if self.app.client else "(no client)"
        dialog.border_subtitle = server_host
