import logging

from textual.app import App, ComposeResult
from textual.widgets import Footer, Header, Static

from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n

log = logging.getLogger(__name__)


class TissueApp(App):
    def __init__(self) -> None:
        super().__init__()
        self.config = ConfigManager()
        i18n.set_language(self.config.settings.language)
        self.theme = self.config.settings.theme

    def compose(self) -> ComposeResult:
        yield Header()
        yield Static("Hello Tissue!")
        yield Footer()

    def change_language(self, lang: str) -> None:
        i18n.set_language(lang)
        self.config.update_settings(language=lang)
        for screen in self.screen_stack:
            screen.refresh(recompose=True)

    def change_theme(self, theme: str) -> None:
        self.theme = theme
        self.config.update_settings(theme=theme)
