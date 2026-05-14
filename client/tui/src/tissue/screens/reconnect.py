import asyncio
import logging

from textual import work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.widgets import Footer, Header, ProgressBar, Static

from tissue.api.client import TissueClient
from tissue.api.errors import TissueApiError
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen

log = logging.getLogger(__name__)


class ReconnectScreen(TissueScreen):
    CSS_PATH = "reconnect.tcss"

    BINDINGS = [
        Binding("escape", "cancel", "cancel"),
    ]

    MAX_RETRIES = 5

    def __init__(self, url: str, config_manager: ConfigManager) -> None:
        super().__init__()
        self.url = url
        self.config_manager = config_manager
        self._cancelled = False

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            ProgressBar(
                total=None,
                show_eta=False,
                show_percentage=False,
                id="reconnect_bar",
            ),
            Static(self._status_text(), id="status"),
            Static(self._progress_text(1), id="status_progress"),
            Static(i18n.get("reconnect_cancel_hint"), id="cancel_hint"),
            id="reconnect-dialog",
        )
        yield Footer()

    def on_mount(self) -> None:
        self._do_reconnect()

    def action_cancel(self) -> None:
        from tissue.screens.connect import ConnectScreen

        self._cancelled = True
        self.config_manager.update_state(current_server_url=None)
        self.app.switch_screen(ConnectScreen(self.config_manager))

    @work(exclusive=True)
    async def _do_reconnect(self) -> None:
        from datetime import datetime

        from tissue.screens.connect import ConnectScreen
        from tissue.screens.login import LoginScreen

        client = TissueClient(host=self.url, token_store=self.app.token_store)
        for attempt in range(1, self.MAX_RETRIES + 1):
            # cancel on escape
            if self._cancelled:
                await client.close()
                return

            self._update_status(attempt)
            try:
                system_info = await client.ping()
            except TissueApiError as e:
                log.warning("Reconnect %d/%d failed: %s", attempt, self.MAX_RETRIES, e)
                if attempt < self.MAX_RETRIES:
                    delay = 2 ** (
                        attempt - 1
                    )  # exponential backoff - 1, 2, 4, 8 seconds
                    await self._sleep_cancellable(delay)
                continue

            if self._cancelled:
                await client.close()
                return
            if self.app.client is not None:
                await self.app.client.close()

            # succeeds ping (connection)
            self.app.client = client
            self.app.system_info = system_info
            self.config_manager.update_state(
                current_server_url=client.host,
                last_connected_at=datetime.now().astimezone(),
            )
            self.app.switch_screen(LoginScreen(system_info, self.config_manager))
            return

        # fail all retries
        await client.close()
        if not self._cancelled:
            self.config_manager.update_state(current_server_url=None)
            self.app.switch_screen(
                ConnectScreen(self.config_manager, failed_url=self.url)
            )

    async def _sleep_cancellable(self, seconds: float) -> None:
        step = 0.1
        elapsed = 0.0
        while elapsed < seconds:
            if self._cancelled:
                return
            await asyncio.sleep(step)
            elapsed += step

    def _update_status(self, attempt: int) -> None:
        self.query_one("#status_progress", Static).update(self._progress_text(attempt))

    def _status_text(self) -> str:
        return i18n.get("reconnect_connecting", url=self.url)

    def _progress_text(self, attempt: int) -> str:
        return f"({attempt}/{self.MAX_RETRIES})"
