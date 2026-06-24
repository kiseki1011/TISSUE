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
# An @mention token in a comment body. The '@' must begin a word (line start or
# after whitespace) — same boundary the MentionAutoComplete enforces — so an email
# or "user@host" never reads as a mention. Resolved against the roster before
# sending, so a stray '@foo' that isn't a real member is simply ignored.
_MENTION_RE = re.compile(r"(?<![^\s])@([A-Za-z0-9_.\-]+)")

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

    def on_key(self, event: events.Key) -> None:
        # While composing a reply (banner up, input focused), Esc backs out of
        # reply mode rather than the screen's default Esc (leave search). Scoped so
        # Esc is untouched whenever there's no active reply.
        if (
            event.key == "escape"
            and self._reply_to is not None
            and self.app.focused is not None
            and self.app.focused.id == "hub-comment-input"
        ):
            self._cancel_reply(refocus=True)
            event.stop()

    def _comment_section(self, comments: list[CommentDetailResponse]) -> list[Widget]:
        """Comments header, the (already-loaded) thread, and the add form. Rendered
        with the rest of the detail in one paint — `_render_issue_detail` fetches
        the comments first so there's no async "Loading…" → thread swap."""
        # The thread is rebuilt on every detail render; drop any reply target left
        # over from the previous issue and repopulate as the roots are built. Bump
        # the generation so an in-flight post worker that captured the old one skips
        # mounting into this freshly-rebuilt thread (that render re-fetched the
        # comments, so its own copy is already there).
        self._reply_to = None
        self._reply_targets = {}
        self._comment_gen += 1
        if comments:
            loaded: list[Widget] = []
            for comment in comments:
                loaded.extend(self._comment_widgets(comment))
        else:
            loaded = [Static("No comments yet.", classes="hub-muted")]
        # Built once so the MentionAutoComplete can target this exact Input instance.
        comment_input = Input(
            placeholder="Add a comment with enter…",
            id="hub-comment-input",
            classes="hub-comment-input",
        )
        return [
            Rule(),
            Static("Comments", classes="hub-section-title"),
            Vertical(*loaded, id="hub-comments", classes="hub-comments"),
            # A wrapper so the "Replying to @…" banner can mount above the input.
            Vertical(
                Horizontal(comment_input, classes="hub-comment-form"),
                # Typing '@' opens a member dropdown (matched by username/name).
                MentionAutoComplete(comment_input, members=lambda: self._members),
                id="hub-comment-compose",
            ),
        ]

    def _comment_widgets(self, c: CommentDetailResponse) -> list[Widget]:
        """A root comment: its head + body, then each reply as a rail-indented
        block (the thread caps at one level deep)."""
        head, body = self._head_body(c, is_root=True)
        out: list[Widget] = [head, body]
        for reply_c in c.replies or []:
            out.append(self._reply_block(reply_c))
        return out

    def _reply_block(self, c: CommentDetailResponse) -> Vertical:
        """A reply's head + body inside a Vertical whose left border draws the
        thread rail showing it's nested under its parent."""
        head, body = self._head_body(c, is_root=False)
        return Vertical(head, body, classes="hub-reply-block")

    def _head_body(
        self, c: CommentDetailResponse, *, is_root: bool
    ) -> tuple[Widget, Widget]:
        """The meta line + body for one comment. The body renders as Markdown (a
        deleted body is a plain italic placeholder). A live root gets a ↳ Reply
        action and a tagged body that an optimistic reply mounts beneath."""
        author = (c.author.display_name or c.author.username) if c.author else None
        author = author or "?"
        username = c.author.username if c.author else None
        # Show "(@username)" beside the display name to disambiguate same-named
        # members; omitted when the display name already is the username.
        shown = f"{author} (@{username})" if username and username != author else author
        meta = " · ".join([shown, format_relative(c.created_at)])
        if c.is_edited:
            meta += " (edited)"
        body: Widget
        if c.is_deleted:
            body = Static(Text("[deleted]", style="italic"), classes="hub-comment-body")
        else:
            body = Markdown((c.content or "").strip(), classes="hub-comment-body")
        head: Widget = Static(meta, markup=False, classes="hub-comment-meta")
        cid = c.comment_id
        if is_root and cid is not None:
            # Anchor for optimistic replies (mounted right after this body).
            body.id = f"hub-comment-body-{cid}"
            # Replies cap at one level, so only roots get a Reply action.
            if not c.is_deleted:
                self._reply_targets[cid] = author
                reply = TextButton(
                    "↳",
                    id=f"{_REPLY_PREFIX}{cid}",
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
        bid = event.button.id or ""
        if not bid.startswith(_REPLY_PREFIX):
            return
        try:
            self._begin_reply(int(bid[len(_REPLY_PREFIX) :]))
        except ValueError:
            pass

    @on(Button.Pressed, "#hub-reply-cancel")
    def _on_reply_cancel(self, event: Button.Pressed) -> None:
        event.stop()
        self._cancel_reply(refocus=True)

    def _begin_reply(self, comment_id: int) -> None:
        """Aim the input at `comment_id`: show (or update) the banner and focus."""
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
        """The @-mentioned usernames in `text` that are real project members, in
        first-seen order (deduped). Resolving against the roster keeps a stray
        '@word' (or an email) from becoming a bogus mention notification."""
        by_username = {
            m.username.casefold(): m.username for m in self._members if m.username
        }
        seen: list[str] = []
        for token in _MENTION_RE.findall(text):
            # Retry without a trailing '.' so an end-of-sentence "@alice." still
            # resolves (the captured token keeps the period, which won't match).
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
        # Capture the reply target and thread generation now: the user may re-aim
        # (or the thread may be rebuilt) while the post is in flight, but this
        # comment still lands where it was aimed and only mounts into its own thread.
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
        gen: int,
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
        except TissueApiError as e:
            log.debug("Hub: failed to add comment to %s: %s", issue_key, e)
            # Leave the composer and any reply banner intact so the user can retry
            # (clearing them would silently turn a retried reply into a root post);
            # only toast if they're still looking at the issue it was meant for.
            if self._detail_issue_key == issue_key:
                self.app.notify("Failed to add comment.", severity="error")
            return
        if self._detail_issue_key != issue_key:
            return
        # Append the new comment instead of reloading the whole thread (a full reload
        # resets the scroll, which reads as a flicker) — but only into the thread it
        # was composed against. A detail re-render bumps the generation and re-fetches
        # the comments, so the new one is already there; skip to avoid a stale mount.
        if self._comment_gen == gen:
            if parent_id is not None:
                await self._append_reply(response, text, parent_id)
            else:
                await self._append_comment(response, text)
        # Reset the composer only if it's still aimed where this post came from; the
        # user may have re-aimed at another comment (whose banner we must not wipe).
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
        """Mount the just-posted reply indented beneath its parent. If the parent
        already has replies the optimistic one shows first (newest); the next full
        reload restores the server's oldest-first order."""
        try:
            box = self.query_one("#hub-comments")
            parent_body = self.query_one(f"#hub-comment-body-{parent_id}")
        except NoMatches:
            return
        comment = self._optimistic_comment(response, text)
        await box.mount(self._reply_block(comment), after=parent_body)
        try:
            self.query_one("#hub-comment-input", Input).scroll_visible(animate=False)
        except NoMatches:
            pass

    async def _append_comment(self, response: CommentCreateResponse, text: str) -> None:
        try:
            box = self.query_one("#hub-comments")
        except NoMatches:
            return
        # Drop the "No comments yet." / "Loading…" placeholder on the first comment.
        await box.query(".hub-muted").remove()
        comment = self._optimistic_comment(response, text)
        await box.mount(*self._comment_widgets(comment))
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
