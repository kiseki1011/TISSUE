import sys
from pathlib import Path

root_path = Path(__file__).parent.parent
sys.path.append(str(root_path))

from textual.app import App, ComposeResult
from src.config import ConfigManager
from src.screens.connect_screen import ConnectScreen
from src.i18n.manager import i18n

class IssueManageApp(App):
    TITLE = "Tissue TUI"
    CSS = """
    Screen {
        background: $surface;
        align: center middle;
    }

    #dialog, #login-container, #signup-container {
        background: $panel;
        border: thick $primary;
        width: 95%;
        max-width: 80;
        height: 95%;
        padding: 1 2;
        content-align: center middle;
    }

    .title {
        text-align: center;
        text-style: bold;
        color: $accent;
        margin-bottom: 1;
        width: 100%;
    }

    .subtitle {
        text-align: center;
        color: $text-muted;
        margin-bottom: 2;
        width: 100%;
    }

    Input {
        margin-bottom: 1;
        border: tall $primary;
    }

    Input:focus {
        border: tall $accent;
    }

    Button {
        width: 100%;
        margin: 1 0;
    }

    .input-field.error {
        border: tall $error;
    }

    .input-field.success {
        border: tall $success;
    }

    Label.error {
        color: $error;
        text-style: italic;
        margin-bottom: 1;
    }

    Label.success {
        color: $success;
        text-style: bold;
        margin-bottom: 1;
    }

    .logo {
        text-align: center;
        color: $success;
        margin-bottom: 1;
        width: 100%;
        height: auto;
    }
    """

    def __init__(self):
        super().__init__()
        self.config_manager = ConfigManager()

    def on_mount(self) -> None:
        config = self.config_manager.get_config()
        # 저장된 언어 설정 반영
        i18n.set_language(config.language)
        
        if not config.current_server:
            self.app.push_screen(ConnectScreen(self.config_manager))
        else:
            self.app.push_screen(ConnectScreen(self.config_manager))

if __name__ == '__main__':
    app = IssueManageApp()
    app.run()
