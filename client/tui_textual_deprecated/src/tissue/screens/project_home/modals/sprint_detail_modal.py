from __future__ import annotations

from typing import TYPE_CHECKING

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll
from textual.widgets import Static

from tissue.api.errors import TissueApiError
from tissue.screens.base import ScrollableModal
from tissue.screens.project_home.modals.edit_sprint_modal import EditSprintModal
from tissue.screens.project_home.modals.sprint_transition_modal import (
    SprintTransitionModal,
    available_sprint_actions,
)
from tissue.screens.project_home.sprint_rendering import (
    sprint_detail_widgets,
)

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.sprint_detail import SprintDetail


class SprintDetailModal(ScrollableModal[None]):
    """Read a sprint and act on it: edit, transition."""

    CSS_PATH = "sprint_detail_modal.tcss"

    BINDINGS = [
        Binding("e", "edit", "edit"),
        Binding("t", "transition", "transition"),
        Binding("escape", "close", "close"),
    ]

    def __init__(
        self,
        *,
        sprint_id: int,
        project_key: str,
        state_colors: dict[int, str],
        is_manager: bool = False,
    ) -> None:
        super().__init__()
        self._sprint_id = sprint_id
        self._project_key = project_key
        self._state_colors = state_colors
        self._is_manager = is_manager
        self._sprint: SprintDetail | None = None
        self._status = ""

    def compose(self) -> ComposeResult:
        with Container(id="sdm-dialog", classes="dialog"):
            with VerticalScroll(id="sdm-scroll"):
                yield Vertical(Static("Loading…", classes="sdm-muted"), id="sdm-body")

    def on_mount(self) -> None:
        dialog = self.query_one("#sdm-dialog", Container)
        dialog.border_title = "Sprint"
        dialog.border_subtitle = "Esc to close"
        self._reload()

    def action_close(self) -> None:
        self.dismiss(None)

    def _reload(self) -> None:
        self.run_worker(self._load(), group="sdm-load", exclusive=True)

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
        self._sprint = sprint
        self._status = (sprint.status or "").upper()
        dialog = self.query_one("#sdm-dialog", Container)
        dialog.border_title = sprint.sprint_key or "Sprint"
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
        dialog.border_subtitle = self._subtitle()

    def _subtitle(self) -> str:
        hints = []
        if self._is_editable():
            hints.append("e edit")
        if self._can_transition():
            hints.append("t transition")
        hints.append("Esc close")
        return " · ".join(hints)

    def _is_editable(self) -> bool:
        return self._is_manager and self._status in ("PLANNING", "ACTIVE")

    def _can_transition(self) -> bool:
        return self._is_manager and bool(available_sprint_actions(self._status))

    def action_edit(self) -> None:
        if self._sprint is None or not self._is_editable():
            return
        self.app.push_screen(
            EditSprintModal(
                sprint_id=self._sprint_id,
                current={
                    "title": self._sprint.title or "",
                    "goal": self._sprint.goal or "",
                    "dueAt": (
                        self._sprint.due_at.isoformat() if self._sprint.due_at else ""
                    ),
                },
                show_due=self._status == "ACTIVE",
            ),
            self._on_mutated,
        )

    def action_transition(self) -> None:
        if not self._can_transition():
            return
        self.app.push_screen(
            SprintTransitionModal(sprint_id=self._sprint_id, status=self._status),
            self._on_mutated,
        )

    def _on_mutated(self, updated: bool | None) -> None:
        if updated:
            self._reload()

    async def _mount(self, widgets: list[Widget]) -> None:
        body = self.query_one("#sdm-body", Vertical)
        with self.app.batch_update():
            await body.remove_children()
            await body.mount(*widgets)
