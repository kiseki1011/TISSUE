from __future__ import annotations

import logging
from collections.abc import Container
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
from tissue.screens.project_home.modals.create_sprint_modal import CreateSprintModal
from tissue.screens.project_home.modals.open_issue_filter_modal import (
    OpenIssueFilterModal,
)
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
    from tissue.screens.project_home.issue_filter import IssueFilter

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
            await box.mount(Static("No sprints.", classes="hub-muted"))
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
        # The pool the up button can pull from. An issue belongs to only one
        # sprint, so adding one already in another would quietly steal it. Only
        # offer issues with no sprint (sprint_id is None) and not already here.
        in_sprint = {issue.issue_key for issue in issues if issue.issue_key}
        self._sprint_open_page = 0
        try:
            open_page = await client.issues.search_project_issues(
                self._project_key,
                state_categories=self._sprint_open_filter.state_categories_arg(),
                priorities=self._sprint_open_filter.priorities_arg(),
                assignee_member_ids=self._sprint_open_filter.assignee_arg(),
            )
            open_issues = self._filter_open_pool(open_page.content or [], in_sprint)
            self._sprint_open_has_next = bool(open_page.has_next)
        except TissueApiError as error:
            log.debug(
                "Hub: failed to load open issues for sprint %s: %s", sprint_id, error
            )
            open_issues = []
            self._sprint_open_has_next = False
        self._sprint_detail_id = sprint_id
        self._sprint_detail_issues = issues
        self._sprint_open_issues = open_issues
        self._sprint_detail_status = (sprint.status or "").upper()
        # Current values an edit (✎) modal fills in.
        self._sprint_edit_current = {
            "title": sprint.title or "",
            "goal": sprint.goal or "",
            "dueAt": sprint.due_at.isoformat() if sprint.due_at else "",
        }
        await self._mount_detail(self._sprint_widgets(sprint, issues, open_issues))
        # Sprints have no activity timeline, so clear whatever the last issue left.
        await self._clear_timeline()
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    def _sprint_widgets(
        self,
        sprint: SprintDetail,
        issues: list[IssueSummary],
        open_issues: list[IssueSummary],
    ) -> list[Widget]:
        """Build the sprint read view with a ↑/↓ transfer row between the two lists.

        Built in order:

        - Meta rows
        - The sprint's issues
        - The transfer row
        - Open issues

        ↑ adds the selected open issue to the sprint, ↓ removes the selected
        sprint issue.
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
        widgets.append(Static(f"Issues ({len(issues)})", classes="hub-section-title"))
        if issues:
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
            widgets.append(Static("No issues.", classes="hub-muted"))
        add_button = Button("↑ +", id="hub-sprint-add", classes="hub-transfer-btn")
        add_button.tooltip = "Add the selected open issue to this sprint"
        remove_button = Button(
            "↓ -", id="hub-sprint-remove", classes="hub-transfer-btn"
        )
        remove_button.tooltip = "Remove the selected sprint issue"
        widgets.append(
            Horizontal(add_button, remove_button, classes="hub-transfer-row")
        )
        widgets.append(
            Horizontal(
                Static(f"Open issues ({len(open_issues)})", classes="hub-open-title"),
                TextButton("⚙", id="hub-sprint-open-filter", classes="hub-row-action"),
                classes="hub-open-header",
            )
        )
        if open_issues:
            widgets.append(
                _DashTable(
                    columns,
                    _issue_rows(
                        open_issues,
                        self._state_colors,
                        self.app.theme_variables,
                        with_due=True,
                    ),
                    id="hub-sprint-open-table",
                    classes="hub-table hub-sprint-issues",
                )
            )
        else:
            widgets.append(Static("No open issues to add.", classes="hub-muted"))
        if open_issues and self._sprint_open_has_next:
            widgets.append(self._sprint_open_more_button())
        return widgets

    def _sprint_open_more_button(self) -> TextButton:
        return TextButton(
            "↓ Load more", id="hub-sprint-open-more", classes="hub-load-more"
        )

    def _filter_open_pool(
        self,
        content: list[IssueSummary],
        in_sprint: Container[str | None],
        *,
        exclude: Container[str | None] = frozenset(),
    ) -> list[IssueSummary]:
        """Pool issues: not already in this sprint, in no sprint, not shown yet."""
        return [
            issue
            for issue in content
            if issue.issue_key not in in_sprint
            and issue.sprint_id is None
            and issue.issue_key not in exclude
        ]

    @on(Button.Pressed, "#hub-sprint-add")
    def _on_sprint_add(self) -> None:
        self._transfer_sprint_issue(add=True)

    @on(Button.Pressed, "#hub-sprint-remove")
    def _on_sprint_remove(self) -> None:
        self._transfer_sprint_issue(add=False)

    def _transfer_sprint_issue(self, *, add: bool) -> None:
        """Move the cursor-selected issue into or out of the sprint, then redraw."""
        sprint_id = self._sprint_detail_id
        if sprint_id is None:
            return
        table_id = "#hub-sprint-open-table" if add else "#hub-sprint-issues-table"
        source = self._sprint_open_issues if add else self._sprint_detail_issues
        try:
            table = self.query_one(table_id, DataTable)
        except NoMatches:
            return
        row = table.cursor_row
        if not (0 <= row < len(source)):
            return
        issue_key = source[row].issue_key
        if not issue_key:
            return
        self.run_worker(
            self._do_transfer(sprint_id, issue_key, add=add),
            exclusive=True,
            group="hub-detail",
        )

    async def _do_transfer(self, sprint_id: int, issue_key: str, *, add: bool) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            if add:
                await client.sprints.add_sprint_issues(sprint_id, [issue_key])
            else:
                await client.sprints.remove_sprint_issues(sprint_id, [issue_key])
        except TissueApiError as error:
            verb = "add" if add else "remove"
            self.app.notify(
                f"Couldn't {verb} {issue_key}: {error.detail or 'please try again'}",
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
            self.run_worker(
                self._render_sprint_detail(self._sprint_detail_id, focus_detail=False),
                exclusive=True,
                group="hub-detail",
            )

    @on(Button.Pressed, "#hub-sprint-open-filter")
    def _on_sprint_open_filter(self) -> None:
        """The Open issues ⚙ opens a filter modal scoped to that pool."""
        self.run_worker(
            self._open_sprint_open_filter(), exclusive=True, group="hub-filter"
        )

    async def _open_sprint_open_filter(self) -> None:
        await self._ensure_members_loaded()
        self.app.push_screen(
            OpenIssueFilterModal(
                current=self._sprint_open_filter, members=self._members
            ),
            self._on_sprint_open_filter_applied,
        )

    def _on_sprint_open_filter_applied(self, new_filter: IssueFilter | None) -> None:
        if new_filter is None or self._sprint_detail_id is None:
            return
        self._sprint_open_filter = new_filter
        self.run_worker(
            self._render_sprint_detail(self._sprint_detail_id, focus_detail=False),
            exclusive=True,
            group="hub-detail",
        )

    @on(Button.Pressed, "#hub-sprint-open-more")
    def _on_sprint_open_more(self) -> None:
        self.run_worker(
            self._load_more_sprint_open(), exclusive=True, group="hub-open-more"
        )

    async def _load_more_sprint_open(self) -> None:
        """Fetch the next page of the open-issue pool and append it (Load more)."""
        if self._sprint_open_loading_more or not self._sprint_open_has_next:
            return
        sprint_id = self._sprint_detail_id
        client = self.app.client
        if sprint_id is None or client is None:
            return
        self._sprint_open_loading_more = True
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                state_categories=self._sprint_open_filter.state_categories_arg(),
                priorities=self._sprint_open_filter.priorities_arg(),
                assignee_member_ids=self._sprint_open_filter.assignee_arg(),
                page=self._sprint_open_page + 1,
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load more open issues: %s", error)
            self._sprint_open_loading_more = False
            return
        self._sprint_open_page += 1
        self._sprint_open_has_next = bool(page.has_next)
        in_sprint = {i.issue_key for i in self._sprint_detail_issues if i.issue_key}
        shown = frozenset(i.issue_key for i in self._sprint_open_issues)
        new = self._filter_open_pool(list(page.content or []), in_sprint, exclude=shown)
        if new:
            self._sprint_open_issues.extend(new)
            self._append_sprint_open_rows(new)
        self._update_sprint_open_more()
        self._sprint_open_loading_more = False

    def _append_sprint_open_rows(self, issues: list[IssueSummary]) -> None:
        try:
            table = self.query_one("#hub-sprint-open-table", DataTable)
        except NoMatches:
            return
        rows = _issue_rows(
            issues, self._state_colors, self.app.theme_variables, with_due=True
        )
        for row in rows:
            table.add_row(*row)
        try:
            self.query_one(".hub-open-title", Static).update(
                f"Open issues ({len(self._sprint_open_issues)})"
            )
        except NoMatches:
            pass

    def _update_sprint_open_more(self) -> None:
        """Drop the Load-more button once the pool is fully loaded."""
        if self._sprint_open_has_next:
            return
        try:
            self.query_one("#hub-sprint-open-more").remove()
        except NoMatches:
            return

    async def _clear_timeline(self) -> None:
        try:
            box = self.query_one("#hub-detail-timeline-inner")
        except NoMatches:
            return
        await box.remove_children()
