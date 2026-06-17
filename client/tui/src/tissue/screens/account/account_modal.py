from pathlib import Path
from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.widgets import Button, Label, Rule

from tissue.api.generated.models.member_profile import MemberProfile
from tissue.rendering.icon import make_icon_widget
from tissue.screens.base import TissueModal
from tissue.util.datetime_fmt import format_relative

if TYPE_CHECKING:
    from tissue.app import TissueApp

_PROFILE_ASSET_DIR = Path(__file__).parent.parent.parent / "assets" / "profile"


class AccountModal(TissueModal[None]):
    """View account info and launch profile / password / delete / logout actions."""

    CSS_PATH = "account_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    if TYPE_CHECKING:
        app: TissueApp

    def compose(self) -> ComposeResult:
        image = make_icon_widget(self._profile_image_path())
        image.add_class("account-image")

        password_btn = Button("Change password", id="account-password-btn")
        password_btn.disabled = self._is_oidc_mode()

        with VerticalScroll(id="account-dialog", classes="dialog"):
            with Vertical(id="account-inner"):
                yield Container(image, classes="account-image-wrap")
                yield Container(*self._info_rows(), classes="account-info")
                yield Rule(classes="account-divider")
                with Vertical(classes="account-actions"):
                    yield Button("Edit profile", id="account-edit-btn")
                    yield password_btn
                    yield Button(
                        "Delete account",
                        id="account-delete-btn",
                        classes="-btn-error",
                    )
                    yield Button(
                        "Logout",
                        id="account-logout-btn",
                        classes="-btn-secondary",
                    )

    def on_mount(self) -> None:
        dialog = self.query_one("#account-dialog", VerticalScroll)
        dialog.border_title = "Account"
        dialog.border_subtitle = "Esc to close"
        self.query_one("#account-edit-btn", Button).focus()

    def _info_rows(self) -> list[Horizontal]:
        profile = self._cached_profile()
        return [
            _account_row("Name", profile.name if profile and profile.name else "-"),
            _account_row(
                "Username",
                profile.username if profile and profile.username else "-",
            ),
            _account_row("Email", profile.email if profile and profile.email else "-"),
            _account_row("Role", profile.role if profile and profile.role else "-"),
            _account_row(
                "Joined", format_relative(profile.joined_at) if profile else "-"
            ),
        ]

    def _repaint_info_rows(self) -> None:
        info = self.query_one(".account-info", Container)
        info.remove_children()
        info.mount_all(self._info_rows())

    def _cached_profile(self) -> MemberProfile | None:
        client = self.app.client
        return client.account.cached_profile if client is not None else None

    def _profile_image_path(self) -> Path:
        name = (
            "default_profile_dark.png"
            if self._is_dark_theme()
            else "default_profile_light.png"
        )
        return _PROFILE_ASSET_DIR / name

    def _is_dark_theme(self) -> bool:
        theme = getattr(self.app, "current_theme", None)
        if theme is None:
            return True
        return bool(getattr(theme, "dark", True))

    def _is_oidc_mode(self) -> bool:
        info = self.app.system_info
        setup = info.setup if info is not None else None
        return bool(setup and (setup.auth_mode or "").upper() == "OIDC")

    def _email_required(self) -> bool:
        info = self.app.system_info
        setup = info.setup if info is not None else None
        return bool(setup and setup.email_required)

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#account-edit-btn")
    def _on_edit(self) -> None:
        from tissue.screens.account.edit_profile_modal import EditProfileModal

        self.app.push_screen(
            EditProfileModal(email_required=self._email_required()),
            self._on_edit_closed,
        )

    @on(Button.Pressed, "#account-password-btn")
    def _on_password(self) -> None:
        if self._is_oidc_mode():
            return
        from tissue.screens.account.change_password_modal import ChangePasswordModal

        self.app.push_screen(ChangePasswordModal())

    @on(Button.Pressed, "#account-delete-btn")
    def _on_delete(self) -> None:
        from tissue.screens.account.delete_account_modal import DeleteAccountModal

        self.app.push_screen(DeleteAccountModal())

    @on(Button.Pressed, "#account-logout-btn")
    def _on_logout(self) -> None:
        from tissue.screens.account.logout_modal import LogoutModal

        self.app.push_screen(LogoutModal(), self._on_logout_confirmed)

    def _on_logout_confirmed(self, confirmed: bool | None) -> None:
        if confirmed:
            self.app.logout()

    def _on_edit_closed(self, updated: bool | None) -> None:
        if updated:
            self._repaint_info_rows()


def _account_row(key: str, value: str) -> Horizontal:
    return Horizontal(
        Label(f"{key}:", classes="account-info-key"),
        Label(value, classes="account-info-value"),
        classes="account-info-row",
    )
