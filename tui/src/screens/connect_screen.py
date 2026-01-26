from textual.app import ComposeResult
from textual.screen import Screen
from textual.widgets import Header, Footer, Input, Button, Label, ListView, ListItem
from textual.containers import Container, Horizontal
from textual import on, events
from src.config import ConfigManager
from src.screens.login_screen import LoginScreen
from src.api.client import ServerClient
from src.i18n.manager import i18n

class ConnectScreen(Screen):
    CSS = """
    ConnectScreen { align: center middle; }
    #dialog { padding: 2; border: solid green; width: 60%; height: auto; margin: 2; }
    .input-row { height: 3; margin-bottom: 1; }
    #server_input { width: 3fr; }
    #connect_btn { width: 1fr; margin-left: 1; }
    .title { text-align: center; text-style: bold; margin-bottom: 1; }
    .subtitle { margin-top: 2; margin-bottom: 1; color: yellow; }
    """

    def __init__(self, config_manager: ConfigManager):
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Label(i18n.get("connect_title"), classes="title"),
            Horizontal(
                Input(placeholder=i18n.get("server_placeholder"), id="server_input"),
                Button(i18n.get("connect_btn"), variant="primary", id="connect_btn"),
                classes="input-row"
            ),
            Label(i18n.get("recent_servers"), classes="subtitle"),
            ListView(id="history_list"),
            id="dialog"
        )
        yield Footer()

    def on_mount(self) -> None:
        self.update_history()

    def on_key(self, event: events.Key) -> None:
        if event.key == "down": self.focus_next()
        elif event.key == "up": self.focus_previous()

    def update_history(self):
        history = self.config_manager.get_config().server_history
        list_view = self.query_one("#history_list", ListView)
        list_view.clear()
        for url in history:
            list_view.append(ListItem(Label(url)))

    @on(Input.Submitted, "#server_input")
    async def on_input_submitted(self):
        await self.connect_action()

    @on(Button.Pressed, "#connect_btn")
    async def connect_action(self):
        url = self.query_one("#server_input", Input).value.strip()
        if not url:
            self.app.notify(i18n.get("error_enter_url"), severity="error", timeout=3)
            return
        client = ServerClient(url)
        self.app.notify(i18n.get("connecting", url=url), timeout=3)
        try:
            info = await client.get_system_info()
            if info:
                self.config_manager.save_server(url)
                self.update_history()
                self.app.push_screen(LoginScreen(info))
            else:
                self.app.notify(i18n.get("connect_failed", url=url), severity="error", timeout=3)
        except Exception as e: self.app.notify(f"Error: {e}", severity="error", timeout=3)

    @on(ListView.Selected, "#history_list")
    def on_history_selected(self, event: ListView.Selected):
        index = self.query_one("#history_list", ListView).index
        if index is not None:
            history = self.config_manager.get_config().server_history
            if 0 <= index < len(history):
                self.query_one("#server_input", Input).value = history[index]