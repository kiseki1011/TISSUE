from collections.abc import Callable, Iterator
from typing import TYPE_CHECKING

from textual.command import DiscoveryHit, Hit, Hits, Provider

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
        """Commands available for the current app state."""
        client = self.app.client
        if client is not None and client.is_authenticated:
            yield (
                "Home",
                self._home,
                "Go to the home screen",
            )
            yield (
                "Projects",
                self._projects,
                "Browse projects",
            )
            yield (
                "Wiki",
                self._wiki,
                "Browse the wiki",
            )
            yield (
                "Account",
                self._account,
                "View and manage your account",
            )
            yield (
                "Logout",
                self._logout,
                "Sign out and return to the login screen",
            )

    def _home(self) -> None:
        self.app.show_home()

    def _projects(self) -> None:
        self.app.show_projects()

    def _wiki(self) -> None:
        self.app.show_wiki()

    def _account(self) -> None:
        self.app.show_account()

    def _logout(self) -> None:
        self.app.logout()
