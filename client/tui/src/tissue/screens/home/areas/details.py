from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual.containers import Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Markdown, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.rendering import (
    _key_detail_row,
    _visibility_label,
)
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.color_type import color_hex
from tissue.widgets.detail_row import detail_row
from tissue.widgets.issue_render import (
    color_chip,
    custom_field_section,
    member_name,
    priority_chip,
    type_text,
)

if TYPE_CHECKING:
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail

log = logging.getLogger(__name__)


class DetailsMixin(HomeScreenBase):
    """The Details panel: render a selected project / issue into it.

    The issue view mirrors the project hub's read detail (same fields, same
    Status/Priority colour chips), but read-only — no edit/transition controls,
    no comments, no activity timeline."""

    def _box(self, title: str, box_id: str, children: list[Widget]) -> Vertical:
        box = Vertical(*children, id=box_id, classes="dashboard-box panel")
        box.border_title = title
        return box

    def _detail_box(self) -> VerticalScroll:
        inner = Vertical(
            Static("Select an item to see details.", classes="dashboard-muted"),
            id="dashboard-detail-inner",
        )
        box = VerticalScroll(inner, id="dashboard-detail", classes="dashboard-box")
        box.border_title = "Details"
        box.can_focus = False  # not a focus/nav target
        return box

    def _render_project_detail(
        self, p: ProjectSummary, *, show_open_hint: bool = False
    ) -> None:
        widgets: list[Widget] = [
            Static(p.title or "-", markup=False, classes="dashboard-detail-title"),
            _key_detail_row(p.key or "-"),
            detail_row("Visibility", _visibility_label(p.visibility)),
            detail_row("Created", format_relative(p.created_at)),
            detail_row("Updated", format_relative(p.last_updated_at)),
            detail_row("Archived", "Yes" if p.archived else "No"),
            Static(
                p.description or "No description.",
                markup=False,
                classes="dashboard-detail-desc",
            ),
        ]

        if show_open_hint:
            widgets.append(
                Static("Press Enter to open", classes="dashboard-detail-hint")
            )
        self._mount_detail(widgets)

    async def _render_issue_detail(self, issue_key: str) -> None:
        """Fetch the full issue (the list summaries lack the type/assignee/author/
        body and the state colour) and render it read-only into the detail pane."""
        client = self.app.client
        if client is None:
            return
        try:
            issue = await client.issues.get_issue(issue_key)
        except TissueApiError as e:
            log.debug("Dashboard: failed to load issue %s: %s", issue_key, e)
            self._mount_detail(
                [Static("Couldn't load issue.", classes="dashboard-muted")]
            )
            return
        try:
            custom_fields = await client.issues.get_issue_custom_fields(issue_key)
        except TissueApiError as e:
            log.debug(
                "Dashboard: failed to load custom fields for %s: %s", issue_key, e
            )
            custom_fields = []
        options_by_field = await self._load_field_options(issue, custom_fields)
        self._mount_detail(
            self._issue_detail_widgets(issue, custom_fields, options_by_field)
        )

    async def _load_field_options(
        self,
        issue: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
    ) -> dict[int, list[FieldOptionDetail]]:
        """The issue type's field options (field id -> options) so SELECT_OPTION /
        CHECKLIST custom fields display their option names. Fetched only when an
        option-bearing field is present; best-effort (read-only here)."""
        needs_options = any(
            cf.issue_field_type in ("SELECT_OPTION", "CHECKLIST")
            for cf in custom_fields
        )
        client = self.app.client
        if not needs_options or client is None:
            return {}
        type_id = issue.issue_type.id if issue.issue_type else None
        if type_id is None:
            return {}
        try:
            issue_type = await client.issues.get_issue_type(type_id)
        except TissueApiError as e:
            log.debug("Dashboard: failed to load issue type %s options: %s", type_id, e)
            return {}
        return {
            f.id: list(f.options or [])
            for f in (issue_type.fields or [])
            if f.id is not None
        }

    def _issue_detail_widgets(
        self,
        d: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
    ) -> list[Widget]:
        state = d.current_state
        current_state_label = (state.display_name if state else None) or "-"
        widgets: list[Widget] = [
            Static(d.title or "-", markup=False, classes="dashboard-detail-title"),
            detail_row("Key", d.issue_key or "-"),
            detail_row(
                "Status",
                color_chip(current_state_label, state.color if state else None),
            ),
            detail_row("Priority", priority_chip(self.app.theme_variables, d.priority)),
            detail_row("Type", type_text(d.issue_type)),
            detail_row("Assignee", member_name(d.assignee)),
            detail_row("Author", member_name(d.author)),
            detail_row(
                "Story points",
                "-" if d.story_point is None else str(d.story_point),
            ),
            detail_row("Due", format_relative(d.due_at)),
            detail_row("Created", format_relative(d.created_at)),
            detail_row("Updated", format_relative(d.last_updated_at)),
            # Custom fields: a blank line below the standard fields, read-only here
            # (the dashboard detail has no edit controls).
            *custom_field_section(custom_fields, options_by_field),
            Rule(),
        ]
        content = (d.content or "").strip()
        widgets.append(
            Markdown(content, classes="dashboard-content")
            if content
            else Static(Text("(empty)", style="italic"), classes="dashboard-muted")
        )
        return widgets

    async def _load_state_colors(self) -> None:
        """Build a state-id -> colour map from every workflow so the dashboard's
        issue tables can tint each Status with its workflow-defined colour.

        Best-effort: a failure just leaves table statuses uncoloured. The detail
        pane colours its Status straight from the issue's own state, so it never
        depends on this map."""
        client = self.app.client
        if client is None:
            return
        try:
            summaries = await client.workflows.list_workflows()
        except TissueApiError as e:
            log.debug("Dashboard: failed to list workflows: %s", e)
            return
        colors: dict[int, str] = {}
        for summary in summaries:
            workflow_id = summary.id
            if workflow_id is None:
                continue
            workflow = self._workflow_cache.get(workflow_id)
            if workflow is None:
                try:
                    workflow = await client.workflows.get_workflow(workflow_id)
                except TissueApiError as e:
                    log.debug(
                        "Dashboard: failed to load workflow %s: %s", workflow_id, e
                    )
                    continue
                self._workflow_cache[workflow_id] = workflow
            for s in workflow.states or []:
                if s.id is not None and s.color:
                    hex_color = color_hex(s.color)
                    if hex_color:
                        colors[s.id] = hex_color
        self._state_colors = colors

    def _mount_detail(self, widgets: list[Widget]) -> None:
        try:
            inner = self.query_one("#dashboard-detail-inner")
        except NoMatches:
            return
        # Batch the clear + remount so the pane repaints once, not as an empty
        # frame then a full one (the flicker when switching the selection).
        with self.app.batch_update():
            inner.remove_children()
            inner.mount(*widgets)
