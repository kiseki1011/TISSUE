import dataclasses
import logging

from textual.app import App
from textual.binding import Binding

from tissue.api.client import TissueClient
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.connect import ConnectScreen
from tissue.screens.option import OptionModal

log = logging.getLogger(__name__)


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
        self.system_info = None

    def on_mount(self) -> None:
        config = self.config_manager.get_config()
        i18n.set_language(config.language)
        self._apply_binding_labels()
        i18n.subscribe(self._apply_binding_labels)
        self.theme = config.theme
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

    async def on_unmount(self) -> None:
        i18n.unsubscribe(self._apply_binding_labels)
        await self.client.aclose()

    def _handle_exception(self, error: Exception) -> None:
        log.exception("Unhandled exception", exc_info=error)
        super()._handle_exception(error)
