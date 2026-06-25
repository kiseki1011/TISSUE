from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual.containers import Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Static

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
from tissue.widgets.issue_render import issue_read_view

if TYPE_CHECKING:
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail

log = logging.getLogger(__name__)


class DetailsMixin(HomeScreenBase):
    """Render a selected project or issue into the Details panel.

    The issue view mirrors the project hub's read detail (same fields, same
    Status/Priority color chips), but read-only. It has none of these:
        - edit/transition controls
        - comments
        - activity timeline
    """

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
        box.can_focus = False
        return box

    def _render_project_detail(
        self, project: ProjectSummary, *, show_open_hint: bool = False
    ) -> None:
        widgets: list[Widget] = [
            Static(
                project.title or "-",
                markup=False,
                classes="dashboard-detail-title",
            ),
            _key_detail_row(project.key or "-"),
            detail_row("Visibility", _visibility_label(project.visibility)),
            detail_row("Created", format_relative(project.created_at)),
            detail_row("Updated", format_relative(project.last_updated_at)),
            detail_row("Archived", "Yes" if project.archived else "No"),
            Static(
                project.description or "No description.",
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
        """Fetch the full issue and render it read-only into the detail pane.

        The list summaries lack the type, assignee, author, body, and state
        color, so the full issue is fetched here.
        """
        client = self.app.client
        if client is None:
            return
        try:
            issue = await client.issues.get_issue(issue_key)
        except TissueApiError as error:
            log.debug("Dashboard: failed to load issue %s: %s", issue_key, error)
            self._mount_detail(
                [Static("Couldn't load issue.", classes="dashboard-muted")]
            )
            return
        try:
            custom_fields = await client.issues.get_issue_custom_fields(issue_key)
        except TissueApiError as error:
            log.debug(
                "Dashboard: failed to load custom fields for %s: %s",
                issue_key,
                error,
            )
            custom_fields = []
        options_by_field = await self._load_field_options(issue, custom_fields)
        # An unexpected issue shape should not take the whole app down
        widgets: list[Widget]
        try:
            widgets = self._issue_detail_widgets(issue, custom_fields, options_by_field)
        except Exception:
            log.exception("Dashboard: failed to build issue detail for %s", issue_key)
            widgets = [Static("Couldn't render this issue.", classes="dashboard-muted")]
        self._mount_detail(widgets)

    async def _load_field_options(
        self,
        issue: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
    ) -> dict[int, list[FieldOptionDetail]]:
        """Look up the issue type's field options, keyed by field id.

        Lets `SELECT_OPTION` and `CHECKLIST` custom fields show their option
        names. Fetched only when an option-bearing field is present. If it
        fails we skip it, since this view is read-only.
        """
        needs_options = any(
            field.issue_field_type in ("SELECT_OPTION", "CHECKLIST")
            for field in custom_fields
        )
        client = self.app.client
        if not needs_options or client is None:
            return {}
        type_id = issue.issue_type.id if issue.issue_type else None
        if type_id is None:
            return {}
        try:
            issue_type = await client.issues.get_issue_type(type_id)
        except TissueApiError as error:
            log.debug(
                "Dashboard: failed to load issue type %s options: %s",
                type_id,
                error,
            )
            return {}
        return {
            field.id: list(field.options or [])
            for field in (issue_type.fields or [])
            if field.id is not None
        }

    def _issue_detail_widgets(
        self,
        detail: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
    ) -> list[Widget]:
        # Shared read-only render with the dashboard's own CSS classes. The hub's
        # expanded-mode detail modal renders the same view so the two can't drift.
        return issue_read_view(
            detail,
            custom_fields,
            options_by_field,
            self.app.theme_variables,
            title_class="dashboard-detail-title",
            content_class="dashboard-content",
            muted_class="dashboard-muted",
        )

    async def _load_state_colors(self) -> None:
        """Map every workflow state id to its color.

        Lets the dashboard's issue tables tint each Status with its
        workflow-defined color. If it fails we skip it, leaving table statuses
        uncolored. The detail pane colors its Status straight from the issue's
        own state, so it never depends on this map.
        """
        client = self.app.client
        if client is None:
            return
        try:
            summaries = await client.workflows.list_workflows()
        except TissueApiError as error:
            log.debug("Dashboard: failed to list workflows: %s", error)
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
                except TissueApiError as error:
                    log.debug(
                        "Dashboard: failed to load workflow %s: %s",
                        workflow_id,
                        error,
                    )
                    continue
                self._workflow_cache[workflow_id] = workflow
            for state in workflow.states or []:
                if state.id is not None and state.color:
                    hex_color = color_hex(state.color)
                    if hex_color:
                        colors[state.id] = hex_color
        self._state_colors = colors

    def _mount_detail(self, widgets: list[Widget]) -> None:
        try:
            inner = self.query_one("#dashboard-detail-inner")
        except NoMatches:
            return
        # Batch the clear and remount so the pane repaints once, not as an empty
        # frame then a full one, which flickers when switching the selection.
        with self.app.batch_update():
            inner.remove_children()
            inner.mount(*widgets)
