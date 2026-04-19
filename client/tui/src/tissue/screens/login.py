from textual import events, on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import Button, Footer, Header, Input, Label, Static

from tissue.api.auth import AuthAPI
from tissue.api.client import TissueClient
from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.models.auth import SystemInfo
from tissue.screens.signup import SignupScreen


class LoginScreen(Screen):
    CSS_PATH = "css/login.tcss"

    def __init__(self, system_info: SystemInfo):
        super().__init__()
        self.system_info = system_info
        self.config_manager = ConfigManager()

    def on_mount(self) -> None:
        self.query_one("#email", Input).focus()

    def compose(self) -> ComposeResult:
        url = self.config_manager.get_config().current_server
        yield Header()
        yield Container(
            Horizontal(
                Button("\u2190", id="back_btn", variant="default"),
                id="header_row",
            ),
            Static(TISSUE_LOGO, classes="logo"),
            Label("Terminal Issue Collaboration", classes="title"),
            Label(f"Server: {url}", classes="subtitle"),
            Input(
                placeholder=i18n.get("email_placeholder"),
                id="email",
                classes="input-field",
            ),
            Input(
                placeholder=i18n.get("password_placeholder"),
                password=True,
                id="password",
                classes="input-field",
            ),
            Horizontal(
                Button(i18n.get("login_btn"), variant="primary", id="login_btn"),
                Button(i18n.get("signup_btn"), variant="success", id="signup_btn"),
                id="btn-row",
            ),
            Label(
                i18n.get("signup_notice")
                if not self.system_info.setup.allow_signup
                else "",
                id="signup_notice",
            ),
            id="login-container",
        )
        yield Footer()

    def on_key(self, event: events.Key) -> None:
        if event.key == "down":
            self.focus_next()
        elif event.key == "up":
            self.focus_previous()
        elif event.key == "escape":
            self.on_back()

    @on(Button.Pressed, "#back_btn")
    def on_back(self):
        self.app.pop_screen()

    @on(Input.Submitted)
    async def on_input_submitted(self):
        await self.on_login()

    @on(Button.Pressed, "#login_btn")
    async def on_login(self):
        e_in = self.query_one("#email", Input)
        p_in = self.query_one("#password", Input)
        e_in.remove_class("error")
        p_in.remove_class("error")

        if not e_in.value or not p_in.value:
            self.app.notify(
                i18n.get("error_enter_credentials"), severity="error", timeout=3
            )
            if not e_in.value:
                e_in.add_class("error")
            if not p_in.value:
                p_in.add_class("error")
            return

        client = TissueClient(
            self.config_manager.get_config().current_server, self.config_manager
        )
        auth_api = AuthAPI(client)
        self.app.notify(i18n.get("logging_in"), timeout=3)
        res = await auth_api.login(e_in.value, p_in.value)
        if res:
            self.app.notify(i18n.get("welcome", email=e_in.value), timeout=3)
            self.config_manager.save_tokens(res.access_token, res.refresh_token)
        else:
            self.app.notify(i18n.get("login_failed"), severity="error", timeout=3)
            e_in.add_class("error")
            p_in.add_class("error")

    @on(Button.Pressed, "#signup_btn")
    def on_signup(self):
        self.app.push_screen(SignupScreen(self.system_info))
