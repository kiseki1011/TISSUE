import logging
from datetime import datetime

from pydantic import HttpUrl, TypeAdapter, ValidationError
from textual import on, work
from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.widgets import Button, Footer, Input, Label, Static

from tissue.api.client import TissueClient
from tissue.api.errors import (
    ConnectionFailed,
    NotTissueServer,
    ServerError,
    TissueApiError,
)
from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen
from tissue.screens.login import LoginScreen
from tissue.util.datetime_fmt import format_relative

log = logging.getLogger(__name__)

_url_validator = TypeAdapter(HttpUrl)


class ConnectScreen(TissueScreen):
    CSS_PATH = "connect.tcss"

    HORIZONTAL_BREAKPOINTS = [
        (0, "-narrow"),
        (72, "-normal"),
    ]

    VERTICAL_BREAKPOINTS = [
        (0, "-short"),
        (22, "-normal"),
    ]

    def __init__(
        self,
        config_manager: ConfigManager,
        failed_url: str | None = None,
    ) -> None:
        super().__init__()
        self.config_manager = config_manager
        self.failed_url = failed_url

    def compose(self) -> ComposeResult:
        url_input = Input(
            placeholder=i18n.get("connect_url_input_placeholder"),
            id="url_input",
        )
        url_input.border_title = i18n.get("connect_url_input_border_title")

        children: list = [
            Static(TISSUE_LOGO, classes="logo"),
            self._build_status_label(),
            Horizontal(
                url_input,
                Button(i18n.get("connect_server_btn"), id="connect_btn"),
                classes="input_row",
            ),
            Static(
                i18n.get("connect_url_err_msg"),
                id="url_err_msg",
                classes="error",
            ),
        ]

        dialog = Container(*children, classes="dialog", id="dialog")
        dialog.border_title = i18n.get("connect_dialog_border_title")

        yield dialog
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()

    def _build_status_label(self) -> Label:
        # Reconnection fails
        if self.failed_url:
            return Label(
                i18n.get("connect_failed_to_connect", url=self.failed_url),
                id="connect_status",
                classes="status-msg -error",
            )
        last_url = self.config_manager.state.current_server_url
        last_at = self.config_manager.state.last_connected_at

        # Current server url and last connected history exists
        if last_url and last_at:
            return Label(
                i18n.get(
                    "connect_last_connected",
                    url=last_url,
                    when=format_relative(last_at),
                ),
                id="connect_status",
                classes="status-msg",
            )

        # Current server url is empty
        return Label("", id="connect_status", classes="status-msg")

    @on(Button.Pressed, "#connect_btn")
    @on(Input.Submitted, "#url_input")
    def connect_server(self) -> None:
        url = self.query_one("#url_input", Input).value.strip()

        if not self._is_valid_url(url):
            self._show_error("connect_url_err_msg")
            return

        self._hide_error()
        self._do_connect(url)

    @on(Input.Changed, "#url_input")
    def url_input_changed(self) -> None:
        self._hide_error()

    @work(exclusive=True)
    async def _do_connect(self, url: str) -> None:
        client = TissueClient(host=url, token_store=self.app.token_store)
        try:
            system_info = await client.ping()
        except ConnectionFailed:
            await client.close()
            self._show_error("connect_error_unreachable")
            return
        except NotTissueServer:
            await client.close()
            self._show_error("connect_error_invalid_server")
            return
        except ServerError:
            await client.close()
            self._show_error("connect_error_server")
            return
        except TissueApiError as e:
            log.warning("connect failed: %s", e)
            await client.close()
            self._show_error("connect_error_generic")
            return

        if self.app.client is not None:
            await self.app.client.close()

        self.app.client = client
        self.app.system_info = system_info
        self.config_manager.update_state(
            current_server_url=client.host,
            last_connected_at=datetime.now().astimezone(),
        )

        if self.failed_url is not None:
            self.failed_url = None
            self.refresh(recompose=True)

        self.app.push_screen(LoginScreen(system_info, self.config_manager))

    def _show_error(self, message_key: str) -> None:
        self.query_one("#url_input", Input).add_class("-error")
        err_static = self.query_one("#url_err_msg", Static)
        err_static.update(i18n.get(message_key))
        err_static.add_class("-visible")

    def _hide_error(self) -> None:
        self.query_one("#url_input", Input).remove_class("-error")
        self.query_one("#url_err_msg", Static).remove_class("-visible")

    @staticmethod
    def _is_valid_url(url: str) -> bool:
        if not url:
            return False
        try:
            _url_validator.validate_python(url)
            return True
        except ValidationError:
            return False
