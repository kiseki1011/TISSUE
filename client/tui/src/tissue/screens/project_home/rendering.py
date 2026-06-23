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
    _SPRINT_STATUS_VAR,
)
from tissue.util.datetime_fmt import format_date
from tissue.widgets.issue_render import color_chip as _color_chip
from tissue.widgets.issue_render import priority_chip as _priority_chip

if TYPE_CHECKING:
    from tissue.api.generated.models.activity_log_response import ActivityLogResponse
    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.issue_summary import IssueSummary


def _humanize_key(key: str) -> str:
    """An activity field/data key as a label: drop a trailing Name/Key, split
    camelCase, sentence-case. e.g. 'assigneeName' -> 'Assignee', 'storyPoint' ->
    'Story point', 'targetIssueKey' -> 'Target issue'.

    A key that already starts uppercase is a pre-formatted label — a custom field
    name, capitalised server-side (e.g. 'Version', 'ReproduceSteps') — so it's shown
    as-is, matching the detail pane. Built-in fields use lowercase camelCase keys."""
    if key[:1].isupper():
        return key
    base = re.sub(r"(Name|Key)$", "", key) or key
    spaced = re.sub(r"(?<!^)(?=[A-Z])", " ", base)
    return spaced[:1].upper() + spaced[1:].lower()


def _change_label(field: str, field_names: dict[str, str]) -> str:
    """The display label for a change key. A legacy custom-field key
    `customFields.{id}` is resolved to the field's name via `field_names` (id ->
    label) and capitalised — fixing historical entries logged with the id (new
    entries already carry the name). Everything else goes through `_humanize_key`."""
    prefix = "customFields."
    if field.startswith(prefix):
        field_id = field[len(prefix) :]
        name = field_names.get(field_id)
        if name:
            return name[:1].upper() + name[1:]
        return f"Custom field {field_id}"
    return _humanize_key(field)


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


def _activity_details(
    a: ActivityLogResponse, field_names: dict[str, str] | None = None
) -> list[str]:
    """Detail lines for an event: each `changes` entry as `Field: before →
    after` (e.g. a transition's `State: To Do → In Progress`), then each
    meaningful `data` entry as `Label: value` (e.g. ISSUE_ASSIGNED's
    `Assignee: Bob Lee`). Skips the per-event context keys and the raw
    old*/new* data that the `changes` line already covers.

    `field_names` (custom field id -> label) resolves legacy `customFields.{id}`
    change keys to the field's name."""
    field_names = field_names or {}
    lines: list[str] = []
    for field, change in (a.changes or {}).items():
        before = "" if change.var_from is None else str(change.var_from)
        after = "" if change.to is None else str(change.to)
        label = _change_label(field, field_names)
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
