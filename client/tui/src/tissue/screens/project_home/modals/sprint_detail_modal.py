"""A centered, read-only sprint detail modal. Used by the hub's expanded mode
(CTRL+F): with [2] hidden, pressing Enter on a sprint row pops its detail here —
mirrors IssueDetailModal. Shows the sprint meta + its issues (read-only: no
add/remove transfer controls, no open-issue pool)."""

from __future__ import annotations

from typing import TYPE_CHECKING

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.screens.home.widgets import _DashTable
from tissue.screens.project_home.areas.sprints import sprint_meta_widgets
from tissue.screens.project_home.rendering import _issue_rows

if TYPE_CHECKING:
    from textual.widget import Widget


class SprintDetailModal(TissueModal[None]):
    """Read-only sprint detail in a centered dialog. Dismisses on Esc."""

    CSS_PATH = "sprint_detail_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self, *, sprint_id: int, project_key: str, state_colors: dict[int, str]
    ) -> None:
        super().__init__()
        self._sprint_id = sprint_id
        self._project_key = project_key
        self._state_colors = state_colors

    def compose(self) -> ComposeResult:
        with Container(id="sdm-dialog", classes="dialog"):
            with VerticalScroll(id="sdm-scroll"):
                yield Vertical(Static("Loading…", classes="sdm-muted"), id="sdm-body")

    def on_mount(self) -> None:
        dialog = self.query_one("#sdm-dialog", Container)
        dialog.border_title = "Sprint"
        dialog.border_subtitle = "Esc to close"
        self.run_worker(self._load(), group="sdm-load")

    def action_close(self) -> None:
        self.dismiss(None)

    async def _load(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            sprint = await client.sprints.get_sprint(self._sprint_id)
        except TissueApiError:
            await self._mount([Static("Couldn't load sprint.", classes="sdm-muted")])
            return
        try:
            page = await client.issues.search_project_issues(
                self._project_key, sprint_ids=[self._sprint_id]
            )
            issues = list(page.content or [])
        except TissueApiError:
            issues = []
        self.query_one("#sdm-dialog", Container).border_title = (
            sprint.sprint_key or "Sprint"
        )
        widgets: list[Widget] = sprint_meta_widgets(
            sprint, self.app.theme_variables, title_class="sdm-title"
        )
        widgets.append(Static(f"Issues ({len(issues)})", classes="sdm-section-title"))
        if issues:
            widgets.append(
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
                    id="sdm-issues",
                    classes="hub-table",
                )
            )
        else:
            widgets.append(Static("No issues.", classes="sdm-muted"))
        await self._mount(widgets)

    async def _mount(self, widgets: list[Widget]) -> None:
        body = self.query_one("#sdm-body", Vertical)
        with self.app.batch_update():
            await body.remove_children()
            await body.mount(*widgets)
