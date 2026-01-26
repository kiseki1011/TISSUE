from textual.app import ComposeResult
from textual.screen import Screen
from textual.widgets import Header, Footer, Input, Button, Label
from textual.containers import Container, Horizontal
from textual import on, events
from src.api.client import SystemInfo, ServerClient
from src.config import ConfigManager
from src.i18n.manager import i18n

class SignupScreen(Screen):
    CSS = """
    SignupScreen { align: center middle; }
    #signup-container { padding: 2; border: solid green; width: 60%; height: auto; margin: 2; }
    .input-field { margin-bottom: 1; border: tall $background; }
    .input-field.error { border: tall red; }
    .title { text-align: center; text-style: bold; margin-bottom: 2; }
    #email-row { height: 3; margin-bottom: 1; }
    #email { width: 3fr; }
    #verify_btn { width: 1fr; margin-left: 1; }
    .verified { color: green; text-style: bold; text-align: center; margin-bottom: 1; }
    .waiting { color: yellow; text-align: center; margin-bottom: 1; }
    #btn-row { margin-top: 1; height: 3; align: center middle; }
    #submit_btn { margin-right: 1; width: 1fr; }
    #cancel_btn { margin-left: 1; width: 1fr; }
    """

    def __init__(self, system_info: SystemInfo):
        super().__init__()
        self.system_info = system_info
        self.config_manager = ConfigManager()
        self.is_verified = False
        self.check_timer = None

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Label(i18n.get("signup_title"), classes="title"),
            Horizontal(
                Input(placeholder=i18n.get("email_placeholder"), id="email", classes="input-field"),
                Button("Verify", variant="primary", id="verify_btn"),
                id="email-row"
            ),
            Label("", id="status_label"),
            Input(placeholder=i18n.get("username_placeholder"), id="username", classes="input-field"),
            Input(placeholder=i18n.get("name_placeholder"), id="name", classes="input-field"),
            Input(placeholder=i18n.get("password_placeholder"), password=True, id="password", classes="input-field"),
            Horizontal(
                Button(i18n.get("signup_btn"), variant="success", id="submit_btn", disabled=True),
                Button(i18n.get("back_to_login"), variant="default", id="cancel_btn"),
                id="btn-row"
            ),
            id="signup-container"
        )
        yield Footer()

    def on_key(self, event: events.Key) -> None:
        if event.key == "down": self.focus_next()
        elif event.key == "up": self.focus_previous()

    @on(Button.Pressed, "#verify_btn")
    async def on_verify(self):
        email = self.query_one("#email", Input).value
        if not email or "@" not in email:
            self.app.notify("Invalid email", severity="error")
            return
        client = ServerClient(self.config_manager.get_config().current_server)
        if await client.request_verification(email):
            self.app.notify("Email sent!", timeout=3)
            self.query_one("#verify_btn").disabled = True
            lbl = self.query_one("#status_label")
            lbl.update("Waiting for verification..."); lbl.classes = "waiting"
            self.check_timer = self.set_interval(3.0, lambda: self.check_status(email, client))
        else: self.app.notify("Failed to send email", severity="error")

    async def check_status(self, email: str, client: ServerClient):
        if await client.check_verification_status(email):
            self.is_verified = True
            if self.check_timer: self.check_timer.stop()
            lbl = self.query_one("#status_label")
            lbl.update("✅ Email Verified!"); lbl.classes = "verified"
            self.query_one("#submit_btn").disabled = False
            self.query_one("#verify_btn").display = False

    @on(Button.Pressed, "#submit_btn")
    async def on_signup(self):
        vals = [self.query_one(f"#{i}", Input).value for i in ["email", "username", "name", "password"]]
        if not all(vals):
            self.app.notify(i18n.get("error_fill_all"), severity="error")
            return
        client = ServerClient(self.config_manager.get_config().current_server)
        if await client.signup(*vals):
            self.app.notify(i18n.get("signup_success"), timeout=3)
            self.app.pop_screen()
        else: self.app.notify(i18n.get("signup_failed", reason="Conflict"), severity="error")

    @on(Button.Pressed, "#cancel_btn")
    def on_cancel(self):
        if self.check_timer: self.check_timer.stop()
        self.app.pop_screen()
