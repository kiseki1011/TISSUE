import re

from textual import events, on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import Button, Footer, Header, Input, Label

from tissue.api.client import TissueClient
from tissue.api.member import MemberAPI
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.models.auth import SystemInfo

EMAIL_PATTERN = re.compile(r"[^@]+@[^@]+\.[^@]+")


class SignupScreen(Screen):
    CSS_PATH = "css/signup.tcss"

    def __init__(self, system_info: SystemInfo):
        super().__init__()
        self.system_info = system_info
        self.config_manager = ConfigManager()
        self.signup_token = None
        self.verification_id = None
        self.check_timer = None
        self.email_timer = None
        self.username_timer = None

    def on_mount(self) -> None:
        self.query_one("#email", Input).focus()

    def compose(self) -> ComposeResult:
        yield Header()
        yield Container(
            Horizontal(
                Button("\u2190", id="back_btn", variant="default"),
                id="header_row",
            ),
            Label(i18n.get("signup_title"), classes="title"),
            Horizontal(
                Input(
                    placeholder=i18n.get("email_placeholder"),
                    id="email",
                    classes="input-field",
                ),
                Button("Verify", variant="primary", id="verify_btn"),
                id="email-row",
            ),
            Label("", id="email_status", classes="status-msg"),
            Input(
                placeholder=i18n.get("username_placeholder"),
                id="username",
                classes="input-field",
            ),
            Label("", id="username_status", classes="status-msg"),
            Input(
                placeholder=i18n.get("name_placeholder"),
                id="name",
                classes="input-field",
            ),
            Input(
                placeholder=i18n.get("password_placeholder"),
                password=True,
                id="password",
                classes="input-field",
            ),
            Horizontal(
                Button(
                    i18n.get("signup_btn"),
                    variant="success",
                    id="submit_btn",
                    disabled=True,
                ),
                id="btn-row",
            ),
            id="signup-container",
        )
        yield Footer()

    def on_key(self, event: events.Key) -> None:
        if event.key == "down":
            self.focus_next()
        elif event.key == "up":
            self.focus_previous()
        elif event.key == "escape":
            self.on_cancel()

    @on(Button.Pressed, "#back_btn")
    def on_back(self):
        self.on_cancel()

    @on(Input.Changed, "#email")
    def on_email_changed(self, event: Input.Changed):
        self.signup_token = None
        self.verification_id = None
        if self.check_timer:
            self.check_timer.stop()

        self.query_one("#verify_btn").disabled = False
        self.query_one("#verify_btn").display = True
        self.query_one("#verify_btn").variant = "primary"
        self.query_one("#verify_btn").label = "Verify"
        self.query_one("#submit_btn").disabled = True

        if self.email_timer:
            self.email_timer.stop()
        if event.value:
            self.update_status("#email", "#email_status", "")
            self.email_timer = self.set_timer(0.5, self.validate_email)
        else:
            self.update_status("#email", "#email_status", "")

    @on(Input.Changed, "#username")
    def on_username_changed(self, event: Input.Changed):
        if self.username_timer:
            self.username_timer.stop()
        if event.value:
            self.username_timer = self.set_timer(0.5, self.validate_username)
        else:
            self.update_status("#username", "#username_status", "")

    async def validate_email(self):
        email = self.query_one("#email", Input).value

        if not EMAIL_PATTERN.match(email):
            self.update_status(
                "#email", "#email_status", "Invalid email format", is_error=True
            )
            return

        client = TissueClient(self.config_manager.get_config().current_server)
        member_api = MemberAPI(client)
        status = await member_api.check_email_availability(email)

        if status == 204:
            self.update_status(
                "#email", "#email_status", "Email is available", is_error=False
            )
        elif status == 409:
            self.update_status(
                "#email", "#email_status", "Email is already in use", is_error=True
            )
        else:
            self.update_status("#email", "#email_status", "")

    async def validate_username(self):
        username = self.query_one("#username", Input).value
        if len(username) < 3:
            self.update_status(
                "#username", "#username_status", "Username too short", is_error=True
            )
            return

        client = TissueClient(self.config_manager.get_config().current_server)
        member_api = MemberAPI(client)
        is_available = await member_api.check_username_availability(username)
        if is_available:
            self.update_status(
                "#username", "#username_status", "Username is available", is_error=False
            )
        else:
            self.update_status(
                "#username",
                "#username_status",
                "Username is already taken",
                is_error=True,
            )

    def update_status(
        self, input_id: str, label_id: str, message: str, is_error: bool = False
    ):
        inp = self.query_one(input_id, Input)
        lbl = self.query_one(label_id, Label)
        lbl.update(message)

        inp.remove_class("error", "success")
        lbl.remove_class("error", "success", "waiting")

        if message:
            cls = "error" if is_error else "success"
            inp.add_class(cls)
            lbl.add_class(cls)

    @on(Button.Pressed, "#verify_btn")
    async def on_verify(self):
        email = self.query_one("#email", Input).value
        if not email or not EMAIL_PATTERN.match(email):
            self.update_status(
                "#email", "#email_status", "Invalid email format", is_error=True
            )
            return

        client = TissueClient(self.config_manager.get_config().current_server)
        member_api = MemberAPI(client)

        result = await member_api.request_verification(email)
        status = result["status"]
        ver_id = result["verificationId"]

        if status == 200 and ver_id:
            self.verification_id = ver_id
            self.app.notify("Email sent! Check inbox.")
            self.query_one("#verify_btn").disabled = True
            self.query_one("#verify_btn").label = "Sent"

            lbl = self.query_one("#email_status")
            lbl.update("Waiting for verification...")
            lbl.remove_class("error", "success")
            lbl.add_class("waiting")

            self.check_timer = self.set_interval(
                2.0, lambda: self.check_status(member_api)
            )
        elif status == 400:
            self.update_status(
                "#email", "#email_status", "Invalid email format", is_error=True
            )
        else:
            self.update_status(
                "#email",
                "#email_status",
                "Server failed to send email.",
                is_error=True,
            )

    async def check_status(self, member_api: MemberAPI):
        if not self.verification_id:
            return

        signup_token = await member_api.get_verification_status(self.verification_id)

        if signup_token:
            self.signup_token = signup_token
            if self.check_timer:
                self.check_timer.stop()

            lbl = self.query_one("#email_status")
            lbl.update("Email Verified!")
            lbl.classes = "status-msg success"

            self.query_one("#submit_btn").disabled = False

            btn = self.query_one("#verify_btn")
            btn.disabled = True
            btn.label = "Verified"
            btn.variant = "success"
            btn.display = True

    @on(Button.Pressed, "#submit_btn")
    async def on_signup(self):
        vals = [
            self.query_one(f"#{i}", Input).value
            for i in ["email", "username", "name", "password"]
        ]
        if not all(vals):
            self.app.notify(i18n.get("error_fill_all"), severity="error")
            return

        if not self.signup_token:
            self.app.notify("Email verification required", severity="error")
            return

        client = TissueClient(self.config_manager.get_config().current_server)
        member_api = MemberAPI(client)
        if await member_api.signup(*vals, self.signup_token):
            self.app.notify(i18n.get("signup_success"), timeout=3)
            self.app.pop_screen()
        else:
            self.app.notify(
                i18n.get("signup_failed", reason="Conflict"), severity="error"
            )

    @on(Button.Pressed, "#cancel_btn")
    def on_cancel(self):
        if self.check_timer:
            self.check_timer.stop()
        if self.email_timer:
            self.email_timer.stop()
        if self.username_timer:
            self.username_timer.stop()
        self.app.pop_screen()
