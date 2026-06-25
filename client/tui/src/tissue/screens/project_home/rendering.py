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
from tissue.widgets.issue_render import review_status_chip as _review_status_chip
from tissue.widgets.issue_render import type_chip as _type_chip

if TYPE_CHECKING:
    from tissue.api.generated.models.activity_log_response import ActivityLogResponse
    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.issue_summary import IssueSummary


def _humanize_key(key: str) -> str:
    """Turn an activity field/data key into a tidy label.

    A key that already starts with a capital is a custom field label the server
    capitalized, so show it as-is. Built-in fields use camelCase.
    """
    if key[:1].isupper():
        return key
    base = re.sub(r"(Name|Key)$", "", key) or key
    spaced = re.sub(r"(?<!^)(?=[A-Z])", " ", base)
    return spaced[:1].upper() + spaced[1:].lower()


def _change_label(field: str, field_names: dict[str, str]) -> str:
    """The label to show for a change key.

    An old `customFields.{id}` key is looked up in `field_names` to get the
    field's name, fixing past entries that logged the id. New entries already
    carry the name.
    """
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
    """A sprint's status as a pill with a colored background."""
    if not status:
        return "-"
    variable = _SPRINT_STATUS_VAR.get(status)
    background_color = theme_variables.get(variable) if variable else None
    return _color_chip(status.capitalize(), background_color, pad=pad)


def _issue_rows(
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    *,
    with_due: bool = False,
) -> list[list[str | Text]]:
    """Issue summaries as DataTable rows, adding a Due cell at the end when
    `with_due`.

    Callers that ask for the Due column must add it to their column headers too.
    """
    rows: list[list[str | Text]] = []
    for issue in issues:
        row: list[str | Text] = [
            _fit(issue.issue_key or "-", 10),
            Text(_truncate(issue.title or "-", 15)),
            _color_chip(
                issue.current_state_label or "-",
                state_colors.get(issue.current_state_id)
                if issue.current_state_id is not None
                else None,
                pad=False,
            ),
            _priority_chip(theme_variables, issue.priority),
        ]
        if with_due:
            row.append(format_date(issue.due_at))
        rows.append(row)
    return rows


def _issue_list_rows(
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    member_names: dict[int, str],
    *,
    with_review_status: bool = False,
) -> list[list[str | Text]]:
    """The [1]/[3] issue-list DataTable rows.

    `member_names` looks up the assignee id to show a name.

    `with_review_status` (the [3] Requested Reviews view) adds a "Review" column
    at the front and trims the Title to make room. Only that view has a useful
    review status per row.
    """
    title_width = 14 if with_review_status else 19
    rows: list[list[str | Text]] = []
    for issue in issues:
        row: list[str | Text] = []
        if with_review_status:
            row.append(
                _review_status_chip(
                    theme_variables, issue.my_review_status, compact=True, pad=False
                )
            )
        row.extend(
            [
                _fit(issue.issue_key or "-", 10),
                _type_chip(issue.issue_type_name, issue.issue_type_color),
                Text(_truncate(issue.title or "-", title_width)),
                _color_chip(
                    issue.current_state_label or "-",
                    state_colors.get(issue.current_state_id)
                    if issue.current_state_id is not None
                    else None,
                    pad=False,
                ),
                _priority_chip(theme_variables, issue.priority),
                "-" if issue.story_point is None else str(issue.story_point),
                format_date(issue.due_at),
                # Text() so a '[...]' in a display name does not crash markup parsing
                Text(member_names.get(issue.assignee_member_id, "-"))
                if issue.assignee_member_id is not None
                else "-",
            ]
        )
        rows.append(row)
    return rows


def _transition_label(
    transition: AvailableTransition,
    current_state_label: str,
    target_labels: dict[int, str],
) -> str:
    target = (
        target_labels.get(transition.transition_id)
        if transition.transition_id
        else None
    )
    label = (
        f"{transition.display_label or '?'}: {current_state_label} → {target or '?'}"
    )
    if not transition.can_execute and transition.blocked_reasons:
        reasons = [
            reason.message for reason in transition.blocked_reasons if reason.message
        ]
        if reasons:
            label += f"  ⚠ {'; '.join(reasons)}"
    return label


def _activity_details(
    activity: ActivityLogResponse, field_names: dict[str, str] | None = None
) -> list[str]:
    """Detail lines for an event, one per `changes` then `data` entry.

    Skips event context keys and the raw old*/new* data the `changes` line
    already shows. `field_names` looks up old `customFields.{id}` change keys.
    """
    field_names = field_names or {}
    lines: list[str] = []
    for field, change in (activity.changes or {}).items():
        before = "" if change.var_from is None else str(change.var_from)
        after = "" if change.to is None else str(change.to)
        label = _change_label(field, field_names)
        if before and after:
            lines.append(f"{label}: {before} → {after}")
        elif after:
            lines.append(f"{label}: {after}")
        elif before:
            lines.append(f"{label}: {before} (cleared)")
    for key, value in (activity.data or {}).items():
        if key in _ACTIVITY_DATA_SKIP or not value:
            continue
        lines.append(f"{_humanize_key(key)}: {value}")
    return lines


def _activity_label(activity: ActivityLogResponse) -> str:
    """Turn the event type into plain words.

    e.g. ISSUE_STATUS_CHANGED -> 'Status changed'.
    """
    event_type = (activity.type or "").strip()
    if not event_type:
        return "Activity"
    return event_type.removeprefix("ISSUE_").replace("_", " ").capitalize()
