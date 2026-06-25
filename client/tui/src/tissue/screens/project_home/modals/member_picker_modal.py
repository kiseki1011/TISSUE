from __future__ import annotations

from typing import TYPE_CHECKING

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.widgets import Input, OptionList
from textual.widgets.option_list import Option

from tissue.screens.base import TissueModal
from tissue.widgets.color_type import color_hex

if TYPE_CHECKING:
    from tissue.api.generated.models.project_member_summary import (
        ProjectMemberSummary,
    )

# Special value meaning "clear the current assignee". Member ids are always
# positive, so -1 can never match a real one.
UNASSIGN = -1
_UNASSIGN_ID = "unassign"


class MemberPickerModal(TissueModal[int | None]):
    """Pick what to do with the assignee.

    Choices:
        - Pick a member to assign
        - Clear the assignee (`UNASSIGN`)
        - Cancel
    """

    CSS_PATH = "member_picker_modal.tcss"

    BINDINGS = [
        Binding("escape", "close", "close"),
    ]

    def __init__(
        self, members: list[ProjectMemberSummary], *, assigned: bool = False
    ) -> None:
        super().__init__()
        self._members = members
        self._assigned = assigned

    def compose(self) -> ComposeResult:
        search = Input(placeholder="Filter members…", id="member-picker-search")
        picker = OptionList(id="member-picker-list")
        dialog = Container(search, picker, id="member-picker-dialog", classes="dialog")
        dialog.border_title = "Assignee"
        dialog.border_subtitle = "Esc to cancel"
        yield dialog

    def on_mount(self) -> None:
        self._populate("")
        self.query_one("#member-picker-search", Input).focus()

    def _populate(self, filter_text: str) -> None:
        picker = self.query_one("#member-picker-list", OptionList)
        picker.clear_options()
        filter_text = filter_text.strip().lower()
        # "Clear assignee" isn't a member, so don't show it while filtering.
        if self._assigned and not filter_text:
            warning_color = color_hex(self.app.theme_variables.get("warning"))
            label = "— Clear assignee —"
            prompt = Text(label, style=warning_color) if warning_color else label
            picker.add_option(Option(prompt, id=_UNASSIGN_ID))
        for member in self._members:
            if member.member_id is None:
                continue
            label = self._label(member)
            if filter_text and filter_text not in label.lower():
                continue
            picker.add_option(Option(label, id=str(member.member_id)))

    @staticmethod
    def _label(member: ProjectMemberSummary) -> str:
        name = member.display_name or member.username or "-"
        handle = f" (@{member.username})" if member.username else ""
        return f"{name}{handle}"

    @on(Input.Changed, "#member-picker-search")
    def _on_filter(self, event: Input.Changed) -> None:
        self._populate(event.value)

    @on(Input.Submitted, "#member-picker-search")
    def _on_filter_submit(self) -> None:
        picker = self.query_one("#member-picker-list", OptionList)
        if picker.option_count:
            picker.highlighted = 0
            picker.focus()

    @on(OptionList.OptionSelected, "#member-picker-list")
    def _on_selected(self, event: OptionList.OptionSelected) -> None:
        option_id = event.option.id
        if option_id == _UNASSIGN_ID:
            self.dismiss(UNASSIGN)
        elif option_id is not None:
            self.dismiss(int(option_id))

    def action_close(self) -> None:
        self.dismiss(None)
