from __future__ import annotations

from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import DataTable, Rule

from tissue.screens.base import ScrollableModal
from tissue.screens.project_home.areas.members import (
    fetch_member_issues,
    member_issue_section,
    member_read_view,
)

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.issue_summary import IssueSummary
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )


class MemberDetailModal(ScrollableModal[None]):
    """Read-only member detail in a centered dialog. Dismisses on Esc."""

    CSS_PATH = "member_detail_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(
        self,
        *,
        member: ProjectMemberSummary,
        project_key: str,
        state_colors: dict[int, str],
    ) -> None:
        super().__init__()
        self._member = member
        self._project_key = project_key
        self._state_colors = state_colors
        self._assigned: list[IssueSummary] = []
        self._reviewing: list[IssueSummary] = []

    def compose(self) -> ComposeResult:
        with Container(id="mdm-dialog", classes="dialog"):
            with VerticalScroll(id="mdm-scroll"):
                yield Vertical(
                    *member_read_view(
                        self._member,
                        title_class="mdm-title",
                        spacer_class="mdm-spacer",
                    ),
                    id="mdm-body",
                )

    def on_mount(self) -> None:
        dialog = self.query_one("#mdm-dialog", Container)
        dialog.border_title = (
            self._member.display_name or self._member.username or "Member"
        )
        dialog.border_subtitle = "Esc to close"
        self.run_worker(self._load(), group="mdm-load")

    def action_close(self) -> None:
        self.dismiss(None)

    @on(DataTable.RowSelected, "#mdm-assigned")
    def _on_assigned_selected(self, event: DataTable.RowSelected) -> None:
        event.stop()
        self._open_issue(self._assigned, event.cursor_row)

    @on(DataTable.RowSelected, "#mdm-reviewing")
    def _on_reviewing_selected(self, event: DataTable.RowSelected) -> None:
        event.stop()
        self._open_issue(self._reviewing, event.cursor_row)

    def _open_issue(self, issues: list[IssueSummary], row_index: int) -> None:
        if not (0 <= row_index < len(issues)):
            return
        summary = issues[row_index]
        if not summary.issue_key:
            return
        from tissue.screens.project_home.modals.issue_detail_modal import (
            IssueDetailModal,
        )

        self.app.push_screen(
            IssueDetailModal(
                issue_key=summary.issue_key,
                project_key=self._project_key,
                summary=summary,
            )
        )

    async def _load(self) -> None:
        """Fetch the member's Assigned/Reviewing issues and rebuild the body.

        We rebuild the whole body instead of adding to it. Adding a table to a
        body that is already laid out shrinks it to 0 height, while rebuilding
        lets each issue table size itself. batch_update does the clear and
        rebuild in one step so the first fields don't flash on screen.
        """
        assigned, reviewing = await fetch_member_issues(
            self.app.client, self._project_key, self._member.member_id
        )
        self._assigned = assigned
        self._reviewing = reviewing
        widgets: list[Widget] = member_read_view(
            self._member, title_class="mdm-title", spacer_class="mdm-spacer"
        )
        widgets.append(Rule())
        widgets.extend(
            member_issue_section(
                "Assigned",
                assigned,
                self._state_colors,
                self.app.theme_variables,
                table_id="mdm-assigned",
                title_class="mdm-section-title",
                muted_class="mdm-muted",
            )
        )
        widgets.extend(
            member_issue_section(
                "Reviewing",
                reviewing,
                self._state_colors,
                self.app.theme_variables,
                table_id="mdm-reviewing",
                title_class="mdm-section-title",
                muted_class="mdm-muted",
            )
        )
        try:
            body = self.query_one("#mdm-body", Vertical)
        except NoMatches:
            return  # dismissed before the fetch returned
        with self.app.batch_update():
            await body.remove_children()
            await body.mount(*widgets)
