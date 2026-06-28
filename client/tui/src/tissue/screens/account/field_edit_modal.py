import logging
import re

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.timer import Timer
from textual.validation import Length, Regex, ValidationResult, Validator
from textual.widgets import Button, Input, Label

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.screens.form_helpers import set_status_label
from tissue.widgets.spinner import Spinner

log = logging.getLogger(__name__)

_NAME_MIN = 2
_NAME_MAX = 35
_USERNAME_MIN = 3
_USERNAME_MAX = 22

_USERNAME_RE = re.compile("^[a-z0-9]+$")
_EMAIL_RE = re.compile(r"[^@]+@[^@]+\.[^@]+")

_AVAILABILITY_DEBOUNCE = 0.3
_POLL_INTERVAL = 2.0

# New value must be unique server-side, checked debounced as the user types.
_CHECKS_AVAILABILITY = ("username", "email")
# New value needs an emailed verification token before it can be saved.
_NEEDS_VERIFICATION = ("email",)


def _validators_for(field: str) -> list[Validator]:
    if field == "name":
        return [
            Length(_NAME_MIN, _NAME_MAX, failure_description="Must be 2-35 characters")
        ]
    if field == "username":
        return [
            Length(
                _USERNAME_MIN,
                _USERNAME_MAX,
                failure_description="Must be 3-22 characters",
            ),
            Regex(
                _USERNAME_RE.pattern,
                failure_description="Only lowercase letters and digits",
            ),
        ]
    return [Regex(_EMAIL_RE.pattern, failure_description="Invalid email format")]


class FieldEditModal(TissueModal[bool | None]):
    """Edit a single account field (name, username, or email).

    Opened from an AccountModal pencil icon. `username` and `email` run a
    debounced availability check, and `email` additionally requires the user to
    verify the new address before it can be saved.

    Dismisses `True` once the value is updated, in which case the account
    service refreshes its cached profile, else `None`.
    """

    CSS_PATH = "field_edit_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def __init__(self, *, field: str, current_value: str) -> None:
        super().__init__()
        self._field = field
        self._current_value = current_value

        self._check_timer: Timer | None = None
        # None not checked yet, True available, False already taken.
        self._available: bool | None = None

        self._verification_id: str | None = None
        self._verified_token: str | None = None
        self._poll_timer: Timer | None = None
        self._spinner: Spinner | None = None

    def compose(self) -> ComposeResult:
        field = self._field
        value_input = Input(
            value=self._current_value,
            id="field_edit_input",
            classes="input-field",
            validators=_validators_for(field),
            validate_on=["changed"],
        )
        value_input.border_title = field.capitalize()

        body: list = []
        if field == "email":
            verify_btn = Button("Verify", id="field_edit_verify_btn", disabled=True)
            body.append(Horizontal(value_input, verify_btn, classes="email-row"))
        else:
            body.append(value_input)
        body.append(Label("", id="field_edit_status", classes="status-msg"))
        body.append(
            Horizontal(
                Button("Cancel", id="field_edit_cancel_btn", classes="-btn-error"),
                Button("Save", id="field_edit_save_btn", classes="-btn-success"),
                id="field-edit-buttons",
            )
        )

        dialog = Container(
            Container(*body, id="field-edit-form"),
            id="field-edit-dialog",
            classes="dialog",
        )
        dialog.border_title = f"Edit {field}"
        dialog.border_subtitle = "Esc to cancel"
        yield dialog

    def on_mount(self) -> None:
        if self._field == "email":
            self._spinner = Spinner(self, self.query_one("#field_edit_status", Label))
        self.query_one("#field_edit_input", Input).focus()

    def on_unmount(self) -> None:
        self._stop_all_timers()

    def action_close(self) -> None:
        self._stop_all_timers()
        self.dismiss(None)

    @on(Input.Changed, "#field_edit_input")
    def _on_changed(self, event: Input.Changed) -> None:
        self._render_status(event.value, event.validation_result)
        if self._field not in _CHECKS_AVAILABILITY:
            return

        self._available = None
        if self._field == "email":
            self._reset_verification()

        value = event.value.strip()
        if value == self._current_value:
            self._restart_check_timer(schedule=False)
            return
        self._restart_check_timer(schedule=_format_valid(event))

    def _reset_verification(self) -> None:
        """Drop any pending verification.

        The email changed, so the old token is stale.
        """
        self._stop_poll()
        self._verification_id = None
        self._verified_token = None
        verify_btn = self.query_one("#field_edit_verify_btn", Button)
        verify_btn.label = "Verify"
        verify_btn.disabled = True

    def _restart_check_timer(self, *, schedule: bool) -> None:
        """Push the debounce timer forward so the worker fires once typing pauses."""
        if self._check_timer is not None:
            self._check_timer.stop()
            self._check_timer = None
        if schedule:
            self._check_timer = self.set_timer(_AVAILABILITY_DEBOUNCE, self._do_check)

    @work(exclusive=True, group="field_edit_check")
    async def _do_check(self) -> None:
        client = self.app.client
        if client is None:
            return
        field_input = self.query_one("#field_edit_input", Input)
        value = field_input.value.strip()
        if not value or value == self._current_value:
            return

        check_fn = (
            client.account.check_email_available
            if self._field == "email"
            else client.account.check_username_available
        )
        try:
            available = await check_fn(value)
        except TissueApiError as error:
            if field_input.value.strip() != value:  # input moved on while awaiting
                return
            log.warning("%s availability check failed: %s", self._field, error)
            self._set_status(f"Unable to check {self._field} availability", "error")
            return
        if not self.is_mounted or field_input.value.strip() != value:
            return

        self._available = available
        if available:
            self._set_status("Available", "success")
            if self._field == "email":
                self.query_one("#field_edit_verify_btn", Button).disabled = False
        else:
            self._set_status(f"{self._field.capitalize()} already in use", "error")
            if self._field == "email":
                self.query_one("#field_edit_verify_btn", Button).disabled = True

    @on(Button.Pressed, "#field_edit_verify_btn")
    def _on_verify_pressed(self) -> None:
        email = self.query_one("#field_edit_input", Input).value.strip()
        if email and email != self._current_value:
            self._do_request_verification(email)

    @work(exclusive=True, group="field_edit_verify_request")
    async def _do_request_verification(self, email: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            verification_id = await client.account.request_signup_verification(email)
        except TissueApiError as error:
            log.warning("Email verification request failed: %s", error)
            self._set_status("Failed to send verification email", "error")
            return
        if not self.is_mounted:
            return

        self._verification_id = verification_id
        self.app.notify("Verification email sent. Check your inbox.", timeout=3)

        verify_btn = self.query_one("#field_edit_verify_btn", Button)
        verify_btn.label = "Sent"
        verify_btn.disabled = True

        label = self.query_one("#field_edit_status", Label)
        label.remove_class("-error", "-success")
        label.add_class("-waiting")
        if self._spinner is not None:
            self._spinner.start("Waiting for verification email")

        self._poll_timer = self.set_interval(_POLL_INTERVAL, self._poll_verification)

    @work(group="field_edit_verify_poll")
    async def _poll_verification(self) -> None:
        verification_id = self._verification_id
        client = self.app.client
        if verification_id is None or client is None:
            return
        try:
            status = await client.account.check_signup_verification(verification_id)
        except TissueApiError as error:
            log.warning("Verification status check failed: %s", error)
            return
        if status.status != "VERIFIED" or not status.verified_token:
            return

        self._verified_token = status.verified_token
        self._stop_poll()
        if not self.is_mounted:
            return

        verify_btn = self.query_one("#field_edit_verify_btn", Button)
        verify_btn.label = "✓"
        verify_btn.disabled = True
        self._set_status("✓", "success")

    @on(Button.Pressed, "#field_edit_cancel_btn")
    def _on_cancel(self) -> None:
        self.action_close()

    @on(Button.Pressed, "#field_edit_save_btn")
    @on(Input.Submitted, "#field_edit_input")
    def _on_save(self) -> None:
        self._do_save()

    @work(exclusive=True, group="field_edit_save")
    async def _do_save(self) -> None:
        client = self.app.client
        if client is None:
            return
        value = self.query_one("#field_edit_input", Input).value.strip()
        if value == self._current_value:
            self.app.notify("No changes to save")
            return
        if not self._is_valid(value):
            return

        try:
            await self._apply(client, value)
        except TissueApiError as error:
            log.warning("Profile update failed: %s", error)
            reason = error.detail or error.title or str(error)
            self.app.notify(f"Update failed: {reason}", severity="error")
            return

        self.app.notify(f"{self._field.capitalize()} updated")
        self._stop_all_timers()
        self.dismiss(True)

    def _is_valid(self, value: str) -> bool:
        """Re-check format and uniqueness/verification at save time."""
        field = self._field
        if field == "name" and not (_NAME_MIN <= len(value) <= _NAME_MAX):
            self._set_status("Must be 2-35 characters", "error")
            return False
        if field == "username":
            if not (
                _USERNAME_MIN <= len(value) <= _USERNAME_MAX
                and _USERNAME_RE.fullmatch(value)
            ):
                self._set_status("Must be 3-22 characters", "error")
                return False
            if self._available is False:
                self._set_status("Username already in use", "error")
                return False
        if field == "email":
            if not _EMAIL_RE.fullmatch(value):
                self._set_status("Invalid email format", "error")
                return False
            if self._available is False:
                self._set_status("Email already in use", "error")
                return False
            if not self._verified_token:
                self._set_status("Email verification required", "error")
                return False
        return True

    async def _apply(self, client, value: str) -> None:
        if self._field == "name":
            await client.account.update_name(value)
        elif self._field == "username":
            await client.account.update_username(value)
        else:
            assert self._verified_token is not None
            await client.account.update_email(
                new_email=value, verification_token=self._verified_token
            )

    def _render_status(self, value: str, result: ValidationResult | None) -> None:
        if not value or result is None or result.is_valid:
            self._set_status()
            return
        failure_messages = result.failure_descriptions
        self._set_status(failure_messages[0] if failure_messages else "", "error")

    def _set_status(self, message: str = "", kind: str | None = None) -> None:
        set_status_label(self, "field_edit_status", message, kind)

    def _stop_poll(self) -> None:
        if self._poll_timer is not None:
            self._poll_timer.stop()
            self._poll_timer = None
        if self._spinner is not None:
            self._spinner.stop()

    def _stop_all_timers(self) -> None:
        if self._check_timer is not None:
            self._check_timer.stop()
            self._check_timer = None
        self._stop_poll()


def _format_valid(event: Input.Changed) -> bool:
    return bool(
        event.value.strip()
        and event.validation_result is not None
        and event.validation_result.is_valid
    )
