from __future__ import annotations

from typing import TYPE_CHECKING

from textual.widgets import Footer
from textual.widgets._footer import FooterKey

if TYPE_CHECKING:
    from textual.app import ComposeResult
    from textual.widget import Widget


class TissueFooter(Footer):
    """Footer with two house conventions over Textual's default.

    - Every binding's description shows with a capitalized first letter (`search`
      -> `Search`, `palette` -> `Palette`), matching how `TissueApp.get_key_display`
      handles key display, so descriptions stay lowercase in code but read
      consistently. A screen may relabel keys by state via
      `footer_description_overrides()` (e.g. CTRL+F = Close/Open details).
    - The command-palette key flows inline with our shortcuts (right after Quit)
      instead of being pinned to the far right. Textual docks it right via the
      `-command-palette` class, so we drop that class and re-position it.

    The 1-char gap before each description is the FooterKey's own CSS padding, so
    every description (built-in and ours) is spaced uniformly.
    """

    def compose(self) -> ComposeResult:
        overrides_fn = getattr(self.screen, "footer_description_overrides", None)
        overrides: dict[str, str] = {}
        if callable(overrides_fn):
            result = overrides_fn()
            if isinstance(result, dict):
                overrides = result

        def capitalize(widget: Widget) -> None:
            if isinstance(widget, FooterKey) and widget.description:
                description = overrides.get(widget.action, widget.description)
                widget.description = description[:1].upper() + description[1:]

        # Pull the command-palette key out of Textual's docked-right slot so it can
        # sit inline after Quit instead of pinned far right.
        palette: FooterKey | None = None
        rest: list[Widget] = []
        for widget in super().compose():
            if isinstance(widget, FooterKey) and widget.has_class("-command-palette"):
                palette = widget
            else:
                rest.append(widget)
        for widget in rest:
            capitalize(widget)

        if palette is None:
            yield from rest
            return

        # Drop the docking class (it also carries the far-right separator), then
        # insert the palette right after the Quit key, or at the end if this
        # screen has no Quit binding shown.
        palette.remove_class("-command-palette")
        capitalize(palette)
        inserted = False
        for widget in rest:
            yield widget
            if (
                not inserted
                and isinstance(widget, FooterKey)
                and widget.action == "app.quit"
            ):
                yield palette
                inserted = True
        if not inserted:
            yield palette
