import asyncio
import logging
from datetime import datetime

from textual.app import App
from textual.binding import Binding
from textual.css.query import NoMatches

from tissue.api.client import TissueClient
from tissue.api.errors import TissueApiError
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.auth.token_store import create_token_store
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.connect import ConnectScreen
from tissue.screens.login import LoginScreen
from tissue.screens.option import OptionModal
from tissue.screens.reconnect import ReconnectScreen
from tissue.theming import generate_btn_variant_css

log = logging.getLogger(__name__)


class TissueApp(App):
    CSS_PATH = "global.tcss"

    BORDER_STYLES = (
        "round",
        "solid",
        "heavy",
        "dashed",
        "double",
        "hkey",
        "tab",
        "ascii",
    )

    CSS = generate_btn_variant_css(BORDER_STYLES)

    BINDINGS = [
        Binding("ctrl+o", "options", "options"),
    ]

    def __init__(self) -> None:
        super().__init__()
        self.config = ConfigManager()
        i18n.set_language(self.config.settings.language)
        self.theme = self.config.settings.theme
        self._apply_border_style(self.config.settings.border_style)
        self.token_store = create_token_store()
        self.client: TissueClient | None = None
        self.system_info: SystemInfoDetails | None = None

    RECONNECT_SCREEN_DELAY = 0.5  # 500ms before falling back to ReconnectScreen

    async def on_mount(self) -> None:
        saved_url = self.config.state.current_server_url
        if not saved_url:
            self.push_screen(ConnectScreen(self.config))
            return

        # current server url exists
        client = TissueClient(host=saved_url, token_store=self.token_store)
        try:
            system_info = await asyncio.wait_for(
                client.ping(), timeout=self.RECONNECT_SCREEN_DELAY
            )
        # connection fails in RECONNECT_SCREEN_DELAY window
        except (TimeoutError, TissueApiError) as e:
            log.debug("Initial ping failed, showing reconnect screen: %s", e)
            await client.close()
            self.push_screen(ReconnectScreen(saved_url, self.config))
            return

        # connection succeeds
        self.client = client
        self.system_info = system_info
        self.config.update_state(last_connected_at=datetime.now().astimezone())
        self.push_screen(LoginScreen(system_info, self.config))

    async def on_unmount(self) -> None:
        if self.client is not None:
            await self.client.close()
            self.client = None

    def change_language(self, lang: str) -> None:
        i18n.set_language(lang)
        self.config.update_settings(language=lang)

        focused_id = self.focused.id if self.focused else None

        for screen in self.screen_stack:
            screen.refresh(recompose=True)

        if focused_id:
            self.call_after_refresh(self._refocus_by_id, focused_id)

    def _refocus_by_id(self, widget_id: str) -> None:
        try:
            self.screen.query_one(f"#{widget_id}").focus()
        except NoMatches:
            log.debug("Could not refocus #%s after recompose", widget_id)

    def change_theme(self, theme: str) -> None:
        self.theme = theme
        self.config.update_settings(theme=theme)

    def change_border_style(self, style: str) -> None:
        self._apply_border_style(style)
        self.config.update_settings(border_style=style)

    def action_options(self) -> None:
        if isinstance(self.screen, OptionModal):
            return
        self.push_screen(OptionModal(self.config))

    def _apply_border_style(self, style: str) -> None:
        for s in self.BORDER_STYLES:
            self.remove_class(f"-border-{s}")
        if style != "round":
            self.add_class(f"-border-{style}")
        for screen in self.screen_stack:
            self.stylesheet.update(screen)
