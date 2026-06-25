from __future__ import annotations

import logging

from textual import on
from textual.containers import Horizontal, Vertical
from textual.css.query import NoMatches
from textual.widgets import (
    Button,
    Label,
)

from tissue.screens.wiki._base import WikiScreenBase
from tissue.screens.wiki.modals import (
    FilterTag,
    TagFilterModal,
)
from tissue.screens.wiki.rendering import (
    _tags_text,
)

log = logging.getLogger(__name__)


class TagFilterMixin(WikiScreenBase):
    """Adds/removes filter tags via a modal and renders the active-filter chips."""

    @on(Button.Pressed, "#wiki-filter-add-btn")
    def _on_filter_add_pressed(self, event: Button.Pressed) -> None:
        event.stop()
        selected = {tid for tid, _, _ in self._filter_tags}
        self.app.push_screen(TagFilterModal(selected), self._on_filter_picked)

    def _on_filter_picked(self, result: list[FilterTag] | None) -> None:
        if result is None:  # cancelled — leave the filter as it was
            return
        self._filter_tags = result
        self._render_filter_chips()
        # Re-run so the result list reflects the new filter immediately, and land
        # focus on it (the user just confirmed a filter and wants to browse it).
        self._rerun_search(focus_results=True)

    @on(Button.Pressed, ".wiki-filter-chip")
    def _on_filter_chip_pressed(self, event: Button.Pressed) -> None:
        event.stop()
        name = event.button.name  # carries the tag id as a string
        if name is None:
            return
        try:
            tid = int(name)
        except ValueError:
            return
        self._filter_tags = [t for t in self._filter_tags if t[0] != tid]
        self._render_filter_chips()
        self._rerun_search()

    def _render_filter_chips(self) -> None:
        """Refresh the active-filter chip band below the tree. Each chip is a
        removable button (click → drop that tag); empty filter shows a dim hint."""
        try:
            box = self.query_one("#wiki-filter-chips", Vertical)
        except NoMatches:
            return
        self.run_worker(
            self._replace_filter_chips(box), exclusive=True, group="wiki-filter-chips"
        )

    async def _replace_filter_chips(self, box: Vertical) -> None:
        await box.remove_children()
        if not self._filter_tags:
            await box.mount(Label("No filter", classes="wiki-filter-empty"))
            return
        await box.mount_all(self._pack_filter_rows())

    def _pack_filter_rows(self) -> list[Horizontal]:
        """Pack chip buttons into wrapping rows so a long filter set flows onto
        the next line instead of overflowing the narrow sidebar. Each chip is a
        pill (click → remove) carrying its tag id as the button name."""
        budget = 32  # sidebar inner width, with a small margin
        rows: list[list[Button]] = [[]]
        width = 0
        for tid, name, color in self._filter_tags:
            # Pill is " name " (len + 2) + 1 right margin between chips.
            chip_w = len(name) + 3
            if rows[-1] and width + chip_w > budget:
                rows.append([])
                width = 0
            rows[-1].append(
                Button(
                    _tags_text([(name, color)]),
                    name=str(tid),
                    classes="wiki-filter-chip",
                )
            )
            width += chip_w
        return [Horizontal(*row, classes="wiki-filter-chip-row") for row in rows]
