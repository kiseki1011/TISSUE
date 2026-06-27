from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.app import ComposeResult
from textual.binding import Binding
from textual.containers import Container, Horizontal
from textual.css.query import NoMatches
from textual.widgets import Button, Input, SelectionList, Static
from textual.widgets.selection_list import Selection

from tissue.api.errors import TissueApiError
from tissue.screens.base import TissueModal

if TYPE_CHECKING:
    from textual.timer import Timer

    from tissue.api.generated.models.member_candidate_summary import (
        MemberCandidateSummary,
    )

log = logging.getLogger(__name__)

_DEBOUNCE = 0.3


class MemberAddModal(TissueModal["bool | None"]):
    """Search global members and add the chosen ones to the project.

    Dismisses True after a successful add (so the list reloads), None on cancel.
    Manager-only on the server; the button that opens this is gated to managers.
    """

    CSS_PATH = "member_add_modal.tcss"

    BINDINGS = [Binding("escape", "cancel", "cancel")]

    def __init__(self, *, project_key: str) -> None:
        super().__init__()
        self._project_key = project_key
        self._search_timer: Timer | None = None
        self._adding = False

    def compose(self) -> ComposeResult:
        with Container(id="mam-dialog", classes="dialog"):
            yield Input(
                placeholder="Search by name, username or email…", id="mam-search"
            )
            yield SelectionList[str](id="mam-results")
            yield Static("", id="mam-status", classes="mam-status")
            with Horizontal(id="mam-buttons"):
                yield Button("Cancel", id="mam-cancel", classes="-btn-error")
                yield Button("Add", id="mam-add", classes="-btn-success")

    def on_mount(self) -> None:
        dialog = self.query_one("#mam-dialog", Container)
        dialog.border_title = "Add Members"
        dialog.border_subtitle = "Esc to cancel"
        self.query_one("#mam-search", Input).focus()
        # Show all candidates up front (blank keyword), before any typing.
        self.run_worker(self._search(None), exclusive=True, group="mam-search")

    @on(Input.Changed, "#mam-search")
    def _on_search_changed(self, event: Input.Changed) -> None:
        if self._search_timer is not None:
            self._search_timer.stop()
        keyword = event.value.strip() or None
        self._search_timer = self.set_timer(
            _DEBOUNCE,
            lambda: self.run_worker(
                self._search(keyword), exclusive=True, group="mam-search"
            ),
        )

    async def _search(self, keyword: str | None) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            candidates = await client.project_members.list_member_candidates(
                self._project_key, keyword=keyword
            )
        except TissueApiError as error:
            self._set_status(f"Couldn't search: {error.detail or 'please try again'}")
            return
        results = self.query_one("#mam-results", SelectionList)
        results.clear_options()
        if not candidates:
            self._set_status("No matching members.")
            return
        self._set_status("")
        for candidate in candidates:
            if candidate.member_id is None:
                continue
            results.add_option(Selection(_label(candidate), str(candidate.member_id)))

    def _set_status(self, message: str) -> None:
        try:
            self.query_one("#mam-status", Static).update(message)
        except NoMatches:
            pass

    def action_cancel(self) -> None:
        self.dismiss(None)

    @on(Button.Pressed, "#mam-cancel")
    def _on_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self.dismiss(None)

    @on(Button.Pressed, "#mam-add")
    def _on_add(self, event: Button.Pressed) -> None:
        event.stop()
        if self._adding:
            return
        selected = self.query_one("#mam-results", SelectionList).selected
        member_ids = [int(value) for value in selected]
        if not member_ids:
            self._set_status("Select at least one member to add.")
            return
        self._adding = True
        self.run_worker(self._add(member_ids), exclusive=True, group="mam-add")

    async def _add(self, member_ids: list[int]) -> None:
        client = self.app.client
        if client is None:
            self._adding = False
            return
        try:
            await client.project_members.add_project_members(
                self._project_key, member_ids
            )
        except TissueApiError as error:
            self._set_status(f"Couldn't add: {error.detail or 'please try again'}")
            self._adding = False
            return
        self.dismiss(True)


def _label(candidate: MemberCandidateSummary) -> str:
    name = candidate.display_name or candidate.username or "-"
    parts = [name]
    if candidate.display_name and candidate.username:
        parts.append(f"({candidate.username})")
    if candidate.email:
        parts.append(f"· {candidate.email}")
    return "  ".join(parts)
