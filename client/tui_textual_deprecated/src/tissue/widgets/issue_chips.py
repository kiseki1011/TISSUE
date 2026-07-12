from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text

from tissue.widgets.color_type import chip_style, color_hex

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_type_info import IssueTypeInfo

PRIORITY_VAR: dict[str, str] = {
    "P0": "error",
    "P1": "warning",
    "P2": "primary",
    "P3": "secondary",
    "P4": "success",
}

REVIEW_STATUS_CHIP: dict[str, tuple[str, str]] = {
    "PENDING": ("Pending", "secondary"),
    "APPROVED": ("Approved", "success"),
    "CHANGES_REQUESTED": ("Changes requested", "warning"),
}

_REVIEW_STATUS_SHORT: dict[str, str] = {"CHANGES_REQUESTED": "Changes"}


def color_chip(label: str, color: str | None, *, pad: bool = True) -> str | Text:
    """Render `label` as a colored pill, falling back to plain text."""
    style = chip_style(color)
    if not style:
        return Text(label)
    return Text(f" {label} " if pad else label, style=style)


def priority_chip(theme_variables: dict[str, str], priority: str | None) -> str | Text:
    if not priority:
        return "-"
    variable = PRIORITY_VAR.get(priority)
    bg = theme_variables.get(variable) if variable else None
    return color_chip(priority, bg)


def review_status_chip(
    theme_variables: dict[str, str],
    status: str | None,
    *,
    compact: bool = False,
    pad: bool = True,
) -> str | Text:
    if not status:
        return "-"
    label, variable = REVIEW_STATUS_CHIP.get(status, (status, "secondary"))
    if compact:
        label = _REVIEW_STATUS_SHORT.get(status, label)
    return color_chip(label, theme_variables.get(variable), pad=pad)


def type_chip(name: str | None, color: str | None) -> str | Text:
    if not name:
        return "-"
    hex_color = color_hex(color)
    return Text(name, style=hex_color) if hex_color else Text(name)


def type_text(issue_type: IssueTypeInfo | None) -> str | Text:
    if issue_type is None:
        return "-"
    return Text(issue_type.display_name or "-", style="bold")
