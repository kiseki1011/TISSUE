from __future__ import annotations

import logging
from typing import TYPE_CHECKING

from textual import on
from textual.containers import Horizontal
from textual.widget import Widget
from textual.widgets import Button, Static

from tissue.api.errors import TissueApiError
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.widgets.issue_link import IssueLink
from tissue.widgets.issue_refs import issue_ref_row
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )

log = logging.getLogger(__name__)

_LEVEL_BY_HIERARCHY = {"EPIC": 1, "STANDARD": 2, "SUBTASK": 3, "MICROTASK": 4}
_HIERARCHY_BY_LEVEL = {1: "EPIC", 2: "STANDARD", 3: "SUBTASK", 4: "MICROTASK"}

_CANDIDATE_LIMIT = 100

_PARENT_RM = "hub-hier-parent-rm"
_CHILD_RM_CLASS = "hub-hier-child-rm"


class HierarchyMixin(ProjectHomeBase):
    """Parent/children section in issue detail."""

    def _hierarchy_section(
        self,
        detail: IssueCommonDetail,
        parent: IssueIdentifierResponse | None,
        children: list[IssueIdentifierResponse],
    ) -> list[Widget]:
        level = _LEVEL_BY_HIERARCHY.get(self._hierarchy_state.hierarchy or "")
        known = level is not None
        can_have_parent = known and (level - 1) in _HIERARCHY_BY_LEVEL
        can_have_children = known and (level + 1) in _HIERARCHY_BY_LEVEL
        parent_set = parent is not None and bool(parent.issue_key)
        shown_children = [child for child in children if child.issue_key]

        widgets: list[Widget] = []
        if can_have_parent or parent_set:
            header: list[Widget] = [Static("Parent", classes="hub-hier-title")]
            if can_have_parent and not parent_set:
                header.append(
                    TextButton("+", id="hub-hier-parent-add", classes="hub-row-action")
                )
            widgets.append(Horizontal(*header, classes="hub-hier-header"))
            if parent_set and parent is not None:
                widgets.append(
                    issue_ref_row(
                        parent,
                        remove_button=TextButton(
                            "✕",
                            id=_PARENT_RM,
                            name=parent.issue_key,
                            classes="hub-row-action",
                        ),
                    )
                )

        if can_have_children or shown_children:
            header = [Static("Children", classes="hub-hier-title")]
            if can_have_children:
                header.append(
                    TextButton("+", id="hub-hier-child-add", classes="hub-row-action")
                )
            widgets.append(Horizontal(*header, classes="hub-hier-header"))
            if shown_children:
                for child in shown_children:
                    widgets.append(
                        issue_ref_row(
                            child,
                            remove_button=TextButton(
                                "✕",
                                name=child.issue_key,
                                classes=f"hub-row-action {_CHILD_RM_CLASS}",
                            ),
                        )
                    )
        return widgets

    @on(IssueLink.Clicked)
    def _on_hier_open(self, event: IssueLink.Clicked) -> None:
        event.stop()
        self._open_issue_modal(event.issue_key)

    @on(Button.Pressed, "#hub-hier-parent-add")
    def _on_parent_add(self, event: Button.Pressed) -> None:
        event.stop()
        if self._hierarchy_state.busy:
            return
        self.run_worker(self._open_parent_picker(), exclusive=True, group="hub-hier")

    @on(Button.Pressed, "#hub-hier-child-add")
    def _on_child_add(self, event: Button.Pressed) -> None:
        event.stop()
        if self._hierarchy_state.busy:
            return
        self.run_worker(self._open_children_picker(), exclusive=True, group="hub-hier")

    @on(Button.Pressed, "#hub-hier-parent-rm")
    def _on_parent_remove(self, event: Button.Pressed) -> None:
        event.stop()
        issue_key = self._detail_state.issue_key
        if self._hierarchy_state.busy or issue_key is None:
            return
        self._hierarchy_state.busy = True
        self.run_worker(
            self._detach_hierarchy(issue_key, issue_key),
            exclusive=True,
            group="hub-hier-mut",
        )

    @on(Button.Pressed, ".hub-hier-child-rm")
    def _on_child_remove(self, event: Button.Pressed) -> None:
        event.stop()
        child_key = event.button.name
        issue_key = self._detail_state.issue_key
        if self._hierarchy_state.busy or not child_key or issue_key is None:
            return
        self._hierarchy_state.busy = True
        self.run_worker(
            self._detach_hierarchy(child_key, issue_key),
            exclusive=True,
            group="hub-hier-mut",
        )

    async def _ensure_issue_type_hierarchy(self) -> None:
        if self._hierarchy_state.issue_type_hierarchy:
            return
        client = self.app.client
        if client is None:
            return
        try:
            types = await client.issues.list_issue_types()
        except TissueApiError as error:
            log.debug("Hub: failed to load issue type catalog: %s", error)
            return
        self._hierarchy_state.issue_type_hierarchy = {
            issue_type.id: issue_type.hierarchy
            for issue_type in types
            if issue_type.id is not None and issue_type.hierarchy
        }

    async def _candidates(
        self, hierarchy: str, exclude: set[str]
    ) -> list[tuple[str, str]]:
        client = self.app.client
        if client is None:
            return []
        await self._ensure_issue_type_hierarchy()
        try:
            page = await client.issues.search_project_issues(
                self._project_key, size=_CANDIDATE_LIMIT
            )
        except TissueApiError as error:
            log.debug("Hub: failed to load hierarchy candidates: %s", error)
            return []
        candidates: list[tuple[str, str]] = []
        for summary in page.content or []:
            if summary.issue_key is None or summary.issue_key in exclude:
                continue
            if summary.issue_type_id is None:
                continue
            if (
                self._hierarchy_state.issue_type_hierarchy.get(summary.issue_type_id)
                != hierarchy
            ):
                continue
            candidates.append(
                (
                    self._candidate_label(summary.issue_key, summary.title),
                    summary.issue_key,
                )
            )
        return candidates

    @staticmethod
    def _candidate_label(issue_key: str, title: str | None) -> str:
        return issue_key + (f"  {title}" if title else "")

    async def _open_parent_picker(self) -> None:
        issue_key = self._detail_state.issue_key
        if issue_key is None:
            return
        parent_hier = self._relative_hierarchy(-1)
        if parent_hier is None:
            self.app.notify(
                "This issue is top-level and can't have a parent.", severity="warning"
            )
            return
        candidates = await self._candidates(parent_hier, exclude={issue_key})
        if self._detail_state.issue_key != issue_key:
            return
        self._hierarchy_state.picker_issue_key = issue_key
        self._push_hierarchy_picker(
            candidates=candidates,
            multi=False,
            title="Set parent",
            subtitle=f"{parent_hier} issues · Esc to cancel",
            callback=self._on_parent_picked,
        )

    async def _open_children_picker(self) -> None:
        issue_key = self._detail_state.issue_key
        if issue_key is None:
            return
        child_hier = self._relative_hierarchy(1)
        if child_hier is None:
            self.app.notify(
                "This issue is at the lowest level and can't have children.",
                severity="warning",
            )
            return
        candidates = await self._candidates(child_hier, exclude={issue_key})
        if self._detail_state.issue_key != issue_key:
            return
        self._hierarchy_state.picker_issue_key = issue_key
        self._push_hierarchy_picker(
            candidates=self._without_existing_children(candidates),
            multi=True,
            title="Add children",
            subtitle=f"{child_hier} issues · Esc to cancel",
            callback=self._on_children_picked,
        )

    def _relative_hierarchy(self, offset: int) -> str | None:
        level = _LEVEL_BY_HIERARCHY.get(self._hierarchy_state.hierarchy or "")
        return _HIERARCHY_BY_LEVEL.get((level or 0) + offset)

    def _without_existing_children(
        self, candidates: list[tuple[str, str]]
    ) -> list[tuple[str, str]]:
        existing = {
            child.issue_key
            for child in self._hierarchy_state.children
            if child.issue_key
        }
        return [(label, key) for label, key in candidates if key not in existing]

    def _push_hierarchy_picker(
        self,
        *,
        candidates: list[tuple[str, str]],
        multi: bool,
        title: str,
        subtitle: str,
        callback,
    ) -> None:
        from tissue.screens.project_home.modals.issue_picker_modal import (
            IssuePickerModal,
        )

        self.app.push_screen(
            IssuePickerModal(
                candidates=candidates,
                multi=multi,
                title=title,
                subtitle=subtitle,
            ),
            callback,
        )

    def _on_parent_picked(self, picked: list[str] | None) -> None:
        parent_key = picked[0] if picked else None
        issue_key = self._hierarchy_state.picker_issue_key
        if (
            parent_key is None
            or issue_key is None
            or self._detail_state.issue_key != issue_key
        ):
            return
        if self._hierarchy_state.busy:
            return
        self._hierarchy_state.busy = True
        self.run_worker(
            self._assign_parent(issue_key, parent_key),
            exclusive=True,
            group="hub-hier-mut",
        )

    def _on_children_picked(self, picked: list[str] | None) -> None:
        issue_key = self._hierarchy_state.picker_issue_key
        if not picked or issue_key is None or self._detail_state.issue_key != issue_key:
            return
        if self._hierarchy_state.busy:
            return
        self._hierarchy_state.busy = True
        self.run_worker(
            self._apply_add_children(issue_key, picked),
            exclusive=True,
            group="hub-hier-mut",
        )

    async def _assign_parent(self, issue_key: str, parent_key: str) -> None:
        client = self.app.client
        try:
            if client is not None:
                await client.issues.assign_parent(issue_key, parent_key)
        except TissueApiError as error:
            log.debug("Hub: failed to set parent of %s: %s", issue_key, error)
            if self._detail_state.issue_key == issue_key:
                self.app.notify(
                    getattr(error, "detail", None) or "Couldn't set parent.",
                    severity="error",
                )
        finally:
            self._hierarchy_state.busy = False
        self._refresh_detail(issue_key)

    async def _apply_add_children(self, issue_key: str, child_keys: list[str]) -> None:
        client = self.app.client
        try:
            if client is not None:
                result = await client.issues.add_children(
                    self._project_key, issue_key, child_keys
                )
                failed = result.fail_count or 0
                if failed and self._detail_state.issue_key == issue_key:
                    self.app.notify(
                        f"{failed} of {len(child_keys)} couldn't be attached.",
                        severity="error",
                    )
        except TissueApiError as error:
            log.debug("Hub: failed to add children to %s: %s", issue_key, error)
            if self._detail_state.issue_key == issue_key:
                self.app.notify(
                    getattr(error, "detail", None) or "Couldn't add children.",
                    severity="error",
                )
        finally:
            self._hierarchy_state.busy = False
        self._refresh_detail(issue_key)

    async def _detach_hierarchy(self, target_key: str, refresh_key: str) -> None:
        client = self.app.client
        try:
            if client is not None:
                await client.issues.remove_parent(target_key)
        except TissueApiError as error:
            log.debug("Hub: failed to detach %s: %s", target_key, error)
            if self._detail_state.issue_key == refresh_key:
                self.app.notify(
                    getattr(error, "detail", None)
                    or "Couldn't detach — this issue type requires a parent.",
                    severity="error",
                )
        finally:
            self._hierarchy_state.busy = False
        self._refresh_detail(refresh_key)
