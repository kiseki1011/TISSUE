from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Grid, Horizontal, Vertical, VerticalScroll
from textual.widget import Widget
from textual.widgets import Button, Checkbox, DataTable, Static

from tissue.api.errors import TissueApiError
from tissue.screens.base import PostAuthScreen
from tissue.screens.project_home.issue_table import ISSUE_TABLE_ID, issue_table
from tissue.screens.project_home.workflow_colors import load_state_colors
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row
from tissue.widgets.filter_checkbox import FilterCheckbox
from tissue.widgets.issue_chips import color_chip, priority_chip, type_chip

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.workflow_detail import WorkflowDetail

log = logging.getLogger(__name__)


class TrashScreen(PostAuthScreen):
    """A project's trash: its soft-deleted issues, read-only, with restore.

    Deleted issues are hidden from the detail endpoint, so [2] shows what the list
    summary carries rather than a full fetch.
    """

    CSS_PATH = "trash.tcss"

    BINDINGS = [Binding("r", "restore", "restore")]

    def __init__(self, project_key: str) -> None:
        super().__init__()
        self._project_key = project_key
        self._issues: list[IssueSummary] = []
        self._selected_key: str | None = None
        self._mine_only = False
        self._members: dict[int, str] = {}
        self._state_colors: dict[int, str] = {}
        self._workflow_cache: dict[int, WorkflowDetail] = {}

    def compose_content(self) -> ComposeResult:
        with Container(id="trash-body"):
            with Horizontal(id="trash-toolbar"):
                back = Button("←", id="trash-back", classes="search-filter-btn")
                back.tooltip = "Back to project home"
                yield back
                yield FilterCheckbox(
                    "Mine only", value=self._mine_only, id="trash-mine"
                )
            with Grid(id="trash-grid"):
                yield Vertical(id="trash-list-box", classes="trash-box")
                with Vertical(id="trash-detail-box", classes="trash-box"):
                    yield VerticalScroll(id="trash-detail-scroll")
                    with Horizontal(id="trash-detail-actions"):
                        yield Button(
                            "Restore",
                            id="trash-restore",
                            classes="trash-restore-btn",
                            disabled=True,
                        )

    def on_mount(self) -> None:
        self.set_top_bar_status(f"{self._project_key} · Trash")
        self.query_one("#trash-list-box").border_title = "[1] Deleted issues"
        self.query_one("#trash-detail-box").border_title = "[2] Details"
        self.run_worker(self._load(), exclusive=True, group="trash-load")

    def check_action(self, action: str, parameters: tuple[object, ...]) -> bool | None:
        if action == "restore":
            return self._selected_key is not None
        return super().check_action(action, parameters)

    async def _load(self) -> None:
        client = self.app.client
        if client is None:
            return
        self._state_colors = await load_state_colors(client, self._workflow_cache, log)
        if not self._members:
            await self._load_members()
        await self._fetch_trash()

    async def _load_members(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            members = await client.project_members.list_project_members(
                self._project_key
            )
        except TissueApiError as error:
            log.debug("Trash: failed to load members: %s", error)
            return
        self._members = {
            member.member_id: (member.display_name or member.username or "-")
            for member in members
            if member.member_id is not None
        }

    async def _fetch_trash(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            page = await client.issues.list_project_trash(
                self._project_key, mine_only=self._mine_only
            )
        except TissueApiError as error:
            log.debug("Trash: failed to load: %s", error)
            self._issues = []
        else:
            self._issues = list(page.content or [])
        await self._render_list()

    async def _render_list(self) -> None:
        box = self.query_one("#trash-list-box", Vertical)
        await box.remove_children()
        if not self._issues:
            self._selected_key = None
            self.refresh_bindings()
            await box.mount(Static("Trash is empty.", classes="trash-empty"))
            self._show_detail(None)
            return
        table = issue_table(
            self._issues, self._state_colors, self.app.theme_variables, self._members
        )
        await box.mount(table)
        table.focus()
        self._select_index(0)

    def _select_index(self, index: int) -> None:
        if not (0 <= index < len(self._issues)):
            return
        summary = self._issues[index]
        self._selected_key = summary.issue_key
        self.refresh_bindings()
        self._show_detail(summary)

    def _show_detail(self, summary: IssueSummary | None) -> None:
        self.run_worker(
            self._render_detail(summary), exclusive=True, group="trash-detail"
        )

    @on(DataTable.RowHighlighted, f"#{ISSUE_TABLE_ID}")
    def _on_highlighted(self, event: DataTable.RowHighlighted) -> None:
        self._select_index(event.cursor_row)

    async def _render_detail(self, summary: IssueSummary | None) -> None:
        scroll = self.query_one("#trash-detail-scroll", VerticalScroll)
        await scroll.remove_children()
        restore = self.query_one("#trash-restore", Button)
        if summary is None:
            restore.disabled = True
            await scroll.mount(
                Static("Select an issue to see details.", classes="trash-muted")
            )
            return
        restore.disabled = False
        await scroll.mount_all(self._detail_widgets(summary))

    def _detail_widgets(self, summary: IssueSummary) -> list[Widget]:
        assignee = summary.assignee_member_id
        state_color = (
            self._state_colors.get(summary.current_state_id)
            if summary.current_state_id is not None
            else None
        )
        return [
            Static(Text(summary.title or "-", style="bold"), classes="trash-title"),
            detail_row("Key", summary.issue_key or "-"),
            detail_row(
                "Status",
                color_chip(summary.current_state_label or "-", state_color),
            ),
            detail_row(
                "Priority", priority_chip(self.app.theme_variables, summary.priority)
            ),
            detail_row(
                "Type", type_chip(summary.issue_type_name, summary.issue_type_color)
            ),
            detail_row(
                "Assignee",
                self._members.get(assignee, "-") if assignee is not None else "-",
            ),
            detail_row(
                "Story points",
                "-" if summary.story_point is None else str(summary.story_point),
            ),
            detail_row("Due", format_relative(summary.due_at)),
        ]

    @on(Button.Pressed, "#trash-back")
    def _on_back(self) -> None:
        self.app.pop_screen()

    @on(Checkbox.Changed, "#trash-mine")
    def _on_toggle_mine(self, event: Checkbox.Changed) -> None:
        if event.value == self._mine_only:
            return
        self._mine_only = event.value
        self.run_worker(self._fetch_trash(), exclusive=True, group="trash-load")

    def action_restore(self) -> None:
        self._do_restore()

    @on(Button.Pressed, "#trash-restore")
    def _on_restore(self) -> None:
        self._do_restore()

    def _do_restore(self) -> None:
        if self._selected_key is not None:
            self.run_worker(
                self._restore(self._selected_key),
                exclusive=True,
                group="trash-restore",
            )

    async def _restore(self, issue_key: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            await client.issues.restore_issue(issue_key)
        except TissueApiError as error:
            self.app.notify(f"Couldn't restore {issue_key}: {error}", severity="error")
            return
        self.app.notify(f"Restored {issue_key}.")
        self.run_worker(self._fetch_trash(), exclusive=True, group="trash-load")
