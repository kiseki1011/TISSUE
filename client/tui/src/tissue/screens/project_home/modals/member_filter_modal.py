from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal, Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widgets import Button, Label, RadioButton, RadioSet
from textual.widgets.selection_list import Selection

from tissue.screens.base import TissueModal
from tissue.screens.project_home.member_filter import MemberFilter
from tissue.widgets.filter_selection_list import FilterSelectionList as SelectionList


class MemberFilterModal(TissueModal["MemberFilter | None"]):
    """Filter the [1] Members list by active status and role.

    Closes with the chosen `MemberFilter`, or None if cancelled.
    """

    CSS_PATH = "member_filter_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    _ACTIVE_OPTIONS = (
        ("All", "all"),
        ("Active only", "active"),
        ("Inactive only", "inactive"),
    )
    _ROLE_OPTIONS = (("Member", "MEMBER"), ("Manager", "MANAGER"))
    _KIND_OPTIONS = (
        ("All", "all"),
        ("Humans only", "human"),
        ("Agents only", "agent"),
    )

    def __init__(self, *, current: MemberFilter) -> None:
        super().__init__()
        self._current = current

    def compose(self) -> ComposeResult:
        current = self._current
        with Container(id="mfm-dialog", classes="dialog"):
            with VerticalScroll(id="mfm-scroll"), Vertical(id="mfm-body"):
                yield Label("Active", classes="mfm-label")
                with RadioSet(id="mfm-active"):
                    for label, value in self._ACTIVE_OPTIONS:
                        yield RadioButton(
                            label,
                            value=(value == current.active),
                            id=f"mfm-active-{value}",
                        )
                yield Label("Type", classes="mfm-label")
                with RadioSet(id="mfm-kind"):
                    for label, value in self._KIND_OPTIONS:
                        yield RadioButton(
                            label,
                            value=(value == current.kind),
                            id=f"mfm-kind-{value}",
                        )
                yield Label("Role", classes="mfm-label")
                yield SelectionList[str](
                    *(
                        Selection(label, value, value in current.roles)
                        for label, value in self._ROLE_OPTIONS
                    ),
                    id="mfm-role",
                )
            with Horizontal(id="mfm-buttons"):
                yield Button("Reset", id="mfm-reset")
                yield Button("Cancel", id="mfm-cancel", classes="-btn-error")
                yield Button("Apply", id="mfm-apply", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#mfm-dialog", Container)
        dialog.border_title = "Filter Members"
        dialog.border_subtitle = "Esc to cancel"
        try:
            self.query_one("#mfm-active", RadioSet).focus()
        except NoMatches:
            pass

    def action_cancel(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#mfm-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    @on(Button.Pressed, "#mfm-apply")
    def _on_apply(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(self._collect())

    @on(Button.Pressed, "#mfm-reset")
    def _on_reset(self, event: Button.Pressed) -> None:
        event.stop()
        self.query_one("#mfm-active-all", RadioButton).value = True
        self.query_one("#mfm-kind-all", RadioButton).value = True
        self.query_one("#mfm-role", SelectionList).deselect_all()

    def _collect(self) -> MemberFilter:
        pressed = self.query_one("#mfm-active", RadioSet).pressed_button
        active = "all"
        if pressed is not None and pressed.id:
            active = pressed.id.removeprefix("mfm-active-")
        kind_pressed = self.query_one("#mfm-kind", RadioSet).pressed_button
        kind = "all"
        if kind_pressed is not None and kind_pressed.id:
            kind = kind_pressed.id.removeprefix("mfm-kind-")
        roles = tuple(self.query_one("#mfm-role", SelectionList).selected)
        return MemberFilter(active=active, kind=kind, roles=roles)
