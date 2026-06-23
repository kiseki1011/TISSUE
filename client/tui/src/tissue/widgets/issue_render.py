"""Shared issue-rendering primitives: status / priority colour chips, issue type
text, and member-name resolution. Pure (no `self` / DOM), so any screen's detail
pane or table can render an issue identically — keeping the dashboard and the
project hub from drifting apart."""

from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual.widget import Widget
from textual.widgets import Markdown, Rule, Static

from tissue.util.datetime_fmt import format_relative
from tissue.widgets.color_type import chip_style
from tissue.widgets.detail_row import detail_row

if TYPE_CHECKING:
    from collections.abc import Callable

    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_type_info import IssueTypeInfo
    from tissue.api.generated.models.project_member_info import ProjectMemberInfo

# Priority has no server-defined colour, so the TUI fixes one: each level maps to
# a theme variable used as the chip *background* (P0 loudest, P4 softest).
PRIORITY_VAR: dict[str, str] = {
    "P0": "error",
    "P1": "warning",
    "P2": "primary",
    "P3": "secondary",
    "P4": "success",
}


def color_chip(label: str, color: str | None, *, pad: bool = True) -> str | Text:
    """`label` as a solid pill — `color` fills the text *background* with a
    readable foreground. `color` is a ColorType enum name or an already-resolved
    hex; falls back to plain text when there's no colour. `pad=False` drops the
    surrounding spaces so it fits a tight column."""
    style = chip_style(color)
    if not style:
        return label
    return Text(f" {label} " if pad else label, style=style)


def priority_chip(theme_variables: dict[str, str], priority: str | None) -> str | Text:
    """Pn as a background pill, coloured from a fixed priority->theme map and the
    screen's resolved theme variables."""
    if not priority:
        return "-"
    variable = PRIORITY_VAR.get(priority)
    bg = theme_variables.get(variable) if variable else None
    return color_chip(priority, bg)


def type_text(issue_type: IssueTypeInfo | None) -> str | Text:
    """Issue type in bold (no colour)."""
    if issue_type is None:
        return "-"
    return Text(issue_type.display_name or "-", style="bold")


def member_name(info: ProjectMemberInfo | None) -> str:
    if info is None:
        return "-"
    return info.display_name or info.username or "-"


def custom_field_label(info: CustomFieldValueInfo) -> str:
    """A custom field's label with its first letter capitalised (the server stores
    them lower/camel-cased, e.g. 'reproduceSteps')."""
    label = info.field_label or "Field"
    return label[:1].upper() + label[1:]


def custom_field_display_value(
    info: CustomFieldValueInfo,
    options_by_field: dict[int, list[FieldOptionDetail]],
) -> str:
    """A custom field's value as display text, formatted by its field type:
    BOOLEAN -> Yes/No, PERCENTAGE -> n%, SELECT_OPTION -> the option's name,
    CHECKLIST -> the checked options' names. `options_by_field` (field id ->
    options) resolves SELECT/CHECKLIST ids to names. TEXT is handled separately
    (rendered as Markdown), so it just falls through to its raw string here."""
    value = info.value
    if value is None or value == "":
        return "-"
    field_type = info.issue_field_type
    if field_type == "BOOLEAN":
        return "Yes" if value else "No"
    if field_type == "PERCENTAGE":
        return f"{value}%"
    options = options_by_field.get(info.field_id) if info.field_id is not None else None
    if field_type == "SELECT_OPTION" and options:
        name = next((o.name for o in options if o.id == value), None)
        return name or str(value)
    if field_type == "CHECKLIST":
        # Stored as {str(optionId): checked}; show the checked options' names.
        if isinstance(value, dict):
            checked = {int(k) for k, v in value.items() if v}
            names = [o.name or "-" for o in (options or []) if o.id in checked]
            return ", ".join(names) if names else "-"
        if isinstance(value, list):  # tolerate a raw id list
            return ", ".join(str(v) for v in value) if value else "-"
    return str(value)


def custom_field_section(
    custom_fields: list[CustomFieldValueInfo],
    options_by_field: dict[int, list[FieldOptionDetail]],
    *,
    edit_button: Callable[[int], Widget] | None = None,
) -> list[Widget]:
    """The custom-field rows for the detail pane, type-aware: a TEXT field *with
    content* renders as a borderless Markdown box (under a label row); an empty
    TEXT field and every other type render as a normal `key: value` row (value on
    the right, like the common fields). `edit_button(field_id)` (project hub) adds a
    ✎ action to each row; pass None (dashboard) for a read-only section. Returns an
    empty list when there are no custom fields. Leads with a blank spacer so the
    section sits a line below the standard fields."""
    if not custom_fields:
        return []
    widgets: list[Widget] = [Static("", classes="detail-gap")]
    for cf in custom_fields:
        label = custom_field_label(cf)
        action = (
            edit_button(cf.field_id)
            if edit_button is not None and cf.field_id is not None
            else None
        )
        body = str(cf.value).strip() if cf.value else ""
        if cf.issue_field_type == "TEXT" and body:
            # Non-empty TEXT: a label row, then a borderless Markdown box below.
            widgets.append(detail_row(label, "", action=action))
            widgets.append(Markdown(body, classes="cf-text"))
        else:
            # Empty TEXT or any other type: a normal key: value row (an empty TEXT
            # shows "-" on the right, matching the common fields).
            widgets.append(
                detail_row(
                    label,
                    custom_field_display_value(cf, options_by_field),
                    action=action,
                )
            )
    return widgets


def issue_read_view(
    d: IssueCommonDetail,
    custom_fields: list[CustomFieldValueInfo],
    options_by_field: dict[int, list[FieldOptionDetail]],
    theme_variables: dict[str, str],
    *,
    title_class: str = "detail-title",
    content_class: str = "detail-content",
    muted_class: str = "detail-muted",
) -> list[Widget]:
    """A read-only issue detail: title, the standard field rows, the custom-field
    section, then the body (Markdown, or an italic '(empty)'). Shared by the
    dashboard's detail pane and the hub's expanded-mode detail modal so they can't
    drift; callers pass their own CSS class names for the title/body/empty text."""
    state = d.current_state
    current_state_label = (state.display_name if state else None) or "-"
    widgets: list[Widget] = [
        Static(d.title or "-", markup=False, classes=title_class),
        detail_row("Key", d.issue_key or "-"),
        detail_row(
            "Status", color_chip(current_state_label, state.color if state else None)
        ),
        detail_row("Priority", priority_chip(theme_variables, d.priority)),
        detail_row("Type", type_text(d.issue_type)),
        detail_row("Assignee", member_name(d.assignee)),
        detail_row("Author", member_name(d.author)),
        detail_row(
            "Story points", "-" if d.story_point is None else str(d.story_point)
        ),
        detail_row("Due", format_relative(d.due_at)),
        detail_row("Created", format_relative(d.created_at)),
        detail_row("Updated", format_relative(d.last_updated_at)),
        *custom_field_section(custom_fields, options_by_field),
        Rule(),
    ]
    content = (d.content or "").strip()
    widgets.append(
        Markdown(content, classes=content_class)
        if content
        else Static(Text("(empty)", style="italic"), classes=muted_class)
    )
    return widgets
