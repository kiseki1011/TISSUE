"""Custom commands surfaced through the textual command palette (Ctrl+P).

Each command is conditionally available based on the current screen and
authentication state, so e.g. "Logout" only shows when authenticated.
"""

from collections.abc import Callable, Iterator
from typing import TYPE_CHECKING

from textual.command import DiscoveryHit, Hit, Hits, Provider

from tissue.i18n.manager import i18n

if TYPE_CHECKING:
    from tissue.app import TissueApp


type _Command = tuple[str, Callable[[], None], str]


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

    def _available_commands(self) -> Iterator[_Command]:
        """Yield commands available for the current app state"""
        # Logout
        client = self.app.client
        if client is not None and client.is_authenticated:
            yield (
                i18n.get("command_logout"),
                self._logout,
                i18n.get("command_logout_help"),
            )

    def _logout(self) -> None:
        self.app.logout()
