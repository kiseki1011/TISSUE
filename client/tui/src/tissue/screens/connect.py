from pydantic import HttpUrl, TypeAdapter, ValidationError
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import Button, Footer, Header, Input, Static

from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n

_url_validator = TypeAdapter(HttpUrl)


class ConnectScreen(Screen):
    CSS_PATH = "connect.tcss"

    BINDINGS = [
        Binding("ctrl+o", "option_menu", "options"),
    ]

    HORIZONTAL_BREAKPOINTS = [
        (0, "-narrow"),
        (72, "-normal"),
    ]

    VERTICAL_BREAKPOINTS = [
        (0, "-short"),
        (22, "-normal"),
    ]

    def __init__(self, config_manager: ConfigManager) -> None:
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Static(TISSUE_LOGO, classes="logo"),
            Horizontal(
                Input(
                    placeholder=i18n.get("connect_url_input_placeholder"),
                    id="url_input",
                ),
                Button(i18n.get("connect_server_btn"), id="connect_btn"),
                classes="input_row",
            ),
            Static(i18n.get("connect_url_err_msg"), id="url_err_msg", classes="error"),
            classes="dialog",
            id="dialog",
        )
        yield Footer()

    def on_mount(self) -> None:
        dialog_container = self.query_one("#dialog", Container)
        dialog_container.border_title = i18n.get("connect_dialog_border_title")

        server_url_input = self.query_one("#url_input", Input)
        server_url_input.border_title = i18n.get("connect_url_input_border_title")

    @on(Button.Pressed, "#connect_btn")
    @on(Input.Submitted, "#url_input")
    def connect_server(self) -> None:
        url = self.query_one("#url_input", Input).value.strip()

        if not self._is_valid_url(url):
            self._show_error()
            return

        self._hide_error()
        # TODO: @work로 실제 연결 시도
        # self._do_connect(url)

    @on(Input.Changed, "#url_input")
    def url_input_changed(self) -> None:
        self._hide_error()

    # @work(exclusive=True)
    # async def _do_connect(self, url: str) -> None:

    def action_option_menu(self) -> None:
        # Settings 모달 띄우기
        pass

    def _show_error(self) -> None:
        self.query_one("#url_input", Input).add_class("-error")
        self.query_one("#url_err_msg", Static).add_class("-visible")

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
