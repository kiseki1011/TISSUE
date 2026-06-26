from __future__ import annotations

from textual.css.query import NoMatches

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import (
    _AGENT_MODE_LABELS,
    _VIEW_CYCLE,
    _VIEW_LABELS,
)


class LayoutMixin(ProjectHomeBase):
    """Left-column layout controls for the [1]/[3] stack."""

    def _collapse_hint(self, box_id: str) -> str:
        return "Open: CTRL+W" if self._collapsed_box == box_id else "Close: CTRL+W"

    def _box1_title(self) -> str:
        """The [1] border title, all three views with the active one highlighted.

        `[` is escaped because the title is markup.
        """
        segments: list[str] = []
        for view in _VIEW_CYCLE:
            label = _VIEW_LABELS[view]
            if view == self._view_mode:
                if view == "issues" and self._issues_total:
                    label = f"{label} {len(self._issues)}/{self._issues_total}"
                segments.append(f"[bold]\\[{label}][/bold]")
            else:
                segments.append(f"[dim]{_VIEW_LABELS[view]}[/dim]")
        return "[bold]\\[1][/bold] " + " | ".join(segments)

    def _box3_title(self) -> str:
        """The [3] title showing both modes, active one highlighted."""
        segments: list[str] = []
        for mode in ("work", "reviews"):
            label = _AGENT_MODE_LABELS[mode]
            if mode == self._agent_mode:
                segments.append(f"[bold]\\[{label}][/bold]")
            else:
                segments.append(f"[dim]{label}[/dim]")
        return "[bold]\\[3][/bold] " + " | ".join(segments)

    def _refresh_box_chrome(self) -> None:
        """Set both stacked boxes' titles and borders from view and collapse state."""
        try:
            issues = self.query_one("#hub-issues-box")
            agent = self.query_one("#hub-agent-issues-box")
        except NoMatches:
            return
        issues.border_title = self._box1_title()
        if self._collapsed_box == "issues-box":
            issues.border_subtitle = self._collapse_hint("issues-box")
        else:
            issues.border_subtitle = (
                f"Switch: CTRL+T  ·  {self._collapse_hint('issues-box')}"
            )
        agent.border_title = self._box3_title()
        if self._collapsed_box == "agent-box":
            agent.border_subtitle = self._collapse_hint("agent-box")
        else:
            agent.border_subtitle = (
                f"Switch: CTRL+T  ·  {self._collapse_hint('agent-box')}"
            )

    def _current_hub_box(self) -> str | None:
        """Which of the three boxes ('1' list / '2' detail / '3' agent) holds focus.

        Focus sits on a widget inside the box, so walk up to its container.
        """
        node = self.app.focused
        while node is not None:
            if node.id == "hub-issues-box":
                return "1"
            if node.id == "hub-detail":
                return "2"
            if node.id == "hub-agent-issues-box":
                return "3"
            node = node.parent
        return None

    def _focus_hub_box(self, box_id: str) -> None:
        if box_id == "2":
            self.action_focus_detail()
        elif box_id == "3":
            self.action_focus_agent_issues()
        else:
            self.action_focus_issues()

    def action_nav(self, direction: str) -> None:
        """h/l move focus across [1] list ▸ [2] detail ▸ [3] agent (and wrap).

        Expanded mode hides [2] (`visibility: hidden`, can't be focused) and
        leaves it out of the loop. Without this, stepping onto it does nothing
        and h/l look reversed for the [1]↔[3] pair.
        """
        order = ("1", "3") if self._expanded else ("1", "2", "3")
        current = self._current_hub_box()
        if current not in order:
            # No box (or the now-hidden [2]) holds focus, so start the loop.
            self._focus_hub_box("1" if direction == "l" else order[-1])
            return
        step = 1 if direction == "l" else -1
        self._focus_hub_box(order[(order.index(current) + step) % len(order)])

    def action_scroll_detail(self, direction: str) -> None:
        """j/k scroll the [2] detail body when it holds focus.

        List tables bind their own j/k (row cursor) and take the key first, so
        this only runs when the detail pane (or nothing scrollable) is focused.
        """
        focused = self.app.focused
        if focused is None or focused.id != "hub-detail-main":
            return
        if direction == "down":
            focused.scroll_down()
        else:
            focused.scroll_up()

    def _focused_left_box(self) -> str | None:
        """Which stacked box ('issues-box' / 'agent-box') holds focus.

        Focus sits on the table inside the box, so walk up to it.
        """
        node = self.app.focused
        while node is not None:
            if node.id == "hub-issues-box":
                return "issues-box"
            if node.id == "hub-agent-issues-box":
                return "agent-box"
            node = node.parent
        return None

    def action_toggle_collapse(self) -> None:
        """CTRL+W collapse the focused [1]/[3] box, or restore the collapsed one.

        Does nothing unless a left box holds focus.
        """
        box_id = self._focused_left_box()
        if box_id is None:
            return
        self._collapsed_box = None if self._collapsed_box == box_id else box_id
        self._apply_collapse()

    def _apply_collapse(self) -> None:
        """Show `_collapsed_box` in the grid's row sizing and redraw the borders.

        The collapsed box gets a thin row, the other takes the rest.
        """
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
        """CTRL+F expand the [1]/[3] column to full width, hiding [2] (and back).

        While expanded there's no [2] pane, so Enter on an issue row opens its
        detail as a centered modal instead.
        """
        self._expanded = not self._expanded
        self.set_class(self._expanded, "-expanded")
        # Footer label comes from _expanded (see footer_description_overrides).
        self.refresh_bindings()

    def _open_issue_modal(self, issue_key: str) -> None:
        """Pop a read-only issue detail modal for expanded mode, where [2] is hidden."""
        from tissue.screens.project_home.modals.issue_detail_modal import (
            IssueDetailModal,
        )

        self.app.push_screen(IssueDetailModal(issue_key=issue_key))
