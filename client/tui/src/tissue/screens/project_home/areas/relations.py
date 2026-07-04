from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Button, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.widgets.issue_refs import _RELATION_ROWS, relation_rows
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.screens.project_home.modals.relation_add_modal import (
        RelationCandidate,
    )

log = logging.getLogger(__name__)

_CANDIDATE_LIMIT = 100

_REL_RM_CLASS = "hub-rel-rm"

# Maps a chosen (possibly reverse) type to the forward API enum plus whether
# source/target must be swapped, since the API has no `*_BY` relation types.
_RELATION_API: dict[str, tuple[str, bool]] = {
    "RELEVANT": ("RELEVANT", False),
    "BLOCKS": ("BLOCKS", False),
    "BLOCKED_BY": ("BLOCKS", True),
    "CAUSES": ("CAUSES", False),
    "CAUSED_BY": ("CAUSES", True),
    "DUPLICATES": ("DUPLICATES", False),
    "DUPLICATED_BY": ("DUPLICATES", True),
}


class RelationsMixin(ProjectHomeBase):
    """Relations section in issue detail."""

    def _relations_section(self, detail: IssueCommonDetail) -> list[Widget]:
        widgets: list[Widget] = [
            Horizontal(
                Static("Relations", classes="hub-hier-title"),
                TextButton("+", id="hub-rel-add", classes="hub-row-action"),
                classes="hub-hier-header",
            )
        ]
        widgets.extend(
            relation_rows(
                self._relation_state.relations, remove_button=self._rel_remove_button
            )
        )
        return widgets

    def _rel_remove_button(self, target_key: str) -> TextButton:
        return TextButton(
            "✕", name=target_key, classes=f"hub-row-action {_REL_RM_CLASS}"
        )

    def _related_keys(self) -> set[str]:
        relations = self._relation_state.relations
        if relations is None:
            return set()
        keys: set[str] = set()
        for attr, _arrow, _label, _removable in _RELATION_ROWS:
            for related_issue in getattr(relations, attr) or []:
                if related_issue.issue_key:
                    keys.add(related_issue.issue_key)
        return keys

    @on(Button.Pressed, "#hub-rel-add")
    def _on_rel_add(self, event: Button.Pressed) -> None:
        event.stop()
        if self._relation_state.busy:
            return
        self.run_worker(self._open_relation_modal(), exclusive=True, group="hub-rel")

    @on(Button.Pressed, ".hub-rel-rm")
    def _on_rel_remove(self, event: Button.Pressed) -> None:
        event.stop()
        target_key = event.button.name
        issue_key = self._detail_state.issue_key
        if self._relation_state.busy or not target_key or issue_key is None:
            return
        self._relation_state.busy = True
        self.run_worker(
            self._remove_relation(issue_key, target_key),
            exclusive=True,
            group="hub-rel-mut",
        )

    async def _relation_candidates(self, issue_key: str) -> list[RelationCandidate]:
        from tissue.screens.project_home.modals.relation_add_modal import (
            RelationCandidate,
        )

        client = self.app.client
        if client is None:
            return []
        try:
            page = await client.issues.search_project_issues(
                self._project_key,
                state_categories=["INITIAL", "ACTIVE", "COMPLETED"],
                size=_CANDIDATE_LIMIT,
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load relation candidates: %s", error)
            return []
        exclude = self._related_keys() | {issue_key}
        candidates: list[RelationCandidate] = []
        for summary in page.content or []:
            key = summary.issue_key
            if not key or key in exclude:
                continue
            if summary.current_state_category == "ABORTED":
                continue
            candidates.append(
                RelationCandidate(
                    key=key,
                    title=summary.title,
                    type_name=summary.issue_type_name,
                    type_color=summary.issue_type_color,
                    status_label=summary.current_state_label,
                    priority=summary.priority,
                    completed=summary.current_state_category == "COMPLETED",
                )
            )
        return candidates

    async def _open_relation_modal(self) -> None:
        from tissue.screens.project_home.modals.relation_add_modal import (
            RelationAddModal,
        )

        issue_key = self._detail_state.issue_key
        if issue_key is None:
            return
        candidates = await self._relation_candidates(issue_key)
        if self._detail_state.issue_key != issue_key:
            return
        self._relation_state.picker_issue_key = issue_key
        self.app.push_screen(
            RelationAddModal(candidates=candidates), self._on_relation_picked
        )

    def _on_relation_picked(self, result: tuple[str, str] | None) -> None:
        issue_key = self._relation_state.picker_issue_key
        if (
            result is None
            or issue_key is None
            or self._detail_state.issue_key != issue_key
        ):
            return
        if self._relation_state.busy:
            return
        relation_type, target_key = result
        self._relation_state.busy = True
        self.run_worker(
            self._add_relation(issue_key, target_key, relation_type),
            exclusive=True,
            group="hub-rel-mut",
        )

    async def _add_relation(
        self, issue_key: str, target_key: str, relation_type: str
    ) -> None:
        client = self.app.client
        forward_type, invert = _RELATION_API.get(relation_type, (relation_type, False))
        source_key, dest_key = (
            (target_key, issue_key) if invert else (issue_key, target_key)
        )
        dest_project = dest_key.rsplit("-", 1)[0]
        try:
            if client is not None:
                await client.issues.add_issue_relation(
                    source_key, dest_project, dest_key, forward_type
                )
        except TissueApiError as error:
            log.debug(
                "Hub: failed to add relation %s->%s: %s", source_key, dest_key, error
            )
            if self._detail_state.issue_key == issue_key:
                self.app.notify(
                    getattr(error, "detail", None) or "Couldn't add relation.",
                    severity="error",
                )
        finally:
            self._relation_state.busy = False
        self._refresh_detail(issue_key)

    async def _remove_relation(self, issue_key: str, target_key: str) -> None:
        client = self.app.client
        target_project = target_key.rsplit("-", 1)[0]
        try:
            if client is not None:
                await client.issues.remove_issue_relation(
                    issue_key, target_project, target_key
                )
        except TissueApiError as error:
            log.debug(
                "Hub: failed to remove relation %s->%s: %s",
                issue_key,
                target_key,
                error,
            )
            if self._detail_state.issue_key == issue_key:
                self.app.notify(
                    getattr(error, "detail", None) or "Couldn't remove relation.",
                    severity="error",
                )
        finally:
            self._relation_state.busy = False
        self._refresh_detail(issue_key)
