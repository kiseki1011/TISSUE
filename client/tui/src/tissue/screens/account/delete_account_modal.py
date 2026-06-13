"""Modal for permanently withdrawing the current member."""

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


class DeleteAccountModal(TissueModal[bool | None]):
    """Confirmation before deleting (withdrawing) the account."""

    CSS_PATH = "delete_account_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def compose(self) -> ComposeResult:
        warning = Static(
            self._warning_text(),
            classes="warning",
            id="delete_account_warning",
        )
        buttons = Horizontal(
            Button(
                i18n.get("delete_account_cancel_btn"),
                id="delete_account_cancel_btn",
            ),
            Button(
                i18n.get("delete_account_confirm_btn"),
                id="delete_account_confirm_btn",
                classes="-btn-error",
            ),
            id="delete-account-buttons",
        )

        form_children: list = [warning]

        if not self._is_oidc_mode():
            password_input = Input(
                password=True,
                placeholder=i18n.get("login_password_placeholder"),
                id="delete_account_password",
                classes="input-field",
            )
            password_input.border_title = i18n.get("delete_account_password_label")
            form_children.extend(
                [
                    password_input,
                    Label(
                        "",
                        id="delete_account_password_status",
                        classes="status-msg",
                    ),
                ]
            )
        form_children.append(buttons)

        form = Container(*form_children, id="delete-account-form")
        dialog = Container(
            form,
            id="delete-account-dialog",
            classes="dialog",
        )
        dialog.border_title = i18n.get("delete_account_title")
        dialog.border_subtitle = i18n.get("workspace_create_modal_close_hint")
        yield dialog

    def on_mount(self) -> None:
        if self._is_oidc_mode():
            self.query_one("#delete_account_confirm_btn", Button).focus()
        else:
            self.query_one("#delete_account_password", Input).focus()

    def action_close(self) -> None:
        self.dismiss(None)

    def _is_oidc_mode(self) -> bool:
        info = self.app.system_info
        setup = info.setup if info is not None else None
        return bool(setup and (setup.auth_mode or "").upper() == "OIDC")

    @on(Button.Pressed, "#delete_account_cancel_btn")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#delete_account_confirm_btn")
    @on(Input.Submitted, "#delete_account_password")
    def _on_confirm(self) -> None:
        if self._is_oidc_mode():
            self._do_withdraw(None)
            return
        password = self.query_one("#delete_account_password", Input).value
        if not password:
            self._set_status(
                "delete_account_password",
                i18n.get("login_validation_required"),
                "error",
            )
            return
        self._do_withdraw(password)

    @work(exclusive=True, group="delete_account")
    async def _do_withdraw(self, password: str | None) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.account.withdraw(password)
        except TissueApiError as e:
            log.warning("Account withdrawal failed: %s", e)
            reason = self._failure_reason(e)
            self.app.notify(
                i18n.get("delete_account_failed", reason=reason),
                severity="error",
            )
            return

        self.app.notify(i18n.get("delete_account_success"))
        self.dismiss(True)
        self.app.logout()

    def _warning_text(self) -> str:
        info = self.app.system_info
        days = info.member_deletion_retention_days if info is not None else None
        if days is None:
            return i18n.get("delete_account_warning_unknown")
        return i18n.get("delete_account_warning", days=days)

    @staticmethod
    def _failure_reason(exc: TissueApiError) -> str:
        if exc.title in ("INVALID_PASSWORD", "PASSWORD_MISMATCH"):
            return i18n.get("delete_account_error_invalid_password")
        return exc.detail or exc.title or str(exc)

    def _set_status(
        self, input_id: str, message: str = "", kind: str | None = None
    ) -> None:
        label = self.query_one(f"#{input_id}_status", Label)
        label.remove_class("-error", "-waiting", "-success")
        label.update(message if kind is not None else "")
        if kind is not None:
            label.add_class(f"-{kind}")
