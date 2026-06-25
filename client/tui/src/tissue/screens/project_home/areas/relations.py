from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Button, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.widgets.issue_render import _RELATION_ROWS, relation_rows
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail

log = logging.getLogger(__name__)

# How many project issues to scan as relation candidates (searchable within page).
_CANDIDATE_LIMIT = 100

_REL_RM_CLASS = "hub-rel-rm"


class RelationsMixin(ProjectHomeBase):
    """The issue detail's relations section, below the hierarchy.

    Always shown (any issue can relate to another): a 'Relations' header with '+', then
    one row per relation — a direction arrow + verb (→ Blocks, ← Blocked by, ↔ Relevant)
    + the related issue. Outgoing relations (blocks/causes/duplicates) and the symmetric
    relevant carry a '✕' to remove from this side; the incoming inverses are read-only
    (owned by the other issue). '+' opens a modal to pick a type + a same-project target
    (the backend allows cross-project too, but the picker stays same-project for now).
    Add/remove are best-effort — failures surface as a notify."""

    def _relations_section(self, d: IssueCommonDetail) -> list[Widget]:
        widgets: list[Widget] = [
            Horizontal(
                Static("Relations", classes="hub-hier-title"),
                TextButton("+", id="hub-rel-add", classes="hub-row-action"),
                classes="hub-hier-header",
            )
        ]
        rows = relation_rows(
            self._detail_relations, remove_button=self._rel_remove_button
        )
        if rows:
            widgets.extend(rows)
        else:
            widgets.append(Static("No relations.", classes="hub-muted"))
        return widgets

    def _rel_remove_button(self, target_key: str) -> TextButton:
        """A ✕ that removes the relation between this issue and `target_key`."""
        return TextButton(
            "✕", name=target_key, classes=f"hub-row-action {_REL_RM_CLASS}"
        )

    def _related_keys(self) -> set[str]:
        rels = self._detail_relations
        if rels is None:
            return set()
        keys: set[str] = set()
        for attr, _arrow, _label, _removable in _RELATION_ROWS:
            for it in getattr(rels, attr) or []:
                if it.issue_key:
                    keys.add(it.issue_key)
        return keys

    @on(Button.Pressed, "#hub-rel-add")
    def _on_rel_add(self, event: Button.Pressed) -> None:
        event.stop()
        if self._relations_busy:
            return
        self.run_worker(self._open_relation_modal(), exclusive=True, group="hub-rel")

    @on(Button.Pressed, ".hub-rel-rm")
    def _on_rel_remove(self, event: Button.Pressed) -> None:
        event.stop()
        target_key = event.button.name
        issue_key = self._detail_issue_key
        if self._relations_busy or not target_key or issue_key is None:
            return
        self._relations_busy = True
        self.run_worker(
            self._remove_relation(issue_key, target_key),
            exclusive=True,
            group="hub-rel-mut",
        )

    async def _relation_candidates(self, issue_key: str) -> list[tuple[str, str]]:
        """`(label, key)` pairs for this project's issues, minus the issue itself and
        any already-related issue (one relation per pair). Best-effort, capped."""
        client = self.app.client
        if client is None:
            return []
        try:
            page = await client.issues.search_project_issues(
                self._project_key, size=_CANDIDATE_LIMIT
            )
        except TissueApiError as e:
            log.debug("Hub: failed to load relation candidates: %s", e)
            return []
        exclude = self._related_keys() | {issue_key}
        out: list[tuple[str, str]] = []
        for s in page.content or []:
            if not s.issue_key or s.issue_key in exclude:
                continue
            label = s.issue_key + (f"  {s.title}" if s.title else "")
            out.append((label, s.issue_key))
        return out

    async def _open_relation_modal(self) -> None:
        from tissue.screens.project_home.relation_add_modal import RelationAddModal

        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        candidates = await self._relation_candidates(issue_key)
        if self._detail_issue_key != issue_key:
            return
        self._rel_picker_issue = issue_key
        self.app.push_screen(
            RelationAddModal(candidates=candidates), self._on_relation_picked
        )

    def _on_relation_picked(self, result: tuple[str, str] | None) -> None:
        issue_key = self._rel_picker_issue
        if result is None or issue_key is None or self._detail_issue_key != issue_key:
            return
        if self._relations_busy:
            return
        relation_type, target_key = result
        self._relations_busy = True
        self.run_worker(
            self._add_relation(issue_key, target_key, relation_type),
            exclusive=True,
            group="hub-rel-mut",
        )

    async def _add_relation(
        self, issue_key: str, target_key: str, relation_type: str
    ) -> None:
        client = self.app.client
        target_project = target_key.rsplit("-", 1)[0]
        try:
            if client is not None:
                await client.issues.add_issue_relation(
                    issue_key, target_project, target_key, relation_type
                )
        except TissueApiError as e:
            log.debug(
                "Hub: failed to add relation %s->%s: %s", issue_key, target_key, e
            )
            if self._detail_issue_key == issue_key:
                self.app.notify(
                    getattr(e, "detail", None) or "Couldn't add relation.",
                    severity="error",
                )
        finally:
            self._relations_busy = False
        self._refresh_detail(issue_key)

    async def _remove_relation(self, issue_key: str, target_key: str) -> None:
        client = self.app.client
        target_project = target_key.rsplit("-", 1)[0]
        try:
            if client is not None:
                await client.issues.remove_issue_relation(
                    issue_key, target_project, target_key
                )
        except TissueApiError as e:
            log.debug(
                "Hub: failed to remove relation %s->%s: %s", issue_key, target_key, e
            )
            if self._detail_issue_key == issue_key:
                self.app.notify(
                    getattr(e, "detail", None) or "Couldn't remove relation.",
                    severity="error",
                )
        finally:
            self._relations_busy = False
        self._refresh_detail(issue_key)
