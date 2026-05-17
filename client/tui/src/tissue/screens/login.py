import logging

from textual import on, work
from textual.app import ComposeResult
from textual.containers import Center, Container, Horizontal
from textual.widgets import Button, Footer, Header, Input, Label, Static

from tissue.api.errors import (
    ConnectionFailed,
    InvalidCredentials,
    ServerError,
    TissueApiError,
)
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueScreen
from tissue.widgets.social_button import SocialButton

log = logging.getLogger(__name__)


class LoginScreen(TissueScreen):
    CSS_PATH = "login.tcss"

    HORIZONTAL_BREAKPOINTS = [
        (0, "-h-narrow"),
        (78, "-h-medium"),
        (155, "-h-wide"),
    ]

    VERTICAL_BREAKPOINTS = [
        (0, "-v-short"),
        (42, "-v-tall"),
    ]

    def __init__(
        self, system_info: SystemInfoDetails, config_manager: ConfigManager
    ) -> None:
        super().__init__()
        self.system_info = system_info
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        email_required = self._email_required()
        identifier_label_key = (
            "login_email_label" if email_required else "login_username_label"
        )
        identifier_placeholder_key = (
            "login_email_placeholder"
            if email_required
            else "login_username_placeholder"
        )

        identifier_input = Input(
            placeholder=i18n.get(identifier_placeholder_key),
            id="identifier",
            classes="input-field",
        )
        identifier_input.border_title = i18n.get(identifier_label_key)

        password_input = Input(
            placeholder=i18n.get("login_password_placeholder"),
            password=True,
            id="password",
            classes="input-field",
        )
        password_input.border_title = i18n.get("login_password_label")

        server_url = self.config_manager.state.current_server_url or ""

        form_children: list = [
            identifier_input,
            Label("", id="identifier_status", classes="status-msg"),
            password_input,
            Label("", id="password_status", classes="status-msg"),
            Button(i18n.get("login_btn"), id="login_btn"),
            Button(
                i18n.get("login_signup_btn"),
                id="signup_btn",
                classes="-btn-success",
            ),
        ]
        providers = self._social_providers()
        if providers:
            form_children.append(
                Label(
                    i18n.get("login_oauth_separator"),
                    classes="oauth-separator",
                )
            )
            form_children.append(
                Horizontal(
                    *[SocialButton(p) for p in providers],
                    classes="oauth-row",
                )
            )
        if not self._allow_signup():
            form_children.append(
                Label(i18n.get("login_signup_disabled_notice"), id="signup_notice")
            )

        # Left pane: logo (centered) + server URL subtitle
        left_pane = Container(
            Center(Static(TISSUE_LOGO, classes="logo")),
            Label(f"Server: {server_url}", classes="dialog-subtitle"),
            id="left-pane",
        )

        # Right pane: login form wrapped with extra container so the scrollbar
        # attaches to the pane edge
        right_pane = Container(
            Container(*form_children, id="login-form"),
            id="right-pane",
        )

        dialog = Container(
            left_pane,
            right_pane,
            id="dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("login_dialog_border_title")

        yield Header()
        yield dialog
        yield Footer()

    def on_mount(self) -> None:
        self._apply_initial_breakpoints()
        self.query_one("#identifier", Input).focus()

    @on(Input.Changed, "#identifier")
    def on_identifier_changed(self) -> None:
        self._clear_input_status("identifier")

    @on(Input.Changed, "#password")
    def on_password_changed(self) -> None:
        self._clear_input_status("password")

    @on(Input.Submitted)
    @on(Button.Pressed, "#login_btn")
    def on_login_pressed(self) -> None:
        identifier = self.query_one("#identifier", Input).value.strip()
        password = self.query_one("#password", Input).value

        self._clear_input_status("identifier")
        self._clear_input_status("password")

        has_error = False
        if not identifier:
            self._set_input_error("identifier", "login_validation_required")
            has_error = True
        if not password:
            self._set_input_error("password", "login_validation_required")
            has_error = True
        if has_error:
            return

        self._do_login(identifier, password)

    @on(Button.Pressed, "#signup_btn")
    def on_signup_pressed(self) -> None:
        from tissue.screens.signup import SignupScreen

        self.app.push_screen(SignupScreen(self.system_info, self.config_manager))

    # TODO: _do_social_login()
    @on(Button.Pressed, "SocialButton")
    def on_social_pressed(self, event: Button.Pressed) -> None:
        if not isinstance(event.button, SocialButton):
            return
        self.app.notify(
            f"TODO: OAuth login via {event.button.provider.title()}",
            timeout=3,
        )

    # TODO: OIDC button/login

    @work(exclusive=True)
    async def _do_login(self, identifier: str, password: str) -> None:
        if self.app.client is None:
            log.error("Login attempted but TissueClient is not set")
            return

        self.app.notify(i18n.get("login_logging_in"), timeout=3)

        try:
            await self.app.client.login(identifier, password)
        except InvalidCredentials:
            self._mark_login_failed()
            return
        except ConnectionFailed:
            self.app.notify(
                i18n.get("login_error_unreachable"), severity="error", timeout=5
            )
            return
        except ServerError:
            self.app.notify(i18n.get("login_error_server"), severity="error", timeout=5)
            return
        except TissueApiError as e:
            log.warning("Login failed: %s", e)
            if e.status == 429:
                self.app.notify(
                    i18n.get("login_error_rate_limited"),
                    severity="error",
                    timeout=5,
                )
            else:
                self.app.notify(
                    i18n.get("login_error_generic"), severity="error", timeout=5
                )
            return

        self.app.notify(i18n.get("login_welcome", identifier=identifier), timeout=3)
        self.app.route_to_post_login()

    def _mark_login_failed(self) -> None:
        self.query_one("#identifier", Input).add_class("-error")
        self._set_input_error("password", "login_failed")

    def _clear_input_status(self, input_id: str) -> None:
        self.query_one(f"#{input_id}", Input).remove_class("-error")
        lbl = self.query_one(f"#{input_id}_status", Label)
        lbl.update("")
        lbl.remove_class("-error")

    def _set_input_error(self, input_id: str, message_key: str) -> None:
        self.query_one(f"#{input_id}", Input).add_class("-error")
        lbl = self.query_one(f"#{input_id}_status", Label)
        lbl.update(i18n.get(message_key))
        lbl.add_class("-error")

    def _email_required(self) -> bool:
        setup = self.system_info.setup
        return bool(setup and setup.email_required)

    def _allow_signup(self) -> bool:
        setup = self.system_info.setup
        return bool(setup and setup.allow_signup)

    def _social_providers(self) -> list[str]:
        setup = self.system_info.setup
        if not setup or not setup.auth_providers:
            return []
        return [p for p in setup.auth_providers if p != "EMAIL"]
