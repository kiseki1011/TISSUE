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
    """The Goal shown as a Markdown block (like the issue body), with a ✎ when editable.

    Shared by the hub [2] detail and SprintDetailModal.
    """
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
    """Build the sprint info rows shared by the hub [2] detail and SprintDetailModal.

    Shared so the two can't get out of sync. Callers pass their own title/spacer
    class. `with_actions` adds the edit (✎) and transition (⇄) buttons, only on
    a non-terminal sprint (Planning/Active); the read-only modal leaves it off.
    """
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
    """The [1] box's Sprints view plus the [2] sprint read view.

    Also owns the CTRL+T cycle that swaps the [1] list between
    Issues, Sprints, and Members.
    """

    def action_toggle_list(self) -> None:
        # When [3] holds focus, CTRL+T switches that box's mode instead of cycling
        # [1]'s views.
        if self._current_hub_box() == "3":
            self._toggle_agent_mode()
            return
        # The focused table is about to be removed, which would jump focus to the
        # search bar and flicker. Move focus to the host that stays put first, so
        # the load can re-focus the new table.
        focused = self.app.focused
        keep_focus = focused is not None and focused.id in (
            "hub-issues-table",
            "hub-sprints-table",
            "hub-members-table",
            "hub-list-host",
            # CTRL+T can fire while the search input has focus. The Sprints view
            # turns it off, and Textual moves focus from a disabled widget onto
            # the filter button (a dead end), so move focus to the host first.
            "hub-search",
        )
        if keep_focus:
            try:
                self.query_one("#hub-list-host").focus()
            except NoMatches:
                pass
        current_index = _VIEW_CYCLE.index(self._view_mode)
        self._switch_view(
            _VIEW_CYCLE[(current_index + 1) % len(_VIEW_CYCLE)], focus_list=keep_focus
        )

    def _set_view_chrome(self, mode: str) -> None:
        """Save `_view_mode` and refresh the box title and border without loading.

        Any path that swaps #hub-list-host calls this first so the title and
        border can never disagree with what's shown.
        """
        self._view_mode = mode
        # Drop a waiting detail/search from the view we're leaving. An old timer
        # would otherwise draw the old view's row into [2] before the new one fills.
        self._cancel_detail_timer()
        self._cancel_search_timer()
        # Clear the keyword so it doesn't carry over between views (e.g. an issue
        # keyword filtering the members list down to nothing).
        try:
            self.query_one("#hub-search", Input).value = ""
        except NoMatches:
            pass
        self._refresh_box_chrome()
        self._update_create_button()
        self._update_filter_button()
        self._update_search_input()

    def _switch_view(self, mode: str, *, focus_list: bool = False) -> None:
        """Switch the [1] list to `mode`, cancelling any load still running.

        All list loads share the single exclusive `hub-list` worker group, so
        only one table is ever mounted into #hub-list-host. `focus_list`
        re-focuses the new table once it mounts.
        """
        if self._view_mode == mode:
            return
        self._set_view_chrome(mode)
        self._run_view_load(mode, focus_list=focus_list)

    def _run_view_load(self, mode: str, *, focus_list: bool = False) -> None:
        """Load `mode` in the shared exclusive `hub-list` group.

        The current search keyword is applied to the loaded view and stays put
        across switches. Sprints ignores it.
        """
        try:
            keyword = self.query_one("#hub-search", Input).value.strip() or None
        except NoMatches:
            keyword = None
        self.run_worker(
            self._load_view(mode, focus_list, keyword),
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
        """Open the create-sprint form, showing and selecting the sprint on success."""
        self.app.push_screen(
            CreateSprintModal(project_key=self._project_key), self._on_sprint_created
        )

    def _open_sprint_modal(self, sprint_id: int) -> None:
        """Pop a read-only sprint detail modal (expanded mode, where [2] is hidden)."""
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
        """Fill `self._sprints` without drawing, for the filter modal's picker.

        May open from the Issues view before Sprints has ever loaded. Does
        nothing once the list is present, since the Sprints view re-fetches on its
        own switch.
        """
        client = self.app.client
        if client is None or self._sprints:
            return
        try:
            page = await client.sprints.list_project_sprints(self._project_key)
            self._sprints = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load sprints for filter: %s", error)

    async def _load_sprints(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.sprints.list_project_sprints(
                self._project_key, statuses=self._sprint_filter.statuses_arg()
            )
            self._sprints = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load sprints: %s", error)
            self._sprints = []
        await self._render_sprints()
        # Fill [2] with the first sprint so it doesn't show the old one on switch.
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
        # Expanded mode hides [2], so an explicit Enter pops the detail as a modal.
        if focus_detail and self._expanded:
            self._open_sprint_modal(sprint_id)
            return
        # Shares the issue-detail worker group so the two never draw into [2] at
        # once. Waits for a pause so scrolling the list doesn't fetch every sprint
        # passed over.
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
        client = self.app.client
        if client is None:
            return
        # Clear the issue key so late comment/activity workers for an earlier
        # issue stop (they check _detail_issue_key) instead of overwriting [2].
        self._detail_issue_key = None
        # Sprints have no useful activity feed, so drop the timeline column and its
        # separating line, same as the member view.
        self.add_class("-no-timeline")
        try:
            sprint = await client.sprints.get_sprint(sprint_id)
        except TissueApiError as error:
            log.debug("Hub: failed to load sprint %s: %s", sprint_id, error)
            await self._mount_detail(
                [Static("Couldn't load sprint.", classes="hub-muted")]
            )
            await self._clear_timeline()
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key, sprint_ids=[sprint_id]
            )
            issues = list(page.content or [])
        except TissueApiError as error:
            log.debug("Hub: failed to load issues for sprint %s: %s", sprint_id, error)
            issues = []
        self._sprint_detail_id = sprint_id
        self._sprint_detail_issues = issues
        self._sprint_detail_status = (sprint.status or "").upper()
        # Current values an edit (✎) modal fills in.
        self._sprint_edit_current = {
            "title": sprint.title or "",
            "goal": sprint.goal or "",
            "dueAt": sprint.due_at.isoformat() if sprint.due_at else "",
        }
        await self._mount_detail(self._sprint_widgets(sprint, issues))
        # Sprints have no activity timeline, so clear whatever the last issue left.
        await self._clear_timeline()
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    def _sprint_widgets(
        self,
        sprint: SprintDetail,
        issues: list[IssueSummary],
    ) -> list[Widget]:
        """Build the sprint read view: meta rows, the goal block, then its issues.

        Enter on an issue opens its read-only detail. The `-` button removes the
        focused issue from the sprint (after a confirm).
        """
        columns = [
            ("Key", 10),
            ("Title", None),
            ("Status", 11),
            ("Priority", 8),
            ("Due", 11),
        ]
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
        if issues:
            remove_button = TextButton(
                "-", id="hub-sprint-remove-issue", classes="hub-row-action"
            )
            remove_button.tooltip = "Remove the focused issue from this sprint"
            widgets.append(
                Horizontal(
                    Static(f"Issues ({len(issues)})", classes="hub-open-title"),
                    remove_button,
                    classes="hub-open-header",
                )
            )
            widgets.append(
                _DashTable(
                    columns,
                    _issue_rows(
                        issues,
                        self._state_colors,
                        self.app.theme_variables,
                        with_due=True,
                    ),
                    id="hub-sprint-issues-table",
                    classes="hub-table hub-sprint-issues",
                )
            )
        else:
            widgets.append(Static("Issues (0)", classes="hub-section-title"))
            widgets.append(Static("No issues.", classes="hub-muted"))
        return widgets

    @on(Button.Pressed, "#hub-sprint-remove-issue")
    def _on_sprint_remove_issue(self) -> None:
        """The `-` button asks to remove the focused sprint issue, then does it."""
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
        """Enter on a sprint issue opens its read-only detail modal."""
        event.stop()
        row = event.cursor_row
        if 0 <= row < len(self._sprint_detail_issues):
            issue_key = self._sprint_detail_issues[row].issue_key
            if issue_key:
                self._open_issue_modal(issue_key)

    def _focused_sprint_issue_key(self) -> str | None:
        """The issue key under the cursor in the sprint's Issues table, if any."""
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
        """A ✎ on a sprint field opens its one-field edit modal."""
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
        """The Goal ✎ opens the Markdown goal editor."""
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
        """The Status ⇄ opens the start/complete/cancel picker (it does the work)."""
        if self._sprint_detail_id is None or self._sprint_detail_status is None:
            return
        self.app.push_screen(
            SprintTransitionModal(
                sprint_id=self._sprint_detail_id, status=self._sprint_detail_status
            ),
            self._on_sprint_edited,
        )

    def _on_sprint_edited(self, updated: bool | None) -> None:
        """Redraw the sprint detail after a successful edit or transition."""
        if updated and self._sprint_detail_id is not None:
            # A transition can change which sprint is active, so drop the index.
            self._sprints_by_id = None
            self.run_worker(
                self._render_sprint_detail(self._sprint_detail_id, focus_detail=False),
                exclusive=True,
                group="hub-detail",
            )

    @on(Button.Pressed, "#hub-add-to-sprint")
    def _on_add_to_active_sprint(self) -> None:
        """The Current Sprint `+` adds the shown issue to the active sprint."""
        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        self.run_worker(
            self._add_issue_to_active_sprint(issue_key),
            exclusive=True,
            group="hub-add-sprint",
        )

    async def _ensure_sprint_index(self) -> None:
        """Lazily load all sprints keyed by id, used by the issue detail.

        Resolves an issue's current sprint name and finds the active sprint.
        Cleared on a sprint transition so the active sprint stays current.
        """
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
        """The project's single ACTIVE sprint from the loaded index (or None)."""
        for sprint in (self._sprints_by_id or {}).values():
            if (sprint.status or "").upper() == "ACTIVE":
                return sprint
        return None

    async def _add_issue_to_active_sprint(self, issue_key: str) -> None:
        """Add `issue_key` to the project's active sprint, or notify why not.

        Shared by the [1] Issues ctrl+s shortcut and the issue detail `+` button.
        """
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
        """Update the cached list row's sprint and redraw the detail if it's shown.

        Keeps the [2] "Current Sprint" row in step with the add without a reload.
        """
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
