from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.containers import Horizontal
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Button, DataTable, Input, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.screens.home.rendering import _fit, _truncate
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.screens.project_home.constants import (
    _OPEN_STATE_CATEGORIES,
    _VIEW_CYCLE,
)
from tissue.screens.project_home.modals.create_sprint_modal import CreateSprintModal
from tissue.screens.project_home.rendering import _issue_rows, _sprint_status_chip
from tissue.util.datetime_fmt import format_date, format_relative
from tissue.widgets.detail_row import detail_row

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.sprint_detail import SprintDetail

log = logging.getLogger(__name__)


def sprint_meta_widgets(
    s: SprintDetail, theme_variables: dict[str, str], *, title_class: str
) -> list[Widget]:
    """Sprint meta read rows (title + key/status/number/goal/dates) ending with a
    Rule. Shared by the hub's [2] sprint detail and the expanded-mode
    SprintDetailModal so the two can't drift; callers pass their own title class."""
    return [
        Static(s.title or "-", markup=False, classes=title_class),
        detail_row("Key", s.sprint_key or "-"),
        detail_row("Status", _sprint_status_chip(theme_variables, s.status)),
        detail_row("Number", "-" if s.sprint_number is None else str(s.sprint_number)),
        detail_row("Goal", (s.goal or "").strip() or "-"),
        detail_row("Started", format_relative(s.started_at)),
        detail_row("Due", format_relative(s.due_at)),
        detail_row("Completed", format_relative(s.completed_at)),
        detail_row("Created", format_relative(s.created_at)),
        Rule(),
    ]


class SprintsMixin(ProjectHomeBase):
    """The [1] box's Sprints view: the sprint list (one of the CTRL+T list views)
    plus the sprint read view (meta + its issues) in [2]. Also owns the CTRL+T
    cycle that swaps the [1] list between Issues / Sprints / Members."""

    def action_toggle_list(self) -> None:
        # CTRL+T is focus-aware: when [3] holds focus it toggles that box's mode
        # (Agent Work ↔ Requested reviews); otherwise it cycles [1]'s views.
        if self._current_hub_box() == "3":
            self._toggle_agent_mode()
            return
        # Keep focus on [1] across the swap when it holds focus now. The focused
        # table is about to be removed (→ focus would jump to the search bar and
        # flicker), so park focus on the persistent, always-focusable host first;
        # the load then focuses the new table (or leaves it on the host when the
        # next view is empty and has no table).
        focused = self.app.focused
        keep_focus = focused is not None and focused.id in (
            "hub-issues-table",
            "hub-sprints-table",
            "hub-members-table",
            "hub-list-host",
            # CTRL+T fires while the search input has focus too. The Sprints view
            # disables the input, and Textual blurs a disabled focused widget onto
            # the next focusable one (the ⚙ filter button — a dead end). Park focus
            # on the host first (as for the tables) so the disable never blurs it.
            "hub-search",
        )
        if keep_focus:
            try:
                self.query_one("#hub-list-host").focus()
            except NoMatches:
                pass
        i = _VIEW_CYCLE.index(self._view_mode)
        self._switch_view(
            _VIEW_CYCLE[(i + 1) % len(_VIEW_CYCLE)], focus_list=keep_focus
        )

    def _set_view_chrome(self, mode: str) -> None:
        """Record `_view_mode` and refresh the box chrome + create button WITHOUT
        loading. Any path that swaps #hub-list-host (toggle, search) calls this
        first so the chrome can never disagree with what's shown."""
        self._view_mode = mode
        # Drop any pending detail render from the view we're leaving — its seed
        # _select_*(0) will queue the new view's detail, and a stale timer would
        # otherwise render the old view's row into [2] first. Likewise drop a pending
        # search.
        self._cancel_detail_timer()
        self._cancel_search_timer()
        # Each view searches independently — clear the keyword so it doesn't leak
        # across (e.g. an issue keyword filtering the members list to nothing). The
        # subsequent _run_view_load reads the now-empty box and loads unfiltered.
        try:
            self.query_one("#hub-search", Input).value = ""
        except NoMatches:
            pass
        # Box titles/subtitles (view name + CTRL+T + Close/Open hints) are owned by
        # LayoutMixin so the collapse state stays in sync with the view.
        self._refresh_box_chrome()
        # The create button is context-aware (+ issue / S sprint / disabled), and the
        # search box's placeholder/enabled-state tracks the view too.
        self._update_create_button()
        self._update_search_input()

    def _switch_view(self, mode: str, *, focus_list: bool = False) -> None:
        """Flip the [1] box to another list view, then (re)load it. All list
        loads share the single exclusive `hub-list` worker group, so a switch
        cancels any in-flight load and only one table is ever mounted into
        #hub-list-host. `focus_list` re-focuses the new table once it mounts."""
        if self._view_mode == mode:
            return
        self._set_view_chrome(mode)
        self._run_view_load(mode, focus_list=focus_list)

    def _run_view_load(self, mode: str, *, focus_list: bool = False) -> None:
        """Spawn the loader for `mode` in the shared exclusive `hub-list` group.
        When `focus_list`, focus the [1] table once it's mounted so a keyboard
        toggle keeps focus on [1] instead of dropping it to the search bar. The
        current search keyword is applied to the loaded view (persists across
        switches); the Sprints view ignores it."""
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
        """Open the create-sprint form; on success, show Sprints and select it."""
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
        for idx, s in enumerate(self._sprints):
            if s.id == sprint_id:
                self._select_sprint(idx)
                return

    async def _ensure_sprints_loaded(self) -> None:
        """Populate `self._sprints` WITHOUT rendering the list — for the filter
        modal's Sprint picker, which may open from the Issues view before the
        Sprints view has ever loaded. No-op once the roster is present (the Sprints
        view re-fetches on its own switch, so this can't leave it stale)."""
        client = self.app.client
        if client is None or self._sprints:
            return
        try:
            page = await client.sprints.list_project_sprints(self._project_key)
            self._sprints = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load sprints for filter: %s", e)

    async def _load_sprints(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.sprints.list_project_sprints(self._project_key)
            self._sprints = list(page.content or [])
        except TissueApiError as e:
            log.debug("Hub: failed to load sprints: %s", e)
            self._sprints = []
        await self._render_sprints()
        # Seed the detail with the first sprint so [2] isn't stale on switch.
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
                _fit(s.sprint_key or "-", 9),
                Text(_truncate(s.title or "-", 20)),
                _sprint_status_chip(self.app.theme_variables, s.status, pad=False),
                format_date(s.due_at),
            ]
            for s in self._sprints
        ]
        await box.mount(
            _DashTable(
                [("Key", 9), ("Title", None), ("Status", 11), ("Due", 11)],
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

    def _select_sprint(self, idx: int, *, focus_detail: bool = False) -> None:
        if not (0 <= idx < len(self._sprints)):
            return
        sprint_id = self._sprints[idx].id
        if sprint_id is None:
            return
        # Expanded mode hides [2]; an explicit Enter pops the detail as a modal
        # (mirrors the issues list).
        if focus_detail and self._expanded:
            self._open_sprint_modal(sprint_id)
            return
        # Shares the issue-detail worker group so the two never render into [2]
        # concurrently; debounced like the issue list so scrolling the sprint list
        # doesn't fetch every sprint it passes over.
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
        # Sprint detail replaces the issue detail; clear the issue key so any
        # late-arriving comment/activity workers for a prior issue bail (they
        # guard on _detail_issue_key) instead of clobbering the sprint view.
        self._detail_issue_key = None
        # Sprints have no useful activity feed (only started/completed), so the
        # sprint view drops the timeline column and its separating line — same as
        # the member view.
        self.add_class("-no-timeline")
        try:
            sprint = await client.sprints.get_sprint(sprint_id)
        except TissueApiError as e:
            log.debug("Hub: failed to load sprint %s: %s", sprint_id, e)
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
        except TissueApiError as e:
            log.debug("Hub: failed to load issues for sprint %s: %s", sprint_id, e)
            issues = []
        # Open (non-terminal) issues the ↑ button can pull in: not already in this
        # sprint, AND not in *another* sprint either. An issue has a single sprint
        # FK, so adding one that already belongs elsewhere would silently steal it —
        # so the pool only offers unassigned (sprint_id is None) issues.
        in_sprint = {i.issue_key for i in issues if i.issue_key}
        try:
            open_page = await client.issues.search_project_issues(
                self._project_key, state_categories=_OPEN_STATE_CATEGORIES
            )
            open_issues = [
                i
                for i in (open_page.content or [])
                if i.issue_key not in in_sprint and i.sprint_id is None
            ]
        except TissueApiError as e:
            log.debug("Hub: failed to load open issues for sprint %s: %s", sprint_id, e)
            open_issues = []
        self._sprint_detail_id = sprint_id
        self._sprint_detail_issues = issues
        self._sprint_open_issues = open_issues
        await self._mount_detail(self._sprint_widgets(sprint, issues, open_issues))
        # Sprints have no activity timeline; clear whatever the last issue left.
        await self._clear_timeline()
        if focus_detail:
            self.query_one("#hub-detail-main").focus()

    def _sprint_widgets(
        self,
        s: SprintDetail,
        issues: list[IssueSummary],
        open_issues: list[IssueSummary],
    ) -> list[Widget]:
        """Sprint read view: meta rows, the sprint's issues, a ↑/↓ transfer row,
        then the open issues that can be pulled in. ↑ adds the selected open issue
        to the sprint; ↓ removes the selected sprint issue from it."""
        cols = [
            ("Key", 10),
            ("Title", None),
            ("Status", 11),
            ("Priority", 8),
            ("Due", 11),
        ]
        widgets: list[Widget] = sprint_meta_widgets(
            s, self.app.theme_variables, title_class="hub-detail-title"
        )
        widgets.append(Static(f"Issues ({len(issues)})", classes="hub-section-title"))
        if issues:
            widgets.append(
                _DashTable(
                    cols,
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
        # ↑ pulls the selected open issue into the sprint; ↓ drops the selected
        # sprint issue back out.
        add_btn = Button("↑ +", id="hub-sprint-add", classes="hub-transfer-btn")
        add_btn.tooltip = "Add the selected open issue to this sprint"
        remove_btn = Button("↓ -", id="hub-sprint-remove", classes="hub-transfer-btn")
        remove_btn.tooltip = "Remove the selected sprint issue"
        widgets.append(Horizontal(add_btn, remove_btn, classes="hub-transfer-row"))
        widgets.append(
            Static(f"Open issues ({len(open_issues)})", classes="hub-section-title")
        )
        if open_issues:
            widgets.append(
                _DashTable(
                    cols,
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
        return widgets

    @on(Button.Pressed, "#hub-sprint-add")
    def _on_sprint_add(self) -> None:
        self._transfer_sprint_issue(add=True)

    @on(Button.Pressed, "#hub-sprint-remove")
    def _on_sprint_remove(self) -> None:
        self._transfer_sprint_issue(add=False)

    def _transfer_sprint_issue(self, *, add: bool) -> None:
        """Move the cursor-selected issue into (add) or out of (remove) the open
        sprint, then re-render its detail."""
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
        except TissueApiError as e:
            verb = "add" if add else "remove"
            self.app.notify(
                f"Couldn't {verb} {issue_key}: {e.detail or 'please try again'}",
                severity="error",
            )
            return
        # Re-render so both lists reflect the move (shares the detail group).
        await self._render_sprint_detail(sprint_id, focus_detail=False)

    async def _clear_timeline(self) -> None:
        try:
            box = self.query_one("#hub-detail-timeline-inner")
        except NoMatches:
            return
        await box.remove_children()
