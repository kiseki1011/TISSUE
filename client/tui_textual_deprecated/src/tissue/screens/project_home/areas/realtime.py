from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase

if TYPE_CHECKING:
    from tissue.api.realtime import RealtimeEvent

log = logging.getLogger(__name__)

# Types that can change the current user's assigned / in-progress header counts
_COUNT_TYPES = frozenset(
    {"ISSUE_ASSIGNED", "ISSUE_UNASSIGNED", "ISSUE_TRANSITIONED", "ISSUE_DELETED"}
)


class RealtimeMixin(ProjectHomeBase):
    """Applies incoming realtime (SSE) events to the [1] list and the open detail.

    For an existing issue, an event triggers a targeted refetch of just that issue,
    run through the same paths a local edit uses, so the change lands without flicker.
    A newly created issue instead reloads [1] in place, since its sorted position
    isn't known locally. Sprint events refresh the sprint list/detail when shown.
    """

    def handle_realtime_event(self, event: RealtimeEvent) -> None:
        if event.project_key != self._project_key:
            return
        if self._is_self_event(event):
            return
        if event.category == "sprint":
            self._handle_sprint_event(event)
            return
        if event.category != "issue":
            return
        if event.type == "ISSUE_CREATED":
            self._refresh_header()
            self._handle_remote_create()
            return
        issue_key = event.issue_key
        if issue_key is None:
            return
        if event.type in _COUNT_TYPES:
            self._refresh_header()
        if event.type == "ISSUE_DELETED":
            self._handle_remote_delete(issue_key)
            return
        if self._detail_state.issue_key == issue_key:
            self.run_worker(
                self._revalidate_detail(issue_key),
                exclusive=True,
                group="hub-detail-revalidate",
            )
        elif self._issue_in_list(issue_key):
            self.run_worker(
                self._live_patch_row(issue_key),
                exclusive=True,
                group=f"hub-live-{issue_key}",
            )

    def _handle_sprint_event(self, event: RealtimeEvent) -> None:
        # Any sprint change invalidates the cached id->sprint index used to name
        # sprints elsewhere.
        self._sprint_state.by_id = None
        if self._ui.view_mode != "sprints":
            return
        open_id = self._sprint_state.detail_id
        worker = (
            self._reload_and_select_sprint(open_id)
            if open_id is not None
            else self._load_sprints()
        )
        self.run_worker(worker, exclusive=True, group="hub-list")

    def _refresh_header(self) -> None:
        self.run_worker(self._load_header_stats(), exclusive=True, group="hub-header")

    def _handle_remote_create(self) -> None:
        """Reload [1] so a teammate's new issue appears, keeping the user's place.

        Skipped outside the plain issues view or during a search. The reload reapplies
        the current filter and sort server-side.
        """
        if self._ui.view_mode != "issues" or self._issue_list.keyword:
            return
        self.run_worker(self._live_reload_issues(), exclusive=True, group="hub-list")

    def _handle_remote_delete(self, issue_key: str) -> None:
        """Drop the stale cached view and, if it's the open issue, clear the pane so the
        user can't act on an issue that no longer exists."""
        self._detail_state.cache.pop(issue_key, None)
        if self._detail_state.issue_key == issue_key:
            self.app.notify("This issue was deleted.", severity="warning")
            self.run_worker(
                self._reset_detail_pane(), exclusive=True, group="hub-detail"
            )

    def _issue_in_list(self, issue_key: str) -> bool:
        return any(
            summary.issue_key == issue_key for summary in self._issue_list.issues
        ) or any(summary.issue_key == issue_key for summary in self._agent_work.issues)

    async def _live_patch_row(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        token = self._detail_state.begin_fetch(issue_key)
        try:
            view = await client.issues.get_issue_detail(issue_key)
        except TissueApiError as error:
            log.debug("Hub: realtime row refresh failed for %s: %s", issue_key, error)
            return
        if not self._detail_state.is_latest_fetch(issue_key, token):
            return
        self._detail_state.cache[issue_key] = view
        if view.common is not None:
            self._sync_list_row(view.common)

    def _is_self_event(self, event: RealtimeEvent) -> bool:
        """Our own change already updated the UI optimistically, so skip the echo."""
        actor = event.actor_member_id
        return actor is not None and actor == self._current_member_id()

    def _current_member_id(self) -> int | None:
        client = self.app.client
        profile = client.account.cached_profile if client is not None else None
        username = profile.username if profile is not None else None
        if not username:
            return None
        for member in self._member_list.members:
            if member.username == username:
                return member.member_id
        return None
