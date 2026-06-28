from typing import TYPE_CHECKING, TypeVar

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import VerticalScroll
from textual.css.query import NoMatches
from textual.screen import ModalScreen, Screen

from tissue.widgets.footer import TissueFooter

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TissueScreen(Screen):
    if TYPE_CHECKING:
        app: TissueApp

    BINDINGS = [
        Binding("ctrl+o", "app.options", "options"),
        Binding("ctrl+q", "app.quit", "quit", priority=True),
        Binding("up", "screen_focus_previous", show=False),
        Binding("down", "screen_focus_next", show=False),
    ]

    def action_screen_focus_previous(self) -> None:
        self.focus_previous()

    def action_screen_focus_next(self) -> None:
        self.focus_next()

    def _apply_initial_breakpoints(self) -> None:
        """Attach breakpoint classes before first paint.

        Textual only sets these on the first resize event, which happens after
        the first paint. Without applying them up front the layout flashes once
        when pushing a new screen at a small terminal size. Call this in
        on_mount.
        """
        if self.app is None:
            return
        width, height = self.app.size
        # Screen-level overrides app-level.
        horizontal_breakpoints = (
            self.HORIZONTAL_BREAKPOINTS or self.app.HORIZONTAL_BREAKPOINTS or []
        )
        vertical_breakpoints = (
            self.VERTICAL_BREAKPOINTS or self.app.VERTICAL_BREAKPOINTS or []
        )
        # Highest matching threshold wins, mirrors Screen._get_breakpoint_classes.
        for threshold, name in sorted(horizontal_breakpoints, reverse=True):
            if width >= threshold:
                self.add_class(name)
                break
        for threshold, name in sorted(vertical_breakpoints, reverse=True):
            if height >= threshold:
                self.add_class(name)
                break


class PostAuthScreen(TissueScreen):
    """Base for screens reachable only after login.

    Wraps subclass content (yielded from `compose_content`) with the shared
    chrome, a TopBar docked on top and the Footer at the bottom. The TopBar
    lives in `compose` (not `on_mount`) so it survives re-mounts/recomposes and
    avoids Textual's per-MRO-class `on_mount` dispatch.

    Subclasses implement `compose_content` (not `compose`) and put their main
    content in a `Container(id="screen-body")`.
    """

    DEFAULT_CSS = """
    PostAuthScreen #screen-body {
        width: 100%;
        height: 1fr;
    }
    """

    # Breadcrumb shown in the TopBar, overridden directly or via top_bar_breadcrumb().
    SCREEN_TITLE = ""

    def compose(self) -> ComposeResult:
        from tissue.widgets.top_bar import TopBar

        yield TopBar(self.top_bar_breadcrumb())
        yield from self.compose_content()
        yield TissueFooter()

    def compose_content(self) -> ComposeResult:
        """Subclasses yield their main content here.

        Wrap it in `Container(id="screen-body")` so it fills the space between
        the TopBar and the Footer.
        """
        yield from ()

    def top_bar_breadcrumb(self) -> str:
        return self.SCREEN_TITLE


class RefreshableScreen(PostAuthScreen):
    """PostAuthScreen with the `r` refresh binding.

    Must implement `refresh_data()` with actual fetch and repopulate logic.
    Optionally override `can_refresh()` to show the binding by context
    (only on certain tabs or only when a data widget is present).
    """

    BINDINGS = [
        Binding("r", "refresh", "refresh"),
    ]

    async def action_refresh(self) -> None:
        if not self.can_refresh():
            return
        await self.refresh_data()

    async def refresh_data(self) -> None:
        """Override with the actual refresh logic."""
        raise NotImplementedError("Subclass must implement refresh_data()")

    def can_refresh(self) -> bool:
        """Whether `r` is allowed, return `False` to hide the binding."""
        return True

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        if action == "refresh":
            return self.can_refresh()
        return super().check_action(action, parameters)


_T = TypeVar("_T")


class TissueModal(ModalScreen[_T]):
    if TYPE_CHECKING:
        app: TissueApp


class ScrollableModal(TissueModal[_T]):
    """A read-only modal whose body VerticalScroll scrolls with j/k."""

    BINDINGS = [
        Binding("j", "scroll_body('down')", show=False),
        Binding("k", "scroll_body('up')", show=False),
    ]

    def action_scroll_body(self, direction: str) -> None:
        try:
            scroller = self.query(VerticalScroll).first()
        except NoMatches:
            return
        if direction == "down":
            scroller.scroll_down()
        else:
            scroller.scroll_up()
