"""Modal for changing the current member's password."""

import logging
import re

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.validation import Length, Regex, ValidationResult, Validator
from textual.widgets import Button, Input, Label

from tissue.api.errors import TissueApiError
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueModal

log = logging.getLogger(__name__)

# Mirrors backend regex on UpdateMemberPasswordRequest.newPassword
_PASSWORD_REGEX = r"^(?=.*[A-Za-z])(?=.*\d).{8,30}$"

_REQUIRED_FIELDS = (
    "change_password_current",
    "change_password_new",
    "change_password_confirm",
)


class ChangePasswordModal(TissueModal[bool | None]):
    """Current password + new password + confirm."""

    CSS_PATH = "change_password_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def compose(self) -> ComposeResult:
        current = Input(
            password=True,
            placeholder=i18n.get("login_password_placeholder"),
            id="change_password_current",
            classes="input-field",
            validators=[
                Length(
                    minimum=1,
                    failure_description=i18n.get("login_validation_required"),
                ),
            ],
            validate_on=["changed"],
        )
        current.border_title = i18n.get("change_password_current_label")

        new_password = Input(
            password=True,
            placeholder=i18n.get("login_password_placeholder"),
            id="change_password_new",
            classes="input-field",
            validators=[
                Length(
                    minimum=8,
                    maximum=30,
                    failure_description=i18n.get("signup_password_length"),
                ),
                Regex(
                    _PASSWORD_REGEX,
                    failure_description=i18n.get("signup_password_weak"),
                ),
            ],
            validate_on=["changed"],
        )
        new_password.border_title = i18n.get("change_password_new_label")

        confirm = Input(
            password=True,
            placeholder=i18n.get("login_password_placeholder"),
            id="change_password_confirm",
            classes="input-field",
            validators=[
                _MatchValidator(
                    self,
                    failure_description=i18n.get("signup_password_mismatch"),
                ),
            ],
            validate_on=["changed"],
        )
        confirm.border_title = i18n.get("change_password_confirm_label")

        form = Container(
            current,
            Label("", id="change_password_current_status", classes="status-msg"),
            new_password,
            Label("", id="change_password_new_status", classes="status-msg"),
            confirm,
            Label("", id="change_password_confirm_status", classes="status-msg"),
            Button(
                i18n.get("change_password_submit_btn"),
                id="change_password_submit_btn",
                classes="-btn-success",
            ),
            id="change-password-form",
        )
        dialog = Container(
            form,
            id="change-password-dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("change_password_title")
        dialog.border_subtitle = i18n.get("workspace_create_modal_close_hint")
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
        # If the new password changes, the confirm field needs re-validation.
        if input_id == "change_password_new":
            confirm = self.query_one("#change_password_confirm", Input)
            confirm.validate(confirm.value)
        self._render_status(input_id, event.value, event.validation_result)

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
            self._set_status(
                "change_password_new",
                i18n.get("signup_password_weak"),
                "error",
            )
            return

        if new_password != confirm:
            self._set_status(
                "change_password_confirm",
                i18n.get("signup_password_mismatch"),
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
        except TissueApiError as e:
            log.warning("Password change failed: %s", e)
            reason = self._failure_reason(e)
            self.app.notify(
                i18n.get("change_password_failed", reason=reason),
                severity="error",
            )
            return

        self.app.notify(i18n.get("change_password_success"))
        self.dismiss(True)

    @staticmethod
    def _failure_reason(exc: TissueApiError) -> str:
        if exc.title in ("INVALID_PASSWORD", "PASSWORD_MISMATCH"):
            return i18n.get("change_password_error_invalid_current")
        return exc.detail or exc.title or str(exc)

    def _check_required_fields(self) -> Input | None:
        first_empty: Input | None = None
        for fid in _REQUIRED_FIELDS:
            inp = self.query_one(f"#{fid}", Input)
            if not inp.value:
                self._set_status(fid, i18n.get("login_validation_required"), "error")
                if first_empty is None:
                    first_empty = inp
        if first_empty is not None:
            first_empty.focus()
        return first_empty

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


class _MatchValidator(Validator):
    """Confirm field must match `#change_password_new` at validate time."""

    def __init__(self, modal: ChangePasswordModal, failure_description: str) -> None:
        super().__init__(failure_description=failure_description)
        self._modal = modal

    def validate(self, value: str) -> ValidationResult:
        new_password = self._modal.query_one("#change_password_new", Input).value
        if value == new_password:
            return self.success()
        return self.failure(self.failure_description or "")
