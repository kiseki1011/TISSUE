from __future__ import annotations

import logging

from rich.text import Text
from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.css.query import NoMatches
from textual.widgets import Button, Label, SelectionList
from textual.widgets.selection_list import Selection

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal
from tissue.screens.wiki.tag_colors import tag_chip_style

log = logging.getLogger(__name__)

# (tag id, name, color-enum-or-None)
FilterTag = tuple[int, str, str | None]


def _pill(name: str, color: str | None) -> Text:
    """A single tag rendered as a solid pill (name on its own color); plain text
    when the tag has no resolvable color."""
    style = tag_chip_style(color)
    text = f" {name} "
    return Text(text, style=style) if style else Text(name)


class TagFilterModal(TissueModal[list[FilterTag] | None]):
    """Choose which tags to filter the document list by: a multi-select of the
    global tag catalog (colored pills), pre-checking the active ones. Dismisses
    with the chosen (id, name, color) tuples, or None if cancelled."""

    CSS_PATH = "tag_filter_modal.tcss"

    BINDINGS = [
        Binding("escape", "cancel", "cancel"),
    ]

    def __init__(self, selected_ids: set[int]) -> None:
        super().__init__()
        # Tag ids active in the current filter (pre-checked when the list loads).
        self._selected_ids = set(selected_ids)
        # id -> (id, name, color), filled once the catalog loads.
        self._by_id: dict[int, FilterTag] = {}

    def compose(self) -> ComposeResult:
        with Container(id="tag-filter-dialog", classes="dialog"):
            yield Label("Loading tags…", id="tag-filter-status", classes="status-msg")
            # Populated in _load_tags once the catalog arrives.
            yield SelectionList[int](id="tag-filter-list")
            with Horizontal(id="tag-filter-buttons"):
                yield Button("Cancel", id="tag-filter-cancel", classes="-btn-error")
                yield Button("Apply", id="tag-filter-apply", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#tag-filter-dialog", Container)
        dialog.border_title = "Filter by tag"
        dialog.border_subtitle = "Esc to cancel"
        self.run_worker(self._load_tags(), exclusive=True, group="tag-filter-catalog")

    async def _load_tags(self) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            tags = await client.wiki.search_tags()
        except TissueApiError as e:
            log.debug("Tag filter: couldn't load catalog: %s", e)
            self._status("Couldn't load tags. Close and try again.")
            return
        catalog = [
            (t.tag_id, t.name, t.color) for t in tags if t.tag_id is not None and t.name
        ]
        self._by_id = {tid: (tid, name, color) for tid, name, color in catalog}
        try:
            select = self.query_one("#tag-filter-list", SelectionList)
        except NoMatches:
            return
        if not catalog:
            self._status("No tags yet — create some on a document first.")
            return
        for tid, name, color in catalog:
            select.add_option(
                Selection(_pill(name, color), tid, tid in self._selected_ids)
            )
        self._status("")
        select.focus()

    @on(Button.Pressed, "#tag-filter-apply")
    def _on_apply(self, event: Button.Pressed) -> None:
        event.stop()
        try:
            select = self.query_one("#tag-filter-list", SelectionList)
        except NoMatches:
            self.dismiss(None)
            return
        chosen = [self._by_id[tid] for tid in select.selected if tid in self._by_id]
        self.dismiss(chosen)

    @on(Button.Pressed, "#tag-filter-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    def action_cancel(self) -> None:
        self.dismiss(None)

    def _status(self, message: str) -> None:
        try:
            label = self.query_one("#tag-filter-status", Label)
        except NoMatches:
            return
        label.update(message)
        label.display = bool(message)
