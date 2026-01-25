from textual.app import ComposeResult
from textual.screen import Screen
from textual.widgets import Header, Footer, Input, Button, Label, ListView, ListItem
from textual.containers import Container, Horizontal
from textual import on
from src.config import ConfigManager
from src.screens.login_screen import LoginScreen

class ConnectScreen(Screen):
    """Server connection screen."""
    
    # CSS for layout
    CSS = """
    .input-row {
        height: 3;
        margin-bottom: 1;
    }
    #server_input {
        width: 3fr;
    }
    #connect_btn {
        width: 1fr;
        margin-left: 1;
    }
    """

    def __init__(self, config_manager: ConfigManager):
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Label("Connect to Server", classes="title"),
            
            # 1. Horizontal layout for Input and Button
            Horizontal(
                Input(placeholder="http://localhost:8080", id="server_input"),
                Button("Connect", variant="primary", id="connect_btn"),
                classes="input-row"
            ),
            
            Label("Recent Servers", classes="subtitle"),
            ListView(id="history_list"),
            
            id="dialog"
        )
        yield Footer()

    def on_mount(self) -> None:
        self.update_history()

    def update_history(self):
        """Reloads the history list from config."""
        history = self.config_manager.get_config().server_history
        list_view = self.query_one("#history_list", ListView)
        
        # Clear existing items to avoid duplicates
        list_view.clear()
        
        for url in history:
            list_view.append(ListItem(Label(url)))

    # 2. Handle Enter key in Input
    @on(Input.Submitted, "#server_input")
    def on_input_submitted(self):
        self.connect_action()

    @on(Button.Pressed, "#connect_btn")
    def connect_action(self):
        input_widget = self.query_one("#server_input", Input)
        url = input_widget.value.strip()
        
        if url:
            self.config_manager.save_server(url)
            self.app.notify(f"Connected to {url}")
            
            # 3. Refresh history list immediately
            self.update_history()
            
            # 화면 전환: 로그인 화면을 스택에 추가(push)하여 보여줌
            self.app.push_screen(LoginScreen())

    @on(ListView.Selected, "#history_list")
    def on_history_selected(self, event: ListView.Selected):
        label = event.item.query_one(Label)
        url = str(label.renderable)
        
        input_widget = self.query_one("#server_input", Input)
        input_widget.value = url
        # Optional: Auto-connect on selection?
        # self.connect_action()
