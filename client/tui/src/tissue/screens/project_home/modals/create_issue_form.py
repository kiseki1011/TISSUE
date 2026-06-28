from __future__ import annotations

from collections.abc import Iterable
from dataclasses import dataclass
from typing import TYPE_CHECKING, Any

from tissue.widgets.custom_field_input import UNSET, CustomFieldInput

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_field_detail import IssueFieldDetail
    from tissue.api.generated.models.issue_type_summary import IssueTypeSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

PRIORITIES = ["P0", "P1", "P2", "P3", "P4"]
DEFAULT_PRIORITY = "P2"

LEVEL_BY_HIERARCHY = {"EPIC": 1, "STANDARD": 2, "SUBTASK": 3, "MICROTASK": 4}
HIERARCHY_BY_LEVEL = {1: "EPIC", 2: "STANDARD", 3: "SUBTASK", 4: "MICROTASK"}
PARENT_REQUIRED_HIERARCHIES = {"SUBTASK", "MICROTASK"}


@dataclass(frozen=True)
class CreateIssueFormValues:
    issue_type_id: int | None
    hierarchy: str | None
    title: str
    priority: str
    assignee_member_id: int | None
    story_point_text: str
    story_points_enabled: bool
    due_at: str | None
    summary: str | None
    content: str | None
    custom_fields: dict[str, Any]
    parent_issue_key: str | None

    def validate(self) -> None:
        if self.issue_type_id is None:
            raise ValueError("Select an issue type.")
        if self.hierarchy in PARENT_REQUIRED_HIERARCHIES and not self.parent_issue_key:
            raise ValueError("Select a parent issue for this type.")
        if not (2 <= len(self.title) <= 50):
            raise ValueError("Title must be 2-50 characters.")
        if (
            self.story_points_enabled
            and self.story_point_text
            and not self.story_point_text.isdigit()
        ):
            raise ValueError("Story points must be a non-negative integer.")

    def to_create_kwargs(self) -> dict[str, Any]:
        self.validate()
        if self.issue_type_id is None:
            raise ValueError("Select an issue type.")
        return {
            "issue_type_id": self.issue_type_id,
            "title": self.title,
            "priority": self.priority,
            "content": self.content,
            "summary": self.summary,
            "assignee_member_id": self.assignee_member_id,
            "story_point": self._story_point(),
            "due_at": self.due_at,
            "custom_fields": self.custom_fields or None,
            "parent_issue_key": self.parent_issue_key,
        }

    def _story_point(self) -> int | None:
        if not self.story_points_enabled or not self.story_point_text:
            return None
        return int(self.story_point_text)


def member_choices(members: list[ProjectMemberSummary]) -> list[tuple[str, int]]:
    choices: list[tuple[str, int]] = []
    for member in members:
        if member.member_id is None:
            continue
        name = member.display_name or member.username or "-"
        handle = f" (@{member.username})" if member.username else ""
        choices.append((f"{name}{handle}", member.member_id))
    return choices


def hierarchy_of(
    issue_types: list[IssueTypeSummary], type_id: int | None
) -> str | None:
    if type_id is None:
        return None
    return next(
        (
            issue_type.hierarchy
            for issue_type in issue_types
            if issue_type.id == type_id
        ),
        None,
    )


def parent_hierarchy_of(
    issue_types: list[IssueTypeSummary], type_id: int | None
) -> str | None:
    level = LEVEL_BY_HIERARCHY.get(hierarchy_of(issue_types, type_id) or "")
    return HIERARCHY_BY_LEVEL.get((level or 0) - 1)


def parent_required(issue_types: list[IssueTypeSummary], type_id: int | None) -> bool:
    return hierarchy_of(issue_types, type_id) in PARENT_REQUIRED_HIERARCHIES


def custom_field_inputs(fields: list[IssueFieldDetail]) -> list[CustomFieldInput]:
    return [
        CustomFieldInput(
            field_id=field.id,
            label=_cap(field.name or "Field"),
            ftype=field.type or "TEXT",
            required=bool(field.required),
            options=list(field.options or []),
        )
        for field in sorted(fields, key=lambda field: field.position or 0)
        if field.id is not None
    ]


def collect_custom_fields(inputs: Iterable[CustomFieldInput]) -> dict[str, Any]:
    values: dict[str, Any] = {}
    for field_input in inputs:
        value = field_input.get_value()
        if value is UNSET:
            continue
        values[str(field_input.field_id)] = value
    return values


def parent_candidate_labels(
    issue_types: list[IssueTypeSummary],
    parent_hierarchy: str,
    summaries: Iterable[Any],
) -> tuple[list[tuple[str, str]], dict[str, str]]:
    hierarchy_by_type = {
        issue_type.id: issue_type.hierarchy
        for issue_type in issue_types
        if issue_type.id is not None
    }
    candidates: list[tuple[str, str]] = []
    label_by_key: dict[str, str] = {}
    for summary in summaries:
        if summary.issue_key is None or summary.issue_type_id is None:
            continue
        if hierarchy_by_type.get(summary.issue_type_id) != parent_hierarchy:
            continue
        label = summary.issue_key + (f"  {summary.title}" if summary.title else "")
        candidates.append((label, summary.issue_key))
        label_by_key[summary.issue_key] = label
    return candidates, label_by_key


def _cap(text: str) -> str:
    return text[:1].upper() + text[1:]
