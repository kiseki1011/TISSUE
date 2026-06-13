import asyncio
import logging
from datetime import datetime

from pydantic import HttpUrl, TypeAdapter, ValidationError
from textual import work
from textual.app import ComposeResult
from textual.containers import Container
from textual.widgets import Footer, Static

from tissue.api.client import TissueClient
from tissue.api.errors import (
    ConnectionFailed,
    NotTissueServer,
    ServerError,
    TissueApiError,
)
from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.screens.base import TissueScreen
from tissue.widgets.spinner import Spinner

log = logging.getLogger(__name__)

_url_validator = TypeAdapter(HttpUrl)

_CONNECT_ERROR_MESSAGES = {
    "connect_error_unreachable": "Cannot reach server. Check the URL and network.",
    "connect_error_invalid_server": "Not a Tissue server.",
    "connect_error_server": "Server returned an error. Try again later.",
    "connect_error_generic": "Connection failed.",
}


class ConnectingScreen(TissueScreen):
    """Connect to a server passed on the CLI.

    `tissue -c <url>`

    Pings the server with a few retries while showing a spinner. On success it saves the
    server as the current server and routes to the project list (when a stored session
    can be restored) or the login screen.
    """

    CSS_PATH = "connecting.tcss"

    MAX_RETRIES = 5
    RETRY_DELAY = 1.0

    def __init__(self, url: str, config_manager: ConfigManager) -> None:
        super().__init__()
        self.url = url
        self.config_manager = config_manager
        self._spinner: Spinner | None = None

    def compose(self) -> ComposeResult:
        yield Container(
            Static(TISSUE_LOGO, classes="logo"),
            Static("", id="connect_spinner"),
            Static("", id="connect_progress"),
            id="connecting-dialog",
        )
        yield Footer()

    def on_mount(self) -> None:
        self._spinner = Spinner(self, self.query_one("#connect_spinner", Static))
        self._connect()

    @work(exclusive=True)
    async def _connect(self) -> None:
        if not self._is_valid_url(self.url):
            self.app.exit(
                return_code=2,
                message=f"{'Invalid URL. Use http:// or https://'}: {self.url}",
            )
            return

        spinner = self._spinner
        assert spinner is not None
        progress = self.query_one("#connect_progress", Static)

        client = TissueClient(host=self.url, token_store=self.app.token_store)
        spinner.start(f"Connecting to {client.host}")

        error_key = "connect_error_generic"
        for attempt in range(1, self.MAX_RETRIES + 1):
            progress.update(f"({attempt}/{self.MAX_RETRIES})")
            try:
                system_info = await client.ping()
            except NotTissueServer:
                error_key = "connect_error_invalid_server"
                break
            except ConnectionFailed:
                error_key = "connect_error_unreachable"
            except ServerError:
                error_key = "connect_error_server"
            except TissueApiError as e:
                log.warning("CLI connect attempt %d failed: %s", attempt, e)
                error_key = "connect_error_generic"
            else:
                spinner.stop()
                await self._on_success(client, system_info)
                return

            if attempt < self.MAX_RETRIES:
                await asyncio.sleep(self.RETRY_DELAY)

        spinner.stop()
        await client.close()
        self.app.exit(
            return_code=1,
            message=f"{_CONNECT_ERROR_MESSAGES[error_key]} ({client.host})",
        )

    async def _on_success(self, client: TissueClient, system_info) -> None:
        from tissue.screens.auth.login import LoginScreen
        from tissue.screens.home.home import HomeScreen

        if self.app.client is not None:
            await self.app.client.close()
        self.app.client = client
        self.app.system_info = system_info
        self.config_manager.update_state(
            current_server_url=client.host,
            last_connected_at=datetime.now().astimezone(),
        )

        # Skip login screen when a stored session can be restored
        saved_token = self.app.token_store.load(client.host)
        if saved_token is not None:
            try:
                if await client.auth.restore_session(saved_token):
                    self.app.switch_screen(HomeScreen())
                    return
            except TissueApiError as e:
                log.debug("Session restore failed: %s", e)
            client.clear_tokens()

        self.app.switch_screen(LoginScreen(system_info, self.config_manager))

    @staticmethod
    def _is_valid_url(url: str) -> bool:
        if not url:
            return False
        try:
            _url_validator.validate_python(url)
            return True
        except ValidationError:
            return False
