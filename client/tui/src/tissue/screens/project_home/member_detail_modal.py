"""A centered, read-only project-member detail modal. Used by the hub's expanded
mode (CTRL+F): with [2] hidden, pressing Enter on a member row pops their detail
here — mirrors IssueDetailModal. The member data is already loaded (passed in), so
there's no fetch."""

from __future__ import annotations

from typing import TYPE_CHECKING

from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Vertical, VerticalScroll

from tissue.screens.base import TissueModal
from tissue.screens.project_home.areas.members import member_read_view

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )


class MemberDetailModal(TissueModal[None]):
    """Read-only member detail in a centered dialog. Dismisses on Esc."""

    CSS_PATH = "member_detail_modal.tcss"

    BINDINGS = [Binding("escape", "close", "close")]

    def __init__(self, *, member: ProjectMemberSummary) -> None:
        super().__init__()
        self._member = member

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

    def action_close(self) -> None:
        self.dismiss(None)
