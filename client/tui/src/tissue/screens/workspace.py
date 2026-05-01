import dataclasses
import logging

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Horizontal
from textual.screen import Screen
from textual.widgets import ContentSwitcher, Footer, Header
from textual.widgets.option_list import Option

from tissue.api.auth import AuthAPI
from tissue.api.errors import ApiNetworkError, ApiResponseError, TissueApiError
from tissue.api.member import MemberAPI
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.connect import ConnectScreen
from tissue.screens.list_action_menu import ListActionMenu
from tissue.screens.logout_confirm import LogoutConfirmModal
from tissue.widgets.invitations_panel import InvitationsPanel
from tissue.widgets.sidebar import Sidebar
from tissue.widgets.workspaces_panel import WorkspacesPanel

log = logging.getLogger(__name__)


class WorkspaceScreen(Screen):
    CSS_PATH = "css/workspace.tcss"

    BINDINGS = [
        Binding("ctrl+b", "toggle_sidebar", "sidebar", priority=True),
    ]

    def __init__(self, config_manager: ConfigManager) -> None:
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        yield Header()
        with Horizontal():
            yield Sidebar(id="sidebar")
            with ContentSwitcher(id="content", initial="workspaces_panel"):
                yield WorkspacesPanel(id="workspaces_panel")
                yield InvitationsPanel(id="invitations_panel")
        yield Footer()

    def on_mount(self) -> None:
        self._apply_binding_labels()
        i18n.subscribe(self._apply_binding_labels)
        self._load_profile()

    def on_unmount(self) -> None:
        i18n.unsubscribe(self._apply_binding_labels)

    def _apply_binding_labels(self) -> None:
        labels = {
            "ctrl+b": i18n.get("binding_toggle_sidebar"),
        }
        for key, label in labels.items():
            existing = self._bindings.key_to_bindings.get(key, [])
            self._bindings.key_to_bindings[key] = [
                dataclasses.replace(b, description=label) for b in existing
            ]

    def action_toggle_sidebar(self) -> None:
        sidebar = self.query_one(Sidebar)
        sidebar.collapsed = not sidebar.collapsed

    @work(exclusive=True, group="profile")
    async def _load_profile(self) -> None:
        try:
            profile = await MemberAPI(self.app.client).get_my_profile()
        except ApiResponseError as e:
            log.warning("Profile load failed: %s", e)
            if e.status_code == 401:
                await self._return_to_auth(after_logout=False)
                return
            self.app.notify(
                i18n.get("profile_load_failed"), severity="error", timeout=3
            )
            return
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("Profile load error: %s", e)
            self.app.notify(
                i18n.get("profile_load_failed"), severity="error", timeout=3
            )
            return

        self.app.current_profile = profile
        self.query_one(Sidebar).set_user(profile.name, profile.username)

    async def _return_to_auth(self, *, after_logout: bool) -> None:
        self.config_manager.clear_tokens()
        self.app.current_profile = None
        try:
            info = await AuthAPI(self.app.client).get_system_info()
        except (ApiNetworkError, TissueApiError) as e:
            log.warning("system-info probe failed: %s", e)
            if after_logout:
                self.app.notify(i18n.get("logged_out"), timeout=2)
            else:
                self.app.notify(
                    i18n.get("session_expired_offline"),
                    severity="warning",
                    timeout=3,
                )
            self.app.switch_screen(ConnectScreen(self.config_manager))
            return
        self.app.system_info = info
        config = self.config_manager.get_config()
        if config.current_server:
            self.config_manager.save_server(config.current_server, info.server_name)
        if after_logout:
            self.app.notify(i18n.get("logged_out"), timeout=2)
        else:
            self.app.notify(i18n.get("session_expired"), severity="warning", timeout=3)
        from tissue.screens.login import LoginScreen

        self.app.switch_screen(ConnectScreen(self.config_manager))
        self.app.push_screen(LoginScreen(info, self.config_manager))

    @on(Sidebar.ItemSelected)
    def _on_item_selected(self, event: Sidebar.ItemSelected) -> None:
        self.query_one("#content", ContentSwitcher).current = event.panel_id

    @on(Sidebar.UserMenuRequested)
    def _on_user_menu_requested(self, event: Sidebar.UserMenuRequested) -> None:
        self._open_user_menu(event.x, event.y)

    @work
    async def _open_user_menu(self, x: int, y: int) -> None:
        options = [
            Option(i18n.get("user_menu_my_profile"), id="my_profile"),
            Option(i18n.get("user_menu_account_management"), id="account_management"),
            Option(i18n.get("user_menu_logout"), id="logout"),
        ]
        result = await self.app.push_screen_wait(
            ListActionMenu(options=options, anchor_x=x, anchor_y=y)
        )
        if result in ("my_profile", "account_management"):
            self.app.notify(i18n.get("feature_todo"), timeout=2)
        elif result == "logout":
            confirmed = await self.app.push_screen_wait(LogoutConfirmModal())
            if confirmed:
                self._logout()

    @work(exclusive=True, group="logout")
    async def _logout(self) -> None:
        tokens = self.config_manager.get_tokens()
        if tokens:
            try:
                await AuthAPI(self.app.client).logout(tokens.refresh_token)
            except (ApiResponseError, ApiNetworkError, TissueApiError) as e:
                log.warning("Logout API failed (ignored): %s", e)
        await self._return_to_auth(after_logout=True)
