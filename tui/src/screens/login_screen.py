from textual.app import ComposeResult
from textual.screen import Screen
from textual.widgets import Header, Footer, Input, Button, Label, Static
from textual.containers import Container, Horizontal
from textual import on, events
from src.api.client import SystemInfo, ServerClient
from src.config import ConfigManager
from src.i18n.manager import i18n
from src.assets.logo import TISSUE_LOGO
from src.screens.signup_screen import SignupScreen

class LoginScreen(Screen):
    CSS = """
    LoginScreen { align: center middle; }
    #login-container { padding: 2; border: solid blue; width: 60%; height: auto; margin: 2; }
    .input-field { margin-bottom: 1; border: tall $background; }
    .input-field.error { border: tall red; }
    .logo { text-align: center; color: green; margin-bottom: 1; }
    .subtitle { text-align: center; color: yellow; margin-bottom: 2; }
    #btn-row { margin-top: 1; height: 3; align: center middle; }
    #login_btn { margin-right: 1; width: 1fr; }
    #signup_btn { margin-left: 1; width: 1fr; }
    #signup_notice { margin-top: 1; color: yellow; text-align: center; }
    """
    
    def __init__(self, system_info: SystemInfo):
        super().__init__()
        self.system_info = system_info
        self.config_manager = ConfigManager()

    def compose(self) -> ComposeResult:
        url = self.config_manager.get_config().current_server
        yield Header()
        yield Container(
            Static(TISSUE_LOGO, classes="logo"),
            Label(f"{self.system_info.server_name} ({url})", classes="subtitle"),
            Input(placeholder=i18n.get("email_placeholder"), id="email", classes="input-field"),
            Input(placeholder=i18n.get("password_placeholder"), password=True, id="password", classes="input-field"),
            Horizontal(
                Button(i18n.get("login_btn"), variant="primary", id="login_btn"),
                Button(i18n.get("signup_btn"), variant="default", id="signup_btn"),
                id="btn-row"
            ),
            Label(i18n.get("signup_notice") if not self.system_info.setup.allow_signup else "", id="signup_notice"),
            id="login-container"
        )
        yield Footer()

    def on_key(self, event: events.Key) -> None:
        if event.key == "down": self.focus_next()
        elif event.key == "up": self.focus_previous()

    @on(Input.Submitted)
    async def on_input_submitted(self): await self.on_login()

    @on(Button.Pressed, "#login_btn")
    async def on_login(self):
        e_in, p_in = self.query_one("#email", Input), self.query_one("#password", Input)
        e_in.remove_class("error"); p_in.remove_class("error")
        if not e_in.value or not p_in.value:
            self.app.notify(i18n.get("error_enter_credentials"), severity="error", timeout=3)
            if not e_in.value: e_in.add_class("error")
            if not p_in.value: p_in.add_class("error")
            return
        client = ServerClient(self.config_manager.get_config().current_server)
        self.app.notify(i18n.get("logging_in"), timeout=3)
        res = await client.login(e_in.value, p_in.value)
        if res:
            self.app.notify(i18n.get("welcome", email=e_in.value), timeout=3)
            self.config_manager.save_tokens(res.accessToken, res.refreshToken)
        else:
            self.app.notify(i18n.get("login_failed"), severity="error", timeout=3)
            e_in.add_class("error"); p_in.add_class("error")

    @on(Button.Pressed, "#signup_btn")
    def on_signup(self): self.app.push_screen(SignupScreen(self.system_info))