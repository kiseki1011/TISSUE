from __future__ import annotations

from typing import TypeVar

from rich.segment import Segment
from textual.strip import Strip
from textual.widgets import SelectionList
from textual.widgets._option_list import OptionDoesNotExist

SelectionType = TypeVar("SelectionType")


class FilterSelectionList(SelectionList[SelectionType]):
    """SelectionList that draws the inner toggle glyph only when a row is selected.

    Textual always draws the glyph and changes only its color between on and off,
    which is hard to tell apart. Blanking it when the row isn't selected makes an
    empty box read clearly as unchecked. Same idea as `FilterCheckbox`.
    """

    def render_line(self, y: int) -> Strip:
        strip = super().render_line(y)
        _, scroll_y = self.scroll_offset
        try:
            selection = self.get_option_at_index(scroll_y + y)
        except OptionDoesNotExist:
            return strip
        if selection.value in self._selected:
            return strip
        # The glyph is the second segment, right after the left bracket (see
        # SelectionList.render_line). Blank it, keeping its style and width.
        segments = list(strip)
        if len(segments) < 2:
            return strip
        glyph = segments[1]
        segments[1] = Segment(" " * len(glyph.text), glyph.style, glyph.control)
        return Strip(segments, strip.cell_length)
