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
    """The detail's right-hand activity timeline: load the issue's recent events
    and render each as a ● node with its detail lines."""

    async def _load_activity(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            activities = await client.activity.list_issue_activities(issue_key)
        except TissueApiError as e:
            log.debug("Hub: failed to load activity for %s: %s", issue_key, e)
            activities = []
        # The detail may have been rebuilt for another issue while we awaited.
        if self._detail_issue_key != issue_key:
            return
        try:
            box = self.query_one("#hub-detail-timeline-inner")
        except NoMatches:
            return
        widgets: list[Widget] = [Static("Activity", classes="hub-timeline-title")]
        if not activities:
            widgets.append(Static("No activity.", classes="hub-muted"))
        else:
            for entry in activities:
                widgets.extend(self._activity_widgets(entry))
        # Batch the clear + remount so the timeline repaints once (not as an empty
        # frame then a loaded one) — same anti-flicker as detail._mount_detail.
        with self.app.batch_update():
            await box.remove_children()
            await box.mount(*widgets)

    def _activity_widgets(self, a: ActivityLogResponse) -> list[Widget]:
        """A timeline node: a ● event line, a │ meta line (relative time · actor),
        and a │ line per field change (e.g. a transition's before → after). Each
        node is wrapped so events stay visually grouped and evenly spaced."""
        # Resolve the theme's accent to a #hex — ANSI themes (ansi-dark/ansi-light)
        # expose it as an `ansi_*` name that Textual understands but Rich's Text
        # style parser does NOT, so passing it raw crashes rendering. color_hex()
        # routes it through Textual's parser (and passes hex themes through).
        accent = color_hex(self.app.theme_variables.get("accent"))
        head = Text()
        head.append("● ", style=accent)
        head.append(_activity_label(a))
        sub = " · ".join(
            filter(
                None,
                [format_relative(a.occurred_at), self._member_label(a.actor_member_id)],
            )
        )
        children: list[Widget] = [
            Static(head, classes="hub-timeline-event"),
            Static(f"│  {sub}", markup=False, classes="hub-timeline-meta"),
        ]
        # Resolve any legacy `customFields.{id}` change key to the field's name,
        # using the custom field definitions loaded for the current issue.
        field_names = {
            str(field_id): cf.field_label
            for field_id, cf in self._detail_custom_fields.items()
            if cf.field_label
        }
        for line in _activity_details(a, field_names):
            children.append(
                Static(f"│  {line}", markup=False, classes="hub-timeline-change")
            )
        return [Vertical(*children, classes="hub-timeline-node")]

    def _member_label(self, member_id: int | None) -> str | None:
        """Resolve an actor member id to a display name via the loaded members."""
        if member_id is None:
            return None
        for member in self._members:
            if member.member_id == member_id:
                return member.display_name or member.username
        return None
