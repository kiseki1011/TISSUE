"""Backwards-compatible aliases for the wiki tag color helpers. The
implementation now lives in `tissue.widgets.color_type`, shared with issue
status/priority coloring."""

from tissue.widgets.color_type import COLOR_NAMES
from tissue.widgets.color_type import chip_style as tag_chip_style
from tissue.widgets.color_type import color_fg as tag_fg
from tissue.widgets.color_type import color_hex as tag_hex

__all__ = ["COLOR_NAMES", "tag_chip_style", "tag_fg", "tag_hex"]
