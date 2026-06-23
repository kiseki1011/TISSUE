from pathlib import Path
from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import Button, Label, Rule

from tissue.api.generated.models.member_profile import MemberProfile
from tissue.rendering.icon import make_icon_widget
from tissue.screens.base import TissueModal
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.spatial_focus import focus_in_direction
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.app import TissueApp

_PROFILE_ASSET_DIR = Path(__file__).parent.parent.parent / "assets" / "profile"


class AccountModal(TissueModal[None]):
    """View account info and launch profile / password / delete / logout actions.

    Each editable field (name, username, email) carries a pencil icon that opens
    a single-field edit modal. In OIDC mode the identity provider owns name and
    email, so only username is editable and only its pencil is shown.
    """

    CSS_PATH = "account_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
        # Arrow / hjkl navigate the focusable controls by position: the edit pencils
        # are a vertical column (j/k), the bottom actions a horizontal row (h/l), and
        # j/k cross between the two (see widgets/spatial_focus).
        Binding("up,k", "focus_dir('up')", show=False),
        Binding("down,j", "focus_dir('down')", show=False),
        Binding("left,h", "focus_dir('left')", show=False),
        Binding("right,l", "focus_dir('right')", show=False),
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
                with Horizontal(id="account-top"):
                    yield Container(image, classes="account-image-wrap")
                    yield Container(*self._info_rows(), classes="account-info")
                yield Rule(classes="account-divider")
                with Horizontal(classes="account-actions"):
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
        self._focus_first_action()

    def _focus_first_action(self) -> None:
        """Land on the first pencil if any field is editable, else the first action."""
        icon = self.query(".account-edit-icon")
        target = icon.first() if icon else self.query_one("#account-password-btn")
        target.focus()

    def _info_rows(self) -> list[Horizontal]:
        profile = self._cached_profile()
        oidc = self._is_oidc_mode()
        return [
            _account_row(
                "Name",
                profile.name if profile and profile.name else "-",
                field="name",
                editable=not oidc,
            ),
            _account_row(
                "Username",
                profile.username if profile and profile.username else "-",
                field="username",
                editable=True,
            ),
            _account_row(
                "Email",
                profile.email if profile and profile.email else "-",
                field="email",
                editable=not oidc and self._email_required(),
            ),
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

    def _field_value(self, field: str) -> str:
        profile = self._cached_profile()
        if profile is None:
            return ""
        return {
            "name": profile.name,
            "username": profile.username,
            "email": profile.email,
        }.get(field) or ""

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

    def action_focus_dir(self, direction: str) -> None:
        focus_in_direction(self, direction)

    @on(Button.Pressed, ".account-edit-icon")
    def _on_edit_field(self, event: Button.Pressed) -> None:
        from tissue.screens.account.field_edit_modal import FieldEditModal

        field = (event.button.id or "").removeprefix("account-edit-")
        self.app.push_screen(
            FieldEditModal(field=field, current_value=self._field_value(field)),
            lambda updated: self._on_edit_closed(updated, field),
        )

    def _on_edit_closed(self, updated: bool | None, field: str) -> None:
        if not updated:
            return
        self._repaint_info_rows()
        # Refocus only after the repaint settles: remove_children defers its prune,
        # so the old pencil (same id) lingers in the DOM until the next refresh.
        self.call_after_refresh(self._focus_pencil, field)

    def _focus_pencil(self, field: str) -> None:
        try:
            self.query_one(f"#account-edit-{field}", TextButton).focus()
        except NoMatches:
            pass

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


def _account_row(
    key: str, value: str, *, field: str | None = None, editable: bool = False
) -> Horizontal:
    children: list = [
        Label(f"{key}:", classes="account-info-key"),
        Label(value, classes="account-info-value"),
    ]
    if editable and field:
        children.append(
            TextButton("✎", id=f"account-edit-{field}", classes="account-edit-icon")
        )
    return Horizontal(*children, classes="account-info-row")
