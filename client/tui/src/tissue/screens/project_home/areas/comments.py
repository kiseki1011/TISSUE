from __future__ import annotations

import logging
import re
from datetime import UTC, datetime
from typing import TYPE_CHECKING

from rich.text import Text
from textual import events, on
from textual.containers import Horizontal, Vertical
from textual.css.query import NoMatches
from textual.widget import Widget
from textual.widgets import Button, Input, Markdown, Rule, Static

from tissue.api.errors import TissueApiError
from tissue.api.generated.models.comment_author_info import CommentAuthorInfo
from tissue.api.generated.models.comment_detail_response import CommentDetailResponse
from tissue.screens.project_home._base import ProjectHomeBase
from tissue.util.datetime_fmt import format_relative
from tissue.widgets.mention_autocomplete import MentionAutoComplete
from tissue.widgets.text_button import TextButton

_REPLY_PREFIX = "hub-comment-reply-"
# '@' must begin a word so an email or "user@host" never reads as a mention.
_MENTION_RE = re.compile(r"(?<![^\s])@([A-Za-z0-9_.\-]+)")

if TYPE_CHECKING:
    from tissue.api.generated.models.comment_create_response import (
        CommentCreateResponse,
    )

log = logging.getLogger(__name__)


class CommentsMixin(ProjectHomeBase):
    """The detail's comment thread, a nested list plus an Enter-to-submit input."""

    def on_descendant_focus(self, event: events.DescendantFocus) -> None:
        if event.widget.id == "hub-comment-input":
            event.widget.scroll_visible(animate=False)

    def on_key(self, event: events.Key) -> None:
        # While composing a reply, Esc backs out of reply mode rather than firing
        # the screen's default Esc (leave search). Scoped so Esc is untouched
        # when no reply is active.
        if (
            event.key == "escape"
            and self._reply_to is not None
            and self.app.focused is not None
            and self.app.focused.id == "hub-comment-input"
        ):
            self._cancel_reply(refocus=True)
            event.stop()

    def _comment_section(self, comments: list[CommentDetailResponse]) -> list[Widget]:
        """Header, thread, and add form in one paint, avoiding a "Loading…" swap."""
        # Bump the count so a still-running post worker that kept the old number
        # skips adding its comment into this freshly rebuilt thread.
        self._reply_to = None
        self._reply_targets = {}
        self._comment_gen += 1
        if comments:
            loaded: list[Widget] = []
            for comment in comments:
                loaded.extend(self._comment_widgets(comment))
        else:
            loaded = [Static("No comments yet.", classes="hub-muted")]
        # Built once so MentionAutoComplete can target this exact Input instance.
        comment_input = Input(
            placeholder="Add a comment with enter…",
            id="hub-comment-input",
            classes="hub-comment-input",
        )
        return [
            Rule(),
            Static("Comments", classes="hub-section-title"),
            Vertical(*loaded, id="hub-comments", classes="hub-comments"),
            # Wrapper so the "Replying to @…" banner can mount above the input.
            Vertical(
                Horizontal(comment_input, classes="hub-comment-form"),
                MentionAutoComplete(comment_input, members=lambda: self._members),
                id="hub-comment-compose",
            ),
        ]

    def _comment_widgets(self, comment: CommentDetailResponse) -> list[Widget]:
        """A root comment plus its replies as indented blocks, one level deep."""
        head, body = self._head_body(comment, is_root=True)
        out: list[Widget] = [head, body]
        for reply in comment.replies or []:
            out.append(self._reply_block(reply))
        return out

    def _reply_block(self, comment: CommentDetailResponse) -> Vertical:
        """A reply's head and body, with a left border line to show it's nested."""
        head, body = self._head_body(comment, is_root=False)
        return Vertical(head, body, classes="hub-reply-block")

    def _head_body(
        self, comment: CommentDetailResponse, *, is_root: bool
    ) -> tuple[Widget, Widget]:
        """The meta line and body for one comment.

        A live root also gets a ↳ Reply action and a tagged body. A reply we
        show right away (before the server confirms) is added under it.
        """
        author = (
            (comment.author.display_name or comment.author.username)
            if comment.author
            else None
        )
        author = author or "?"
        username = comment.author.username if comment.author else None
        # Add "(@username)" to tell apart members with the same name, unless the
        # display name already is the username.
        shown = f"{author} (@{username})" if username and username != author else author
        meta = " · ".join([shown, format_relative(comment.created_at)])
        if comment.is_edited:
            meta += " (edited)"
        body: Widget
        if comment.is_deleted:
            body = Static(Text("[deleted]", style="italic"), classes="hub-comment-body")
        else:
            body = Markdown((comment.content or "").strip(), classes="hub-comment-body")
        head: Widget = Static(meta, markup=False, classes="hub-comment-meta")
        comment_id = comment.comment_id
        if is_root and comment_id is not None:
            # Anchor for replies we show right away, added right after this body.
            body.id = f"hub-comment-body-{comment_id}"
            if not comment.is_deleted:
                self._reply_targets[comment_id] = author
                reply = TextButton(
                    "↳",
                    id=f"{_REPLY_PREFIX}{comment_id}",
                    classes="hub-row-action hub-comment-reply",
                )
                reply.tooltip = "Reply"
                head = Horizontal(
                    Static(meta, markup=False, classes="hub-comment-meta"),
                    reply,
                    classes="hub-comment-head",
                )
        return head, body

    @on(Button.Pressed, ".hub-comment-reply")
    def _on_reply_pressed(self, event: Button.Pressed) -> None:
        event.stop()
        button_id = event.button.id or ""
        if not button_id.startswith(_REPLY_PREFIX):
            return
        try:
            self._begin_reply(int(button_id[len(_REPLY_PREFIX) :]))
        except ValueError:
            pass

    @on(Button.Pressed, "#hub-reply-cancel")
    def _on_reply_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self._cancel_reply(refocus=True)

    def _begin_reply(self, comment_id: int) -> None:
        """Aim the input at `comment_id`, showing the banner and taking focus."""
        self._reply_to = comment_id
        author = self._reply_targets.get(comment_id, "?")
        label = Text.assemble(("Replying to ", ""), (f"@{author}", "bold"))
        try:
            self.query_one("#hub-reply-target", Static).update(label)
        except NoMatches:
            try:
                compose = self.query_one("#hub-comment-compose")
            except NoMatches:
                return
            compose.mount(
                Horizontal(
                    Static(label, id="hub-reply-target", classes="hub-reply-target"),
                    TextButton("✕", id="hub-reply-cancel", classes="hub-row-action"),
                    id="hub-reply-banner",
                    classes="hub-reply-banner",
                ),
                before=0,
            )
        try:
            self.query_one("#hub-comment-input", Input).focus()
        except NoMatches:
            pass

    def _cancel_reply(self, *, refocus: bool = False) -> None:
        self._reply_to = None
        for banner in self.query("#hub-reply-banner"):
            banner.remove()
        if refocus:
            try:
                self.query_one("#hub-comment-input", Input).focus()
            except NoMatches:
                pass

    @on(Input.Submitted, "#hub-comment-input")
    def _on_comment_input_submitted(self, event: Input.Submitted) -> None:
        self._submit_comment(event.value)

    def _extract_mentions(self, text: str) -> list[str]:
        """Real project members @-mentioned in `text`, no repeats, first-seen order.

        Looking each one up in the member list keeps a stray '@word' or an
        email from becoming a fake mention notification.
        """
        by_username = {
            member.username.casefold(): member.username
            for member in self._members
            if member.username
        }
        seen: list[str] = []
        for token in _MENTION_RE.findall(text):
            # Try again without a trailing '.' so an end-of-sentence "@alice."
            # still matches. The captured token keeps the period, which won't.
            canon = by_username.get(token.casefold()) or by_username.get(
                token.rstrip(".").casefold()
            )
            if canon and canon not in seen:
                seen.append(canon)
        return seen

    def _submit_comment(self, text: str) -> None:
        issue_key = self._detail_issue_key
        text = text.strip()
        if issue_key is None or not text:
            return
        # Capture the reply target and thread count now. The user may re-aim or
        # the thread may rebuild while the post is still sending, but this comment
        # still lands where it was aimed and only adds into its own thread.
        parent_id = self._reply_to
        mentions = self._extract_mentions(text)
        self.run_worker(
            self._post_comment(issue_key, text, parent_id, self._comment_gen, mentions),
            exclusive=True,
            group="hub-comment-post",
        )

    async def _post_comment(
        self,
        issue_key: str,
        text: str,
        parent_id: int | None,
        comment_gen: int,
        mentions: list[str],
    ) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            response = await client.comments.create_comment(
                issue_key,
                text,
                parent_comment_id=parent_id,
                mentioned_usernames=mentions or None,
            )
        except TissueApiError as error:
            log.debug("Hub: failed to add comment to %s: %s", issue_key, error)
            # Leave the composer and reply banner intact so the user can retry.
            # Clearing them would silently turn a retried reply into a root post.
            # Only show the message if they're still on the issue it was meant for.
            if self._detail_issue_key == issue_key:
                self.app.notify("Failed to add comment.", severity="error")
            return
        if self._detail_issue_key != issue_key:
            return
        # Add to the end instead of reloading the whole thread, since a full
        # reload resets the scroll and flickers. Only when the count still
        # matches, though. A mismatch means a detail redraw already re-fetched
        # the comments, so skip to avoid adding a stale copy.
        if self._comment_gen == comment_gen:
            if parent_id is not None:
                await self._append_reply(response, text, parent_id)
            else:
                await self._append_comment(response, text)
        # Reset the composer only if it's still aimed where this post came from.
        # The user may have re-aimed at another comment, whose banner we must keep.
        if self._reply_to == parent_id:
            try:
                self.query_one("#hub-comment-input", Input).value = ""
            except NoMatches:
                pass
            if parent_id is not None:
                self._cancel_reply()

    async def _append_reply(
        self, response: CommentCreateResponse, text: str, parent_id: int
    ) -> None:
        """Add the just-posted reply indented beneath its parent.

        Shown right away (before the server confirms), so it sits newest-first.
        The next full reload puts back the server's oldest-first order.
        """
        try:
            comments_box = self.query_one("#hub-comments")
            parent_body = self.query_one(f"#hub-comment-body-{parent_id}")
        except NoMatches:
            return
        comment = self._optimistic_comment(response, text)
        await comments_box.mount(self._reply_block(comment), after=parent_body)
        try:
            self.query_one("#hub-comment-input", Input).scroll_visible(animate=False)
        except NoMatches:
            pass

    async def _append_comment(self, response: CommentCreateResponse, text: str) -> None:
        try:
            comments_box = self.query_one("#hub-comments")
        except NoMatches:
            return
        # Drop the empty/loading placeholder when the first comment lands.
        await comments_box.query(".hub-muted").remove()
        comment = self._optimistic_comment(response, text)
        await comments_box.mount(*self._comment_widgets(comment))
        try:
            self.query_one("#hub-comment-input", Input).scroll_visible(animate=False)
        except NoMatches:
            pass

    def _optimistic_comment(
        self, response: CommentCreateResponse, text: str
    ) -> CommentDetailResponse:
        """A local stand-in for the just-posted comment, added without re-fetching.

        Author comes from the saved profile. The real version from the server
        lands on the next reload.
        """
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
