"""Modal for editing the current member's name and username."""

import logging
import re

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.timer import Timer
from textual.validation import Length, Regex, ValidationResult
from textual.widgets import Button, Input, Label

from tissue.api.errors import TissueApiError
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueModal

log = logging.getLogger(__name__)

_USERNAME_REGEX = "^[a-z0-9]+$"
_USERNAME_RE = re.compile(_USERNAME_REGEX)
_AVAILABILITY_DEBOUNCE = 0.3


class EditProfileModal(TissueModal[bool | None]):
    """Edit name + username. Dismisses with True on a successful save."""

    CSS_PATH = "edit_profile_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def __init__(self) -> None:
        super().__init__()
        self._initial_name: str = ""
        self._initial_username: str = ""
        self._username_check_timer: Timer | None = None
        # None = unknown, True/False = checked.
        self._username_available: bool | None = None

    def compose(self) -> ComposeResult:
        profile = self._cached_profile()
        self._initial_name = (profile.name if profile and profile.name else "") or ""
        self._initial_username = (
            profile.username if profile and profile.username else ""
        ) or ""

        name_input = Input(
            value=self._initial_name,
            id="edit_profile_name",
            classes="input-field",
            validators=[
                Length(
                    minimum=2,
                    maximum=35,
                    failure_description=i18n.get("signup_name_length"),
                ),
            ],
            validate_on=["changed"],
        )
        name_input.border_title = i18n.get("home_account_label_name")

        username_input = Input(
            value=self._initial_username,
            id="edit_profile_username",
            classes="input-field",
            validators=[
                Length(
                    minimum=3,
                    maximum=22,
                    failure_description=i18n.get("signup_username_length"),
                ),
                Regex(
                    _USERNAME_REGEX,
                    failure_description=i18n.get("signup_username_invalid"),
                ),
            ],
            validate_on=["changed"],
        )
        username_input.border_title = i18n.get("home_account_label_username")

        form = Container(
            name_input,
            Label("", id="edit_profile_name_status", classes="status-msg"),
            username_input,
            Label("", id="edit_profile_username_status", classes="status-msg"),
            Button(
                i18n.get("home_account_edit_save_btn"),
                id="edit_profile_save_btn",
                classes="-btn-success",
            ),
            id="edit-profile-form",
        )
        dialog = Container(
            form,
            id="edit-profile-dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("home_account_edit_title")
        dialog.border_subtitle = i18n.get("workspace_create_modal_close_hint")
        yield dialog

    def on_mount(self) -> None:
        self.query_one("#edit_profile_name", Input).focus()

    def on_unmount(self) -> None:
        self._stop_username_check_timer()

    def action_close(self) -> None:
        self._stop_username_check_timer()
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
        # Same as initial → no API call needed.
        if new_value == self._initial_username:
            self._username_available = None
            self._restart_username_check_timer(schedule=False)
            return
        self._username_available = None
        self._restart_username_check_timer(schedule=self._format_valid(event))

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
                i18n.get("signup_username_check_failed"),
                "error",
            )
            return
        # Stale-response guard.
        if inp.value.strip() != value:
            return
        self._username_available = available
        if available:
            self._set_status(
                "edit_profile_username",
                i18n.get("signup_username_available"),
                "success",
            )
        else:
            self._set_status(
                "edit_profile_username",
                i18n.get("signup_username_taken"),
                "error",
            )

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

        name_changed = name != self._initial_name
        username_changed = username != self._initial_username

        if not name_changed and not username_changed:
            self.app.notify(i18n.get("home_account_edit_no_changes"))
            return

        if name_changed and not (2 <= len(name) <= 35):
            self._set_status(
                "edit_profile_name", i18n.get("signup_name_length"), "error"
            )
            return
        if username_changed and not (
            3 <= len(username) <= 22 and _USERNAME_RE.fullmatch(username)
        ):
            self._set_status(
                "edit_profile_username", i18n.get("signup_username_length"), "error"
            )
            return
        if username_changed and self._username_available is False:
            return

        try:
            if name_changed:
                await client.account.update_name(name)
                self._initial_name = name
            if username_changed:
                await client.account.update_username(username)
                self._initial_username = username
        except TissueApiError as e:
            log.warning("Profile update failed: %s", e)
            reason = e.detail or e.title or str(e)
            self.app.notify(
                i18n.get("home_account_edit_failed", reason=reason),
                severity="error",
            )
            return

        self.app.notify(i18n.get("home_account_edit_success"))
        self._stop_username_check_timer()
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
