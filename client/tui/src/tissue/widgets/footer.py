from __future__ import annotations

from typing import TYPE_CHECKING

from textual.widgets import Footer
from textual.widgets._footer import FooterKey

if TYPE_CHECKING:
    from textual.app import ComposeResult
    from textual.widget import Widget


class TissueFooter(Footer):
    """Footer that normalizes labels and keeps the palette key inline."""

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

        # Textual docks the command palette to the far right; this app keeps it
        # inline with the other shortcuts.
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

        # The docking class also carries the far-right separator.
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
