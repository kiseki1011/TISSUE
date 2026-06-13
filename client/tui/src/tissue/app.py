import asyncio
import logging
from collections.abc import Iterable
from datetime import datetime

from textual.app import App, SystemCommand
from textual.screen import Screen

from tissue.api.client import TissueClient
from tissue.api.errors import TissueApiError
from tissue.api.generated.models.system_info_details import SystemInfoDetails
from tissue.auth.token_store import create_token_store
from tissue.commands import TissueCommands
from tissue.config.manager import ConfigManager
from tissue.screens.auth.connecting import ConnectingScreen
from tissue.screens.auth.login import LoginScreen
from tissue.screens.home.home import HomeScreen
from tissue.screens.option import OptionModal
from tissue.theming import generate_btn_variant_css

log = logging.getLogger(__name__)


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

    def __init__(self, *, debug: bool = False, connect_url: str | None = None) -> None:
        super().__init__()
        self._debug = debug
        self._connect_url = connect_url
        self.config = ConfigManager()
        self.theme = self.config.settings.theme
        self._apply_border_style(self.config.settings.border_style)
        self.token_store = create_token_store()
        self.client: TissueClient | None = None
        self.system_info: SystemInfoDetails | None = None

    INITIAL_PING_TIMEOUT = 0.5  # 500ms before showing the connecting screen

    async def on_mount(self) -> None:
        if self._debug:
            asyncio.get_event_loop().set_exception_handler(self._async_exc_handler)

        if self._connect_url:
            self.push_screen(ConnectingScreen(self._connect_url, self.config))
            return

        saved_url = self.config.state.current_server_url
        if not saved_url:
            self.exit(
                return_code=1,
                message="No server configured. Connect with: tissue -c <url>",
            )
            return

        # Current server url exists
        client = TissueClient(host=saved_url, token_store=self.token_store)
        try:
            system_info = await asyncio.wait_for(
                client.ping(), timeout=self.INITIAL_PING_TIMEOUT
            )
        # If unreachable within the `INITIAL_PING_TIMEOUT`, retry with spinner
        except (TimeoutError, TissueApiError) as e:
            log.debug("Initial ping failed, showing connecting screen: %s", e)
            await client.close()
            self.push_screen(ConnectingScreen(saved_url, self.config))
            return

        # Connection succeeds
        self.client = client
        self.system_info = system_info
        self.config.update_state(last_connected_at=datetime.now().astimezone())

        # Restore the previous session from a stored token
        saved_token = self.token_store.load(saved_url)
        if saved_token is not None:
            try:
                if await client.auth.restore_session(saved_token):
                    self._route_to_last_screen()
                    return
            except TissueApiError as e:
                log.debug("Session restore (login) failed: %s", e)
            client.clear_tokens()

        # If restore fails, go to login screen
        self.push_screen(LoginScreen(system_info, self.config))

    def _route_to_last_screen(self) -> None:
        """Restore the screen the user was on before they closed the app."""
        if self.client is None:
            return

        # TODO: recall the last-opened project via state.current_project_key
        self.push_screen(HomeScreen())

    async def on_unmount(self) -> None:
        if self.client is not None:
            await self.client.close()
            self.client = None

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
            # Force apply the new tcss for the screen
            self.stylesheet.update(screen)

    _HIDDEN_SYSTEM_COMMANDS = ("theme", "maximize", "screenshot")

    def get_system_commands(self, screen: Screen) -> Iterable[SystemCommand]:
        """Hide built-in commands we don't want to expose.

        Theme is in OptionModal, maximize is unused.
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

    def logout(self) -> None:
        """Log out the current session and return to LoginScreen.

        Shared entry point used by both the command palette and the profile
        sidebar's logout button.
        """
        self.run_worker(self._do_logout(), exclusive=True, group="logout")

    async def _do_logout(self) -> None:
        from tissue.screens.auth.login import LoginScreen

        if self.client is not None:
            await self.client.auth.logout()
        if self.system_info is not None:
            self.switch_screen(LoginScreen(self.system_info, self.config))

    def route_to_post_login(self) -> None:
        """Switch to the post-login landing screen (home)."""
        client = self.client
        info = self.system_info
        if client is None or info is None:
            log.error("route_to_post_login called without client/system_info")
            return

        # Record (server, username) to determine first-time login
        profile = client.account.cached_profile
        if profile is not None and profile.username:
            self.config.mark_login_seen(client.host, profile.username)

        self.switch_screen(HomeScreen())

    def _async_exc_handler(self, loop, context: dict) -> None:
        """asyncio uncaught-task exception hook.

        Only active in `--debug` mode."""
        exc = context.get("exception")
        msg = context.get("message", str(exc))
        log.error("Unhandled async exception", exc_info=exc)
        name = type(exc).__name__ if exc is not None else "?"
        short = (msg or "")[:200]
        self.notify(
            f"[debug] {name}: {short}",
            severity="error",
            timeout=10,
        )
