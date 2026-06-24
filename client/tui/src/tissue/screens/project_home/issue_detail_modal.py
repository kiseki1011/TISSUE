"""A centered, read-only issue detail modal. Used by the hub's expanded mode
(CTRL+F): with [2] hidden, pressing Enter on an issue row pops its detail here.
Renders the same read view as the dashboard (shared `issue_read_view`)."""

from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.widgets.issue_render import issue_read_view

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail

log = logging.getLogger(__name__)


class IssueDetailModal(TissueModal[None]):
    """Read-only issue detail in a centered dialog. Dismisses on Esc."""

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

    async def _load(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            issue = await client.issues.get_issue(self._issue_key)
        except TissueApiError as e:
            log.debug("Detail modal: failed to load %s: %s", self._issue_key, e)
            await self._mount([Static("Couldn't load issue.", classes="idm-muted")])
            return
        try:
            custom_fields = await client.issues.get_issue_custom_fields(self._issue_key)
        except TissueApiError as e:
            log.debug(
                "Detail modal: failed custom fields for %s: %s", self._issue_key, e
            )
            custom_fields = []
        options_by_field = await self._load_field_options(issue, custom_fields)
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
            )
        )

    async def _load_field_options(
        self,
        issue: IssueCommonDetail,
        custom_fields: list[CustomFieldValueInfo],
    ) -> dict[int, list[FieldOptionDetail]]:
        """The issue type's field options (field id -> options) so SELECT_OPTION /
        CHECKLIST fields display their names; best-effort, read-only."""
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
            log.debug("Detail modal: failed type %s options: %s", type_id, e)
            return {}
        return {
            f.id: list(f.options or [])
            for f in (issue_type.fields or [])
            if f.id is not None
        }

    async def _mount(self, widgets: list[Widget]) -> None:
        body = self.query_one("#idm-body", Vertical)
        with self.app.batch_update():
            await body.remove_children()
            await body.mount(*widgets)
