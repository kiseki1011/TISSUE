"""Pure issue-rendering helpers, shared so every screen renders an issue alike."""

from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual.widget import Widget
from textual.widgets import Markdown, Rule, Static

from tissue.util.datetime_fmt import format_relative
from tissue.widgets.color_type import chip_style, color_hex
from tissue.widgets.detail_row import detail_row
from tissue.widgets.issue_link import IssueLink, IssueRefRow

if TYPE_CHECKING:
    from collections.abc import Callable

    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )
    from tissue.api.generated.models.issue_relations_detail import (
        IssueRelationsDetail,
    )
    from tissue.api.generated.models.issue_type_info import IssueTypeInfo
    from tissue.api.generated.models.project_member_info import ProjectMemberInfo
    from tissue.api.generated.models.related_issue_info import RelatedIssueInfo

# Priority has no server-defined color, so the TUI fixes one. Each level maps to a
# theme variable used as the chip background (P0 loudest, P4 softest).
PRIORITY_VAR: dict[str, str] = {
    "P0": "error",
    "P1": "warning",
    "P2": "primary",
    "P3": "secondary",
    "P4": "success",
}

# Reviewer ReviewStatus -> (short label, theme color variable). Pending is muted,
# approved reads as success, changes-requested as a warning (work needed, not error).
REVIEW_STATUS_CHIP: dict[str, tuple[str, str]] = {
    "PENDING": ("Pending", "secondary"),
    "APPROVED": ("Approved", "success"),
    "CHANGES_REQUESTED": ("Changes requested", "warning"),
}


def color_chip(label: str, color: str | None, *, pad: bool = True) -> str | Text:
    """`label` as a solid pill, with `color` as the background.

    `color` is a ColorType enum name or a hex string. Falls back to plain text
    when there's no color. `pad=False` drops the surrounding spaces to fit a
    tight column.
    """
    style = chip_style(color)
    if not style:
        # Return a Text so DataTable doesn't parse a bare str as markup and crash
        # on a label containing a '[...]' sequence.
        return Text(label)
    return Text(f" {label} " if pad else label, style=style)


def priority_chip(theme_variables: dict[str, str], priority: str | None) -> str | Text:
    """Pn as a background pill, colored from a fixed priority->theme map and the
    screen's theme variables."""
    if not priority:
        return "-"
    variable = PRIORITY_VAR.get(priority)
    bg = theme_variables.get(variable) if variable else None
    return color_chip(priority, bg)


# Shorter labels for tight list-table columns ("Changes requested" is too wide
# there). The detail pane keeps the full REVIEW_STATUS_CHIP labels.
_REVIEW_STATUS_SHORT: dict[str, str] = {"CHANGES_REQUESTED": "Changes"}


def review_status_chip(
    theme_variables: dict[str, str],
    status: str | None,
    *,
    compact: bool = False,
    pad: bool = True,
) -> str | Text:
    """A reviewer's ReviewStatus as a background pill colored from a fixed status ->
    theme map.

    ReviewStatus:
        - Pending
        - Approved
        - Changes requested

    `compact` swaps in a shorter label for table columns, `pad=False` drops the
    surrounding spaces."""
    if not status:
        return "-"
    label, variable = REVIEW_STATUS_CHIP.get(status, (status, "secondary"))
    if compact:
        label = _REVIEW_STATUS_SHORT.get(status, label)
    return color_chip(label, theme_variables.get(variable), pad=pad)


def type_chip(name: str | None, color: str | None) -> str | Text:
    """Issue type as colored TEXT.

    The type's ColorType tints the foreground only, not a background pill, so it reads
    differently from the Status/Priority chips. The color goes through `color_hex`
    (which also dodges the ANSI-theme crash). Falls back to plain text / '-' when
    there's no type or color."""
    if not name:
        return "-"
    hex_color = color_hex(color)
    return Text(name, style=hex_color) if hex_color else Text(name)


def type_text(issue_type: IssueTypeInfo | None) -> str | Text:
    """Issue type in bold (no color)."""
    if issue_type is None:
        return "-"
    return Text(issue_type.display_name or "-", style="bold")


def _ref_link(key: str, issue_type: IssueTypeInfo | None) -> IssueLink:
    """The issue key as a clickable link, followed by its type label tinted with the
    type's color.

    The key inherits the link color (so it brightens on hover), the type keeps its own
    color."""
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
    """A related-issue row (a hierarchy parent/child or a relation).

    The key as a link, its type label in the type's color, and a status chip on
    the right. An optional `prefix` (e.g. a relation's direction arrow + verb)
    leads the row, `remove_button` (a ✕) trails it for interactive sections."""
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


_PROGRESS_WIDTH = 10


def _progress_bar(pct: int) -> Text:
    """A 10-cell filled/empty block bar plus the percentage.

    Theme-agnostic. Filled cells use the row's text color, empty cells are dimmed.
    No theme variable, so it can't hit the ANSI-theme Rich-style crash."""
    pct = max(0, min(100, pct))
    filled = round(pct / 100 * _PROGRESS_WIDTH)
    bar = Text()
    bar.append("█" * filled)
    bar.append("░" * (_PROGRESS_WIDTH - filled), style="dim")
    bar.append(f"  {pct}%")
    return bar


def progress_block(detail: IssueCommonDetail) -> list[Widget]:
    """A single count-based Progress row (done children / total children), for any
    issue that has children.

    Shown only when the value is present, so leaf issues, and parents with no
    children yet, show nothing.

    The point-based progress the server also computes for EPICs is intentionally not
    shown. One consistent progress metric across every hierarchy level reads more
    clearly than two stacked bars.
    """
    if detail.count_based_progress is None:
        return []
    return [detail_row("Progress", _progress_bar(detail.count_based_progress))]


def member_name(info: ProjectMemberInfo | None) -> str:
    if info is None:
        return "-"
    return info.display_name or info.username or "-"


def custom_field_label(info: CustomFieldValueInfo) -> str:
    """A custom field's label with its first letter capitalized (the server stores
    them lower/camel-cased, e.g. 'reproduceSteps')."""
    label = info.field_label or "Field"
    return label[:1].upper() + label[1:]


def custom_field_display_value(
    info: CustomFieldValueInfo,
    options_by_field: dict[int, list[FieldOptionDetail]],
) -> str:
    """A custom field's value as display text, formatted by its field type.

    Field type:
        - `BOOLEAN` -> Yes/No
        - `PERCENTAGE` -> n%
        - `SELECT_OPTION` -> the option's name
        - `CHECKLIST` -> the checked options' names

    `options_by_field` (field id -> options) maps SELECT/CHECKLIST ids to names.
    TEXT is handled separately (rendered as Markdown), so it just falls through to
    its raw string here.
    """
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
        # Stored as {str(optionId): checked}, so show the checked options' names.
        if isinstance(value, dict):
            try:
                checked = {int(key) for key, is_checked in value.items() if is_checked}
            except ValueError:
                return str(value)  # a non-numeric key, show the raw value, don't crash
            names = [
                option.name or "-" for option in (options or []) if option.id in checked
            ]
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
    """The custom-field rows for the detail pane.

    A TEXT field with content renders as a borderless Markdown box under a label
    row. An empty TEXT field and every other type render as a normal `key: value`
    row. `edit_button(field_id)` adds a ✎ action to each row (project hub), pass
    None for a read-only section (dashboard). Leads with a blank spacer so the
    section sits a line below the standard fields.
    """
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


def reviewer_read_block(
    detail: IssueCommonDetail, theme_variables: dict[str, str]
) -> list[Widget]:
    """A read-only reviewers block, a bold 'Reviewers' header then one row per
    reviewer (name on the left, status chip on the right).

    Empty when the issue has no reviewers. Leads with a blank spacer so it sits a
    line below the fields above it. Used by read views that should surface reviewers
    (e.g. the detail modal)."""
    reviewers = detail.reviewers or []
    if not reviewers:
        return []
    widgets: list[Widget] = [
        Static("", classes="detail-gap"),
        Static(Text("Reviewers", style="bold")),
    ]
    for reviewer in reviewers:
        widgets.append(
            detail_row(
                member_name(reviewer.participant),
                review_status_chip(theme_variables, reviewer.status),
            )
        )
    return widgets


def hierarchy_read_block(
    parent: IssueIdentifierResponse | None,
    children: list[IssueIdentifierResponse] | None,
) -> list[Widget]:
    """A read-only parent/children block.

    A bold 'Parent' header + its link row, and/or a 'Children' header + link rows,
    with a blank line between the two. Each row shows the key (link) + colored type +
    status chip. Empty (returns []) when the issue has neither. Leads with a blank
    spacer."""
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


# One entry per relation row, in display order, as
# (IssueRelationsDetail attr, direction arrow, verb label, removable from THIS issue's
# side). The arrow reads relative to the current issue:
#   - → this issue acts on the other
#   - ← the other acts on this issue
#   - ↔ mutual
# Directional incoming relations (blocked_by/caused_by/duplicated_by) are owned by the
# other issue so they're read-only here. RELEVANT is symmetric (removable from either
# side).
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
    """One row per relation, no group headers.

    A direction arrow + verb prefix, then the related issue (key link + colored type
    + status chip), then a ✕ on removable rows. `remove_button(key)` builds the ✕ for
    removable rows (outgoing + relevant), pass None for a read-only block."""
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
    """A read-only relations block, a bold 'Relations' header then one directional row
    per related issue.

    Empty (returns []) when the issue has none. Leads with a blank spacer."""
    rows = relation_rows(relations)
    if not rows:
        return []
    return [
        Static("", classes="detail-gap"),
        Static(Text("Relations", style="bold")),
        *rows,
    ]


def issue_read_view(
    detail: IssueCommonDetail,
    custom_fields: list[CustomFieldValueInfo],
    options_by_field: dict[int, list[FieldOptionDetail]],
    theme_variables: dict[str, str],
    *,
    title_class: str = "detail-title",
    content_class: str = "detail-content",
    muted_class: str = "detail-muted",
    show_reviewers: bool = False,
    parent: IssueIdentifierResponse | None = None,
    children: list[IssueIdentifierResponse] | None = None,
    relations: IssueRelationsDetail | None = None,
) -> list[Widget]:
    """A read-only issue detail (title, field rows, custom fields, then the body).

    Shared by the dashboard's detail pane and the hub's detail modal so they can't
    drift. Callers pass their own CSS class names for the title/body/empty text.
    `show_reviewers` adds a read-only reviewers block before the body (the inline
    hub pane renders its own interactive reviewer section instead). `parent` and
    `children` add a read-only hierarchy block after the reviewers, omit them for a
    flat read view.
    """
    state = detail.current_state
    current_state_label = (state.display_name if state else None) or "-"
    widgets: list[Widget] = [
        Static(detail.title or "-", markup=False, classes=title_class),
        detail_row("Key", detail.issue_key or "-"),
        detail_row(
            "Status", color_chip(current_state_label, state.color if state else None)
        ),
        detail_row("Priority", priority_chip(theme_variables, detail.priority)),
        detail_row("Type", type_text(detail.issue_type)),
        detail_row("Assignee", member_name(detail.assignee)),
        detail_row("Author", member_name(detail.author)),
        detail_row(
            "Story points",
            "-" if detail.story_point is None else str(detail.story_point),
        ),
        *progress_block(detail),
        detail_row("Due", format_relative(detail.due_at)),
        detail_row("Created", format_relative(detail.created_at)),
        detail_row("Updated", format_relative(detail.last_updated_at)),
        *custom_field_section(custom_fields, options_by_field),
        *(reviewer_read_block(detail, theme_variables) if show_reviewers else []),
        *hierarchy_read_block(parent, children),
        *relations_read_block(relations),
        Rule(),
    ]
    content = (detail.content or "").strip()
    widgets.append(
        Markdown(content, classes=content_class)
        if content
        else Static(Text("(empty)", style="italic"), classes=muted_class)
    )
    return widgets
