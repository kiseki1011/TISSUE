"""Custom commands surfaced through the textual command palette (Ctrl+P).

Each command is conditionally available based on the current screen and
authentication state, so e.g. "Logout" only shows when authenticated.
"""

from collections.abc import Callable, Iterator
from typing import TYPE_CHECKING

from textual.command import CommandPalette, DiscoveryHit, Hit, Hits, Provider
from textual.screen import Screen

from tissue.i18n.manager import i18n

if TYPE_CHECKING:
    from tissue.app import TissueApp


# TODO: 솔직히 이해 못함
type _Command = tuple[str, Callable[[], None], str]
"""(display name, callback, help text) — what each provider entry returns."""


class TissueCommands(Provider):
    if TYPE_CHECKING:
        app: TissueApp

    async def discover(self) -> Hits:
        for name, callback, help_text in self._available_commands():
            yield DiscoveryHit(name, callback, help=help_text)

    async def search(self, query: str) -> Hits:
        matcher = self.matcher(query)
        for name, callback, help_text in self._available_commands():
            score = matcher.match(name)
            if score > 0:
                yield Hit(
                    score,
                    matcher.highlight(name),
                    callback,
                    help=help_text,
                )

    def _active_screen(self) -> Screen | None:
        for screen in reversed(self.app.screen_stack):
            if not isinstance(screen, CommandPalette):
                return screen
        return None

    def _available_commands(self) -> Iterator[_Command]:
        """Yield commands available for the current app state"""
        from tissue.screens.connect import ConnectScreen
        from tissue.screens.workspace_home import WorkspaceHomeScreen

        active = self._active_screen()

        # Change server
        if not isinstance(active, ConnectScreen):
            yield (
                i18n.get("command_change_server"),
                self._change_server,
                i18n.get("command_change_server_help"),
            )

        # Home
        # TODO: must be available on WorkspaceHomeScreen, ProjectHomeScreen
        if isinstance(active, WorkspaceHomeScreen):
            yield (
                i18n.get("command_home"),
                self._go_home,
                i18n.get("command_home_help"),
            )

        # Logout
        client = self.app.client
        if client is not None and client.is_authenticated:
            yield (
                i18n.get("command_logout"),
                self._logout,
                i18n.get("command_logout_help"),
            )

    def _change_server(self) -> None:
        from tissue.screens.connect import ConnectScreen

        self.app.switch_screen(ConnectScreen(self.app.config))

    def _logout(self) -> None:
        self.app.run_worker(self._do_logout(), exclusive=True, group="logout")

    async def _do_logout(self) -> None:
        from tissue.screens.login import LoginScreen

        client = self.app.client
        if client is not None:
            await client.auth.logout()
        if self.app.system_info is not None:
            self.app.switch_screen(LoginScreen(self.app.system_info, self.app.config))

    def _go_home(self) -> None:
        self.app.config.update_state(current_workspace_key=None)
        self.app.pop_screen()
