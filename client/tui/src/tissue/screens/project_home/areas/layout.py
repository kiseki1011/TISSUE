from __future__ import annotations

from textual.css.query import NoMatches

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import (
    _AGENT_MODE_LABELS,
    _VIEW_CYCLE,
    _VIEW_LABELS,
)


class LayoutMixin(ProjectHomeBase):
    """Layout and focus actions for the project hub."""

    def _collapse_hint(self, box_id: str) -> str:
        return "Open: CTRL+W" if self._ui.collapsed_box == box_id else "Close: CTRL+W"

    def _box1_title(self) -> str:
        segments = [
            self._title_segment(_VIEW_LABELS[view], active=view == self._ui.view_mode)
            for view in _VIEW_CYCLE
        ]
        return "[bold]\\[1][/bold] " + " | ".join(segments)

    def _box3_title(self) -> str:
        segments = [
            self._title_segment(
                _AGENT_MODE_LABELS[mode], active=mode == self._ui.agent_mode
            )
            for mode in ("work", "reviews")
        ]
        return "[bold]\\[3][/bold] " + " | ".join(segments)

    @staticmethod
    def _title_segment(label: str, *, active: bool) -> str:
        return f"[bold]\\[{label}][/bold]" if active else f"[dim]{label}[/dim]"

    def _refresh_box_chrome(self) -> None:
        try:
            issues = self.query_one("#hub-issues-box")
            agent = self.query_one("#hub-agent-issues-box")
        except NoMatches:
            return
        issues.border_title = self._box1_title()
        if self._ui.collapsed_box == "issues-box":
            issues.border_subtitle = self._collapse_hint("issues-box")
        else:
            total = (
                f"Total: {self._issue_list.total} · "
                if self._ui.view_mode == "issues" and self._issue_list.total
                else ""
            )
            issues.border_subtitle = (
                f"{total}Switch: CTRL+T · {self._collapse_hint('issues-box')}"
            )
        agent.border_title = self._box3_title()
        if self._ui.collapsed_box == "agent-box":
            agent.border_subtitle = self._collapse_hint("agent-box")
        else:
            agent.border_subtitle = (
                f"Switch: CTRL+T · {self._collapse_hint('agent-box')}"
            )

    def _current_hub_box(self) -> str | None:
        return self._focused_ancestor_id(
            {
                "hub-issues-box": "1",
                "hub-detail": "2",
                "hub-agent-issues-box": "3",
            }
        )

    def _focus_hub_box(self, box_id: str) -> None:
        if box_id == "2":
            self.action_focus_detail()
        elif box_id == "3":
            self.action_focus_agent_issues()
        else:
            self.action_focus_issues()

    def action_nav(self, direction: str) -> None:
        order = ("1", "3") if self._ui.expanded else ("1", "2", "3")
        current = self._current_hub_box()
        if current not in order:
            self._focus_hub_box("1" if direction == "l" else order[-1])
            return
        step = 1 if direction == "l" else -1
        self._focus_hub_box(order[(order.index(current) + step) % len(order)])

    def action_scroll_detail(self, direction: str) -> None:
        panel = self._detail_panel()
        if panel is None or not panel.body_has_focus():
            return
        if direction == "down":
            panel.scroll_body("down")
        else:
            panel.scroll_body("up")

    def _focused_left_box(self) -> str | None:
        return self._focused_ancestor_id(
            {
                "hub-issues-box": "issues-box",
                "hub-agent-issues-box": "agent-box",
            }
        )

    def _focused_ancestor_id(self, mapping: dict[str, str]) -> str | None:
        node = self.app.focused
        while node is not None:
            if node.id in mapping:
                return mapping[node.id]
            node = node.parent
        return None

    def _restore_project_ui(self) -> None:
        saved = self.app.config.project_ui_state(self._project_key)
        self._ui.restore(
            saved,
            valid_view_modes=_VIEW_CYCLE,
            valid_agent_modes=_AGENT_MODE_LABELS,
        )

    def _persist_project_ui(self) -> None:
        self.app.config.save_project_ui_state(self._project_key, self._ui.to_config())

    def action_toggle_collapse(self) -> None:
        box_id = self._focused_left_box()
        if box_id is None:
            return
        self._ui.collapsed_box = None if self._ui.collapsed_box == box_id else box_id
        self._persist_project_ui()
        self._apply_collapse()

    def _apply_collapse(self) -> None:
        try:
            grid = self.query_one("#hub-grid")
        except NoMatches:
            return
        grid.remove_class("-collapse-top", "-collapse-bottom")
        if self._ui.collapsed_box == "issues-box":
            grid.add_class("-collapse-top")
        elif self._ui.collapsed_box == "agent-box":
            grid.add_class("-collapse-bottom")
        self._refresh_box_chrome()

    def action_toggle_expand(self) -> None:
        self._ui.expanded = not self._ui.expanded
        self.set_class(self._ui.expanded, "-expanded")
        self.refresh_bindings()
        self._persist_project_ui()

    def _open_issue_modal(self, issue_key: str) -> None:
        from tissue.screens.project_home.modals.issue_detail_modal import (
            IssueDetailModal,
        )

        self.app.push_screen(IssueDetailModal(issue_key=issue_key))
