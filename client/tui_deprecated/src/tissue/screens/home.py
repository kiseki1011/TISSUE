import dataclasses
import logging

from textual import on, work
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.screen import Screen
from textual.widgets import ContentSwitcher, Footer

# from textual.widgets import Header
from tissue.api.auth import AuthAPI
from tissue.api.errors import ApiNetworkError, ApiResponseError, TissueApiError
from tissue.api.member import MemberAPI
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.connect import ConnectScreen
from tissue.screens.logout_confirm import LogoutConfirmModal
from tissue.widgets.i18n_widgets import I18nLabel
from tissue.widgets.invitations_panel import InvitationsPanel
from tissue.widgets.sidebar import Sidebar
from tissue.widgets.workspaces_panel import WorkspacesPanel

log = logging.getLogger(__name__)


class _ProfilePanel(Container):
    DEFAULT_CSS = """
    _ProfilePanel {
        align: center middle;
        height: 1fr;
        width: 100%;
    }
    _ProfilePanel I18nLabel {
        color: $text-muted;
        text-style: italic;
    }
    """

    def compose(self) -> ComposeResult:
        yield I18nLabel("profile_panel_todo")


class _AccountPanel(Container):
    DEFAULT_CSS = """
    _AccountPanel {
        align: center middle;
        height: 1fr;
        width: 100%;
    }
    _AccountPanel I18nLabel {
        color: $text-muted;
        text-style: italic;
    }
    """

    def compose(self) -> ComposeResult:
        yield I18nLabel("account_panel_todo")


class HomeScreen(Screen):
    CSS_PATH = "css/home.tcss"

    BINDINGS = [
        Binding("ctrl+b", "toggle_sidebar", "sidebar", priority=True),
        Binding("1", "switch_panel('workspaces_panel')", show=False, priority=True),
        Binding("2", "switch_panel('invitations_panel')", show=False, priority=True),
        Binding("3", "switch_panel('account_panel')", show=False, priority=True),
        Binding("ctrl+l", "logout", "log out", priority=True),
        Binding("tab", "next_area", show=False, priority=True),
        Binding("shift+tab", "prev_area", show=False, priority=True),
    ]

    _AREA_IDS = ("sidebar_user_slot", "sidebar_items", "content")

    def __init__(self, config_manager: ConfigManager) -> None:
        super().__init__()
        self.config_manager = config_manager

    def compose(self) -> ComposeResult:
        # yield Header()
        with Horizontal():
            yield Sidebar(id="sidebar")
            with ContentSwitcher(id="content", initial="workspaces_panel"):
                yield WorkspacesPanel(id="workspaces_panel")
                yield InvitationsPanel(id="invitations_panel")
                yield _AccountPanel(id="account_panel")
                yield _ProfilePanel(id="my_profile_panel")
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

    def action_switch_panel(self, panel_id: str) -> None:
        self.query_one(Sidebar).select_panel(panel_id)

    def action_logout(self) -> None:
        self._handle_logout()

    def action_next_area(self) -> None:
        self._cycle_area(1)

    def action_prev_area(self) -> None:
        self._cycle_area(-1)

    def _cycle_area(self, direction: int) -> None:
        focused = self.focused
        cur_idx = self._find_focused_area_index(focused)
        if cur_idx < 0:
            cur_idx = 0
        new_idx = (cur_idx + direction) % len(self._AREA_IDS)
        self._focus_area_at(new_idx)

    def _find_focused_area_index(self, focused) -> int:
        if focused is None:
            return -1
        for idx, area_id in enumerate(self._AREA_IDS):
            area = self.query_one(f"#{area_id}")
            node = focused
            while node is not None:
                if node is area:
                    return idx
                node = node.parent
        return -1

    def _focus_area_at(self, idx: int) -> None:
        area_id = self._AREA_IDS[idx]
        if area_id == "content":
            switcher = self.query_one("#content", ContentSwitcher)
            current = switcher.query_one(f"#{switcher.current}")
            self._focus_panel_default(current)
            return
        area = self.query_one(f"#{area_id}")
        if area.can_focus:
            area.focus()
            return
        target = self._first_focusable(area)
        if target:
            target.focus()

    def _focus_panel_default(self, panel) -> None:
        if hasattr(panel, "focus_default"):
            panel.focus_default()
            return
        target = self._first_focusable(panel)
        if target:
            target.focus()
        elif panel.can_focus:
            panel.focus()

    @staticmethod
    def _first_focusable(root):
        for w in root.walk_children(with_self=False):
            if getattr(w, "can_focus", False):
                return w
        return None

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
        self.query_one(Sidebar).set_user(profile.name, profile.username, profile.email)

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
        switcher = self.query_one("#content", ContentSwitcher)
        switcher.current = event.panel_id
        panel = switcher.query_one(f"#{event.panel_id}")
        self._focus_panel_default(panel)

    @on(Sidebar.LogoutRequested)
    def _on_logout_requested(self, event: Sidebar.LogoutRequested) -> None:
        self._handle_logout()

    @work
    async def _handle_logout(self) -> None:
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
