import asyncio
import logging
from collections.abc import Iterable
from datetime import datetime

from textual.app import App, SystemCommand
from textual.binding import Binding
from textual.keys import format_key
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

log = logging.getLogger(__name__)


class TissueApp(App):
    CSS_PATH = "global.tcss"

    COMMANDS = App.COMMANDS | {TissueCommands}

    def __init__(self, *, debug: bool = False, connect_url: str | None = None) -> None:
        super().__init__()
        self.animation_level = "none"
        self._debug = debug
        self._connect_url = connect_url
        self.config = ConfigManager()
        self.theme = self.config.settings.theme
        self.token_store = create_token_store()
        self.client: TissueClient | None = None
        self.system_info: SystemInfoDetails | None = None
        self._session_expiring = False

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
        client = TissueClient(
            host=saved_url,
            token_store=self.token_store,
            on_session_expired=self._on_session_expired,
        )
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

    def get_key_display(self, binding: Binding) -> str:
        """Spell out the ctrl modifier ("ctrl+q") instead of the caret ("^q").

        Mirrors Textual's default but skips its ctrl→caret conversion.
        """
        if binding.key_display:
            return binding.key_display
        modifiers, key = binding.parse_key()
        return "+".join([*modifiers, format_key(key)])

    _HIDDEN_SYSTEM_COMMANDS = ("theme", "maximize", "screenshot", "keys", "quit")

    def get_system_commands(self, screen: Screen) -> Iterable[SystemCommand]:
        """Hide built-in commands we don't want to expose."""
        for command in super().get_system_commands(screen):
            title = command.title.lower()
            if any(keyword in title for keyword in self._HIDDEN_SYSTEM_COMMANDS):
                continue
            yield command

    def action_options(self) -> None:
        if isinstance(self.screen, OptionModal):
            return
        self.push_screen(OptionModal(self.config))

    def show_account(self) -> None:
        """Open the account panel from the command palette."""
        from tissue.screens.account.account_modal import AccountModal

        if isinstance(self.screen, AccountModal):
            return
        self.push_screen(AccountModal())

    def show_home(self) -> None:
        """Navigate to the home screen (command palette)."""
        self._navigate_to_screen(HomeScreen())

    def show_wiki(self) -> None:
        """Navigate to the wiki screen (command palette)."""
        from tissue.screens.wiki.wiki import WikiScreen

        self._navigate_to_screen(WikiScreen())

    def _navigate_to_screen(self, screen: Screen) -> None:
        """Palette navigation: collapse drill-in screens/modals, then show `screen`.

        Pop everything stacked on top of the base content screen, then replace
        that content screen with `screen`. We never pop past index 1: the App's
        auto-created default screen at index 0 was never `push_screen`-ed, so it
        has no result callback and `switch_screen` would raise trying to pop one.
        """
        while len(self.screen_stack) > 2:
            self.pop_screen()
        if len(self.screen_stack) > 1:
            self.switch_screen(screen)
        else:  # defensive: nothing pushed yet (shouldn't happen post-login)
            self.push_screen(screen)

    def logout(self) -> None:
        """Log out the current session and return to LoginScreen."""
        self.run_worker(self._do_logout(), exclusive=True, group="logout")

    async def _do_logout(self) -> None:
        if self.client is not None:
            await self.client.auth.logout()
        self._reset_to_login()

    def _reset_to_login(self) -> None:
        """Collapse any stacked screens/modals, then land on the login screen.

        Reuses `_navigate_to_screen` so the same stack-collapse rule applies:
        never pop past the base screen before `switch_screen` (which would
        IndexError on the callback-less default screen).
        """
        if self.system_info is None:
            return
        self._navigate_to_screen(LoginScreen(self.system_info, self.config))

    def _on_session_expired(self) -> None:
        """Called when a token refresh fails. Route back to login once, with a notice,
        instead of leaving the user stuck on a screen."""
        if self.system_info is None or self._session_expiring:
            return
        self._session_expiring = True
        self.notify(
            "Your session expired. Please log in again.",
            severity="warning",
            timeout=8,
        )
        self._reset_to_login()

    def route_to_post_login(self) -> None:
        """Switch to the post-login landing screen (home)."""
        client = self.client
        info = self.system_info
        if client is None or info is None:
            log.error("route_to_post_login called without client/system_info")
            return

        self._session_expiring = False

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
