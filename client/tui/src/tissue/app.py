import dataclasses
import logging

from textual.app import App
from textual.binding import Binding

from tissue.api.client import TissueClient
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.connect import ConnectScreen

log = logging.getLogger(__name__)


class TissueApp(App):
    TITLE = "Tissue TUI"
    CSS_PATH = "global.tcss"

    BINDINGS = [
        Binding("ctrl+q", "quit", "quit", priority=True),
    ]

    def __init__(self):
        super().__init__()
        self.config_manager = ConfigManager()
        self.client = TissueClient(self.config_manager)

    def on_mount(self) -> None:
        config = self.config_manager.get_config()
        i18n.set_language(config.language)
        self._apply_binding_labels()
        self.theme = "monokai"
        self.push_screen(ConnectScreen(self.config_manager))

    def _apply_binding_labels(self) -> None:
        labels = {"ctrl+q": i18n.get("binding_quit")}
        for key, label in labels.items():
            existing = self._bindings.key_to_bindings.get(key, [])
            self._bindings.key_to_bindings[key] = [
                dataclasses.replace(b, description=label) for b in existing
            ]

    async def on_unmount(self) -> None:
        await self.client.aclose()

    def _handle_exception(self, error: Exception) -> None:
        log.exception("Unhandled exception", exc_info=error)
        super()._handle_exception(error)
