from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.css.query import NoMatches
from textual.widgets import DataTable, Input, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _VIEW_CYCLE
from tissue.screens.project_home.modals.create_sprint_modal import CreateSprintModal
from tissue.screens.project_home.sprint_rendering import sprint_list_table

if TYPE_CHECKING:
    from tissue.api.generated.models.page_response_sprint_summary import (
        PageResponseSprintSummary,
    )

log = logging.getLogger(__name__)


class SprintListMixin(ProjectHomeBase):
    """Switches [1] between issues, sprints, and members."""

    def action_cycle_view(self, direction: str = "next") -> None:
        keep_focus = self._should_refocus_list_after_switch()
        if keep_focus:
            self._focus_list_host()
        self._switch_view(self._adjacent_view_mode(direction), focus_list=keep_focus)

    def _should_refocus_list_after_switch(self) -> bool:
        focused = self.app.focused
        return focused is not None and focused.id in (
            "hub-issues-table",
            "hub-sprints-table",
            "hub-members-table",
            "hub-agent-issues-table",
            "hub-list-host",
            "hub-search",
        )

    def _focus_list_host(self) -> None:
        panel = self._issue_list_panel()
        if panel is not None:
            panel.focus_host()

    def _adjacent_view_mode(self, direction: str) -> str:
        step = -1 if direction == "prev" else 1
        current_index = _VIEW_CYCLE.index(self._ui.view_mode)
        return _VIEW_CYCLE[(current_index + step) % len(_VIEW_CYCLE)]

    def _set_view_chrome(self, mode: str) -> None:
        self._ui.view_mode = mode
        self._persist_project_ui()
        self._cancel_detail_timer()
        self._cancel_search_timer()
        self._clear_list_search()
        self._refresh_box_chrome()
        self._update_create_button()
        self._update_filter_button()
        self._update_search_input()
        self.refresh_bindings()

    def _clear_list_search(self) -> None:
        try:
            self.query_one("#hub-search", Input).value = ""
        except NoMatches:
            pass

    def _switch_view(self, mode: str, *, focus_list: bool = False) -> None:
        if self._ui.view_mode == mode:
            return
        self._set_view_chrome(mode)
        self._run_view_load(mode, focus_list=focus_list)

    def _run_view_load(self, mode: str, *, focus_list: bool = False) -> None:
        self.run_worker(
            self._load_view(mode, focus_list, self._search_keyword()),
            exclusive=True,
            group="hub-list",
        )

    async def _load_view(
        self, mode: str, focus_list: bool, keyword: str | None = None
    ) -> None:
        if mode == "sprints":
            await self._load_sprints()
        elif mode == "members":
            await self._load_members_list(keyword)
        elif mode in ("agent", "reviews"):
            await self._load_agent_issues(keyword)
        else:
            await self._load_issues(keyword)
        if focus_list:
            self.action_focus_issues()

    def _open_create_sprint(self) -> None:
        self.app.push_screen(
            CreateSprintModal(project_key=self._project_key), self._on_sprint_created
        )

    def _open_sprint_modal(self, sprint_id: int) -> None:
        from tissue.screens.project_home.modals.sprint_detail_modal import (
            SprintDetailModal,
        )

        self.app.push_screen(
            SprintDetailModal(
                sprint_id=sprint_id,
                project_key=self._project_key,
                state_colors=self._state_colors,
                is_manager=self._is_project_manager(),
            )
        )

    def _on_sprint_created(self, sprint_id: int | None) -> None:
        if sprint_id is None:
            return
        self._set_view_chrome("sprints")
        self.run_worker(
            self._reload_and_select_sprint(sprint_id),
            exclusive=True,
            group="hub-list",
        )

    async def _reload_and_select_sprint(self, sprint_id: int) -> None:
        await self._load_sprints()
        for index, sprint in enumerate(self._sprint_state.sprints):
            if sprint.id == sprint_id:
                self._select_sprint(index)
                return

    async def _ensure_sprints_loaded(self) -> None:
        if self._sprint_state.sprints:
            return
        page = await self._fetch_sprints()
        if page is not None:
            self._sprint_state.sprints = list(page.content or [])

    async def _load_sprints(self) -> None:
        page = await self._fetch_sprints(statuses=self._filters.sprint.statuses_arg())
        self._sprint_state.sprints = (
            list(page.content or []) if page is not None else []
        )
        await self._render_sprints()
        self._select_first_sprint()

    async def _fetch_sprints(
        self, *, statuses: list[str] | None = None
    ) -> PageResponseSprintSummary | None:
        client = self.app.client
        if client is None:
            return None
        try:
            return await client.sprints.list_project_sprints(
                self._project_key, statuses=statuses
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load sprints: %s", error)
            return None

    def _select_first_sprint(self) -> None:
        if self._sprint_state.sprints:
            self._select_sprint(0)

    async def _render_sprints(self) -> None:
        panel = self._issue_list_panel()
        if panel is None:
            return
        if not self._sprint_state.sprints:
            await panel.replace_content(
                [Static("No sprints.", classes="hub-list-empty")]
            )
            return
        await panel.replace_content(
            [sprint_list_table(self._sprint_state.sprints, self.app.theme_variables)]
        )

    @on(DataTable.RowHighlighted, "#hub-sprints-table")
    def _on_sprint_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_sprint(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-sprints-table")
    def _on_sprint_selected(self, event: DataTable.RowSelected) -> None:
        self._select_sprint(event.cursor_row, focus_detail=True)

    def _select_sprint(self, index: int, *, focus_detail: bool = False) -> None:
        if not (0 <= index < len(self._sprint_state.sprints)):
            return
        sprint_id = self._sprint_state.sprints[index].id
        if sprint_id is None:
            return
        if focus_detail and self._ui.expanded:
            self._open_sprint_modal(sprint_id)
            return
        self._debounce_detail(
            lambda: self.run_worker(
                self._render_sprint_detail(sprint_id, focus_detail=focus_detail),
                exclusive=True,
                group="hub-detail",
            ),
            immediate=focus_detail,
        )
