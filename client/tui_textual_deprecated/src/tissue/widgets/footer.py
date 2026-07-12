from __future__ import annotations

from typing import TYPE_CHECKING

from textual.containers import HorizontalGroup
from textual.widgets import Footer
from textual.widgets._footer import FooterKey

if TYPE_CHECKING:
    from textual.app import ComposeResult
    from textual.widget import Widget

# App-level chrome shown on the footer's first row; everything else drops to the
# second row.
_COMMON_ACTIONS = frozenset({"app.options", "app.quit", "refresh", "action_refresh"})


class TissueFooter(Footer):
    """Two-row footer: common shortcuts on top, screen-specific ones below."""

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

        common: list[Widget] = []
        rest: list[Widget] = []
        for widget in super().compose():
            is_palette = isinstance(widget, FooterKey) and widget.has_class(
                "-command-palette"
            )
            if is_palette:
                # Undock it so it flows inline with the other common keys.
                widget.remove_class("-command-palette")
            capitalize(widget)
            is_common = is_palette or (
                isinstance(widget, FooterKey) and widget.action in _COMMON_ACTIONS
            )
            (common if is_common else rest).append(widget)

        if common:
            yield HorizontalGroup(*common, classes="footer-row")
        if rest:
            yield HorizontalGroup(*rest, classes="footer-row")
