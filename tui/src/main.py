import sys
from pathlib import Path

root_path = Path(__file__).parent.parent
sys.path.append(str(root_path))

from textual.app import App, ComposeResult
from src.config import ConfigManager
from src.screens.connect_screen import ConnectScreen
from src.i18n.manager import i18n

class IssueManageApp(App):
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
