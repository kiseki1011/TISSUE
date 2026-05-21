from typing import TYPE_CHECKING, TypeVar

from textual.binding import Binding
from textual.css.query import NoMatches
from textual.screen import ModalScreen, Screen

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TissueScreen(Screen):
    if TYPE_CHECKING:
        app: TissueApp

    BINDINGS = [
        Binding("up", "screen_focus_previous", show=False),
        Binding("down", "screen_focus_next", show=False),
    ]

    def action_screen_focus_previous(self) -> None:
        self.focus_previous()

    def action_screen_focus_next(self) -> None:
        self.focus_next()

    # HACK: This is vibecoded. Side-effects may exist.
    def _apply_initial_breakpoints(self) -> None:
        """Attach breakpoint classes before first paint.

        By the looks of it, Textual sets these on the first resize event, which happens
        after the first render. This causes a layout flash (afterimage) when
        pushing a new screen at a small terminal size.

        Call this in on_mount.
        """
        if self.app is None:
            return
        width, height = self.app.size
        # Screen-level overrides app-level.
        h_bps = self.HORIZONTAL_BREAKPOINTS or self.app.HORIZONTAL_BREAKPOINTS or []
        v_bps = self.VERTICAL_BREAKPOINTS or self.app.VERTICAL_BREAKPOINTS or []
        # Highest matching threshold wins; mirrors Screen._get_breakpoint_classes.
        for threshold, name in sorted(h_bps, reverse=True):
            if width >= threshold:
                self.add_class(name)
                break
        for threshold, name in sorted(v_bps, reverse=True):
            if height >= threshold:
                self.add_class(name)
                break


class PostAuthScreen(TissueScreen):
    """Base for screens reachable only after login.

    Adds toggleable `ProfileSidebar` usable from any post-login
    screen. The sidebar is docked left and pushes existing screen content
    rather than overlaying it.
    """

    BINDINGS = [
        Binding("ctrl+comma", "toggle_profile_sidebar", "profile"),
    ]

    def action_toggle_profile_sidebar(self) -> None:
        # Lazy import to avoid a cycle: widget imports modals → modals import
        # TissueModal from this module.
        from tissue.widgets.profile_sidebar import ProfileSidebar

        try:
            sidebar = self.query_one(ProfileSidebar)
        except NoMatches:
            self.mount(ProfileSidebar())
            return
        sidebar.remove()


class RefreshableScreen(PostAuthScreen):
    """PostAuthScreen with the `r` refresh binding.

    Must implement `refresh_data()` with actual fetch and repopulate logic.
    Optionally override `can_refresh()` to allow/show the binding by context
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
        """Whether `r` is allowed in current context.

        Return False to hide the binding when refresh is not meaningful in the
        current context.
        """
        return True

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        if action == "refresh":
            return self.can_refresh()
        return super().check_action(action, parameters)


_T = TypeVar("_T")


class TissueModal(ModalScreen[_T]):
    if TYPE_CHECKING:
        app: TissueApp
