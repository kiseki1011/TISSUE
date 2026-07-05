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
            if not self._on_home():
                yield (
                    "Home",
                    self._home,
                    "Go to the home screen",
                )
            if self._on_trash():
                yield (
                    "Project Home",
                    self._project_home,
                    "Return to this project's hub",
                )
            elif self._project_key() is not None:
                yield (
                    "Trash",
                    self._trash,
                    "View and restore this project's deleted issues",
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

    def _on_home(self) -> bool:
        """The command palette was invoked from the home screen itself."""
        from tissue.screens.home.home import HomeScreen

        return isinstance(self.screen, HomeScreen)

    def _project_key(self) -> str | None:
        """The current project's key, when the palette is open over its hub."""
        from tissue.screens.project_home.project_home import ProjectHomeScreen

        screen = self.screen
        return screen._project_key if isinstance(screen, ProjectHomeScreen) else None

    def _on_trash(self) -> bool:
        """The palette was invoked over a project's trash screen."""
        from tissue.screens.project_home.trash import TrashScreen

        return isinstance(self.screen, TrashScreen)

    def _trash(self) -> None:
        from tissue.screens.project_home.trash import TrashScreen

        project_key = self._project_key()
        if project_key is not None:
            self.app.push_screen(TrashScreen(project_key))

    def _project_home(self) -> None:
        """Trash sits on top of its project hub, so popping returns to it."""
        if self._on_trash():
            self.app.pop_screen()

    def _home(self) -> None:
        self.app.show_home()

    def _account(self) -> None:
        self.app.show_account()

    def _logout(self) -> None:
        self.app.logout()
