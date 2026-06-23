import logging
from urllib.parse import urlparse

from textual import on, work
from textual.app import ComposeResult
from textual.containers import Center, Container, Horizontal
from textual.widgets import Button, Input, Label, Static

from tissue.api.errors import (
    ConnectionFailed,
    InvalidCredentials,
    ServerError,
    TissueApiError,
)
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.api.models.auth import TokenPair
from tissue.assets.logo import TISSUE_LOGO
from tissue.config.manager import ConfigManager
from tissue.screens.auth.restore_account_modal import RestoreAccountModal
from tissue.screens.base import TissueScreen
from tissue.widgets.footer import TissueFooter
from tissue.widgets.oidc_login_button import OidcLoginButton
from tissue.widgets.text_button import TextButton

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
        server_url = self.config_manager.state.current_server_url or ""
        if self._is_oidc_mode():
            yield self._oidc_dialog(server_url)
        else:
            yield self._local_dialog(server_url)
        yield TissueFooter()

    def _local_dialog(self, server_url: str) -> Container:
        # Left pane: logo (centered) + server URL subtitle
        left_pane = Container(
            Center(Static(TISSUE_LOGO, classes="logo")),
            Label(f"Server: {server_url}", classes="dialog-subtitle"),
            id="left-pane",
        )
        # Right pane: login form wrapped with extra container
        right_pane = Container(
            Container(*self._local_form_children(), id="login-form"),
            id="right-pane",
        )
        dialog = Container(left_pane, right_pane, id="dialog", classes="dialog")
        dialog.border_title = "Login to Tissue"
        return dialog

    def _oidc_dialog(self, server_url: str) -> Container:
        # No left/right pane split, kept across all sizes
        card = Container(
            Center(Static(TISSUE_LOGO, classes="logo")),
            Label(f"Server: {server_url}", classes="dialog-subtitle"),
            Container(*self._oidc_form_children(), classes="oidc-action"),
            id="oidc-card",
        )
        return Container(card, id="oidc-dialog")

    def _local_form_children(self) -> list:
        email_required = self._email_required()
        identifier_label = "Email" if email_required else "Username"
        identifier_placeholder = (
            "user@mycompany.com" if email_required else "yourusername"
        )

        identifier_input = Input(
            placeholder=identifier_placeholder,
            id="identifier",
            classes="input-field",
        )
        identifier_input.border_title = identifier_label

        password_input = Input(
            placeholder="********",
            password=True,
            id="password",
            classes="input-field",
        )
        password_input.border_title = "Password"

        return [
            identifier_input,
            Label("", id="identifier_status", classes="status-msg"),
            password_input,
            Label("", id="password_status", classes="status-msg"),
            Button("Login", id="login_btn"),
            Button(
                "Sign up",
                id="signup_btn",
                classes="-btn-success",
                disabled=not self._allow_signup(),
            ),
            Horizontal(
                TextButton("Restore Account", id="restore_link"),
                id="restore-row",
            ),
        ]

    def _oidc_form_children(self) -> list:
        oidc = self._oidc()
        icon_key = (oidc.provider_name if oidc else None) or ""
        return [
            OidcLoginButton(
                f"Login with {self._idp_label()}",
                icon_key=icon_key,
                id="oidc_login_btn",
            )
        ]

    def on_mount(self) -> None:
        if self._is_oidc_mode():
            self.query_one("#oidc_login_btn", Button).focus()
            return

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
            self._set_input_error("identifier", "Required field")
            has_error = True
        if not password:
            self._set_input_error("password", "Required field")
            has_error = True
        if has_error:
            return

        self._do_login(identifier, password)

    @on(Button.Pressed, "#signup_btn")
    def on_signup_pressed(self) -> None:
        from tissue.screens.auth.signup import SignupScreen

        self.app.push_screen(SignupScreen(self.system_info, self.config_manager))

    @on(Button.Pressed, "#restore_link")
    def on_restore_pressed(self) -> None:
        identifier = self.query_one("#identifier", Input).value.strip()
        modal = RestoreAccountModal(
            email_required=self._email_required(),
            prefill_identifier=identifier,
        )
        self.app.push_screen(modal, self._on_restore_closed)

    def _on_restore_closed(self, restored_identifier: str | None) -> None:
        """On restore success, prefill the login form and focus password."""
        if not restored_identifier:
            return
        identifier_input = self.query_one("#identifier", Input)
        identifier_input.value = restored_identifier
        self.query_one("#password", Input).focus()

    @on(Button.Pressed, "#oidc_login_btn")
    def on_oidc_pressed(self) -> None:
        from tissue.screens.auth.oidc_device import OidcDeviceModal

        self.app.push_screen(OidcDeviceModal(self._idp_label()), self._on_oidc_done)

    def _on_oidc_done(self, token: TokenPair | None) -> None:
        if token is not None:
            self._complete_oidc_login(token)

    @work(exclusive=True)
    async def _complete_oidc_login(self, token: TokenPair) -> None:
        client = self.app.client
        if client is None:
            log.error("OIDC login completed but TissueClient is not set")
            return
        client.set_tokens(token)
        await client._prefetch_user_context()
        profile = client.account.cached_profile
        if profile is None:
            client.clear_tokens()
            self.app.notify(
                "Login failed. Please try again.", severity="error", timeout=5
            )
            return
        identifier = profile.username or profile.email or ""
        self.app.notify(f"Welcome, {identifier}", timeout=3)
        self.app.route_to_post_login()

    @work(exclusive=True)
    async def _do_login(self, identifier: str, password: str) -> None:
        if self.app.client is None:
            log.error("Login attempted but TissueClient is not set")
            return

        self.app.notify("Logging in...", timeout=3)

        try:
            await self.app.client.auth.login(identifier, password)
        except InvalidCredentials:
            self._mark_login_failed()
            return
        except ConnectionFailed:
            self.app.notify(
                "Cannot reach server. Check the URL and network.",
                severity="error",
                timeout=5,
            )
            return
        except ServerError:
            self.app.notify(
                "Server error. Please try again later.", severity="error", timeout=5
            )
            return
        except TissueApiError as e:
            log.warning("Login failed: %s", e)
            if e.status == 429:
                self.app.notify(
                    "Too many login attempts. Please wait and try again.",
                    severity="error",
                    timeout=5,
                )
            else:
                self.app.notify(
                    "Login failed. Please try again.", severity="error", timeout=5
                )
            return

        self.app.notify(f"Welcome, {identifier}", timeout=3)
        self.app.route_to_post_login()

    def _mark_login_failed(self) -> None:
        self.query_one("#identifier", Input).add_class("-error")
        self._set_input_error("password", "Invalid credentials")

    def _clear_input_status(self, input_id: str) -> None:
        self.query_one(f"#{input_id}", Input).remove_class("-error")
        lbl = self.query_one(f"#{input_id}_status", Label)
        lbl.update("")
        lbl.remove_class("-error")

    def _set_input_error(self, input_id: str, message: str) -> None:
        self.query_one(f"#{input_id}", Input).add_class("-error")
        lbl = self.query_one(f"#{input_id}_status", Label)
        lbl.update(message)
        lbl.add_class("-error")

    def _email_required(self) -> bool:
        setup = self.system_info.setup
        return bool(setup and setup.email_required)

    def _allow_signup(self) -> bool:
        setup = self.system_info.setup
        return bool(setup and setup.allow_signup)

    def _is_oidc_mode(self) -> bool:
        setup = self.system_info.setup
        return bool(setup and (setup.auth_mode or "").upper() == "OIDC")

    def _oidc(self):
        setup = self.system_info.setup
        return setup.oidc if setup else None

    def _idp_label(self) -> str:
        oidc = self._oidc()
        label = "SSO"
        if oidc:
            if oidc.provider_name:
                label = oidc.provider_name
            elif oidc.issuer_uri:
                host = urlparse(oidc.issuer_uri).hostname
                if host:
                    label = host
        return label[:1].upper() + label[1:]
