"""Modal for editing the current member's name, username, and email."""

import logging
import re

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.timer import Timer
from textual.validation import Length, Regex, ValidationResult
from textual.widgets import Button, Input, Label

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.widgets.spinner import Spinner

log = logging.getLogger(__name__)

_EMAIL_REGEX = r"[^@]+@[^@]+\.[^@]+"
_USERNAME_REGEX = "^[a-z0-9]+$"
_USERNAME_RE = re.compile(_USERNAME_REGEX)
_AVAILABILITY_DEBOUNCE = 0.3
_POLL_INTERVAL = 2.0


class EditProfileModal(TissueModal[bool | None]):
    CSS_PATH = "edit_profile_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def __init__(self, *, email_required: bool = False) -> None:
        super().__init__()
        self._email_required = email_required

        self._initial_name: str = ""
        self._initial_username: str = ""
        self._initial_email: str = ""

        self._username_check_timer: Timer | None = None
        self._username_available: bool | None = None

        self._email_check_timer: Timer | None = None
        self._email_available: bool | None = None
        self._verification_id: str | None = None
        self._verified_token: str | None = None
        self._poll_timer: Timer | None = None
        self._email_spinner: Spinner | None = None

    def compose(self) -> ComposeResult:
        profile = self._cached_profile()
        self._initial_name = (profile.name if profile and profile.name else "") or ""
        self._initial_username = (
            profile.username if profile and profile.username else ""
        ) or ""
        self._initial_email = (profile.email if profile and profile.email else "") or ""

        oidc = self._is_oidc_mode()

        form_children: list = []

        if self._email_required:
            email_input = Input(
                value=self._initial_email,
                id="edit_profile_email",
                classes="input-field",
                disabled=oidc,
                validators=[
                    Regex(
                        _EMAIL_REGEX,
                        failure_description="Invalid email format",
                    ),
                ],
                validate_on=["changed"],
            )
            email_input.border_title = "Email"

            verify_btn = Button(
                "Verify",
                id="edit_profile_verify_btn",
                disabled=True,
            )
            form_children.extend(
                [
                    Horizontal(email_input, verify_btn, classes="email-row"),
                    Label("", id="edit_profile_email_status", classes="status-msg"),
                ]
            )

        name_input = Input(
            value=self._initial_name,
            id="edit_profile_name",
            classes="input-field",
            disabled=oidc,
            validators=[
                Length(
                    minimum=2,
                    maximum=35,
                    failure_description="Must be 2-35 characters",
                ),
            ],
            validate_on=["changed"],
        )
        name_input.border_title = "Name"

        username_input = Input(
            value=self._initial_username,
            id="edit_profile_username",
            classes="input-field",
            validators=[
                Length(
                    minimum=3,
                    maximum=22,
                    failure_description="Must be 3-22 characters",
                ),
                Regex(
                    _USERNAME_REGEX,
                    failure_description="Only lowercase letters and digits",
                ),
            ],
            validate_on=["changed"],
        )
        username_input.border_title = "Username"

        buttons = Horizontal(
            Button(
                "Cancel",
                id="edit_profile_cancel_btn",
            ),
            Button(
                "Save",
                id="edit_profile_save_btn",
                classes="-btn-success",
            ),
            id="edit-profile-buttons",
        )
        form_children.extend(
            [
                name_input,
                Label("", id="edit_profile_name_status", classes="status-msg"),
                username_input,
                Label("", id="edit_profile_username_status", classes="status-msg"),
                buttons,
            ]
        )

        form = Container(*form_children, id="edit-profile-form")
        dialog = Container(
            form,
            id="edit-profile-dialog",
            classes="dialog",
        )
        dialog.border_title = "Edit profile"
        dialog.border_subtitle = "Esc to cancel"
        yield dialog

    def on_mount(self) -> None:
        if self._email_required:
            self._email_spinner = Spinner(
                self, self.query_one("#edit_profile_email_status", Label)
            )
        if self._is_oidc_mode():
            focus_id = "#edit_profile_username"
        elif self._email_required:
            focus_id = "#edit_profile_email"
        else:
            focus_id = "#edit_profile_name"
        self.query_one(focus_id, Input).focus()

    def on_unmount(self) -> None:
        self._stop_username_check_timer()
        self._stop_email_check_timer()
        self._stop_poll()

    def action_close(self) -> None:
        self._stop_username_check_timer()
        self._stop_email_check_timer()
        self._stop_poll()
        self.dismiss(None)

    @on(Input.Changed, "#edit_profile_name")
    def _on_name_changed(self, event: Input.Changed) -> None:
        self._render_status("edit_profile_name", event.value, event.validation_result)

    @on(Input.Changed, "#edit_profile_username")
    def _on_username_changed(self, event: Input.Changed) -> None:
        self._render_status(
            "edit_profile_username", event.value, event.validation_result
        )
        new_value = event.value.strip()
        if new_value == self._initial_username:
            self._username_available = None
            self._restart_username_check_timer(schedule=False)
            return
        self._username_available = None
        self._restart_username_check_timer(schedule=self._format_valid(event))

    @on(Input.Changed, "#edit_profile_email")
    def _on_email_changed(self, event: Input.Changed) -> None:
        self._render_status("edit_profile_email", event.value, event.validation_result)
        new_value = event.value.strip()

        self._stop_poll()
        self._verification_id = None
        self._verified_token = None
        self._email_available = None

        verify_btn = self.query_one("#edit_profile_verify_btn", Button)
        verify_btn.label = "Verify"
        verify_btn.disabled = True

        if new_value == self._initial_email:
            self._restart_email_check_timer(schedule=False)
            return
        self._restart_email_check_timer(schedule=self._format_valid(event))

    @staticmethod
    def _format_valid(event: Input.Changed) -> bool:
        return bool(
            event.value.strip()
            and event.validation_result is not None
            and event.validation_result.is_valid
        )

    def _restart_username_check_timer(self, *, schedule: bool) -> None:
        if self._username_check_timer is not None:
            self._username_check_timer.stop()
            self._username_check_timer = None
        if not schedule:
            return
        self._username_check_timer = self.set_timer(
            _AVAILABILITY_DEBOUNCE, self._do_check_username
        )

    def _stop_username_check_timer(self) -> None:
        if self._username_check_timer is not None:
            self._username_check_timer.stop()
            self._username_check_timer = None

    def _restart_email_check_timer(self, *, schedule: bool) -> None:
        if self._email_check_timer is not None:
            self._email_check_timer.stop()
            self._email_check_timer = None
        if not schedule:
            return
        self._email_check_timer = self.set_timer(
            _AVAILABILITY_DEBOUNCE, self._do_check_email
        )

    def _stop_email_check_timer(self) -> None:
        if self._email_check_timer is not None:
            self._email_check_timer.stop()
            self._email_check_timer = None

    @work(exclusive=True, group="edit_profile_username_check")
    async def _do_check_username(self) -> None:
        client = self.app.client
        if client is None:
            return
        inp = self.query_one("#edit_profile_username", Input)
        value = inp.value.strip()
        if not value or value == self._initial_username:
            return
        try:
            available = await client.account.check_username_available(value)
        except TissueApiError as e:
            log.warning("Username availability check failed: %s", e)
            self._set_status(
                "edit_profile_username",
                "Unable to check username availability",
                "error",
            )
            return
        if inp.value.strip() != value:
            return
        self._username_available = available
        if available:
            self._set_status(
                "edit_profile_username",
                "Available",
                "success",
            )
        else:
            self._set_status(
                "edit_profile_username",
                "Username already in use",
                "error",
            )

    @work(exclusive=True, group="edit_profile_email_check")
    async def _do_check_email(self) -> None:
        client = self.app.client
        if client is None:
            return
        inp = self.query_one("#edit_profile_email", Input)
        value = inp.value.strip()
        if not value or value == self._initial_email:
            return
        try:
            available = await client.account.check_email_available(value)
        except TissueApiError as e:
            log.warning("Email availability check failed: %s", e)
            self._set_status(
                "edit_profile_email",
                "Unable to check email availability",
                "error",
            )
            return
        if inp.value.strip() != value:
            return
        self._email_available = available
        verify_btn = self.query_one("#edit_profile_verify_btn", Button)
        if available:
            self._set_status(
                "edit_profile_email",
                "Available",
                "success",
            )
            verify_btn.disabled = False
        else:
            self._set_status(
                "edit_profile_email",
                "Email already in use",
                "error",
            )
            verify_btn.disabled = True

    @on(Button.Pressed, "#edit_profile_verify_btn")
    def _on_verify_pressed(self) -> None:
        email = self.query_one("#edit_profile_email", Input).value.strip()
        if not email or email == self._initial_email:
            return
        self._do_request_verification(email)

    @work(exclusive=True, group="edit_profile_verify_request")
    async def _do_request_verification(self, email: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            verification_id = await client.account.request_signup_verification(email)
        except TissueApiError as e:
            log.warning("Email verification request failed: %s", e)
            self._set_status(
                "edit_profile_email",
                "Failed to send verification email",
                "error",
            )
            return

        self._verification_id = verification_id
        self.app.notify("Verification email sent. Check your inbox.", timeout=3)

        verify_btn = self.query_one("#edit_profile_verify_btn", Button)
        verify_btn.label = "Sent"
        verify_btn.disabled = True

        label = self.query_one("#edit_profile_email_status", Label)
        label.remove_class("-error", "-success")
        label.add_class("-waiting")
        if self._email_spinner is not None:
            self._email_spinner.start("Waiting for verification email")

        self._poll_timer = self.set_interval(_POLL_INTERVAL, self._poll_verification)

    @work(group="edit_profile_verify_poll")
    async def _poll_verification(self) -> None:
        verification_id = self._verification_id
        client = self.app.client
        if verification_id is None or client is None:
            return
        try:
            status = await client.account.check_signup_verification(verification_id)
        except TissueApiError as e:
            log.warning("Verification status check failed: %s", e)
            return
        if status.status != "VERIFIED" or not status.verified_token:
            return

        self._verified_token = status.verified_token
        self._stop_poll()

        verify_btn = self.query_one("#edit_profile_verify_btn", Button)
        verify_btn.label = "✓"
        verify_btn.disabled = True

        self._set_status("edit_profile_email", "✓", "success")

    def _stop_poll(self) -> None:
        if self._poll_timer is not None:
            self._poll_timer.stop()
            self._poll_timer = None
        if self._email_spinner is not None:
            self._email_spinner.stop()

    @on(Button.Pressed, "#edit_profile_cancel_btn")
    def _on_cancel_pressed(self) -> None:
        self.action_close()

    @on(Button.Pressed, "#edit_profile_save_btn")
    def _on_save_pressed(self) -> None:
        self._do_save()

    @work(exclusive=True, group="edit_profile_save")
    async def _do_save(self) -> None:
        client = self.app.client
        if client is None:
            return

        name = self.query_one("#edit_profile_name", Input).value.strip()
        username = self.query_one("#edit_profile_username", Input).value.strip()
        email = (
            self.query_one("#edit_profile_email", Input).value.strip()
            if self._email_required
            else ""
        )

        name_changed = name != self._initial_name
        username_changed = username != self._initial_username
        email_changed = self._email_required and email != self._initial_email

        if not name_changed and not username_changed and not email_changed:
            self.app.notify("No changes to save")
            return

        if name_changed and not (2 <= len(name) <= 35):
            self._set_status("edit_profile_name", "Must be 2-35 characters", "error")
            return
        if username_changed and not (
            3 <= len(username) <= 22 and _USERNAME_RE.fullmatch(username)
        ):
            self._set_status(
                "edit_profile_username", "Must be 3-22 characters", "error"
            )
            return
        if username_changed and self._username_available is False:
            return
        if email_changed:
            if self._email_available is False:
                self._set_status("edit_profile_email", "Email already in use", "error")
                return
            if not self._verified_token:
                self._set_status(
                    "edit_profile_email",
                    "Email verification required",
                    "error",
                )
                return

        try:
            if name_changed:
                await client.account.update_name(name)
                self._initial_name = name
            if username_changed:
                await client.account.update_username(username)
                self._initial_username = username
            if email_changed:
                assert self._verified_token is not None
                await client.account.update_email(
                    new_email=email, verification_token=self._verified_token
                )
                self._initial_email = email
        except TissueApiError as e:
            log.warning("Profile update failed: %s", e)
            reason = e.detail or e.title or str(e)
            self.app.notify(
                f"Update failed: {reason}",
                severity="error",
            )
            return

        self.app.notify("Profile updated")
        self._stop_username_check_timer()
        self._stop_email_check_timer()
        self._stop_poll()
        self.dismiss(True)

    def _render_status(
        self,
        input_id: str,
        value: str,
        result: ValidationResult | None,
    ) -> None:
        if not value or result is None or result.is_valid:
            self._set_status(input_id)
            return
        msgs = result.failure_descriptions
        self._set_status(input_id, msgs[0] if msgs else "", "error")

    def _set_status(
        self, input_id: str, message: str = "", kind: str | None = None
    ) -> None:
        label = self.query_one(f"#{input_id}_status", Label)
        label.remove_class("-error", "-waiting", "-success")
        label.update(message if kind is not None else "")
        if kind is not None:
            label.add_class(f"-{kind}")

    def _cached_profile(self):
        client = self.app.client
        return client.account.cached_profile if client is not None else None

    def _is_oidc_mode(self) -> bool:
        info = self.app.system_info
        setup = info.setup if info is not None else None
        return bool(setup and (setup.auth_mode or "").upper() == "OIDC")
