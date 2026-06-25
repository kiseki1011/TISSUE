from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual.containers import Vertical
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.rendering import _activity_details, _activity_label
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.color_type import color_hex

if TYPE_CHECKING:
    from tissue.api.generated.models.activity_log_response import ActivityLogResponse

log = logging.getLogger(__name__)


class ActivityMixin(ProjectHomeBase):
    """The list of the issue's recent events, shown on the right of the detail."""

    async def _load_activity(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            activities = await client.activity.list_issue_activities(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load activity for %s: %s", issue_key, error)
            activities = []
        # The detail may have been rebuilt for another issue while we awaited.
        if self._detail_issue_key != issue_key:
            return
        try:
            timeline_inner = self.query_one("#hub-detail-timeline-inner")
        except NoMatches:
            return
        widgets: list[Widget] = [Static("Activity", classes="hub-timeline-title")]
        if not activities:
            widgets.append(Static("No activity.", classes="hub-muted"))
        else:
            for entry in activities:
                widgets.extend(self._activity_widgets(entry))
        # Clear and add back in one step so the list draws once, not empty first.
        with self.app.batch_update():
            await timeline_inner.remove_children()
            await timeline_inner.mount(*widgets)

    def _activity_widgets(self, activity: ActivityLogResponse) -> list[Widget]:
        """Build one event block.

        Lines:
            - A ● event line
            - A │ details line
            - A │ line for each field that changed
        """
        # ANSI themes give accent an `ansi_*` name that Rich's Text style parser
        # won't take, so color_hex() turns it into a #hex.
        # Hex themes pass through unchanged.
        accent = color_hex(self.app.theme_variables.get("accent"))
        event_line = Text()
        event_line.append("● ", style=accent)
        event_line.append(_activity_label(activity))
        meta_line = " · ".join(
            filter(
                None,
                [
                    format_relative(activity.occurred_at),
                    self._member_label(activity.actor_member_id),
                ],
            )
        )
        children: list[Widget] = [
            Static(event_line, classes="hub-timeline-event"),
            Static(f"│  {meta_line}", markup=False, classes="hub-timeline-meta"),
        ]
        # Look up the field's label for old `customFields.{id}` change keys.
        field_names = {
            str(field_id): custom_field.field_label
            for field_id, custom_field in self._detail_custom_fields.items()
            if custom_field.field_label
        }
        for line in _activity_details(activity, field_names):
            children.append(
                Static(f"│  {line}", markup=False, classes="hub-timeline-change")
            )
        return [Vertical(*children, classes="hub-timeline-node")]

    def _member_label(self, member_id: int | None) -> str | None:
        if member_id is None:
            return None
        for member in self._members:
            if member.member_id == member_id:
                return member.display_name or member.username
        return None
