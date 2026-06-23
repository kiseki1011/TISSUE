from __future__ import annotations

from textual.css.query import NoMatches

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _VIEW_CYCLE, _VIEW_LABELS


class LayoutMixin(ProjectHomeBase):
    """Left-column layout controls for the [1]/[3] stack.

    CTRL+W collapses the focused box to a sliver (the other box claims the height);
    pressing it again restores the 1:1 split. Each box's border subtitle shows the
    matching Close/Open hint, and [1] also carries its CTRL+T cycle hint."""

    def _collapse_hint(self, box_id: str) -> str:
        return "Open: CTRL+W" if self._collapsed_box == box_id else "Close: CTRL+W"

    def _box1_title(self) -> str:
        """The [1] border title showing all three views — the active one bold and
        wrapped in [brackets], the others dimmed and un-bold. No explicit colour, so
        each segment follows the box's border-title-color (primary, or accent on
        focus) and `[dim]` just dims whichever is active. Markup, so `[` is escaped."""
        segments: list[str] = []
        for view in _VIEW_CYCLE:
            label = _VIEW_LABELS[view]
            if view == self._view_mode:
                segments.append(f"[bold]\\[{label}][/bold]")
            else:
                segments.append(f"[dim]{label}[/dim]")
        return "[bold]\\[1][/bold] " + " | ".join(segments)

    def _refresh_box_chrome(self) -> None:
        """Set both stacked boxes' border title + subtitle from the current view and
        collapse state. [1] lists all three views (active bracketed) and hints CTRL+T
        + Close/Open; [3] is fixed and hints only Close/Open."""
        try:
            issues = self.query_one("#hub-issues-box")
            agent = self.query_one("#hub-agent-issues-box")
        except NoMatches:
            return
        issues.border_title = self._box1_title()
        if self._collapsed_box == "issues-box":
            # Collapsed: the list is hidden, so just show how to reopen it.
            issues.border_subtitle = self._collapse_hint("issues-box")
        else:
            issues.border_subtitle = (
                f"Switch: CTRL+T  ·  {self._collapse_hint('issues-box')}"
            )
        agent.border_title = "[bold]\\[3] Agent Work[/bold]"
        agent.border_subtitle = self._collapse_hint("agent-box")

    def _focused_left_box(self) -> str | None:
        """Which stacked box ('issues-box' / 'agent-box') currently holds focus —
        focus sits on the table inside it, so walk up to the box."""
        node = self.app.focused
        while node is not None:
            if node.id == "hub-issues-box":
                return "issues-box"
            if node.id == "hub-agent-issues-box":
                return "agent-box"
            node = node.parent
        return None

    def action_toggle_collapse(self) -> None:
        """CTRL+W: collapse the focused [1]/[3] box (or restore it if it's already
        the collapsed one). No-op unless a left box holds focus."""
        box = self._focused_left_box()
        if box is None:
            return
        self._collapsed_box = None if self._collapsed_box == box else box
        self._apply_collapse()

    def _apply_collapse(self) -> None:
        """Reflect `_collapsed_box` in the grid's row sizing (a sliver row for the
        collapsed box; the other takes the rest) and refresh the box chrome."""
        try:
            grid = self.query_one("#hub-grid")
        except NoMatches:
            return
        grid.remove_class("-collapse-top", "-collapse-bottom")
        if self._collapsed_box == "issues-box":
            grid.add_class("-collapse-top")
        elif self._collapsed_box == "agent-box":
            grid.add_class("-collapse-bottom")
        self._refresh_box_chrome()

    def action_toggle_expand(self) -> None:
        """CTRL+F: expand the [1]/[3] column to full width, hiding [2] (and back).
        While expanded, Enter on an issue row opens its detail as a centered modal
        (there's no [2] pane to render into)."""
        self._expanded = not self._expanded
        self.set_class(self._expanded, "-expanded")
        # The footer label flips between 'Close details' and 'Open details' based on
        # _expanded (footer_description_overrides); re-render it now.
        self.refresh_bindings()

    def _ensure_not_expanded(self) -> None:
        """Leave full-width mode so the [2] pane is visible again. Used when
        selecting a sprint/member (which render into [2] and have no detail modal),
        so an Enter there isn't a feedback-less no-op against the hidden pane."""
        if self._expanded:
            self._expanded = False
            self.set_class(False, "-expanded")

    def _open_issue_modal(self, issue_key: str) -> None:
        """Pop a read-only issue detail modal (expanded mode, where [2] is hidden)."""
        from tissue.screens.project_home.issue_detail_modal import IssueDetailModal

        self.app.push_screen(IssueDetailModal(issue_key=issue_key))
