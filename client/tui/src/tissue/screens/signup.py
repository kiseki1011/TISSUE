import logging
import re

from textual import events, on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import Button, Footer, Header, Input, Label, Static

from tissue.api.auth import AuthAPI
from tissue.api.errors import ApiNetworkError, ApiResponseError, TissueApiError
from tissue.api.member import MemberAPI
from tissue.assets.logo_small import TISSUE_LOGO_SMALL
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.models.auth import SystemInfo
from tissue.widgets.i18n_widgets import (
    I18nButton,
    I18nContainer,
    I18nInput,
    I18nLabel,
)
from tissue.widgets.modal_input import ModalInput
from tissue.widgets.spinner import Spinner

log = logging.getLogger(__name__)

EMAIL_PATTERN = re.compile(r"[^@]+@[^@]+\.[^@]+")

_FULL_HEIGHT_THRESHOLD = 44
_COMPACT_HEIGHT_THRESHOLD = 34


class SignupScreen(Screen[str | None]):
    CSS_PATH = ["css/_buttons.tcss", "css/signup.tcss"]

    BINDINGS = [
        Binding("escape", "back", "back"),
        Binding("down", "nav_down", show=False, priority=True),
        Binding("up", "nav_up", show=False, priority=True),
    ]

    def __init__(self, system_info: SystemInfo, config_manager: ConfigManager):
        super().__init__()
        self.system_info = system_info
        self.config_manager = config_manager
        self.email_required = system_info.is_email_required()
        self.verified_token: str | None = None
        self.verification_id: str | None = None
        self.verification_poll_timer = None
        self.email_debounce_timer = None
        self.username_debounce_timer = None
        self.email_spinner: Spinner | None = None

    def compose(self) -> ComposeResult:
        url = self.config_manager.get_config().current_server
        email_widgets = []
        if self.email_required:
            email_widgets = [
                Horizontal(
                    I18nInput(
                        placeholder_key="email_placeholder",
                        title_key="email_title",
                        id="email",
                        classes="input-field",
                    ),
                    I18nButton(
                        key="verify_btn",
                        id="verify_btn",
                        classes="-secondary",
                        disabled=True,
                    ),
                    id="email-row",
                ),
                I18nLabel("", id="email_status", classes="status-msg"),
            ]

        yield Header()
        yield I18nContainer(
            Button("←", id="back_btn", classes="back-btn", variant="default"),
            Static(TISSUE_LOGO_SMALL, classes="logo"),
            Label(f"Server: {url}", classes="subtitle"),
            *email_widgets,
            I18nInput(
                placeholder_key="username_placeholder",
                title_key="username_title",
                id="username",
                classes="input-field",
            ),
            I18nLabel("", id="username_status", classes="status-msg"),
            I18nInput(
                placeholder_key="name_placeholder",
                title_key="name_title",
                id="name",
                classes="input-field",
            ),
            I18nInput(
                placeholder_key="password_placeholder",
                title_key="password_title",
                id="password",
                password=True,
                classes="input-field",
            ),
            I18nInput(
                placeholder_key="password_confirm_placeholder",
                title_key="password_confirm_title",
                id="password_confirm",
                password=True,
                classes="input-field",
            ),
            I18nLabel("", id="password_confirm_status", classes="status-msg"),
            I18nButton(
                key="signup_btn",
                id="submit_btn",
                classes="-success",
                disabled=self.email_required,
            ),
            id="signup-dialog",
            title_key="signup_title",
        )
        yield Footer()

    def on_mount(self) -> None:
        dialog = self.query_one("#signup-dialog", Container)
        if not self.email_required:
            dialog.add_class("-no-email")

        if self.email_required:
            self.email_spinner = Spinner(self, self.query_one("#email_status", Label))

        first_id = "#email" if self.email_required else "#username"
        self.query_one(first_id, ModalInput).focus()

    def on_resize(self, event: events.Resize) -> None:
        self._apply_compact_mode()

    def _apply_compact_mode(self) -> None:
        dialog = self.query_one("#signup-dialog", Container)
        h = self.size.height
        if h < _COMPACT_HEIGHT_THRESHOLD:
            dialog.add_class("-compact")
            dialog.remove_class("-auto")
        elif h < _FULL_HEIGHT_THRESHOLD:
            dialog.add_class("-auto")
            dialog.remove_class("-compact")
        else:
            dialog.remove_class("-auto")
            dialog.remove_class("-compact")

    def action_nav_down(self) -> None:
        self.focus_next()

    def action_nav_up(self) -> None:
        self.focus_previous()

    def action_back(self) -> None:
        self._stop_timers()
        self.dismiss(None)

    @on(Button.Pressed, "#back_btn")
    def on_back_pressed(self) -> None:
        self.action_back()

    def _stop_timers(self) -> None:
        if self.verification_poll_timer:
            self.verification_poll_timer.stop()
        if self.email_debounce_timer:
            self.email_debounce_timer.stop()
        if self.username_debounce_timer:
            self.username_debounce_timer.stop()
        self._stop_email_spinner()

    def _start_email_spinner(self) -> None:
        if self.email_spinner:
            self.email_spinner.start(i18n.get("email_waiting"))

    def _stop_email_spinner(self) -> None:
        if self.email_spinner:
            self.email_spinner.stop()

    @on(Input.Changed, "#email")
    def on_email_changed(self, event: Input.Changed) -> None:
        self.verified_token = None
        self.verification_id = None
        if self.verification_poll_timer:
            self.verification_poll_timer.stop()
        self._stop_email_spinner()

        verify_btn = self.query_one("#verify_btn", I18nButton)
        verify_btn.disabled = True
        verify_btn.set_i18n_key("verify_btn")

        self.query_one("#submit_btn", I18nButton).disabled = True

        if self.email_debounce_timer:
            self.email_debounce_timer.stop()
        if event.value:
            self._set_status("#email", "#email_status", None)
            self.email_debounce_timer = self.set_timer(0.5, self.validate_email)
        else:
            self._set_status("#email", "#email_status", None)

    @on(Input.Changed, "#username")
    def on_username_changed(self, event: Input.Changed) -> None:
        if self.username_debounce_timer:
            self.username_debounce_timer.stop()
        if event.value:
            self.username_debounce_timer = self.set_timer(0.5, self.validate_username)
        else:
            self._set_status("#username", "#username_status", None)

    @on(Input.Changed, "#password")
    @on(Input.Changed, "#password_confirm")
    def on_password_changed(self, event: Input.Changed) -> None:
        self._check_password_match()

    def _check_password_match(self) -> None:
        pw = self.query_one("#password", ModalInput).value
        confirm = self.query_one("#password_confirm", ModalInput).value
        if not confirm:
            self._set_status("#password_confirm", "#password_confirm_status", None)
            return
        if pw == confirm:
            self._set_status(
                "#password_confirm",
                "#password_confirm_status",
                "password_match",
                is_error=False,
            )
        else:
            self._set_status(
                "#password_confirm",
                "#password_confirm_status",
                "password_mismatch",
                is_error=True,
            )

    def validate_email(self) -> None:
        email = self.query_one("#email", ModalInput).value
        if not EMAIL_PATTERN.match(email):
            self._set_status("#email", "#email_status", "email_invalid", is_error=True)
            return
        self._check_email(email)

    @work(exclusive=True, group="check_email")
    async def _check_email(self, email: str) -> None:
        try:
            is_available = await MemberAPI(self.app.client).check_email_availability(
                email
            )
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Email availability check failed: %s", e)
            self._set_status("#email", "#email_status", None)
            return
        verify_btn = self.query_one("#verify_btn", I18nButton)
        if is_available:
            verify_btn.disabled = False
            self._set_status(
                "#email", "#email_status", "email_available", is_error=False
            )
        else:
            verify_btn.disabled = True
            self._set_status("#email", "#email_status", "email_taken", is_error=True)

    def validate_username(self) -> None:
        username = self.query_one("#username", ModalInput).value
        if len(username) < 3:
            self._set_status(
                "#username", "#username_status", "username_short", is_error=True
            )
            return
        self._check_username(username)

    @work(exclusive=True, group="check_username")
    async def _check_username(self, username: str) -> None:
        try:
            is_available = await MemberAPI(self.app.client).check_username_availability(
                username
            )
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Username availability check failed: %s", e)
            self._set_status("#username", "#username_status", None)
            return
        if is_available:
            self._set_status(
                "#username", "#username_status", "username_available", is_error=False
            )
        else:
            self._set_status(
                "#username", "#username_status", "username_taken", is_error=True
            )

    def _set_status(
        self,
        input_id: str,
        label_id: str,
        key: str | None,
        is_error: bool = False,
    ) -> None:
        inp = self.query_one(input_id, ModalInput)
        lbl = self.query_one(label_id, I18nLabel)

        inp.remove_class("error", "success")
        lbl.remove_class("error", "success", "waiting")

        if not key:
            lbl.clear_i18n()
            return

        lbl.set_i18n_key(key)
        cls = "error" if is_error else "success"
        inp.add_class(cls)
        lbl.add_class(cls)

    @on(I18nButton.Pressed, "#verify_btn")
    def on_verify(self) -> None:
        email = self.query_one("#email", ModalInput).value
        if not email or not EMAIL_PATTERN.match(email):
            self._set_status("#email", "#email_status", "email_invalid", is_error=True)
            return
        self._do_request_verification(email)

    @work(exclusive=True)
    async def _do_request_verification(self, email: str) -> None:
        try:
            ver_id = await MemberAPI(self.app.client).request_verification(email)
        except ApiResponseError as e:
            log.warning("Verification request failed: %s", e)
            if e.status_code == 400:
                self._set_status(
                    "#email", "#email_status", "email_invalid", is_error=True
                )
            else:
                self._set_status(
                    "#email", "#email_status", "email_send_failed", is_error=True
                )
            return
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Verification request error: %s", e)
            self._set_status(
                "#email", "#email_status", "email_send_failed", is_error=True
            )
            return

        self.verification_id = ver_id
        self.app.notify(i18n.get("email_sent_notify"))
        verify_btn = self.query_one("#verify_btn", I18nButton)
        verify_btn.disabled = True
        verify_btn.set_i18n_key("verify_sent")

        lbl = self.query_one("#email_status", I18nLabel)
        lbl.remove_class("error", "success")
        lbl.add_class("waiting")
        self._start_email_spinner()

        self.verification_poll_timer = self.set_interval(2.0, self.check_status)

    @work(group="check_status")
    async def check_status(self) -> None:
        if not self.verification_id:
            return

        try:
            verified_token = await MemberAPI(self.app.client).get_verification_status(
                self.verification_id
            )
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Verification status check failed: %s", e)
            return

        if verified_token:
            self.verified_token = verified_token
            if self.verification_poll_timer:
                self.verification_poll_timer.stop()
            self._stop_email_spinner()

            lbl = self.query_one("#email_status", I18nLabel)
            lbl.set_i18n_key("email_verified")
            lbl.remove_class("error", "success", "waiting")
            lbl.add_class("success")

            self.query_one("#submit_btn", I18nButton).disabled = False

            verify_btn = self.query_one("#verify_btn", I18nButton)
            verify_btn.disabled = True
            verify_btn.set_i18n_key("verify_done")

    @on(I18nButton.Pressed, "#submit_btn")
    def on_signup(self) -> None:
        fields = ["username", "name", "password", "password_confirm"]
        if self.email_required:
            fields.insert(0, "email")

        vals = [self.query_one(f"#{i}", ModalInput).value for i in fields]
        if not all(vals):
            self.app.notify(i18n.get("error_fill_all"), severity="error")
            return

        if self.email_required and not self.verified_token:
            self.app.notify(i18n.get("error_verify_required"), severity="error")
            return

        pw = self.query_one("#password", ModalInput).value
        confirm = self.query_one("#password_confirm", ModalInput).value
        if pw != confirm:
            self._set_status(
                "#password_confirm",
                "#password_confirm_status",
                "password_mismatch",
                is_error=True,
            )
            return

        self._do_signup()

    @work(exclusive=True)
    async def _do_signup(self) -> None:
        email = (
            self.query_one("#email", ModalInput).value if self.email_required else ""
        )
        username = self.query_one("#username", ModalInput).value
        name = self.query_one("#name", ModalInput).value
        password = self.query_one("#password", ModalInput).value
        verified_token = self.verified_token or ""

        try:
            await MemberAPI(self.app.client).signup(
                email, username, name, password, verified_token
            )
        except ApiResponseError as e:
            log.warning("Signup failed: %s", e)
            reason = self._signup_error_reason(e)
            self.app.notify(i18n.get("signup_failed", reason=reason), severity="error")
            return
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Signup error: %s", e)
            self.app.notify(i18n.get("signup_failed", reason=str(e)), severity="error")
            return

        identifier = email if self.email_required else username
        try:
            res = await AuthAPI(self.app.client).login(identifier, password)
        except (ApiResponseError, ApiNetworkError, TissueApiError) as e:
            log.warning("Auto login after signup failed: %s", e)
            self.app.notify(i18n.get("signup_success"), timeout=3)
            self._stop_timers()
            self.dismiss(identifier)
            return

        self.config_manager.save_tokens(res.access_token, res.refresh_token)
        self.app.notify(i18n.get("welcome", identifier=identifier), timeout=3)
        self._stop_timers()
        self.dismiss(None)

    def _signup_error_reason(self, e: ApiResponseError) -> str:
        if not e.code:
            return f"HTTP {e.status_code}"
        key = f"api_error.{e.code}"
        translated = i18n.get(key)
        return translated if translated != key else e.code
