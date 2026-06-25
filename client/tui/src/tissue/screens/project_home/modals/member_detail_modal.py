from __future__ import annotations

from typing import TYPE_CHECKING

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import Rule

from tissue.screens.base import TissueModal
from tissue.screens.project_home.areas.members import (
    fetch_member_issues,
    member_issue_section,
    member_read_view,
)

if TYPE_CHECKING:
    from textual.widget import Widget

    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )


class MemberDetailModal(TissueModal[None]):
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
