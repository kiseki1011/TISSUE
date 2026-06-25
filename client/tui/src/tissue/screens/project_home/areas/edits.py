from __future__ import annotations

from textual import on
from textual.widgets import Button

from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.modals.custom_field_edit_modal import (
    CustomFieldEditModal,
)
from tissue.screens.project_home.modals.description_edit_modal import (
    DescriptionEditModal,
)
from tissue.screens.project_home.modals.issue_field_edit_modal import (
    IssueFieldEditModal,
)

_FIELD_BY_ID = {
    "hub-edit-title": "title",
    "hub-edit-priority": "priority",
    "hub-edit-due": "dueAt",
    "hub-edit-sp": "storyPoint",
}


class EditsMixin(ProjectHomeBase):
    """Inline field edits: a ✎ button next to each editable issue field opens a
    single-field modal; on a successful save the detail re-renders. Common fields
    use `IssueFieldEditModal`; custom fields use the type-specific
    `CustomFieldEditModal`."""

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

    @on(Button.Pressed, ".hub-desc-edit")
    def _on_description_edit(self, event: Button.Pressed) -> None:
        event.stop()
        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        self.app.push_screen(
            DescriptionEditModal(
                issue_key=issue_key,
                current_content=self._edit_current.get("content"),
            ),
            self._on_field_edited,
        )

    @on(Button.Pressed, ".hub-cf-edit")
    def _on_custom_field_edit(self, event: Button.Pressed) -> None:
        issue_key = self._detail_issue_key
        button_id = event.button.id or ""
        try:
            field_id = int(button_id.removeprefix("hub-cf-edit-"))
        except ValueError:
            return
        field = self._detail_custom_fields.get(field_id)
        if issue_key is None or field is None:
            return
        self.app.push_screen(
            CustomFieldEditModal(
                issue_key=issue_key,
                field=field,
                options=self._detail_field_options.get(field_id, []),
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
