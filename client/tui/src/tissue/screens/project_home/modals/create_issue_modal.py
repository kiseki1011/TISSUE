from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.widgets import Button, Input, Select, Static, TextArea

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.screens.project_home.modals.create_issue_form import (
    DEFAULT_PRIORITY,
    PRIORITIES,
    CreateIssueFormValues,
    collect_custom_fields,
    custom_field_inputs,
    hierarchy_of,
    member_choices,
    parent_candidate_labels,
    parent_hierarchy_of,
    parent_required,
)
from tissue.widgets.custom_field_input import CustomFieldInput
from tissue.widgets.datetime_pickers import DueDateTimePicker

if TYPE_CHECKING:
    from typing import Any

    from textual.widget import Widget

    from tissue.api.generated.models.issue_type_summary import IssueTypeSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

log = logging.getLogger(__name__)


class CreateIssueModal(TissueModal[str | None]):
    """Create an issue in one scrollable form."""

    CSS_PATH = "create_issue_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self, *, project_key: str, members: list[ProjectMemberSummary]
    ) -> None:
        super().__init__()
        self._project_key = project_key
        self._members = members
        self._issue_types: list[IssueTypeSummary] = []
        self._loaded_type_id: int | None = None
        self._submitting = False
        self._parent_key: str | None = None
        self._parent_label_by_key: dict[str, str] = {}

    def compose(self) -> ComposeResult:
        with Container(id="cim-dialog", classes="dialog"):
            # The scroll lives directly in the dialog so its scrollbar sits right
            # at the edge. #cim-form holds the left/right padding inside it.
            with VerticalScroll(id="cim-scroll"):
                with Vertical(id="cim-form"):
                    yield self._titled(
                        Select([], prompt="Select a type…", id="cim-type"),
                        "Issue type *",
                    )
                    yield self._titled(
                        Button("Select parent…", id="cim-parent-btn"),
                        "Parent",
                    )
                    yield self._titled(
                        Input(
                            placeholder="2-50 characters",
                            max_length=50,
                            id="cim-title",
                        ),
                        "Title *",
                    )
                    with Horizontal(id="cim-pa-row"):
                        yield self._titled(
                            Select(
                                [(priority, priority) for priority in PRIORITIES],
                                value=DEFAULT_PRIORITY,
                                allow_blank=False,
                                id="cim-priority",
                            ),
                            "Priority",
                        )
                        yield self._titled(
                            Select(
                                self._member_choices(),
                                prompt="Unassigned",
                                id="cim-assignee",
                            ),
                            "Assignee",
                        )
                    with Horizontal(id="cim-sd-row"):
                        yield self._titled(
                            Input(
                                placeholder="STANDARD type only",
                                type="integer",
                                id="cim-sp",
                                disabled=True,
                            ),
                            "Story points",
                        )
                        yield self._titled(DueDateTimePicker(id="cim-due"), "Due date")
                    yield self._titled(
                        Input(
                            placeholder="short summary (optional)",
                            max_length=2000,
                            id="cim-summary",
                        ),
                        "Summary",
                    )
                    yield self._titled(TextArea(id="cim-content"), "Description")
                    yield Vertical(id="cim-custom-fields")
            yield Static("", id="cim-status", classes="status-msg")
            with Horizontal(id="cim-actions"):
                yield Button("Cancel", id="cim-cancel", classes="-btn-error")
                yield Button("Create", id="cim-create", classes="-btn-success")

    @staticmethod
    def _titled(widget: Widget, title: str) -> Widget:
        widget.border_title = title
        return widget

    def _member_choices(self) -> list[tuple[str, int]]:
        return member_choices(self._members)

    def on_mount(self) -> None:
        dialog = self.query_one("#cim-dialog", Container)
        dialog.border_title = "New Issue"
        dialog.border_subtitle = "Esc to cancel"
        self._apply_parent_gate(None)
        self.run_worker(self._load_types(), exclusive=True, group="cim-types")
        self.query_one("#cim-title", Input).focus()

    async def _load_types(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            types = await client.issues.list_issue_types()
        except TissueApiError as error:
            log.debug("Create: failed to list issue types: %s", error)
            self._error("Could not load issue types.")
            return
        self._issue_types = types
        select = self.query_one("#cim-type", Select)
        select.set_options(
            (issue_type.name or "-", issue_type.id)
            for issue_type in types
            if issue_type.id is not None
        )

    @on(Select.Changed, "#cim-type")
    def _on_type_changed(self, event: Select.Changed) -> None:
        value = event.value
        type_id = value if isinstance(value, int) else None
        self._apply_story_point_gate(type_id)
        self._apply_parent_gate(type_id)
        if type_id is None:
            return
        self.run_worker(
            self._load_custom_fields(type_id), exclusive=True, group="cim-cf"
        )

    async def _load_custom_fields(self, type_id: int) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            detail = await client.issues.get_issue_type(type_id)
        except TissueApiError as error:
            log.debug("Create: failed to load issue type %s: %s", type_id, error)
            if self._selected_type_id() == type_id:
                self._error("Could not load this type's custom fields.")
                self._loaded_type_id = type_id
            return
        if self._selected_type_id() != type_id:
            return
        inputs = custom_field_inputs(list(detail.fields or []))
        host = self.query_one("#cim-custom-fields", Vertical)
        with self.app.batch_update():
            await host.remove_children()
            if inputs:
                await host.mount(*inputs)
        self._loaded_type_id = type_id

    def _selected_type_id(self) -> int | None:
        value = self.query_one("#cim-type", Select).value
        return value if isinstance(value, int) else None

    def _hierarchy_of(self, type_id: int | None) -> str | None:
        return hierarchy_of(self._issue_types, type_id)

    def _parent_hierarchy_of(self, type_id: int | None) -> str | None:
        return parent_hierarchy_of(self._issue_types, type_id)

    def _apply_parent_gate(self, type_id: int | None) -> None:
        self._parent_key = None
        self._parent_label_by_key = {}
        parent_button = self.query_one("#cim-parent-btn", Button)
        parent_button.label = "Select parent…"
        if self._parent_hierarchy_of(type_id) is None:
            parent_button.display = False
            return
        parent_button.display = True
        parent_button.border_title = (
            "Parent *" if parent_required(self._issue_types, type_id) else "Parent"
        )

    @on(Button.Pressed, "#cim-parent-btn")
    def _on_parent_btn(self, event: Button.Pressed) -> None:
        event.stop()
        self.run_worker(self._open_parent_picker(), exclusive=True, group="cim-parent")

    async def _open_parent_picker(self) -> None:
        from tissue.screens.project_home.modals.issue_picker_modal import (
            IssuePickerModal,
        )

        parent_hier = self._parent_hierarchy_of(self._selected_type_id())
        if parent_hier is None:
            return
        candidates = await self._parent_candidates(parent_hier)
        self.app.push_screen(
            IssuePickerModal(
                candidates=candidates,
                multi=False,
                title="Select parent",
                subtitle=f"{parent_hier} issues · Esc to cancel",
            ),
            self._on_parent_picked,
        )

    async def _parent_candidates(self, parent_hier: str) -> list[tuple[str, str]]:
        client = self.app.client
        if client is None:
            return []
        try:
            page = await client.issues.search_project_issues(
                self._project_key, size=100
            )
        except TissueApiError as error:
            log.debug("Create: failed to load parent candidates: %s", error)
            return []
        candidates, self._parent_label_by_key = parent_candidate_labels(
            self._issue_types, parent_hier, page.content or []
        )
        return candidates

    def _on_parent_picked(self, picked: list[str] | None) -> None:
        key = picked[0] if picked else None
        if key is None:
            return
        self._parent_key = key
        self.query_one("#cim-parent-btn", Button).label = self._parent_label_by_key.get(
            key, key
        )

    def _apply_story_point_gate(self, type_id: int | None) -> None:
        story_point_input = self.query_one("#cim-sp", Input)
        if self._hierarchy_of(type_id) == "STANDARD":
            story_point_input.disabled = False
            story_point_input.placeholder = "integer (optional)"
        else:
            story_point_input.value = ""
            story_point_input.disabled = True
            story_point_input.placeholder = "STANDARD type only"

    @on(Button.Pressed, "#cim-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#cim-create")
    def _on_create(self) -> None:
        if self._submitting:
            return
        self._submitting = True
        self.run_worker(self._do_create(), group="cim-create")

    def _error(self, message: str) -> None:
        self.query_one("#cim-status", Static).update(message)

    def _create_issue_kwargs(self) -> dict[str, Any]:
        return self._read_form_values().to_create_kwargs()

    def _read_form_values(self) -> CreateIssueFormValues:
        type_id = self._selected_type_id()
        title = self.query_one("#cim-title", Input).value.strip()
        priority = str(self.query_one("#cim-priority", Select).value)

        assignee = self.query_one("#cim-assignee", Select).value
        assignee_id = assignee if isinstance(assignee, int) else None

        story_point_input = self.query_one("#cim-sp", Input)

        due = self.query_one("#cim-due", DueDateTimePicker).datetime
        due_at = due.assume_system_tz().to_instant().format_iso() if due else None

        summary = self.query_one("#cim-summary", Input).value.strip() or None
        content = self.query_one("#cim-content", TextArea).text.strip() or None

        return CreateIssueFormValues(
            issue_type_id=type_id,
            hierarchy=self._hierarchy_of(type_id),
            title=title,
            priority=priority,
            assignee_member_id=assignee_id,
            story_point_text=story_point_input.value.strip(),
            story_points_enabled=not story_point_input.disabled,
            due_at=due_at,
            summary=summary,
            content=content,
            custom_fields=collect_custom_fields(self.query(CustomFieldInput)),
            parent_issue_key=self._parent_key,
        )

    async def _do_create(self) -> None:
        try:
            client = self.app.client
            if client is None:
                return
            selected = self._selected_type_id()
            if selected is not None and self._loaded_type_id != selected:
                self._error("Still loading this type's fields — try again in a moment.")
                return
            try:
                kwargs = self._create_issue_kwargs()
            except ValueError as error:
                self._error(str(error))
                return
            try:
                issue_key = await client.issues.create_issue(
                    self._project_key, **kwargs
                )
            except TissueApiError as error:
                self._error(
                    getattr(error, "detail", None) or str(error) or "Create failed."
                )
                return
            self.dismiss(issue_key)
        finally:
            self._submitting = False
