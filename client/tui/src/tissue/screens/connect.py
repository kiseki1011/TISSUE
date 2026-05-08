from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Horizontal
from textual.screen import Screen
from textual.widgets import Footer, Header


class ConnectScreen(Screen):
    CSS_PATH = "connect.tcss"

    BINDINGS = [Binding("ctrl+o", "option_menu", "options")]

    def compose(self) -> ComposeResult:
        yield Header()
        yield Horizontal(
            # Input
            # Button
        )
        yield Footer()
