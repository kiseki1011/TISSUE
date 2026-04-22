from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import Button, Footer, Header, Input, Label, Static

from tissue.api.auth import AuthAPI
from tissue.api.factory import create_client
from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.models.auth import SystemInfo
from tissue.screens.signup import SignupScreen
from tissue.screens.social_login import SocialLoginModal
from tissue.widgets.bracket_button import BracketButton
from tissue.widgets.modal_input import ModalInput


class LoginScreen(Screen):
    CSS_PATH = "css/login.tcss"

    BINDINGS = [
        Binding("escape", "back", "Back"),
        Binding("down", "nav_down", show=False, priority=True),
        Binding("up", "nav_up", show=False, priority=True),
        Binding("left", "nav_left", show=False, priority=True),
        Binding("right", "nav_right", show=False, priority=True),
        Binding("j", "vim_down", show=False, priority=True),
        Binding("k", "vim_up", show=False, priority=True),
        Binding("h", "vim_left", show=False, priority=True),
        Binding("l", "vim_right", show=False, priority=True),
    ]

    def __init__(self, system_info: SystemInfo, config_manager: ConfigManager):
        super().__init__()
        self.system_info = system_info
        self.config_manager = config_manager

    def on_mount(self) -> None:
        dialog = self.query_one("#dialog", Container)
        title = i18n.get("login_border_title")
        if self.config_manager.get_config().stub_mode:
            badge = i18n.get("stub_mode_badge")
            title = f"{title} [$accent]{badge}[/]"
        dialog.border_title = title

        title_key = (
            "email_title" if self.system_info.is_email_required() else "username_title"
        )
        email_in = self.query_one("#email", ModalInput)
        email_in.border_title = i18n.get(title_key)
        password_in = self.query_one("#password", ModalInput)
        password_in.border_title = i18n.get("password_title")

        email_in.focus()

    def compose(self) -> ComposeResult:
        url = self.config_manager.get_config().current_server
        id_placeholder = i18n.get(
            "email_placeholder"
            if self.system_info.is_email_required()
            else "username_placeholder"
        )
        yield Header()
        yield Container(
            Button("\u2190", id="back_btn", variant="default"),
            Static(TISSUE_LOGO, classes="logo"),
            Label(f"Server: {url}", classes="subtitle"),
            Horizontal(
                ModalInput(
                    placeholder=id_placeholder,
                    id="email",
                    classes="input-field",
                ),
                id="email_row",
                classes="centered-row",
            ),
            Horizontal(
                ModalInput(
                    placeholder="*********",
                    password=True,
                    id="password",
                    classes="input-field",
                ),
                id="password_row",
                classes="centered-row",
            ),
            Horizontal(
                Horizontal(
                    BracketButton(i18n.get("login_btn"), id="login_btn"),
                    BracketButton(i18n.get("signup_btn"), id="signup_btn"),
                    id="btn-row",
                ),
                id="btn_row_wrapper",
                classes="centered-row",
            ),
            *self._oauth_row_children(),
            Label(
                i18n.get("signup_notice")
                if not self.system_info.setup.allow_signup
                else "",
                id="signup_notice",
            ),
            id="dialog",
        )
        yield Footer()

    def check_action(self, action: str, parameters: tuple) -> bool | None:
        focused = self.focused
        if action in ("vim_down", "vim_up", "vim_left", "vim_right"):
            if isinstance(focused, ModalInput) and focused._editing:
                return False
        if action in ("nav_left", "nav_right"):
            if not isinstance(focused, BracketButton):
                return False
            if focused.id == "oauth_btn":
                return False
        return True

    def action_nav_down(self) -> None:
        focused = self.focused
        if isinstance(focused, BracketButton):
            if focused.id in ("login_btn", "signup_btn"):
                oauth = self._oauth_button()
                if oauth is not None:
                    oauth.focus()
                return
            if focused.id == "oauth_btn":
                return
        self.focus_next()

    def action_nav_up(self) -> None:
        focused = self.focused
        if isinstance(focused, BracketButton):
            if focused.id in ("login_btn", "signup_btn"):
                self.query_one("#password", ModalInput).focus()
                return
            if focused.id == "oauth_btn":
                self.query_one("#login_btn", BracketButton).focus()
                return
        self.focus_previous()

    def action_nav_left(self) -> None:
        focused = self.focused
        if isinstance(focused, BracketButton) and focused.id in (
            "login_btn",
            "signup_btn",
        ):
            self._toggle_btn_row()

    def action_nav_right(self) -> None:
        focused = self.focused
        if isinstance(focused, BracketButton) and focused.id in (
            "login_btn",
            "signup_btn",
        ):
            self._toggle_btn_row()

    def action_vim_down(self) -> None:
        self.action_nav_down()

    def action_vim_up(self) -> None:
        self.action_nav_up()

    def action_vim_left(self) -> None:
        self.action_nav_left()

    def action_vim_right(self) -> None:
        self.action_nav_right()

    def _toggle_btn_row(self) -> None:
        focused = self.focused
        if not isinstance(focused, BracketButton):
            return
        target = "signup_btn" if focused.id == "login_btn" else "login_btn"
        self.query_one(f"#{target}", BracketButton).focus()

    def _oauth_button(self) -> BracketButton | None:
        try:
            return self.query_one("#oauth_btn", BracketButton)
        except Exception:
            return None

    def _social_providers(self) -> list[str]:
        return [p for p in self.system_info.setup.auth_providers if p != "EMAIL"]

    def _oauth_row_children(self):
        if not self._social_providers():
            return []
        return [
            Horizontal(
                Horizontal(
                    BracketButton(i18n.get("oauth_btn"), id="oauth_btn"),
                    id="oauth-row",
                ),
                id="oauth_row_wrapper",
                classes="centered-row",
            ),
        ]

    def action_back(self):
        self.app.pop_screen()

    @on(Button.Pressed, "#back_btn")
    def on_back_pressed(self):
        self.action_back()

    @on(Input.Submitted)
    async def on_input_submitted(self):
        await self.on_login()

    @on(Button.Pressed, "#login_btn")
    async def on_login(self):
        e_in = self.query_one("#email", ModalInput)
        p_in = self.query_one("#password", ModalInput)
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

        client = create_client(
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
        self.app.push_screen(SignupScreen(self.system_info, self.config_manager))

    @on(Button.Pressed, "#oauth_btn")
    def on_oauth(self) -> None:
        providers = self._social_providers()
        if not providers:
            return
        self.app.push_screen(SocialLoginModal(providers), self._on_provider_chosen)

    def _on_provider_chosen(self, provider: str | None) -> None:
        if provider:
            self.app.notify(
                f"OAuth login via {provider.title()} — coming soon", timeout=3
            )
