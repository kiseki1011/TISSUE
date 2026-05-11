import logging

from textual.app import App

from tissue.auth.token_store import create_token_store
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.connect import ConnectScreen

log = logging.getLogger(__name__)


class TissueApp(App):
    CSS_PATH = "global.tcss"
    BORDER_STYLES = ("round", "solid", "heavy")

    def __init__(self) -> None:
        super().__init__()
        self.config = ConfigManager()
        i18n.set_language(self.config.settings.language)
        self.theme = self.config.settings.theme
        self._apply_border_style(self.config.settings.border_style)
        self.token_store = create_token_store()

    def on_mount(self) -> None:
        self.push_screen(ConnectScreen(self.config))

    def change_language(self, lang: str) -> None:
        i18n.set_language(lang)
        self.config.update_settings(language=lang)
        for screen in self.screen_stack:
            screen.refresh(recompose=True)

    def change_theme(self, theme: str) -> None:
        self.theme = theme
        self.config.update_settings(theme=theme)

    def change_border_style(self, style: str) -> None:
        self._apply_border_style(style)
        self.config.update_settings(border_style=style)

    def _apply_border_style(self, style: str) -> None:
        for s in self.BORDER_STYLES:
            self.remove_class(f"-border-{s}")
        if style != "round":
            self.add_class(f"-border-{style}")
