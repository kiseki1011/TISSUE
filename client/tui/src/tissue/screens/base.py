from typing import TYPE_CHECKING, TypeVar

from textual.binding import Binding
from textual.screen import ModalScreen, Screen

if TYPE_CHECKING:
    from tissue.app import TissueApp


class TissueScreen(Screen):
    if TYPE_CHECKING:
        app: TissueApp

    # Arrow up/down move focus between form fields (Tab/Shift+Tab still works)
    BINDINGS = [
        Binding("up", "screen_focus_previous", show=False),
        Binding("down", "screen_focus_next", show=False),
    ]

    def action_screen_focus_previous(self) -> None:
        self.focus_previous()

    def action_screen_focus_next(self) -> None:
        self.focus_next()

    # HACK: This is a vibecoded piece of code. Side-effects may exist.
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
        # Screen-level overrides app-level (per textual convention).
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


_T = TypeVar("_T")


class TissueModal(ModalScreen[_T]):
    if TYPE_CHECKING:
        app: TissueApp
