import logging

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Input, Label, Static

from tissue.api.errors import TissueApiError
from tissue.i18n.manager import i18n
from tissue.screens.base import TissueModal

log = logging.getLogger(__name__)

_REQUIRED_FIELDS = ("restore_identifier", "restore_password")


class RestoreAccountModal(TissueModal[str | None]):
    CSS_PATH = "restore_account_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def __init__(self, *, email_required: bool, prefill_identifier: str = "") -> None:
        super().__init__()
        self._email_required = email_required
        self._prefill_identifier = prefill_identifier

    def compose(self) -> ComposeResult:
        identifier_label_key = (
            "restore_email_label" if self._email_required else "restore_username_label"
        )
        identifier_placeholder_key = (
            "login_email_placeholder"
            if self._email_required
            else "login_username_placeholder"
        )

        identifier_input = Input(
            value=self._prefill_identifier,
            placeholder=i18n.get(identifier_placeholder_key),
            id="restore_identifier",
            classes="input-field",
        )
        identifier_input.border_title = i18n.get(identifier_label_key)

        password_input = Input(
            password=True,
            placeholder=i18n.get("login_password_placeholder"),
            id="restore_password",
            classes="input-field",
        )
        password_input.border_title = i18n.get("restore_password_label")

        warning = Static(
            i18n.get("restore_warning"),
            classes="warning",
            id="restore_warning",
        )
        buttons = Horizontal(
            Button(i18n.get("restore_cancel_btn"), id="restore_cancel_btn"),
            Button(
                i18n.get("restore_submit_btn"),
                id="restore_submit_btn",
                classes="-btn-success",
            ),
            id="restore-buttons",
        )
        form = Container(
            warning,
            identifier_input,
            Label("", id="restore_identifier_status", classes="status-msg"),
            password_input,
            Label("", id="restore_password_status", classes="status-msg"),
            buttons,
            id="restore-form",
        )
        dialog = Container(form, id="restore-dialog", classes="dialog")
        dialog.border_title = i18n.get("restore_title")
        dialog.border_subtitle = i18n.get("workspace_create_modal_close_hint")
        yield dialog

    def on_mount(self) -> None:
        target_id = (
            "restore_password" if self._prefill_identifier else "restore_identifier"
        )
        self.query_one(f"#{target_id}", Input).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#restore_cancel_btn")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#restore_submit_btn")
    @on(Input.Submitted)
    def _on_submit(self) -> None:
        first_empty = self._check_required_fields()
        if first_empty is not None:
            return

        identifier = self.query_one("#restore_identifier", Input).value.strip()
        password = self.query_one("#restore_password", Input).value
        self._do_restore(identifier=identifier, password=password)

    @work(exclusive=True, group="restore_account")
    async def _do_restore(self, *, identifier: str, password: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.account.restore(identifier, password)
        except TissueApiError as e:
            log.warning("Restore failed: %s", e)
            self.app.notify(
                i18n.get("restore_failed", reason=self._failure_reason(e)),
                severity="error",
            )
            return

        self.app.notify(i18n.get("restore_success"))
        self.dismiss(identifier)

    @staticmethod
    def _failure_reason(exc: TissueApiError) -> str:
        if exc.title == "RESTORE_INVALID_CREDENTIALS":
            return i18n.get("restore_error_invalid_credentials")
        if exc.title == "RESTORE_NOT_DELETED":
            return i18n.get("restore_error_not_deleted")
        return exc.detail or exc.title or str(exc)

    def _check_required_fields(self) -> Input | None:
        first_empty: Input | None = None
        for fid in _REQUIRED_FIELDS:
            inp = self.query_one(f"#{fid}", Input)
            if not inp.value.strip():
                self._set_status(fid, i18n.get("login_validation_required"), "error")
                if first_empty is None:
                    first_empty = inp
        if first_empty is not None:
            first_empty.focus()
        return first_empty

    def _set_status(
        self, input_id: str, message: str = "", kind: str | None = None
    ) -> None:
        label = self.query_one(f"#{input_id}_status", Label)
        label.remove_class("-error", "-waiting", "-success")
        label.update(message if kind is not None else "")
        if kind is not None:
            label.add_class(f"-{kind}")
