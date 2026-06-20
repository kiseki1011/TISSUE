from __future__ import annotations

import logging

from rich.markdown import Markdown as RichMarkdown
from textual.containers import Vertical, VerticalScroll
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Rule, Static

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.issue_summary import IssueSummary
from tissue.api.generated.models.project_summary import ProjectSummary
from tissue.api.generated.models.wiki_document_search_result import (
    WikiDocumentSearchResult,
)
from tissue.screens.home._base import HomeScreenBase
from tissue.screens.home.rendering import (
    _key_detail_row,
    _visibility_label,
)
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.detail_row import detail_row

log = logging.getLogger(__name__)


class DetailsMixin(HomeScreenBase):
    """The Details panel: render a selected project / issue / wiki doc into it."""

    def _box(self, title: str, box_id: str, children: list[Widget]) -> Vertical:
        box = Vertical(*children, id=box_id, classes="dashboard-box panel")
        box.border_title = title
        return box

    def _detail_box(self) -> VerticalScroll:
        inner = Vertical(
            Static("Select an item to see details.", classes="dashboard-muted"),
            id="dashboard-detail-inner",
        )
        box = VerticalScroll(inner, id="dashboard-detail", classes="dashboard-box")
        box.border_title = "Details"
        box.can_focus = False  # not a focus/nav target
        return box

    def _render_project_detail(
        self, p: ProjectSummary, *, show_open_hint: bool = False
    ) -> None:
        widgets: list[Widget] = [
            Static(p.title or "-", markup=False, classes="dashboard-detail-title"),
            _key_detail_row(p.key or "-"),
            detail_row("Visibility", _visibility_label(p.visibility)),
            detail_row("Created", format_relative(p.created_at)),
            detail_row("Updated", format_relative(p.last_updated_at)),
            detail_row("Archived", "Yes" if p.archived else "No"),
            Static(
                p.description or "No description.",
                markup=False,
                classes="dashboard-detail-desc",
            ),
        ]

        if show_open_hint:
            widgets.append(
                Static("Press Enter to open", classes="dashboard-detail-hint")
            )
        self._mount_detail(widgets)

    def _render_issue_detail(self, i: IssueSummary) -> None:
        story = "-" if i.story_point is None else str(i.story_point)
        self._mount_detail(
            [
                Static(i.title or "-", markup=False, classes="dashboard-detail-title"),
                _key_detail_row(i.issue_key or "-"),
                detail_row("Status", i.current_state_label or "-"),
                detail_row("Category", i.current_state_category or "-"),
                detail_row("Priority", i.priority or "-"),
                detail_row("Story pts", story),
                detail_row("Due", format_relative(i.due_at)),
            ]
        )

    def _wiki_meta(self, d: WikiDocumentSearchResult) -> list[Widget]:
        return [
            Static(d.title or "-", markup=False, classes="dashboard-detail-title"),
            detail_row("Version", d.current_version or "-"),
            detail_row("Locked", "🔒" if d.locked else "-"),
            detail_row("Modified", format_relative(d.last_modified_at)),
            detail_row("Created", format_relative(d.created_at)),
        ]

    async def _render_wiki_detail(self, d: WikiDocumentSearchResult) -> None:
        client = self.app.client
        if client is None or d.id is None:
            self._mount_detail(self._wiki_meta(d))
            return
        try:
            doc = await client.wiki.get_document(d.id)
        except TissueApiError as e:
            log.debug("Dashboard: failed to load wiki content: %s", e)
            self._mount_detail(
                [
                    *self._wiki_meta(d),
                    Static("Couldn't load content.", classes="dashboard-muted"),
                ]
            )
            return
        content = (doc.content or "").strip()
        body: Widget = (
            Static(RichMarkdown(content), classes="dashboard-markdown")
            if content
            else Static("No content.", classes="dashboard-muted")
        )
        self._mount_detail([*self._wiki_meta(d), Rule(), body])

    def _mount_detail(self, widgets: list[Widget]) -> None:
        try:
            inner = self.query_one("#dashboard-detail-inner")
        except NoMatches:
            return
        inner.remove_children()
        inner.mount(*widgets)
