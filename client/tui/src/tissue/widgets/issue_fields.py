from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual.widget import Widget
from textual.widgets import Markdown, Static

from tissue.widgets.detail_row import detail_row

if TYPE_CHECKING:
    from collections.abc import Callable

    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.project_member_info import ProjectMemberInfo

_PROGRESS_WIDTH = 10


def _progress_bar(pct: int) -> Text:
    pct = max(0, min(100, pct))
    filled = round(pct / 100 * _PROGRESS_WIDTH)
    bar = Text()
    bar.append("█" * filled)
    bar.append("░" * (_PROGRESS_WIDTH - filled), style="dim")
    bar.append(f"  {pct}%")
    return bar


def progress_block(detail: IssueCommonDetail) -> list[Widget]:
    if detail.count_based_progress is None:
        return []
    return [detail_row("Progress", _progress_bar(detail.count_based_progress))]


def member_name(info: ProjectMemberInfo | None) -> str:
    if info is None:
        return "-"
    return info.display_name or info.username or "-"


def custom_field_label(info: CustomFieldValueInfo) -> str:
    label = info.field_label or "Field"
    return label[:1].upper() + label[1:]


def custom_field_display_value(
    info: CustomFieldValueInfo,
    options_by_field: dict[int, list[FieldOptionDetail]],
) -> str:
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
        name = next((option.name for option in options if option.id == value), None)
        return name or str(value)
    if field_type == "CHECKLIST":
        if isinstance(value, dict):
            try:
                checked = {int(key) for key, is_checked in value.items() if is_checked}
            except ValueError:
                return str(value)
            names = [
                option.name or "-" for option in (options or []) if option.id in checked
            ]
            return ", ".join(names) if names else "-"
        if isinstance(value, list):
            return ", ".join(str(v) for v in value) if value else "-"
    return str(value)


def custom_field_section(
    custom_fields: list[CustomFieldValueInfo],
    options_by_field: dict[int, list[FieldOptionDetail]],
    *,
    edit_button: Callable[[int], Widget] | None = None,
) -> list[Widget]:
    if not custom_fields:
        return []
    widgets: list[Widget] = [Static("", classes="detail-gap")]
    for field in custom_fields:
        label = custom_field_label(field)
        action = (
            edit_button(field.field_id)
            if edit_button is not None and field.field_id is not None
            else None
        )
        body = str(field.value).strip() if field.value else ""
        if field.issue_field_type == "TEXT" and body:
            widgets.append(detail_row(label, "", action=action))
            widgets.append(Markdown(body, classes="cf-text"))
        else:
            widgets.append(
                detail_row(
                    label,
                    custom_field_display_value(field, options_by_field),
                    action=action,
                )
            )
    return widgets
