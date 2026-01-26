from textual.app import ComposeResult
from textual.screen import Screen
from textual.widgets import Header, Footer, Input, Button, Label, ListView, ListItem, Static
from textual.containers import Container, Horizontal
from textual import on, events
from src.config import ConfigManager
from src.screens.login_screen import LoginScreen
from src.api.client import ServerClient
from src.i18n.manager import i18n
from src.assets.logo import TISSUE_LOGO

class ConnectScreen(Screen):
    CSS = """
    #history_list {
        height: 1fr;
        border: none;
        margin-top: 0;
        background: $surface;
        padding: 0;
    }
    
    #history_list:focus {
        border: none;
        outline: none;
        background: $surface; /* Ensure background doesn't change brightness */
        tint: $surface 0%; /* Remove any focus tint */
    }
    
    .subtitle {
        margin-top: 3;
        margin-bottom: 1;
        color: $text-muted;
    }

    /* Table Header Style */
    .table-header {
        height: 1;
        width: 100%;
        margin-top: 1;
        background: $surface;
        color: $text-muted;
        text-style: bold;
        padding: 0 2;
    }

    /* List Item Layout */
    ListItem {
        padding: 1 2;
        background: $surface;
        height: auto;
    }

    ListItem > Horizontal {
        height: auto;
        width: 100%;
    }

    /* Columns: 50% split */
    .col {
        width: 1fr;
    }
    
    .col-right {
        text-align: left;
    }

    /* Highlight Selection */
    ListItem.-highlight {
        background: $secondary;
        text-style: bold; 
    }
    
    #server_input {
        width: 4fr;
    }
    
    #connect_btn {
        width: 1fr;
        margin-left: 1;
        margin-top: 0;
    }

    .input-row {
        height: 3;
        width: 100%;
        margin-bottom: 1;
    }
    """

    def __init__(self, config_manager: ConfigManager):
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Static(TISSUE_LOGO, classes="logo"),
            Label(i18n.get("connect_title"), classes="title"),
            Horizontal(
                Input(placeholder=i18n.get("server_placeholder"), id="server_input"),
                Button(i18n.get("connect_btn"), variant="primary", id="connect_btn"),
                classes="input-row"
            ),
            Label(i18n.get("recent_servers"), classes="subtitle"),
            # Header Row
            Horizontal(
                Label("Server URL", classes="col"),
                Label("Last Connected", classes="col col-right"),
                classes="table-header"
            ),
            ListView(id="history_list"),
            id="dialog"
        )
        yield Footer()

    def on_mount(self) -> None:
        self.query_one("#server_input", Input).focus()
        self.update_history()

    def on_screen_resume(self) -> None:
        self.query_one("#server_input", Input).focus()
        self.update_history()

    def on_key(self, event: events.Key) -> None:
        if event.key == "down": self.focus_next()
        elif event.key == "up": self.focus_previous()

    def update_history(self):
        history = self.config_manager.get_config().server_history
        list_view = self.query_one("#history_list", ListView)
        list_view.clear()
        for item in history:
            date_str = item.last_connected.strftime("%Y-%m-%d %H:%M")
            
            # Simple ListItem with 50/50 labels
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
        url = self.query_one("#server_input", Input).value.strip()
        if not url:
            self.app.notify(i18n.get("error_enter_url"), severity="error", timeout=2)
            return
        client = ServerClient(url)
        self.app.notify(i18n.get("connecting", url=url), timeout=2)
        try:
            info = await client.get_system_info()
            if info:
                self.app.notify(i18n.get("connect_success", url=url), timeout=2)
                self.config_manager.save_server(url)
                self.update_history()
                self.app.push_screen(LoginScreen(info))
            else:
                self.app.notify(i18n.get("connect_failed", url=url), severity="error", timeout=2)
        except Exception as e: self.app.notify(f"Error: {e}", severity="error", timeout=2)

    @on(ListView.Selected, "#history_list")
    def on_list_selected(self, event: ListView.Selected):
        index = self.query_one("#history_list", ListView).index
        if index is not None:
            history = self.config_manager.get_config().server_history
            if 0 <= index < len(history):
                self.query_one("#server_input", Input).value = history[index].url