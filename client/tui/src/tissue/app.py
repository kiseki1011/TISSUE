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
from tissue.commands import TissueCommands
from tissue.config.manager import ConfigManager
from tissue.domain.auth.token_store import create_token_store
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

    # wait interval before showing the connecting screen
    INITIAL_PING_TIMEOUT = 0.5

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

        client = TissueClient(
            host=saved_url,
            token_store=self.token_store,
            on_session_expired=self._on_session_expired,
        )
        try:
            system_info = await asyncio.wait_for(
                client.ping(), timeout=self.INITIAL_PING_TIMEOUT
            )
        # If unreachable within INITIAL_PING_TIMEOUT, retry with a spinner.
        except (TimeoutError, TissueApiError) as e:
            log.debug("Initial ping failed, showing connecting screen: %s", e)
            await client.close()
            self.push_screen(ConnectingScreen(saved_url, self.config))
            return

        self.client = client
        self.system_info = system_info
        self.config.update_state(last_connected_at=datetime.now().astimezone())

        # Restore the previous session from a stored token.
        saved_token = self.token_store.load(saved_url)
        if saved_token is not None:
            try:
                if await client.auth.restore_session(saved_token):
                    self._route_to_last_screen()
                    return
            except TissueApiError as e:
                log.debug("Session restore (login) failed: %s", e)
            client.clear_tokens()

        self.push_screen(LoginScreen(system_info, self.config))

    def _route_to_last_screen(self) -> None:
        """Restore the screen the user was on before they closed the app.

        When that was a project hub, push it straight onto a (covered) Home in the
        same tick, so the hub is what paints first (no dashboard flash) while Home
        stays underneath for back-navigation.
        """
        if self.client is None:
            return
        self.push_screen(HomeScreen())
        self._push_last_project()

    def _push_last_project(self) -> None:
        """Stack the last-open project hub on top of the dashboard, if any.

        Pushed without a flash since it's added before the dashboard paints. The
        hub tolerates a now-inaccessible project (its loads fall back to empty),
        and navigating Home from the palette clears the saved key.
        """
        key = self.config.state.current_project_key
        if not key:
            return
        from tissue.screens.project_home.project_home import ProjectHomeScreen

        self.push_screen(ProjectHomeScreen(key))

    async def on_unmount(self) -> None:
        if self.client is not None:
            await self.client.close()
            self.client = None

    def change_theme(self, theme: str) -> None:
        self.theme = theme
        self.config.update_settings(theme=theme)

    def get_key_display(self, binding: Binding) -> str:
        """Format a shortcut for the footer, uppercase and spelled out.

        Changes how the key reads, not what's bound. By case:
            - with ctrl/alt/shift -> all uppercase ("ctrl+p" -> "CTRL+P")
            - a lone letter -> uppercase ("r" -> "R")
        """
        if binding.key_display:
            return binding.key_display.upper()
        modifiers, key = binding.parse_key()
        key = format_key(key)
        if modifiers:
            return "+".join([m.upper() for m in modifiers] + [key.upper()])
        return key.upper() if len(key) == 1 else key

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
        # Landing on the dashboard means no project is open. Record it now, up
        # front, so quitting before the dashboard finishes loading still restores
        # the dashboard rather than the project we just left.
        self.config.set_last_project(None)
        self._navigate_to_screen(HomeScreen())

    def _navigate_to_screen(self, screen: Screen) -> None:
        """Collapse drill-in screens/modals for palette navigation, then show `screen`.

        Pop everything stacked on top of the base content screen, then replace
        that content screen with `screen`. We never pop past index 1. The App's
        auto-created default screen at index 0 was never `push_screen`-ed, so it
        has no result callback and `switch_screen` would raise trying to pop one.
        """
        while len(self.screen_stack) > 2:
            self.pop_screen()
        if len(self.screen_stack) > 1:
            self.switch_screen(screen)
        else:  # defensive, nothing pushed yet (shouldn't happen post-login)
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

        Reuses `_navigate_to_screen` so the same stack-collapse rule applies.
        Never pop past the base screen before `switch_screen`, which would
        IndexError on the callback-less default screen.
        """
        if self.system_info is None:
            return
        self._navigate_to_screen(LoginScreen(self.system_info, self.config))

    def _on_session_expired(self) -> None:
        """Route back to login once with a notice when a token refresh fails.

        Avoids leaving the user stuck on a screen.
        """
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

        # Record (server, username) so we can detect first-time login.
        profile = client.account.cached_profile
        if profile is not None and profile.username:
            self.config.mark_login_seen(client.host, profile.username)

        # A fresh login lands on the dashboard, not a restored project.
        self.config.set_last_project(None)
        self.switch_screen(HomeScreen())

    def _handle_exception(self, error: Exception) -> None:
        # Log the traceback before Textual tears the app down. It otherwise only
        # prints to the console, leaving no trace once the app is gone.
        log.critical("Unhandled exception, app is exiting", exc_info=error)
        super()._handle_exception(error)

    async def _flush_next_callbacks(self) -> None:
        # A stray CancelledError from a cancelled widget swap can leak out here and
        # quietly kill the app. Swallow it.
        try:
            await super()._flush_next_callbacks()
        except asyncio.CancelledError:
            task = asyncio.current_task()
            if task is not None and task.cancelling() > 0:
                raise
            log.warning("Recovered a stray CancelledError from a next-callback")

    def _async_exc_handler(self, loop, context: dict) -> None:
        """asyncio uncaught-task exception hook, only active in `--debug` mode."""
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
