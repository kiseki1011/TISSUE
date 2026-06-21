"""Resolve a ColorType enum name (e.g. "PINK", "ANSI_RED") to a `#rrggbb` hex via
Textual's colour parser — which, unlike Rich's, understands the ANSI names — so a
Rich `Text` can render in that colour.

Shared by wiki tags and issue status/priority colouring; the backend SSOT is
`ColorType.java` (mirrored by the generated `*_validate_enum` colour validators)."""

from __future__ import annotations

from textual.color import Color as TextualColor
from textual.color import ColorParseError as TextualColorParseError

# ColorType enum names, in enum order. SSOT: the generated colour validators
# (which mirror backend ColorType.java).
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
    # The canonical value is the ColorType enum NAME ("ansi_bright_blue",
    # "limegreen") — but also accept a raw hex ("#ff4500", already resolved from a
    # theme variable) and a display name ("ANSI Bright Blue") by trying the value
    # as-is, then with spaces turned into underscores, then with spaces removed,
    # and using whichever Textual's parser recognises.
    base = color.strip().lower()
    for candidate in (base, base.replace(" ", "_"), base.replace(" ", "")):
        try:
            return TextualColor.parse(candidate).rgb
        except TextualColorParseError:
            continue
    return None


def color_hex(color: str | None) -> str:
    """A ``#rrggbb`` hex for a ColorType enum name (Rich-renderable), or "" when
    the name is empty / not a colour Textual knows."""
    rgb = _rgb(color)
    return f"#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}" if rgb else ""


def color_fg(color: str | None) -> str:
    """A readable foreground (#000000 / #ffffff) for text drawn ON `color` —
    black on light backgrounds, white on dark — or "" when no colour."""
    rgb = _rgb(color)
    if rgb is None:
        return ""
    r, g, b = rgb
    # perceived luminance (ITU-R BT.601); > ~55% -> use black text
    return "#000000" if (0.299 * r + 0.587 * g + 0.114 * b) > 140 else "#ffffff"


def chip_style(color: str | None) -> str:
    """A Rich style ``"<fg> on <bg>"`` that fills `color` as a solid pill
    background with a readable foreground, or "" when there is no colour."""
    bg = color_hex(color)
    if not bg:
        return ""
    return f"{color_fg(color)} on {bg}"
