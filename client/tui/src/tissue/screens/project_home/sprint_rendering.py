from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Markdown, Rule, Static

from tissue.screens.home.rendering import _fit, _truncate
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home.rendering import _issue_rows, _sprint_status_chip
from tissue.util.datetime_fmt import format_date, format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.sprint_detail import SprintDetail
    from tissue.api.generated.models.sprint_summary import SprintSummary

SPRINT_FIELD_BY_BUTTON_ID = {
    "hub-sprint-edit-title": "title",
    "hub-sprint-edit-due": "dueAt",
}


def sprint_list_table(
    sprints: list[SprintSummary],
    theme_variables: dict[str, str],
) -> _DashTable:
    rows: list[list[str | Text]] = [
        [
            str(index + 1),
            _fit(sprint.sprint_key or "-", 9),
            Text(_truncate(sprint.title or "-", 20)),
            _sprint_status_chip(theme_variables, sprint.status, pad=False),
            format_date(sprint.due_at),
        ]
        for index, sprint in enumerate(sprints)
    ]
    return _DashTable(
        [
            ("#", None),
            ("Key", 9),
            ("Title", None),
            ("Status", 11),
            ("Due", 11),
        ],
        rows,
        id="hub-sprints-table",
        classes="hub-table",
    )


def sprint_detail_widgets(
    sprint: SprintDetail,
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    *,
    title_class: str,
    content_class: str,
    muted_class: str,
    issue_title_class: str,
    issue_table_id: str,
    issue_table_classes: str,
    spacer_class: str = "hub-detail-spacer",
    issue_empty_title_class: str | None = None,
    with_actions: bool = False,
    with_issue_remove: bool = False,
) -> list[Widget]:
    status = (sprint.status or "").upper()
    goal_editable = with_actions and status in ("PLANNING", "ACTIVE")
    widgets = sprint_meta_widgets(
        sprint,
        theme_variables,
        title_class=title_class,
        spacer_class=spacer_class,
        with_actions=with_actions,
    )
    widgets.extend(
        sprint_goal_widgets(
            sprint.goal,
            with_edit=goal_editable,
            title_class=title_class,
            content_class=content_class,
            muted_class=muted_class,
        )
    )
    widgets.append(Rule())
    widgets.extend(
        sprint_issue_widgets(
            issues,
            state_colors,
            theme_variables,
            issue_title_class=issue_title_class,
            issue_table_id=issue_table_id,
            issue_table_classes=issue_table_classes,
            issue_empty_title_class=issue_empty_title_class or issue_title_class,
            muted_class=muted_class,
            with_remove=with_issue_remove,
        )
    )
    return widgets


def sprint_goal_widgets(
    goal: str | None,
    *,
    with_edit: bool,
    title_class: str,
    content_class: str,
    muted_class: str,
) -> list[Widget]:
    header: list[Widget] = [Static("Goal", classes=title_class)]
    if with_edit:
        header.append(
            TextButton(
                "✎",
                id="hub-sprint-edit-goal",
                classes="hub-row-action hub-sprint-goal-edit",
            )
        )
    text = (goal or "").strip()
    body: Widget = (
        Markdown(text, classes=content_class)
        if text
        else Static("No goal yet.", classes=muted_class)
    )
    return [Horizontal(*header, classes="hub-title-row"), body]


def sprint_meta_widgets(
    sprint: SprintDetail,
    theme_variables: dict[str, str],
    *,
    title_class: str,
    spacer_class: str = "hub-detail-spacer",
    with_actions: bool = False,
) -> list[Widget]:
    status = (sprint.status or "").upper()
    can_act = with_actions and status in ("PLANNING", "ACTIVE")
    title_widget = Static(sprint.title or "-", markup=False, classes=title_class)
    title_block: Widget = (
        Horizontal(
            title_widget,
            _sprint_edit_button("hub-sprint-edit-title"),
            classes="hub-title-row",
        )
        if can_act
        else title_widget
    )
    return [
        title_block,
        Static("", classes=spacer_class),
        detail_row("Key", sprint.sprint_key or "-"),
        detail_row(
            "Status",
            _sprint_status_chip(theme_variables, sprint.status),
            action=TextButton("⇄", id="hub-sprint-transition", classes="hub-row-action")
            if can_act
            else None,
        ),
        detail_row(
            "Number",
            "-" if sprint.sprint_number is None else str(sprint.sprint_number),
        ),
        detail_row("Started", format_relative(sprint.started_at)),
        detail_row(
            "Due",
            format_relative(sprint.due_at),
            action=_sprint_edit_button("hub-sprint-edit-due")
            if can_act and status == "ACTIVE"
            else None,
        ),
        detail_row("Completed", format_relative(sprint.completed_at)),
        detail_row("Created", format_relative(sprint.created_at)),
        Rule(),
    ]


def sprint_issue_widgets(
    issues: list[IssueSummary],
    state_colors: dict[int, str],
    theme_variables: dict[str, str],
    *,
    issue_title_class: str,
    issue_table_id: str,
    issue_table_classes: str,
    issue_empty_title_class: str,
    muted_class: str,
    with_remove: bool = False,
) -> list[Widget]:
    if not issues:
        return [
            Static("Issues (0)", classes=issue_empty_title_class),
            Static("No issues.", classes=muted_class),
        ]

    title = Static(f"Issues ({len(issues)})", classes=issue_title_class)
    header: Widget = (
        Horizontal(title, sprint_remove_issue_button(), classes="hub-open-header")
        if with_remove
        else title
    )
    return [
        header,
        _DashTable(
            [
                ("Key", 10),
                ("Title", None),
                ("Status", 11),
                ("Priority", 8),
                ("Due", 11),
            ],
            _issue_rows(issues, state_colors, theme_variables, with_due=True),
            id=issue_table_id,
            classes=issue_table_classes,
        ),
    ]


def sprint_remove_issue_button() -> TextButton:
    button = TextButton("-", id="hub-sprint-remove-issue", classes="hub-row-action")
    button.tooltip = "Remove the focused issue from this sprint"
    return button


def _sprint_edit_button(button_id: str) -> TextButton:
    return TextButton("✎", id=button_id, classes="hub-row-action hub-sprint-field-edit")
