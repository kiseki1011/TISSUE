from __future__ import annotations

from typing import TYPE_CHECKING

from textual.css.query import NoMatches

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _VIEW_CYCLE, _VIEW_LABELS

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary


class LayoutMixin(ProjectHomeBase):
    """Layout and focus actions for the project hub."""

    def _box1_title(self) -> str:
        segments = [
            self._title_segment(_VIEW_LABELS[view], active=view == self._ui.view_mode)
            for view in _VIEW_CYCLE
        ]
        return "[bold]\\[1][/bold] " + " | ".join(segments)

    @staticmethod
    def _title_segment(label: str, *, active: bool) -> str:
        return f"[bold]\\[{label}][/bold]" if active else f"[dim]{label}[/dim]"

    def _refresh_box_chrome(self) -> None:
        try:
            issues = self.query_one("#hub-issues-box")
        except NoMatches:
            return
        issues.border_title = self._box1_title()
        total = (
            f"Total: {self._issue_list.total} · "
            if self._ui.view_mode == "issues" and self._issue_list.total
            else ""
        )
        issues.border_subtitle = f"{total}Switch: < >"

    def _current_hub_box(self) -> str | None:
        return self._focused_ancestor_id(
            {"hub-issues-box": "1", "hub-detail": "2", "hub-activity": "3"}
        )

    def _focus_hub_box(self, box_id: str) -> None:
        if box_id == "2":
            self.action_focus_detail()
        elif box_id == "3":
            self.action_focus_activity()
        else:
            self.action_focus_issues()

    def _activity_visible(self) -> bool:
        return (
            not self._ui.expanded
            and not self._ui.activity_closed
            and not self.has_class("-no-timeline")
            and not self.has_class("-h-narrow")
        )

    def _nav_order(self) -> tuple[str, ...]:
        if self._ui.expanded:
            return ("1",)
        return ("1", "2", "3") if self._activity_visible() else ("1", "2")

    def action_nav(self, direction: str) -> None:
        order = self._nav_order()
        current = self._current_hub_box()
        if current not in order:
            self._focus_hub_box("1")
            return
        step = 1 if direction == "l" else -1
        self._focus_hub_box(order[(order.index(current) + step) % len(order)])

    def action_focus_activity(self) -> None:
        if not self._activity_visible():
            return
        panel = self._activity_panel()
        if panel is not None:
            panel.focus_scroll()

    def action_scroll_detail(self, direction: str) -> None:
        activity = self._activity_panel()
        if activity is not None and activity.has_focus_in():
            activity.scroll_activity(direction)
            return
        panel = self._detail_panel()
        if panel is None or not panel.body_has_focus():
            return
        panel.scroll_body(direction)

    def _focused_ancestor_id(self, mapping: dict[str, str]) -> str | None:
        node = self.app.focused
        while node is not None:
            if node.id in mapping:
                return mapping[node.id]
            node = node.parent
        return None

    def _restore_project_ui(self) -> None:
        saved = self.app.config.project_ui_state(self._project_key)
        self._ui.restore(saved, valid_view_modes=_VIEW_CYCLE)

    def _persist_project_ui(self) -> None:
        self.app.config.save_project_ui_state(self._project_key, self._ui.to_config())

    def action_toggle_expand(self) -> None:
        self._ui.expanded = not self._ui.expanded
        self.set_class(self._ui.expanded, "-expanded")
        self.refresh_bindings()
        self._persist_project_ui()
        self.run_worker(self._reflow_list_titles(), exclusive=True, group="hub-reflow")

    def action_toggle_activity(self) -> None:
        self._ui.activity_closed = not self._ui.activity_closed
        if self._ui.activity_closed:
            activity = self._activity_panel()
            if activity is not None and activity.has_focus_in():
                self.action_focus_detail()
        self._apply_activity_state()
        self.refresh_bindings()
        self._persist_project_ui()

    def _apply_activity_state(self) -> None:
        self.set_class(self._ui.activity_closed, "-activity-closed")

    def _open_issue_modal(
        self, issue_key: str, summary: IssueSummary | None = None
    ) -> None:
        from tissue.screens.project_home.modals.issue_detail_modal import (
            IssueDetailModal,
        )

        # A warmed cache (highlighted/prefetched row) opens instantly; the summary
        # gives cold opens a populated skeleton instead of a blank "Loading…".
        self.app.push_screen(
            IssueDetailModal(
                issue_key=issue_key,
                project_key=self._project_key,
                summary=summary or self._summary_for(issue_key),
                cached_view=self._detail_state.cache.get(issue_key),
            )
        )
