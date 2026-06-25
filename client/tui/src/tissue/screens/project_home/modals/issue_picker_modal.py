"""Searchable issue picker for the parent/children hierarchy controls.

`multi=False` (parent): an `OptionList` where highlighting + Enter (or a click)
picks a single issue and dismisses immediately. `multi=True` (children): a
`SelectionList` of checkboxes with an Apply button; the checked set is tracked in
`_checked` so picks survive the list being rebuilt as you search. The caller
pre-filters `candidates` to the issues whose type sits exactly one hierarchy level
above/below this issue — this modal only searches and selects, and the backend
re-validates. Dismisses with the chosen issue key(s), or None on cancel.
"""

from __future__ import annotations

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.content import Content
from textual.css.query import NoMatches
from textual.widgets import Button, Input, Label, OptionList, SelectionList
from textual.widgets.option_list import Option
from textual.widgets.selection_list import Selection

from tissue.screens.base import TissueModal


class IssuePickerModal(TissueModal["list[str] | None"]):
    """Pick one issue (multi=False) or several (multi=True) from a candidate set."""

    CSS_PATH = "issue_picker_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    def __init__(
        self,
        *,
        candidates: list[tuple[str, str]],
        multi: bool,
        title: str,
        subtitle: str | None = None,
    ) -> None:
        super().__init__()
        # (display label, issue key) pairs, already filtered to legal candidates.
        self._all = list(candidates)
        self._multi = multi
        self._title = title
        self._subtitle = subtitle
        self._checked: set[str] = set()
        self._kw = ""
        # Guards SelectedChanged while we programmatically rebuild the multi list.
        self._rebuilding = False

    def _matches(self) -> list[tuple[str, str]]:
        kw = self._kw
        return [(lbl, key) for lbl, key in self._all if not kw or kw in lbl.casefold()]

    def _selections(self) -> list[Selection[str]]:
        # Wrap labels in Content so an issue title containing '[' (e.g. "[BUG]" or
        # "list[int]") is shown literally, not parsed as Textual markup — raw titles
        # would crash the list (MarkupError on "[/...]") or silently drop "[TAG]".
        return [
            Selection(Content(lbl), key, key in self._checked)
            for lbl, key in self._matches()
        ]

    def _options(self) -> list[Option]:
        return [Option(Content(lbl), id=key) for lbl, key in self._matches()]

    def compose(self) -> ComposeResult:
        with Container(id="picker-dialog", classes="dialog"):
            yield Input(placeholder="Search issues…", id="picker-search")
            if self._multi:
                yield SelectionList[str](*self._selections(), id="picker-list")
                yield Label("", id="picker-count", classes="picker-count")
                with Horizontal(id="picker-buttons"):
                    yield Button("Cancel", id="picker-cancel", classes="-btn-error")
                    yield Button("Apply", id="picker-apply", classes="-btn-success")
            else:
                yield OptionList(*self._options(), id="picker-list")

    def on_mount(self) -> None:
        dialog = self.query_one("#picker-dialog", Container)
        dialog.border_title = self._title
        dialog.border_subtitle = self._subtitle or "Esc to cancel"
        if self._multi:
            self._update_count()
        if not self._all:
            self._empty_note()
        self.query_one("#picker-search", Input).focus()

    def _empty_note(self) -> None:
        """Surface that nothing is selectable (no issue of the required hierarchy)."""
        if self._multi:
            self.query_one("#picker-count", Label).update("No eligible issues.")

    @on(Input.Changed, "#picker-search")
    def _on_search(self, event: Input.Changed) -> None:
        if self._multi:
            self._sync_checked()
        self._kw = event.value.strip().casefold()
        self._rebuild()

    def _rebuild(self) -> None:
        try:
            if self._multi:
                lst = self.query_one("#picker-list", SelectionList)
                self._rebuilding = True
                try:
                    lst.clear_options()
                    lst.add_options(self._selections())
                finally:
                    self._rebuilding = False
            else:
                opts = self.query_one("#picker-list", OptionList)
                opts.clear_options()
                opts.add_options(self._options())
        except NoMatches:
            return

    @on(OptionList.OptionSelected, "#picker-list")
    def _on_option(self, event: OptionList.OptionSelected) -> None:
        # Single-select: the OptionList option id is the issue key.
        key = event.option.id
        if key:
            self.dismiss([key])

    @on(SelectionList.SelectedChanged, "#picker-list")
    def _on_changed(self) -> None:
        if self._rebuilding:
            return
        self._sync_checked()
        self._update_count()

    def _sync_checked(self) -> None:
        """Fold the live selection into the tracked set: drop the currently-shown
        keys, then re-add the checked ones — so filtered-out picks survive."""
        try:
            lst = self.query_one("#picker-list", SelectionList)
        except NoMatches:
            return
        shown = {key for _, key in self._matches()}
        self._checked = (self._checked - shown) | set(lst.selected)

    def _update_count(self) -> None:
        try:
            label = self.query_one("#picker-count", Label)
        except NoMatches:
            return
        label.update(f"{len(self._checked)} selected")

    @on(Button.Pressed, "#picker-apply")
    def _on_apply(self, event: Button.Pressed) -> None:
        event.stop()
        self._sync_checked()
        self.dismiss(sorted(self._checked))

    @on(Button.Pressed, "#picker-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    def action_cancel(self) -> None:
        self.dismiss(None)
