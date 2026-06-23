from __future__ import annotations

from textual.widgets import Footer
from textual.widgets._footer import FooterKey


class TissueFooter(Footer):
    """Footer that shows each binding's description with a capitalised first letter
    (e.g. `search` -> `Search`, `palette` -> `Palette`), centralising the convention
    the way `TissueApp.get_key_display` centralises key display — so binding
    descriptions stay lowercase in code but read consistently in the footer.

    The 1-char gap before each description is the FooterKey's own CSS padding, so
    every description (built-in and ours) is spaced uniformly.
    """

    def compose(self):  # type: ignore[override]
        # A screen may relabel some keys by state (e.g. CTRL+F = Close/Open details);
        # it exposes those as {action: description} via footer_description_overrides.
        overrides_fn = getattr(self.screen, "footer_description_overrides", None)
        overrides: dict[str, str] = {}
        if callable(overrides_fn):
            result = overrides_fn()
            if isinstance(result, dict):
                overrides = result
        for widget in super().compose():
            if isinstance(widget, FooterKey) and widget.description:
                description = overrides.get(widget.action, widget.description)
                widget.description = description[:1].upper() + description[1:]
            yield widget
