"""Shared wiki-tag colour helpers.

The server stores a tag's colour as a ColorType enum name (e.g. "PINK",
"ANSI_RED", "INDIGO"). Textual's colour parser understands every one of those
names — including the ANSI ones, which Rich's parser does not — so we resolve
the name there and emit a plain ``#rrggbb`` hex that a Rich ``Text`` (used to
render the tag) can apply.
"""

from __future__ import annotations

from textual.color import Color as TextualColor
from textual.color import ColorParseError as TextualColorParseError

# ColorType enum names, in enum order. SSOT: the generated
# AttachWikiTagRequest.color_validate_enum (mirrors backend ColorType.java).
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
    # "limegreen"). Be tolerant, though, of a server that still sends the display
    # name ("ANSI Bright Blue", "Lime Green") — try the value as-is, then with
    # spaces turned into underscores, then with spaces removed, and use whichever
    # Textual's parser recognises.
    base = color.strip().lower()
    for candidate in (base, base.replace(" ", "_"), base.replace(" ", "")):
        try:
            return TextualColor.parse(candidate).rgb
        except TextualColorParseError:
            continue
    return None


def tag_hex(color: str | None) -> str:
    """A ``#rrggbb`` hex for a ColorType enum name (Rich-renderable), or "" when
    the name is empty / not a colour Textual knows."""
    rgb = _rgb(color)
    return f"#{rgb[0]:02x}{rgb[1]:02x}{rgb[2]:02x}" if rgb else ""


def tag_fg(color: str | None) -> str:
    """A readable foreground (#000000 / #ffffff) for text drawn ON the tag's
    colour — black on light backgrounds, white on dark — or "" when no colour."""
    rgb = _rgb(color)
    if rgb is None:
        return ""
    r, g, b = rgb
    # perceived luminance (ITU-R BT.601); > ~55% -> use black text
    return "#000000" if (0.299 * r + 0.587 * g + 0.114 * b) > 140 else "#ffffff"


def tag_chip_style(color: str | None) -> str:
    """A Rich style ``"<fg> on <bg>"`` that fills a tag's background with its
    colour (so it reads as a solid pill), or "" when the tag has no colour."""
    bg = tag_hex(color)
    if not bg:
        return ""
    return f"{tag_fg(color)} on {bg}"
