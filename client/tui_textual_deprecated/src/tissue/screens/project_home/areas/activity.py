from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual.containers import Vertical
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
    """Issue activity timeline."""

    async def _load_activity(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            activities = await client.activity.list_issue_activities(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load activity for %s: %s", issue_key, error)
            activities = []
        if self._detail_state.issue_key != issue_key:
            return
        panel = self._activity_panel()
        if panel is None:
            return
        widgets: list[Widget] = [Static("Activity", classes="hub-timeline-title")]
        if not activities:
            widgets.append(Static("No activity.", classes="hub-muted"))
        else:
            for entry in activities:
                widgets.extend(self._activity_widgets(entry))
        await panel.replace(widgets)

    def _activity_widgets(self, activity: ActivityLogResponse) -> list[Widget]:
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
        field_names = {
            str(field_id): custom_field.field_label
            for field_id, custom_field in self._detail_state.custom_fields.items()
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
        for member in self._member_list.members:
            if member.member_id == member_id:
                return member.display_name or member.username
        return None
