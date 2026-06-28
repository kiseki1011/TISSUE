from __future__ import annotations

import logging

from textual.css.query import NoMatches
from textual.widgets import Input

from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.panels import MyWorkPanel, ProjectsPanel, SearchResultsPanel

log = logging.getLogger(__name__)


class NavigationMixin(HomeScreenBase):
    """Box focus and navigation via number/h/l jumps plus post-load focus."""

    def _focus_after_load(self) -> None:
        focused = self.focused
        if focused is None or focused.id == "dashboard-search":
            # _focus_box previews the highlighted row's detail, so the first
            # project shows at once instead of waiting for a cursor move.
            self._focus_box(ProjectsPanel.BOX_ID)

    def action_focus_search(self) -> None:
        try:
            self.query_one("#dashboard-search", Input).focus()
        except NoMatches:
            pass

    def action_leave_search(self) -> None:
        """Esc in the search box returns focus to the boxes, a no-op elsewhere.

        Moving focus off the Input lets the box-jump digits (1/2/3) act again
        instead of being typed into the search field.
        """
        if self.focused is not None and self.focused.id == "dashboard-search":
            self._focus_box(self._BOX_IDS[0])

    def action_focus_box(self, box_id: str) -> None:
        self._focus_box(box_id)

    def action_nav(self, direction: str) -> None:
        """Cycle focus through the boxes.

        direction:
            - l -> next
            - h -> previous
        """
        order = self._BOX_IDS
        current = self._current_box_id()
        if current not in order:
            self._focus_box(order[0] if direction == "l" else order[-1])
            return
        step = 1 if direction == "l" else -1
        self._focus_box(order[(order.index(current) + step) % len(order)])

    def _focus_box(self, box_id: str) -> None:
        box = self._dashboard_box(box_id)
        if box is None:
            return
        row = box.focus_table_or_box()
        if row is not None:
            # Focusing alone doesn't move the cursor, so RowHighlighted won't fire.
            # Drive the already-highlighted row's detail so it shows immediately on
            # every box switch, not only after a cursor move or Enter.
            self._preview_focused_row(box_id, row)

    def _preview_focused_row(self, box_id: str, row: int) -> None:
        """Render the detail for the focused box's highlighted row."""
        if box_id == ProjectsPanel.BOX_ID:
            self._select_project(row)
        elif box_id == MyWorkPanel.BOX_ID:
            self._select_mywork(row)
        elif box_id == SearchResultsPanel.BOX_ID:
            self._select_searched(row)

    def _current_box_id(self) -> str | None:
        """Which box contains the currently focused widget."""
        node = self.focused
        while node is not None:
            if node.id in self._BOX_IDS:
                return node.id
            node = node.parent
        return None
