from __future__ import annotations

from textual import on
from textual.widgets import Button

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.issue_field_edit_modal import IssueFieldEditModal

_FIELD_BY_ID = {
    "hub-edit-title": "title",
    "hub-edit-priority": "priority",
    "hub-edit-due": "dueAt",
    "hub-edit-sp": "storyPoint",
}


class EditsMixin(ProjectHomeBase):
    """Inline field edits: a ✎ button next to each editable issue field opens a
    single-field modal; on a successful save the detail re-renders."""

    @on(Button.Pressed, ".hub-field-edit")
    def _on_field_edit(self, event: Button.Pressed) -> None:
        issue_key = self._detail_issue_key
        field = _FIELD_BY_ID.get(event.button.id or "")
        if issue_key is None or field is None:
            return
        self.app.push_screen(
            IssueFieldEditModal(
                issue_key=issue_key,
                field=field,
                current_value=self._edit_current.get(field),
            ),
            self._on_field_edited,
        )

    def _on_field_edited(self, updated: bool | None) -> None:
        issue_key = self._detail_issue_key
        if not updated or issue_key is None:
            return
        self.run_worker(
            self._render_issue_detail(issue_key, focus_detail=False),
            exclusive=True,
            group="hub-detail",
        )
