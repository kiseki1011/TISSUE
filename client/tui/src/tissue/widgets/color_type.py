"""Turn a ColorType enum name (e.g. "PINK", "ANSI_RED") into a `#rrggbb` hex.

Uses Textual's color parser because it understands the ANSI names that Rich
does not.
"""

from __future__ import annotations

from textual.color import Color as TextualColor
from textual.color import ColorParseError as TextualColorParseError

# ColorType enum names, in enum order, matching the generated color validators
# that mirror the backend ColorType.java enum (the one place colors are defined).
COLOR_NAMES: list[str] = [
    "ANSI_BLACK",
    "ANSI_RED",
    "ANSI_GREEN",
    "ANSI_YELLOW",
    "ANSI_BLUE",
    "ANSI_MAGENTA",
    "ANSI_CYAN",
    "ANSI_WHITE",
    "ANSI_BRIGHT_BLACK",
    "ANSI_BRIGHT_RED",
    "ANSI_BRIGHT_GREEN",
    "ANSI_BRIGHT_YELLOW",
    "ANSI_BRIGHT_BLUE",
    "ANSI_BRIGHT_MAGENTA",
    "ANSI_BRIGHT_CYAN",
    "ANSI_BRIGHT_WHITE",
    "PINK",
    "MAROON",
    "RED",
    "ORANGERED",
    "DARKORANGE",
    "LIMEGREEN",
    "LIGHTGREEN",
    "LIGHTYELLOW",
    "MEDIUMBLUE",
    "MIDNIGHTBLUE",
    "INDIGO",
    "MAGENTA",
    "BROWN",
    "TAN",
]


def _rgb(color: str | None) -> tuple[int, int, int] | None:
    if not color:
        return None
    # The usual value is the ColorType enum NAME ("ansi_bright_blue", "limegreen"),
    # but also accept a raw hex ("#ff4500", from a theme variable) and a display
    # name ("ANSI Bright Blue"). Try the value three ways and use whichever
    # Textual's parser recognizes.
    #   - as-is
    #   - spaces turned into underscores
    #   - spaces removed
    base = color.strip().lower()
    for candidate in (base, base.replace(" ", "_"), base.replace(" ", "")):
        try:
            return TextualColor.parse(candidate).rgb
        except TextualColorParseError:
            continue
    return None


def color_hex(color: str | None) -> str:
    """A ``#rrggbb`` hex for a ColorType enum name.

    Returns "" when it is empty or not a color Textual knows.
    """
    rgb = _rgb(color)
    return f"#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}" if rgb else ""


def color_fg(color: str | None) -> str:
    """A readable foreground (#000000 / #ffffff) for text drawn ON `color`.

    Black on light backgrounds, white on dark, or "" when there is no color.
    """
    rgb = _rgb(color)
    if rgb is None:
        return ""
    r, g, b = rgb
    # perceived luminance (ITU-R BT.601), above ~55% means use black text
    return "#000000" if (0.299 * r + 0.587 * g + 0.114 * b) > 140 else "#ffffff"


def chip_style(color: str | None) -> str:
    """A Rich style ``"<fg> on <bg>"`` filling `color` as a solid pill with a
    readable foreground, or "" when there is no color."""
    bg = color_hex(color)
    if not bg:
        return ""
    return f"{color_fg(color)} on {bg}"
