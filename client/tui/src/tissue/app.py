import asyncio
import logging
from collections.abc import Iterable
from datetime import datetime

from textual.app import App, SystemCommand
from textual.binding import Binding
from textual.css.query import NoMatches
from textual.screen import Screen

from tissue.api.client import TissueClient
from tissue.api.errors import TissueApiError
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.auth.token_store import create_token_store
from tissue.commands import TissueCommands
from tissue.config.manager import ConfigManager
from tissue.i18n.manager import i18n
from tissue.screens.connect import ConnectScreen
from tissue.screens.login import LoginScreen
from tissue.screens.option import OptionModal
from tissue.screens.reconnect import ReconnectScreen
from tissue.theming import generate_btn_variant_css

log = logging.getLogger(__name__)


# TODO: workspace 혹은 ws 하나로 통일
class TissueApp(App):
    CSS_PATH = "global.tcss"

    BORDER_STYLES = (
        "round",
        "solid",
        "heavy",
        "dashed",
        "double",
        "hkey",
        "tab",
        "ascii",
    )

    CSS = generate_btn_variant_css(BORDER_STYLES)

    COMMANDS = App.COMMANDS | {TissueCommands}

    BINDINGS = [
        Binding("ctrl+o", "options", "options"),
    ]

    def __init__(self) -> None:
        super().__init__()
        self.config = ConfigManager()
        i18n.set_language(self.config.settings.language)
        self.theme = self.config.settings.theme
        self._apply_border_style(self.config.settings.border_style)
        self.token_store = create_token_store()
        self.client: TissueClient | None = None
        self.system_info: SystemInfoDetails | None = None

    RECONNECT_SCREEN_DELAY = 0.5  # 500ms before falling back to ReconnectScreen

    async def on_mount(self) -> None:
        saved_url = self.config.state.current_server_url
        if not saved_url:
            self.push_screen(ConnectScreen(self.config))
            return

        # Current server url exists
        client = TissueClient(host=saved_url, token_store=self.token_store)
        try:
            system_info = await asyncio.wait_for(
                client.ping(), timeout=self.RECONNECT_SCREEN_DELAY
            )
        # Connection fails in RECONNECT_SCREEN_DELAY window
        except (TimeoutError, TissueApiError) as e:
            log.debug("Initial ping failed, showing reconnect screen: %s", e)
            await client.close()
            self.push_screen(ReconnectScreen(saved_url, self.config))
            return

        # Connection succeeds
        self.client = client
        self.system_info = system_info
        self.config.update_state(last_connected_at=datetime.now().astimezone())

        # Restore the previous session from a stored token.
        saved_token = self.token_store.load(saved_url)
        if saved_token is not None:
            try:
                if await client.restore_session(saved_token):
                    self._route_to_last_screen()
                    return
            except TissueApiError as e:
                log.debug("Session restore (login) failed: %s", e)
            client.clear_tokens()

        # Restore failed → manual login
        self.push_screen(LoginScreen(system_info, self.config))

    def _route_to_last_screen(self) -> None:
        """Restore the screen the user was on before they closed the app."""
        from tissue.screens.home import HomeScreen
        from tissue.screens.workspace_home import WorkspaceHomeScreen

        if self.client is None:
            return

        # Capture before pushing HomeScreen
        saved_ws_key = self.config.state.current_workspace_key

        self.push_screen(HomeScreen())

        if not saved_ws_key:
            return

        # TODO: workspaces -> workspace_summary_list 고려
        # WorkspaceSummaryResponse를 WorkspaceSummary 혹은 다른 이름으로 변경 예정
        workspaces = self.client.cached_workspaces or []
        matching_workspace = next(
            (w for w in workspaces if w.workspace_key == saved_ws_key),
            None,
        )
        if matching_workspace is not None:
            self.push_screen(WorkspaceHomeScreen(matching_workspace))

    async def on_unmount(self) -> None:
        if self.client is not None:
            await self.client.close()
            self.client = None

    def change_language(self, lang: str) -> None:
        """Changes the current language setting and all mounted screens by recomposing
        the screen.

        The focus id is saved to maintain the focus even after recompose.
        """
        i18n.set_language(lang)
        self.config.update_settings(language=lang)

        focused_id = self.focused.id if self.focused else None

        for screen in self.screen_stack:
            screen.refresh(recompose=True)

        if focused_id:
            self.call_after_refresh(self._refocus_by_id, focused_id)

    def _refocus_by_id(self, widget_id: str) -> None:
        try:
            self.screen.query_one(f"#{widget_id}").focus()
        except NoMatches:
            log.debug("Could not refocus #%s after recompose", widget_id)

    def change_theme(self, theme: str) -> None:
        self.theme = theme
        self.config.update_settings(theme=theme)

    def change_border_style(self, style: str) -> None:
        self._apply_border_style(style)
        self.config.update_settings(border_style=style)

    def _apply_border_style(self, style: str) -> None:
        for s in self.BORDER_STYLES:
            self.remove_class(f"-border-{s}")
        if style != "round":
            self.add_class(f"-border-{style}")
        for screen in self.screen_stack:
            # Force apply the new tcss for the screen (due to cache)
            self.stylesheet.update(screen)

    _HIDDEN_SYSTEM_COMMANDS = ("theme", "maximize")

    def get_system_commands(self, screen: Screen) -> Iterable[SystemCommand]:
        """Hide built-in commands we don't expose (theme is in OptionModal,
        maximize is unused).
        """
        for command in super().get_system_commands(screen):
            title = command.title.lower()
            if any(keyword in title for keyword in self._HIDDEN_SYSTEM_COMMANDS):
                continue
            yield command

    def action_options(self) -> None:
        if isinstance(self.screen, OptionModal):
            return
        self.push_screen(OptionModal(self.config))

    def route_to_post_login(self) -> None:
        """Branch to the right post-login screen.

        Decision matrix:
            - multi-tenant=true → always HomeScreen (WorkspaceCreate popup modal on
            first login, handled separately by HomeScreen).
            - multi-tenant=false & user owns 1 workspace → sent to WorkspaceHomeScreen.
            - everything else → HomeScreen

        First login (server, username) is recorded here so future logins skip
        WorkspaceCreate popup modal.
        """
        from tissue.screens.home import HomeScreen
        from tissue.screens.workspace_home import WorkspaceHomeScreen

        client = self.client
        info = self.system_info
        if client is None or info is None:
            log.error("route_to_post_login called without client/system_info")
            return

        profile = client.cached_member_profile
        workspaces = client.cached_workspaces or []
        multi_tenant = bool(info.multi_tenant)

        # Record (server, username) to determine first-time login.
        if profile is not None and profile.username:
            self.config.mark_login_seen(client.host, profile.username)

        self.switch_screen(HomeScreen())
        if not multi_tenant and len(workspaces) == 1:
            self.push_screen(WorkspaceHomeScreen(workspaces[0]))
