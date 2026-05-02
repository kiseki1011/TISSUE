import dataclasses
import logging

from textual.app import App
from textual.binding import Binding

from tissue.api.client import TissueClient
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.models.auth import SystemInfo
from tissue.models.member import MemberProfile
from tissue.screens.connect import ConnectScreen
from tissue.screens.option import OptionModal
from tissue.themes import register_custom_themes

log = logging.getLogger(__name__)

EXCLUDED_THEMES = {"textual-ansi"}


class TissueApp(App):
    TITLE = "Tissue TUI"
    CSS_PATH = "global.tcss"

    BINDINGS = [
        Binding("ctrl+q", "quit", "quit", priority=True),
        Binding("ctrl+o", "open_options", "options", priority=True),
    ]

    def __init__(self):
        super().__init__()
        self.config_manager = ConfigManager()
        self.client = TissueClient(self.config_manager)
        self.system_info: SystemInfo | None = None
        self.current_profile: MemberProfile | None = None

    def on_mount(self) -> None:
        register_custom_themes(self)
        config = self.config_manager.get_config()
        i18n.set_language(config.language)
        self._apply_binding_labels()
        i18n.subscribe(self._apply_binding_labels)
        self.theme = config.theme

        tokens = self.config_manager.get_tokens()
        if tokens and tokens.access_token and config.current_server:
            from tissue.screens.home import HomeScreen

            self.client.set_base_url(config.current_server)
            self.push_screen(HomeScreen(self.config_manager))
        else:
            self.push_screen(ConnectScreen(self.config_manager))

    def _apply_binding_labels(self) -> None:
        labels = {
            "ctrl+q": i18n.get("binding_quit"),
            "ctrl+o": i18n.get("binding_options"),
        }
        for key, label in labels.items():
            existing = self._bindings.key_to_bindings.get(key, [])
            self._bindings.key_to_bindings[key] = [
                dataclasses.replace(b, description=label) for b in existing
            ]

    def action_open_options(self) -> None:
        if isinstance(self.screen, OptionModal):
            return
        self.push_screen(OptionModal(self.config_manager))

    @property
    def theme_options(self) -> list[tuple[str, str]]:
        return [
            (t, t)
            for t in sorted(self.available_themes.keys())
            if t not in EXCLUDED_THEMES
        ]

    async def on_unmount(self) -> None:
        i18n.unsubscribe(self._apply_binding_labels)
        await self.client.aclose()

    def _handle_exception(self, error: Exception) -> None:
        log.exception("Unhandled exception", exc_info=error)
        super()._handle_exception(error)
