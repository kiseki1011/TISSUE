from pathlib import Path

from textual import on
from textual.app import ComposeResult
from textual.containers import Container, Horizontal
from textual.widgets import Button, Rule, Static

from tissue.api.generated.models.member_profile import MemberProfile
from tissue.i18n.manager import i18n
from tissue.rendering.icon import make_icon_widget
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.sidebar_nav_button import SidebarNavButton

_PROFILE_ASSET_DIR = Path(__file__).parent.parent / "assets" / "profile"


class MyAccountTab(Horizontal):
    """My Account tab.

    - Left pane: profile info + sidebar nav buttons
    - Right pane: renders based on the selected sidebar button
    """

    DEFAULT_CSS = """
    MyAccountTab {
        width: 100%;
        height: 100%;
        padding: 1;
    }

    #account-profile-pane {
        width: 42;
        height: 100%;
        margin-right: 1;
        overflow-y: auto;
    }

    #account-detail-pane {
        width: 1fr;
        height: 100%;
        overflow-y: auto;
    }

    .account-detail-content {
        width: 100%;
        height: auto;
        padding: 1 2;
    }

    .account-profile-image-wrap {
        width: 100%;
        height: auto;
        align-horizontal: center;
        padding: 1 0;
    }

    .account-profile-image {
        width: 16;
        height: 8;
    }

    .account-profile-info {
        width: 100%;
        height: auto;
        padding: 0 2;
        margin-top: 1;
    }

    .account-divider {
        width: 100%;
        margin: 1 0;
        color: $primary;
    }

    .account-nav-buttons {
        width: 100%;
        height: auto;

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
            margin-top: 1;
        }

        SidebarNavButton.-danger:hover {
            background: $error 15% !important;
            color: $accent !important;
        }

        SidebarNavButton.-danger:focus {
            background: $error 30% !important;
            color: $error !important;
        }

        SidebarNavButton.-selected {
            text-style: bold;
        }
    }
    """

    _VIEW_TO_BUTTON_ID = {
        "edit_profile": "acc_edit_profile_btn",
        "change_password": "acc_change_password_btn",
        "withdraw": "acc_withdraw_btn",
    }

    _VIEW_TO_TITLE_KEY = {
        "edit_profile": "home_account_btn_edit_profile",
        "change_password": "home_account_btn_change_password",
        "withdraw": "home_account_btn_withdraw",
    }

    def __init__(self) -> None:
        super().__init__(id="account-split")
        self._view: str | None = None

    def compose(self) -> ComposeResult:
        yield self._build_profile_pane()
        yield self._build_detail_pane()

    def on_mount(self) -> None:
        self._show_view("edit_profile")

    def _build_profile_pane(self) -> Container:
        profile = self._cached_profile()

        image = make_icon_widget(self._profile_image_path())
        image.add_class("account-profile-image")

        info_rows = [
            detail_row(
                i18n.get("home_account_label_name"),
                (profile.name if profile and profile.name else "-"),
            ),
            detail_row(
                i18n.get("home_account_label_username"),
                (profile.username if profile and profile.username else "-"),
            ),
            detail_row(
                i18n.get("home_account_label_email"),
                (profile.email if profile and profile.email else "-"),
            ),
            detail_row(
                i18n.get("home_account_label_joined"),
                format_relative(profile.joined_at) if profile else "-",
            ),
        ]

        pane = Container(
            Container(image, classes="account-profile-image-wrap"),
            Container(*info_rows, classes="account-profile-info"),
            Rule(classes="account-divider"),
            Container(
                SidebarNavButton(
                    i18n.get("home_account_btn_edit_profile"),
                    id="acc_edit_profile_btn",
                ),
                SidebarNavButton(
                    i18n.get("home_account_btn_change_password"),
                    id="acc_change_password_btn",
                ),
                SidebarNavButton(
                    i18n.get("home_account_btn_withdraw"),
                    id="acc_withdraw_btn",
                    classes="-danger",
                ),
                classes="account-nav-buttons",
            ),
            classes="panel",
            id="account-profile-pane",
        )
        pane.border_title = i18n.get("home_account_profile_title")
        return pane

    def _build_detail_pane(self) -> Container:
        pane = Container(
            Container(
                Static(
                    i18n.get("home_account_detail_placeholder"),
                    classes="detail-empty",
                ),
                classes="account-detail-content",
                id="account-detail-content",
            ),
            classes="panel",
            id="account-detail-pane",
        )
        pane.border_title = i18n.get("home_detail_title")
        return pane

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

    @on(Button.Pressed, "#acc_edit_profile_btn")
    def _on_edit_profile_pressed(self) -> None:
        self._show_view("edit_profile")

    @on(Button.Pressed, "#acc_change_password_btn")
    def _on_change_password_pressed(self) -> None:
        self._show_view("change_password")

    @on(Button.Pressed, "#acc_withdraw_btn")
    def _on_withdraw_pressed(self) -> None:
        self._show_view("withdraw")

    def _show_view(self, view: str) -> None:
        """Re-render the right pane based on the selected sidebar action."""
        self._view = view

        selected_id = self._VIEW_TO_BUTTON_ID[view]
        for bid in self._VIEW_TO_BUTTON_ID.values():
            btn = self.query_one(f"#{bid}", SidebarNavButton)
            btn.set_class(bid == selected_id, "-selected")

        self.query_one("#account-detail-pane", Container).border_title = i18n.get(
            self._VIEW_TO_TITLE_KEY[view]
        )

        content = self.query_one("#account-detail-content", Container)
        content.remove_children()

        if view == "withdraw":
            content.mount(
                Static(
                    i18n.get("home_account_withdraw_warning"),
                    classes="warning",
                )
            )

        content.mount(
            Static(i18n.get("home_account_coming_soon"), classes="detail-empty")
        )
