"""Pure rendering helpers for the ProjectHome hub: each takes plain values (no
`self`) and returns a string / Rich `Text` / label, so the area mixins can share
them without cross-area coupling."""

from __future__ import annotations

import re
from typing import TYPE_CHECKING

from rich.text import Text

from tissue.screens.home.rendering import _fit, _truncate
from tissue.screens.project_home.constants import (
    _ACTIVITY_DATA_SKIP,
    _PRIORITY_VAR,
    _SPRINT_STATUS_VAR,
)
from tissue.util.datetime_fmt import format_date
from tissue.widgets.color_type import chip_style

if TYPE_CHECKING:
    from tissue.api.generated.models.activity_log_response import ActivityLogResponse
    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.issue_type_info import IssueTypeInfo
    from tissue.api.generated.models.project_member_info import ProjectMemberInfo


def _member_name(info: ProjectMemberInfo | None) -> str:
    if info is None:
        return "-"
    return info.display_name or info.username or "-"


def _humanize_key(key: str) -> str:
    """An activity field/data key as a label: drop a trailing Name/Key, split
    camelCase, sentence-case. e.g. 'assigneeName' -> 'Assignee', 'storyPoint' ->
    'Story point', 'targetIssueKey' -> 'Target issue'."""
    base = re.sub(r"(Name|Key)$", "", key) or key
    spaced = re.sub(r"(?<!^)(?=[A-Z])", " ", base)
    return spaced[:1].upper() + spaced[1:].lower()


def _color_chip(label: str, color: str | None, *, pad: bool = True) -> str | Text:
    """`label` as a solid pill — `color` fills the text *background* with a
    readable foreground. `color` is a ColorType enum name or an already-resolved
    hex; falls back to plain text when there's no colour. `pad=False` drops the
    surrounding spaces so it fits a tight column."""
    style = chip_style(color)
    if not style:
        return label
    return Text(f" {label} " if pad else label, style=style)


def _priority_chip(theme_variables: dict[str, str], priority: str | None) -> str | Text:
    """Pn as a background pill, coloured from a fixed priority->theme map and the
    screen's resolved theme variables."""
    if not priority:
        return "-"
    variable = _PRIORITY_VAR.get(priority)
    bg = theme_variables.get(variable) if variable else None
    return _color_chip(priority, bg)


def _sprint_status_chip(
    theme_variables: dict[str, str], status: str | None, *, pad: bool = True
) -> str | Text:
    """A sprint's status (PLANNING/ACTIVE/COMPLETED/CANCELLED) as a background
    pill, coloured from a fixed status->theme map. Capitalised for display."""
    if not status:
        return "-"
    variable = _SPRINT_STATUS_VAR.get(status)
    bg = theme_variables.get(variable) if variable else None
    return _color_chip(status.capitalize(), bg, pad=pad)


def _issue_rows(
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
) -> list[list[str | Text]]:
    """Issue summaries as DataTable rows — Key / Title / Status chip / Priority
    chip. Shared by the [1] issues list and a sprint's issue sub-list, so both
    tables look identical. `state_colors` tints Status with its workflow colour."""
    return [
        [
            _fit(i.issue_key or "-", 9),
            Text(_truncate(i.title or "-", 20)),
            _color_chip(
                i.current_state_label or "-",
                state_colors.get(i.current_state_id)
                if i.current_state_id is not None
                else None,
                pad=False,
            ),
            _priority_chip(theme_variables, i.priority),
        ]
        for i in issues
    ]


def _issue_list_rows(
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    member_names: dict[int, str],
) -> list[list[str | Text]]:
    """The [1] Issues list's DataTable rows — Key / Title / Status / Priority /
    Points / Due / Assignee. `member_names` resolves the assignee id to a name.

    Issue *type* is not shown: the list endpoint's `IssueSummary` doesn't carry
    it (only the detail's `IssueCommonDetail` does)."""
    return [
        [
            _fit(i.issue_key or "-", 9),
            Text(_truncate(i.title or "-", 24)),
            _color_chip(
                i.current_state_label or "-",
                state_colors.get(i.current_state_id)
                if i.current_state_id is not None
                else None,
                pad=False,
            ),
            _priority_chip(theme_variables, i.priority),
            "-" if i.story_point is None else str(i.story_point),
            format_date(i.due_at),
            member_names.get(i.assignee_member_id, "-")
            if i.assignee_member_id is not None
            else "-",
        ]
        for i in issues
    ]


def _custom_field_label(info: CustomFieldValueInfo) -> str:
    """A custom field's label with its first letter capitalised (the server stores
    them lower/camel-cased, e.g. 'reproduceSteps')."""
    label = info.field_label or "Field"
    return label[:1].upper() + label[1:]


def _custom_field_value(info: CustomFieldValueInfo) -> str:
    """A custom field's value as display text, formatted by its field type
    (BOOLEAN -> Yes/No, PERCENTAGE -> n%, CHECKLIST/multi -> comma-joined)."""
    value = info.value
    if value is None or value == "":
        return "-"
    field_type = info.issue_field_type
    if field_type == "BOOLEAN":
        return "Yes" if value else "No"
    if field_type == "PERCENTAGE":
        return f"{value}%"
    if isinstance(value, list):
        return ", ".join(str(v) for v in value) if value else "-"
    return str(value)


def _type_text(issue_type: IssueTypeInfo | None) -> str | Text:
    """Issue type in bold (no colour)."""
    if issue_type is None:
        return "-"
    return Text(issue_type.display_name or "-", style="bold")


def _transition_label(
    t: AvailableTransition,
    current_state_label: str,
    target_labels: dict[int, str],
) -> str:
    target = target_labels.get(t.transition_id) if t.transition_id else None
    label = f"{t.display_label or '?'}: {current_state_label} → {target or '?'}"
    if not t.can_execute and t.blocked_reasons:
        reasons = [r.message for r in t.blocked_reasons if r.message]
        if reasons:
            label += f"  ⚠ {'; '.join(reasons)}"
    return label


def _activity_details(a: ActivityLogResponse) -> list[str]:
    """Detail lines for an event: each `changes` entry as `Field: before →
    after` (e.g. a transition's `State: To Do → In Progress`), then each
    meaningful `data` entry as `Label: value` (e.g. ISSUE_ASSIGNED's
    `Assignee: Bob Lee`). Skips the per-event context keys and the raw
    old*/new* data that the `changes` line already covers."""
    lines: list[str] = []
    for field, change in (a.changes or {}).items():
        before = "" if change.var_from is None else str(change.var_from)
        after = "" if change.to is None else str(change.to)
        label = _humanize_key(field)
        if before and after:
            lines.append(f"{label}: {before} → {after}")
        elif after:
            lines.append(f"{label}: {after}")
        elif before:
            lines.append(f"{label}: {before} (cleared)")
    for key, value in (a.data or {}).items():
        if key in _ACTIVITY_DATA_SKIP or not value:
            continue
        lines.append(f"{_humanize_key(key)}: {value}")
    return lines


def _activity_label(a: ActivityLogResponse) -> str:
    """Humanise the event type, e.g. ISSUE_STATUS_CHANGED -> 'Status changed'."""
    raw = (a.type or "").strip()
    if not raw:
        return "Activity"
    return raw.removeprefix("ISSUE_").replace("_", " ").capitalize()
