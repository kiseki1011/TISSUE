from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual.widget import Widget
from textual.widgets import Static

from tissue.widgets.color_type import color_hex
from tissue.widgets.issue_chips import color_chip
from tissue.widgets.issue_link import IssueLink, IssueRefRow

if TYPE_CHECKING:
    from collections.abc import Callable

    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_relations_detail import (
        IssueRelationsDetail,
    )
    from tissue.api.generated.models.issue_type_info import IssueTypeInfo
    from tissue.api.generated.models.related_issue_info import RelatedIssueInfo


def _ref_link(key: str, issue_type: IssueTypeInfo | None) -> IssueLink:
    label = Text(key)
    if issue_type is not None and issue_type.display_name:
        label.append("  ")
        label.append(issue_type.display_name, style=color_hex(issue_type.color))
    return IssueLink(key, label)


def issue_ref_row(
    ref: IssueIdentifierResponse | RelatedIssueInfo,
    *,
    prefix: Widget | None = None,
    remove_button: Widget | None = None,
) -> IssueRefRow:
    key = ref.issue_key or "-"
    state = ref.current_state
    status_label = (state.display_name if state else None) or "-"
    status = color_chip(status_label, state.color if state else None)
    status_text = status if isinstance(status, Text) else Text(status)
    children: list[Widget] = []
    if prefix is not None:
        children.append(prefix)
    children.append(_ref_link(key, ref.issue_type))
    children.append(Static(status_text, classes="iref-status"))
    if remove_button is not None:
        children.append(remove_button)
    return IssueRefRow(*children)


def hierarchy_read_block(
    parent: IssueIdentifierResponse | None,
    children: list[IssueIdentifierResponse] | None,
) -> list[Widget]:
    has_parent = parent is not None and bool(parent.issue_key)
    kids = [child for child in (children or []) if child.issue_key]
    if not has_parent and not kids:
        return []
    widgets: list[Widget] = [Static("", classes="detail-gap")]
    if has_parent and parent is not None:
        widgets.append(Static(Text("Parent", style="bold")))
        widgets.append(issue_ref_row(parent))
    if kids:
        if has_parent:
            widgets.append(Static("", classes="detail-gap"))
        widgets.append(Static(Text("Children", style="bold")))
        widgets.extend(issue_ref_row(child) for child in kids)
    return widgets


_RELATION_ROWS: list[tuple[str, str, str, bool]] = [
    ("blocks", "→", "Blocks", True),
    ("blocked_by", "←", "Blocked by", False),
    ("causes", "→", "Causes", True),
    ("caused_by", "←", "Caused by", False),
    ("duplicates", "→", "Duplicates", True),
    ("duplicated_by", "←", "Duplicated by", False),
    ("relevant", "↔", "Relevant", True),
]


def relation_rows(
    relations: IssueRelationsDetail | None,
    *,
    remove_button: Callable[[str], Widget] | None = None,
) -> list[Widget]:
    if relations is None:
        return []
    rows: list[Widget] = []
    for attr, arrow, label, removable in _RELATION_ROWS:
        for item in getattr(relations, attr) or []:
            key = item.issue_key
            button = (
                remove_button(key) if (removable and remove_button and key) else None
            )
            prefix = Static(f"{arrow} {label}", classes="iref-rel-label")
            rows.append(issue_ref_row(item, prefix=prefix, remove_button=button))
    return rows


def relations_read_block(relations: IssueRelationsDetail | None) -> list[Widget]:
    rows = relation_rows(relations)
    if not rows:
        return []
    return [
        Static("", classes="detail-gap"),
        Static(Text("Relations", style="bold")),
        *rows,
    ]
