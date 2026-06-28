from __future__ import annotations

import logging
import re
from dataclasses import dataclass
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


@dataclass(frozen=True)
class CommentDraft:
    issue_key: str
    text: str
    parent_id: int | None
    comment_gen: int
    mentions: list[str]


class CommentsMixin(ProjectHomeBase):
    """Comment thread in issue detail."""

    def on_descendant_focus(self, event: events.DescendantFocus) -> None:
        if event.widget.id == "hub-comment-input":
            event.widget.scroll_visible(animate=False)

    def on_key(self, event: events.Key) -> None:
        if (
            event.key == "escape"
            and self._comment_state.reply_to is not None
            and self.app.focused is not None
            and self.app.focused.id == "hub-comment-input"
        ):
            self._cancel_reply(refocus=True)
            event.stop()

    def _comment_section(self, comments: list[CommentDetailResponse]) -> list[Widget]:
        self._reset_comment_compose_state()
        loaded = self._loaded_comment_widgets(comments)
        comment_input = self._comment_input()
        return [
            Rule(),
            Static("Comments", classes="hub-section-title"),
            Vertical(*loaded, id="hub-comments", classes="hub-comments"),
            Vertical(
                Horizontal(comment_input, classes="hub-comment-form"),
                MentionAutoComplete(
                    comment_input, members=lambda: self._member_list.members
                ),
                id="hub-comment-compose",
            ),
        ]

    def _reset_comment_compose_state(self) -> None:
        self._comment_state.reply_to = None
        self._comment_state.reply_targets = {}
        self._comment_state.generation += 1

    def _loaded_comment_widgets(
        self, comments: list[CommentDetailResponse]
    ) -> list[Widget]:
        if not comments:
            return [Static("No comments yet.", classes="hub-muted")]
        loaded: list[Widget] = []
        for comment in comments:
            loaded.extend(self._comment_widgets(comment))
        return loaded

    @staticmethod
    def _comment_input() -> Input:
        return Input(
            placeholder="Add a comment with enter…",
            id="hub-comment-input",
            classes="hub-comment-input",
        )

    def _comment_widgets(self, comment: CommentDetailResponse) -> list[Widget]:
        head, body = self._head_body(comment, is_root=True)
        out: list[Widget] = [head, body]
        for reply in comment.replies or []:
            out.append(self._reply_block(reply))
        return out

    def _reply_block(self, comment: CommentDetailResponse) -> Vertical:
        head, body = self._head_body(comment, is_root=False)
        return Vertical(head, body, classes="hub-reply-block")

    def _head_body(
        self, comment: CommentDetailResponse, *, is_root: bool
    ) -> tuple[Widget, Widget]:
        author = (
            (comment.author.display_name or comment.author.username)
            if comment.author
            else None
        )
        author = author or "?"
        username = comment.author.username if comment.author else None
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
            body.id = f"hub-comment-body-{comment_id}"
            if not comment.is_deleted:
                self._comment_state.reply_targets[comment_id] = author
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
        self._comment_state.reply_to = comment_id
        author = self._comment_state.reply_targets.get(comment_id, "?")
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
        self._comment_state.reply_to = None
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
        by_username = {
            member.username.casefold(): member.username
            for member in self._member_list.members
            if member.username
        }
        seen: list[str] = []
        for token in _MENTION_RE.findall(text):
            canon = by_username.get(token.casefold()) or by_username.get(
                token.rstrip(".").casefold()
            )
            if canon and canon not in seen:
                seen.append(canon)
        return seen

    def _submit_comment(self, text: str) -> None:
        issue_key = self._detail_state.issue_key
        text = text.strip()
        if issue_key is None or not text:
            return
        self.run_worker(
            self._post_comment(self._comment_draft(issue_key, text)),
            exclusive=True,
            group="hub-comment-post",
        )

    def _comment_draft(self, issue_key: str, text: str) -> CommentDraft:
        return CommentDraft(
            issue_key=issue_key,
            text=text,
            parent_id=self._comment_state.reply_to,
            comment_gen=self._comment_state.generation,
            mentions=self._extract_mentions(text),
        )

    async def _post_comment(self, draft: CommentDraft) -> None:
        client = self.app.client
        if client is None:
            return
        try:
            response = await client.comments.create_comment(
                draft.issue_key,
                draft.text,
                parent_comment_id=draft.parent_id,
                mentioned_usernames=draft.mentions or None,
            )
        except TissueApiError as error:
            log.debug("Hub: failed to add comment to %s: %s", draft.issue_key, error)
            if self._detail_state.issue_key == draft.issue_key:
                self.app.notify("Failed to add comment.", severity="error")
            return
        self._detail_state.cache.pop(draft.issue_key, None)
        if self._detail_state.issue_key != draft.issue_key:
            return
        await self._append_posted_comment(response, draft)
        self._reset_comment_input(draft)

    async def _append_posted_comment(
        self, response: CommentCreateResponse, draft: CommentDraft
    ) -> None:
        if self._comment_state.generation != draft.comment_gen:
            return
        if draft.parent_id is not None:
            await self._append_reply(response, draft.text, draft.parent_id)
        else:
            await self._append_comment(response, draft.text)

    def _reset_comment_input(self, draft: CommentDraft) -> None:
        if self._comment_state.reply_to == draft.parent_id:
            try:
                self.query_one("#hub-comment-input", Input).value = ""
            except NoMatches:
                pass
            if draft.parent_id is not None:
                self._cancel_reply()

    async def _append_reply(
        self, response: CommentCreateResponse, text: str, parent_id: int
    ) -> None:
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
