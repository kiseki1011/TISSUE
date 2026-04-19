from textual.app import App

from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.connect import ConnectScreen


class TissueApp(App):
    TITLE = "Tissue TUI"
    CSS_PATH = "app.tcss"

    def __init__(self):
        super().__init__()
        self.config_manager = ConfigManager()

    def on_mount(self) -> None:
        config = self.config_manager.get_config()
        i18n.set_language(config.language)
        self.push_screen(ConnectScreen(self.config_manager))
