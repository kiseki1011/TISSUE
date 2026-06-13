from pathlib import Path
from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.widgets import Button, Label, Rule

from tissue.api.generated.models.member_profile import MemberProfile
from tissue.i18n.manager import i18n
from tissue.rendering.icon import make_icon_widget
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.sidebar_nav_button import SidebarNavButton

if TYPE_CHECKING:
    from tissue.app import TissueApp

_PROFILE_ASSET_DIR = Path(__file__).parent.parent / "assets" / "profile"


class ProfileSidebar(Container):
    """Profile pane with account action buttons.

    Bindings are widget-level so they are only active while the sidebar is mounted and
    something inside it has focus.
    """

    DEFAULT_CLASSES = "panel"

    BINDINGS = [
        Binding("e", "edit_profile", show=False),
        Binding("p", "change_password", show=False),
        Binding("d", "delete_account", show=False),
        Binding("l", "logout", show=False),
    ]

    DEFAULT_CSS = """
    ProfileSidebar {
        dock: left;
        width: 41;
        height: 1fr;
        overflow-y: auto;
        background: $surface;
        border-title-align: center;
    }

    ProfileSidebar .account-profile-image-wrap {
        width: 100%;
        height: auto;
        align-horizontal: center;
        padding: 1 0;
    }

    ProfileSidebar .account-profile-image {
        width: 16;
        height: 8;
    }

    ProfileSidebar .account-profile-info {
        width: 100%;
        height: auto;
        padding: 0 2;
        margin-top: 1;
    }

    ProfileSidebar .profile-info-row {
        width: 100%;
        height: auto;
        margin-bottom: 1;
    }

    ProfileSidebar .profile-info-key {
        width: 10;
        color: $text-muted;
        text-style: bold;
    }

    ProfileSidebar .profile-info-value {
        width: 1fr;
    }

    ProfileSidebar .account-divider {
        width: 100%;
        margin: 1 0;
        color: $primary;
    }

    ProfileSidebar .account-nav-buttons {
        width: 100%;
        height: auto;
        padding: 0 0;

        SidebarNavButton {
            margin-bottom: 1;
        }

        SidebarNavButton:hover {
            background: $primary 15% !important;
        }

        SidebarNavButton:focus {
            background: $primary 30% !important;
            color: $primary !important;
        }

        SidebarNavButton.-danger {
            color: $error;
            margin: 1 0;
        }

        SidebarNavButton.-danger:hover {
            background: $error 15% !important;
            color: $accent !important;
        }

        SidebarNavButton.-danger:focus {
            background: $error 30% !important;
            color: $error !important;
        }

        SidebarNavButton#profile_sidebar_logout_btn {
            color: $warning-darken-2;
        }

        SidebarNavButton#profile_sidebar_logout_btn:hover {
            background: $warning 15% !important;
        }

        SidebarNavButton#profile_sidebar_logout_btn:focus {
            background: $warning 30% !important;
            color: $warning-darken-2 !important;
        }
    }
    """

    if TYPE_CHECKING:
        app: TissueApp

    def compose(self) -> ComposeResult:
        image = make_icon_widget(self._profile_image_path())
        image.add_class("account-profile-image")

        password_btn = SidebarNavButton(
            i18n.get("home_account_btn_change_password"),
            id="profile_sidebar_password_btn",
            shortcut="p",
        )
        password_btn.disabled = self._is_oidc_mode()

        yield Container(image, classes="account-profile-image-wrap")
        yield Container(*self._info_rows(), classes="account-profile-info")
        yield Rule(classes="account-divider")
        yield Container(
            SidebarNavButton(
                i18n.get("home_account_btn_edit_profile"),
                id="profile_sidebar_edit_btn",
                shortcut="e",
            ),
            password_btn,
            SidebarNavButton(
                i18n.get("home_account_btn_withdraw"),
                id="profile_sidebar_withdraw_btn",
                classes="-danger",
                shortcut="d",
            ),
            SidebarNavButton(
                i18n.get("home_account_btn_logout"),
                id="profile_sidebar_logout_btn",
                shortcut="l",
            ),
            classes="account-nav-buttons",
        )

    def on_mount(self) -> None:
        self.border_title = i18n.get("home_account_profile_title")
        self.query_one("#profile_sidebar_edit_btn", SidebarNavButton).focus()

    def _info_rows(self) -> list:
        profile = self._cached_profile()
        return [
            _profile_row(
                i18n.get("home_account_label_name"),
                (profile.name if profile and profile.name else "-"),
            ),
            _profile_row(
                i18n.get("home_account_label_username"),
                (profile.username if profile and profile.username else "-"),
            ),
            _profile_row(
                i18n.get("home_account_label_email"),
                (profile.email if profile and profile.email else "-"),
            ),
            _profile_row(
                i18n.get("home_account_label_system_role"),
                (profile.role if profile and profile.role else "-"),
            ),
            _profile_row(
                i18n.get("home_account_label_joined"),
                format_relative(profile.joined_at) if profile else "-",
            ),
        ]

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

    def _repaint_info_rows(self) -> None:
        info = self.query_one(".account-profile-info", Container)
        info.remove_children()
        info.mount_all(self._info_rows())

    @on(Button.Pressed, "#profile_sidebar_edit_btn")
    def _on_edit_pressed(self) -> None:
        self.action_edit_profile()

    @on(Button.Pressed, "#profile_sidebar_password_btn")
    def _on_password_pressed(self) -> None:
        self.action_change_password()

    @on(Button.Pressed, "#profile_sidebar_withdraw_btn")
    def _on_withdraw_pressed(self) -> None:
        self.action_delete_account()

    @on(Button.Pressed, "#profile_sidebar_logout_btn")
    def _on_logout_pressed(self) -> None:
        self.action_logout()

    def action_edit_profile(self) -> None:
        from tissue.screens.account.edit_profile_modal import EditProfileModal

        self.app.push_screen(
            EditProfileModal(email_required=self._email_required()),
            self._on_edit_closed,
        )

    def _email_required(self) -> bool:
        info = self.app.system_info
        setup = info.setup if info is not None else None
        return bool(setup and setup.email_required)

    def _is_oidc_mode(self) -> bool:
        info = self.app.system_info
        setup = info.setup if info is not None else None
        return bool(setup and (setup.auth_mode or "").upper() == "OIDC")

    def action_change_password(self) -> None:
        if self._is_oidc_mode():
            return
        from tissue.screens.account.change_password_modal import ChangePasswordModal

        self.app.push_screen(ChangePasswordModal())

    def action_delete_account(self) -> None:
        from tissue.screens.account.delete_account_modal import DeleteAccountModal

        self.app.push_screen(DeleteAccountModal())

    def action_logout(self) -> None:
        from tissue.screens.account.logout_modal import LogoutModal

        self.app.push_screen(LogoutModal(), self._on_logout_confirmed)

    def _on_logout_confirmed(self, confirmed: bool | None) -> None:
        if confirmed:
            self.app.logout()

    def _on_edit_closed(self, updated: bool | None) -> None:
        if updated:
            self._repaint_info_rows()


def _profile_row(key: str, value: str) -> Horizontal:
    """Sidebar `key`: `value` row.

    Uses dedicated `profile-info-*` classes so it doesnt use the `.detail-*` rules
    in screen-level CSS.
    """
    return Horizontal(
        Label(f"{key}:", classes="profile-info-key"),
        Label(value, classes="profile-info-value"),
        classes="profile-info-row",
    )
