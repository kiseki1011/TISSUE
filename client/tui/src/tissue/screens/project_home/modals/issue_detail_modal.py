from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.screens.base import ScrollableModal
from tissue.widgets.issue_link import IssueLink
from tissue.widgets.issue_read import issue_read_view

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail

log = logging.getLogger(__name__)


class IssueDetailModal(ScrollableModal[None]):
    """Read-only issue detail dialog opened from the hub's expanded mode."""

    CSS_PATH = "issue_detail_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(self, *, issue_key: str) -> None:
        super().__init__()
        self._issue_key = issue_key

    def compose(self) -> ComposeResult:
        with Container(id="idm-dialog", classes="dialog"):
            with VerticalScroll(id="idm-scroll"):
                yield Vertical(Static("Loading…", classes="idm-muted"), id="idm-body")

    def on_mount(self) -> None:
        dialog = self.query_one("#idm-dialog", Container)
        dialog.border_title = self._issue_key
        dialog.border_subtitle = "Esc to close"
        self.run_worker(self._load(), group="idm-load")

    def action_close(self) -> None:
        self.dismiss(None)

    @on(IssueLink.Clicked)
    def _on_hier_open(self, event: IssueLink.Clicked) -> None:
        """Open the linked issue. A click on this same issue is ignored."""
        event.stop()
        if event.issue_key != self._issue_key:
            self.app.push_screen(IssueDetailModal(issue_key=event.issue_key))

    async def _load(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            issue = await client.issues.get_issue(self._issue_key)
        except TissueApiError as error:
            log.debug("Detail modal: failed to load %s: %s", self._issue_key, error)
            await self._mount([Static("Couldn't load issue.", classes="idm-muted")])
            return
        try:
            custom_fields = await client.issues.get_issue_custom_fields(self._issue_key)
        except TissueApiError as error:
            log.debug(
                "Detail modal: failed custom fields for %s: %s", self._issue_key, error
            )
            custom_fields = []
        options_by_field = await self._load_field_options(issue, custom_fields)
        try:
            parent = await client.issues.get_issue_parent(self._issue_key)
        except TissueApiError as error:
            log.debug("Detail modal: failed parent for %s: %s", self._issue_key, error)
            parent = None
        try:
            children = await client.issues.get_issue_children(self._issue_key)
        except TissueApiError as error:
            log.debug(
                "Detail modal: failed children for %s: %s", self._issue_key, error
            )
            children = []
        try:
            relations = await client.issues.get_issue_relations(self._issue_key)
        except TissueApiError as error:
            log.debug(
                "Detail modal: failed relations for %s: %s", self._issue_key, error
            )
            relations = None
        await self._mount(
            issue_read_view(
                issue,
                custom_fields,
                options_by_field,
                self.app.theme_variables,
                title_class="idm-title",
                content_class="idm-content",
                muted_class="idm-muted",
                show_reviewers=True,
                parent=parent,
                children=children,
                relations=relations,
            )
        )

    async def _load_field_options(
        self,
        issue: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
    ) -> dict[int, list[FieldOptionDetail]]:
        """Field options keyed by field id, so SELECT_OPTION and CHECKLIST
        fields can show their names.

        Read-only. If it fails we just skip it.
        """
        needs_options = any(
            custom_field.issue_field_type in ("SELECT_OPTION", "CHECKLIST")
            for custom_field in custom_fields
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
            log.debug("Detail modal: failed type %s options: %s", type_id, error)
            return {}
        return {
            field.id: list(field.options or [])
            for field in (issue_type.fields or [])
            if field.id is not None
        }

    async def _mount(self, widgets: list[Widget]) -> None:
        body = self.query_one("#idm-body", Vertical)
        with self.app.batch_update():
            await body.remove_children()
            await body.mount(*widgets)
