from textual import events, on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import (
    Button,
    Footer,
    Header,
    Input,
    Label,
    ListItem,
    ListView,
    Static,
)

from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.dev.fixtures import MOCK_SERVER_HISTORY, MOCK_SYSTEM_INFO
from tissue.i18n.manager import i18n
from tissue.screens.login import LoginScreen
from tissue.widgets.bracket_button import BracketButton
from tissue.widgets.modal_input import ModalInput


class ConnectScreen(Screen):
    CSS_PATH = "css/connect.tcss"

    BINDINGS = [
        Binding("j,down", "nav_down", show=False, priority=True),
        Binding("k,up", "nav_up", show=False, priority=True),
    ]

    def __init__(self, config_manager: ConfigManager):
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Static(TISSUE_LOGO, classes="logo"),
            Horizontal(
                ModalInput(
                    placeholder=i18n.get("server_placeholder"), id="server_input"
                ),
                BracketButton(i18n.get("connect_btn"), id="connect_btn"),
                classes="input-row",
            ),
            ListView(id="history_list"),
            id="dialog",
        )
        yield Footer()

    def on_mount(self) -> None:
        dialog = self.query_one("#dialog", Container)
        dialog.border_title = "Connect to Tissue Server"

        server_input = self.query_one("#server_input", ModalInput)
        server_input.border_title = "Server Domain"

        history_list = self.query_one("#history_list", ListView)
        history_list.border_title = "Server URL ──────────────────────── Last Connected"

        self.query_one("#server_input", ModalInput).focus()
        self.update_history()

    def on_screen_resume(self) -> None:
        self.query_one("#server_input", ModalInput).focus()
        self.update_history()

    def check_action(self, action: str, parameters: tuple) -> bool | None:
        if action in ("nav_down", "nav_up"):
            focused = self.focused
            if isinstance(focused, ModalInput) and focused._editing:
                return False
        return True

    def action_nav_down(self) -> None:
        focused = self.focused
        if isinstance(focused, ListView):
            list_view = self.query_one("#history_list", ListView)
            if list_view.index is None:
                list_view.index = 0
                return
            if list_view.index >= len(list_view.children) - 1:
                list_view.index = None
                self.focus_next()
                return
            list_view.index += 1
            return
        self.focus_next()

    def action_nav_up(self) -> None:
        focused = self.focused
        if isinstance(focused, ListView):
            list_view = self.query_one("#history_list", ListView)
            if list_view.index is None:
                list_view.index = 0
                return
            if list_view.index == 0:
                list_view.index = None
                self.focus_previous()
                return
            list_view.index -= 1
            return
        self.focus_previous()

    def on_key(self, event: events.Key) -> None:
        if event.key == "escape":
            focused = self.focused
            if isinstance(focused, ModalInput) and not focused._editing:
                self.set_focus(None)

    def update_history(self):
        history = MOCK_SERVER_HISTORY
        list_view = self.query_one("#history_list", ListView)
        list_view.clear()
        for item in history:
            date_str = item.last_connected.strftime("%Y-%m-%d %H:%M")
            li = ListItem(
                Horizontal(
                    Label(item.url, classes="col"),
                    Label(date_str, classes="col col-right"),
                )
            )
            list_view.append(li)

    @on(Input.Submitted, "#server_input")
    async def on_input_submitted(self):
        await self.connect_action()

    @on(Button.Pressed, "#connect_btn")
    async def connect_action(self):
        url = self.query_one("#server_input", ModalInput).value.strip()
        if not url:
            self.app.notify(i18n.get("error_enter_url"), severity="error", timeout=2)
            return
        self.app.notify(i18n.get("connecting", url=url), timeout=2)
        info = MOCK_SYSTEM_INFO
        self.app.notify(i18n.get("connect_success", url=url), timeout=2)
        self.app.push_screen(LoginScreen(info))

    @on(ListView.Selected, "#history_list")
    def on_list_selected(self, event: ListView.Selected):
        index = self.query_one("#history_list", ListView).index
        if index is not None:
            history = MOCK_SERVER_HISTORY
            if 0 <= index < len(history):
                self.query_one("#server_input", ModalInput).value = history[index].url
                self.query_one("#server_input", ModalInput).focus()
