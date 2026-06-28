from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.containers import Horizontal
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Button, DataTable, Input, Markdown, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.rendering import _fit, _truncate
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import _VIEW_CYCLE
from tissue.screens.project_home.modals.confirm_modal import ConfirmModal
from tissue.screens.project_home.modals.create_sprint_modal import CreateSprintModal
from tissue.screens.project_home.modals.sprint_field_edit_modal import (
    SprintFieldEditModal,
)
from tissue.screens.project_home.modals.sprint_goal_edit_modal import (
    SprintGoalEditModal,
)
from tissue.screens.project_home.modals.sprint_transition_modal import (
    SprintTransitionModal,
)
from tissue.screens.project_home.rendering import _issue_rows, _sprint_status_chip
from tissue.util.datetime_fmt import format_date, format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.page_response_sprint_summary import (
        PageResponseSprintSummary,
    )
    from tissue.api.generated.models.sprint_detail import SprintDetail
    from tissue.api.generated.models.sprint_summary import SprintSummary

log = logging.getLogger(__name__)

_SPRINT_FIELD_BY_ID = {
    "hub-sprint-edit-title": "title",
    "hub-sprint-edit-due": "dueAt",
}


def _sprint_edit_button(button_id: str) -> TextButton:
    return TextButton("✎", id=button_id, classes="hub-row-action hub-sprint-field-edit")


def sprint_goal_widgets(
    goal: str | None,
    *,
    with_edit: bool,
    title_class: str,
    content_class: str,
    muted_class: str,
) -> list[Widget]:
    header: list[Widget] = [Static("Goal", classes=title_class)]
    if with_edit:
        header.append(
            TextButton(
                "✎",
                id="hub-sprint-edit-goal",
                classes="hub-row-action hub-sprint-goal-edit",
            )
        )
    text = (goal or "").strip()
    body: Widget = (
        Markdown(text, classes=content_class)
        if text
        else Static("No goal yet.", classes=muted_class)
    )
    return [Horizontal(*header, classes="hub-title-row"), body]


def sprint_meta_widgets(
    sprint: SprintDetail,
    theme_variables: dict[str, str],
    *,
    title_class: str,
    spacer_class: str = "hub-detail-spacer",
    with_actions: bool = False,
) -> list[Widget]:
    status = (sprint.status or "").upper()
    can_act = with_actions and status in ("PLANNING", "ACTIVE")
    title_widget = Static(sprint.title or "-", markup=False, classes=title_class)
    title_block: Widget = (
        Horizontal(
            title_widget,
            _sprint_edit_button("hub-sprint-edit-title"),
            classes="hub-title-row",
        )
        if can_act
        else title_widget
    )
    return [
        title_block,
        Static("", classes=spacer_class),
        detail_row("Key", sprint.sprint_key or "-"),
        detail_row(
            "Status",
            _sprint_status_chip(theme_variables, sprint.status),
            action=TextButton("⇄", id="hub-sprint-transition", classes="hub-row-action")
            if can_act
            else None,
        ),
        detail_row(
            "Number",
            "-" if sprint.sprint_number is None else str(sprint.sprint_number),
        ),
        detail_row("Started", format_relative(sprint.started_at)),
        detail_row(
            "Due",
            format_relative(sprint.due_at),
            action=_sprint_edit_button("hub-sprint-edit-due")
            if can_act and status == "ACTIVE"
            else None,
        ),
        detail_row("Completed", format_relative(sprint.completed_at)),
        detail_row("Created", format_relative(sprint.created_at)),
        Rule(),
    ]


class SprintsMixin(ProjectHomeBase):
    """Sprints view and [1] list switching."""

    def action_toggle_list(self) -> None:
        if self._current_hub_box() == "3":
            self._toggle_agent_mode()
            return

        keep_focus = self._should_refocus_list_after_switch()
        if keep_focus:
            self._focus_list_host()
        self._switch_view(self._next_view_mode(), focus_list=keep_focus)

    def _should_refocus_list_after_switch(self) -> bool:
        focused = self.app.focused
        return focused is not None and focused.id in (
            "hub-issues-table",
            "hub-sprints-table",
            "hub-members-table",
            "hub-list-host",
            "hub-search",
        )

    def _focus_list_host(self) -> None:
        try:
            self.query_one("#hub-list-host").focus()
        except NoMatches:
            pass

    def _next_view_mode(self) -> str:
        current_index = _VIEW_CYCLE.index(self._view_mode)
        return _VIEW_CYCLE[(current_index + 1) % len(_VIEW_CYCLE)]

    def _set_view_chrome(self, mode: str) -> None:
        self._view_mode = mode
        self._persist_project_ui()
        self._cancel_detail_timer()
        self._cancel_search_timer()
        self._clear_list_search()
        self._refresh_box_chrome()
        self._update_create_button()
        self._update_filter_button()
        self._update_search_input()

    def _clear_list_search(self) -> None:
        try:
            self.query_one("#hub-search", Input).value = ""
        except NoMatches:
            pass

    def _switch_view(self, mode: str, *, focus_list: bool = False) -> None:
        if self._view_mode == mode:
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
        for index, sprint in enumerate(self._sprints):
            if sprint.id == sprint_id:
                self._select_sprint(index)
                return

    async def _ensure_sprints_loaded(self) -> None:
        if self._sprints:
            return
        page = await self._fetch_sprints()
        if page is not None:
            self._sprints = list(page.content or [])

    async def _load_sprints(self) -> None:
        page = await self._fetch_sprints(statuses=self._sprint_filter.statuses_arg())
        self._sprints = list(page.content or []) if page is not None else []
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
        if self._sprints:
            self._select_sprint(0)

    async def _render_sprints(self) -> None:
        box = self.query_one("#hub-list-host")
        await box.remove_children()
        if not self._sprints:
            await box.mount(Static("No sprints.", classes="hub-list-empty"))
            return
        rows: list[list[str | Text]] = [
            [
                str(index + 1),
                _fit(sprint.sprint_key or "-", 9),
                Text(_truncate(sprint.title or "-", 20)),
                _sprint_status_chip(self.app.theme_variables, sprint.status, pad=False),
                format_date(sprint.due_at),
            ]
            for index, sprint in enumerate(self._sprints)
        ]
        await box.mount(
            _DashTable(
                [("#", None), ("Key", 9), ("Title", None), ("Status", 11), ("Due", 11)],
                rows,
                id="hub-sprints-table",
                classes="hub-table",
            )
        )

    @on(DataTable.RowHighlighted, "#hub-sprints-table")
    def _on_sprint_highlighted(self, event: DataTable.RowHighlighted) -> None:
        if event.data_table.has_focus:
            self._select_sprint(event.cursor_row)

    @on(DataTable.RowSelected, "#hub-sprints-table")
    def _on_sprint_selected(self, event: DataTable.RowSelected) -> None:
        self._select_sprint(event.cursor_row, focus_detail=True)

    def _select_sprint(self, index: int, *, focus_detail: bool = False) -> None:
        if not (0 <= index < len(self._sprints)):
            return
        sprint_id = self._sprints[index].id
        if sprint_id is None:
            return
        if focus_detail and self._expanded:
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

    async def _render_sprint_detail(
        self, sprint_id: int, *, focus_detail: bool
    ) -> None:
        if not self._start_sprint_detail():
            return

        sprint = await self._load_sprint(sprint_id)
        if sprint is None:
            return
        issues = await self._load_sprint_issues(sprint_id)
        self._store_sprint_detail(sprint_id, sprint, issues)

        await self._mount_detail(self._sprint_widgets(sprint, issues))
        await self._clear_timeline()
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    def _start_sprint_detail(self) -> bool:
        if self.app.client is None:
            return False
        self._detail_issue_key = None
        self.add_class("-no-timeline")
        return True

    async def _load_sprint(self, sprint_id: int) -> SprintDetail | None:
        client = self.app.client
        if client is None:
            return None
        try:
            return await client.sprints.get_sprint(sprint_id)
        except TissueApiError as error:
            log.debug("Hub: failed to load sprint %s: %s", sprint_id, error)
            await self._mount_detail(
                [Static("Couldn't load sprint.", classes="hub-muted")]
            )
            await self._clear_timeline()
            return None

    async def _load_sprint_issues(self, sprint_id: int) -> list[IssueSummary]:
        client = self.app.client
        if client is None:
            return []
        try:
            page = await client.issues.search_project_issues(
                self._project_key, sprint_ids=[sprint_id]
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load issues for sprint %s: %s", sprint_id, error)
            return []
        return list(page.content or [])

    def _store_sprint_detail(
        self, sprint_id: int, sprint: SprintDetail, issues: list[IssueSummary]
    ) -> None:
        self._sprint_detail_id = sprint_id
        self._sprint_detail_issues = issues
        self._sprint_detail_status = (sprint.status or "").upper()
        self._sprint_edit_current = {
            "title": sprint.title or "",
            "goal": sprint.goal or "",
            "dueAt": sprint.due_at.isoformat() if sprint.due_at else "",
        }

    def _sprint_widgets(
        self,
        sprint: SprintDetail,
        issues: list[IssueSummary],
    ) -> list[Widget]:
        status = (sprint.status or "").upper()
        goal_editable = self._is_project_manager() and status in ("PLANNING", "ACTIVE")
        widgets: list[Widget] = sprint_meta_widgets(
            sprint,
            self.app.theme_variables,
            title_class="hub-detail-title",
            with_actions=self._is_project_manager(),
        )
        widgets.extend(
            sprint_goal_widgets(
                sprint.goal,
                with_edit=goal_editable,
                title_class="hub-detail-title",
                content_class="hub-content",
                muted_class="hub-muted",
            )
        )
        widgets.append(Rule())
        widgets.extend(self._sprint_issue_widgets(issues))
        return widgets

    def _sprint_issue_widgets(self, issues: list[IssueSummary]) -> list[Widget]:
        if issues:
            return [
                Horizontal(
                    Static(f"Issues ({len(issues)})", classes="hub-open-title"),
                    self._remove_sprint_issue_button(),
                    classes="hub-open-header",
                ),
                _DashTable(
                    [
                        ("Key", 10),
                        ("Title", None),
                        ("Status", 11),
                        ("Priority", 8),
                        ("Due", 11),
                    ],
                    _issue_rows(
                        issues,
                        self._state_colors,
                        self.app.theme_variables,
                        with_due=True,
                    ),
                    id="hub-sprint-issues-table",
                    classes="hub-table hub-sprint-issues",
                ),
            ]
        return [
            Static("Issues (0)", classes="hub-section-title"),
            Static("No issues.", classes="hub-muted"),
        ]

    def _remove_sprint_issue_button(self) -> TextButton:
        button = TextButton("-", id="hub-sprint-remove-issue", classes="hub-row-action")
        button.tooltip = "Remove the focused issue from this sprint"
        return button

    @on(Button.Pressed, "#hub-sprint-remove-issue")
    def _on_sprint_remove_issue(self) -> None:
        issue_key = self._focused_sprint_issue_key()
        if issue_key is None:
            self.app.notify("Focus an issue to remove.", severity="warning")
            return
        self.app.push_screen(
            ConfirmModal(
                message=f"⚠ Remove {issue_key} from this sprint?",
                title="Remove issue",
                confirm_label="Remove",
            ),
            lambda confirmed: self._on_remove_issue_confirmed(issue_key, confirmed),
        )

    @on(DataTable.RowSelected, "#hub-sprint-issues-table")
    def _on_sprint_issue_enter(self, event: DataTable.RowSelected) -> None:
        event.stop()
        row = event.cursor_row
        if 0 <= row < len(self._sprint_detail_issues):
            issue_key = self._sprint_detail_issues[row].issue_key
            if issue_key:
                self._open_issue_modal(issue_key)

    def _focused_sprint_issue_key(self) -> str | None:
        try:
            table = self.query_one("#hub-sprint-issues-table", DataTable)
        except NoMatches:
            return None
        row = table.cursor_row
        if not (0 <= row < len(self._sprint_detail_issues)):
            return None
        return self._sprint_detail_issues[row].issue_key

    def _on_remove_issue_confirmed(
        self, issue_key: str, confirmed: bool | None
    ) -> None:
        if not confirmed or self._sprint_detail_id is None:
            return
        self.run_worker(
            self._remove_sprint_issue(self._sprint_detail_id, issue_key),
            exclusive=True,
            group="hub-detail",
        )

    async def _remove_sprint_issue(self, sprint_id: int, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.sprints.remove_sprint_issues(sprint_id, [issue_key])
        except TissueApiError as error:
            self.app.notify(
                f"Couldn't remove {issue_key}: {error.detail or 'please try again'}",
                severity="error",
            )
            return
        await self._render_sprint_detail(sprint_id, focus_detail=False)

    @on(Button.Pressed, ".hub-sprint-field-edit")
    def _on_sprint_field_edit(self, event: Button.Pressed) -> None:
        field = _SPRINT_FIELD_BY_ID.get(event.button.id or "")
        if self._sprint_detail_id is None or field is None:
            return
        self.app.push_screen(
            SprintFieldEditModal(
                sprint_id=self._sprint_detail_id,
                field=field,
                current_value=self._sprint_edit_current.get(field),
            ),
            self._on_sprint_edited,
        )

    @on(Button.Pressed, ".hub-sprint-goal-edit")
    def _on_sprint_goal_edit(self, event: Button.Pressed) -> None:
        event.stop()
        if self._sprint_detail_id is None:
            return
        self.app.push_screen(
            SprintGoalEditModal(
                sprint_id=self._sprint_detail_id,
                current_goal=self._sprint_edit_current.get("goal"),
            ),
            self._on_sprint_edited,
        )

    @on(Button.Pressed, "#hub-sprint-transition")
    def _on_sprint_transition(self) -> None:
        if self._sprint_detail_id is None or self._sprint_detail_status is None:
            return
        self.app.push_screen(
            SprintTransitionModal(
                sprint_id=self._sprint_detail_id, status=self._sprint_detail_status
            ),
            self._on_sprint_edited,
        )

    def _on_sprint_edited(self, updated: bool | None) -> None:
        if not updated or self._sprint_detail_id is None:
            return
        self._sprints_by_id = None
        self.run_worker(
            self._render_sprint_detail(self._sprint_detail_id, focus_detail=False),
            exclusive=True,
            group="hub-detail",
        )

    @on(Button.Pressed, "#hub-add-to-sprint")
    def _on_add_to_active_sprint(self) -> None:
        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        self.run_worker(
            self._add_issue_to_active_sprint(issue_key),
            exclusive=True,
            group="hub-add-sprint",
        )

    async def _ensure_sprint_index(self) -> None:
        if self._sprints_by_id is not None:
            return
        client = self.app.client
        if client is None:
            self._sprints_by_id = {}
            return
        try:
            page = await client.sprints.list_project_sprints(self._project_key)
        except TissueApiError as error:
            log.debug("Hub: failed to load sprint index: %s", error)
            self._sprints_by_id = {}
            return
        self._sprints_by_id = {
            sprint.id: sprint
            for sprint in (page.content or [])
            if sprint.id is not None
        }

    def _active_sprint(self) -> SprintSummary | None:
        for sprint in (self._sprints_by_id or {}).values():
            if (sprint.status or "").upper() == "ACTIVE":
                return sprint
        return None

    async def _add_issue_to_active_sprint(self, issue_key: str) -> None:
        await self._ensure_sprint_index()
        active = self._active_sprint()
        if active is None or active.id is None:
            self.app.notify("No active sprint to add to.", severity="warning")
            return
        client = self.app.client
        if client is None:
            return
        try:
            await client.sprints.add_sprint_issues(active.id, [issue_key])
        except TissueApiError as error:
            self.app.notify(
                f"Couldn't add {issue_key}: {error.detail or 'please try again'}",
                severity="error",
            )
            return
        self.app.notify(
            f"Added {issue_key} to {active.sprint_key or 'the active sprint'}."
        )
        self._reflect_issue_sprint_change(issue_key, active.id)

    def _reflect_issue_sprint_change(self, issue_key: str, sprint_id: int) -> None:
        for summary in (*self._issues, *self._agent_issues):
            if summary.issue_key == issue_key:
                summary.sprint_id = sprint_id
        if self._detail_issue_key == issue_key:
            self._refresh_detail(issue_key)

    async def _clear_timeline(self) -> None:
        try:
            box = self.query_one("#hub-detail-timeline-inner")
        except NoMatches:
            return
        await box.remove_children()
