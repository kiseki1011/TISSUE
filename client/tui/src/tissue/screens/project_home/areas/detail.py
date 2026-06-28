from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual.widget import Widget
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home.areas.detail_render import DetailRenderMixin

if TYPE_CHECKING:
    from collections.abc import Callable

    from tissue.api.generated.models.available_transition import AvailableTransition
    from tissue.api.generated.models.custom_field_value_info import (
        CustomFieldValueInfo,
    )
    from tissue.api.generated.models.field_option_detail import FieldOptionDetail
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_detail_view import IssueDetailView
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )

log = logging.getLogger(__name__)


class DetailMixin(DetailRenderMixin):
    """Loads, caches, and mounts the [2] detail pane."""

    _DETAIL_DEBOUNCE = 0.10

    def _cancel_detail_timer(self) -> None:
        if self._detail_timer is not None:
            self._detail_timer.stop()
            self._detail_timer = None

    def _debounce_detail(
        self, render: Callable[[], object], *, immediate: bool
    ) -> None:
        self._cancel_detail_timer()
        if immediate:
            render()
        else:
            self._detail_timer = self.set_timer(self._DETAIL_DEBOUNCE, render)

    async def _render_issue_detail(
        self, issue_key: str, *, focus_detail: bool, force: bool = False
    ) -> None:
        if not self._start_issue_detail(issue_key):
            return

        cached = None if force else self._detail_state.cache.get(issue_key)
        if cached is not None:
            await self._show_cached_detail(issue_key, cached, focus_detail=focus_detail)
            return

        if not force:
            await self._show_skeleton(issue_key)

        view = await self._load_detail_view(issue_key)
        if view is None:
            return
        self._detail_state.cache[issue_key] = view
        await self._apply_detail_view(view, focus_detail=focus_detail)

    def _start_issue_detail(self, issue_key: str) -> bool:
        if self.app.client is None:
            return False
        self.remove_class("-no-timeline")
        self._detail_state.issue_key = issue_key
        self.run_worker(
            self._load_activity(issue_key), exclusive=True, group="hub-activity"
        )
        return True

    async def _show_cached_detail(
        self, issue_key: str, view: IssueDetailView, *, focus_detail: bool
    ) -> None:
        await self._apply_detail_view(view, focus_detail=focus_detail)
        self.run_worker(
            self._revalidate_detail(issue_key),
            exclusive=True,
            group="hub-detail-revalidate",
        )

    async def _show_skeleton(self, issue_key: str) -> None:
        summary = self._summary_for(issue_key)
        if summary is not None:
            await self._mount_detail(self._skeleton_widgets(summary))

    async def _load_detail_view(self, issue_key: str) -> IssueDetailView | None:
        client = self.app.client
        if client is None:
            return None
        try:
            return await client.issues.get_issue_detail(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load issue %s: %s", issue_key, error)
            if self._detail_state.issue_key == issue_key:
                await self._mount_detail(
                    [Static("Couldn't load issue.", classes="hub-muted")]
                )
            return None

    async def _prefetch_issue_details(self, issue_keys: list[str]) -> None:
        client = self.app.client
        if client is None:
            return
        for issue_key in issue_keys:
            if issue_key in self._detail_state.cache:
                continue
            try:
                view = await client.issues.get_issue_detail(issue_key)
            except TissueApiError as error:
                log.debug("Hub: failed to prefetch issue %s: %s", issue_key, error)
                continue
            self._detail_state.cache[issue_key] = view

    async def _revalidate_detail(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            fresh = await client.issues.get_issue_detail(issue_key)
        except TissueApiError as error:
            log.debug("Hub: failed to refresh issue %s: %s", issue_key, error)
            return
        if fresh == self._detail_state.cache.get(issue_key):
            return
        self._detail_state.cache[issue_key] = fresh
        await self._apply_detail_view(fresh, focus_detail=False)

    async def _apply_detail_view(
        self, view: IssueDetailView, *, focus_detail: bool
    ) -> None:
        issue = view.common
        if issue is None:
            await self._mount_detail(
                [Static("Couldn't load issue.", classes="hub-muted")]
            )
            return

        transitions = view.available_transitions or []
        custom_fields = view.custom_fields or []
        comments = (view.comments.content or []) if view.comments else []
        parent = view.parent
        children = view.children or []

        await self._load_detail_dependencies()
        if not self._is_current_detail(issue):
            return

        target_labels = self._build_transition_target_labels(transitions)
        options_by_field = self._custom_field_options(custom_fields)
        self._store_detail_state(
            issue, transitions, custom_fields, options_by_field, children, view
        )

        widgets = self._safe_issue_widgets(
            issue,
            transitions,
            target_labels,
            custom_fields,
            options_by_field,
            comments,
            parent,
            children,
        )
        await self._mount_detail(widgets)
        if focus_detail:
            self._focus_detail_body()

    async def _load_detail_dependencies(self) -> None:
        await self._ensure_issue_type_hierarchy()
        await self._ensure_sprint_index()

    def _is_current_detail(self, issue: IssueCommonDetail) -> bool:
        return self._detail_state.issue_key == issue.issue_key

    def _build_transition_target_labels(
        self, transitions: list[AvailableTransition]
    ) -> dict[int, str]:
        return {
            transition.transition_id: (
                (
                    transition.target_state.display_name
                    if transition.target_state
                    else None
                )
                or "?"
            )
            for transition in transitions
            if transition.transition_id is not None
        }

    def _custom_field_options(
        self, custom_fields: list[CustomFieldValueInfo]
    ) -> dict[int, list[FieldOptionDetail]]:
        return {
            custom_field.field_id: list(custom_field.options or [])
            for custom_field in custom_fields
            if custom_field.field_id is not None
        }

    def _store_detail_state(
        self,
        issue: IssueCommonDetail,
        transitions: list[AvailableTransition],
        custom_fields: list[CustomFieldValueInfo],
        options_by_field: dict[int, list[FieldOptionDetail]],
        children: list[IssueIdentifierResponse],
        view: IssueDetailView,
    ) -> None:
        self._detail_state.assigned = issue.assignee is not None
        self._transitions_by_id = {
            transition.transition_id: transition
            for transition in transitions
            if transition.transition_id is not None
        }
        self._detail_state.custom_fields = {
            custom_field.field_id: custom_field
            for custom_field in custom_fields
            if custom_field.field_id is not None
        }
        self._detail_state.field_options = options_by_field
        self._hierarchy_state.hierarchy = (
            self._hierarchy_state.issue_type_hierarchy.get(issue.issue_type.id)
            if issue.issue_type and issue.issue_type.id is not None
            else None
        )
        self._hierarchy_state.children = children
        self._relation_state.relations = view.relations

    async def _mount_detail(self, widgets: list[Widget]) -> None:
        panel = self._detail_panel()
        if panel is not None:
            await panel.replace_body(widgets)

    async def _reset_detail_pane(self) -> None:
        self._detail_state.issue_key = None
        self.remove_class("-no-timeline")
        await self._mount_detail(
            [Static("Select an issue to see details.", classes="hub-muted")]
        )
        await self._clear_timeline()

    def action_focus_detail(self) -> None:
        self._focus_detail_body()

    def _focus_detail_body(self) -> None:
        panel = self._detail_panel()
        if panel is not None:
            panel.focus_body()
