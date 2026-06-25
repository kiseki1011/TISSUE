"""Add an issue relation: pick a relation type, then a target issue.

A `Select` chooses the relation type (from the current issue's perspective — it
*blocks* / *causes* / *duplicates* / is *relevant* to the target); a searchable
`OptionList` picks the target. Selecting an issue dismisses with `(relation_type,
target_issue_key)`; Esc dismisses with None. The caller pre-filters `candidates` to
same-project issues that aren't the issue itself or already related; the backend
re-validates (one relation per pair, no self-reference).
"""

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
    """Pick a relation type + a target issue. Dismisses with (type, target key)."""

    CSS_PATH = "relation_add_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    # (display label, relation type sent to the API), most common first.
    _TYPES: list[tuple[str, str]] = [
        ("Relevant to", "RELEVANT"),
        ("Blocks", "BLOCKS"),
        ("Causes", "CAUSES"),
        ("Duplicates", "DUPLICATES"),
    ]

    def __init__(self, *, candidates: list[tuple[str, str]]) -> None:
        super().__init__()
        # (display label, issue key) pairs, already filtered to legal targets.
        self._all = list(candidates)
        self._kw = ""

    def _matches(self) -> list[tuple[str, str]]:
        kw = self._kw
        return [(lbl, key) for lbl, key in self._all if not kw or kw in lbl.casefold()]

    def _options(self) -> list[Option]:
        # Content() keeps an issue title containing '[' literal (no markup parsing).
        return [Option(Content(lbl), id=key) for lbl, key in self._matches()]

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
        if not self._all:
            self.query_one("#rel-list", OptionList).add_option(
                Option("No eligible issues.", disabled=True)
            )
        self.query_one("#rel-search", Input).focus()

    @on(Input.Changed, "#rel-search")
    def _on_search(self, event: Input.Changed) -> None:
        self._kw = event.value.strip().casefold()
        try:
            opts = self.query_one("#rel-list", OptionList)
        except NoMatches:
            return
        opts.clear_options()
        opts.add_options(self._options())

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
