from __future__ import annotations

from dataclasses import dataclass

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.css.query import NoMatches
from textual.widgets import Input, OptionList, Select
from textual.widgets.option_list import Option

from tissue.screens.base import TissueModal
from tissue.widgets.color_type import color_hex
from tissue.widgets.filter_checkbox import FilterCheckbox as Checkbox
from tissue.widgets.issue_chips import color_chip, priority_chip


@dataclass(frozen=True)
class RelationCandidate:
    """One pickable issue with the fields shown in its row."""

    key: str
    title: str | None
    type_name: str | None
    type_color: str | None
    status_label: str | None
    priority: str | None
    completed: bool


class RelationAddModal(TissueModal["tuple[str, str] | None"]):
    """Pick a relation type and target issue.

    Reverse types (Blocked by / Caused by / Duplicated by) have no API enum, so
    their value carries a `_BY` suffix and the caller inverts source/target.
    """

    CSS_PATH = "relation_add_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    # Most common type first so it becomes the Select's default.
    _TYPES: list[tuple[str, str]] = [
        ("Relevant to", "RELEVANT"),
        ("Blocks", "BLOCKS"),
        ("Blocked by", "BLOCKED_BY"),
        ("Causes", "CAUSES"),
        ("Caused by", "CAUSED_BY"),
        ("Duplicates", "DUPLICATES"),
        ("Duplicated by", "DUPLICATED_BY"),
    ]

    def __init__(self, *, candidates: list[RelationCandidate]) -> None:
        super().__init__()
        self._candidates = list(candidates)
        self._search_text = ""
        self._show_completed = False

    def _matches(self) -> list[RelationCandidate]:
        search = self._search_text
        result: list[RelationCandidate] = []
        for cand in self._candidates:
            if cand.completed and not self._show_completed:
                continue
            if (
                search
                and search not in cand.key.casefold()
                and not (cand.title and search in cand.title.casefold())
            ):
                continue
            result.append(cand)
        return result

    def _row(self, cand: RelationCandidate) -> Text:
        text = Text(cand.key, style="bold")
        if cand.type_name:
            text.append("  ")
            text.append(cand.type_name, style=color_hex(cand.type_color))
        status = color_chip(cand.status_label or "-", None)
        text.append("  ")
        text.append(status if isinstance(status, Text) else Text(status))
        priority = priority_chip(self.app.theme_variables, cand.priority)
        text.append(" ")
        text.append(priority if isinstance(priority, Text) else Text(priority))
        if cand.title:
            text.append("  ")
            text.append(cand.title)
        return text

    def _options(self) -> list[Option]:
        # Text renders without markup parsing so a '[' in a title shows as is.
        return [Option(self._row(cand), id=cand.key) for cand in self._matches()]

    def compose(self) -> ComposeResult:
        with Container(id="rel-dialog", classes="dialog"):
            yield Select(
                self._TYPES, value="RELEVANT", allow_blank=False, id="rel-type"
            )
            yield Input(placeholder="Search issues…", id="rel-search")
            yield Checkbox("Show completed", value=False, id="rel-completed")
            yield OptionList(*self._options(), id="rel-list")

    def on_mount(self) -> None:
        dialog = self.query_one("#rel-dialog", Container)
        dialog.border_title = "Add relation"
        dialog.border_subtitle = "Pick a type, then an issue · Esc to cancel"
        if not self._candidates:
            self.query_one("#rel-list", OptionList).add_option(
                Option("No eligible issues.", disabled=True)
            )
        self.query_one("#rel-search", Input).focus()

    def _rebuild(self) -> None:
        try:
            option_list = self.query_one("#rel-list", OptionList)
        except NoMatches:
            return
        option_list.clear_options()
        option_list.add_options(self._options())

    @on(Input.Changed, "#rel-search")
    def _on_search(self, event: Input.Changed) -> None:
        self._search_text = event.value.strip().casefold()
        self._rebuild()

    @on(Checkbox.Changed, "#rel-completed")
    def _on_completed(self, event: Checkbox.Changed) -> None:
        self._show_completed = event.value
        self._rebuild()

    @on(OptionList.OptionSelected, "#rel-list")
    def _on_option(self, event: OptionList.OptionSelected) -> None:
        key = event.option.id
        if not key:
            return
        rel_type = self.query_one("#rel-type", Select).value
        if not isinstance(rel_type, str):
            return
        self.dismiss((rel_type, key))

    def action_cancel(self) -> None:
        self.dismiss(None)
