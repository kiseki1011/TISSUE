from __future__ import annotations

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.modals.edit_issue_modal import EditIssueModal


class EditsMixin(ProjectHomeBase):
    """Issue field editing (the issue branch of the `e` action)."""

    def _edit_issue(self) -> None:
        issue_key = self._detail_state.issue_key
        if issue_key is None:
            return
        self.app.push_screen(
            EditIssueModal(
                issue_key=issue_key,
                current=dict(self._detail_state.edit_current),
                custom_fields=list(self._detail_state.custom_fields.values()),
                options_by_field=self._detail_state.field_options,
            ),
            self._on_field_edited,
        )

    def _on_field_edited(self, updated: bool | None) -> None:
        issue_key = self._detail_state.issue_key
        if not updated or issue_key is None:
            return
        self.run_worker(
            self._render_issue_detail(issue_key, focus_detail=False, force=True),
            exclusive=True,
            group="hub-detail",
        )
