import logging

from textual import events, on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import Button, Footer, Header, Input, Label, Static

from tissue.api.auth import AuthAPI
from tissue.api.errors import ApiNetworkError, ApiResponseError, TissueApiError
from tissue.assets.logo_small import TISSUE_LOGO_SMALL
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.models.auth import SystemInfo
from tissue.screens.signup import SignupScreen
from tissue.widgets.bracket_button import BracketButton
from tissue.widgets.modal_input import ModalInput
from tissue.widgets.social_button_small import SocialButtonSmall

log = logging.getLogger(__name__)

_FULL_HEIGHT_THRESHOLD = 40
_COMPACT_HEIGHT_THRESHOLD = 32


class LoginScreen(Screen):
    CSS_PATH = ["css/_buttons.tcss", "css/login.tcss"]

    BINDINGS = [
        Binding("escape", "back", "back"),
        Binding("down", "nav_down", show=False, priority=True),
        Binding("up", "nav_up", show=False, priority=True),
        Binding("left", "nav_left", show=False, priority=True),
        Binding("right", "nav_right", show=False, priority=True),
    ]

    def __init__(self, system_info: SystemInfo, config_manager: ConfigManager):
        super().__init__()
        self.system_info = system_info
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        url = self.config_manager.get_config().current_server
        identifier_placeholder = i18n.get(
            "email_placeholder"
            if self.system_info.is_email_required()
            else "username_placeholder"
        )
        yield Header()
        yield Container(
            Button("\u2190", id="back_btn", classes="back-btn", variant="default"),
            Static(TISSUE_LOGO_SMALL, classes="logo"),
            Label(f"Server: {url}", classes="subtitle"),
            ModalInput(
                placeholder=identifier_placeholder,
                id="identifier",
                classes="input-field",
            ),
            Label("", id="identifier_status", classes="status-msg"),
            ModalInput(
                placeholder=i18n.get("password_placeholder"),
                password=True,
                id="password",
                classes="input-field",
            ),
            Label("", id="password_status", classes="status-msg"),
            BracketButton(i18n.get("login_btn"), id="login_btn", classes="-secondary"),
            BracketButton(i18n.get("signup_btn"), id="signup_btn", classes="-success"),
            *self._oauth_separator(),
            *self._oauth_buttons(),
            *self._signup_notice(),
            id="login-dialog",
        )
        yield Footer()

    def on_mount(self) -> None:
        dialog = self.query_one("#login-dialog", Container)
        dialog.border_title = i18n.get("login_border_title")

        title_key = (
            "email_title" if self.system_info.is_email_required() else "username_title"
        )
        identifier_input = self.query_one("#identifier", ModalInput)
        identifier_input.border_title = i18n.get(title_key)
        password_input = self.query_one("#password", ModalInput)
        password_input.border_title = i18n.get("password_title")

        identifier_input.focus()

    def on_resize(self, event: events.Resize) -> None:
        self._apply_compact_mode()

    def _apply_compact_mode(self) -> None:
        dialog = self.query_one("#login-dialog", Container)
        h = self.size.height
        has_oauth = bool(self._social_providers())
        if h < _COMPACT_HEIGHT_THRESHOLD:
            dialog.add_class("-compact")
            dialog.remove_class("-auto")
        elif h < _FULL_HEIGHT_THRESHOLD or not has_oauth:
            dialog.add_class("-auto")
            dialog.remove_class("-compact")
        else:
            dialog.remove_class("-auto")
            dialog.remove_class("-compact")

    def check_action(self, action: str, parameters: tuple) -> bool | None:
        focused = self.focused
        if action in ("nav_left", "nav_right"):
            if not isinstance(focused, SocialButtonSmall):
                return False
        return True

    def action_nav_down(self) -> None:
        self.focus_next()

    def action_nav_up(self) -> None:
        self.focus_previous()

    def action_nav_left(self) -> None:
        self._nav_social(-1)

    def action_nav_right(self) -> None:
        self._nav_social(1)

    def action_back(self) -> None:
        self.app.pop_screen()

    def _nav_social(self, direction: int) -> None:
        focused = self.focused
        if not isinstance(focused, SocialButtonSmall):
            return
        buttons = list(self.query(SocialButtonSmall))
        try:
            idx = buttons.index(focused)
        except ValueError:
            return
        new_idx = idx + direction
        if 0 <= new_idx < len(buttons):
            buttons[new_idx].focus()

    def _oauth_separator(self) -> list[Label]:
        if not self._social_providers():
            return []
        # TODO: use i18n
        return [
            Label(
                "───────────── Continue with ─────────────",
                classes="oauth-provider-seperator",
            )
        ]

    def _oauth_buttons(self) -> list[Horizontal]:
        providers = self._social_providers()
        if not providers:
            return []
        return [
            Horizontal(
                *[SocialButtonSmall(p) for p in providers],
                classes="oauth-row",
            )
        ]

    def _social_providers(self) -> list[str]:
        return [p for p in self.system_info.setup.auth_providers if p != "EMAIL"]

    def _signup_notice(self) -> list[Label]:
        if self.system_info.setup.allow_signup:
            return []
        return [Label(i18n.get("signup_notice"), id="signup_notice")]

    @on(Button.Pressed, "#back_btn")
    def on_back_pressed(self) -> None:
        self.action_back()

    @on(Input.Changed, "#identifier")
    def on_identifier_changed(self, event: Input.Changed) -> None:
        self._clear_input_status("identifier")

    @on(Input.Changed, "#password")
    def on_password_changed(self, event: Input.Changed) -> None:
        self._clear_input_status("password")

    def _clear_input_status(self, input_id: str) -> None:
        self.query_one(f"#{input_id}", ModalInput).remove_class("error")
        lbl = self.query_one(f"#{input_id}_status", Label)
        lbl.update("")
        lbl.remove_class("error")

    def _set_input_error(self, input_id: str, message: str) -> None:
        self.query_one(f"#{input_id}", ModalInput).add_class("error")
        lbl = self.query_one(f"#{input_id}_status", Label)
        lbl.update(message)
        lbl.add_class("error")

    @on(Input.Submitted)
    @on(Button.Pressed, "#login_btn")
    def on_login(self) -> None:
        identifier_input = self.query_one("#identifier", ModalInput)
        password_input = self.query_one("#password", ModalInput)
        self._clear_input_status("identifier")
        self._clear_input_status("password")

        has_error = False
        if not identifier_input.value:
            self._set_input_error("identifier", i18n.get("validation_required"))
            has_error = True
        if not password_input.value:
            self._set_input_error("password", i18n.get("validation_required"))
            has_error = True
        if has_error:
            return

        self._do_login(identifier_input.value, password_input.value)

    @work(exclusive=True)
    async def _do_login(self, identifier: str, password: str) -> None:
        self.app.notify(i18n.get("logging_in"), timeout=3)
        try:
            res = await AuthAPI(self.app.client).login(identifier, password)
        except ApiResponseError as e:
            log.warning("Login failed: %s", e)
            self._mark_login_failed()
            return
        except ApiNetworkError as e:
            log.warning("Login network error: %s", e)
            self._mark_login_failed()
            return
        except TissueApiError as e:
            log.error("Login api error: %s", e)
            self._mark_login_failed()
            return

        self.app.notify(i18n.get("welcome", identifier=identifier), timeout=3)
        self.config_manager.save_tokens(res.access_token, res.refresh_token)

    def _mark_login_failed(self) -> None:
        msg = i18n.get("login_failed")
        self.app.notify(msg, severity="error", timeout=3)
        self.query_one("#identifier", ModalInput).add_class("error")
        self._set_input_error("password", msg)

    @on(Button.Pressed, "#signup_btn")
    def on_signup(self) -> None:
        self.app.push_screen(SignupScreen(self.system_info, self.config_manager))

    @on(SocialButtonSmall.Pressed)
    def on_social_small_pressed(self, event: SocialButtonSmall.Pressed) -> None:
        self._on_provider_chosen(event.provider)

    # TODO: add client
    def _on_provider_chosen(self, provider: str | None) -> None:
        if provider:
            self.app.notify(f"TODO: OAuth login via {provider.title()}", timeout=3)
