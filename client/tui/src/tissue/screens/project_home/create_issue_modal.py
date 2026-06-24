"""A single scrollable dialog that creates an issue: all of its fields are filled
in one form (not a modal per field) and submitted together. The common fields are
fixed; the custom-field section below them rebuilds whenever the issue type
changes, mounting a `CustomFieldInput` per field the chosen type defines."""

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
from tissue.widgets.custom_field_input import UNSET, CustomFieldInput
from tissue.widgets.datetime_pickers import DueDateTimePicker

if TYPE_CHECKING:
    from typing import Any

    from textual.widget import Widget

    from tissue.api.generated.models.issue_type_summary import IssueTypeSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

log = logging.getLogger(__name__)

_PRIORITIES = ["P0", "P1", "P2", "P3", "P4"]
# Major is the most common default (mirrors IssueFieldEditModal's priority default).
_DEFAULT_PRIORITY = "P2"

# IssueHierarchy ladder (mirrors the backend): a parent sits exactly one level
# above its child. SUBTASK/MICROTASK can't be created standalone (server rejects).
_LEVEL_BY_HIERARCHY = {"EPIC": 1, "STANDARD": 2, "SUBTASK": 3, "MICROTASK": 4}
_HIERARCHY_BY_LEVEL = {1: "EPIC", 2: "STANDARD", 3: "SUBTASK", 4: "MICROTASK"}
_PARENT_REQUIRED_HIERARCHIES = {"SUBTASK", "MICROTASK"}


def _cap(text: str) -> str:
    """Capitalise the first letter (the server stores lower/camelCase field
    labels like 'version'), matching how the detail pane renders them."""
    return text[:1].upper() + text[1:]


class CreateIssueModal(TissueModal[str | None]):
    """Create an issue in one scrollable form.

    Dismisses with the new issue's key on success, or None on cancel. The issue
    type drives the dynamic custom-field section: picking a type fetches its field
    definitions and mounts a `CustomFieldInput` per field.
    """

    CSS_PATH = "create_issue_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self, *, project_key: str, members: list[ProjectMemberSummary]
    ) -> None:
        super().__init__()
        self._project_key = project_key
        self._members = members
        self._issue_types: list[IssueTypeSummary] = []
        # The type whose custom fields are currently mounted. Guards both the
        # stale-fetch remount AND the create path: submit is blocked until this
        # matches the selected type, so _collect never reads an empty (still
        # loading) or stale (previous type's) custom-field set.
        self._loaded_type_id: int | None = None
        # True while a create POST is in flight — blocks a second submit (which
        # would create a duplicate issue, since cancelling can't un-send a POST).
        self._submitting = False
        # The chosen parent issue key (None = none picked). Required for
        # SUBTASK/MICROTASK types, optional for STANDARD, hidden for EPIC. Cleared on
        # every type change (the valid parent level moves with the type).
        self._parent_key: str | None = None
        self._parent_label_by_key: dict[str, str] = {}

    def compose(self) -> ComposeResult:
        with Container(id="cim-dialog", classes="dialog"):
            # The scroll lives directly in the dialog (no horizontal padding) so
            # its scrollbar sits flush at the dialog edge; #cim-form carries the
            # content's horizontal padding, inside the scrollbar.
            with VerticalScroll(id="cim-scroll"):
                with Vertical(id="cim-form"):
                    yield self._titled(
                        Select([], prompt="Select a type…", id="cim-type"),
                        "Issue type *",
                    )
                    # Parent picker — hidden until a type that can have a parent is
                    # picked; _apply_parent_gate shows/labels it per the type's
                    # hierarchy. The button opens a single-select issue picker.
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
                    # Priority and Assignee share one row, split 1:1.
                    with Horizontal(id="cim-pa-row"):
                        yield self._titled(
                            Select(
                                [(p, p) for p in _PRIORITIES],
                                value=_DEFAULT_PRIORITY,
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
                    # Story points and Due date share one row, split 1:1.
                    with Horizontal(id="cim-sd-row"):
                        # Starts disabled: story points apply only to STANDARD
                        # types, and no type is selected yet. _apply_story_point_gate
                        # flips this on every type change.
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
                    # Rebuilt on every issue-type change.
                    yield Vertical(id="cim-custom-fields")
            yield Static("", id="cim-status", classes="status-msg")
            with Horizontal(id="cim-actions"):
                yield Button("Cancel", id="cim-cancel", classes="-btn-error")
                yield Button("Create", id="cim-create", classes="-btn-success")

    @staticmethod
    def _titled(widget: Widget, title: str) -> Widget:
        """Put the field name in the control's border title (instead of a label
        above it), so each input reads as a labelled box."""
        widget.border_title = title
        return widget

    def _member_choices(self) -> list[tuple[str, int]]:
        choices: list[tuple[str, int]] = []
        for member in self._members:
            if member.member_id is None:
                continue
            name = member.display_name or member.username or "-"
            handle = f" (@{member.username})" if member.username else ""
            choices.append((f"{name}{handle}", member.member_id))
        return choices

    def on_mount(self) -> None:
        dialog = self.query_one("#cim-dialog", Container)
        dialog.border_title = "New Issue"
        dialog.border_subtitle = "Esc to cancel"
        # No type selected yet -> the parent picker starts hidden.
        self._apply_parent_gate(None)
        self.run_worker(self._load_types(), exclusive=True, group="cim-types")
        self.query_one("#cim-title", Input).focus()

    async def _load_types(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            types = await client.issues.list_issue_types()
        except TissueApiError as e:
            log.debug("Create: failed to list issue types: %s", e)
            self._error("Could not load issue types.")
            return
        self._issue_types = types
        select = self.query_one("#cim-type", Select)
        select.set_options((t.name or "-", t.id) for t in types if t.id is not None)

    @on(Select.Changed, "#cim-type")
    def _on_type_changed(self, event: Select.Changed) -> None:
        # Select values here are type ids (int); a blank/NULL selection is the
        # NoSelection sentinel, which isinstance filters out.
        value = event.value
        type_id = value if isinstance(value, int) else None
        # Story points can be set directly only on STANDARD-hierarchy issues, so
        # re-gate the input on every type change (including back to blank).
        self._apply_story_point_gate(type_id)
        # The parent picker's visibility/requirement also depends on the type.
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
        except TissueApiError as e:
            log.debug("Create: failed to load issue type %s: %s", type_id, e)
            # Mark the load concluded (with no fields) so the create guard doesn't
            # deadlock on a type whose fetch failed; the server still validates any
            # required custom fields.
            if self._selected_type_id() == type_id:
                self._error("Could not load this type's custom fields.")
                self._loaded_type_id = type_id
            return
        # The selection may have changed again while we awaited.
        if self._selected_type_id() != type_id:
            return
        fields = sorted(detail.fields or [], key=lambda f: f.position or 0)
        inputs = [
            CustomFieldInput(
                field_id=f.id,
                label=_cap(f.name or "Field"),
                ftype=f.type or "TEXT",
                required=bool(f.required),
                options=list(f.options or []),
            )
            for f in fields
            if f.id is not None
        ]
        host = self.query_one("#cim-custom-fields", Vertical)
        # Clear + remount in one paint (same anti-flicker as the detail panes).
        with self.app.batch_update():
            await host.remove_children()
            if inputs:
                await host.mount(*inputs)
        self._loaded_type_id = type_id

    def _selected_type_id(self) -> int | None:
        value = self.query_one("#cim-type", Select).value
        return value if isinstance(value, int) else None

    def _hierarchy_of(self, type_id: int | None) -> str | None:
        if type_id is None:
            return None
        return next((t.hierarchy for t in self._issue_types if t.id == type_id), None)

    def _parent_hierarchy_of(self, type_id: int | None) -> str | None:
        """The hierarchy a parent of this type must have (exactly one level up), or
        None when the type can't have a parent (EPIC) or isn't resolved."""
        level = _LEVEL_BY_HIERARCHY.get(self._hierarchy_of(type_id) or "")
        return _HIERARCHY_BY_LEVEL.get((level or 0) - 1)

    def _apply_parent_gate(self, type_id: int | None) -> None:
        """Show/label the parent picker per the selected type's hierarchy: hidden for
        EPIC (no parent) and no-selection, shown and starred for SUBTASK/MICROTASK
        (required), shown and optional for STANDARD. Clears any prior pick — the valid
        parent level moves with the type."""
        self._parent_key = None
        self._parent_label_by_key = {}
        btn = self.query_one("#cim-parent-btn", Button)
        btn.label = "Select parent…"
        if self._parent_hierarchy_of(type_id) is None:
            btn.display = False
            return
        btn.display = True
        required = self._hierarchy_of(type_id) in _PARENT_REQUIRED_HIERARCHIES
        btn.border_title = "Parent *" if required else "Parent"

    @on(Button.Pressed, "#cim-parent-btn")
    def _on_parent_btn(self, event: Button.Pressed) -> None:
        event.stop()
        self.run_worker(self._open_parent_picker(), exclusive=True, group="cim-parent")

    async def _open_parent_picker(self) -> None:
        from tissue.screens.project_home.issue_picker_modal import IssuePickerModal

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
        """`(label, key)` for the project's issues of the required parent hierarchy."""
        client = self.app.client
        if client is None:
            return []
        hier_by_type = {
            t.id: t.hierarchy for t in self._issue_types if t.id is not None
        }
        try:
            page = await client.issues.search_project_issues(
                self._project_key, size=100
            )
        except TissueApiError as e:
            log.debug("Create: failed to load parent candidates: %s", e)
            return []
        out: list[tuple[str, str]] = []
        self._parent_label_by_key = {}
        for s in page.content or []:
            if s.issue_key is None or s.issue_type_id is None:
                continue
            if hier_by_type.get(s.issue_type_id) != parent_hier:
                continue
            label = s.issue_key + (f"  {s.title}" if s.title else "")
            out.append((label, s.issue_key))
            self._parent_label_by_key[s.issue_key] = label
        return out

    def _on_parent_picked(self, picked: list[str] | None) -> None:
        key = picked[0] if picked else None
        if key is None:
            return
        self._parent_key = key
        self.query_one("#cim-parent-btn", Button).label = self._parent_label_by_key.get(
            key, key
        )

    def _apply_story_point_gate(self, type_id: int | None) -> None:
        """Enable the story-point input only for STANDARD types.

        The backend rejects a directly-set point on any other hierarchy (EPIC
        points are rolled up from children; SUBTASK/MICROTASK carry none), so a
        disabled, cleared input keeps the form from submitting a value the server
        would 400 on."""
        sp = self.query_one("#cim-sp", Input)
        if self._hierarchy_of(type_id) == "STANDARD":
            sp.disabled = False
            sp.placeholder = "integer (optional)"
        else:
            sp.value = ""
            sp.disabled = True
            sp.placeholder = "STANDARD type only"

    @on(Button.Pressed, "#cim-cancel")
    def _on_cancel(self) -> None:
        self.dismiss(None)

    def action_close(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#cim-create")
    def _on_create(self) -> None:
        # Synchronous double-submit guard: this handler runs to completion before
        # the next click's does, so a second press while a create is in flight is
        # dropped here (exclusive=True can't help — cancelling a worker can't
        # un-send a POST already on the wire). The worker clears the flag on any
        # non-dismiss exit, so a failed attempt can be retried.
        if self._submitting:
            return
        self._submitting = True
        self.run_worker(self._do_create(), group="cim-create")

    def _error(self, message: str) -> None:
        self.query_one("#cim-status", Static).update(message)

    def _collect(self) -> dict[str, Any]:
        """Read and validate every field into create_issue kwargs.

        Raises ValueError (with a user-facing message) on the first invalid or
        missing-required field."""
        type_id = self._selected_type_id()
        if type_id is None:
            raise ValueError("Select an issue type.")
        # SUBTASK/MICROTASK can't be created standalone — require a parent.
        if self._hierarchy_of(type_id) in _PARENT_REQUIRED_HIERARCHIES and (
            not self._parent_key
        ):
            raise ValueError("Select a parent issue for this type.")
        title = self.query_one("#cim-title", Input).value.strip()
        if not (2 <= len(title) <= 50):
            raise ValueError("Title must be 2-50 characters.")
        priority = str(self.query_one("#cim-priority", Select).value)

        assignee = self.query_one("#cim-assignee", Select).value
        assignee_id = assignee if isinstance(assignee, int) else None

        # A disabled input means the selected type can't carry story points;
        # never submit a (possibly stale) value the server would reject.
        sp_input = self.query_one("#cim-sp", Input)
        if sp_input.disabled:
            story_point = None
        else:
            sp_raw = sp_input.value.strip()
            if sp_raw and not sp_raw.isdigit():
                raise ValueError("Story points must be a non-negative integer.")
            story_point = int(sp_raw) if sp_raw else None

        due = self.query_one("#cim-due", DueDateTimePicker).datetime
        due_at = due.assume_system_tz().to_instant().format_iso() if due else None

        summary = self.query_one("#cim-summary", Input).value.strip() or None
        content = self.query_one("#cim-content", TextArea).text.strip() or None

        custom_fields: dict[str, Any] = {}
        for cf in self.query(CustomFieldInput):
            value = cf.get_value()
            if value is UNSET:
                continue
            custom_fields[str(cf.field_id)] = value

        return {
            "issue_type_id": type_id,
            "title": title,
            "priority": priority,
            "content": content,
            "summary": summary,
            "assignee_member_id": assignee_id,
            "story_point": story_point,
            "due_at": due_at,
            "custom_fields": custom_fields or None,
            "parent_issue_key": self._parent_key,
        }

    async def _do_create(self) -> None:
        try:
            client = self.app.client
            if client is None:
                return
            # Block until the selected type's custom fields have actually mounted.
            # The field load runs in a separate worker, so without this _collect
            # could read an empty set (fields still loading -> required fields
            # silently dropped) or a previous type's set (fast type switch ->
            # wrong fields paired with the new type id).
            selected = self._selected_type_id()
            if selected is not None and self._loaded_type_id != selected:
                self._error("Still loading this type's fields — try again in a moment.")
                return
            try:
                kwargs = self._collect()
            except ValueError as e:
                self._error(str(e))
                return
            try:
                issue_key = await client.issues.create_issue(
                    self._project_key, **kwargs
                )
            except TissueApiError as e:
                self._error(getattr(e, "detail", None) or str(e) or "Create failed.")
                return
            self.dismiss(issue_key)
        finally:
            self._submitting = False
