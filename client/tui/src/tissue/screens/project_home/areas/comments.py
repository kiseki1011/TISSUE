from __future__ import annotations

import logging
from datetime import UTC, datetime
from typing import TYPE_CHECKING

from rich.text import Text
from textual import events, on
from textual.containers import Horizontal, Vertical
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Input, Markdown, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.comment_author_info import CommentAuthorInfo
from tissue.api.generated.models.comment_detail_response import CommentDetailResponse
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.util.datetime_fmt import format_relative

if TYPE_CHECKING:
    from tissue.api.generated.models.comment_create_response import (
        CommentCreateResponse,
    )

log = logging.getLogger(__name__)


class CommentsMixin(ProjectHomeBase):
    """The detail's comments: render the section + the (nested) comment list, and
    add a comment via the input (Enter submits)."""

    def on_descendant_focus(self, event: events.DescendantFocus) -> None:
        # When the comment input gains focus, scroll it into view — on a long
        # thread it sits below the fold, so focusing it brings it (and the newest
        # comments above it) into view with a minimal scroll.
        if event.widget.id == "hub-comment-input":
            event.widget.scroll_visible(animate=False)

    def _comment_section(self, comments: list[CommentDetailResponse]) -> list[Widget]:
        """Comments header, the (already-loaded) thread, and the add form. Rendered
        with the rest of the detail in one paint — `_render_issue_detail` fetches
        the comments first so there's no async "Loading…" → thread swap."""
        if comments:
            loaded: list[Widget] = []
            for comment in comments:
                loaded.extend(self._comment_widgets(comment, depth=0))
        else:
            loaded = [Static("No comments yet.", classes="hub-muted")]
        return [
            Rule(),
            Static("Comments", classes="hub-section-title"),
            Vertical(*loaded, id="hub-comments", classes="hub-comments"),
            Horizontal(
                Input(
                    placeholder="Add a comment with enter…",
                    id="hub-comment-input",
                    classes="hub-comment-input",
                ),
                classes="hub-comment-form",
            ),
        ]

    def _comment_widgets(self, c: CommentDetailResponse, depth: int) -> list[Widget]:
        """A comment as a bold meta line (author · when) + its body, recursing into
        nested replies (indented). The body renders as Markdown; a deleted body
        shows a plain italic placeholder."""
        author = (c.author.display_name or c.author.username) if c.author else None
        meta = " · ".join([author or "?", format_relative(c.created_at)])
        if c.is_edited:
            meta += " (edited)"
        indent = " hub-comment-indent" if depth else ""
        body: Widget
        if c.is_deleted:
            body = Static(
                Text("[deleted]", style="italic"),
                classes=f"hub-comment-body{indent}",
            )
        else:
            body = Markdown(
                (c.content or "").strip(), classes=f"hub-comment-body{indent}"
            )
        out: list[Widget] = [
            Static(meta, markup=False, classes=f"hub-comment-meta{indent}"),
            body,
        ]
        for reply in c.replies or []:
            out.extend(self._comment_widgets(reply, depth + 1))
        return out

    @on(Input.Submitted, "#hub-comment-input")
    def _on_comment_input_submitted(self, event: Input.Submitted) -> None:
        self._submit_comment(event.value)

    def _submit_comment(self, text: str) -> None:
        issue_key = self._detail_issue_key
        text = text.strip()
        if issue_key is None or not text:
            return
        self.run_worker(
            self._post_comment(issue_key, text),
            exclusive=True,
            group="hub-comment-post",
        )

    async def _post_comment(self, issue_key: str, text: str) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            response = await client.comments.create_comment(issue_key, text)
        except TissueApiError as e:
            log.debug("Hub: failed to add comment to %s: %s", issue_key, e)
            self.app.notify("Failed to add comment.", severity="error")
            return
        if self._detail_issue_key != issue_key:
            return
        try:
            self.query_one("#hub-comment-input", Input).value = ""
        except NoMatches:
            pass
        # Append the new comment instead of reloading the whole thread: a full
        # reload clears #hub-comments, which resets the scroll to the top and then
        # needs a forced scroll-to-bottom — the jump that reads as a flicker.
        # Appending leaves the existing comments (and the scroll) in place; a
        # minimal scroll_visible keeps the input on screen.
        await self._append_comment(response, text)

    async def _append_comment(self, response: CommentCreateResponse, text: str) -> None:
        try:
            box = self.query_one("#hub-comments")
        except NoMatches:
            return
        # Drop the "No comments yet." / "Loading…" placeholder on the first comment.
        await box.query(".hub-muted").remove()
        comment = self._optimistic_comment(response, text)
        await box.mount(*self._comment_widgets(comment, depth=0))
        try:
            self.query_one("#hub-comment-input", Input).scroll_visible(animate=False)
        except NoMatches:
            pass

    def _optimistic_comment(
        self, response: CommentCreateResponse, text: str
    ) -> CommentDetailResponse:
        """Build a local stand-in for the just-posted comment so it can be appended
        without re-fetching the thread. Author comes from the cached profile (the
        current member); the server-canonical version lands on the next reload."""
        client = self.app.client
        profile = client.account.cached_profile if client else None
        author = (
            CommentAuthorInfo(displayName=profile.name, username=profile.username)
            if profile
            else None
        )
        return CommentDetailResponse(
            commentId=response.comment_id,
            content=text,
            author=author,
            createdAt=datetime.now(UTC),
            isEdited=False,
            isDeleted=False,
            replies=[],
        )
