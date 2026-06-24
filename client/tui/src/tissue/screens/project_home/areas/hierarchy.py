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
from tissue.widgets.text_button import TextButton

if TYPE_CHECKING:
    from tissue.api.generated.models.issue_common_detail import IssueCommonDetail
    from tissue.api.generated.models.issue_identifier_response import (
        IssueIdentifierResponse,
    )

log = logging.getLogger(__name__)

# IssueHierarchy ladder (mirrors the backend enum): a parent must sit exactly one
# level above its child. Level 1 is the top (EPIC), 4 the bottom (MICROTASK).
_LEVEL_BY_HIERARCHY = {"EPIC": 1, "STANDARD": 2, "SUBTASK": 3, "MICROTASK": 4}
_HIERARCHY_BY_LEVEL = {1: "EPIC", 2: "STANDARD", 3: "SUBTASK", 4: "MICROTASK"}

# How many project issues to scan for legal parent/child candidates. The picker
# filters this page by hierarchy client-side; a project with more matching issues
# than this won't surface them all (searchable within the page).
_CANDIDATE_LIMIT = 100

_PARENT_RM = "hub-hier-parent-rm"
_CHILD_RM_CLASS = "hub-hier-child-rm"


class HierarchyMixin(ProjectHomeBase):
    """The issue detail's parent/children hierarchy, below the reviewers.

    Parent renders as a single common-field-style row (label left, the parent key as
    a left-aligned link, then a '+' to set or '✕' to detach) — single-valued, so it
    reads like Status/Priority. Children render as a Reviewers-style section (a
    'Children' header with '+', then one link row each with a '✕'). A section is
    shown ONLY when this issue's hierarchy can hold that relation (EPIC has no parent
    row; MICROTASK has no children section) — no "top/bottom level" placeholder. The
    pickers offer only issues exactly one hierarchy level above/below; the backend
    re-validates. Detach is best-effort: SUBTASK/MICROTASK must keep a parent, so the
    server rejects orphaning them — surfaced as a notify (the user's chosen UX)."""

    def _hierarchy_section(
        self,
        d: IssueCommonDetail,
        parent: IssueIdentifierResponse | None,
        children: list[IssueIdentifierResponse],
    ) -> list[Widget]:
        level = _LEVEL_BY_HIERARCHY.get(self._detail_hierarchy or "")
        # `known` is False only when the type catalog didn't resolve this issue's
        # hierarchy (e.g. a transient list_issue_types failure) — then we can't tell
        # what it may hold, so we show a section only for a relation that already
        # exists (a fetched parent/child), never an empty/"can't have" one.
        known = level is not None
        can_have_parent = known and (level - 1) in _HIERARCHY_BY_LEVEL
        can_have_children = known and (level + 1) in _HIERARCHY_BY_LEVEL
        parent_set = parent is not None and bool(parent.issue_key)
        shown_children = [c for c in children if c.issue_key]

        widgets: list[Widget] = []
        # PARENT — a section like Children (bold header, the parent as a link row
        # below); omitted entirely when this type can't have a parent and none is set.
        if can_have_parent or parent_set:
            header: list[Widget] = [Static("Parent", classes="hub-hier-title")]
            if can_have_parent and not parent_set:
                header.append(
                    TextButton("+", id="hub-hier-parent-add", classes="hub-row-action")
                )
            widgets.append(Horizontal(*header, classes="hub-hier-header"))
            if parent_set and parent is not None:
                widgets.append(
                    Horizontal(
                        self._hier_link(parent),
                        TextButton(
                            "✕",
                            id=_PARENT_RM,
                            name=parent.issue_key,
                            classes="hub-row-action",
                        ),
                        classes="hub-hier-row",
                    )
                )
            else:
                widgets.append(Static("No parent.", classes="hub-muted"))

        # CHILDREN — the same section structure; omitted when this type can't have
        # children and none exist.
        if can_have_children or shown_children:
            header = [Static("Children", classes="hub-hier-title")]
            if can_have_children:
                header.append(
                    TextButton("+", id="hub-hier-child-add", classes="hub-row-action")
                )
            widgets.append(Horizontal(*header, classes="hub-hier-header"))
            if shown_children:
                for c in shown_children:
                    widgets.append(
                        Horizontal(
                            self._hier_link(c),
                            TextButton(
                                "✕",
                                name=c.issue_key,
                                classes=f"hub-row-action {_CHILD_RM_CLASS}",
                            ),
                            classes="hub-hier-row",
                        )
                    )
            else:
                widgets.append(Static("No children.", classes="hub-muted"))
        return widgets

    def _hier_link(self, ident: IssueIdentifierResponse) -> IssueLink:
        """The related issue's key (+ type label) as plain left-aligned clickable
        text (opens its detail modal on click)."""
        key = ident.issue_key or "-"
        label = key + (f"  {ident.issue_type_label}" if ident.issue_type_label else "")
        return IssueLink(key, label)

    @on(IssueLink.Clicked)
    def _on_hier_open(self, event: IssueLink.Clicked) -> None:
        event.stop()
        self._open_issue_modal(event.issue_key)

    @on(Button.Pressed, "#hub-hier-parent-add")
    def _on_parent_add(self, event: Button.Pressed) -> None:
        event.stop()
        if self._hierarchy_busy:
            return
        self.run_worker(self._open_parent_picker(), exclusive=True, group="hub-hier")

    @on(Button.Pressed, "#hub-hier-child-add")
    def _on_child_add(self, event: Button.Pressed) -> None:
        event.stop()
        if self._hierarchy_busy:
            return
        self.run_worker(self._open_children_picker(), exclusive=True, group="hub-hier")

    @on(Button.Pressed, "#hub-hier-parent-rm")
    def _on_parent_remove(self, event: Button.Pressed) -> None:
        event.stop()
        issue_key = self._detail_issue_key
        if self._hierarchy_busy or issue_key is None:
            return
        self._hierarchy_busy = True
        self.run_worker(
            self._detach_hierarchy(issue_key, issue_key),
            exclusive=True,
            group="hub-hier-mut",
        )

    @on(Button.Pressed, ".hub-hier-child-rm")
    def _on_child_remove(self, event: Button.Pressed) -> None:
        event.stop()
        child_key = event.button.name
        issue_key = self._detail_issue_key
        if self._hierarchy_busy or not child_key or issue_key is None:
            return
        self._hierarchy_busy = True
        self.run_worker(
            self._detach_hierarchy(child_key, issue_key),
            exclusive=True,
            group="hub-hier-mut",
        )

    async def _ensure_issue_type_hierarchy(self) -> None:
        """Load the issue-type catalog once (type id -> hierarchy) so candidates and
        the current issue's level can be resolved client-side (IssueSummary carries
        only the type id, not its hierarchy)."""
        if self._issue_type_hierarchy:
            return
        client = self.app.client
        if client is None:
            return
        try:
            types = await client.issues.list_issue_types()
        except TissueApiError as e:
            log.debug("Hub: failed to load issue type catalog: %s", e)
            return
        self._issue_type_hierarchy = {
            t.id: t.hierarchy for t in types if t.id is not None and t.hierarchy
        }

    async def _candidates(
        self, hierarchy: str, exclude: set[str]
    ) -> list[tuple[str, str]]:
        """`(label, key)` pairs for the project's issues of the given hierarchy,
        minus `exclude`. Best-effort; capped at `_CANDIDATE_LIMIT`."""
        client = self.app.client
        if client is None:
            return []
        await self._ensure_issue_type_hierarchy()
        try:
            page = await client.issues.search_project_issues(
                self._project_key, size=_CANDIDATE_LIMIT
            )
        except TissueApiError as e:
            log.debug("Hub: failed to load hierarchy candidates: %s", e)
            return []
        out: list[tuple[str, str]] = []
        for s in page.content or []:
            if s.issue_key is None or s.issue_key in exclude:
                continue
            if s.issue_type_id is None:
                continue
            if self._issue_type_hierarchy.get(s.issue_type_id) != hierarchy:
                continue
            label = s.issue_key + (f"  {s.title}" if s.title else "")
            out.append((label, s.issue_key))
        return out

    async def _open_parent_picker(self) -> None:
        from tissue.screens.project_home.issue_picker_modal import IssuePickerModal

        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        level = _LEVEL_BY_HIERARCHY.get(self._detail_hierarchy or "")
        parent_hier = _HIERARCHY_BY_LEVEL.get((level or 0) - 1)
        if parent_hier is None:
            self.app.notify(
                "This issue is top-level and can't have a parent.", severity="warning"
            )
            return
        candidates = await self._candidates(parent_hier, exclude={issue_key})
        if self._detail_issue_key != issue_key:
            return
        self._hier_picker_issue = issue_key
        self.app.push_screen(
            IssuePickerModal(
                candidates=candidates,
                multi=False,
                title="Set parent",
                subtitle=f"{parent_hier} issues · Esc to cancel",
            ),
            self._on_parent_picked,
        )

    async def _open_children_picker(self) -> None:
        from tissue.screens.project_home.issue_picker_modal import IssuePickerModal

        issue_key = self._detail_issue_key
        if issue_key is None:
            return
        level = _LEVEL_BY_HIERARCHY.get(self._detail_hierarchy or "")
        child_hier = _HIERARCHY_BY_LEVEL.get((level or 0) + 1)
        if child_hier is None:
            self.app.notify(
                "This issue is at the lowest level and can't have children.",
                severity="warning",
            )
            return
        candidates = await self._candidates(child_hier, exclude={issue_key})
        if self._detail_issue_key != issue_key:
            return
        # Exclude already-attached children using the FRESHEST list (read after the
        # candidate-search await), so a child detached during the search isn't
        # wrongly omitted and a child attached meanwhile isn't re-offered.
        existing = {c.issue_key for c in self._detail_children if c.issue_key}
        candidates = [(lbl, k) for lbl, k in candidates if k not in existing]
        self._hier_picker_issue = issue_key
        self.app.push_screen(
            IssuePickerModal(
                candidates=candidates,
                multi=True,
                title="Add children",
                subtitle=f"{child_hier} issues · Esc to cancel",
            ),
            self._on_children_picked,
        )

    def _on_parent_picked(self, picked: list[str] | None) -> None:
        key = picked[0] if picked else None
        issue_key = self._hier_picker_issue
        if key is None or issue_key is None or self._detail_issue_key != issue_key:
            return
        if self._hierarchy_busy:
            return
        self._hierarchy_busy = True
        self.run_worker(
            self._assign_parent(issue_key, key), exclusive=True, group="hub-hier-mut"
        )

    def _on_children_picked(self, picked: list[str] | None) -> None:
        issue_key = self._hier_picker_issue
        if not picked or issue_key is None or self._detail_issue_key != issue_key:
            return
        if self._hierarchy_busy:
            return
        self._hierarchy_busy = True
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
        except TissueApiError as e:
            log.debug("Hub: failed to set parent of %s: %s", issue_key, e)
            if self._detail_issue_key == issue_key:
                self.app.notify(
                    getattr(e, "detail", None) or "Couldn't set parent.",
                    severity="error",
                )
        finally:
            self._hierarchy_busy = False
        self._refresh_detail(issue_key)

    async def _apply_add_children(self, issue_key: str, child_keys: list[str]) -> None:
        client = self.app.client
        try:
            if client is not None:
                result = await client.issues.add_children(
                    self._project_key, issue_key, child_keys
                )
                failed = result.fail_count or 0
                if failed and self._detail_issue_key == issue_key:
                    self.app.notify(
                        f"{failed} of {len(child_keys)} couldn't be attached.",
                        severity="error",
                    )
        except TissueApiError as e:
            log.debug("Hub: failed to add children to %s: %s", issue_key, e)
            if self._detail_issue_key == issue_key:
                self.app.notify(
                    getattr(e, "detail", None) or "Couldn't add children.",
                    severity="error",
                )
        finally:
            self._hierarchy_busy = False
        self._refresh_detail(issue_key)

    async def _detach_hierarchy(self, target_key: str, refresh_key: str) -> None:
        """Clear `target_key`'s parent. For a child ✕ that's the child; for the
        parent ✕ it's this issue. SUBTASK/MICROTASK must keep a parent — the server
        returns PARENT_REQUIRED, surfaced as a notify."""
        client = self.app.client
        try:
            if client is not None:
                await client.issues.remove_parent(target_key)
        except TissueApiError as e:
            log.debug("Hub: failed to detach %s: %s", target_key, e)
            if self._detail_issue_key == refresh_key:
                self.app.notify(
                    getattr(e, "detail", None)
                    or "Couldn't detach — this issue type requires a parent.",
                    severity="error",
                )
        finally:
            self._hierarchy_busy = False
        self._refresh_detail(refresh_key)
