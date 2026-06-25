from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container
from textual.content import Content
from textual.css.query import NoMatches
from textual.widgets import Input, OptionList, Select
from textual.widgets.option_list import Option

from tissue.screens.base import TissueModal


class RelationAddModal(TissueModal["tuple[str, str] | None"]):
    """Pick a relation type and a target issue.

    Closes with (type, target key), or None if cancelled. The caller passes in
    only issues from the same project that are allowed, and the server checks
    again to be sure.
    """

    CSS_PATH = "relation_add_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    # Most common type first so it becomes the Select's default.
    _TYPES: list[tuple[str, str]] = [
        ("Relevant to", "RELEVANT"),
        ("Blocks", "BLOCKS"),
        ("Causes", "CAUSES"),
        ("Duplicates", "DUPLICATES"),
    ]

    def __init__(self, *, candidates: list[tuple[str, str]]) -> None:
        super().__init__()
        self._candidates = list(candidates)
        self._search_text = ""

    def _matches(self) -> list[tuple[str, str]]:
        search_text = self._search_text
        return [
            (label, key)
            for label, key in self._candidates
            if not search_text or search_text in label.casefold()
        ]

    def _options(self) -> list[Option]:
        # Content() skips markup parsing so a '[' in a title shows as is.
        return [Option(Content(label), id=key) for label, key in self._matches()]

    def compose(self) -> ComposeResult:
        with Container(id="rel-dialog", classes="dialog"):
            yield Select(
                self._TYPES, value="RELEVANT", allow_blank=False, id="rel-type"
            )
            yield Input(placeholder="Search issues…", id="rel-search")
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

    @on(Input.Changed, "#rel-search")
    def _on_search(self, event: Input.Changed) -> None:
        self._search_text = event.value.strip().casefold()
        try:
            option_list = self.query_one("#rel-list", OptionList)
        except NoMatches:
            return
        option_list.clear_options()
        option_list.add_options(self._options())

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
