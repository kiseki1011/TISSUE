import logging
import re

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.validation import Length, Regex, ValidationResult, Validator
from textual.widgets import Button, Input, Label

from tissue.api.errors import TissueApiError
from tissue.screens.account._helpers import failure_reason, set_field_status
from tissue.screens.base import TissueModal
from tissue.screens.form_helpers import (
    first_empty_required_field,
    render_validation_status,
)

log = logging.getLogger(__name__)

_PASSWORD_REGEX = r"^(?=.*[A-Za-z])(?=.*\d).{8,30}$"

_REQUIRED_FIELDS = (
    "change_password_current",
    "change_password_new",
    "change_password_confirm",
)


class ChangePasswordModal(TissueModal[bool | None]):
    """Modal for changing the current member's password.

    Dismisses with `True` after a successful change, `None` on cancel.
    """

    CSS_PATH = "change_password_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def compose(self) -> ComposeResult:
        current = Input(
            password=True,
            placeholder="********",
            id="change_password_current",
            classes="input-field",
            validators=[
                Length(
                    minimum=1,
                    failure_description="Required field",
                ),
            ],
            validate_on=["changed"],
        )
        current.border_title = "Current password"

        new_password = Input(
            password=True,
            placeholder="********",
            id="change_password_new",
            classes="input-field",
            validators=[
                Length(
                    minimum=8,
                    maximum=30,
                    failure_description="Must be 8-30 characters",
                ),
                Regex(
                    _PASSWORD_REGEX,
                    failure_description="Must include letter and digit",
                ),
            ],
            validate_on=["changed"],
        )
        new_password.border_title = "New password"

        confirm = Input(
            password=True,
            placeholder="********",
            id="change_password_confirm",
            classes="input-field",
            validators=[
                _MatchValidator(
                    self,
                    failure_description="Passwords do not match",
                ),
            ],
            validate_on=["changed"],
        )
        confirm.border_title = "Confirm new password"

        buttons = Horizontal(
            Button(
                "Cancel",
                id="change_password_cancel_btn",
                classes="-btn-error",
            ),
            Button(
                "Change password",
                id="change_password_submit_btn",
                classes="-btn-success",
            ),
            id="change-password-buttons",
        )
        form = Container(
            current,
            Label("", id="change_password_current_status", classes="status-msg"),
            new_password,
            Label("", id="change_password_new_status", classes="status-msg"),
            confirm,
            Label("", id="change_password_confirm_status", classes="status-msg"),
            buttons,
            id="change-password-form",
        )
        dialog = Container(
            form,
            id="change-password-dialog",
            classes="dialog",
        )
        dialog.border_title = "Change password"
        dialog.border_subtitle = "Esc to cancel"
        yield dialog

    def on_mount(self) -> None:
        self.query_one("#change_password_current", Input).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Input.Changed)
    def _on_input_changed(self, event: Input.Changed) -> None:
        input_id = event.input.id
        if input_id is None:
            return
        if input_id == "change_password_new":
            confirm = self.query_one("#change_password_confirm", Input)
            confirm.validate(confirm.value)
        self._render_status(input_id, event.value, event.validation_result)

    @on(Button.Pressed, "#change_password_cancel_btn")
    def _on_cancel_pressed(self) -> None:
        self.action_close()

    @on(Button.Pressed, "#change_password_submit_btn")
    @on(Input.Submitted)
    def _on_submit(self) -> None:
        if self._check_required_fields() is not None:
            return

        current = self.query_one("#change_password_current", Input).value
        new_password = self.query_one("#change_password_new", Input).value
        confirm = self.query_one("#change_password_confirm", Input).value

        if not (
            8 <= len(new_password) <= 30 and re.fullmatch(_PASSWORD_REGEX, new_password)
        ):
            set_field_status(
                self,
                "change_password_new",
                "Must include letter and digit",
                "error",
            )
            return

        if new_password != confirm:
            set_field_status(
                self,
                "change_password_confirm",
                "Passwords do not match",
                "error",
            )
            return

        self._do_change(current_password=current, new_password=new_password)

    @work(exclusive=True, group="change_password")
    async def _do_change(self, *, current_password: str, new_password: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.account.update_password(
                original_password=current_password, new_password=new_password
            )
        except TissueApiError as error:
            log.warning("Password change failed: %s", error)
            reason = failure_reason(
                error, password_message="Current password is incorrect"
            )
            self.app.notify(
                f"Failed to change password: {reason}",
                severity="error",
            )
            return

        self.app.notify("Password changed")
        self.dismiss(True)

    def _check_required_fields(self) -> Input | None:
        return first_empty_required_field(self, _REQUIRED_FIELDS)

    def _render_status(
        self,
        input_id: str,
        value: str,
        result: ValidationResult | None,
    ) -> None:
        render_validation_status(self, input_id, value, result)


class _MatchValidator(Validator):
    """Confirm field must match the current value of `#change_password_new`."""

    def __init__(self, modal: ChangePasswordModal, failure_description: str) -> None:
        super().__init__(failure_description=failure_description)
        self._modal = modal

    def validate(self, value: str) -> ValidationResult:
        new_password = self._modal.query_one("#change_password_new", Input).value
        if value == new_password:
            return self.success()
        return self.failure(self.failure_description or "")
