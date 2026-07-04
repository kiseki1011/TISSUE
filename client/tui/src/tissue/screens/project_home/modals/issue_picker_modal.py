from __future__ import annotations

from dataclasses import dataclass

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.content import Content
from textual.css.query import NoMatches
from textual.widgets import Button, Input, Label, OptionList
from textual.widgets.option_list import Option
from textual.widgets.selection_list import Selection

from tissue.screens.base import TissueModal
from tissue.widgets.filter_checkbox import FilterCheckbox as Checkbox
from tissue.widgets.filter_selection_list import FilterSelectionList as SelectionList


def _clip(text: str, width: int) -> str:
    return text if len(text) <= width else text[: width - 1] + "…"


@dataclass(frozen=True)
class PickerCandidate:
    """One selectable issue, with the columns the modal renders and filters on."""

    key: str
    title: str | None = None
    issue_type_name: str | None = None
    status_label: str | None = None
    priority: str | None = None
    category: str | None = None

    @property
    def search_text(self) -> str:
        return self.key + (f"  {self.title}" if self.title else "")


class IssuePickerModal(TissueModal["list[str] | None"]):
    """Pick one issue or several from a searchable list."""

    CSS_PATH = "issue_picker_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    def __init__(
        self,
        *,
        candidates: list[PickerCandidate],
        multi: bool,
        title: str,
        subtitle: str | None = None,
    ) -> None:
        super().__init__()
        self._candidates = list(candidates)
        self._multi = multi
        self._title = title
        self._subtitle = subtitle
        self._checked: set[str] = set()
        self._keyword = ""
        self._include_completed = False
        # Ignore SelectedChanged while we rebuild the multi list in code.
        self._rebuilding = False

    def _matches(self) -> list[PickerCandidate]:
        keyword = self._keyword
        result: list[PickerCandidate] = []
        for cand in self._candidates:
            if cand.category == "ABORTED":
                continue
            if cand.category == "COMPLETED" and not self._include_completed:
                continue
            if keyword and keyword not in cand.search_text.casefold():
                continue
            result.append(cand)
        return result

    @staticmethod
    def _row_text(cand: PickerCandidate) -> str:
        title = _clip(cand.title or "", 30)
        type_name = _clip(cand.issue_type_name or "-", 12)
        status = _clip(cand.status_label or "-", 14)
        priority = cand.priority or "-"
        return f"{cand.key:<10}  {title:<30}  {type_name:<12}  {status:<14}  {priority}"

    def _selections(self) -> list[Selection[str]]:
        # Content wraps rows so a title with '[' (e.g. "[BUG]") shows as-is
        # instead of being read as Textual markup, which would crash or drop it.
        return [
            Selection(
                Content(self._row_text(cand)), cand.key, cand.key in self._checked
            )
            for cand in self._matches()
        ]

    def _options(self) -> list[Option]:
        return [
            Option(Content(self._row_text(cand)), id=cand.key)
            for cand in self._matches()
        ]

    def compose(self) -> ComposeResult:
        with Container(id="picker-dialog", classes="dialog"):
            yield Input(placeholder="Search issues…", id="picker-search")
            yield Checkbox("Include completed issues", id="picker-completed")
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
        if not self._candidates:
            self._empty_note()
        self.query_one("#picker-search", Input).focus()

    def _empty_note(self) -> None:
        if self._multi:
            self.query_one("#picker-count", Label).update("No eligible issues.")

    @on(Input.Changed, "#picker-search")
    def _on_search(self, event: Input.Changed) -> None:
        if self._multi:
            self._sync_checked()
        self._keyword = event.value.strip().casefold()
        self._rebuild()

    @on(Checkbox.Changed, "#picker-completed")
    def _on_completed_toggle(self, event: Checkbox.Changed) -> None:
        # Sync against the currently-shown filter BEFORE flipping the flag, so a
        # checked-but-now-hidden pick survives (mirrors _on_search's ordering).
        if self._multi:
            self._sync_checked()
        self._include_completed = event.value
        self._rebuild()

    def _rebuild(self) -> None:
        try:
            if self._multi:
                selection_list = self.query_one("#picker-list", SelectionList)
                self._rebuilding = True
                try:
                    selection_list.clear_options()
                    selection_list.add_options(self._selections())
                finally:
                    self._rebuilding = False
            else:
                option_list = self.query_one("#picker-list", OptionList)
                option_list.clear_options()
                option_list.add_options(self._options())
        except NoMatches:
            return

    @on(OptionList.OptionSelected, "#picker-list")
    def _on_option(self, event: OptionList.OptionSelected) -> None:
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
        """Merge what is checked now into the saved set so hidden picks survive."""
        try:
            selection_list = self.query_one("#picker-list", SelectionList)
        except NoMatches:
            return
        shown = {cand.key for cand in self._matches()}
        self._checked = (self._checked - shown) | set(selection_list.selected)

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
