from __future__ import annotations

from typing import TYPE_CHECKING

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.screens.base import ScrollableModal
from tissue.screens.project_home.sprint_rendering import (
    sprint_detail_widgets,
)

if TYPE_CHECKING:
    from textual.widget import Widget


class SprintDetailModal(ScrollableModal[None]):
    """Read-only sprint detail dialog."""

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
        widgets: list[Widget] = sprint_detail_widgets(
            sprint,
            issues,
            self._state_colors,
            self.app.theme_variables,
            title_class="sdm-title",
            content_class="sdm-content",
            muted_class="sdm-muted",
            issue_title_class="sdm-section-title",
            issue_table_id="sdm-issues",
            issue_table_classes="hub-table",
            spacer_class="sdm-spacer",
        )
        await self._mount(widgets)

    async def _mount(self, widgets: list[Widget]) -> None:
        body = self.query_one("#sdm-body", Vertical)
        with self.app.batch_update():
            await body.remove_children()
            await body.mount(*widgets)
